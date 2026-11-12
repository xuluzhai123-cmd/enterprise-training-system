<template>
  <div v-loading="loading">
    <el-page-header content="在线考试" @back="router.back()" />

    <el-card v-if="!submitted" shadow="never" style="margin-top: 14px">
      <div class="exam-head">
        <div>
          <div class="title">{{ paper.className }} · {{ paper.courseTitle }}</div>
          <div class="sub">共 {{ paper.questions?.length || 0 }} 题，总分 {{ paper.totalScore }} 分，及格线 60%</div>
        </div>
        <div class="timer" :class="{ danger: remain <= 60 }">
          <el-icon><Timer /></el-icon>
          剩余 {{ formatTime(remain) }}
        </div>
        <el-button type="primary" :loading="submitting" @click="onSubmit">交 卷</el-button>
      </div>
    </el-card>

    <el-card v-for="(q, index) in paper.questions" :key="q.id" shadow="never" class="question-card">
      <div class="stem">
        <span class="no">{{ index + 1 }}.</span>
        <el-tag size="small" :type="q.type === 'MULTI' ? 'warning' : 'primary'" effect="plain">
          {{ q.type === 'MULTI' ? '多选' : '单选' }}
        </el-tag>
        <span class="stem-text">{{ q.stem }}</span>
        <span class="score">{{ q.score }} 分</span>
      </div>

      <el-radio-group v-if="q.type !== 'MULTI'" v-model="answers[q.id]" class="options">
        <el-radio v-for="opt in parseOptions(q.options)" :key="opt.k" :value="opt.k" class="option">
          {{ opt.k }}. {{ opt.v }}
        </el-radio>
      </el-radio-group>

      <el-checkbox-group v-else v-model="answers[q.id]" class="options">
        <el-checkbox v-for="opt in parseOptions(q.options)" :key="opt.k" :value="opt.k" class="option">
          {{ opt.k }}. {{ opt.v }}
        </el-checkbox>
      </el-checkbox-group>
    </el-card>

    <div v-if="!submitted" class="submit-bar">
      <el-button type="primary" size="large" :loading="submitting" @click="onSubmit">交 卷</el-button>
    </div>

    <el-dialog v-model="resultVisible" title="考试结果" width="420px" :close-on-click-modal="false"
      :show-close="false">
      <div class="result">
        <el-progress type="circle" :percentage="resultPercent" :width="140"
          :color="result.passed === 1 ? '#67c23a' : '#f56c6c'">
          <template #default>
            <div class="result-score">{{ result.score }}</div>
            <div class="result-total">/ {{ result.totalScore }} 分</div>
          </template>
        </el-progress>
        <p class="result-text">
          答对 {{ result.correctCount }} / {{ result.totalCount }} 题，{{ result.passed === 1 ? '恭喜通过！' : '未达到及格线，请继续努力' }}
        </p>
      </div>
      <template #footer>
        <el-button @click="goBack">返回我的培训</el-button>
        <el-button type="primary" @click="retry">再考一次</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer } from '@element-plus/icons-vue'
import { startExam, submitExam } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const paper = reactive({ classId: null, className: '', courseTitle: '', duration: 20, totalScore: 0, questions: [] })
const answers = reactive({})
const remain = ref(0)
const startTime = ref('')
const resultVisible = ref(false)
const result = ref({ score: 0, totalScore: 0, correctCount: 0, totalCount: 0, passed: 0 })

let timer = null

const parseOptions = options => {
  try {
    const parsed = JSON.parse(options)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

const formatTime = seconds => {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0')
  const s = (seconds % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

const resultPercent = computed(() => {
  if (!result.value.totalScore) return 0
  return Math.round((result.value.score * 100) / result.value.totalScore)
})

const now = () => {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const start = async () => {
  loading.value = true
  try {
    const data = await startExam(route.params.classId)
    Object.assign(paper, data)
    paper.questions.forEach(q => {
      answers[q.id] = q.type === 'MULTI' ? [] : ''
    })
    remain.value = (data.duration || 20) * 60
    startTime.value = now()
    timer = setInterval(() => {
      remain.value -= 1
      if (remain.value <= 0) {
        clearInterval(timer)
        ElMessage.warning('考试时间到，系统自动交卷')
        onSubmit(true)
      }
    }, 1000)
  } finally {
    loading.value = false
  }
}

const buildAnswers = () => {
  const payload = {}
  paper.questions.forEach(q => {
    const value = answers[q.id]
    if (Array.isArray(value)) {
      if (value.length) payload[q.id] = value.join(',')
    } else if (value) {
      payload[q.id] = value
    }
  })
  return payload
}

const onSubmit = async auto => {
  if (submitted.value) return
  const payload = buildAnswers()
  if (!auto && Object.keys(payload).length === 0) {
    ElMessage.warning('还没有作答，不能交卷')
    return
  }
  if (!auto) {
    await ElMessageBox.confirm(
      `已作答 ${Object.keys(payload).length} / ${paper.questions.length} 题，确定交卷吗？`,
      '交卷确认',
      { type: 'warning' }
    )
  }
  submitting.value = true
  try {
    const data = await submitExam({
      classId: paper.classId,
      startTime: startTime.value,
      answers: payload
    })
    result.value = data
    submitted.value = true
    clearInterval(timer)
    resultVisible.value = true
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.push('/my')

const retry = () => {
  resultVisible.value = false
  submitted.value = false
  Object.keys(answers).forEach(key => delete answers[key])
  start()
}

onMounted(start)

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.exam-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.sub {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.timer {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
  font-variant-numeric: tabular-nums;
}

.timer.danger {
  color: #f56c6c;
}

.question-card {
  margin-top: 14px;
}

.stem {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 15px;
  line-height: 1.7;
}

.stem .no {
  font-weight: 700;
}

.stem .stem-text {
  flex: 1;
}

.stem .score {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding-left: 22px;
}

.option {
  height: auto;
  white-space: normal;
  margin-right: 0;
}

.submit-bar {
  text-align: center;
  margin: 22px 0 8px;
}

.result {
  text-align: center;
  padding: 10px 0 4px;
}

.result-score {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
}

.result-total {
  font-size: 12px;
  color: #909399;
}

.result-text {
  color: #606266;
  margin-top: 14px;
}
</style>
