/**
 * 小程序端请求封装，对接 /api/mini/* 与现有后端
 * baseURL 与 xngl-service 同域，可在 config/prod.ts 或环境变量中配置
 */
import Taro from '@tarojs/taro'

const TOKEN_KEY = 'xngl_mp_token'
const USER_INFO_KEY = 'xngl_mp_user'

export function getToken(): string {
  return Taro.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  Taro.setStorageSync(TOKEN_KEY, token)
}

export function clearToken() {
  Taro.removeStorageSync(TOKEN_KEY)
}

export interface MiniUserInfo {
  id: number
  name: string
  role: string
  userType?: string
}

export function setUserInfo(info: MiniUserInfo | null) {
  if (info) Taro.setStorageSync(USER_INFO_KEY, JSON.stringify(info))
  else Taro.removeStorageSync(USER_INFO_KEY)
}

export function getUserInfo(): MiniUserInfo | null {
  const raw = Taro.getStorageSync(USER_INFO_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as MiniUserInfo
  } catch {
    return null
  }
}

/** 是否为仅司机端账号（登录后只展示司机工作台） */
export function isDriverOnly(): boolean {
  const u = getUserInfo()
  return u?.role === 'DRIVER' || u?.userType === 'DRIVER'
}

// 开发时可改为本地或测试环境
const BASE_URL = process.env.TARO_APP_API_BASE || 'http://localhost:8090'

export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, unknown>
  header?: Record<string, string>
}

export interface ApiResult<T = unknown> {
  code: number
  data: T
  message?: string
}

export function request<T = unknown>(options: RequestOptions): Promise<ApiResult<T>> {
  const token = getToken()
  return Taro.request({
    url: BASE_URL + options.url,
    method: options.method || 'GET',
    data: options.data,
    header: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.header,
    },
  }).then((res) => {
    const body = res.data as ApiResult<T>
    if (res.statusCode === 401) {
      clearToken()
      Taro.navigateTo({ url: '/pages/login/index' })
      return Promise.reject(new Error('未登录'))
    }
    return body
  })
}

/** 登录（code 或 账号密码，依后端约定） */
export function login(data: { code?: string; username?: string; password?: string; phone?: string; smsCode?: string }) {
  return request<{ token: string; user: { id: number; name: string; role?: string } }>({
    url: '/api/mini/auth/login',
    method: 'POST',
    data,
  })
}

/** 当前出土单位/用户信息 */
export function getCurrentOrg() {
  return request<{ orgName: string; userId: number; userName: string }>({ url: '/api/mini/excavation-orgs/current' })
}

/** 可用项目列表 */
export function getProjects(params?: { pageNo?: number; pageSize?: number }) {
  return request<{ records: Array<{ id: number; name: string; status: string }>; total: number }>({
    url: '/api/mini/excavation-orgs/projects',
    data: params as Record<string, unknown>,
  })
}

/** 我的出土拍照列表 */
export function getPhotos(params?: { projectId?: number; pageNo?: number; pageSize?: number }) {
  return request<{ records: Array<{ id: number; projectId: number; projectName?: string; shootTime: string; remark?: string; auditStatus: string }>; total: number }>({
    url: '/api/mini/photos',
    data: params as Record<string, unknown>,
  })
}

/** 上传出土照片 */
export function uploadPhoto(data: { projectId: number; siteId?: number; photoType?: string; remark?: string; fileId?: string }) {
  return request<{ id: number }>({ url: '/api/mini/photos', method: 'POST', data })
}

/** 打卡异常申报列表 */
export function getCheckinExceptions(params?: { pageNo?: number; pageSize?: number }) {
  return request<{ records: Array<{ id: number; checkinRecordId: string; exceptionType: string; reason: string; status: string; createTime: string }>; total: number }>({
    url: '/api/mini/checkin-exceptions',
    data: params as Record<string, unknown>,
  })
}

/** 提交打卡异常申报 */
export function submitCheckinException(data: { checkinRecordId: string; exceptionType: string; reason: string; attachmentIds?: string[] }) {
  return request<{ id: number }>({ url: '/api/mini/checkin-exceptions', method: 'POST', data })
}

/** 延期申报列表 */
export function getDelayApplies(params?: { pageNo?: number; pageSize?: number }) {
  return request<{ records: Array<{ id: number; bizType: string; projectId: number; projectName?: string; requestedEndTime: string; status: string; createTime: string }>; total: number }>({
    url: '/api/mini/delay-applies',
    data: params as Record<string, unknown>,
  })
}

/** 提交延期申报 */
export function submitDelayApply(data: { bizType: string; bizId?: string; projectId: number; requestedEndTime: string; reason: string; attachmentIds?: string[] }) {
  return request<{ id: number }>({ url: '/api/mini/delay-applies', method: 'POST', data })
}

/** 问题反馈列表 */
export function getFeedbacks(params?: { pageNo?: number; pageSize?: number }) {
  return request<{ records: Array<{ id: number; feedbackType: string; content: string; status: string; createTime: string }>; total: number }>({
    url: '/api/mini/feedbacks',
    data: params as Record<string, unknown>,
  })
}

/** 提交问题反馈 */
export function submitFeedback(data: { feedbackType: string; content: string; projectId?: number; siteId?: number; attachmentIds?: string[] }) {
  return request<{ id: number }>({ url: '/api/mini/feedbacks', method: 'POST', data })
}

/** 司机首页聚合 */
export function getDriverHome() {
  return request<{ tasks: unknown[]; alerts: unknown[]; permits: unknown[] }>({ url: '/api/mini/drivers/home' })
}

/** 司机任务列表 */
export function getDriverTasks() {
  return request<{ records: unknown[]; total: number }>({ url: '/api/mini/drivers/tasks' })
}

/** 司机预警列表 */
export function getDriverAlerts() {
  return request<{ records: unknown[]; total: number }>({ url: '/api/mini/drivers/alerts' })
}
