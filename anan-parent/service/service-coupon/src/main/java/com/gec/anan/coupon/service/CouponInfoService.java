package com.gec.anan.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.anan.model.entity.coupon.CouponInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.anan.model.form.coupon.UseCouponForm;
import com.gec.anan.model.vo.base.PageVo;
import com.gec.anan.model.vo.coupon.AvailableCouponVo;
import com.gec.anan.model.vo.coupon.NoReceiveCouponVo;
import com.gec.anan.model.vo.coupon.NoUseCouponVo;
import com.gec.anan.model.vo.coupon.UsedCouponVo;

import java.math.BigDecimal;
import java.util.List;

public interface CouponInfoService extends IService<CouponInfo> {



    // 优惠券领取记录分页列表
    PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId);



    //未使用优惠卷分页列表
    PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId);

    PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId);

    Boolean receive(Long customerId, Long couponId);


    //获取未使用的最佳优惠券信息
    List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount);


    //使用优惠卷
    BigDecimal useCoupon(UseCouponForm useCouponForm);


}
