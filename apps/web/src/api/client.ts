import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3001'

export const apiClient = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 可以在这里添加认证 token
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      // 服务器返回错误状态码
      const message = error.response.data?.message || error.response.statusText
      return Promise.reject(new Error(message))
    } else if (error.request) {
      // 请求发出但没有收到响应
      return Promise.reject(new Error('无法连接到服务器，请检查后端服务是否运行'))
    } else {
      // 请求配置出错
      return Promise.reject(new Error(error.message))
    }
  }
)
