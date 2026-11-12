import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const Layout = () => import('@/layout/index.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'dept',
        name: 'Dept',
        component: () => import('@/views/dept/index.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', roles: ['ADMIN'] }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '员工管理', icon: 'User', roles: ['ADMIN'] }
      },
      {
        path: 'course',
        name: 'Course',
        component: () => import('@/views/course/index.vue'),
        meta: { title: '课程管理', icon: 'Reading', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'course/:id',
        name: 'CourseDetail',
        component: () => import('@/views/course/detail.vue'),
        meta: { title: '课程章节', roles: ['ADMIN', 'TEACHER'], hidden: true }
      },
      {
        path: 'class',
        name: 'Class',
        component: () => import('@/views/clazz/index.vue'),
        meta: { title: '培训班', icon: 'Calendar', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'enroll',
        name: 'Enroll',
        component: () => import('@/views/enroll/index.vue'),
        meta: { title: '报名审批', icon: 'Tickets', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'question',
        name: 'Question',
        component: () => import('@/views/question/index.vue'),
        meta: { title: '题库管理', icon: 'EditPen', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'score',
        name: 'Score',
        component: () => import('@/views/score/index.vue'),
        meta: { title: '成绩查询', icon: 'Medal', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'my',
        name: 'My',
        component: () => import('@/views/my/index.vue'),
        meta: { title: '我的培训', icon: 'Notebook', roles: ['STUDENT'] }
      },
      {
        path: 'learn/:id',
        name: 'Learn',
        component: () => import('@/views/learn/index.vue'),
        meta: { title: '课程学习', roles: ['STUDENT'], hidden: true }
      },
      {
        path: 'exam/:classId',
        name: 'Exam',
        component: () => import('@/views/exam/index.vue'),
        meta: { title: '在线考试', roles: ['STUDENT'], hidden: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(to => {
  const store = useUserStore()
  if (to.path === '/login') {
    return true
  }
  if (!store.token) {
    return { path: '/login' }
  }
  const roles = to.meta?.roles
  if (roles && roles.length && !roles.includes(store.role)) {
    return { path: store.role === 'STUDENT' ? '/my' : '/dashboard' }
  }
  return true
})

router.afterEach(to => {
  document.title = to.meta?.title ? `${to.meta.title} - 企业培训管理系统` : '企业培训管理系统'
})

export default router
