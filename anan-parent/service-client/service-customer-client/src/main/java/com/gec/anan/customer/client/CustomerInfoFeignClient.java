package com.gec.anan.customer.client;

import com.gec.anan.common.result.Result;
import com.gec.anan.model.form.customer.UpdateWxPhoneForm;
import com.gec.anan.model.vo.customer.CustomerLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-customer")
public interface CustomerInfoFeignClient {


    /**
     * 获取客户OpenId
     * @param customerId
     * @return
     */
    @GetMapping("/customer/info/getCustomerOpenId/{customerId}")
    Result<String> getCustomerOpenId(@PathVariable("customerId") Long customerId);

    //定义login服务调度的接口
    @GetMapping("/customer/info/login/{code}")
    public Result<Long> login(@PathVariable String code);

    //定义获取用户登录信息的接口
    @GetMapping("/customer/info/getCustomerLoginInfo/{customerId}")
     public Result<CustomerLoginVo> LoginInfo(@PathVariable Long customerId);

    //定义更新用户微信手机号的接口
    @PostMapping("/customer/info/updateWxPhoneNumber")
    public Result<Boolean>  updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm);

}