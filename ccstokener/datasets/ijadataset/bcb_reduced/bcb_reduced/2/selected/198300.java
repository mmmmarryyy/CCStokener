package com.frameworkset.common.tag.export.properties;

import java.io.IOException;
import java.net.URL;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

/**
 * 
 * @author biaoping.yin
 * created on 2005-5-14 
 * version 1.0
 */
public class ConfigFactory {

    private static Configture configture;

    /**
	 * �Ƿ�ǿ�ƴ�xml�ж�ȡ��������Ϣ
	 * @param force
	 * @return Configture
	 */
    public static Configture createConfigure(boolean force) {
        Digester digester = new Digester();
        digester.setValidating(false);
        if (configture == null || force) {
            configture = new Configture();
            digester.push(configture);
            digester.addBeanPropertySetter("exportconfig/description", "description");
            digester.addBeanPropertySetter("exportconfig/version", "version");
            digester.addObjectCreate("exportconfig/export", ExportConfig.class);
            digester.addBeanPropertySetter("exportconfig/export/description", "description");
            digester.addSetProperties("exportconfig/export/config", new String[] { "name", "type", "class" }, new String[] { "name", "type", "exportClass" });
            digester.addSetNext("exportconfig/export", "addExportConfig");
            ClassLoader classLoader = ConfigFactory.class.getClassLoader();
            URL url = classLoader.getResource("export.xml");
            try {
                digester.parse(url.openStream());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
        return configture;
    }

    public static void main(String[] args) {
        System.out.println(createConfigure(false));
    }
}
