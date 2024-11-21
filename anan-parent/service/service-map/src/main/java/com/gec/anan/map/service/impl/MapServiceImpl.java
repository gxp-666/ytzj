package com.gec.anan.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.map.service.MapService;
import com.gec.anan.model.form.map.CalculateDrivingLineForm;
import com.gec.anan.model.vo.map.DrivingLineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${tencent.cloud.map.key}")
    private String key;

    //计算驾驶线路
    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm form) {
        //1、client【url】【提交数据（from、to、key）】
        // 参考：https://apis.map.qq.com/ws/direction/v1/driving/?from=39.984042,116.307535&to=39.976249,116.316569&key=[你的key]
        //请求腾讯提供接口，按照接口要求传递相关参数，返回需要结果
        //使用HttpClient，目前Spring封装调用工具使用RestTemplate
        //定义调用腾讯地址
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";
        //封装传递参数
        //2、起点和终点的坐标【创建map存放多个键值对】
        Map<String, String> map = new HashMap();
        map.put("from", form.getStartPointLatitude() + "," + form.getStartPointLongitude());
        map.put("to", form.getEndPointLatitude() + "," + form.getEndPointLongitude());
        map.put("key", key);
        //3、通过http工具访问map的http接口【RestTemplate】
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);
        //4、得到响应结果、解析结果、返回DrivingLineVo的对象数据
        //创建vo
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        //4-1：判断响应状态
        int status = result.getIntValue("status");
        if(status!=0){
            System.out.println("form = " + form);
            System.out.println("result = " + result);
            System.out.println("key = "+ key);
            throw new AnanException(ResultCodeEnum.MAP_FAIL);
        }
        //路线方案的第一个方案
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        //4-2：路线信息
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));
        //4-3：距离信息 distance    number 是  方案总距离，单位：米  6.856  6.86
        drivingLineVo.setDistance(
                route.getBigDecimal("distance")
                        .divide(new BigDecimal(1000))
                        .setScale(2, RoundingMode.HALF_UP)
        );
        //4-4：时间信息  duration
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        //5、返回vo对象
        return drivingLineVo;
    }

}
