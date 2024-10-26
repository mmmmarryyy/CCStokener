package net.jforum.view.admin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.api.integration.mail.pop.POPListener;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.entities.UserSession;
import net.jforum.repository.ModulesRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.SecurityConstants;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * @author Rafael Steil
 * @version $Id: AdminAction.java,v 1.23 2007/02/25 13:48:34 rafaelsteil Exp $
 */
public class AdminAction extends Command {

    /** 
	 * @see net.jforum.Command#list()
	 */
    public void list() {
        this.login();
    }

    public void login() {
        UserSession us = SessionFacade.getUserSession();
        PermissionControl pc = SecurityRepository.get(us.getUserId());
        if (!SessionFacade.isLogged() || pc == null || !pc.canAccess(SecurityConstants.PERM_ADMINISTRATION)) {
            String returnPath = this.request.getContextPath() + "/admBase/login" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
            JForumExecutionContext.setRedirect(this.request.getContextPath() + "/jforum" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) + "?module=user&action=login&returnPath=" + returnPath);
        } else {
            this.setTemplateName(TemplateKeys.ADMIN_INDEX);
        }
    }

    public void menu() {
        if (this.checkAdmin()) {
            this.setTemplateName(TemplateKeys.ADMIN_MENU);
        }
    }

    public void main() throws Exception {
        if (this.checkAdmin()) {
            this.setTemplateName(TemplateKeys.ADMIN_MAIN);
            this.context.put("installModuleExists", ModulesRepository.getModuleClass("install") != null);
            this.context.put("sessions", SessionFacade.getAllSessions());
            ForumDAO dao = DataAccessDriver.getInstance().newForumDAO();
            this.context.put("stats", dao.getBoardStatus());
            this.checkBoardVersion();
        }
    }

    public void fetchMail() throws Exception {
        new Thread(new Runnable() {

            public void run() {
                try {
                    new POPListener().execute(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        this.main();
    }

    private void checkBoardVersion() {
        String data = this.readVersionFromSocket();
        if (data == null || data.trim().length() == 0) {
            this.context.put("developmentVersion", false);
            return;
        }
        int index = data.indexOf('\n');
        String version = data.substring(0, index).trim();
        String notes = data.substring(index + 1, data.length());
        this.matchVersion(version);
        this.context.put("notes", notes);
    }

    private void matchVersion(String latest) {
        String current = SystemGlobals.getValue(ConfigKeys.VERSION);
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");
        if (currentParts[2].indexOf('-') > -1) {
            currentParts[2] = currentParts[2].substring(0, currentParts[2].indexOf('-'));
        }
        if (Integer.parseInt(latestParts[2]) > Integer.parseInt(currentParts[2]) || Integer.parseInt(latestParts[1]) > Integer.parseInt(currentParts[1]) || Integer.parseInt(latestParts[0]) > Integer.parseInt(currentParts[0])) {
            this.context.put("upToDate", false);
        } else {
            this.context.put("upToDate", true);
        }
        this.context.put("latestVersion", latest);
        this.context.put("currentVersion", current);
        this.context.put("developmentVersion", current.indexOf("-dev") > -1);
    }

    private String readVersionFromSocket() {
        InputStream is = null;
        OutputStream os = null;
        String data = null;
        try {
            URL url = new URL(SystemGlobals.getValue(ConfigKeys.JFORUM_VERSION_URL));
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            os = new ByteArrayOutputStream();
            int available = is.available();
            while (available > 0) {
                byte[] b = new byte[available];
                is.read(b);
                os.write(b);
                available = is.available();
            }
            data = os.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                }
            }
        }
        return data;
    }

    public boolean checkAdmin() {
        int userId = SessionFacade.getUserSession().getUserId();
        if (SecurityRepository.get(userId).canAccess(SecurityConstants.PERM_ADMINISTRATION)) {
            return true;
        }
        JForumExecutionContext.setRedirect(JForumExecutionContext.getRequest().getContextPath() + "/admBase/login" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
        super.ignoreAction();
        return false;
    }

    public Template process(RequestContext request, ResponseContext response, SimpleHash context) {
        return super.process(request, response, context);
    }
}
