package org.utopia.efreet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Types;
import java.util.Hashtable;
import java.util.Iterator;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import org.jdom.input.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.apache.log4j.Logger;

/**
 * This class is the main factory for the DAO creation, it reads and parses
 * the XML files containing the models for different DAOs.
 */
public class DAOFactory {

    private static Hashtable models = null;

    static Logger logger = Logger.getLogger(DAOFactory.class.getName());

    /**
     * Main function to create DAOs <br>
     * If the factory does not find the model for the the name, it will try
     * to read an XML file from the resources path.
     * @param name Name of the DAO
     * @return A new DAO based on the model of the same name
     */
    public static DataAccessObject createDAO(String name) throws EfreetException {
        if (name == null) {
            logger.error("DAO name is not defined");
            throw new EfreetException("DAO name is not defined");
        }
        if (models == null) models = new Hashtable();
        DAOModel dm = (DAOModel) models.get(name);
        if (dm == null) {
            dm = readXML(name);
            models.put(name, dm);
        }
        if (dm == null) {
            logger.error("DAO model is not defined");
            throw new EfreetException("DAO model is not defined");
        }
        DataAccessObject dao = new DataAccessObject();
        dao.setModel(dm);
        return dao;
    }

    /**
     * Recover the URL based on the name of the DAO
     * it searches the current classloader first (for non-web applications)
     * then it searches the context for any specific user variable
     * then it searches in the contextloader 
     * @param name
     * @return
     * @throws EfreetException
     */
    private static URL getURL(String name) throws EfreetException {
        URL url = ClassLoader.getSystemResource(name + ".xml");
        try {
            if (url == null) {
                try {
                    Context initContext = new InitialContext();
                    Context envContext = (Context) initContext.lookup("java:/comp/env");
                    String xmlFileDir = (String) envContext.lookup("xml/efreet");
                    url = new URL("file:" + xmlFileDir + "/" + name + ".xml");
                } catch (NameNotFoundException nnfe) {
                    logger.warn("Name not found on context ");
                } catch (NamingException e) {
                    logger.error("Error retrieving Context : ", e);
                }
            }
            try {
                if (url != null) {
                    url.openConnection();
                }
            } catch (FileNotFoundException fnfe) {
                url = null;
            }
            if (url == null) {
                url = Thread.currentThread().getContextClassLoader().getResource(name + ".xml");
            }
        } catch (IOException ioe) {
            logger.error("Error reading XML file", ioe);
            throw new EfreetException(ioe.getMessage());
        }
        return url;
    }

    /**
     * Read an XML file for the model for one DAO. <br>
     * It'll search the file as a resource. <br>
     * This method may be used to force reading of any changes 
     * to the XML files.
     * @param name Name of the DAO
     * @return a new DAOModel based on the XML
     */
    public static DAOModel readXML(String name) throws EfreetException {
        URL url = getURL(name);
        try {
            SAXBuilder saxb = new SAXBuilder();
            Document jdomtree = saxb.build(url);
            Element dao = jdomtree.getRootElement();
            DAOModel model = new DAOModel();
            model.setDataSource(dao.getAttributeValue("datasource"));
            Iterator queryList = dao.getChildren("query").iterator();
            while (queryList.hasNext()) {
                Object proximo = queryList.next();
                if (proximo instanceof Element) {
                    Element thisQuery = (Element) proximo;
                    Query q = new Query();
                    q.setName(thisQuery.getAttributeValue("name"));
                    String qType = thisQuery.getAttributeValue("type");
                    if (qType != null) {
                        if (qType.equalsIgnoreCase("query")) {
                            q.setType(Query.Q_QUERY);
                        } else if (qType.equalsIgnoreCase("update")) {
                            q.setType(Query.Q_UPDATE);
                        } else if (qType.equalsIgnoreCase("procedure")) {
                            q.setType(Query.Q_PROCEDURE);
                        } else if (qType.equalsIgnoreCase("conditional")) {
                            q.setType(Query.Q_CONDITIONAL);
                        }
                    }
                    q.setStatement(thisQuery.getTextNormalize());
                    Iterator paramList = thisQuery.getChildren("parameter").iterator();
                    while (paramList.hasNext()) {
                        Element thisParam = (Element) paramList.next();
                        ParameterModel pModel = new ParameterModel();
                        pModel.setParamName(thisParam.getTextNormalize());
                        try {
                            String psiz = thisParam.getAttributeValue("size");
                            pModel.setParamSize(Integer.parseInt(psiz));
                        } catch (Exception e) {
                            pModel.setParamSize(0);
                        }
                        String pType = thisParam.getAttributeValue("type");
                        int iType = Types.JAVA_OBJECT;
                        if (pType != null) {
                            if (pType.equalsIgnoreCase("number") || pType.equalsIgnoreCase("numeric")) {
                                iType = Types.NUMERIC;
                            } else if (pType.equalsIgnoreCase("char")) {
                                iType = Types.CHAR;
                            } else if (pType.equalsIgnoreCase("date")) {
                                iType = Types.DATE;
                            } else if (pType.equalsIgnoreCase("time")) {
                                iType = Types.TIME;
                            } else if (pType.equalsIgnoreCase("timestamp")) {
                                iType = Types.TIMESTAMP;
                            } else {
                                iType = Types.JAVA_OBJECT;
                            }
                        }
                        pModel.setParamType(iType);
                        String posParam = thisParam.getAttributeValue("index");
                        if (posParam != null) {
                            try {
                                int posP = Integer.parseInt(posParam);
                                q.addParameterAt(pModel, posP);
                            } catch (Exception e) {
                                logger.warn("Error on XML file parameter ", e);
                            }
                        }
                    }
                    Iterator resultList = thisQuery.getChildren("result").iterator();
                    while (resultList.hasNext()) {
                        Element thisResult = (Element) resultList.next();
                        ResultModel rModel = new ResultModel();
                        rModel.setResultName(thisResult.getTextNormalize());
                        try {
                            String psiz = thisResult.getAttributeValue("size");
                            rModel.setResultSize(Integer.parseInt(psiz));
                        } catch (Exception e) {
                            rModel.setResultSize(0);
                        }
                        String pType = thisResult.getAttributeValue("type");
                        int iType = Types.JAVA_OBJECT;
                        if (pType != null) {
                            if (pType.equalsIgnoreCase("number") || pType.equalsIgnoreCase("numeric")) {
                                iType = Types.NUMERIC;
                            } else if (pType.equalsIgnoreCase("char")) {
                                iType = Types.CHAR;
                            } else if (pType.equalsIgnoreCase("date")) {
                                iType = Types.DATE;
                            } else if (pType.equalsIgnoreCase("time")) {
                                iType = Types.TIME;
                            } else if (pType.equalsIgnoreCase("timestamp")) {
                                iType = Types.TIMESTAMP;
                            } else {
                                iType = Types.JAVA_OBJECT;
                            }
                        }
                        rModel.setResultType(iType);
                        String posResult = thisResult.getAttributeValue("index");
                        if (posResult != null) {
                            try {
                                int posR = Integer.parseInt(posResult);
                                q.addResultAt(rModel, posR);
                            } catch (NumberFormatException e) {
                                logger.warn("Error on XML file result ", e);
                            }
                        }
                    }
                    model.addQuery(q);
                }
            }
            return model;
        } catch (JDOMException jde) {
            logger.error("Error reading XML", jde);
            throw new EfreetException(jde.getMessage());
        } catch (IOException ioe) {
            logger.error("Error reading XML file", ioe);
            throw new EfreetException(ioe.getMessage());
        }
    }
}
