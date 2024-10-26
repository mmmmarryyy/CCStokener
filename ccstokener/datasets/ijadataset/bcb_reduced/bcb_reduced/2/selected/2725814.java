package org.iocframework.hrdt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import org.iocframework.Factory;
import org.iocframework.Factory.Worker;
import org.iocframework.conf.ExternalConfigException;
import org.iocframework.hrdt.Staffing.Context;
import org.iocframework.log.DefaultLoggerFacade;
import org.iocframework.log.LoggerFacade;
import com.taliasplayground.convert.PackagedConverters;
import com.taliasplayground.hrdt.objects.BranchValue;
import com.taliasplayground.hrdt.objects.DTObject;
import com.taliasplayground.hrdt.objects.ExpandedName;
import com.taliasplayground.hrdt.parsers.ObjectParser;
import com.taliasplayground.lang.Assert;
import com.taliasplayground.lang.Pair;

/**
 * <p>
 * </p>
 * 
 * @author David M. Sledge
 */
public class FactoryStaffer {

    public static final String WORKERS_PROPERTY = "workers";

    public static final String RECRUITER_MAP_LOCATION = "META-INF/ioc.recruiters.hrdt";

    public static final ExpandedName FACTORY_CLASS = new ExpandedName(Utils.IOC_NS, "Factory");

    public static final Map<? extends ExpandedName, ?> coreObjRecruiters;

    public static final Map<? extends ExpandedName, ?> corePropRecruiters;

    public static final Map<? extends ExpandedName, ?> initObjRecruiters;

    public static final Map<? extends ExpandedName, ?> initPropRecruiters;

    static {
        Map<ExpandedName, Object> propRecruiters = new HashMap<ExpandedName, Object>();
        Map<ExpandedName, Object> objRecruiters = new HashMap<ExpandedName, Object>();
        Pair<Map<? extends ExpandedName, ?>, Map<? extends ExpandedName, ?>> recruiters;
        try {
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Invoker"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireInvoker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "BatchInvoker"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireBatchInvoker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Singleton"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "worksOnlyOnce"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Ref"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireReferrer"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Local"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireLocalObjectProvider"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Linker"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireLinker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Chain"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireWorkerChain"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Catcher"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireCatcher"));
            objRecruiters.put(null, Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireSimpleMapMaker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "FactoryRef"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireFactoryReferrer"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Converter"), Staffing.getObjectRecruiterMethod(CoreRecruiters.class, "hireConverterReferrer"));
            coreObjRecruiters = Collections.unmodifiableMap(objRecruiters);
            objRecruiters = new HashMap<ExpandedName, Object>();
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Map"), Staffing.getObjectRecruiterMethod(SupportRecruiters.class, "hireMapMaker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "List"), Staffing.getObjectRecruiterMethod(SupportRecruiters.class, "hireCollectionMaker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Set"), Staffing.getObjectRecruiterMethod(SupportRecruiters.class, "hireCollectionMaker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Array"), Staffing.getObjectRecruiterMethod(SupportRecruiters.class, "hireArrayMaker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Field"), Staffing.getObjectRecruiterMethod(SupportRecruiters.class, "hireFieldWorker"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "PropsSet"), Staffing.getObjectRecruiterMethod(SupportRecruiters.class, "hireBatchSetter"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Setter"), Staffing.getObjectRecruiterMethod(SupportRecruiters.class, "hireSetter"));
            objRecruiters.put(new ExpandedName(Utils.IOC_NS, "Getter"), Staffing.getObjectRecruiterMethod(SupportRecruiters.class, "hireGetter"));
            propRecruiters.put(new ExpandedName(Utils.IOC_NS, "invokers"), new Pair<PathStack, BranchValue>(new PathStack("core"), (BranchValue) ObjectParser.parse("{<org" + ".iocframework$BatchInvoker>instance:" + "{<org.iocframework.hrdt$Worker>},key:" + "'obj',methods:{<org.iocframework.hrdt$" + "Value>},}")));
            propRecruiters.put(new ExpandedName(Utils.IOC_NS, "props"), new Pair<PathStack, BranchValue>(new PathStack("core"), (BranchValue) ObjectParser.parse("{<org" + ".iocframework$PropsSet>instance:{<org" + ".iocframework.hrdt$Worker>},key:'obj'," + "props:{<org.iocframework.hrdt$Value>},}")));
            propRecruiters.put(new ExpandedName(Utils.IOC_NS, "catcher"), new Pair<PathStack, BranchValue>(new PathStack("core"), (BranchValue) ObjectParser.parse("{<org" + ".iocframework$Catcher>worker:{<org" + ".iocframework.hrdt$Worker>},key:'cause'," + "handlers:{<org.iocframework.hrdt$Value>},}")));
            propRecruiters.put(new ExpandedName(Utils.IOC_NS, "dependsOn"), new Pair<PathStack, BranchValue>(new PathStack("core"), (BranchValue) ObjectParser.parse("{<org.iocframework$Chain>worker:" + "{<org.iocframework.hrdt$Worker>},dependsOn:" + "{<org.iocframework.hrdt$Value>},}")));
            propRecruiters.put(new ExpandedName(Utils.IOC_NS, "singleton"), new Pair<PathStack, BranchValue>(new PathStack("core"), (BranchValue) ObjectParser.parse("{<org.iocframework$Singleton>worker:" + "{<org.iocframework.hrdt$Worker>},}")));
            propRecruiters.put(new ExpandedName(Utils.IOC_NS, "processors"), new Pair<PathStack, BranchValue>(new PathStack("core"), (BranchValue) ObjectParser.parse("{<org.iocframework$Chain>worker:" + "{<org.iocframework.hrdt$Worker>},key:'obj',processors:" + "{<org.iocframework.hrdt$Value>},}")));
            corePropRecruiters = Collections.unmodifiableMap(propRecruiters);
            propRecruiters = new HashMap<ExpandedName, Object>();
            recruiters = Staffing.loadRecruitersInClasspath(RECRUITER_MAP_LOCATION);
        } catch (ExternalConfigException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        objRecruiters.putAll(recruiters.get1());
        propRecruiters.putAll(recruiters.get2());
        initObjRecruiters = Collections.unmodifiableMap(objRecruiters);
        initPropRecruiters = Collections.unmodifiableMap(propRecruiters);
    }

