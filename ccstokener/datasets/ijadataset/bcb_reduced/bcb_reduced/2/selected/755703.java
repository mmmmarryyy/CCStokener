package net.ogi.maven.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.SQLException;
import net.ogi.maven.db.ArtifactDB;
import net.ogi.maven.parsers.POMParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * @author Ognjen Bubalo
 * @version $Id$
 */
public class ArtifactsGetter {

    private ArtifactsGetter() {
        throw new AssertionError();
    }

    /**
	 * Gets artifacts with a specific prefix.
	 * @param prefix
	 * @param adb
	 * @param limitNum
	 * @return
	 * @throws UnknownHostException
	 */
    public static String getAndPutIntoDB(String prefix, ArtifactDB adb, int limitNum) throws UnknownHostException {
        String result = null;
        try {
            String urlStr = "http://search.maven.org/solrsearch/select?q=" + prefix + "&wt=json";
            JSONObject json = (JSONObject) JSONValue.parse(ArtifactsGetter.getData(urlStr));
            JSONObject responseObj = (JSONObject) JSONValue.parse(json.get("response").toString());
            int numFound = Integer.parseInt(responseObj.get("numFound").toString());
            System.out.println("numFound: " + numFound);
            if ((limitNum != 0) && (numFound > limitNum)) numFound = limitNum;
            urlStr = "http://search.maven.org/solrsearch/select?q=" + prefix + "&rows=" + numFound + "&wt=json";
            json = (JSONObject) JSONValue.parse(ArtifactsGetter.getData(urlStr));
            responseObj = (JSONObject) JSONValue.parse(json.get("response").toString());
            JSONArray array = (JSONArray) JSONValue.parse(responseObj.get("docs").toString());
            for (int i = 0; i < array.size(); ++i) {
                System.out.println(array.get(i));
                JSONObject project = (JSONObject) array.get(i);
                String timestamp = project.get("timestamp").toString();
                String groupId = project.get("g").toString();
                String artifactId = project.get("a").toString();
                System.out.println(timestamp + " " + groupId + " " + artifactId);
                urlStr = "http://search.maven.org/solrsearch/select?q=g:%22" + groupId + "%22%20AND%20a:%22" + artifactId + "%22%20AND%20v:%22%22%20&wt=json";
                JSONObject versions = (JSONObject) JSONValue.parse(ArtifactsGetter.getData(urlStr));
                responseObj = (JSONObject) JSONValue.parse(versions.get("response").toString());
                JSONArray versionArray = (JSONArray) JSONValue.parse(responseObj.get("docs").toString());
                for (int j = 0; j < versionArray.size(); ++j) {
                    JSONObject versionJSON = (JSONObject) versionArray.get(j);
                    String version = versionJSON.get("v").toString();
                    String delimiter = "\\.";
                    String[] temp = groupId.split(delimiter);
                    StringBuilder urlStrBuilder = new StringBuilder("http://search.maven.org/remotecontent?filepath=");
                    System.out.println("temps length: " + temp.length);
                    urlStrBuilder.append(temp[0]);
                    for (int k = 1; k < temp.length; k++) {
                        System.out.println(temp[k]);
                        urlStrBuilder.append("/" + temp[k]);
                    }
                    System.out.println("groupId: " + groupId);
                    urlStrBuilder.append("/" + artifactId);
                    urlStrBuilder.append("/" + version);
                    urlStrBuilder.append("/" + artifactId + "-" + version + ".pom");
                    System.out.println("Whole URL: " + urlStrBuilder);
                    String data = null;
                    try {
                        data = (ArtifactsGetter.getData(urlStrBuilder.toString()));
                    } catch (FileNotFoundException e) {
                        System.out.println("NO POM FOR THIS");
                        continue;
                    } catch (UnknownHostException e) {
                        throw e;
                    } catch (NoRouteToHostException e) {
                        throw new UnknownHostException();
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    POMParser pp = new POMParser(data);
                    System.out.println(pp.getPomObject());
                    adb.insertPOM(pp.getPomObject(), false, timestamp);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoRouteToHostException e) {
            throw new UnknownHostException();
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * Gets data from the Maven Central Repository.
	 * 
	 * @param adb
	 * @param com
	 * @param org
	 * @param net
	 * @param prefix
	 * @return
	 * @throws UnknownHostException
	 * @throws SQLException
	 */
    public static String getCentralRepoArtifacts(ArtifactDB adb, boolean com, boolean org, boolean net, String prefix) throws UnknownHostException, SQLException {
        int limitNum = 0;
        File flgTry = new File("limit.flg");
        if (flgTry.exists()) limitNum = 10;
        try {
            adb.connectToDB();
            adb.initDB();
            if (com) {
                ArtifactsGetter.getAndPutIntoDB("com", adb, limitNum);
            }
            if (org) {
                ArtifactsGetter.getAndPutIntoDB("org", adb, limitNum);
            }
            if (net) {
                ArtifactsGetter.getAndPutIntoDB("net", adb, limitNum);
            }
            if (prefix != null && prefix.length() > 0) {
                ArtifactsGetter.getAndPutIntoDB(prefix, adb, limitNum);
            }
        } catch (SQLException e) {
            throw e;
        } catch (UnknownHostException e) {
            adb.initDB();
            adb.restoreTables();
            throw e;
        }
        return "a";
    }

    /**
	 * Sends an URL request and returns the answer.
	 * 
	 * @param urlStr
	 * @return the answer.
	 * @throws FileNotFoundException
	 * @throws UnknownHostException
	 */
    public static String getData(String urlStr) throws FileNotFoundException, UnknownHostException, IOException {
        String result = null;
        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            result = sb.toString();
        } catch (UnknownHostException e) {
            throw e;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (NoRouteToHostException e) {
            throw e;
        } catch (SocketException e) {
            throw new UnknownHostException();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
