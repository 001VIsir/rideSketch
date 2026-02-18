/**
 * 地图相关类型定义
 */

// POI信息
export interface PoiInfo {
  id: string
  name: string
  type: string
  typecode: string
  latitude: string
  longitude: string
  address: string
  province: string
  city: string
  district: string
}

// 地址搜索结果
export interface AddressSearchResult {
  status: string
  info: string
  pois: PoiInfo[]
}

// 地理编码信息
export interface GeocodeInfo {
  formattedAddress: string
  country: string
  province: string
  city: string
  citycode: string
  district: string
  township: string
  street: string
  number: string
  lat: string
  lng: string
  confidence: string
  level: string
}

// 地理编码结果
export interface GeoCodeResult {
  status: string
  info: string
  geocodes: GeocodeInfo
}

// 地址组件
export interface AddressComponent {
  country: string
  province: string
  city: string
  citycode: string
  district: string
  township: string
  street: string
  number: string
}

// 逆地理编码结果
export interface ReGeoCodeResult {
  status: string
  info: string
  formattedAddress: string
  addressComponent: AddressComponent
}

// 地图中心点
export interface MapCenter {
  longitude: number
  latitude: number
}

// 地图配置
export interface MapConfig {
  center: [number, number]
  zoom: number
  viewMode?: '2D' | '3D'
  pitch?: number
  rotation?: number
}
