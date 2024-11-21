package com.gec.anan.payment.service;

import com.gec.anan.model.form.payment.PaymentInfoForm;
import com.gec.anan.model.vo.payment.WxPrepayVo;
import jakarta.servlet.http.HttpServletRequest;

public interface WxPayService {


    WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm);

    void wxnotify(HttpServletRequest request);

    Boolean queryPayStatus(String orderNo);


    //支付成功后的方法
    void handleOrder(String orderNo);

}
