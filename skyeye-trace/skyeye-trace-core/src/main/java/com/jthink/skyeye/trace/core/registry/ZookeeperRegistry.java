package com.jthink.skyeye.trace.core.registry;

import com.jthink.skyeye.base.constant.Constants;
import com.jthink.skyeye.trace.core.generater.IdGen;
import com.jthink.skyeye.trace.core.dto.RegisterDto;
import com.jthink.skyeye.trace.core.generater.IncrementIdGen;
import org.I0Itec.zkclient.ZkClient;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 利用zookeeper实现注册中心
 * @date 2017-03-23 10:14:22
 */
public class ZookeeperRegistry implements Registry {

    /**
     * 向注册中心进行注册，生成该服务的编号并返回
     * @param registerDto
     * @return
     */
    @Override
    public String register(RegisterDto registerDto) {
        String host = registerDto.getHost();
        String app = registerDto.getApp();

        // 向注册中心注册
        ZkClient zkClient = registerDto.getZkClient();
        zkClient.createPersistent(Constants.ZK_REGISTRY_SERVICE_ROOT_PATH + Constants.SLASH + app, true);//创建/skyeye/registry/service/app这个永久节点
        IdGen idGen = new IncrementIdGen(registerDto);
        String id = idGen.nextId();//为该app/host分配一个id
        zkClient.createEphemeral(Constants.ZK_REGISTRY_SERVICE_ROOT_PATH + Constants.SLASH + app + Constants.SLASH + host, id);//创建 /skyeye/registry/service/app/host临时节点,上面存储这workerid

        return id;
    }

}
