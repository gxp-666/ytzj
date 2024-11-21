package com.gec.anan.customer.controller;

import com.gec.anan.common.annotation.Log;
import com.gec.anan.common.constant.RedisConstant;
import com.gec.anan.common.login.AnanLogin;
import com.gec.anan.common.result.Result;
import com.gec.anan.common.util.AuthContextHolder;
import com.gec.anan.customer.service.CustomerService;
import com.gec.anan.model.form.customer.UpdateWxPhoneForm;
import com.gec.anan.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {



    @Autowired
    private CustomerService customerInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return  Result.ok(customerInfoService.login(code));
    }



    @Operation(summary = "更新用户微信手机号")
    @AnanLogin
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
        updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
        // 注意注意! 个人版的吧下面注释掉
        //customerInfoService.updateWxPhoneNumber(updateWxPhoneForm);
        return Result.ok(true);
    }

    //AnanLogin 登录拦截注解 [表示需要登录才能进行访问]
    @Operation(summary = "获取用户信息的web接口")
    @GetMapping("/getCustomerLoginInfo")
    @AnanLogin
    public Result<CustomerLoginVo> getCustomerLoginInfo() {

        //从AuthContextHolder获取id
        Long customerId = AuthContextHolder.getUserId();

        return  Result.ok(customerInfoService.getCustomerLoginInfo(customerId));
    }



}

