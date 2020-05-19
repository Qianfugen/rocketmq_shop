package cn.qianfg.service.impl;

import cn.qianfg.api.IUserService;
import cn.qianfg.constant.ShopCode;
import cn.qianfg.exception.CastException;
import cn.qianfg.shop.mapper.TradeUserMapper;
import cn.qianfg.shop.pojo.TradeUser;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Service(interfaceClass = IUserService.class)
public class UserServiceImpl implements IUserService {

    @Autowired
    private TradeUserMapper userMapper;

    @Override
    public TradeUser findOne(Long userId) {
        if (userId == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return userMapper.selectByPrimaryKey(userId);
    }
}
