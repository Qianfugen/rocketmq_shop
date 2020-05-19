package cn.qianfg.api;

import cn.qianfg.entity.Result;
import cn.qianfg.shop.pojo.TradeCoupon;

public interface ICouponService {

    /**
     * 根据couponId查询优惠券
     *
     * @param couponId
     * @return
     */
    TradeCoupon findOne(Long couponId);

    /**
     * 更改优惠券状态
     *
     * @param coupon
     * @return
     */
    Result changeCouponStatus(TradeCoupon coupon);
}
