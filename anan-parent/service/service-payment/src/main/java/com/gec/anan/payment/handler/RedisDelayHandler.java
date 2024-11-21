package com.gec.anan.payment.handler;

import com.gec.anan.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class RedisDelayHandler {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void listener() {
        new Thread(()->{
            while (true){
                // 获取到阻塞队列
                RBlockingDeque<String> blockingDeque = redissonClient
                        .getBlockingDeque("queue_cancel");
                try {
                    // 从阻塞队列中获取到订单Id
                    String orderId = blockingDeque.take();
                    // 订单Id 不为空的时候，调用取消订单方法
                    if(!StringUtils.isEmpty(orderId)) {
                        log.info("接收延时队列成功，订单id：{}", orderId);
                        orderInfoService.orderCancel(Long.parseLong(orderId));
                    }
                } catch (InterruptedException e) {
                    log.error("接收延时队列失败");
                    e.printStackTrace();
                }
            }
        }).start();
    }
}