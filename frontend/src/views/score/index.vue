<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.classId" placeholder="全部培训班" clearable filterable style="width: 240px">
        <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <el-button type="warning" :icon="MagicStick" :disabled="!query.classId" :loading="batching" @click="onBatch">
        批量生成评语
      </el-button>
      <span v-if="!query.classId" style="color: #909399; font-size: 12px">先选一个培训班</span>
      <div class="spacer"></div>
      <el-tag type="success" effect="plain">通过率：{{ passRate }}%</el-tag>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="realName" label="学员" width="110" />
      <el-table-column prop="className" label="培训班" min-width="180" show-overflow-tooltip />
      <el-table-column prop="courseTitle" label="课程" min-width="140" show-overflow-tooltip />
      <el-table-column label="成绩" width="130" align="center">
        <template #default="{ row }">
          <span :class="row.passed === 1 ? 'pass' : 'fail'">{{ row.score }} 分</span>
          <el-tag v-if="showAttempt(row)" type="info" size="small" effect="plain" style="margin-left: 6px">
            第{{ row.attemptNo }}次
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.passed === 1 ? 'success' : 'danger'" size="small">
            {{ row.passed === 1 ? '通过' : '未通过' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="考试评语" min-width="240">
        <template #default="{ row }">
          <span v-if="row.aiComment" class="comment">{{ row.aiComment }}</span>
          <el-tag v-else type="info" size="small" effect="plain">未生成</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="MagicStick" @click="openComment(row)">AI 评语</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @current-change="load"
        @size-change="search" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" top="8vh">
      <div v-loading="generating" class="comment-box">
        <p v-if="commentText" class="comment-text">{{ commentText }}</p>
        <el-empty v-else :description="generating ? '正在生成…' : '还没有评语，点下面的按钮生成'" :image-size="70" />
      </div>
      <div v-if="meta" class="meta">{{ meta }}</div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" :icon="MagicStick" :loading="generating" @click="onGenerate">
          {{ commentText ? '重新生成' : '生成评语' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh, Search } from '@element-plus/icons-vue'
import { examRecords, classOptions, aiGenerateComment, aiBatchComment } from '@/api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const classes = ref([])

const query = reactive({ current: 1, size: 10, classId: null })

const passRate = computed(() => {
  if (!list.value.length) return 0
  const passed = list.value.filter(r => r.passed === 1).length
  return Math.round((passed * 100) / list.value.length)
})

// 只有"一次就通过"的学员不标注次数；
// 补考过的（次数 > 1），以及只考了一次但没过的，都要标注是第几次。
const showAttempt = (row) => {
  if (!row || !row.attemptNo) return false
  const count = row.attemptCount || 1
  return count > 1 || row.passed !== 1
}

const load = async () => {
  loading.value = true
  try {
    const data = await examRecords({ ...query, mine: false })
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
  Object.assign(query, { current: 1, size: 10, classId: null })
  load()
}

/* ---------------- AI 评语 ---------------- */
const dialogVisible = ref(false)
const generating = ref(false)
const batching = ref(false)
const current = ref(null)
const commentText = ref('')
const meta = ref('')

const dialogTitle = computed(() => {
  const row = current.value
  if (!row) return 'AI 考试评语'
  const parts = [row.realName || '', `${row.score ?? 0} 分`, row.passed === 1 ? '通过' : '未通过']
  if (showAttempt(row)) parts.push(`第 ${row.attemptNo} 次`)
  return `考试评语 · ${parts.join(' · ')}`
})

const openComment = (row) => {
  current.value = row
  commentText.value = row.aiComment || ''
  meta.value = ''
  dialogVisible.value = true
}

const onGenerate = async () => {
  if (!current.value) return
  generating.value = true
  try {
    const data = await aiGenerateComment({
      examRecordId: current.value.id,
      force: !!commentText.value
    })
    commentText.value = data.content
    current.value.aiComment = data.content
    meta.value = data.cached
      ? '已复用之前生成的评语（没花 token）'
      : `本次消耗 ${data.tokens} tokens / ${data.costMs}ms`
    ElMessage.success(data.cached ? '已读取历史评语' : '评语已生成并保存')
  } finally {
    generating.value = false
  }
}

const onBatch = async () => {
  batching.value = true
  try {
    const data = await aiBatchComment(query.classId, 5)
    if (!data.total) {
      ElMessage.info('这个班的考试记录都已有评语了')
    } else {
      ElMessage.success(`已生成 ${data.generated} 条，失败 ${data.failed} 条，共处理 ${data.total} 条`)
    }
    load()
  } finally {
    batching.value = false
  }
}

onMounted(async () => {
  classes.value = await classOptions()
  load()
})
</script>

<style scoped>
.pass {
  color: #67c23a;
  font-weight: 600;
}

.fail {
  color: #f56c6c;
  font-weight: 600;
}

.comment {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  color: #606266;
}

.comment-box {
  min-height: 120px;
}

.comment-text {
  line-height: 1.9;
  font-size: 14px;
  color: #303133;
  text-indent: 2em;
  margin: 0;
}

.meta {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
