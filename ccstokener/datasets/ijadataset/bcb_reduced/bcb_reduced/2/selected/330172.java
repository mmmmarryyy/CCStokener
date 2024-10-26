package apollo.dataadapter.das2;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import javax.swing.JOptionPane;
import apollo.datamodel.*;
import apollo.config.Config;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.DataListener;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.NotImplementedException;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.das.*;
import apollo.dataadapter.das.simple.*;
import apollo.dataadapter.*;
import apollo.main.LoadUtil;
import org.apache.log4j.*;
import org.bdgp.io.*;
import org.bdgp.util.*;
import java.util.Properties;

/**
 * <p>I implement a <code>AbstractDataAdapter</code>, allowing Apollo to 
 * get information from DAS datasources, using 'simple'
 * implementions of <code>DASServerI</code>, <code>DASDsn</code> etc. </p>
**/
public class DAS2Adapter extends AbstractApolloAdapter {

    protected static final Logger logger = LogManager.getLogger(DAS2Adapter.class);

    private static long groupIdInt = 0;

    private DAS2AdapterGUI gui;

    String server;

    DASDsn dsn;

    DASSegment segment;

    private SequenceI genomeSeq = null;

    int low = 1;

    int high = 100000;

    private Properties stateInformation = new StateInformation();

    DAS2Request dasServer;

    String filename;

    /** This indicates no GUI so certain non-apollo, non-gui apps can use this
   *  adapter. (But who sets this?) */
    boolean NO_GUI = false;

    public DAS2Adapter() {
    }

    public DAS2Request getDASServer() {
        return dasServer;
    }

    public void setDASServer(DAS2Request newValue) {
        dasServer = newValue;
        logger.debug("DAS2Adapter.setDasServer: URL = " + dasServer.getURL());
    }

    IOOperation[] supportedOperations = { ApolloDataAdapterI.OP_READ_DATA, ApolloDataAdapterI.OP_READ_SEQUENCE, ApolloDataAdapterI.OP_WRITE_DATA };

    public void init() {
    }

    public DASDsn getDSN() {
        return dsn;
    }

    public String getName() {
        return "DAS/2 server";
    }

    public String getType() {
        return "CGI server";
    }

    public DataInputType getInputType() {
        return DataInputType.URL;
    }

    public void setInput(String filename) {
        logger.debug("setInput: filename = " + filename);
        this.filename = filename;
    }

    public String getInput() {
        return filename;
    }

    /** Used when Apollo was called from the command line rather than the GUI */
    public void setDataInput(DataInput dataInput) {
        super.setDataInput(dataInput);
        setInput(dataInput.getInputString());
    }

    public IOOperation[] getSupportedOperations() {
        return supportedOperations;
    }

    public DataAdapterUI getUI(IOOperation op) {
        if (gui == null) gui = new DAS2AdapterGUI(op);
        gui.setIOOperation(op);
        return gui;
    }

    private void setDSN(DASDsn dsn) {
        logger.debug("DAS2Adapter.setDSN: dsn = " + dsn + "; capabilities = " + ((SimpleDASDsn) dsn).getCapabilities());
        this.dsn = dsn;
    }

    private void setSegment(DASSegment segment) {
        this.segment = segment;
        logger.trace("DAS2Adapter.setSegment: segment = " + segment);
    }

    private DASSegment getSegment() {
        return segment;
    }

    private void setLow(int low) {
        this.low = low;
    }

    private void setHigh(int high) {
        this.high = high;
    }

    private int getLow() {
        return low;
    }

    private int getHigh() {
        return high;
    }

