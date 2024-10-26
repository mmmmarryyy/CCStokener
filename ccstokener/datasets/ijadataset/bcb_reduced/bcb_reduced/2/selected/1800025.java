package com.dynomedia.esearch.util.groupkeycache.util;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GkcConfig {

    private static final Log logger = LogFactory.getLog(GkcConfig.class);

    private static GkcConfig config = new GkcConfig();

    /** keyBuffer读取线程数
	 *  当keyBuffer的并发写入压力较大时，可增加读取线程数，缓解keyBuffer在短时间内占用的大量内存空间。
	 * */
    private Integer keyBufferConsumerThreadCount = 1;

    /** key的最小长度
	 *  GroupkeyCache将自动检查每个缓存Key值的长度。keyMinlength <= validKeylength <= keyMaxlength，
	 *  如果原始Key值长度超过了允许的最大长度，GroupkeyCache将自动切割Key值长度为keyMaxlength.
	 * */
    private Integer keyMinlength = 1;

    /** key的最大长度*/
    private Integer keyMaxlength = 10;

    /** GroupkeyCache维护线程。
	 *  如果GroupkeyCache中有较多的分组对象或其内部缓存了大量的Key值对象，可增加维护线程数量以提高程序的整体运行速度。
	 * */
    private Integer groupkeyConsumerThreadCount = 1;

    /** GroupkeyCache维护线程的间歇时间（秒）。
	 *  增加统计线程的间歇时间可以在一段更长的统计时间内获得更多的统计样本。
	 * */
    private Integer groupkeySortInterval = 600;

    /** 每个分组对象可缓存的经过排序后的Key值对象数量上限。
	 * */
    private Integer groupkeySize = 50;

    /**
	 * Groupkey对象内部的排序容器和采样容器之间的容量匹配系数，默认5，即采样容器大小 = groupkeySize*groupkeySamplingMatchfactor
	 */
    private Integer groupkeySamplingMatchfactor = 5;

    /** consumerThreadPool线程数 consumerThreadCount = groupkeyConsumerThreadCount+keyBufferConsumerThreadCount*/
    private Integer consumerThreadCount = 0;

    /**
	 * 当isFlush=true，并且排序线程完成排序操作后，除继续缓存计数次数最高的groupkeySize条Key值对象外，会清空所有其他的Key值对象。
	 */
    private boolean isFlush = false;

    /** 统计结果是否存储
	 *  当isStore为true时，GroupkeyCache将会在每次排序操作完成之后，将每个分组的排序结果以xml文件方式缓存至gkcfiles/persistent目录。
	 * */
    private boolean isStore = true;

    /**
	 * 是否启用监控线程 
	 */
    private boolean isMonitor = false;

    /** gkc数据存储目录*/
    private File dataDir;

    private GkcConfig() {
        if (logger.isInfoEnabled()) logger.info("GkcConfig loading gkc-config.properties...");
        try {
            Properties prop;
            prop = new Properties();
            URL url = Thread.currentThread().getContextClassLoader().getResource("gkc-config.properties");
            if (url != null) prop.load(url.openStream());
            if (prop.get("keyBufferConsumerThreadCount") != null) keyBufferConsumerThreadCount = Integer.parseInt((String) prop.get("keyBufferConsumerThreadCount"));
            if (prop.get("keyMinlength") != null) keyMinlength = Integer.parseInt((String) prop.get("keyMinlength"));
            if (prop.get("keyMaxlength") != null) keyMaxlength = Integer.parseInt((String) prop.get("keyMaxlength"));
            if (prop.get("groupkeyConsumerThreadCount") != null) groupkeyConsumerThreadCount = Integer.parseInt((String) prop.get("groupkeyConsumerThreadCount"));
            if (prop.get("groupkeySortInterval") != null) groupkeySortInterval = Integer.parseInt((String) prop.get("groupkeySortInterval"));
            if (prop.get("groupkeySize") != null) groupkeySize = Integer.parseInt((String) prop.get("groupkeySize"));
            if (prop.get("groupkeySamplingMatchfactor") != null) groupkeySamplingMatchfactor = Integer.parseInt((String) prop.get("groupkeySamplingMatchfactor"));
            if (prop.get("isFlush") != null) {
                if (((String) prop.get("isFlush")).equalsIgnoreCase("true")) isFlush = true;
            }
            if (prop.get("isStore") != null) {
                if (((String) prop.get("isStore")).equalsIgnoreCase("false")) isStore = false;
            }
            consumerThreadCount = groupkeyConsumerThreadCount + keyBufferConsumerThreadCount;
            dataDir = new File(new File(url.toURI()).getParentFile(), "gkcfiles" + File.separatorChar + "persistent");
            if (prop.get("isMonitor") != null) {
                if (((String) prop.get("isMonitor")).equalsIgnoreCase("true")) {
                    isMonitor = true;
                    consumerThreadCount = consumerThreadCount + 1;
                }
            }
            if (logger.isInfoEnabled()) logger.info("GkcConfig init complete." + this);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("GkcConfig init error!", e);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\r\t[keyBufferConsumerThreadCount]:" + keyBufferConsumerThreadCount).append("\r\t[groupkeyConsumerThreadCount]:" + groupkeyConsumerThreadCount).append("\r\t[groupkeySortInterval]:" + groupkeySortInterval).append("\r\t[groupkeySize]:" + groupkeySize).append("\r\t[groupkeySamplingMatchfactor]:" + groupkeySamplingMatchfactor).append("\r\t[isFlush]:" + isFlush).append("\r\t[consumerThreadCount]:" + consumerThreadCount).append("\r\t[isStore]:" + isStore).append("\r\t[isMonitor]:" + isMonitor).append("\r\t[dataDir]:" + dataDir.getAbsolutePath());
        return sb.toString();
    }

    public static GkcConfig getInstance() {
        return config;
    }

    public Integer getGroupkeyConsumerThreadCount() {
        return groupkeyConsumerThreadCount;
    }

    public void setGroupkeyConsumerThreadCount(Integer groupkeyConsumerThreadCount) {
        this.groupkeyConsumerThreadCount = groupkeyConsumerThreadCount;
    }

    public Integer getKeyBufferConsumerThreadCount() {
        return keyBufferConsumerThreadCount;
    }

    public void setKeyBufferConsumerThreadCount(Integer keyBufferConsumerThreadCount) {
        this.keyBufferConsumerThreadCount = keyBufferConsumerThreadCount;
    }

    public Integer getConsumerThreadCount() {
        return consumerThreadCount;
    }

    public void setConsumerThreadCount(Integer consumerThreadCount) {
        this.consumerThreadCount = consumerThreadCount;
    }

    public Integer getGroupkeySize() {
        return groupkeySize;
    }

    public void setGroupkeySize(Integer groupkeySize) {
        this.groupkeySize = groupkeySize;
    }

    public boolean isStore() {
        return isStore;
    }

    public void setStore(boolean isStore) {
        this.isStore = isStore;
    }

    public Integer getKeyMinlength() {
        return keyMinlength;
    }

    public void setKeyMinlength(Integer keyMinlength) {
        this.keyMinlength = keyMinlength;
    }

    public Integer getKeyMaxlength() {
        return keyMaxlength;
    }

    public void setKeyMaxlength(Integer keyMaxlength) {
        this.keyMaxlength = keyMaxlength;
    }

    public File getDataDir() {
        return dataDir;
    }

    public void setDataDir(File dataDir) {
        this.dataDir = dataDir;
    }

    public boolean isMonitor() {
        return isMonitor;
    }

    public Integer getGroupkeySortInterval() {
        return groupkeySortInterval;
    }

    public void setGroupkeySortInterval(Integer groupkeySortInterval) {
        this.groupkeySortInterval = groupkeySortInterval;
    }

    public Integer getGroupkeySamplingMatchfactor() {
        return groupkeySamplingMatchfactor;
    }

    public void setGroupkeySamplingMatchfactor(Integer groupkeySamplingMatchfactor) {
        this.groupkeySamplingMatchfactor = groupkeySamplingMatchfactor;
    }

    public boolean isFlush() {
        return isFlush;
    }

    public void setFlush(boolean isFlush) {
        this.isFlush = isFlush;
    }
}
