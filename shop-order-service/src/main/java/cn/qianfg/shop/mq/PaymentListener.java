package cn.qianfg.shop.mq;

import cn.qianfg.constant.ShopCode;
import cn.qianfg.shop.mapper.TradeOrderMapper;
import cn.qianfg.shop.pojo.TradeOrder;
import cn.qianfg.shop.pojo.TradePay;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.pay.topic}", consumerGroup = "${mq.pay.consumer.group}", messageModel = MessageModel.BROADCASTING)
public class PaymentListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeOrderMapper orderMapper;

    @SneakyThrows
    @Override
    public void onMessage(MessageExt message) {
        log.info("接收到支付成功的消息");
        try {
            //1.解析消息内容
            String body = new String(message.getBody(), "UTF-8");
            TradePay tradePay = JSON.parseObject(message.getBody(), TradePay.class);
            //2.根据订单ID查询订单对象
            TradeOrder order = orderMapper.selectByPrimaryKey(tradePay.getOrderId());
            //3.更改订单支付的状态为已支付
            order.setOrderStatus(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
            //4.更新订单数据到数据库
            orderMapper.updateByPrimaryKey(order);
            log.info("更改订单支付状态为已支付");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
