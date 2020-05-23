package cn.qianfg.shop.controller;

import cn.qianfg.api.IOrderService;
import cn.qianfg.entity.Result;
import cn.qianfg.shop.pojo.TradeOrder;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Reference
    private IOrderService orderService;

    @RequestMapping("/confirm")
    public Result confirmOrder(@RequestBody TradeOrder order) {
        return orderService.confirmOrder(order);
    }

}
