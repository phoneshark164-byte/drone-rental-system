import axios from 'axios'

// 创建axios实例
const request = axios.create({
  baseURL: '/',
  timeout: 60000,
  withCredentials: true // 发送cookie以维持session
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 打印完整响应用于调试
    console.log('API响应:', response.config.url, response.data)

    const res = response.data

    // 如果返回的数据不是对象类型（可能是HTML错误页面）
    if (typeof res !== 'object' || res === null) {
      console.error('返回数据格式错误:', res)
      return Promise.reject(new Error('服务器返回数据格式错误'))
    }

    // 如果返回的状态码不是200，则认为是错误
    if (res.code !== 200) {
      console.error('API错误:', res.message)

      // 401未授权，跳转到登录页
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userRole')
        localStorage.removeItem('userInfo')
        window.location.href = '/user/login'
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  error => {
    console.error('响应错误:', error.config?.url, error)

    // 处理网络错误
    if (error.response) {
      console.error('错误状态码:', error.response.status)
      console.error('错误数据:', error.response.data)

      switch (error.response.status) {
        case 401:
          localStorage.removeItem('token')
          localStorage.removeItem('userRole')
          localStorage.removeItem('userInfo')
          window.location.href = '/user/login'
          break
        case 403:
          console.error('没有权限访问')
          break
        case 404:
          console.error('请求的资源不存在')
          break
        case 500:
          console.error('服务器错误')
          break
        default:
          console.error('请求失败:', error.response.data?.message || '网络错误')
      }
    }

    return Promise.reject(error)
  }
)

export default request
