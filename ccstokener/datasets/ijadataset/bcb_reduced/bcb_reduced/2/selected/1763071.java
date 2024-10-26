package uk.ac.lkl.expresser.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
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
import uk.ac.lkl.common.util.XMLUtilities;
import uk.ac.lkl.common.util.expression.Expression;
import uk.ac.lkl.common.util.expression.LocatedExpression;
import uk.ac.lkl.common.util.restlet.EntityMap;
import uk.ac.lkl.common.util.restlet.RestletException;
import uk.ac.lkl.common.util.restlet.XMLConversionContext;
import uk.ac.lkl.common.util.restlet.XMLConverterManager;
import uk.ac.lkl.common.util.value.IntegerValue;
import uk.ac.lkl.expresser.client.rpc.ExpresserService;
import uk.ac.lkl.expresser.server.objectify.ColorSpecificRuleExpression;
import uk.ac.lkl.expresser.server.objectify.DAO;
import uk.ac.lkl.expresser.server.objectify.ServerExpressedObject;
import uk.ac.lkl.expresser.server.objectify.ServerLocatedExpression;
import uk.ac.lkl.expresser.server.objectify.ServerPattern;
import uk.ac.lkl.expresser.server.objectify.ServerProjectNames;
import uk.ac.lkl.expresser.server.objectify.ServerShape;
import uk.ac.lkl.expresser.server.objectify.ServerTiedNumber;
import uk.ac.lkl.expresser.server.objectify.ServerTile;
import uk.ac.lkl.expresser.server.objectify.ServerGroupShape;
import uk.ac.lkl.expresser.server.objectify.ServerTotalTilesExpression;
import uk.ac.lkl.migen.system.expresser.model.ColorResourceAttributeHandle;
import uk.ac.lkl.migen.system.expresser.model.ExpresserModel;
import uk.ac.lkl.migen.system.expresser.model.ExpresserModelImpl;
import uk.ac.lkl.migen.system.expresser.model.ExpressionValueSource;
import uk.ac.lkl.migen.system.expresser.model.ModelColor;
import uk.ac.lkl.migen.system.expresser.model.exception.ColorSpecificationException;
import uk.ac.lkl.migen.system.expresser.model.shape.block.BlockShape;
import uk.ac.lkl.migen.system.expresser.model.shape.block.ModelGroupShape;
import uk.ac.lkl.migen.system.server.converter.ConversionUtilities;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.SendResponse.Status;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
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

    @Override
    public String[] fetchPreviousWork(String userKey, String projectName) {
        String[] result = new String[6];
        DAO dao = ServerUtils.getDao();
        result[2] = SystemProperty.Environment.applicationVersion.get();
        if (userKey == null) {
            userKey = ServerUtils.generateGUIDString();
        } else {
            String projectKey = createProjectKey(userKey, projectName);
            ExpresserModel model = new ExpresserModelImpl();
            Query<ServerTile> tiles = dao.getTiles(projectKey);
            for (ServerTile tile : tiles) {
                if (tile.getParentId() == null) {
                    BlockShape patternShape = tile.getPatternShape();
                    if (patternShape != null) {
                        model.addObject(patternShape);
                    }
                }
            }
            Query<ServerGroupShape> groupShapes = dao.getGroupShapes(projectKey);
            for (ServerGroupShape groupShape : groupShapes) {
                if (groupShape.getParentId() == null) {
                    BlockShape patternShape = groupShape.getPatternShape();
                    if (patternShape != null) {
                        model.addObject(patternShape);
                    }
                }
            }
            Query<ServerPattern> patterns = dao.getPatterns(projectKey);
            for (ServerPattern pattern : patterns) {
                if (pattern.getParentId() == null) {
                    BlockShape patternShape = pattern.getPatternShape();
                    if (patternShape != null) {
                        model.addObject(patternShape);
                    }
                }
            }
            Query<ServerLocatedExpression> locatedExpressions = dao.getLocatedExpressions(projectKey);
            for (ServerLocatedExpression locatedExpression : locatedExpressions) {
                if (locatedExpression.getParentId() == null) {
                    LocatedExpression<IntegerValue> locatedModelExpression = locatedExpression.getLocatedExpression();
                    if (locatedModelExpression != null) {
                        model.addLocatedExpression(locatedModelExpression);
                    }
                }
            }
            ServerTotalTilesExpression serverTotalTilesExpression = dao.getTotalTilesExpression(projectKey);
            if (serverTotalTilesExpression != null) {
                Expression<IntegerValue> totalTilesExpression = serverTotalTilesExpression.getExpression();
                if (totalTilesExpression != null) {
                    model.setTotalAllocationExpression(totalTilesExpression);
                }
            }
            Query<ColorSpecificRuleExpression> allColorSpecificRuleExpressions = dao.getAllColorSpecificRuleExpressions(projectKey);
            ModelGroupShape modelAsAGroup = model.getModelAsAGroup();
            for (ColorSpecificRuleExpression rule : allColorSpecificRuleExpressions) {
                String colorName = rule.getColorName();
                try {
                    ModelColor color = ModelColor.colorFromSpecification(colorName);
                    ColorResourceAttributeHandle handle = BlockShape.colorResourceAttributeHandle(color);
                    modelAsAGroup.addAttribute(handle, new ExpressionValueSource<IntegerValue>(rule.getExpression()));
                } catch (ColorSpecificationException e) {
                    e.printStackTrace();
                }
            }
            returnModelXML(model, result);
        }
        result[0] = userKey;
        ServerProjectNames projectNameObject = dao.getProjectNames(userKey);
        String newProjectNamesAsString = "";
        boolean projectNameIsNew = true;
        String[] projectNames = null;
        if (projectNameObject != null) {
            projectNames = projectNameObject.getProjectNames();
            for (String oldProjectName : projectNames) {
                if (oldProjectName.equalsIgnoreCase(projectName)) {
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
            if (projectNameObject == null) {
                projectNames = new String[1];
                projectNames[0] = projectName;
                projectNameObject = new ServerProjectNames(userKey, projectNames);
                result[3] = projectName;
            } else {
                if (projectNameIsNew) {
                    String[] newProjectNames = new String[projectNames.length + 1];
                    int index = 0;
                    for (String oldProjectName : projectNames) {
                        newProjectNames[index] = oldProjectName;
                        index++;
                    }
                    newProjectNames[index] = projectName;
                    projectNameObject.setProjectNames(newProjectNames);
                }
            }
            dao.ofy().put(projectNameObject);
        }
        String guids = ServerUtils.generateGUIDString();
        for (int i = 0; i < 1000; i++) {
            guids += "," + ServerUtils.generateGUIDString();
        }
        result[4] = guids;
        return result;
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
            Document document = XMLUtilities.createDocument();
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
    public String tileCreatedOrUpdated(int x, int y, String colorName, String tileId, String userKey, String projectName, String xmppRecipients) {
        if (tileId == null) {
            String error = "No id given for tile.";
            ServerUtils.severe(error);
            return error;
        }
        DAO dao = ServerUtils.getDao();
        ServerTile tile = dao.getTile(tileId);
        ;
        if (tile == null) {
            String projectKey = createProjectKey(userKey, projectName);
            tile = new ServerTile(tileId, x, y, colorName, projectKey);
        } else {
            tile.setX(x);
            tile.setY(y);
        }
        dao.persistObject(tile);
        if (xmppRecipients != null) {
            logShapeCreation(tile.getPatternShape(), userKey, projectName, xmppRecipients);
        }
        return null;
    }

    /**
     * @param userKey
     * @param projectName
     * @return a new key that is the concatenation of the user key and name if the name is non-null
     * otherwise the user key
     */
    public String createProjectKey(String userKey, String projectName) {
        if (projectName == null || projectName.equals(UNNAMED_PROJECT)) {
            return userKey;
        } else {
            return userKey + "." + projectName.toUpperCase();
        }
    }

    @Override
    public String groupShapeMoved(int x, int y, String id, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        ServerGroupShape groupShape = dao.getTileGroup(id);
        if (groupShape == null) {
            String error = "No building block with id " + id + " found. Maybe a collaborator deleted it.";
            ServerUtils.severe(error);
            return error;
        }
        groupShape.setX(x);
        groupShape.setY(y);
        dao.persistObject(groupShape);
        return null;
    }

    @Override
    public String patternMoved(int x, int y, String id, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        ServerPattern pattern = dao.getPattern(id);
        if (pattern == null) {
            String error = "No pattern with id " + id + " found. Maybe a collaborator deleted it.";
            ServerUtils.severe(error);
            return error;
        }
        pattern.setX(x);
        pattern.setY(y);
        dao.persistObject(pattern);
        return null;
    }

    @Override
    public String shapeDeleted(String id, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        if (dao.shapeDeleted(id)) {
            return null;
        } else {
            String error = "Warning. Did not find a shape with the id " + id;
            ServerUtils.severe(error);
            return error;
        }
    }

    public String expressionDeleted(String id, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        if (dao.expressionDeleted(id)) {
            return null;
        } else {
            String error = "Did not find a located expression with the id " + id;
            ServerUtils.severe(error);
            return error;
        }
    }

    @Override
    public String shapeCopied(String id, String idOfCopy, String userKey, String projectName, int x, int y) {
        DAO dao = ServerUtils.getDao();
        ServerExpressedObject copy = dao.shapeCopied(id, idOfCopy, x, y);
        if (copy == null) {
            String error = "Unable to find the shape with the id " + id;
            ServerUtils.severe(error);
            return error;
        }
        return null;
    }

    @Override
    public String groupCreated(String id, int x, int y, ArrayList<String> subShapeIds, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        String projectKey = createProjectKey(userKey, projectName);
        ServerGroupShape groupShape = new ServerGroupShape(x, y, id, subShapeIds, projectKey);
        dao.persistObject(groupShape);
        return null;
    }

    @Override
    public String patternCreatedOrUpdated(String id, String buildingBlockId, int modelX, int modelY, String xml, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        if (id == null) {
            String error = "No id provided for pattern with building block id " + buildingBlockId;
            ServerUtils.severe(error);
            return error;
        }
        ServerPattern pattern = dao.getPattern(id);
        if (pattern == null) {
            String projectKey = createProjectKey(userKey, projectName);
            pattern = new ServerPattern(modelX, modelY, id, xml, projectKey);
        } else {
            pattern.setXml(xml);
            pattern.setX(modelX);
            pattern.setY(modelY);
        }
        dao.persistObject(pattern);
        if (buildingBlockId != null) {
            ServerShape buildingBlock = dao.getShape(buildingBlockId);
            if (buildingBlock != null) {
                buildingBlock.setParentId(id);
                dao.persistObject(buildingBlock);
            }
        } else {
            return ServerUtils.severe("No buildingBlockId passed to patternCreatedOrUpdated.");
        }
        return null;
    }

    @Override
    public String expressionCreatedOrUpdated(String id, int x, int y, String xml, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        if (id == null) {
            return "No id given for expression.";
        }
        ServerLocatedExpression locatedExpression = dao.getLocatedExpression(id);
        if (locatedExpression == null) {
            String projectKey = createProjectKey(userKey, projectName);
            locatedExpression = new ServerLocatedExpression(x, y, id, xml, projectKey);
        } else {
            locatedExpression.setXml(xml);
            locatedExpression.setX(x);
            locatedExpression.setY(y);
        }
        dao.persistObject(locatedExpression);
        return null;
    }

    @Override
    public String expressionMoved(int x, int y, String id, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        ServerLocatedExpression locatedExpression = dao.getLocatedExpression(id);
        if (locatedExpression == null) {
            String error = "No located expression with id " + id + " found. Maybe a collaborator deleted it.";
            ServerUtils.severe(error);
            return error;
        }
        locatedExpression.setX(x);
        locatedExpression.setY(y);
        dao.persistObject(locatedExpression);
        return null;
    }

    @Override
    public String sendMessageToMetafora(String messageBody, String recipients) {
        if (recipients == null) {
            return "No XMPP recipients";
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

    private void logShapeCreation(BlockShape shape, String userKey, String projectName, String xmppRecipients) {
        KaleidoscopeCommonFormatMetaforaLogger logger = new KaleidoscopeCommonFormatMetaforaLogger(userKey);
        Element actionElement = logger.actionElementForShapeCreation(shape);
        String action = XMLUtilities.nodeToString(actionElement);
        sendMessageToMetafora(action, xmppRecipients);
    }

    @Override
    public String updateTiedNumber(String id, int value, String name, boolean named, int displayMode, boolean locked, boolean keyAvailable) {
        DAO dao = ServerUtils.getDao();
        ServerTiedNumber tiedNumber = dao.getTiedNumber(id);
        if (tiedNumber == null) {
            tiedNumber = new ServerTiedNumber(id, value, name, named, displayMode, locked, keyAvailable);
        } else {
            tiedNumber.setValue(value);
            tiedNumber.setName(name);
            tiedNumber.setNamed(named);
            tiedNumber.setDisplayMode(displayMode);
            tiedNumber.setLocked(locked);
            tiedNumber.setKeyAvailable(keyAvailable);
        }
        dao.persistObject(tiedNumber);
        return null;
    }

    @Override
    public String updateTotalTilesExpression(String xml, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        String projectKey = createProjectKey(userKey, projectName);
        ServerTotalTilesExpression serverTotalTilesExpression = dao.getTotalTilesExpression(projectKey);
        if (serverTotalTilesExpression == null) {
            serverTotalTilesExpression = new ServerTotalTilesExpression(projectKey, xml);
        } else {
            serverTotalTilesExpression.setXml(xml);
        }
        dao.persistObject(serverTotalTilesExpression);
        return null;
    }

    @Override
    public String updateColorSpecificRuleExpression(String xml, String colorName, String userKey, String projectName) {
        DAO dao = ServerUtils.getDao();
        String projectKey = createProjectKey(userKey, projectName);
        ColorSpecificRuleExpression serverColorSpecificRuleExpression = dao.getColorSpecificRuleExpression(projectKey, colorName);
        if (serverColorSpecificRuleExpression == null) {
            serverColorSpecificRuleExpression = new ColorSpecificRuleExpression(projectKey, colorName, xml);
        } else {
            serverColorSpecificRuleExpression.setXml(xml);
        }
        dao.persistObject(serverColorSpecificRuleExpression);
        return null;
    }

    @Override
    public String freshGuid() {
        return ServerUtils.generateGUIDString();
    }

    @Override
    public String logMessage(String message, String levelName, String userKey, String projectName) {
        Level level = Level.parse(levelName);
        if (level == Level.INFO) {
            level = Level.WARNING;
            message = "INFO: " + message;
        }
        ServerUtils.logMessage(level, "Client reported: " + message + "\nUserKey: " + userKey + "\nProjectName: " + projectName);
        return null;
    }

    @Override
    public String[] fetchURLContents(String urlString, String userKey) {
        String result[] = new String[2];
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            String userAgent = getThreadLocalRequest().getHeader("user-agent");
            connection.setRequestProperty("User-Agent", userAgent);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder contents = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                contents.append(line + "\r");
            }
            in.close();
            result[0] = contents.toString();
        } catch (Exception e) {
            e.printStackTrace();
            result[1] = ServerUtils.severe("Error while trying to fetch " + urlString + ". " + e.getMessage());
        }
        return result;
    }
}
