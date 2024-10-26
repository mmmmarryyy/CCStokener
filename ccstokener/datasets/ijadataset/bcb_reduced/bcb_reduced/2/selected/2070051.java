package com.google.code.jahath.endpoint.vch;

import com.google.code.jahath.Connection;
import com.google.code.jahath.common.http.HttpConstants;
import com.google.code.jahath.common.http.HttpException;
import com.google.code.jahath.http.HttpGateway;
import com.google.code.jahath.http.HttpRequest;
import com.google.code.jahath.http.HttpResponse;
import com.google.code.jahath.tcp.SocketAddress;

public class VCHClient {

    private final HttpClient httpClient;

    public VCHClient(SocketAddress server, HttpGateway gateway) {
        httpClient = new HttpClient(server, gateway);
    }

    public Connection createConnection(String serviceName) throws VCHException {
        try {
            HttpRequest request = httpClient.createRequest(HttpRequest.Method.POST, "/services/" + serviceName);
            HttpResponse response = request.execute();
            switch(response.getStatusCode()) {
                case HttpConstants.StatusCodes.CREATED:
                    String location = Util.getRequiredHeader(response, HttpConstants.Headers.LOCATION);
                    String path = httpClient.getPath(location);
                    if (path == null) {
                        throw new VCHProtocolException("The server returned an unexpected value for the Location header (" + location + "): the location identified by the URL is on a different server.");
                    }
                    if (!path.startsWith("/connections/")) {
                        throw new VCHProtocolException("The server returned a location (" + location + ") that doesn't conform to the VC/H specification");
                    }
                    String connectionId = path.substring(13);
                    if (!isValidConnectionId(connectionId)) {
                        throw new VCHProtocolException("The server returned an invalid connection ID (" + connectionId + ")");
                    }
                    return new ConnectionImpl(httpClient, connectionId);
                case HttpConstants.StatusCodes.NOT_FOUND:
                    throw new NoSuchServiceException(serviceName);
                default:
                    throw Util.createException(response);
            }
        } catch (HttpException ex) {
            throw new VCHConnectionException(ex);
        }
    }

    private static boolean isConnectionIdChar(char c) {
        return 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || '0' <= c && c <= '9' || c == '-' || c == '.' || c == '_' || c == ':' || c == '!' || c == '@';
    }

    private static boolean isValidConnectionId(String connectionId) {
        int len = connectionId.length();
        if (len < 10) {
            return false;
        } else {
            for (int i = 0; i < len; i++) {
                if (!isConnectionIdChar(connectionId.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    public void shutdown() {
    }
}
