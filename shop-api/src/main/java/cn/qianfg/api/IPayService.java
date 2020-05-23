package cn.qianfg.api;

import cn.qianfg.entity.Result;
import cn.qianfg.shop.pojo.TradePay;

public interface IPayService {

    Result createPayment(TradePay tradePay);

    Result callbackPayment(TradePay tradePay);

}
