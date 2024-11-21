package com.gec.anan.map.service.impl;

import com.gec.anan.common.constant.RedisConstant;
import com.gec.anan.common.constant.SystemConstant;
import com.gec.anan.common.util.LocationUtil;
import com.gec.anan.driver.client.DriverInfoFeignClient;
import com.gec.anan.map.repository.OrderServiceLocationRepository;
import com.gec.anan.map.service.LocationService;
import com.gec.anan.model.entity.driver.DriverSet;
import com.gec.anan.model.entity.map.OrderServiceLocation;
import com.gec.anan.model.form.map.OrderServiceLocationForm;
import com.gec.anan.model.form.map.SearchNearByDriverForm;
import com.gec.anan.model.form.map.UpdateDriverLocationForm;
import com.gec.anan.model.form.map.UpdateOrderLocationForm;
import com.gec.anan.model.vo.map.NearByDriverVo;
import com.gec.anan.model.vo.map.OrderLocationVo;
import com.gec.anan.model.vo.map.OrderServiceLastLocationVo;
import com.gec.anan.order.client.OrderInfoFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * LocationServiceImpl类实现了LocationService接口，提供地理位置相关操作的具体实现。
 * 主要使用Redis的GEO功能来存储和操作司机的地理位置信息。
 */
@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    // 注入RedisTemplate用于操作Redis数据库
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderServiceLocationRepository orderServiceLocationRepository;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Override
    public BigDecimal calculateOrderRealDistance(Long orderId) {
        List<OrderServiceLocation> orderServiceLocationList = orderServiceLocationRepository.findByOrderIdOrderByCreateTimeAsc(orderId);
        double realDistance = 0;
        if(!CollectionUtils.isEmpty(orderServiceLocationList)) {
            for (int i = 0, size=orderServiceLocationList.size()-1; i < size; i++) {
                OrderServiceLocation location1 = orderServiceLocationList.get(i);
                OrderServiceLocation location2 = orderServiceLocationList.get(i+1);

                double distance = LocationUtil.getDistance(location1.getLatitude().doubleValue(), location1.getLongitude().doubleValue(), location2.getLatitude().doubleValue(), location2.getLongitude().doubleValue());
                realDistance += distance;
            }
        }
        //测试过程中，没有真正代驾，实际代驾GPS位置没有变化，模拟：实际代驾里程 = 预期里程 + 5
        //测试模拟结果：由于测试环境时没有办法移动，查询预付的距离、再随机加上一点偏差
        if (realDistance == 0) {
            int ran = (int) (Math.random() * 5 + 1);
            return orderInfoFeignClient.getOrderInfo(orderId).getData().getExpectDistance().add(new BigDecimal(ran));
        }
        return new BigDecimal(realDistance);
    }


    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        // 搜索经纬度位置5公里以内的司机
        //定义经纬度点
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(), searchNearByDriverForm.getLatitude().doubleValue());
        //定义距离：5公里(系统配置)
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS, RedisGeoCommands.DistanceUnit.KILOMETERS);
        //定义以point点为中心，distance为距离这么一个范围
        Circle circle = new Circle(point, distance);

        //定义GEO参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance() //包含距离
                .includeCoordinates() //包含坐标
                .sortAscending(); //排序：升序

        // 1.GEORADIUS获取附近范围内的信息
        GeoResults<RedisGeoCommands.GeoLocation<String>> result = this.redisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);

        //2.收集信息，存入list
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = result.getContent();

        //3.返回计算后的信息
        List<NearByDriverVo> list = new ArrayList();
        if(!CollectionUtils.isEmpty(content)) {
            Iterator<GeoResult<RedisGeoCommands.GeoLocation<String>>> iterator = content.iterator();
            while (iterator.hasNext()) {
                GeoResult<RedisGeoCommands.GeoLocation<String>> item = iterator.next();

                //司机id
                Long driverId = Long.parseLong(item.getContent().getName());
                //当前距离
                BigDecimal currentDistance = new BigDecimal(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);
                log.info("司机：{}，距离：{}",driverId, item.getDistance().getValue());

                //获取司机接单设置参数
                DriverSet driverSet = driverInfoFeignClient.getDriverSet(driverId).getData();
                //接单里程判断，acceptDistance==0：不限制，
                if(driverSet.getAcceptDistance().doubleValue() != 0 && driverSet.getAcceptDistance().subtract(currentDistance).doubleValue() < 0) {
                    continue;
                }
                //订单里程判断，orderDistance==0：不限制
                if(driverSet.getOrderDistance().doubleValue() != 0 && driverSet.getOrderDistance().subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
                    continue;
                }

                //满足条件的附近司机信息
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                list.add(nearByDriverVo);
            }
        }
        return list;
    }

    // 更新订单地址到缓存
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {

        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());

        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
        redisTemplate.opsForValue().set(key,orderLocationVo);
        return true;
    }


    // 获取缓存中的订单位置 获取订单经纬度位置
    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        String key = RedisConstant.UPDATE_ORDER_LOCATION + orderId;
        OrderLocationVo orderLocationVo = (OrderLocationVo)redisTemplate.opsForValue().get(key);
        return orderLocationVo;
    }

    //批量保存代驾服务订单位置
    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {

        List<OrderServiceLocation> list = new ArrayList<>();
        //OrderServiceLocation
        orderLocationServiceFormList.forEach(orderServiceLocationForm->{
            //orderServiceLocationForm -- OrderServiceLocation
            OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
            BeanUtils.copyProperties(orderServiceLocationForm,orderServiceLocation);
            orderServiceLocation.setId(ObjectId.get().toString());
            orderServiceLocation.setCreateTime(new Date());

            list.add(orderServiceLocation);
            //orderServiceLocationRepository.save(orderServiceLocation);
        });
        //批量添加到MongoDB
        orderServiceLocationRepository.saveAll(list);
        return true;
    }

    //获取订单最后一个位置
    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        //查询MongoDB
        //查询条件 ：orderId
        //根据创建时间降序排列
        //最新一条数据
        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Order.desc("createTime")));
        query.limit(1);

        OrderServiceLocation orderServiceLocation =
                mongoTemplate.findOne(query, OrderServiceLocation.class);
        OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
        BeanUtils.copyProperties(orderServiceLocation,orderServiceLastLocationVo);
        return orderServiceLastLocationVo;
    }




    /**
     * 更新司机的地理位置信息。
     *
     * @param updateDriverLocationForm 包含司机位置信息的表单，包括经度、纬度和司机ID。
     * @return 总是返回true，表示操作执行完毕。
     */
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        // 创建一个Point对象表示地理位置的经纬度
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(), updateDriverLocationForm.getLatitude().doubleValue());
        // 将司机的地理位置信息添加到Redis的GEO集合中
        redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point, updateDriverLocationForm.getDriverId().toString());
        return true;
    }

    /**
     * 移除司机的地理位置信息。
     *
     * @param driverId 司机的ID。
     * @return 总是返回true，表示操作执行完毕。
     */
    @Override
    public Boolean removeDriverLocation(Long driverId) {
        // 从Redis的GEO集合中移除指定司机的地理位置信息
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        return true;
    }
}
