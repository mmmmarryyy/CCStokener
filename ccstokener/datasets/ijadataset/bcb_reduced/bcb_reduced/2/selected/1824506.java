package uk.ac.lkl.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.lkl.common.util.JREXMLUtilities;
import uk.ac.lkl.common.util.expression.Expression;
import uk.ac.lkl.common.util.expression.LocatedExpression;
import uk.ac.lkl.common.util.restlet.EntityMap;
import uk.ac.lkl.common.util.restlet.RestletException;
import uk.ac.lkl.common.util.restlet.XMLConversionContext;
import uk.ac.lkl.common.util.restlet.XMLConverterManager;
import uk.ac.lkl.common.util.value.IntegerValue;
import uk.ac.lkl.client.CommonUtils;
import uk.ac.lkl.client.Utilities;
import uk.ac.lkl.client.rpc.ExpresserService;
import uk.ac.lkl.server.objectify.ColorSpecificRuleExpression;
import uk.ac.lkl.server.objectify.DAO;
import uk.ac.lkl.server.objectify.MovedEvent;
import uk.ac.lkl.server.objectify.NextTimeStamp;
import uk.ac.lkl.server.objectify.OpenSessionChannels;
import uk.ac.lkl.server.objectify.PartOfEvent;
import uk.ac.lkl.server.objectify.PreviousTimeStamp;
import uk.ac.lkl.server.objectify.ServerLocatedExpression;
import uk.ac.lkl.server.objectify.ServerPattern;
import uk.ac.lkl.server.objectify.ServerProjectNames;
import uk.ac.lkl.server.objectify.ServerShape;
import uk.ac.lkl.server.objectify.ServerTiedNumber;
import uk.ac.lkl.server.objectify.ServerTile;
import uk.ac.lkl.server.objectify.ServerGroupShape;
import uk.ac.lkl.server.objectify.ServerTotalTilesExpression;
import uk.ac.lkl.server.objectify.TiedNumberDisplayModeEvent;
import uk.ac.lkl.server.objectify.TiedNumberLockedEvent;
import uk.ac.lkl.server.objectify.TiedNumberNameEvent;
import uk.ac.lkl.server.objectify.TiedNumberValueEvent;
import uk.ac.lkl.server.objectify.TimeStampLink;
import uk.ac.lkl.server.objectify.XMLUpdateEvent;
import uk.ac.lkl.migen.system.expresser.CommonFormatEventType;
import uk.ac.lkl.migen.system.expresser.KaleidoscopeCommonFormatLogger;
import uk.ac.lkl.migen.system.expresser.model.ColorResourceAttributeHandle;
import uk.ac.lkl.migen.system.expresser.model.ExpresserModel;
import uk.ac.lkl.migen.system.expresser.model.ExpresserModelImpl;
import uk.ac.lkl.migen.system.expresser.model.ExpressionValueSource;
import uk.ac.lkl.migen.system.expresser.model.ModelColor;
import uk.ac.lkl.migen.system.expresser.model.shape.block.BlockShape;
import uk.ac.lkl.migen.system.expresser.model.shape.block.GroupShape;
import uk.ac.lkl.migen.system.expresser.model.shape.block.ModelGroupShape;
import uk.ac.lkl.migen.system.expresser.model.shape.block.PatternShape;
import uk.ac.lkl.migen.system.expresser.model.tiednumber.TiedNumberExpression;
import uk.ac.lkl.migen.system.server.converter.ConversionUtilities;
import uk.ac.lkl.migen.system.util.MiGenUtilities;
import uk.ac.lkl.migen.system.util.URLLocalFileCache;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.SendResponse.Status;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Query;

/**
 * Implements the remote service for the web-based version of Expresser
 * 
 * @author Ken Kahn
 * 
 */
public class ExpresserServiceImpl extends RemoteServiceServlet implements ExpresserService {

    private static final String UNNAMED_PROJECT = "unnamed project";

    private static final XMPPService metaforaXMPP = XMPPServiceFactory.getXMPPService();

    private static final JID fromJID = new JID("Metafora@web-expresser.appspotchat.com");

    public ExpresserServiceImpl() {
        super();
        if (!MiGenUtilities.isFactoryRepositoryInitialised()) {
            MiGenUtilities.initialiseFactoryRepositoryForNonGwtClient();
        }
    }

    public String[] fetchModel(String contextKey, String projectName, String asOfTimeString) {
        return fetchModel(contextKey, projectName, asOfTimeString, null, null, null);
    }

