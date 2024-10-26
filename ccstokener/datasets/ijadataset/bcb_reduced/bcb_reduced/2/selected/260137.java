package se.sics.tasim.is.common;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.util.ByteArrayISO8859Writer;
import se.sics.isl.html.HtmlWriter;

public class RegistrationPage extends HttpPage {

    private static final Logger log = Logger.getLogger(RegistrationPage.class.getName());

    private static final boolean SUPPORT_CMD = true;

    private static final boolean SUPPORT_CLAIM = true;

    private final InfoServer infoServer;

    private String password;

    private boolean isRemoteRegistrationEnabled = false;

    private URL[] notificationTargets;

    public RegistrationPage(InfoServer infoServer) {
        this(infoServer, null, null, false);
    }

    public RegistrationPage(InfoServer infoServer, String notification, String password, boolean isRemoteRegistrationEnabled) {
        this.infoServer = infoServer;
        this.password = password;
        this.isRemoteRegistrationEnabled = isRemoteRegistrationEnabled;
        if (notification != null) {
            StringTokenizer tok = new StringTokenizer(notification, ", \t");
            int len = tok.countTokens();
            if (len > 0) {
                try {
                    URL[] n = new URL[len];
                    for (int i = 0; i < len; i++) {
                        n[i] = new URL(tok.nextToken());
                    }
                    this.notificationTargets = n;
                } catch (Exception e) {
                    log.log(Level.WARNING, "could not handle notifications " + notification, e);
                }
            }
        }
    }

    public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws HttpException, IOException {
        String message = null;
        boolean created = false;
        String name = null;
        String email = null;
        if (password != null && !password.equals(request.getParameter("pw"))) {
            response.sendError(HttpURLConnection.HTTP_FORBIDDEN);
            request.setHandled(true);
            return;
        }
        String cmd = SUPPORT_CMD && isRemoteRegistrationEnabled ? trim(request.getParameter("cmd")) : null;
        if (HttpRequest.__POST.equals(request.getMethod()) || (SUPPORT_CMD && cmd != null)) {
            String pw1 = trim(request.getParameter("p1"));
            String pw2 = trim(request.getParameter("p2"));
            name = trim(request.getParameter("name"));
            email = trim(request.getParameter("email"));
            if (name == null) {
                message = "You must enter a user name";
            } else if (SUPPORT_CMD && cmd != null) {
                created = true;
                if ("validate".equals(cmd)) {
                    String password = infoServer.getUserPassword(name);
                    if (password == null) {
                        try {
                            infoServer.validateUserInfo(name, pw1, email);
                            message = "ok:create";
                        } catch (Exception e) {
                            message = "error:" + e.getMessage();
                        }
                    } else if (password.equals(pw1)) {
                        message = "ok:exists(" + infoServer.getUserID(name) + ')';
                    } else {
                        message = "error:agent already exists with another password";
                    }
                } else if ("create".equals(cmd)) {
                    String password = infoServer.getUserPassword(name);
                    if (password == null) {
                        try {
                            int userID = infoServer.createUser(name, pw1, email);
                            callNotification(userID);
                            message = "ok:create";
                        } catch (Exception e) {
                            message = "error:" + e.getMessage();
                        }
                    } else if (password.equals(pw1)) {
                        message = "ok:exists";
                    } else {
                        message = "error:agent already exists with another password";
                    }
                } else if ("claim".equals(cmd)) {
                    try {
                        int userID = infoServer.claimUser(name, pw1, email);
                        callNotification(userID);
                        message = "ok:create";
                    } catch (Exception e) {
                        message = "error:" + e.getMessage();
                    }
                } else {
                    message = "error:unknown command";
                }
                message = "<cmd>" + message + "</cmd>";
            } else {
                if (pw1 == null || pw1.length() < 4) {
                    message = "No password (please use at least 4 characters)";
                } else if (pw1.equals(pw2)) {
                    try {
                        int userID = infoServer.createUser(name, pw1, email);
                        message = "User " + name + " has been registered";
                        created = true;
                        callNotification(userID);
                    } catch (Exception e) {
                        message = "Error: " + e.getMessage();
                    }
                } else {
                    message = "Passwords do not match";
                }
            }
        }
        HtmlWriter page = new HtmlWriter();
        if (created) {
            page.text(message);
        } else {
            page.pageStart("Agent/User Registration");
            if (message != null) {
                page.tag("font", "color=red").h3(message).tagEnd("font").p().tag("hr").p();
            }
            page.h2("Register new Agent/User").form("", "POST").table(HtmlWriter.BORDERED).attr("cellpadding", 2).td("Agent/User Name").td("<input name=name type=text length=22");
            if (name != null) {
                page.text(" value='").text(name).text('\'');
            }
            page.text('>').tr().td("Email").td("<input name=email type=text length=22");
            if (email != null) {
                page.text(" value='").text(email).text('\'');
            }
            page.text('>').tr().td("Password").td("<input name=p1 type=password length=22>").tr().td("Password (retype)").td("<input name=p2 type=password length=22>").tableEnd().text("<input type=submit value='Register'>").formEnd();
        }
        page.close();
        ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer();
        page.write(writer);
        response.setContentType(HttpFields.__TextHtml);
        response.setContentLength(writer.size());
        writer.writeTo(response.getOutputStream());
        response.commit();
    }

    private void callNotification(final int userID) {
        if (notificationTargets != null) {
            new Thread(new Runnable() {

                public void run() {
                    for (int i = 0, n = notificationTargets.length; i < n; i++) {
                        try {
                            URL url = new URL(notificationTargets[i], "?id=" + userID);
                            URLConnection conn = url.openConnection();
                            int length = conn.getContentLength();
                            conn.getInputStream().close();
                        } catch (Exception e) {
                            log.log(Level.WARNING, "could not notify " + notificationTargets[i], e);
                        }
                    }
                }
            }).start();
        }
    }

    private String trim(String text) {
        return (text != null) && ((text = text.trim()).length() > 0) ? text : null;
    }
}
