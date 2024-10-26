package org.openremote.modeler.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.openremote.modeler.client.Configuration;
import org.openremote.modeler.client.Constants;
import org.openremote.modeler.client.utils.ScreenFromTemplate;
import org.openremote.modeler.client.utils.SensorLink;
import org.openremote.modeler.domain.Absolute;
import org.openremote.modeler.domain.Account;
import org.openremote.modeler.domain.Cell;
import org.openremote.modeler.domain.ControllerConfig;
import org.openremote.modeler.domain.Device;
import org.openremote.modeler.domain.DeviceCommand;
import org.openremote.modeler.domain.DeviceCommandRef;
import org.openremote.modeler.domain.DeviceMacro;
import org.openremote.modeler.domain.DeviceMacroItem;
import org.openremote.modeler.domain.DeviceMacroRef;
import org.openremote.modeler.domain.Protocol;
import org.openremote.modeler.domain.ProtocolAttr;
import org.openremote.modeler.domain.Role;
import org.openremote.modeler.domain.Screen;
import org.openremote.modeler.domain.ScreenPair;
import org.openremote.modeler.domain.Sensor;
import org.openremote.modeler.domain.Slider;
import org.openremote.modeler.domain.Switch;
import org.openremote.modeler.domain.Template;
import org.openremote.modeler.domain.UICommand;
import org.openremote.modeler.domain.User;
import org.openremote.modeler.domain.ScreenPair.OrientationType;
import org.openremote.modeler.domain.component.Gesture;
import org.openremote.modeler.domain.component.ImageSource;
import org.openremote.modeler.domain.component.Navigate;
import org.openremote.modeler.domain.component.SensorOwner;
import org.openremote.modeler.domain.component.UIButton;
import org.openremote.modeler.domain.component.UIComponent;
import org.openremote.modeler.domain.component.UIGrid;
import org.openremote.modeler.domain.component.UIImage;
import org.openremote.modeler.domain.component.UILabel;
import org.openremote.modeler.domain.component.UISlider;
import org.openremote.modeler.domain.component.UISwitch;
import org.openremote.modeler.exception.BeehiveNotAvailableException;
import org.openremote.modeler.exception.NotAuthenticatedException;
import org.openremote.modeler.service.DeviceCommandService;
import org.openremote.modeler.service.DeviceMacroService;
import org.openremote.modeler.service.DeviceService;
import org.openremote.modeler.service.ResourceService;
import org.openremote.modeler.service.SensorService;
import org.openremote.modeler.service.SliderService;
import org.openremote.modeler.service.SwitchService;
import org.openremote.modeler.service.TemplateService;
import org.openremote.modeler.service.UserService;
import org.openremote.modeler.utils.UIComponentBox;
import flexjson.ClassLocator;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import flexjson.Path;

/**
 * 
 * @author javen
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 *
 */
public class TemplateServiceImpl implements TemplateService {

    private static Logger log = Logger.getLogger(TemplateServiceImpl.class);

    ;

    private Configuration configuration;

    private UserService userService;

    private ResourceService resourceService;

    private DeviceService deviceService;

    private DeviceCommandService deviceCommandService;

    private SwitchService switchService;

    private SliderService sliderService;

    private SensorService sensorService;

    private DeviceMacroService deviceMacroService;

    @Override
    public Template saveTemplate(Template screenTemplate) {
        log.debug("save Template Name: " + screenTemplate.getName());
        screenTemplate.setContent(getTemplateContent(screenTemplate.getScreen()));
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("name", screenTemplate.getName()));
        params.add(new BasicNameValuePair("content", screenTemplate.getContent()));
        params.add(new BasicNameValuePair("shared", screenTemplate.isShared() + ""));
        params.add(new BasicNameValuePair("keywords", screenTemplate.getKeywords()));
        log.debug("TemplateContent" + screenTemplate.getContent());
        try {
            String saveRestUrl = configuration.getBeehiveRESTRootUrl() + "account/" + userService.getAccount().getOid() + "/template/";
            HttpPost httpPost = new HttpPost(saveRestUrl);
            addAuthentication(httpPost);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(formEntity);
            HttpClient httpClient = new DefaultHttpClient();
            String result = httpClient.execute(httpPost, new ResponseHandler<String>() {

                @Override
                public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
                    BufferedReader buffReader = new BufferedReader(reader);
                    StringBuilder sb = new StringBuilder();
                    String line = "";
                    while ((line = buffReader.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    return sb.toString();
                }
            });
            if (result.indexOf("<id>") != -1 && result.indexOf("</id>") != -1) {
                long templateOid = Long.parseLong(result.substring(result.indexOf("<id>") + "<id>".length(), result.indexOf("</id>")));
                screenTemplate.setOid(templateOid);
                resourceService.saveTemplateResourcesToBeehive(screenTemplate);
            } else {
                throw new BeehiveNotAvailableException();
            }
        } catch (Exception e) {
            throw new BeehiveNotAvailableException("Failed to save screen as a template: " + (e.getMessage() == null ? "" : e.getMessage()), e);
        }
        log.debug("save Template Ok!");
        return screenTemplate;
    }

