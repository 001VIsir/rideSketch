# 高德地图 JS API 2.0 开发文档

本文档整理自高德开放平台官方文档，方便开发者在项目中使用高德地图JS API。

> 官方文档地址: https://lbs.amap.com/api/javascript-api-v2

---

## 目录

1. [概述](#1-概述)
2. [准备](#2-准备)
3. [快速上手](#3-快速上手)
4. [核心功能](#4-核心功能)
5. [入门教程](#5-入门教程)
6. [进阶教程](#6-进阶教程)
7. [参考资源](#7-参考资源)

---

## 1. 概述

### 1.1 简介

地图 JS API 2.0 是高德开放平台免费提供的第四代 Web 地图渲染引擎，以 WebGL 为主要绘图手段，本着"更轻、更快、更易用"的服务原则，广泛采用了各种前沿技术，交互体验、视觉体验大幅提升，同时提供了众多新增能力和特性。

**特点**：
- 与 1.x 版本的 JS API 接口基本保持一致
- 提供了适配器方便开发者零成本升级
- 兼容 IE10 及以上的所有浏览器环境
- 支持 PC 端、移动端自动适配

### 1.2 新特性预览

| 特性 | 说明 |
|------|------|
| 多边形吸附 | 支持多边形吸附功能 |
| MultiPolygon | 支持多边形多边形 |
| LabelMarker与主图标注避让 | 矢量标注与主图标注避让 |
| zoom范围扩展至 [2,20] | 缩放级别范围扩大 |
| 高性能矢量标注 LabelMarker | 高性能矢量标注 |
| 轨迹动画 | 支持轨迹动画效果 |

### 1.3 功能概览

#### 图层
- **官方图层**：标准矢量图层(TileLayer)、简易行政区图层(DistrictLayer)、卫星图层(TileLayer.Satellite)、路网数据(TileLayer.RoadNet)、实时路况数据(TileLayer.Traffic)、建筑楼块图层(Buildings)、室内地图(IndoorMap)
- **三方标准图层**：WMS图层(TileLayer.WMS)、WMTS图层(TileLayer.WMTS)、XYZ切片图层(TileLayer)
- **展示自有数据**：图片图层(ImageLayer)、Canvas图层(CanvasLayer)、任意切片图层(TileLayer.Flexible)、完全自定义图层(CustomLayer)、热力图(Heatmap)
- **地图控件**：比例尺(Scale)、缩放工具条(ToolBar)、鹰眼(HawkEye)、图层切换(MapType)、定位按钮(Geolocation)
- **信息窗体**：InfoWindow
- **右键菜单**：ContexMenu

#### 点标记
- **点标记**：一般点标记(Marker)、矢量点标记(LabelMarker)、文本标记(Text)、圆点标记(CircleMarker)、灵活点标记(ElasticMarker)
- **海量点标记**：矢量点标记图层(LabelsLayer)、海量点(MassMarks)、点聚合(MarkerCluster)
- **矢量图形**：圆(Circle)、折线(Polyline)、多边形(Polygon)、椭圆(Ellipse)、矩形(RecTangle)、弧线(BesizerCurve)

#### 坐标变换
- **坐标系变换**：经纬度 <-> 地图容器坐标、经纬度 <-> 地图平面坐标
- **地图坐标转换**：GPS(WGS84) -> 高德(GCJ02)、百度(Baidu) -> 高德、图吧(MapBar) -> 高德

#### 服务接口
- **搜索**：输入提示(Autocomplete)、POI搜索(PlaceSearch)
- **路线规划**：驾车(Driving)、货车(Truck)、步行(Walking)、骑行(Riding)、公交(Transfer)
- **行政区查询**：DistrictSearch
- **地理编码**：Geocoder
- **定位**：初始定位(Map)、城市定位(CitySearch)、浏览器定位(Geolocation)
- **其他服务**：天气(Weather)、公交站点(StationSearch)、公交线路(LineSearch)
- **支持库**：空间几何计算(GeometryUtil)、通用函数库(Util)、DOM函数库(DomUtil)、浏览器信息(Browser)

---

## 2. 准备

### 2.1 成为开发者并创建 Key

#### 步骤1：登录控制台
登录[高德开放平台控制台](https://console.amap.com/)，如果没有开发者账号，请[注册开发者](https://console.amap.com/dev/id)。

#### 步骤2：创建 Key
进入应用管理，创建新应用，新应用中添加 Key，服务平台选择 **Web端(JS API)**。

#### 步骤3：获取 Key 和安全密钥
创建成功后，可获取 Key 和安全密钥。

> **提示**：安全密钥机制旨在提升用户对 Key 的安全有效管理，降低明文传输被窃取的风险。2021年12月02日后创建的 Key 必须配备安全密钥一起使用。

---

## 3. 快速上手

### 3.1 第一个地图示例

#### 步骤1：准备 HTML 页面

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="initial-scale=1.0, user-scalable=no, width=device-width">
    <title>HELLO，AMAP!</title>
    <style>
        html, body, #container {
            width: 100%;
            height: 100%;
        }
    </style>
</head>
<body>
    <div id="container"></div>
</body>
</html>
```

#### 步骤2：JS API 的加载和地图初始化

使用 JS API Loader 来加载，引入在控制台申请的 Key 和安全密钥：

```html
<!-- 安全密钥配置 -->
<script type="text/javascript">
    window._AMapSecurityConfig = {
        securityJsCode: "「你申请的安全密钥」",
    };
</script>

<!-- 引入 JS API Loader -->
<script src="https://webapi.amap.com/loader.js"></script>

<!-- 初始化地图 -->
<script type="text/javascript">
    AMapLoader.load({
        key: "「你申请的应用Key」", // 申请好的Web端开发者 Key，调用 load 时必填
        version: "2.0", // 指定要加载的 JS API 的版本，缺省时默认为 1.4.15
    })
    .then((AMap) => {
        const map = new AMap.Map("container");
    })
    .catch((e) => {
        console.error(e); // 加载错误提示
    });
</script>
```

#### 步骤3：添加点标记 Marker

```javascript
const marker = new AMap.Marker({
    position: [116.39, 39.9], // 位置
});
map.add(marker); // 添加到地图
```

#### 步骤4：添加事件和信息窗体

```javascript
// 创建信息窗体
const infoWindow = new AMap.InfoWindow({
    isCustom: true, // 使用自定义窗体
    content: "<div>HELLO,AMAP!</div>", // 信息窗体的内容可以是任意html片段
    offset: new AMap.Pixel(16, -45),
});

// 绑定click事件
const onMarkerClick = function(e) {
    infoWindow.open(map, e.target.getPosition()); // 打开信息窗体
};

const marker = new AMap.Marker({
    position: [116.481181, 39.989792],
});
map.add(marker);
marker.on("click", onMarkerClick);
```

#### 步骤5：添加折线 Polyline

```javascript
const lineArr = [
    [116.368904, 39.913423],
    [116.382122, 39.901176],
    [116.387271, 39.912501],
    [116.398258, 39.904600]
];

const polyline = new AMap.Polyline({
    path: lineArr, // 设置线覆盖物路径
    strokeColor: "#3366FF", // 线颜色
    strokeWeight: 5, // 线宽
    strokeStyle: "solid", // 线样式
});

map.add(polyline);
```

### 3.2 线上资源

- **示例中心 Demo**: https://lbs.amap.com/demo/jsapi-v2/example/map-lifecycle/map-show
- **地图快速生成器**: https://lbs.amap.com/tools/creater

---

## 4. 核心功能

### 4.1 AMap.Marker (点标记)

| 参数 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| position | 点标记的位置信息 | Array | - |
| offset | 相对于基点的偏移位置 | Array | [0,0] |
| icon | 点标记的图标 | String/Icon | 默认图标 |
| title | 鼠标hover时的标题 | String | - |
| clickable | 是否可点击 | Boolean | true |
| draggable | 是否可拖拽 | Boolean | false |

**常用方法**：
- `map.add(marker)` - 添加到地图
- `marker.on(event, handler)` - 绑定事件
- `marker.setPosition(position)` - 设置位置
- `marker.getPosition()` - 获取位置

### 4.2 AMap.InfoWindow (信息窗体)

| 参数 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| isCustom | 使用自定义窗体 | Boolean | false |
| content | 信息窗体的内容(任意html片段) | String | - |
| offset | 相对于基点的偏移位置 | Array | [0,0] |
| position | 信息窗体的位置信息 | Array | - |
| showShadow | 是否显示阴影 | Boolean | false |

**常用方法**：
- `infoWindow.open(map, position)` - 打开信息窗体
- `infoWindow.close()` - 关闭信息窗体

### 4.3 AMap.Polyline (折线)

| 参数 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| path | 设置线覆盖物路径 | Array | - |
| strokeColor | 线颜色(16进制颜色代码) | String | #00D3FC |
| strokeWeight | 线宽 | Number | 2 |
| strokeStyle | 线样式(solid/dashed) | String | solid |
| strokeOpacity | 线透明度 | Number | 1 |

### 4.4 AMap.Map (地图)

| 参数 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| container | 地图容器元素或id | HTMLElement/String | - |
| center | 地图中心点坐标 | Array | [116.397428, 39.90923] |
| zoom | 地图缩放级别 | Number | 12 |
| viewMode | 地图视图模式(2D/3D) | String | '2D' |
| pitch | 倾斜角度(3D模式有效) | Number | 0 |
| rotation | 旋转角度 | Number | 0 |

**常用方法**：
- `map.add(overlay)` - 添加覆盖物
- `map.remove(overlay)` - 移除覆盖物
- `map.setCenter(position)` - 设置中心点
- `map.setZoom(zoom)` - 设置缩放级别
- `map.on(event, handler)` - 绑定事件

---

## 5. 入门教程

### 5.1 展示地图

```javascript
// 创建地图实例
const map = new AMap.Map('container', {
    zoom: 12, // 初始缩放级别
    center: [116.397428, 39.90923], // 初始中心点
    viewMode: '2D', // 2D或3D
});
```

### 5.2 展示图层

```javascript
// 卫星图层
const satelliteLayer = new AMap.TileLayer.Satellite();
// 路网图层
const roadNetLayer = new AMap.TileLayer.RoadNet();

// 添加到地图
map.add(satelliteLayer);
map.add(roadNetLayer);
```

### 5.3 添加地图控件

```javascript
// 添加缩放工具条
map.addControl(new AMap.ToolBar({
    position: 'RT'
}));

// 添加定位按钮
map.addControl(new AMap.Geolocation());

// 添加比例尺
map.addControl(new AMap.Scale());

// 添加鹰眼
map.addControl(new AMap.HawkEye({
    isOpen: true
}));
```

### 5.4 添加多边形

```javascript
const polygon = new AMap.Polygon({
    path: [
        [116.403322, 39.920255],
        [116.410703, 39.897555],
        [116.402292, 39.892353],
        [116.389846, 39.891365]
    ],
    fillColor: '#1791fc',
    fillOpacity: 0.3,
    strokeColor: '#1791fc',
    strokeWeight: 2
});

map.add(polygon);
```

### 5.5 搜索地点

```javascript
// POI搜索
const placeSearch = new AMap.PlaceSearch({
    city: '010', // 城市
    citylimit: true,
    pageSize: 10,
    pageIndex: 1
});

placeSearch.search('北京大学', function(status, result) {
    if (status === 'complete' && result.info === 'OK') {
        // 处理搜索结果
        const pois = result.poiList.pois;
        pois.forEach(poi => {
            const marker = new AMap.Marker({
                position: poi.location
            });
            map.add(marker);
        });
    }
});
```

### 5.6 规划路线

```javascript
// 驾车路线规划
const driving = new AMap.Driving({
    policy: AMap.DrivingPolicy.LEAST_TIME,
    map: map,
    panel: "panel"
});

driving.search([
    { keyword: '北京市天安门', city: '北京' },
    { keyword: '北京市北京大学', city: '北京' }
], function(status, result) {
    // 回调处理
});
```

---

## 6. 进阶教程

### 6.1 JS API 安全密钥使用

```javascript
// 在引入loader.js之前配置安全密钥
window._AMapSecurityConfig = {
    securityJsCode: "你的安全密钥",
};
```

### 6.2 坐标转换

```javascript
// GPS坐标转高德坐标
AMap.convertFrom([116.39, 39.9], 'gps', function(status, result) {
    if (result.info === 'OK') {
        const coord = result.locations[0];
        console.log(coord.lng, coord.lat);
    }
});

// 百度坐标转高德坐标
AMap.convertFrom([116.39, 39.9], 'baidu', function(status, result) {
    // 处理转换结果
});
```

### 6.3 地理编码与逆地理编码

```javascript
// 地理编码 - 地址转坐标
const geocoder = new AMap.Geocoder();
geocoder.getLocation('北京市朝阳区望京', function(status, result) {
    if (status === 'complete' && result.info === 'OK') {
        const location = result.geocodes[0].location;
        console.log(location.lng, location.lat);
    }
});

// 逆地理编码 - 坐标转地址
geocoder.getAddress([116.39, 39.9], function(status, result) {
    if (status === 'complete' && result.info === 'OK') {
        const address = result.regeocode.formattedAddress;
        console.log(address);
    }
});
```

### 6.4 定位服务

```javascript
// 浏览器定位
const geolocation = new AMap.Geolocation({
    enableHighAccuracy: true, // 是否使用高精度定位
    timeout: 10000, // 超时时间
    buttonPosition: 'RB' // 定位按钮位置
});

map.addControl(geolocation);

geolocation.getCurrentPosition(function(status, result) {
    if (status === 'complete') {
        const position = result.position;
        console.log(position.lng, position.lat);
    }
});
```

### 6.5 海量点标记

```javascript
// 创建海量点
const massMarks = new AMap.MassMarks({
    zIndex: 100,
    style: {
        url: 'marker.png',
        anchor: new AMap.Pixel(10, 30),
        size: new AMap.Size(20, 30)
    }
});

const data = [
    { lnglat: [116.39, 39.9], name: '点1' },
    { lnglat: [116.40, 39.91], name: '点2' },
    // ... 更多点数据
];

massMarks.setData(data);
map.add(massMarks);
```

### 6.6 点聚合

```javascript
const markers = [];

// 添加大量标记点
for (let i = 0; i < 100; i++) {
    markers.push(new AMap.Marker({
        position: [116.39 + Math.random() * 0.5, 39.9 + Math.random() * 0.5]
    }));
}

// 创建聚合实例
const cluster = new AMap.MarkerCluster(map, markers, {
    maxZoom: 15,
    gridSize: 80
});
```

---

## 7. 参考资源

### 7.1 官方文档链接

| 资源 | 地址 |
|------|------|
| 官方首页 | https://lbs.amap.com/api/javascript-api-v2 |
| 示例中心 | https://lbs.amap.com/demo/list/js-api-v2 |
| 参考手册 | https://lbs.amap.com/api/javascript-api-v2/documentation |
| 升级指南 | https://lbs.amap.com/api/javascript-api-v2/update |
| 常见问题 | https://lbs.amap.com/faq/web/javascript-api |
| 创建工单 | https://console.amap.com/dev/ticket/type |

### 7.2 TypeScript 支持

获取 TypeScript 环境支持可使用 DTS 声明文件库：
```
npm install @amap/amap-jsapi-types
```

### 7.3 常用 URL

- JS API Loader: `https://webapi.amap.com/loader.js`
- 地图瓦片服务: `https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}`

---

## 附录：错误码说明

| 错误码 | 说明 |
|--------|------|
| 1000 | 服务端错误 |
| 1001 | key无效或不存在 |
| 1002 | key配额已用完 |
| 1003 | key被禁用 |
| 1004 | 签名错误 |
| 1005 | 协议不支持 |
| 1006 | 安全验证失败 |
| 1007 | 请求临界值Exceeded |
| 1008 | 设备不支持定位 |
| 1009 | 请求接口失败 |

---

> 文档最后更新时间: 2025年01月16日
> 本文档由开发者整理，仅供内部参考使用。如有疑问，请查阅官方文档或提交工单。
