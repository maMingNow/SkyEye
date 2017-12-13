package com.jthink.skyeye.data.hbase.api;

import org.apache.hadoop.hbase.client.Result;

/**
 * Callback for mapping rows of a {@link ResultScanner} on a per-row basis.
 * Implementations of this interface perform the actual work of mapping each row to a result object, but don't need to worry about exception handling.
 *
 * @author Costin Leau
 */
/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc copy from spring data hadoop hbase, modified by JThink, use the 1.0.0 api
 * @date 2016-11-15 15:42:46
 * 该接口用于操作hbase的scan表对象,即一行数据进行解析成一个对象
 */
public interface RowMapper<T> {

    /**
     * 返回一个结果,该结果是一行数据
     * @param result 该数据结果对应的hbase的原始数据
     * @param rowNum 该结果是第几行数据
     */
    T mapRow(Result result, int rowNum) throws Exception;
}
