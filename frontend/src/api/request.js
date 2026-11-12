import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：统一带上 Sa-Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['satoken'] = token
  }
  return config
})

// 响应拦截：后端 HTTP 恒为 200，业务状态看 body.code
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      if (location.hash !== '#/login') {
        location.hash = '#/login'
      }
      ElMessage.warning(res.msg || '请先登录')
      return Promise.reject(new Error(res.msg || '未登录'))
    }
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  error => {
    ElMessage.error(error.message || '网络异常，请检查后端是否启动')
    return Promise.reject(error)
  }
)

export default request
