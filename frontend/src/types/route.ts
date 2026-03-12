/**
 * 路线相关类型定义
 */

// 出行方式
export type RouteMode = 'riding' | 'walking'

// 路线规划请求
export interface RoutePlanningRequest {
  mode: RouteMode
  origin: string // 格式: longitude,latitude
  destination: string // 格式: longitude,latitude
  waypoints?: string // 多个途经点用|分隔
}

// 路线步骤信息
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

// 途经点
export interface Waypoint {
  id: string
  name: string
  lng: number
  lat: number
}
