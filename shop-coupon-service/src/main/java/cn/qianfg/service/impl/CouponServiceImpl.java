package cn.qianfg.service.impl;

import cn.qianfg.api.ICouponService;
import cn.qianfg.shop.mapper.TradeCouponMapper;
import cn.qianfg.shop.pojo.TradeCoupon;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Service(interfaceClass = ICouponService.class)
public class CouponServiceImpl implements ICouponService {

    @Autowired
    private TradeCouponMapper couponMapper;

    @Override
    public TradeCoupon findOne(Long couponId) {
        return couponMapper.selectByPrimaryKey(couponId);
    }
}
