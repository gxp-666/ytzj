package com.gec.anan.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.anan.customer.mapper.CustomerInfoMapper;
import com.gec.anan.customer.mapper.CustomerLoginLogMapper;
import com.gec.anan.customer.service.CustomerInfoService;
import com.gec.anan.model.entity.customer.CustomerInfo;
import com.gec.anan.model.entity.customer.CustomerLoginLog;
import com.gec.anan.model.form.customer.UpdateWxPhoneForm;
import com.gec.anan.model.vo.customer.CustomerInfoVo;
import com.gec.anan.model.vo.customer.CustomerLoginVo;
import io.netty.util.internal.StringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    //获取腾讯接口
    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private CustomerLoginLogMapper customerLoginLogMapper;


    @Override
    public String getCustomerOpenId(Long customerId) {
        CustomerInfo customerInfo = this.getOne(new LambdaQueryWrapper<CustomerInfo>()
                .eq(CustomerInfo::getId, customerId).select(CustomerInfo::getWxOpenId));
        return customerInfo.getWxOpenId();
    }

    //获取用户登录信息
    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
        CustomerInfo customerInfo = this.getById(customerId);
        //拷贝数据到vo对象中[ 登录的vo对象数据]
        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        BeanUtils.copyProperties(customerInfo, customerLoginVo);
        //提供给前端来判断要加载手机号码
        boolean isBingPhone = StringUtils.hasText(customerInfo.getPhone());
        customerLoginVo.setIsBindPhone(isBingPhone);
        //返回对象
        return customerLoginVo;
    }


    /**
     * 更新用户手机号
     * @Transactional:  声明注解  出现异常就进行事务回滚
     * @SneakyThrows  忽略异常
     * @param updateWxPhoneForm
     * @return
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    @SneakyThrows // 忽略异常
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        //1.调用微信提供的api来获取用户的手机号
        WxMaPhoneNumberInfo phoneNoInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getCode());
        String phone = phoneNoInfo.getPhoneNumber();
        log.info("获取到当前微信用户的手机号码:{}",phone);
        //2.跟新手机号
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setId(updateWxPhoneForm.getCustomerId());
        customerInfo.setPhone(phone);

        return this.updateById(customerInfo) ;
    }


    //乘客登录
    @Override
    public Long login(String code) {
        //1.获取openid
        WxMaJscode2SessionResult sessionInfo = null;
        try {
            System.err.println("wxMaService="+wxMaService);
            System.err.println("wxMaService.getUserService()="+wxMaService.getUserService());
            sessionInfo = wxMaService.getUserService().getSessionInfo(code);

        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
        String openid = sessionInfo.getOpenid();
        log.info("获取到user的openid : {}", openid);

        //2.openid判断用户数据是否存在 [非空判断省略]数据库查询判断
        CustomerInfo customerInfo = this.getOne(new LambdaQueryWrapper<CustomerInfo>().eq(CustomerInfo::getWxOpenId, openid));

        //2-1不存在 [第一次登录 ,添加数据]
        if (customerInfo == null) {
            customerInfo = new CustomerInfo();
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));//时间戳
            customerInfo.setAvatarUrl("https://img1.baidu.com/it/u=1293434993,500512152&fm=253&fmt=auto&app=138&f=JPEG?w=400&h=400");
            customerInfo.setWxOpenId(openid);
            this.save(customerInfo);//注册用户数据
        }
        //2-2添加用户登录日志
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录成功");
        customerLoginLogMapper.insert(customerLoginLog);
        //2-3返回用户id
        return customerInfo.getId();
    }




}
