package com.mainatom.utils;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Информация о версии приложения. Версия загружается для package. Подразумевается,
 * что в нем есть файл version.properties со строками version и date
 */
public class VersionInfo {

    private static String VERSION_FILE = "version.properties";

    private String _package = "com.mainatom";

    private String _version;

    private String _versionBuild;

    private String _date;

    /**
     * Инициализирует объект версией указанного пакета
     *
     * @param aPackage пакет
     */
    public VersionInfo(String aPackage) {
        _package = aPackage;
        loadVersion();
    }

    /**
     * Инициализирует объект версией пакета, в котором находится объект
     *
     * @param main объект
     */
    public VersionInfo(Object main) {
        try {
            _package = main.getClass().getPackage().getName();
        } catch (Exception e) {
        }
        loadVersion();
    }

    /**
     * Версия
     */
    public String getVersion() {
        String sV = _version == null ? "SNAPSHOT" : _version;
        String sB = _versionBuild == null ? "" : _versionBuild;
        if (sB.length() > 0) {
            return sV + "-b" + sB;
        } else {
            return sV;
        }
    }

    /**
     * Дата
     */
    public String getDate() {
        return _date == null ? "LAST" : _date;
    }

    private void loadVersion() {
        try {
            String res = "/" + _package.replace('.', '/') + "/" + VERSION_FILE;
            URL url = getClass().getResource(res);
            if (url != null) {
                Properties prop = new Properties();
                InputStream st = url.openStream();
                try {
                    prop.load(st);
                } finally {
                    st.close();
                }
                _version = prop.getProperty("version");
                _versionBuild = prop.getProperty("version.build");
                _date = prop.getProperty("version.date");
            }
        } catch (IOException e) {
        }
    }
}
