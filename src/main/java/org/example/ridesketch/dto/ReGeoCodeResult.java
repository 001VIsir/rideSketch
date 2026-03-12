package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 逆地理编码结果（坐标转地址）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReGeoCodeResult {

    /**
     * 状态码
     */
    private String status;

    /**
     * 返回状态信息
     */
    private String info;

    /**
     * 格式化地址
     */
    private String formattedAddress;

    /**
     * 地址组件
     */
    private AddressComponent addressComponent;

    /**
     * 地址组件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressComponent {

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
    }
}
