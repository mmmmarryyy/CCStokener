package com.bbn.skyheld;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import com.bbn.skyheld.util.*;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;

public class HeldSkyhookServer {

    private static final int PORT = 8000;

    private static final int REF_LIFETIME_SECONDS = 60;

    private static final String BASE_PATH = "/lis/";

    private static final String REF_PATH = "/ref/";

    private static final String HOSTNAME = "localhost";

    private HttpServer server;

    private HttpContext mainContext;

    private Map<InetAddress, HttpContext> contexts;

    private ScheduledExecutorService scheduler;

    public static void main(String[] args) throws IOException {
        HeldSkyhookServer srv = new HeldSkyhookServer();
        srv.run();
    }

    public HeldSkyhookServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        this.mainContext = server.createContext(BASE_PATH, new HeldHandler(this));
        this.mainContext.setAuthenticator(new PassThroughBasicAuthenticator());
        server.setExecutor(null);
        contexts = new HashMap<InetAddress, HttpContext>();
        scheduler = Executors.newScheduledThreadPool(1);
    }

    public void run() throws IOException {
        System.err.print("Starting HTTP server...");
        server.start();
        System.err.println("Done");
    }

    public boolean knownHost(InetAddress remoteHost) {
        return contexts.containsKey(remoteHost);
    }

    public URI addReferenceContext(final InetAddress remoteHost, HttpPost req) throws URISyntaxException {
        String refPath = REF_PATH + RandomID.nextId();
        HeldHandler refHandler = new HeldHandler(this, req);
        HttpContext refContext = server.createContext(refPath, refHandler);
        this.contexts.put(remoteHost, refContext);
        this.scheduler.schedule(new ReferenceContextCleaner(remoteHost, this), REF_LIFETIME_SECONDS, TimeUnit.SECONDS);
        return new URI("http://" + HOSTNAME + ((PORT != 80) ? ":" + PORT : "") + refPath);
    }

    public void removeReferenceContext(InetAddress remoteHost) {
        HttpContext refContext = this.contexts.get(remoteHost);
        this.server.removeContext(refContext);
        this.contexts.remove(remoteHost);
    }

    public String newEntity(InetAddress ip) {
        return "pres:" + ip.getHostAddress() + "@" + HOSTNAME;
    }

    private class HeldHandler implements HttpHandler {

        private static final String BACKGROUND_REQUEST_URI = "https://api.skyhookwireless.com/wps2/location";

        HttpClient client = new DefaultHttpClient();

        HttpPost cachedRequest = null;

        HeldSkyhookServer server = null;

        public HeldHandler(HeldSkyhookServer server) {
            this.server = server;
        }

        public HeldHandler(HeldSkyhookServer server, HttpPost cachedRequest) {
            this.server = server;
            this.cachedRequest = cachedRequest;
        }

        @Override
        public void handle(HttpExchange exch) throws IOException {
            System.err.println("Entered handler");
            HttpPost req = new HttpPost(BACKGROUND_REQUEST_URI);
            String requestText = "", responseText = "";
            String backgroundRequestText = "", backgroundResponseText = "";
            LocationRequest lrq = new LocationRequest();
            LocationResponse lrs = new LocationResponse();
            URI locationURI = null;
            try {
                requestText = IOUtils.toString(exch.getRequestBody());
                if (exch.getPrincipal() != null) {
                    lrq.setAccessUsername(exch.getPrincipal().getName());
                    lrq.setAccessPassword(exch.getPrincipal().getRealm());
                }
                if (requestText.length() > 0) lrq.initFromHeldRequest(requestText);
                if (this.cachedRequest != null) {
                    req = this.cachedRequest;
                } else {
                    backgroundRequestText = lrq.toSkyhookRequest();
                    req.setEntity(new StringEntity(backgroundRequestText));
                    req.setHeader("Content-Type", "text/xml");
                    req.setHeader("X-ForwardedFor", exch.getRemoteAddress().getAddress().getHostAddress());
                    InetAddress remoteHost = exch.getRemoteAddress().getAddress();
                    if (!this.server.knownHost(remoteHost)) {
                        locationURI = server.addReferenceContext(remoteHost, req);
                    }
                }
                HttpResponse resp = this.client.execute((HttpUriRequest) req);
                backgroundResponseText = IOUtils.toString(resp.getEntity().getContent());
                lrs.initFromSkyhookResponse(backgroundResponseText);
                URI[] locationURIs = new URI[1];
                locationURIs[0] = locationURI;
                lrs.setLocationURIs(locationURIs);
                if (lrq.isExact() && (!lrq.getLocationTypes().contains(LocationRequest.LocationType.civic)) && (!lrq.getLocationTypes().contains(LocationRequest.LocationType.any))) lrs.setCivic(null);
                if (lrq.isExact() && (!lrq.getLocationTypes().contains(LocationRequest.LocationType.geodetic)) && (!lrq.getLocationTypes().contains(LocationRequest.LocationType.any))) lrs.setGeodetic(null);
                if (lrq.isExact() && (!lrq.getLocationTypes().contains(LocationRequest.LocationType.locationURI)) && (!lrq.getLocationTypes().contains(LocationRequest.LocationType.any))) lrs.setGeodetic(null);
                responseText = lrs.toHeldResponse(this.server.newEntity(exch.getRemoteAddress().getAddress()));
                System.err.println(requestText);
                System.err.println(backgroundRequestText);
                System.err.println(backgroundResponseText);
                System.err.println(responseText);
                exch.sendResponseHeaders(200, responseText.length());
                exch.getResponseBody().write(responseText.getBytes());
                exch.close();
            } catch (Exception e) {
                e.printStackTrace();
                exch.sendResponseHeaders(500, 0);
                exch.close();
            }
        }
    }

    class PassThroughBasicAuthenticator extends Authenticator {

        @Override
        public Result authenticate(HttpExchange exch) {
            String user = "", pass = "";
            if (exch.getRequestHeaders().getFirst("Authorization") == null) {
                return new Authenticator.Failure(401);
            }
            String[] authHeader = exch.getRequestHeaders().getFirst("Authorization").split("( )+");
            if (!authHeader[0].equals("Basic")) {
                return new Authenticator.Failure(401);
            }
            String auth;
            try {
                auth = new String(Base64.decode(authHeader[1]));
            } catch (IOException e) {
                return new Authenticator.Failure(401);
            }
            String[] tokens = auth.split(":", 2);
            if (tokens.length != 2) return new Authenticator.Failure(401);
            user = tokens[0];
            pass = tokens[1];
            return new Authenticator.Success(new HttpPrincipal(user, pass));
        }
    }

    class ReferenceContextCleaner implements Callable<Object> {

        private HeldSkyhookServer server;

        private InetAddress remoteHost;

        public ReferenceContextCleaner(InetAddress remoteHost, HeldSkyhookServer server) {
            this.remoteHost = remoteHost;
            this.server = server;
        }

        @Override
        public Object call() throws Exception {
            server.removeReferenceContext(remoteHost);
            return null;
        }
    }

    class PrintContexts implements Runnable {

        private HeldSkyhookServer server;

        public PrintContexts(HeldSkyhookServer server) {
            this.server = server;
        }

        @Override
        public void run() {
            System.err.println("----------");
            for (InetAddress host : server.contexts.keySet()) {
                System.err.println(server.contexts.get(host).getPath());
            }
        }
    }
}
