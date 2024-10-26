package org.apache.shindig.gadgets.preload;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.JsonUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletResponse;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Processes a single batch of pipeline data into tasks.
 */
public class PipelinedDataPreloader {

    private final RequestPipeline requestPipeline;

    private final ContainerConfig config;

    private static Set<String> HTTP_RESPONSE_HEADERS = ImmutableSet.of("content-type", "location", "set-cookie");

    @Inject
    public PipelinedDataPreloader(RequestPipeline requestPipeline, ContainerConfig config) {
        this.requestPipeline = requestPipeline;
        this.config = config;
    }

    /** Create preload tasks from a batch of social and http preloads */
    public Collection<Callable<PreloadedData>> createPreloadTasks(GadgetContext context, PipelinedData.Batch batch) {
        List<Callable<PreloadedData>> preloadList = Lists.newArrayList();
        Collection<Object> socialRequest = Lists.newArrayList();
        for (Map.Entry<String, PipelinedData.BatchItem> preloadEntry : batch.getPreloads().entrySet()) {
            PipelinedData.BatchItem preloadItem = preloadEntry.getValue();
            switch(preloadItem.getType()) {
                case HTTP:
                    preloadList.add(new HttpPreloadTask(context, (RequestAuthenticationInfo) preloadItem.getData(), preloadEntry.getKey()));
                    break;
                case SOCIAL:
                    socialRequest.add(preloadItem.getData());
                    break;
                case VARIABLE:
                    preloadList.add(new VariableTask(preloadEntry.getKey(), preloadItem.getData()));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown pipeline type");
            }
        }
        if (!socialRequest.isEmpty()) {
            preloadList.add(new SocialPreloadTask(context, socialRequest));
        }
        return preloadList;
    }

    /**
   * Hook for executing a JSON RPC fetch for social data.  Subclasses can override
   * to provide special handling (e.g., directly invoking a local API)
   *
   * @param request the social request
   * @return the response to the request
   * @throws GadgetException if there are errors processing the gadget spec
   */
    protected HttpResponse executeSocialRequest(HttpRequest request) throws GadgetException {
        return requestPipeline.execute(request);
    }

    private static class VariableTask implements Callable<PreloadedData> {

        private ImmutableMap<String, Object> result;

        public VariableTask(String key, Object data) {
            this.result = (data == null) ? ImmutableMap.of("id", (Object) key) : ImmutableMap.of("id", key, "data", data);
        }

        public PreloadedData call() throws Exception {
            return new PreloadedData() {

                public Collection<Object> toJson() throws PreloadException {
                    return ImmutableList.<Object>of(result);
                }
            };
        }
    }

    /**
   * Callable for issuing HttpRequests to JsonRpcServlet.
   */
    private class SocialPreloadTask implements Callable<PreloadedData> {

        private final GadgetContext context;

        private final Collection<? extends Object> socialRequests;

        public SocialPreloadTask(GadgetContext context, Collection<? extends Object> socialRequests) {
            this.context = context;
            this.socialRequests = socialRequests;
        }

        public PreloadedData call() throws Exception {
            HttpResponse response;
            String token = context.getParameter("st");
            if (token == null) {
                response = new HttpResponseBuilder().setHttpStatusCode(HttpServletResponse.SC_FORBIDDEN).setResponseString("Security token missing").create();
            } else {
                Uri uri = getSocialUri(context, token);
                String socialRequestsJson = JsonSerializer.serialize(socialRequests);
                HttpRequest request = new HttpRequest(uri).setIgnoreCache(context.getIgnoreCache()).setSecurityToken(context.getToken()).setMethod("POST").setAuthType(AuthType.NONE).setPostBody(CharsetUtil.getUtf8Bytes(socialRequestsJson)).addHeader("Content-Type", "application/json; charset=UTF-8").setContainer(context.getContainer()).setGadget(context.getUrl());
                response = executeSocialRequest(request);
            }
            String responseText;
            if (response.getHttpStatusCode() < 400) {
                responseText = response.getResponseAsString();
            } else {
                responseText = JsonSerializer.serialize(createJsonError(response.getHttpStatusCode(), null, response));
            }
            final List<Object> data = parseSocialResponse(socialRequests, responseText);
            return new PreloadedData() {

                public Collection<Object> toJson() {
                    return data;
                }
            };
        }
    }

    /**
   * Parse the response from a social request into a list of response objects
   */
    static List<Object> parseSocialResponse(Collection<? extends Object> requests, String response) throws JSONException {
        final List<Object> data = Lists.newArrayList();
        if (response.startsWith("[")) {
            JSONArray array = new JSONArray(response);
            for (int i = 0; i < array.length(); i++) {
                data.add(array.get(i));
            }
        } else {
            JSONObject error = new JSONObject(response);
            for (Object request : requests) {
                JSONObject itemResponse = new JSONObject();
                itemResponse.put("error", error);
                itemResponse.put("id", JsonUtil.getProperty(request, "id"));
                data.add(itemResponse);
            }
        }
        return data;
    }

