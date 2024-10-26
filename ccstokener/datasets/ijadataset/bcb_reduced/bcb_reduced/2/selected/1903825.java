package edu.harvard.iq.safe.saasystem.web;

import edu.harvard.iq.safe.saasystem.ejb.AuAttributesFacade;
import edu.harvard.iq.safe.saasystem.ejb.AuOverviewTableFacade;
import edu.harvard.iq.safe.saasystem.ejb.HostAttributesFacade;
import edu.harvard.iq.safe.saasystem.ejb.LockssBoxTableFacade;
import edu.harvard.iq.safe.saasystem.ejb.SuccessfulPollReplicaIpFacade;
import edu.harvard.iq.safe.saasystem.entities.AuOverviewTable;
import edu.harvard.iq.safe.saasystem.entities.LockssBoxTable;
import edu.harvard.iq.safe.saasystem.entities.SuccessfulPollReplicaIp;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLAuOverviewDAO;
import edu.harvard.iq.safe.saasystem.etl.dao.MySQLDAOFactory;
import edu.harvard.iq.safe.saasystem.etl.util.sql.DAOFactory;
import edu.harvard.iq.safe.saasystem.util.ConfigFile;
import edu.harvard.iq.safe.saasystem.util.GeographicDataServiceBean;
import java.security.Principal;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.*;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Akio Sone
 */
@ManagedBean(name = "dashboardMB")
@SessionScoped
public class DashboardManagedBean {

    static final Logger logger = Logger.getLogger(DashboardManagedBean.class.getName());

    @EJB
    AuOverviewTableFacade auOverviewTableFacade;

    @EJB
    SuccessfulPollReplicaIpFacade successfulPollReplicaIpFacade;

    @EJB
    LockssBoxTableFacade lockssBoxTableFacade;

    @EJB
    ConfigFile configFile;

    @EJB
    GeographicDataServiceBean geographicDataServiceBean;

    @EJB
    AuAttributesFacade auAttributesFacade;

    @EJB
    HostAttributesFacade hostAttributesFacade;

    List<LockssBoxTable> lockssBoxList = null;

    List<AuOverviewTable> auSummaryList = null;

    List<AUSummaryDataForChart> auSummaryDataList = new ArrayList<AUSummaryDataForChart>();

    List<SuccessfulPollReplicaIp> pollList = null;

    Map<String, String> geounitcodeToRegionCodeTable = null;

    Map<String, String> regionCodeToLabeTable = null;

    Map<String, Integer> freqTable = new TreeMap<String, Integer>();

    Map<String, List<String>> auNameToLocationListTable = new LinkedHashMap<String, List<String>>();

    List<String> auList = new ArrayList<String>();

    List<AUReplicaData> replicaData = new ArrayList<AUReplicaData>();

    Map<String, String> auNameToAuShortNameTable = null;

    Map<String, String> ipAddressToRegionCodeTable = null;

    StreamedContent auOdumMap;

    StreamedContent safePLNMemberMap;

    static final int N_AUS_FOR_CHART = 5;

    public DashboardManagedBean() {
    }

