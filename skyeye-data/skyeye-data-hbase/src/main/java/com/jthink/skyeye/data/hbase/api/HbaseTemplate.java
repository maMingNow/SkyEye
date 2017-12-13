package com.jthink.skyeye.data.hbase.api;

import com.jthink.skyeye.base.constant.EventType;
import com.jthink.skyeye.base.constant.MiddleWare;
import com.jthink.skyeye.base.dto.EventLog;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Central class for accessing the HBase API. Simplifies the use of HBase and helps to avoid common errors.
 * It executes core HBase workflow, leaving application code to invoke actions and extract results.
 *
 * @author Costin Leau
 * @author Shaun Elliott
 */
/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc copy from spring data hadoop hbase, modified by JThink, use the 1.0.0 api
 * @date 2016-11-15 15:42:46
 */
public class HbaseTemplate implements HbaseOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(HbaseTemplate.class);

    private Configuration configuration;//hadoop的配置对象

    private volatile Connection connection;//hbase的一个连接

    public HbaseTemplate(Configuration configuration) {
        this.setConfiguration(configuration);
        Assert.notNull(configuration, " a valid configuration is required");
    }

    //对某个hbase的表进行一个操作,具体操作在action实现类中实现
    @Override
    public <T> T execute(String tableName, TableCallback<T> action) {
        Assert.notNull(action, "Callback object must not be null");
        Assert.notNull(tableName, "No table specified");

        StopWatch sw = new StopWatch();//开始计时
        sw.start();
        String status = EventLog.MONITOR_STATUS_SUCCESS;
        Table table = null;
        try {
            table = this.getConnection().getTable(TableName.valueOf(tableName));//返回一个hbase的表对象
            return action.doInTable(table);//对该表进行操作
        } catch (Throwable throwable) {
            status = EventLog.MONITOR_STATUS_FAILED;//执行失败
            throw new HbaseSystemException(throwable);
        } finally {
            if (null != table) {
                try {
                    table.close();
                    sw.stop();
                    LOGGER.info(EventLog.buildEventLog(EventType.middleware_opt, MiddleWare.HBASE.symbol(), sw.getTotalTimeMillis(), status, "hbase请求").toString());
                    //此时打印一个中间件日志
                } catch (IOException e) {
                    LOGGER.error("hbase资源释放失败");
                }
            }
        }
    }

    @Override
    public <T> List<T> find(String tableName, String family, final RowMapper<T> action) {
        Scan scan = new Scan();
        scan.setCaching(5000);
        scan.addFamily(Bytes.toBytes(family));//记录查询哪些family
        return this.find(tableName, scan, action);
    }

    @Override
    public <T> List<T> find(String tableName, String family, String qualifier, final RowMapper<T> action) {
        Scan scan = new Scan();
        scan.setCaching(5000);
        scan.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));//对hbase要过滤的列进行过滤
        return this.find(tableName, scan, action);
    }

    //查询一个List结果
    @Override
    public <T> List<T> find(String tableName, final Scan scan, final RowMapper<T> action) {
        return this.execute(tableName, new TableCallback<List<T>>() {//返回值处理成一个List
            @Override
            public List<T> doInTable(Table table) throws Throwable {
                int caching = scan.getCaching();
                // 如果caching未设置(默认是1)，将默认配置成5000
                if (caching == 1) {
                    scan.setCaching(5000);
                }
                ResultScanner scanner = table.getScanner(scan);//对表进行scan处理
                try {
                    List<T> rs = new ArrayList<T>();
                    int rowNum = 0;
                    for (Result result : scanner) {//循环每一个结果集
                        rs.add(action.mapRow(result, rowNum++));//对每一个结果进行封装
                    }
                    return rs;
                } finally {
                    scanner.close();
                }
            }
        });
    }

    //查询一行数据
    @Override
    public <T> T get(String tableName, String rowName, final RowMapper<T> mapper) {
        return this.get(tableName, rowName, null, null, mapper);
    }

    @Override
    public <T> T get(String tableName, String rowName, String familyName, final RowMapper<T> mapper) {
        return this.get(tableName, rowName, familyName, null, mapper);
    }

    /**
     * @param tableName
     * @param rowName 属于rowkey
     * @param mapper 如何解析该行数据
     */
    @Override
    public <T> T get(String tableName, final String rowName, final String familyName, final String qualifier, final RowMapper<T> mapper) {
        return this.execute(tableName, new TableCallback<T>() {
            @Override
            public T doInTable(Table table) throws Throwable {
                Get get = new Get(Bytes.toBytes(rowName));
                if (StringUtils.isNotBlank(familyName)) {
                    byte[] family = Bytes.toBytes(familyName);
                    if (StringUtils.isNotBlank(qualifier)) {
                        get.addColumn(family, Bytes.toBytes(qualifier));
                    }
                    else {
                        get.addFamily(family);
                    }
                }
                Result result = table.get(get);
                return mapper.mapRow(result, 0);
            }
        });
    }

    //对一行数据进行增删改查操作
    @Override
    public void execute(String tableName, MutatorCallback action) {
        Assert.notNull(action, "Callback object must not be null");
        Assert.notNull(tableName, "No table specified");

        StopWatch sw = new StopWatch();
        sw.start();
        String status = EventLog.MONITOR_STATUS_SUCCESS;
        BufferedMutator mutator = null;
        try {
            BufferedMutatorParams mutatorParams = new BufferedMutatorParams(TableName.valueOf(tableName));
            mutator = this.getConnection().getBufferedMutator(mutatorParams.writeBufferSize(3 * 1024 * 1024));//创建该表的一个缓冲通道
            action.doInMutator(mutator);//缓冲通道作用于action,让action进行具体的处理
        } catch (Throwable throwable) {
            status = EventLog.MONITOR_STATUS_FAILED;
            throw new HbaseSystemException(throwable);
        } finally {
            if (null != mutator) {
                try {
                    mutator.flush();
                    mutator.close();
                    sw.stop();
                    LOGGER.info(EventLog.buildEventLog(EventType.middleware_opt, MiddleWare.HBASE.symbol(), sw.getTotalTimeMillis(), status, "hbase请求").toString());//记录埋点日志
                } catch (IOException e) {
                    LOGGER.error("hbase mutator资源释放失败");
                }
            }
        }
    }

    //参数是要更改的一条数据
    @Override
    public void saveOrUpdate(String tableName, final Mutation mutation) {
        this.execute(tableName, new MutatorCallback() {
            @Override
            public void doInMutator(BufferedMutator mutator) throws Throwable {
                mutator.mutate(mutation);//action持有缓冲通道mutator,在缓冲通道mutator里面进行mutation处理
            }
        });
    }

    @Override
    public void saveOrUpdates(String tableName, final List<Mutation> mutations) {
        this.execute(tableName, new MutatorCallback() {
            @Override
            public void doInMutator(BufferedMutator mutator) throws Throwable {
                mutator.mutate(mutations);
            }
        });
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        if (null == this.connection) {
            synchronized (this) {
                if (null == this.connection) {
                    try {
                        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(200, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
                        // init pool
                        poolExecutor.prestartCoreThread();
                        this.connection = ConnectionFactory.createConnection(configuration, poolExecutor);
                    } catch (IOException e) {
                        LOGGER.error("hbase connection资源池创建失败");
                    }
                }
            }
        }
        return this.connection;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
