package cn.qianfg;

import cn.qianfg.api.ICouponService;
import cn.qianfg.api.IGoodsService;
import cn.qianfg.api.IOrderService;
import cn.qianfg.api.IUserService;
import cn.qianfg.shop.pojo.TradeGoods;
import cn.qianfg.shop.pojo.TradeOrder;
import cn.qianfg.shop.pojo.TradeUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest(classes = ShopOrderServiceApplication.class)
class ShopOrderServiceApplicationTests {

    @Autowired
    private IOrderService orderService;

    @Test
    public void confirmOrder() {
        Long couponId = 984390130081161398L;
        Long goodsId = 345959443973935104L;
        Long userId = 345963634385633280L;

        //984390130081161398	100.00	345963634385633280		0	coupon
        //345959443973935104	华为P30	999	5000.00	夜间拍照更美	2019-07-09 20:38:00 goods
        //345963634385633280	刘备	123L	18888888888L	100	2019-07-09 13:37:03	900 user

        TradeOrder order = new TradeOrder();
        order.setGoodsId(goodsId);
        order.setUserId(userId);
        order.setCouponId(couponId);
        order.setAddress("深圳");
        order.setGoodsNumber(1);
        order.setGoodsPrice(new BigDecimal(5000));
        order.setOrderAmount(new BigDecimal(5000));
        order.setMoneyPaid(new BigDecimal(100));
        order.setShippingFee(BigDecimal.ZERO);

        orderService.confirmOrder(order);
    }

}
