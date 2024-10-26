package com.xakcop.skgt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.xakcop.skgt.model.HiddenParams;

public class PageRetriever {

    public static final String SERVICE_URL = "http://gps.skgt-bg.com/Web/SelectByLine.aspx";

    private static final Logger log = Logger.getLogger(PageRetriever.class.getName());

    URL url;

    HiddenParams hiddenParams;

    Integer typeId;

    Integer lineId;

    Integer routeId;

    Integer stopId;

    public PageRetriever() {
        try {
            this.url = new URL(SERVICE_URL);
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, "Cannot make URL", e);
            throw new RuntimeException(e);
        }
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    public void setStopId(Integer stopId) {
        this.stopId = stopId;
    }

    public void setHiddenParams(HiddenParams hiddenParams) {
        this.hiddenParams = hiddenParams;
    }

    Map<String, String> getPostParams() {
        Map<String, String> result = new HashMap<String, String>();
        if (hiddenParams != null) {
            result.put("__VIEWSTATE", hiddenParams.getViewState());
            result.put("__EVENTVALIDATION", hiddenParams.getEventValidation());
        }
        if (typeId != null) {
            result.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$ddlTransportType");
            result.put("ctl00$ContentPlaceHolder1$ddlTransportType", typeId.toString());
            result.put("ctl00$ScriptManager1", "ctl00$ContentPlaceHolder1$upMain|ctl00$ContentPlaceHolder1$ddlTransportType");
        }
        if (lineId != null) {
            result.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$ddlLines");
            result.put("ctl00$ContentPlaceHolder1$ddlLines", lineId.toString());
            result.put("ctl00$ScriptManager1", "ctl00$ContentPlaceHolder1$upMain|ctl00$ContentPlaceHolder1$ddlLines");
        }
        if (routeId != null) {
            result.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$rblRoute$0");
            result.put("ctl00$ContentPlaceHolder1$rblRoute", routeId.toString());
            result.put("ctl00$ScriptManager1", "ctl00$ContentPlaceHolder1$upMain|ctl00$ContentPlaceHolder1$rblRoute$0");
        }
        if (stopId != null) {
            result.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$ddlStops");
            result.put("ctl00$ContentPlaceHolder1$ddlStops", stopId.toString());
            result.put("ctl00$ScriptManager1", "ctl00$ContentPlaceHolder1$upMain|ctl00$ContentPlaceHolder1$ddlStops");
        }
        return result;
    }

    public String fetchPage() {
        Map<String, String> params = getPostParams();
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            String query = "";
            for (Map.Entry<String, String> param : params.entrySet()) {
                query += param.getKey() + "=" + URLEncoder.encode(param.getValue(), "UTF-8") + "&";
            }
            writer.write(query);
            writer.close();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuffer sb = new StringBuffer();
                String line = null;
                while (true) {
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line);
                }
                String result = sb.toString();
                return result;
            } else {
                log.severe("Service returned: " + responseCode);
            }
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Service error", ex);
        }
        return null;
    }
}