    /**
   * This string is used when the user navigates around the genome - it should
   * contain both the new segment and high/low information. Armed with this, we can set the
   * DASSegment in this data adapter. The new data is set into the adapter with
   * two calls: setStateInformation (copying in the "old" state of the adapter
   * and then setRegion (copying in the new choices of region into the
   * adapter).
  **/
    public void setRegion(String region) throws ApolloAdapterException {
        SimpleDASSegment segment = (SimpleDASSegment) getSegment();
        logger.debug("DAS2Adapter.setRegion: region = " + region);
        if (region == null) {
            return;
        }
        StringTokenizer tokenizer = new StringTokenizer(region);
        String chrString = null;
        String chromosome = null;
        String start = null;
        String end = null;
        if (tokenizer.hasMoreTokens()) {
            chrString = tokenizer.nextToken();
        }
        if (tokenizer.hasMoreTokens()) {
            chromosome = tokenizer.nextToken();
        }
        if (tokenizer.hasMoreTokens()) {
            start = tokenizer.nextToken();
        }
        if (tokenizer.hasMoreTokens()) {
            end = tokenizer.nextToken();
        }
        if (chromosome == null || start == null || end == null) {
            return;
        }
        if (segment != null) {
            segment.setId(chromosome);
            segment.setStart(start);
            segment.setStop(end);
        } else {
            setSegment(new SimpleDASSegment(null, chromosome, start, end, null, null, null));
        }
        setLow(Integer.valueOf(start).intValue());
        setHigh(Integer.valueOf(end).intValue());
        getStateInformation().setProperty(StateInformation.REGION, region);
        getStateInformation().setProperty(StateInformation.SEGMENT_START, start);
        getStateInformation().setProperty(StateInformation.SEGMENT_STOP, end);
    }

    /**
   * The adapter is characterised by dsn (e.g. http://url/.../entry_point)
   * and segment (e.g. chr=1, start=... stop=....)
   * With this information we can make a das-call. So we write
   * this information out in entirety.
  **/
    public Properties getStateInformation() {
        return stateInformation;
    }

    /**
   * <p>With the following keys in the adapter properties, we can re-create a 
   * our internal state and start a new das-call.</p>
   * 
   * <ul>
   * <li> DAS_server_url </li>
   * <li> DSN_sourceId </li>
   * <li> DSN_source </li>
   * <li> DSN_sourceVersion </li>
   * <li> DSN_mapMaster </li>
   * <li> Segment_segment </li>
   * <li> Segment_id </li>
   * <li> Segment_start </li>
   * <li> Segment_stop </li>
   * <li> Segment_orientation </li>
   * <li> Segment_subparts </li>
   * <li> Segment_length </li>
   * </ul>
   *
  **/
    public void setStateInformation(Properties newProperties) {
        String proxySet;
        String proxyHost;
        String proxyPort;
        Properties props = getStateInformation();
        String dsnSourceId;
        String segmentId;
        String lowString;
        String highString;
        props.putAll(newProperties);
        if (props.getProperty(StateInformation.SERVER_URL) != null) {
            setDASServer(new DAS2Request(props.getProperty(StateInformation.SERVER_URL)));
        }
        dsnSourceId = props.getProperty(StateInformation.DSN_SOURCE_ID);
        logger.trace("setStateInformation: dsnSourceId = " + dsnSourceId);
        if (dsnSourceId != null && dsnSourceId.trim().length() > 0) {
            DAS2Request server = getDASServer();
            DASDsn dsn = server.getDSN(dsnSourceId);
            if (dsn == null) {
                logger.debug("DAS2Adapter: setting DSN to a new simpleDASDsn based on properties");
                setDSN(new SimpleDASDsn(dsnSourceId, props.getProperty(StateInformation.DSN_SOURCE_VERSION), props.getProperty(StateInformation.DSN_SOURCE), props.getProperty(StateInformation.DSN_MAP_MASTER), props.getProperty(StateInformation.DSN_DESCRIPTION)));
            } else setDSN(dsn);
        }
        segmentId = props.getProperty(StateInformation.SEGMENT_SEGMENT);
        if (segmentId != null && segmentId.trim().length() > 0) {
            setSegment(new SimpleDASSegment(segmentId, props.getProperty(StateInformation.SEGMENT_ID), props.getProperty(StateInformation.SEGMENT_START), props.getProperty(StateInformation.SEGMENT_STOP), props.getProperty(StateInformation.SEGMENT_ORIENTATION), props.getProperty(StateInformation.SEGMENT_SUBPARTS), props.getProperty(StateInformation.SEGMENT_LENGTH)));
        } else logger.debug("DAS2Adapter.setStateInformation: segment id is null!");
        lowString = props.getProperty(StateInformation.SEGMENT_START);
        if (lowString != null && lowString.trim().length() > 0) {
            logger.trace("setStateInformation: setLow to " + lowString);
            setLow(Integer.valueOf(lowString).intValue());
        }
        highString = props.getProperty(StateInformation.SEGMENT_STOP);
        if (highString != null && highString.trim().length() > 0) {
            setHigh(Integer.valueOf(highString).intValue());
        }
        proxySet = props.getProperty(StateInformation.HTTP_PROXY_SET);
        if (proxySet != null && "true".equals(proxySet)) {
            proxyHost = props.getProperty(StateInformation.HTTP_PROXY_HOST);
            proxyPort = props.getProperty(StateInformation.HTTP_PROXY_PORT);
            System.setProperty("proxySet", "true");
            System.setProperty("proxyHost", proxyHost);
            System.setProperty("proxyPort", proxyPort);
        }
        if (props.getProperty(StateInformation.REGION) != null) {
            try {
                setRegion(props.getProperty(StateInformation.REGION));
            } catch (ApolloAdapterException exception) {
                throw new NonFatalDataAdapterException(exception.getMessage());
            }
        }
        stateInformation = props;
    }

