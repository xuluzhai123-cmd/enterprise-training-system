<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="账号 / 姓名 / 手机号" clearable style="width: 220px"
        @keyup.enter="search" />
      <el-tree-select v-model="query.deptId" :data="deptTreeData" :props="{ label: 'name', value: 'id', children: 'children' }"
        check-strictly clearable placeholder="所属部门" style="width: 180px" />
      <el-select v-model="query.role" placeholder="角色" clearable style="width: 140px">
        <el-option label="管理员" value="ADMIN" />
        <el-option label="讲师" value="TEACHER" />
        <el-option label="学员" value="STUDENT" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="启用" :value="0" />
        <el-option label="禁用" :value="1" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增员工</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="username" label="登录账号" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100" />
      <el-table-column prop="deptName" label="所属部门" min-width="120" />
      <el-table-column label="角色" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="roleTag(row.role)" size="small">{{ roleName(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 0" :width="58" inline-prompt active-text="启用" inactive-text="禁用"
            @change="val => toggleStatus(row, val ? 0 : 1)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="230" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background
        @current-change="load" @size-change="search" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑员工' : '新增员工'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="用于登录，创建后不建议修改" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="所属部门" prop="deptId">
          <el-tree-select v-model="form.deptId" :data="deptTreeData" :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly placeholder="请选择部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio value="ADMIN">管理员</el-radio>
            <el-radio value="TEACHER">讲师</el-radio>
            <el-radio value="STUDENT">学员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" placeholder="不填默认 123456" />
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
import {
  userPage, createUser, updateUser, deleteUser, updateUserStatus, resetUserPassword, deptTree
} from '@/api'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const deptTreeData = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ current: 1, size: 10, keyword: '', deptId: null, role: '', status: null })
const form = reactive({ id: null, username: '', realName: '', deptId: null, role: 'STUDENT', phone: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const roleName = role => ({ ADMIN: '管理员', TEACHER: '讲师', STUDENT: '学员' }[role] || role)
const roleTag = role => ({ ADMIN: 'danger', TEACHER: 'warning', STUDENT: 'success' }[role] || 'info')

const load = async () => {
  loading.value = true
  try {
    const data = await userPage(query)
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
  Object.assign(query, { current: 1, size: 10, keyword: '', deptId: null, role: '', status: null })
  load()
}

const openCreate = () => {
  Object.assign(form, { id: null, username: '', realName: '', deptId: null, role: 'STUDENT', phone: '', password: '' })
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, {
    id: row.id, username: row.username, realName: row.realName,
    deptId: row.deptId, role: row.role, phone: row.phone, password: ''
  })
  dialogVisible.value = true
}

const onSubmit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateUser({ ...form })
    } else {
      await createUser({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row, statusValue) => {
  await updateUserStatus(row.id, statusValue)
  ElMessage.success(statusValue === 0 ? '已启用' : '已禁用')
  load()
}

const resetPwd = async row => {
  await ElMessageBox.confirm(`将「${row.realName}」的密码重置为 123456？`, '提示', { type: 'warning' })
  await resetUserPassword(row.id)
  ElMessage.success('已重置为 123456')
}

const onDelete = async row => {
  await ElMessageBox.confirm(`确定删除员工「${row.realName}」吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  deptTreeData.value = await deptTree()
  load()
})
</script>
