package com.sin.server.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import org.json.JSONException;
import com.google.inject.Inject;
import com.sin.createcrcontext.CreateCrContext;
import com.sin.createcrcontext.CreateCrContextImpl;

public class CrwServiceImpl implements CrwService {

    private static final Logger log = Logger.getLogger(CrwServiceImpl.class.getName());

    private CreateCrContext createCrContext;

    private HttpURLConnection connection;

    private BufferedReader rd;

    private StringBuilder sb;

    private String outstr;

    @Inject
    public CrwServiceImpl(CreateCrContext createCrContext) {
        this.createCrContext = createCrContext;
    }

    @Override
    public String getOutStr(String urlstrMain, String urlStrRC, String User_Agent, String locale, String themes, String domain, String pathinfo) throws JSONException {
        log.warning("new request urlstrMain" + urlstrMain + " domain " + domain + " path " + pathinfo);
        try {
            URL url = new URL(urlstrMain);
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User_Agent", User_Agent);
            connection.setRequestProperty("locale", locale);
            connection.setRequestProperty("themes", themes);
            connection.setRequestProperty("domain", domain);
            connection.setRequestProperty("pathinfo", pathinfo);
            rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF8"));
            sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            CreateCrContextImpl createCrContext = new CreateCrContextImpl();
            outstr = createCrContext.makeFrom(domain, pathinfo, sb.toString());
            if (outstr != null && outstr.length() > 200) {
                return outstr;
            } else {
                log.severe("Cant return outst");
            }
        } catch (IOException e) {
            log.severe(e.getMessage());
        } finally {
            connection.disconnect();
            rd = null;
            sb = null;
            outstr = null;
        }
        return null;
    }
}
