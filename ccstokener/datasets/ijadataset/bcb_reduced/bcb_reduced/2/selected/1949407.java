package org.dcm4chex.webview.mbean;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chex.webview.CustomLaunchProperties;
import org.dcm4chex.webview.InstanceComparator;
import org.dcm4chex.webview.InstanceContainer;
import org.dcm4chex.webview.UIDQuery;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MBean to manage the WebView launcher service.
 * <p>
 * This service provides methods and configuration attributes to open a web (applet) based Dicom viewer.
 * <p>
 * Use standard DICOM C-FIND to find instances.
 * 
 * @author franz.willer@agfa.com
 * @version $Revision$ $Date$
 * @since 04.10.2006
 */
public class WebViewService extends ServiceMBeanSupport {

    private String calledAET;

    private String callingAET;

    private String host;

    private int port;

    private LaunchProperties launchProperties = new LaunchProperties();

    private boolean ignorePRinStudy = false;

    private boolean selectMultiPR = false;

    static Logger log = LoggerFactory.getLogger(WebViewService.class);

    /**
     * Get the calledAET of the PACS to query.
     * 
     * @return Returns the calledAET.
     */
    public String getCalledAET() {
        return calledAET;
    }

    /**
     * Set the calledAET of the PACS to query.
     * 
     * @param calledAET The calledAET to set.
     */
    public void setCalledAET(String calledAET) {
        this.calledAET = calledAET;
    }

    /**
     * Get the calling AET used in Dicom Query.
     * @return Returns the callingAET.
     */
    public String getCallingAET() {
        return callingAET;
    }

    /**
     * set the calling AET used in Dicom Query.
     * @param callingAET The callingAET to set.
     */
    public void setCallingAET(String callingAET) {
        this.callingAET = callingAET;
    }

    /**
     * Get the Hostname or IP address of the PACS
     * 
     * @return Returns the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the Hostname or IP address of the PACS
     * 
     * @param host The host to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the DICOM port number of the PACS to query.
     * 
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the DICOM port number of the PACS to query.
     * 
     * @param port The port to set.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * List of SOP Class UIDs of images.
     * @return
     */
    public String getImageSOPClasses() {
        return toUIDListString(launchProperties.getImageCUIDs());
    }

    public void setImageSOPClasses(String s) {
        launchProperties.setImageCUIDs(parseUIDs(s));
    }

    /**
     * List of SOP Class UIDs of presentation states.
     * @return
     */
    public String getPresentationStateCUIDs() {
        return toUIDListString(launchProperties.getPresentationStateCUIDs());
    }

    public void setPresentationStateCUIDs(String s) {
        launchProperties.setPresentationStateCUIDs(parseUIDs(s));
    }

    /**
     * Config String to get launch parameter for List of SOPInstanceUIDs per series.
     * <p/>
     * Format:&lt;parametername&gt;|&lt;descr&gt;|&lt;descrValue&gt;|&lt;list seperator&gt;|&lt; uid seperator&gt;|&lt;postfix&gt;<br/>
     * descrValue: either value from a DICOM attribute (e.g. 0008103E or SeriesDescription) 
     *  or a numbering (#, #-1 or #a -&gt; 1,2,3,.. 0,1,2,.. or a,b,c,.. )<br/>
     * e.g.:SEQUENCE|Seq. Nr.|0008103E|;|;| --> SEQUENCE1="Seq. Nr.[seriesdescr];[UID1];[UID2]"
     * @return 
     */
    public String getParaSeriesInstances() {
        return launchProperties.getParaSeriesInstances();
    }

    public void setParaSeriesInstances(String s) {
        launchProperties.setParaSeriesInstances(s);
    }

    /**
     * Config String to get launch parameter for List of presentation states.
     * <p/>
     *  Format:&lt;parametername&gt;|&lt;descr&gt;|&lt;descrValue&gt;|&lt;list seperator&gt;|&lt; uid seperator&gt;|&lt;postfix&gt;<br/>
     *  descrValue: either value from a DICOM attribute (e.g. 0008103E or SeriesDescription) 
     *  or a numbering (#, #-1 or #a -&gt; 1,2,3,.. 0,1,2,.. or a,b,c,.. )<br/>
     *  e.g.:PR_UIDS||||;| --> PR_UIDS="[UID1];[UID2]"
    */
    public String getParaPresentationStates() {
        return launchProperties.getParaPresentationStates();
    }

