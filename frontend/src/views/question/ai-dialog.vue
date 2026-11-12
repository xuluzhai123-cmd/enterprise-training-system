<template>
  <el-dialog :model-value="modelValue" title="AI 自动出题" width="960px" top="6vh" :close-on-click-modal="false"
    @update:model-value="val => emit('update:modelValue', val)" @closed="onClosed">
    <!-- 第一步：告诉 AI 出什么题 -->
    <el-form :model="form" label-width="90px" class="ai-form">
      <el-form-item label="课程">
        <el-select v-model="form.courseId" placeholder="选择课程" filterable style="width: 320px">
          <el-option v-for="c in courses" :key="c.id" :label="c.title" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="题目数量">
        <el-radio-group v-model="form.count">
          <el-radio-button :value="3">3 道</el-radio-button>
          <el-radio-button :value="5">5 道</el-radio-button>
          <el-radio-button :value="10">10 道</el-radio-button>
        </el-radio-group>
        <span class="tip">一次最多 10 道，越多越慢</span>
      </el-form-item>
      <el-form-item label="难度">
        <el-radio-group v-model="form.difficulty">
          <el-radio-button value="简单">简单</el-radio-button>
          <el-radio-button value="中等">中等</el-radio-button>
          <el-radio-button value="困难">困难</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="题型">
        <el-radio-group v-model="form.type">
          <el-radio-button value="MIXED">混合</el-radio-button>
          <el-radio-button value="SINGLE">单选</el-radio-button>
          <el-radio-button value="MULTI">多选</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" :icon="MagicStick" @click="onGenerate">
          {{ loading ? '模型正在出题…' : '开始生成' }}
        </el-button>
        <span v-if="meta" class="tip">
          本次消耗 {{ meta.tokens }} tokens / {{ meta.costMs }}ms
          <template v-if="meta.dropped">· 模型返回中有 {{ meta.dropped }} 道格式不合法已丢弃</template>
        </span>
      </el-form-item>
    </el-form>

    <!-- 第二步：人来把关，勾选要入库的题 -->
    <div v-if="items.length" class="preview">
      <div class="preview-head">
        <span>共 {{ items.length }} 道，已选 {{ selectedCount }} 道</span>
        <el-button link type="primary" @click="toggleAll">全选 / 反选</el-button>
      </div>
      <el-table :data="items" border stripe max-height="300" size="small">
        <el-table-column width="46" align="center">
          <template #header>
            <el-checkbox :model-value="allSelected" @change="onToggleAll" />
          </template>
          <template #default="{ row }">
            <el-checkbox v-model="row.selected" />
          </template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column label="题型" width="84" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'MULTI' ? 'warning' : 'primary'" size="small">
              {{ row.type === 'MULTI' ? '多选' : '单选' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选项" min-width="240">
          <template #default="{ row }">
            <div v-for="opt in row.options" :key="opt.k" class="opt">
              {{ opt.k }}. {{ opt.v }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="answer" label="答案" width="80" align="center" />
        <el-table-column prop="score" label="分值" width="64" align="center" />
        <el-table-column prop="explain" label="解析" min-width="150" show-overflow-tooltip />
      </el-table>
    </div>

    <el-empty v-else-if="!loading" description="选好课程和数量，点「开始生成」看看 AI 出的题" :image-size="70" />

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="success" :disabled="!selectedCount" :loading="saving" @click="onSave">
        保存选中 {{ selectedCount }} 道
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { aiGenerateQuestions, aiSaveQuestions, courseOptions } from '@/api'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'saved'])

const courses = ref([])
const items = ref([])
const loading = ref(false)
const saving = ref(false)
const meta = ref(null)

const form = reactive({ courseId: null, count: 5, difficulty: '中等', type: 'MIXED' })

const selectedCount = computed(() => items.value.filter(i => i.selected).length)
const allSelected = computed(() => items.value.length > 0 && selectedCount.value === items.value.length)

// 每次打开弹窗拉一次课程下拉（课程可能刚加过）
watch(
  () => props.modelValue,
  async val => {
    if (!val) return
    if (!courses.value.length) {
      courses.value = await courseOptions()
    }
    // 默认选中第一门课，省一次点击
    if (!form.courseId) {
      form.courseId = courses.value[0]?.id ?? null
    }
  }
)

const onGenerate = async () => {
  if (!form.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }
  loading.value = true
  items.value = []
  meta.value = null
  try {
    const data = await aiGenerateQuestions({ ...form })
    items.value = (data.questions || []).map(q => ({ ...q, selected: true }))
    meta.value = { tokens: data.tokens, costMs: data.costMs, dropped: data.dropped }
    ElMessage.success(`生成成功，共 ${items.value.length} 道`)
  } finally {
    loading.value = false
  }
}

const onToggleAll = val => {
  items.value.forEach(i => (i.selected = !!val))
}

const toggleAll = () => {
  const target = !allSelected.value
  items.value.forEach(i => (i.selected = target))
}

const onSave = async () => {
  const picked = items.value.filter(i => i.selected)
  if (!picked.length) return
  saving.value = true
  try {
    const count = await aiSaveQuestions({
      courseId: form.courseId,
      // 只传后端需要的字段（explain 不存库，但留着也无害）
      questions: picked.map(({ selected, ...rest }) => rest)
    })
    ElMessage.success(`已入库 ${count} 道题`)
    emit('saved')
    emit('update:modelValue', false)
  } finally {
    saving.value = false
  }
}

const onClosed = () => {
  items.value = []
  meta.value = null
}
</script>

<style scoped>
.ai-form {
  margin-bottom: 6px;
}
.tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
.preview {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}
.opt {
  line-height: 20px;
  font-size: 12px;
  color: #606266;
}
</style>