    public CurationSet getCurationSet() throws ApolloAdapterException {
        CurationSet curation = null;
        BufferedInputStream bis = null;
        try {
            fireProgressEvent(new ProgressEvent(this, new Double(5.0), "Finding data..."));
            InputStream xml_stream;
            xml_stream = getDAS2XMLStream();
            bis = new BufferedInputStream(xml_stream);
            if (!NO_GUI) super.clearOldData();
            fireProgressEvent(new ProgressEvent(this, new Double(10.0), "Reading XML..."));
        } catch (Exception e) {
            logger.error("Error trying to open XML stream: " + e.getMessage(), e);
            return null;
        }
        try {
            curation = new CurationSet();
            DAS2FeatureSaxParser parser = new DAS2FeatureSaxParser();
            parser.parse(bis, curation);
            bis.close();
            setCurationRegion(curation);
            return curation;
        } catch (Exception e) {
            logger.error("Error trying to parse XML: " + e.getMessage(), e);
            return null;
        }
    }

    /** If we've requested a file, open a stream to it.
   *  Otherwise, open a stream to the URL. */
    private InputStream getDAS2XMLStream() throws ApolloAdapterException {
        DASDsn theDSN = getDSN();
        logger.debug("DAS2Adapter.getDAS2XMLStream: theDSN = " + theDSN);
        if (theDSN == null) {
            return das2XmlFileInputStream(getInput());
        }
        String query = makeFeatureQuery();
        logger.debug("DAS2Adapter.getDAS2XMLStream: feature query = " + query);
        try {
            URL url = new URL(query);
            InputStream stream = url.openStream();
            Thread.sleep(50);
            if (stream.available() <= 1) {
                logger.error("Couldn't fetch requested region: " + query);
                return null;
            }
            return stream;
        } catch (Exception e) {
            logger.error("Couldn't open stream to " + query);
            throw new ApolloAdapterException("Couldn't open stream to " + query);
        }
    }

    private String getRegionString(DASSegment segment) {
        return segment.getSegment() + "/" + low + ":" + high;
    }

    /** Construct a name for the Apollo window based on a feature query URL like
   *  http://das.biopackages.net/das/genome/yeast/S228C/feature?overlaps=chrI/1111:2222 */
    private String makeRegionName(String featureQueryURL) {
        return featureQueryURL;
    }

    /** The feature query should be constructed by using the CAPABILITY for
   *  this source from the SOURCE request, e.g.
   *  <CAPABILITY type="features" query_id="volvox/1/features"> */
    private String makeFeatureQuery() {
        String region = getRegionString(getSegment());
        SimpleDASDsn dsn = (SimpleDASDsn) getDSN();
        String featureRequest = dsn.getCapabilityURI("features");
        if (featureRequest == null) {
            String warning = "Warning: couldn't get 'features' capability for " + dsn.getSourceId() + "; capabilities hash = " + dsn.getCapabilities() + "\nUsing hardcoded query 'feature'.";
            logger.warn(warning);
            JOptionPane.showMessageDialog(null, warning, "Warning", JOptionPane.WARNING_MESSAGE);
            featureRequest = "feature";
        }
        String baseURL = dsn.getMapMaster();
        if (!baseURL.endsWith("/")) baseURL = baseURL + "/";
        return baseURL + featureRequest + "?overlaps=" + region;
    }

