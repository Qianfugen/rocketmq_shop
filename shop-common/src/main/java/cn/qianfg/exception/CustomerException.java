package cn.qianfg.exception;

import cn.qianfg.constant.ShopCode;

public class CustomerException extends RuntimeException {

    private ShopCode shopCode;

    public CustomerException(ShopCode shopCode) {
        this.shopCode = shopCode;
    }
}
