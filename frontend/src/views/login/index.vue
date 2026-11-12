<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="34" color="#409eff"><School /></el-icon>
        <h1>企业培训管理系统</h1>
        <p>Enterprise Training Management</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入登录账号" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock"
            show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">登 录</el-button>
        </el-form-item>
      </el-form>

      <div class="quick">
        <span>演示账号：</span>
        <el-link type="primary" @click="fill('admin')">管理员</el-link>
        <el-link type="warning" @click="fill('teacher01')">讲师</el-link>
        <el-link type="success" @click="fill('stu001')">学员</el-link>
        <span class="pwd">密码统一 123456</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const fill = username => {
  form.username = username
  form.password = '123456'
}

const onSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(userStore.home)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b5876 55%, #4e4376 100%);
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 10px;
  padding: 34px 34px 24px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.28);
}

.brand {
  text-align: center;
  margin-bottom: 22px;
}

.brand h1 {
  font-size: 20px;
  margin: 8px 0 4px;
  color: #303133;
}

.brand p {
  margin: 0;
  font-size: 12px;
  color: #a8abb2;
  letter-spacing: 1px;
}

.quick {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #909399;
  border-top: 1px dashed #ebeef5;
  padding-top: 14px;
}

.quick .pwd {
  margin-left: auto;
  color: #c0c4cc;
}
</style>