    @Override
    public String[] fetchModel(String contextKey, String projectName, String asOfTimeString, String userName, String contextName, String activityName) {
        String[] result = new String[9];
        DAO dao = ServerUtils.getDao();
        long asOfTime;
        long currentTime = 0;
        if (asOfTimeString == null) {
            asOfTime = DAO.DISTANT_FUTURE_TIME;
        } else {
            try {
                asOfTime = Long.parseLong(asOfTimeString);
            } catch (NumberFormatException e) {
                result[8] = ServerUtils.warn("Unable to parse time stamp as a Long: " + asOfTimeString);
                asOfTime = DAO.DISTANT_FUTURE_TIME;
            }
            currentTime = asOfTime;
        }
        result[2] = SystemProperty.Environment.applicationVersion.get();
        if (contextKey == null) {
            contextKey = ServerUtils.generateGUIDString();
        } else {
            HashMap<String, TiedNumberExpression<IntegerValue>> idToTiedNumberMap = new HashMap<String, TiedNumberExpression<IntegerValue>>();
            String projectKey = createProjectKey(contextKey, projectName);
            ExpresserModel model = new ExpresserModelImpl();
            Query<ServerTile> tiles = dao.getTiles(projectKey, asOfTime);
            for (ServerTile tile : tiles) {
                if (addShapeToModel(tile, asOfTime, model, idToTiedNumberMap, dao) && tile.getTimeStamp() > currentTime) {
                    currentTime = tile.getTimeStamp();
                }
            }
            Query<ServerGroupShape> groupShapes = dao.getGroupShapes(projectKey, asOfTime);
            for (ServerGroupShape groupShape : groupShapes) {
                if (addShapeToModel(groupShape, asOfTime, model, idToTiedNumberMap, dao) && groupShape.getTimeStamp() > currentTime) {
                    currentTime = groupShape.getTimeStamp();
                }
            }
            Query<ServerPattern> patterns = dao.getPatterns(projectKey, asOfTime);
            for (ServerPattern pattern : patterns) {
                XMLUpdateEvent xmlUpdateEvent = dao.getXMLUpdateEvent(pattern.getId(), asOfTime);
                if (xmlUpdateEvent != null) {
                    pattern.setXml(xmlUpdateEvent.getXml());
                    if (xmlUpdateEvent.getTimeStamp() > currentTime) {
                        currentTime = pattern.getTimeStamp();
                    }
                }
                if (addShapeToModel(pattern, asOfTime, model, idToTiedNumberMap, dao) && pattern.getTimeStamp() > currentTime) {
                    currentTime = pattern.getTimeStamp();
                }
            }
            Query<ServerLocatedExpression> locatedExpressions = dao.getLocatedExpressions(projectKey, asOfTime);
            for (ServerLocatedExpression locatedExpression : locatedExpressions) {
                String id = locatedExpression.getId();
                if (dao.topLevelPartOfModel(locatedExpression, asOfTime)) {
                    XMLUpdateEvent xmlUpdateEvent = dao.getXMLUpdateEvent(id, asOfTime);
                    if (xmlUpdateEvent != null) {
                        locatedExpression.setXml(xmlUpdateEvent.getXml());
                    }
                    LocatedExpression<IntegerValue> locatedModelExpression = locatedExpression.getLocatedExpression(idToTiedNumberMap);
                    if (locatedModelExpression != null) {
                        MovedEvent movedEvent = dao.getMovedEvent(id, asOfTime);
                        if (movedEvent != null) {
                            locatedModelExpression.setX(movedEvent.getX());
                            locatedModelExpression.setY(movedEvent.getY());
                        }
                        model.addLocatedExpression(locatedModelExpression);
                    }
                    if (locatedExpression.getTimeStamp() > currentTime) {
                        currentTime = locatedExpression.getTimeStamp();
                    }
                }
            }
            ServerTotalTilesExpression serverTotalTilesExpression = dao.getTotalTilesExpression(projectKey, asOfTime);
            if (serverTotalTilesExpression != null) {
                Expression<IntegerValue> totalTilesExpression = serverTotalTilesExpression.getExpression(idToTiedNumberMap);
                if (totalTilesExpression != null) {
                    model.setTotalAllocationExpression(totalTilesExpression);
                }
                if (serverTotalTilesExpression.getTimeStamp() > currentTime) {
                    currentTime = serverTotalTilesExpression.getTimeStamp();
                }
            }
            List<ModelColor> colors = model.getPalette().getColors();
            ModelGroupShape modelAsAGroup = model.getModelAsAGroup();
            for (ModelColor color : colors) {
                String colorName = color.toHTMLString();
                ColorSpecificRuleExpression rule = dao.getColorSpecificRuleExpression(projectKey, colorName, asOfTime);
                if (rule != null) {
                    ColorResourceAttributeHandle handle = BlockShape.colorResourceAttributeHandle(color);
                    modelAsAGroup.addAttribute(handle, new ExpressionValueSource<IntegerValue>(rule.getExpression(idToTiedNumberMap)));
                    if (rule.getTimeStamp() > currentTime) {
                        currentTime = rule.getTimeStamp();
                    }
                }
            }
            List<TiedNumberExpression<IntegerValue>> allTiedNumbers = model.getAllTiedNumbers();
            for (TiedNumberExpression<IntegerValue> tiedNumber : allTiedNumbers) {
                dao.updateTiedNumber(tiedNumber, asOfTime);
            }
            returnModelXML(model, result);
        }
        result[0] = contextKey;
        ServerProjectNames projectNamesObject = dao.getProjectNames(contextKey);
        String newProjectNamesAsString = "";
        boolean projectNameIsNew = true;
        String[] projectNames = null;
        if (projectNamesObject != null) {
            projectNames = projectNamesObject.getProjectNames();
            String projectDescriptionStart = projectName + ";";
            for (String oldProjectName : projectNames) {
                if (oldProjectName.equalsIgnoreCase(projectName) || oldProjectName.startsWith(projectDescriptionStart)) {
                    projectNameIsNew = false;
                } else {
                    newProjectNamesAsString += oldProjectName + ",";
                }
            }
        }
        if (projectName == null) {
            newProjectNamesAsString += UNNAMED_PROJECT;
        } else {
            newProjectNamesAsString += projectName;
        }
        result[3] = newProjectNamesAsString;
        if (projectName != null) {
            String projectDescription = projectName + ";" + userName + ";" + contextName + ";" + activityName + ";" + new Date().getTime();
            if (projectNamesObject == null) {
                projectNames = new String[1];
                projectNames[0] = projectDescription;
                projectNamesObject = new ServerProjectNames(contextKey, projectNames);
                result[3] = projectName;
            } else {
                if (projectNameIsNew) {
                    String[] newProjectNames = new String[projectNames.length + 1];
                    int index = 0;
                    for (String oldProjectName : projectNames) {
                        newProjectNames[index] = oldProjectName;
                        index++;
                    }
                    newProjectNames[index] = projectDescription;
                    projectNamesObject.setProjectNames(newProjectNames);
                }
            }
            dao.ofy().put(projectNamesObject);
        }
        String guids = ServerUtils.generateGUIDString();
        for (int i = 0; i < 1000; i++) {
            guids += "," + ServerUtils.generateGUIDString();
        }
        result[4] = guids;
        if (asOfTimeString == null) {
            asOfTimeString = Long.toString(currentTime);
        }
        result[5] = getPreviousTimeStamp(asOfTimeString, contextKey, projectName);
        result[6] = Long.toString(currentTime);
        result[7] = getNextTimeStamp(asOfTimeString, contextKey, projectName);
        return result;
    }

