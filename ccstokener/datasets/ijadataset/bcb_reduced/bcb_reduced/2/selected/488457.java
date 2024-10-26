package org.embl.ebi.SoaplabServer.gowlab;

import org.embl.ebi.SoaplabServer.*;
import org.embl.ebi.SoaplabShare.*;
import org.embl.ebi.analysis.*;
import org.embl.ebi.analysis.events.*;
import embl.ebi.tools.*;
import embl.ebi.utils.*;
import embl.ebi.soap.SOAPToolkit;
import HTTPClient.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * An implementation of a Job interface where a "Job" means to fetch a
 * web page, and extracting useful data from it. <p>
 *
 * @author <A HREF="mailto:senger@ebi.ac.uk">Martin Senger</A>
 * @version $Id: JobGowlab.java,v 1.1.1.1 2005/06/15 09:00:19 marsenger Exp $
 */
public class JobGowlab implements Job {

    String id;

    protected embl.ebi.tools.Log log;

    boolean localized = false;

    boolean beingLocalized = false;

    boolean terminated = false;

    long created = -1;

    long started = -1;

    long ended = -1;

    long elapsed = -1;

    protected Map inputs = new Hashtable();

    protected PersistenceManager percy;

    protected boolean jobRemoved = false;

    protected AnalysisMetadataAccessor metadataAccessor;

    protected ResultManager resultManager;

    protected EventManager eventManager;

    protected String lastEvent;

    protected SOAPToolkit toolkit;

    HTTPConnection con;

    /**************************************************************************
     * The constructor that gets all input data.
     *
     * @param id is assigned by a JobManager.
     * @param inputs contain all input data (but may be empty now)
     * @param metadataAccessor gives access to metadata describing
     * this analysis
     * @param toolkit gives access to the surrounding servlet container
     * @param percy is a persistence manager which can store and
     * retrieve this job's results and other characterictics.
     **************************************************************************/
    public JobGowlab(String id, Map inputs, AnalysisMetadataAccessor metadataAccessor, SOAPToolkit toolkit, PersistenceManager percy) throws SoaplabException {
        this.id = id;
        this.inputs = inputs;
        this.metadataAccessor = metadataAccessor;
        this.percy = percy;
        this.toolkit = toolkit;
        log = SoaplabUtils.createLog(id, toolkit);
        log.msg("Gowlab job created.");
        try {
            percy.setJob(this);
        } catch (Exception e) {
            PersistenceManagerImpl.percyWarning(log, e);
        }
        eventManager = new EventManagerImpl(log);
        resultManager = new ResultManagerImpl(this, metadataAccessor, new SupportedDataTypesGowlab(), percy, createPropertiesForResultManager());
        resultManager.setState(new JobState(JobState.CREATED, eventManager));
        created = System.currentTimeMillis();
    }

    protected Map createPropertiesForResultManager() {
        Hashtable props = new Hashtable();
        props.put(ResultManagerImpl.RM_LOG, log);
        String resultDir = toolkit.getAttribute(ResultManagerImpl.RM_RESULT_DIR);
        if (resultDir != null) props.put(ResultManagerImpl.RM_RESULT_DIR, resultDir);
        return props;
    }

    /**************************************************************************
     * A constructor used for a re-creation of a JobGowlab instance
     * using data stored in a local database. <p>
     *
     * A job re-created by this constructor is good only for
     * retrieving old results (which are already in the local
     * database) - for anything else it does not have enough
     * information (it does not have access to its metadata so it
     * cannot run anything, it does not have its input data). <p>
     *
     * Note that this constructor is designed to work closely with the
     * PersistenceManager, and it is unconvenient to be used for
     * anything else.
     **************************************************************************/
    public JobGowlab(String id, SOAPToolkit toolkit, PersistenceManager percy, boolean localized, String status, long created, long started, long ended, long elapsed, String lastEvent) throws SoaplabException {
        this.id = id;
        this.percy = percy;
        this.toolkit = toolkit;
        log = SoaplabUtils.createLog(id, toolkit);
        log.msg("Gowlab job re-created.");
        resultManager = new ResultManagerImpl(this, null, null, percy, createPropertiesForResultManager());
        this.localized = localized;
        resultManager.setState(JobState.checkAndGetState(status));
        this.created = created * 1000;
        this.started = started * 1000;
        this.ended = ended * 1000;
        this.elapsed = elapsed;
        this.lastEvent = lastEvent;
    }

