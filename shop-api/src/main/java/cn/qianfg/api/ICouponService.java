package cn.qianfg.api;

import cn.qianfg.shop.pojo.TradeCoupon;

public interface ICouponService {

    /**
     * 根据couponId查询优惠券
     * @param couponId
     * @return
     */
    TradeCoupon findOne(Long couponId);
}
