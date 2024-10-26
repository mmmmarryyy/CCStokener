package edu.harvard.iq.safe.saasystem.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

/**
 *
 * @author Akio Sone
 */
@Singleton
@LocalBean
@Startup
public class SAASConfigurationRegistryBean {

    static final Logger logger = Logger.getLogger(SAASConfigurationRegistryBean.class.getName());

    static String PLN_MEMBER_IP_LIST_KEY = "id.initialV3PeerList";

    static String SAAS_BUILD_INFO_FILENAME = "saasBuildInfo.properties";

    static String SAAS_TIME_STAMP_PATTERN = "yyyy-MM-dd'T'HH-mm-ss";

    String timestampPattern = null;

    static String SAAS_DEFAULT_TIME_ZONE = "America/New_York";

    final Properties saasBuildInfo = new LinkedProperties();

    private final Properties saasConfigProperties = new Properties();

    private final Properties saasAuditConfigProperties = new Properties();

    private final CopyOnWriteArraySet<String> ipSet = new CopyOnWriteArraySet<String>();

    private final ConcurrentHashMap<String, String> ipToTimezoneOffsetMap = new ConcurrentHashMap<String, String>();

    public ConcurrentHashMap<String, String> getIpToTimezoneOffsetMap() {
        return ipToTimezoneOffsetMap;
    }

    public String getConfigProperty(String key) {
        return saasConfigProperties.getProperty(key);
    }

    public void setSaasConfigProperty(String key, String value) {
        saasConfigProperties.setProperty(key, value);
    }

    public void setSaasConfigProperties(Properties ps) {
        saasConfigProperties.clear();
        for (String key : ps.stringPropertyNames()) {
            saasConfigProperties.setProperty(key, ps.getProperty(key));
        }
    }

    public String deleteConfigProperty(String key) {
        return (String) saasConfigProperties.remove(key);
    }

    public Properties getSaasConfigProperties() {
        return saasConfigProperties;
    }

    public CopyOnWriteArraySet<String> getIpSet() {
        return ipSet;
    }

    public Properties getSaasAuditConfigProperties() {
        return saasAuditConfigProperties;
    }

    public void setSaasAuditConfigProperty(String key, String value) {
        saasAuditConfigProperties.setProperty(key, value);
    }

    public void setSaasAuditConfigProperties(Properties ps) {
        saasAuditConfigProperties.clear();
        for (String key : ps.stringPropertyNames()) {
            saasAuditConfigProperties.setProperty(key, ps.getProperty(key));
        }
    }

    String sassLocalConfigFileName;

    String saasAuditConfigFileName;

    public String getSaasAuditConfigFileName() {
        return saasAuditConfigFileName;
    }

    public String getSassLocalConfigFileName() {
        return sassLocalConfigFileName;
    }

    XStream xstream = null;

    int homePageVisitCount = 0;

    public int getHomePageVisitCount() {
        return homePageVisitCount;
    }

    public void addHomePageVisitCount() {
        this.homePageVisitCount++;
    }

    static final Set<String> plnBoundAuditParameterKeySet = new TreeSet<String>();

    static {
        plnBoundAuditParameterKeySet.addAll(Arrays.asList(PLNConfigFileXPathReader.xpathKeyListAudit));
    }