    /**************************************************************************
     * Set one input value. This should be used only if this job was
     * created initially with an empty input set (in other words it is
     * not allowed to add new inputs into already created one unless
     * it was created empty).
     **************************************************************************/
    public void setInputValue(String inputName, java.lang.Object value) throws SoaplabException {
        if (localized) return;
        if (inputName == null) throw new SoaplabException("setInputValue: No name of input data was given.");
        if (value == null) throw new SoaplabException("Null value encoutered in input data '" + inputName + "'.");
        inputs.put(inputName, value);
    }

    /**************************************************************************
     * Get this job ID.
     **************************************************************************/
    public String getId() {
        return id;
    }

    /**************************************************************************
     * Has this job finished?
     **************************************************************************/
    public boolean isTerminated() {
        return terminated;
    }

    /**************************************************************************
     * Run it.
     **************************************************************************/
    public synchronized void run() throws SoaplabException {
        if (localized || (resultManager.getState().get() != JobState.CREATED)) throw new SoaplabException(AnalysisWS.FAULT_NOT_RUNNABLE);
        checkInputs();
        log.debug("Inputs check passed (" + inputs.size() + " inputs).");
        GowlabData data = convertInputs();
        log.debug("Inputs converted.");
        try {
            RunPlugIn fetcher = null;
            AnalysisDef analysis = metadataAccessor.getAnalysisDef();
            String supplier = analysis.get(AnalysisWS.ANALYSIS_SUPPLIER);
            HTTPClient.URI supplierURI = new HTTPClient.URI(supplier);
            con = new HTTPConnection(supplierURI);
            Properties analysisProps = new Properties();
            analysisProps.put(RunPlugIn.SERVICE_NAME, analysis.appName);
            String debugStr = toolkit.getAttribute(RunPlugIn.DEBUG);
            if (debugStr != null) analysisProps.put(RunPlugIn.DEBUG, debugStr);
            String launcher = metadataAccessor.getEventDef().actions[0].launcher;
            if (launcher != null) {
                String l = launcher.toLowerCase();
                if (l.equals(RunPlugIn.METHOD_GET)) analysisProps.put(RunPlugIn.PREFERRED_METHOD, RunPlugIn.METHOD_GET); else if (l.equals(RunPlugIn.METHOD_POST)) analysisProps.put(RunPlugIn.PREFERRED_METHOD, RunPlugIn.METHOD_POST); else if (l.endsWith(".xsl")) fetcher = new RunPlugInXSLTImpl(l, toolkit.getAttribute(RunPlugIn.XSLT_DIR)); else if (l.endsWith(".pl") || l.endsWith(".sh")) fetcher = new RunPlugInExternalImpl(l); else if (l.endsWith(".external")) fetcher = new RunPlugInExternalImpl(l.substring(0, l.length() - 10)); else fetcher = (RunPlugIn) ICreator.createInstance(launcher);
            }
            StringBuffer buf = new StringBuffer(50);
            OutputPropertyDef[] opd = metadataAccessor.getOutputDefs();
            for (int i = 0; i < opd.length; i++) {
                if (opd[i].name.equals(ResultManager.RESULT_REPORT)) continue;
                if (opd[i].name.equals(ResultManager.RESULT_DETAILED_STATUS)) continue;
                if (buf.length() > 0) buf.append(",");
                buf.append(opd[i].name);
            }
            analysisProps.put(RunPlugIn.RESULT_NAMES, new String(buf));
            if (fetcher == null) fetcher = new RunPlugInDefaultImpl();
            ExecuteThread executeThread = new ExecuteThread(fetcher, supplierURI, data.formData, data.fileData, analysisProps);
            resultManager.getState().set(JobState.RUNNING);
            executeThread.start();
            new LazyResultFetcherThread().start();
        } catch (AnalysisException e) {
            logAndThrow(e.getMessage());
        } catch (embl.ebi.utils.GException e) {
            logAndThrow("Loading RunPlugIn class failed. " + e.getMessage());
        } catch (HTTPClient.ProtocolNotSuppException e) {
            logAndThrow("Supplier uses an unsupported protocol. " + e.getMessage());
        } catch (HTTPClient.ParseException e) {
            logAndThrow("Supplier is invalid. " + e.getMessage());
        }
    }

