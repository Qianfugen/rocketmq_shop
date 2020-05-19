package cn.qianfg.api;

import cn.qianfg.entity.Result;
import cn.qianfg.shop.pojo.TradeGoods;
import cn.qianfg.shop.pojo.TradeGoodsNumberLog;

public interface IGoodsService {
    /**
     * 根据商品ID查找商品对象
     *
     * @param goodsId
     * @return
     */
    TradeGoods findOne(Long goodsId);

    /**
     * 扣减库存
     *
     * @param goodsNumberLog
     * @return
     */
    Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog);
}
