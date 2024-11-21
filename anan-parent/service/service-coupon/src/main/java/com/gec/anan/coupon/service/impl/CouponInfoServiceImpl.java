package com.gec.anan.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.anan.common.constant.RedisConstant;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.coupon.mapper.CouponInfoMapper;
import com.gec.anan.coupon.mapper.CustomerCouponMapper;
import com.gec.anan.coupon.service.CouponInfoService;
import com.gec.anan.model.entity.coupon.CouponInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.anan.model.entity.coupon.CustomerCoupon;
import com.gec.anan.model.form.coupon.UseCouponForm;
import com.gec.anan.model.vo.base.PageVo;
import com.gec.anan.model.vo.coupon.AvailableCouponVo;
import com.gec.anan.model.vo.coupon.NoReceiveCouponVo;
import com.gec.anan.model.vo.coupon.NoUseCouponVo;
import com.gec.anan.model.vo.coupon.UsedCouponVo;
import com.gec.anan.model.vo.order.NewOrderDataVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {


    @Autowired
    CouponInfoMapper couponInfoMapper;


    @Autowired
    CustomerCouponMapper customerCouponMapper;

    @Autowired
    RedissonClient redissonClient;

    //使用优惠卷
    @Transactional(noRollbackFor = Exception.class)
    @Override
    public BigDecimal useCoupon(UseCouponForm useCouponForm) {
        //获取乘客优惠券
        CustomerCoupon customerCoupon = customerCouponMapper.selectById(useCouponForm.getCustomerCouponId());
        if(null == customerCoupon) {
            throw new AnanException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }
        //获取优惠券信息
        CouponInfo couponInfo = couponInfoMapper.selectById(customerCoupon.getCouponId());
        if(null == couponInfo) {
            throw new AnanException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }
        //判断该优惠券是否为乘客所有
        if(customerCoupon.getCustomerId().longValue() != useCouponForm.getCustomerId().longValue()) {
            throw new AnanException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        //获取优惠券减免金额
        BigDecimal reduceAmount = null;
        if(couponInfo.getCouponType().intValue() == 1) {
            //使用门槛判断
            //2.1.1.没门槛，订单金额必须大于优惠券减免金额
            if (couponInfo.getConditionAmount().doubleValue() == 0 && useCouponForm.getOrderAmount().subtract(couponInfo.getAmount()).doubleValue() > 0) {
                //减免金额
                reduceAmount = couponInfo.getAmount();
            }
            //2.1.2.有门槛，订单金额大于优惠券门槛金额
            if (couponInfo.getConditionAmount().doubleValue() > 0 && useCouponForm.getOrderAmount().subtract(couponInfo.getConditionAmount()).doubleValue() > 0) {
                //减免金额
                reduceAmount = couponInfo.getAmount();
            }
        } else {
            //使用门槛判断
            //订单折扣后金额
            BigDecimal discountOrderAmount = useCouponForm.getOrderAmount().multiply(couponInfo.getDiscount()).divide(new BigDecimal("10")).setScale(2, RoundingMode.HALF_UP);
            //订单优惠金额
            //2.2.1.没门槛
            if (couponInfo.getConditionAmount().doubleValue() == 0) {
                //减免金额
                reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
            }
            //2.2.2.有门槛，订单折扣后金额大于优惠券门槛金额
            if (couponInfo.getConditionAmount().doubleValue() > 0 && discountOrderAmount.subtract(couponInfo.getConditionAmount()).doubleValue() > 0) {
                //减免金额
                reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
            }
        }
        if(reduceAmount.doubleValue() > 0) {
            int row = couponInfoMapper.updateUseCount(couponInfo.getId());
            if(row == 1) {
                CustomerCoupon updateCustomerCoupon = new CustomerCoupon();
                updateCustomerCoupon.setId(customerCoupon.getId());
                updateCustomerCoupon.setUsedTime(new Date());
                updateCustomerCoupon.setOrderId(useCouponForm.getOrderId());
                customerCouponMapper.updateById(updateCustomerCoupon);
                return reduceAmount;
            }
        }
        throw new AnanException(ResultCodeEnum.DATA_ERROR);
    }

    //定义一个方法来构建AvailableCouponVo的对象
    private AvailableCouponVo buildBestNoUseCouponVo(NoUseCouponVo noUseCouponVo, BigDecimal reduceAmount) {
        AvailableCouponVo bestNoUseCouponVo = new AvailableCouponVo();
        BeanUtils.copyProperties(noUseCouponVo, bestNoUseCouponVo);
        bestNoUseCouponVo.setCouponId(noUseCouponVo.getId());
        bestNoUseCouponVo.setReduceAmount(reduceAmount);
        return bestNoUseCouponVo;
    }
    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount) {
        //1、可使用的优惠卷集合定义
        List<AvailableCouponVo> availableCouponVoList = new ArrayList<>();
        //2、获取未使用的优惠卷列表
        List<NoUseCouponVo>  list =   couponInfoMapper.findNoUseList(customerId);
        //3、现金
        List<NoUseCouponVo> typeList = list.stream().filter(item -> item.getCouponType().intValue() == 1).collect(Collectors.toList());
        //逻辑判断、计算优惠价格【存放在List中】
        for (NoUseCouponVo noUseCouponVo : typeList) {
            //获取优惠卷减免金额【现金券】
            BigDecimal reduceAmount = noUseCouponVo.getAmount();
            //1、没门槛 订单金额要大于优惠卷的金额
            if (noUseCouponVo.getConditionAmount().doubleValue()== 0 && orderAmount.subtract(reduceAmount).doubleValue()>=0){
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo,reduceAmount));
            }
            //2.1.2.有门槛，订单金额大于优惠券门槛金额
            if (noUseCouponVo.getConditionAmount().doubleValue() > 0 && orderAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }


        }

        //2.2.折扣券
        List<NoUseCouponVo> type2List = list.stream().filter(item -> item.getCouponType().intValue() == 2).collect(Collectors.toList());
        for (NoUseCouponVo noUseCouponVo : type2List) {
            //使用门槛判断
            //订单折扣后金额
            BigDecimal discountOrderAmount = orderAmount.multiply(noUseCouponVo.getDiscount()).divide(new BigDecimal("10")).setScale(2, RoundingMode.HALF_UP);
            //减免金额
            BigDecimal reduceAmount = orderAmount.subtract(discountOrderAmount);
            //订单优惠金额
            //2.2.1.没门槛
            if (noUseCouponVo.getConditionAmount().doubleValue() == 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
            //2.2.2.有门槛，订单折扣后金额大于优惠券门槛金额
            if (noUseCouponVo.getConditionAmount().doubleValue() > 0 && discountOrderAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
        }

        //排序
        if (!CollectionUtils.isEmpty(availableCouponVoList)) {
            Collections.sort(availableCouponVoList, new Comparator<AvailableCouponVo>() {
                @Override
                public int compare(AvailableCouponVo o1, AvailableCouponVo o2) {
                    return o1.getReduceAmount().compareTo(o2.getReduceAmount());
                }
            });
        }
        return availableCouponVoList;
    }


    // 优惠券
    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId) {
       IPage<NoReceiveCouponVo> iPageInfo =  couponInfoMapper.findNoReceivePage(pageParam,customerId);
       return new PageVo<>(iPageInfo.getRecords(),iPageInfo.getPages(),iPageInfo.getTotal());
    }

    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoUseCouponVo> pageInfo = couponInfoMapper.findNoUsePage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId) {
       IPage<UsedCouponVo> pageInfo = couponInfoMapper.findUsedPage(pageParam, customerId);
       return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean receive(Long customerId, Long couponId) {
        //1、查询优惠券
        CouponInfo couponInfo = this.getById(couponId);
        if(null == couponInfo) {
            throw new AnanException(ResultCodeEnum.DATA_ERROR);
        }

        //2、优惠券过期日期判断
        if (couponInfo.getExpireTime().before(new Date())) {
            throw new AnanException(ResultCodeEnum.COUPON_EXPIRE);
        }

        //3、校验库存，优惠券领取数量判断
        if (couponInfo.getPublishCount() !=0 && couponInfo.getReceiveCount() >= couponInfo.getPublishCount()) {
            throw new AnanException(ResultCodeEnum.COUPON_LESS);
        }

        RLock lock = null;
        try {
            // 初始化分布式锁
            //每人领取限制  与 优惠券发行总数 必须保证原子性，使用customerId减少锁的粒度，增加并发能力
            lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + customerId);
            boolean flag = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME, RedisConstant.COUPON_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (flag) {
                //4、校验每人限领数量
                if (couponInfo.getPerLimit() > 0) {
                    //4.1、统计当前用户对当前优惠券的已经领取的数量
                    long count = customerCouponMapper.selectCount(new LambdaQueryWrapper<CustomerCoupon>().eq(CustomerCoupon::getCouponId, couponId).eq(CustomerCoupon::getCustomerId, customerId));
                    //4.2、校验限领数量
                    if (count >= couponInfo.getPerLimit()) {
                        throw new AnanException(ResultCodeEnum.COUPON_USER_LIMIT);
                    }
                }

                //5、更新优惠券领取数量
                int row = 0;
                if (couponInfo.getPublishCount() == 0) {//没有限制
                    row = couponInfoMapper.updateReceiveCount(couponId);
                } else {
                    row = couponInfoMapper.updateReceiveCountByLimit(couponId);
                }
                if (row == 1) {
                    //6、保存领取记录
                    this.saveCustomerCoupon(customerId, couponId, couponInfo.getExpireTime());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != lock) {
                lock.unlock();
            }
        }
        throw new AnanException(ResultCodeEnum.COUPON_LESS);
    }


    private void saveCustomerCoupon(Long customerId, Long couponId, Date expireTime) {
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setCouponId(couponId);
        customerCoupon.setStatus(1);
        customerCoupon.setReceiveTime(new Date());
        customerCoupon.setExpireTime(expireTime);
        customerCouponMapper.insert(customerCoupon);
    }


}