    /**************************************************************************
     * Block and wait for completion.
     **************************************************************************/
    public void waitFor() throws SoaplabException {
        if (localized) return;
        if (con == null) return;
        log.debug("waitFor started");
        synchronized (con) {
            while (resultManager.getState().get() == JobState.RUNNING) {
                try {
                    con.wait();
                } catch (Exception e) {
                }
            }
        }
        log.debug("waitFor ended");
    }

    /**************************************************************************
     * Terminate this running job.
     **************************************************************************/
    public void terminate() throws SoaplabException {
        if (localized || con == null) throw new SoaplabException(AnalysisWS.FAULT_NOT_RUNNING);
        resultManager.getState().set(JobState.TERMINATED_BY_REQUEST);
        con.stop();
    }

    /**************************************************************************
     * Job created - returns number of seconds from the BOE.
     **************************************************************************/
    public long getCreated() throws SoaplabException {
        return created / 1000;
    }

    /**************************************************************************
     * Job started - returns number of seconds from the BOE.
     **************************************************************************/
    public long getStarted() throws SoaplabException {
        return started / 1000;
    }

    /**************************************************************************
     * Job ended - returns number of seconds from the BOE.
     **************************************************************************/
    public long getEnded() throws SoaplabException {
        return ended / 1000;
    }

    /**************************************************************************
     * Job time elapsed - returns number in milliseconds.
     *
     * Note that 'elapsed' is properly filled only after job has been
     * finished.
     **************************************************************************/
    public long getElapsed() throws SoaplabException {
        if (elapsed == -1 && started > -1 && ended > started) elapsed = ended - started;
        return elapsed;
    }

    /**************************************************************************
     * Besides the obvious - returning this job state - it also starts
     * 'localization' if the job is finished.
     **************************************************************************/
    public synchronized String getStatus() throws SoaplabException {
        if (terminated && !localized && !beingLocalized) {
            beingLocalized = true;
            new ResultFetcherThread().start();
        }
        terminated = resultManager.getState().isCompleted();
        return resultManager.getState().getAsString();
    }

    /**************************************************************************
     * Note that field 'lastEvent' is filled only after job has been
     * finished.
     **************************************************************************/
    public String getLastEvent() throws SoaplabException {
        if (eventManager == null) {
            if (lastEvent == null) return new AnalysisEvent("").toXML(); else return lastEvent;
        }
        return eventManager.getLastEvent().toXML();
    }

    /**************************************************************************
     * Return all (available) results.
     **************************************************************************/
    public Map getResults() throws SoaplabException {
        if (localized) return percy.getResults(this);
        try {
            getStatus();
            return resultManager.getResults();
        } catch (AnalysisException e) {
            throw new SoaplabException(e.getMessage());
        }
    }

    /**************************************************************************
     * Return all wanted (and available) results.
     **************************************************************************/
    public Map getResults(String[] resultNames) throws SoaplabException {
        try {
            if (localized) return percy.getResults(this, resultNames);
        } catch (ClassCastException e) {
            throw new SoaplabException(DGUtils.stackTraceToString(e));
        }
        try {
            getStatus();
            return resultManager.getResults(resultNames);
        } catch (AnalysisException e) {
            throw new SoaplabException(e.getMessage());
        }
    }

    /**************************************************************************
     * Destroy resources related to this job.
     **************************************************************************/
    public void destroy() {
        jobRemoved = true;
        try {
            percy.removeJob(id);
            log.msg("Job removed.");
        } catch (Exception e) {
            PersistenceManagerImpl.percyWarning(log, e);
        }
    }

