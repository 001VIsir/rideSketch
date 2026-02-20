import AMapLoader from '@amap/amap-jsapi-loader'

// 高德地图安全配置
;(window as any)._AMapSecurityConfig = {
  securityJsCode: '47d0577f4f07e4c9da34a4da538576b5',
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
    key: 'aa25cb3c8d595079f7c00b6aac239b24', // 高德地图JS API Key
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
      citylimit: true,  // 限制在城市范围内
      pageSize: 20,
      pageIndex: 1,
      extensions: 'all',
    })
    placeSearch.search(keyword, (status: string, result: any) => {
      if (status === 'complete' && result.info === 'OK') {
        const pois = result.poiList?.pois || []
        // 转换为统一格式
        const formattedPois = pois.map((poi: any) => ({
          id: poi.id,
          name: poi.name,
          type: poi.type,
          typecode: poi.typecode,
          latitude: poi.location.getLat(),
          longitude: poi.location.getLng(),
          address: poi.address,
          province: poi.province,
          city: poi.city,
          district: poi.adname
        }))
        resolve(formattedPois)
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

// 标记实例缓存
let startMarker: any = null
let endMarker: any = null
let waypointMarkers: any[] = []
let routePolylines: any[] = []

/**
 * 创建起点标记
 */
export async function createStartMarker(lng: number, lat: number, name?: string): Promise<any> {
  const AMap = await loadAMap()

  // 清除已有起点标记
  if (startMarker) {
    mapInstance.remove(startMarker)
  }

  const markerContent = `
    <div class="amap-marker-start">
      <div class="marker-icon">A</div>
      <div class="marker-label">起点</div>
    </div>
  `

  startMarker = new AMap.Marker({
    position: [lng, lat],
    title: name || '起点',
    content: markerContent,
    offset: new AMap.Pixel(-15, -30),
  })

  mapInstance.add(startMarker)
  return startMarker
}

/**
 * 创建终点标记
 */
export async function createEndMarker(lng: number, lat: number, name?: string): Promise<any> {
  const AMap = await loadAMap()

  // 清除已有终点标记
  if (endMarker) {
    mapInstance.remove(endMarker)
  }

  const markerContent = `
    <div class="amap-marker-end">
      <div class="marker-icon">B</div>
      <div class="marker-label">终点</div>
    </div>
  `

  endMarker = new AMap.Marker({
    position: [lng, lat],
    title: name || '终点',
    content: markerContent,
    offset: new AMap.Pixel(-15, -30),
  })

  mapInstance.add(endMarker)
  return endMarker
}

/**
 * 途经点标记
 */
export async function createWaypointMarker(lng: number, lat: number, index: number): Promise<any> {
  const AMap = await loadAMap()

  const markerContent = `
    <div class="amap-marker-waypoint">
      <div class="marker-icon">${index + 1}</div>
    </div>
  `

  const marker = new AMap.Marker({
    position: [lng, lat],
    title: `途经点${index + 1}`,
    content: markerContent,
    offset: new AMap.Pixel(-10, -10),
  })

  mapInstance.add(marker)
  waypointMarkers.push(marker)
  return marker
}

/**
 * 绘制路线
 */
export async function drawRoute(path: [number, number][], color?: string): Promise<any> {
  const AMap = await loadAMap()

  // 清除已有路线
  clearRoute()

  if (!path || path.length === 0) {
    return null
  }

  const polyline = new AMap.Polyline({
    path: path,
    strokeColor: color || '#409eff',
    strokeWeight: 5,
    strokeOpacity: 0.8,
    strokeStyle: 'solid',
    strokeDashArray: [10, 5],
  })

  mapInstance.add(polyline)
  routePolylines.push(polyline)

  // 自动调整视野
  mapInstance.setFitView([polyline])

  return polyline
}

/**
 * 绘制多条路线（用于展示不同方案）
 */
export async function drawRoutes(paths: [number, number][], colors?: string[]): Promise<any[]> {
  const defaultColors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c']
  const polylines: any[] = []

  // 清除已有路线
  clearRoute()

  for (let i = 0; i < paths.length; i++) {
    const path = paths[i]
    if (!path || (path as any[]).length === 0) continue

    const AMap = await loadAMap()
    const color = colors?.[i] || defaultColors[i % defaultColors.length]

    const polyline = new AMap.Polyline({
      path: path,
      strokeColor: color,
      strokeWeight: 5,
      strokeOpacity: 0.8,
      strokeStyle: 'solid',
    })

    mapInstance.add(polyline)
    polylines.push(polyline)
    routePolylines.push(polyline)
  }

  // 调整视野
  if (polylines.length > 0) {
    mapInstance.setFitView(polylines)
  }

  return polylines
}

/**
 * 清除路线
 */
export function clearRoute(): void {
  // 清除路线
  if (routePolylines.length > 0) {
    mapInstance.remove(routePolylines)
    routePolylines = []
  }
}

/**
 * 添加标记（简化接口）
 * @param position 位置 [lng, lat]
 * @param label 标签文字（A/B/数字等）
 * @param title 标题
 */
export async function addMarker(position: [number, number], label?: string, title?: string): Promise<any> {
  const AMap = await loadAMap()

  // 创建标记内容
  const content = label
    ? `<div style="
        width: 24px;
        height: 24px;
        background: ${label === 'A' ? '#409eff' : label === 'B' ? '#67c23a' : '#e6a23c'};
        border-radius: 50%;
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        font-weight: bold;
        box-shadow: 0 2px 4px rgba(0,0,0,0.3);
      ">${label}</div>`
    : ''

  const marker = new AMap.Marker({
    position,
    content,
    title: title || '',
    offset: label ? new AMap.Pixel(-12, -12) : new AMap.Pixel(-13, -30),
  })

  if (mapInstance) {
    mapInstance.add(marker)
  }

  return marker
}

/**
 * 移除标记
 * @param marker 标记对象
 */
export function removeMarker(marker: any): void {
  if (mapInstance && marker) {
    mapInstance.remove(marker)
  }
}

/**
 * 清除所有标记和路线
 */
export function clearAllRouteMarkers(): void {
  clearRoute()

  // 清除起点
  if (startMarker) {
    mapInstance.remove(startMarker)
    startMarker = null
  }

  // 清除终点
  if (endMarker) {
    mapInstance.remove(endMarker)
    endMarker = null
  }

  // 清除途经点
  if (waypointMarkers.length > 0) {
    mapInstance.remove(waypointMarkers)
    waypointMarkers = []
  }
}

/**
 * 解析路径字符串为坐标数组
 */
export function parsePathString(pathStr: string): [number, number][] {
  if (!pathStr) return []

  const points = pathStr.split(';')
  const result: [number, number][] = []
  for (const point of points) {
    const coords = point.split(',')
    if (coords.length >= 2) {
      const lng = Number(coords[0])
      const lat = Number(coords[1])
      if (!isNaN(lng) && !isNaN(lat)) {
        result.push([lng, lat])
      }
    }
  }
  return result
}
