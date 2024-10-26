package org.apache.http.impl.conn;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

/**
 * Static helper methods.
 */
public final class Helper {

    /** Disabled default constructor. */
    private Helper() {
    }

    /**
     * Executes a request.
     */
    public static HttpResponse execute(HttpRequest req, HttpClientConnection conn, HttpHost target, HttpRequestExecutor exec, HttpProcessor proc, HttpParams params, HttpContext ctxt) throws Exception {
        ctxt.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        ctxt.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target);
        ctxt.setAttribute(ExecutionContext.HTTP_REQUEST, req);
        req.setParams(new DefaultedHttpParams(req.getParams(), params));
        exec.preProcess(req, proc, ctxt);
        HttpResponse rsp = exec.execute(req, conn, ctxt);
        rsp.setParams(new DefaultedHttpParams(rsp.getParams(), params));
        exec.postProcess(rsp, proc, ctxt);
        return rsp;
    }
}
