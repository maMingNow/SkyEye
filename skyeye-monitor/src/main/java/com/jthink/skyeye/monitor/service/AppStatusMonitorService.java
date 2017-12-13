package com.jthink.skyeye.monitor.service;

import com.jthink.skyeye.data.rabbitmq.service.RabbitmqService;
import com.jthink.skyeye.monitor.listener.ScrollChildrenChangeListener;
import com.jthink.skyeye.base.constant.Constants;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 应用系统状态监控服务
 * @date 2016-09-23 14:55:28
 */
@Service
public class AppStatusMonitorService {

    @Autowired
    private CuratorFramework curatorFramework;
    @Autowired
    private RabbitmqService rabbitmqService;
    @Autowired
    private ZkClient zkClient;
    @Autowired
    private AppInfoService appInfoService;

    public void init() throws Exception {
        //先监听/skyeye/monitor/scroll节点,因为新增节点都是先进入这个节点的,因此先对他进行监听,比如先部署一个项目
        PathChildrenCache pathChildrenCache = new PathChildrenCache(curatorFramework, Constants.ROOT_PATH_EPHEMERAL, true);//对/skyeye/monitor/scroll进行监控
        pathChildrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        pathChildrenCache.getListenable().addListener(new ScrollChildrenChangeListener(this.rabbitmqService, this.zkClient, this.appInfoService));
    }
}
