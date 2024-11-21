package com.gec.anan.driver.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.gec.anan.common.constant.SystemConstant;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.common.util.LocationUtil;
import com.gec.anan.dispatch.client.NewOrderFeignClient;
import com.gec.anan.driver.service.OrderService;
import com.gec.anan.map.client.LocationFeignClient;
import com.gec.anan.map.client.MapFeignClient;
import com.gec.anan.model.entity.order.OrderInfo;
import com.gec.anan.model.enums.OrderStatus;
import com.gec.anan.model.form.map.CalculateDrivingLineForm;
import com.gec.anan.model.form.order.OrderFeeForm;
import com.gec.anan.model.form.order.StartDriveForm;
import com.gec.anan.model.form.order.UpdateOrderBillForm;
import com.gec.anan.model.form.order.UpdateOrderCartForm;
import com.gec.anan.model.form.rules.FeeRuleRequestForm;
import com.gec.anan.model.form.rules.ProfitsharingRuleRequestForm;
import com.gec.anan.model.form.rules.RewardRuleRequestForm;
import com.gec.anan.model.vo.map.DrivingLineVo;
import com.gec.anan.model.vo.map.OrderLocationVo;
import com.gec.anan.model.vo.map.OrderServiceLastLocationVo;
import com.gec.anan.model.vo.order.*;
import com.gec.anan.model.vo.rules.FeeRuleResponseVo;
import com.gec.anan.model.vo.rules.ProfitsharingRuleResponseVo;
import com.gec.anan.model.vo.rules.RewardRuleResponseVo;
import com.gec.anan.order.client.OrderInfoFeignClient;
import com.gec.anan.rules.client.FeeRuleFeignClient;
import com.gec.anan.rules.client.ProfitsharingRuleFeignClient;
import com.gec.anan.rules.client.RewardRuleFeignClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    NewOrderFeignClient newOrderFeignClient;

    @Autowired
    MapFeignClient mapFeignClient;

    @Autowired
    LocationFeignClient locationFeignClient;

    @Autowired
    FeeRuleFeignClient feeRuleFeignClient;

    @Autowired
    RewardRuleFeignClient rewardRuleFeignClient;

    @Autowired
    ProfitsharingRuleFeignClient profitsharingRuleFeignClient;


    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        return orderInfoFeignClient.sendOrderBillInfo(orderId, driverId).getData();
    }


    //获取订单账单详细信息
    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long driverId) {
        //订单信息
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        if(orderInfo.getDriverId().longValue() != driverId.longValue()) {
            throw new AnanException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //账单信息
        OrderBillVo orderBillVo = null;
        //分账信息
        OrderProfitsharingVo orderProfitsharing = null;
        if (orderInfo.getStatus().intValue() >= OrderStatus.END_SERVICE.getStatus().intValue()) {
            orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();

            //获取分账信息
            orderProfitsharing = orderInfoFeignClient.getOrderProfitsharing(orderId).getData();
        }

        //封装订单信息
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setOrderId(orderId);
        BeanUtils.copyProperties(orderInfo, orderInfoVo);
        orderInfoVo.setOrderBillVo(orderBillVo);
        orderInfoVo.setOrderProfitsharingVo(orderProfitsharing);
        return orderInfoVo;
    }

    //司机更新车辆信息
    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        return orderInfoFeignClient.updateOrderCart(updateOrderCartForm).getData();
    }


    //订单开始服务
    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        return orderInfoFeignClient.startDrive(startDriveForm).getData();
    }


    //结束代驾
    @Override
    @SneakyThrows
    public Boolean endDrive(OrderFeeForm orderFeeForm) {

        //1 根据orderId获取订单信息，判断当前订单是否司机接单
        CompletableFuture<OrderInfo> orderInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId()).getData();
            if (orderInfo.getDriverId() != orderFeeForm.getDriverId()) {
                throw new AnanException(ResultCodeEnum.ILLEGAL_REQUEST);
            }
            return orderInfo;
        });

        //防止刷单
        CompletableFuture<OrderServiceLastLocationVo> orderServiceLastLocationVoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            OrderServiceLastLocationVo orderServiceLastLocationVo = locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData();
            return orderServiceLastLocationVo;
        });

        //上面两个合并
        CompletableFuture.allOf(orderInfoCompletableFuture,
                orderServiceLastLocationVoCompletableFuture).join();

        //获取两个线程执行结果
        OrderInfo orderInfo = orderInfoCompletableFuture.get();

        OrderServiceLastLocationVo orderServiceLastLocationVo = orderServiceLastLocationVoCompletableFuture.get();

        //司机当前位置 距离 结束代驾位置
        double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(),
                orderInfo.getEndPointLongitude().doubleValue(),
                orderServiceLastLocationVo.getLatitude().doubleValue(),
                orderServiceLastLocationVo.getLongitude().doubleValue());
        if(distance > SystemConstant.DRIVER_END_LOCATION_DISTION) {
            throw new AnanException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
        }

        //2 计算订单实际里程
        CompletableFuture<BigDecimal> realDistanceCompletableFuture = CompletableFuture.supplyAsync(() -> {
            BigDecimal realDistance =
                    locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData();
            return realDistance;
        });

        //3 计算代驾实际费用
        CompletableFuture<FeeRuleResponseVo> feeRuleResponseVoCompletableFuture =
                realDistanceCompletableFuture.thenApplyAsync((realDistance) -> {
                    //远程调用，计算代驾费用
                    //封装FeeRuleRequestForm
                    FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
                    feeRuleRequestForm.setDistance(realDistance);
                    feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());

                    //计算司机到达代驾开始位置时间
                    //orderInfo.getArriveTime() - orderInfo.getAcceptTime()
                    // 分钟 = 毫秒 / 1000 * 60
                    Integer waitMinute =
                            Math.abs((int) ((orderInfo.getArriveTime().getTime() - orderInfo.getAcceptTime().getTime()) / (1000 * 60)));
                    feeRuleRequestForm.setWaitMinute(waitMinute);
                    //远程调用 代驾费用
                    FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();
                    //实际费用 = 代驾费用 + 其他费用（停车费）
                    BigDecimal totalAmount =
                            feeRuleResponseVo.getTotalAmount().add(orderFeeForm.getTollFee())
                                    .add(orderFeeForm.getParkingFee())
                                    .add(orderFeeForm.getOtherFee())
                                    .add(orderInfo.getFavourFee());
                    feeRuleResponseVo.setTotalAmount(totalAmount);
                    return feeRuleResponseVo;
                });

        //4 计算系统奖励
        CompletableFuture<Long> orderNumCompletableFuture = CompletableFuture.supplyAsync(() -> {
            String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
            String endTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
            Long orderNum = orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();
            return orderNum;
        });

        CompletableFuture<RewardRuleResponseVo> rewardRuleResponseVoCompletableFuture =
                orderNumCompletableFuture.thenApplyAsync((orderNum) -> {
                    //4.2.封装参数
                    RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
                    rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
                    rewardRuleRequestForm.setOrderNum(orderNum);
                    RewardRuleResponseVo rewardRuleResponseVo = rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();

                    return rewardRuleResponseVo;
                });


        //5 计算分账信息
        CompletableFuture<ProfitsharingRuleResponseVo> profitsharingRuleResponseVoCompletableFuture = feeRuleResponseVoCompletableFuture.thenCombineAsync(orderNumCompletableFuture,
                (feeRuleResponseVo, orderNum) -> {
                    ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
                    profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
                    profitsharingRuleRequestForm.setOrderNum(orderNum);

                    ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();
                    return profitsharingRuleResponseVo;
                });

        //合并
        CompletableFuture.allOf(
                orderInfoCompletableFuture,
                realDistanceCompletableFuture,
                feeRuleResponseVoCompletableFuture,
                orderNumCompletableFuture,
                rewardRuleResponseVoCompletableFuture,
                profitsharingRuleResponseVoCompletableFuture
        ).join();

        //获取执行结果
        BigDecimal realDistance = realDistanceCompletableFuture.get();
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoCompletableFuture.get();
        RewardRuleResponseVo rewardRuleResponseVo = rewardRuleResponseVoCompletableFuture.get();
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleResponseVoCompletableFuture.get();

        //6 封装实体类，结束代驾更新订单，添加账单和分账信息
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        //路桥费、停车费、其他费用
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        //乘客好处费
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());

        //实际里程
        updateOrderBillForm.setRealDistance(realDistance);
        //订单奖励信息
        BeanUtils.copyProperties(rewardRuleResponseVo, updateOrderBillForm);
        //代驾费用信息
        BeanUtils.copyProperties(feeRuleResponseVo, updateOrderBillForm);
        //分账相关信息
        BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());
        orderInfoFeignClient.endDrive(updateOrderBillForm);

        return true;
    }

    //计算最佳驾驶路线
    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    }


    //司机到达驾驶起点位置
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        //防止刷单，计算司机的经纬度与代驾的起始经纬度是否在1公里范围内
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        OrderLocationVo orderLocationVo = locationFeignClient.getCacheOrderLocation(orderId).getData();
        //司机的位置与代驾起始点位置的距离
        double distance = LocationUtil.getDistance(
                orderInfo.getStartPointLatitude().doubleValue(),
                orderInfo.getStartPointLongitude().doubleValue(),
                orderLocationVo.getLatitude().doubleValue(),
                orderLocationVo.getLongitude().doubleValue());
        if (distance > SystemConstant.DRIVER_START_LOCATION_DISTION) {
            throw new AnanException(ResultCodeEnum.DRIVER_START_LOCATION_DISTION_ERROR);
        }
        return orderInfoFeignClient.driverArriveStartLocation(orderId, driverId).getData();
    }

    //获取乘客订单状态
    @Override
    public Integer getOrderStatus(Long orderId) {
        return orderInfoFeignClient.getOrderStatus(orderId).getData();
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        return orderInfoFeignClient.searchDriverCurrentOrder(driverId).getData();
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        return newOrderFeignClient.findNewOrderQueueData(driverId).getData();
    }

    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        return orderInfoFeignClient.robNewOrder(driverId, orderId).getData();
    }
}
