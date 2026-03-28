package com.drone.rental.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.DroneSystemConfig;

import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface DroneSystemConfigService extends IService<DroneSystemConfig> {

    /**
     * 获取所有配置（按分组返回）
     */
    Map<String, Map<String, String>> getAllConfigs();

    /**
     * 获取指定配置的值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 更新配置
     */
    boolean updateConfig(String configKey, String configValue);

    /**
     * 批量更新配置
     */
    boolean batchUpdateConfigs(Map<String, String> configs);
}
