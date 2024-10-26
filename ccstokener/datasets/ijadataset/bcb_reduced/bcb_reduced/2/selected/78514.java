package seamonster.svrs;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Vector;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLSession;
import seamonster.svrs.preferences.PreferenceQuery;

public class Connector {

    private static Vector<String> errors = new Vector<String>();

    private String svrsQuery = "/";

    private String svrsPayload = "";

    private enum Method {

        GET, POST, PUT, HEAD
    }

    private Method svrsMethod = Method.GET;

    private TrustManager[] trustAll;

    private SSLContext sslContext;

    private Writer out;

    private Response response;

    private static Connector _instance;

    public static Connector instance() {
        if (_instance == null) {
            _instance = new Connector();
        }
        errors.clear();
        return _instance;
    }

    public Connector() {
        try {
            trustAll = new javax.net.ssl.TrustManager[] { new javax.net.ssl.X509TrustManager() {

                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            } };
            sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (GeneralSecurityException e) {
            errors.add("A security exception occurred while setting up the SSL context.");
        }
    }

    /**
	 * Ask SVRS if a resource with the specific id exists already.
	 * @param id
	 * @return boolean
	 */
    public boolean contains(SVRSUUID id) {
        return contains(id, 0);
    }

    /**
	 * Ask SVRS if a resource with the specific id and version exists already.
	 * @param id
	 * @param version <= 0 gets the latest version
	 * @return boolean
	 */
    public boolean contains(SVRSUUID id, int version) {
        this.svrsMethod = Method.HEAD;
        this.svrsQuery = makeQuery("resource/" + id.toString());
        if (version > 0) this.svrsQuery += "/" + version;
        System.out.println("Method: " + this.svrsMethod + ", Query: " + this.svrsQuery);
        int response = performRequest();
        System.out.println("performRequest response: " + response);
        if (response < 300) {
            return true;
        }
        System.out.println("errors: " + errors.toString());
        return false;
    }

    /**
	 * Submit an upload request to SVRS.
	 * @param r
	 * @return HTTP ResponseCode (< 300 is success)
	 */
    public int uploadResource(String xml, SVRSUUID id) {
        this.svrsMethod = Method.PUT;
        this.svrsQuery = makeQuery("resource/" + id.toString());
        this.svrsPayload = xml;
        int response = performRequest();
        System.out.println("Responsecode: " + response);
        return response;
    }

    /**
	 * Send a search query to SVRS. 
	 * @param searchText
	 * @return HTTP ResponseCode (< 300 is success)
	 */
    public int search(String searchText) {
        this.svrsMethod = Method.POST;
        this.svrsQuery = makeQuery("search");
        this.svrsPayload = searchText;
        return performRequest();
    }

    /**
	 * Download a resource 
	 * @param id
	 * @param version <= 0 gets the latest version
	 * @return HTTP ResponseCode (< 300 is success)
	 */
    public int downloadResource(SVRSUUID id, int version) {
        this.svrsMethod = Method.GET;
        this.svrsQuery = makeQuery("resource/" + id.toString());
        if (version > 0) this.svrsQuery += "/" + version;
        return performRequest();
    }

    /**
	 * Perform the request based upon the three variables:
	 * svrsMethod, svrsQuery and svrsPayload.
	 * @return HTTP ResponseCode (< 300 is success)
	 */
    private int performRequest() {
        HttpURLConnection connection = null;
        int returnValue = 300;
        int retryAttempts = 0;
        int maxRetries = PreferenceQuery.getRetries();
        boolean failure = true;
        int increaseTimeout = 0;
        while (failure) {
            failure = false;
            try {
                connection = getConnection();
                if (connection == null) {
                    return returnValue;
                }
                if (this.svrsMethod == Method.GET) {
                    connection.setDoOutput(false);
                    connection.setRequestMethod("GET");
                } else if (this.svrsMethod == Method.HEAD) {
                    connection.setDoOutput(false);
                    connection.setRequestMethod("HEAD");
                } else {
                    connection.setRequestProperty("Content-Type", "application/xml");
                    connection.setRequestProperty("Content-Length", Integer.toString(this.svrsPayload.length()));
                    if (this.svrsMethod == Method.POST) {
                        connection.setDoOutput(true);
                        connection.setRequestMethod("POST");
                    } else if (this.svrsMethod == Method.PUT) {
                        connection.setDoOutput(true);
                        connection.setRequestMethod("PUT");
                    }
                }
                connection.connect();
                if (connection.getDoOutput()) {
                    out = new OutputStreamWriter(connection.getOutputStream());
                    out.write(this.svrsPayload);
                    out.flush();
                }
                if (connection.getResponseCode() == 201) {
                } else if (connection.getResponseCode() < 300 && this.svrsMethod != Method.HEAD) {
                    response = new Response();
                    try {
                        response.setReponseFromInputStream(connection.getInputStream());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (connection.getResponseCode() == 400) {
                    System.out.println("Responsecode 400:Failure");
                    returnValue = connection.getResponseCode();
                    response = new Response();
                    try {
                        response.setReponseFromInputStream(connection.getInputStream());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("Response from server:" + response.getResponseDoc().toString());
                } else if (connection.getResponseCode() == 401) {
                    String status = connection.getHeaderField("Auth-Status");
                    errors.add(status);
                    failure = true;
                } else if (connection.getResponseCode() == 403) {
                    increaseTimeout = increaseTimeout + 1;
                    if (increaseTimeout <= 10) {
                        Thread.sleep(increaseTimeout * 100);
                        failure = true;
                    } else {
                        increaseTimeout = 0;
                    }
                } else if (connection.getResponseCode() == 404) {
                    String status = "404 Not found";
                    errors.add(status);
                }
                returnValue = connection.getResponseCode();
            } catch (SocketTimeoutException e) {
                if (retryAttempts++ >= maxRetries) {
                    errors.add("connection timed out after " + maxRetries + " retries.");
                    e.printStackTrace();
                } else {
                    failure = true;
                }
            } catch (UnknownHostException e) {
                if (retryAttempts++ >= maxRetries) {
                    errors.add("unknown host");
                    e.printStackTrace();
                } else {
                    failure = true;
                }
            } catch (SocketException e) {
                errors.add("network error (" + e.getMessage() + ")");
            } catch (IOException e) {
                errors.add("I/O error (" + e.getMessage() + ")");
            } catch (Exception e) {
                errors.add("some unknown communication error occured: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                        out = null;
                    }
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                }
            }
        }
        return returnValue;
    }

    /**
	 * Sets up the connection to SVRS.
	 * @return
	 */
    private HttpURLConnection getConnection() {
        String urlString = PreferenceQuery.getAddress() + ":" + PreferenceQuery.getPort() + this.svrsQuery;
        System.out.println("urlstring: " + urlString);
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (PreferenceQuery.getPassword() != null && PreferenceQuery.getUsername() != null) {
                String auth = PreferenceQuery.getUsername() + ":" + PreferenceQuery.getPassword();
                String encodedAuth = Base64Coder.encodeString(auth);
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            }
            if (connection == null) {
                errors.add("unknown error creating SSL socket.");
                return null;
            }
            connection.setRequestProperty("User-Agent", "SeaMonster/");
            connection.setConnectTimeout((int) (PreferenceQuery.getConnectTimeout() * 1000.0));
            connection.setReadTimeout((int) (PreferenceQuery.getReadTimout() * 1000.0));
            connection.setInstanceFollowRedirects(true);
            return connection;
        } catch (MalformedURLException e) {
            errors.add("malformed URL");
            return null;
        } catch (IOException e) {
            errors.add("I/O error");
            return null;
        }
    }

    private String makeQuery(String string) {
        return PreferenceQuery.getPrefix() + string;
    }

    public static Vector<String> getErrors() {
        return errors;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