    public void setParaPresentationStates(String s) {
        launchProperties.setParaPresentationStates(s);
    }

    /**
     * Get jar file of web viewer applet.
     * <p/>
     * @return Returns the appletArchive.
     */
    public String getAppletArchive() {
        return launchProperties.getAppletArchive();
    }

    /**
     * @param appletArchive The appletArchive to set.
     */
    public void setAppletArchive(String appletArchive) {
        launchProperties.setAppletArchive(appletArchive);
    }

    /**
     * Class name of web viewer applet.
     * 
     * @return Returns the appletClass.
     */
    public String getAppletClass() {
        return launchProperties.getAppletClass();
    }

    /**
     * @param appletClass The appletClass to set.
     */
    public void setAppletClass(String appletClass) {
        launchProperties.setAppletClass(appletClass);
    }

    /**
     * List of applet parameters with fixed values.<br/>
     *      Format:&lt;parametername&gt;=&lt;value&gt;<br/>
     *      Separate multible parameters with ';' or newline.
     * 
     * @return Returns the appletParameterMap.
     */
    public String getAppletParameterMap() {
        return toParameterListString(launchProperties.getAppletParameterMap());
    }

    /**
     * @param appletParameterMap The appletParameterMap to set.
     */
    public void setAppletParameterMap(String appletParameterMap) {
        launchProperties.setAppletParameterMap(parseParameterList(appletParameterMap));
    }

    /**
     * Name of a custom LaunchProperties class.
     * <p>
     * This class must implement CustomLaunchProperties and can be used to customize launch properties
     * for special needs of a webviewer applet.
     * <p>
     * As example the WebViewerProperties class is used to set a title and patient info for the 'WebViewer' applet.
     *  
     * @return Returns the customLaunchPropertiesClass.
     */
    public String getCustomLaunchPropertiesClass() {
        CustomLaunchProperties customProps = launchProperties.getCustomLaunchPropertiesClass();
        return customProps == null ? "NONE" : customProps.getClass().getName();
    }

    /**
     * @param customLaunchPropertiesClass The customLaunchPropertiesClass to set.
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void setCustomLaunchPropertiesClass(String customPropsName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        CustomLaunchProperties customProps = null;
        if (customPropsName != null && !customPropsName.equalsIgnoreCase("NONE")) {
            customProps = (CustomLaunchProperties) Class.forName(customPropsName).newInstance();
        }
        launchProperties.setCustomLaunchPropertiesClass(customProps);
    }

    /**
     * List of applet parameters with values from a DICOM attribute of the C-FIND result.
     * <p>
     * e.g.: patName=00100010 or patName=PatientName.
     * 
     * @return Returns the result2appletParameterMap.
     */
    public String getResult2ParameterMap() {
        return toParameterListString(launchProperties.getResult2appletParameterMap());
    }

    /**
     * @param result2ParameterMap The result2appletParameterMap to set.
     */
    public void setResult2ParameterMap(String result2ParameterMap) {
        launchProperties.setResult2appletParameterMap(parseParameterList(result2ParameterMap));
    }

    public String getInstanceComparatorClass() {
        InstanceComparator ic = launchProperties.getInstanceComparatorClass();
        return ic == null ? "NONE" : ic.getClass().getName();
    }

    public void setInstanceComparatorClass(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (className == null || "NONE".equalsIgnoreCase(className)) {
            launchProperties.setInstanceComparatorClass(null);
        } else {
            Class cl = Class.forName(className);
            launchProperties.setInstanceComparatorClass((InstanceComparator) cl.newInstance());
        }
    }

    public Properties getLaunchPropertiesForAccNr(String accNr, Boolean ignorePR, Boolean selectPR) {
        DicomObject keys = new BasicDicomObject();
        keys.putString(Tag.AccessionNumber, VR.SH, accNr);
        if (!keys.contains(Tag.PatientName)) keys.putNull(Tag.PatientName, null);
        if (!keys.contains(Tag.PatientBirthDate)) keys.putNull(Tag.PatientBirthDate, null);
        if (!keys.contains(Tag.StudyDescription)) keys.putNull(Tag.StudyDescription, null);
        return getLaunchPropertiesForQuery(keys, ignorePR, selectPR);
    }

