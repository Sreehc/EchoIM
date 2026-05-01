import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResponse } from '@/types/api'

const TOKEN_KEY = 'echoim_admin_token'
const ADMIN_INFO_KEY = 'echoim_admin_info'

const client = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ADMIN_INFO_KEY)
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  },
)

export async function getJson<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const response = await client.get<ApiResponse<T>>(url, config)
  return response.data.data
}

export async function postJson<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const response = await client.post<ApiResponse<T>>(url, data, config)
  return response.data.data
}

export async function putJson<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const response = await client.put<ApiResponse<T>>(url, data, config)
  return response.data.data
}

export async function deleteJson<T = void>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const response = await client.delete<ApiResponse<T>>(url, config)
  return response.data.data
}

export { TOKEN_KEY, ADMIN_INFO_KEY }
