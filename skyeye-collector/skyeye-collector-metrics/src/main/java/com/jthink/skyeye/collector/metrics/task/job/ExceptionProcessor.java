package com.jthink.skyeye.collector.metrics.task.job;

import com.jthink.skyeye.base.constant.Constants;
import com.jthink.skyeye.base.constant.EventType;
import com.jthink.skyeye.base.dto.AlertDto;
import com.jthink.skyeye.base.dto.ApiLog;
import com.jthink.skyeye.base.dto.EventLog;
import com.jthink.skyeye.base.dto.LogDto;
import com.jthink.skyeye.data.rabbitmq.service.RabbitmqService;
import org.I0Itec.zkclient.ZkClient;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 异常处理的job，包含第三方系统异常、任务调度异常
 * @date 2016-11-21 17:23:57
 *
 * 报警信息发送到队列中
 */
public class ExceptionProcessor extends Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionProcessor.class);

    private RabbitmqService rabbitmqService;

    private ZkClient zkClient;

    public ExceptionProcessor(List<EventType> types) {
        super(types);
    }

    @Override
    public void doJob(EventLog log, LogDto logDto, BulkRequestBuilder bulkRequest) {
        // 进行第三方异常和中间件的报警
        if (this.getTypes().indexOf(log.getEventType()) != -1) {
            if (log.getStatus().equals(EventLog.MONITOR_STATUS_FAILED)) {//说明是执行失败了
                // 状态为失败才做处理
                String type = log.getEventType().symbol();//日志类型
                //哪个节点,哪个应用产生的错误日志
                String host = logDto.getHost();
                String app = logDto.getApp();
                //获取该host--app下的数据内容,里面包含邮箱内容
                String[] datas = this.zkClient.readData(Constants.ROOT_PATH_PERSISTENT + Constants.SLASH + app + Constants.SLASH + host).toString().split(Constants.SEMICOLON);
                if (log instanceof ApiLog) {
                    ApiLog apiLog = (ApiLog) log;
                    //构建报警信息内容  ---- datas[1]表示该应用所在的服务器上部署的路径
                    String info = this.buildMsg(this.getTime(logDto.getDay(), logDto.getTime()), app, host, datas[1], this.buildInfo(type, apiLog.getUniqueName(), apiLog.getStatus()));
                    LOGGER.info("发送一条报警\n, {}", info);
                    this.rabbitmqService.sendMessage(info, datas[0]);//向datas[0]所在的邮件地址发送一个信息
                } else {
                    String info = this.buildMsg(this.getTime(logDto.getDay(), logDto.getTime()), app, host, datas[1], this.buildInfo(type, log.getUniqueName(), log.getStatus()));
                    LOGGER.info("发送一条报警\n, {}", info);
                    this.rabbitmqService.sendMessage(info, datas[0]);
                }
            }
        }

        // 进行后续的处理
        if (null != this.getNextJob()) {
            this.getNextJob().doJob(log, logDto, bulkRequest);
        }
    }

    /**
     * 构造具体的报警信息
     * @param type 日志事件类型
     * @param name 日志事件对应的唯一服务名字
     * @param status 成功、失败的状态
     * @return 报警信息
     */
    private String buildInfo(String type, String name, String status) {
        StringBuffer sb = new StringBuffer();
        String join = Constants.MONITOR_MAIL_INFO_EXEC; //execute
        if (type.equals(EventType.invoke_interface.symbol()) || type.equals(EventType.thirdparty_call.symbol())) {
            join = Constants.MONITOR_MAIL_INFO_CALL;//第三方调用 call
        } else if (type.equals(EventType.middleware_opt.symbol())) {//中间件
            join = Constants.MONITOR_MAIL_INFO_REQUEST;//request
        }
        //比如xxx服务 call 失败,即第三方服务 调用 失败
        sb.append(name).append(Constants.SPACE).append(join).append(Constants.SPACE).append(status);
        return sb.toString();
    }

    /**
     * 构造报警msg
     * @param time
     * @param app
     * @param host
     * @param deploy app部署在服务器的路径
     * @param msg
     * @return
     */
    private String buildMsg(String time, String app, String host, String deploy, String msg) {
        AlertDto alertDto = new AlertDto(time, app, host, deploy, msg);
        return alertDto.toString();
    }

    /**
     * 根据给定的day和time返回时间（2016-11-23 16:42:40）
     * @param day
     * @param time
     * @return
     */
    private String getTime(String day, String time) {
        return day + Constants.SPACE + time.substring(0, time.lastIndexOf(Constants.POINT));
    }

    public RabbitmqService getRabbitmqService() {
        return rabbitmqService;
    }

    public void setRabbitmqService(RabbitmqService rabbitmqService) {
        this.rabbitmqService = rabbitmqService;
    }

    public ZkClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
    }
}
