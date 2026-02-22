import axios from 'axios'

const API_BASE_URL = '/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
})

// 请求拦截器 - 添加 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('ridesketch_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 - 处理 token 过期
request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('ridesketch_token')
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

// 路线基本信息
export interface RouteBasicInfo {
  id: number
  title: string
  description: string
  distance: number
  duration: number
  authorId: number
  authorName: string
  authorAvatar: string
  likes: number
  comments: number
  createTime: string
  city: string
  path?: string
}

// 路线详情
export interface RouteDetail extends RouteBasicInfo {
  path: string
  steps?: any[]
}

// 评论信息
export interface CommentInfo {
  id: number
  routeId: number
  userId: number
  username: string
  avatar: string
  content: string
  createTime: string
  replies?: CommentInfo[]
}

// 发布路线请求
export interface PublishRouteRequest {
  title: string
  description: string
  city: string
  distance: number
  duration: number
  path: string
}

// 更新路线请求
export interface UpdateRouteRequest {
  title?: string
  description?: string
  city?: string
}

// 获取路线列表
export async function getRouteList(params?: {
  page?: number
  size?: number
  city?: string
}): Promise<{ list: RouteBasicInfo[]; total: number }> {
  const response = await request.get<any>('/community/routes', { params })
  const data = response.data.data
  // 后端返回的是数组，需要转换为 {list, total} 格式，并适配字段名
  const list = Array.isArray(data) ? data.map((item: any) => ({
    id: item.id,
    title: item.title,
    description: item.description,
    distance: item.totalDistance ? Number(item.totalDistance) * 1000 : 0, // 转换为米
    duration: item.estimatedTime ? item.estimatedTime : 0, // 秒
    authorId: item.userId,
    authorName: item.nickname || item.username,
    authorAvatar: item.avatar,
    likes: item.likes || 0,
    comments: 0, // 后端未返回评论数
    createTime: item.createTime,
    city: item.startPoint, // 使用起点作为城市
    path: item.routePath
  })) : []
  return {
    list,
    total: list.length
  }
}

// 获取路线详情
export async function getRouteDetail(id: number): Promise<RouteDetail> {
  const response = await request.get<any>(`/community/route/${id}`)
  return response.data.data
}

// 发布路线 - 适配前后端字段不一致
export async function publishRoute(data: PublishRouteRequest): Promise<RouteBasicInfo> {
  // 转换前端字段到后端字段
  const backendData = {
    title: data.title,
    description: data.description,
    startPoint: data.city || '未知',
    endPoint: data.city || '未知',
    routePath: data.path || '[]',
    totalDistance: data.distance ? data.distance / 1000 : 0, // 转换为千米
    estimatedTime: data.duration ? data.duration / 60 : 0, // 转换为分钟
    difficulty: 1,
    tags: '[]',
    isPublic: 1
  }
  const response = await request.post<any>('/community/route', backendData)
  return response.data.data
}

// 更新路线
export async function updateRoute(id: number, data: UpdateRouteRequest): Promise<RouteBasicInfo> {
  const response = await request.put<any>(`/community/route/${id}`, data)
  return response.data.data
}

// 删除路线
export async function deleteRoute(id: number): Promise<void> {
  await request.delete<any>(`/community/route/${id}`)
}

// 点赞路线
export async function likeRoute(id: number): Promise<{ likes: number }> {
  const response = await request.post<any>(`/community/route/${id}/like`)
  return response.data.data
}

// 取消点赞
export async function unlikeRoute(id: number): Promise<{ likes: number }> {
  const response = await request.delete<any>(`/community/route/${id}/like`)
  return response.data.data
}

// 获取评论列表
export async function getComments(routeId: number): Promise<CommentInfo[]> {
  const response = await request.get<any>(`/community/route/${routeId}/comments`)
  return response.data.data
}

// 发布评论
export async function postComment(routeId: number, content: string): Promise<CommentInfo> {
  // 后端API路径是 /comment 而不是 /comments
  const response = await request.post<any>(`/community/route/${routeId}/comment`, { content })
  return response.data.data
}

// 删除评论
export async function deleteComment(routeId: number, commentId: number): Promise<void> {
  await request.delete<any>(`/community/route/${routeId}/comments/${commentId}`)
}

export default {
  getRouteList,
  getRouteDetail,
  publishRoute,
  updateRoute,
  deleteRoute,
  likeRoute,
  unlikeRoute,
  getComments,
  postComment,
  deleteComment,
}
