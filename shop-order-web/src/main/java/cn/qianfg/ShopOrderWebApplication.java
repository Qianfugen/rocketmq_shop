package cn.qianfg;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubboConfiguration
@SpringBootApplication
public class ShopOrderWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopOrderWebApplication.class, args);
    }

}
