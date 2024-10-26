package org.openorb.orb.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.orb.ORBRuntimeException;
import org.apache.orb.util.LifecycleHelper;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.portable.ValueFactory;
import org.openorb.orb.util.Trace;
import org.openorb.util.ExceptionTool;
import org.openorb.util.NamingUtils;
import org.openorb.util.NumberCache;
import org.openorb.util.RepoIDHelper;

/**
 * This class provides all required operations and structures for the
 * org.openorb implementation.
 *
 * @author Chris Wood
 * @author Jerome Daniel
 * @author Michael Rumpf
 * @author Alex Andrushchak
 */
public class ORB extends org.openorb.orb.core.ORBSingleton {

    /**
     * Synchronize for access to the below.
     */
    private java.lang.Object m_sync_state = new byte[0];

    /**
     * This hash table contains all references to the OpenORB features
     */
    private Map m_features = new HashMap();

    /**
     * Each ORB instance has its own initial references list added with
     * Portable Interceptors. Some other initial references are
     * available for all ORB instances.
     */
    private Map m_initial_references = new HashMap();

    /**
     * The OpenORB loader is the entity which is responsible to load all
     * OpenORB parts. This entity also contains all OpenORB properties
     * specified from an XML profile, from the command line or from the
     * application.
     */
    private org.openorb.orb.config.ORBLoader m_loader = null;

    /**
     * List of outstanding defered requests.
     */
    private LinkedList m_deferredReq = new LinkedList();

    /**
     * This target is resolved very frequently for every operation,
     * so it is stored for quick access by the getPICurrent operation.
     */
    private org.openorb.orb.pi.CurrentImpl m_pi_current;

    /**
     * Caching of the property openorb.URLCodeBase.
     */
    private String m_url_codebase = null;

    /**
     * The flag to indicate if the ORB is being shutting down.
     */
    private boolean m_shutting_down = false;

    /**
     * The flag to indicate if the ORB has been shut down.
     */
    private boolean m_hasShutdown = false;

    /**
     * The flag to indicate if the ORB has been destroyed.
     */
    private boolean m_destroyed = false;

    /**
     * Orb instances must be created through ORB.init().
     */
    public ORB() {
    }

    /**
     * This operation is used to set an OpenORB feature reference.
     *
     * @param feature
     * @param reference
     */
    public void setFeature(String feature, java.lang.Object reference) {
        if (getLogger() != null && getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("Setting the feature \"" + feature + "\".");
        }
        synchronized (m_sync_state) {
            if (reference != null) {
                m_features.put(feature, reference);
            } else {
                m_features.remove(feature);
            }
        }
    }

    /**
     * This operation is used to get an OpenORB feature reference.
     *
     * @param feature
     * @return Object
     */
    public java.lang.Object getFeature(String feature) {
        synchronized (m_sync_state) {
            return m_features.get(feature);
        }
    }

    /**
     * This operation returns the configurator used by this ORB instance.
     *
     * @return ORBLoader
     */
    public org.openorb.orb.config.ORBLoader getLoader() {
        return m_loader;
    }

    /**
     * Returns the PICurrent implementation. The PICurrent must be resolved
     * with every invocation both at the client and server end, so it's avalable
     * from this operation for optimization reasons.
     *
     * @return CurrentImpl
     */
    public org.openorb.orb.pi.CurrentImpl getPICurrent() {
        if (m_pi_current == null) {
            throw new org.omg.CORBA.BAD_INV_ORDER("Unable to make invocation, orb is not fully initialized", MinorCodes.BAD_INV_ORDER_ORB, CompletionStatus.COMPLETED_NO);
        }
        return m_pi_current;
    }

    /**
     * Register an initial reference.
     *
     * @param name
     * @param target
     */
    public void addInitialReference(String name, org.omg.CORBA.Object target) {
        if (getLogger() != null && getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("Adding initial reference \"" + name + "\" to reference table.");
        }
        if (name.equals("PICurrent")) {
            m_pi_current = (org.openorb.orb.pi.CurrentImpl) target;
        }
        synchronized (m_sync_state) {
            if (target != null) {
                m_initial_references.put(name, target);
            } else {
                m_initial_references.remove(name);
            }
        }
    }

    /**
     * Add service information.
     *
     * @param service_type
     * @param service_information
     */
    public void addServiceInformation(short service_type, org.omg.CORBA.ServiceInformation service_information) {
        synchronized (m_sync_state) {
            Map service_info = (Map) getFeature("ServiceInfo");
            if (service_info == null) {
                if (service_information == null) {
                    return;
                }
                service_info = new HashMap();
                setFeature("ServiceInfo", service_info);
            }
            if (service_information != null) {
                service_info.put(NumberCache.getShort(service_type), service_information);
            } else {
                service_info.remove(NumberCache.getShort(service_type));
            }
        }
    }

