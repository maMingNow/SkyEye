package com.jthink.skyeye.monitor.listener;

import com.jthink.skyeye.data.rabbitmq.service.RabbitmqService;
import com.jthink.skyeye.monitor.service.AppInfoService;
import com.jthink.skyeye.monitor.service.CacheService;
import com.jthink.skyeye.base.constant.Constants;
import com.jthink.skyeye.base.constant.LogCollectionStatus;
import com.jthink.skyeye.base.dto.AlertDto;
import com.jthink.skyeye.base.dto.MailDto;
import com.jthink.skyeye.base.util.DateUtil;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 监听临时节点变化
 * @date 2016-09-23 11:33:05
 */
public class AppChildrenChangeListener implements PathChildrenCacheListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppChildrenChangeListener.class);

    private RabbitmqService rabbitmqService;

    private ZkClient zkClient;

    private AppInfoService appInfoService;

    public AppChildrenChangeListener(RabbitmqService rabbitmqService, ZkClient zkClient, AppInfoService appInfoService) {
        this.rabbitmqService = rabbitmqService;
        this.zkClient = zkClient;
        this.appInfoService = appInfoService;
    }

    //监听一个app的变化----对/skyeye/monitor/scroll/app进行监控
    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        String node = Constants.EMPTY_STR;
        String app = Constants.EMPTY_STR;
        String host = Constants.EMPTY_STR;
        String info = Constants.EMPTY_STR;
        String[] datas = null;
        switch (event.getType()) {
            case CHILD_ADDED://说明增加了一个host
                node = event.getData().getPath();//具体的路径 /skyeye/monitor/scroll/app/host
                app = this.getApp(node);
                host = this.getHost(node);
                if (!CacheService.appHosts.contains(node)) {//说明该host是第一次加入
                    datas = this.zkClient.readData(Constants.ROOT_PATH_PERSISTENT + Constants.SLASH + app + Constants.SLASH + host).toString().split(Constants.SEMICOLON); //获取/skyeye/monitor/query/app/host下面的数据

                    info = this.buildMsg(DateUtil.format(new Date(System.currentTimeMillis()), DateUtil.YYYYMMDDHHMMSS), app,
                            this.getHost(node), datas[1], Constants.APP_START);

                    // add to the queue
                    this.rabbitmqService.sendMessage(info, datas[0]);//发送信息,说明新增了一个app,datas[0]是邮箱地址
                    LOGGER.info(info);
                    CacheService.appHosts.add(node);//添加一个该host节点进入缓存
                    this.appInfoService.add(host, app, Constants.ZK_NODE_TYPE_EPHEMERAL, this.calLogCollectionStatus(app, host));//存储mysql一个临时节点,说明该app现在还活着
                }
                this.appInfoService.add(host, app, Constants.ZK_NODE_TYPE_PERSISTENT, LogCollectionStatus.HISTORY);//存储一个永久节点,即该app曾经部署成功过
                break;
            case CHILD_REMOVED://说明一个host下线了
                node = event.getData().getPath();
                app = this.getApp(node);
                host = this.getHost(node);
                datas = this.zkClient.readData(Constants.ROOT_PATH_PERSISTENT + Constants.SLASH + app + Constants.SLASH + host).toString().split(Constants.SEMICOLON);

                info = this.buildMsg(DateUtil.format(new Date(System.currentTimeMillis()), DateUtil.YYYYMMDDHHMMSS), app,
                        this.getHost(node), datas[1], Constants.APP_STOP);//设置该app下线了

                // add to the queue
                this.rabbitmqService.sendMessage(info, datas[0]);
                LOGGER.info(info);
                if (CacheService.appHosts.contains(node)) {
                    CacheService.appHosts.remove(node);
                    this.appInfoService.delete(host, app, Constants.ZK_NODE_TYPE_EPHEMERAL);//在存储中删除该临时节点
                }
                break;
            case CHILD_UPDATED://说明一个host有变化
                node = event.getData().getPath();
                datas = this.zkClient.readData(node).toString().split(Constants.SEMICOLON);
                app = this.getApp(node);
                host = this.getHost(node);

                String detail = Constants.APP_APPENDER_STOP;//默认是该节点的kafka有问题了,暂时客户端失败了
                LogCollectionStatus status = LogCollectionStatus.STOPPED;

                if (datas[0].equals(Constants.APP_APPENDER_RESTART_KEY)) {//说明该节点的kafka已经好了,客户端已经恢复正常
                    // 如果是kafka appender restart
                    detail = Constants.APP_APPENDER_RESTART;
                    status = LogCollectionStatus.RUNNING;
                }

                //构建一个信息
                info = this.buildMsg(DateUtil.format(new Date(Long.parseLong(datas[1])), DateUtil.YYYYMMDDHHMMSS), app,
                        this.getHost(node), datas[2], detail);

                // add to the queue
                this.rabbitmqService.sendMessage(info, this.zkClient.readData(Constants.ROOT_PATH_PERSISTENT + Constants.SLASH + app + Constants.SLASH + host).toString().split(Constants.SEMICOLON)[0]);
                LOGGER.info(info);
                this.appInfoService.update(host, app, Constants.ZK_NODE_TYPE_EPHEMERAL, status);//更新app在host上的新的状态
                break;
        }
    }

    /**
     * 根据node获取app
     * @param node
     * @return
     */
    private String getApp(String node) {
        String tmp = node.substring(0, node.lastIndexOf(Constants.SLASH));
        return this.getLast(tmp);
    }

    /**
     * 根据node获取host
     * @param node
     * @return
     */
    private String getHost(String node) {
        return this.getLast(node);
    }

    /**
     * 返回末尾字符串
     * @param line
     * @return
     */
    private String getLast(String line) {
        return line.substring(line.lastIndexOf(Constants.SLASH) + 1);
    }

    /**
     * 构造报警msg
     * @param time
     * @param app
     * @param host
     * @param deploy
     * @param msg
     * @return
     */
    private String buildMsg(String time, String app, String host, String deploy, String msg) {
        AlertDto alertDto = new AlertDto(time, app, host, deploy, msg);
        return alertDto.toString();
    }

    /**
     * 根据app和host计算LogCollectionStatus
     * @param app
     * @param host
     * @return 查看该app是否还运行中
     */
    public LogCollectionStatus calLogCollectionStatus(String app, String host) {
        String[] datas = this.zkClient.readData(Constants.ROOT_PATH_EPHEMERAL + Constants.SLASH + app + Constants.SLASH + host).toString().split(Constants.SEMICOLON);
        if (datas[0].equals(Constants.APPENDER_INIT_DATA) || datas[0].equals(Constants.APP_APPENDER_RESTART_KEY)) {
            return LogCollectionStatus.RUNNING;
        }
        return LogCollectionStatus.STOPPED;
    }
}
