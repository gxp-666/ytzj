package com.gec.anan.dispatch.service;

import com.gec.anan.model.vo.dispatch.NewOrderTaskVo;
import com.gec.anan.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface NewOrderService {

    //添加并启动任务
    Long addAndStartTask(NewOrderTaskVo newOrderTaskVo);

    //执行任务
    Boolean executeTask(Long jobId);

    //查询司机新订单数据
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);


    //清空新订单队列数据
    Boolean clearNewOrderQueueData(Long driverId);
}
