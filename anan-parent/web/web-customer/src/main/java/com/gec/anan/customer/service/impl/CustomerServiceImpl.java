package com.gec.anan.customer.service.impl;

import com.gec.anan.common.constant.RedisConstant;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.Result;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.customer.client.CustomerInfoFeignClient;
import com.gec.anan.customer.service.CustomerService;
import com.gec.anan.model.form.customer.UpdateWxPhoneForm;
import com.gec.anan.model.vo.customer.CustomerLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {


    @Autowired
    private CustomerInfoFeignClient customerInfoFeignClient;


    @Autowired
    RedisTemplate redisTemplate;

    //调用Service的方法得到userid 然后生成token 返回前端
    @Override
    public String login(String code) {
        System.out.println("124444444444444444444444444444444444444444444");

        //1,是feign 远程调用Service,得到result
        Result<Long> result = customerInfoFeignClient.login(code);

        //2.result的状态码
        if (result.getCode().intValue() != 200) {
            throw new AnanException(result.getCode(), result.getMessage());
        }

        //3.获取userid 判断id是否为空
        Long customerId = result.getData();
        if (customerId == null) {
            throw new AnanException(ResultCodeEnum.DATA_ERROR);
        }
        //4.生成token
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        //5.把token 存放到redis中 user:login
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token, customerId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

        //6.响应token回到前端
        return token;
    }


    @Override
    public CustomerLoginVo getCustomerLoginInfo(long customerId) {
        //远程调用
        Result<CustomerLoginVo> result = customerInfoFeignClient.LoginInfo(customerId);
        //2.result的状态码
        if (result.getCode().intValue() != 200) {
            throw new AnanException(result.getCode(), result.getMessage());
        }

        //3.获取vo
        CustomerLoginVo loginVo = result.getData();
        if (loginVo == null) {
            throw new AnanException(ResultCodeEnum.DATA_ERROR);
        }
        //返回对象数据
        return loginVo;
    }


    /**
     * 更新登录用户手机号
     * @param updateWxPhoneForm
     * @return
     */
    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {

        return customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneForm).getData();
    }
}
