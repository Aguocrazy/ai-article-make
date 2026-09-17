import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'

/**
 * 后端统一响应格式（对应 com.aiarticle.common.BaseResponse）
 */
export interface BaseResponse<T = unknown> {
  code: number
  data: T
  message: string
}

/**
 * 创建 axios 实例
 */
const request: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 10000,
  // 同源走 Vite 代理；仍携带 cookie 以便 Session 登录态回传
  withCredentials: true,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error),
)

// 响应拦截器：统一处理后端 BaseResponse 格式
request.interceptors.response.use(
  (response: AxiosResponse<BaseResponse>) => {
    const res = response.data
    // code = 0 表示成功（对应 ErrorCode.SUCCESS）
    if (res.code === 0) {
      return response
    }
    // 未登录是探测登录态的正常结果，不当成控制台错误
    if (res.code !== 40100) {
      console.error(`请求失败: ${res.message} (code=${res.code})`)
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    console.error('网络请求异常:', error.message)
    return Promise.reject(error)
  },
)

/**
 * 类型安全的请求方法，直接返回 data 部分
 */
export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await request.get<BaseResponse<T>>(url, config)
  return res.data.data
}

export async function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await request.post<BaseResponse<T>>(url, data, config)
  return res.data.data
}

export default request