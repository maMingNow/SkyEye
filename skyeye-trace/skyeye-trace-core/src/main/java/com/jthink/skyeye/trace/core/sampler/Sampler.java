package com.jthink.skyeye.trace.core.sampler;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 采样率接口
 * @date 2017-02-15 09:46:06
 */
public interface Sampler {

    /**
     * 是否采集--true表示要采样,false表示不采样该数据
     * @return
     */
    boolean isCollect();
}
