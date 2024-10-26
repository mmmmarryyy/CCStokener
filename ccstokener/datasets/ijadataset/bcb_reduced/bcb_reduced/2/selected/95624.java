package org.openremote.modeler.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.openremote.modeler.client.Configuration;
import org.openremote.modeler.client.Constants;
import org.openremote.modeler.client.model.Command;
import org.openremote.modeler.client.utils.PanelsAndMaxOid;
import org.openremote.modeler.configuration.PathConfig;
import org.openremote.modeler.domain.Absolute;
import org.openremote.modeler.domain.Cell;
import org.openremote.modeler.domain.CommandDelay;
import org.openremote.modeler.domain.CommandRefItem;
import org.openremote.modeler.domain.ControllerConfig;
import org.openremote.modeler.domain.DeviceCommand;
import org.openremote.modeler.domain.DeviceCommandRef;
import org.openremote.modeler.domain.DeviceMacro;
import org.openremote.modeler.domain.DeviceMacroItem;
import org.openremote.modeler.domain.DeviceMacroRef;
import org.openremote.modeler.domain.Group;
import org.openremote.modeler.domain.GroupRef;
import org.openremote.modeler.domain.Panel;
import org.openremote.modeler.domain.ProtocolAttr;
import org.openremote.modeler.domain.Screen;
import org.openremote.modeler.domain.ScreenPair;
import org.openremote.modeler.domain.ScreenPairRef;
import org.openremote.modeler.domain.Sensor;
import org.openremote.modeler.domain.Template;
import org.openremote.modeler.domain.UICommand;
import org.openremote.modeler.domain.ScreenPair.OrientationType;
import org.openremote.modeler.domain.component.Gesture;
import org.openremote.modeler.domain.component.ImageSource;
import org.openremote.modeler.domain.component.SensorOwner;
import org.openremote.modeler.domain.component.UIButton;
import org.openremote.modeler.domain.component.UIComponent;
import org.openremote.modeler.domain.component.UIControl;
import org.openremote.modeler.domain.component.UIGrid;
import org.openremote.modeler.domain.component.UIImage;
import org.openremote.modeler.domain.component.UILabel;
import org.openremote.modeler.domain.component.UISlider;
import org.openremote.modeler.domain.component.UISwitch;
import org.openremote.modeler.domain.component.UITabbar;
import org.openremote.modeler.domain.component.UITabbarItem;
import org.openremote.modeler.exception.BeehiveNotAvailableException;
import org.openremote.modeler.exception.FileOperationException;
import org.openremote.modeler.exception.IllegalRestUrlException;
import org.openremote.modeler.exception.ResourceFileLostException;
import org.openremote.modeler.exception.UIRestoreException;
import org.openremote.modeler.exception.XmlExportException;
import org.openremote.modeler.exception.XmlParserException;
import org.openremote.modeler.protocol.ProtocolContainer;
import org.openremote.modeler.service.ControllerConfigService;
import org.openremote.modeler.service.DeviceCommandService;
import org.openremote.modeler.service.DeviceMacroService;
import org.openremote.modeler.service.ResourceService;
import org.openremote.modeler.service.UserService;
import org.openremote.modeler.touchpanel.TouchPanelTabbarDefinition;
import org.openremote.modeler.utils.FileUtilsExt;
import org.openremote.modeler.utils.JsonGenerator;
import org.openremote.modeler.utils.ProtocolCommandContainer;
import org.openremote.modeler.utils.StringUtils;
import org.openremote.modeler.utils.UIComponentBox;
import org.openremote.modeler.utils.XmlParser;
import org.openremote.modeler.utils.ZipUtils;
import org.springframework.ui.velocity.VelocityEngineUtils;

/**
 * The Class ResourceServiceImpl.
 * 
 * @author Allen, Handy, Javen
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 * 
 */
public class ResourceServiceImpl implements ResourceService {

    public static final String PANEL_XML_TEMPLATE = "panelXML.vm";

    public static final String CONTROLLER_XML_TEMPLATE = "controllerXML.vm";

    /** The Constant logger. */
    private static final Logger LOGGER = Logger.getLogger(ResourceServiceImpl.class);

    /** The configuration. */
    private Configuration configuration;

    /** The device command service. */
    private DeviceCommandService deviceCommandService;

    /** The device macro service. */
    private DeviceMacroService deviceMacroService;

    private VelocityEngine velocity;

    private UserService userService;

    private ControllerConfigService controllerConfigService = null;

    /**
    * {@inheritDoc}
    */
    public String downloadZipResource(long maxOid, String sessionId, List<Panel> panels) {
        initResources(panels, maxOid);
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        File zipFile = this.getExportResource(panels);
        if (zipFile == null) {
            throw new XmlExportException("Failed to export! User folder is empty!");
        }
        return pathConfig.getZipUrl(userService.getAccount()) + zipFile.getName();
    }

    /**
    * Replace url.
    * 
    * @param activities
    *           the activities
    * @param sessionId
    *           the user id
    */
    private File compressFilesToZip(File[] files, String zipFilePath, List<String> ignoreExtentions) {
        List<File> compressedfiles = new ArrayList<File>();
        for (File file : files) {
            if (file == null || ignoreExtentions.contains(FilenameUtils.getExtension(file.getName()))) {
                continue;
            }
            compressedfiles.add(file);
        }
        File zipFile = new File(zipFilePath);
        FileUtilsExt.deleteQuietly(zipFile);
        ZipUtils.compress(zipFile.getAbsolutePath(), compressedfiles);
        return zipFile;
    }

