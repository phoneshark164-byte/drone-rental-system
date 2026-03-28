package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dao.entity.DroneSystemConfig;
import com.drone.rental.dao.mapper.DroneSystemConfigMapper;
import com.drone.rental.service.service.DroneSystemConfigService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现
 */
@Service
public class DroneSystemConfigServiceImpl extends ServiceImpl<DroneSystemConfigMapper, DroneSystemConfig> implements DroneSystemConfigService {

    @Override
    public Map<String, Map<String, String>> getAllConfigs() {
        List<DroneSystemConfig> configs = list();
        Map<String, Map<String, String>> result = new HashMap<>();
        for (DroneSystemConfig config : configs) {
            String group = config.getConfigGroup();
            if (!result.containsKey(group)) {
                result.put(group, new HashMap<>());
            }
            result.get(group).put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        DroneSystemConfig config = getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneSystemConfig>()
                .eq(DroneSystemConfig::getConfigKey, configKey));
        return config != null ? config.getConfigValue() : defaultValue;
    }

    @Override
    public boolean updateConfig(String configKey, String configValue) {
        DroneSystemConfig config = getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneSystemConfig>()
                .eq(DroneSystemConfig::getConfigKey, configKey));
        if (config == null) {
            throw new BusinessException("配置不存在: " + configKey);
        }
        config.setConfigValue(configValue);
        return updateById(config);
    }

    @Override
    public boolean batchUpdateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            updateConfig(entry.getKey(), entry.getValue());
        }
        return true;
    }
}
