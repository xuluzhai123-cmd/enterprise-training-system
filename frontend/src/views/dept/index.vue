<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增部门</el-button>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
      <div class="spacer"></div>
      <span class="tip">部门支持树形层级，删除前需先清空下级部门与部门员工</span>
    </div>

    <el-table v-loading="loading" :data="treeData" row-key="id" :tree-props="{ children: 'children' }"
      default-expand-all border>
      <el-table-column prop="name" label="部门名称" min-width="240" />
      <el-table-column prop="sort" label="排序" width="90" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
      <el-table-column label="操作" width="180" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="openCreate(row)">新增下级</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="parentOptions" :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly clearable placeholder="不选则为顶级部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="如：技术部" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用"
            inactive-text="停用" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { deptTree, createDept, updateDept, deleteDept, deptList } from '@/api'

const loading = ref(false)
const saving = ref(false)
const treeData = ref([])
const flatData = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const form = reactive({ id: null, parentId: null, name: '', sort: 0, status: 1 })

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const parentOptions = computed(() => {
  const mark = (nodes, excludeId) =>
    nodes
      .filter(n => n.id !== excludeId)
      .map(n => ({ ...n, children: n.children ? mark(n.children, excludeId) : [] }))
  return mark(treeData.value, form.id)
})

const load = async () => {
  loading.value = true
  try {
    treeData.value = await deptTree()
    flatData.value = await deptList()
  } finally {
    loading.value = false
  }
}

const openCreate = row => {
  Object.assign(form, { id: null, parentId: row?.id || null, name: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, { id: row.id, parentId: row.parentId || null, name: row.name, sort: row.sort, status: row.status })
  dialogVisible.value = true
}

const onSubmit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, parentId: form.parentId || 0 }
    if (form.id) {
      await updateDept(payload)
    } else {
      await createDept(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onDelete = async row => {
  await ElMessageBox.confirm(`确定删除部门「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.tip {
  font-size: 12px;
  color: #a8abb2;
}
</style>