    /**
    * Builds the lirc rest url.
    * 
    * @param restAPIUrl
    *           the rESTAPI url
    * @param ids
    *           the ids
    * 
    * @return the uRL
    */
    private URL buildLircRESTUrl(String restAPIUrl, String ids) {
        URL lircUrl;
        try {
            lircUrl = new URL(restAPIUrl + "?ids=" + ids);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Lirc file url is invalid", e);
        }
        return lircUrl;
    }

    /**
    * {@inheritDoc}
    */
    public String getDotImportFileForRender(String sessionId, InputStream inputStream) {
        File tmpDir = new File(PathConfig.getInstance(configuration).userFolder(sessionId));
        if (tmpDir.exists() && tmpDir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(tmpDir);
            } catch (IOException e) {
                throw new FileOperationException("Error in deleting temp dir", e);
            }
        }
        new File(PathConfig.getInstance(configuration).userFolder(sessionId)).mkdirs();
        String dotImportFileContent = "";
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        FileOutputStream fileOutputStream = null;
        try {
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    if (Constants.PANEL_DESC_FILE.equalsIgnoreCase(StringUtils.getFileExt(zipEntry.getName()))) {
                        dotImportFileContent = IOUtils.toString(zipInputStream);
                    }
                    if (!checkXML(zipInputStream, zipEntry, "iphone")) {
                        throw new XmlParserException("The iphone.xml schema validation failed, please check it");
                    } else if (!checkXML(zipInputStream, zipEntry, "controller")) {
                        throw new XmlParserException("The controller.xml schema validation failed, please check it");
                    }
                    if (!FilenameUtils.getExtension(zipEntry.getName()).matches("(xml|import|conf)")) {
                        File file = new File(PathConfig.getInstance(configuration).userFolder(sessionId) + zipEntry.getName());
                        FileUtils.touch(file);
                        fileOutputStream = new FileOutputStream(file);
                        int b;
                        while ((b = zipInputStream.read()) != -1) {
                            fileOutputStream.write(b);
                        }
                        fileOutputStream.close();
                    }
                }
            }
        } catch (IOException e) {
            throw new FileOperationException("Error in reading import file from zip", e);
        } finally {
            try {
                zipInputStream.closeEntry();
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to close import file resources", e);
            }
        }
        return dotImportFileContent;
    }

    /**
    * Check xml.
    * 
    * @param zipInputStream
    *           the zip input stream
    * @param zipEntry
    *           the zip entry
    * @param xmlName
    *           the xml name
    * 
    * @return true, if successful
    * 
    * @throws IOException
    *            Signals that an I/O exception has occurred.
    */
    private boolean checkXML(ZipInputStream zipInputStream, ZipEntry zipEntry, String xmlName) throws IOException {
        if (zipEntry.getName().equals(xmlName + ".xml")) {
            String xsdRelativePath = "iphone".equals(xmlName) ? configuration.getPanelXsdPath() : configuration.getControllerXsdPath();
            String xsdPath = getClass().getResource(xsdRelativePath).getPath();
            if (!XmlParser.checkXmlSchema(xsdPath, IOUtils.toString(zipInputStream))) {
                return false;
            }
        }
        return true;
    }

    /**
    * {@inheritDoc}
    */
    public File uploadImage(InputStream inputStream, String fileName, String sessionId) {
        File file = new File(PathConfig.getInstance(configuration).userFolder(sessionId) + File.separator + fileName);
        return uploadFile(inputStream, file);
    }

    public File uploadImage(InputStream inputStream, String fileName) {
        File file = new File(PathConfig.getInstance(configuration).userFolder(userService.getAccount()) + File.separator + fileName);
        return uploadFile(inputStream, file);
    }

    private File uploadFile(InputStream inputStream, File file) {
        FileOutputStream fileOutputStream = null;
        try {
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileUtils.touch(file);
            fileOutputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            throw new FileOperationException("Failed to save uploaded image", e);
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to close resources on uploaded file", e);
            }
        }
        return file;
    }

    /**
    * Gets the controller xml segment content.
    * 
    * @param command
    *           the device command item
    * @param protocolEventContainer
    *           the protocol event container
    * 
    * @return the controller xml segment content
    */
    public List<Command> getCommandOwnerByUICommand(UICommand command, ProtocolCommandContainer protocolEventContainer, MaxId maxId) {
        List<Command> oneUIButtonEventList = new ArrayList<Command>();
        try {
            if (command instanceof DeviceMacroItem) {
                if (command instanceof DeviceCommandRef) {
                    DeviceCommand deviceCommand = deviceCommandService.loadById(((DeviceCommandRef) command).getDeviceCommand().getOid());
                    addDeviceCommandEvent(protocolEventContainer, oneUIButtonEventList, deviceCommand, maxId);
                } else if (command instanceof DeviceMacroRef) {
                    DeviceMacro deviceMacro = ((DeviceMacroRef) command).getTargetDeviceMacro();
                    deviceMacro = deviceMacroService.loadById(deviceMacro.getOid());
                    for (DeviceMacroItem tempDeviceMacroItem : deviceMacro.getDeviceMacroItems()) {
                        oneUIButtonEventList.addAll(getCommandOwnerByUICommand(tempDeviceMacroItem, protocolEventContainer, maxId));
                    }
                } else if (command instanceof CommandDelay) {
                    CommandDelay delay = (CommandDelay) command;
                    Command uiButtonEvent = new Command();
                    uiButtonEvent.setId(maxId.maxId());
                    uiButtonEvent.setDelay(delay.getDelaySecond());
                    oneUIButtonEventList.add(uiButtonEvent);
                }
            } else if (command instanceof CommandRefItem) {
                DeviceCommand deviceCommand = deviceCommandService.loadById(((CommandRefItem) command).getDeviceCommand().getOid());
                addDeviceCommandEvent(protocolEventContainer, oneUIButtonEventList, deviceCommand, maxId);
            } else {
                return new ArrayList<Command>();
            }
        } catch (Exception e) {
            LOGGER.warn("Some components referenced a removed object:  " + e.getMessage());
            return new ArrayList<Command>();
        }
        return oneUIButtonEventList;
    }

    /**
    * @param protocolEventContainer
    * @param oneUIButtonEventList
    * @param deviceCommand
    */
    private void addDeviceCommandEvent(ProtocolCommandContainer protocolEventContainer, List<Command> oneUIButtonEventList, DeviceCommand deviceCommand, MaxId maxId) {
        String protocolType = deviceCommand.getProtocol().getType();
        List<ProtocolAttr> protocolAttrs = deviceCommand.getProtocol().getAttributes();
        Command uiButtonEvent = new Command();
        uiButtonEvent.setId(maxId.maxId());
        uiButtonEvent.setProtocolDisplayName(protocolType);
        for (ProtocolAttr protocolAttr : protocolAttrs) {
            uiButtonEvent.getProtocolAttrs().put(protocolAttr.getName(), protocolAttr.getValue());
        }
        uiButtonEvent.setLabel(deviceCommand.getName());
        protocolEventContainer.addUIButtonEvent(uiButtonEvent);
        oneUIButtonEventList.add(uiButtonEvent);
    }

    /**
    * Gets the section ids.
    * 
    * @param screenList
    *           the activity list
    * 
    * @return the section ids
    */
    private String getSectionIds(Collection<Screen> screenList) {
        Set<String> sectionIds = new HashSet<String>();
        for (Screen screen : screenList) {
            for (Absolute absolute : screen.getAbsolutes()) {
                if (absolute.getUiComponent() instanceof UIControl) {
                    for (UICommand command : ((UIControl) absolute.getUiComponent()).getCommands()) {
                        addSectionIds(sectionIds, command);
                    }
                }
            }
            for (UIGrid grid : screen.getGrids()) {
                for (Cell cell : grid.getCells()) {
                    if (cell.getUiComponent() instanceof UIControl) {
                        for (UICommand command : ((UIControl) cell.getUiComponent()).getCommands()) {
                            addSectionIds(sectionIds, command);
                        }
                    }
                }
            }
        }
        StringBuffer sectionIdsSB = new StringBuffer();
        int i = 0;
        for (String sectionId : sectionIds) {
            sectionIdsSB.append(sectionId);
            if (i < sectionIds.size() - 1) {
                sectionIdsSB.append(",");
            }
            i++;
        }
        return sectionIdsSB.toString();
    }

    /**
    * @param sectionIds
    * @param command
    */
    private void addSectionIds(Set<String> sectionIds, UICommand command) {
        if (command instanceof DeviceMacroItem) {
            sectionIds.addAll(getDeviceMacroItemSectionIds((DeviceMacroItem) command));
        } else if (command instanceof CommandRefItem) {
            sectionIds.add(((CommandRefItem) command).getDeviceCommand().getSectionId());
        }
    }

    /**
    * Gets the device macro item section ids.
    * 
    * @param deviceMacroItem
    *           the device macro item
    * 
    * @return the device macro item section ids
    */
    private Set<String> getDeviceMacroItemSectionIds(DeviceMacroItem deviceMacroItem) {
        Set<String> deviceMacroRefSectionIds = new HashSet<String>();
        try {
            if (deviceMacroItem instanceof DeviceCommandRef) {
                deviceMacroRefSectionIds.add(((DeviceCommandRef) deviceMacroItem).getDeviceCommand().getSectionId());
            } else if (deviceMacroItem instanceof DeviceMacroRef) {
                DeviceMacro deviceMacro = ((DeviceMacroRef) deviceMacroItem).getTargetDeviceMacro();
                if (deviceMacro != null) {
                    deviceMacro = deviceMacroService.loadById(deviceMacro.getOid());
                    for (DeviceMacroItem nextDeviceMacroItem : deviceMacro.getDeviceMacroItems()) {
                        deviceMacroRefSectionIds.addAll(getDeviceMacroItemSectionIds(nextDeviceMacroItem));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Some components referenced a removed DeviceMacro!");
        }
        return deviceMacroRefSectionIds;
    }

    /**
    * Gets the configuration.
    * 
    * @return the configuration
    */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
    * Sets the configuration.
    * 
    * @param configuration
    *           the new configuration
    */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
    * Gets the device command service.
    * 
    * @return the device command service
    */
    public DeviceCommandService getDeviceCommandService() {
        return deviceCommandService;
    }

    /**
    * Sets the device command service.
    * 
    * @param deviceCommandService
    *           the new device command service
    */
    public void setDeviceCommandService(DeviceCommandService deviceCommandService) {
        this.deviceCommandService = deviceCommandService;
    }

    /**
    * Sets the device macro service.
    * 
    * @param deviceMacroService
    *           the new device macro service
    */
    public void setDeviceMacroService(DeviceMacroService deviceMacroService) {
        this.deviceMacroService = deviceMacroService;
    }

    /**
    * {@inheritDoc}
    */
    public String getRelativeResourcePath(String sessionId, String fileName) {
        return PathConfig.getInstance(configuration).getRelativeResourcePath(fileName, sessionId);
    }

    public void setControllerConfigService(ControllerConfigService controllerConfigService) {
        this.controllerConfigService = controllerConfigService;
    }

    @Override
    public String getRelativeResourcePathByCurrentAccount(String fileName) {
        return PathConfig.getInstance(configuration).getRelativeResourcePath(fileName, userService.getAccount());
    }

    @Override
    public String getPanelsJson(Collection<Panel> panels) {
        try {
            String[] includedPropertyNames = { "groupRefs", "tabbar.tabbarItems", "tabbar.tabbarItems.navigate", "groupRefs.group.tabbar.tabbarItems", "groupRefs.group.tabbar.tabbarItems.navigate", "groupRefs.group.screenRefs", "groupRefs.group.screenRefs.screen.absolutes.uiComponent", "groupRefs.group.screenRefs.screen.gestures", "groupRefs.group.screenRefs.screen.gestures.navigate", "groupRefs.group.screenRefs.screen.absolutes.uiComponent.uiCommand", "groupRefs.group.screenRefs.screen.absolutes.uiComponent.commands", "groupRefs.group.screenRefs.screen.grids.cells.uiComponent", "groupRefs.group.screenRefs.screen.grids.cells.uiComponent.uiCommand", "groupRefs.group.screenRefs.screen.grids.cells.uiComponent.commands", "groupRefs.group.screenRefs.screen.grids.uiComponent.sensor", "groupRefs.group.screenRefs.screen.grids.cells.uiComponent.sensor" };
            String[] excludePropertyNames = { "panelName", "*.displayName", "*.panelXml" };
            return JsonGenerator.serializerObjectInclude(panels, includedPropertyNames, excludePropertyNames);
        } catch (Exception e) {
            LOGGER.error(e);
            return "";
        }
    }

    public String getPanelXML(Collection<Panel> panels) {
        Set<Group> groups = new LinkedHashSet<Group>();
        Set<Screen> screens = new LinkedHashSet<Screen>();
        initGroupsAndScreens(panels, groups, screens);
        try {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("panels", panels);
            context.put("groups", groups);
            context.put("screens", screens);
            context.put("stringUtils", StringUtils.class);
            return VelocityEngineUtils.mergeTemplateIntoString(velocity, PANEL_XML_TEMPLATE, context);
        } catch (VelocityException e) {
            throw new XmlExportException("Failed to read panel.xml", e);
        }
    }

    public VelocityEngine getVelocity() {
        return velocity;
    }

    public void setVelocity(VelocityEngine velocity) {
        this.velocity = velocity;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @SuppressWarnings("unchecked")
    public String getControllerXML(Collection<Screen> screens, long maxOid) {
        MaxId maxId = new MaxId(maxOid + 1);
        UIComponentBox uiComponentBox = new UIComponentBox();
        initUIComponentBox(screens, uiComponentBox);
        Map<String, Object> context = new HashMap<String, Object>();
        ProtocolCommandContainer eventContainer = new ProtocolCommandContainer();
        ProtocolContainer protocolContainer = ProtocolContainer.getInstance();
        Collection<Sensor> sensors = getAllSensorWithoutDuplicate(screens, maxId);
        Collection<UISwitch> switchs = (Collection<UISwitch>) uiComponentBox.getUIComponentsByType(UISwitch.class);
        Collection<UIComponent> buttons = (Collection<UIComponent>) uiComponentBox.getUIComponentsByType(UIButton.class);
        Collection<UIComponent> gestures = (Collection<UIComponent>) uiComponentBox.getUIComponentsByType(Gesture.class);
        Collection<UIComponent> uiSliders = (Collection<UIComponent>) uiComponentBox.getUIComponentsByType(UISlider.class);
        Collection<UIComponent> uiImages = (Collection<UIComponent>) uiComponentBox.getUIComponentsByType(UIImage.class);
        Collection<UIComponent> uiLabels = (Collection<UIComponent>) uiComponentBox.getUIComponentsByType(UILabel.class);
        Collection<ControllerConfig> configs = controllerConfigService.listAllConfigs();
        configs.removeAll(controllerConfigService.listAllexpiredConfigs());
        configs.addAll(controllerConfigService.listAllMissingConfigs());
        context.put("switchs", switchs);
        context.put("buttons", buttons);
        context.put("screens", screens);
        context.put("eventContainer", eventContainer);
        context.put("resouceServiceImpl", this);
        context.put("protocolContainer", protocolContainer);
        context.put("sensors", sensors);
        context.put("gestures", gestures);
        context.put("uiSliders", uiSliders);
        context.put("labels", uiLabels);
        context.put("images", uiImages);
        context.put("maxId", maxId);
        context.put("configs", configs);
        context.put("stringUtils", StringUtils.class);
        return VelocityEngineUtils.mergeTemplateIntoString(velocity, CONTROLLER_XML_TEMPLATE, context);
    }

    private void initGroupsAndScreens(Collection<Panel> panels, Set<Group> groups, Set<Screen> screens) {
        for (Panel panel : panels) {
            List<GroupRef> groupRefs = panel.getGroupRefs();
            for (GroupRef groupRef : groupRefs) {
                groups.add(groupRef.getGroup());
            }
        }
        for (Group group : groups) {
            List<ScreenPairRef> screenRefs = group.getScreenRefs();
            for (ScreenPairRef screenRef : screenRefs) {
                ScreenPair screenPair = screenRef.getScreen();
                if (OrientationType.PORTRAIT.equals(screenPair.getOrientation())) {
                    screens.add(screenPair.getPortraitScreen());
                } else if (OrientationType.LANDSCAPE.equals(screenPair.getOrientation())) {
                    screens.add(screenPair.getLandscapeScreen());
                } else if (OrientationType.BOTH.equals(screenPair.getOrientation())) {
                    screenPair.setInverseScreenIds();
                    screens.add(screenPair.getPortraitScreen());
                    screens.add(screenPair.getLandscapeScreen());
                }
            }
        }
    }

    private Set<Sensor> getAllSensorWithoutDuplicate(Collection<Screen> screens, MaxId maxId) {
        Set<Sensor> sensorWithoutDuplicate = new HashSet<Sensor>();
        Collection<Sensor> allSensors = new ArrayList<Sensor>();
        for (Screen screen : screens) {
            for (Absolute absolute : screen.getAbsolutes()) {
                UIComponent component = absolute.getUiComponent();
                initSensors(allSensors, sensorWithoutDuplicate, component);
            }
            for (UIGrid grid : screen.getGrids()) {
                for (Cell cell : grid.getCells()) {
                    initSensors(allSensors, sensorWithoutDuplicate, cell.getUiComponent());
                }
            }
        }
        for (Sensor sensor : sensorWithoutDuplicate) {
            long currentSensorId = maxId.maxId();
            Collection<Sensor> sensorsWithSameOid = new ArrayList<Sensor>();
            sensorsWithSameOid.add(sensor);
            for (Sensor s : allSensors) {
                if (s.equals(sensor)) {
                    sensorsWithSameOid.add(s);
                }
            }
            for (Sensor s : sensorsWithSameOid) {
                s.setOid(currentSensorId);
            }
        }
        return sensorWithoutDuplicate;
    }

    private void initSensors(Collection<Sensor> allSensors, Set<Sensor> sensorsWithoutDuplicate, UIComponent component) {
        if (component instanceof SensorOwner) {
            SensorOwner sensorOwner = (SensorOwner) component;
            if (sensorOwner.getSensor() != null) {
                allSensors.add(sensorOwner.getSensor());
                sensorsWithoutDuplicate.add(sensorOwner.getSensor());
            }
        }
    }

    private void initUIComponentBox(Collection<Screen> screens, UIComponentBox uiComponentBox) {
        uiComponentBox.clear();
        for (Screen screen : screens) {
            for (Absolute absolute : screen.getAbsolutes()) {
                UIComponent component = absolute.getUiComponent();
                uiComponentBox.add(component);
            }
            for (UIGrid grid : screen.getGrids()) {
                for (Cell cell : grid.getCells()) {
                    uiComponentBox.add(cell.getUiComponent());
                }
            }
            for (Gesture gesture : screen.getGestures()) {
                uiComponentBox.add(gesture);
            }
        }
    }

    @Override
    public void initResources(Collection<Panel> panels, long maxOid) {
        serializePanelsAndMaxOid(panels, maxOid);
        Set<Group> groups = new LinkedHashSet<Group>();
        Set<Screen> screens = new LinkedHashSet<Screen>();
        initGroupsAndScreens(panels, groups, screens);
        String controllerXmlContent = getControllerXML(screens, maxOid);
        String panelXmlContent = getPanelXML(panels);
        String sectionIds = getSectionIds(screens);
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        File userFolder = new File(pathConfig.userFolder(userService.getAccount()));
        if (!userFolder.exists()) {
            boolean success = userFolder.mkdirs();
            if (!success) {
                throw new FileOperationException("Failed to create directory path to user folder '" + userFolder + "'.");
            }
        }
        File defaultImage = new File(pathConfig.getWebRootFolder() + UIImage.DEFAULT_IMAGE_URL);
        FileUtilsExt.copyFile(defaultImage, new File(userFolder, defaultImage.getName()));
        File panelXMLFile = new File(pathConfig.panelXmlFilePath(userService.getAccount()));
        File controllerXMLFile = new File(pathConfig.controllerXmlFilePath(userService.getAccount()));
        File lircdFile = new File(pathConfig.lircFilePath(userService.getAccount()));
        String newIphoneXML = XmlParser.validateAndOutputXML(new File(getClass().getResource(configuration.getPanelXsdPath()).getPath()), panelXmlContent, userFolder);
        controllerXmlContent = XmlParser.validateAndOutputXML(new File(getClass().getResource(configuration.getControllerXsdPath()).getPath()), controllerXmlContent);
        try {
            FileUtilsExt.deleteQuietly(panelXMLFile);
            FileUtilsExt.deleteQuietly(controllerXMLFile);
            FileUtilsExt.deleteQuietly(lircdFile);
            FileUtilsExt.writeStringToFile(panelXMLFile, newIphoneXML);
            FileUtilsExt.writeStringToFile(controllerXMLFile, controllerXmlContent);
            if (sectionIds != null && !sectionIds.equals("")) {
                FileUtils.copyURLToFile(buildLircRESTUrl(configuration.getBeehiveLircdConfRESTUrl(), sectionIds), lircdFile);
            }
            if (lircdFile.exists() && lircdFile.length() == 0) {
                boolean success = lircdFile.delete();
                if (!success) {
                    LOGGER.error("Failed to delete '" + lircdFile + "'.");
                }
            }
        } catch (IOException e) {
            throw new FileOperationException("Failed to write resource: " + e.getMessage(), e);
        }
    }

    private void serializePanelsAndMaxOid(Collection<Panel> panels, long maxOid) {
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        File panelsObjFile = new File(pathConfig.getSerializedPanelsFile(userService.getAccount()));
        ObjectOutputStream oos = null;
        try {
            FileUtilsExt.deleteQuietly(panelsObjFile);
            if (panels == null || panels.size() < 1) {
                return;
            }
            oos = new ObjectOutputStream(new FileOutputStream(panelsObjFile));
            oos.writeObject(panels);
            oos.writeLong(maxOid);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to close output stream to '" + panelsObjFile + "'.");
            }
        }
    }

    public PanelsAndMaxOid restore() {
        try {
            downloadOpenRemoteZip();
        } catch (IOException e) {
            LOGGER.error("Beehive no available !" + e.getMessage());
        }
        return restorePanelsAndMaxOid();
    }

    @SuppressWarnings("unchecked")
    private PanelsAndMaxOid restorePanelsAndMaxOid() {
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        File panelsObjFile = new File(pathConfig.getSerializedPanelsFile(userService.getAccount()));
        if (!panelsObjFile.exists()) {
            return new PanelsAndMaxOid(new ArrayList<Panel>(), 0L);
        }
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(panelsObjFile));
            Collection<Panel> panels = (Collection<Panel>) ois.readObject();
            long maxOid = ois.readLong();
            return new PanelsAndMaxOid(panels, maxOid);
        } catch (Exception e) {
            throw new UIRestoreException("UIDesigner restore failed, incompatible data can not be restore to panels. ", e);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException ioException) {
                LOGGER.warn("Failed to close input stream from '" + panelsObjFile + "': " + ioException.getMessage(), ioException);
            }
        }
    }

    @SuppressWarnings("all")
    public void saveResourcesToBeehive(Collection<Panel> panels) {
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost();
        String beehiveRootRestURL = configuration.getBeehiveRESTRootUrl();
        this.addAuthentication(httpPost);
        String url = beehiveRootRestURL + "account/" + userService.getAccount().getOid() + "/openremote.zip";
        try {
            httpPost.setURI(new URI(url));
            FileBody resource = new FileBody(getExportResource(panels));
            MultipartEntity entity = new MultipartEntity();
            entity.addPart("resource", resource);
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            if (200 != response.getStatusLine().getStatusCode()) {
                throw new BeehiveNotAvailableException("Failed to save resource to Beehive, status code: " + response.getStatusLine().getStatusCode());
            }
        } catch (NullPointerException e) {
            LOGGER.warn("There no resource to upload to beehive at this time. ");
        } catch (IOException e) {
            throw new BeehiveNotAvailableException(e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new IllegalRestUrlException("Invalid Rest URL: " + url + " to save modeler resource to beehive! ", e);
        }
    }

    public void saveTemplateResourcesToBeehive(Template template) {
        boolean share = template.getShareTo() == Template.PUBLIC;
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost();
        String beehiveRootRestURL = configuration.getBeehiveRESTRootUrl();
        String url = "";
        if (!share) {
            url = beehiveRootRestURL + "account/" + userService.getAccount().getOid() + "/template/" + template.getOid() + "/resource/";
        } else {
            url = beehiveRootRestURL + "account/0/template/" + template.getOid() + "/resource/";
        }
        try {
            httpPost.setURI(new URI(url));
            File templateZip = getTemplateZipResource(template);
            if (templateZip == null) {
                LOGGER.warn("There are no template resources for template \"" + template.getName() + "\"to save to beehive!");
                return;
            }
            FileBody resource = new FileBody(templateZip);
            MultipartEntity entity = new MultipartEntity();
            entity.addPart("resource", resource);
            this.addAuthentication(httpPost);
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            if (200 != response.getStatusLine().getStatusCode()) {
                throw new BeehiveNotAvailableException("Failed to save template to Beehive, status code: " + response.getStatusLine().getStatusCode());
            }
        } catch (NullPointerException e) {
            LOGGER.warn("There are no template resources for template \"" + template.getName() + "\"to save to beehive!");
        } catch (IOException e) {
            throw new BeehiveNotAvailableException("Failed to save template to Beehive", e);
        } catch (URISyntaxException e) {
            throw new IllegalRestUrlException("Invalid Rest URL: " + url + " to save template resource to beehive! ", e);
        }
    }

    @Override
    public boolean canRestore() {
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        File panelsObjFile = new File(pathConfig.getSerializedPanelsFile(userService.getAccount()));
        return panelsObjFile.exists();
    }

    @Override
    public void downloadResourcesForTemplate(long templateOid) {
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(configuration.getBeehiveRESTRootUrl() + "account/" + userService.getAccount().getOid() + "/template/" + templateOid + "/resource");
        InputStream inputStream = null;
        FileOutputStream fos = null;
        this.addAuthentication(httpGet);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (200 == response.getStatusLine().getStatusCode()) {
                inputStream = response.getEntity().getContent();
                File userFolder = new File(pathConfig.userFolder(userService.getAccount()));
                if (!userFolder.exists()) {
                    boolean success = userFolder.mkdirs();
                    if (!success) {
                        throw new FileOperationException("Unable to create directories for path '" + userFolder + "'.");
                    }
                }
                File outPut = new File(userFolder, "template.zip");
                FileUtilsExt.deleteQuietly(outPut);
                fos = new FileOutputStream(outPut);
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                ZipUtils.unzip(outPut, pathConfig.userFolder(userService.getAccount()));
                FileUtilsExt.deleteQuietly(outPut);
            } else if (404 == response.getStatusLine().getStatusCode()) {
                LOGGER.warn("There are no resources for this template, ID:" + templateOid);
                return;
            } else {
                throw new BeehiveNotAvailableException("Failed to download resources for template, status code: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException ioException) {
            throw new BeehiveNotAvailableException("I/O exception in handling user's template.zip file: " + ioException.getMessage(), ioException);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioException) {
                    LOGGER.warn("Failed to close input stream from '" + httpGet.getURI() + "': " + ioException.getMessage(), ioException);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioException) {
                    LOGGER.warn("Failed to close file output stream to user's template.zip file: " + ioException.getMessage(), ioException);
                }
            }
        }
    }

    private void downloadOpenRemoteZip() throws IOException {
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(configuration.getBeehiveRESTRootUrl() + "user/" + userService.getCurrentUser().getUsername() + "/openremote.zip");
        LOGGER.debug("Attempting to fetch account configuration from: " + httpGet.getURI());
        InputStream inputStream = null;
        FileOutputStream fos = null;
        try {
            this.addAuthentication(httpGet);
            HttpResponse response = httpClient.execute(httpGet);
            if (HttpServletResponse.SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
                LOGGER.warn("Failed to download openremote.zip for user " + userService.getCurrentUser().getUsername() + " from Beehive. Status code is 404. Will try to restore panels from local resource! ");
                return;
            }
            if (200 == response.getStatusLine().getStatusCode()) {
                inputStream = response.getEntity().getContent();
                File userFolder = new File(pathConfig.userFolder(userService.getAccount()));
                if (!userFolder.exists()) {
                    boolean success = userFolder.mkdirs();
                    if (!success) {
                        throw new FileOperationException("Failed to create the required directories for path '" + userFolder + "'.");
                    }
                }
                File outPut = new File(userFolder, "openremote.zip");
                FileUtilsExt.deleteQuietly(outPut);
                fos = new FileOutputStream(outPut);
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                ZipUtils.unzip(outPut, pathConfig.userFolder(userService.getAccount()));
                FileUtilsExt.deleteQuietly(outPut);
            } else {
                throw new BeehiveNotAvailableException("Failed to download resources, status code: " + response.getStatusLine().getStatusCode());
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioException) {
                    LOGGER.warn("Failed to close input stream from " + httpGet.getURI() + " (" + ioException.getMessage() + ")", ioException);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioException) {
                    LOGGER.warn("Failed to close output stream to user's openremote.zip file: " + ioException.getMessage());
                }
            }
        }
    }

    @Override
    public File getTemplateResource(Template template) {
        return this.getTemplateZipResource(template);
    }

    private File getResourceZipFile(List<String> ignoreExtentions, Collection<File> compresseFiles) {
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        File userFolder = new File(pathConfig.userFolder(userService.getAccount()));
        File[] filesInAccountFolder = userFolder.listFiles();
        if (filesInAccountFolder == null || filesInAccountFolder.length == 0) {
            return null;
        }
        File[] filesInZip = null;
        if (compresseFiles != null && compresseFiles.size() > 0) {
            filesInZip = new File[compresseFiles.size()];
        } else {
            filesInZip = new File[1];
        }
        int i = 0;
        for (File file : compresseFiles) {
            if (file.exists()) {
                filesInZip[i++] = file;
            } else {
                throw new ResourceFileLostException("File '" + file.getName() + "' is lost! ");
            }
        }
        return compressFilesToZip(filesInZip, pathConfig.openremoteZipFilePath(userService.getAccount()), ignoreExtentions);
    }

    private File getTemplateZipResource(Template template) {
        List<String> ignoreExtentions = new ArrayList<String>();
        ignoreExtentions.add("zip");
        ignoreExtentions.add("xml");
        ScreenPair sp = template.getScreen();
        Collection<ImageSource> images = sp.getAllImageSources();
        Collection<File> templateRelatedFiles = getAllImageFiles(images);
        if (templateRelatedFiles.size() == 0) return null;
        return getResourceZipFile(ignoreExtentions, templateRelatedFiles);
    }

    private File getExportResource(Collection<Panel> panels) {
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        List<String> ignoreExtentions = new ArrayList<String>();
        Collection<ImageSource> images = getAllImageSources(panels);
        Collection<File> imageFiles = getAllImageFiles(images);
        File panelXMLFile = new File(pathConfig.panelXmlFilePath(userService.getAccount()));
        File controllerXMLFile = new File(pathConfig.controllerXmlFilePath(userService.getAccount()));
        File panelsObjFile = new File(pathConfig.getSerializedPanelsFile(userService.getAccount()));
        File lircdFile = new File(pathConfig.lircFilePath(userService.getAccount()));
        Collection<File> exportFile = new HashSet<File>();
        exportFile.addAll(imageFiles);
        exportFile.add(panelXMLFile);
        exportFile.add(controllerXMLFile);
        exportFile.add(panelsObjFile);
        if (lircdFile.exists()) {
            exportFile.add(lircdFile);
        }
        ignoreExtentions.add("zip");
        return getResourceZipFile(ignoreExtentions, exportFile);
    }

    private String encode(String namePassword) {
        if (namePassword == null) return null;
        return new String(Base64.encodeBase64(namePassword.getBytes()));
    }

    private Set<File> getAllImageFiles(Collection<ImageSource> images) {
        Set<File> files = new HashSet<File>();
        PathConfig pathConfig = PathConfig.getInstance(configuration);
        File userFolder = new File(pathConfig.userFolder(userService.getAccount()));
        for (ImageSource image : images) {
            String fileName = image.getImageFileName();
            File imageFile = new File(userFolder, fileName);
            if (imageFile.exists()) {
                files.add(imageFile);
            } else {
                throw new ResourceFileLostException("File '" + imageFile.getName() + "' is lost!");
            }
        }
        return files;
    }

    private Set<ImageSource> getAllImageSources(Collection<Panel> panels) {
        Set<ImageSource> imageSources = new HashSet<ImageSource>();
        if (panels != null & panels.size() > 0) {
            for (Panel panel : panels) {
                List<GroupRef> groupRefs = panel.getGroupRefs();
                if (groupRefs != null && groupRefs.size() > 0) {
                    for (GroupRef groupRef : groupRefs) {
                        Group group = groupRef.getGroup();
                        if (group.getTabbar() != null) {
                            imageSources.addAll(getImageSourcesFromTabbar(group.getTabbar()));
                        }
                        List<ScreenPairRef> screenPairRefs = group.getScreenRefs();
                        if (screenPairRefs != null && screenPairRefs.size() > 0) {
                            for (ScreenPairRef screenPairRef : screenPairRefs) {
                                ScreenPair screenPair = screenPairRef.getScreen();
                                if (OrientationType.PORTRAIT.equals(screenPair.getOrientation())) {
                                    imageSources.addAll(screenPair.getPortraitScreen().getAllImageSources());
                                } else if (OrientationType.LANDSCAPE.equals(screenPair.getOrientation())) {
                                    imageSources.addAll(screenPair.getLandscapeScreen().getAllImageSources());
                                } else if (OrientationType.BOTH.equals(screenPair.getOrientation())) {
                                    imageSources.addAll(screenPair.getPortraitScreen().getAllImageSources());
                                    imageSources.addAll(screenPair.getLandscapeScreen().getAllImageSources());
                                }
                            }
                        }
                    }
                }
                if (panel.getTabbar() != null) {
                    imageSources.addAll(getImageSourcesFromTabbar(panel.getTabbar()));
                }
                if (Constants.CUSTOM_PANEL.equals(panel.getType())) {
                    imageSources.addAll(getCustomPanelImages(panel));
                }
            }
        }
        return imageSources;
    }

    private Collection<ImageSource> getImageSourcesFromTabbar(UITabbar uiTabbar) {
        Collection<ImageSource> imageSources = new ArrayList<ImageSource>(5);
        if (uiTabbar != null) {
            List<UITabbarItem> uiTabbarItems = uiTabbar.getTabbarItems();
            if (uiTabbarItems != null && uiTabbarItems.size() > 0) {
                for (UITabbarItem item : uiTabbarItems) {
                    ImageSource image = item.getImage();
                    if (image != null && !image.isEmpty()) {
                        imageSources.add(image);
                    }
                }
            }
        }
        return imageSources;
    }

    private Collection<ImageSource> getCustomPanelImages(Panel panel) {
        Collection<ImageSource> images = new ArrayList<ImageSource>(2);
        if (panel != null) {
            ImageSource vBImage = new ImageSource(panel.getTouchPanelDefinition().getBgImage());
            ImageSource hBgImage = new ImageSource(panel.getTouchPanelDefinition().getHorizontalDefinition().getBgImage());
            ImageSource tbImage = panel.getTouchPanelDefinition().getTabbarDefinition().getBackground();
            if (!vBImage.isEmpty()) {
                images.add(vBImage);
            }
            if (!hBgImage.isEmpty()) {
                images.add(hBgImage);
            }
            if (tbImage != null && !tbImage.isEmpty() && !TouchPanelTabbarDefinition.IPHONE_TABBAR_BACKGROUND.equals(tbImage.getSrc())) {
                images.add(tbImage);
            }
        }
        return images;
    }

    static class MaxId {

        Long maxId = 0L;

        public MaxId(Long maxId) {
            this.maxId = maxId;
        }

        public Long maxId() {
            return maxId++;
        }
    }

    private void addAuthentication(AbstractHttpMessage httpMessage) {
        httpMessage.setHeader(Constants.HTTP_BASIC_AUTH_HEADER_NAME, Constants.HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX + encode(userService.getCurrentUser().getUsername() + ":" + userService.getCurrentUser().getPassword()));
    }
}
