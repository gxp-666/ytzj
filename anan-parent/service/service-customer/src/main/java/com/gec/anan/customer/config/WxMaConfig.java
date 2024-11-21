package com.gec.anan.customer.config;


import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 创建微信工具包对象 , 返回WxMaService
 * 配置WxMaService ,通过WxMaService 可以获取openId
 * */
@Component
public class WxMaConfig {


    @Autowired
    private WxMaProperties wxMaProperties;

    //注入 WxMaService 对象
    @Bean
    public WxMaService wxMaService(){
        //配置对象 注入appid 和秘钥
        WxMaDefaultConfigImpl wxMaDefaultConfig = new WxMaDefaultConfigImpl();
        wxMaDefaultConfig.setAppid(wxMaProperties.getAppId());
        wxMaDefaultConfig.setSecret(wxMaProperties.getSecret());


        //创建WXMaService 对象
        WxMaService wxMaService = new WxMaServiceImpl();
        wxMaService.setWxMaConfig(wxMaDefaultConfig);
        //返回对象
        return  wxMaService;
    }
}