    /**************************************************************************
     * Check input data.
     **************************************************************************/
    protected void checkInputs() throws SoaplabException {
        Map inputDefs = new HashMap();
        InputPropertyDef[] ipd = null;
        try {
            ipd = metadataAccessor.getInputDefs();
            for (int i = 0; i < ipd.length; i++) inputDefs.put(ipd[i].name, ipd[i]);
        } catch (AnalysisException e) {
            throw new SoaplabException(e.getMessage());
        }
        StringBuffer errBuf = new StringBuffer();
        String key;
        for (Iterator it = inputs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            key = (String) entry.getKey();
            InputPropertyDef def = (InputPropertyDef) inputDefs.get(key);
            if (def == null) {
                errBuf.append("Unknown input: " + key + "\n");
                continue;
            }
            if (def.possibleValues != null && def.possibleValues.length > 0) {
                String value = entry.getValue().toString();
                boolean found = false;
                for (int i = 0; i < def.possibleValues.length; i++) {
                    if (def.possibleValues[i].equals(value)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    errBuf.append("Input '" + key + "' cannot have value '" + value + "'." + "\n");
                    continue;
                }
            }
        }
        for (int i = 0; i < ipd.length; i++) {
            if (ipd[i].mandatory && !inputs.containsKey(ipd[i].name)) {
                errBuf.append("Input '" + ipd[i].name + "' is mandatory and cannot remain empty.\n");
                continue;
            }
        }
        if (errBuf.length() > 0) reportInvalidInputs(new String(errBuf));
    }

    protected void reportInvalidInputs(String msg) throws SoaplabException {
        throw new SoaplabException(AnalysisWS.FAULT_NOT_VALID_INPUTS + "\n" + msg);
    }

    /**************************************************************************
     * This is the nucleus of this class (or at least one of them): it
     * converts input data as given by a client (and transported by a
     * web service protocol) to the form understood by a remote web
     * site. The data can be either simple name-value pairs, or they
     * can be used as 'uploaded files'.
     *************************************************************************/
    protected GowlabData convertInputs() throws SoaplabException {
        NVPair[] data = null;
        NVPair[] files = null;
        Vector formData = new Vector();
        Vector fileData = new Vector();
        try {
            ParamDef[] pds = metadataAccessor.getParamDefs();
            for (int i = 0; i < pds.length; i++) {
                ParamDef param = pds[i];
                switch(param.createdFor) {
                    case ParamDef.BASE_PARAMETER:
                        if (inputs.containsKey(param.id) && UUtils.is(inputs.get(param.id).toString())) {
                            String value = param.get(ParamDef.TAG);
                            if (value.equals("")) value = "on";
                            formData.addElement(new NVPair(param.id, value));
                        }
                        break;
                    case ParamDef.RANGE_PARAMETER:
                    case ParamDef.STANDARD_PARAMETER:
                        String tag = param.get(ParamDef.TAG);
                        String paramName = (UUtils.isEmpty(tag) ? param.id : tag);
                        if (inputs.containsKey(param.id)) {
                            formData.addElement(new NVPair(paramName, inputs.get(param.id).toString()));
                        } else if (param.dflt != null) {
                            formData.addElement(new NVPair(paramName, param.dflt));
                        } else if (param.is(ParamDef.SEND_DEFAULTS)) {
                            formData.addElement(new NVPair(paramName, ""));
                        }
                        break;
                    case ParamDef.IO_PARAMETER:
                        IOParamDef iodef = (IOParamDef) param;
                        if (iodef.ioType == IOParamDef.TYPE_INPUT) {
                            if (iodef.ioFormat == IOParamDef.FORMAT_DIRECT) {
                                if (inputs.containsKey(param.id)) fileData.addElement(new NVPair(param.id, processDirectInput(param.id)));
                            } else if (iodef.ioFormat == IOParamDef.FORMAT_URL) {
                                if (inputs.containsKey(param.id)) fileData.addElement(new NVPair(param.id, processURLInput(param.id)));
                            } else {
                                String directName = param.id + "_direct_data";
                                String urlName = param.id + "_url";
                                if (inputs.containsKey(directName)) {
                                    fileData.addElement(new NVPair(param.id, processDirectInput(directName)));
                                } else if (inputs.containsKey(urlName)) {
                                    fileData.addElement(new NVPair(param.id, processURLInput(urlName)));
                                } else {
                                    if (iodef.is(ParamDef.MANDATORY)) {
                                        reportInvalidInputs("Exactly one from the following inputs must be given: " + directName + ", " + urlName + ".");
                                    }
                                }
                            }
                        }
                        break;
                    case ParamDef.CHOICE_PARAMETER:
                    case ParamDef.CHOICE_LIST_PARAMETER:
                        ChoiceParamDef def = (ChoiceParamDef) param;
                        for (int j = 0; j < def.bools.length; j++) {
                            ParamDef bool = def.bools[j];
                            if (inputs.containsKey(bool.id) && UUtils.is(inputs.get(bool.id).toString())) {
                                String value = bool.get(ParamDef.PROMPT);
                                if (UUtils.isEmpty(value)) value = bool.get(ParamDef.TAG);
                                formData.addElement(new NVPair(param.id, value));
                            }
                        }
                        break;
                    default:
                        log.warning("Parameter definition for '" + param.id + "' ignored (unknown parameter type).");
                }
            }
            if (formData.size() > 0) {
                data = new NVPair[formData.size()];
                formData.copyInto(data);
            }
            if (fileData.size() > 0) {
                files = new NVPair[fileData.size()];
                fileData.copyInto(files);
            }
            String queryString = metadataAccessor.getEventDef().actions[0].method;
            if (!UUtils.isEmpty(queryString)) {
                Substitutor engine = new Substitutor(queryString, new PAccessor(data));
                data = new NVPair[] { new NVPair(null, StringUtils.join(engine.process())) };
                files = null;
            }
        } catch (AnalysisException e) {
            throw new SoaplabException(e.getMessage());
        } catch (Exception e) {
            logAndThrow(e.toString());
        }
        return new GowlabData(data, files);
    }

    /**************************************************************************
     * 'inputName' is a name of an input that should be uploaded - the
     * input consists of the data. Store it in a temporary file and
     * return the name of such file.
     *************************************************************************/
    String processDirectInput(String inputName) throws SoaplabException {
        try {
            File tmpFile = File.createTempFile("gowlab.", null);
            tmpFile.deleteOnExit();
            DataOutputStream fileout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
            Object data = inputs.get(inputName);
            if (data instanceof byte[]) fileout.write((byte[]) data, 0, ((byte[]) data).length); else fileout.writeBytes(((String) data).toString());
            fileout.close();
            return tmpFile.getAbsolutePath();
        } catch (IOException e) {
            logAndThrow("In processDirectData: " + e.toString());
        }
        return null;
    }

    /**************************************************************************
     * 'inputName' is a name of an input that should be uploaded - the
     * input consists of a URL pointing to the data. Fetch it and
     * store it in a temporary file and return the name of such file.
     *************************************************************************/
    String processURLInput(String inputURL) throws SoaplabException {
        try {
            File tmpFile = File.createTempFile("gowlab.", null);
            tmpFile.deleteOnExit();
            Object data = inputs.get(inputURL);
            URL url = new URL(data.toString());
            if (url.getProtocol().equals("file")) logAndThrow("Trying to get local file '" + url.toString() + "' is not allowed.");
            URLConnection uc = url.openConnection();
            uc.connect();
            InputStream in = uc.getInputStream();
            byte[] buffer = new byte[256];
            DataOutputStream fileout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fileout.write(buffer, 0, bytesRead);
            }
            fileout.close();
            return tmpFile.getAbsolutePath();
        } catch (IOException e) {
            logAndThrow("In processURLData: " + e.toString());
        }
        return null;
    }