    public Properties getLaunchPropertiesForPatId(String patId, Boolean ignorePR, Boolean selectPR) {
        DicomObject keys = new BasicDicomObject();
        String issuer = null;
        if (patId.indexOf('^') > 0) {
            int pos = patId.lastIndexOf('^');
            issuer = patId.substring(pos + 1);
            patId = patId.substring(0, patId.indexOf('^'));
        }
        keys.putString(Tag.PatientID, VR.LO, patId);
        keys.putString(Tag.IssuerOfPatientID, VR.LO, issuer);
        if (!keys.contains(Tag.PatientName)) keys.putNull(Tag.PatientName, null);
        if (!keys.contains(Tag.PatientBirthDate)) keys.putNull(Tag.PatientBirthDate, null);
        if (!keys.contains(Tag.StudyDescription)) keys.putNull(Tag.StudyDescription, null);
        return getLaunchPropertiesForQuery(keys, ignorePR, selectPR);
    }

    public Properties getLaunchProperties(String studyUID, String seriesUID, String iuid, Boolean ignorePR, Boolean selectPR) {
        DicomObject keys = new BasicDicomObject();
        keys.putString(Tag.StudyInstanceUID, VR.UI, studyUID);
        keys.putString(Tag.SeriesInstanceUID, VR.UI, seriesUID);
        keys.putString(Tag.SOPInstanceUID, VR.UI, iuid);
        return getLaunchPropertiesForQuery(keys, ignorePR, selectPR);
    }

    public Properties getLaunchPropertiesForPresentationState(String studyUID, String seriesUID, String instanceUID) {
        DicomObject keys = new BasicDicomObject();
        keys.putString(Tag.StudyInstanceUID, VR.UI, studyUID);
        keys.putString(Tag.SeriesInstanceUID, VR.UI, seriesUID);
        keys.putString(Tag.SOPInstanceUID, VR.UI, instanceUID);
        keys.putNull(new int[] { Tag.ReferencedSeriesSequence, 0, Tag.SeriesInstanceUID }, VR.UI);
        addResultAttributes(keys);
        InstanceContainer results = new UIDQuery(callingAET, calledAET, host, port).query(keys);
        InstanceContainer instances = null;
        if (results.isEmpty()) {
            log.info("Can't find PresentationState Object:" + instanceUID);
        } else {
            DicomObject prObj = (DicomObject) ((Collection) results.iterateSeries().next()).iterator().next();
            if (launchProperties.getPresentationStateCUIDs().containsValue(prObj.getString(Tag.SOPClassUID))) {
                instances = getPresentationStateInstanceContainer(prObj);
            } else {
                log.warn("Object is not a PresentationState Object:" + instanceUID);
            }
        }
        return launchProperties.getProperties(instances, false, false);
    }

    private InstanceContainer getPresentationStateInstanceContainer(DicomObject prObj) {
        InstanceContainer instances = new InstanceContainer();
        String studyUid = prObj.getString(Tag.StudyInstanceUID);
        DicomElement serSeq = prObj.get(Tag.ReferencedSeriesSequence);
        if (serSeq != null) {
            instances.add(prObj);
            DicomObject obj, imgItem, obj1;
            DicomElement imgSeq;
            String seriesUid;
            for (int i = 0, len = serSeq.countItems(); i < len; i++) {
                obj = serSeq.getDicomObject(i);
                seriesUid = obj.getString(Tag.SeriesInstanceUID);
                imgSeq = obj.get(Tag.ReferencedImageSequence);
                for (int j = 0, jLen = imgSeq.countItems(); j < jLen; j++) {
                    imgItem = imgSeq.getDicomObject(j);
                    obj1 = new BasicDicomObject();
                    obj1.putString(Tag.StudyInstanceUID, VR.UI, studyUid);
                    obj1.putString(Tag.SeriesInstanceUID, VR.UI, seriesUid);
                    obj1.putString(Tag.SOPInstanceUID, VR.UI, imgItem.getString(Tag.ReferencedSOPInstanceUID));
                    obj1.putString(Tag.SOPClassUID, VR.UI, imgItem.getString(Tag.ReferencedSOPClassUID));
                    instances.add(obj1);
                }
            }
        }
        return instances;
    }

