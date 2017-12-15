package com.jthink.skyeye.web.constant;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc es sql template替换的key
 * @date 2016-12-02 15:18:10
 *
 * 该类表示es中sqk模版定义的关键字,这些关键字可以被真实值替换掉
 */
public class EsSqlTemplate {

    public static final String EVENTTYPE = "EVENTTYPE";//事件类型

    public static final String UNIQUENAME = "UNIQUENAME";//唯一的服务名字

    public static final String TIME = "TIME";//查询的时间

    public static final String BEGIN = "BEGIN";//查询范围的开始时间

    public static final String END = "END";//查询范围的结束时间

    public static final String SCOPE = "SCOPE";

    public static final String COST = "COST";

    public static final String DATEFIELD = "DATEFIELD";//事件查询的字段是哪个,比如是day还是time等等
}
