package org.example.ridesketch.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.ridesketch.dto.AddressSearchResult;
import org.example.ridesketch.dto.GeoCodeResult;
import org.example.ridesketch.dto.ReGeoCodeResult;
import org.example.ridesketch.service.MapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * 地图服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private final RestTemplate restTemplate;

    /**
     * 高德地图Web服务API基础URL
     */
    private static final String AMAP_BASE_URL = "https://restapi.amap.com/v3";

    @Value("${amap.key}")
    private String amapKey;

    @Value("${amap.security-key:}")
    private String amapSecurityKey;

    @Override
    public AddressSearchResult searchAddress(String keyword, String city) {
        try {
            String url = AMAP_BASE_URL + "/place/text?key=" + amapKey
                    + "&keywords=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&output=json"
                    + "&offset=20"
                    + "&page=1"
                    + "&extensions=all";

            if (StringUtils.isNotBlank(city)) {
                url += "&city=" + URLEncoder.encode(city, StandardCharsets.UTF_8);
            }

            log.debug("搜索地址请求URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            log.info("搜索地址返回结果: {}", body);

            return parseAddressSearchResult(body);
        } catch (Exception e) {
            log.error("地址搜索失败: {}", e.getMessage(), e);
            return AddressSearchResult.builder()
                    .status("0")
                    .info(e.getMessage())
                    .pois(new ArrayList<>())
                    .build();
        }
    }

    @Override
    public GeoCodeResult geocode(String address) {
        try {
            String url = AMAP_BASE_URL + "/geocode/geo?key=" + amapKey
                    + "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8)
                    + "&output=json";

            log.debug("地理编码请求URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            log.debug("地理编码返回结果: {}", body);

            return parseGeoCodeResult(body);
        } catch (Exception e) {
            log.error("地理编码失败: {}", e.getMessage(), e);
            return GeoCodeResult.builder()
                    .status("0")
                    .info(e.getMessage())
                    .build();
        }
    }

    @Override
    public ReGeoCodeResult reGeocode(String longitude, String latitude) {
        try {
            String location = longitude + "," + latitude;
            String url = AMAP_BASE_URL + "/geocode/regeo?key=" + amapKey
                    + "&location=" + location
                    + "&output=json"
                    + "&extensions=base";

            log.debug("逆地理编码请求URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            log.debug("逆地理编码返回结果: {}", body);

            return parseReGeoCodeResult(body);
        } catch (Exception e) {
            log.error("逆地理编码失败: {}", e.getMessage(), e);
            return ReGeoCodeResult.builder()
                    .status("0")
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 生成高德API签名
     */
    private String generateSig(String params) {
        if (StringUtils.isBlank(amapSecurityKey)) {
            return "";
        }
        try {
            String signStr = params + amapSecurityKey;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(signStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("生成签名失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 解析地址搜索结果
     */
    private AddressSearchResult parseAddressSearchResult(String json) {
        try {
            log.info("原始返回JSON: {}", json.substring(0, Math.min(500, json.length())));

            JSONObject jsonObject = JSON.parseObject(json);
            AddressSearchResult result = new AddressSearchResult();
            result.setStatus(jsonObject.getString("status"));
            result.setInfo(jsonObject.getString("info"));

            log.info("解析地址搜索: status={}, info={}", result.getStatus(), result.getInfo());

            List<AddressSearchResult.PoiInfo> pois = new ArrayList<>();
            if ("1".equals(result.getStatus())) {
                // 直接获取JSON数组而不是字符串
                JSONArray poisArray = jsonObject.getJSONArray("pois");
                log.info("poisArray是否为null: {}, size: {}", poisArray == null, poisArray != null ? poisArray.size() : 0);

                if (poisArray != null && poisArray.size() > 0) {
                    for (int i = 0; i < poisArray.size(); i++) {
                        JSONObject poi = poisArray.getJSONObject(i);
                        // 解析location字段，格式为 "经度,纬度"
                        String location = poi.getString("location");
                        String lat = null;
                        String lng = null;
                        if (StringUtils.isNotBlank(location) && location.contains(",")) {
                            String[] parts = location.split(",");
                            lng = parts[0];
                            lat = parts[1];
                        }

                        AddressSearchResult.PoiInfo poiInfo = AddressSearchResult.PoiInfo.builder()
                                .id(poi.getString("id"))
                                .name(poi.getString("name"))
                                .type(poi.getString("type"))
                                .typecode(poi.getString("typecode"))
                                .latitude(lat)
                                .longitude(lng)
                                .address(poi.getString("address"))
                                .province(poi.getString("pname"))
                                .city(poi.getString("cityname"))
                                .district(poi.getString("adname"))
                                .build();
                        pois.add(poiInfo);
                    }
                }
            }

            result.setPois(pois);
            log.info("最终解析结果: pois数量={}", pois.size());
            return result;
        } catch (Exception e) {
            log.error("解析地址搜索结果失败: {}", e.getMessage(), e);
            return AddressSearchResult.builder()
                    .status("0")
                    .info("解析失败: " + e.getMessage())
                    .pois(new ArrayList<>())
                    .build();
        }
    }

    /**
     * 解析地理编码结果
     */
    private GeoCodeResult parseGeoCodeResult(String json) {
        try {
            JSONObject jsonObject = JSON.parseObject(json);
            GeoCodeResult result = new GeoCodeResult();
            result.setStatus(jsonObject.getString("status"));
            result.setInfo(jsonObject.getString("info"));

            if ("1".equals(result.getStatus())) {
                String geocodesJson = jsonObject.getString("geocodes");
                if (StringUtils.isNotBlank(geocodesJson) && geocodesJson.startsWith("[")) {
                    List<JSONObject> geocodeList = JSON.parseArray(geocodesJson, JSONObject.class);
                    if (!geocodeList.isEmpty()) {
                        JSONObject geo = geocodeList.get(0);

                        // 从location字段解析经纬度，格式为"经度,纬度"
                        String location = geo.getString("location");
                        String lat = null;
                        String lng = null;
                        if (StringUtils.isNotBlank(location) && location.contains(",")) {
                            String[] parts = location.split(",");
                            lng = parts[0];
                            lat = parts[1];
                        }

                        GeoCodeResult.GeocodeInfo geocodeInfo = GeoCodeResult.GeocodeInfo.builder()
                                .formattedAddress(geo.getString("formatted_address"))
                                .country(geo.getString("country"))
                                .province(geo.getString("province"))
                                .city(geo.getString("city"))
                                .citycode(geo.getString("citycode"))
                                .district(geo.getString("district"))
                                .township(geo.getString("township"))
                                .street(geo.getString("street"))
                                .number(geo.getString("number"))
                                .lat(lat)
                                .lng(lng)
                                .confidence(geo.getString("confidence"))
                                .level(geo.getString("level"))
                                .build();
                        result.setGeocodes(geocodeInfo);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("解析地理编码结果失败: {}", e.getMessage(), e);
            return GeoCodeResult.builder()
                    .status("0")
                    .info("解析失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析逆地理编码结果
     */
    private ReGeoCodeResult parseReGeoCodeResult(String json) {
        try {
            JSONObject jsonObject = JSON.parseObject(json);
            ReGeoCodeResult result = ReGeoCodeResult.builder()
                    .status(jsonObject.getString("status"))
                    .info(jsonObject.getString("info"))
                    .build();

            if ("1".equals(result.getStatus())) {
                JSONObject regeocode = jsonObject.getJSONObject("regeocode");
                if (regeocode != null) {
                    result.setFormattedAddress(regeocode.getString("formatted_address"));

                    JSONObject addressComponent = regeocode.getJSONObject("addressComponent");
                    if (addressComponent != null) {
                        ReGeoCodeResult.AddressComponent component = ReGeoCodeResult.AddressComponent.builder()
                                .country(addressComponent.getString("country"))
                                .province(addressComponent.getString("province"))
                                .city(addressComponent.getString("city"))
                                .citycode(addressComponent.getString("citycode"))
                                .district(addressComponent.getString("district"))
                                .township(addressComponent.getString("township"))
                                .street(addressComponent.getString("street"))
                                .number(addressComponent.getString("number"))
                                .build();
                        result.setAddressComponent(component);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("解析逆地理编码结果失败: {}", e.getMessage(), e);
            return ReGeoCodeResult.builder()
                    .status("0")
                    .info("解析失败: " + e.getMessage())
                    .build();
        }
    }
}
