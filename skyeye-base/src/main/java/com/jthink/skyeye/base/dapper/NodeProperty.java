package com.jthink.skyeye.base.dapper;

import com.jthink.skyeye.base.constant.Constants;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 节点性质 说明该节点此时是客户端还是服务端,一个节点是主动发送数据还是被动处理数据,因为一个节点可能会来回切换这两个角色
 * @date 2017-02-17 17:28:01
 */
public enum NodeProperty {

    C(Constants.CLIENT_KEY, Constants.CLIENT_VALUE),//c 和client
    S(Constants.SERVER_KEY, Constants.SERVER_VALUE); //s和 server

    private String symbol;

    private String label;

    private NodeProperty(String symbol, String label) {
        this.symbol = symbol;
        this.label = label;
    }

    public String symbol() {
        return this.symbol;
    }

    public String label() {
        return this.label;
    }
}