    /**
     * @param shape
     * @param asOfTime
     * @param model
     * @param idToTiedNumberMap 
     * @param dao
     * 
     * @return true if top-level shape that is added to model
     */
    public boolean addShapeToModel(ServerShape shape, Long asOfTime, ExpresserModel model, HashMap<String, TiedNumberExpression<IntegerValue>> idToTiedNumberMap, DAO dao) {
        if (dao.topLevelPartOfModel(shape, asOfTime)) {
            if (asOfTime != null) {
                MovedEvent movedEvent = dao.getMovedEvent(shape.getId(), asOfTime);
                if (movedEvent != null) {
                    shape.setX(movedEvent.getX());
                    shape.setY(movedEvent.getY());
                }
            }
            BlockShape patternShape = shape.getPatternShape(asOfTime, idToTiedNumberMap);
            if (patternShape != null) {
                model.addObject(patternShape);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param model
     * @param result
     * @throws TransformerFactoryConfigurationError
     */
    public void returnModelXML(ExpresserModel model, String[] result) {
        try {
            XMLConverterManager converterManager = ConversionUtilities.createConverterManager();
            XMLConversionContext context = new XMLConversionContext(converterManager, new EntityMap());
            Document document = JREXMLUtilities.createDocument();
            Element expresserModelElement = context.convertToXML(document, ExpresserModel.class, model);
            document.appendChild(expresserModelElement);
            OutputStream outputStream = new ByteArrayOutputStream();
            System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult streamResult = new StreamResult(outputStream);
            transformer.transform(source, streamResult);
            result[1] = outputStream.toString();
        } catch (RestletException e) {
            result[4] = e.getMessage();
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            result[4] = e.getMessage();
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            result[4] = e.getMessage();
            e.printStackTrace();
        } catch (TransformerException e) {
            result[4] = e.getMessage();
            e.printStackTrace();
        }
    }

    @Override
    public String[] tileCreatedOrUpdated(int x, int y, String colorName, String tileId, String parentId, String contextKey, String userName, String projectName, String attributeNameValues[], String logRecipients, String previousTimeStamp, int undoCount) {
        String[] result = new String[2];
        if (tileId == null) {
            String error = "No id given for tile.";
            ServerUtils.severe(error);
            result[1] = error;
            return result;
        }
        DAO dao = ServerUtils.getDao();
        ServerTile tile = dao.getTile(tileId);
        String projectKey = createProjectKey(contextKey, projectName);
        if (tile == null) {
            tile = new ServerTile(tileId, x, y, colorName, projectKey);
            dao.persistObject(tile);
            result[0] = tile.getTimeStampString();
            if (logRecipients != null) {
                HashMap<String, TiedNumberExpression<IntegerValue>> idToTiedNumberMap = new HashMap<String, TiedNumberExpression<IntegerValue>>();
                logShapeCreationOrUpdate(CommonFormatEventType.CREATION, tile.getPatternShape(null, idToTiedNumberMap), userName, projectName, attributeNameValues, logRecipients);
            }
        } else {
            MovedEvent moveEvent = tile.move(x, y);
            result[0] = moveEvent.getTimeStampString();
        }
        if (parentId != null) {
            dao.partOfEvent(tileId, parentId, projectKey, tile.getTimeStamp());
        }
        linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        return result;
    }

    private void linkTimeStamps(String previousTimeStamp, int undoCount, String currentTimeStamp, String projectKey) {
        if (currentTimeStamp == null) {
            return;
        }
        if (previousTimeStamp == null) {
            return;
        }
        DAO dao = ServerUtils.getDao();
        String previousUniqueTimeStamp = ServerUtils.createUniqueTimeStamp(projectKey, previousTimeStamp);
        try {
            NextTimeStamp preexistingForwardLink = dao.ofy().get(NextTimeStamp.class, previousUniqueTimeStamp);
            String currentUniqueTimeStamp = ServerUtils.createUniqueTimeStamp(projectKey, currentTimeStamp);
            if (undoCount > 0) {
                dao.invalidateTimeStampsAfter(previousTimeStamp, currentTimeStamp, projectKey);
                String invalidTimeStamp = preexistingForwardLink.getNextTimeStampAndProjectKey();
                preexistingForwardLink.setNextTimeStampAndProjectKey(currentUniqueTimeStamp);
                dao.persistObject(preexistingForwardLink);
                PreviousTimeStamp preexistingBackwardsLink = dao.ofy().find(PreviousTimeStamp.class, invalidTimeStamp);
                if (preexistingBackwardsLink != null) {
                    preexistingBackwardsLink.setPreviousTimeStampAndProjectKey(previousUniqueTimeStamp);
                    dao.persistObject(preexistingBackwardsLink);
                }
                informOpenChannels(projectKey);
            } else {
                String preexistingNextTimeStamp = preexistingForwardLink.getNextTimeStampAndProjectKey();
                if (currentTimeStamp.compareTo(preexistingNextTimeStamp) > 0) {
                    linkTimeStamps(ServerUtils.extractTimeStamp(preexistingNextTimeStamp), undoCount, currentTimeStamp, projectKey);
                    return;
                } else {
                    PreviousTimeStamp preexistingBackwardsLink = dao.ofy().find(PreviousTimeStamp.class, preexistingNextTimeStamp);
                    if (preexistingBackwardsLink != null) {
                        preexistingBackwardsLink.setPreviousTimeStampAndProjectKey(previousUniqueTimeStamp);
                        dao.persistObject(preexistingBackwardsLink);
                    }
                    TimeStampLink nextTimeStampLink = new NextTimeStamp(projectKey, previousTimeStamp, currentTimeStamp);
                    dao.persistObject(nextTimeStampLink);
                    TimeStampLink previousTimeStampLink = new PreviousTimeStamp(projectKey, previousTimeStamp, currentTimeStamp);
                    dao.persistObject(previousTimeStampLink);
                    informOpenChannels(projectKey);
                }
            }
        } catch (NotFoundException e) {
            TimeStampLink nextTimeStampLink = new NextTimeStamp(projectKey, previousTimeStamp, currentTimeStamp);
            dao.persistObject(nextTimeStampLink);
            TimeStampLink previousTimeStampLink = new PreviousTimeStamp(projectKey, previousTimeStamp, currentTimeStamp);
            dao.persistObject(previousTimeStampLink);
            informOpenChannels(projectKey);
        }
    }

    public String getNextTimeStamp(String currentTimeStamp, String contextKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        String previousTimeStampAndProjectKey = ServerUtils.createUniqueTimeStamp(createProjectKey(contextKey, projectName), currentTimeStamp);
        NextTimeStamp nextTimeStampLink = dao.ofy().find(NextTimeStamp.class, previousTimeStampAndProjectKey);
        if (nextTimeStampLink == null) {
            return null;
        } else {
            return nextTimeStampLink.getNextTimeStamp();
        }
    }

    public String getPreviousTimeStamp(String currentTimeStamp, String contextKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        String currentStampAndProjectKey = ServerUtils.createUniqueTimeStamp(createProjectKey(contextKey, projectName), currentTimeStamp);
        TimeStampLink previousTimeStampLink = dao.ofy().find(PreviousTimeStamp.class, currentStampAndProjectKey);
        if (previousTimeStampLink == null) {
            return null;
        } else {
            return previousTimeStampLink.getPreviousTimeStamp();
        }
    }

    /**
     * @param contextKey
     * @param projectName
     * @return a new key that is the concatenation of the user key and name if the name is non-null
     * otherwise the user key
     */
    public static String createProjectKey(String contextKey, String projectName) {
        if (projectName == null || projectName.equals(UNNAMED_PROJECT)) {
            return contextKey;
        } else {
            return contextKey + "." + projectName.toUpperCase();
        }
    }

    @Override
    public String[] shapeMoved(int x, int y, String id, String contextKey, String projectName, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String result[] = new String[2];
        ServerShape groupShape = dao.getShape(id);
        if (groupShape == null) {
            String error = "No shape with id " + id + " found. Maybe a collaborator deleted it.";
            ServerUtils.severe(error);
            result[1] = error;
            return result;
        }
        MovedEvent moveEvent = groupShape.move(x, y);
        result[0] = moveEvent.getTimeStampString();
        String projectKey = createProjectKey(contextKey, projectName);
        linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        return result;
    }

    @Override
    public String[] shapeDeleted(String id, String contextKey, String userName, String projectName, String attributeNameValues[], String logRecipients, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String projectKey = createProjectKey(contextKey, projectName);
        ServerShape serverShape = dao.getShape(id);
        Long timeStamp = dao.shapeDeleted(id, projectName);
        String result[] = new String[2];
        if (timeStamp != null) {
            result[0] = Long.toString(timeStamp);
            linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
            if (logRecipients != null && serverShape != null) {
                HashMap<String, TiedNumberExpression<IntegerValue>> idToTiedNumberMap = new HashMap<String, TiedNumberExpression<IntegerValue>>();
                logShapeCreationOrUpdate(CommonFormatEventType.DELETION, serverShape.getPatternShape(null, idToTiedNumberMap), userName, projectName, attributeNameValues, logRecipients);
            }
        } else {
            String error = "Warning. Did not find a shape with the id " + id;
            ServerUtils.severe(error);
            result[1] = error;
        }
        return result;
    }

    @Override
    public String[] shapeUnmade(String id, boolean isPattern, String attributeNameValues[], String logRecipients, String contextKey, String userName, String projectName, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String projectKey = createProjectKey(contextKey, projectName);
        ServerShape serverShape = dao.getShape(id);
        HashMap<String, TiedNumberExpression<IntegerValue>> idToTiedNumberMap = new HashMap<String, TiedNumberExpression<IntegerValue>>();
        PatternShape patternShape = serverShape.getPatternShape(null, idToTiedNumberMap);
        BlockShape buildingBlock = patternShape.getShape();
        ArrayList<BlockShape> shapesRestoredToTopLevel = new ArrayList<BlockShape>();
        if (isPattern) {
            shapesRestoredToTopLevel.add(buildingBlock);
        } else {
            if (buildingBlock instanceof GroupShape) {
                GroupShape groupShape = (GroupShape) buildingBlock;
                int subShapeCount = groupShape.getShapeCount();
                for (int i = 0; i < subShapeCount; i++) {
                    shapesRestoredToTopLevel.add(groupShape.getShape(i));
                }
            }
        }
        for (BlockShape restoredShape : shapesRestoredToTopLevel) {
            dao.persistObject(new PartOfEvent(restoredShape.getUniqueId(), null, projectKey));
        }
        Long timeStamp = dao.shapeDeleted(id, projectName);
        String result[] = new String[2];
        if (timeStamp != null) {
            result[0] = Long.toString(timeStamp);
            linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
            if (logRecipients != null && serverShape != null) {
                logShapeCreationOrUpdate(CommonFormatEventType.DELETION, patternShape, userName, projectName, attributeNameValues, logRecipients);
                for (BlockShape restoredShape : shapesRestoredToTopLevel) {
                    logShapeCreationOrUpdate(CommonFormatEventType.UPDATE, restoredShape, userName, projectName, attributeNameValues, logRecipients);
                }
            }
        } else {
            String error = "Warning. Did not find a shape with the id " + id;
            ServerUtils.severe(error);
            result[1] = error;
        }
        return result;
    }

    @Override
    public String[] expressionDeleted(String id, boolean errorIfNonExistent, String contextKey, String projectName, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String projectKey = createProjectKey(contextKey, projectName);
        Long timeStamp = dao.expressionDeleted(id, projectName);
        String result[] = new String[2];
        if (timeStamp != null) {
            result[0] = Long.toString(timeStamp);
            linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        } else if (errorIfNonExistent) {
            String error = "Did not find a located expression with the id " + id;
            ServerUtils.severe(error);
            result[1] = error;
        } else {
            return null;
        }
        return result;
    }

    @Override
    public String[] groupCreated(String id, int x, int y, ArrayList<String> subShapeIds, String attributeNameValues[], String logRecipients, String contextKey, String userName, String projectName, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String result[] = new String[2];
        String projectKey = createProjectKey(contextKey, projectName);
        ServerGroupShape groupShape = new ServerGroupShape(x, y, id, subShapeIds, projectKey);
        dao.persistObject(groupShape);
        result[0] = groupShape.getTimeStampString();
        if (logRecipients != null) {
            HashMap<String, TiedNumberExpression<IntegerValue>> idToTiedNumberMap = new HashMap<String, TiedNumberExpression<IntegerValue>>();
            logShapeCreationOrUpdate(CommonFormatEventType.CREATION, groupShape.getPatternShape(null, idToTiedNumberMap), userName, projectName, attributeNameValues, logRecipients);
        }
        linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        return result;
    }

    @Override
    public String[] patternCreatedOrUpdated(String id, String buildingBlockId, int modelX, int modelY, String xml, String attributeNameValues[], String logRecipients, String contextKey, String userName, String projectName, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String result[] = new String[2];
        if (id == null) {
            String error = "No id provided for pattern with building block id " + buildingBlockId;
            ServerUtils.severe(error);
            result[1] = error;
            return result;
        }
        String projectKey = createProjectKey(contextKey, projectName);
        ServerPattern pattern = dao.getPattern(id);
        if (pattern == null) {
            pattern = new ServerPattern(modelX, modelY, id, xml, projectKey);
            dao.persistObject(pattern);
            result[0] = pattern.getTimeStampString();
            if (logRecipients != null) {
                HashMap<String, TiedNumberExpression<IntegerValue>> idToTiedNumberMap = new HashMap<String, TiedNumberExpression<IntegerValue>>();
                logShapeCreationOrUpdate(CommonFormatEventType.CREATION, pattern.getPatternShape(null, idToTiedNumberMap), userName, projectName, attributeNameValues, logRecipients);
            }
        } else {
            if (!pattern.getXml().equals(xml)) {
                XMLUpdateEvent xmlUpdateEvent = pattern.storeXMLUpdateEvent(xml, projectKey);
                result[0] = xmlUpdateEvent.getTimeStampString();
                if (logRecipients != null) {
                    HashMap<String, TiedNumberExpression<IntegerValue>> idToTiedNumberMap = new HashMap<String, TiedNumberExpression<IntegerValue>>();
                    logShapeCreationOrUpdate(CommonFormatEventType.UPDATE, pattern.getPatternShape(null, idToTiedNumberMap), userName, projectName, attributeNameValues, logRecipients);
                }
            }
            if (pattern.getX() != modelX || pattern.getY() != modelY) {
                MovedEvent moveEvent = pattern.move(modelX, modelY);
                result[0] = moveEvent.getTimeStampString();
            }
        }
        if (buildingBlockId != null) {
            long timeStamp = Long.parseLong(result[0]);
            String parentId = dao.getParentId(buildingBlockId, timeStamp);
            if (parentId == null || !parentId.equals(id)) {
                result[0] = dao.partOfEvent(buildingBlockId, id, projectKey, pattern.getTimeStamp());
            }
        } else {
            result[1] = ServerUtils.severe("No buildingBlockId passed to patternCreatedOrUpdated.");
        }
        linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        return result;
    }

    @Override
    public String[] expressionCreatedOrUpdated(String id, int x, int y, String xml, String description, String contextKey, String userName, String projectName, String attributeNameValues[], String logRecipients, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String result[] = new String[2];
        if (id == null) {
            result[1] = "No id given for expression.";
            return result;
        }
        String projectKey = createProjectKey(contextKey, projectName);
        ServerLocatedExpression locatedExpression = dao.getLocatedExpression(id);
        if (locatedExpression == null) {
            locatedExpression = new ServerLocatedExpression(x, y, id, xml, projectKey);
            result[0] = locatedExpression.getTimeStampString();
            dao.persistObject(locatedExpression);
            if (logRecipients != null) {
                logExpressionCreationOrUpdate(true, id, description, userName, projectName, logRecipients);
            }
        } else {
            if (!locatedExpression.getXml().equals(xml)) {
                XMLUpdateEvent xmlUpdateEvent = locatedExpression.storeXMLUpdateEvent(xml, projectKey);
                result[0] = xmlUpdateEvent.getTimeStampString();
                if (logRecipients != null) {
                    logExpressionCreationOrUpdate(false, id, description, userName, projectName, logRecipients);
                }
            }
            if (locatedExpression.getX() != x || locatedExpression.getY() != y) {
                MovedEvent moveEvent = locatedExpression.move(x, y);
                result[0] = moveEvent.getTimeStampString();
            }
        }
        linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        return result;
    }

    @Override
    public String[] expressionMoved(int x, int y, String id, String contextKey, String projectName, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String result[] = new String[2];
        ServerLocatedExpression locatedExpression = dao.getLocatedExpression(id);
        if (locatedExpression == null) {
            String error = "No located expression with id " + id + " found. Maybe a collaborator deleted it.";
            ServerUtils.severe(error);
            result[1] = error;
            return result;
        }
        MovedEvent moveEvent = locatedExpression.move(x, y);
        result[0] = moveEvent.getTimeStampString();
        String projectKey = createProjectKey(contextKey, projectName);
        linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        return result;
    }

    @Override
    public String[] updateTiedNumber(String id, int value, String name, boolean named, int displayMode, boolean locked, boolean keyAvailable, String description, String attributeNameValues[], String logRecipients, String userName, String projectName, String previousTimeStampString, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String result[] = new String[2];
        ServerTiedNumber tiedNumber = dao.getTiedNumber(id);
        String projectKey = createProjectKey(userName, projectName);
        if (tiedNumber == null) {
            tiedNumber = new ServerTiedNumber(id, value, name, displayMode, locked, keyAvailable);
            dao.persistObject(tiedNumber);
            result[0] = tiedNumber.getTimeStampString();
            if (logRecipients != null) {
                logExpressionCreationOrUpdate(true, id, description, userName, projectName, logRecipients);
            }
        } else {
            long previousTimeStamp;
            if (previousTimeStampString == null) {
                previousTimeStamp = DAO.DISTANT_FUTURE_TIME;
            } else {
                previousTimeStamp = Long.parseLong(previousTimeStampString);
            }
            Integer mostRecentTiedNumberValue = dao.getMostRecentTiedNumberValue(id, previousTimeStamp);
            if (mostRecentTiedNumberValue == null) {
                mostRecentTiedNumberValue = tiedNumber.getValue();
            }
            if (mostRecentTiedNumberValue != value) {
                TiedNumberValueEvent tiedNumberValueEvent = new TiedNumberValueEvent(id, value, projectName);
                dao.persistObject(tiedNumberValueEvent);
                result[0] = tiedNumberValueEvent.getTimeStampString();
            }
            String mostRecentTiedNumberName = dao.getMostRecentTiedNumberName(id, previousTimeStamp);
            if (mostRecentTiedNumberName == null) {
                mostRecentTiedNumberName = tiedNumber.getName();
            }
            if (name != null && !name.equals(mostRecentTiedNumberName)) {
                TiedNumberNameEvent tiedNumberNameEvent = new TiedNumberNameEvent(id, name, projectName);
                dao.persistObject(tiedNumberNameEvent);
                result[0] = tiedNumberNameEvent.getTimeStampString();
            }
            Integer mostRecentTiedNumberDisplayMode = dao.getMostRecentTiedNumberDisplayMode(id, previousTimeStamp);
            if (mostRecentTiedNumberDisplayMode == null) {
                mostRecentTiedNumberDisplayMode = tiedNumber.getDisplayMode();
            }
            if (mostRecentTiedNumberDisplayMode != displayMode) {
                TiedNumberDisplayModeEvent tiedNumberDisplayModeEvent = new TiedNumberDisplayModeEvent(id, displayMode, projectName);
                dao.persistObject(tiedNumberDisplayModeEvent);
                result[0] = tiedNumberDisplayModeEvent.getTimeStampString();
            }
            Boolean mostRecentTiedNumberLocked = dao.getMostRecentTiedNumberLocked(id, previousTimeStamp);
            if (mostRecentTiedNumberLocked == null) {
                mostRecentTiedNumberLocked = tiedNumber.isLocked();
            }
            if (mostRecentTiedNumberLocked != locked) {
                TiedNumberLockedEvent tiedNumberLockedEvent = new TiedNumberLockedEvent(id, locked, projectName);
                dao.persistObject(tiedNumberLockedEvent);
                result[0] = tiedNumberLockedEvent.getTimeStampString();
            }
            if (logRecipients != null) {
                logExpressionCreationOrUpdate(false, id, description, userName, projectName, logRecipients);
            }
        }
        linkTimeStamps(previousTimeStampString, undoCount, result[0], projectKey);
        return result;
    }

    @Override
    public String[] updateTotalTilesExpression(String xml, String contextKey, String projectName, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String result[] = new String[2];
        String projectKey = createProjectKey(contextKey, projectName);
        ServerTotalTilesExpression serverTotalTilesExpression = new ServerTotalTilesExpression(projectKey, xml);
        dao.persistObject(serverTotalTilesExpression);
        result[0] = serverTotalTilesExpression.getTimeStampString();
        linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        return result;
    }

    @Override
    public String[] updateColorSpecificRuleExpression(String xml, String colorName, String contextKey, String projectName, String previousTimeStamp, int undoCount) {
        DAO dao = ServerUtils.getDao();
        String result[] = new String[2];
        String projectKey = createProjectKey(contextKey, projectName);
        ColorSpecificRuleExpression serverColorSpecificRuleExpression = new ColorSpecificRuleExpression(projectKey, colorName, xml);
        dao.persistObject(serverColorSpecificRuleExpression);
        result[0] = serverColorSpecificRuleExpression.getTimeStampString();
        linkTimeStamps(previousTimeStamp, undoCount, result[0], projectKey);
        return result;
    }

    @Override
    public String sendMessageToMetafora(String messageBody, String recipients) {
        if (recipients == null) {
            return null;
        }
        String[] recipientsArray = recipients.split(",");
        JID[] jids = new JID[recipientsArray.length];
        int index = 0;
        for (String recipient : recipientsArray) {
            jids[index] = new JID(recipient.trim());
            index++;
        }
        Message message = new MessageBuilder().withRecipientJids(jids).withBody(messageBody).withFromJid(fromJID).build();
        SendResponse response = metaforaXMPP.sendMessage(message);
        Status statusCode = response.getStatusMap().get(jids[0]);
        boolean messageSent = (statusCode == SendResponse.Status.SUCCESS);
        if (messageSent) {
            return null;
        } else {
            String error = "Failed to send message. Status: " + statusCode.toString() + ". Message: " + messageBody;
            ServerUtils.severe(error);
            return error;
        }
    }

    private void logShapeCreationOrUpdate(CommonFormatEventType eventType, BlockShape shape, String userName, String projectName, String[] attributeNameValues, String logRecipients) {
        KaleidoscopeCommonFormatLogger logger = new KaleidoscopeCommonFormatMetaforaLogger(userName);
        Element actionElement = logger.actionElementForShapeCreation(eventType, shape, userName, attributeNameValues);
        String action = JREXMLUtilities.nodeToString(actionElement);
        sendMessageToMetafora(action, logRecipients);
    }

    private void logExpressionCreationOrUpdate(boolean created, String id, String description, String userName, String projectName, String logRecipients) {
        KaleidoscopeCommonFormatLogger logger = new KaleidoscopeCommonFormatMetaforaLogger(userName);
        Element actionElement = logger.actionElementForExpressionCreationOrUpdate(created, id, description);
        String action = JREXMLUtilities.nodeToString(actionElement);
        sendMessageToMetafora(action, logRecipients);
    }

    @Override
    public String freshGuid() {
        return ServerUtils.generateGUIDString();
    }

    @Override
    public String logMessage(String message, String levelName, String contextKey, String projectName) {
        Level level = Level.parse(levelName);
        if (level == Level.INFO) {
            level = Level.WARNING;
            message = "INFO: " + message;
        }
        ServerUtils.logMessage(level, "Client reported: " + message + "\ncontextKey: " + contextKey + "\nProjectName: " + projectName);
        return null;
    }

    @Override
    public String[] fetchURLContents(String urlString, String contextKey) {
        String result[] = new String[2];
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            String userAgent = getThreadLocalRequest().getHeader("user-agent");
            connection.setRequestProperty("User-Agent", userAgent);
            InputStream inputStream = connection.getInputStream();
            result[0] = inputStreamToString(inputStream);
            if (result[0].contains("404 Not Found")) {
                throw new Exception("404 Not found returned from " + urlString);
            }
        } catch (Exception e) {
            InputStream inputStream = URLLocalFileCache.getInputStream(urlString);
            if (inputStream == null) {
                e.printStackTrace();
                result[1] = ServerUtils.severe("Error while trying to fetch " + urlString + ". " + e.getMessage());
            } else {
                try {
                    result[0] = inputStreamToString(inputStream);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    result[1] = ServerUtils.severe("Error while trying to fetch " + urlString + ". " + ioException.getMessage());
                }
            }
        }
        return result;
    }

    protected String inputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder contents = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            contents.append(line + "\r");
        }
        in.close();
        return contents.toString();
    }