    /** A task for loading os:HttpRequest */
    class HttpPreloadTask implements Callable<PreloadedData> {

        private final GadgetContext context;

        private final RequestAuthenticationInfo preload;

        private final String key;

        public HttpPreloadTask(GadgetContext context, RequestAuthenticationInfo preload, String key) {
            this.context = context;
            this.preload = preload;
            this.key = key;
        }

        public PreloadedData call() throws Exception {
            HttpRequest request = HttpPreloader.newHttpRequest(context, preload);
            String refreshIntervalStr = preload.getAttributes().get("refreshInterval");
            if (refreshIntervalStr != null) {
                try {
                    int refreshInterval = Integer.parseInt(refreshIntervalStr);
                    request.setCacheTtl(refreshInterval);
                } catch (NumberFormatException nfe) {
                }
            }
            String method = preload.getAttributes().get("method");
            if (method != null) {
                request.setMethod(method);
            }
            String params = preload.getAttributes().get("params");
            if ((params != null) && !"".equals(params)) {
                if ("POST".equalsIgnoreCase(request.getMethod())) {
                    request.setPostBody(params.getBytes("UTF-8"));
                    request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                } else {
                    UriBuilder uriBuilder = new UriBuilder(request.getUri());
                    String query = uriBuilder.getQuery();
                    query = query == null ? params : query + '&' + params;
                    uriBuilder.setQuery(query);
                    request.setUri(uriBuilder.toUri());
                }
            }
            return new Data(requestPipeline.execute(request));
        }

        class Data implements PreloadedData {

            private final JSONObject data;

            public Data(HttpResponse response) {
                String format = preload.getAttributes().get("format");
                JSONObject wrapper = new JSONObject();
                try {
                    wrapper.put("id", key);
                    if (response.getHttpStatusCode() >= 400) {
                        wrapper.put("error", createJsonError(response.getHttpStatusCode(), null, response));
                    } else {
                        JSONObject data = new JSONObject();
                        wrapper.put("data", data);
                        data.put("status", response.getHttpStatusCode());
                        String responseText = response.getResponseAsString();
                        JSONObject headers = createJsonHeaders(response);
                        if (headers != null) {
                            data.put("headers", headers);
                        }
                        if (format == null || "json".equals(format)) {
                            try {
                                if (responseText.startsWith("[")) {
                                    data.put("content", new JSONArray(responseText));
                                } else {
                                    data.put("content", new JSONObject(responseText));
                                }
                            } catch (JSONException je) {
                                wrapper.remove("data");
                                wrapper.put("error", createJsonError(HttpResponse.SC_NOT_ACCEPTABLE, je.getMessage(), response));
                            }
                        } else {
                            data.put("content", responseText);
                        }
                    }
                } catch (JSONException outerJe) {
                    throw new RuntimeException(outerJe);
                }
                this.data = wrapper;
            }

            public Collection<Object> toJson() {
                return ImmutableList.<Object>of(data);
            }
        }
    }

    private static JSONObject createJsonHeaders(HttpResponse response) throws JSONException {
        JSONObject headers = null;
        for (String header : HTTP_RESPONSE_HEADERS) {
            Collection<String> values = response.getHeaders(header);
            if (values != null && !values.isEmpty()) {
                JSONArray array = new JSONArray();
                for (String value : values) {
                    array.put(value);
                }
                if (headers == null) {
                    headers = new JSONObject();
                }
                headers.put(header, array);
            }
        }
        return headers;
    }

    /**
   * Create {error: { code: [CODE], data: {content: "....", headers: {...}}}}
   */
    private static JSONObject createJsonError(int code, String message, HttpResponse response) throws JSONException {
        JSONObject error = new JSONObject();
        error.put("code", code);
        if (message != null) {
            error.put("message", message);
        }
        JSONObject data = new JSONObject();
        String responseText = response.getResponseAsString();
        if (StringUtils.isNotEmpty(responseText)) {
            data.put("content", responseText);
        }
        JSONObject headers = createJsonHeaders(response);
        if (headers != null) {
            data.put("headers", headers);
        }
        if (data.length() > 0) {
            error.put("data", data);
        }
        return error;
    }

    private Uri getSocialUri(GadgetContext context, String token) {
        String jsonUri = config.getString(context.getContainer(), "gadgets.osDataUri");
        Preconditions.checkNotNull(jsonUri, "No JSON URI available for social preloads");
        Preconditions.checkNotNull(token, "No token available for social preloads");
        UriBuilder builder = UriBuilder.parse(jsonUri.replace("%host%", context.getHost())).addQueryParameter("st", token);
        return builder.toUri();
    }
}
