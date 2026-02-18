package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 地址搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressSearchResult {

    /**
     * 状态码
     */
    private String status;

    /**
     * 返回状态信息
     */
    private String info;

    /**
     * 搜索结果列表
     */
    private List<PoiInfo> pois;

    /**
     * POI信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoiInfo {

        /**
         * POI唯一标识
         */
        private String id;

        /**
         * 名称
         */
        private String name;

        /**
         * 类型
         */
        private String type;

        /**
         * 类型编码
         */
        private String typecode;

        /**
         * 纬度
         */
        private String latitude;

        /**
         * 经度
         */
        private String longitude;

        /**
         * 地址
         */
        private String address;

        /**
         * 省份
         */
        private String province;

        /**
         * 城市
         */
        private String city;

        /**
         * 区域
         */
        private String district;
    }
}
