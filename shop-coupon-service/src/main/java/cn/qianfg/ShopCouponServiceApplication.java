package cn.qianfg;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubboConfiguration
public class ShopCouponServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopCouponServiceApplication.class, args);
    }

}
