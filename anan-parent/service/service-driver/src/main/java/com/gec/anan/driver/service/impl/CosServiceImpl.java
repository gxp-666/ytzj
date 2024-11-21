package com.gec.anan.driver.service.impl;

import com.alibaba.fastjson.JSON;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.driver.config.TencentCloudProperties;
import com.gec.anan.driver.service.CiService;
import com.gec.anan.driver.service.CosService;
import com.gec.anan.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import static com.qcloud.cos.demo.BucketRefererDemo.cosClient;

/**
 * 实现CosService接口，提供与腾讯云COS服务交互的具体实现
 */
@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {


    @Autowired
    CiService ciService;

    /**
     * 根据文件路径获取文件的临时访问URL
     *
     * @param path 文件在COS中的路径
     * @return 文件的临时访问URL，如果路径为空则返回空字符串
     */
    @Override
    public String getImageUrl(String path) {
        if(!StringUtils.hasText(path)) return "";
        //获取cosclient对象
        COSClient cosClient = this.getPrivateCOSClient();
        //GeneratePresignedUrlRequest
        GeneratePresignedUrlRequest request =  new GeneratePresignedUrlRequest(
                        tencentCloudProperties.getBucketPrivate(),
                        path,
                        HttpMethodName.GET);
        //设置临时URL有效期为15分钟
        Date date;
        date = new DateTime().plusMinutes(15).toDate();
        request.setExpiration(date);
        //调用方法获取
        URL url = cosClient.generatePresignedUrl(request);
        cosClient.shutdown();
        return url.toString();
    }

    /**
     * 文件上传
     *
     * @param file 要上传的文件
     * @param path 文件在COS中存储的路径
     * @return 包含上传文件信息的CosUploadVo对象
     */
    @Override
    @SneakyThrows
    public CosUploadVo upload(MultipartFile file, String path) {
        COSClient cosClient = this.getPrivateCOSClient();

        //文件上传
        //元数据信息
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentEncoding("UTF-8");
        meta.setContentType(file.getContentType());

        //向存储桶中保存文件
        String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")); //文件后缀名
        String uploadPath = "/driver/" + path + "/" + UUID.randomUUID().toString().replaceAll("-", "") + fileType;
        // 01.jpg
        // /driver/auth/0o98754.jpg
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                tencentCloudProperties.getBucketPrivate(), uploadPath, file.getInputStream(), meta);
        putObjectRequest.setStorageClass(StorageClass.Standard);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest); //上传文件
        log.info(JSON.toJSONString(putObjectResult));
        cosClient.shutdown();

        //图片的审核判断
        Boolean imageAuditing = ciService.imageAuditing(uploadPath);
        if(!imageAuditing) {
            //删除违规图片
            cosClient.deleteObject(tencentCloudProperties.getBucketPrivate(),uploadPath);
            throw new AnanException(ResultCodeEnum.IMAGE_AUDITION_FAIL);
        }

        //封装返回对象
        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);
        //图片临时访问url，回显使用
        String imageUrl = this.getImageUrl(uploadPath);
        cosUploadVo.setShowUrl(imageUrl);
        return cosUploadVo;
    }

    /**
     * 注入腾讯云配置属性
     */
    @Autowired
    private TencentCloudProperties tencentCloudProperties;

    /**
     * 获取私有COS客户端
     *
     * @return 初始化并配置好的COS客户端对象
     */
    private COSClient getPrivateCOSClient() {
        String secretId = tencentCloudProperties.getSecretId();
        String secretKey = tencentCloudProperties.getSecretKey();
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域
        ClientConfig clientConfig = new ClientConfig(new Region(tencentCloudProperties.getRegion()));
        // 这里建议设置使用 https 协议
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        COSClient cosClient = new COSClient(cred, clientConfig);
        // 4 返回客户端对象
        return cosClient;
    }
}
