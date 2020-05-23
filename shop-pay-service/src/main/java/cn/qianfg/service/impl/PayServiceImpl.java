package cn.qianfg.service.impl;

import cn.qianfg.api.IPayService;
import cn.qianfg.constant.ShopCode;
import cn.qianfg.entity.Result;
import cn.qianfg.exception.CastException;
import cn.qianfg.shop.mapper.TradeMqProducerTempMapper;
import cn.qianfg.shop.mapper.TradePayMapper;
import cn.qianfg.shop.pojo.TradeMqProducerTemp;
import cn.qianfg.shop.pojo.TradePay;
import cn.qianfg.shop.pojo.TradePayExample;
import cn.qianfg.utils.IDWorker;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@Service(interfaceClass = IPayService.class)
public class PayServiceImpl implements IPayService {

    @Autowired
    private TradePayMapper tradePayMapper;

    @Autowired
    private IDWorker idWorker;

    @Value("${rocketmq.producer.group}")
    private String groupName;

    @Value("${mq.pay.topic}")
    private String topic;

    @Value("${mq.pay.tag}")
    private String tag;

    @Autowired
    private TradeMqProducerTempMapper mqProducerTempMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private ThreadPoolTaskExecutor executor;

    @Override
    public Result createPayment(TradePay tradePay) {
        try {
            if (tradePay == null || tradePay.getOrderId() == null) {
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }

            //1.判断订单的支付状态
            TradePayExample example = new TradePayExample();
            TradePayExample.Criteria criteria = example.createCriteria();
            criteria.andOrderIdEqualTo(tradePay.getOrderId());
            criteria.andIsPaidEqualTo(ShopCode.SHOP_PAYMENT_IS_PAID.getCode());
            int r = tradePayMapper.countByExample(example);
            if (r > 0) {
                CastException.cast(ShopCode.SHOP_PAYMENT_IS_PAID);
            }

            //2.设置订单的状态为未支付
            tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
            //3.保持支付订单
            tradePay.setPayId(idWorker.nextId());
            tradePayMapper.insert(tradePay);
            log.info("创建支付订单成功: " + tradePay.getPayId());
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage(), ShopCode.SHOP_FAIL.getCode());
        }

        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage(), ShopCode.SHOP_SUCCESS.getCode());
    }

    @Override
    public Result callbackPayment(TradePay tradePay) {
        log.info("支付回调");
        if (tradePay == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //1.判断用户支付状态
        if (tradePay.getIsPaid().intValue() == ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode().intValue()) {
            //2.更新支付订单状态为已支付
            Long payId = tradePay.getPayId();
            TradePay pay = tradePayMapper.selectByPrimaryKey(payId);
            //判断支付订单是否存在
            if (pay == null) {
                CastException.cast(ShopCode.SHOP_PAYMENT_NOT_FOUND);
            }
            pay.setIsPaid(ShopCode.SHOP_PAYMENT_IS_PAID.getCode());
            int r = tradePayMapper.updateByPrimaryKey(pay);
            log.info("支付订单状态改为已支付");
            if (r == 1) {
                //3.创建支付成功的消息
                TradeMqProducerTemp mqProducerTemp = new TradeMqProducerTemp();
                String key = String.valueOf(tradePay.getPayId());
                mqProducerTemp.setId(String.valueOf(idWorker.nextId()));
                mqProducerTemp.setGroupName(groupName);
                mqProducerTemp.setMsgTag(tag);
                mqProducerTemp.setMsgTopic(topic);
                mqProducerTemp.setMsgKey(key);
                mqProducerTemp.setMsgBody(JSON.toJSONString(tradePay));
                mqProducerTemp.setCreateTime(new Date());
                //4.将消息持久化到数据库
                mqProducerTempMapper.insert(mqProducerTemp);
                log.info("将支付成功的消息持久化到数据库");
                //使用线程池优化消息发送
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        //5.发送消息MQ
                        try {
                            SendResult sendResult = sendMessage(topic, tag, key, JSON.toJSONString(tradePay));
                            if (sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
                                log.info("消息发送成功");
                                //6.等待发送结果,如果MQ消息,删除发送成功的消息
                                mqProducerTempMapper.deleteByPrimaryKey(mqProducerTemp.getId());
                                log.info("持久化到数据库的消息删除");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage(), ShopCode.SHOP_SUCCESS.getCode());
        } else {
            //订单支付失败
            CastException.cast(ShopCode.SHOP_PAYMENT_PAY_ERROR);
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage(), ShopCode.SHOP_FAIL.getCode());
        }
    }

    private SendResult sendMessage(String topic, String tag, String key, String body) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        if (topic == null) {
            CastException.cast(ShopCode.SHOP_MQ_TOPIC_IS_EMPTY);
        }
        if (body == null) {
            CastException.cast(ShopCode.SHOP_MQ_MESSAGE_BODY_IS_EMPTY);
        }
        Message message = new Message(topic, tag, key, body.getBytes());
        SendResult sendResult = rocketMQTemplate.getProducer().send(message);
        return sendResult;
    }
}
