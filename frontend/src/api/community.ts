import request from './user'

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
  return response.data.data
}

// 获取路线详情
export async function getRouteDetail(id: number): Promise<RouteDetail> {
  const response = await request.get<any>(`/community/route/${id}`)
  return response.data.data
}

// 发布路线
export async function publishRoute(data: PublishRouteRequest): Promise<RouteBasicInfo> {
  const response = await request.post<any>('/community/route', data)
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
  const response = await request.post<any>(`/community/route/${routeId}/comments`, { content })
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
