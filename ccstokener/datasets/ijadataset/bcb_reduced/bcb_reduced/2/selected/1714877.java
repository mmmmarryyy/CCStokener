package org.jaffa.wsapi.apis;

import com.predic8.soamodel.Difference;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.WSDLParser;
import com.predic8.wsdl.diff.WsdlDiffGenerator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONArray;
import org.apache.log4j.Logger;
import org.jaffa.wsapi.apis.data.WSDLCompareDetails;
import org.jaffa.wsapi.apis.data.WSDLCompareDto;

/**
 *
 * @author Saravanan
 */
public class WSDLCompareService {

    private static Logger log = Logger.getLogger(WSDLCompareService.class);

    private static final String IDENTICAL = "Identical";

    private static final String MODIFIED = "Modified";

    private static final String ADDED = "Added";

    private static final String REMOVED = "Removed";

    public JSONArray populateWsdlDiffList(WSDLCompareDto wsdlCompareDto) throws MalformedURLException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("Populate Wsdl List");
        }
        String remoteURL = wsdlCompareDto.getRemoteURL();
        String remoteUserId = wsdlCompareDto.getRemoteUserId();
        String remotePassword = wsdlCompareDto.getRemotePassword();
        String localURL = wsdlCompareDto.getLocalUrl();
        String userName = wsdlCompareDto.getUserName();
        String password = wsdlCompareDto.getPassword();
        JSONArray jsonArray = null;
        try {
            String remoteServiceHtml = getURLData(remoteURL, remoteUserId, remotePassword);
            Map<String, String> remoteWsdlMap = parse(remoteServiceHtml);
            if (log.isDebugEnabled()) {
                Set<Entry<String, String>> set = remoteWsdlMap.entrySet();
                Iterator<Entry<String, String>> itr = set.iterator();
                while (itr.hasNext()) {
                    Entry<String, String> entry = itr.next();
                    log.debug(entry);
                }
            }
            String localServiceHtml = getURLData(localURL, userName, password);
            Map<String, String> localWsdlMap = parse(localServiceHtml);
            if (log.isDebugEnabled()) {
                Set<Entry<String, String>> set = localWsdlMap.entrySet();
                Iterator<Entry<String, String>> itr = set.iterator();
                while (itr.hasNext()) {
                    Entry<String, String> entry = itr.next();
                    log.debug(entry);
                }
            }
            jsonArray = compare(remoteWsdlMap, localWsdlMap, remoteUserId, remotePassword, userName, password);
            if (log.isDebugEnabled()) {
                log.debug("result: " + jsonArray.toString());
            }
        } catch (MalformedURLException ex) {
            log.error(ex);
            throw ex;
        } catch (IOException ex) {
            log.error(ex);
            ex.printStackTrace();
            throw ex;
        }
        return jsonArray;
    }

    private JSONArray compare(Map<String, String> remoteWsdlMap, Map<String, String> localWsdlMap, String remoteUserId, String remotePwd, String userId, String password) throws MalformedURLException, IOException {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, String> entry : remoteWsdlMap.entrySet()) {
            WSDLCompareDetails details = new WSDLCompareDetails();
            String key = entry.getKey();
            if (localWsdlMap.containsKey(key)) {
                details.setRemoteURL(entry.getValue());
                details.setCurrentURL(localWsdlMap.get(key));
                details.setServiceName(key);
                String remoteWsdl = getURLData(entry.getValue(), remoteUserId, remotePwd);
                String localWsdl = getURLData(localWsdlMap.get(key), userId, password);
                String dataDir = System.getProperty("jboss.server.data.dir");
                String remote = dataDir + File.separator + "remote" + File.separator + key + ".wsdl";
                String local = dataDir + File.separator + "local" + File.separator + key + ".wsdl";
                if (!new File(dataDir + File.separator + "remote").exists()) {
                    new File(dataDir + File.separator + "remote").mkdirs();
                }
                if (!new File(dataDir + File.separator + "local").exists()) {
                    new File(dataDir + File.separator + "local").mkdirs();
                }
                File remoteFile = new File(remote);
                FileWriter remoteWsdlFile = new FileWriter(remoteFile);
                remoteWsdlFile.write(remoteWsdl);
                remoteWsdlFile.close();
                File localFile = new File(local);
                FileWriter localWsdlFile = new FileWriter(localFile);
                localWsdlFile.write(localWsdl);
                localWsdlFile.close();
                WSDLParser parser = new WSDLParser();
                Definitions remoteWsdlDef = parser.parse(remoteFile.getPath());
                Definitions localWsdlDef = parser.parse(localFile.getPath());
                WsdlDiffGenerator diffGen = new WsdlDiffGenerator(remoteWsdlDef, localWsdlDef);
                try {
                    List<Difference> diffList = diffGen.compare();
                    if (diffList.isEmpty()) {
                        details.setChanges(IDENTICAL);
                        details.setDescription(IDENTICAL);
                    } else {
                        details.setChanges(MODIFIED);
                        StringBuilder sb = new StringBuilder();
                        for (Difference diff : diffList) {
                            diffDescription(diff, "", sb);
                        }
                        if (diffList.size() == 1 && sb.toString().indexOf("The location of the port:") >= 0) {
                            details.setChanges(IDENTICAL);
                            details.setDescription(IDENTICAL);
                        } else {
                            details.setChanges(MODIFIED);
                            details.setDescription(sb.toString());
                        }
                    }
                } catch (Exception e) {
                    details.setChanges("Error");
                    details.setDescription("Error on comparison" + "<br>" + "Message :" + e.getMessage());
                }
                if (remoteFile != null && remoteFile.exists()) {
                    remoteFile.delete();
                }
                if (localFile != null && localFile.exists()) {
                    localFile.delete();
                }
            } else {
                details.setChanges(ADDED);
                details.setRemoteURL(entry.getValue());
                details.setCurrentURL("");
                details.setServiceName(key);
                details.setDescription("New WSDL");
            }
            array.add(details.toJson());
            localWsdlMap.remove(key);
        }
        for (Map.Entry<String, String> entry : localWsdlMap.entrySet()) {
            WSDLCompareDetails details = new WSDLCompareDetails();
            details.setChanges(REMOVED);
            details.setRemoteURL(REMOVED);
            details.setCurrentURL(entry.getValue());
            details.setServiceName(entry.getKey());
            details.setDescription("Removed from remote server");
        }
        return array;
    }

    private void diffDescription(Difference diff, String level, StringBuilder sb) {
        sb.append(level).append(diff.getDescription()).append("<br>");
        for (Difference localDiff : diff.getDiffs()) {
            diffDescription(localDiff, level + "&nbsp;&nbsp;", sb);
        }
    }

    private String getURLData(String urlString, String userId, String password) throws MalformedURLException, IOException {
        StringBuilder sb = new StringBuilder();
        if (!urlString.endsWith("?wsdl") && urlString.indexOf("jbossws/services") <= 0) {
            urlString += "/jbossws/services";
        }
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (urlString.endsWith("?wsdl")) {
            if (userId != null) {
                StringBuilder userPwd = new StringBuilder(userId);
                if (password != null) {
                    userPwd.append(':').append(password);
                }
                String encoding = new sun.misc.BASE64Encoder().encode(userPwd.toString().getBytes());
                urlConnection.setRequestProperty("Authorization", "Basic " + encoding);
            }
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        if (log.isDebugEnabled()) {
            log.debug("Html Data :" + sb.toString());
        }
        in.close();
        return sb.toString();
    }

    private Map<String, String> parse(String data) {
        Pattern p = Pattern.compile("<a href=(.*?)>(.*?)</a>");
        Matcher m = p.matcher(data);
        String link = null;
        String key = null;
        Map<String, String> wsdlLinkMap = new HashMap<String, String>();
        while (m.find()) {
            link = m.group(1);
            link = link.replaceAll("'", "");
            key = link.substring(link.lastIndexOf("/") + 1, link.indexOf("?wsdl"));
            if (wsdlLinkMap.containsKey(link)) {
                wsdlLinkMap.put(key, link);
            } else {
                wsdlLinkMap.put(key, link);
            }
        }
        return wsdlLinkMap;
    }
}
