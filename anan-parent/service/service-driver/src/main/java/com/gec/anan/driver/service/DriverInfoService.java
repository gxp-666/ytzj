package com.gec.anan.driver.service;

import com.gec.anan.model.entity.driver.DriverInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.anan.model.entity.driver.DriverSet;
import com.gec.anan.model.form.driver.DriverFaceModelForm;
import com.gec.anan.model.form.driver.UpdateDriverAuthInfoForm;
import com.gec.anan.model.vo.driver.DriverAuthInfoVo;
import com.gec.anan.model.vo.driver.DriverInfoVo;
import com.gec.anan.model.vo.driver.DriverLoginVo;

public interface DriverInfoService extends IService<DriverInfo> {

    Long login(String code);


    DriverLoginVo getDriverLoginInfo(Long driverId);

    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    DriverSet getDriverSet(Long driverId);

    Boolean isFaceRecognition(Long driverId);

    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    Boolean updateServiceStatus(Long driverId, Integer status);

    DriverInfoVo getDriverInfoOrder(Long driverId);

    String getDriverOpenId(Long driverId);

}
