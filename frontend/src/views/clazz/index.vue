<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="培训班名称" clearable style="width: 200px" @keyup.enter="search" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已结束" :value="2" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增培训班</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="name" label="培训班名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="courseName" label="关联课程" min-width="160" show-overflow-tooltip />
      <el-table-column label="培训周期" width="200" align="center">
        <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column label="报名情况" width="140" align="center">
        <template #default="{ row }">
          <el-progress :percentage="percent(row)" :stroke-width="12" :format="() => `${row.enrolledCount}/${row.quota}`" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="changeStatus(row, 1)">发布</el-button>
          <el-button v-else-if="row.status === 1" link type="warning" @click="changeStatus(row, 2)">结束</el-button>
          <el-button v-else link type="info" @click="changeStatus(row, 1)">重新发布</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @current-change="load"
        @size-change="search" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑培训班' : '新增培训班'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="培训班名称" prop="name">
          <el-input v-model="form.name" maxlength="80" placeholder="如：2026 春季 Java 基础班" />
        </el-form-item>
        <el-form-item label="关联课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="选择课程" filterable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.title" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="培训周期" prop="dateRange">
          <el-date-picker v-model="form.dateRange" type="daterange" value-format="YYYY-MM-DD"
            start-placeholder="开始日期" end-placeholder="结束日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="名额" prop="quota">
          <el-input-number v-model="form.quota" :min="1" :max="999" />
          <span class="unit">人</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">草稿</el-radio>
            <el-radio :value="1">发布报名</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { classPage, courseOptions, createClass, updateClass, updateClassStatus, deleteClass } from '@/api'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const courses = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ current: 1, size: 10, keyword: '', status: null })
const form = reactive({
  id: null, name: '', courseId: null, dateRange: [], quota: 20, status: 0
})

const rules = {
  name: [{ required: true, message: '请输入培训班名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择关联课程', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择培训周期', trigger: 'change' }],
  quota: [{ required: true, message: '请输入名额', trigger: 'blur' }]
}

const statusName = s => ({ 0: '草稿', 1: '已发布', 2: '已结束' }[s] || '-')
const statusTag = s => ({ 0: 'info', 1: 'success', 2: 'warning' }[s] || 'info')
const percent = row => (row.quota ? Math.min(100, Math.round((row.enrolledCount * 100) / row.quota)) : 0)

const load = async () => {
  loading.value = true
  try {
    const data = await classPage(query)
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
  Object.assign(query, { current: 1, size: 10, keyword: '', status: null })
  load()
}

const openCreate = () => {
  Object.assign(form, { id: null, name: '', courseId: null, dateRange: [], quota: 20, status: 0 })
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, {
    id: row.id, name: row.name, courseId: row.courseId,
    dateRange: [row.startDate, row.endDate], quota: row.quota, status: row.status
  })
  dialogVisible.value = true
}

const onSubmit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      courseId: form.courseId,
      startDate: form.dateRange[0],
      endDate: form.dateRange[1],
      quota: form.quota,
      status: form.status
    }
    if (form.id) {
      await updateClass(payload)
    } else {
      await createClass(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const changeStatus = async (row, status) => {
  await updateClassStatus(row.id, status)
  ElMessage.success('状态已更新')
  load()
}

const onDelete = async row => {
  await ElMessageBox.confirm(`确定删除培训班「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteClass(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  courses.value = await courseOptions()
  load()
})
</script>

<style scoped>
.unit {
  margin-left: 8px;
  color: #909399;
}
</style>
