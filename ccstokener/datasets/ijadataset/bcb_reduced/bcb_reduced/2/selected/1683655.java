package org.sf.codejen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.substitution.MultiVariableExpander;
import org.apache.commons.digester.substitution.VariableSubstitutor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Parser to parse the codegen config.
 * @author Shane Ng
 *
 */
public class TemplateConfigurationParser extends AbstractObjectCreationFactory {

    private static final TemplateConfigurationParser INSTANCE = new TemplateConfigurationParser();

    private final MultiVariableExpander varExpander = new MultiVariableExpander();

    public static TemplateConfigurationParser getInstance() {
        return INSTANCE;
    }

    @Override
    public Object createObject(Attributes attrs) throws Exception {
        String resource = attrs.getValue("file");
        if (resource != null) {
            return INSTANCE.parseFile(resource);
        } else if ((resource = attrs.getValue("url")) != null) {
            return INSTANCE.parseUrl(resource);
        } else if ((resource = attrs.getValue("resource")) == null) {
            return INSTANCE.parseResource(resource);
        } else {
            return null;
        }
    }

    /**
	 * Parses the resource from class loader. 
	 * @param resourceString resource string to be parsed.
	 * @return the codegen config.
	 * @throws IOException when error is encounter in file reading
	 * @throws SAXException when error is encounter in config parsing
	 */
    public TemplateConfiguration parseResource(String resourceString) throws IOException, SAXException {
        InputStream in = null;
        try {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceString);
            return this.parse(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
	 * Parses the file in specified URL. 
	 * @param urlString URL string to be parsed.
	 * @return the codegen config.
	 * @throws IOException when error is encounter in file reading
	 * @throws SAXException when error is encounter in config parsing
	 */
    public TemplateConfiguration parseUrl(String urlString) throws IOException, SAXException {
        return parseUrl(new URL(urlString));
    }

    /**
	 * Parses the file in specified URL. 
	 * @param url URL instance to be parsed.
	 * @return the codegen config.
	 * @throws IOException when error is encounter in file reading
	 * @throws SAXException when error is encounter in config parsing
	 */
    public TemplateConfiguration parseUrl(URL url) throws IOException, SAXException {
        InputStream in = null;
        try {
            in = url.openStream();
            return this.parse(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
	 * Parses the file in specified file name. 
	 * @param fileName codegen config file to be parsed.
	 * @return the codegen config.
	 * @throws IOException when error is encounter in file reading
	 * @throws SAXException when error is encounter in config parsing
	 */
    public TemplateConfiguration parseFile(String fileName) throws IOException, SAXException {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                return this.parse(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        return null;
    }

    /**
	 * The main method to do the parsing.
	 * <code>class</code> attribute should be specified in 
	 * <code>config/template/postProcessor</code> and
	 * <code>config/template/modelExtractor</code>.
	 * <code>config/template/fileNameGenerator</code>.
	 * <p>
	 * Attributes in <code>config/include</code> will be evaluated in this order:<ol>
	 * <li><code>file</code></li>
	 * <li><code>url</code></li>
	 * <li><code>resource</code></li>
	 * </ol>
	 * 
	 * @param in input stream to be parsed.
	 * @return the codegen config.
	 * @throws IOException when error is encounter in file reading
	 * @throws SAXException when error is encounter in config parsing
	 */
    public TemplateConfiguration parse(InputStream in) throws IOException, SAXException {
        TemplateConfiguration config = new TemplateConfiguration(varExpander);
        Digester digester = new Digester();
        digester.setSubstitutor(new VariableSubstitutor(varExpander));
        digester.push(config);
        digester.addCallMethod("config/property", "addProperty", 2);
        digester.addCallParam("config/property", 0, "key");
        digester.addCallParam("config/property", 1, "value");
        digester.addFactoryCreate("config/include", "org.sf.codejen.TemplateConfigurationParser");
        digester.addSetNext("config/include", "addAll", "java.util.Collection");
        digester.addObjectCreate("config/template", null, "class");
        digester.addSetProperties("config/template");
        digester.addSetNext("config/template", "add", "java.lang.Object");
        digester.addFactoryCreate("config/template/postProcessor", "org.sf.codejen.CodejenObjectFactory");
        digester.addSetNext("config/template/postProcessor", "addPostProcessor", "org.sf.codejen.TemplateProcessor");
        digester.addFactoryCreate("config/template/modelExtractor", "org.sf.codejen.CodejenObjectFactory");
        digester.addSetNext("config/template/modelExtractor", "setModelExtractor", "org.sf.codejen.ModelExtractor");
        digester.addFactoryCreate("config/template/fileNameGenerator", "org.sf.codejen.CodejenObjectFactory");
        digester.addSetNext("config/template/fileNameGenerator", "setFileNameGenerator", "org.sf.codejen.FileNameGenerator");
        digester.parse(in);
        return config;
    }
}
