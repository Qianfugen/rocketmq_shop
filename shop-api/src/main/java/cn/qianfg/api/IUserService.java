package cn.qianfg.api;

import cn.qianfg.shop.pojo.TradeUser;

public interface IUserService {

    /**
     * 根据用户ID查询用户对象
     *
     * @param userId
     * @return
     */
    TradeUser findOne(Long userId);

}
