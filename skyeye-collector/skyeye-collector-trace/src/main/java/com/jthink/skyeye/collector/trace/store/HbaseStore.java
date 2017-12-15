package com.jthink.skyeye.collector.trace.store;

import com.google.common.collect.Lists;
import com.jthink.skyeye.base.constant.Constants;
import com.jthink.skyeye.base.dapper.*;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc span 相关的信息存入hbase
 * @date 2017-02-17 09:47:42
 */
@Component
public class HbaseStore implements Store {

    private static final Logger LOGGER = LoggerFactory.getLogger(HbaseStore.class);

    /**
     * 返回值key是要保存的hbase的表名,value是该表要保存的数据集合
     */
    @Override
    public Map<String, List<Put>> store(String spanJson, Span span) {
        // 将所有的Put返回到上游
        Map<String, List<Put>> puts = new HashMap<String, List<Put>>();
        if (span.getSample()) {
            // 区分出所有的annotation----将List转换成Map,key是Annotation的type类型,比如是cs还是ss等
            Map<String, Annotation> annotationMap = this.distinguishAnnotation(span.getAnnotations());
            Put spanPut = this.storeSpan(span, spanJson, annotationMap);//trace表:
            Put tracePut = this.storeTrace(span, annotationMap);//索引表time_consume, 保存每个trace耗时
            List<Put> annotationPuts = this.storeAnnotation(span, annotationMap);//该方法可以通过某个服务+某个异常+时间范围,可以筛选出此时出现了哪些异常,并且通过rowkey是traceId,可以找到某一个traceId出现过的异常

            puts.put(Constants.TABLE_TRACE, Lists.newArrayList(spanPut));//trace
            if (null != tracePut) {
                puts.put(Constants.TABLE_TIME_CONSUME, Lists.newArrayList(tracePut));//time_consume
            }
            if (null != annotationPuts) {
                puts.put(Constants.TABLE_ANNOTATION, annotationPuts);//annotation
            }

        }
        return puts;
    }

    /**
     * 数据表trace, 保存一个跟踪链条的所有span信息
     * rowkey: traceId
     * columnFamily: span
     * qualifier: [spanId+c, spanId+s ...](有N个span就有N*2个, c/s表示是client还是server采集到的)
     * value: span json value
     * @param span
     * @param spanJson
     * @param annotationMap
     * @return
    1.trace表:
    rowkey是traceId,
    列族family是span,
    每一个列qualifier是spanId+c或者spanId+s,表示是客户端的spanId还是服务端的spanId
    value是具体的span内容组成的字符串

    一个请求调用为一个span,即一个span的内容分布在连续两个服务器上产生的,hbase上客户端产生一个span,服务器上产生一个相同spanid的span
     */
    @Override
    public Put storeSpan(Span span, String spanJson, Map<String, Annotation> annotationMap) {
        String traceId = span.getTraceId();
        String spanId = span.getId();
        if (annotationMap.containsKey(AnnotationType.CS.symbol())) {
            // 如果是client
            spanId += NodeProperty.C.symbol();
        } else {
            // 如果是server
            spanId += NodeProperty.S.symbol();
        }
        Put put = new Put(Bytes.toBytes(traceId));
        //向span列族添加spanId的列,列的值是span具体内容
        put.addColumn(Bytes.toBytes(Constants.TABLE_TRACE_COLUMN_FAMILY), Bytes.toBytes(spanId), Bytes.toBytes(spanJson));

        return put;
    }

