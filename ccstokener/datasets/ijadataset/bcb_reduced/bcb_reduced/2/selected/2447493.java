package org.thechiselgroup.choosel.example.workbench.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.thechiselgroup.choosel.core.client.util.ServiceException;
import org.thechiselgroup.choosel.example.workbench.client.services.ProxyService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class ProxyServiceImpl extends RemoteServiceServlet implements ProxyService {

    @Override
    public String fetchURL(String urlString) throws ServiceException {
        try {
            URL url = new URL(urlString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String content = "";
            String line;
            while ((line = reader.readLine()) != null) {
                content += line + "\n";
            }
            reader.close();
            return content;
        } catch (MalformedURLException e) {
            throw new ServiceException(e.getMessage());
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
