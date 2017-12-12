package com.jthink.skyeye.client.core.register;

import com.jthink.skyeye.base.constant.Constants;
import com.jthink.skyeye.base.constant.RpcType;
import com.jthink.skyeye.client.core.constant.NodeMode;
import com.jthink.skyeye.client.core.util.SysUtil;
import com.jthink.skyeye.trace.core.dto.RegisterDto;
import com.jthink.skyeye.trace.core.generater.IncrementIdGen;
import com.jthink.skyeye.trace.core.registry.Registry;
import com.jthink.skyeye.trace.core.registry.ZookeeperRegistry;
import org.I0Itec.zkclient.ZkClient;

import java.util.ArrayList;
import java.util.List;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc zookeeper注册中心
 * @date 2016-09-21 14:03:46
 */
public class ZkRegister {

    // zkClient
    private ZkClient client;

    public ZkRegister(ZkClient client) {
        this.client = client;
    }

    /**
     * 该方法不是线程安全的
     * 对app进行编号，如果一个host上部署了一个app的多个实例，那么对app原生名字进行编号，第一个app后面加#01，第二个后面加#02，以此类推
     * @param app
     * @param host
     * @return 返回编号,即app#number
     * 表示该app在host节点上存在
     * /app#01
     *      /host1
     *      /host2
     * /app#02
     *      /host1
     *      /host2
     * 上面表示每一个节点部署了2个app实例
     */
    public String mark(String app, String host) {
        // 计算编号
        List<String> apps = this.client.getChildren(Constants.ROOT_PATH_EPHEMERAL);
        List<String> marks = new ArrayList<>();
        int max = 0;//在host上一共部署了多少个app
        for (String a : apps) {
            if (a.startsWith(app)) {
                // 获取当前app
                List<String> hosts = this.client.getChildren(Constants.ROOT_PATH_EPHEMERAL + Constants.SLASH + a);
                if (hosts.contains(host)) {
                    String[] am = a.split(Constants.JING_HAO);//按照#拆分---app#01
                    marks.add(am[1]);
                    int mark = Integer.parseInt(am[1]);
                    if (max < mark) {
                        max = mark;
                    }
                }
            }
        }

        // 如果当前没有坑可以补
        if (max == marks.size()) {
            return app + Constants.JING_HAO + String.format("%02d", max + 1);
        }

        // 如果需要补坑，直接补
        for (int i = 1; i <= max; ++i) {
            String mark = String.format("%02d", i);
            if (!marks.contains(mark)) {
                return app + Constants.JING_HAO + mark;
            }
        }
        return app;
    }

    /**
     * 向注册中心注册节点信息
     * 注册哪个节点,有一个app,邮件通知人是谁---这些信息都是客户端在配置log4j的时候配置上的
     * @param host
     * @param app
     * @param mail
     */
    public void registerNode(String host, String app, String mail) {
        // 注册永久节点用于历史日志查询
        this.create(Constants.SLASH + app + Constants.SLASH + host, NodeMode.PERSISTENT);
        ///skyeye/monitor/query
        this.getClient().writeData(Constants.ROOT_PATH_PERSISTENT + Constants.SLASH + app + Constants.SLASH + host,
                mail + Constants.SEMICOLON + SysUtil.userDir);//在/app/host下面写入数据 mail:项目路径
        // 注册临时节点用于日志滚屏
        ///skyeye/monitor/scroll
        this.getClient().createPersistent(Constants.ROOT_PATH_EPHEMERAL + Constants.SLASH + app, true);//app是永久节点,但是下面的host是临时节点.因此可以知道谁还在活着
        this.create(Constants.SLASH + app + Constants.SLASH + host, NodeMode.EPHEMERAL,
                Constants.APPENDER_INIT_DATA + Constants.SEMICOLON + SysUtil.userDir);// /app/host上创建临时目录,写入数据是 appender_init_data : 项目目录
    }

    /**
     * rpc trace注册中心
     * @param host
     * @param app
     * @param rpc
     */
    public void registerRpc(String host, String app, String rpc) {
        if (!rpc.equals(RpcType.none.symbol())) {
            RegisterDto dto = new RegisterDto(app, host, this.client);
            Registry registry = new ZookeeperRegistry();
            IncrementIdGen.setId(registry.register(dto));
        }
    }

    /**
     * 创建节点
     * @param path
     * @param nodeMode
     */
    private void create(String path, NodeMode nodeMode) {
        if (nodeMode.symbol().equals(NodeMode.PERSISTENT.symbol())) {
            // 创建永久节点
            this.client.createPersistent(nodeMode.label() + path, true);//在label的根节点下创建一个path
        } else if (nodeMode.symbol().equals(NodeMode.EPHEMERAL.symbol())) {
            // 创建临时节点
            this.client.createEphemeral(nodeMode.label() + path);
        }
    }

    /**
     * 创建带data的节点
     * @param path
     * @param nodeMode
     * @param data
     */
    private void create(String path, NodeMode nodeMode, String data) {
        if (nodeMode.symbol().equals(NodeMode.PERSISTENT.symbol())) {
            // 创建永久节点，加入数据
            this.client.createPersistent(nodeMode.label() + path, true);
        } else if (nodeMode.symbol().equals(NodeMode.EPHEMERAL.symbol())) {
            // 创建临时节点，加入数据
            this.client.createEphemeral(nodeMode.label() + path, data);
        }
    }

    /**
     * 写节点数据
     * @param path
     * @param nodeMode
     * @param data
     */
    public void write(String path, NodeMode nodeMode, String data) {
        if (nodeMode.symbol().equals(NodeMode.PERSISTENT.symbol())) {
            // 创建永久节点，加入数据
            this.client.writeData(nodeMode.label() + path, true);
        } else if (nodeMode.symbol().equals(NodeMode.EPHEMERAL.symbol())) {
            // 创建临时节点，加入数据
            this.client.writeData(nodeMode.label() + path, data);
        }
    }

    public ZkClient getClient() {
        return client;
    }

    public void setClient(ZkClient client) {
        this.client = client;
    }
}
