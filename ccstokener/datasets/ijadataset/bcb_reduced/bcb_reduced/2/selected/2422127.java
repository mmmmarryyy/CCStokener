package com.zagile.confluence.plugins.semforms.macros;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.zagile.confluence.plugins.semforms.settings.ZSemanticPluginSettings;

public class ZRAPQueryMacro extends BaseMacro {

    SettingsManager settingsManager;

    ZSemanticPluginSettings semformsSettings;

    public void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public boolean isInline() {
        return false;
    }

    public boolean hasBody() {
        return true;
    }

    public RenderMode getBodyRenderMode() {
        return RenderMode.NO_RENDER;
    }

    public void loadData() {
        Serializable ps = settingsManager.getPluginSettings("com.zagile.confluence.plugins.zsemantic_plugin");
        if (ps == null) ps = new ZSemanticPluginSettings();
        ZSemanticPluginSettings settings = (ZSemanticPluginSettings) ps;
        semformsSettings = settings;
    }

    /**
	     * This method returns XHTML to be displayed on the final page.
	     */
    public String execute(Map params, String body, RenderContext renderContext) throws MacroException {
        loadData();
        String from = (String) params.get("from");
        if (body.length() > 0 && from != null) {
            try {
                URL url;
                String serverUser = null;
                String serverPassword = null;
                url = new URL(semformsSettings.getZRapServerUrl() + "ZRAP_QueryProcessor.php?from=" + URLEncoder.encode(from, "utf-8") + "&query=" + URLEncoder.encode(body, "utf-8"));
                if (url.getUserInfo() != null) {
                    String[] userInfo = url.getUserInfo().split(":");
                    if (userInfo.length == 2) {
                        serverUser = userInfo[0];
                        serverPassword = userInfo[1];
                    }
                }
                URLConnection connection = null;
                InputStreamReader bf;
                if (serverUser != null && serverPassword != null) {
                    connection = url.openConnection();
                    String encoding = new sun.misc.BASE64Encoder().encode((serverUser + ":" + serverPassword).getBytes());
                    connection.setRequestProperty("Authorization", "Basic " + encoding);
                    bf = new InputStreamReader(connection.getInputStream());
                } else {
                    bf = new InputStreamReader(url.openStream());
                }
                BufferedReader bbf = new BufferedReader(bf);
                String line = bbf.readLine();
                String buffer = "";
                while (line != null) {
                    buffer += line;
                    line = bbf.readLine();
                }
                return buffer;
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + e.getLocalizedMessage();
            }
        } else return "Please write an RDQL query in the macro as body and an url of the model as 'from' parameter";
    }
}
