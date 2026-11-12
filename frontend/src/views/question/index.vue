<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.courseId" placeholder="全部课程" clearable filterable style="width: 240px">
        <el-option v-for="c in courses" :key="c.id" :label="c.title" :value="c.id" />
      </el-select>
      <el-select v-model="query.type" placeholder="题型" clearable style="width: 130px">
        <el-option label="单选题" value="SINGLE" />
        <el-option label="多选题" value="MULTI" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增题目</el-button>
      <el-button type="warning" :icon="MagicStick" @click="aiVisible = true">AI 生成</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="stem" label="题干" min-width="280" show-overflow-tooltip />
      <el-table-column label="题型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.type === 'MULTI' ? 'warning' : 'primary'" size="small">
            {{ row.type === 'MULTI' ? '多选' : '单选' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="正确答案" width="110" align="center">
        <template #default="{ row }">
          <el-tag type="success" size="small">{{ row.answer }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="分值" width="80" align="center" />
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @current-change="load"
        @size-change="search" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑题目' : '新增题目'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="选择课程" filterable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.title" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-radio-group v-model="form.type">
            <el-radio value="SINGLE">单选题</el-radio>
            <el-radio value="MULTI">多选题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" prop="stem">
          <el-input v-model="form.stem" type="textarea" :rows="2" maxlength="300" show-word-limit />
        </el-form-item>
        <el-form-item label="选项">
          <div style="width: 100%">
            <div v-for="(opt, idx) in form.optionList" :key="idx" class="option-row">
              <span class="key">{{ opt.k }}</span>
              <el-input v-model="opt.v" placeholder="选项内容" />
              <el-button link type="danger" :disabled="form.optionList.length <= 2" @click="removeOption(idx)">
                删除
              </el-button>
            </div>
            <el-button link type="primary" :disabled="form.optionList.length >= 6" @click="addOption">
              + 添加选项
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="正确答案" prop="answer">
          <el-select v-if="form.type === 'SINGLE'" v-model="form.answer" placeholder="选择正确答案" style="width: 100%">
            <el-option v-for="opt in form.optionList" :key="opt.k" :label="`${opt.k}. ${opt.v}`" :value="opt.k" />
          </el-select>
          <el-checkbox-group v-else v-model="form.answerList">
            <el-checkbox v-for="opt in form.optionList" :key="opt.k" :value="opt.k">
              {{ opt.k }}. {{ opt.v }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="form.score" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI 自动出题：生成 → 预览勾选 → 入库 -->
    <AiDialog v-model="aiVisible" @saved="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, MagicStick } from '@element-plus/icons-vue'
import { questionPage, courseOptions, createQuestion, updateQuestion, deleteQuestion } from '@/api'
import AiDialog from './ai-dialog.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const courses = ref([])
const dialogVisible = ref(false)
const aiVisible = ref(false)
const formRef = ref()

const query = reactive({ current: 1, size: 10, courseId: null, type: '' })
const form = reactive({
  id: null, courseId: null, type: 'SINGLE', stem: '',
  optionList: [], answer: '', answerList: [], score: 10
})

const rules = {
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  stem: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  answer: [{ validator: (rule, value, cb) => (value ? cb() : cb(new Error('请选择正确答案'))), trigger: 'change' }]
}

const defaultOptions = () => [
  { k: 'A', v: '' }, { k: 'B', v: '' }, { k: 'C', v: '' }, { k: 'D', v: '' }
]

const load = async () => {
  loading.value = true
  try {
    const data = await questionPage(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.current = 1
  load()
}

const reset = () => {
  Object.assign(query, { current: 1, size: 10, courseId: null, type: '' })
  load()
}

const openCreate = () => {
  Object.assign(form, {
    id: null, courseId: query.courseId || null, type: 'SINGLE', stem: '',
    optionList: defaultOptions(), answer: '', answerList: [], score: 10
  })
  dialogVisible.value = true
}

const openEdit = row => {
  let optionList = defaultOptions()
  try {
    const parsed = JSON.parse(row.options)
    if (Array.isArray(parsed) && parsed.length) optionList = parsed
  } catch (e) {
    // 老数据格式异常时用默认选项兜底
  }
  const answers = (row.answer || '').split(',').filter(Boolean)
  Object.assign(form, {
    id: row.id, courseId: row.courseId, type: row.type, stem: row.stem,
    optionList, answer: answers[0] || '', answerList: answers, score: row.score
  })
  dialogVisible.value = true
}

const addOption = () => {
  const key = String.fromCharCode(65 + form.optionList.length)
  form.optionList.push({ k: key, v: '' })
}

const removeOption = idx => {
  form.optionList.splice(idx, 1)
  // 重新编号，保证答案和选项对应
  form.optionList.forEach((opt, i) => (opt.k = String.fromCharCode(65 + i)))
}

watch(() => form.type, type => {
  if (type === 'SINGLE') {
    form.answerList = []
  } else {
    form.answer = ''
  }
})

const onSubmit = async () => {
  if (form.type === 'MULTI' && form.answerList.length === 0) {
    ElMessage.warning('请至少选择一个正确答案')
    return
  }
  await formRef.value.validate()
  const answer = form.type === 'SINGLE' ? form.answer : form.answerList.join(',')
  const payload = {
    id: form.id,
    courseId: form.courseId,
    type: form.type,
    stem: form.stem,
    options: JSON.stringify(form.optionList),
    answer,
    score: form.score
  }
  saving.value = true
  try {
    if (form.id) {
      await updateQuestion(payload)
    } else {
      await createQuestion(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onDelete = async row => {
  await ElMessageBox.confirm('确定删除这道题目吗？', '提示', { type: 'warning' })
  await deleteQuestion(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  courses.value = await courseOptions()
  load()
})
</script>

<style scoped>
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.option-row .key {
  width: 20px;
  font-weight: 600;
  color: #606266;
  text-align: center;
}
</style>
