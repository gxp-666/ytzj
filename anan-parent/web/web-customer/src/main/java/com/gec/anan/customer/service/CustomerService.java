package com.gec.anan.customer.service;

import com.gec.anan.model.form.customer.UpdateWxPhoneForm;
import com.gec.anan.model.vo.customer.CustomerLoginVo;

public interface CustomerService {


    //返回token
    String login(String code);

    CustomerLoginVo getCustomerLoginInfo(long customerId);

    Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);
}
