<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="课程名称 / 讲师" clearable style="width: 220px"
        @keyup.enter="search" />
      <el-select v-model="query.category" placeholder="分类" clearable style="width: 130px">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="已上架" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增课程</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="title" label="课程名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.category || '未分类' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="teacherName" label="讲师" width="110" align="center" />
      <el-table-column prop="hours" label="课时" width="90" align="center" />
      <el-table-column prop="chapterCount" label="章节数" width="90" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已上架' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="goDetail(row)">章节</el-button>
          <el-button link type="info" :loading="kbId === row.id" @click="buildKb(row)">构建知识库</el-button>
          <el-button link type="warning" @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @current-change="load"
        @size-change="search" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑课程' : '新增课程'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="课程名称" prop="title">
          <el-input v-model="form.title" maxlength="80" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" filterable allow-create default-first-option placeholder="选择或输入分类"
            style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="讲师">
          <el-input v-model="form.teacherName" placeholder="讲师姓名" />
        </el-form-item>
        <el-form-item label="课时">
          <el-input-number v-model="form.hours" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架"
            inactive-text="下架" />
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { coursePage, createCourse, updateCourse, deleteCourse, ragIngest } from '@/api'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const categories = ['新员工', '技术', '管理', '合规']
const query = reactive({ current: 1, size: 10, keyword: '', category: '', status: null })
const form = reactive({ id: null, title: '', category: '技术', teacherName: '', hours: 8, intro: '', status: 1 })

const rules = {
  title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

// 构建课程知识库：把章节正文切片入库，供学员在「我的培训 → 问 AI 助教」里提问
const kbId = ref(null)
const buildKb = async row => {
  kbId.value = row.id
  try {
    const data = await ragIngest(row.id)
    ElMessage.success(
      `已构建：${data.chunkCount} 块切片（用到 ${data.chapterUsed}/${data.chapterCount} 个章节）`
    )
  } finally {
    kbId.value = null
  }
}

const load = async () => {
  loading.value = true
  try {
    const data = await coursePage(query)
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
  Object.assign(query, { current: 1, size: 10, keyword: '', category: '', status: null })
  load()
}

const openCreate = () => {
  Object.assign(form, { id: null, title: '', category: '技术', teacherName: '', hours: 8, intro: '', status: 1 })
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, {
    id: row.id, title: row.title, category: row.category, teacherName: row.teacherName,
    hours: row.hours, intro: row.intro, status: row.status
  })
  dialogVisible.value = true
}

const onSubmit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateCourse({ ...form })
    } else {
      await createCourse({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async row => {
  await updateCourse({ id: row.id, status: row.status === 1 ? 0 : 1 })
  ElMessage.success('状态已更新')
  load()
}

const goDetail = row => router.push(`/course/${row.id}`)

const onDelete = async row => {
  await ElMessageBox.confirm(`确定删除课程「${row.title}」及其章节吗？`, '提示', { type: 'warning' })
  await deleteCourse(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