    /**
     * Get launch properties for given DICOM Query.
     * 
     * @param keys      Query Dataset
     * @param ignorePR  Ignore presentation state on Study Level.
     * @param selectPR  Select Presentation state if more than one presentation state is found.
     * @return
     */
    public Properties getLaunchPropertiesForQuery(DicomObject keys, Boolean ignorePR, Boolean selectPR) {
        addResultAttributes(keys);
        boolean ignorePRflag = ignorePR == null ? ignorePRinStudy : ignorePR.booleanValue();
        boolean selectPRflag = selectPR == null ? selectMultiPR : selectPR.booleanValue();
        if (!ignorePRflag && selectPRflag) {
            keys.putNull(Tag.PresentationCreationDate, VR.DA);
            keys.putNull(Tag.PresentationCreationTime, VR.TM);
            keys.putNull(Tag.ContentDescription, VR.LO);
            keys.putNull(Tag.ContentLabel, VR.CS);
            keys.putNull(Tag.ContentCreatorName, VR.PN);
        }
        addReturnAttributes(keys, launchProperties.getInstanceComparatorClass());
        InstanceContainer results = new UIDQuery(callingAET, calledAET, host, port).query(keys);
        return launchProperties.getProperties(results, ignorePRflag, selectPRflag);
    }

    private void addReturnAttributes(DicomObject keys, InstanceComparator instanceComparator) {
        if (instanceComparator == null) return;
        int[] tags = instanceComparator.getTags();
        if (tags == null) return;
        for (int i = 0; i < tags.length; i++) {
            if (!keys.contains(tags[i])) keys.putNull(tags[i], null);
        }
    }

    public Properties getLaunchPropertiesForManifest(String manifestURL) throws MalformedURLException, IOException {
        DicomObject manifest = loadManifest(new URL(manifestURL));
        if (manifest == null) {
            log.info("Can't find Manifest:" + manifestURL);
        }
        return getLaunchPropertiesForManifest(manifest);
    }

    public Properties getLaunchPropertiesForManifest(DicomObject manifest) {
        InstanceContainer instances = null;
        if (manifest != null) {
            if (manifest.getString(Tag.SOPClassUID).equals(UID.KeyObjectSelectionDocument)) {
                instances = getKOSInstanceContainer(manifest);
            } else {
                log.warn("Object is not a manifest (Key Object Selection Document):" + manifest);
            }
        }
        return launchProperties.getProperties(instances, false, false);
    }

    private InstanceContainer getKOSInstanceContainer(DicomObject manifest) {
        InstanceContainer instances = new InstanceContainer();
        String patName = manifest.getString(Tag.PatientName);
        String birthDate = manifest.getString(Tag.PatientBirthDate);
        String sex = manifest.getString(Tag.PatientSex);
        String studyIUID, aet, seriesIUID, iuid, cuid;
        DicomElement evSq = manifest.get(Tag.CurrentRequestedProcedureEvidenceSequence);
        DicomElement refSerSq, refSopSq;
        DicomObject evSqItem, refSerItem, refSopItem, obj;
        for (int i = 0, len = evSq.countItems(); i < len; i++) {
            evSqItem = evSq.getDicomObject(i);
            studyIUID = evSqItem.getString(Tag.StudyInstanceUID);
            refSerSq = evSqItem.get(Tag.ReferencedSeriesSequence);
            for (int j = 0, lenj = refSerSq.countItems(); j < lenj; j++) {
                refSerItem = refSerSq.getDicomObject(j);
                seriesIUID = refSerItem.getString(Tag.SeriesInstanceUID);
                refSopSq = refSerItem.get(Tag.ReferencedSOPSequence);
                for (int k = 0, lenk = refSopSq.countItems(); k < lenk; k++) {
                    refSopItem = refSopSq.getDicomObject(k);
                    iuid = refSopItem.getString(Tag.ReferencedSOPInstanceUID);
                    cuid = refSopItem.getString(Tag.ReferencedSOPClassUID);
                    obj = new BasicDicomObject();
                    obj.putString(Tag.PatientName, VR.PN, patName);
                    obj.putString(Tag.PatientBirthDate, VR.DA, birthDate);
                    obj.putString(Tag.StudyInstanceUID, VR.CS, sex);
                    obj.putString(Tag.StudyInstanceUID, VR.UI, studyIUID);
                    obj.putString(Tag.SeriesInstanceUID, VR.UI, seriesIUID);
                    obj.putString(Tag.SOPInstanceUID, VR.UI, iuid);
                    obj.putString(Tag.SOPClassUID, VR.UI, cuid);
                    instances.add(obj);
                }
            }
        }
        return instances;
    }

