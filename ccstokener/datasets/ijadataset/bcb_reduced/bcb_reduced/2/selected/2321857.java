package gov.nih.niaid.bcbb.nexplorer3.server.resources;

import gov.nih.niaid.bcbb.nexplorer3.SequenceAlignment;
import gov.nih.niaid.bcbb.nexplorer3.server.UIConstants;
import gov.nih.niaid.bcbb.nexplorer3.server.Utilities;
import gov.nih.niaid.bcbb.nexplorer3.server.config.JenaModelCacheManager;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodel.InputDataInfo;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.CDAOGraphFactoryImpl;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.CDAOHistoryData;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.CDAONodeViewImpl;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.CDAOTreeViewImpl;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.CDAOViewData;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.CDAOViewInitData;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.NodeObject;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.SequenceAlignmentViewData;
import gov.nih.niaid.bcbb.nexplorer3.server.datamodels.TreeViewData;
import gov.nih.niaid.bcbb.nexplorer3.server.graphics.SequenceImageGenerator;
import gov.nih.niaid.bcbb.nexplorer3.server.interfaces.CDAONodeView;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.cdao.utils.JenaModelParser;
import org.cdao.wrapper.CDAO;
import salvo.jesus.graph.CycleException;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.spi.resource.Singleton;

/**
 * Main Root resource server side class that interacts with client side
 * developed using Java Jersey Framework... Root resource classes are POJOs
 * (Plain Old Java Objects) that are annotated with @Path have at least one
 * method annotated with @Path or a resource method designator annotation such
 * as @GET, @PUT, @POST, @DELETE. Resource methods are methods of a resource
 * class annotated with a resource method designator...
 * 
 */
@SuppressWarnings("deprecation")
@Singleton
@ImplicitProduces("text/html;qs=5")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Path("/")
public class CDAOResources {

    private static Logger logger = Logger.getLogger(CDAOResources.class.getName());

    @Context
    HttpServletRequest servletRequest;

    HttpSessionEvent event;

    private static Map<String, HttpSession> map = new HashMap<String, HttpSession>();

    @Context
    ServletContext servletContext;

    @XmlTransient
    private CDAO cdao;

    @XmlTransient
    private String[] PAPILLOMA_VIRUS_DB_FILES = { "PF02711.rdf", "PF03025.rdf" };

    private int counter = 0;

    private String fileURL;

    private String selectedCharacterMatrixTitle = null;

    public CDAOResources() {
    }

    /**
	 * This method is hosted at URI path "cdao/init". It supports the HTTP GET
	 * method and produces content of the MIME media type "application/json"...
	 * Creates new instance of CDAOViewData, updates TreeViewData,
	 * SequenceAlignmentViewData and other information needed on client side and
	 * returns that instance.
	 * 
	 * @param dataSourceType
	 * @param files
	 * @param queryString
	 * @param proteinId
	 * @param familyIds
	 * @return
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/init")
    public CDAOViewData getCDAOGet(@QueryParam("dataSourceType") String dataSourceType, @QueryParam("fileName") List<String> files, @QueryParam("queryString") String queryString, @QueryParam("proteinId") String proteinId, @QueryParam("familyId") List<String> familyIds) {
        CDAOViewData cdaoViewData = setCDAOSourceData(dataSourceType, files, queryString, proteinId);
        return getCDAOdata(cdaoViewData);
    }

    /**
	 * This method is hosted at URI path "cdao/init". It supports the HTTP POST
	 * method, consumes MIME media type "application/x-www-form-urlencoded" sent
	 * by the client and produces content of the MIME media type
	 * "application/json"... Creates new instance of CDAOViewData, sets
	 * TreeViewData, SequenceAlignmentViewData and other information needed on
	 * client side and returns that instance.
	 * 
	 * @param dataSourceType
	 * @param files
	 * @param queryString
	 * @param proteinId
	 * @param familyIds
	 * @return
	 */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/init")
    public CDAOViewData getCDAOPost(@FormParam("dataSourceType") String dataSourceType, @FormParam("fileName") List<String> files, @FormParam("queryString") String queryString, @FormParam("proteinId") String proteinId, @FormParam("familyId") List<String> familyIds) {
        CDAOViewData cdaoViewData = setCDAOSourceData(dataSourceType, files, queryString, proteinId);
        return getCDAOdata(cdaoViewData);
    }

    /**
	 * This method is hosted at URI path "cdao/load". It supports the HTTP GET
	 * method and produces content of the MIME media type "application/json"...
	 * Creates new instance of CDAOViewInitData, updates InputData and returns
	 * that instance.
	 * 
	 * @return
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/load")
    public CDAOViewInitData getCDAOInitialLoad() {
        CDAOViewInitData cvid = new CDAOViewInitData();
        cvid.setInputData(getInputDataInfo().getInputData());
        return cvid;
    }

    /**
	 * This method is hosted at URI path "util/deleteUploadedContent". It
	 * supports the HTTP POST method, consumes MIME media type
	 * "application/x-www-form-urlencoded" sent by the client and produces
	 * content of the MIME media type "application/json"... Deletes the uploaded
	 * file and returns a string with the message.
	 * 
	 * @param dataSourceType
	 * @param fileName
	 * @return
	 */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("util/deleteUploadedContent")
    public String deleteUploadedContents(@FormParam("dataSourceType") String dataSourceType, @FormParam("fileName") String fileName) {
        HttpSession httpSession = servletRequest.getSession();
        String cdaoInstanceFileString = null;
        if (dataSourceType.equalsIgnoreCase("file")) {
            cdaoInstanceFileString = Utilities.getSessionDir(servletContext, httpSession.getId(), true) + System.getProperty("file.separator") + fileName;
        } else if (dataSourceType.equalsIgnoreCase("url")) {
            cdaoInstanceFileString = Utilities.getSessionDir(servletContext, httpSession.getId(), false) + System.getProperty("file.separator") + fileName;
        }
        File file = new File(cdaoInstanceFileString);
        if (file == null || !file.exists()) {
            return "Error: File does not exist in the server";
        } else {
            boolean b = file.delete();
            if (!b) return "Error: Problem deleting file in the server. The file cannot be deleted."; else return "Success: File successfully deleted in the server.";
        }
    }

