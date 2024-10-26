package com.taobao.top.analysis.worker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import javax.script.ScriptEngineManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.taobao.top.analysis.TopAnalysisConfig;
import com.taobao.top.analysis.data.Alias;
import com.taobao.top.analysis.data.ReportEntry;

/**
 * 
 * 分析日志的工作线程
 * 
 * @author fangweng
 * 
 */
public class LogJobWorker extends AbstractLogJobWorker {

    private static final Log logger = LogFactory.getLog(LogJobWorker.class);

    private static final Log perf_logger = LogFactory.getLog("performance");

    private String splitRegex = ",";

    private String resource;

    private TopAnalysisConfig config;

    /**
	 * @param 线程名
	 * @param 分析的资源类型
	 *            ：用http:,file:等开头代表协议头
	 * @param 分析资源的每一行的分隔符号
	 *            ，切割一条记录为多个列
	 * @param Entry定义池
	 * @param 父级Enrty定义池
	 * @param 结果池
	 * @param 别名池
	 * @param 与其他线程协同的计数器
	 * @param 出错计数器
	 */
    public LogJobWorker(String workerName, String resource, String splitRegex, Map<String, ReportEntry> entryPool, Map<String, ReportEntry> parentEntryPool, Map<String, Map<String, Object>> resultPool, Map<String, Alias> aliasPool, CountDownLatch countDownLatch, AtomicLong errorCounter, TopAnalysisConfig config) {
        this.workerName = workerName;
        this.entryPool = entryPool;
        this.parentEntryPool = parentEntryPool;
        this.resultPool = resultPool;
        this.aliasPool = aliasPool;
        manager = new ScriptEngineManager();
        engine = manager.getEngineByName("js");
        this.countDownLatch = countDownLatch;
        this.errorCounter = errorCounter;
        this.resource = resource;
        this.config = config;
        if (splitRegex != null && !"".equals(splitRegex)) {
            this.splitRegex = splitRegex;
            if (!config.getLogFileEncoding().equals("UTF-8")) {
                try {
                    this.splitRegex = new String(splitRegex.getBytes(), config.getLogFileEncoding());
                } catch (UnsupportedEncodingException e) {
                    logger.error(e, e);
                }
            }
        }
        if (logger.isInfoEnabled()) logger.info(new StringBuilder().append("Worker ").append(workerName).append(" start...").toString());
    }

    @Override
    public void init() {
    }

    @Override
    public void doJob() {
        BufferedReader reader = null;
        int line = 0;
        long size = 0;
        long consume = 0;
        long beg = System.currentTimeMillis();
        try {
            if (resource == null) {
                throw new java.lang.RuntimeException("jobWorker: " + workerName + " analysis resource is null");
            }
            if (resource.startsWith("file:")) {
                File file = new File(resource.substring("file:".length()));
                URL fileResource = null;
                if (!file.exists()) {
                    fileResource = ClassLoader.getSystemResource(resource.substring("file:".length()));
                    if (fileResource == null) throw new java.lang.RuntimeException("Job resource not exist,file : " + resource.substring("file:".length())); else logger.warn("load resource form classpath :" + fileResource.getFile());
                }
                if (fileResource == null) reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), config.getLogFileEncoding())); else reader = new BufferedReader(new InputStreamReader(fileResource.openStream(), config.getLogFileEncoding()));
            } else if (resource.startsWith("http:")) {
                URL url = new URL(resource);
                reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream(), config.getLogFileEncoding()));
            } else {
                throw new RuntimeException("resource must start with file: or http: ...");
            }
            String record;
            while ((record = reader.readLine()) != null) {
                if (record == null || "".equals(record)) continue;
                line += 1;
                size += record.length();
                String[] contents = record.split(splitRegex);
                Iterator<String> keys = entryPool.keySet().iterator();
                ReportEntry entry = null;
                List<ReportEntry> childEntrys = new ArrayList<ReportEntry>();
                Map<String, Object> valueTempPool = new HashMap<String, Object>();
                while (keys.hasNext()) {
                    try {
                        String key = keys.next();
                        entry = entryPool.get(key);
                        if (entry.isLazy()) continue;
                        if (entry.getParent() != null) {
                            childEntrys.add(entry);
                            continue;
                        }
                        process(entry, contents, valueTempPool);
                    } catch (Exception e) {
                        logger.error(new StringBuilder().append("Entry :").append(entry.getId()).append("\r\n record: ").append(record).toString(), e);
                        errorCounter.incrementAndGet();
                    }
                }
                ReportEntry reportEntry = null;
                for (Iterator<ReportEntry> iterator = childEntrys.iterator(); iterator.hasNext(); ) {
                    try {
                        reportEntry = iterator.next();
                        process(reportEntry, contents, valueTempPool);
                    } catch (Exception e) {
                        logger.error(new StringBuilder().append("Entry :").append(reportEntry.getId()).append("\r\n record: ").append(record).toString(), e);
                        errorCounter.incrementAndGet();
                    }
                }
            }
            if (line == 0) logger.error("there are no validate lines in this file..."); else logger.error(new StringBuilder("worker ").append(workerName).append(" process line count: ").append(line));
        } catch (Exception ex) {
            handleError(ex, null);
            this.setSuccess(false);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                    reader = null;
                } catch (Exception ex) {
                    handleError(ex, null);
                }
            }
            consume = System.currentTimeMillis() - beg;
            perf_logger.error(new StringBuilder().append("slave analysis,").append(line).append(",").append(size).append(",").append(consume).toString());
        }
    }

    @Override
    public void destory() {
    }

    @Override
    public void handleError(Exception ex, Object detail) {
        logger.error(new StringBuilder("workerName : ").append(workerName).toString(), ex);
    }
}
