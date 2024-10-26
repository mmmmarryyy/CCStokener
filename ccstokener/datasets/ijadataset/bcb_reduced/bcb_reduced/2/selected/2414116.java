package org.imogene.rcp.core;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.cfg.Configuration;
import org.imogene.common.dao.EntityDao;
import org.imogene.common.dao.LocalizedTextDao;
import org.imogene.common.data.SynchronizableUtil;
import org.imogene.common.data.handler.DataHandlerManager;
import org.imogene.common.data.handler.EntityHandler;
import org.imogene.rcp.core.login.Identity;
import org.imogene.sync.client.OptimizedSynchronizer;
import org.imogene.sync.client.SyncParameters;
import org.imogene.sync.client.binary.BinaryFile;
import org.imogene.sync.client.binary.BinaryFileConverter;
import org.imogene.sync.client.binary.BinaryFileHandler;
import org.imogene.sync.client.binary.BinaryFileHibernateDao;
import org.imogene.sync.client.binary.BinaryFileManager;
import org.imogene.sync.client.clientfilter.ClientFilterDao;
import org.imogene.sync.client.clientfilter.ClientFilterHandler;
import org.imogene.sync.client.dao.SyncParametersDao;
import org.imogene.sync.client.dao.sqlite.HistoryDaoHibernate;
import org.imogene.sync.client.dao.sqlite.SessionManager;
import org.imogene.sync.client.dao.sqlite.SyncParameterDaoHibernate;
import org.imogene.sync.client.dao.sqlite.SynchronizableEntityDaoHibernate;
import org.imogene.sync.client.http.OptimizedSyncClientHttp;
import org.imogene.sync.client.localizedtext.LocalizedTextHibernateDao;
import org.imogene.sync.client.serializer.xml.FilterFieldConverter;
import org.imogene.sync.client.serializer.xml.PasswordConverter;
import org.imogene.sync.client.serializer.xml.RolesConverter;
import org.imogene.sync.client.serializer.xml.SynchronizablesConverter;
import org.imogene.sync.serializer.ImogSerializer;
import org.imogene.sync.serializer.SerializerManager;
import org.imogene.sync.serializer.xml.AssociationConverter;
import org.imogene.sync.serializer.xml.ClassConverter;
import org.imogene.sync.serializer.xml.CollectionConverter;
import org.imogene.sync.serializer.xml.ImogXmlSerializer;
import org.imogene.sync.serializer.xml.PropertyConverter;
import org.imogene.uao.clientfilter.ClientFilter;
import org.imogene.uao.synchronizable.SynchronizableEntity;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import com.thoughtworks.xstream.converters.Converter;

/**
 * The activator class that controls the plug-in life cycle
 * @author MEDES-IMPS
 */
public class ImogPlugin extends AbstractUIPlugin {

    private Logger logger = Logger.getLogger("org.imogene.rcp.core.ImogPlugin");

    private OptimizedSynchronizer s;

    public static final String PLUGIN_ID = "org.imogene.rcp.core";

    public static final String TERMINAL_ID_PROPERTY = "TERMINAL_ID";

    public static final String BINARY_PATH_PROPERTY = "BINARY_PATH";

    private static ImogPlugin plugin;

    private Configuration dbConfig;

    private SerializerManager serializerManager;

    private DataHandlerManager dataHandlerManager;

    private Map<String, EntityHandler> handlers = new HashMap<String, EntityHandler>();

    private AssociationConverter associationConverter;

    private CollectionConverter collectionConverter;

    private PasswordConverter passwordConverter;

    private RolesConverter rolesConverter;

    private SynchronizablesConverter synchronizablesConverter;

    private FilterFieldConverter filterFieldConverter;

    private Set<PropertyConverter> converters = new HashSet<PropertyConverter>();

    private Set<ClassConverter> specificConverters = new HashSet<ClassConverter>();

    private Map<String, String> entityClassReferences = new HashMap<String, String>();

    private Set<SynchronizableEntity> directSend = new HashSet<SynchronizableEntity>();

    private Set<SynchronizableEntity> toSync = new HashSet<SynchronizableEntity>();

    private SyncParameters syncParameters;

    private SyncParametersDao parametersDao;

    private LocalizedTextDao localizedTextDao;

    private Identity currentUserIdentity;

    private File syncWork;

