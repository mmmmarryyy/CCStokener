package net.sf.joafip.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import net.sf.joafip.NoStorableAccess;
import net.sf.joafip.entity.EnumNoMoreDataAction;
import net.sf.joafip.entity.EnumStoreMode;
import net.sf.joafip.entity.FilePersistenceProperties;
import net.sf.joafip.entity.FilePersistencePropertyEntry;
import net.sf.joafip.store.service.conversion.def.InputStreamAndSource;

/**
 * 
 * @author luc peuvrier
 * 
 */
@NoStorableAccess
public final class FilePersistencePropertiesReader {

    private static final FilePersistencePropertiesReader INSTANCE = new FilePersistencePropertiesReader();

    /** data model identifier construction parameter */
    private static final String JOAFIP_DATA_MODEL_IDENTIFIER = "joafip.dataModelIdentifier";

    /** conversion definition file construction parameter */
    private static final String JOAFIP_DATA_MODEL_CONVERSION_DEF_FILE = "joafip.dataModelConvertionDefFile";

    /** storage directory path construction parameter */
    private static final String JOAFIP_PATH = "joafip.path";

    /** crash safe mode on/off construction parameter */
    private static final String JOAFIP_CRASH_SAFE_MODE = "joafip.crashSafeMode";

    /** proxy mode on/off construction parameter */
    private static final String JOAFIP_PROXY_MODE = "joafip.proxyMode";

    /** use cache on/off construction parameter */
    private static final String JOAFIP_FILE_CACHE = "joafip.fileCache";

    /** cache page size construction parameter */
    private static final String JOAFIP_FILE_CACHE_PAGE_SIZE = "joafip.fileCache.pageSize";

    /** cache max number of page construction parameter */
    private static final String JOAFIP_FILE_CACHE_MAX_PAGE = "joafip.fileCache.maxPage";

    /** garbage management on/off construction parameter */
    private static final String JOAFIP_GARBAGE_MANAGEMENT = "joafip.garbageManagement";

    /** zip compression level setup */
    private static final String JOAFIP_ZIP_COMPRESSION_LEVEL = "joafip.zipCompressionLevel";

    /** substitution of java util collection */
    private static final String JOAFIP_SUBSTITUTION_OF_JAVA_UTIL_COLLECTION = "joafip.substitutionOfJavaUtilCollection";

    /** activate or not background garbage sweep */
    private static final String JOAFIP_BACKGROUND_GARBAGE_SWEEP = "joafip.backgroundGarbageSweep";

    /** declare stored enum */
    @Deprecated
    private static final String JOAFIP_STORED_ENUM = "joafip.storedEnum.";

    /** declare stored enum */
    private static final String JOAFIP_STORED_MUTABLE_ENUM = "joafip.storedMutableEnum.";

    /** declare stored enum */
    private static final String JOAFIP_STORED_IMMUTABLE_ENUM = "joafip.storedImmutableEnum.";

    /** install custom object i/o */
    private static final String JOAFIP_OBJECT_I_O = "joafip.objectIO.";

    /** set force enhance of class */
    private static final String JOAFIP_FORCE_ENHANCE = "joafip.forceEnhance.";

    /** set storing mode of class */
    private static final String JOAFIP_STORE_MODE = "joafip.storeMode.";

    /** set a class to be not storable */
    private static final String JOAFIP_NOT_STORABLE = "joafip.notStorable.";

    /** set a class to be deprecated in store */
    private static final String JOAFIP_DEPRECATED_IN_STORE = "joafip.deprecatedInStore.";

    private static final String JOAFIP_STORE_ONLY_MARKED_STORABLE = "joafip.storeOnlyMarkedStorable";

    private static final String JOAFIP_RECORD_SAVE_ACTIONS = "joafip.recordSaveActions";

    /** set substitution */
    private static final String JOAFIP_SUBSTITUTE = "joafip.substitute.";

    private static final String JOAFIP_MAINTENED_IN_MEMORY = "joafip.maintainedInMemory";

    private static final String JOAFIP_MAINTENED_IN_MEMORY_QUOTA = "joafip.maintainedInMemoryQuota";

    private static final String JOAFIP_AUTO_SAVE = "joafip.autoSave";

    private static final String JOAFIP_MAX_IN_MEMORY_THRESHOLD = "joafip.maxInMemoryThreshold";

    private static final String JOAFIP_DATA_FILE_NAME = "joafip.dataFileName";

    private static final String JOAFIP_BACKUP_DATA_FILE_NAME = "joafip.backupDataFileName";

    private static final String JOAFIP_STATE_OK_FLAG_FILE_NAME = "joafip.stateOkFlagFileName";

