package com.gec.anan.driver.controller;

import com.gec.anan.common.login.AnanLogin;
import com.gec.anan.common.result.Result;
import com.gec.anan.common.util.AuthContextHolder;
import com.gec.anan.driver.service.OrderService;
import com.gec.anan.model.form.map.CalculateDrivingLineForm;
import com.gec.anan.model.form.order.OrderFeeForm;
import com.gec.anan.model.form.order.StartDriveForm;
import com.gec.anan.model.form.order.UpdateOrderCartForm;
import com.gec.anan.model.vo.map.DrivingLineVo;
import com.gec.anan.model.vo.order.CurrentOrderInfoVo;
import com.gec.anan.model.vo.order.NewOrderDataVo;
import com.gec.anan.model.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

    @Autowired
    OrderService orderService;


    @Operation(summary = "司机发送账单信息")
    @AnanLogin
    @GetMapping("/sendOrderBillInfo/{orderId}")
    public Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.sendOrderBillInfo(orderId, driverId));
    }

    @Operation(summary = "结束代驾服务更新订单账单")
    @AnanLogin
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody OrderFeeForm orderFeeForm) {
        Long driverId = AuthContextHolder.getUserId();
        orderFeeForm.setDriverId(driverId);
        return Result.ok(orderService.endDrive(orderFeeForm));
    }

    @Operation(summary = "司机到达代驾起始地点")
    @AnanLogin
    @GetMapping("/driverArriveStartLocation/{orderId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.driverArriveStartLocation(orderId, driverId));
    }

    @Operation(summary = "更新代驾车辆信息")
    @AnanLogin
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm) {
        Long driverId = AuthContextHolder.getUserId();
        updateOrderCartForm.setDriverId(driverId);
        Result<Boolean> ok = Result.ok(orderService.updateOrderCart(updateOrderCartForm));
        log.error("web信息如下:{}-----------------------------",ok);
        return ok;
    }

    @Operation(summary = "司机端查找当前订单")
    @AnanLogin
    @GetMapping("/searchDriverCurrentOrder")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.searchDriverCurrentOrder(driverId));
    }

    @Operation(summary = "查询订单状态")
    @AnanLogin
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }




    @Operation(summary = "查询司机新订单数据")
    @AnanLogin
    @GetMapping("/findNewOrderQueueData")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.findNewOrderQueueData(driverId));
    }


    @Operation(summary = "司机抢单")
    @AnanLogin
    @GetMapping("/robNewOrder/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.robNewOrder(driverId, orderId));
    }

    @Operation(summary = "获取订单账单详细信息")
    @AnanLogin
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getOrderInfo(orderId, driverId));
    }


    @Operation(summary = "计算最佳驾驶线路")
    @AnanLogin
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }


    @Operation(summary = "开始代驾服务")
    @AnanLogin
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm) {
        Long driverId = AuthContextHolder.getUserId();
        startDriveForm.setDriverId(driverId);
        return Result.ok(orderService.startDrive(startDriveForm));
    }

}

