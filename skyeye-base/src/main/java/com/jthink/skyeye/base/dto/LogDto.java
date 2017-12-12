package com.jthink.skyeye.base.dto;


import com.jthink.skyeye.base.constant.Constants;

import java.util.Date;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc log dto
 * @date 2016-09-20 10:41:14
 */
public class LogDto {

    private String day;
    private String time;
    private String nanoTime;
    private Date created;
    private String app;
    private String host;
    private String thread;
    private String level;
    private String eventType;
    private String pack;
    private String clazz;
    private String line;//出问题的行号
    private String messageSmart;
    private String messageMax;

    public LogDto() {
    }

    public LogDto(String day, String time, String nanoTime, Date created, String app, String host, String thread, String level, String eventType, String pack, String clazz, String line, String messageSmart, String messageMax) {
        this.day = day;
        this.time = time;
        this.nanoTime = nanoTime;
        this.created = created;
        this.app = app;
        this.host = host;
        this.thread = thread;
        this.level = level;
        this.eventType = eventType;
        this.pack = pack;
        this.clazz = clazz;
        this.line = line;
        this.messageSmart = messageSmart;
        this.messageMax = messageMax;
    }

    //日志内容System.nanoTime() + Constants.SEMICOLON + 原始log格式的日志
    //具体参见log4j2客户端关于KafkaAppender的实现
    /**
     * 原始日志格式是:时间;app;host;调用日志的线程;日志级别;调用class的路径;出问题的代码行;代码中指定的日志内容+回车换行
     * pattern="%d{yyyy-MM-dd HH:mm:ss.SSS};${APP_NAME};${hostName};%t;%-5level;%logger{96};%line;%msg%n"
     * 具体参见客户端client项目中的readme.md里面的模版
     */

    public LogDto(String log) {
        String[] detail = log.split(Constants.SEMICOLON, 9);//按照;拆分
        String date = detail[1];
        this.day = date.substring(0, 10).trim();
        this.time = date.substring(11).trim();//HH:mm:ss.SSS
        this.nanoTime = detail[0].trim();
        this.created = new Date(System.currentTimeMillis());
        this.app = detail[2].trim();
        this.host = detail[3].trim();
        this.thread = detail[4].trim();
        this.level = detail[5].trim();
        String packClazz = detail[6];
        this.pack = Constants.EMPTY_STR;
        this.clazz = packClazz;
        if (packClazz.indexOf(Constants.POINT) != -1) {
            this.pack = packClazz.substring(0, packClazz.lastIndexOf(Constants.POINT));
            this.clazz = packClazz.substring(packClazz.lastIndexOf(Constants.POINT) + 1).trim();
        }
        this.line = detail[7].trim();
        String message = detail[8].trim();
        this.eventType = EventLog.parseEventType(message).symbol();//日志类型是通过输出的日志内容中前缀进行判断的,前缀一定是以|之前的就是日志类型
        this.messageSmart = message;
        this.messageMax = message;
    }

    @Override
    public String toString() {
        // 2016-10-11 19:35:29.636 [pool-5-thread-1] INFO com.xxx.xxx.xxx.xxx.xxxx[42]: xxx xxx
        StringBuffer sb = new StringBuffer();
        sb.append(this.day).append(Constants.SPACE).append(this.time).append(Constants.SPACE).append(this.nanoTime)
                .append(Constants.SPACE).append(Constants.LEFT_MIDDLE_BRAC).append(this.thread).append(Constants.RIGHT_MIDDLE_BRAC)
                .append(Constants.SPACE).append(this.level).append(Constants.SPACE).append(this.pack)
                .append(Constants.POINT).append(this.clazz).append(Constants.LEFT_MIDDLE_BRAC).append(this.line)
                .append(Constants.RIGHT_MIDDLE_BRAC).append(Constants.COLON).append(Constants.SPACE).append(this.messageMax);

        return sb.toString();
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNanoTime() {
        return nanoTime;
    }

    public void setNanoTime(String nanoTime) {
        this.nanoTime = nanoTime;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPack() {
        return pack;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getMessageSmart() {
        return messageSmart;
    }

    public void setMessageSmart(String messageSmart) {
        this.messageSmart = messageSmart;
    }

    public String getMessageMax() {
        return messageMax;
    }

    public void setMessageMax(String messageMax) {
        this.messageMax = messageMax;
    }
}