    private static final String JOAFIP_STATE_BACKUP_OK_FLAG_FILE_NAME = "joafip.stateBackupOkFlagFileName";

    private static final String JOAFIP_GLOBAL_STATE_FLAG_FILE_NAME = "joafip.globalStateFlagFileName";

    private static final String JOAFIP_MAX_FILE_OPERATION_RETRY = "joafip.maxFileOperationRetry";

    private static final String JOAFIP_FILE_OPERATION_RETRY_MS_DELAY = "joafip.fileOperationRetryMsDelay";

    private static final String JOAFIP_NO_MORE_DATA_ACTION = "joafip.noMoreDataAction";

    private static final String DISABLED = "disabled";

    private static final String ENABLED = "enabled";

    public static FilePersistencePropertiesReader getInstance() {
        return INSTANCE;
    }

    private FilePersistencePropertiesReader() {
        super();
    }

    /**
	 * load the setup properties from properties resource
	 * 
	 * @param propertiesSetupResourceName
	 *            setup properties
	 * @param classLoaderProvider
	 * @throws FilePersistenceException
	 */
    public void read(final String propertiesSetupResourceName, final ClassLoaderProvider classLoaderProvider, final FilePersistenceProperties filePersistenceProperties) throws FilePersistenceException {
        try {
            final URL url = classLoaderProvider.getResource(propertiesSetupResourceName);
            final InputStream inputStream = url.openStream();
            final Properties properties = new Properties();
            properties.load(inputStream);
            final Set<Entry<Object, Object>> entrySet = properties.entrySet();
            for (Entry<Object, Object> entry : entrySet) {
                final String key = (String) entry.getKey();
                final String value = (String) entry.getValue();
                if (JOAFIP_PATH.equals(key)) {
                    filePersistenceProperties.setPathName(value);
                } else if (JOAFIP_DATA_MODEL_IDENTIFIER.equals(key)) {
                    filePersistenceProperties.setDataModelIdentifier(intValue(key, value));
                } else if (JOAFIP_DATA_MODEL_CONVERSION_DEF_FILE.equals(key)) {
                    final URL conversionDefUrl = classLoaderProvider.getResource(value);
                    final InputStream conversionDefInputStream = conversionDefUrl.openStream();
                    final InputStreamAndSource fileInputStreamAndSource = new InputStreamAndSource(conversionDefInputStream, value);
                    filePersistenceProperties.setDataModelConversionDefInputStream(fileInputStreamAndSource);
                } else if (JOAFIP_GARBAGE_MANAGEMENT.equals(key)) {
                    filePersistenceProperties.setGarbageManagement(isEnabled(key, value));
                } else if (JOAFIP_PROXY_MODE.equals(key)) {
                    filePersistenceProperties.setProxyMode(isEnabled(key, value));
                } else if (JOAFIP_CRASH_SAFE_MODE.equals(key)) {
                    filePersistenceProperties.setCrashSafeMode(isEnabled(key, value));
                } else if (JOAFIP_FILE_CACHE.equals(key)) {
                    filePersistenceProperties.setUseCacheMode(isEnabled(key, value));
                } else if (JOAFIP_FILE_CACHE_PAGE_SIZE.equals(key)) {
                    filePersistenceProperties.setPageSize(intValue(key, value));
                } else if (JOAFIP_FILE_CACHE_MAX_PAGE.equals(key)) {
                    filePersistenceProperties.setMaxPage(intValue(key, value));
                } else if (JOAFIP_ZIP_COMPRESSION_LEVEL.equals(key)) {
                    filePersistenceProperties.setZipCompressionLevelSetted(true);
                    filePersistenceProperties.setZipCompressionLevel(intValue(key, value));
                } else if (JOAFIP_SUBSTITUTION_OF_JAVA_UTIL_COLLECTION.equals(key)) {
                    filePersistenceProperties.setSubsOfJavaUtil(isEnabled(key, value));
                } else if (key.startsWith(JOAFIP_SUBSTITUTE)) {
                    addSubtitution(filePersistenceProperties, key.substring(JOAFIP_SUBSTITUTE.length()), value);
                } else if (key.startsWith(JOAFIP_STORE_MODE)) {
                    addStoreMode(filePersistenceProperties, key.substring(JOAFIP_STORE_MODE.length()), value);
                } else if (key.startsWith(JOAFIP_NOT_STORABLE)) {
                    addNotStorable(filePersistenceProperties, key.substring(JOAFIP_NOT_STORABLE.length()), isEnabled(key, value));
                } else if (key.startsWith(JOAFIP_DEPRECATED_IN_STORE)) {
                    addDeprecatedInStore(filePersistenceProperties, key.substring(JOAFIP_DEPRECATED_IN_STORE.length()), isEnabled(key, value));
                } else if (key.equals(JOAFIP_STORE_ONLY_MARKED_STORABLE)) {
                    filePersistenceProperties.setStoreOnlyMarkedStorable(isEnabled(key, value));
                } else if (key.equals(JOAFIP_RECORD_SAVE_ACTIONS)) {
                    filePersistenceProperties.setRecordSaveActions(isEnabled(key, value));
                } else if (key.startsWith(JOAFIP_FORCE_ENHANCE)) {
                    addForceEnhance(filePersistenceProperties, key.substring(JOAFIP_FORCE_ENHANCE.length()), isEnabled(key, value));
                } else if (key.startsWith(JOAFIP_OBJECT_I_O)) {
                    final String[] split = value.split(";");
                    if (split.length != 2) {
                        throw new FilePersistenceException("must define two class name " + key + "=" + value);
                    }
                    addObjectIo(filePersistenceProperties, key.substring(JOAFIP_OBJECT_I_O.length()), split[0], split[1]);
                } else if (key.startsWith(JOAFIP_STORED_ENUM)) {
                    addStoredMutableEnum(filePersistenceProperties, key.substring(JOAFIP_STORED_ENUM.length()), isEnabled(key, value));
                } else if (key.startsWith(JOAFIP_STORED_MUTABLE_ENUM)) {
                    addStoredMutableEnum(filePersistenceProperties, key.substring(JOAFIP_STORED_MUTABLE_ENUM.length()), isEnabled(key, value));
                } else if (key.startsWith(JOAFIP_STORED_IMMUTABLE_ENUM)) {
                    addStoredImmutableEnum(filePersistenceProperties, key.substring(JOAFIP_STORED_IMMUTABLE_ENUM.length()), isEnabled(key, value));
                } else if (JOAFIP_BACKGROUND_GARBAGE_SWEEP.equals(key)) {
                    final boolean backgroundGarbageSweepEnabled = true ^ DISABLED.equals(value);
                    filePersistenceProperties.setBackgroundGarbageSweepEnabled(backgroundGarbageSweepEnabled);
                    if (backgroundGarbageSweepEnabled) {
                        filePersistenceProperties.setBackgroundGarbageSweepSleepTime(intValue(key, value));
                    }
                } else if (key.equals(JOAFIP_MAINTENED_IN_MEMORY)) {
                    filePersistenceProperties.setMaintenedInMemory(isEnabled(key, value));
                } else if (key.equals(JOAFIP_MAINTENED_IN_MEMORY_QUOTA)) {
                    filePersistenceProperties.setMaintenedInMemoryQuota(intValue(key, value));
                } else if (key.equals(JOAFIP_AUTO_SAVE)) {
                    filePersistenceProperties.setAutoSaveEnabled(isEnabled(key, value));
                } else if (key.equals(JOAFIP_MAX_IN_MEMORY_THRESHOLD)) {
                    filePersistenceProperties.setMaxInMemoryThreshold(intValue(key, value));
                } else if (key.equals(JOAFIP_DATA_FILE_NAME)) {
                    filePersistenceProperties.setDataFileName(value);
                } else if (key.equals(JOAFIP_BACKUP_DATA_FILE_NAME)) {
                    filePersistenceProperties.setBackupDataFileName(value);
                } else if (key.equals(JOAFIP_STATE_OK_FLAG_FILE_NAME)) {
                    filePersistenceProperties.setStateOkFlagFileName(value);
                } else if (key.equals(JOAFIP_STATE_BACKUP_OK_FLAG_FILE_NAME)) {
                    filePersistenceProperties.setStateBackupOkFlagFileName(value);
                } else if (key.equals(JOAFIP_GLOBAL_STATE_FLAG_FILE_NAME)) {
                    filePersistenceProperties.setGlobalStateFlagFileName(value);
                } else if (key.equals(JOAFIP_MAX_FILE_OPERATION_RETRY)) {
                    filePersistenceProperties.setMaxFileOperationRetry(intValue(key, value));
                } else if (key.equals(JOAFIP_FILE_OPERATION_RETRY_MS_DELAY)) {
                    filePersistenceProperties.setFileOperationRetryMsDelay(intValue(key, value));
                } else if (key.equals(JOAFIP_NO_MORE_DATA_ACTION)) {
                    final EnumNoMoreDataAction noMoreDataAction;
                    if ("delete".equals(value)) {
                        noMoreDataAction = EnumNoMoreDataAction.DELETE_FILE;
                    } else if ("resize".equals(value)) {
                        noMoreDataAction = EnumNoMoreDataAction.RESIZE_FILE;
                    } else if ("rename".equals(value)) {
                        noMoreDataAction = EnumNoMoreDataAction.RENAME_FILE;
                    } else if ("preserve".equals(value)) {
                        noMoreDataAction = EnumNoMoreDataAction.PRESERVE_FILE;
                    } else {
                        throw new FilePersistenceException("on of 'delete','resize','rename','preserve' value expected for key " + key);
                    }
                    filePersistenceProperties.setNoMoreDataAction(noMoreDataAction);
                }
            }
        } catch (MalformedURLException exception) {
            throw new FilePersistenceException(exception);
        } catch (IOException exception) {
            throw new FilePersistenceException(exception);
        } catch (URISyntaxException exception) {
            throw new FilePersistenceException(exception);
        }
    }