    /**
	 * This method is hosted at URI path "cdao/saveHistory". It supports the
	 * HTTP GET method and produces content of the MIME media type
	 * "application/json"... Creates new instance of CDAOHistoryData, updates
	 * CDAOViewData, HistoryId, TimeStamp, Title and Description in the instance
	 * and adds this instance in the CDAOHistoryData list and saves this list on
	 * the current HttpSession. Returns the updated CDAOHistoryData list needed
	 * on the client side..
	 * 
	 * @param title
	 * @param description
	 * @return
	 */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/saveHistory")
    public List<CDAOHistoryData> saveCDAOHistory(@QueryParam("title") String title, @QueryParam("description") String description) {
        HttpSession httpSession = servletRequest.getSession();
        String historyId = String.valueOf(counter++);
        java.util.Date today = new java.util.Date();
        SimpleDateFormat simpDate = new SimpleDateFormat("hh:mm:ss a");
        String timeStamp = (simpDate.format(today)).toString();
        CDAOHistoryData cdaoHistoryData = new CDAOHistoryData();
        cdaoHistoryData.setHistoryId(historyId);
        cdaoHistoryData.setTimeStamp(timeStamp);
        cdaoHistoryData.setTitle(title);
        cdaoHistoryData.setDescription(description);
        cdaoHistoryData.setSessionId(httpSession.getId());
        try {
            CDAOViewData c = new CDAOViewData().cloneObject(getCDAOViewData());
            c.initialize();
            cdaoHistoryData.setNewCdaoViewData(c);
        } catch (JAXBException e) {
            logger.error("Failed to clone CDAOViewData object ");
            e.printStackTrace();
        }
        List<CDAOHistoryData> historyDataList = (List<CDAOHistoryData>) httpSession.getAttribute("historyDataList");
        if (historyDataList == null) {
            historyDataList = new ArrayList<CDAOHistoryData>();
        }
        historyDataList.add(cdaoHistoryData);
        httpSession.setAttribute("historyDataList", historyDataList);
        map.put(httpSession.getId(), httpSession);
        return historyDataList;
    }

    /**
	 * This method is hosted at URI path "cdao/loadHistoryData". It supports the
	 * HTTP POST method, consumes MIME media type
	 * "application/x-www-form-urlencoded" sent by the client and produces
	 * content of the MIME media type "application/json"... Gets the
	 * historyDataList from the current session and checks if the requested id
	 * is there in that list. If yes then gets the CDAOViewData for that id and
	 * returns CDAOViewData needed on the client side.
	 * 
	 * @param id
	 * @return
	 */
    @SuppressWarnings("unchecked")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/loadHistoryData")
    public CDAOViewData loadCDAOHistoryData(@FormParam("id") String id) {
        List<CDAOHistoryData> historyDataList = (List<CDAOHistoryData>) servletRequest.getSession().getAttribute("historyDataList");
        CDAOViewData cdaoViewData = getCDAOViewData();
        for (int i = 0; i < historyDataList.size(); i++) {
            String historyId = historyDataList.get(i).getHistoryId();
            if (historyId.equals(id)) {
                cdaoViewData = historyDataList.get(i).getNewCdaoViewData();
                return getCDAOdata(cdaoViewData);
            }
        }
        return getCDAOdata(cdaoViewData);
    }

    /**
	 * This method is hosted at URI path "cdao/loadHistoryDataForUrl". It
	 * supports the HTTP POST method, consumes MIME media type
	 * "application/x-www-form-urlencoded" sent by the client and produces
	 * content of the MIME media type "application/json"... Gets the
	 * corresponding session with the session id. Then gets the historyDataList
	 * from that session and checks if the requested id is there in that list.
	 * If yes then gets the CDAOViewData for that id and returns CDAOViewData
	 * needed on the client side.
	 * 
	 * @param historyId
	 * @param sessionId
	 * @return
	 */
    @SuppressWarnings({ "unchecked" })
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/loadHistoryDataForUrl")
    public CDAOViewData loadCDAOHistoryDataForUrl(@FormParam("historyId") String historyId, @FormParam("sessionId") String sessionId) {
        HttpSession oldsession = map.get(sessionId);
        List<CDAOHistoryData> historyDataList = (List<CDAOHistoryData>) oldsession.getAttribute("historyDataList");
        CDAOViewData cdaoViewData = getCDAOViewData();
        for (int i = 0; i < historyDataList.size(); i++) {
            String hId = historyDataList.get(i).getHistoryId();
            String sId = historyDataList.get(i).getSessionId();
            if (null != hId && null != sId && hId.equals(historyId) && sId.equals(sessionId)) {
                cdaoViewData = historyDataList.get(i).getNewCdaoViewData();
                return getCDAOdata(cdaoViewData);
            }
        }
        return getCDAOdata(cdaoViewData);
    }

