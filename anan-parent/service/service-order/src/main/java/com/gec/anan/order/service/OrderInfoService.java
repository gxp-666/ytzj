package com.gec.anan.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.anan.model.entity.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.anan.model.form.order.OrderInfoForm;
import com.gec.anan.model.form.order.StartDriveForm;
import com.gec.anan.model.form.order.UpdateOrderBillForm;
import com.gec.anan.model.form.order.UpdateOrderCartForm;
import com.gec.anan.model.vo.base.PageVo;
import com.gec.anan.model.vo.order.*;

import java.math.BigDecimal;

public interface OrderInfoService extends IService<OrderInfo> {

    Long saveOrderInfo(OrderInfoForm orderInfoForm);

    Integer getOrderStatus(Long orderId);

    Boolean robNewOrder(Long driverId, Long orderId);

    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);

    Boolean startDrive(StartDriveForm startDriveForm);

    Long getOrderNumByTime(String startTime, String endTime);

    Boolean endDrive(UpdateOrderBillForm updateOrderBillForm);

    PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId);

    PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);

    OrderBillVo getOrderBillInfo(Long orderId);


    OrderProfitsharingVo getOrderProfitsharing(Long orderId);

    Boolean sendOrderBillInfo(Long orderId, Long driverId);

    OrderPayVo getOrderPayVo(String orderNo, Long customerId);

    Boolean updateOrderPayStatus(String orderNo);

    OrderRewardVo getOrderRewardFee(String orderNo);


    void updateProfitsharingStatus(String orderNo);

    void orderCancel(long l);

    //更新订单优惠卷金额
    Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount);
}