    public String getTemplateContent(ScreenPair screen) {
        try {
            String[] includedPropertyNames = { "*.gestures.uiCommand", "*.absolutes.uiComponent.sensorLink", "*.absolutes.uiComponent.oid", "*.grids.cells.uiComponent.sensorLink", "*.grids.cells.uiComponent.oid", "*.absolutes.uiComponent.uiCommand.deviceCommand.protocol.protocalAttrs", "*.absolutes.uiComponent.commands", "*.absolutes.uiComponent.slider.sliderSensorRef.sensor", "*.absolutes.uiComponent.switchCommand.switchSensorRef.sensor", "*.grids.cells.uiComponent", "*.grids.cells.uiComponent.slider.sliderSensorRef.sensor", "*.grids.cells.uiComponent.switchCommand.switchSensorRef.sensor", "*.grids.cells.uiComponent.uiCommand", "*.grids.cells.uiComponent.uiCommand.deviceCommand.protocol.protocalAttrs", "*.grids.cells.uiComponent.commands", "*.deviceCommand", "*.protocol", "*.attributes" };
            String[] excludePropertyNames = { "grid", "*.refCount", "*.displayName", "*.oid", "*.proxyInformations", "*.proxyInformation", "*.panelXml", "*.deviceCommands", "*.sensors", "*.sliders", "*.configs", "*.switchs", "DeviceMacros" };
            return new JSONSerializer().include(includedPropertyNames).exclude(excludePropertyNames).deepSerialize(screen);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    @Override
    public ScreenFromTemplate buildFromTemplate(Template template) {
        ScreenPair screen = buildScreen(template);
        resetImageSourceLocationForScreen(screen);
        resourceService.downloadResourcesForTemplate(template.getOid());
        return reBuildCommand(screen);
    }

    @Override
    public ScreenPair buildScreen(Template template) {
        String screenJson = template.getContent();
        ScreenPair screenPair = new JSONDeserializer<ScreenPair>().use(null, ScreenPair.class).use("portraitScreen.absolutes.values.uiComponent", new SimpleClassLocator()).use("portraitScreen.grids.values.cells.values.uiComponent", new SimpleClassLocator()).use("portraitScreen.absolutes.values.uiComponent.uiCommand", new SimpleClassLocator()).use("portraitScreen.gestures.values.uiCommand", new SimpleClassLocator()).use("portraitScreen.absolutes.values.uiComponent.sensor", new SimpleClassLocator()).use("portraitScreen.absolutes.values.uiComponent.slider.sliderSensorRef.sensor", new SimpleClassLocator()).use("portraitScreen.absolutes.values.uiComponent.switchCommand.switchCommandOnRef.sensor", new SimpleClassLocator()).use("portraitScreen.absolutes.values.uiComponent.switchCommand.switchCommandOffRef.sensor", new SimpleClassLocator()).use("portraitScreen.grids.values.cells.values.uiComponent.uiCommand", new SimpleClassLocator()).use("portraitScreen.grids.values.cells.values.uiComponent.sensor", new SimpleClassLocator()).use("portraitScreen.grids.values.cells.values.uiComponent.slider.sliderSensorRef.sensor", new SimpleClassLocator()).use("portraitScreen.grids.values.cells.values.uiComponent.switchCommand.switchCommandOnRef.sensor", new SimpleClassLocator()).use("portraitScreen.grids.values.cells.values.uiComponent.switchCommand.switchCommandOffRef.sensor", new SimpleClassLocator()).use("landscapeScreen.absolutes.values.uiComponent", new SimpleClassLocator()).use("landscapeScreen.grids.values.cells.values.uiComponent", new SimpleClassLocator()).use("landscapeScreen.absolutes.values.uiComponent.uiCommand", new SimpleClassLocator()).use("landscapeScreen.gestures.values.uiCommand", new SimpleClassLocator()).use("landscapeScreen.absolutes.values.uiComponent.sensor", new SimpleClassLocator()).use("landscapeScreen.absolutes.values.uiComponent.slider.sliderSensorRef.sensor", new SimpleClassLocator()).use("landscapeScreen.absolutes.values.uiComponent.switchCommand.switchCommandOnRef.sensor", new SimpleClassLocator()).use("landscapeScreen.absolutes.values.uiComponent.switchCommand.switchCommandOffRef.sensor", new SimpleClassLocator()).use("landscapeScreen.grids.values.cells.values.uiComponent.uiCommand", new SimpleClassLocator()).use("landscapeScreen.grids.values.cells.values.uiComponent.sensor", new SimpleClassLocator()).use("landscapeScreen.grids.values.cells.values.uiComponent.slider.sliderSensorRef.sensor", new SimpleClassLocator()).use("landscapeScreen.grids.values.cells.values.uiComponent.switchCommand.switchCommandOnRef.sensor", new SimpleClassLocator()).use("landscapeScreen.grids.values.cells.values.uiComponent.switchCommand.switchCommandOffRef.sensor", new SimpleClassLocator()).deserialize(screenJson);
        resetGestureForScreenPair(screenPair);
        return screenPair;
    }

    @Override
    public boolean deleteTemplate(long templateOid) {
        log.debug("Delete Template id: " + templateOid);
        String deleteRestUrl = configuration.getBeehiveRESTRootUrl() + "account/" + userService.getAccount().getOid() + "/template/" + templateOid;
        HttpDelete httpDelete = new HttpDelete();
        addAuthentication(httpDelete);
        try {
            httpDelete.setURI(new URI(deleteRestUrl));
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(httpDelete);
            if (200 == response.getStatusLine().getStatusCode()) {
                return true;
            } else {
                throw new BeehiveNotAvailableException("Failed to delete template");
            }
        } catch (Exception e) {
            throw new BeehiveNotAvailableException("Failed to delete template: " + e.getMessage(), e);
        }
    }

    public List<Template> getTemplates(boolean fromPrivate) {
        String shared = fromPrivate ? "private" : "public";
        List<Template> templates = new ArrayList<Template>();
        String restURL = configuration.getBeehiveRESTRootUrl() + "account/" + userService.getAccount().getOid() + "/templates/" + shared;
        HttpGet httpGet = new HttpGet(restURL);
        httpGet.setHeader("Accept", "application/json");
        this.addAuthentication(httpGet);
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
                if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                    throw new NotAuthenticatedException("User " + userService.getCurrentUser().getUsername() + " not authenticated! ");
                }
                throw new BeehiveNotAvailableException("Beehive is not available right now! ");
            }
            InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
            BufferedReader buffReader = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = buffReader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            String result = sb.toString();
            TemplateList templateList = buildTemplateListFromJson(result);
            List<TemplateDTO> dtoes = templateList.getTemplates();
            for (TemplateDTO dto : dtoes) {
                templates.add(dto.toTemplate());
            }
        } catch (IOException e) {
            throw new BeehiveNotAvailableException("Failed to get template list, The beehive is not available right now ", e);
        }
        return templates;
    }

    public List<Template> getTemplatesByKeywordsAndPage(String keywords, int page) {
        String newKeywords = keywords;
        if (keywords == null || keywords.trim().length() == 0) {
            newKeywords = TemplateService.NO_KEYWORDS;
        }
        List<Template> templates = new ArrayList<Template>();
        String restURL = configuration.getBeehiveRESTRootUrl() + "templates/keywords/" + newKeywords + "/page/" + page;
        HttpGet httpGet = new HttpGet(restURL);
        httpGet.setHeader("Accept", "application/json");
        this.addAuthentication(httpGet);
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
                if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                    throw new NotAuthenticatedException("User " + userService.getCurrentUser().getUsername() + " not authenticated! ");
                }
                throw new BeehiveNotAvailableException("Beehive is not available right now! ");
            }
            InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
            BufferedReader buffReader = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = buffReader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            String result = sb.toString();
            TemplateList templateList = buildTemplateListFromJson(result);
            List<TemplateDTO> dtoes = templateList.getTemplates();
            for (TemplateDTO dto : dtoes) {
                templates.add(dto.toTemplate());
            }
        } catch (IOException e) {
            throw new BeehiveNotAvailableException("Failed to get template list, The beehive is not available right now ", e);
        }
        return templates;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ScreenFromTemplate reBuildCommand(ScreenPair screenPair) {
        List<Screen> screens = new ArrayList<Screen>();
        if (OrientationType.PORTRAIT.equals(screenPair.getOrientation())) {
            screens.add(screenPair.getPortraitScreen());
        } else if (OrientationType.LANDSCAPE.equals(screenPair.getOrientation())) {
            screens.add(screenPair.getLandscapeScreen());
        } else if (OrientationType.BOTH.equals(screenPair.getOrientation())) {
            screens.add(screenPair.getPortraitScreen());
            screens.add(screenPair.getLandscapeScreen());
        }
        UIComponentBox box = initUIComponentBox(screens);
        Set<Device> devices = getDevices(screens);
        Set<DeviceCommand> commands = getDeviceCommands(screens);
        Set<Slider> sliders = getSliders((Collection<UISlider>) (box.getUIComponentsByType(UISlider.class)));
        Set<Switch> switchs = getSwitches((Collection<UISwitch>) box.getUIComponentsByType(UISwitch.class));
        Set<UIButton> uiButtons = (Set<UIButton>) box.getUIComponentsByType(UIButton.class);
        Set<Sensor> sensors = getSensors(screens);
        Collection<Gesture> gestures = getGestures(screens);
        Set<DeviceMacro> macros = getMacros(box, gestures);
        this.resetNavigateForButtons(uiButtons);
        rebuild(devices, commands, sensors, switchs, sliders, macros, false);
        return new ScreenFromTemplate(devices, screenPair, macros);
    }

    private static UIComponentBox initUIComponentBox(List<Screen> screens) {
        UIComponentBox box = new UIComponentBox();
        for (Screen screen : screens) {
            for (Absolute absolute : screen.getAbsolutes()) {
                UIComponent component = absolute.getUiComponent();
                box.add(component);
            }
            for (UIGrid grid : screen.getGrids()) {
                for (Cell cell : grid.getCells()) {
                    box.add(cell.getUiComponent());
                }
            }
        }
        return box;
    }

    private Set<DeviceCommand> getDeviceCommands(List<Screen> screens) {
        UIComponentBox box = initUIComponentBox(screens);
        Set<DeviceCommand> uiCmds = new HashSet<DeviceCommand>();
        getDeviceCommandsFromSlider(box, uiCmds);
        getDeviceCommandsFromSwitch(box, uiCmds);
        getDeviceCommandsFromButton(box, uiCmds);
        Collection<Gesture> gestures = getGestures(screens);
        getDeviceCommandsFromGesture(gestures, uiCmds);
        getDeviceCommandsFromSensor(screens, uiCmds);
        return uiCmds;
    }

    private void getDeviceCommandsFromSensor(List<Screen> screens, Set<DeviceCommand> uiCmds) {
        Collection<Sensor> sensors = getSensors(screens);
        for (Sensor sensor : sensors) {
            for (DeviceCommand cmd : uiCmds) {
                if (cmd.equals(sensor.getSensorCommandRef().getDeviceCommand())) {
                    sensor.getSensorCommandRef().setDeviceCommand(cmd);
                    sensor.setDevice(cmd.getDevice());
                }
            }
            uiCmds.add(sensor.getSensorCommandRef().getDeviceCommand());
        }
    }

    @SuppressWarnings("unchecked")
    private void getDeviceCommandsFromButton(UIComponentBox box, Set<DeviceCommand> uiCmds) {
        Collection<UIButton> buttons = (Collection<UIButton>) box.getUIComponentsByType(UIButton.class);
        for (UIButton btn : buttons) {
            UICommand cmd = btn.getUiCommand();
            if (cmd != null) {
                if (cmd instanceof DeviceCommandRef) {
                    DeviceCommandRef cmdRef = (DeviceCommandRef) cmd;
                    for (DeviceCommand tmpCmd : uiCmds) {
                        if (tmpCmd.equals(cmdRef.getDeviceCommand())) {
                            cmdRef.setDeviceCommand(tmpCmd);
                        }
                    }
                    uiCmds.add(cmdRef.getDeviceCommand());
                } else if (cmd instanceof DeviceMacroRef) {
                    DeviceMacroRef macroRef = (DeviceMacroRef) cmd;
                    DeviceMacro macro = macroRef.getTargetDeviceMacro();
                    if (macro != null) {
                        Collection<DeviceCommandRef> cmds = getDeviceCommandsRefsFromMacro(macro);
                        for (DeviceCommandRef cmdFromMacro : cmds) {
                            for (DeviceCommand cmdInCommandSet : uiCmds) {
                                if (cmdFromMacro.getDeviceCommand().equals(cmdInCommandSet)) {
                                    cmdFromMacro.setDeviceCommand(cmdInCommandSet);
                                }
                            }
                            uiCmds.add(cmdFromMacro.getDeviceCommand());
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void getDeviceCommandsFromSwitch(UIComponentBox box, Set<DeviceCommand> uiCmds) {
        Collection<Switch> switchs = getSwitches((Collection<UISwitch>) box.getUIComponentsByType(UISwitch.class));
        for (Switch switchToggle : switchs) {
            DeviceCommand onCmd = switchToggle.getSwitchCommandOnRef().getDeviceCommand();
            for (DeviceCommand cmd : uiCmds) {
                if (cmd.equals(onCmd)) {
                    switchToggle.getSwitchCommandOnRef().setDeviceCommand(cmd);
                }
            }
            uiCmds.add(onCmd);
            DeviceCommand offCmd = switchToggle.getSwitchCommandOffRef().getDeviceCommand();
            for (DeviceCommand cmd : uiCmds) {
                if (cmd.equals(offCmd)) {
                    switchToggle.getSwitchCommandOffRef().setDeviceCommand(cmd);
                }
            }
            uiCmds.add(offCmd);
        }
    }

    @SuppressWarnings("unchecked")
    private void getDeviceCommandsFromSlider(UIComponentBox box, Set<DeviceCommand> uiCmds) {
        Collection<Slider> sliders = getSliders((Collection<UISlider>) box.getUIComponentsByType(UISlider.class));
        for (Slider slider : sliders) {
            for (DeviceCommand cmd : uiCmds) {
                if (cmd.equals(slider.getSetValueCmd().getDeviceCommand())) {
                    slider.getSetValueCmd().setDeviceCommand(cmd);
                }
            }
            uiCmds.add(slider.getSetValueCmd().getDeviceCommand());
        }
    }

    private Set<Device> getDevices(List<Screen> screens) {
        Set<Device> devices = new HashSet<Device>();
        Collection<DeviceCommand> deviceCmds = getDeviceCommands(screens);
        for (DeviceCommand cmd : deviceCmds) {
            Device device = cmd.getDevice();
            if (devices.contains(device)) {
                for (Device dvc : devices) {
                    if (dvc.equals(device)) {
                        cmd.setDevice(dvc);
                    }
                }
            }
            devices.add(device);
        }
        return devices;
    }

    private Set<Slider> getSliders(Collection<UISlider> uiSliders) {
        Set<Slider> sliders = new HashSet<Slider>();
        for (UISlider uiSlider : uiSliders) {
            Slider slider = uiSlider.getSlider();
            if (slider != null) {
                for (Slider sld : sliders) {
                    if (slider.equals(sld)) {
                        uiSlider.setSlider(sld);
                    }
                }
                sliders.add(slider);
            }
        }
        return sliders;
    }

    private Set<Switch> getSwitches(Collection<UISwitch> uiSwitchs) {
        Set<Switch> switches = new HashSet<Switch>();
        for (UISwitch uiSwitch : uiSwitchs) {
            Switch switchToggle = uiSwitch.getSwitchCommand();
            if (switchToggle != null) {
                for (Switch swh : switches) {
                    if (switchToggle.equals(swh)) {
                        uiSwitch.setSwitchCommand(swh);
                    }
                }
                switches.add(switchToggle);
            }
        }
        return switches;
    }

    private Set<Sensor> getSensors(List<Screen> screens) {
        Set<Sensor> sensors = new HashSet<Sensor>();
        for (Screen screen : screens) {
            for (Absolute absolute : screen.getAbsolutes()) {
                UIComponent component = absolute.getUiComponent();
                if (component instanceof SensorOwner) {
                    SensorOwner sensorOwner = (SensorOwner) component;
                    Sensor s = sensorOwner.getSensor();
                    if (s != null) {
                        for (Sensor sensor : sensors) {
                            if (sensor.equals(sensorOwner.getSensor())) {
                                sensorOwner.setSensor(sensor);
                            }
                        }
                        initSensorLinker(component, sensorOwner);
                        sensors.add(s);
                    }
                }
            }
            for (UIGrid grid : screen.getGrids()) {
                for (Cell cell : grid.getCells()) {
                    UIComponent component = cell.getUiComponent();
                    if (component instanceof SensorOwner) {
                        SensorOwner sensorOwner = (SensorOwner) component;
                        for (Sensor sensor : sensors) {
                            if (sensor.equals(sensorOwner.getSensor())) {
                                sensorOwner.setSensor(sensor);
                            }
                        }
                        initSensorLinker(component, sensorOwner);
                        sensors.add(sensorOwner.getSensor());
                    }
                }
            }
        }
        return sensors;
    }

    private void initSensorLinker(UIComponent component, SensorOwner sensorOwner) {
        if (component != null) {
            if (component instanceof UILabel) {
                UILabel uiLabel = (UILabel) component;
                if (uiLabel.getSensorLink() == null) {
                    uiLabel.setSensorLink(new SensorLink(sensorOwner.getSensor()));
                }
                uiLabel.getSensorLink().setSensor(sensorOwner.getSensor());
            } else if (component instanceof UIImage) {
                UIImage uiImage = (UIImage) component;
                if (uiImage.getSensorLink() == null) {
                    uiImage.setSensorLink(new SensorLink(sensorOwner.getSensor()));
                }
                uiImage.getSensorLink().setSensor(sensorOwner.getSensor());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<DeviceMacro> getMacros(UIComponentBox box, Collection<Gesture> gestures) {
        Set<DeviceMacro> macros = new HashSet<DeviceMacro>();
        Collection<UIButton> uiButtons = (Collection<UIButton>) box.getUIComponentsByType(UIButton.class);
        for (UIButton btn : uiButtons) {
            if (btn.getUiCommand() instanceof DeviceMacroRef) {
                DeviceMacroRef macroRef = (DeviceMacroRef) btn.getUiCommand();
                if (macroRef.getTargetDeviceMacro() != null) {
                    DeviceMacro macro = macroRef.getTargetDeviceMacro();
                    macros.add(macro);
                    macros.addAll(macro.getSubMacros());
                }
            }
        }
        if (gestures != null && gestures.size() > 0) {
            for (Gesture gesture : gestures) {
                if (gesture.getUiCommand() != null && gesture.getUiCommand() instanceof DeviceMacroRef) {
                    DeviceMacroRef macroRef = (DeviceMacroRef) gesture.getUiCommand();
                    if (macroRef.getTargetDeviceMacro() != null) {
                        DeviceMacro macro = macroRef.getTargetDeviceMacro();
                        macros.add(macro);
                        macros.addAll(macro.getSubMacros());
                    }
                }
            }
        }
        return macros;
    }

    private Collection<DeviceCommandRef> getDeviceCommandsRefsFromMacro(DeviceMacro deviceMacro) {
        Collection<DeviceCommandRef> deviceCommands = new ArrayList<DeviceCommandRef>();
        if (deviceMacro != null) {
            List<DeviceMacroItem> macroRefs = deviceMacro.getDeviceMacroItems();
            if (macroRefs != null && macroRefs.size() > 0) {
                for (DeviceMacroItem macroItem : macroRefs) {
                    if (macroItem instanceof DeviceCommandRef) {
                        DeviceCommandRef cmdRef = (DeviceCommandRef) macroItem;
                        deviceCommands.add(cmdRef);
                    } else if (macroItem instanceof DeviceMacroRef) {
                        DeviceMacroRef macroRef = (DeviceMacroRef) macroItem;
                        if (macroRef.getTargetDeviceMacro() != null) {
                            Collection<DeviceCommandRef> cmds = getDeviceCommandsRefsFromMacro(macroRef.getTargetDeviceMacro());
                            deviceCommands.addAll(cmds);
                        }
                    }
                }
            }
        }
        return deviceCommands;
    }

    private boolean rebuild(Collection<Device> devices, Collection<DeviceCommand> deviceCommands, Collection<Sensor> sensors, Collection<Switch> switches, Collection<Slider> sliders, Collection<DeviceMacro> macros, boolean createNew) {
        boolean isHasNewCmd = false;
        Account account = userService.getAccount();
        for (Device device : devices) {
            device.setAccount(account);
            List<Device> sameDevices = deviceService.loadSameDevices(device);
            if (!createNew) {
                if (sameDevices != null && sameDevices.size() > 0) {
                    device.setOid(sameDevices.get(0).getOid());
                } else {
                    deviceService.saveDevice(device);
                    isHasNewCmd = true;
                }
            } else {
                deviceService.saveDevice(device);
                isHasNewCmd = true;
            }
        }
        for (DeviceCommand deviceCommand : deviceCommands) {
            Protocol protocol = deviceCommand.getProtocol();
            if (protocol.getAttributes() != null) {
                for (ProtocolAttr attr : protocol.getAttributes()) {
                    attr.setProtocol(protocol);
                }
            }
            if (!createNew) {
                List<DeviceCommand> sameCmds = deviceCommandService.loadSameCommands(deviceCommand);
                if (sameCmds != null && sameCmds.size() > 0) {
                    deviceCommand.setOid(sameCmds.get(0).getOid());
                } else {
                    deviceCommandService.save(deviceCommand);
                    isHasNewCmd = true;
                }
            } else {
                deviceCommandService.save(deviceCommand);
                isHasNewCmd = true;
            }
        }
        for (Sensor sensor : sensors) {
            sensor.setAccount(account);
            sensor.getSensorCommandRef().setSensor(sensor);
            sensor.setDevice(sensor.getSensorCommandRef().getDeviceCommand().getDevice());
            if (!createNew) {
                List<Sensor> sameSensors = sensorService.loadSameSensors(sensor);
                if (sameSensors != null && sameSensors.size() > 0) {
                    sensor.setOid(sameSensors.get(0).getOid());
                } else {
                    sensorService.saveSensor(sensor);
                    isHasNewCmd = true;
                }
            } else {
                sensorService.saveSensor(sensor);
                isHasNewCmd = true;
            }
        }
        for (Switch switchToggle : switches) {
            switchToggle.setAccount(account);
            switchToggle.getSwitchCommandOffRef().setOffSwitch(switchToggle);
            switchToggle.getSwitchCommandOnRef().setOnSwitch(switchToggle);
            switchToggle.setDevice(switchToggle.getSwitchCommandOffRef().getDeviceCommand().getDevice());
            switchToggle.getSwitchSensorRef().setSwitchToggle(switchToggle);
            if (!createNew) {
                List<Switch> swhs = switchService.loadSameSwitchs(switchToggle);
                if (swhs != null && swhs.size() > 0) {
                    switchToggle.setOid(swhs.get(0).getOid());
                } else {
                    switchService.save(switchToggle);
                }
            } else {
                switchService.save(switchToggle);
            }
        }
        for (Slider slider : sliders) {
            slider.setAccount(account);
            slider.setDevice(slider.getSetValueCmd().getDeviceCommand().getDevice());
            slider.getSliderSensorRef().setSlider(slider);
            slider.getSetValueCmd().setSlider(slider);
            if (!createNew) {
                List<Slider> sameSliders = sliderService.loadSameSliders(slider);
                if (sameSliders != null && sameSliders.size() > 0) {
                    slider.setOid(sameSliders.get(0).getOid());
                } else {
                    sliderService.save(slider);
                }
            } else {
                sliderService.save(slider);
            }
        }
        for (DeviceMacro macro : macros) {
            macro.setAccount(account);
            saveMacro(macro, createNew);
        }
        prepareToSendToClient(devices, deviceCommands, macros, account);
        return isHasNewCmd;
    }

    private void prepareToSendToClient(Collection<Device> devices, Collection<DeviceCommand> deviceCommands, Collection<DeviceMacro> macros, Account account) {
        for (DeviceCommand deviceCommand : deviceCommands) {
            Protocol protocol = deviceCommand.getProtocol();
            if (protocol.getAttributes() != null) {
                for (ProtocolAttr attr : protocol.getAttributes()) {
                    attr.setProtocol(protocol);
                }
            }
            List<ProtocolAttr> attrs = new ArrayList<ProtocolAttr>();
            for (ProtocolAttr attr : protocol.getAttributes()) {
                attrs.add(attr);
            }
            deviceCommand.getProtocol().setAttributes(attrs);
        }
        account.setConfigs(new ArrayList<ControllerConfig>());
        account.setDeviceMacros(new ArrayList<DeviceMacro>());
        account.setSensors(new ArrayList<Sensor>());
        account.setSliders(new ArrayList<Slider>());
        account.setSwitches(new ArrayList<Switch>());
        account.setDevices(new ArrayList<Device>());
        account.setUsers(new ArrayList<User>());
        userService.getCurrentUser().setRoles(new ArrayList<Role>());
        for (Device device : devices) {
            device.setAccount(null);
            device.setSensors(new ArrayList<Sensor>());
            device.setSwitchs(new ArrayList<Switch>());
            device.setSliders(new ArrayList<Slider>());
            device.setDeviceCommands(new ArrayList<DeviceCommand>());
        }
        for (DeviceMacro macro : macros) {
            macro.setAccount(null);
            List<DeviceMacroItem> items = new ArrayList<DeviceMacroItem>();
            for (DeviceMacroItem item : macro.getDeviceMacroItems()) {
                items.add(item);
            }
            macro.setDeviceMacroItems(items);
        }
    }

    private void saveMacro(DeviceMacro macro, boolean createNew) {
        if (null != macro) {
            List<DeviceMacroItem> items = macro.getDeviceMacroItems();
            if (null != items) {
                for (DeviceMacroItem item : items) {
                    if (item instanceof DeviceMacroRef) {
                        DeviceMacroRef macroRef = (DeviceMacroRef) item;
                        DeviceMacro subMacro = macroRef.getTargetDeviceMacro();
                        saveMacro(subMacro, createNew);
                    }
                    item.setParentDeviceMacro(macro);
                }
            }
            if (!createNew) {
                macro.setAccount(userService.getAccount());
                List<DeviceMacro> sameMacro = deviceMacroService.loadSameMacro(macro);
                if (sameMacro != null && sameMacro.size() > 0) {
                    macro.setOid(sameMacro.get(0).getOid());
                } else {
                    this.deviceMacroService.saveDeviceMacro(macro);
                }
            } else {
                this.deviceMacroService.saveDeviceMacro(macro);
            }
        }
    }

    private String encode(String namePassword) {
        if (namePassword == null) return null;
        return new String(Base64.encodeBase64(namePassword.getBytes()));
    }

    private void addAuthentication(AbstractHttpMessage httpMessage) {
        httpMessage.setHeader(Constants.HTTP_BASIC_AUTH_HEADER_NAME, Constants.HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX + encode(userService.getCurrentUser().getUsername() + ":" + userService.getCurrentUser().getPassword()));
    }

    private TemplateList buildTemplateListFromJson(String templatesJson) {
        TemplateList result = new TemplateList();
        try {
            String validTemplatesJson = "";
            if (templatesJson.contains("{\"template\":")) {
                if (templatesJson.contains("{\"template\":[")) {
                    String tempString = templatesJson.replaceFirst("\\{\"template\":", "");
                    validTemplatesJson = tempString.substring(0, tempString.lastIndexOf("}}")) + "}";
                } else {
                    String tempString = templatesJson.replaceFirst("\\{\"template\":", "[");
                    validTemplatesJson = tempString.substring(0, tempString.lastIndexOf("}}")) + "]}";
                }
                result = new JSONDeserializer<TemplateList>().use(null, TemplateList.class).use("templates", ArrayList.class).deserialize(validTemplatesJson);
            }
        } catch (RuntimeException e) {
            log.warn("Faild to get template list, there are no templats in beehive ");
        }
        return result;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setResourceService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public void setDeviceCommandService(DeviceCommandService deviceCommandService) {
        this.deviceCommandService = deviceCommandService;
    }

    public void setSwitchService(SwitchService switchService) {
        this.switchService = switchService;
    }

    public void setSliderService(SliderService sliderService) {
        this.sliderService = sliderService;
    }

    public void setSensorService(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    public void setDeviceMacroService(DeviceMacroService deviceMacroService) {
        this.deviceMacroService = deviceMacroService;
    }

    /**
    * A class to help flexjson to deserialize a UIComponent
    * 
    * @author javen
    * 
    */
    private static class SimpleClassLocator implements ClassLocator {

        @SuppressWarnings("unchecked")
        public Class locate(Map map, Path currentPath) throws ClassNotFoundException {
            return Class.forName(map.get("class").toString());
        }
    }

    /**
    * A class used to help flexjson convert json string to a template list. 
    * flexjson need a java class to map a json string. 
    * @author javen
    *
    */
    public static class TemplateList {

        private List<TemplateDTO> templates = new ArrayList<TemplateDTO>();

        public List<TemplateDTO> getTemplates() {
            return templates;
        }

        public void setTemplates(List<TemplateDTO> templates) {
            this.templates = templates;
        }
    }

    /**
    * A class used to help flexjson convert json string to template list. 
    * The class Template need a property <b>oid</b>, but in json string it is mapped to <b>id</b>,therefore at first we need convert the string to TemplateDTO and then 
    * convert it to Template later.  
    * @author javen
    *
    */
    public static class TemplateDTO {

        private int id;

        private String content;

        private String name;

        private String keywords;

        private boolean shared = false;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }

        public boolean isShared() {
            return shared;
        }

        public void setShared(boolean shared) {
            this.shared = shared;
        }

        public Template toTemplate() {
            Template template = new Template();
            template.setName(name);
            template.setContent(content);
            template.setOid(id);
            template.setKeywords(keywords);
            template.setShared(shared);
            return template;
        }
    }

    @Override
    public Template updateTemplate(Template template) {
        template.setContent(getTemplateContent(template.getScreen()));
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("name", template.getName()));
        params.add(new BasicNameValuePair("content", template.getContent()));
        params.add(new BasicNameValuePair("shared", template.isShared() + ""));
        params.add(new BasicNameValuePair("keywords", template.getKeywords()));
        try {
            String saveRestUrl = configuration.getBeehiveRESTRootUrl() + "account/" + userService.getAccount().getOid() + "/template/" + template.getOid();
            HttpPut httpPut = new HttpPut(saveRestUrl);
            addAuthentication(httpPut);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPut.setEntity(formEntity);
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(httpPut);
            if (HttpServletResponse.SC_OK == response.getStatusLine().getStatusCode()) {
                resourceService.saveTemplateResourcesToBeehive(template);
            } else if (HttpServletResponse.SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
                return null;
            } else {
                throw new BeehiveNotAvailableException("Failed to update template:" + template.getName() + ", Status code: " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            throw new BeehiveNotAvailableException("Failed to save screen as a template: " + (e.getMessage() == null ? "" : e.getMessage()), e);
        }
        return template;
    }

    private void resetGestureForScreen(Screen screen) {
        List<Gesture> gestures = screen.getGestures();
        if (gestures != null && gestures.size() > 0) {
            for (Gesture gesture : gestures) {
                Navigate navigate = gesture.getNavigate();
                if (navigate.getToGroup() != -1L) {
                    gesture.setNavigate(new Navigate());
                }
            }
        }
    }

    private void resetGestureForScreenPair(ScreenPair screenPair) {
        if (screenPair.getLandscapeScreen() != null) {
            resetGestureForScreen(screenPair.getLandscapeScreen());
        }
        if (screenPair.getPortraitScreen() != null) {
            resetGestureForScreen(screenPair.getPortraitScreen());
        }
    }

    private void resetNavigateForButton(UIButton uiBtn) {
        Navigate navigate = uiBtn.getNavigate();
        if (navigate != null) {
            if (navigate.getToGroup() != -1L) {
                uiBtn.setNavigate(new Navigate());
            }
        }
    }

    private void resetNavigateForButtons(Collection<UIButton> uiBtns) {
        if (uiBtns != null && uiBtns.size() > 0) {
            for (UIButton uiBtn : uiBtns) {
                resetNavigateForButton(uiBtn);
            }
        }
    }

    private void getDeviceCommandsFromGesture(Collection<Gesture> gestures, Set<DeviceCommand> uiCmds) {
        if (gestures != null && gestures.size() > 0) {
            for (Gesture gesture : gestures) {
                UICommand cmd = gesture.getUiCommand();
                if (cmd != null) {
                    if (cmd instanceof DeviceCommandRef) {
                        DeviceCommandRef cmdRef = (DeviceCommandRef) cmd;
                        for (DeviceCommand tmpCmd : uiCmds) {
                            if (tmpCmd.equals(cmdRef.getDeviceCommand())) {
                                cmdRef.setDeviceCommand(tmpCmd);
                            }
                        }
                        uiCmds.add(cmdRef.getDeviceCommand());
                    } else if (cmd instanceof DeviceMacroRef) {
                        DeviceMacroRef macroRef = (DeviceMacroRef) cmd;
                        DeviceMacro macro = macroRef.getTargetDeviceMacro();
                        if (macro != null) {
                            Collection<DeviceCommandRef> cmds = getDeviceCommandsRefsFromMacro(macro);
                            for (DeviceCommandRef cmdFromMacro : cmds) {
                                for (DeviceCommand cmdInCommandSet : uiCmds) {
                                    if (cmdFromMacro.getDeviceCommand().equals(cmdInCommandSet)) {
                                        cmdFromMacro.setDeviceCommand(cmdInCommandSet);
                                    }
                                }
                                uiCmds.add(cmdFromMacro.getDeviceCommand());
                            }
                        }
                    }
                }
            }
        }
    }

    private Collection<Gesture> getGestures(Collection<Screen> screens) {
        Collection<Gesture> gestures = new ArrayList<Gesture>();
        for (Screen screen : screens) {
            Collection<Gesture> gstures = screen.getGestures();
            if (gstures != null && gstures.size() > 0) {
                gestures.addAll(gstures);
            }
        }
        return gestures;
    }

    private void resetImageSourceLocationForScreen(ScreenPair sp) {
        String accountPath = resourceService.getRelativeResourcePathByCurrentAccount("account");
        accountPath = accountPath.substring(0, accountPath.lastIndexOf("/") + 1);
        Collection<ImageSource> images = sp.getAllImageSources();
        if (images != null && images.size() > 0) {
            for (ImageSource image : images) {
                String imageFileName = image.getImageFileName();
                image.setSrc(accountPath + imageFileName);
            }
        }
    }
}