    @Override
    public String reportIndicator(String type, String description, String[] attributeNameValues, String userName, String projectName, boolean testing, String referableObjectProperties[], String logRecipients) {
        KaleidoscopeCommonFormatLogger logger = new KaleidoscopeCommonFormatMetaforaLogger(userName);
        Element actionElement = logger.createActionElement();
        Element[] subElements = logger.addActionElement("OTHER", "indicator", attributeNameValues, description, testing, actionElement);
        Element actionObject = subElements[0];
        Element actionObjectPropertiesElement = subElements[1];
        Element actionContentPropertiesElement = subElements[2];
        actionContentPropertiesElement.appendChild(logger.createPropertyElement("INDICATOR_TYPE", type));
        if (referableObjectProperties != null) {
            actionObject.setAttribute("id", referableObjectProperties[0]);
            actionObject.setAttribute("type", referableObjectProperties[1]);
            String viewURL = referableObjectProperties[2];
            actionObjectPropertiesElement.appendChild(logger.createPropertyElement("VIEW_URL", viewURL));
            String referenceURL = referableObjectProperties[3];
            actionObjectPropertiesElement.appendChild(logger.createPropertyElement("REFERENCE_URL", referenceURL));
        }
        String action = JREXMLUtilities.nodeToString(actionElement);
        return sendMessageToMetafora(action, logRecipients);
    }

