package com.gec.anan.driver.controller;

import com.gec.anan.common.login.AnanLogin;
import com.gec.anan.common.result.Result;
import com.gec.anan.driver.service.OcrService;
import com.gec.anan.model.vo.driver.DriverLicenseOcrVo;
import com.gec.anan.model.vo.driver.IdCardOcrVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "腾讯云识别接口管理")
@RestController
@RequestMapping(value="/ocr")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrController {


    @Autowired
    OcrService ocrService;

    @Operation(summary = "身份证识别")
    @AnanLogin
    @PostMapping("/idCardOcr")
    public Result<IdCardOcrVo> uploadDriverLicenseOcr(@RequestPart("file") MultipartFile file) {
        return Result.ok(ocrService.idCardOcr(file));
    }


    @Operation(summary = "驾驶证识别")
    @AnanLogin
    @PostMapping("/driverLicenseOcr")
    public Result<DriverLicenseOcrVo> driverLicenseOcr(@RequestPart("file") MultipartFile file) {
        return Result.ok(ocrService.driverLicenseOcr(file));
    }
}