    /**************************************************************************
     * Log the given error message and throw SoaplabException using
     * the same message.
     *************************************************************************/
    protected void logAndThrow(String msg) throws SoaplabException {
        log.error(msg);
        throw new SoaplabException(msg);
    }

    /**************************************************************************
     * This should be called only after successful localization. It
     * removes all objects it does not need anymore in order to save
     * memory.
     *
     * TBD: How to remove old Job object? The AnalysisWS should be
     * involved, as well.
     *************************************************************************/
    protected void makeYourselfThin() {
        if (!localized) return;
        inputs = null;
        metadataAccessor = null;
        con = null;
        resultManager.removeResults();
        if (eventManager != null) {
            lastEvent = eventManager.getLastEvent().toXML();
            eventManager = null;
        }
    }

    /******************************************************************************
     ******************************************************************************
     *
     * GowlabData - a container for all inout data, in a form suitable
     * for feeding an HTTPClient
     *
     ******************************************************************************
     ******************************************************************************/
    protected class GowlabData {

        public NVPair[] formData;

        public NVPair[] fileData;

        public GowlabData(NVPair[] formData, NVPair[] fileData) {
            this.formData = formData;
            this.fileData = fileData;
        }
    }

    /******************************************************************************
     ******************************************************************************
     *
     * Implementation of ParameterAccessor interface
     *
     ******************************************************************************
     ******************************************************************************/
    class PAccessor implements ParameterAccessor {