    private int period;

    /**
	 * The constructor
	 */
    public ImogPlugin() {
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        initPreferences();
        createStructure();
        initDatabase();
        initDataHandler();
        initSerializer();
        parseEntityExtension();
        startDb();
        setup();
        initSyncParameters();
        initSynchro();
    }

    public void stop(BundleContext context) throws Exception {
        savePluginPreferences();
        s.stopSynchronizer();
        plugin = null;
        super.stop(context);
    }

    /**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
    public static ImogPlugin getDefault() {
        return plugin;
    }

    /**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
	 * Parse the registered 'entity' extensions.
	 */
    private void parseEntityExtension() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint entity = registry.getExtensionPoint("org.imogene.rcp.core", "Entity");
        IExtension[] entities = entity.getExtensions();
        for (IExtension extension : entities) {
            referenceShortName(extension);
            addDbMapping(extension);
        }
        referenceShortName("BIN", BinaryFile.class.getName());
        referenceShortName("CLTFIL", ClientFilter.class.getName());
        IExtensionPoint handlerPoint = registry.getExtensionPoint("org.imogene.rcp.core", "DataHandler");
        IExtension[] handlers = handlerPoint.getExtensions();
        for (IExtension extension : handlers) {
            addHandler(extension);
        }
        IExtensionPoint classConverterPoint = registry.getExtensionPoint("org.imogene.rcp.core", "ClassConverter");
        IExtension[] classConverters = classConverterPoint.getExtensions();
        for (IExtension extension : classConverters) {
            addClassConverter(extension);
        }
        IExtensionPoint propertyConverterPoint = registry.getExtensionPoint("org.imogene.rcp.core", "PropertyConverter");
        IExtension[] propertyConverters = propertyConverterPoint.getExtensions();
        for (IExtension extension : propertyConverters) {
            addPropertyConverter(extension);
        }
    }

    /**
	 * Initialize the database for this 'Entity' extension.
	 * @param extension the 'Entity' extension
	 */
    private void addDbMapping(IExtension extension) {
        try {
            IConfigurationElement conf = extension.getConfigurationElements()[0];
            Bundle resource = Platform.getBundle(conf.getDeclaringExtension().getNamespaceIdentifier());
            String sqlResource = conf.getAttribute("mappingFile");
            if (sqlResource != null) {
                URL url = resource.getEntry(sqlResource);
                InputStream fin = url.openStream();
                dbConfig.addInputStream(fin);
                fin.close();
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    /**
	 * 
	 * @param extension
	 */
    private void referenceShortName(IExtension extension) {
        try {
            IConfigurationElement conf = extension.getConfigurationElements()[0];
            String shortName = conf.getAttribute("shortName");
            String className = conf.getAttribute("className");
            referenceShortName(shortName, className);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    /**
	 * 
	 * @param shortName
	 * @param className
	 */
    private void referenceShortName(String shortName, String className) {
        logger.debug("Referencing a short name for entity: " + className);
        entityClassReferences.put(shortName, className);
    }

    /**
	 * Return this terminal UUID.
	 * @return the terminal UUID.
	 */
    public String getTerminalId() {
        return getPreferenceStore().getString("TERMINAL_ID");
    }

    /**
	 * Initialize the data Handler with the common values.
	 */
    private void initDataHandler() {
        dataHandlerManager = new DataHandlerManager();
        BinaryFileHandler binaryHandler = new BinaryFileHandler();
        binaryHandler.setDao(new BinaryFileHibernateDao());
        binaryHandler.setDataHandlerManager(dataHandlerManager);
        handlers.put(BinaryFile.class.getName(), binaryHandler);
        ClientFilterHandler clientFilterHandler = new ClientFilterHandler();
        clientFilterHandler.setDao(new ClientFilterDao());
        handlers.put(ClientFilter.class.getName(), clientFilterHandler);
    }

    /**
	 * Initialize the serializer elements
	 */
    private void initSerializer() {
        serializerManager = new SerializerManager();
        associationConverter = new AssociationConverter();
        associationConverter.setDataHandlerManager(dataHandlerManager);
        collectionConverter = new CollectionConverter();
        collectionConverter.setDataHandlerManager(dataHandlerManager);
        passwordConverter = new PasswordConverter();
        rolesConverter = new RolesConverter();
        synchronizablesConverter = new SynchronizablesConverter();
        filterFieldConverter = new FilterFieldConverter();
        BinaryFileConverter binaryConverter = new BinaryFileConverter();
        ClassConverter binaryClassConverter = new ClassConverter();
        binaryClassConverter.setAlias("org.imogene.data.Binary");
        binaryClassConverter.setClassType("org.imogene.sync.client.binary.BinaryFile");
        binaryClassConverter.setConverter(binaryConverter);
        specificConverters.add(binaryClassConverter);
    }

    /**
	 * Registers a property converter
	 * @param extension the converter definition
	 */
    private void addPropertyConverter(IExtension extension) {
        try {
            IConfigurationElement conf = extension.getConfigurationElements()[0];
            String entityClassName = conf.getAttribute("entityClassName");
            String propertyName = conf.getAttribute("propertyName");
            String converterType = conf.getAttribute("type");
            PropertyConverter converter = new PropertyConverter();
            converter.setClassName(entityClassName);
            if (converterType.equals("association")) {
                converter.setConverter(associationConverter);
            } else if (converterType.equals("collection")) {
                converter.setConverter(collectionConverter);
            } else if (converterType.equals("password")) {
                converter.setConverter(passwordConverter);
            } else if (converterType.equals("roles")) {
                converter.setConverter(rolesConverter);
            } else if (converterType.equals("synchronizables")) {
                converter.setConverter(synchronizablesConverter);
            } else if (converterType.equals("filterfield")) {
                converter.setConverter(filterFieldConverter);
            }
            converter.setPropertyName(propertyName);
            converters.add(converter);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
	 * Register a class converter
	 * @param extension the converter definition
	 */
    private void addClassConverter(IExtension extension) {
        try {
            IConfigurationElement conf = extension.getConfigurationElements()[0];
            String alias = conf.getAttribute("alias");
            String entityClassName = conf.getAttribute("entityClassName");
            Converter entityConverter = (Converter) conf.createExecutableExtension("converterClass");
            ClassConverter classConverter = new ClassConverter();
            classConverter.setAlias(alias);
            classConverter.setClassType(entityClassName);
            classConverter.setConverter(entityConverter);
            specificConverters.add(classConverter);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
	 * Add an entity handler to the application
	 * @param handlerExtension the handler definition
	 */
    private void addHandler(IExtension handlerExtension) {
        try {
            IConfigurationElement conf = handlerExtension.getConfigurationElements()[0];
            String entityClassName = conf.getAttribute("entityClassName");
            EntityDao dao = (EntityDao) conf.createExecutableExtension("dao");
            EntityHandler handler = (EntityHandler) conf.createExecutableExtension("handler");
            handler.setDao(dao);
            boolean hasTranslatableFields = Boolean.parseBoolean(conf.getAttribute("hasTranslatableField"));
            if (hasTranslatableFields) {
                if (localizedTextDao == null) localizedTextDao = new LocalizedTextHibernateDao();
                handler.setI18nDao(localizedTextDao);
            }
            handlers.put(entityClassName, handler);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
	 * Initialize the common properties of the database
	 */
    private void initDatabase() {
        dbConfig = new Configuration();
        dbConfig.setProperty("hibernate.show_sql", "false");
        dbConfig.setProperty("hibernate.format_sql", "true");
        dbConfig.setProperty("hibernate.dialect", "org.imogene.sync.client.dao.sqlite.SQLiteDialect");
        dbConfig.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
        dbConfig.setProperty("hibernate.connection.url", "jdbc:sqlite:" + Platform.getLocation().toOSString() + "/imog.db");
        dbConfig.setProperty("hibernate.hbm2ddl.auto", "update");
        InputStream historyMapping = getClass().getClassLoader().getResourceAsStream("org/imogene/sync/client/history/SyncHistory.hbm.xml");
        dbConfig.addInputStream(historyMapping);
        InputStream binaryMapping = getClass().getClassLoader().getResourceAsStream("org/imogene/sync/client/binary/BinaryFile.hbm.xml");
        dbConfig.addInputStream(binaryMapping);
        InputStream roleMapping = getClass().getClassLoader().getResourceAsStream("org/imogene/uao/role/Role.hbm.xml");
        dbConfig.addInputStream(roleMapping);
        InputStream paramMapping = getClass().getClassLoader().getResourceAsStream("org/imogene/sync/client/SyncParameters.hbm.xml");
        dbConfig.addInputStream(paramMapping);
        InputStream syncMapping = getClass().getClassLoader().getResourceAsStream("org/imogene/uao/synchronizable/SynchronizableEntity.hbm.xml");
        dbConfig.addInputStream(syncMapping);
        InputStream clientFilterMapping = getClass().getClassLoader().getResourceAsStream("org/imogene/uao/clientfilter/ClientFilter.hbm.xml");
        dbConfig.addInputStream(clientFilterMapping);
        InputStream localizedTextMapping = getClass().getClassLoader().getResourceAsStream("org/imogene/sync/client/localizedtext/LocalizedText.hbm.xml");
        dbConfig.addInputStream(localizedTextMapping);
    }

    /**
	 * Start the database
	 */
    private void startDb() {
        SessionManager.setConfiguration(dbConfig);
        logger.debug("Opening the session.");
        SessionManager.getInstance().getSession();
    }

    /**
	 * Create the directories structure
	 */
    private void createStructure() {
        Location location = Platform.getInstanceLocation();
        String binaryDirPath = location.getURL().getPath() + "/binary/";
        File binaryDir = new File(binaryDirPath);
        if (!binaryDir.exists()) binaryDir.mkdir();
        BinaryFileManager.getInstance().setBinary_file_dir(binaryDirPath);
        getPreferenceStore().setValue(BINARY_PATH_PROPERTY, binaryDirPath);
        File idDir = new File(location.getURL().getPath() + "/identity");
        if (!idDir.exists()) idDir.mkdir();
        syncWork = new File(location.getURL().getPath() + "/syncWork");
        if (!syncWork.exists()) syncWork.mkdir();
    }

    /**
	 * Setup the system properties 
	 * created from the extension points.
	 */
    private void setup() {
        SynchronizableUtil.getInstance().setEntityClassReferences(entityClassReferences);
        dataHandlerManager.setHandlers(handlers);
        ImogXmlSerializer xmlSerializer = new ImogXmlSerializer(ImogPlugin.class.getClassLoader());
        xmlSerializer.setDataHandlerManager(dataHandlerManager);
        xmlSerializer.setClassConverters(specificConverters);
        xmlSerializer.setPropertyConverters(converters);
        Map<String, ImogSerializer> serializers = new HashMap<String, ImogSerializer>();
        serializers.put("xml", xmlSerializer);
        serializerManager.setSerializers(serializers);
    }

    /**
	 * 
	 * @return
	 */
    public DataHandlerManager getDataHandlerManager() {
        return dataHandlerManager;
    }

    /**
	 * start the synchronization thread
	 */
    private void initSynchro() {
        s = new OptimizedSynchronizer(syncWork.getAbsolutePath());
        s.setLoop(getPreferenceStore().getBoolean("SYNC_LOOP"));
        s.setPeriod(getPreferenceStore().getInt("SYNC_PERIOD"));
        s.setDataManager(dataHandlerManager);
        s.setSerializerManager(serializerManager);
        s.setHistoryDao(new HistoryDaoHibernate());
        SyncParameters parameters = parametersDao.load();
        s.setSyncClient(new OptimizedSyncClientHttp(getPreferenceStore().getString("SYNC_URL"), parameters.getLogin(), parameters.getPassword()));
        s.setSyncParametersDao(parametersDao);
    }

    public OptimizedSynchronizer startSynchro() {
        initSynchro();
        s.start();
        return s;
    }

    public void restartServer(boolean loop, int period) {
        if (!loop && s.getLoop()) {
            s.stopSynchronizer();
        }
        if (loop && (!s.getLoop() || period != s.getPeriod())) {
            s.stopSynchronizer();
            new Thread() {

                @Override
                public void run() {
                    while (s.getState() != Thread.State.TERMINATED && s.getState() != Thread.State.NEW) {
                        try {
                            sleep(1000);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    startSynchro();
                }
            }.start();
        }
    }

    /**
	 * create the sync parameters
	 */
    private void initSyncParameters() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint entity = registry.getExtensionPoint("org.imogene.rcp.core", "Entity");
        IExtension[] entities = entity.getExtensions();
        for (IExtension extension : entities) {
            IConfigurationElement conf = extension.getConfigurationElements()[0];
            try {
                if (conf.getAttribute("synchronizable").equals("true")) {
                    String className = conf.getAttribute("className");
                    String shortName = conf.getAttribute("shortName");
                    if (className != null && !className.equals("")) toSync.add(addEntity(className, shortName));
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        toSync.add(addEntity(BinaryFile.class.getName(), "BIN"));
        toSync.add(addEntity(ClientFilter.class.getName(), "CLTFIL"));
        parametersDao = new SyncParameterDaoHibernate();
        syncParameters = new SyncParameters();
        syncParameters.setModifiedFrom(getTerminalId().toString());
        syncParameters.setDirectSend(directSend);
        syncParameters.setSynchronizable(toSync);
        syncParameters.setType("xml");
        syncParameters.setUrl(getPreferenceStore().getString("SYNC_URL"));
        parametersDao.store(syncParameters);
    }

    /**
	 * Sets the current user
	 * @param currentUserIdentity the current user identity
	 */
    public void setCurrentUserIdentity(Identity currentUserIdentity) {
        this.currentUserIdentity = currentUserIdentity;
        setUserParameters();
    }

    /**
	 * Gets the current user
	 * @return
	 */
    public Identity getCurrentUserIdentity() {
        return currentUserIdentity;
    }

    /**
	 * adds the user login and password to the sync parameters
	 * @param login the user login
	 * @param password the user password
	 */
    private void setUserParameters() {
        if (syncParameters == null || currentUserIdentity == null) throw new RuntimeException("parameters not initialized, thread not started"); else {
            syncParameters.setLogin(currentUserIdentity.getLogin());
            syncParameters.setPassword(currentUserIdentity.getPassword());
            parametersDao.store(syncParameters);
        }
    }

    /**
	 * returns the sync parameters
	 * @return the sync parameters
	 */
    public SyncParameters getUserParameters() {
        return syncParameters;
    }

    /**
	 * Add an entity to entity that could be synchronizable
	 * @param classToAdd the entity class
	 * @return the synchronizable entity description
	 */
    private SynchronizableEntity addEntity(String className, String shortName) {
        SynchronizableEntity entity = new SynchronizableEntity();
        entity.setId(shortName);
        entity.setName(className);
        entity.setModified(new Date(System.currentTimeMillis()));
        SynchronizableEntityDaoHibernate dao = new SynchronizableEntityDaoHibernate();
        dao.store(entity);
        return entity;
    }

    private void initPreferences() {
        String url = ImogPlugin.getDefault().getPreferenceStore().getString("SYNC_URL");
        if (url == null || url.equals("")) {
            url = "http://localhost:8080/ImogeneSynchro/";
            ImogPlugin.getDefault().getPreferenceStore().setValue("SYNC_URL", url);
        }
        period = ImogPlugin.getDefault().getPreferenceStore().getInt("SYNC_PERIOD");
        if (period == 0) {
            period = 10000;
            ImogPlugin.getDefault().getPreferenceStore().setValue("SYNC_PERIOD", period);
        }
        String termId = ImogPlugin.getDefault().getPreferenceStore().getString("TERMINAL_ID");
        if (termId == null || termId.equals("")) {
            termId = UUID.randomUUID().toString();
            ImogPlugin.getDefault().getPreferenceStore().setValue("TERMINAL_ID", termId);
        }
        getPreferenceStore().setDefault(Constants.KEY_BACKUP_PATH, Constants.BACUP_PATH_DEFAULT);
        getPreferenceStore().setDefault(Constants.KEY_BACKUP_ACTIVATED, Constants.BACKUP_ACTIVATED_DEFAULT);
        getPreferenceStore().setDefault(Constants.KEY_BACKUP_ENCRYPTION, Constants.BACKUP_ENCRYPTION_DEFAULT);
        savePluginPreferences();
    }

    public String getBackupPath() {
        return getPreferenceStore().getString(Constants.KEY_BACKUP_PATH);
    }

    public boolean IsBackupActivated() {
        return getPreferenceStore().getBoolean(Constants.KEY_BACKUP_ACTIVATED);
    }

    public boolean isBackupEncrypted() {
        return getPreferenceStore().getBoolean(Constants.KEY_BACKUP_ENCRYPTION);
    }
}
