package com.gec.anan.common.login;


import com.gec.anan.common.constant.RedisConstant;
import com.gec.anan.common.execption.AnanException;
import com.gec.anan.common.result.ResultCodeEnum;
import com.gec.anan.common.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

//定义切面类
@Component
@Aspect
public class AnanLoginAspect {


    @Autowired
    RedisTemplate redisTemplate;

    //定义一个切点表达式,增强处理
    @Around("execution(* com.gec.anan.*.controller.*.*(..)) && @annotation(ananLogin)")
    public Object login(ProceedingJoinPoint proceedingJoinPoint, AnanLogin ananLogin) throws Throwable {
        //1.获取请求对象\
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();

        //2.获取请求头中token
        String token = request.getHeader("token");
        //3.判断是否有token
        if (token == null) {
            throw new AnanException(ResultCodeEnum.LOGIN_AUTH);
        }
        //4.有token 则 查询
        String customerId = (String) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        //5.然后判断cutomerId不为空 ,并且把CustomId 存放在当前线程的副本ThreadLocal
        if (customerId != null) {
            AuthContextHolder.setUserId(Long.parseLong(customerId));
        }
        //6.执行业务方法
        return proceedingJoinPoint.proceed();
    }
}
