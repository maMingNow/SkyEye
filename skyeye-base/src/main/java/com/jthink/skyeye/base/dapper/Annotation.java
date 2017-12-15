package com.jthink.skyeye.base.dapper;

import java.io.Serializable;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc annotation定义
 * @date 2016-11-04 17:31:55
 */
public class Annotation implements Serializable {

    // 时间戳
    private Long timestamp;//什么时间点发生的该事件
    // annotation的value, 取自AnnotationType的symbol
    private String value;//此时事件的类型
    // endpoint
    private EndPoint endPoint;//哪个节点发生的该事件

    public Annotation() {

    }

    public Annotation(Long timestamp, String value, EndPoint endPoint) {
        this.timestamp = timestamp;
        this.value = value;
        this.endPoint = endPoint;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "timestamp=" + timestamp +
                ", value='" + value + '\'' +
                ", endPoint=" + endPoint +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Annotation that = (Annotation) o;

        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        return endPoint != null ? endPoint.equals(that.endPoint) : that.endPoint == null;

    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (endPoint != null ? endPoint.hashCode() : 0);
        return result;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public EndPoint getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(EndPoint endPoint) {
        this.endPoint = endPoint;
    }
}
