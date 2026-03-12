package org.example.ridesketch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.common.Result;
import org.example.ridesketch.dto.AddressSearchResult;
import org.example.ridesketch.dto.GeoCodeResult;
import org.example.ridesketch.dto.ReGeoCodeResult;
import org.example.ridesketch.service.MapService;
import org.springframework.web.bind.annotation.*;

/**
 * 地图服务控制器
 * <p>
 * 提供地图相关的地理编码、逆地理编码和POI搜索功能。
 * 基于高德地图API实现，为路线规划和社区分享提供地图服务支持。
 * 所有接口前缀为 /api/map
 *
 * @author rideSketch
 * @version 1.0.0
 * @see MapService
 * @see org.example.ridesketch.dto.AddressSearchResult
 * @see org.example.ridesketch.dto.GeoCodeResult
 * @see org.example.ridesketch.dto.ReGeoCodeResult
 */
@Slf4j
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    /**
     * 地址搜索接口（关键词搜索POI）
     * <p>
     * 根据关键词搜索周边的兴趣点（POI），支持按城市范围筛选。
     * 常用于用户在地图上搜索地点、查找目的地等场景。
     *
     * @api GET /api/map/search?keyword=关键词&city=城市名（可选）
     * @param keyword 搜索关键词，如"餐厅"、"商场"、"地铁站"等POI名称或类型
     * @param city 城市名称（可选），指定搜索的城市范围，不填则全国搜索
     * @return Result 包含AddressSearchResult
     *         - poiList: 匹配的POI列表，每个包含name（名称）、address（地址）、
     *                    location（经纬度坐标）、type（类型）等信息
     * @see AddressSearchResult
     * @see org.example.ridesketch.entity.POI
     */
    @GetMapping("/search")
    public Result<AddressSearchResult> searchAddress(
            @RequestParam String keyword,
            @RequestParam(required = false) String city) {
        log.info("地址搜索: keyword={}, city={}", keyword, city);
        // 调用地图服务进行POI搜索
        AddressSearchResult result = mapService.searchAddress(keyword, city);
        return Result.success(result);
    }

    /**
     * 地理编码接口（地址转坐标）
     * <p>
     * 将文字地址转换为对应的经纬度坐标。
     * 常用于用户输入地址后获取坐标，以便在地图上标注或进行路线规划。
     *
     * @api GET /api/map/geocode?address=详细地址
     * @param address 详细地址字符串，支持省市区+街道门牌号等格式
     *                例如："北京市朝阳区建国路93号"
     * @return Result 包含GeoCodeResult
     *         - location: 经纬度坐标，格式为"经度,纬度"（如"116.397428,39.90923"）
     *         - precision: 匹配精度
     * @see GeoCodeResult
     */
    @GetMapping("/geocode")
    public Result<GeoCodeResult> geocode(@RequestParam String address) {
        log.info("地理编码: address={}", address);
        // 调用地图服务进行地理编码
        GeoCodeResult result = mapService.geocode(address);
        return Result.success(result);
    }

    /**
     * 逆地理编码接口（坐标转地址）
     * <p>
     * 将经纬度坐标转换为对应的文字地址描述。
     * 常用于获取用户当前位置的详细地址，或在地图上点击获取地点信息。
     *
     * @api GET /api/map/regeocode?longitude=经度&latitude=纬度
     * @param longitude 经度值，如"116.397428"
     * @param latitude 纬度值，如"39.90923"
     * @return Result 包含ReGeoCodeResult
     *         - address: 简要地址描述
     *         - formattedAddress: 完整格式化地址
     *         - province、城市、district等: 各级行政区划信息
     * @see ReGeoCodeResult
     */
    @GetMapping("/regeocode")
    public Result<ReGeoCodeResult> reGeocode(
            @RequestParam String longitude,
            @RequestParam String latitude) {
        log.info("逆地理编码: longitude={}, latitude={}", longitude, latitude);
        // 调用地图服务进行逆地理编码
        ReGeoCodeResult result = mapService.reGeocode(longitude, latitude);
        return Result.success(result);
    }
}
