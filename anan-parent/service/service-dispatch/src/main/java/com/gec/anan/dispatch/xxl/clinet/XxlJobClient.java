package com.gec.anan.dispatch.xxl.clinet;

import com.alibaba.fastjson.JSONObject;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.dispatch.xxl.config.XxlJobClientConfig;
import com.gec.anan.model.entity.dispatch.XxlJobInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * https://dandelioncloud.cn/article/details/1598865461087518722
 */
@Slf4j
@Component
public class XxlJobClient {

    @Autowired
    private XxlJobClientConfig xxlJobClientConfig;

    @Autowired
    private RestTemplate restTemplate;

    @SneakyThrows
    public Long addJob(String executorHandler, String param, String corn, String desc) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        // 设置任务描述
        xxlJobInfo.setJobDesc(desc);
        // 设置任务作者
        xxlJobInfo.setAuthor("qy");
        // 设置调度类型为CRON表达式
        xxlJobInfo.setScheduleType("CRON");
        // 设置CRON表达式配置
        xxlJobInfo.setScheduleConf(corn);
        // 设置粘合类型为BEAN
        xxlJobInfo.setGlueType("BEAN");
        // 设置执行器处理器
        xxlJobInfo.setExecutorHandler(executorHandler);
        // 设置执行器参数
        xxlJobInfo.setExecutorParam(param);
        // 设置执行器路由策略为FIRST
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        // 设置执行器阻塞策略为SERIAL_EXECUTION
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        // 设置误火策略为FIRE_ONCE_NOW
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        // 设置执行器超时时间为0，表示不超时
        xxlJobInfo.setExecutorTimeout(0);
        // 设置执行器失败重试次数为0
        xxlJobInfo.setExecutorFailRetryCount(0);

        // 创建HTTP请求头，设置内容类型为JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体，包含任务信息和请求头
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 获取添加任务的URL
        String url = xxlJobClientConfig.getAddUrl();
        // 发送POST请求到XXL-JOB客户端添加任务
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        // 检查HTTP响应状态码和任务执行结果代码
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 记录增加任务成功的日志信息
            log.info("增加xxl执行任务成功,返回信息:{}", response.getBody().toJSONString());
            // 返回任务ID
            return response.getBody().getLong("content");
        }
        // 记录增加任务失败的日志信息
        log.info("调用xxl增加执行任务失败:{}", response.getBody().toJSONString());
        // 抛出数据错误异常
        throw new AnanException(ResultCodeEnum.DATA_ERROR);
    }

    /**
     * 启动任务
     *
     * @param jobId 任务ID
     * @return 启动成功返回true，否则抛出异常
     */
    public Boolean startJob(Long jobId) {
        // 创建XXL-JOB信息对象并设置任务ID
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        // 创建HTTP请求头，设置内容类型为JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体，包含任务信息和请求头
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 获取启动任务的URL
        String url = xxlJobClientConfig.getStartJobUrl();
        // 发送POST请求到XXL-JOB客户端启动任务
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        // 检查HTTP响应状态码和任务执行结果代码
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 记录启动任务成功的日志信息
            log.info("启动xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        // 记录启动任务失败的日志信息
        log.info("启动xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        // 抛出数据错误异常
        throw new AnanException(ResultCodeEnum.DATA_ERROR);
    }

    /**
     * 停止任务
     *
     * @param jobId 任务ID
     * @return 停止成功返回true，否则抛出异常
     */
    public Boolean stopJob(Long jobId) {
        // 创建XXL-JOB信息对象并设置任务ID
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        // 创建HTTP请求头，设置内容类型为JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体，包含任务信息和请求头
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 获取停止任务的URL
        String url = xxlJobClientConfig.getStopJobUrl();
        // 发送POST请求到XXL-JOB客户端停止任务
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        // 检查HTTP响应状态码和任务执行结果代码
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 记录停止任务成功的日志信息
            log.info("停止xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        // 记录停止任务失败的日志信息
        log.info("停止xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        // 抛出数据错误异常
        throw new AnanException(ResultCodeEnum.DATA_ERROR);
    }

    /**
     * 删除任务
     *
     * @param jobId 任务ID
     * @return 删除成功返回true，否则抛出异常
     */
    public Boolean removeJob(Long jobId) {
        // 创建XXL-JOB信息对象并设置任务ID
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        // 创建HTTP请求头，设置内容类型为JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体，包含任务信息和请求头
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 获取删除任务的URL
        String url = xxlJobClientConfig.getRemoveUrl();
        // 发送POST请求到XXL-JOB客户端删除任务
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        // 检查HTTP响应状态码和任务执行结果代码
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 记录删除任务成功的日志信息
            log.info("删除xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        // 记录删除任务失败的日志信息
        log.info("删除xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        // 抛出数据错误异常
        throw new AnanException(ResultCodeEnum.DATA_ERROR);
    }

    /**
     * 添加并启动任务
     *
     * @param executorHandler 执行器处理器
     * @param param           执行器参数
     * @param corn            CRON表达式
     * @param desc            任务描述
     * @return 添加并启动任务成功返回任务ID，否则抛出异常
     */
    public Long addAndStart(String executorHandler, String param, String corn, String desc) {
        // 创建XXL-JOB信息对象并设置相关属性
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        xxlJobInfo.setJobDesc(desc);
        xxlJobInfo.setAuthor("qy");
        xxlJobInfo.setScheduleType("CRON");
        xxlJobInfo.setScheduleConf(corn);
        xxlJobInfo.setGlueType("BEAN");
        xxlJobInfo.setExecutorHandler(executorHandler);
        xxlJobInfo.setExecutorParam(param);
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        xxlJobInfo.setExecutorTimeout(0);
        xxlJobInfo.setExecutorFailRetryCount(0);

        // 创建HTTP请求头，设置内容类型为JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体，包含任务信息和请求头
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 获取添加并启动任务的URL
        String url = xxlJobClientConfig.getAddAndStartUrl();
        // 发送POST请求到XXL-JOB客户端添加并启动任务
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        // 检查HTTP响应状态码和任务执行结果代码
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 记录添加并启动任务成功的日志信息
            log.info("增加并开始执行xxl任务成功,返回信息:{}", response.getBody().toJSONString());
            // 返回任务ID
            return response.getBody().getLong("content");
        }
        // 记录添加并启动任务失败的日志信息
        log.info("增加并开始执行xxl任务失败:{}", response.getBody().toJSONString());
        // 抛出数据错误异常
        throw new AnanException(ResultCodeEnum.DATA_ERROR);
    }
}
