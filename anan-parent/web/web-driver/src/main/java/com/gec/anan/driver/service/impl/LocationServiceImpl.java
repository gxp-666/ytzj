package com.gec.anan.driver.service.impl;

import com.gec.anan.common.constant.RedisConstant;
import com.gec.anan.common.constant.SystemConstant;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.driver.client.DriverInfoFeignClient;
import com.gec.anan.driver.service.LocationService;
import com.gec.anan.map.client.LocationFeignClient;
import com.gec.anan.model.entity.driver.DriverSet;
import com.gec.anan.model.form.map.OrderServiceLocationForm;
import com.gec.anan.model.form.map.SearchNearByDriverForm;
import com.gec.anan.model.form.map.UpdateDriverLocationForm;
import com.gec.anan.model.form.map.UpdateOrderLocationForm;
import com.gec.anan.model.vo.map.NearByDriverVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Autowired
    LocationFeignClient locationFeignClient;

    @Autowired
    DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return locationFeignClient.saveOrderServiceLocation(orderLocationServiceFormList).getData();
    }

    // 更新订单位置到Redis缓存
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        return locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm).getData();
    }


    // 开启接单服务：更新司机经纬度位置
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        //根据司机id查询set信息
        DriverSet driverSet = driverInfoFeignClient.getDriverSet(updateDriverLocationForm.getDriverId()).getData();
        //判断当前的司机是否开启了接单
        if (driverSet != null && driverSet.getServiceStatus().intValue() == 1) {//开启了接单
            return locationFeignClient.updateDriverLocation(updateDriverLocationForm).getData();
        } else {//抛出异常
            throw new AnanException(ResultCodeEnum.NO_START_SERVICE);
        }

    }
}