    /**
	 * This method is hosted at URI path "cdao/updateHistoryData". It supports
	 * the HTTP GET method and produces content of the MIME media type
	 * "application/json"... Gets the historyDataList from the current session
	 * and checks if the requested id is there in that list. If yes then updates
	 * title, description for that id saves the list on the session again and
	 * also returns the updated list needed on the client side..
	 * 
	 * @param title
	 * @param description
	 * @param id
	 * @return
	 */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/updateHistoryData")
    public List<CDAOHistoryData> updateCDAOHistoryData(@QueryParam("title") String title, @QueryParam("description") String description, @QueryParam("id") String id) {
        HttpSession httpSession = servletRequest.getSession();
        List<CDAOHistoryData> historyDataList = (List<CDAOHistoryData>) httpSession.getAttribute("historyDataList");
        for (int i = 0; i < historyDataList.size(); i++) {
            String historyId = historyDataList.get(i).getHistoryId();
            if (historyId.equals(id)) {
                historyDataList.get(i).setTitle(title);
                historyDataList.get(i).setDescription(description);
            }
        }
        httpSession.setAttribute("historyDataList", historyDataList);
        return historyDataList;
    }

    /**
	 * This method is hosted at URI path "cdao/deleteHistoryData". It supports
	 * the HTTP POST method, consumes MIME media type
	 * "application/x-www-form-urlencoded" sent by the client and produces
	 * content of the MIME media type "application/json"... Gets the
	 * historyDataList from the current session and checks if the requested id
	 * is there in that list. If yes then deletes that CDAOHistoryData from the
	 * list and saves the updated list on the session again and also returns the
	 * updated list needed on the client side..
	 * 
	 * @param id
	 * @return
	 */
    @SuppressWarnings("unchecked")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/deleteHistoryData")
    public List<CDAOHistoryData> deleteCDAOHistoryData(@FormParam("id") String id) {
        HttpSession httpSession = servletRequest.getSession();
        List<CDAOHistoryData> historyDataList = (List<CDAOHistoryData>) httpSession.getAttribute("historyDataList");
        for (int i = 0; i < historyDataList.size(); i++) {
            String historyId = historyDataList.get(i).getHistoryId();
            if (historyId.equals(id)) {
                historyDataList.remove(i);
            }
        }
        httpSession.setAttribute("historyDataList", historyDataList);
        return historyDataList;
    }

    /**
	 * This method is hosted at URI path "cdao/loadHistory". It supports the
	 * HTTP GET method and produces content of the MIME media type
	 * "application/json"... Gets the historyDataList from the current session
	 * and returns that list neede on the client side..
	 * 
	 * @return
	 */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cdao/loadHistory")
    public List<CDAOHistoryData> loadCDAOHistory() {
        HttpSession httpSession = servletRequest.getSession();
        List<CDAOHistoryData> historyDataList = (List<CDAOHistoryData>) httpSession.getAttribute("historyDataList");
        if (historyDataList == null) {
            historyDataList = new ArrayList<CDAOHistoryData>();
        }
        return historyDataList;
    }

