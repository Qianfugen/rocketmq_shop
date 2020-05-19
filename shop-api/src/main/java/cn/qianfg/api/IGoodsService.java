package cn.qianfg.api;

import cn.qianfg.shop.pojo.TradeGoods;

public interface IGoodsService {
    /**
     * 根据商品ID查找商品对象
     *
     * @param goodsId
     * @return
     */
    TradeGoods findOne(Long goodsId);
}
