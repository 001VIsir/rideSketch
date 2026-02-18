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
 * 地图控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    /**
     * 地址搜索（关键词搜索POI）
     *
     * @param keyword 搜索关键词
     * @param city   城市名称（可选）
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<AddressSearchResult> searchAddress(
            @RequestParam String keyword,
            @RequestParam(required = false) String city) {
        log.info("地址搜索: keyword={}, city={}", keyword, city);
        AddressSearchResult result = mapService.searchAddress(keyword, city);
        return Result.success(result);
    }

    /**
     * 地理编码（地址转坐标）
     *
     * @param address 地址
     * @return 地理编码结果
     */
    @GetMapping("/geocode")
    public Result<GeoCodeResult> geocode(@RequestParam String address) {
        log.info("地理编码: address={}", address);
        GeoCodeResult result = mapService.geocode(address);
        return Result.success(result);
    }

    /**
     * 逆地理编码（坐标转地址）
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 逆地理编码结果
     */
    @GetMapping("/regeocode")
    public Result<ReGeoCodeResult> reGeocode(
            @RequestParam String longitude,
            @RequestParam String latitude) {
        log.info("逆地理编码: longitude={}, latitude={}", longitude, latitude);
        ReGeoCodeResult result = mapService.reGeocode(longitude, latitude);
        return Result.success(result);
    }
}
