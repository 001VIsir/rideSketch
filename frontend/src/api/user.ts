import axios from 'axios'

const API_BASE_URL = '/api'

// Token 存储 key
const TOKEN_KEY = 'ridesketch_token'
const USER_ID_KEY = 'ridesketch_user_id'

function notifyAuthChanged(): void {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event('auth-changed'))
  }
}

// 获取 token
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

// 设置 token
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  notifyAuthChanged()
}

// 获取用户ID
export function getUserId(): number | null {
  const id = localStorage.getItem(USER_ID_KEY)
  return id ? parseInt(id) : null
}

// 设置用户ID
export function setUserId(userId: number): void {
  localStorage.setItem(USER_ID_KEY, userId.toString())
  notifyAuthChanged()
}

// 移除 token
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
  notifyAuthChanged()
}

// 创建 axios 实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
})

// 请求拦截器 - 添加 token
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理 token 过期
request.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      // 跳转到登录页
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

// 用户信息类型
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
}

// 登录请求
export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

// 注册请求
export interface RegisterRequest {
  username: string
  email: string
  password: string
  nickname?: string
}

// 更新用户信息请求
export interface UpdateUserRequest {
  nickname?: string
  avatar?: string
  email?: string
}

// 登录响应
export interface AuthResponse {
  success: boolean
  message: string
  data: {
    token: string
    userId: number
    username: string
    nickname: string
    avatar: string
  }
}

// 获取当前用户信息
export async function getCurrentUser(): Promise<UserInfo> {
  const response = await request.get<any>('/auth/me')
  return response.data.data
}

// 更新用户信息
export async function updateUserInfo(data: UpdateUserRequest): Promise<UserInfo> {
  const response = await request.put<any>('/auth/me', data)
  return response.data.data
}

// 用户登录
export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await request.post<any>('/auth/login', data)
  if (response.data.success && response.data.data.token) {
    setToken(response.data.data.token)
    setUserId(response.data.data.userId)
  }
  return response.data
}

// 用户注册
export async function register(data: RegisterRequest): Promise<any> {
  const response = await request.post<any>('/auth/register', data)
  return response.data
}

// 用户登出
export function logout(): void {
  removeToken()
}

export default {
  getToken,
  setToken,
  removeToken,
  getUserId,
  setUserId,
  getCurrentUser,
  updateUserInfo,
  login,
  register,
  logout,
}