    /**
     * 索引表time_consume, 保存每个trace耗时
     * rowkey: serviceId + cs时间
     * columnFamily: trace
     * qualifier: traceId ...
     * value: 整个调用链条耗时
     * @param span
     * @param annotationMap
     * @return
     *
    索引表time_consume, 保存每个trace耗时
    该方法保存每一个trace的整体耗时时间

    rowkey是接口+method+开始时间
    columnFamily列族是trace
    qualifier列名是由traceId组成
    value是该trace从开始到结束的用时
     */
    @Override
    public Put storeTrace(Span span, Map<String, Annotation> annotationMap) {
        if (null == span.getParentId() && annotationMap.containsKey(AnnotationType.CS.symbol())) {
            // 是root span, 并且是client端
            Annotation csAnnotation = annotationMap.get(AnnotationType.CS.symbol());//开始时间
            Annotation crAnnotation = annotationMap.get(AnnotationType.CR.symbol());//结束时间

            long consumeTime = crAnnotation.getTimestamp() - csAnnotation.getTimestamp();//消耗时间
            String rowKey = span.getServiceId() + Constants.UNDER_LINE + csAnnotation.getTimestamp();
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(Constants.TABLE_TIME_CONSUME_COLUMN_FAMILY), Bytes.toBytes(span.getTraceId()),
                    Bytes.toBytes(consumeTime));
            return put;
        }
        return null;
    }

    /**
     * 索引表annotation, 保存自定义的异常信息
     * rowkey: serviceId + ExceptionType + cs/sr时间
     * columnFamily: trace
     * qualifier: traceId ...
     * value: binaryAnnotation的value
     * @param span
     * @param annotationMap
     * @return
     * 该方法可以通过某个服务+某个异常+时间范围,可以筛选出此时出现了哪些异常,并且通过rowkey是traceId,可以找到某一个traceId出现过的异常
     *
    索引表annotation
    该方法可以通过某个服务+某个异常+时间范围,可以筛选出此时出现了哪些异常,
    该方法可以知道异常都是哪个traceId打印出来的

    rowkey是接口+method+_+异常类型+_+发生异常的时间点
    列族是trace,
    列是traceid,
    value是自定义的value信息,即异常信息内容
     */
    @Override
    public List<Put> storeAnnotation(Span span, Map<String, Annotation> annotationMap) {
        List<BinaryAnnotation> annotations = span.getBinaryAnnotations();
        if (null != annotations && annotations.size() != 0) {//说明该span有异常信息
            List<Put> puts = new ArrayList<Put>();
            // 如果有自定义异常
            for (BinaryAnnotation annotation : annotations) {
                String rowKey = span.getServiceId() + Constants.UNDER_LINE + annotation.getType()
                        + Constants.UNDER_LINE + this.getBinaryAnnotationTimestamp(annotationMap);
                //rowkey是接口+method+_+异常类型+_+发生异常的时间点
                Put put = new Put(Bytes.toBytes(rowKey));
                //列族是trace,列是traceid,value是自定义的value信息
                put.addColumn(Bytes.toBytes(Constants.TABLE_ANNOTATION_COLUMN_FAMILY), Bytes.toBytes(span.getTraceId()),
                        Bytes.toBytes(annotation.getValue() == null ? annotation.getType() : annotation.getValue()));
                puts.add(put);
            }
            return puts;
        }
        return null;
    }

    /**
     * 获取binaryAnnotation的发生时间戳，这里取的是客户端发送时间或者服务端接受时间（每个span要么是客户端，要么是服务端，这两个时间仅仅相差一个网络传输时间）
     * @param annotationMap
     * @return
     */
    private Long getBinaryAnnotationTimestamp(Map<String, Annotation> annotationMap) {
        Long timestamp = System.currentTimeMillis();
        if (annotationMap.containsKey(AnnotationType.CS.symbol())) {//发生异常的时间点
            // cs
            timestamp = annotationMap.get(AnnotationType.CS.symbol()).getTimestamp();
        }
        if (annotationMap.containsKey(AnnotationType.SR.symbol())) {
            // sr
            timestamp = annotationMap.get(AnnotationType.SR.symbol()).getTimestamp();
        }
        return timestamp;
    }


    /**
     * 区分出不同的annotation
     * @param annotations
     * @return
     * 将List转换成Map,key是Annotation的type类型,比如是cs还是ss等
     */
    private Map<String, Annotation> distinguishAnnotation(List<Annotation> annotations) {
        Map<String, Annotation> annotationMap = new HashMap<String, Annotation>();
        for (Annotation annotation : annotations) {
            annotationMap.put(annotation.getValue(), annotation);
        }
        return annotationMap;
    }

}
