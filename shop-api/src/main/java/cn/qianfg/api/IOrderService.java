package cn.qianfg.api;

import cn.qianfg.entity.Result;
import cn.qianfg.shop.pojo.TradeOrder;

public interface IOrderService {
    /**
     * 下单接口
     *
     * @param tradeOrder
     * @return
     */
    Result confirmOrder(TradeOrder tradeOrder);

    TradeOrder fineOne(Long orderId);
}