    @Override
    public String listenForModelUpdates(String contextKey, String projectName) {
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        String projectKey = createProjectKey(contextKey, projectName);
        String clientId = createClientId(projectKey);
        String channelToken = channelService.createChannel(clientId);
        DAO dao = ServerUtils.getDao();
        OpenSessionChannels openChannels = dao.find(OpenSessionChannels.class, projectKey);
        if (openChannels == null) {
            openChannels = new OpenSessionChannels(projectKey, clientId);
        } else {
            openChannels.addChannel(clientId);
        }
        dao.persistObject(openChannels);
        return channelToken;
    }

    private String createClientId(String projectKey) {
        HttpServletRequest request = getThreadLocalRequest();
        String userAgent = request.getHeader("user-agent");
        String userAgentHash = Integer.toHexString(userAgent.hashCode());
        String ipAddress = request.getRemoteAddr();
        return projectKey + userAgentHash + ipAddress;
    }

    public void informOpenChannels(String projectKey) {
        String[] clientIds = OpenSessionChannels.getClientIds(projectKey);
        if (clientIds == null) {
            return;
        }
        String exceptClientId = createClientId(projectKey);
        for (String clientId : clientIds) {
            if (!clientId.equals(exceptClientId)) {
                ChannelService channelService = ChannelServiceFactory.getChannelService();
                channelService.sendMessage(new ChannelMessage(clientId, "reloadModel"));
            }
        }
    }

