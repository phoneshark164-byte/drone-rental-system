package com.drone.rental.web.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 高德地图地理编码工具类
 * 用于将地址转换为经纬度坐标
 */
@Slf4j
public class AmapGeocodingUtil {

    /**
     * 高德地图API Key
     */
    private static final String AMAP_API_KEY = "c692ec51e7cc024d0853e05ea8ee2979";

    /**
     * 地理编码API地址
     */
    private static final String GEOCODING_URL = "https://restapi.amap.com/v3/geocode/geo";

    /**
     * 将地址转换为经纬度坐标
     *
     * @param address 地址描述，如"四川省成都市武侯区"
     * @return 包含经纬度的数组 [longitude, latitude]，失败返回 null
     */
    public static double[] geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.warn("地理编码失败：地址为空");
            return null;
        }

        try {
            // 构建请求URL
            String url = String.format("%s?key=%s&address=%s",
                    GEOCODING_URL,
                    AMAP_API_KEY,
                    URLEncoder.encode(address, StandardCharsets.UTF_8));

            // 发送请求
            String response = HttpUtil.get(url, 5000);
            log.info("地理编码请求：地址={}, 响应={}", address, response);

            if (response == null || response.isEmpty()) {
                log.error("地理编码失败：响应为空");
                return null;
            }

            // 解析响应
            JSONObject json = JSONUtil.parseObj(response);
            String status = json.getStr("status");

            if ("1".equals(status)) {
                // 获取第一个匹配的地理位置
                JSONObject geocodes = json.getJSONArray("geocodes").getJSONObject(0);
                String location = geocodes.getStr("location");

                if (location != null && !location.isEmpty()) {
                    String[] coords = location.split(",");
                    if (coords.length == 2) {
                        double longitude = Double.parseDouble(coords[0]);
                        double latitude = Double.parseDouble(coords[1]);
                        log.info("地理编码成功：{} -> 经度={}, 纬度={}", address, longitude, latitude);
                        return new double[]{longitude, latitude};
                    }
                }
            } else {
                String errorMsg = json.getStr("info");
                log.error("地理编码失败：{}", errorMsg);
            }

        } catch (Exception e) {
            log.error("地理编码异常：address={}, error={}", address, e.getMessage());
        }

        return null;
    }

    /**
     * 将地址转换为经纬度坐标（指定城市）
     *
     * @param address 地址描述
     * @param city    指定查询城市
     * @return 包含经纬度的数组 [longitude, latitude]，失败返回 null
     */
    public static double[] geocode(String address, String city) {
        if (address == null || address.trim().isEmpty()) {
            log.warn("地理编码失败：地址为空");
            return null;
        }

        try {
            String url = String.format("%s?key=%s&address=%s&city=%s",
                    GEOCODING_URL,
                    AMAP_API_KEY,
                    URLEncoder.encode(address, StandardCharsets.UTF_8),
                    URLEncoder.encode(city, StandardCharsets.UTF_8));

            String response = HttpUtil.get(url, 5000);
            JSONObject json = JSONUtil.parseObj(response);

            if ("1".equals(json.getStr("status"))) {
                JSONObject geocodes = json.getJSONArray("geocodes").getJSONObject(0);
                String location = geocodes.getStr("location");

                if (location != null && !location.isEmpty()) {
                    String[] coords = location.split(",");
                    if (coords.length == 2) {
                        double longitude = Double.parseDouble(coords[0]);
                        double latitude = Double.parseDouble(coords[1]);
                        log.info("地理编码成功：{} (城市:{}) -> 经度={}, 纬度={}", address, city, longitude, latitude);
                        return new double[]{longitude, latitude};
                    }
                }
            } else {
                log.error("地理编码失败：{}", json.getStr("info"));
            }

        } catch (Exception e) {
            log.error("地理编码异常：address={}, city={}, error={}", address, city, e.getMessage());
        }

        return null;
    }

    /**
     * 逆地理编码：将经纬度转换为地址
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 详细地址，失败返回 null
     */
    public static String reverseGeocode(double longitude, double latitude) {
        try {
            String url = String.format("https://restapi.amap.com/v3/geocode/regeo?key=%s&location=%s,%s",
                    AMAP_API_KEY,
                    longitude,
                    latitude);

            String response = HttpUtil.get(url, 5000);
            JSONObject json = JSONUtil.parseObj(response);

            if ("1".equals(json.getStr("status"))) {
                JSONObject regeocode = json.getJSONObject("regeocode");
                String formattedAddress = regeocode.getStr("formatted_address");
                log.info("逆地理编码成功：经度={}, 纬度={} -> 地址={}", longitude, latitude, formattedAddress);
                return formattedAddress;
            }

        } catch (Exception e) {
            log.error("逆地理编码异常：lng={}, lat={}, error={}", longitude, latitude, e.getMessage());
        }

        return null;
    }
}
