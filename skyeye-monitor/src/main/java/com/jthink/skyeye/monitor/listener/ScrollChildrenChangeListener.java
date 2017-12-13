package com.jthink.skyeye.monitor.listener;

import com.jthink.skyeye.data.rabbitmq.service.RabbitmqService;
import com.jthink.skyeye.monitor.service.AppInfoService;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc app的root节点变化监听
 * @date 2016-09-23 14:49:36
 */
public class ScrollChildrenChangeListener implements PathChildrenCacheListener  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrollChildrenChangeListener.class);

    private RabbitmqService rabbitmqService;

    private ZkClient zkClient;

    private AppInfoService appInfoService;

    public ScrollChildrenChangeListener(RabbitmqService rabbitmqService, ZkClient zkClient, AppInfoService appInfoService) {
        this.rabbitmqService = rabbitmqService;
        this.zkClient = zkClient;
        this.appInfoService = appInfoService;
    }

    //先监听/skyeye/monitor/scroll节点,因为新增节点都是先进入这个节点的,因此先对他进行监听,比如先部署一个项目
    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
            case CHILD_ADDED://比如新加入一个app
                PathChildrenCache pathChildrenCache = new PathChildrenCache(client, event.getData().getPath(), true);
                pathChildrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
                pathChildrenCache.getListenable().addListener(new AppChildrenChangeListener(this.rabbitmqService, this.zkClient, this.appInfoService));//为新增的app节点增加一个监听
                LOGGER.info("app added: " + event.getData().getPath());
                break;
            case CHILD_REMOVED://理论上这段应该不会出现的,因为app节点本身是一个永久的节点
                LOGGER.info("app removed: " + event.getData().getPath());
                break;
        }
    }
}
