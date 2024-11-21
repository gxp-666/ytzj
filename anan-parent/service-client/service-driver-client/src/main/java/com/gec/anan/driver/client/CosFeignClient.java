package com.gec.anan.driver.client;

import com.gec.anan.common.result.Result;
import com.gec.anan.model.vo.driver.CosUploadVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(value = "service-driver")
public interface CosFeignClient {

    /**
     * 上传
     * @param file
     * @param path
     * @return
     */
    @PostMapping(value = "/cos/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CosUploadVo> upload(@RequestPart("file") MultipartFile file, @RequestParam("path") String path);
}