    /**
     * This operation is used to get a loader name from the command line or properties arguments.
     *
     * @param args
     * @param properties
     * @return String
     */
    private String scan_for_loader_name(String[] args, java.util.Properties properties) {
        String name = null;
        if (properties != null) {
            name = properties.getProperty("openorb.ORBLoader");
        }
        if (name == null) {
            if (properties != null) {
                name = properties.getProperty("ORBLoader");
            }
        }
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-ORBLoader")) {
                    name = args[i].substring(11);
                }
            }
        }
        return name;
    }

    /**
     * This operation is used to get a loader name from the command line or properties arguments.
     *
     * @param app
     * @param properties
     * @return String
     */
    private String scan_for_loader_name(java.applet.Applet app, java.util.Properties properties) {
        String name = null;
        if (properties != null) {
            name = (String) properties.get("openorb.ORBLoader");
        }
        if (name == null) {
            if (properties != null) {
                name = (String) properties.get("ORBLoader");
            }
        }
        if (app != null) {
            if (app.getParameter("openorb.ORBLoader") != null) {
                name = app.getParameter("openorb.ORBLoader");
            } else if (app.getParameter("ORBLoader") != null) {
                name = app.getParameter("ORBLoader");
            }
        }
        return name;
    }

    /**
     * Return the value for the property openorb.URLCodeBase.
     */
    public String getURLCodeBase() {
        if (m_url_codebase == null) {
            m_url_codebase = m_loader.getStringProperty("openorb.URLCodeBase", "");
        }
        return m_url_codebase;
    }

    /**
     * Return an initial reference.
     * This method tries to find a reference from the following
     * locations in the following order:
     * <ol>
     *   <li>internal initial reference table</li>
     *   <li>-ORBInitRef parameter</li>
     *   <li>-ORBDefaultInitRef</li>
     * </ol>
     *
     * @param object_name
     * @return Object
     * @throws org.omg.CORBA.ORBPackage.InvalidName
     */
    public org.omg.CORBA.Object resolve_initial_references(String object_name) throws org.omg.CORBA.ORBPackage.InvalidName {
        org.omg.CORBA.Object reference;
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("Try resolve initial reference \"" + object_name + "\" from reference table.");
        }
        synchronized (m_sync_state) {
            reference = (org.omg.CORBA.Object) m_initial_references.get(object_name);
            if (reference != null) {
                if (getLogger().isDebugEnabled() && Trace.isLow()) {
                    getLogger().debug("Resolved initial reference \"" + object_name + "\" from reference table.");
                }
                return reference;
            }
            if (m_pi_current == null) {
                throw new org.omg.CORBA.ORBPackage.InvalidName("Object not found : " + object_name);
            }
        }
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("Try resolve initial reference \"" + object_name + "\" from InitRef property.");
        }
        String string_ref = m_loader.getStringProperty("InitRef." + object_name, "");
        if (string_ref.length() > 0) {
            try {
                if (getLogger().isDebugEnabled() && Trace.isLow()) {
                    getLogger().debug("ORB::resolve_initial_references resolve with InitRef." + object_name + "=" + string_ref);
                }
                reference = string_to_object(string_ref);
                synchronized (m_sync_state) {
                    m_initial_references.put(object_name, reference);
                }
                if (getLogger().isDebugEnabled() && Trace.isLow()) {
                    getLogger().debug("Resolved initial reference \"" + object_name + "\" from InitRef.");
                }
                return reference;
            } catch (org.omg.CORBA.SystemException ex) {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("Call to string_to_object ( \"" + string_ref + "\" ) failed.");
                }
            }
        }
        String default_init_ref = m_loader.getStringProperty("openorb.DefaultInitRef", "");
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("Try resolve initial reference \"" + object_name + "\" with DefaultInitRef " + default_init_ref);
        }
        if (default_init_ref.length() > 0) {
            try {
                if (!(default_init_ref.startsWith("corbaname") && object_name.equals(NamingUtils.NS_NAME_LONG))) {
                    string_ref = default_init_ref + "/" + object_name;
                    if (getLogger().isDebugEnabled() && Trace.isLow()) {
                        getLogger().debug("ORB::resolve_initial_references resolve with DefaultInitRef: " + string_ref);
                    }
                    reference = string_to_object(string_ref);
                    if (reference == null || !reference._non_existent()) {
                        if (getLogger().isDebugEnabled() && Trace.isLow()) {
                            getLogger().debug("Resolved initial reference \"" + object_name + "\" from DefaultInitRef.");
                        }
                        return reference;
                    }
                }
            } catch (org.omg.CORBA.SystemException ex) {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("Call to string_to_object ( \"" + string_ref + "\" ) failed.");
                }
            }
        }
        String mapped = NamingUtils.rirMapping(object_name);
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("Try resolve initial reference \"" + object_name + "\" with fallback  now: " + object_name + " -> " + mapped);
        }
        if (null != mapped) {
            reference = NamingUtils.resolveObjectFromNamingService(this, mapped, object_name.equals(NamingUtils.NS_NAME_LONG));
        } else {
            reference = null;
        }
        if (reference != null && !reference._non_existent()) {
            if (getLogger().isDebugEnabled() && Trace.isLow()) {
                getLogger().debug("Resolved initial reference \"" + object_name + "\" from fallback:\n" + reference);
            }
            return reference;
        }
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("Failed resolving initial references \"" + object_name + "\" from InitRef, DefaultInitRef or fallback.");
        }
        throw new org.omg.CORBA.ORBPackage.InvalidName("Object not found : " + object_name);
    }

    /**
     * Return as a string sequence all available initial services.
     *
     * @return String[]
     */
    public String[] list_initial_services() {
        synchronized (m_sync_state) {
            return (String[]) m_initial_references.keySet().toArray(new String[m_initial_references.size()]);
        }
    }

    /**
     * Run the server side.
     */
    public void run() {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("ORB running.");
        }
        org.openorb.orb.net.ServerManager svrmgr = (org.openorb.orb.net.ServerManager) getFeature("ServerCPCManager");
        if (svrmgr != null) {
            svrmgr.startup(true, true);
        }
    }

    /**
     * Stop the orb ( and the object adapter if required )
     *
     * @param wait_for_completion
     */
    public void shutdown(boolean wait_for_completion) {
        synchronized (m_sync_state) {
            if (m_shutting_down) {
                if (wait_for_completion) {
                    while (!m_hasShutdown) {
                        try {
                            m_sync_state.wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                return;
            }
            m_shutting_down = true;
        }
        org.openorb.orb.net.ServerManager svrmgr = (org.openorb.orb.net.ServerManager) getFeature("ServerCPCManager");
        if (svrmgr != null) {
            svrmgr.shutdown(wait_for_completion);
        }
        org.openorb.orb.net.ClientManager cltmgr = (org.openorb.orb.net.ClientManager) getFeature("ClientCPCManager");
        if (cltmgr != null) {
            cltmgr.shutdown(wait_for_completion, false);
        }
        synchronized (m_sync_state) {
            m_hasShutdown = true;
            m_sync_state.notifyAll();
        }
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("ORB shutdown.");
        }
    }

    /**
     * Destroy the ORB.
     * It shall shutdown the ORB and release resources.
     */
    public void destroy() {
        shutdown(true);
        synchronized (m_sync_state) {
            if (m_destroyed) {
                throw new org.omg.CORBA.OBJECT_NOT_EXIST("ORB has been destroyed.");
            }
            m_destroyed = true;
            m_features = null;
            m_initial_references = null;
            m_loader = null;
            m_deferredReq = null;
            m_pi_current = null;
            m_url_codebase = null;
        }
    }

    /**
     * A finalizer which will call shutdown.
     */
    protected void finalize() {
        synchronized (m_sync_state) {
            if (m_destroyed) {
                return;
            }
        }
        destroy();
    }

    /**
     * Check if some work is pending.
     *
     * @return boolean
     */
    public boolean work_pending() {
        org.openorb.orb.net.ServerManager svrmgr = (org.openorb.orb.net.ServerManager) getFeature("ServerCPCManager");
        if (svrmgr != null) {
            return svrmgr.work_pending();
        }
        return false;
    }

    /**
     * Perform all current work.
     */
    public void perform_work() {
        org.openorb.orb.net.ServerManager svrmgr = (org.openorb.orb.net.ServerManager) getFeature("ServerCPCManager");
        if (svrmgr != null) {
            svrmgr.serve_request(false);
        }
    }

    /**
     * Create an output stream.
     *
     * @return OutputStream
     */
    public org.omg.CORBA.portable.OutputStream create_output_stream() {
        return new org.openorb.orb.io.ListOutputStream(this);
    }

    /**
     * Create a named value list.
     *
     * @param count
     * @return NVList
     */
    public org.omg.CORBA.NVList create_list(int count) {
        org.omg.CORBA.NVList nv = new org.openorb.orb.core.dii.NVList(this);
        for (int i = 0; i < count; i++) {
            nv.add(0);
        }
        return nv;
    }

    /**
     * Create a named value.
     *
     * @param name
     * @param value
     * @param flags
     * @return NamedValue
     */
    public org.omg.CORBA.NamedValue create_named_value(String name, org.omg.CORBA.Any value, int flags) {
        return new org.openorb.orb.core.dii.NamedValue(name, value, flags);
    }

    /**
     * Create an exception list.
     *
     * @return ExceptionList
     */
    public org.omg.CORBA.ExceptionList create_exception_list() {
        return new org.openorb.orb.core.dii.ExceptionList();
    }

    /**
     * Create a context list.
     *
     * @return ContextList
     */
    public org.omg.CORBA.ContextList create_context_list() {
        return new org.openorb.orb.core.dii.ContextList();
    }

    /**
     * Return default context.
     *
     * @return Context
     */
    public org.omg.CORBA.Context get_default_context() {
        return new org.openorb.orb.core.dii.Context("", null, this);
    }

    /**
     * Create an environment.
     *
     * @return Environment
     */
    public org.omg.CORBA.Environment create_environment() {
        return new org.openorb.orb.core.dii.Environment();
    }

    /**
     * Connect an object to the adapter.
     *
     * @param obj
     */
    public void connect(org.omg.CORBA.Object obj) {
        if (obj == null) {
            return;
        }
        org.openorb.orb.adapter.boa.BOA boa = (org.openorb.orb.adapter.boa.BOA) getFeature("BOA");
        if (boa == null) {
            throw new org.omg.CORBA.NO_IMPLEMENT();
        }
        boa.connect((org.omg.CORBA.portable.ObjectImpl) obj, true);
        boa.obj_is_ready((org.omg.CORBA.portable.ObjectImpl) obj);
    }

    /**
     * Disconnect an object from the adapter.
     *
     * @param obj
     */
    public void disconnect(org.omg.CORBA.Object obj) {
        org.openorb.orb.adapter.boa.BOA boa = (org.openorb.orb.adapter.boa.BOA) getFeature("BOA");
        if (boa == null) {
            throw new org.omg.CORBA.NO_IMPLEMENT();
        }
        boa.disconnect((org.omg.CORBA.portable.ObjectImpl) obj);
    }

    /**
     * Convert an object reference to a string.
     *
     * @param obj
     * @return String
     */
    public String object_to_string(org.omg.CORBA.Object obj) {
        byte[] encoded;
        try {
            org.omg.IOP.CodecFactory factory = (org.omg.IOP.CodecFactory) resolve_initial_references("CodecFactory");
            org.omg.IOP.Codec codec = factory.create_codec(new org.omg.IOP.Encoding(org.omg.IOP.ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2));
            org.omg.CORBA.Any any = create_any();
            any.insert_Object(obj);
            encoded = codec.encode_value(any);
            if (encoded != null) {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(encoded.length * 2);
                org.openorb.util.HexPrintStream hps = new org.openorb.util.HexPrintStream(bos);
                hps.write(encoded);
                return "IOR:" + bos.toString();
            }
        } catch (final org.omg.CORBA.ORBPackage.InvalidName ex) {
            getLogger().error("Unable to resolve CodecFactory.", ex);
        } catch (final org.omg.IOP.CodecFactoryPackage.UnknownEncoding ex) {
            getLogger().error("An encoding could not be created.", ex);
        } catch (final org.omg.IOP.CodecPackage.InvalidTypeForEncoding ex) {
            getLogger().error("Invalid encoding type.", ex);
        } catch (final java.io.IOException ex) {
            getLogger().error("Unexpected IOException.", ex);
        }
        return null;
    }

    /**
     * Convert a string to an object reference.
     *
     * @param str
     * @return Object
     */
    public org.omg.CORBA.Object string_to_object(String str) {
        int sch_idx = str.indexOf(':');
        if (sch_idx < 0) {
            throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 7, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
        String scheme = str.substring(0, sch_idx).toLowerCase();
        if (scheme.equals("ior")) {
            int len = str.length();
            if ((len % 2) != 0) {
                throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
            }
            if (len < 28) {
                throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
            }
            byte[] buf = new byte[(len - 4) / 2];
            int j = 0;
            char c;
            for (int i = 4; i < len; i += 2) {
                c = str.charAt(i);
                if (c >= '0' && c <= '9') {
                    buf[j] = (byte) ((c - '0') << 4);
                } else if (c >= 'a' && c <= 'f') {
                    buf[j] = (byte) ((c - 'a' + 0xA) << 4);
                } else if (c >= 'A' && c <= 'F') {
                    buf[j] = (byte) ((c - 'A' + 0xA) << 4);
                } else {
                    throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
                }
                c = str.charAt(i + 1);
                if (c >= '0' && c <= '9') {
                    buf[j] += (byte) (c - '0');
                } else if (c >= 'a' && c <= 'f') {
                    buf[j] += (byte) (c - 'a' + 0xA);
                } else if (c >= 'A' && c <= 'F') {
                    buf[j] += (byte) (c - 'A' + 0xA);
                } else {
                    throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
                }
                ++j;
            }
            try {
                org.omg.IOP.CodecFactory factory = (org.omg.IOP.CodecFactory) resolve_initial_references("CodecFactory");
                org.omg.IOP.Codec codec = factory.create_codec(new org.omg.IOP.Encoding(org.omg.IOP.ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2));
                org.omg.CORBA.Any any = codec.decode_value(buf, get_primitive_tc(org.omg.CORBA.TCKind.tk_objref));
                return any.extract_Object();
            } catch (final org.omg.CORBA.ORBPackage.InvalidName ex) {
                getLogger().error("Unable to resolve CodecFactory.", ex);
            } catch (final org.omg.IOP.CodecFactoryPackage.UnknownEncoding ex) {
                getLogger().error("An encoding could not be created.", ex);
            } catch (final org.omg.IOP.CodecPackage.FormatMismatch ex) {
                getLogger().error("Encoding format does not match.", ex);
            } catch (final org.omg.IOP.CodecPackage.TypeMismatch ex) {
                getLogger().error("Encoding types do not match.", ex);
            }
            throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        } else if (scheme.equals("corbaloc")) {
            return scan_url_loc(str.substring(9));
        } else if (scheme.equals("corbaname")) {
            String loc = str.substring(10);
            return scan_url_name(loc);
        } else if (scheme.equals("file") || scheme.equals("ftp") || scheme.equals("http")) {
            try {
                java.net.URL url = new java.net.URL(str);
                java.io.InputStream is = url.openStream();
                java.io.InputStreamReader rd = new java.io.InputStreamReader(is);
                java.io.BufferedReader inpt = new java.io.BufferedReader(rd);
                String string_ref = inpt.readLine();
                inpt.close();
                return string_to_object(string_ref);
            } catch (final java.net.MalformedURLException ex) {
                final String msg = "Invalid URL: " + str + ".";
                getLogger().error(msg, ex);
                throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(msg, org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
            } catch (final java.io.IOException ex) {
                final String msg = "Unexpected IOException.";
                getLogger().error(msg, ex);
                throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(msg, org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
            }
        } else {
            throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 7, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
    }

    /**
     * Send multiple oneway requests.
     *
     * @param req
     */
    public void send_multiple_requests_oneway(org.omg.CORBA.Request[] req) {
        for (int i = 0; i < req.length; i++) {
            req[i].send_oneway();
        }
    }

    /**
     * Send multiple deferred requests.
     *
     * @param req
     */
    public void send_multiple_requests_deferred(org.omg.CORBA.Request[] req) {
        for (int i = 0; i < req.length; i++) {
            req[i].send_deferred();
        }
        synchronized (m_deferredReq) {
            for (int i = 0; i < req.length; i++) {
                m_deferredReq.add(req[i]);
            }
        }
    }

    /**
     * Poll next response
     *
     * @return boolean
     */
    public boolean poll_next_response() {
        synchronized (m_deferredReq) {
            if (m_deferredReq.isEmpty()) {
                throw new org.omg.CORBA.BAD_INV_ORDER(org.omg.CORBA.OMGVMCID.value | 11, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
            }
            Iterator itt = m_deferredReq.iterator();
            while (itt.hasNext()) {
                org.omg.CORBA.Request req = (org.omg.CORBA.Request) itt.next();
                if (req.poll_response()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the next response. If poll_next_response would return true then
     * the first completed request will be returned, otherwise the oldest and
     * first request will be waited for and returned.
     *
     * @return Request
     */
    public org.omg.CORBA.Request get_next_response() {
        org.openorb.orb.core.dii.Request req = null;
        synchronized (m_deferredReq) {
            if (m_deferredReq.isEmpty()) {
                throw new org.omg.CORBA.BAD_INV_ORDER(org.omg.CORBA.OMGVMCID.value | 11, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
            }
            Iterator itt = m_deferredReq.iterator();
            boolean found = false;
            while (itt.hasNext()) {
                req = (org.openorb.orb.core.dii.Request) itt.next();
                if (req.poll_response()) {
                    itt.remove();
                    found = true;
                    break;
                }
            }
            if (!found) {
                req = (org.openorb.orb.core.dii.Request) m_deferredReq.removeFirst();
            }
        }
        req.get_response();
        return req;
    }

    /**
     * Set the ORB parameters.
     *
     * @param args
     * @param properties
     */
    protected void set_parameters(String[] args, java.util.Properties properties) {
        String loader_name = scan_for_loader_name(args, properties);
        if (loader_name == null) {
            loader_name = "org.openorb.orb.config.OpenORBLoader";
        }
        try {
            m_loader = (org.openorb.orb.config.ORBLoader) Thread.currentThread().getContextClassLoader().loadClass(loader_name).newInstance();
        } catch (final java.lang.Throwable ex) {
            throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE("Unable to initialize orb loader (" + ex + ")"), ex);
        }
        m_loader.init(args, properties, this);
        if (getLogger() == null) {
            enableLogging(org.openorb.orb.util.Trace.getLogger().getChildLogger("" + System.identityHashCode(this)));
        }
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug("ORB created");
        }
    }

    /**
     * Set the ORB parameters for applet.
     *
     * @param app
     * @param properties
     */
    protected void set_parameters(java.applet.Applet app, java.util.Properties properties) {
        String loader_name = scan_for_loader_name(app, properties);
        if (loader_name == null) {
            loader_name = "org.openorb.orb.config.OpenORBLoader";
        }
        try {
            m_loader = (org.openorb.orb.config.ORBLoader) Thread.currentThread().getContextClassLoader().loadClass(loader_name).newInstance();
        } catch (final java.lang.Throwable ex) {
            throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE("Unable to initialize orb loader (" + ex + ")"), ex);
        }
        m_loader.init(null, properties, this);
        if (getLogger() == null) {
            enableLogging(org.openorb.orb.util.Trace.getLogger().getChildLogger("" + System.identityHashCode(this)));
        }
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug("ORB created.");
        }
    }

    /**
     * This function is used to register a value factory with a supplied
     * configuration. This method is a non-standard extension supporting value factories
     * that implement Avalon lifecycle methods.
     *
     * @param id The IDL valuetype identifier.
     * @param factory The value factory instance.
     * @param config The factory configuration.
     * @return The registered value factory.
     */
    public ValueFactory register_value_factory(String id, ValueFactory factory, Configuration config) {
        final Context context = (Context) getFeature("CONTEXT");
        final String category = config.getAttribute("category", "factory");
        final DefaultServiceManager manager = new DefaultServiceManager();
        manager.put("orb", this);
        manager.makeReadOnly();
        try {
            LifecycleHelper.pipeline(factory, getLogger().getChildLogger(category), context, config, manager);
        } catch (final Throwable e) {
            final String error = "Pipeline exception while registering value factory.";
            throw new ORBRuntimeException(error, e);
        }
        return register_value_factory(id, factory);
    }

    /**
     * This function is used to register a value factory.
     *
     * @param id the IDL valuetype identifier
     * @param factory the value factory instance
     * @return ValueFactory
     */
    public org.omg.CORBA.portable.ValueFactory register_value_factory(String id, org.omg.CORBA.portable.ValueFactory factory) {
        synchronized (m_sync_state) {
            Map factories = (Map) getFeature("ValueFactory");
            if (factories == null) {
                factories = new HashMap();
                setFeature("ValueFactory", factories);
            }
            factories.put(id, factory);
        }
        return factory;
    }

    /**
     * This function is used to unregister a value factory.
     *
     * @param id
     */
    public void unregister_value_factory(String id) {
        synchronized (m_sync_state) {
            Map factories = (Map) getFeature("ValueFactory");
            if (factories != null) {
                factories.remove(id);
            }
        }
    }

    /**
     * This function is used to find a value factory.
     *
     * @param id The type id of the value type.
     * @return ValueFactory The value factory if one was found, or null otherwise.
     */
    public org.omg.CORBA.portable.ValueFactory lookup_value_factory(String id) {
        org.omg.CORBA.portable.ValueFactory ret = null;
        if (!id.startsWith("RMI:")) {
            synchronized (m_sync_state) {
                Map factories = (Map) getFeature("ValueFactory");
                if (factories != null) {
                    ret = (org.omg.CORBA.portable.ValueFactory) factories.get(id);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
            String classname;
            try {
                classname = RepoIDHelper.idToClass(id, RepoIDHelper.TYPE_DEFAULT_FACTORY);
            } catch (final IllegalArgumentException ex) {
                return null;
            }
            Class clz = null;
            if (classname != null) {
                try {
                    clz = Thread.currentThread().getContextClassLoader().loadClass(classname);
                } catch (final Throwable ex) {
                    return null;
                }
            }
            try {
                ret = (org.omg.CORBA.portable.ValueFactory) clz.newInstance();
            } catch (final Exception ex) {
                final String error = "Default value factory for '" + classname + "' raised a instantiation error.";
                throw new ORBRuntimeException(error, ex);
            }
        }
        return ret;
    }

    /**
     * This function is used to set a delegate.
     *
     * @param wrapper
     */
    public void set_delegate(java.lang.Object wrapper) {
        if (wrapper instanceof org.omg.PortableServer.Servant) {
            org.omg.PortableServer.portable.Delegate delegate = (org.omg.PortableServer.portable.Delegate) getFeature("POADelegate");
            if (delegate == null) {
                throw new org.omg.CORBA.INITIALIZE("Missing POA delegate");
            }
            ((org.omg.PortableServer.Servant) wrapper)._set_delegate(delegate);
        } else {
            throw new org.omg.CORBA.BAD_PARAM(0, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
    }

    /**
     * Return the service informaton.
     *
     * @param service_type
     * @param service_information
     * @return boolean
     */
    public boolean get_service_information(short service_type, org.omg.CORBA.ServiceInformationHolder service_information) {
        synchronized (m_sync_state) {
            Map service_info = (Map) getFeature("ServiceInfo");
            if (service_info == null) {
                return false;
            }
            service_information.value = (org.omg.CORBA.ServiceInformation) service_info.get(NumberCache.getShort(service_type));
            return (service_information.value != null);
        }
    }

    /**
     * This operations creates a CORBA policy.
     *
     * @param policy_type
     * @param val
     * @return Policy
     * @throws org.omg.CORBA.PolicyError
     */
    public org.omg.CORBA.Policy create_policy(int policy_type, org.omg.CORBA.Any val) throws org.omg.CORBA.PolicyError {
        org.omg.PortableInterceptor.PolicyFactory policy_factory = (org.omg.PortableInterceptor.PolicyFactory) getFeature("PolicyFactory");
        if (policy_factory != null) {
            return policy_factory.create_policy(policy_type, val);
        }
        throw new org.omg.CORBA.PolicyError(org.omg.CORBA.BAD_POLICY_VALUE.value);
    }

    /**
     * This function scans a CORBA URL iioploc to extract reference information.
     *
     * @param loc
     * @return Object
     */
    protected org.omg.CORBA.Object scan_url_loc(String loc) {
        if (loc.startsWith("rir:/")) {
            try {
                return resolve_initial_references(loc.substring(5));
            } catch (final org.omg.CORBA.ORBPackage.InvalidName ex) {
                final String msg = "Unable to resolve " + loc.substring(5) + ".";
                getLogger().error(msg, ex);
                throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(msg, org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
            }
        }
        int keyidx = loc.indexOf('/');
        if (keyidx < 0) {
            throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
        return scan_url_body(loc.substring(0, keyidx), getKeyFromString(loc.substring(keyidx + 1)));
    }

    /**
     * Method scan_url_body.
     *
     * @param addr
     * @param key
     * @return Object
     */
    protected org.omg.CORBA.Object scan_url_body(String addr, final byte[] key) {
        final List list = new ArrayList();
        int index;
        int old = 0;
        String s = null;
        while (true) {
            index = addr.indexOf(",", old);
            if (index < 0) {
                s = addr.substring(old);
                if (s.startsWith("iiop:")) {
                    list.add(s.substring(5));
                } else if (s.startsWith(":")) {
                    list.add(s.substring(1));
                } else {
                    throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 8, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
                }
                break;
            } else {
                s = addr.substring(old, index);
                if (s.startsWith("iiop:")) {
                    list.add(s.substring(5));
                } else if (s.startsWith(":")) {
                    list.add(s.substring(1));
                } else {
                    throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 8, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
                }
                old = index + 1;
            }
        }
        if (list.isEmpty()) {
            throw Trace.signalIllegalCondition(getLogger(), "List is empty");
        }
        org.omg.IOP.Codec codec = null;
        try {
            org.omg.IOP.CodecFactory factory = (org.omg.IOP.CodecFactory) resolve_initial_references("CodecFactory");
            codec = factory.create_codec(new org.omg.IOP.Encoding(org.omg.IOP.ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2));
        } catch (org.omg.CORBA.ORBPackage.InvalidName ex) {
            getLogger().error("Invalid package name", ex);
        } catch (org.omg.IOP.CodecFactoryPackage.UnknownEncoding ex) {
            getLogger().error("Non existent encoding", ex);
        }
        if (codec == null) {
            throw Trace.signalIllegalCondition(getLogger(), "No CODEC found");
        }
        org.omg.CORBA.Any any = create_any();
        String version;
        int port;
        org.omg.IIOP.Version iiop_version = new org.omg.IIOP.Version();
        org.omg.IIOP.ProfileBody_1_1 body_2 = null;
        byte[][] bodies = new byte[list.size()][];
        int lastbody = 0;
        int alternates = 0;
        for (int i = 0; i < list.size(); i++) {
            addr = (String) list.get(i);
            index = addr.indexOf("@");
            if (index != -1) {
                version = addr.substring(0, index);
                addr = addr.substring(index + 1);
                index = version.indexOf(".");
                if (index == -1) {
                    throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
                }
                try {
                    iiop_version.major = Byte.parseByte(version.substring(0, index));
                    iiop_version.minor = Byte.parseByte(version.substring(index + 1));
                } catch (final NumberFormatException ex) {
                    final String msg = "Parsing of IIOP minor or major version number failed.";
                    getLogger().error(msg, ex);
                    throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(msg, org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
                }
            } else {
                iiop_version.major = (byte) 1;
                iiop_version.minor = (byte) 0;
            }
            index = addr.indexOf(":");
            if (index != -1) {
                try {
                    port = Integer.parseInt(addr.substring(index + 1));
                    if (port > 0xFFFF) {
                        throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
                    }
                } catch (final NumberFormatException ex) {
                    final String msg = "Parsing of the port number failed: " + addr.substring(index + 1) + ".";
                    getLogger().error(msg, ex);
                    throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(msg, org.omg.CORBA.OMGVMCID.value | 9, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
                }
                addr = addr.substring(0, index);
            } else {
                port = 683;
            }
            if (getLogger().isDebugEnabled() && Trace.isHigh()) {
                getLogger().debug("Scanning URL location, searching object " + addr + ":" + port + "/" + new String(key));
            }
            switch(iiop_version.minor) {
                case 0:
                    org.omg.IIOP.ProfileBody_1_0 body_0 = new org.omg.IIOP.ProfileBody_1_0();
                    body_0.host = addr;
                    body_0.port = (short) port;
                    body_0.iiop_version = iiop_version;
                    body_0.object_key = key;
                    org.omg.IIOP.ProfileBody_1_0Helper.insert(any, body_0);
                    try {
                        bodies[lastbody++] = codec.encode_value(any);
                    } catch (final org.omg.IOP.CodecPackage.InvalidTypeForEncoding ex) {
                        getLogger().error("Invalid encoding type.", ex);
                    }
                    break;
                case 1:
                    org.omg.IIOP.ProfileBody_1_1 body_1 = new org.omg.IIOP.ProfileBody_1_1();
                    body_1.host = addr;
                    body_1.port = (short) port;
                    body_1.iiop_version = iiop_version;
                    body_1.object_key = key;
                    body_1.components = new org.omg.IOP.TaggedComponent[0];
                    org.omg.IIOP.ProfileBody_1_1Helper.insert(any, body_1);
                    try {
                        bodies[lastbody++] = codec.encode_value(any);
                    } catch (final org.omg.IOP.CodecPackage.InvalidTypeForEncoding ex) {
                        getLogger().error("Invalid encoding type.", ex);
                    }
                    break;
                case 2:
                    if (body_2 == null) {
                        body_2 = new org.omg.IIOP.ProfileBody_1_1();
                        body_2.host = addr;
                        body_2.port = (short) port;
                        body_2.iiop_version = iiop_version;
                        body_2.object_key = key;
                    } else {
                        org.omg.IIOP.ListenPoint lp = new org.omg.IIOP.ListenPoint();
                        lp.host = addr;
                        lp.port = (short) port;
                        org.omg.IIOP.ListenPointHelper.insert(any, lp);
                        try {
                            bodies[bodies.length - (alternates++) - 1] = codec.encode_value(any);
                        } catch (final org.omg.IOP.CodecPackage.InvalidTypeForEncoding ex) {
                            getLogger().error("Invalid encoding type.", ex);
                        }
                    }
                    break;
                default:
                    Trace.signalIllegalCondition(getLogger(), "IIOP minor version '" + iiop_version.minor + "' not supported!");
                    break;
            }
        }
        if (body_2 != null) {
            body_2.components = new org.omg.IOP.TaggedComponent[alternates];
            for (int i = 0; i < alternates; ++i) {
                body_2.components[i] = new org.omg.IOP.TaggedComponent(org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS.value, bodies[bodies.length - alternates + i]);
            }
            org.omg.IIOP.ProfileBody_1_1Helper.insert(any, body_2);
            try {
                bodies[lastbody++] = codec.encode_value(any);
            } catch (final org.omg.IOP.CodecPackage.InvalidTypeForEncoding ex) {
                getLogger().error("Invalid encoding type.", ex);
            }
        }
        org.omg.IOP.IOR ior = new org.omg.IOP.IOR("", new org.omg.IOP.TaggedProfile[lastbody]);
        for (int i = 0; i < lastbody; ++i) {
            ior.profiles[i] = new org.omg.IOP.TaggedProfile(org.omg.IOP.TAG_INTERNET_IOP.value, bodies[i]);
        }
        return new ObjectStub(this, ior);
    }

    /**
     * This function scans a CORBA URL corbaname to extract reference information.
     * This method is only called when a corbaname URL is passed to string_to_object.
     * Therefore the runtime dependency to the NamingService surfaces only in this case.
     *
     * @param loc The corbaname URL without the prefix "corbaname:".
     * @return The object retrieved from the location specified by loc.
     */
    protected org.omg.CORBA.Object scan_url_name(String loc) {
        String name = null;
        int hidx = loc.indexOf("#");
        if (hidx < 0) {
            hidx = loc.length();
        } else {
            name = loc.substring(hidx + 1);
        }
        int sidx = loc.indexOf("/");
        String url = loc.substring(0, hidx);
        if (sidx < 0 || sidx > hidx) {
            url += "/" + NamingUtils.NS_NAME_LONG;
        }
        org.omg.CORBA.Object namingobj = scan_url_loc(url);
        if (name.equals("")) {
            return namingobj;
        }
        String methodName = null;
        Class ncext_clz = null;
        Class ncextstub_clz = null;
        try {
            ncext_clz = Thread.currentThread().getContextClassLoader().loadClass("org.omg.CosNaming.NamingContextExt");
            ncextstub_clz = Thread.currentThread().getContextClassLoader().loadClass("org.omg.CosNaming._NamingContextExtStub");
            if (namingobj.getClass().isAssignableFrom(ncext_clz)) {
                methodName = new String("resolve_str");
            } else {
                if (namingobj._is_a("IDL:omg.org/CosNaming/NamingContextExt:1.0")) {
                    methodName = new String("resolve_str");
                }
            }
        } catch (java.lang.ClassNotFoundException ex) {
        }
        Class nc_clz = null;
        Class ncstub_clz = null;
        try {
            nc_clz = Thread.currentThread().getContextClassLoader().loadClass("org.omg.CosNaming.NamingContext");
            ncstub_clz = Thread.currentThread().getContextClassLoader().loadClass("org.omg.CosNaming._NamingContextStub");
            if (methodName == null) {
                if (namingobj.getClass().isAssignableFrom(nc_clz)) {
                    methodName = new String("resolve");
                } else {
                    if (namingobj._is_a("IDL:omg.org/CosNaming/NamingContext:1.0")) {
                        methodName = new String("resolve");
                    }
                }
            }
            if (methodName == null) {
                throw new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
            }
        } catch (final java.lang.ClassNotFoundException ex) {
            final String msg = "CosNaming related classes could not be found!";
            getLogger().error(msg, ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(msg, org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
        }
        try {
            java.lang.Object ncstub_obj = null;
            java.lang.Class[] paramTypes = null;
            java.lang.Object[] paramObjects = null;
            if (methodName.equals("resolve")) {
                java.lang.reflect.Constructor ncstub_ctor = ncstub_clz.getConstructor(new java.lang.Class[] { org.omg.CORBA.portable.Delegate.class });
                ncstub_obj = ncstub_ctor.newInstance(new java.lang.Object[] { ((org.omg.CORBA.portable.ObjectImpl) namingobj)._get_delegate() });
                java.util.StringTokenizer st = new java.util.StringTokenizer(name, "/");
                Class ncomp_clz = Thread.currentThread().getContextClassLoader().loadClass("org.omg.CosNaming.NameComponent");
                java.lang.reflect.Field ncomp_id_fld = ncomp_clz.getDeclaredField("id");
                java.lang.reflect.Field ncomp_kind_fld = ncomp_clz.getDeclaredField("kind");
                java.lang.Object ncomp_arr = java.lang.reflect.Array.newInstance(ncomp_clz, st.countTokens());
                paramTypes = new Class[] { ncomp_arr.getClass() };
                int i = 0;
                while (st.hasMoreTokens()) {
                    java.lang.Object ncomp_obj = ncomp_clz.newInstance();
                    String id = "";
                    String kind = "";
                    String token = st.nextToken();
                    int idx = token.indexOf('.');
                    if (idx > 0) {
                        id = token.substring(0, idx);
                        kind = token.substring(idx + 1);
                    } else {
                        id = token;
                    }
                    ncomp_id_fld.set(ncomp_obj, id);
                    ncomp_kind_fld.set(ncomp_obj, kind);
                    java.lang.reflect.Array.set(ncomp_arr, i++, ncomp_obj);
                }
                paramObjects = new java.lang.Object[] { ncomp_arr };
            } else {
                ncstub_obj = ncextstub_clz.newInstance();
                java.lang.reflect.Method setDelegate = ncextstub_clz.getMethod("_set_delegate", new java.lang.Class[] { org.omg.CORBA.portable.Delegate.class });
                setDelegate.invoke(ncstub_obj, new java.lang.Object[] { ((org.omg.CORBA.portable.ObjectImpl) namingobj)._get_delegate() });
                paramTypes = new java.lang.Class[] { java.lang.String.class };
                paramObjects = new java.lang.Object[] { name };
            }
            java.lang.reflect.Method resolve = ncstub_obj.getClass().getDeclaredMethod(methodName, paramTypes);
            org.omg.CORBA.Object obj = null;
            try {
                obj = (org.omg.CORBA.Object) resolve.invoke(ncstub_obj, paramObjects);
            } catch (final InvocationTargetException ex) {
                InvocationTargetException invocex = ex;
                Class notfoundex_clz = Thread.currentThread().getContextClassLoader().loadClass("org.omg.CosNaming.NamingContextPackage.NotFound");
                String msg = "";
                if (!invocex.getTargetException().getClass().isAssignableFrom(notfoundex_clz)) {
                    msg = "Call to resolve failed with the following exception: ";
                    getLogger().error(msg, invocex);
                }
                throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(msg, org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
            } catch (final Exception ex) {
                final String msg = "Calling invoke on the resolve method failed with " + "the following exception: ";
                getLogger().error(msg, ex);
                throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(msg, org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
            }
            return obj;
        } catch (final java.lang.InstantiationException ex) {
            getLogger().error("", ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
        } catch (final java.lang.IllegalAccessException ex) {
            getLogger().error("", ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
        } catch (final java.lang.ClassNotFoundException ex) {
            getLogger().error("", ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
        } catch (final java.lang.reflect.InvocationTargetException ex) {
            getLogger().error("", ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
        } catch (final java.lang.NoSuchMethodException ex) {
            getLogger().error("", ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
        } catch (final java.lang.NoSuchFieldException ex) {
            getLogger().error("", ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM(org.omg.CORBA.OMGVMCID.value | 10, org.omg.CORBA.CompletionStatus.COMPLETED_NO), ex);
        }
    }

    /**
     * Get an object key from a stringified object key.
     *
     * @param str
     * @return byte[]
     */
    protected byte[] getKeyFromString(final String str) {
        String val = "";
        List list = new ArrayList();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '%') {
                val = "" + str.charAt(i + 1) + str.charAt(i + 2);
                list.add(NumberCache.getByte((byte) Short.parseShort(val, 16)));
                i += 2;
            } else {
                list.add(NumberCache.getByte((byte) str.charAt(i)));
            }
        }
        byte[] key = new byte[list.size()];
        for (int i = 0; i < key.length; i++) {
            key[i] = ((Byte) list.get(i)).byteValue();
        }
        return key;
    }
}