    /**
	 * This method is hosted at URI path "cdao/process". It supports the HTTP
	 * GET method and produces content of the MIME media type
	 * "application/json"... Sets the input options in the CDAOViewData instance
	 * and Updates Tree & Sequence data in the CDAOViewData instance and saves
	 * the instance on the current HTTP session and also returns the updated
	 * instance needed on the client side..
	 * 
	 * @param matrixId
	 * @param treeId
	 * @param viewOption
	 * @param isDrawBorder
	 * @param isShowBootStrap
	 * @param isShowInternalNodeLabel
	 * @param isShowOTULabels
	 * @param isRightJustifyLabel
	 * @param verticalSpacing
	 * @param treeWidth
	 * @param lineThickness
	 * @param layout
	 * @param doAction
	 * @param nodeURI
	 * @param x
	 * @param y
	 * @param hideColStartIndex
	 * @param hideColEndIndex
	 * @param hideRowStartIndex
	 * @param hideRowEndIndex
	 * @param revealIndex
	 * @param colorScheme
	 * @param proteinId
	 * @return
	 */
    @GET
    @Path("cdao/process")
    @Produces(MediaType.APPLICATION_JSON)
    public CDAOViewData getCDAODataService(@QueryParam("matrixId") String matrixId, @QueryParam("treeId") String treeId, @QueryParam("viewOption") String viewOption, @QueryParam("isDrawBorder") boolean isDrawBorder, @QueryParam("isShowBootStrap") boolean isShowBootStrap, @QueryParam("isShowInternalNodeLabel") boolean isShowInternalNodeLabel, @QueryParam("isShowOTULabels") boolean isShowOTULabels, @QueryParam("isRightJustifyLabel") boolean isRightJustifyLabel, @QueryParam("verticalSpacing") float verticalSpacing, @QueryParam("treeWidth") float treeWidth, @QueryParam("lineThickness") int lineThickness, @QueryParam("layout") String layout, @QueryParam("doAction") String doAction, @QueryParam("selectedNodeURI") String nodeURI, @QueryParam("x") int x, @QueryParam("y") int y, @QueryParam("hideColStartIndex") int hideColStartIndex, @QueryParam("hideColEndIndex") int hideColEndIndex, @QueryParam("hideRowStartIndex") int hideRowStartIndex, @QueryParam("hideRowEndIndex") int hideRowEndIndex, @QueryParam("revealIndex") int revealIndex, @QueryParam("colorScheme") String colorScheme, @QueryParam("proteinId") String proteinId) {
        final long startTime = System.nanoTime();
        final long endTime;
        boolean showTree = true, showMatrix = true;
        CDAOViewData cdaoViewData = getCDAOViewData();
        try {
            if (treeId != null && treeId.trim().length() == 0) treeId = null;
            if (matrixId != null && matrixId.trim().length() == 0) matrixId = null;
            if (nodeURI != null && nodeURI.trim().length() == 0) nodeURI = null;
            if (matrixId != null) matrixId = Utilities.unformattedURI(matrixId);
            if (treeId != null) treeId = Utilities.unformattedURI(treeId);
            if (nodeURI != null) nodeURI = Utilities.unformattedURI(nodeURI);
            TreeViewData treeData = cdaoViewData.getTreeViewData();
            if (treeId == null) treeData.setImageUrl(null);
            treeData.setDrawBorder(isDrawBorder);
            treeData.setShowBootStrap(isShowBootStrap);
            treeData.setShowInternalNodeLabel(isShowInternalNodeLabel);
            treeData.setShowOTULabels(isShowOTULabels);
            treeData.setRightJustifyLabel(isRightJustifyLabel);
            cdaoViewData.setVerticalSpacing(verticalSpacing);
            cdaoViewData.setViewOption(viewOption);
            treeData.setTreeWidth(treeWidth);
            treeData.setNodeURI(nodeURI);
            treeData.setLineThickness(lineThickness);
            treeData.setLayout(layout);
            if (proteinId != null && proteinId.trim().length() > 0 && treeId != null) {
                CDAOResourceManager.getInstance().addTreeAction(fileURL, treeId, "highlight", proteinId);
                cdaoViewData.setDoAction("highlight");
            }
            if (doAction != null && !doAction.equals("null")) {
                cdaoViewData.setDoAction(doAction);
            }
            if (matrixId != null && doAction != null) {
                if (doAction.startsWith("reveal")) {
                    CDAOResourceManager.getInstance().addSeqAlignmentTreeAction(fileURL, matrixId, doAction, revealIndex, revealIndex, -1, -1);
                } else if (((hideColStartIndex >= 0 && hideColEndIndex >= 0) || (hideRowStartIndex >= 0 && hideRowEndIndex >= 0))) {
                    CDAOResourceManager.getInstance().addSeqAlignmentTreeAction(fileURL, matrixId, doAction, hideRowStartIndex, hideColStartIndex, hideRowEndIndex, hideColEndIndex);
                }
            }
            SequenceAlignmentViewData sequenceAlignmentViewData = cdaoViewData.getSeqAlignmentViewData();
            if (matrixId == null) sequenceAlignmentViewData.setImageUrl(null);
            sequenceAlignmentViewData.setHideColStartIndex(hideColStartIndex);
            sequenceAlignmentViewData.setHideColEndIndex(hideColEndIndex);
            sequenceAlignmentViewData.setRevealIndex(revealIndex);
            sequenceAlignmentViewData.setColorScheme(colorScheme);
            if (doAction != null && treeId != null) {
                if (doAction.equals("reset")) {
                    CDAOResourceManager.getInstance().clearTreeAction(fileURL, treeId);
                } else {
                    CDAOResourceManager.getInstance().addTreeAction(fileURL, treeId, doAction, nodeURI);
                }
            }
            cdaoViewData.setActionList(CDAOResourceManager.getInstance().getActionList(fileURL, treeId));
            String matrixURI;
            if (null != matrixId && matrixId.contains(",")) matrixURI = matrixId.split(",")[0]; else matrixURI = matrixId;
            cdaoViewData.setSeqActionList(CDAOResourceManager.getInstance().getSeqActionList(fileURL, matrixURI));
            generateTreeAndSequence(cdaoViewData, treeId, matrixId, showTree, showMatrix);
            servletRequest.getSession().setAttribute("cdaoViewData", cdaoViewData);
        } finally {
            endTime = System.nanoTime();
        }
        final long duration = endTime - startTime;
        cdaoViewData.setModelLoadingTime(TimeUnit.NANOSECONDS.toMillis(duration) / 1000.0f);
        return cdaoViewData;
    }

    /**
	 * Creates a new instance of CDAOViewData object and initializes it , sets
	 * DataSourceType, FileName, queryString and proteinId and returns the
	 * corresponding object..
	 * 
	 * @param dataSourceType
	 * @param files
	 * @param queryString
	 * @param proteinId
	 * @return
	 */
    private CDAOViewData setCDAOSourceData(String dataSourceType, List<String> files, String queryString, String proteinId) {
        CDAOViewData cdaoViewData = new CDAOViewData();
        String file = null;
        if (files != null && files.size() > 0) {
            file = files.get(0);
        }
        if (null != dataSourceType) cdaoViewData.getSourceData().setDataSourceType(dataSourceType);
        if (null != file) cdaoViewData.getSourceData().setFile(file);
        if (null != proteinId) cdaoViewData.getSourceData().setProteinId(proteinId);
        if (null != queryString) cdaoViewData.getSourceData().setQueryString(queryString);
        return cdaoViewData;
    }

