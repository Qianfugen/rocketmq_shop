package cn.qianfg.service.impl;

import cn.qianfg.api.ICouponService;
import cn.qianfg.api.IGoodsService;
import cn.qianfg.api.IOrderService;
import cn.qianfg.api.IUserService;
import cn.qianfg.constant.ShopCode;
import cn.qianfg.entity.MQEntity;
import cn.qianfg.entity.Result;
import cn.qianfg.exception.CastException;
import cn.qianfg.shop.mapper.TradeOrderMapper;
import cn.qianfg.shop.pojo.*;
import cn.qianfg.utils.IDWorker;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Component
@Service(interfaceClass = IOrderService.class)
public class OrderServiceImpl implements IOrderService {

    @Reference
    private IGoodsService goodsService;

    @Reference
    private IUserService userService;

    @Reference
    private ICouponService couponService;

    @Value("${mq.order.topic}")
    private String topic;

    @Value("${mq.order.tag.cancel}")
    private String tag;

    @Autowired
    private TradeOrderMapper orderMapper;

    @Autowired
    private IDWorker idWorker;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public Result confirmOrder(TradeOrder order) {
        //1.校验订单
        checkOrder(order);
        //2.生成预订单
        Long orderId = savePreOrder(order);
        try {
            //3.扣减库存
            reduceGoodsNum(order);
            //4.扣减优惠券
            updateCouponStatus(order);
            //5.使用余额
            reduceMoneyPaid(order);

            //模拟异常抛出
            CastException.cast(ShopCode.SHOP_FAIL);

            //6.确认订单
            updateOrderStatus(order);
            //7.返回成功状态
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage(), ShopCode.SHOP_SUCCESS.getCode());
        } catch (Exception e) {
            //1.确认订单失败,发送消息
            MQEntity mqEntity = new MQEntity();
            mqEntity.setOrderId(orderId);
            mqEntity.setUserId(order.getUserId());
            mqEntity.setUserMoney(order.getMoneyPaid());
            mqEntity.setGoodsId(order.getGoodsId());
            mqEntity.setGoodsNum(order.getGoodsNumber());
            mqEntity.setCouponId(order.getCouponId());
            //2.返回失败状态
            try {
                sendCancelOrder(topic, tag, order.getOrderId().toString(), JSON.toJSONString(mqEntity));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage(), ShopCode.SHOP_FAIL.getCode());
        }
    }

    /**
     * 发送订单确认失败的消息
     *
     * @param topic
     * @param tag
     * @param toString
     * @param toJSONString
     */
    private void sendCancelOrder(String topic, String tag, String keys, String body) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message message = new Message(topic, tag, keys, body.getBytes());
        rocketMQTemplate.getProducer().send(message);
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
        log.info("开始查找商品");
        log.info("goodsId: " + order.getGoodsId());
        TradeGoods goods = goodsService.findOne(order.getGoodsId());
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

    /**
     * 核算运费 >100 0元,<=100 10元
     *
     * @param orderAmount
     * @return
     */
    private BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        if (orderAmount.compareTo(new BigDecimal(100)) == 1) {
            return BigDecimal.ZERO;
        }
        {
            return new BigDecimal(10);
        }
    }

    /**
     * 生成预订单
     *
     * @param order
     * @return
     */
    private Long savePreOrder(TradeOrder order) {
        //1.设置订单状态不可见
        order.setOrderStatus(ShopCode.SHOP_ORDER_NO_CONFIRM.getCode());
        //2.设置订单ID
        order.setOrderId(idWorker.nextId());
        //3.核算运费是否正确
        BigDecimal shippingFee = calculateShippingFee(order.getOrderAmount());
        if (order.getShippingFee().compareTo(shippingFee) != 0) {
            CastException.cast(ShopCode.SHOP_ORDER_SHIPPINGFEE_INVALID);
        }
        //4.核算订单总价是否正确
        log.info("order:" + order);
        BigDecimal orderAmount = order.getGoodsPrice().multiply(new BigDecimal(order.getGoodsNumber()));
        orderAmount.add(shippingFee);
        if (order.getOrderAmount().compareTo(orderAmount) != 0) {
            CastException.cast(ShopCode.SHOP_ORDERMOUNT_INVALID);
        }
        //5.核算优惠券信息是否合法
        Long couponId = order.getCouponId();
        if (couponId != null) {
            log.info("开始查找优惠券");
            log.info("优惠券ID: " + couponId.toString());
            TradeCoupon coupon = couponService.findOne(couponId);
            //优惠券不存在
            if (coupon == null) {
                CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
            }
            //优惠券已经使用
            if ((coupon.getIsUsed()).equals(ShopCode.SHOP_COUPON_ISUSED.getCode())) {
                CastException.cast(ShopCode.SHOP_COUPON_INVALIED);
            }
            //优惠券可以使用
            order.setCouponPaid(coupon.getCouponPrice());
        } else {
            //没有优惠券,设置优惠价为 0
            order.setCouponPaid(BigDecimal.ZERO);
        }
        //6.判断余额是否正确
        BigDecimal moneyPaid = order.getMoneyPaid();
        if (moneyPaid != null) {
            //比较余额是否大于 0
            int r = moneyPaid.compareTo(BigDecimal.ZERO);
            //余额小于 0
            if (r == -1) {
                CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
            }
            //余额大于 0
            if (r >= 0) {
                //查询用户信息
                TradeUser user = userService.findOne(order.getUserId());
                if (user == null) {
                    CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
                }
                //比较余额是否大于用户账户余额
                if (user.getUserMoney().compareTo(moneyPaid.longValue()) == -1) {
                    CastException.cast(ShopCode.SHOP_MONEY_PAID_INVALIS);
                }
            }
        } else {
            order.setMoneyPaid(BigDecimal.ZERO);
        }
        //7.计算订单支付总价  =  订单总价-优惠价-余额
        BigDecimal payAmount = orderAmount.subtract(order.getCouponPaid()).subtract(order.getMoneyPaid());
        order.setPayAmount(payAmount);
        //8.设置订单添加时间
        order.setAddTime(new Date());
        //9.保存预订单
        orderMapper.insert(order);
        //返回订单ID
        return order.getOrderId();
    }

    /**
     * 扣减库存
     *
     * @param order
     */
    private void reduceGoodsNum(TradeOrder order) {
        TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
        goodsNumberLog.setGoodsId(order.getGoodsId());
        goodsNumberLog.setOrderId(order.getOrderId());
        goodsNumberLog.setGoodsNumber(order.getGoodsNumber());
        Result result = goodsService.reduceGoodsNum(goodsNumberLog);
        if (!result.getSuccess()) {
            //扣减库存失败
            CastException.cast(ShopCode.SHOP_REDUCE_GOODS_NUM_FAIL);
        }

        log.info("订单:[" + order.getOrderId() + "] 扣减库存:[" + order.getGoodsNumber() + "个]成功");

    }

    /**
     * 扣减优惠券
     *
     * @param order
     */
    private void updateCouponStatus(TradeOrder order) {
        //判断用户是否使用优惠券
        if (!StringUtils.isEmpty(order.getCouponId())) {
            //封装优惠券对象
            TradeCoupon coupon = couponService.findOne(order.getCouponId());
            coupon.setIsUsed(ShopCode.SHOP_COUPON_ISUSED.getCode());
            coupon.setUsedTime(new Date());
            coupon.setOrderId(order.getOrderId());
            Result result = couponService.updateCouponStatus(coupon);
            //判断执行结果
            if (result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())) {
                CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
            }

            log.info("订单:[" + order.getOrderId() + "] 使用优惠券[" + coupon.getCouponPrice() + "元] 成功");
        }

    }

    /**
     * 使用余额
     *
     * @param order
     */
    private void reduceMoneyPaid(TradeOrder order) {
        //判断订单中的余额是否合法
        if (order.getMoneyPaid() != null && order.getMoneyPaid().compareTo(BigDecimal.ZERO) == 1) {
            TradeUserMoneyLog userMoneyLog = new TradeUserMoneyLog();
            userMoneyLog.setOrderId(order.getOrderId());
            userMoneyLog.setUserId(order.getUserId());
            userMoneyLog.setUseMoney(order.getMoneyPaid());
            userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_PAID.getCode());
            //扣减余额
            Result result = userService.updateMoneyPaid(userMoneyLog);
            if (result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())) {
                CastException.cast(ShopCode.SHOP_USER_MONEY_REDUCE_FAIL);
            }
            log.info("订单: [" + order.getOrderId() + "] 扣减余额成功");

        } else {
            CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
        }
    }

    /**
     * 确认订单
     *
     * @param order
     */
    private void updateOrderStatus(TradeOrder order) {
        order.setOrderStatus(ShopCode.SHOP_ORDER_CONFIRM.getCode());
        order.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
        order.setConfirmTime(new Date());
        int r = orderMapper.updateByPrimaryKey(order);
        if (r < 0) {
            CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
        }

        log.info("订单: [" + order.getOrderId() + "] 确认订单");
    }


    public TradeUser findUser(Long userId) {
        if (userId == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return userService.findOne(userId);
    }

    @Override
    public TradeOrder fineOne(Long orderId) {
        return orderMapper.selectByPrimaryKey(orderId);
    }
}
