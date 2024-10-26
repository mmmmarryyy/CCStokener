package org.j2eespider.ide.data.rules;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import org.j2eespider.ide.data.domain.ConfigSpider;
import org.j2eespider.ide.data.domain.Crud;
import org.j2eespider.ide.data.domain.CrudClass;
import org.j2eespider.ide.data.domain.CrudField;
import org.j2eespider.ide.data.domain.CrudFieldValidator;
import org.j2eespider.ide.data.domain.FieldLocation;
import org.j2eespider.ide.data.domain.HtmlType;
import org.j2eespider.ide.data.domain.LayoutColor;
import org.j2eespider.ide.data.domain.PluginRequired;
import org.j2eespider.ide.data.domain.Repository;
import org.j2eespider.ide.data.domain.RepositoryLink;
import org.j2eespider.ide.data.domain.RepositoryTemplateInfo;
import org.j2eespider.ide.data.domain.SimpleSiteLayout;
import org.j2eespider.ide.data.domain.SiteLayout;
import org.j2eespider.ide.data.domain.Template;
import org.j2eespider.ide.data.domain.TemplateCommand;
import org.j2eespider.ide.data.domain.TemplateFile;
import org.j2eespider.ide.data.domain.TemplateFileIncrement;
import org.j2eespider.ide.data.domain.ValidatorType;
import org.j2eespider.util.BuildManagerUtil;
import org.j2eespider.util.xstream.converters.IncrementConverter;
import org.j2eespider.util.xstream.converters.TemplateFileMapConverter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;

public class XmlManager {

    public static void writeXml(Object data, String pathArquivo, XML_TYPE xmlType) {
        XStream xstream = getXStream(xmlType);
        String xml = xstream.toXML(data);
        File spiderFile = new File(pathArquivo);
        try {
            spiderFile.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(spiderFile);
            fileOutputStream.write(xml.getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static Object loadXml(String pathFile, XML_TYPE xmlType) {
        XStream xstream = getXStream(xmlType);
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(pathFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return xstream.fromXML(fileReader);
    }

    public static Object loadXml(InputStream inputStream, XML_TYPE xmlType) {
        XStream xstream = getXStream(xmlType);
        return xstream.fromXML(inputStream);
    }

    public static Object loadXmlFromString(String xmlContent, XML_TYPE xmlType) {
        XStream xstream = getXStream(xmlType);
        return xstream.fromXML(xmlContent);
    }

    public static Object loadXmlFromUrl(URL url, int timeout, XML_TYPE xmlType) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        BufferedInputStream buffInputStream = new BufferedInputStream(connection.getInputStream());
        return loadXml(buffInputStream, xmlType);
    }

    /**
	 * Load XML of TemplateCode 
	 * @param templateFolder (XML_TYPE.TEMPLATE_FILES or XML_TYPE.TEMPLATE_FILES_JAR)
	 * @param xmlType
	 * @return
	 * @throws IOException
	 */
    public static Object loadTemplateCodeXml(String templateFolder, XML_TYPE xmlType) throws IOException {
        String pathDatabaseTypeConfig = BuildManagerUtil.getPathFolderTemplates() + File.separator + templateFolder + File.separator + xmlType.getFileName();
        if (xmlType == XML_TYPE.TEMPLATE_FILES_JAR && new File(pathDatabaseTypeConfig).exists() == false) {
            return null;
        }
        return loadXml(pathDatabaseTypeConfig, xmlType);
    }

    /**
	 * Instance and configure XStream
	 * @return
	 */
    private static XStream getXStream(XML_TYPE xmlType) {
        XStream xstream = new XStream();
        if (xmlType == XML_TYPE.TEMPLATE_FILES_JAR || xmlType == XML_TYPE.TEMPLATE_FILES) {
            xstream.useAttributeFor("entryDepends", String.class);
            xstream.registerConverter(new TemplateFileMapConverter(xstream.getMapper()));
            if (xmlType == XML_TYPE.TEMPLATE_FILES) {
                xstream.registerConverter(new IncrementConverter());
            }
        } else if (xmlType == XML_TYPE.TEMPLATE) {
            xstream.registerConverter(new TemplateFileMapConverter(xstream.getMapper()));
            xstream.addImplicitCollection(ArrayList.class, "list");
        } else if (xmlType == XML_TYPE.REPOSITORY || xmlType == XML_TYPE.REPOSITORY_TEMPLATE_INFO) {
            xstream.addImplicitCollection(ArrayList.class, "list");
        } else if (xmlType == XML_TYPE.TEMPLATE_COMMAND) {
            xstream.registerConverter(new TemplateFileMapConverter(xstream.getMapper()));
        }
        Annotations.configureAliases(xstream, ConfigSpider.class);
        Annotations.configureAliases(xstream, LayoutColor.class);
        Annotations.configureAliases(xstream, SimpleSiteLayout.class);
        Annotations.configureAliases(xstream, SiteLayout.class);
        Annotations.configureAliases(xstream, Template.class);
        Annotations.configureAliases(xstream, TemplateFile.class);
        Annotations.configureAliases(xstream, TemplateFileIncrement.class);
        Annotations.configureAliases(xstream, TemplateCommand.class);
        Annotations.configureAliases(xstream, Crud.class);
        Annotations.configureAliases(xstream, CrudClass.class);
        Annotations.configureAliases(xstream, CrudField.class);
        Annotations.configureAliases(xstream, CrudFieldValidator.class);
        Annotations.configureAliases(xstream, HtmlType.class);
        Annotations.configureAliases(xstream, ValidatorType.class);
        Annotations.configureAliases(xstream, PluginRequired.class);
        Annotations.configureAliases(xstream, RepositoryTemplateInfo.class);
        Annotations.configureAliases(xstream, Repository.class);
        Annotations.configureAliases(xstream, RepositoryLink.class);
        Annotations.configureAliases(xstream, FieldLocation.class);
        xstream.setMode(XStream.NO_REFERENCES);
        return xstream;
    }

    public enum XML_TYPE {

        CONFIG_SPIDER(".spider"), TEMPLATE("template.xml"), TEMPLATE_FILES("templateFiles.xml"), TEMPLATE_FILES_JAR("templateFiles-jar.xml"), TEMPLATE_COMMAND(""), DATABASE_TYPE_CONFIG("databaseTypeConfig.xml"), SPIDER_BUILD(".spiderBuild"), VALIDATOR_TYPE("validatorConfig.xml"), HTML_TYPE("fieldConfig.xml"), FIELD_LOCATION("fieldLocation.xml"), TEMPLATE_LIST("templateList.xml"), REPOSITORY("repo.xml"), REPOSITORY_TEMPLATE_INFO("");

        private String fileName;

        private XML_TYPE(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
