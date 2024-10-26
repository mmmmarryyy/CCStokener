package magoffin.matt.ieat.biz.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import magoffin.matt.ieat.biz.BizContext;
import magoffin.matt.ieat.biz.DomainObjectFactory;
import magoffin.matt.ieat.biz.RecipeIOBiz;
import magoffin.matt.ieat.domain.Recipe;
import magoffin.matt.ieat.domain.Ui;
import magoffin.matt.ieat.domain.UiData;
import magoffin.matt.ieat.domain.User;
import magoffin.matt.ieat.util.EmailRecipeCommand;
import magoffin.matt.ieat.util.IgnoreValidation;
import magoffin.matt.ieat.util.MailMergeSupport;
import magoffin.matt.util.StringUtil;
import magoffin.matt.xweb.XData;
import magoffin.matt.xweb.XwebMessage;
import magoffin.matt.xweb.XwebMessages;
import magoffin.matt.xweb.XwebModel;
import magoffin.matt.xweb.impl.XDataImpl;
import magoffin.matt.xweb.impl.XwebMessageImpl;
import magoffin.matt.xweb.impl.XwebMessagesImpl;
import magoffin.matt.xweb.impl.XwebModelImpl;
import magoffin.matt.xweb.util.MessagesSource;
import org.apache.commons.collections.FastHashMap;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContextException;
import org.springframework.mail.SimpleMailMessage;

/**
 * Implementation of the RecipeIOBiz business interface.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl>
 *   <dt>domainObjectFactory</dt>
 *   <dd>The {@link magoffin.matt.ieat.biz.DomainObjectFactory} instance for 
 *   creating new domain objects.</dd>
 * 
 *   <dt>messagesSource</dt>
 *   <dd>The {@link magoffin.matt.xweb.util.MessagesSource} object for using 
 *   resource messages.</dd>
 * 
 *   <dt>recipeMl2iEatStylesheet</dt>
 *   <dd>The classpath-relative path to the XSLT stylesheet for transforming a 
 *   RecipeML XML document to the iEat Recipe format.</dd>
 * 
 *   <dt>ieat2RecipeMlStylesheet</dt>
 *   <dd>The classpath-relative path to the XSLT stylesheet for transforming an
 *   iEat Recipe into the RecipeML XML format.</dd>
 * 
 *   <dt>ieat2TextStylesheet</dt>
 *   <dd>The classpath-relative path to the XSLT stylesheet for transforming an 
 *   iEat Recipe into a plain text format.</dd>
 * 
 *   <dt>cacheXslt</dt>
 *   <dd>If <em>true</em> then will parse the XSLT templates only once. If 
 *   <em>false</em> then will parse the XSLT templates each time they are 
 *   needed. The latter is not thread-safe and only useful for debugging.
 *   Defaults to <em>true</em>.</dd>
 * 
 *   <dt>jaxbContext</dt>
 *   <dd>The JAXB context to use. Defaults to 
 *   <code>magoffin.matt.ieat.domain</code>.</dd>
 * 
 *   <dt>emailRecipeMailMergeSupport</dt>
 *   <dd>The {@link magoffin.matt.ieat.util.MailMergeSupport} instance to use 
 *   for sending recipe emails in the {@link #emailRecipe(EmailRecipeCommand, BizContext)}
 *   method.</dd>
 * </dl>
 * 
 * @author Matt Magoffin (spamsqr@msqr.us)
 * @version $Revision: 23 $ $Date: 2009-05-03 18:59:25 -0400 (Sun, 03 May 2009) $
 */
public class RecipeIOBizImpl implements RecipeIOBiz {

    /** The RecipeML export format. */
    public static final String EXPORT_FORMAT_RECIPEML = "RecipeML";

    /** The plain text export format. */
    public static final String EXPORT_FORMAT_TEXT = "text";

    /** The supported export format values. */
    public static final String[] SUPPORTED_EXPORT_FORMATS = new String[] { EXPORT_FORMAT_RECIPEML, EXPORT_FORMAT_TEXT };

    private String recipeMl2ieatStylesheet = null;

    private String ieat2RecipeMlStylesheet = null;

    private String ieat2TextStylesheet = null;

    private String jaxbContext = "magoffin.matt.ieat.domain";

    private DomainObjectFactory domainObjectFactory = null;

    private MessagesSource messagesSource = null;

    private boolean cacheXslt = true;

