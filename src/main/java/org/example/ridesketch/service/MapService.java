package org.example.ridesketch.service;

import org.example.ridesketch.dto.AddressSearchResult;
import org.example.ridesketch.dto.GeoCodeResult;
import org.example.ridesketch.dto.ReGeoCodeResult;

/**
 * 地图服务接口
 */
public interface MapService {

    /**
     * 地址搜索（关键词搜索POI）
     *
     * @param keyword 搜索关键词
     * @param city   城市名称（可选）
     * @return 搜索结果
     */
    AddressSearchResult searchAddress(String keyword, String city);

    /**
     * 地理编码（地址转坐标）
     *
     * @param address 地址
     * @return 地理编码结果
     */
    GeoCodeResult geocode(String address);

    /**
     * 逆地理编码（坐标转地址）
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 逆地理编码结果
     */
    ReGeoCodeResult reGeocode(String longitude, String latitude);
}
