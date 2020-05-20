package cn.qianfg.api;

import cn.qianfg.entity.Result;
import cn.qianfg.shop.pojo.TradeOrder;
import cn.qianfg.shop.pojo.TradeUser;
import cn.qianfg.shop.pojo.TradeUserMoneyLog;

public interface IUserService {

    /**
     * 根据用户ID查询用户对象
     *
     * @param userId
     * @return
     */
    TradeUser findOne(Long userId);

    /**
     * 扣减余额
     *
     * @param userMoneyLog
     * @return
     */
    Result updateMoneyPaid(TradeUserMoneyLog userMoneyLog);
}
