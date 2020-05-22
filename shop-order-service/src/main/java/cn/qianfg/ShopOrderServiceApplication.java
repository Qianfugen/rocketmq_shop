package cn.qianfg;

import cn.qianfg.utils.IDWorker;
import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDubboConfiguration
public class ShopOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopOrderServiceApplication.class, args);
    }

    @Bean
    public IDWorker getBean(){
        return new IDWorker(1,1);
    }

}
