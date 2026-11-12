<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo">
        <el-icon :size="20"><School /></el-icon>
        <span>企业培训管理系统</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#1f2d3d" text-color="#bfcbd9"
        active-text-color="#409eff">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="breadcrumb">{{ currentTitle }}</div>
        <div class="user">
          <el-tag size="small" :type="tagType" effect="plain">{{ userStore.roleName }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-name">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" disabled>{{ userStore.userInfo?.deptName || '未分配部门' }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const allMenus = [
  { path: '/dashboard', title: '数据看板', icon: 'DataAnalysis', roles: ['ADMIN', 'TEACHER'] },
  { path: '/dept', title: '部门管理', icon: 'OfficeBuilding', roles: ['ADMIN'] },
  { path: '/user', title: '员工管理', icon: 'User', roles: ['ADMIN'] },
  { path: '/course', title: '课程管理', icon: 'Reading', roles: ['ADMIN', 'TEACHER'] },
  { path: '/class', title: '培训班', icon: 'Calendar', roles: ['ADMIN', 'TEACHER'] },
  { path: '/enroll', title: '报名审批', icon: 'Tickets', roles: ['ADMIN', 'TEACHER'] },
  { path: '/question', title: '题库管理', icon: 'EditPen', roles: ['ADMIN', 'TEACHER'] },
  { path: '/score', title: '成绩查询', icon: 'Medal', roles: ['ADMIN', 'TEACHER'] },
  { path: '/my', title: '我的培训', icon: 'Notebook', roles: ['STUDENT'] }
]

const menus = computed(() => allMenus.filter(m => m.roles.includes(userStore.role)))

const activeMenu = computed(() => '/' + (route.path.split('/')[1] || ''))
const currentTitle = computed(() => route.meta?.title || '')
const tagType = computed(() => {
  if (userStore.isAdmin) return 'danger'
  if (userStore.isTeacher) return 'warning'
  return 'success'
})

const onCommand = async command => {
  if (command !== 'logout') return
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #1f2d3d;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.menu {
  border-right: none;
}

.header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.breadcrumb {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  outline: none;
}

.main {
  padding: 16px;
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
