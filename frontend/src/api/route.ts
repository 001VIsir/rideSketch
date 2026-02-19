import axios from 'axios'

const API_BASE_URL = '/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
})

// 出行方式
export type RouteMode = 'riding' | 'walking'

// 路线规划请求
export interface RoutePlanningRequest {
  mode: RouteMode
  origin: string // 格式: longitude,latitude
  destination: string // 格式: longitude,latitude
  waypoints?: string // 多个途经点用|分隔
}

// 路线点
export interface RoutePoint {
  lng: number
  lat: number
  name?: string
}

// 步骤信息
export interface StepInfo {
  instruction: string
  distance: string
  duration: string
  road: string
  orientation: string
  path: string
}

// 路径信息
export interface PathInfo {
  distance: string
  duration: string
  strategy: string
  steps: StepInfo[]
  path: string
}

// 路线信息
export interface RouteInfo {
  origin: string
  destination: string
  waypoints: string
  paths: PathInfo[]
}

// 路线规划结果
export interface RoutePlanningResult {
  status: string
  info: string
  route: RouteInfo
}

// AI路线规划请求
export interface AIRoutePlanningRequest {
  description: string
  city?: string
}

// AI路线规划结果
export interface AIRoutePlanningResult {
  description: string
  routes: RoutePlanningResult[]
}

// 图案路书请求
export interface PatternRouteRequest {
  description?: string
  pattern?: string
  city: string
  distance?: number
}

// 图案路书结果
export interface PatternRouteResult {
  pattern: string
  city: string
  distance: number
  route: RoutePlanningResult
}

// 路线规划
export async function planRoute(data: RoutePlanningRequest): Promise<RoutePlanningResult> {
  const response = await request.post<any>('/route/plan', data)
  return response.data.data
}

// 骑行路线规划
export async function planRidingRoute(
  origin: string,
  destination: string,
  waypoints?: string
): Promise<RoutePlanningResult> {
  const params = new URLSearchParams()
  params.append('origin', origin)
  params.append('destination', destination)
  if (waypoints) {
    params.append('waypoints', waypoints)
  }
  const response = await request.get<any>(`/route/riding?${params.toString()}`)
  return response.data.data
}

// 步行路线规划
export async function planWalkingRoute(
  origin: string,
  destination: string,
  waypoints?: string
): Promise<RoutePlanningResult> {
  const params = new URLSearchParams()
  params.append('origin', origin)
  params.append('destination', destination)
  if (waypoints) {
    params.append('waypoints', waypoints)
  }
  const response = await request.get<any>(`/route/walking?${params.toString()}`)
  return response.data.data
}

// AI智能路线规划
export async function planAIRoute(data: AIRoutePlanningRequest): Promise<AIRoutePlanningResult> {
  const response = await request.post<any>('/route/ai-plan', data)
  return response.data.data
}

// 图案路书生成
export async function generatePatternRoute(data: PatternRouteRequest): Promise<PatternRouteResult> {
  const response = await request.post<any>('/route/pattern', data)
  return response.data.data
}

export default {
  planRoute,
  planRidingRoute,
  planWalkingRoute,
  planAIRoute,
  generatePatternRoute,
}
