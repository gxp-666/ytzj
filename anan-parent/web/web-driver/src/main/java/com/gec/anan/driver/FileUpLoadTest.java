package com.gec.anan.driver;


import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.FileInputStream;

public class FileUpLoadTest {


    public static void main(String[] args) throws Exception {

        //创建一个minio的客户端对象
        MinioClient client = MinioClient.builder()
                .endpoint("http://192.168.137.131:9000")
                .credentials("admin", "admin123456")
                .build();
        //判断存储桶是否存在
        boolean isExist = client.bucketExists(BucketExistsArgs.builder().bucket("yuntu").build());
        if (!isExist) {       // 如果不存在，那么此时就创建一个新的桶
            client.makeBucket(MakeBucketArgs.builder().bucket("yuntu").build());
        } else {  // 如果存在打印信息
            System.out.println("Bucket 'daijia' already exists.");
        }
        //存在 创建文件流 构建put 对象
        FileInputStream fileInputStream = new FileInputStream(
                "P:\\GZ2428期-4阶段\\Day-1111(微服务-云途智驾)\\猪猪侠测试minion.jpg");

        PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                .bucket("yuntu")
                .stream(fileInputStream, fileInputStream.available(), -1)
                .object("GGB.jpg")
                .build();

        //上传文件到存储桶中
        client.putObject(putObjectArgs);
        //返回一个访问的url
        String fileUrl = "http://192.168.137.131:9000/yuntu/GGB.jpg";
        System.out.println(fileUrl);


    }
}
