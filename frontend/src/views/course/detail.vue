<template>
  <div v-loading="loading">
    <el-page-header content="课程章节" @back="router.back()">
      <template #extra>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增章节</el-button>
      </template>
    </el-page-header>

    <el-card shadow="never" style="margin-top: 14px">
      <el-descriptions :column="4" border>
        <el-descriptions-item label="课程名称">{{ course.title }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ course.category }}</el-descriptions-item>
        <el-descriptions-item label="讲师">{{ course.teacherName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="课时">{{ course.hours }} 小时</el-descriptions-item>
        <el-descriptions-item label="简介" :span="4">{{ course.intro || '暂无简介' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 14px" header="章节列表">
      <el-table :data="chapters" border stripe>
        <el-table-column prop="sort" label="序号" width="80" align="center" />
        <el-table-column prop="title" label="章节标题" min-width="240" />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'VIDEO' ? 'primary' : 'success'" size="small">
              {{ row.type === 'VIDEO' ? '视频' : '文档' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="110" align="center">
          <template #default="{ row }">{{ row.duration }} 分钟</template>
        </el-table-column>
        <el-table-column prop="content" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑章节' : '新增章节'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="form.title" maxlength="80" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio value="VIDEO">视频</el-radio>
            <el-radio value="DOC">文档</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="form.duration" :min="0" :max="999" />
          <span class="unit">分钟</span>
        </el-form-item>
        <el-form-item label="章节说明">
          <el-input v-model="form.content" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="1" :max="999" />
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { courseDetail, createChapter, updateChapter, deleteChapter } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const course = ref({})
const chapters = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const form = reactive({ id: null, courseId: null, title: '', type: 'VIDEO', duration: 30, content: '', sort: 1 })

const rules = {
  title: [{ required: true, message: '请输入章节标题', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const data = await courseDetail(route.params.id)
    course.value = data.course
    chapters.value = data.chapters
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, {
    id: null, courseId: Number(route.params.id), title: '', type: 'VIDEO',
    duration: 30, content: '', sort: chapters.value.length + 1
  })
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const onSubmit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateChapter({ ...form })
    } else {
      await createChapter({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onDelete = async row => {
  await ElMessageBox.confirm(`确定删除章节「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteChapter(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.unit {
  margin-left: 8px;
  color: #909399;
}
</style>
