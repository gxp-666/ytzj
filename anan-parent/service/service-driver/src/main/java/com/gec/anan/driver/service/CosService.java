package com.gec.anan.driver.service;

import com.gec.anan.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {
    /**
     * 获取临时签名的url
     * @param path
     * @return
     */
    String getImageUrl(String path);

    /**
     * 上传图片的业务逻辑代码
     * @param file
     * @param path
     * @return
     */
    CosUploadVo upload(MultipartFile file, String path);
}
