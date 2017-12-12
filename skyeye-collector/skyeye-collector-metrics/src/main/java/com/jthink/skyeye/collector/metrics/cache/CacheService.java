package com.jthink.skyeye.collector.metrics.cache;

import com.jthink.skyeye.base.constant.Constants;
import com.jthink.skyeye.base.constant.NameInfoType;
import com.jthink.skyeye.data.jpa.domain.NameInfo;
import com.jthink.skyeye.data.jpa.domain.ServiceInfo;
import com.jthink.skyeye.data.jpa.repository.NameInfoRepository;
import com.jthink.skyeye.data.jpa.repository.ServiceInfoRepository;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 缓存名称采集相关的数据
 * @date 2016-11-22 09:23:19
 */
@Service
public class CacheService implements InitializingBean {

    private static Logger LOGGER = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private NameInfoRepository nameInfoRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;
    private SetOperations<String, String> setOps;//redis里面封装set的方法

    private static final String CONFIG_PREFIX = "jthink_monitor_collector";
    private static final String API_NAME_PREFIX = "jthink_monitor_collector_api_name";
    private static final String ACCOUNT_NAME_PREFIX = "jthink_monitor_collector_account_name";
    private static final String THIRD_NAME_PREFIX = "jthink_monitor_collector_third_name";

    //每一个日志类型作为key,value是该数据类型在redis里面的key
    private static final Map<String, String> mapping = new HashMap<String, String>() {
        {
            put(NameInfoType.API.symbol(), API_NAME_PREFIX);
            put(NameInfoType.ACCOUNT.symbol(), ACCOUNT_NAME_PREFIX);
            put(NameInfoType.THIRD.symbol(), THIRD_NAME_PREFIX);
        }
    };

    /**
     * 保存到数据库
     * @param nameInfo
     */
    public void save(NameInfo nameInfo) {
        this.nameInfoRepository.save(nameInfo);
    }

    /**
     * 保存到redis
     * 根据采集的类型和值存入redis
     * @param type 根据事件类型找到reids的key
     * @param value 将redis添加该value
     * @return
     */
    public void add(String type, String value) {
        this.setOps.add(mapping.get(type), value);
    }

    /**
     * 根据采集的类型和值判断是否存在
     *
     * @param type 日志类型
     * @param value set集合中是否有该value
     * @return
     */
    public boolean isExists(String type, String value) {
        return this.setOps.isMember(mapping.get(type), value);
    }

    /**
     * 将数据库中的配置表进行缓存---加入到redis中
     */
    private void loadCache() {
        StopWatch sw = new StopWatch();
        sw.start();
        LOGGER.info("start load config to cache");

        Iterable<NameInfo> nameInfos = this.nameInfoRepository.findAll();//查询数据库的全部数据

        for (Iterator<NameInfo> it = nameInfos.iterator(); it.hasNext();) {
            NameInfo nameInfo = it.next();
            this.setOps.add(mapping.get(nameInfo.getNameInfoPK().getType()), nameInfo.getNameInfoPK().getName());//key是通过事件类型去查询redis的key
        }

        sw.stop();
        LOGGER.info("load config to cache end, cost {} ms", sw.getTime());
    }

    /**
     * 将redis中的配置信息清除
     */
    private void clearCache() {
        StopWatch sw = new StopWatch();
        sw.start();
        LOGGER.info("start clear config cache");

        Set<String> keys = this.redisTemplate.keys(CONFIG_PREFIX + Constants.XING_HAO);//jthink_monitor_collector *,即查获jthink_monitor_collector开头的redis的key集合
        this.redisTemplate.delete(keys);//删除缓存redis里面的这些key对应的数据

        sw.stop();
        LOGGER.info("clear config cache end, cost {} ms", sw.getTime());
    }

    /**
     * 缓存数据初始化
     */
    public void load() {
        this.clearCache();
        this.loadCache();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.setOps = this.redisTemplate.opsForSet();
        this.load();
    }
}
