package cn.qianfg.service.impl;

import cn.qianfg.api.IGoodsService;
import cn.qianfg.api.IOrderService;
import cn.qianfg.api.IUserService;
import cn.qianfg.constant.ShopCode;
import cn.qianfg.entity.Result;
import cn.qianfg.exception.CastException;
import cn.qianfg.shop.pojo.TradeGoods;
import cn.qianfg.shop.pojo.TradeOrder;
import cn.qianfg.shop.pojo.TradeUser;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Service(interfaceClass = IOrderService.class)
public class OrderServiceImpl implements IOrderService {

    @Reference
    private IGoodsService iGoodsService;

    @Reference
    private IUserService userService;

    @Override
    public Result confirmOrder(TradeOrder tradeOrder) {
        //1.校验订单

        //2.生成预订单

        try {
            //3.扣减库存

            //4.扣减优惠券

            //5.使用余额

            //6.确认订单

            //7.返回成功状态

        } catch (Exception e) {
            //1.确认订单失败,发送消息

            //2.返回失败状态
        }
        return null;
    }

    /**
     * 校验订单
     *
     * @param order
     */
    private void checkOrder(TradeOrder order) {
        //1.校验订单是否存在
        if (order == null) {
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        //2.校验订单中的商品是否存在
        TradeGoods goods = iGoodsService.findOne(order.getGoodsId());
        if (goods == null) {
            CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);
        }
        //3.校验下单用户是否存在
        TradeUser user = userService.findOne(order.getUserId());
        if (user == null) {
            CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
        }
        //4.校验订单金额是否合法
        if (order.getGoodsPrice().compareTo(goods.getGoodsPrice()) != 0) {
            CastException.cast(ShopCode.SHOP_GOODS_PRICE_INVALID);
        }
        //5.校验订单商品数量是否合法
        if (order.getGoodsNumber() > goods.getGoodsNumber()) {
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }

        log.info("校验订单通过");

    }
}
