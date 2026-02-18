package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地理编码结果（地址转坐标）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoCodeResult {

    /**
     * 状态码
     */
    private String status;

    /**
     * 返回状态信息
     */
    private String info;

    /**
     * 地理编码信息
     */
    private GeocodeInfo geocodes;

    /**
     * 地理编码详细信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeocodeInfo {

        /**
         * 格式化地址
         */
        private String formattedAddress;

        /**
         * 国家
         */
        private String country;

        /**
         * 省份
         */
        private String province;

        /**
         * 城市
         */
        private String city;

        /**
         * 城市编码
         */
        private String citycode;

        /**
         * 区域
         */
        private String district;

        /**
         * 乡镇
         */
        private String township;

        /**
         * 街道
         */
        private String street;

        /**
         * 门牌号
         */
        private String number;

        /**
         * 纬度
         */
        private String lat;

        /**
         * 经度
         */
        private String lng;

        /**
         * 置信度
         */
        private String confidence;

        /**
         * 匹配级别
         */
        private String level;
    }
}