    /**
	 * Updates Tree & Sequence data in the CDAOViewData instance and saves the
	 * instance on the current HTTP session and also returns the updated
	 * instance needed on the client side..
	 * 
	 * @param cdaoViewData
	 * @return
	 */
    public CDAOViewData getCDAOdata(CDAOViewData cdaoViewData) {
        final long startTime = System.nanoTime();
        final long endTime;
        try {
            String file = cdaoViewData.getSourceData().getFile();
            String proteinId = cdaoViewData.getSourceData().getProteinId();
            String dataSourceType = cdaoViewData.getSourceData().getDataSourceType();
            String queryString = cdaoViewData.getSourceData().getQueryString();
            cdaoViewData.initialize();
            file = Utilities.unformattedURI(file);
            logger.debug("#####proteinId " + proteinId);
            initialize(dataSourceType, file, queryString, cdaoViewData);
            if (cdaoViewData.getErrorMessage() != null) {
                servletRequest.getSession().setAttribute("cdaoViewData", cdaoViewData);
                return cdaoViewData;
            }
            List<String> treeDescList = cdaoViewData.getTreeDescriptionList();
            List<String> charMatrixDescList = cdaoViewData.getCharMatrixDescriptionList();
            String treeId = null;
            if (treeDescList != null && treeDescList.size() > 0) {
                treeId = treeDescList.get(0);
            }
            String matrixId = null;
            if (charMatrixDescList != null && charMatrixDescList.size() > 0) matrixId = charMatrixDescList.get(0);
            if (proteinId != null && proteinId.trim().length() > 0 && treeId != null) {
                CDAOResourceManager.getInstance().addTreeAction(fileURL, treeId, "highlight", proteinId);
                cdaoViewData.setDoAction("highlight");
            }
            generateTreeAndSequence(cdaoViewData, treeId, matrixId, true, true);
            servletRequest.getSession().setAttribute("cdaoViewData", cdaoViewData);
        } finally {
            endTime = System.nanoTime();
        }
        final long duration = endTime - startTime;
        cdaoViewData.setModelLoadingTime(TimeUnit.NANOSECONDS.toMillis(duration) / 1000.0f);
        return cdaoViewData;
    }

    /**
	 * @return
	 */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
    public CDAOResources getXml() {
        return this;
    }

    /**
	 * Updates CDAO object and trees/matrices list in CDAOviewData
	 * 
	 * @param dataSourceType
	 * @param fileName
	 * @param queryString
	 * @param vd
	 */
    public void initialize(String dataSourceType, String fileName, String queryString, CDAOViewData vd) {
        logger.debug("INITIALIZE........");
        logger.info("###### fileName " + fileName + " dataSourceType=" + dataSourceType);
        System.setProperty("java.awt.headless", "true");
        CDAOResourceManager.getInstance().clearTreeAction();
        CDAOResourceManager.getInstance().clearSeqAction();
        String cdaoInstanceFileString = null;
        String cacheId = dataSourceType + "_" + fileName;
        Vector<String> fileList = new Vector<String>();
        if (fileName != null && dataSourceType != null && (dataSourceType.equalsIgnoreCase("DB") || dataSourceType.equalsIgnoreCase("paveDB")) && fileName.startsWith("Papilloma")) {
            for (int i = 0; i < PAPILLOMA_VIRUS_DB_FILES.length; i++) {
                String file = servletRequest.getRealPath(System.getProperty("file.separator") + "nexplorer/data") + System.getProperty("file.separator") + PAPILLOMA_VIRUS_DB_FILES[i];
                fileList.add(file);
            }
        } else if (fileName != null && dataSourceType != null) {
            HttpSession httpSession = servletRequest.getSession();
            if (dataSourceType.equalsIgnoreCase("Upload file")) {
                cdaoInstanceFileString = Utilities.getSessionDir(servletContext, httpSession.getId(), true) + System.getProperty("file.separator") + fileName;
            } else if (dataSourceType.equalsIgnoreCase("Upload from URL")) {
                cdaoInstanceFileString = Utilities.getSessionDir(servletContext, httpSession.getId(), false) + System.getProperty("file.separator") + fileName;
            } else {
                cdaoInstanceFileString = servletRequest.getRealPath(System.getProperty("file.separator") + "nexplorer/data") + System.getProperty("file.separator") + fileName;
            }
            fileList.add(cdaoInstanceFileString);
        }
        for (int i = 0; i < fileList.size(); i++) {
            String fileN = fileList.get(i);
            logger.info("###### fileurl " + fileN);
            File cdaoInstanceFile = new File(fileN);
            if (!cdaoInstanceFile.exists()) {
                logger.info("###### !cdaoInstanceFile.exists() " + fileURL);
                logger.error("File Absent Error: Input RDF file: " + fileName + "(" + cdaoInstanceFileString + ") not present.");
                vd.setErrorMessage("ERROR: File not found: " + fileName + " (" + cdaoInstanceFileString + ")");
                return;
            }
        }
        JenaModelParser jenaModelParser = JenaModelCacheManager.getInstance().cdaoObj(cacheId, fileList);
        JenaModelParser.setInstance(jenaModelParser);
        logger.info("Read Jena model- " + cacheId);
        cdao = null;
        try {
            cdao = new CDAO();
            vd.setCharMatrixDescriptionList(cdao.getCharacterMatrixList());
            try {
                String queryString1 = "SELECT ?treeResource ?treeDescription WHERE { { ?treeResource rdf:type cdao:Network }  OPTIONAL { ?treeResource rdfs:label ?treeDescription}} ";
                vd.setQueryResultList(cdao.executeQuery(queryString));
                vd.setTreeDescriptionList(cdao.getTreesList(queryString1));
            } catch (Exception ex) {
                vd.setErrorMessage("ERROR: Invalid SPARQL Query\n\n\n" + queryString);
                vd.setTreeDescriptionList(null);
            }
        } catch (Exception e) {
            logger.error("Failed to read CDAO file - " + cacheId);
            e.printStackTrace();
        }
    }