        Properties data;

        public PAccessor(NVPair[] formData) {
            data = new Properties();
            for (int i = 0; i < formData.length; i++) data.put(formData[i].getName(), formData[i].getValue());
        }

        public String[] getValues(String name) {
            String[] result = new String[] { data.getProperty(name) };
            return result;
        }

        public String getValue(String name, int idx) {
            return data.getProperty(name);
        }

        public String getQualifier(String name) {
            return name;
        }

        public int getCount(String name) {
            return 1;
        }

        public int getCount() {
            return 1;
        }
    }

    /******************************************************************************
     ******************************************************************************
     *
     * ExecuteThread - a separate thread for fetching data from a web site
     *
     ******************************************************************************
     ******************************************************************************/
    class ExecuteThread extends Thread {

        RunPlugIn fetcher;

        HTTPClient.URI supplierURI;

        NVPair[] formData;

        NVPair[] files;

        Properties analysisProps;

        public ExecuteThread(RunPlugIn fetcher, HTTPClient.URI supplierURI, NVPair[] formData, NVPair[] files, Properties analysisProps) {
            this.fetcher = fetcher;
            this.supplierURI = supplierURI;
            this.formData = formData;
            this.files = files;
            this.analysisProps = analysisProps;
            fetcher.setResultManager(resultManager);
            fetcher.setEventManager(eventManager);
            fetcher.setProperties(analysisProps);
        }

        /**************************************************************************
	 * The meat...
	 **************************************************************************/
        public void run() {
            started = System.currentTimeMillis();
            fetcher.fetch(con, supplierURI, formData, files);
            ended = System.currentTimeMillis();
            synchronized (con) {
                con.notifyAll();
            }
        }
    }

    /******************************************************************************
     ******************************************************************************
     *
     * ResultFetcherThread - a separate thread for localizing results
     * into local database
     *
     *  It uses some global variables:
     *     PersistenceManager        percy
     *     AnalysisMetadataAccessor  metadataAccessor
     *     Log                       log
     *     boolean                   beingLocalized, localized
     *     ResultManager             resultManager
     *     
     ******************************************************************************
     ******************************************************************************/
    class ResultFetcherThread extends Thread {

        /**********************************************************************
	 * The main job...
	 **********************************************************************/
        public void run() {
            try {
                OutputPropertyDef[] opd = metadataAccessor.getOutputDefs();
                log.debug("Asking for localization.");
                percy.localize(JobGowlab.this, opd);
                localized = true;
                log.debug("Localization successful.");
                makeYourselfThin();
            } catch (Exception e) {
                PersistenceManagerImpl.percyWarning(log, e);
            } finally {
                beingLocalized = false;
            }
        }
    }

    /******************************************************************************
     ******************************************************************************
     *
     * LazyResultFetcherThread - a separate thread that asks, time to
     * time, for this job status - which may ignite localization (when
     * the job is finished). It dies when the job is localized, or
     * when it tries 'maxTrials' times (which is here hard-coded).
     *
     *  It uses some global variables:
     *     PersistenceManager        percy
     *     Log                       log
     *     boolean                   localized
     *     
     ******************************************************************************
     ******************************************************************************/
    class LazyResultFetcherThread extends Thread {

        long pollingInterval = 1 * 60 * 1000;

        int maxTrials = 15;

        /**********************************************************************
	 * The main job...
	 **********************************************************************/
        public void run() {
            while (!localized && maxTrials-- > 0) {
                if (jobRemoved) return;
                try {
                    Thread.sleep(pollingInterval);
                    log.debug("Lazy fetcher woke up asking for status.");
                    getStatus();
                } catch (SoaplabException e) {
                } catch (InterruptedException e) {
                }
            }
            if (localized) log.debug("Lazy fetcher calls it a day."); else log.debug("Lazy fetcher calls it a day - without being successful.");
        }
    }
}
