package cn.qianfg.service.impl;

import cn.qianfg.api.ICouponService;
import cn.qianfg.constant.ShopCode;
import cn.qianfg.entity.Result;
import cn.qianfg.exception.CastException;
import cn.qianfg.shop.mapper.TradeCouponMapper;
import cn.qianfg.shop.pojo.TradeCoupon;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Service(interfaceClass = ICouponService.class)
public class CouponServiceImpl implements ICouponService {

    @Autowired
    private TradeCouponMapper couponMapper;

    @Override
    public TradeCoupon findOne(Long couponId) {
        return couponMapper.selectByPrimaryKey(couponId);
    }

    @Override
    public Result updateCouponStatus(TradeCoupon coupon) {
        try {
            //判断请求参数是否合法
            if (coupon == null || StringUtils.isEmpty(coupon.getCouponId())) {
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }
            //更新优惠券状态为已使用
            couponMapper.updateByPrimaryKey(coupon);
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage(), ShopCode.SHOP_SUCCESS.getCode());
        } catch (Exception e) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage(), ShopCode.SHOP_FAIL.getCode());
        }
    }
}