    /**
	 * Gets CDAOViewData from the session, if null then initializes a new
	 * CDAOViewData object and Returns CDAOViewData..
	 * 
	 * @return
	 */
    private CDAOViewData getCDAOViewData() {
        HttpSession httpSession = servletRequest.getSession();
        CDAOViewData cdaoViewData = (CDAOViewData) httpSession.getAttribute("cdaoViewData");
        if (cdaoViewData == null) {
            cdaoViewData = new CDAOViewData();
        }
        return cdaoViewData;
    }

    /**
	 * Updates Tree data in the CDAOTreeViewImpl instance if treeId is there and
	 * Sequence data in the SequenceAlignment instance if Sequence Id is there.
	 * 
	 * @param cdaoViewData
	 * @param treeId
	 * @param matrixId
	 * @param showTree
	 * @param showMatrix
	 */
    public void generateTreeAndSequence(CDAOViewData cdaoViewData, String treeId, String matrixId, boolean showTree, boolean showMatrix) {
        CDAOTreeViewImpl tree = null;
        @SuppressWarnings("unused") SequenceAlignment seqAlignment = null;
        if (showTree && treeId != null && treeId.trim().length() > 0 && treeId.contains("http:")) {
            tree = updateTreeDataAndGetTree(cdaoViewData, treeId);
        }
        if (showMatrix && matrixId != null && matrixId.trim().length() > 0 && matrixId.contains("http:")) {
            seqAlignment = updateSeqAlignmentDataAndGetAlignment(cdaoViewData, matrixId, tree);
        }
    }

    /**
	 * Updates Sequence Alignment data and returns SequenceAlignment
	 * 
	 * @param cdaoViewData
	 * @param matrixId
	 * @param t
	 * @return
	 */
    public SequenceAlignment updateSeqAlignmentDataAndGetAlignment(CDAOViewData cdaoViewData, String matrixId, CDAOTreeViewImpl t) {
        String matrixURI;
        if (matrixId.contains(",")) matrixURI = matrixId.split(",")[0]; else matrixURI = matrixId;
        Resource charMatrixResource = cdao.getResource(matrixURI);
        SequenceAlignment sequenceAlignment = new SequenceAlignment(charMatrixResource, cdao);
        SequenceAlignmentViewData sequenceAlignmentViewData = cdaoViewData.getSeqAlignmentViewData();
        if (null != cdaoViewData.getSeqActionList() && cdaoViewData.getSeqActionList().size() > 0) {
            try {
                cdaoViewData.getSeqAlignmentViewData().executeAction(cdaoViewData.getSeqActionList());
            } catch (Exception e) {
                cdaoViewData.setErrorMessage("ERROR: Sequence alignment executeAction failed: \n" + matrixURI);
                logger.error("ERROR: Sequence alignment executeAction failed: \n" + matrixURI);
                e.printStackTrace();
                return null;
            }
        }
        File file = getTmpFile("png");
        File seqFile = new File(file.getAbsolutePath().replace(".png", "_seq.png"));
        String seqImageurl = servletRequest.getContextPath() + "/tmp/" + seqFile.getName();
        sequenceAlignmentViewData.setImageUrl(seqImageurl);
        new SequenceImageGenerator(t, seqFile, sequenceAlignment, cdaoViewData);
        return sequenceAlignment;
    }

    /**
	 * Updates Tree data and returns CDAOTreeViewImpl
	 * 
	 * @param cdaoViewData
	 * @param treeId
	 * @return
	 */
    public CDAOTreeViewImpl updateTreeDataAndGetTree(CDAOViewData cdaoViewData, String treeId) {
        String treeURI = "";
        if (treeId.contains(",")) treeURI = treeId.split(",")[0]; else treeURI = treeId;
        CDAOGraphFactoryImpl gf = new CDAOGraphFactoryImpl();
        Resource selectedResource = cdao.getResource(treeURI);
        TreeViewData treeViewData = cdaoViewData.getTreeViewData();
        treeViewData.clearNodeData();
        treeViewData.setImageUrl("");
        CDAOTreeViewImpl t = new CDAOTreeViewImpl(selectedResource, cdaoViewData);
        t.setGraphFactory(gf);
        try {
            t.initialize();
            if (t.getVertexSet().size() > 0) {
                t.setRoot(t.getRoot());
                File file = getTmpFile("png");
                String imageurl = servletRequest.getContextPath() + "/tmp/" + file.getName();
                treeViewData.setImageUrl(imageurl);
                logger.info("imageurl=" + imageurl);
                if (null != cdaoViewData.getActionList() && cdaoViewData.getActionList().size() > 0) {
                    logger.debug("cdaoViewData.getDoAction() != null");
                    t.executeAction(cdaoViewData.getActionList());
                }
                t.drawTree(file);
                for (Iterator<?> iterator = t.getVerticesIterator(); iterator.hasNext(); ) {
                    CDAONodeViewImpl cdaoNode = (CDAONodeViewImpl) iterator.next();
                    treeViewData.addNodeData(cdaoNode);
                }
                for (Iterator<?> iterator = t.getVerticesIterator(); iterator.hasNext(); ) {
                    CDAONodeViewImpl cdaoNode = (CDAONodeViewImpl) iterator.next();
                    if (cdaoNode.isCollapsed()) {
                        List<CDAONodeView> children = t.getAllChildrenOnly(cdaoNode);
                        treeViewData.removeNodeData(children);
                    }
                }
            }
        } catch (CycleException ex) {
            cdaoViewData.setErrorMessage("ERROR: CycleException in the tree " + treeId);
        } catch (Exception e) {
            cdaoViewData.setErrorMessage("ERROR: Tree Parsing error " + treeId);
            logger.error("Tree Parsing error" + e);
            e.printStackTrace();
        }
        return t;
    }

