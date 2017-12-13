package com.jthink.skyeye.base.constant;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc
 * @date 2016-11-17 10:02:53
//name表示一个url 或者 第三方名字  或者 帐号名字,类型表示是api、还是带有账户的api,还是第三方请求,app表示产生该日志的app是哪个app
//用于记录哪个app产生了哪些类型的外部请求,用于web页面进行用户筛选,展示每一个app下,每一个name下产生的统计值
 */
public enum NameInfoType {

    API(Constants.API, "api名称"),
    ACCOUNT(Constants.ACCOUNT, "account名称"),
    THIRD(Constants.THIRD, "第三方名称");

    private String symbol;

    private String label;

    private NameInfoType(String symbol, String label) {
        this.symbol = symbol;
        this.label = label;
    }

    public String symbol() {
        return this.symbol;
    }

    public String label() {
        return label;
    }
}
