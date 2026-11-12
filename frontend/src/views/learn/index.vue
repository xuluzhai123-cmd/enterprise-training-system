<template>
  <div v-loading="loading">
    <el-page-header :content="className || '课程学习'" @back="router.back()" />

    <el-card shadow="never" style="margin-top: 14px">
      <div class="progress-row">
        <div class="progress-info">
          <span class="label">学习进度</span>
          <el-progress :percentage="progress" :stroke-width="14" style="width: 320px" />
          <span class="hint">每完成一节自动保存</span>
        </div>
        <div class="actions">
          <el-button type="success" :disabled="progress < 100" @click="goExam">去考试</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 14px" header="课程章节">
      <el-collapse v-model="activeNames">
        <el-collapse-item v-for="(chapter, index) in chapters" :key="chapter.id" :name="chapter.id">
          <template #title>
            <div class="chapter-title">
              <span class="index">第 {{ index + 1 }} 节</span>
              <span class="name">{{ chapter.title }}</span>
              <el-tag size="small" :type="chapter.type === 'VIDEO' ? 'primary' : 'success'" effect="plain">
                {{ chapter.type === 'VIDEO' ? '视频' : '文档' }}
              </el-tag>
              <span class="duration">{{ chapter.duration }} 分钟</span>
              <el-tag v-if="doneSet.includes(chapter.id)" type="success" size="small" style="margin-left: auto">
                已学完
              </el-tag>
            </div>
          </template>
          <p class="content">{{ chapter.content || '本节暂无补充说明，请按培训要求完成学习。' }}</p>
          <div v-if="!doneSet.includes(chapter.id)">
            <el-button type="primary" plain size="small" :loading="savingId === chapter.id"
              @click="markDone(chapter)">
              标记本节已学完
            </el-button>
          </div>
          <div v-else style="display: flex; align-items: center; gap: 10px">
            <el-tag type="success" effect="plain" size="small">本节已完成</el-tag>
            <el-button link type="info" size="small" :loading="savingId === chapter.id"
              @click="undoDone(chapter)">
              撤销
            </el-button>
          </div>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-if="!chapters.length" description="该课程还没有配置章节" />
    </el-card>

    <!-- 课程问答：答案只依据本课程章节资料，回答里带 [1][2] 引用编号 -->
    <el-button class="ask-fab" type="primary" round :icon="ChatDotRound" @click="openAsk">
      问 AI 助教
    </el-button>

    <el-drawer v-model="askVisible" :title="`课程问答 · ${courseTitle || className}`" size="460px">
      <div class="chat">
        <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
          <div class="bubble">{{ m.text }}</div>
          <div v-if="m.sources && m.sources.length" class="sources">
            <span class="src-label">依据</span>
            <el-tooltip v-for="s in m.sources" :key="s.no" :content="s.snippet" placement="top">
              <el-tag size="small" effect="plain" type="info">[{{ s.no }}] {{ s.title }}</el-tag>
            </el-tooltip>
          </div>
          <div v-if="m.meta" class="meta">{{ m.meta }}</div>
        </div>
        <div v-if="asking" class="msg ai">
          <div class="bubble typing">正在检索课程资料…</div>
        </div>
      </div>
      <template #footer>
        <el-input v-model="question" :disabled="asking" placeholder="例如：乐观锁是怎么防止超卖的？"
          @keyup.enter="sendAsk">
          <template #append>
            <el-button :loading="asking" @click="sendAsk">发送</el-button>
          </template>
        </el-input>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { learnDetail, toggleLearnChapter, ragAsk, courseOptions } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const savingId = ref(null)
const className = ref('')
const classId = ref(null)
const chapters = ref([])
const doneSet = ref([])
const progress = ref(0)
const activeNames = ref([])

const enrollId = route.params.id

// ---------------- 课程问答（RAG） ----------------
const courseId = ref(null)
const courseTitle = ref('')
const askVisible = ref(false)
const question = ref('')
const asking = ref(false)
const messages = ref([])

const openAsk = () => {
  askVisible.value = true
  if (!messages.value.length) {
    messages.value.push({
      role: 'ai',
      text: `我是《${courseTitle.value || '课程'}》的助教。我只根据章节内容回答，资料里没有的会直接告诉你。`
    })
  }
}

const sendAsk = async () => {
  const q = question.value.trim()
  if (!q || asking.value) return
  messages.value.push({ role: 'user', text: q })
  question.value = ''
  asking.value = true
  try {
    const data = await ragAsk({ courseId: courseId.value, question: q })
    messages.value.push({
      role: 'ai',
      text: data.answer,
      sources: data.sources || [],
      meta: data.tokens ? `tokens ${data.tokens} · ${data.costMs} ms` : ''
    })
  } catch (e) {
    // 失败不吞问题：放回输入框，方便改一版再问
    question.value = q
  } finally {
    asking.value = false
  }
}

const markDone = chapter => toggle(chapter.id, true)
const undoDone = chapter => toggle(chapter.id, false)

const toggle = async (chapterId, done) => {
  savingId.value = chapterId
  try {
    // 进度由后端按「已完成章节 / 总章节」重新计算后返回，前端不再自己算
    const percent = await toggleLearnChapter(enrollId, chapterId, done)
    progress.value = percent
    if (done) {
      if (!doneSet.value.includes(chapterId)) doneSet.value.push(chapterId)
      ElMessage.success(`已记录，当前进度 ${percent}%`)
    } else {
      doneSet.value = doneSet.value.filter(id => id !== chapterId)
      ElMessage.info(`已撤销，当前进度 ${percent}%`)
    }
  } finally {
    savingId.value = null
  }
}

const goExam = () => router.push(`/exam/${classId.value}`)

onMounted(async () => {
  loading.value = true
  try {
    const data = await learnDetail(enrollId)
    className.value = data.className
    classId.value = data.enroll?.classId
    chapters.value = data.chapters || []
    courseId.value = data.courseId ?? data.enroll?.courseId ?? null
    const opts = await courseOptions().catch(() => [])
    courseTitle.value = (opts || []).find(o => o.id === courseId.value)?.title || className.value
    doneSet.value = data.doneChapterIds || []
    progress.value = data.enroll?.progress ?? 0
    activeNames.value = chapters.value.length ? [chapters.value[0].id] : []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.progress-info .label {
  font-weight: 600;
  color: #303133;
}

.progress-info .hint {
  color: #a8abb2;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 10px;
}

.chapter-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding-right: 12px;
}

.chapter-title .index {
  color: #909399;
  font-size: 13px;
}

.chapter-title .name {
  font-weight: 600;
}

.chapter-title .duration {
  color: #a8abb2;
  font-size: 13px;
}

.content {
  color: #606266;
  line-height: 1.8;
  margin: 4px 0 12px;
}

.ask-fab {
  position: fixed;
  right: 26px;
  bottom: 34px;
  z-index: 20;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.35);
}

.chat {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 8px;
}

.msg {
  display: flex;
  flex-direction: column;
  max-width: 100%;
}

.msg.user {
  align-items: flex-end;
}

.msg .bubble {
  max-width: 88%;
  padding: 10px 13px;
  border-radius: 10px;
  line-height: 1.75;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg.user .bubble {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 2px;
}

.msg.ai .bubble {
  background: #f4f6f8;
  color: #303133;
  border-top-left-radius: 2px;
}

.msg .bubble.typing {
  color: #909399;
}

.sources {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 7px;
}

.sources .src-label {
  font-size: 12px;
  color: #a8abb2;
}

.meta {
  margin-top: 5px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
