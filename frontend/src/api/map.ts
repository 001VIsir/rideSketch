import axios from 'axios'
import type {
  AddressSearchResult,
  GeoCodeResult,
  ReGeoCodeResult,
} from '@/types/map'

const API_BASE_URL = '/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
})

/**
 * 地址搜索（关键词搜索POI）
 */
export async function searchAddress(keyword: string, city?: string): Promise<AddressSearchResult> {
  const params = new URLSearchParams()
  params.append('keyword', keyword)
  if (city) {
    params.append('city', city)
  }

  const response = await request.get<any>('/map/search', { params })
  return response.data.data
}

/**
 * 地理编码（地址转坐标）
 */
export async function geocode(address: string): Promise<GeoCodeResult> {
  const params = new URLSearchParams()
  params.append('address', address)

  const response = await request.get<any>('/map/geocode', { params })
  return response.data.data
}

/**
 * 逆地理编码（坐标转地址）
 */
export async function reGeocode(longitude: string, latitude: string): Promise<ReGeoCodeResult> {
  const params = new URLSearchParams()
  params.append('longitude', longitude)
  params.append('latitude', latitude)

  const response = await request.get<any>('/map/regeocode', { params })
  return response.data.data
}

export default {
  searchAddress,
  geocode,
  reGeocode,
}
