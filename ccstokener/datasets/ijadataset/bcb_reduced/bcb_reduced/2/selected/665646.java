package net.flysource.client.network;

import net.flysource.client.FlyShareApp;
import net.flysource.client.exceptions.ConnectException;
import net.flysource.client.exceptions.EmptySearchResultsException;
import net.flysource.client.exceptions.NoDownloadSourcesException;
import net.flysource.client.exceptions.ServiceMapException;
import net.flysource.client.util.FSConfig;
import net.flysource.client.util.FSMessages;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;
import net.flysource.common.*;
import net.flysource.common.exceptions.NotConnectedException;
import net.flysource.common.exceptions.NetworkConfigException;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class ConnectionManager {

    Logger LOG = Logger.getLogger(ConnectionManager.class);

    public static final int NOT_CONNECTED = 0;

    public static final int CONNECTED = 1;

    private int status = NOT_CONNECTED;

    private String sessionId = "";

    private String currentUserId = "";

    private WebServices webServices = null;

    public ConnectionManager() {
    }

    public void init() throws ConnectException {
        String serviceAddr;
        try {
            serviceAddr = getServiceMap();
        } catch (ServiceMapException e) {
            throw new ConnectException(FSMessages.getMessage("net.noService"));
        }
        String url = "http://" + StringUtils.trimToEmpty(serviceAddr);
        HessianProxyFactory factory = new HessianProxyFactory();
        try {
            webServices = (WebServices) factory.create(WebServices.class, url);
        } catch (MalformedURLException e) {
            throw new ConnectException(FSMessages.getMessage("login.cantFindServer"));
        }
    }

    public boolean isConnected() {
        return status == CONNECTED;
    }

    public void login(String userid, String password) throws ConnectException {
        status = NOT_CONNECTED;
        setCurrentUserId("");
        sessionId = webServices.doLogin(userid, password, FSConfig.getSetting("net.clientPort"), FlyShareApp.CLIENT_VERSION);
        if (sessionId.equalsIgnoreCase(WebServices.INVALID_LOGIN)) throw new ConnectException(FSMessages.getMessage("login.invalid")); else if (sessionId.equalsIgnoreCase(WebServices.SERVER_PROBLEM)) throw new ConnectException(FSMessages.getMessage("login.serverProblem")); else if (sessionId.equalsIgnoreCase(WebServices.WRONG_CLIENT_VERSION)) throw new ConnectException(FSMessages.getMessage("login.wrongClientVersion")); else if (sessionId.equalsIgnoreCase(WebServices.SERVER_FULL)) throw new ConnectException(FSMessages.getMessage("login.serverFull"));
        setCurrentUserId(userid);
        status = CONNECTED;
    }

    public void logout() {
        if (isConnected()) {
            status = NOT_CONNECTED;
            try {
                webServices.doLogout(sessionId);
            } catch (HessianRuntimeException e) {
            }
        }
        sessionId = "";
        setCurrentUserId("");
    }

    public SearchResult[] doSearch(SearchRequest searchRequest) throws EmptySearchResultsException, NotConnectedException {
        if (status == NOT_CONNECTED) return null;
        SearchResult[] results = null;
        try {
            results = webServices.doSearch(sessionId, searchRequest);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
        if (results == null) throw new EmptySearchResultsException();
        return results;
    }

    public FileSource[] getFileSources(String filename, long crc) throws NoDownloadSourcesException, NotConnectedException {
        if (status == NOT_CONNECTED) return null;
        FileSource[] results = null;
        try {
            results = webServices.getFileSources(sessionId, filename, crc);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
        if (results == null) throw new NoDownloadSourcesException();
        return results;
    }

    public FileSource[] getUserSources(String userId, String filename, long crc) throws NoDownloadSourcesException, NotConnectedException {
        if (status == NOT_CONNECTED) return null;
        FileSource[] results = null;
        try {
            results = webServices.getUserSources(sessionId, userId, filename, crc);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
        if (results == null) throw new NoDownloadSourcesException();
        return results;
    }

    public String getNetworkStatus() throws NotConnectedException {
        try {
            return webServices.getNetworkStatusMessage(sessionId);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
    }

    public String getMessageofTheDay(String id) {
        StringBuffer mod = new StringBuffer();
        int serverModId = 0;
        int clientModId = 0;
        BufferedReader input = null;
        try {
            URL url = new URL(FlyShareApp.BASE_WEBSITE_URL + "/mod.txt");
            input = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            inputLine = input.readLine();
            try {
                clientModId = Integer.parseInt(id);
                serverModId = Integer.parseInt(inputLine);
            } catch (NumberFormatException e) {
            }
            if (clientModId < serverModId || clientModId == 0) {
                mod.append(serverModId);
                mod.append('|');
                while ((inputLine = input.readLine()) != null) mod.append(inputLine);
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } finally {
            try {
                input.close();
            } catch (Exception e) {
            }
        }
        return mod.toString();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void pingServer() throws NotConnectedException {
        try {
            webServices.pingServer(sessionId);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
    }

    public void refreshLibrary() throws NotConnectedException {
        try {
            webServices.clearDirectory(sessionId);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
    }

    public int getLatestVersion() {
        int version = -1;
        BufferedReader input = null;
        try {
            URL url = new URL(FlyShareApp.BASE_WEBSITE_URL + "/clientversion.txt");
            input = new BufferedReader(new InputStreamReader(url.openStream()));
            String rec = input.readLine();
            try {
                version = Integer.parseInt(rec);
            } catch (NumberFormatException e) {
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } finally {
            try {
                input.close();
            } catch (Exception e) {
            }
        }
        return version;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void sendLibrary(String sessionId, LibraryEntry[] entries) throws NotConnectedException {
        try {
            webServices.sendLibrary(sessionId, entries);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
    }

    public void removeEntries(String sessionId, String[] filenames) throws NotConnectedException {
        try {
            webServices.removeEntries(sessionId, filenames);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
    }

    public String getServiceMap() throws ServiceMapException {
        BufferedReader input = null;
        String serviceListing = null;
        try {
            URL url = new URL(FlyShareApp.BASE_WEBSITE_URL + "/servicemap.txt");
            input = new BufferedReader(new InputStreamReader(url.openStream()));
            serviceListing = StringUtils.trimToEmpty(input.readLine());
            if (serviceListing.length() == 0) throw new ServiceMapException();
            LOG.info("Using service " + serviceListing);
        } catch (MalformedURLException e) {
            throw new ServiceMapException();
        } catch (IOException e) {
            throw new ServiceMapException();
        } finally {
            try {
                input.close();
            } catch (Exception e) {
            }
        }
        return serviceListing;
    }

    public BrowseCategory[] getBrowseCategories() throws NotConnectedException, EmptySearchResultsException {
        if (status == NOT_CONNECTED) return null;
        BrowseCategory[] results = null;
        try {
            results = webServices.getBrowseCategories(sessionId);
        } catch (HessianRuntimeException e) {
            throw new NotConnectedException();
        }
        if (results == null) throw new EmptySearchResultsException();
        return results;
    }

    public void checkShareConfig() throws NetworkConfigException {
        webServices.checkSharePort(sessionId);
    }
}
