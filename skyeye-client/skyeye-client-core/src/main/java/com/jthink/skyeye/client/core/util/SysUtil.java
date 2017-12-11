package com.jthink.skyeye.client.core.util;

import com.jthink.skyeye.base.constant.Constants;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 系统相关的util
 * @date 2016-09-27 19:38:39
 */
public class SysUtil {

    public static String host = Constants.EMPTY_STR;//本地节点的host名字
    public static String userDir = Constants.EMPTY_STR;//本地的工作目录

    static {
        if (System.getenv("COMPUTERNAME") != null) {//自定义
            host = System.getenv("COMPUTERNAME");
        } else {
            try {
                host = (InetAddress.getLocalHost()).getHostName();
            } catch (UnknownHostException uhe) {
                String host = uhe.getMessage();
                if (host != null) {
                    int colon = host.indexOf(':');
                    if (colon > 0) {
                        host = host.substring(0, colon);
                    }
                }
                host = "UnknownHost";
            }
        }

        userDir = System.getProperty("user.dir", "<NA>");
    }

}
