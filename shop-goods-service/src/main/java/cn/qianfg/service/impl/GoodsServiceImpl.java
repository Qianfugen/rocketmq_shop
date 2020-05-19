package cn.qianfg.service.impl;

import cn.qianfg.api.IGoodsService;
import cn.qianfg.constant.ShopCode;
import cn.qianfg.exception.CastException;
import cn.qianfg.shop.mapper.TradeGoodsMapper;
import cn.qianfg.shop.pojo.TradeGoods;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
//dubbo的service
@Service(interfaceClass = IGoodsService.class)
public class GoodsServiceImpl implements IGoodsService {

    @Autowired
    private TradeGoodsMapper goodsMapper;

    @Override
    public TradeGoods findOne(Long goodsId) {
        if (goodsId == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return goodsMapper.selectByPrimaryKey(goodsId);
    }
}