    /**
     * <p>
     * </p>
     * 
     * @author David M. Sledge
     */
    public enum ContextKey {

        TYPE_CONVERTER, FACTORY, INIT_STACK
    }

    /**
     * @param dtObj
     * @param parent
     * @param rsrcStr
     * @return
     * @throws ExternalConfigException
     * @throws NoSuchMethodException
     * @throws ParseException
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     */
    public static Factory staffFactory(DTObject dtObj, Factory parent, String rsrcStr) throws ExternalConfigException, NoSuchMethodException, ParseException, ClassNotFoundException {
        Assert.notNullArg(dtObj, "dtFactory may not be null.");
        LoggerFacade log = new DefaultLoggerFacade();
        Map<Enum<?>, Object> conditions = new HashMap<Enum<?>, Object>();
        dtObj = dtObj.copyObject(true);
        Map<BranchValue, String> pathMap = new HashMap<BranchValue, String>(PathMapper.mapPaths(dtObj, rsrcStr));
        conditions.put(Staffing.ContextKey.PATH_MAP, pathMap);
        conditions.put(ContextKey.TYPE_CONVERTER, PackagedConverters.buildGenericConverter());
        Map<ExpandedName, Object> objRecruiterMap = new HashMap<ExpandedName, Object>(initObjRecruiters);
        Map<ExpandedName, Object> propRecruiterMap = new HashMap<ExpandedName, Object>(initPropRecruiters);
        objRecruiterMap.putAll(coreObjRecruiters);
        propRecruiterMap.putAll(corePropRecruiters);
        conditions.put(Staffing.ContextKey.OBJ_RECRUITER_MAP, objRecruiterMap);
        conditions.put(Staffing.ContextKey.PROP_RECRUITER_MAP, propRecruiterMap);
        Factory factory = new Factory(parent);
        conditions.put(ContextKey.FACTORY, factory);
        Stack<Worker> stack = new Stack<Worker>();
        conditions.put(ContextKey.INIT_STACK, stack);
        ExpandedName eName = dtObj.getClassName();
        DTObject jobDescrs;
        if (FACTORY_CLASS.equals(eName)) {
            log.debug(FactoryStaffer.class, null, "global value is a {0} DTObject", FACTORY_CLASS);
            jobDescrs = new DTObject();
            Object value = dtObj.get(WORKERS_PROPERTY);
            if (value != null) {
                log.debug(FactoryStaffer.class, null, "Checking value type of " + WORKERS_PROPERTY + " property");
                if (!(value instanceof DTObject)) {
                    throw new ExternalConfigException(pathMap.get(dtObj) + ":  The " + WORKERS_PROPERTY + " property must be" + " an object if specified.");
                }
                jobDescrs.setAll((DTObject) value);
            }
        } else {
            jobDescrs = dtObj;
        }
        if (jobDescrs.getSize() > 0) {
            Context context = new Context();
            for (Entry<? extends ExpandedName, ?> entry : jobDescrs.getPropertiesSet()) {
                ExpandedName expName = entry.getKey();
                if (expName.getNs() == null) {
                    factory.setManager(expName.getLocalName(), context.hireWorker(entry.getValue(), conditions));
                }
            }
        }
        pathMap.clear();
        return factory;
    }

    /**
     * @param url
     * @param parent
     * @return
     * @throws ExternalConfigException
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws ParseException
     * @throws ClassNotFoundException
     */
    public static Factory staffFactory(URL url, Factory parent) throws ExternalConfigException, IOException, NoSuchMethodException, ParseException, ClassNotFoundException {
        Object blueprint;
        InputStream is = url.openStream();
        try {
            blueprint = ObjectParser.parse(is);
            if (!(blueprint instanceof DTObject)) {
                throw new ExternalConfigException("Root value of the HRDT" + " configuration file must be an object");
            }
        } finally {
            is.close();
        }
        return staffFactory((DTObject) blueprint, parent, url.toString());
    }

    /**
     * @param file
     * @param parent
     * @return
     * @throws ExternalConfigException
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws ParseException
     * @throws ClassNotFoundException
     */
    public static Factory staffFactory(File file, Factory parent) throws ExternalConfigException, IOException, NoSuchMethodException, ParseException, ClassNotFoundException {
        Object blueprint;
        InputStream is = new FileInputStream(file);
        try {
            blueprint = ObjectParser.parse(is);
            if (!(blueprint instanceof DTObject)) {
                throw new ExternalConfigException("Root value of the HRDT" + " configuration file must be an object");
            }
        } finally {
            is.close();
        }
        return staffFactory((DTObject) blueprint, parent, file.toString());
    }
}
