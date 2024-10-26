package net.sf.crudelia.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import net.sf.crudelia.ActionResult;
import net.sf.crudelia.CrudeliaException;
import net.sf.crudelia.IScopeHandler;
import net.sf.crudelia.Store;
import net.sf.crudelia.cfg.tasks.BaseProperty;
import net.sf.crudelia.cfg.tasks.Issue;
import net.sf.crudelia.cfg.tasks.commons.CompositeId;
import net.sf.crudelia.cfg.tasks.commons.Fetch;
import net.sf.crudelia.cfg.tasks.commons.Key;
import net.sf.crudelia.cfg.tasks.commons.OrderProperties;
import net.sf.crudelia.cfg.tasks.commons.SurrogateId;
import net.sf.crudelia.cfg.tasks.delete.Delete;
import net.sf.crudelia.cfg.tasks.delete.DeleteByID;
import net.sf.crudelia.cfg.tasks.delete.DeleteByQuery;
import net.sf.crudelia.cfg.tasks.find.CopyProperty;
import net.sf.crudelia.cfg.tasks.find.CopyToBean;
import net.sf.crudelia.cfg.tasks.find.Find;
import net.sf.crudelia.cfg.tasks.find.FindByID;
import net.sf.crudelia.cfg.tasks.find.FindByQuery;
import net.sf.crudelia.cfg.tasks.list.Limit;
import net.sf.crudelia.cfg.tasks.list.PageList;
import net.sf.crudelia.cfg.tasks.saveorupdate.SaveOrUpdate;
import net.sf.hibernate.Session;
import org.apache.commons.digester.Digester;
import org.xml.sax.InputSource;

/**
 */
public class Config {

    protected static final String CONFIG_TAG = "crudelia-config";

    protected static final String ACTION_TAG = "crudelia-action";

    protected static final String FIND_TAG = "find";

    protected static final String BYID_TAG = "by-id";

    protected static final String BYCOMPOSITEID_TAG = "by-composite-id";

    protected static final String BYQUERY_TAG = "by-query";

    protected static final String NAMEDPAR_TAG = "named-par";

    protected static final String JDBCPAR_TAG = "jdbc-par";

    protected static final String HQL_TAG = "hql";

    protected static final String FETCH_TAG = "fetch";

    protected static final String LIST_TAG = "list";

    protected static final String DELETE_TAG = "delete";

    protected static final String SAVEORUPDATE_TAG = "save-or-update";

    protected static final String BYALL_TAG = "all";

    protected static final String PARENT_TAG = "parent";

    protected static final String PAGELIST_TAG = "page-list";

    protected static final String ORDER_TAG = "order";

    protected static final String ORDERBY_TAG = "order-by";

    protected static final String ORDERTYPE_TAG = "order-type";

    protected static final String TOTALROWS_TAG = "total-rows";

    protected static final String ROWSPERPAGE_TAG = "rows-per-page";

    protected static final String PAGENUMBER_TAG = "page-number";

    protected static final String LIMIT_TAG = "limit";

    protected static final String MAXRESULTS_TAG = "max-results";

    protected static final String FIRSTRESULT_TAG = "first-result";

    protected static final String ONISSUE_TAG = "on-issue";

    protected static final String STORE_TAG = "store";

    protected static final String COMPOSITE_PARENT_TAG = "composite-parent";

    protected static final String PROPERTY_TAG = "property";

    protected static final String CHECKFIELD_TAG = "check-field";

    protected static final String KEY_TAG = "key";

    protected static final String METHOD_TAG = "method";

    protected static final String SESSION_TAG = "session";

    protected static final String SESSION_FACTORY_TAG = "session-factory";

    protected static final String COPY_TO_BEAN_TAG = "copy-to-bean";

    protected static final String COPY_PROPERTY_TAG = "copy-property";

    Map crudeliaActions;

    Digester digester;

    SessionHandler sessionHandler;

    protected String registrations[] = { "-//Crudelia//Crudelia Configuration DTD 1.0//EN", "/net/sf/crudelia/resources/crudelia-configuration-1.0.dtd" };

