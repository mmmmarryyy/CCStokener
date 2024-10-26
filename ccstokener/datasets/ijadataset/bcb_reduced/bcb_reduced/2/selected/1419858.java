package net.sf.rwp.core.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletRequest;
import net.sf.rwp.core.CoreAttributes;
import org.springframework.context.annotation.Scope;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zkmax.xel.ognl.OGNLFactory;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Label;
import org.zkoss.zul.api.Window;

@org.springframework.stereotype.Component("uiManager")
@Scope("singleton")
public class DefaultUIManager implements UIManager {

    @Override
    public Session getSession() {
        return getDesktop().getSession();
    }

    @Override
    public Desktop getDesktop() {
        return Executions.getCurrent().getDesktop();
    }

    @Override
    public Page getPage() {
        return (Page) getDesktop().getAttribute(CoreAttributes.MAIN_UI_PAGE);
    }

    @Override
    public Component createComponents(String uri, Component parent, Map<String, Object> args) {
        Component comp = null;
        URL url = getClass().getClassLoader().getResource(uri);
        if (url == null) {
            throw new IllegalArgumentException("Could not find " + uri + " in class path.");
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            PageDefinition pageDef = Executions.getCurrent().getPageDefinitionDirectly(reader, "zul");
            pageDef.setExpressionFactoryClass(OGNLFactory.class);
            comp = Executions.createComponents(pageDef, parent, args);
        } catch (IOException e) {
            throw new Error("Could not open file: " + uri);
        }
        return comp;
    }

    @Override
    public void openUI(String uiName, Map<String, Object> parameters) {
        final UIFragment ui = (UIFragment) SpringUtil.getBean(uiName, UIFragment.class);
        if (ui == null) {
            throw new IllegalArgumentException("UI " + uiName + " not found in application context.");
        }
        ui.init(parameters);
    }

    @Override
    public void openUI(String uiName) {
        openUI(uiName, new HashMap<String, Object>());
    }

    @Override
    public Locale getLocale() {
        Locale locale = (Locale) getSession().getAttribute(Attributes.PREFERRED_LOCALE);
        if (locale == null) {
            ServletRequest request = (ServletRequest) Executions.getCurrent().getNativeRequest();
            locale = request.getLocale();
        }
        return locale;
    }

    @Override
    public TimeZone getTimeZone() {
        TimeZone zone = (TimeZone) getSession().getAttribute(Attributes.PREFERRED_TIME_ZONE);
        if (zone == null) {
            zone = TimeZone.getDefault();
        }
        return zone;
    }

    @Override
    public void setLocale(Locale locale) {
        getSession().setAttribute(Attributes.PREFERRED_LOCALE, locale);
    }

    @Override
    public void setTimeZone(TimeZone timeZone) {
        getSession().setAttribute(Attributes.PREFERRED_TIME_ZONE, timeZone);
    }

    @Override
    public Component createComponents(String uri, Component parent) {
        return createComponents(uri, parent, new HashMap<String, Object>());
    }

    @Override
    public void notifySuccess(String message) {
        Window win = (Window) getPage().getFellow("outerWindow").getFellow("notifyWindow");
        win.setSclass("notifySuccess");
        win.getChildren().clear();
        win.appendChild(new Label(message));
        win.doPopup();
    }
}
