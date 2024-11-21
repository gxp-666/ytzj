package com.gec.anan.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.anan.common.constant.RedisConstant;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.model.entity.order.*;
import com.gec.anan.model.enums.OrderStatus;
import com.gec.anan.model.form.order.OrderInfoForm;
import com.gec.anan.model.form.order.StartDriveForm;
import com.gec.anan.model.form.order.UpdateOrderBillForm;
import com.gec.anan.model.form.order.UpdateOrderCartForm;
import com.gec.anan.model.vo.base.PageVo;
import com.gec.anan.model.vo.order.*;
import com.gec.anan.order.mapper.*;
import com.gec.anan.order.service.OrderInfoService;
import com.gec.anan.order.service.OrderMonitorService;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Autowired
    OrderMonitorService orderMonitorService;

    @Autowired
    private OrderBillMapper orderBillMapper;

    @Autowired
    private OrderProfitsharingMapper orderProfitsharingMapper;

    @Autowired
    OrderMonitorMapper orderMonitorMapper;

    //更新订单优惠卷金额
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        int row = orderBillMapper.updateCouponAmount(orderId, couponAmount);
        if(row != 1) {
            throw new AnanException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateProfitsharingStatus(String orderNo) {
        //查询订单
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderNo, orderNo)
                .select(OrderInfo::getId));

        //更新状态条件
        LambdaQueryWrapper<OrderProfitsharing> updateQueryWrapper = new LambdaQueryWrapper<>();
        updateQueryWrapper.eq(OrderProfitsharing::getOrderId, orderInfo.getId());
        //更新字段
        OrderProfitsharing updateOrderProfitsharing = new OrderProfitsharing();
        updateOrderProfitsharing.setStatus(2);
        orderProfitsharingMapper.update(updateOrderProfitsharing, updateQueryWrapper);
    }



    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        //查询订单
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo)
                .select(OrderInfo::getId,OrderInfo::getDriverId));
        //账单
        OrderBill orderBill = orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>()
                .eq(OrderBill::getOrderId, orderInfo.getId()).select(OrderBill::getRewardFee));
        OrderRewardVo orderRewardVo = new OrderRewardVo();
        orderRewardVo.setOrderId(orderInfo.getId());
        orderRewardVo.setDriverId(orderInfo.getDriverId());
        orderRewardVo.setRewardFee(orderBill.getRewardFee());
        return orderRewardVo;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateOrderPayStatus(String orderNo) {
        //查询订单，判断订单状态，如果已更新支付状态，直接返回
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        queryWrapper.select(OrderInfo::getId, OrderInfo::getDriverId, OrderInfo::getStatus);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if(null == orderInfo || orderInfo.getStatus().intValue() == OrderStatus.PAID.getStatus().intValue()) return true;

        //更新订单状态
        LambdaQueryWrapper<OrderInfo> updateQueryWrapper = new LambdaQueryWrapper<>();
        updateQueryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.PAID.getStatus());
        updateOrderInfo.setPayTime(new Date());
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            //记录日志
            this.log(orderInfo.getId(), OrderStatus.PAID.getStatus());
        } else {
            log.error("订单支付回调更新订单状态失败，订单号为：" + orderNo);
            throw new AnanException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }


    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo, customerId);
        if(null != orderPayVo) {
            String content = orderPayVo.getStartLocation() + " 到 " + orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }




    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        //更新订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
        //只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            //记录日志
            this.log(orderId, OrderStatus.UNPAID.getStatus());
        } else {
            throw new AnanException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }


    //获取乘客订单分页列表
    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo =  orderInfoMapper.selectCustomerOrderPage(pageParam,customerId);
        return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
    }

    //获取司机订单分页列表
    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo =  orderInfoMapper.selectDriverOrderPage(pageParam,driverId);
        return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
    }

    //根据id获取账单信息
    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        //OrderBill
        OrderBill orderBill = orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>()
                .eq(OrderBill::getOrderId, orderId));
        //返回vo
        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill,orderBillVo);

        return orderBillVo;
    }

    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {

        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(new LambdaQueryWrapper<OrderProfitsharing>().eq(OrderProfitsharing::getOrderId, orderId));
        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
        BeanUtils.copyProperties(orderProfitsharing, orderProfitsharingVo);
        return orderProfitsharingVo;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        //更新订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, updateOrderBillForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, updateOrderBillForm.getDriverId());
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        updateOrderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        updateOrderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        updateOrderInfo.setEndServiceTime(new Date());
        updateOrderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        //只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            //记录日志
            this.log(updateOrderBillForm.getOrderId(), OrderStatus.END_SERVICE.getStatus());

            //插入实际账单数据
            OrderBill orderBill = new OrderBill();
            BeanUtils.copyProperties(updateOrderBillForm, orderBill);
            orderBill.setOrderId(updateOrderBillForm.getOrderId());
            orderBill.setPayAmount(orderBill.getTotalAmount());
            orderBillMapper.insert(orderBill);

            //插入分账信息数据
            OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
            BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
            orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
            orderProfitsharing.setRuleId(updateOrderBillForm.getProfitsharingRuleId());
            orderProfitsharing.setStatus(1);
            orderProfitsharingMapper.insert(orderProfitsharing);
        } else {
            throw new AnanException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }



    //根据时间段获取订单数
    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(OrderInfo::getStartServiceTime, startTime);
        queryWrapper.lt(OrderInfo::getStartServiceTime, endTime);
        Long count = orderInfoMapper.selectCount(queryWrapper);
        return count;
    }

    //司机端查找当前订单
    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        //封装条件
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getDriverId, driverId);
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus()
        };
        wrapper.in(OrderInfo::getStatus, statusArray);
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到vo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }


    // 司机到达目的地
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        // 更新订单状态和到达时间，条件：orderId + driverId
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.eq(OrderInfo::getDriverId, driverId);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        orderInfo.setArriveTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);

        if (rows == 1) {
            return true;
        } else {
            throw new AnanException(ResultCodeEnum.UPDATE_ERROR);
        }
    }


    //司机更新驾驶车辆信息
    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, updateOrderCartForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm, orderInfo);
        orderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows == 1) {
            return true;
        } else {
            throw new AnanException(ResultCodeEnum.UPDATE_ERROR);
        }
    }


    //开始代驾服务
    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        //根据订单id  +  司机id  更新订单状态  和 开始代驾时间
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        //更新订单状态
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        orderInfo.setStartServiceTime(new Date());

        //更新订单
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows == 1) {
            //记录日志
            this.log(startDriveForm.getOrderId(), OrderStatus.START_SERVICE.getStatus());
        } else {
            throw new AnanException(ResultCodeEnum.UPDATE_ERROR);
        }
        //先把订单监控统计的数据表进行初始化 [先提供一条对应订单对的监控的数据(数据都是为空的)]
        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorMapper.insert(orderMonitor);
        return true;
    }


    /**
     * 查找乘客端当前订单
     */
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        //设置查询条件 ,查询符合状态的订单
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getCustomerId, customerId);
        //各种状态
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus(),
                OrderStatus.UNPAID.getStatus()
        };
        //发起查询
        wrapper.in(OrderInfo::getStatus, statusArray);
        //获取最新一条记录
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");

        //调用方法
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);

        //封装到CurrentOrderInfoVo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }


    //根据订单id获取订单状态
    @Override
    public Integer getOrderStatus(Long orderId) {
        //sql语句： select status from order_info where id=?
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.select(OrderInfo::getStatus);
        //调用mapper方法
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //订单不存在
        if (orderInfo == null) {
            return OrderStatus.NULL_ORDER.getStatus();
        }

        return orderInfo.getStatus();
    }

    /**
     * 司机抢单操作
     *
     * @param driverId
     * @param orderId
     * @return
     */
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        //抢单成功或取消订单，都会删除该key，redis判断，减少数据库压力
        if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败
            throw new AnanException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        // 初始化分布式锁，创建一个RLock实例
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
        try {
            /**
             * TryLock是一种非阻塞式的分布式锁，实现原理：Redis的SETNX命令
             * 参数：
             *     waitTime：等待获取锁的时间
             *     leaseTime：加锁的时间
             */
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME, RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            //获取到锁
            if (flag) {
                //二次判断，防止重复抢单
                if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
                    //抢单失败
                    throw new AnanException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                //修改订单状态
                //update order_info set status = 2, driver_id = #{driverId} where id = #{id}
                //修改字段
                OrderInfo orderInfo = new OrderInfo();
                orderInfo.setId(orderId);
                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
                orderInfo.setAcceptTime(new Date());
                orderInfo.setDriverId(driverId);
                int rows = orderInfoMapper.updateById(orderInfo);
                if (rows != 1) {
                    //抢单失败
                    throw new AnanException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                //记录日志
                this.log(orderId, orderInfo.getStatus());
                //删除redis订单标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        } catch (InterruptedException e) {
            //抢单失败
            throw new AnanException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        return true;
    }


    /**
     * 保存订单信息
     *
     * @param orderInfoForm 订单信息表单，包含用户提交的订单详情
     * @return 插入后的订单ID
     */
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        // 创建一个新的订单信息对象
        OrderInfo orderInfo = new OrderInfo();
        // 将表单中的属性复制到订单信息对象中
        BeanUtils.copyProperties(orderInfoForm, orderInfo);
        // 生成唯一的订单号
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        // 设置订单的初始状态为等待接单
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        // 设置订单号
        orderInfo.setOrderNo(orderNo);
        // 插入订单信息到数据库
        orderInfoMapper.insert(orderInfo);

        //发送延迟消息、实现15分钟超时后的取消
        this.sendDelayMessage(orderInfo.getId());

        // 记录日志
        log(orderInfo.getId(), orderInfo.getStatus());

        // 设置接单标识为0，表示订单不在等待接单状态
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK, "0",
                RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);
        // 返回订单ID
        return orderInfo.getId();
    }

    //发送延迟消息的处理方法
    private void sendDelayMessage(Long id) {
        try {
            //创建一个阻塞队列
            RBlockingDeque<Object> queueCancel = redissonClient.getBlockingDeque("queue_cancel");
            //将阻塞队列放到延迟队列中
            RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(queueCancel);
            //发送信息到延迟队列中
            delayedQueue.offer(id.toString(), 15, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void orderCancel(long orderId) {
        //1、获取订单信息
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //2、状态校验判断
        if (orderInfo.getStatus() == OrderStatus.WAITING_ACCEPT.getStatus()) {
            //3、修改订单状态
            OrderInfo entity = new OrderInfo();
            entity.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
            entity.setId(orderInfo.getId());
            int update = orderInfoMapper.updateById(entity);
            //4、删除redis中存放的key
            if (update > 0) {
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + orderInfo.getId());
            }
        }
    }

    /**
     * 记录订单状态日志
     *
     * @param orderId 订单ID
     * @param status  订单状态
     */
    private void log(Long orderId, Integer status) {
        // 创建一个新的订单状态日志对象
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        // 设置订单ID
        orderStatusLog.setOrderId(orderId);
        // 设置订单状态
        orderStatusLog.setOrderStatus(status);
        // 设置操作时间
        orderStatusLog.setOperateTime(new Date());
        // 插入订单状态日志到数据库
        orderStatusLogMapper.insert(orderStatusLog);
    }
}