    private MailMergeSupport emailRecipeMailMergeSupport = null;

    private static final Logger LOG = Logger.getLogger(RecipeIOBizImpl.class);

    private static final String RECIPEML_TO_IEAT = "r2i";

    private static final String IEAT_TO_RECIPEML = "i2r";

    private static final String IEAT_TO_TEXT = "i2t";

    private TransformerFactory transformerFactory = null;

    private Map<String, Templates> templatesMap = null;

    private Map<String, String> stylesheetMap = null;

    private JAXBContext context = null;

    @SuppressWarnings("unchecked")
    private Map<String, XwebMessages> msgMap = new FastHashMap();

    /**
	 * Instance initialization method.
	 * 
	 * <p>This method must be called after all fields have been configured
	 * and before any {@link RecipeIOBiz} methods are called.</p>
	 */
    public void init() {
        if (this.domainObjectFactory == null) {
            throw new RuntimeException("The domainObjectFactory property is not set");
        }
        if (this.messagesSource == null) {
            throw new RuntimeException("The messagesSource property is not set");
        }
        if (this.emailRecipeMailMergeSupport == null) {
            throw new RuntimeException("The emailRecipeMailMergeSupport property is not set");
        }
        this.transformerFactory = TransformerFactory.newInstance();
        this.stylesheetMap = new HashMap<String, String>();
        if (this.recipeMl2ieatStylesheet == null) {
            throw new RuntimeException("The recipeML2iEatStylesheet property is not set");
        }
        this.stylesheetMap.put(RECIPEML_TO_IEAT, this.recipeMl2ieatStylesheet);
        getTemplates(RECIPEML_TO_IEAT);
        if (this.ieat2RecipeMlStylesheet == null) {
            throw new RuntimeException("The iEat2RecipeMlStylesheet property is not set");
        }
        this.stylesheetMap.put(IEAT_TO_RECIPEML, this.ieat2RecipeMlStylesheet);
        getTemplates(IEAT_TO_RECIPEML);
        if (this.ieat2TextStylesheet == null) {
            throw new RuntimeException("The iEat2TextStylesheet property is not set");
        }
        this.stylesheetMap.put(IEAT_TO_TEXT, this.ieat2TextStylesheet);
        getTemplates(IEAT_TO_TEXT);
        try {
            context = JAXBContext.newInstance("magoffin.matt.xweb:" + jaxbContext);
        } catch (JAXBException e) {
            throw new RuntimeException("Can't get JAXBContext from [" + this.jaxbContext + "]", e);
        }
    }

