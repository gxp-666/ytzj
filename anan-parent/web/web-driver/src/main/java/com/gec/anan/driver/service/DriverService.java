package com.gec.anan.driver.service;

import com.gec.anan.model.form.driver.DriverFaceModelForm;
import com.gec.anan.model.form.driver.UpdateDriverAuthInfoForm;
import com.gec.anan.model.vo.driver.DriverAuthInfoVo;
import com.gec.anan.model.vo.driver.DriverLoginVo;

public interface DriverService {


    String login(String code);

    DriverLoginVo getDriverLoginInfo(Long driverId);

    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    Boolean isFaceRecognition(Long driverId);

    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    Boolean startService(Long driverId);

    Boolean stopService(Long driverId);

}
