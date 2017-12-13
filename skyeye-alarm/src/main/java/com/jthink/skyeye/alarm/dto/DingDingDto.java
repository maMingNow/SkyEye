package com.jthink.skyeye.alarm.dto;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 钉钉报警dto
 * @date 2017-09-29 17:32:33
 */
public class DingDingDto {

    private String msgtype = "text";

    private Text text;//叮叮的信息内容

    private At at;//是否@人

    public String getMsgtype() {
        return msgtype;
    }

    public DingDingDto setMsgtype(String msgtype) {
        this.msgtype = msgtype;
        return this;
    }

    public Text getText() {
        return text;
    }

    public DingDingDto setText(Text text) {
        this.text = text;
        return this;
    }

    public At getAt() {
        return at;
    }

    public DingDingDto setAt(At at) {
        this.at = at;
        return this;
    }

    //叮叮内容
    public static class Text {
        private String content;

        public Text() {
        }

        public Text(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public Text setContent(String content) {
            this.content = content;
            return this;
        }
    }

    //是否@全体人员
    public static class At {
        private Boolean isAtAll;

        public At() {
        }

        public At(boolean isAtAll) {
            this.isAtAll = isAtAll;
        }

        public Boolean getIsAtAll() {
            return isAtAll;
        }

        public At setIsAtAll(Boolean atAll) {
            isAtAll = atAll;
            return this;
        }
    }
}
