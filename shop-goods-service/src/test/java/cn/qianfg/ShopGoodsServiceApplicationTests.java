package cn.qianfg;

import cn.qianfg.api.IGoodsService;
import cn.qianfg.service.impl.GoodsServiceImpl;
import cn.qianfg.shop.pojo.TradeGoods;
import cn.qianfg.shop.pojo.TradeUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ShopGoodsServiceApplication.class)
class ShopGoodsServiceApplicationTests {

    @Autowired
    private IGoodsService goodsService;

    @Test
    void contextLoads() {
        TradeGoods goods = goodsService.findOne(345959443973935104L);
        System.out.println(goods.getGoodsId()+" "+goods.getGoodsName()+" "+goods.getGoodsPrice());
    }


}
