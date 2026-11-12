<template>
  <div class="page-card">
    <el-tabs v-model="tab">
      <el-tab-pane label="我的培训" name="mine">
        <el-table v-loading="loading" :data="myList" border stripe>
          <el-table-column prop="className" label="培训班" min-width="200" show-overflow-tooltip />
          <el-table-column prop="courseTitle" label="课程" min-width="160" show-overflow-tooltip />
          <el-table-column label="审批状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="学习进度" width="160" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.progress || 0" :stroke-width="10" />
            </template>
          </el-table-column>
          <el-table-column label="成绩" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.score !== null && row.score !== undefined">
                {{ row.score }} 分
                <el-tag :type="row.passed === 1 ? 'success' : 'danger'" size="small">
                  {{ row.passed === 1 ? '通过' : '未通过' }}
                </el-tag>
              </span>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 1">
                <el-button link type="primary" @click="goLearn(row)">去学习</el-button>
                <el-button link type="success" @click="goExam(row)">参加考试</el-button>
                <el-button link type="danger" @click="onCancel(row)">取消报名</el-button>
              </template>
              <template v-else-if="row.status === 0">
                <el-button link type="danger" @click="onCancel(row)">取消报名</el-button>
              </template>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="培训班报名" name="browse">
        <el-row :gutter="14">
          <el-col v-for="item in classList" :key="item.id" :xs="24" :sm="12" :md="8" style="margin-bottom: 14px">
            <el-card shadow="hover">
              <div class="class-title">{{ item.name }}</div>
              <div class="class-meta">
                <p>课程：{{ item.courseName || '-' }}</p>
                <p>周期：{{ item.startDate }} ~ {{ item.endDate }}</p>
                <p>名额：{{ item.enrolledCount }} / {{ item.quota }}</p>
              </div>
              <el-button type="primary" :disabled="!item.canApply" style="width: 100%" @click="onApply(item)">
                {{ item.canApply ? '立即报名' : (item.status !== 1 ? '未开放' : '已报名/已满') }}
              </el-button>
            </el-card>
          </el-col>
          <el-col v-if="!classList.length">
            <el-empty description="暂无可报名的培训班" />
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myEnroll, classPage, cancelEnroll, applyEnroll } from '@/api'

const router = useRouter()
const tab = ref('mine')
const loading = ref(false)
const myList = ref([])
const classList = ref([])

const statusName = s => ({ 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已取消' }[s] || '-')
const statusTag = s => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info')

const loadMine = async () => {
  loading.value = true
  try {
    myList.value = await myEnroll()
  } finally {
    loading.value = false
  }
}

const loadClasses = async () => {
  const data = await classPage({ current: 1, size: 100, status: 1 })
  classList.value = data.records
}

const goLearn = row => router.push(`/learn/${row.id}`)
const goExam = row => router.push(`/exam/${row.classId}`)

const onCancel = async row => {
  await ElMessageBox.confirm(`确定取消报名「${row.className}」吗？`, '提示', { type: 'warning' })
  await cancelEnroll(row.id)
  ElMessage.success('已取消报名')
  loadMine()
  loadClasses()
}

const onApply = async item => {
  await ElMessageBox.confirm(`确定报名「${item.name}」吗？报名后需等待管理员审批。`, '报名确认', { type: 'info' })
  await applyEnroll(item.id)
  ElMessage.success('报名成功，请等待审批')
  loadMine()
  loadClasses()
}

onMounted(() => {
  loadMine()
  loadClasses()
})
</script>

<style scoped>
.class-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}

.class-meta p {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
}

.muted {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