    /**
	 * @param charMatrixResourceMap
	 * @return
	 */
    public List<String> getCharMatrixResourceList(Map<String, Resource> charMatrixResourceMap) {
        List<String> charMatrixResourceList = null;
        if (charMatrixResourceMap != null && charMatrixResourceMap.size() > 0) {
            charMatrixResourceList = new ArrayList<String>(charMatrixResourceMap.size());
            Iterator<String> it = charMatrixResourceMap.keySet().iterator();
            while (it.hasNext()) {
                String description = it.next();
                charMatrixResourceList.add(description);
            }
        }
        return charMatrixResourceList;
    }

    /**
	 * @param fileName
	 * @return
	 */
    @GET
    @Path("util/loadFile")
    @Produces(MediaType.APPLICATION_JSON)
    public String getFileContent(@QueryParam("fileName") String fileName) {
        StringBuilder contents = new StringBuilder();
        try {
            String file = servletRequest.getRealPath(System.getProperty("file.separator") + "nexplorer/help") + System.getProperty("file.separator") + fileName;
            BufferedReader input = new BufferedReader(new FileReader(new File(file)));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return contents.toString();
    }

    /**
	 * @param node
	 * @return
	 */
    @GET
    @Path("util/loadNodeInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public NodeObject getNodeInfo(@QueryParam("node") String node) {
        node = Utilities.unformattedURI(node);
        NodeObject nodeObject = new NodeObject();
        List<String> propList = CDAOResourceManager.getInstance().getNodeDetailList().get(node);
        if (propList == null || propList.size() == 0) {
            propList = cdao.getNodeInfo(node);
        }
        nodeObject.setPropertyList(propList);
        node = Utilities.unformattedURI(node);
        return nodeObject;
    }

    /**
	 * @param treeId
	 * @param node
	 * @param propList
	 * @return
	 */
    @GET
    @Path("cdao/updateNode")
    @Produces(MediaType.APPLICATION_JSON)
    public String updateNodeInfo(@QueryParam("treeId") String treeId, @QueryParam("nodeURI") String node, @QueryParam("prop") List<String> propList) {
        String message = null;
        treeId = Utilities.unformattedURI(treeId);
        node = Utilities.unformattedURI(node);
        CDAOResourceManager.getInstance().addOrUpdateNodeDetailList(node, propList);
        return message;
    }

    /**
	 * @param url
	 * @return
	 */
    @POST
    @Path("util/uploadUrl")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public InputDataInfo uploadUrl(@FormParam("url") String url) {
        HttpSession httpSession = servletRequest.getSession();
        String name = UIConstants.TMP_UPLOAD_FILE_NAME + "_" + System.currentTimeMillis() + ".rdf";
        try {
            name = name.substring(name.indexOf("/") + 1);
        } catch (Exception ex) {
        }
        String fileName = Utilities.getSessionDir(servletContext, httpSession.getId(), false) + System.getProperty("file.separator") + name;
        writeURLContentToFile(url, fileName);
        Set<InputDataItem> uploadInfo = getUploadDataInfoFromSession();
        InputDataItem udi = new InputDataItem(InputDataItem.URL, url, name);
        uploadInfo.add(udi);
        InputDataInfo inputInfo = getInputDataInfo();
        return inputInfo.getInputInfo(InputDataItem.URL);
    }

    /**
	 * @return
	 */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("util/uploadFile")
    @Produces(MediaType.TEXT_HTML)
    public String uploadFile() {
        Set<InputDataItem> uploadInfo = getUploadDataInfoFromSession();
        if (ServletFileUpload.isMultipartContent(servletRequest)) {
            FileItem uploadItem = getFileItem(servletRequest);
            if (uploadItem == null) {
                return null;
            }
            String uploadedFileName = uploadItem.getName();
            String formattedFileName = "";
            if (uploadedFileName != null) {
                formattedFileName = FilenameUtils.getName(uploadedFileName);
            }
            HttpSession httpSession = servletRequest.getSession();
            String fileName = Utilities.getSessionDir(servletContext, httpSession.getId(), true) + System.getProperty("file.separator") + formattedFileName;
            byte[] fileContents = uploadItem.get();
            writeToFile(fileName, fileContents);
            InputDataItem udi = new InputDataItem(InputDataItem.FILE, uploadedFileName, formattedFileName);
            uploadInfo.add(udi);
        }
        StringWriter sw = new StringWriter();
        try {
            JSONJAXBContext context = new JSONJAXBContext(InputDataInfo.class);
            JSONMarshaller jm = (JSONMarshaller) context.createMarshaller();
            jm.marshallToJSON(getInputDataInfo(), sw);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        String json = sw.getBuffer().toString();
        return json;
    }

    /**
	 * @return
	 */
    @SuppressWarnings("unchecked")
    private Set<InputDataItem> getUploadDataInfoFromSession() {
        Set<InputDataItem> uploadInfo;
        HttpSession httpSession = servletRequest.getSession();
        if (httpSession.getAttribute("uploadInfo") != null) {
            uploadInfo = (Set<InputDataItem>) httpSession.getAttribute("uploadInfo");
        } else {
            uploadInfo = new HashSet<InputDataItem>();
            httpSession.setAttribute("uploadInfo", uploadInfo);
        }
        return uploadInfo;
    }

    /**
	 * @return
	 */
    private InputDataInfo getInputDataInfo() {
        InputDataInfo inputDataInfo = new InputDataInfo();
        Set<InputDataItem> uploadInfo = getUploadDataInfoFromSession();
        HttpSession httpSession = servletRequest.getSession();
        boolean[] b = { true, false };
        for (int j = 0; j < b.length; j++) {
            File uploadFile = new File(Utilities.getSessionDir(servletContext, httpSession.getId(), b[j]));
            File[] files = uploadFile.listFiles();
            if (files != null) {
                for (InputDataItem udi : uploadInfo) {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].getName().equals(udi.getFileName())) {
                            inputDataInfo.add(udi);
                            break;
                        }
                    }
                }
            }
        }
        return inputDataInfo;
    }

    /**
	 * @param fileName
	 * @param fileContents
	 */
    private void writeToFile(String fileName, byte[] fileContents) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(fileContents);
            fos.close();
        } catch (FileNotFoundException ex) {
            logger.error("FileNotFoundException : " + ex);
        } catch (IOException ioe) {
            logger.error("IOException : " + ioe);
        }
    }

    /**
	 * @param request
	 * @return
	 */
    @SuppressWarnings("unchecked")
    private FileItem getFileItem(HttpServletRequest request) {
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            List<FileItem> items = upload.parseRequest(request);
            for (FileItem item : items) {
                if (!item.isFormField() && "uploadFormElement".equals(item.getFieldName())) {
                    return item;
                }
            }
        } catch (FileUploadException e) {
            logger.error("Error uploading : " + e);
            return null;
        }
        logger.debug("getFileItem method called");
        return null;
    }

    /**
	 * @param cdao
	 * @return
	 */
    @SuppressWarnings("unused")
    private List<String> getCharacterMatrixNameList(CDAO cdao) {
        Map<String, Resource> charMatrixResourceMap = cdao.getCharacterMatrixMap();
        Resource charMatrixResource = null;
        List<String> charMatrixDescriptionList;
        if (charMatrixResourceMap != null && charMatrixResourceMap.size() > 0) {
            charMatrixDescriptionList = getCharMatrixResourceList(charMatrixResourceMap);
            if (selectedCharacterMatrixTitle == null) selectedCharacterMatrixTitle = charMatrixDescriptionList.get(0);
            logger.info("Selected Character Matrix : " + selectedCharacterMatrixTitle);
            charMatrixResource = charMatrixResourceMap.get(selectedCharacterMatrixTitle);
        } else {
            charMatrixDescriptionList = null;
        }
        return charMatrixDescriptionList;
    }

    /**
	 * @param suffix
	 * @return
	 */
    public File getTmpFile(String suffix) {
        File tmpDir = getTmpDir();
        long rnd = System.currentTimeMillis();
        String tmpFile = tmpDir.getAbsolutePath() + "/cdao_" + rnd + "." + suffix;
        return new File(tmpFile);
    }

    /**
	 * @return
	 */
    public File getTmpDir() {
        File tmpDir = new File(servletRequest.getRealPath("/tmp"));
        if (!tmpDir.exists()) {
            tmpDir.mkdir();
        }
        return tmpDir;
    }

    /**
	 * @param url
	 * @param fileName
	 */
    public void writeURLContentToFile(String url, String fileName) {
        BufferedInputStream urlin = null;
        BufferedOutputStream fout = null;
        try {
            int bufSize = 8 * 1024;
            urlin = new BufferedInputStream(new URL(url).openConnection().getInputStream(), bufSize);
            fout = new BufferedOutputStream(new FileOutputStream(fileName), bufSize);
            int read = -1;
            byte[] buf = new byte[bufSize];
            while ((read = urlin.read(buf, 0, bufSize)) >= 0) {
                fout.write(buf, 0, read);
            }
            fout.flush();
        } catch (Exception ex) {
        } finally {
            if (urlin != null) {
                try {
                    urlin.close();
                } catch (IOException cioex) {
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException cioex) {
                }
            }
        }
    }

    /**
	 * @param fileURL
	 * @return
	 */
    @SuppressWarnings({ "unused" })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("uploadURL")
    private String uploadURL(@QueryParam("fileURL") String fileURL) {
        String name = UIConstants.TMP_UPLOAD_FILE_NAME + "_" + System.currentTimeMillis() + ".rdf";
        try {
            name = fileURL.substring(name.indexOf("/") + 1);
        } catch (Exception ex) {
        }
        String cdaoInstanceFileString = servletRequest.getRealPath(System.getProperty("file.separator") + "nexplorer/data") + System.getProperty("file.separator") + fileURL;
        writeURLContentToFile(fileURL, cdaoInstanceFileString);
        return fileURL;
    }
}
