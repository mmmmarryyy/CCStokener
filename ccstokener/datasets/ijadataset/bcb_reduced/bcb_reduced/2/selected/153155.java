package org.nex.ts.server.ws.model;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.net.URLEncoder;
import org.nex.ts.TopicSpacesException;
import org.nex.ts.server.common.model.Ticket;
import org.nex.ts.smp.SubjectMapProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * @author park
 * <p>Cohere's API for addNode<br />
 * http://cohere.open.ac.uk/help/code-doc/Cohere-API/_apilib.html
 * addnode
Add a node. Requires login
Node addNode (string $name, string $desc, [string $private = &quot;&quot;], [string $nodetypeid = &quot;&quot;], [string $imageurlid = &quot;&quot;], [string $imagethumbnail = &quot;&quot;])
Parameters:
    * string name
    * string desc
    * string private optional, can be Y or N, defaults to users preferred setting
    * string nodetypeid optional, the id of the nodetype this node is, defaults to 'Idea' node type id.
    * string imageurlid optional, the urlid of the url for the image that is being used as this node's icon
    * string imagethumbnail optional, the local server path to the thumbnail of the image used for this node
    * return: Node or Error
 * </p>
 * <p>
 * addurl
Add a URL. Requires login
URL addURL (string $url, string $title, string $desc, [string $clip = &quot;&quot;])
Parameters:
    * string url
    * string title
    * string desc
    * string clip (optional);
    * return: URL or Error
 *</p>
 * <p>
 * addurltonode
Add a URL to a Node. Requires login, user must be owner of both the node and URL
Node addURLToNode (string $urlid, string $nodeid, [string $comments = &quot;&quot;])
Parameters:
    * string urlid
    * string nodeid
    * string comments (optional)
    * return: Node or Error
 * </p>
 * <p>Technically, there are three processes:
 * <li>Add a node and get it back to get its nodeId</li>
 * <li>Add a URL and get it back to get its urlID</li>
 * <li>Add a URL to the Node with nodeId and urlId</li>
 * </p>
 */
public class WS_CohereExporter {

    private final String COHERE_QUERY_SUFFIX = "api/service.php";

    private String COHERE_URL = "";

    private String COHERE_QUERY = "";

    private SubjectMapProvider smp;

    /**
	 * Beware Cohere uses email addresses for usernames
	 */
    public WS_CohereExporter(SubjectMapProvider s) {
        smp = s;
        COHERE_URL = (String) smp.getProperty("Cohere");
        COHERE_QUERY = COHERE_URL + COHERE_QUERY_SUFFIX;
        smp.logDebug("WS_CohereExporter+ " + COHERE_QUERY);
    }