    private int intValue(final String key, final String value) throws FilePersistenceException {
        final int intValue;
        try {
            intValue = Integer.parseInt(value);
        } catch (Exception exception) {
            throw new FilePersistenceException("integer value expected for key " + key, exception);
        }
        return intValue;
    }

    private boolean isEnabled(final String key, final String value) throws FilePersistenceException {
        final String lowerCase = value.toLowerCase();
        final boolean result;
        if (ENABLED.equals(lowerCase)) {
            result = true;
        } else if (DISABLED.equals(lowerCase)) {
            result = false;
        } else {
            throw new FilePersistenceException("\"" + ENABLED + "\" or \"" + DISABLED + "\" expected for key " + key);
        }
        return result;
    }

    private void addSubtitution(final FilePersistenceProperties filePersistenceProperties, final String toSubstituteClassName, final String substitudeAndSynchro) throws FilePersistenceException {
        final String[] strings = substitudeAndSynchro.split(",");
        if (strings.length != 2) {
            throw new FilePersistenceException("bad value " + substitudeAndSynchro);
        }
        final String substituteClassName = strings[0];
        final String synchronizerClassName = strings[1];
        filePersistenceProperties.getSubstitutionSet().add(new FilePersistencePropertyEntry(new String[] { toSubstituteClassName, substituteClassName, synchronizerClassName }));
    }

