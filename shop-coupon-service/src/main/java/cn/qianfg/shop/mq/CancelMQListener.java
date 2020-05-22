package cn.qianfg.shop.mq;

import cn.qianfg.constant.ShopCode;
import cn.qianfg.entity.MQEntity;
import cn.qianfg.shop.mapper.TradeCouponMapper;
import cn.qianfg.shop.pojo.TradeCoupon;
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
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeCouponMapper couponMapper;

    @SneakyThrows
    @Override
    public void onMessage(MessageExt message) {
        try {
            //1.解析消息内容
            String body = new String(message.getBody(), "UTF-8");
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            log.info("接收到消息");
            //2.查询优惠券信息
            TradeCoupon coupon = couponMapper.selectByPrimaryKey(mqEntity.getCouponId());
            if (coupon != null) {
                coupon.setOrderId(null);
                coupon.setUsedTime(null);
                coupon.setIsUsed(ShopCode.SHOP_COUPON_UNUSED.getCode());
                couponMapper.updateByPrimaryKey(coupon);
                log.info("回退优惠券成功");
            }
            //3.更新优惠券状态
        } catch (Exception e) {
            e.printStackTrace();
            log.error("回退优惠券失败");
        }
    }
}
