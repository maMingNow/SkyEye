package com.jthink.skyeye.base.constant;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 日志类型
 * @date 2016-09-26 16:52:53
事件日志的设计---也是一个es的表,专门为事件日志设计的es表
1.正常日志
2.api项目,比如微服务,该项目为客户端开放
因此在该项目中的日志,想知道每一个客户端的耗时、成功失败次数。
3.调用中间件操作时候记录的日志,读/写中间件,比如hbase，redis等,我们想知道中间件的耗时
4.任务执行,定时任务,不管是mr还是spark还是等等,当发生失败的时候,打印一些日志
5.自定义的日志内容,框架只能把自定义的日志存储到hdfs上,通过backup 消费组完成,后续可以自己跑spark等任务提取日志
即关于自定义日志这块,框架只能完成收集的工作
6.rpc
7.第三方系统调用,
比方说有一个金融公司,想判断客户输入的卡号和姓名是否一致,但是该金融公司内部没有该数据库。
因此他需要对接其他公司项目,因此不能部署在公司内部,是外部的一个http请求,因此记录是否成功、耗时等情况。

 */
public enum EventType {

    normal(Constants.EVENT_TYPE_NORMAL, "正常入库日志"),
    invoke_interface(Constants.EVENT_TYPE_INVOKE_INTERFACE, "api调用"),
    middleware_opt(Constants.EVENT_TYPE_MIDDLEWARE_OPT, "中间件操作"),
    job_execute(Constants.EVENT_TYPE_JOB_EXECUTE, "job执行状态"),
    custom_log(Constants.EVENT_TYPE_CUSTOM_LOG, "自定义埋点日志"),
    rpc_trace(Constants.EVENT_TYPE_RPC_TRACE, "rpc trace跟踪日志"),
    thirdparty_call(Constants.EVENT_TYPE_THIRDPARTY_CALL, "第三方系统调用");

    private String symbol;

    private String label;

    private EventType(String symbol, String label) {
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