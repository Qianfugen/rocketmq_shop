package cn.qianfg;

import cn.qianfg.api.IPayService;
import cn.qianfg.constant.ShopCode;
import cn.qianfg.shop.pojo.TradePay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;

@SpringBootTest(classes = ShopPayServiceApplication.class)
class ShopPayServiceApplicationTests {

    @Autowired
    private IPayService payService;

    @Test
    public void createPayment() {
        long orderId = 461442663539937280L;

        TradePay tradePay = new TradePay();
        tradePay.setOrderId(orderId);
        tradePay.setPayAmount(new BigDecimal(4800));
        payService.createPayment(tradePay);
    }

    @Test
    public void callbackPayment() throws IOException {
        long payId = 461740161181556736L;
        long orderId = 461442663539937280L;

        TradePay tradePay = new TradePay();
        tradePay.setPayId(payId);
        tradePay.setOrderId(orderId);
        tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
        payService.callbackPayment(tradePay);

        System.in.read();
    }
}