    private void addStoreMode(final FilePersistenceProperties filePersistenceProperties, final String pathClassName, final String value) throws FilePersistenceException {
        final EnumStoreMode storeMode = EnumStoreMode.forValue(value);
        if (storeMode == null) {
            throw new FilePersistenceException("bad value " + value + ", expected " + EnumStoreMode.NOT_USE_STANDARD_SERIALIZATION.getValue() + ", " + EnumStoreMode.SERIALIZE_AND_G_ZIPPED_IN_ONE_RECORD.getValue() + ", " + EnumStoreMode.SERIALIZE_AND_ZIPPED_IN_ONE_RECORD.getValue() + ", " + EnumStoreMode.SERIALIZE_IN_ONE_RECORD.getValue() + ", or " + EnumStoreMode.STORE_NOT_LAZY + " expected");
        }
        filePersistenceProperties.getStoreModeMap().put(pathClassName, storeMode);
    }

    private void addNotStorable(final FilePersistenceProperties filePersistenceProperties, final String pathClassName, final boolean value) throws FilePersistenceException {
        filePersistenceProperties.getNotStorableMap().put(pathClassName, value);
    }

    private void addDeprecatedInStore(final FilePersistenceProperties filePersistenceProperties, final String pathClassName, final boolean value) throws FilePersistenceException {
        filePersistenceProperties.getDeprecatedInStoreMap().put(pathClassName, value);
    }

    private void addStoredMutableEnum(final FilePersistenceProperties filePersistenceProperties, final String enumClassName, final boolean enabled) {
        if (enabled) {
            filePersistenceProperties.getMutableEnumSet().add(enumClassName);
        }
    }

    private void addStoredImmutableEnum(final FilePersistenceProperties filePersistenceProperties, final String enumClassName, final boolean enabled) {
        if (enabled) {
            filePersistenceProperties.getImmutableEnumSet().add(enumClassName);
        }
    }

    private void addObjectIo(final FilePersistenceProperties filePersistenceProperties, final String className, final String objectInputClassName, final String objectOutputClassName) {
        filePersistenceProperties.getObjectIoSet().add(new FilePersistencePropertyEntry(new String[] { className, objectInputClassName, objectOutputClassName }));
    }

    private void addForceEnhance(final FilePersistenceProperties filePersistenceProperties, final String className, final boolean enabled) {
        if (enabled) {
            filePersistenceProperties.getForceEnhanceSet().add(className);
        }
    }
}
