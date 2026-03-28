package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.dao.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