    public String exportScanPost(String jsonString, Ticket credentials) throws TopicSpacesException {
        smp.logDebug("WS_CohereExporter.exportScanPost- " + jsonString);
        String result = "";
        String username = credentials.getOwner();
        String password = credentials.getOldPassword();
        JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonString);
        smp.logDebug("WS_CohereExporter.exportScanPost-1 " + jsonObject.toString());
        String title = "", description = "", scanURL = "", resourceURL = "";
        try {
            HttpURLConnection connection;
            String loginurl = COHERE_QUERY + "?format=xml&method=login&username=" + username + "&password=" + password;
            URL url = new URL(loginurl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.connect();
            int rc = connection.getResponseCode();
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer body = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                body.append(line);
                body.append('\r');
            }
            rd.close();
            smp.logDebug("WS_CohereExporter login " + rc + " " + body.toString());
            String sessionId = null;
            Map<String, List<String>> headers = connection.getHeaderFields();
            smp.logDebug("HEADERS " + headers);
            List<String> cookies = headers.get("Set-Cookie");
            smp.logDebug("CCCC " + cookies);
            String ck = null;
            String user = null;
            if (cookies != null) {
                Iterator<String> itr = cookies.iterator();
                String cky;
                while (itr.hasNext()) {
                    cky = itr.next().split(";", 2)[0];
                    smp.logDebug("COOKIE " + cky);
                    if (cky.startsWith("user") && !(cky.indexOf("del") > 0)) {
                        user = cky;
                    }
                    if (cky.startsWith("Cohere")) {
                        ck = cky;
                        break;
                    }
                }
            }
            if (user != null) user = user.substring("user-".length());
            if (ck != null) {
                ck = ck.substring("Cohere=".length());
            }
            connection.disconnect();
            sessionId = ck;
            smp.logDebug("COOKIES " + sessionId + " user " + user);
            String validate = COHERE_QUERY + "?format=xml&method=validatesession&userid=" + user;
            smp.logDebug("VALIDATE " + validate);
            url = new URL(validate);
            connection = (HttpURLConnection) url.openConnection();
            String myCookie = "userid=" + user;
            connection.setRequestProperty("Cookie", myCookie);
            myCookie = "Cohere=" + sessionId;
            connection.setRequestProperty("Cookie", myCookie);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            rc = connection.getResponseCode();
            is = connection.getInputStream();
            rd = new BufferedReader(new InputStreamReader(is));
            body = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                body.append(line);
                body.append('\r');
            }
            rd.close();
            connection.disconnect();
            smp.logDebug("WS_CohereExporter validate " + rc + " " + body.toString());
            JSONObject node = (JSONObject) jsonObject.get("node");
            title = (String) node.get("title");
            description = (String) node.get("description");
            scanURL = (String) node.get("node_url");
            resourceURL = (String) node.get("field_url");
            smp.logDebug("WS_CohereExporter.exportScanPost-1 " + title + " " + scanURL + " " + resourceURL + " " + description);
            String nodeQuery = COHERE_QUERY + makeNodeQuery(title, description);
            smp.logDebug("WS_CohereExporter.exportScanPost-2 " + nodeQuery);
            String nodeid = _addNode(nodeQuery, user, sessionId);
            smp.logDebug("WS_CohereExporter.exportScanPost-3 " + nodeid);
            if (nodeid != null) {
                nodeQuery = COHERE_QUERY + makeUrlQuery(title, scanURL, "Scan URL");
                String urlid = _addNode(nodeQuery, user, sessionId);
                smp.logDebug("WS_CohereExporter.exportScanPost-4 " + urlid);
                if (urlid != null) {
                    nodeQuery = COHERE_QUERY + makeAddUrlQuery(urlid, nodeid);
                    _addNode(nodeQuery, user, sessionId);
                    nodeQuery = COHERE_QUERY + makeUrlQuery(title, resourceURL, "Node URL");
                    urlid = _addNode(nodeQuery, user, sessionId);
                    smp.logDebug("WS_CohereExporter.exportScanPost-5 " + urlid);
                    if (urlid != null) {
                        nodeQuery = COHERE_QUERY + makeAddUrlQuery(urlid, nodeid);
                        _addNode(nodeQuery, user, sessionId);
                    }
                }
            }
        } catch (Exception e) {
            smp.logError(e.getMessage(), e);
            result = e.getMessage();
        }
        return result;
    }

    String makeNodeQuery(String title, String description) throws Exception {
        StringBuilder buf = new StringBuilder("?format=xml&method=addnode&name=");
        String t = URLEncoder.encode(scrubUnicode(title), "UTF-8");
        String d = URLEncoder.encode(scrubUnicode(description), "UTF-8");
        buf.append(t + "&desc=");
        buf.append(d + "&private=N");
        String result = buf.toString();
        return result;
    }

    String makeUrlQuery(String title, String url, String desc) throws Exception {
        StringBuilder buf = new StringBuilder("?format=xml&method=addurl&url=");
        buf.append(url + "&title=");
        String t = URLEncoder.encode(scrubUnicode(title), "UTF-8");
        String d = URLEncoder.encode(scrubUnicode(desc), "UTF-8");
        buf.append(t + "&desc=" + d);
        String result = buf.toString();
        return result;
    }

    String makeAddUrlQuery(String urlid, String nodeid) throws Exception {
        StringBuilder buf = new StringBuilder("?format=xml&method=addurltonode&urlid=");
        buf.append(urlid + "&nodeid=" + nodeid);
        String result = buf.toString();
        return result;
    }

    private String _addNode(String _urx, String userId, String sessionId) throws TopicSpacesException {
        smp.logDebug("WS_CohereExporter._addNode- " + _urx + " " + userId + " " + sessionId);
        String body = null;
        try {
            HttpURLConnection connection;
            connection = (HttpURLConnection) new URL(_urx).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Charset", "URF-8");
            String myCookie = "userid=" + userId;
            connection.setRequestProperty("Cookie", myCookie);
            myCookie = "Cohere=" + sessionId;
            connection.setRequestProperty("Cookie", myCookie);
            connection.connect();
            int rc = connection.getResponseCode();
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer bx = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                bx.append(line);
                bx.append('\r');
            }
            rd.close();
            body = bx.toString();
            smp.logDebug("WS_CohereExporter._addNode " + rc + " " + body);
        } catch (Exception e) {
            smp.logError("WS_CohereExporter._addNode error: " + e.getMessage());
            throw new TopicSpacesException(e);
        }
        return parseImport(body);
    }

    /**
	 * Just remove unicode stuff that comes in in strange ways.
	 * @param text
	 * @return
	 */
    String scrubUnicode(String text) {
        int len = text.length();
        StringBuilder buf = new StringBuilder();
        int c, x;
        for (int i = 0; i < len; i++) {
            c = text.charAt(i);
            if (c == 8220 || c == 8221) {
                buf.append('"');
            } else if (c > 8000) {
                buf.append(' ');
            } else if (c < 127) buf.append((char) c); else buf.append(' ');
        }
        String result = buf.toString();
        smp.logDebug("WS_CohereExporter.scrubUnicode+ " + result);
        return result;
    }

    private String parseImport(String xml) throws TopicSpacesException {
        smp.logDebug("WS_CohereExporter.parseImport " + xml);
        if (xml != null) {
            try {
                StringReader r = new StringReader(xml);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(r);
                String temp = null;
                String text = null;
                boolean isUser = false, isNode = false, isConnection = false, isURL = false, isLinktype = false, isRole = false, isFrom = false, isTo = false;
                int eventType = xpp.getEventType();
                boolean isStop = false;
                while (!(isStop || eventType == XmlPullParser.END_DOCUMENT)) {
                    temp = xpp.getName();
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                        System.out.println("Start document");
                    } else if (eventType == XmlPullParser.END_DOCUMENT) {
                        System.out.println("WS_CohereExporter game over");
                    } else if (eventType == XmlPullParser.START_TAG) {
                        System.out.println("Start tag " + temp);
                        text = null;
                        if (temp.equalsIgnoreCase("nodeset")) {
                        } else if (temp.equalsIgnoreCase("connectionset")) {
                        } else if (temp.equalsIgnoreCase("connections")) {
                        } else if (temp.equalsIgnoreCase("connection")) {
                            isConnection = true;
                        } else if (temp.equalsIgnoreCase("nodes")) {
                        } else if (temp.equalsIgnoreCase("cnode")) {
                            isNode = true;
                        } else if (temp.equalsIgnoreCase("connid")) {
                        } else if (temp.equalsIgnoreCase("nodeid")) {
                        } else if (temp.equalsIgnoreCase("name")) {
                        } else if (temp.equalsIgnoreCase("label")) {
                        } else if (temp.equalsIgnoreCase("creationdate")) {
                        } else if (temp.equalsIgnoreCase("modificationdate")) {
                        } else if (temp.equalsIgnoreCase("users")) {
                        } else if (temp.equalsIgnoreCase("user")) {
                            isUser = true;
                        } else if (temp.equalsIgnoreCase("userid")) {
                        } else if (temp.equalsIgnoreCase("private")) {
                        } else if (temp.equalsIgnoreCase("photo")) {
                        } else if (temp.equalsIgnoreCase("lastlogin")) {
                        } else if (temp.equalsIgnoreCase("description")) {
                        } else if (temp.equalsIgnoreCase("privatedata")) {
                        } else if (temp.equalsIgnoreCase("website")) {
                        } else if (temp.equalsIgnoreCase("sociallearnid")) {
                        } else if (temp.equalsIgnoreCase("isgroup")) {
                        } else if (temp.equalsIgnoreCase("hasdesc")) {
                        } else if (temp.equalsIgnoreCase("urls")) {
                        } else if (temp.equalsIgnoreCase("url")) {
                            isURL = true;
                        } else if (temp.equalsIgnoreCase("urlid")) {
                        } else if (temp.equalsIgnoreCase("title")) {
                        } else if (temp.equalsIgnoreCase("from")) {
                            isFrom = true;
                        } else if (temp.equalsIgnoreCase("to")) {
                            isTo = true;
                        } else if (temp.equalsIgnoreCase("linktype")) {
                            isLinktype = true;
                        } else if (temp.equalsIgnoreCase("linktypeid")) {
                        } else if (temp.equalsIgnoreCase("groupid")) {
                        } else if (temp.equalsIgnoreCase("grouplabel")) {
                        } else if (temp.equalsIgnoreCase("fromrole")) {
                        } else if (temp.equalsIgnoreCase("role")) {
                            isRole = true;
                        } else if (temp.equalsIgnoreCase("roleid")) {
                        } else if (temp.equalsIgnoreCase("torole")) {
                        } else if (temp.equalsIgnoreCase("fromcontexttypeid")) {
                        } else if (temp.equalsIgnoreCase("tocontexttypeid")) {
                        } else if (temp.equalsIgnoreCase("totalno")) {
                        } else if (temp.equalsIgnoreCase("start")) {
                        } else if (temp.equalsIgnoreCase("count")) {
                        } else if (temp.equalsIgnoreCase("connectedness")) {
                        } else if (temp.equalsIgnoreCase("startdatetime")) {
                        } else if (temp.equalsIgnoreCase("enddatetime")) {
                        } else if (temp.equalsIgnoreCase("image")) {
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        System.out.println("End tag " + temp + " // " + text);
                        if (temp.equalsIgnoreCase("nodeid")) {
                            if (isNode) return text;
                        } else if (temp.equalsIgnoreCase("urlid")) {
                            return text;
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        text = xpp.getText().trim();
                    } else if (eventType == XmlPullParser.CDSECT) {
                        text = xpp.getText().trim();
                    }
                    eventType = xpp.next();
                }
            } catch (Exception e) {
                throw new TopicSpacesException(e.getMessage());
            }
        }
        return null;
    }

    String cleanup(String in) {
        String result = in;
        smp.logDebug("WS_CohereExporter.cleanup- " + result);
        result.replaceAll("\\�", "'");
        smp.logDebug("WS_CohereExporter.cleanup+ " + result);
        return result;
    }
}
