<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.classId" placeholder="全部培训班" clearable filterable style="width: 240px">
        <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="审批状态" clearable style="width: 140px">
        <el-option label="待审批" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
        <el-option label="已取消" :value="3" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-tag type="warning" effect="plain">待审批：{{ pendingCount }} 条</el-tag>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="realName" label="学员" width="110" />
      <el-table-column prop="deptName" label="部门" width="130" />
      <el-table-column prop="className" label="培训班" min-width="180" show-overflow-tooltip />
      <el-table-column prop="courseTitle" label="课程" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="130" align="center">
        <template #default="{ row }">
          <el-progress :percentage="row.progress || 0" :stroke-width="8" />
        </template>
      </el-table-column>
      <el-table-column label="成绩" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.score !== null && row.score !== undefined">{{ row.score }} 分</span>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="报名时间" width="170" align="center" />
      <el-table-column label="操作" width="170" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="success" @click="onApprove(row)">通过</el-button>
            <el-button link type="danger" @click="onReject(row)">驳回</el-button>
          </template>
          <span v-else class="muted">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @current-change="load"
        @size-change="search" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { enrollPage, classOptions, approveEnroll, rejectEnroll } from '@/api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pendingCount = ref(0)
const classes = ref([])

const query = reactive({ current: 1, size: 10, classId: null, status: null })

const statusName = s => ({ 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已取消' }[s] || '-')
const statusTag = s => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info')

const load = async () => {
  loading.value = true
  try {
    const data = await enrollPage(query)
    list.value = data.records
    total.value = data.total
    const pending = await enrollPage({ current: 1, size: 1, status: 0 })
    pendingCount.value = pending.total
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.current = 1
  load()
}

const reset = () => {
  Object.assign(query, { current: 1, size: 10, classId: null, status: null })
  load()
}

const onApprove = async row => {
  await ElMessageBox.confirm(`通过「${row.realName}」报名的「${row.className}」？`, '审批确认', { type: 'info' })
  await approveEnroll(row.id)
  ElMessage.success('已通过')
  load()
}

const onReject = async row => {
  await ElMessageBox.confirm(`驳回「${row.realName}」的报名申请？`, '审批确认', { type: 'warning' })
  await rejectEnroll(row.id)
  ElMessage.success('已驳回')
  load()
}

onMounted(async () => {
  classes.value = await classOptions()
  load()
})
</script>

<style scoped>
.muted {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
