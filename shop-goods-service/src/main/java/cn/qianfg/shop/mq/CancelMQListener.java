package cn.qianfg.shop.mq;

import cn.qianfg.constant.ShopCode;
import cn.qianfg.entity.MQEntity;
import cn.qianfg.shop.mapper.TradeGoodsMapper;
import cn.qianfg.shop.mapper.TradeGoodsNumberLogMapper;
import cn.qianfg.shop.mapper.TradeMqConsumerLogMapper;
import cn.qianfg.shop.pojo.*;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Value("${mq.order.consumer.group.name}")
    private String groupName;

    @Autowired
    private TradeMqConsumerLogMapper mqConsumerLogMapper;

    @Autowired
    private TradeGoodsMapper goodsMapper;

    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @SneakyThrows
    @Override
    public void onMessage(MessageExt message) {
        String msgId = null;
        String tags = null;
        String keys = null;
        String body = null;
        try {
            //1.解析消息内容
            msgId = message.getMsgId();
            tags = message.getTags();
            keys = message.getKeys();
            body = new String(message.getBody(), "UTF-8");

            log.info("接收消息成功");

            //2.查询消息消费记录
            TradeMqConsumerLogKey primaryKey = new TradeMqConsumerLogKey();
            primaryKey.setGroupName(groupName);
            primaryKey.setMsgTag(tags);
            primaryKey.setMsgKey(keys);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(primaryKey);

            if (mqConsumerLog != null) {
                //3.判断如果消费过
                //3.1 获得消息处理状态
                Integer status = mqConsumerLog.getConsumerStatus();

                if (status.intValue() == ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode().intValue()) {
                    log.info("消息: " + msgId + " 已经处理过");
                    return;
                }

                if (status.intValue() == ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode().intValue()) {
                    log.info("消息: " + msgId + " 正在处理中");
                    return;
                }

                if (status.intValue() == ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode().intValue()) {
                    //获得消息处理失败的次数
                    Integer times = mqConsumerLog.getConsumerTimes();

                    //超过三次,直接放弃
                    if (times > 3) {
                        log.info("消息: " + msgId + " 处理超过3次,不能再进行处理");
                        return;
                    }

                    //没有超过三次
                    //将处理状态改为正在处理
                    mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());

                    //使用数据库乐观锁进行更新
                    TradeMqConsumerLogExample example = new TradeMqConsumerLogExample();
                    TradeMqConsumerLogExample.Criteria criteria = example.createCriteria();
                    criteria.andMsgTagEqualTo(tags);
                    criteria.andMsgKeyEqualTo(keys);
                    criteria.andGroupNameEqualTo(groupName);
                    criteria.andConsumerTimesEqualTo(0);
                    int r = mqConsumerLogMapper.updateByExampleSelective(mqConsumerLog, example);

                    if (r <= 0) {
                        //没有修改成功
                        log.info("并发修改,稍后处理");
                    }
                }
            } else {
                //4.判断如果没有消费过
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setGroupName(groupName);
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setMsgId(msgId);
                mqConsumerLog.setMsgBody(body);
                mqConsumerLog.setConsumerTimes(0);
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());

                //将消息处理信息添加到数据库
                mqConsumerLogMapper.insert(mqConsumerLog);

            }
            //5.回退库存
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            Long goodsId = mqEntity.getGoodsId();
            TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
            goods.setGoodsNumber(goods.getGoodsNumber() + mqEntity.getGoodsNum());
            goodsMapper.updateByPrimaryKey(goods);

            //记录库存操作日志
            TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
            goodsNumberLog.setOrderId(mqEntity.getOrderId());
            goodsNumberLog.setGoodsId(mqEntity.getGoodsId());
            goodsNumberLog.setGoodsNumber(mqEntity.getGoodsNum());
            goodsNumberLog.setLogTime(new Date());
            //数据库表如果设置了orderId和goodsId联合绑定为主键,会冲突,所以取消
            log.info(goodsNumberLog.toString());
            goodsNumberLogMapper.insert(goodsNumberLog);

            //6.将消息处理状态改为成功
            mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
            mqConsumerLog.setConsumerTimestamp(new Date());
            mqConsumerLogMapper.updateByPrimaryKey(mqConsumerLog);

            log.info("回退库存成功");
        } catch (Exception e) {
            e.printStackTrace();
            //查询消息消费记录
            TradeMqConsumerLogKey primaryKey = new TradeMqConsumerLogKey();
            primaryKey.setGroupName(groupName);
            primaryKey.setMsgTag(tags);
            primaryKey.setMsgKey(keys);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(primaryKey);

            if (mqConsumerLog == null) {
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setMsgId(msgId);
                mqConsumerLog.setMsgBody(body);
                //第一次记录失败次数为1
                mqConsumerLog.setConsumerTimes(1);
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());

                //将消息处理信息添加到数据库
                mqConsumerLogMapper.insert(mqConsumerLog);
            } else {
                //失败次数+1
                mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes() + 1);
                //更新数据库的失败次数
                mqConsumerLogMapper.updateByPrimaryKey(mqConsumerLog);
            }

        }
    }
}