    private Templates getTemplates(String key) {
        Templates templates = null;
        try {
            if (cacheXslt) {
                if (templatesMap == null) {
                    templatesMap = new LinkedHashMap<String, Templates>();
                }
                if (!templatesMap.containsKey(key)) {
                    templates = this.transformerFactory.newTemplates(getStylesheetSource(this.stylesheetMap.get(key)));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Loaded templates [" + templates + "] for XSLT " + this.stylesheetMap.get(key));
                    }
                } else {
                    templates = templatesMap.get(key);
                }
            } else {
                templates = this.transformerFactory.newTemplates(getStylesheetSource(this.stylesheetMap.get(key)));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Loaded templates [" + templates + "] for XSLT " + this.stylesheetMap.get(key));
                }
            }
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Can't load stylesheet from " + this.stylesheetMap.get(key), e);
        }
        return templates;
    }

    private Source getStylesheetSource(String stylesheetResource) throws ApplicationContextException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading XSLT stylesheet from " + stylesheetResource);
        }
        try {
            URL url = this.getClass().getClassLoader().getResource(stylesheetResource);
            String urlPath = url.toString();
            String systemId = urlPath.substring(0, urlPath.lastIndexOf('/') + 1);
            return new StreamSource(url.openStream(), systemId);
        } catch (IOException e) {
            throw new RuntimeException("Can't load XSLT stylesheet from " + stylesheetResource, e);
        }
    }

    private Unmarshaller getUnmarshaller() {
        try {
            Unmarshaller u = context.createUnmarshaller();
            u.setEventHandler(new IgnoreValidation());
            u.setValidating(false);
            return u;
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to get JAXB Unmarshaller for JAXB context '" + jaxbContext + "'", e);
        }
    }

    private Marshaller getMarshaller() {
        try {
            Marshaller m = context.createMarshaller();
            m.setEventHandler(new IgnoreValidation());
            m.setEventHandler(IgnoreValidation.IGNORE_VALIDATION);
            return m;
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to get JAXB Unmarshaller for JAXB context '" + jaxbContext + "'", e);
        }
    }

    private Object getJAXBObjectFromTransform(Templates t, InputStream xmlIn) {
        try {
            Transformer xform = t.newTransformer();
            JAXBResult result = new JAXBResult(getUnmarshaller());
            xform.transform(new StreamSource(xmlIn), result);
            return result.getResult();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Unable to obtain transformer", e);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to transform via JAXB", e);
        } catch (TransformerException e) {
            throw new RuntimeException("Unable to transform", e);
        }
    }

    private void outputTransform(Object jaxbObject, Templates templates, OutputStream out) {
        try {
            Result result = new StreamResult(out);
            Source source = new JAXBSource(getMarshaller(), jaxbObject);
            Transformer t = templates.newTransformer();
            t.transform(source, result);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Unable to obtain transformer", e);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to transform via JAXB", e);
        } catch (TransformerException e) {
            throw new RuntimeException("Unable to transform", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void exportXDataTransform(Recipe recipe, String templatesKey, Locale locale, OutputStream out) {
        UiData data = domainObjectFactory.getUiDataInstance();
        data.getRecipe().add(recipe);
        XData xData = getXData(locale);
        XwebModel xModel = new XwebModelImpl();
        xModel.setAny(data);
        xData.setXModel(xModel);
        outputTransform(xData, getTemplates(templatesKey), out);
    }

    private XData getXData(Locale locale) {
        XData xData = new XDataImpl();
        xData.setXMsg(getMessages(locale));
        return xData;
    }

    @SuppressWarnings("unchecked")
    private XwebMessages getMessages(Locale locale) {
        String key = locale.toString();
        if (msgMap.containsKey(key)) {
            return msgMap.get(key);
        }
        synchronized (msgMap) {
            if (msgMap.containsKey(key)) {
                return msgMap.get(key);
            }
            XwebMessages xMsgs = new XwebMessagesImpl();
            Enumeration enumeration = messagesSource.getKeys(locale);
            while (enumeration.hasMoreElements()) {
                String msgKey = (String) enumeration.nextElement();
                XwebMessage xMsg = new XwebMessageImpl();
                xMsgs.getMsg().add(xMsg);
                xMsg.setKey(msgKey);
                Object val = messagesSource.getMessage(msgKey, null, locale);
                if (val != null) {
                    xMsg.setValue(val.toString());
                }
            }
            msgMap.put(key, xMsgs);
            return xMsgs;
        }
    }

    public Recipe createFromRecipeML(InputStream in) {
        Recipe recipe = null;
        Object o = getJAXBObjectFromTransform(getTemplates(RECIPEML_TO_IEAT), in);
        if (!(o instanceof Ui) || ((Ui) o).getRecipe().size() < 1) {
            throw new RuntimeException("Unable to parse Recipe from XML input: " + o);
        }
        recipe = (Recipe) ((Ui) o).getRecipe().get(0);
        return recipe;
    }

    public void exportRecipe(Recipe recipe, Locale locale, OutputStream out, String format) {
        if (EXPORT_FORMAT_RECIPEML.equals(format)) {
            exportXDataTransform(recipe, IEAT_TO_RECIPEML, locale, out);
        } else if (EXPORT_FORMAT_TEXT.equals(format)) {
            exportXDataTransform(recipe, IEAT_TO_TEXT, locale, out);
        } else {
            throw new IllegalArgumentException("The format '" + format + "' is not supported.");
        }
    }

    public String getFileNameForExport(Recipe recipe, String format) {
        if (SUPPORTED_EXPORT_FORMATS[0].equals(format)) {
            return recipe.getName() + ".xml";
        } else if (SUPPORTED_EXPORT_FORMATS[1].equals(format)) {
            return recipe.getName() + ".txt";
        } else {
            throw new IllegalArgumentException("The format '" + format + "' is not supported.");
        }
    }

    public String getMimeForExport(Recipe recipe, String format) {
        if (SUPPORTED_EXPORT_FORMATS[0].equals(format)) {
            return "text/xml";
        } else if (SUPPORTED_EXPORT_FORMATS[1].equals(format)) {
            return "text/plain";
        } else {
            throw new IllegalArgumentException("The format '" + format + "' is not supported.");
        }
    }

    public String[] getSupportedExportFormats() {
        return SUPPORTED_EXPORT_FORMATS;
    }

    public void emailRecipe(EmailRecipeCommand emailCommand, BizContext bizContext) {
        User actingUser = bizContext.getActingUser();
        Recipe recipe = emailCommand.getRecipe();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Locale locale = Locale.getDefault();
        if (actingUser.getLanguage() != null && actingUser.getCountry() != null) {
            locale = new Locale(actingUser.getLanguage(), actingUser.getCountry());
        }
        exportRecipe(recipe, locale, out, EXPORT_FORMAT_TEXT);
        Map<String, Object> model = new LinkedHashMap<String, Object>();
        model.put("user", actingUser);
        model.put("recipe", out.toString());
        String message = emailCommand.getMessage();
        if (StringUtil.trimToNull(message) != null) {
            message = "\n-----\n" + message + "\n-----\n";
        } else {
            message = "";
        }
        model.put("message", message);
        SimpleMailMessage msg = new SimpleMailMessage(emailRecipeMailMergeSupport.getMessageTemplate());
        msg.setTo(emailCommand.getTo());
        msg.setReplyTo(actingUser.getEmail());
        emailRecipeMailMergeSupport.sendMerge(locale, getClass().getClassLoader(), model, msg);
    }

    /**
	 * @return the cacheXslt
	 */
    public boolean isCacheXslt() {
        return cacheXslt;
    }

    /**
	 * @param cacheXslt the cacheXslt to set
	 */
    public void setCacheXslt(boolean cacheXslt) {
        this.cacheXslt = cacheXslt;
    }

    /**
	 * @return the domainObjectFactory
	 */
    public DomainObjectFactory getDomainObjectFactory() {
        return domainObjectFactory;
    }

    /**
	 * @param domainObjectFactory the domainObjectFactory to set
	 */
    public void setDomainObjectFactory(DomainObjectFactory domainObjectFactory) {
        this.domainObjectFactory = domainObjectFactory;
    }

    /**
	 * @return the emailRecipeMailMergeSupport
	 */
    public MailMergeSupport getEmailRecipeMailMergeSupport() {
        return emailRecipeMailMergeSupport;
    }

    /**
	 * @param emailRecipeMailMergeSupport the emailRecipeMailMergeSupport to set
	 */
    public void setEmailRecipeMailMergeSupport(MailMergeSupport emailRecipeMailMergeSupport) {
        this.emailRecipeMailMergeSupport = emailRecipeMailMergeSupport;
    }

    /**
	 * @return the ieat2RecipeMlStylesheet
	 */
    public String getIeat2RecipeMlStylesheet() {
        return ieat2RecipeMlStylesheet;
    }

    /**
	 * @param ieat2RecipeMlStylesheet the ieat2RecipeMlStylesheet to set
	 */
    public void setIeat2RecipeMlStylesheet(String ieat2RecipeMlStylesheet) {
        this.ieat2RecipeMlStylesheet = ieat2RecipeMlStylesheet;
    }

    /**
	 * @return the ieat2TextStylesheet
	 */
    public String getIeat2TextStylesheet() {
        return ieat2TextStylesheet;
    }

    /**
	 * @param ieat2TextStylesheet the ieat2TextStylesheet to set
	 */
    public void setIeat2TextStylesheet(String ieat2TextStylesheet) {
        this.ieat2TextStylesheet = ieat2TextStylesheet;
    }

    /**
	 * @return the jaxbContext
	 */
    public String getJaxbContext() {
        return jaxbContext;
    }

    /**
	 * @param jaxbContext the jaxbContext to set
	 */
    public void setJaxbContext(String jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    /**
	 * @return the messagesSource
	 */
    public MessagesSource getMessagesSource() {
        return messagesSource;
    }

    /**
	 * @param messagesSource the messagesSource to set
	 */
    public void setMessagesSource(MessagesSource messagesSource) {
        this.messagesSource = messagesSource;
    }

    /**
	 * @return the recipeMl2ieatStylesheet
	 */
    public String getRecipeMl2ieatStylesheet() {
        return recipeMl2ieatStylesheet;
    }

    /**
	 * @param recipeMl2ieatStylesheet the recipeMl2ieatStylesheet to set
	 */
    public void setRecipeMl2ieatStylesheet(String recipeMl2ieatStylesheet) {
        this.recipeMl2ieatStylesheet = recipeMl2ieatStylesheet;
    }
}