    protected InputStream getConfigurationInputStream(String resource) throws CrudeliaException {
        InputStream stream = Config.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new CrudeliaException(resource + " not found");
        }
        return stream;
    }

    /**
	* Use the mappings and properties specified in an application
	* resource named <tt>crudelia.cfg.xml</tt>.
	*/
    public void configure() throws CrudeliaException {
        configure("/crudelia.cfg.xml");
    }

    /**
	 * Use the mappings and properties specified in the given application
	 * resource. The format of the resource is defined in
	 * <tt>hibernate-configuration-2.0.dtd</tt>.
	 *
	 * The resource is found via <tt>getConfigurationInputStream(resource)</tt>.
	 */
    public void configure(String resource) throws CrudeliaException {
        InputStream stream = null;
        try {
            stream = getConfigurationInputStream(resource);
            createConfig(stream, resource);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    public void configure(String path, ServletContext context) throws CrudeliaException {
        InputStream input = null;
        try {
            URL url = context.getResource(path);
            input = context.getResourceAsStream(path);
            configure(input, url.toExternalForm());
        } catch (MalformedURLException e) {
            throw new CrudeliaException(e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * Use the mappings and properties specified in the given document.
	 * The format of the document is defined in
	 * <tt>hibernate-configuration-2.0.dtd</tt>.
	 *
	 * @param url URL from which you wish to load the configuration
	 * @return A configuration configured via the file
	 * @throws HibernateException
	 */
    public void configure(URL url) throws CrudeliaException {
        InputStream stream = null;
        try {
            stream = url.openStream();
            createConfig(url.openStream(), url.toString());
        } catch (IOException ioe) {
            throw new CrudeliaException("could not configure from URL: " + url, ioe);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * Use the mappings and properties specified in the given application
	 * file. The format of the file is defined in
	 * <tt>hibernate-configuration-2.0.dtd</tt>.
	 *
	 * @param configFile <tt>File</tt> from which you wish to load the configuration
	 * @return A configuration configured via the file
	 * @throws HibernateException
	 */
    public void configure(File configFile) throws CrudeliaException {
        InputStream stream = null;
        try {
            stream = new FileInputStream(configFile);
            createConfig(stream, configFile.toString());
        } catch (FileNotFoundException fnfe) {
            throw new CrudeliaException("could not find file: " + configFile, fnfe);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    public void configure(InputStream stream, String resourceName) throws CrudeliaException {
        createConfig(stream, resourceName);
    }

    protected void createConfig(InputStream stream, String resourceName) throws CrudeliaException {
        try {
            InputSource resName = new InputSource(resourceName);
            resName.setByteStream(stream);
            crudeliaActions = new HashMap();
            digester = new Digester();
            digester.setValidating(true);
            digester.setNamespaceAware(true);
            digester.setUseContextClassLoader(true);
            for (int i = 0; i < registrations.length; i += 2) {
                URL url = this.getClass().getResource(registrations[i + 1]);
                if (url != null) {
                    digester.register(registrations[i], url.toString());
                }
            }
            sessionRules();
            actionsRules();
            findRules();
            deleteRules();
            listRules();
            saveOrUpdateRules();
            digester.push(this);
            digester.parse(resName);
        } catch (Exception e) {
            throw new CrudeliaException(e);
        }
    }

    protected void sessionRules() {
        String tag = CONFIG_TAG + "/" + SESSION_FACTORY_TAG;
        digester.addObjectCreate(tag, SessionHandler.class);
        digester.addSetProperties(tag, new String[] { "url" }, new String[] { "sessionFactoryUrl" });
        digester.addSetNext(tag, "setSessionHandler");
        tag = CONFIG_TAG + "/" + SESSION_TAG;
        digester.addObjectCreate(tag, SessionHandler.class);
        digester.addSetProperties(CONFIG_TAG + "/" + SESSION_TAG, new String[] { "class", "method", "close" }, new String[] { "sessionHandlerClass", "sessionHandlerMethod", "close" });
        digester.addSetNext(tag, "setSessionHandler");
    }

    protected void actionsRules() {
        String tag = CONFIG_TAG + "/" + ACTION_TAG;
        digester.addObjectCreate(tag, CrudeliaAction.class);
        digester.addSetProperties(tag, new String[] { "error-fw", "error-msg", "ok-fw" }, new String[] { "errorFw", "errorMsg", "okFw" });
        digester.addSetNext(tag, "addCrudeliaAction");
    }

    protected void findRules() {
        String tag = CONFIG_TAG + "/" + ACTION_TAG + "/" + FIND_TAG;
        taskRules(tag, Find.class);
        String setMethodName = "setFindBy";
        byIdRules(tag, FindByID.class, setMethodName, BYID_TAG, BYCOMPOSITEID_TAG);
        byQueryRules(tag, FindByQuery.class, setMethodName);
        baseRule(tag + "/" + COPY_TO_BEAN_TAG, CopyToBean.class, "setCopyToBean");
        baseRule(tag + "/" + COPY_TO_BEAN_TAG + "/" + COPY_PROPERTY_TAG, CopyProperty.class, "addCopyProperty");
        digester.addSetProperties(tag + "/" + COPY_TO_BEAN_TAG + "/" + COPY_PROPERTY_TAG, "bean-property", "beanProperty");
        fetchRules(tag);
    }

    protected void byIdRules(String tag, Class classToCreate, String setMethodName, String idTag, String compositeIDTag) {
        String tagByID = tag + "/" + idTag;
        digester.addRule(tagByID, new IdRule(classToCreate, SurrogateId.class));
        digester.addSetProperties(tagByID);
        digester.addSetNext(tagByID, setMethodName);
        digester.addRule(tagByID, new PopOnEndRule());
        String tagByCompositeID = tag + "/" + compositeIDTag;
        digester.addRule(tagByCompositeID, new IdRule(classToCreate, CompositeId.class));
        digester.addSetProperties(tagByCompositeID);
        digester.addSetNext(tagByCompositeID, setMethodName);
        digester.addRule(tagByCompositeID, new PopOnEndRule());
        keyRules(tagByCompositeID);
    }

    protected void baseRule(String tag, Class classToAdd, String setMethod) {
        digester.addObjectCreate(tag, classToAdd);
        digester.addSetProperties(tag);
        digester.addSetNext(tag, setMethod);
    }

    protected void byQueryRules(String tag, Class classToCreate, String setMethodName) {
        String tagByQuery = tag + "/" + BYQUERY_TAG;
        String hqlTag = tagByQuery + "/" + HQL_TAG;
        digester.addObjectCreate(tagByQuery, classToCreate);
        digester.addCallMethod(hqlTag, "setHql", 0);
        String namedParTag = tagByQuery + "/" + NAMEDPAR_TAG;
        digester.addObjectCreate(namedParTag, BaseProperty.class);
        digester.addSetProperties(namedParTag);
        digester.addSetNext(namedParTag, "addNamedPar");
        String jdbcParTag = tagByQuery + "/" + JDBCPAR_TAG;
        digester.addObjectCreate(jdbcParTag, BaseProperty.class);
        digester.addSetProperties(jdbcParTag);
        digester.addSetNext(jdbcParTag, "addJdbcPar");
        digester.addSetNext(tagByQuery, setMethodName);
        orderRules(tagByQuery);
    }

    protected void fetchRules(String tag) {
        String tagFetch = tag + "/" + FETCH_TAG;
        baseRule(tagFetch, Fetch.class, "addFetch");
    }

    protected void deleteRules() {
        String tag = CONFIG_TAG + "/" + ACTION_TAG + "/" + DELETE_TAG;
        taskRules(tag, Delete.class);
        String setMethodName = "setDeleteBy";
        byIdRules(tag, DeleteByID.class, setMethodName, BYID_TAG, BYCOMPOSITEID_TAG);
        byQueryRules(tag, DeleteByQuery.class, setMethodName);
    }

    protected void listRules() {
        String tag = CONFIG_TAG + "/" + ACTION_TAG + "/" + LIST_TAG;
        taskRules(tag, net.sf.crudelia.cfg.tasks.list.List.class);
        byQueryRules(tag, net.sf.crudelia.cfg.tasks.list.ListByQuery.class, "setListBy");
        fetchRules(tag);
        String tagPageList = tag + "/" + PAGELIST_TAG;
        baseRule(tagPageList, PageList.class, "setLimit");
        baseRule(tagPageList + "/" + TOTALROWS_TAG, Store.class, "setTotalRows");
        baseRule(tagPageList + "/" + ROWSPERPAGE_TAG, BaseProperty.class, "setRowsPerPage");
        baseRule(tagPageList + "/" + PAGENUMBER_TAG, Store.class, "setPageNumber");
        String tagLimit = tag + "/" + LIMIT_TAG;
        baseRule(tagLimit, Limit.class, "setLimit");
        baseRule(tagLimit + "/" + MAXRESULTS_TAG, BaseProperty.class, "setMaxResults");
        baseRule(tagLimit + "/" + FIRSTRESULT_TAG, BaseProperty.class, "setFirstResult");
    }

    protected void saveOrUpdateRules() {
        String tag = CONFIG_TAG + "/" + ACTION_TAG + "/" + SAVEORUPDATE_TAG;
        taskRules(tag, SaveOrUpdate.class);
        digester.addSetProperties(tag, new String[] { "copy-from-bean", "load-before-update" }, new String[] { "copyFromBean", "loadBeforeUpdate" });
        baseRule(tag + "/" + PROPERTY_TAG, BaseProperty.class, "addProperty");
        baseRule(tag + "/" + CHECKFIELD_TAG, BaseProperty.class, "addCheckField");
        baseRule(tag + "/" + METHOD_TAG, BaseProperty.class, "setMethod");
    }

    protected void taskRules(String tag, Class objToCreate) {
        baseRule(tag, objToCreate, "addTask");
        digester.addSetProperties(tag, "if-exists", "ifExists");
        baseRule(tag + "/" + ONISSUE_TAG, Issue.class, "setIssue");
        baseRule(tag + "/" + STORE_TAG, Store.class, "setStore");
    }

    protected void keyRules(String tag) {
        String tagKey = tag + "/" + KEY_TAG;
        baseRule(tagKey, Key.class, "addKey");
    }

    protected void orderRules(String tag) {
        String tagOrder = tag + "/" + ORDER_TAG;
        baseRule(tagOrder, OrderProperties.class, "addOrder");
        String tagOrderBy = tagOrder + "/" + ORDERBY_TAG;
        baseRule(tagOrderBy, BaseProperty.class, "setOrderBy");
        String tagOrderType = tagOrder + "/" + ORDERTYPE_TAG;
        baseRule(tagOrderType, BaseProperty.class, "setOrderType");
    }

    public void addCrudeliaAction(CrudeliaAction crudeliaAction) {
        crudeliaActions.put(crudeliaAction.getName(), crudeliaAction);
    }

    public CrudeliaAction getCrudeliaAction(String name) {
        return (CrudeliaAction) crudeliaActions.get(name);
    }

    public Map getCrudeliaActions() {
        return crudeliaActions;
    }

    public void setCrudeliaActions(Map map) {
        crudeliaActions = map;
    }

    public ActionResult executeAction(String actionName, IScopeHandler scopeHandler, Object bean) throws CrudeliaException {
        return executeAction(actionName, scopeHandler, null, bean);
    }

    public ActionResult executeAction(String actionName, IScopeHandler scopeHandler, Session session, Object bean) {
        CrudeliaAction action = (CrudeliaAction) crudeliaActions.get(actionName);
        if (action == null) {
        }
        return action.execute(scopeHandler, sessionHandler, session, bean);
    }

    public SessionHandler getSessionHandler() {
        return sessionHandler;
    }

    public void setSessionHandler(SessionHandler handler) {
        sessionHandler = handler;
    }
}
