package org.elip.stewiemaze.server.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.elip.stewiemaze.server.DatastoreHelper;
import org.elip.stewiemaze.server.FacebookHelper;
import org.elip.stewiemaze.server.services.PlayerService;
import org.elip.stewiemaze.server.services.RegisterPlayerService;
import org.elip.stewiemaze.server.utils.Constants;
import org.elip.stewiemaze.shared.entities.Player;
import com.restfb.types.User;

public class FacebookAuthenticationServlet extends HttpServlet {

    private Logger logger = Logger.getLogger(FacebookAuthenticationServlet.class.getName());

    private static final String OAUTH_URL = "https://graph.facebook.com/oauth/access_token";

    /**
	 * 
	 */
    private static final long serialVersionUID = -6293406033616761819L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp);
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        String error = req.getParameter("error");
        if (error != null) {
            out.println("You denied permission for this app, no posts will be made on your behalf");
            out.println("<br>");
            out.println("<a href='http://stewiemaze.appspot.com'>Return to game</a>");
            return;
        }
        String appCode = req.getParameter("code");
        String accessToken = getUserAccessToken(appCode);
        accessToken = accessToken.split("&")[0];
        String username = FacebookHelper.getUsername(accessToken);
        Player player = DatastoreHelper.getPlayer(username);
        if (player != null) {
            DatastoreHelper.updateAccessToken(username, accessToken);
            logger.fine("posting score " + player.getHighscore() + " for user " + player.getNickname());
            FacebookHelper.postScoreToWall(username, player.getHighscore() + "");
            try {
                logger.fine("publishing score " + player.getHighscore() + " for user " + player.getNickname());
                FacebookHelper.publishScore(player.getHighscore() + "", player.getUserId());
            } catch (RuntimeException e) {
                logger.fine("Caught runtime exception while publishing score" + e);
            }
        }
        resp.sendRedirect(resp.encodeRedirectURL("http://stewiemaze.appspot.com/logged_in.html"));
    }

    private String getUserAccessToken(String code) throws IOException {
        StringBuilder params = new StringBuilder().append("&code=").append(code).append("&redirect_uri=").append(URLEncoder.encode(Constants.FACEBOOK_AUTHENTICATE_URI, "UTF-8"));
        return getAccessToken(params.toString());
    }

    private String getAccessToken(String params) throws IOException {
        StringBuilder constantParams = new StringBuilder().append("client_id=").append(Constants.APP_ID).append("&client_secret=").append(Constants.SECRET_KEY);
        params = constantParams.append(params).toString();
        URL url = new URL(OAUTH_URL);
        URLConnection conn = url.openConnection();
        try {
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            conn.getOutputStream().write(params.toString().getBytes());
            conn.getOutputStream().flush();
        } finally {
            conn.getOutputStream().close();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        try {
            String inputLine = in.readLine();
            if (inputLine == null || !inputLine.contains("=")) {
                return null;
            }
            String accessToken = inputLine.split("=")[1];
            return accessToken;
        } finally {
            in.close();
        }
    }
}
