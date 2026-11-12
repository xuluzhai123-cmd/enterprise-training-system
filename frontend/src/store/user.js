import { defineStore } from 'pinia'
import { login as loginApi, getMe, logout as logoutApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    role: state => state.userInfo?.role || '',
    roleName: state => {
      const map = { ADMIN: '管理员', TEACHER: '讲师', STUDENT: '学员' }
      return map[state.userInfo?.role] || '未知'
    },
    isAdmin: state => state.userInfo?.role === 'ADMIN',
    isTeacher: state => state.userInfo?.role === 'TEACHER',
    isStudent: state => state.userInfo?.role === 'STUDENT',
    home: state => (state.userInfo?.role === 'STUDENT' ? '/my' : '/dashboard')
  },
  actions: {
    async login(form) {
      const data = await loginApi(form)
      this.token = data.token
      this.userInfo = data
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(data))
      return data
    },
    async fetchMe() {
      const me = await getMe()
      this.userInfo = me
      localStorage.setItem('userInfo', JSON.stringify(me))
      return me
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 退出失败也允许本地清空
      }
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
