package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.SysNotification;
import com.drone.rental.dao.mapper.SysNotificationMapper;
import com.drone.rental.service.service.SysNotificationService;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现
 */
@Service
public class SysNotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification> implements SysNotificationService {
}
