package com.jthink.skyeye.web.dto;

import com.jthink.skyeye.data.hbase.api.RowMapper;
import org.apache.hadoop.hbase.client.Result;

import java.util.Map;
import java.util.NavigableMap;

import static com.jthink.skyeye.base.constant.Constants.TABLE_ANNOTATION_COLUMN_FAMILY;


/**
 * @author Aiur
 * @version 0.0.1
 * @desc 对应 HBase annotation 表
 * @date 2017-03-30 15:15:06
 *
 */
public class TraceAnnotationDto {

    private String rowKey;//主键
    private String iface;//提供的接口+服务method
    private String method;//提供服务的method
    private String type;//异常类型
    private Long timestamp;//该备注的时间点
    private String traceId;//该备注属于哪个trace
    private String value;//该备注的具体内容


    @Override
    public String toString() {

        return rowKey + "\t" +
                iface + "\t" +
                method + "\t" +
                type + "\t" +
                timestamp + "\t" +
                traceId + "\t" +
                value;
    }

    public String getRowKey() {
        return rowKey;
    }

    public TraceAnnotationDto setRowKey(String rowKey) {
        this.rowKey = rowKey;
        return this;
    }

    public String getIface() {
        return iface;
    }

    public TraceAnnotationDto setIface(String iface) {
        this.iface = iface;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public TraceAnnotationDto setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getType() {
        return type;
    }

    public TraceAnnotationDto setType(String type) {
        this.type = type;
        return this;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public TraceAnnotationDto setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getTraceId() {
        return traceId;
    }

    public TraceAnnotationDto setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public String getValue() {
        return value;
    }

    public TraceAnnotationDto setValue(String value) {
        this.value = value;
        return this;
    }

    public static class TraceAnnotationMapper implements RowMapper<TraceAnnotationDto> {

        @Override
        public TraceAnnotationDto mapRow(Result res, int rowNum) throws Exception {
            String rowKey = new String(res.getRow());
            NavigableMap<byte[], byte[]> familyMap = res.getFamilyMap(TABLE_ANNOTATION_COLUMN_FAMILY.getBytes());


            String[] ss = rowKey.split("_");
            String iface = ss[0];
            String method = ss[1];
            String type = ss[2];
            long timestamp = Long.parseLong(ss[3]);

            String traceId = "", value = "";
            for (Map.Entry<byte[], byte[]> entry : familyMap.entrySet()) {//该时间点产生的所有事件
                traceId = new String(entry.getKey());
                value = new String(entry.getValue());
            }

            TraceAnnotationDto tad = new TraceAnnotationDto();
            tad.setRowKey(rowKey).setIface(iface).setMethod(method).setType(type).setTimestamp(timestamp);
            tad.setTraceId(traceId).setValue(value);
            return tad;
        }
    }

}
