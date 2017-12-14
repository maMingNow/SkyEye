package com.jthink.skyeye.trace.core.registry;

import com.jthink.skyeye.trace.core.dto.RegisterDto;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 注册中心注册器
 * @date 2017-03-23 10:10:06
 */
public interface Registry {

    /**
     * 对服务进行注册
     * @return 返回注册serviceID
     * 每一个服务只能开启一个唯一的ID,全局共享,该唯一ID是在该节点上用于生成分布式上的唯一的ID
     */
    String register(RegisterDto registerDto);
}