    @Override
    public String windowClosing(String contextKey, String projectName) {
        String projectKey = createProjectKey(contextKey, projectName);
        String clientId = createClientId(projectKey);
        DAO dao = ServerUtils.getDao();
        OpenSessionChannels openChannels = dao.find(OpenSessionChannels.class, projectKey);
        if (openChannels != null) {
            openChannels.removeChannel(clientId);
            dao.persistObject(openChannels);
        }
        return null;
    }

    @Override
    public String[] fetchProjectNames(String contextKey) {
        DAO dao = ServerUtils.getDao();
        ServerProjectNames projectNames = dao.getProjectNames(contextKey);
        if (projectNames == null) {
            return null;
        } else {
            return projectNames.getProjectNames();
        }
    }

    @Override
    public String[] isUpToDate(String modelURL) {
        String result[] = new String[3];
        int lastSlashIndex = modelURL.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            Utilities.warn("Can't parse model URL: " + modelURL);
            result[0] = "true";
            return result;
        }
        String modelName = modelURL.substring(lastSlashIndex + 1);
        int dotIndex = modelName.indexOf('.');
        if (dotIndex >= 0) {
            modelName = modelName.substring(0, dotIndex);
        }
        int previousSlashIndex = modelURL.lastIndexOf('/', lastSlashIndex - 1);
        String contextKey = modelURL.substring(previousSlashIndex + 1, lastSlashIndex);
        result[1] = contextKey;
        result[2] = modelName;
        String asOfTimeString = CommonUtils.getURLParameter("time", modelURL);
        boolean upToDate = getNextTimeStamp(asOfTimeString, contextKey, modelName) == null;
        result[0] = Boolean.toString(upToDate);
        return result;
    }
}
