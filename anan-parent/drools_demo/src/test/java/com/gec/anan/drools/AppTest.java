package com.gec.anan.drools;

import com.gec.anan.drools.model.Order;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AppTest {

    @Autowired
    private KieContainer kieContainer;

    @Test
    public void test(){
        //1.开启session回话
        KieSession kieSession = kieContainer.newKieSession();
        //2.出发规则
        kieSession.fireAllRules();
        //结束回话
        kieSession.dispose();

    }

    @Test
    public void test2(){
        //1.开启session回话
        KieSession kieSession = kieContainer.newKieSession();

        //2.提供fact对象
        Order order = new Order();
        order.setAmout(50);
        //3.把对象放到工作空间
        kieSession.insert(order);
        //4.触发规则
        kieSession.fireAllRules();
        //结束会话
        kieSession.dispose();
        System.out.println("订单金额为: "+order.getAmout()+"获取到的积分为: "+order.getScore());

    }
}