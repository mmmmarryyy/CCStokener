package com.makotan.util.property.impl;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import com.makotan.exception.IORuntimeException;
import com.makotan.util.property.PropertyFileUtil;
import com.makotan.util.property.UniResourceBundle;

public class PropertyFileUtilImpl implements PropertyFileUtil {

    private String prefix = "";

    private String postfix = ".properties";

    private boolean fstMode = false;

    private boolean fileNotFoundIsException = false;

    private Map<String, CacheInfo> cache = Collections.synchronizedMap(new HashMap<String, CacheInfo>());

    public PropertyFileUtilImpl() {
    }

    public UniResourceBundle getResourceBundle(String resourceName, Locale locale) {
        UniResourceBundle bundle = null;
        String localeFileName = getLocaleResourceName(resourceName, locale);
        String defFileName = getDefaultResourceName(resourceName);
        try {
            bundle = getCache(resourceName, localeFileName, defFileName);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return bundle;
    }

    public UniResourceBundle getResourceBundle(String resourceName, String envName) {
        UniResourceBundle bundle = null;
        String localeFileName = getEnvResourceName(resourceName, envName);
        String defFileName = getDefaultResourceName(resourceName);
        try {
            bundle = getCache(resourceName, localeFileName, defFileName);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return bundle;
    }

    public String getText(String resourceName, Locale locale, String key) {
        UniResourceBundle bundle = this.getResourceBundle(resourceName, locale);
        return bundle.getPlanText(key);
    }

    public String getText(String resourceName, String key) {
        UniResourceBundle bundle = this.getResourceBundle(resourceName);
        return bundle.getPlanText(key);
    }

    public String getTextEnv(String resourceName, String evnName, String key) {
        UniResourceBundle bundle = this.getResourceBundle(resourceName, evnName);
        return bundle.getPlanText(key);
    }

    public String getFormatText(String resourceName, Locale locale, String key, Object... args) {
        UniResourceBundle bundle = this.getResourceBundle(resourceName, locale);
        return bundle.getFormatText(key, args);
    }

    public String getFormatText(String resourceName, String key, Object... args) {
        UniResourceBundle bundle = this.getResourceBundle(resourceName);
        return bundle.getFormatText(key, args);
    }

    public String getFormatTextEnv(String resourceName, String evnName, String key, Object... args) {
        UniResourceBundle bundle = this.getResourceBundle(resourceName, evnName);
        return bundle.getFormatText(key, args);
    }

    protected UniResourceBundle getCache(String resourceName, String localeFileName, String defFileName) throws IOException {
        if (fstMode && cache.containsKey(resourceName)) {
            return cache.get(resourceName).getBundle();
        }
        FileInfo localInfo = createFileInfo(localeFileName);
        FileInfo defInfo = createFileInfo(defFileName);
        if (isThrowFileSystemException(localInfo, defInfo)) {
        }
        if (cache.containsKey(resourceName)) {
            CacheInfo info = cache.get(resourceName);
            if (hitCacheInfo(info, localInfo, defInfo)) {
                return info.getBundle();
            }
        }
        return setupCacheUniResourceBundle(resourceName, localInfo, defInfo);
    }

    protected boolean hitCacheInfo(CacheInfo info, FileInfo localInfo, FileInfo defInfo) {
        if (info.getDefModTime() == defInfo.lastModTime && info.getLocalModTime() == localInfo.lastModTime) {
            return true;
        }
        return false;
    }

    protected boolean isThrowFileSystemException(FileInfo localInfo, FileInfo defInfo) {
        if (fileNotFoundIsException && localInfo.fileName == null && defInfo.fileName == null) {
            return true;
        }
        return false;
    }

    protected FileInfo createFileInfo(String fileName) throws IOException {
        FileInfo info = new FileInfo();
        URL resource = getUrl(fileName);
        if (resource == null) {
            info.lastModTime = 0;
            info.fileName = null;
        } else {
            info.lastModTime = resource.openConnection().getDate();
            info.fileName = fileName;
        }
        return info;
    }

    protected URL getUrl(String fileName) {
        if (fileName == null) {
            return null;
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL resource = contextClassLoader.getResource(fileName);
        return resource;
    }

    protected UniResourceBundle setupCacheUniResourceBundle(String resourceName, FileInfo localInfo, FileInfo defInfo) throws IOException {
        CacheInfo info = new CacheInfo();
        info.setDefModTime(defInfo.lastModTime);
        info.setLocalModTime(localInfo.lastModTime);
        UniResourceBundle bundle = createUniResourceBundle(localInfo.fileName, defInfo.fileName);
        if (bundle == null && (!fileNotFoundIsException)) {
            bundle = new UniResourceBundle(new Properties());
        }
        info.setBundle(bundle);
        cache.put(resourceName, info);
        return info.getBundle();
    }

    protected UniResourceBundle createUniResourceBundle(String localeFileName, String defFileName) throws IOException {
        UniResourceBundle resultBundle = null;
        UniResourceBundle localBundle = createUniResourceBundle(localeFileName);
        UniResourceBundle bundle = createUniResourceBundle(defFileName);
        if (bundle != null) {
            if (localBundle != null) {
                localBundle.setParentResourceBundle(bundle);
                resultBundle = localBundle;
            } else {
                resultBundle = bundle;
            }
        } else {
            resultBundle = localBundle;
        }
        return resultBundle;
    }

    protected UniResourceBundle createUniResourceBundle(String localeFileName) throws IOException {
        UniResourceBundle localBundle = null;
        URL resource = getUrl(localeFileName);
        if (resource == null) {
            return null;
        }
        URLConnection urlConnection = resource.openConnection();
        if (urlConnection.getDoInput()) {
            Properties properties = new Properties();
            properties.load(urlConnection.getInputStream());
            localBundle = new UniResourceBundle(properties);
        }
        return localBundle;
    }

    protected String getEnvResourceName(String resourceName, String envTitle) {
        String prop = System.getProperty(envTitle);
        return getDefaultResourceName(resourceName + "_" + prop);
    }

    protected String getDefaultResourceName(String resourceName) {
        return prefix + resourceName + postfix;
    }

    protected String getLocaleResourceName(String resourceName, Locale locale) {
        return getDefaultResourceName(resourceName + "_" + locale.getLanguage());
    }

    public UniResourceBundle getResourceBundle(String resourceName) {
        return getResourceBundle(resourceName, Locale.getDefault());
    }

    private class CacheInfo {

        private UniResourceBundle bundle;

        private long localModTime;

        private long defModTime;

        public UniResourceBundle getBundle() {
            return bundle;
        }

        public void setBundle(UniResourceBundle bundle) {
            this.bundle = bundle;
        }

        public long getDefModTime() {
            return defModTime;
        }

        public void setDefModTime(long defModTime) {
            this.defModTime = defModTime;
        }

        public long getLocalModTime() {
            return localModTime;
        }

        public void setLocalModTime(long localModTime) {
            this.localModTime = localModTime;
        }
    }

    private class FileInfo {

        private long lastModTime;

        private String fileName;
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isFstMode() {
        return fstMode;
    }

    public void setFstMode(boolean fstMode) {
        this.fstMode = fstMode;
    }

    public boolean isFileNotFoundIsException() {
        return fileNotFoundIsException;
    }

    public void setFileNotFoundIsException(boolean fileNotFoundIsNull) {
        this.fileNotFoundIsException = fileNotFoundIsNull;
    }
}
