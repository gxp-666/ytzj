package com.gec.anan.order.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.anan.model.entity.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.anan.model.vo.order.OrderListVo;
import com.gec.anan.model.vo.order.OrderPayVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

        IPage<OrderListVo> selectCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId);

        IPage<OrderListVo> selectDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);

        OrderPayVo selectOrderPayVo(String orderNo, Long customerId);

}
