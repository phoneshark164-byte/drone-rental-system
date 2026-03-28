package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.dao.entity.UserPaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户支付记录Mapper
 */
@Mapper
public interface UserPaymentRecordMapper extends BaseMapper<UserPaymentRecord> {
}
