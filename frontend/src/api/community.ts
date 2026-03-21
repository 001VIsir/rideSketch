import { createHttpClient } from './http'

const request = createHttpClient(10000)

interface RouteVO {
  id: number
  userId: number
  username: string
  nickname: string
  avatar: string
  title: string
  description: string
  startPoint: string
  endPoint: string
  waypoints?: string
  routePath?: string
  totalDistance?: number | string
  estimatedTime?: number
  difficulty?: number
  tags?: string
  likes: number
  views?: number
  isPublic?: number
  liked?: boolean
  createTime: string
  updateTime?: string
}

export interface RouteBasicInfo {
  id: number
  userId: number
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
  liked?: boolean
}

export interface RouteDetail extends RouteBasicInfo {
  startPoint: string
  endPoint: string
  waypoints?: string
  difficulty?: number
  tags?: string
  updateTime?: string
}

export interface CommentInfo {
  id: number
  routeId: number
  userId: number
  username: string
  nickname?: string
  avatar: string
  content: string
  createTime: string
  parentId?: number
  replies?: CommentInfo[]
}

export interface PublishRouteRequest {
  title: string
  description: string
  city: string
  distance: number
  duration: number
  path: string
}

export interface UpdateRouteRequest {
  title?: string
  description?: string
}

function mapRouteVO(item: RouteVO): RouteBasicInfo {
  const distanceKm = Number(item.totalDistance || 0)
  const durationMinutes = Number(item.estimatedTime || 0)

  return {
    id: item.id,
    userId: item.userId,
    title: item.title || '',
    description: item.description || '',
    distance: Math.round(distanceKm * 1000),
    duration: durationMinutes * 60,
    authorId: item.userId,
    authorName: item.nickname || item.username || '匿名用户',
    authorAvatar: item.avatar || '',
    likes: item.likes || 0,
    comments: 0,
    createTime: item.createTime,
    city: item.startPoint || '',
    path: item.routePath || '',
    liked: item.liked || false,
  }
}

export async function getRouteList(params?: {
  page?: number
  size?: number
  city?: string
}): Promise<{ list: RouteBasicInfo[]; total: number }> {
  const response = await request.get<any>('/community/routes', { params })
  const data: RouteVO[] = Array.isArray(response.data.data) ? response.data.data : []
  const list = data.map(mapRouteVO)

  return {
    list,
    total: list.length,
  }
}

export async function getRouteDetail(id: number): Promise<RouteDetail> {
  const response = await request.get<any>(`/community/route/${id}`)
  const route = response.data.data as RouteVO
  const mapped = mapRouteVO(route)

  return {
    ...mapped,
    startPoint: route.startPoint || '',
    endPoint: route.endPoint || '',
    waypoints: route.waypoints,
    difficulty: route.difficulty,
    tags: route.tags,
    updateTime: route.updateTime,
  }
}

export async function publishRoute(data: PublishRouteRequest): Promise<RouteBasicInfo> {
  const backendData = {
    title: data.title,
    description: data.description,
    startPoint: data.city || '未知',
    endPoint: data.city || '未知',
    routePath: data.path || '[]',
    totalDistance: data.distance ? data.distance / 1000 : 0,
    estimatedTime: data.duration ? Math.round(data.duration / 60) : 0,
    difficulty: 1,
    tags: '[]',
    isPublic: 1,
  }

  const response = await request.post<any>('/community/route', backendData)
  return mapRouteVO(response.data.data as RouteVO)
}

export async function updateRoute(id: number, data: UpdateRouteRequest): Promise<RouteBasicInfo> {
  const response = await request.put<any>(`/community/route/${id}`, data)
  return mapRouteVO(response.data.data as RouteVO)
}

export async function deleteRoute(id: number): Promise<void> {
  await request.delete<any>(`/community/route/${id}`)
}

async function toggleLike(id: number): Promise<{ liked: boolean; likes: number }> {
  const response = await request.post<any>(`/community/route/${id}/like`)
  return response.data.data
}

export async function likeRoute(id: number): Promise<{ liked: boolean; likes: number }> {
  return toggleLike(id)
}

export async function unlikeRoute(id: number): Promise<{ liked: boolean; likes: number }> {
  return toggleLike(id)
}

export async function getComments(routeId: number): Promise<CommentInfo[]> {
  const response = await request.get<any>(`/community/route/${routeId}/comments`)
  return response.data.data
}

export async function postComment(routeId: number, content: string): Promise<CommentInfo> {
  const response = await request.post<any>(`/community/route/${routeId}/comment`, { content })
  return response.data.data
}

export async function deleteComment(commentId: number): Promise<void> {
  await request.delete<any>(`/community/comment/${commentId}`)
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
