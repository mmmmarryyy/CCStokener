package edu.harvard.iq.safe.saasystem.web.trac1;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.*;
import java.nio.channels.FileChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import edu.harvard.iq.safe.saasystem.ejb.*;
import edu.harvard.iq.safe.saasystem.entities.*;
import edu.harvard.iq.safe.saasystem.trac1.*;
import edu.harvard.iq.safe.saasystem.util.*;
import javax.ejb.EJB;
import edu.harvard.iq.safe.saasystem.auditschema.*;
import edu.harvard.iq.safe.saasystem.versioning.SAASSubversionServiceBean;
import java.net.URL;
import java.text.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.primefaces.event.FlowEvent;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 *
 * @author Akio Sone
 */
@ManagedBean(name = "tRACAuditChecklistMB")
@SessionScoped
public class TRACAuditChecklistManagedBean {

    static final Logger logger = Logger.getLogger(TRACAuditChecklistManagedBean.class.getName());

    @EJB
    TracAuditChecklistDataFacade tracAuditChecklistDataFacade;

    @EJB
    TracAuditChecklistResultFacade tracAuditChecklistResultFacade;

    @EJB
    SAASConfigurationRegistryBean saasConfigurationRegistryBean;

    @EJB
    ConfigFile configFile;

    TracAuditChecklistResult tracAuditChecklistResult;

    String tracCriteriaPropertiesFileName = "trac1Criteria.properties";

    Properties tracCriteriaCheckList;

    int aspectCodeOffsetValue = 16;

    FastDateFormat fdf = null;

    String timestampPattern = null;

    String timezoneId = null;

    String tracDataTable = "trac_audit_checklist_data";

    String tracResultTable = "trac_audit_checklist_result";

    boolean tracResultTableAvailable;

    boolean tracDataTableAvailable;

    static final String TRAC_VERSION = "1.0";

    static final String PLN_GROUP_NAME = "Not Available";

    static final int DEFAULT_REPORT_INTERVAL = 7;

    int reportInterval;

    public TRACAuditChecklistManagedBean() {
    }

    String sectionAcaption;

    public String getSectionAcaption() {
        return sectionAcaption;
    }

    public void setSectionAcaption(String sectionAcaption) {
        this.sectionAcaption = sectionAcaption;
    }

    String sectionBcaption;

    public String getSectionBcaption() {
        return sectionBcaption;
    }

    public void setSectionBcaption(String sectionBcaption) {
        this.sectionBcaption = sectionBcaption;
    }

    String sectionCcaption;

    public String getSectionCcaption() {
        return sectionCcaption;
    }

    public void setSectionCcaption(String sectionCcaption) {
        this.sectionCcaption = sectionCcaption;
    }

    List<TRACCriteriaCheckListItem> tracSectionA;

    public List<TRACCriteriaCheckListItem> getTracSectionA() {
        return tracSectionA;
    }

    public void setTracSectionA(List<TRACCriteriaCheckListItem> tracSectionA) {
        this.tracSectionA = tracSectionA;
    }

    List<TRACCriteriaCheckListItem> tracSectionB;

    public List<TRACCriteriaCheckListItem> getTracSectionB() {
        return tracSectionB;
    }

    public void setTracSectionB(List<TRACCriteriaCheckListItem> tracSectionB) {
        this.tracSectionB = tracSectionB;
    }

    List<TRACCriteriaCheckListItem> tracSectionC;

    public List<TRACCriteriaCheckListItem> getTracSectionC() {
        return tracSectionC;
    }

    public void setTracSectionC(List<TRACCriteriaCheckListItem> tracSectionC) {
        this.tracSectionC = tracSectionC;
    }

    int aspectCount = 0;

    String birtViewerServerUrl = null;

    public String getBirtViewerServerUrl() {
        return birtViewerServerUrl;
    }

    public void setBirtViewerServerUrl(String birtViewerServerUrl) {
        this.birtViewerServerUrl = birtViewerServerUrl;
    }

    String updateButtonMessage;

    public String getUpdateButtonMessage() {
        return updateButtonMessage;
    }

    public void setUpdateButtonMessage(String updateButtonMessage) {
        this.updateButtonMessage = updateButtonMessage;
    }

    int secAitems = 0;

    int secBitems = 0;

    int secCitems = 0;

    Map<String, String> tracDataMap;

