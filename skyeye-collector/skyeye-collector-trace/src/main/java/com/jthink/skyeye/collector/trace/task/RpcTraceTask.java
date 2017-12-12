package com.jthink.skyeye.collector.trace.task;

import com.alibaba.fastjson.JSON;
import com.jthink.skyeye.base.constant.Constants;
import com.jthink.skyeye.base.constant.EventType;
import com.jthink.skyeye.base.dapper.Span;
import com.jthink.skyeye.base.dto.EventLog;
import com.jthink.skyeye.base.dto.LogDto;
import com.jthink.skyeye.base.dto.RpcTraceLog;
import com.jthink.skyeye.collector.core.callback.KafkaOffsetCommitCallback;
import com.jthink.skyeye.collector.core.configuration.kafka.KafkaProperties;
import com.jthink.skyeye.collector.core.task.Task;
import com.jthink.skyeye.collector.trace.cache.CacheService;
import com.jthink.skyeye.collector.trace.store.Store;
import com.jthink.skyeye.data.hbase.api.HbaseTemplate;
import com.jthink.skyeye.data.jpa.domain.ServiceInfo;
import com.jthink.skyeye.data.jpa.pk.ServiceInfoPK;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc rpc trace 追踪的task, 将kafka中的rpc trace跟踪的记录保存到数据库
 * @date 2017-02-20 16:27:15
 */
@Component
public class RpcTraceTask implements Task {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcTraceTask.class);

    @Autowired
    private KafkaConsumer kafkaConsumer;
    @Autowired
    private KafkaProperties kafkaProperties;
    @Autowired
    private Store hbaseStore;
    @Autowired
    private HbaseTemplate hbaseTemplate;
    @Autowired
    private CacheService cacheService;

    public static Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<TopicPartition, OffsetAndMetadata>();
    private Thread thread;

    @Override
    public void run() {
        this.doTask();
    }

    @Override
    public void doTask() {
        this.thread = Thread.currentThread();
        int count = 0;
        try {
            while (true) {
                ConsumerRecords<byte[], String> records = this.kafkaConsumer.poll(this.kafkaProperties.getPollTimeout());
                if (!records.isEmpty()) {
                    List<Mutation> spanPuts = new ArrayList<Mutation>();//trace 这个key存在
                    List<Mutation> annotationPuts = new ArrayList<Mutation>();//annotation这个key存在
                    List<Mutation> tracePuts = new ArrayList<Mutation>();//time_consume这个key存在
                    for (ConsumerRecord<byte[], String> record : records) {
                        String value = record.value();
                        LogDto logDto = this.getLogDto(value);
                        if (logDto != null) {
                            String logValue = logDto.getMessageMax();//用户输入的日志内容
                            String type = EventLog.parseEventType(logValue).symbol();
                            if (type.equals(EventType.rpc_trace.symbol())) {//该事件是rpc事件
                                // 如果是rpc trace日志
                                RpcTraceLog log = RpcTraceLog.parseRpcTraceLog(logValue);
                                String logContent = log.getLog();//具体rpc日志内容
                                // 将span的json字符串转换成Span对象
                                Span span = JSON.parseObject(logContent, Span.class);

                                // 采集iface和method
                                String serviceId = span.getServiceId();
                                ServiceInfo serviceInfo = this.buildServiceInfo(serviceId);//构建一个服务对象,即接口+服务方法组成的对象
                                if (null != serviceInfo) {
                                    if (!this.cacheService.isExists(CacheService.SERVICE_INFO_TYPE, serviceInfo.getSid())) {//查看该redis的事件key是否包含了该接口+方法组成的服务
                                        // 如果api不存在
                                        LOGGER.info("从rpc trace中采集到service, 为: {}", serviceId);
                                        this.cacheService.save(serviceInfo);//保存到数据库
                                        this.cacheService.add(CacheService.SERVICE_INFO_TYPE, serviceInfo.getSid());//保存到redis
                                    }
                                }

                                // 存储进入hbase
                                Map<String, List<Put>> puts = this.hbaseStore.store(logContent, span);
                                if (puts.containsKey(Constants.TABLE_TRACE)) {//trace 这个key存在
                                    spanPuts.addAll(puts.get(Constants.TABLE_TRACE));
                                }
                                if (puts.containsKey(Constants.TABLE_TIME_CONSUME)) {//time_consume这个key存在
                                    tracePuts.addAll(puts.get(Constants.TABLE_TIME_CONSUME));
                                }
                                if (puts.containsKey(Constants.TABLE_ANNOTATION)) {//annotation这个key存在
                                    annotationPuts.addAll(puts.get(Constants.TABLE_ANNOTATION));
                                }
                            }
                        } else {
                            LOGGER.info("record transform error, {}", value);
                        }

                        currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));

                        count++;
                        if (count >= 1000) {
                            // 当达到了1000触发向kafka提交offset
                            kafkaConsumer.commitAsync(currentOffsets, new KafkaOffsetCommitCallback());
                            count = 0;
                        }
                    }

                    // 存储入hbase  三个表内存储数据
                    this.storeToHbase(Constants.TABLE_TRACE, spanPuts);
                    this.storeToHbase(Constants.TABLE_TIME_CONSUME, tracePuts);
                    this.storeToHbase(Constants.TABLE_ANNOTATION, annotationPuts);

                    kafkaConsumer.commitAsync(currentOffsets, new KafkaOffsetCommitCallback());
                    LOGGER.info("processed {} records, " +
                            "{} span records stored in hbase table trace, " +
                            "{} annotation records stored in hbase table annotation, " +
                            "{} time_consume records in hbase table time_consume",
                            records.count(), spanPuts.size(), annotationPuts.size(), tracePuts.size());
                }
            }
        } catch (WakeupException e) {
            // do not process, this is shutdown
            LOGGER.error("wakeup, start to shutdown, {}", e);
        } catch (Exception e) {
            LOGGER.error("process records error, {}", e);
        } finally {
            kafkaConsumer.commitSync(currentOffsets);
            LOGGER.info("finally commit the offset");
            // 不需要主动调kafkaConsumer.close(), spring bean容器会调用
        }
    }

    /**
     * 构造ServiceInfo
     * @param serviceId 接口_方法组成的字符串
     * @return
     */
    private ServiceInfo buildServiceInfo(String serviceId) {
        String[] detail = serviceId.split(Constants.UNDER_LINE);//_拆分
        if (detail.length == 2) {
            ServiceInfoPK serviceInfoPK = new ServiceInfoPK();
            serviceInfoPK.setIface(detail[0]).setMethod(detail[1]);
            ServiceInfo serviceInfo = new ServiceInfo();
            return serviceInfo.setServiceInfoPK(serviceInfoPK).setSid(serviceId);
        }
        return null;
    }

    /**
     * 存储进入hbase
     * @param table
     * @param puts
     */
    private void storeToHbase(String table, List<Mutation> puts) throws IOException {
        if (null != puts && puts.size() != 0) {
            this.hbaseTemplate.saveOrUpdates(table, puts);
        }
    }

    @Override
    public Thread executeThread() {
        return this.thread;
    }

    /**
     * 根据line构造并返回LogDto
     * @param line
     * @return
     */
    private LogDto getLogDto(String line) {
        try {
            LogDto logDto = new LogDto(line);
            return logDto;
        } catch (Exception e) {
            return null;
        }
    }

}
