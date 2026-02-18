declare namespace AMap {
  class Map {
    constructor(container: string | HTMLElement, opts?: MapOptions)
    setCenter(lnglat: LngLat | [number, number]): void
    getCenter(): LngLat
    setZoom(zoom: number): void
    getZoom(): number
    addControl(control: any): void
    on(event: string, handler: Function): void
    off(event: string, handler: Function): void
    destroy(): void
  }

  interface MapOptions {
    viewMode?: '2D' | '3D'
    zoom?: number
    center?: LngLat | [number, number]
    mapStyle?: string
    pitch?: number
    rotation?: number
  }

  class LngLat {
    constructor(lng: number, lat: number)
    getLng(): number
    getLat(): number
  }

  class ToolBar {
    constructor(opts?: any)
  }

  class Scale {
    constructor(opts?: any)
  }

  class Geolocation {
    constructor(opts?: any)
  }

  class PlaceSearch {
    constructor(opts?: any)
    search(keyword: string, callback: Function): void
  }

  class Geocoder {
    constructor(opts?: any)
    getLocation(address: string, callback: Function): void
    getAddress(lnglat: [number, number], callback: Function): void
  }
}

export {}
