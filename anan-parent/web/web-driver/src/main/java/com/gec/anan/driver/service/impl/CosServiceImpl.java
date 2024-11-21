package com.gec.anan.driver.service.impl;

import com.gec.anan.common.result.Result;
import com.gec.anan.driver.client.CosFeignClient;
import com.gec.anan.driver.service.CosService;
import com.gec.anan.model.vo.driver.CosUploadVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {



    @Autowired
    private CosFeignClient client;


    @Override
    public CosUploadVo upload(MultipartFile file, String path) {
        Result<CosUploadVo> upload = client.upload(file, path);
        CosUploadVo cosUploadVo = upload.getData();
        return cosUploadVo;
    }
}
