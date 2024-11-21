package com.gec.anan.customer.controller;

import com.gec.anan.common.login.AnanLogin;
import com.gec.anan.common.result.Result;
import com.gec.anan.common.util.AuthContextHolder;
import com.gec.anan.customer.service.CustomerInfoService;
import com.gec.anan.model.entity.customer.CustomerInfo;
import com.gec.anan.model.form.customer.UpdateWxPhoneForm;
import com.gec.anan.model.vo.customer.CustomerInfoVo;
import com.gec.anan.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/customer/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoController {

	@Autowired
	private CustomerInfoService customerInfoService;


	@Operation(summary = "获取客户OpenId")
	@GetMapping("/getCustomerOpenId/{customerId}")
	public Result<String> getCustomerOpenId(@PathVariable Long customerId) {
		return Result.ok(customerInfoService.getCustomerOpenId(customerId));
	}


	//定义一个获取用户的登录信息的接口
	@Operation(summary = "获取用户的登录信息")
	@GetMapping("/getCustomerLoginInfo/{customerId}")
	public Result<CustomerLoginVo> LoginInfo(@PathVariable Long customerId){
		return Result.ok(customerInfoService.getCustomerLoginInfo(customerId));
	}


	/**
	 * 定义一个乘客登录的接口
	 * @param code
	 * @return
	 */
	@Operation(summary = "小程序授权登录")
	@GetMapping("/login/{code}")
	public Result<Long> login(@PathVariable String code){
		return Result.ok(customerInfoService.login(code));
	}


 	/**
	 * 定义一个跟新用户手机号的接口
	 * @param updateWxPhoneForm
	 * @return
	 */
	@Operation(summary = "更新用户微信手机号")
	@PostMapping("/updateWxPhoneNumber")
	public Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
		updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
		customerInfoService.updateWxPhoneNumber(updateWxPhoneForm);
		return Result.ok(true);
	}


	@Operation(summary = "获取客户基本信息")
	@GetMapping("/getCustomerInfo/{customerId}")

	public Result<CustomerInfo> getCustomerInfo(@PathVariable Long customerId) {
		return Result.ok(customerInfoService.getById(customerId));
	}
}