    @PostConstruct
    public void saasStartup() {
        Properties gfJvmProps = System.getProperties();
        if (gfJvmProps.containsKey("saas.localconfig.file")) {
            sassLocalConfigFileName = gfJvmProps.getProperty("saas.localconfig.file");
            if (StringUtils.isNotBlank(sassLocalConfigFileName)) {
                try {
                    logger.log(Level.INFO, "sassLocalConfigFileName={0}", sassLocalConfigFileName);
                    Properties localConfigProps = new Properties();
                    InputStream is = null;
                    try {
                        is = new FileInputStream(sassLocalConfigFileName);
                        localConfigProps.loadFromXML(is);
                        if (logger.isLoggable(Level.FINE)) {
                            for (String key : localConfigProps.stringPropertyNames()) {
                                logger.log(Level.FINE, "key={0}:value={1}", new Object[] { key, localConfigProps.getProperty(key) });
                            }
                        }
                        saasConfigProperties.putAll(localConfigProps);
                    } catch (FileNotFoundException ex) {
                        logger.log(Level.WARNING, "specified config file was not found", ex);
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "IO error occurred", ex);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException ex) {
                                logger.log(Level.WARNING, "failed to close the opened local config file", ex);
                            }
                        }
                    }
                    URL url = SAASConfigurationRegistryBean.class.getResource(SAAS_BUILD_INFO_FILENAME);
                    saasBuildInfo.load(url.openStream());
                    Set<String> tmpKeys = saasBuildInfo.stringPropertyNames();
                    for (String ky : tmpKeys) {
                        logger.log(Level.FINE, "{0}(from the saasconfig)={1}", new Object[] { ky, saasConfigProperties.getProperty(ky) });
                        logger.log(Level.FINE, "{0}(from the properties)={1}", new Object[] { ky, saasBuildInfo.getProperty(ky) });
                        saasConfigProperties.put(ky, saasBuildInfo.getProperty(ky));
                    }
                    logger.log(Level.FINE, "sassConfigProperties={0}", saasConfigProperties.toString());
                    String targetPropertyKey = "saas.member.ip.tag";
                    String targetPropertyValue = saasConfigProperties.getProperty(targetPropertyKey);
                    if (StringUtils.isBlank(targetPropertyValue)) {
                        targetPropertyValue = PLN_MEMBER_IP_LIST_KEY;
                    } else {
                        targetPropertyValue = targetPropertyValue.trim();
                    }
                    logger.log(Level.INFO, "targetPropertyValue={0}", targetPropertyValue);
                    String lockssXmlUrl = (saasConfigProperties.getProperty("saas.lockss.xml.url")).trim();
                    logger.log(Level.INFO, "lockss.xml url={0}", lockssXmlUrl);
                    if (StringUtils.isBlank(lockssXmlUrl)) {
                        logger.log(Level.WARNING, "lockss.xml is not specified");
                        saasConfigProperties.put("saas.ip.fromlockssxml", "");
                    } else {
                        PLNConfigFileXPathReader xpathReader = new PLNConfigFileXPathReader();
                        Map<String, List<String>> resultMap = xpathReader.read(lockssXmlUrl, "UTF-8");
                        if (resultMap != null && resultMap.containsKey(targetPropertyValue)) {
                            logger.log(Level.INFO, "raw ip list={0}", resultMap.get(targetPropertyValue));
                            ipSet.addAll(PLNmemberIpDAO.getPeerIpAddresses(resultMap.get(targetPropertyValue)));
                            logger.log(Level.INFO, "ipSet: lockss.xml={0}", ipSet);
                        }
                        if (resultMap != null && resultMap.containsKey("quorum")) {
                            logger.log(Level.INFO, "quorum={0}", resultMap.get("quorum"));
                        }
                        if (resultMap != null && resultMap.containsKey("maxPollDuration")) {
                            logger.log(Level.INFO, "maxPollDuration={0}", resultMap.get("maxPollDuration"));
                        }
                        saasConfigProperties.put("saas.ip.fromlockssxml", StringUtils.join(ipSet, ","));
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "sassLocalConfigFile cannot be read", ex);
                }
            } else {
                logger.log(Level.INFO, "sassLocalConfigFileName is null or empty");
            }
        } else {
            logger.log(Level.INFO, "saas.localconfig.file is not included in the JVM options");
        }
        if (gfJvmProps.containsKey("saas.captcha.privatekey")) {
            saasConfigProperties.put("saas.captcha.privatekey", gfJvmProps.getProperty("saas.captcha.privatekey"));
        } else {
            logger.log(Level.INFO, "saas.captcha.privatekey is not included in the JVM options");
        }
        if (!saasConfigProperties.containsKey("saas.timezone") || StringUtils.isBlank(saasConfigProperties.getProperty("saas.timezone"))) {
            logger.log(Level.INFO, "saas.timezone is not stored in saas-local-config.xml: check JVM options");
            if (gfJvmProps.containsKey("saas.timezone") && StringUtils.isNotBlank(gfJvmProps.getProperty("saas.timezone"))) {
                logger.log(Level.INFO, "saas.timezone is stored in JVM options and it is not blank={0}", gfJvmProps.containsKey("saas.timezone"));
                saasConfigProperties.put("saas.timezone", gfJvmProps.getProperty("saas.timezone"));
            } else {
                logger.log(Level.INFO, "saas.timezone is not specified in the JVM options: use the default=[{0}]", SAAS_DEFAULT_TIME_ZONE);
            }
        }
        logger.log(Level.INFO, "saas.timezone is {0}", saasConfigProperties.getProperty("saas.timezone"));
        saasAuditConfigFileName = gfJvmProps.getProperty("saas.audit.config.file");
        if (StringUtils.isNotBlank(saasAuditConfigFileName)) {
            logger.log(Level.INFO, "saasAuditConfigFileName={0}", saasAuditConfigFileName);
            Properties auditConfigProps = new Properties();
            InputStream is = null;
            try {
                is = new FileInputStream(saasAuditConfigFileName);
                auditConfigProps.loadFromXML(is);
                for (String key : auditConfigProps.stringPropertyNames()) {
                    logger.log(Level.FINE, "key={0}:value={1}", new Object[] { key, auditConfigProps.getProperty(key) });
                    if (key.equals("saas.targetIp")) {
                        auditConfigProps.setProperty(key, auditConfigProps.getProperty(key).replaceAll("\\s", ""));
                    }
                    if (key.equals("saas.audit.config.timestamp.pattern")) {
                        auditConfigProps.setProperty(key, auditConfigProps.getProperty(key).replaceAll("[:\\?\\|\\$/\\*\\\\]", "-"));
                    }
                }
                saasAuditConfigProperties.putAll(auditConfigProps);
            } catch (FileNotFoundException ex) {
                logger.log(Level.WARNING, "specified audit config file was not found", ex);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "saasAuditConfigFile cannot be read", ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "failed to close the audit config file", ex);
                    }
                }
            }
        } else {
            logger.log(Level.INFO, "no saved audit-config file");
        }
        timestampPattern = saasAuditConfigProperties.getProperty("saas.audit.config.timestamp.pattern");
        if (StringUtils.isBlank(timestampPattern)) {
            timestampPattern = SAAS_TIME_STAMP_PATTERN;
            logger.log(Level.INFO, "time-stamp pattern is set to the default[{0}]", timestampPattern);
        } else {
            logger.log(Level.INFO, "time-stamp pattern is set to [{0}]", timestampPattern);
        }
        Set<String> tmpKeys = saasBuildInfo.stringPropertyNames();
        String plnAuditParamPrefix = "saas.audit.pln.config.";
        for (String ky : tmpKeys) {
            if (plnBoundAuditParameterKeySet.contains(ky)) {
                logger.log(Level.FINE, "{0}(to be added)={1}", new Object[] { ky, saasBuildInfo.getProperty(ky) });
                saasAuditConfigProperties.put(plnAuditParamPrefix + ky, saasBuildInfo.getProperty(ky));
            }
        }
        xstream = new XStream(new JsonHierarchicalStreamDriver());
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("SAASConfigurationRegistryBean", SAASConfigurationRegistryBean.class);
    }

    @PreDestroy
    public void applicationShutdown() {
        logger.log(Level.INFO, "applicationShutdown() is called by SAAS at {0}", DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date()));
    }

    public void saveNetworkSettings(String networkPropFileName) {
        String timeStamp = DateFormatUtils.format(new java.util.Date(), timestampPattern + " zz");
        saasConfigProperties.setProperty("saas.configproperties.lastsaved", timeStamp);
        logger.log(Level.INFO, "network-settings saved at:{0}", timeStamp);
        logger.log(Level.FINE, "network properties before save={0}", xstream.toXML(saasConfigProperties));
        File networkPropFile = null;
        if (networkPropFileName != null) {
            networkPropFile = new File(networkPropFileName);
            if (networkPropFile.exists()) {
                File netwrokPropFileBkUp = new File(networkPropFileName + ".bk");
                boolean bkupResult = backupFile(networkPropFile, netwrokPropFileBkUp);
                if (!bkupResult) {
                    logger.log(Level.WARNING, "network-config-backup-file was not saved");
                }
            }
        } else {
            logger.log(Level.WARNING, "SassLocalConfigFileName is not found: new data cannot be saved");
            return;
        }
        savePropertiesFile(saasConfigProperties, networkPropFile);
    }

    public void saveAuditSettings(String auditPropFileName) {
        String timeStamp = DateFormatUtils.format(new java.util.Date(), timestampPattern + " zz");
        saasAuditConfigProperties.setProperty("saas.auditconfigproperties.lastsaved", timeStamp);
        logger.log(Level.INFO, "audit-settings saved at:{0}", timeStamp);
        logger.log(Level.FINE, "audit properties before save={0}", xstream.toXML(saasAuditConfigProperties));
        File auditPropFile = new File(auditPropFileName);
        if (auditPropFile.exists()) {
            File auditPropFileBkUp = new File(auditPropFileName + ".bk");
            boolean bkupResult = backupFile(auditPropFile, auditPropFileBkUp);
            if (!bkupResult) {
                logger.log(Level.WARNING, "audit-config-backup-file was not saved");
            }
        }
        savePropertiesFile(saasAuditConfigProperties, auditPropFile);
    }

    public void saveCurrentSettings() {
        logger.log(Level.INFO, "---------------------- saveCurrentSettings starts ---------------------");
        String timeStamp = DateFormatUtils.format(new java.util.Date(), timestampPattern);
        logger.log(Level.INFO, "---------------------- Network-side starts ---------------------");
        logger.log(Level.FINE, "network={0}", xstream.toXML(saasConfigProperties));
        saasConfigProperties.setProperty("saas.configproperties.lastsaved", timeStamp);
        logger.log(Level.INFO, "saved: time-stamp={0}", timeStamp);
        String networkPropFileName = getSassLocalConfigFileName();
        saveNetworkSettings(networkPropFileName);
        logger.log(Level.INFO, "SaasConfigProperties has been saved");
        logger.log(Level.INFO, "---------------------- Network-side ends ---------------------");
        logger.log(Level.INFO, "---------------------- Audit-side starts ---------------------");
        saasAuditConfigProperties.setProperty("saas.auditconfigproperties.lastsaved", timeStamp);
        logger.log(Level.INFO, "saved: time-stamp={0}", timeStamp);
        logger.log(Level.FINE, "audit={0}", xstream.toXML(saasAuditConfigProperties));
        String auditPropFileName = getSaasAuditConfigFileName();
        saveAuditSettings(auditPropFileName);
        logger.log(Level.INFO, "SaasAuditConfigProperties has been saved");
        logger.log(Level.INFO, "---------------------- Audit-side ends ---------------------");
        logger.log(Level.INFO, "---------------------- saveCurrentSettings ends ---------------------");
    }

    public void savePropertiesFile(Properties newProp, File newPropFileName) {
        String writeTimestamp = DateFormatUtils.format(new java.util.Date(), timestampPattern);
        OutputStream os = null;
        try {
            os = new FileOutputStream(newPropFileName);
            newProp.storeToXML(os, "new set of properties saved at:" + writeTimestamp, "UTF-8");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed to save new data in the config-file");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "failed to close the config file", ex);
                }
            }
        }
    }

    public boolean backupFile(File oldFile, File newFile) {
        boolean isBkupFileOK = false;
        FileChannel sourceChannel = null;
        FileChannel targetChannel = null;
        try {
            sourceChannel = new FileInputStream(oldFile).getChannel();
            targetChannel = new FileOutputStream(newFile).getChannel();
            targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO exception occurred while copying config file", e);
        } finally {
            if ((newFile != null) && (newFile.exists()) && (newFile.length() > 0)) {
                isBkupFileOK = true;
            }
            try {
                if (sourceChannel != null) {
                    sourceChannel.close();
                }
                if (targetChannel != null) {
                    targetChannel.close();
                }
            } catch (IOException e) {
                logger.log(Level.INFO, "closing channels failed");
            }
        }
        return isBkupFileOK;
    }

    int etlHttprequestTimeout;

    public int getEtlHttprequestTimeout() {
        if (StringUtils.isNotBlank(saasConfigProperties.getProperty("saas.etl.httprequest.timeout"))) {
            etlHttprequestTimeout = Integer.parseInt(saasConfigProperties.getProperty("saas.etl.httprequest.timeout"));
        } else {
            etlHttprequestTimeout = SAASEJBConstants.SAAS_ETL_HTTPRQUEST_TIMEOUT;
        }
        return etlHttprequestTimeout;
    }

    String daemonStatusAccessAccount = null;

    public String getDaemonStatusAccessAccount() {
        if (StringUtils.isNotBlank(saasConfigProperties.getProperty("saas.daemonstatus.account"))) {
            daemonStatusAccessAccount = saasConfigProperties.getProperty("saas.daemonstatus.account");
        } else {
            logger.log(Level.SEVERE, "saas.daemonstatus.account is not set");
        }
        return daemonStatusAccessAccount;
    }

    String daemonStatusAccessPassword;

    public String getDaemonStatusAccessPassword() {
        if (StringUtils.isNotBlank(saasConfigProperties.getProperty("saas.daemonstatus.password"))) {
            daemonStatusAccessPassword = saasConfigProperties.getProperty("saas.daemonstatus.password");
        } else {
            logger.log(Level.SEVERE, "saas.daemonstatus.password is not set");
        }
        return daemonStatusAccessPassword;
    }

    String daemonStatusPort;

    public String getDaemonStatusPort() {
        if (StringUtils.isNotBlank(saasConfigProperties.getProperty("saas.daemonstatus.port"))) {
            daemonStatusPort = saasConfigProperties.getProperty("saas.daemonstatus.port");
        } else {
            daemonStatusPort = SAASEJBConstants.SAAS_DAEMONSTATUS_PORT;
        }
        return daemonStatusPort;
    }
}
