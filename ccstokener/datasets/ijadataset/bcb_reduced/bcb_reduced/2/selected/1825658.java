package it.jwallpaper.plugins.vladstudio;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import it.jwallpaper.JWallpaperChanger;
import it.jwallpaper.config.ConfigurationSupport;
import it.jwallpaper.forms.AbstractConfigurationPanel;
import it.jwallpaper.menues.JawcMenu;
import it.jwallpaper.menues.JawcMenuItem;
import it.jwallpaper.platform.Platform;
import it.jwallpaper.platform.TrayIconMessageType;
import it.jwallpaper.platform.WallpaperResizeMode;
import it.jwallpaper.plugins.AbstractPlugin;
import it.jwallpaper.plugins.PluginExecutionResult;
import it.jwallpaper.plugins.PluginInterface;
import it.jwallpaper.plugins.vladstudio.util.CryptoUtils;
import it.jwallpaper.util.HttpUtils;
import it.jwallpaper.util.IoUtils;
import it.jwallpaper.util.MessageUtils;
import it.jwallpaper.util.SerializationUtils;
import it.jwallpaper.util.UiUtils;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VladStudioPlugin extends AbstractPlugin implements PluginInterface, ActionListener {

    private Log logger = LogFactory.getLog(this.getClass());

    private static final String VLADSTUDIO_LOGIN_URL = "http://www.vladstudio.com/companion/signin.php?email=${email}&keycode=${password}";

    private static final String VLADSTUDIO_LASTIMAGE_URL = "http://www.vladstudio.com/companion/getwallpaper.php?id=latest&width=${width}&height=${height}&email=${email}&keycode=${password}";

    private static final String VLADSTUDIO_RANDOMIMAGE_URL = "http://www.vladstudio.com/companion/getwallpaper.php?id=random&width=${width}&height=${height}&email=${email}&keycode=${password}";

    private static final String VLADSTUDIO_TELL_A_FRIEND_URL = "http://www.vladstudio.com/companion/c.php?go=tell";

    private static final String VLADSTUDIO_SEND_ECARD = "http://www.vladstudio.com/companion/c.php?go=cards";

    private static final String VLADSTUDIO_WEBSITE = "http://www.vladstudio.com";

    private static final String VLADSTUDIO_BROWSE_WALLPAPERS = "http://www.vladstudio.com/wallpapers/";

    private static final String CMD_SET_RANDOM = "CMD_SET_RANDOM";

    private static final String CMD_SET_LATEST = "CMD_SET_LATEST";

    private static final String CMD_TELL_A_FRIEND = "CMD_TELL_A_FRIEND";

    private static final String CMD_SEND_ECARD = "CMD_SEND_ECARD";

    private static final String CMD_VLADSTUDIO_WEBSITE = "CMD_VLADSTUDIO_WEBSITE";

    private static final String CMD_BROWSE_WALLPAPERS = "CMD_BROWSE_WALLPAPERS";

    private VladStudioPluginConfig config = new VladStudioPluginConfig();

    private VladStudioPluginConfigPanel configPanel;

    private static final String LASTIMAGEFILE = "VladStudioPlugin-LastImage.ser";

    private File lastImageFile;

    private String lastImage;

    private enum GetImageMode {

        LAST, RANDOM
    }

    ;

    public void initialize() {
        lastImageFile = new File(ConfigurationSupport.getConfigurationFolder(), LASTIMAGEFILE);
        try {
            lastImage = StringUtils.defaultString((String) SerializationUtils.load(lastImageFile));
        } catch (Exception e) {
            lastImage = "";
            logger.error(e);
        }
        if (isEnabled()) {
            try {
                doLogin();
            } catch (IOException exc) {
                logger.error("Vladstudio connection not available " + exc.toString());
            }
        }
    }

    public PluginExecutionResult changeImageInternal() {
        Map<String, String> imageInfo = null;
        PluginExecutionResult result = new PluginExecutionResult(true);
        try {
            if (!config.isRegistered()) {
                doLogin();
            }
            if (config.isSearchNew() && isThereNewLastImage()) {
                imageInfo = vladstudioGetImageInfo(GetImageMode.LAST);
                lastImage = imageInfo.get("name");
                SerializationUtils.save(lastImageFile, lastImage);
                result.setMessageToDisplay(MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.latestImage", imageInfo.get("name")));
            } else if (config.isSetRandom()) {
                imageInfo = vladstudioGetImageInfo(GetImageMode.RANDOM);
                result.setMessageToDisplay(MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.changedImage", imageInfo.get("name")));
            }
            if (imageInfo != null) {
                setImageInfoAsWallpaper(imageInfo);
            } else {
                return new PluginExecutionResult(false);
            }
        } catch (IOException ioe) {
            showUnavailableConnectionMessage();
            logger.error("Vladstudio connection not available " + ioe.toString());
            return new PluginExecutionResult(false, MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.noconnection"));
        } catch (Exception exc) {
            logger.error(exc);
            return new PluginExecutionResult(false);
        }
        return result;
    }

    private void setImageImmediate(GetImageMode getImageMode) {
        Map<String, String> imageInfo = null;
        Platform.getPlatform().setWorkingIcon(true);
        try {
            if (!config.isRegistered()) {
                doLogin();
            }
            imageInfo = vladstudioGetImageInfo(getImageMode);
            if (imageInfo != null) {
                setImageInfoAsWallpaper(imageInfo);
                String message = "";
                if (getImageMode.equals(GetImageMode.LAST)) {
                    message = MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.latestImage", imageInfo.get("name"));
                } else {
                    message = MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.changedImage", imageInfo.get("name"));
                }
                Platform.getPlatform().showBalloonMessage(getPluginName(), message, TrayIconMessageType.NONE);
            }
        } catch (Exception exc) {
            logger.error("Error setting image immediate", exc);
            Platform.getPlatform().showBalloonMessage(getPluginName(), MessageUtils.getMessage(JWallpaperChanger.class, "error.changingImage", exc.toString()), TrayIconMessageType.ERROR);
            return;
        } finally {
            Platform.getPlatform().setWorkingIcon(false);
        }
    }

    private void setImageInfoAsWallpaper(Map<String, String> imageInfo) throws Exception {
        String imageName;
        String imageFileName;
        String imageUrl;
        File imageFile;
        imageName = imageInfo.get("name");
        imageUrl = imageInfo.get("url");
        if (imageUrl.indexOf('/') != -1) {
            imageFileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        } else {
            imageFileName = imageName;
        }
        if (config.isSaveToDisk()) {
            imageFile = new File(config.getDownloadTo(), imageFileName);
            if (!imageFile.exists()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Downloading image " + imageUrl);
                }
                HttpUtils.downloadImage(imageFile, imageUrl);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Image already dowloaded in " + imageFile.getAbsolutePath());
                }
            }
            setWallpaper(imageFile, WallpaperResizeMode.STRETCHED);
        } else {
            imageFile = IoUtils.getTempWallpaperFile();
            HttpUtils.downloadImage(imageFile, imageUrl);
            setWallpaper(imageFile, WallpaperResizeMode.STRETCHED);
        }
    }

    private boolean isThereNewLastImage() throws IOException {
        Map<String, String> imageInfo = vladstudioGetImageInfo(GetImageMode.LAST);
        String lastImage = StringUtils.defaultString(imageInfo.get("name"));
        return !lastImage.equals(this.lastImage);
    }

    public AbstractConfigurationPanel getPluginConfigPanel() {
        if (configPanel == null) {
            configPanel = new VladStudioPluginConfigPanel(this, config);
        }
        return configPanel;
    }

    public JawcMenu getPluginMenu() {
        JawcMenu pluginMenu = new JawcMenu(VladStudioPlugin.class, "plugin.VladStudioPlugin.menu");
        JawcMenuItem changeRandomImageMenu = new JawcMenuItem(VladStudioPlugin.class, "plugin.VladStudioPlugin.menu.setRandom");
        changeRandomImageMenu.addActionListener(this);
        changeRandomImageMenu.setActionCommand(CMD_SET_RANDOM);
        pluginMenu.add(changeRandomImageMenu);
        JawcMenuItem changeLatestImageMenu = new JawcMenuItem(VladStudioPlugin.class, "plugin.VladStudioPlugin.menu.setLatest");
        changeLatestImageMenu.addActionListener(this);
        changeLatestImageMenu.setActionCommand(CMD_SET_LATEST);
        pluginMenu.add(changeLatestImageMenu);
        pluginMenu.addSeparator();
        JawcMenuItem websiteMenuMenu = new JawcMenuItem(VladStudioPlugin.class, "plugin.VladStudioPlugin.menu.vladstudioWebsite");
        websiteMenuMenu.addActionListener(this);
        websiteMenuMenu.setActionCommand(CMD_VLADSTUDIO_WEBSITE);
        pluginMenu.add(websiteMenuMenu);
        JawcMenuItem browseWallpaperMenu = new JawcMenuItem(VladStudioPlugin.class, "plugin.VladStudioPlugin.menu.browseWallpapers");
        browseWallpaperMenu.addActionListener(this);
        browseWallpaperMenu.setActionCommand(CMD_BROWSE_WALLPAPERS);
        pluginMenu.add(browseWallpaperMenu);
        JawcMenuItem sendEcardMenu = new JawcMenuItem(VladStudioPlugin.class, "plugin.VladStudioPlugin.menu.sendEcard");
        sendEcardMenu.addActionListener(this);
        sendEcardMenu.setActionCommand(CMD_SEND_ECARD);
        pluginMenu.add(sendEcardMenu);
        JawcMenuItem tellAFriendMenu = new JawcMenuItem(VladStudioPlugin.class, "plugin.VladStudioPlugin.menu.tellAFriend");
        tellAFriendMenu.addActionListener(this);
        tellAFriendMenu.setActionCommand(CMD_TELL_A_FRIEND);
        pluginMenu.add(tellAFriendMenu);
        return pluginMenu;
    }

    public void setConfiguration(XMLConfiguration configuration) {
        config.setConfiguration(configuration);
    }

    public XMLConfiguration getConfiguration() {
        return config.getConfiguration();
    }

    public boolean needsConfiguration() {
        if (config.isSaveToDisk()) {
            File downloadFolder = new File(config.getDownloadTo());
            return !(downloadFolder.exists() && downloadFolder.canWrite() && downloadFolder.isDirectory());
        }
        return false;
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(CMD_SET_RANDOM)) {
            setImageImmediate(GetImageMode.RANDOM);
        } else if (event.getActionCommand().equals(CMD_SET_LATEST)) {
            setImageImmediate(GetImageMode.LAST);
        } else if (event.getActionCommand().equals(CMD_TELL_A_FRIEND)) {
            openUrl(VLADSTUDIO_TELL_A_FRIEND_URL);
        } else if (event.getActionCommand().equals(CMD_SEND_ECARD)) {
            openUrl(VLADSTUDIO_SEND_ECARD);
        } else if (event.getActionCommand().equals(CMD_VLADSTUDIO_WEBSITE)) {
            openUrl(VLADSTUDIO_WEBSITE);
        } else if (event.getActionCommand().equals(CMD_BROWSE_WALLPAPERS)) {
            openUrl(VLADSTUDIO_BROWSE_WALLPAPERS);
        }
    }

    private void openUrl(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
            Platform.getPlatform().openUrl(url);
        } catch (Exception exc) {
            logger.error(exc);
            Platform.getPlatform().showBalloonMessage(getPluginName(), MessageUtils.getMessage(JWallpaperChanger.class, "error.openingUrl", exc.toString()), TrayIconMessageType.ERROR);
        }
    }

    public boolean doLogin() throws IOException {
        if (StringUtils.isEmpty(config.getEmail()) || StringUtils.isEmpty(config.getPassword())) {
            config.setRegistered(false);
            config.setRegisteredName(MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.unregistered"));
            return false;
        } else {
            String user = null;
            try {
                user = vladstudioLogin();
            } catch (IOException ioe) {
                config.setRegistered(false);
                config.setRegisteredName(MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.unregistered"));
                throw ioe;
            }
            if (user.equals("0")) {
                config.setRegistered(false);
                config.setRegisteredName(MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.unregistered"));
            } else if (user.equals("-1")) {
                config.setRegistered(false);
                config.setRegisteredName(MessageUtils.getMessage(VladStudioPlugin.class, "plugin.VladStudioPlugin.expired"));
            } else {
                config.setRegistered(true);
                config.setRegisteredName(user);
            }
            return true;
        }
    }

    private void showUnavailableConnectionMessage() {
        Platform.getPlatform().showBalloonMessage(getPluginName(), MessageUtils.getMessage(VladStudioPlugin.class, "error.connectionUnavailable"), TrayIconMessageType.ERROR);
    }

    private Map<String, String> vladstudioGetImageInfo(GetImageMode imageMode) throws IOException {
        Map<String, String> ret;
        HashMap<String, String> parameters = new HashMap<String, String>();
        Dimension screenSize = UiUtils.getScreenSize();
        parameters.put("width", "" + screenSize.width);
        parameters.put("height", "" + screenSize.height);
        parameters.put("email", config.getEmail());
        parameters.put("password", CryptoUtils.encryptAsHex(config.getPassword()));
        ret = sendRequest(imageMode.equals(GetImageMode.LAST) ? VLADSTUDIO_LASTIMAGE_URL : VLADSTUDIO_RANDOMIMAGE_URL, parameters);
        return ret;
    }

    private String vladstudioLogin() throws IOException {
        Map<String, String> ret;
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("email", config.getEmail());
        parameters.put("password", CryptoUtils.encryptAsHex(config.getPassword()));
        ret = sendRequest(VLADSTUDIO_LOGIN_URL, parameters);
        return ret.get("reg");
    }

    private Map<String, String> sendRequest(String request, Map<String, String> parameters) throws IOException {
        URL url;
        StrSubstitutor sub = new StrSubstitutor(parameters);
        String resolvedUrl = sub.replace(request);
        url = new URL(resolvedUrl);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending to VladStudio: " + resolvedUrl);
        }
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(false);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();
        InputStream in = httpURLConnection.getInputStream();
        List lines = IOUtils.readLines(in);
        IOUtils.closeQuietly(in);
        HashMap<String, String> results = new HashMap<String, String>();
        for (int i = 0; i < lines.size(); i++) {
            String txt = (String) lines.get(i);
            int pos = txt.indexOf('=');
            if (pos != -1) {
                String key = txt.substring(0, pos);
                String value = txt.substring(pos + 1);
                results.put(key, value);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Received from VladStudio: " + results);
        }
        return results;
    }
}
