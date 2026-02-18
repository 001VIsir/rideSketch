import AMapLoader from '@amap/amap-jsapi-loader'

// 高德地图安全配置
;(window as any)._AMapSecurityConfig = {
  securityJsCode: '',
}

// 地图实例缓存
let mapInstance: any = null
let AMapClass: any = null

/**
 * 加载高德地图
 */
export async function loadAMap(): Promise<any> {
  if (AMapClass) {
    return AMapClass
  }

  AMapClass = await AMapLoader.load({
    key: '30df485f0872725106bacd290344efd5', // 高德地图JS API Key
    version: '2.0',
    plugins: [
      'AMap.ToolBar',
      'AMap.Scale',
      'AMap.Geolocation',
      'AMap.PlaceSearch',
      'AMap.Geocoder',
    ],
  })

  return AMapClass
}

/**
 * 创建地图实例
 */
export async function createMap(container: HTMLElement, options?: any): Promise<any> {
  const AMap = await loadAMap()

  const defaultOptions: any = {
    viewMode: '2D',
    zoom: 15,
    center: [116.397428, 39.90923], // 默认北京天安门
    mapStyle: 'amap://styles/normal',
    ...options,
  }

  mapInstance = new AMap.Map(container, defaultOptions)

  // 添加工具条
  mapInstance.addControl(new AMap.ToolBar({
    position: 'RT',
    visible: true,
    showZoomText: true,
  }))

  // 添加比例尺
  mapInstance.addControl(new AMap.Scale({
    position: 'LB',
  }))

  return mapInstance
}

/**
 * 获取地图实例
 */
export function getMapInstance(): any {
  return mapInstance
}

/**
 * 销毁地图实例
 */
export function destroyMap(): void {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
}

/**
 * 设置地图中心点
 */
export function setMapCenter(lng: number, lat: number): void {
  if (mapInstance) {
    mapInstance.setCenter([lng, lat])
  }
}

/**
 * 设置地图缩放级别
 */
export function setMapZoom(zoom: number): void {
  if (mapInstance) {
    mapInstance.setZoom(zoom)
  }
}

/**
 * 地理编码（地址转坐标）
 */
export async function geocode(address: string): Promise<{ lng: number; lat: number } | null> {
  const AMap = await loadAMap()
  return new Promise((resolve) => {
    const geocoder = new AMap.Geocoder({
      city: '全国',
    })
    geocoder.getLocation(address, (status: string, result: any) => {
      if (status === 'complete' && result.info === 'OK') {
        const location = result.geocodes[0].location
        resolve({
          lng: location.getLng(),
          lat: location.getLat(),
        })
      } else {
        resolve(null)
      }
    })
  })
}

/**
 * 逆地理编码（坐标转地址）
 */
export async function reGeocode(lng: number, lat: number): Promise<string | null> {
  const AMap = await loadAMap()
  return new Promise((resolve) => {
    const geocoder = new AMap.Geocoder({
      radius: 1000,
      extensions: 'base',
    })
    geocoder.getAddress([lng, lat], (status: string, result: any) => {
      if (status === 'complete' && result.info === 'OK') {
        resolve(result.regeocode.formattedAddress)
      } else {
        resolve(null)
      }
    })
  })
}

/**
 * 地点搜索
 */
export async function placeSearch(
  keyword: string,
  city?: string
): Promise<any[]> {
  const AMap = await loadAMap()
  return new Promise((resolve) => {
    const placeSearch = new AMap.PlaceSearch({
      city: city || '全国',
      citylimit: false,
      pageSize: 20,
      pageIndex: 1,
      extensions: 'all',
    })
    placeSearch.search(keyword, (status: string, result: any) => {
      if (status === 'complete' && result.info === 'OK') {
        resolve(result.poiList?.pois || [])
      } else {
        resolve([])
      }
    })
  })
}

export default {
  loadAMap,
  createMap,
  getMapInstance,
  destroyMap,
  setMapCenter,
  setMapZoom,
  geocode,
  reGeocode,
  placeSearch,
}