    @PostConstruct
    public void initialize() {
        logger.info("+++++ DashboardFManagedBean: postConstruct step: start ++++++++");
        if (auSummaryList == null || auSummaryList.isEmpty()) {
            auSummaryList = auOverviewTableFacade.getNewerVerifiedAus(N_AUS_FOR_CHART);
            if (auSummaryList == null) {
                MySQLDAOFactory daof = (MySQLDAOFactory) DAOFactory.getDAOFactory(DAOFactory.DBvendor.MySQL);
                MySQLAuOverviewDAO maodao = (MySQLAuOverviewDAO) daof.getAuOverviewDAO();
                maodao.createTable();
                if (auOverviewTableFacade.isTableExistent("au_overview_table")) {
                    auSummaryList = auOverviewTableFacade.getNewerVerifiedAus(N_AUS_FOR_CHART);
                }
            }
        }
        geounitcodeToRegionCodeTable = geographicDataServiceBean.getGeounitcodeToRegionCodeTable();
        regionCodeToLabeTable = geographicDataServiceBean.getRegionCodeToLabeTable();
        auNameToAuShortNameTable = auAttributesFacade.getAuNameToAuShortNameTable();
        ipAddressToRegionCodeTable = hostAttributesFacade.getIpAddressToRegionCodeTable();
        lockssBoxList = lockssBoxTableFacade.findAll();
        Map<String, String> regionCodeToLabeTableNew = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : regionCodeToLabeTable.entrySet()) {
            if (Integer.parseInt(entry.getKey()) < 90) {
                regionCodeToLabeTableNew.put(entry.getKey(), entry.getValue());
            }
        }
        Set<String> regionCodeSet = new TreeSet<String>(regionCodeToLabeTableNew.values());
        logger.fine("RegionCodelabelSet=" + regionCodeSet);
        if (auSummaryList != null) {
            for (AuOverviewTable auSummary : auSummaryList) {
                logger.fine("working on au name=" + auSummary.getAuName() + ":pollId=" + auSummary.getPollId());
                Integer verifiedReplicas = auSummary.getAuNVerifiedReplicas();
                if (auSummary.getAuNVerifiedReplicas() > 0) {
                    verifiedReplicas++;
                }
                auSummaryDataList.add(new AUSummaryDataForChart(auSummary.getAuName(), auSummary.getAuNReplicas(), verifiedReplicas, auNameToAuShortNameTable.get(auSummary.getAuName())));
                if (auSummary.getPollId() == null) {
                    logger.fine("pollId is null");
                    replicaData.add(new AUReplicaData(0, 0, 0, 0, 0, auSummary.getAuName(), auNameToAuShortNameTable.get(auSummary.getAuName())));
                    continue;
                }
                pollList = successfulPollReplicaIpFacade.findPollByPollId(auSummary.getPollId());
                logger.fine("pollList=" + pollList);
                if (pollList == null || pollList.isEmpty()) {
                    logger.fine("pollList is null");
                    replicaData.add(new AUReplicaData(0, 0, 0, 0, 0, auSummary.getAuName(), auNameToAuShortNameTable.get(auSummary.getAuName())));
                    continue;
                }
                List<String> ipList = new ArrayList<String>();
                for (SuccessfulPollReplicaIp row : pollList) {
                    logger.fine("current Ip=" + row.getIpAddress());
                    String regionCode = ipAddressToRegionCodeTable.get(row.getIpAddress());
                    logger.fine("regionCode=" + regionCode);
                    String regionLabel = regionCodeToLabeTable.get(regionCode);
                    logger.fine("regionLabel=" + regionLabel);
                    if (StringUtils.isBlank(regionLabel)) {
                        regionLabel = "Others";
                    }
                    ipList.add(regionLabel);
                }
                logger.fine("ipList" + ipList + " for au=" + auSummary.getAuName());
                if (ipList == null || ipList.isEmpty()) {
                    logger.fine("ipList is null");
                    replicaData.add(new AUReplicaData(0, 0, 0, 0, 0, auSummary.getAuName(), auNameToAuShortNameTable.get(auSummary.getAuName())));
                    continue;
                }
                int nonUS = 0;
                for (String label : regionCodeSet) {
                    int count = Collections.frequency(ipList, label);
                    if (label.equals("Others")) {
                        count += nonUS;
                        freqTable.put(label, count);
                    } else {
                        freqTable.put(label, count);
                    }
                }
                logger.fine("freqTable=" + freqTable);
                auNameToLocationListTable.put(auSummary.getAuName(), ipList);
                auList.add(auSummary.getAuName());
                Integer northeast = 0;
                if (freqTable.get("NORTHEAST") != null) {
                    northeast = freqTable.get("NORTHEAST");
                }
                Integer midwest = 0;
                if (freqTable.get("MIDWEST") != null) {
                    midwest = freqTable.get("MIDWEST");
                }
                Integer south = 0;
                if (freqTable.get("SOUTH") != null) {
                    south = freqTable.get("SOUTH");
                }
                Integer west = 0;
                if (freqTable.get("WEST") != null) {
                    west = freqTable.get("WEST");
                }
                Integer others = 0;
                if (freqTable.get("Others") != null) {
                    others = freqTable.get("Others");
                }
                replicaData.add(new AUReplicaData(northeast, midwest, south, west, others, auSummary.getAuName(), auNameToAuShortNameTable.get(auSummary.getAuName())));
                logger.info("replicaData=" + replicaData);
            }
        }
        URL url = DashboardManagedBean.class.getResource("us_map_template.svg");
        try {
            safePLNMemberMap = new DefaultStreamedContent(url.openStream(), "image/svg+xml");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "failed to read the svg file", ex);
        }
    }

    public List<AuOverviewTable> getAuSummaryList() {
        return auSummaryList;
    }

    public void setAuSummaryList(List<AuOverviewTable> auSummaryList) {
        this.auSummaryList = auSummaryList;
    }

    public List<AUReplicaData> getReplicaData() {
        return replicaData;
    }

    public void setReplicaData(List<AUReplicaData> replicaData) {
        this.replicaData = replicaData;
    }

    public List<AUSummaryDataForChart> getAuSummaryDataList() {
        return auSummaryDataList;
    }

    public StreamedContent getAuOdumMap() {
        return auOdumMap;
    }

    public void setAuOdumMap(StreamedContent auOdumMap) {
        this.auOdumMap = auOdumMap;
    }

    public StreamedContent getSafePLNMemberMap() {
        return safePLNMemberMap;
    }

    public void setSafePLNMemberMap(StreamedContent safePLNMemberMap) {
        this.safePLNMemberMap = safePLNMemberMap;
    }
}
