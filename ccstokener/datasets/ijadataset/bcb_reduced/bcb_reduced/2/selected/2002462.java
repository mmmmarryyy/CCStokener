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
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import edu.harvard.iq.safe.saasystem.ejb.*;
import edu.harvard.iq.safe.saasystem.entities.*;
import edu.harvard.iq.safe.saasystem.util.*;
import javax.ejb.EJB;
import edu.harvard.iq.safe.saasystem.auditschema.*;
import edu.harvard.iq.safe.saasystem.versioning.SAASSubversionServiceBean;
import java.net.URL;
import org.apache.commons.lang.StringUtils;
import org.primefaces.event.FlowEvent;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 *
 * @author Akio Sone
 */
@ManagedBean(name = "tRACAuditChecklistAllMB")
@SessionScoped
public class TRACAuditChecklistAllManagedBean {

    static final Logger logger = Logger.getLogger(TRACAuditChecklistAllManagedBean.class.getName());

    String tracCriteriaPropertiesFileName = "trac1Criteria.properties";

    Properties tracCriteriaCheckList;

    int aspectCodeOffsetValue = 16;

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

    TRACCriteriaCheckList tracCheckList = new TRACCriteriaCheckList();

    List<TRACCriteriaCheckListItem> tracSectionAll;

    public List<TRACCriteriaCheckListItem> getTracSectionAll() {
        return tracSectionAll;
    }

    public void setTracSectionAll(List<TRACCriteriaCheckListItem> tracSectionAll) {
        this.tracSectionAll = tracSectionAll;
    }

    @PostConstruct
    public void init() {
        logger.info("+++++ TRACAuditChecklistWizardMB: init() start ++++++++");
        try {
            tracSectionAll = new ArrayList<TRACCriteriaCheckListItem>();
            List<TRACCriteriaCheckListItem> tracSectionA = new ArrayList<TRACCriteriaCheckListItem>();
            List<TRACCriteriaCheckListItem> tracSectionB = new ArrayList<TRACCriteriaCheckListItem>();
            List<TRACCriteriaCheckListItem> tracSectionC = new ArrayList<TRACCriteriaCheckListItem>();
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
                        if (aspectCode.length() == 4) {
                            tracSectionAll.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), "", "", ""));
                        }
                    }
                } else if (aspectCode.startsWith("B")) {
                    if (aspectCode.equals("B")) {
                        sectionBcaption = tracCriteriaCheckList.getProperty(key);
                    } else {
                        if (aspectCode.length() == 4) {
                            tracSectionAll.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), "", "", ""));
                        }
                    }
                } else if (aspectCode.startsWith("C")) {
                    if (aspectCode.equals("C")) {
                        sectionCcaption = tracCriteriaCheckList.getProperty(key);
                    } else {
                        if (aspectCode.length() == 4) {
                            tracSectionAll.add(new TRACCriteriaCheckListItem(aspectCode, tracCriteriaCheckList.getProperty(key), "", "", ""));
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
        logger.info("+++++ TRACAuditChecklistWizardMB: init() end   ++++++++");
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

    public String saveCheckList(ActionEvent actionEvent) {
        logger.info("section A");
        for (TRACCriteriaCheckListItem item : tracCheckList.getTracSectionA()) {
            logger.info(item.CriterionId + "=" + item.evidenceText);
        }
        logger.info("section B");
        for (TRACCriteriaCheckListItem item : tracCheckList.getTracSectionB()) {
            logger.info(item.CriterionId + "=" + item.evidenceText);
        }
        logger.info("section C");
        for (TRACCriteriaCheckListItem item : tracCheckList.getTracSectionC()) {
            logger.info(item.CriterionId + "=" + item.evidenceText);
        }
        return "/index.xhtml?faces-redirect=true";
    }
}
