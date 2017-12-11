package com.jthink.skyeye.client.core.constant;

import com.jthink.skyeye.base.constant.Constants;

/**
 * JThink@JThink
 * 使用zookeeper的永久节点和临时节点的性质
 * @author JThink
 * @version 0.0.1
 * @desc 节点性质
 * @date 2016-09-21 15:47:17
 */
public enum NodeMode {

    EPHEMERAL("EPHEMERAL", Constants.ROOT_PATH_EPHEMERAL),
    PERSISTENT("PERSISTENT", Constants.ROOT_PATH_PERSISTENT);

    private String symbol;//是永久节点还是临时节点

    private String label;//根节点路径

    private NodeMode(String symbol, String label) {
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
