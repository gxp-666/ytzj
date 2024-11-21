package com.gec.anan.driver.service;

import com.gec.anan.model.form.map.CalculateDrivingLineForm;
import com.gec.anan.model.form.order.OrderFeeForm;
import com.gec.anan.model.form.order.StartDriveForm;
import com.gec.anan.model.form.order.UpdateOrderCartForm;
import com.gec.anan.model.vo.map.DrivingLineVo;
import com.gec.anan.model.vo.order.CurrentOrderInfoVo;
import com.gec.anan.model.vo.order.NewOrderDataVo;
import com.gec.anan.model.vo.order.OrderInfoVo;

import java.util.List;

public interface OrderService {


    Integer getOrderStatus(Long orderId);

    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    Boolean robNewOrder(Long driverId, Long orderId);

    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    OrderInfoVo getOrderInfo(Long orderId, Long driverId);

    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);

    Boolean startDrive(StartDriveForm startDriveForm);

    Boolean endDrive(OrderFeeForm orderFeeForm);

    Boolean sendOrderBillInfo(Long orderId, Long driverId);

}