    @PostConstruct
    public void init() {
        logger.info("+++++ TRACAuditChecklistMB: init() start ++++++++");
        tracSectionA = new ArrayList<TRACCriteriaCheckListItem>();
        tracSectionB = new ArrayList<TRACCriteriaCheckListItem>();
        tracSectionC = new ArrayList<TRACCriteriaCheckListItem>();
        Set<String> tableSet = tracAuditChecklistDataFacade.findAllTables();
        List<TracAuditChecklistData> tracData = tracAuditChecklistDataFacade.findAll();
        tracDataMap = new LinkedHashMap<String, String>();
        if (tableSet.contains(tracDataTable) && !tracData.isEmpty()) {
            logger.info("tracDataTable exists and not empty (read-back case): size=" + tracData.size());
            tracDataTableAvailable = true;
            String value = null;
            for (TracAuditChecklistData tracDataRow : tracData) {
                if (tracDataRow.getEvidence() == null) {
                    value = "";
                } else {
                    value = tracDataRow.getEvidence();
                }
                tracDataMap.put(tracDataRow.getAspectId(), value);
            }
            logger.info("size of tracDataMap=" + tracDataMap.size());
        } else {
            tracDataTableAvailable = false;
            logger.info("tracDataTable is not available: no-read-back case");
        }
        timestampPattern = configFile.getTimestampPattern();
        if (StringUtils.isBlank(timestampPattern)) {
            timestampPattern = TRACConstants.TIME_STAMP_PATTERN_FOR_WEB_PAGE;
        } else {
            if (!timestampPattern.endsWith(" zz") && !timestampPattern.endsWith(" Z")) {
                timestampPattern += " zz";
            }
        }
        logger.log(Level.INFO, "timestampPattern to be used={0}", timestampPattern);
        if (StringUtils.isBlank(configFile.getSaastimezone())) {
            timezoneId = TRACConstants.DEFAULT_TIME_ZONE;
        } else {
            timezoneId = configFile.getSaastimezone();
        }
        logger.log(Level.INFO, "timezone to be used={0}", timezoneId);
        fdf = FastDateFormat.getInstance(timestampPattern, TimeZone.getTimeZone(timezoneId));
        try {
            URL url = TRACAuditChecklistManagedBean.class.getResource(tracCriteriaPropertiesFileName);
            tracCriteriaCheckList = new LinkedProperties();
            tracCriteriaCheckList.load(url.openStream());
            Set<String> tmpKeys = tracCriteriaCheckList.stringPropertyNames();
            List<String> sortWrkList = new ArrayList<String>();
            sortWrkList.addAll(tmpKeys);
            sortList(sortWrkList);
            for (String key : sortWrkList) {
                String aspectCode = key.substring(aspectCodeOffsetValue);
                logger.info("aspectCode=" + aspectCode);
                if (aspectCode.startsWith("A")) {
                    if (aspectCode.equals("A")) {
                        sectionAcaption = tracCriteriaCheckList.getProperty(key);
                    } else {
                        if (aspectCode.length() == 4 || aspectCode.length() == 5) {
                            if (tracDataTableAvailable) {
                                tracSectionA.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), tracDataMap.get(aspectCode), "", ""));
                            } else {
                                tracSectionA.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), "", "", ""));
                            }
                            secAitems++;
                        }
                    }
                } else if (aspectCode.startsWith("B")) {
                    if (aspectCode.equals("B")) {
                        sectionBcaption = tracCriteriaCheckList.getProperty(key);
                    } else {
                        if (aspectCode.length() == 4 || aspectCode.length() == 5) {
                            if (tracDataTableAvailable) {
                                tracSectionB.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), tracDataMap.get(aspectCode), "", ""));
                            } else {
                                tracSectionB.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), "", "", ""));
                            }
                            secBitems++;
                        }
                    }
                } else if (aspectCode.startsWith("C")) {
                    if (aspectCode.equals("C")) {
                        sectionCcaption = tracCriteriaCheckList.getProperty(key);
                    } else {
                        if (aspectCode.length() == 4 || aspectCode.length() == 5) {
                            if (tracDataTableAvailable) {
                                tracSectionC.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), tracDataMap.get(aspectCode), "", ""));
                            } else {
                                tracSectionC.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), "", "", ""));
                            }
                            secCitems++;
                        }
                    }
                } else {
                    logger.warning("offset value is wrong: 16th character must be A or B or C");
                }
            }
        } catch (FileNotFoundException ex) {
            logger.log(Level.WARNING, "specified properties file was not found", ex);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "IO error occurred", ex);
        } finally {
        }
        aspectCount = secAitems + secBitems + secCitems;
        birtViewerServerUrl = configFile.getBirtViewerServerFrameset();
        if (tableSet.contains(tracResultTable)) {
            logger.info("tracResultTable exists");
            tracResultTableAvailable = true;
        } else {
            tracResultTableAvailable = false;
            logger.info("tracResultTable does not exist");
        }
        logger.info("+++++ TRACAuditChecklistMB: init() end   ++++++++");
    }

    public boolean isTracResultTableAvailable() {
        return tracResultTableAvailable;
    }

    public void setTracResultTableAvailable(boolean tracResultTableAvailable) {
        this.tracResultTableAvailable = tracResultTableAvailable;
    }

    class LinkedProperties extends Properties {

        private final LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

        @Override
        public Enumeration<Object> keys() {
            return Collections.<Object>enumeration(keys);
        }

        @Override
        public Object put(Object key, Object value) {
            keys.add(key);
            return super.put(key, value);
        }
    }

    void sortList(List<String> aItems) {
        Collections.sort(aItems, String.CASE_INSENSITIVE_ORDER);
    }

    public String gotoHomePage() {
        logger.info("go to Home Page");
        return "/index.xhtml?faces-redirect=true";
    }

    public void saveCheckList(ActionEvent actionEvent) {
        int rows = tracAuditChecklistDataFacade.count();
        logger.info("how many existing data rows=" + rows);
        if (rows > 0) {
            tracAuditChecklistDataFacade.deleteAll();
        }
        logger.info("update section A");
        for (TRACCriteriaCheckListItem item : getTracSectionA()) {
            TracAuditChecklistData tracAuditChecklistData = new TracAuditChecklistData(1L);
            tracAuditChecklistData.setAspectId(item.CriterionId);
            tracAuditChecklistData.setEvidence(item.evidenceText);
            tracAuditChecklistDataFacade.create(tracAuditChecklistData);
        }
        logger.info("update section B");
        for (TRACCriteriaCheckListItem item : getTracSectionB()) {
            TracAuditChecklistData tracAuditChecklistData = new TracAuditChecklistData(1L);
            tracAuditChecklistData.setAspectId(item.CriterionId);
            tracAuditChecklistData.setEvidence(item.evidenceText);
            tracAuditChecklistDataFacade.create(tracAuditChecklistData);
        }
        logger.info("update section C");
        for (TRACCriteriaCheckListItem item : getTracSectionC()) {
            TracAuditChecklistData tracAuditChecklistData = new TracAuditChecklistData(1L);
            tracAuditChecklistData.setAspectId(item.CriterionId);
            tracAuditChecklistData.setEvidence(item.evidenceText);
            tracAuditChecklistDataFacade.create(tracAuditChecklistData);
        }
        updateButtonMessage = "TRAC Audit Checklist saved at: " + fdf.format(new Date());
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Saving the TRAC Audit Checklist  Finished:", updateButtonMessage));
    }

    public void clearChecklist(ActionEvent actionEvent) {
        logger.info("+++++++++++++++++ TRACAuditChecklistManagedBean: clearChecklist() start +++++++++++++++++");
        logger.info("resetting the section A");
        for (TRACCriteriaCheckListItem item : getTracSectionA()) {
            item.evidenceText = "";
        }
        logger.info("resetting the section B");
        for (TRACCriteriaCheckListItem item : getTracSectionB()) {
            item.evidenceText = "";
        }
        logger.info("resetting the section C");
        for (TRACCriteriaCheckListItem item : getTracSectionC()) {
            item.evidenceText = "";
        }
        tracAuditChecklistDataFacade.deleteAll();
        logger.info("+++++++++++++++++ TRACAuditChecklistManagedBean: clearChecklist() end +++++++++++++++++");
    }

    public void generateReport(ActionEvent actionEvent) {
        logger.info("+++++++++++++++++ TRACAuditChecklistManagedBean: generateReport() start +++++++++++++++++");
        int rows = tracAuditChecklistResultFacade.count();
        logger.info("how many existing rows=" + rows);
        if (rows > 0) {
            tracAuditChecklistResultFacade.deleteAll();
        }
        tracAuditChecklistResult = new TracAuditChecklistResult(1L);
        String emailTracList = saasConfigurationRegistryBean.getSaasAuditConfigProperties().getProperty("saas.audit.report.emailnotice.recipients");
        String emailAdmin = saasConfigurationRegistryBean.getSaasConfigProperties().getProperty("saas.support.contactus.emailaddress");
        String plnGroupName = saasConfigurationRegistryBean.getSaasConfigProperties().getProperty("saas.daemonstatus.groupname");
        if (StringUtils.isBlank(plnGroupName)) {
            plnGroupName = PLN_GROUP_NAME;
        }
        String reportIntervalString = saasConfigurationRegistryBean.getSaasAuditConfigProperties().getProperty("saas.audit.report.interval");
        if (StringUtils.isBlank(reportIntervalString)) {
            reportInterval = DEFAULT_REPORT_INTERVAL;
        } else {
            reportInterval = Integer.parseInt(reportIntervalString);
        }
        tracAuditChecklistResult.setAdminEmail(emailAdmin);
        tracAuditChecklistResult.setAuditReportEmail(emailTracList);
        tracAuditChecklistResult.setAuditReportInterval(reportInterval);
        tracAuditChecklistResult.setGroupName(plnGroupName);
        tracAuditChecklistResult.setTracVersion(TRAC_VERSION);
        List<TracAuditChecklistData> evidenceList = tracAuditChecklistDataFacade.findAll();
        logger.info("size of the retrieved evidence list=" + evidenceList.size());
        int secApass = 0;
        int secBpass = 0;
        int secCpass = 0;
        int secAitem = 0;
        int secBitem = 0;
        int secCitem = 0;
        for (TracAuditChecklistData row : evidenceList) {
            if (row.getAspectId().startsWith("A")) {
                secAitem++;
                if (StringUtils.isNotBlank(row.getEvidence())) {
                    secApass++;
                }
            } else if (row.getAspectId().startsWith("B")) {
                secBitem++;
                if (StringUtils.isNotBlank(row.getEvidence())) {
                    secBpass++;
                }
            } else if (row.getAspectId().startsWith("C")) {
                secCitem++;
                if (StringUtils.isNotBlank(row.getEvidence())) {
                    secCpass++;
                }
            }
        }
        if (secAitem == 0) {
            secAitem = secAitems;
        }
        if (secBitem == 0) {
            secBitem = secBitems;
        }
        if (secCitem == 0) {
            secCitem = secCitems;
        }
        tracAuditChecklistResult.setSecACrtTotal(secAitem);
        tracAuditChecklistResult.setSecBCrtTotal(secBitem);
        tracAuditChecklistResult.setSecCCrtTotal(secCitem);
        tracAuditChecklistResult.setSecACrtPass(secApass);
        tracAuditChecklistResult.setSecBCrtPass(secBpass);
        tracAuditChecklistResult.setSecCCrtPass(secCpass);
        int pass3secs = secApass + secBpass + secCpass;
        int all3secs = secAitem + secBitem + secCitem;
        logger.info("number of all TRAC criteria=" + all3secs);
        logger.info("non-blank evidence boxes=" + pass3secs);
        logger.info("aspectCount=" + aspectCount);
        if (all3secs == 0 && aspectCount > 0) {
            all3secs = aspectCount;
        }
        if (all3secs > 0) {
            int diff = all3secs - pass3secs;
            if (diff > 0) {
                tracAuditChecklistResult.setOverallResult(Boolean.FALSE);
                logger.info("Not all criteria are met: failed items=" + diff);
            } else if (diff == 0) {
                tracAuditChecklistResult.setOverallResult(Boolean.TRUE);
                logger.info("All criteria are passed");
            } else {
                logger.severe("counting TRAC criteria had some problems:diff=" + diff);
                tracAuditChecklistResult.setOverallResult(Boolean.FALSE);
            }
        } else {
            logger.severe("The number of TRAC criteria is not positive:=" + all3secs);
            tracAuditChecklistResult.setOverallResult(Boolean.FALSE);
        }
        tracAuditChecklistResult.setResultTabulationDate(System.currentTimeMillis());
        tracAuditChecklistResultFacade.create(tracAuditChecklistResult);
        updateButtonMessage = "TRAC Audit Report generated at: " + fdf.format(new Date());
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "TRAC-report generation Finished:", updateButtonMessage));
        logger.info("+++++++++++++++++ TRACAuditChecklistManagedBean: generateReport() end ++++++++++");
    }
}
