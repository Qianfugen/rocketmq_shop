package cn.qianfg.shop.controller;

import cn.qianfg.api.IPayService;
import cn.qianfg.entity.Result;
import cn.qianfg.shop.pojo.TradePay;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference
    private IPayService payService;

    @RequestMapping("/createPayment")
    public Result createPayment(@RequestBody TradePay pay){
        return payService.createPayment(pay);
    }

    @RequestMapping("/callbackPayment")
    public Result callbackPayment(@RequestBody TradePay pay){
        return payService.callbackPayment(pay);
    }

}