    private DicomObject loadManifest(URL url) throws IOException {
        java.net.HttpURLConnection httpUrlConn = (java.net.HttpURLConnection) url.openConnection();
        InputStream bis = httpUrlConn.getInputStream();
        DicomObject manifest = null;
        try {
            DicomInputStream in = new DicomInputStream(bis);
            manifest = in.readDicomObject();
        } finally {
            try {
                bis.close();
            } catch (IOException ignore) {
            }
        }
        return manifest;
    }

    /**
     * Ignore Presentation States if launching a web viewer with study level (studyUID or accNr.
     * 
     * @return
     */
    public boolean isIgnorePresentationStateForStudies() {
        return ignorePRinStudy;
    }

    public void setIgnorePresentationStateForStudies(boolean ignorePresentationStateForStudies) {
        ignorePRinStudy = ignorePresentationStateForStudies;
    }

    /**
     * @return Returns the selectMultiPR.
     */
    public boolean isSelectMultiPR() {
        return selectMultiPR;
    }

    /**
     * @param selectMultiPR The selectMultiPR to set.
     */
    public void setSelectMultiPR(boolean selectMultiPR) {
        this.selectMultiPR = selectMultiPR;
    }

    /**
     * @param keys
     */
    private void addResultAttributes(DicomObject keys) {
        Map map = launchProperties.getResult2appletParameterMap();
        if (map != null && !map.isEmpty()) {
            int tag;
            for (Iterator iter = map.values().iterator(); iter.hasNext(); ) {
                tag = Tag.toTag((String) iter.next());
                if (!keys.contains(tag)) {
                    keys.putNull(tag, null);
                }
            }
        }
        if (launchProperties.getCustomLaunchPropertiesClass() != null) {
            int[][] resultAttrs = launchProperties.getCustomLaunchPropertiesClass().getResultAttributes();
            if (resultAttrs != null) {
                for (int i = 0; i < resultAttrs.length; i++) {
                    if (!keys.contains(resultAttrs[i][0])) {
                        keys.putNull(resultAttrs[i], null);
                    }
                }
            }
        }
    }

    protected void startService() throws Exception {
    }

    /**
     * @param appletParameterMap
     */
    private String toParameterListString(Map paraMap) {
        if (paraMap == null || paraMap.isEmpty()) return "NONE";
        String nl = System.getProperty("line.separator", "\n");
        StringBuffer sb = new StringBuffer();
        Iterator iter = paraMap.entrySet().iterator();
        Map.Entry entry;
        while (iter.hasNext()) {
            entry = (Map.Entry) iter.next();
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append(nl);
        }
        return sb.toString();
    }

    private static Map parseParameterList(String params) {
        Map map = new LinkedHashMap();
        if (params == null || params.length() < 1 || params.equalsIgnoreCase("NONE")) {
            return map;
        }
        StringTokenizer st = new StringTokenizer(params, "\t\r\n;");
        String tk;
        int pos;
        while (st.hasMoreTokens()) {
            tk = st.nextToken().trim();
            pos = tk.indexOf('=');
            if (pos != -1) {
                map.put(tk.substring(0, pos), tk.substring(pos + 1));
            } else {
                map.put(tk, "");
            }
        }
        return map;
    }

    private String toUIDListString(Map uids) {
        if (uids == null || uids.isEmpty()) return "";
        String nl = System.getProperty("line.separator", "\n");
        StringBuffer sb = new StringBuffer();
        Iterator iter = uids.keySet().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(nl);
        }
        return sb.toString();
    }

    private static Map parseUIDs(String uids) {
        StringTokenizer st = new StringTokenizer(uids, " \t\r\n;");
        String uid, name;
        Map map = new LinkedHashMap();
        while (st.hasMoreTokens()) {
            uid = st.nextToken().trim();
            name = uid;
            if (isDigit(uid.charAt(0))) {
                if (!UIDUtils.isValidUID(uid)) throw new IllegalArgumentException("UID " + uid + " isn't a valid UID!");
            } else {
                uid = UID.forName(name);
            }
            map.put(name, uid);
        }
        return map;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
