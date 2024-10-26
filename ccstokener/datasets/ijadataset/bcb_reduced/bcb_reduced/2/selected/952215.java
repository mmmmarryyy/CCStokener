package googlefeeds;

import com.google.gdata.client.http.AuthSubUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author thufir
 */
public class RetrieveFeedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static String[] acceptedFeedPrefixList = { "http://www.google.com/calendar/feeds", "https://www.google.com/calendar/feeds" };

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String principal = Utility.getCookieValueWithName(request.getCookies(), Utility.LOGIN_COOKIE_NAME);
        if (principal == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unidentified principal.");
            return;
        }
        String authSubToken = TokenManager.retrieveToken(principal);
        if (authSubToken == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "User isn't authorized through AuthSub.");
            return;
        }
        if (request.getQueryString() == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Query string is required.");
            return;
        }
        String queryString = URLDecoder.decode(request.getQueryString(), "UTF-8");
        Map<String, String> queryParams = Utility.parseQueryString(queryString);
        String queryUri = queryParams.get("href");
        String token = queryParams.get("token");
        String timestamp = queryParams.get("timestamp");
        if ((queryUri == null) || (token == null) || (timestamp == null)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing a query parameter.");
            return;
        }
        if (!verifyFeedRequest(principal, queryUri, token, timestamp, "GET")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request failed validation.");
            return;
        }
        handleGetRequest(request, response, queryUri, authSubToken);
    }

    /**
   * Handles a GET request by issuing a GET to the requested feed with the
   * AuthSub token attached in a header.  The output from the server will
   * be proxied back to the requestor.
   * POST/PUT/DELETE can be handled in a similar manner except that the XML
   * sent as part of the request should be sent to the server.
   */
    private void handleGetRequest(HttpServletRequest req, HttpServletResponse resp, String queryUri, String authSubToken) throws ServletException, IOException {
        HttpURLConnection connection = null;
        try {
            connection = openConnectionFollowRedirects(queryUri, authSubToken);
        } catch (GeneralSecurityException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error creating authSub header.");
            return;
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed URL - " + e.getMessage());
            return;
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "IOException - " + e.getMessage());
            return;
        }
        int respCode = connection.getResponseCode();
        if (respCode != HttpServletResponse.SC_OK) {
            Map<String, List<String>> headers = connection.getHeaderFields();
            StringBuffer errorMessage = new StringBuffer("Failed to retrive calendar feed from: ");
            errorMessage.append(queryUri);
            errorMessage.append(".\nServer Error Response:\n");
            errorMessage.append(connection.getResponseMessage());
            for (Iterator<String> iter = headers.keySet().iterator(); iter.hasNext(); ) {
                String header = iter.next();
                List<String> headerValues = headers.get(header);
                for (Iterator<String> headerIter = headerValues.iterator(); headerIter.hasNext(); ) {
                    String headerVal = headerIter.next();
                    errorMessage.append(header + ": " + headerVal + ", ");
                }
            }
            resp.sendError(respCode, errorMessage.toString());
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                resp.getWriter().write(line);
            }
        } catch (IOException e) {
        }
    }

    /**
   * Open a HTTP connection to the provided URL with the AuthSub token specified
   * in the header.  Follow redirects returned by the server - a new AuthSub
   * signature will be computed for each of the redirected-to URLs.
   */
    private HttpURLConnection openConnectionFollowRedirects(String urlStr, String authSubToken) throws MalformedURLException, GeneralSecurityException, IOException {
        boolean redirectsDone = false;
        HttpURLConnection connection = null;
        while (!redirectsDone) {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String authHeader = null;
            authHeader = AuthSubUtil.formAuthorizationHeader(authSubToken, Utility.getPrivateKey(), url, "GET");
            connection.setRequestProperty("Authorization", authHeader);
            connection.setInstanceFollowRedirects(false);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                urlStr = connection.getHeaderField("Location");
                if (urlStr == null) {
                    redirectsDone = true;
                }
            } else {
                redirectsDone = true;
            }
        }
        return connection;
    }

    /**
   * Verifies the request for a feed by:
   * a. validating that the request belongs to a known list of feeds
   * b. validating the token (to protect against url command attacks)
   *<p>
   * This verification is in order to prevent the proxy from URL command attacks
   * which is a cross site scripting problem.
   */
    private boolean verifyFeedRequest(String cookie, String feed, String token, String timestamp, String method) {
        int url_i;
        for (url_i = 0; url_i < acceptedFeedPrefixList.length; url_i++) {
            if (feed.toLowerCase().startsWith(acceptedFeedPrefixList[url_i].toLowerCase())) {
                break;
            }
        }
        if (url_i == acceptedFeedPrefixList.length) {
            return false;
        }
        return SecureUrl.isTokenValid(token, cookie, feed, method, timestamp);
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