    /** Open the requested file as a stream; check that file really does appear to contain
   *  DAS2XML before returning stream. */
    private InputStream das2XmlFileInputStream(String filename) throws ApolloAdapterException {
        if (filename == null) throw new ApolloAdapterException("No filename or DAS/2 source specified");
        InputStream stream = null;
        String path = apollo.util.IOUtil.findFile(filename, false);
        try {
            fireProgressEvent(new ProgressEvent(this, new Double(6.0), "Opening file..."));
            logger.debug("Trying to open DAS2XML file " + filename);
            stream = new FileInputStream(path);
        } catch (Exception e) {
            stream = null;
            throw new ApolloAdapterException("could not open DAS2XML file " + filename + " for reading.");
        }
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(path));
        } catch (Exception e) {
            stream = null;
            throw new ApolloAdapterException("Error: could not open DAS2XML file " + path + " for reading.");
        }
        fireProgressEvent(new ProgressEvent(this, new Double(7.0), "Validating file..."));
        for (int i = 0; i < 12; i++) {
            String line;
            try {
                line = in.readLine();
            } catch (Exception e) {
                throw new ApolloAdapterException("Error: DAS2XML file " + filename + " is empty.");
            }
            if (line.toLowerCase().indexOf("das2") >= 0) return stream;
            if (line.toLowerCase().indexOf("das/genome/2") >= 0) return stream;
            if (line.toLowerCase().indexOf("das/2.") >= 0) return stream;
        }
        throw new ApolloAdapterException("Error: file " + filename + " does not appear to contain DAs2XML.");
    }

    /** Not currently used */
    private List parseFeatureSet() {
        StrandedFeatureSetI parentStandedFeatureSet;
        DASSegment inputSegment;
        DASDsn theDSN = getDSN();
        logger.debug("DAS2Adapter.parseFeatureSet: theDSN = " + theDSN);
        inputSegment = getSegment();
        return getDASServer().getFeatures(theDSN, new DASSegment[] { inputSegment });
    }

    private void initializeMethodNameFeatureTypeFeatureSet(FeatureSetI targetFeatureSet, DASFeature theSourceFeature) {
        targetFeatureSet.setId(theSourceFeature.getTypeId());
        targetFeatureSet.setStrand(getStrandForOrientation(theSourceFeature.getOrientation()));
        targetFeatureSet.setProgramName(theSourceFeature.getMethodLabel());
        targetFeatureSet.setDatabase(theSourceFeature.getTypeId());
        targetFeatureSet.setFeatureType(getMethodAndFeatureType(theSourceFeature));
        targetFeatureSet.setName(theSourceFeature.getTypeLabel());
    }

    private void initializeGroupFeatureSet(FeatureSetI groupFeatureSet, DASFeature theFeature, boolean setIsHolder) {
        String methodAndFeatureType = getMethodAndFeatureType(theFeature);
        groupFeatureSet.setStrand(getStrandForOrientation(theFeature.getOrientation()));
        groupFeatureSet.setFeatureType(getMethodAndFeatureType(theFeature));
        if (theFeature.getGroupId() != null) {
            groupFeatureSet.setId(theFeature.getGroupId());
            groupFeatureSet.setName(theFeature.getGroupId());
        } else {
            groupFeatureSet.setId(theFeature.getId());
            groupFeatureSet.setName(theFeature.getId());
        }
        if (theFeature.getScore() != null) {
            groupFeatureSet.setScore(Double.parseDouble(theFeature.getScore()));
        }
    }

    private void addFeaturePairToGroupFeatureSet(FeatureSetI groupFeatureSet, DASFeature theFeature) {
        SeqFeatureI sf_1 = new SeqFeature();
        SeqFeatureI sf_2 = new SeqFeature();
        FeaturePair featurePair = new FeaturePair(sf_1, sf_2);
        String featureId = theFeature.getId();
        String methodLabelFeatureType;
        String orientation = theFeature.getOrientation();
        String score = theFeature.getScore();
        String start = theFeature.getStart();
        String end = theFeature.getEnd();
        String targetId = theFeature.getTargetId();
        String targetStart = theFeature.getTargetStart();
        String targetStop = theFeature.getTargetStop();
        methodLabelFeatureType = getMethodAndFeatureType(theFeature);
        sf_1.setId(featureId);
        sf_2.setId(featureId);
        sf_1.setFeatureType(methodLabelFeatureType);
        sf_2.setFeatureType(methodLabelFeatureType);
        if (score != null) {
            sf_1.setScore(Double.parseDouble(score));
            sf_2.setScore(Double.parseDouble(score));
        }
        if (targetId != null) {
            sf_1.setName(targetId);
            sf_2.setName(targetId);
            sf_2.setStrand(1);
            sf_2.setLow(Integer.parseInt(targetStart));
            sf_2.setHigh(Integer.parseInt(targetStop));
            sf_2.setRefSequence(groupFeatureSet.getHitSequence());
        } else {
            sf_1.setName(featureId);
            sf_2.setName(featureId);
        }
        sf_1.setStrand(getStrandForOrientation(orientation));
        if (start != null) {
            sf_1.setLow(Integer.parseInt(start));
        }
        if (end != null) {
            sf_1.setHigh(Integer.parseInt(end));
        }
        sf_1.setRefSequence(groupFeatureSet.getHitSequence());
        featurePair = new FeaturePair(sf_1, sf_2);
        if (groupFeatureSet.getId() != null) {
            featurePair.setName(groupFeatureSet.getId() + " span " + groupFeatureSet.size());
        } else {
            featurePair.setName(" span " + groupFeatureSet.size());
        }
        groupFeatureSet.addFeature(featurePair);
    }

    /**
   * <p>
   * Make a call to the chosen DSN, passing in the chosen segment/range into a 
   * das "features" command. Parse the result into a nested heirarchy of FeatureSets.
   * </p>
   * <p>Level 1 - StrandedFeatureSet. Common parent to all lower level sets</p>
   * <p>Level 2 - FeatureSets keyed by Method Name and Feature Type</p>
   * <p>Level 3 - FeatureSets for the parent Method and Type, keyed by GroupId, containing max score within group,
   * and having handles to aligned sequences </p>
   * <p>Level 4 - FeaturePairs for the parent Group, containing start, end, 
   * target start, target end.</p>
  **/
    public StrandedFeatureSetI getAnalysisRegion(CurationSet curation) throws ApolloAdapterException {
        List allDASFeatures;
        DASFeature theFeature;
        StrandedFeatureSetI parentStrandedFeatureSet = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
        HashMap methodNameFeatureTypes = new HashMap();
        HashMap groupsForAMethodNameAndFeatureType;
        String methodNameFeatureTypeAndOrientation;
        FeatureSetI methodNameFeatureTypeFeatureSet = null;
        String groupIdAndOrientation;
        FeatureSetI groupFeatureSet = null;
        double numberOfFeatures = 0;
        double featureCount = 0;
        double currentPercentage;
        double percentFeaturesLastDisplayed = 0;
        String groupId;
        boolean initializeAsHolder = false;
        fireProgressEvent(new ProgressEvent(this, new Double(0.0), "Getting features from DASServer"));
        allDASFeatures = parseFeatureSet();
        numberOfFeatures = allDASFeatures.size();
        fireProgressEvent(new ProgressEvent(this, new Double(50.0), "Building browser image"));
        parentStrandedFeatureSet = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
        parentStrandedFeatureSet.setName("Analyses");
        Iterator features = allDASFeatures.iterator();
        while (features.hasNext()) {
            theFeature = (DASFeature) features.next();
            methodNameFeatureTypeAndOrientation = getMethodAndFeatureType(theFeature) + theFeature.getOrientation();
            if (methodNameFeatureTypes.get(methodNameFeatureTypeAndOrientation) == null) {
                methodNameFeatureTypeFeatureSet = new FeatureSet();
                initializeMethodNameFeatureTypeFeatureSet(methodNameFeatureTypeFeatureSet, theFeature);
                groupsForAMethodNameAndFeatureType = new HashMap();
                methodNameFeatureTypes.put(methodNameFeatureTypeAndOrientation, new Object[] { methodNameFeatureTypeFeatureSet, groupsForAMethodNameAndFeatureType });
                parentStrandedFeatureSet.addFeature(methodNameFeatureTypeFeatureSet);
            } else {
                Object[] setAndHashMap = (Object[]) methodNameFeatureTypes.get(methodNameFeatureTypeAndOrientation);
                methodNameFeatureTypeFeatureSet = (FeatureSet) setAndHashMap[0];
                groupsForAMethodNameAndFeatureType = (HashMap) setAndHashMap[1];
            }
            groupId = theFeature.getGroupId();
            if (groupId != null && groupId.trim().length() > 0) {
                groupIdAndOrientation = theFeature.getGroupId() + theFeature.getOrientation();
                initializeAsHolder = false;
            } else {
                groupIdInt++;
                groupIdAndOrientation = theFeature.getId() + " " + groupIdInt;
                initializeAsHolder = true;
            }
            if (groupsForAMethodNameAndFeatureType.get(groupIdAndOrientation) == null) {
                groupFeatureSet = new FeatureSet();
                initializeGroupFeatureSet(groupFeatureSet, theFeature, initializeAsHolder);
                groupsForAMethodNameAndFeatureType.put(groupIdAndOrientation, groupFeatureSet);
                methodNameFeatureTypeFeatureSet.addFeature(groupFeatureSet);
            } else {
                groupFeatureSet = (FeatureSet) groupsForAMethodNameAndFeatureType.get(groupIdAndOrientation);
            }
            addFeaturePairToGroupFeatureSet(groupFeatureSet, theFeature);
        }
        parentStrandedFeatureSet.setRefSequence(curation.getRefSequence());
        return parentStrandedFeatureSet;
    }

    public FeatureSetI getAnnotatedRegion() throws ApolloAdapterException {
        return new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    }

    public SequenceI getSequence(String id) throws ApolloAdapterException {
        throw new NotImplementedException();
    }

    /**
   * <p>The dbxref passed into this argument is only checked to make sure
   * that the the IdValue it contains is the same as the Id for the DASSegment
   * that this adapter is poised on. The range etc passed into the lazy sequence
   * are determined by the Segment that this adapter already has stored on it. </p>
   *
   * <p> Note carefully: the sequence should NOT be fetched from the DASServer and 
   * DASDSN set on this adapter: that Server/DSN may not be a reference server. Instead, 
   * we must to go to the server and DSN referenced by the 'MapMaster' - this
   * points to the URL of the reference server, which will have the raw sequence. </p>
   *
   * 3/2006: Not currently used
  **/
    public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException {
        logger.error("getSequence(" + dbxref + "): don't yet know how to get sequence");
        return null;
    }

    public SequenceI getSequence(DbXref dbxref, int start, int end) throws ApolloAdapterException {
        throw new NotImplementedException();
    }

    public Vector getSequences(DbXref[] dbxref) throws ApolloAdapterException {
        throw new NotImplementedException();
    }

    public Vector getSequences(DbXref[] dbxref, int[] start, int[] end) throws ApolloAdapterException {
        throw new NotImplementedException();
    }

    public String getRawAnalysisResults(String id) throws ApolloAdapterException {
        throw new NotImplementedException();
    }

    /**
   * Direct conversion of a DAS feature orientation +,-,0 into +1, -1, 0.
  **/
    private int getStrandForOrientation(String orientation) {
        if (orientation.equals("+")) {
            return 1;
        } else if (orientation.equals("-")) {
            return -1;
        } else if (orientation.equals("0")) {
            return 0;
        } else {
            throw new apollo.dataadapter.NonFatalDataAdapterException("Received orientation: " + orientation + "-- I only accept +,-,0");
        }
    }

    /**
   * Concatenates a DAS Feature's method label (if not null) and feature type
   * into a string, separated by a colon.
  **/
    private String getMethodAndFeatureType(DASFeature theFeature) {
        if (theFeature.getMethodLabel() != null && theFeature.getMethodLabel().trim().length() > 0) {
            return theFeature.getMethodLabel() + ":" + theFeature.getTypeId();
        } else {
            return theFeature.getTypeId();
        }
    }

    public void clearStateInformation() {
        stateInformation = new StateInformation();
    }

    public void validateStateInformation() {
        String region = getStateInformation().getProperty(StateInformation.REGION);
        String lowText = getStateInformation().getProperty(StateInformation.SEGMENT_START);
        String highText = getStateInformation().getProperty(StateInformation.SEGMENT_STOP);
        if (getDSN() == null) throw new apollo.dataadapter.NonFatalDataAdapterException("getDSN returned null");
        if (getSegment() == null) throw new apollo.dataadapter.NonFatalDataAdapterException("getSegment returned null.  region = " + region);
        if (lowText == null || highText == null) {
            throw new NonFatalDataAdapterException("Low/High range must be specified");
        }
    }

    /** Curation set needs top-level info--chromosome, start, end.
   *  FOR NOW, get these from the first and last results. */
    private void setCurationRegion(CurationSet curation) {
        if (getSegment() != null) curation.setChromosome(getSegment().getId());
        if (getDSN() == null) curation.setName(getInput()); else curation.setName(makeRegionName(makeFeatureQuery()));
        StrandedFeatureSetI results = curation.getResults();
        SeqFeatureI firstresult = results.getFeatureAt(0);
        if (firstresult == null) {
            logger.warn("DAS2Adapter.setCurationRegion: no results!");
            return;
        }
        curation.setLow(firstresult.getLow());
        SeqFeatureI lastresult = results.getFeatureAt(results.getFeatures().size() - 1);
        int padding = (lastresult.getHigh() - lastresult.getLow()) / 20;
        curation.setHigh(lastresult.getHigh() + padding);
        logger.debug("Set region to " + curation.getChromosome() + ":" + curation.getLow() + "-" + curation.getHigh() + " + " + padding + " based on extent of first and last results");
        curation.setStrand(1);
    }

    /** Main method for writing DAS2XML file */
    public void commitChanges(CurationSet curation) {
        String filename = apollo.util.IOUtil.findFile(getInput(), true);
        if (filename == null) filename = getInput();
        if (filename == null) return;
        if (Config.getConfirmOverwrite()) {
            File handle = new File(filename);
            if (handle.exists()) {
                if (!LoadUtil.areYouSure(filename + " already exists--overwrite?")) {
                    apollo.main.DataLoader loader = new apollo.main.DataLoader();
                    loader.saveFileDialog(curation);
                    return;
                }
            }
        }
        setInput(filename);
        String msg = "Saving data to file " + filename;
        fireProgressEvent(new ProgressEvent(this, new Double(10.0), msg));
        if (DAS2Writer.writeXML(curation, filename, "Apollo version: " + apollo.main.Version.getVersion())) {
            logger.info("Saved DAS2XML to " + filename);
        } else {
            String message = "Failed to save DAS2XML to " + filename;
            logger.error(message);
            JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** For testing directly from the command line--give a DAS2XML file as the argument. */
    public static void main(String[] args) {
        DAS2FeatureSaxParser featureParser = new DAS2FeatureSaxParser();
        try {
            String test_file_name = args[0];
            File test_file = new File(test_file_name);
            FileInputStream fistr = new FileInputStream(test_file);
            BufferedInputStream bis = new BufferedInputStream(fistr);
            logger.info("Opened input stream to " + test_file_name);
            CurationSet curation = new CurationSet();
            curation.setName(test_file_name);
            featureParser.parse(bis, curation);
            bis.close();
            DAS2Adapter adapter = new DAS2Adapter();
            adapter.setInput(test_file_name);
            adapter.setCurationRegion(curation);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
