package ee.webAppToolkit.example.website;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.google.inject.name.Named;
import ee.webAppToolkit.core.RequestMethod;
import ee.webAppToolkit.core.Result;
import ee.webAppToolkit.core.annotations.Context;
import ee.webAppToolkit.core.annotations.Delete;
import ee.webAppToolkit.core.annotations.Flash;
import ee.webAppToolkit.core.annotations.Get;
import ee.webAppToolkit.core.annotations.Optional;
import ee.webAppToolkit.core.annotations.Post;
import ee.webAppToolkit.core.annotations.Put;
import ee.webAppToolkit.localization.LocalizedString;
import ee.webAppToolkit.navigation.annotations.HideFromNavigation;
import ee.webAppToolkit.navigation.annotations.NavigationDisplayName;
import ee.webAppToolkit.parameters.annotations.Parameter;
import ee.webAppToolkit.rendering.RenderingController;

public class RestController extends RenderingController {

    @Get
    @NavigationDisplayName(@LocalizedString("navigation.rest"))
    public Result index(@Flash("result") String result) {
        return render(result);
    }

    @Post
    public void index(@Parameter("requestMethod") RequestMethod requestMethod, @Optional @Parameter("key") String key, @Optional @Parameter("value") String value, HttpServletRequest httpServletRequest, @Context String context) throws IOException {
        String result = _makeRestCall(requestMethod, key, value, httpServletRequest, context);
        flash.put("result", result);
        redirect("index");
    }

    @Get
    @HideFromNavigation
    public Result rest(@Named("store") Map<String, String> store) {
        return render(store, true);
    }

    @Put
    public Result rest(@Named("store") Map<String, String> store, @Parameter("key") String key, @Parameter("value") String value) {
        store.put(key, value);
        return output(key + " stored", true);
    }

    @Delete
    public Result rest(@Named("store") Map<String, String> store, @Parameter("key") String key) {
        store.remove(key);
        return output(key + " removed", true);
    }

    private String _makeRestCall(RequestMethod requestMethod, String key, String value, HttpServletRequest httpServletRequest, String context) throws MalformedURLException, IOException, ProtocolException, UnsupportedEncodingException {
        String file = context + "/" + "rest";
        if (requestMethod.equals(RequestMethod.DELETE)) {
            file += "?key=" + key;
        }
        URL url = new URL("http", httpServletRequest.getServerName(), httpServletRequest.getServerPort(), file);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(requestMethod.toString());
        switch(requestMethod) {
            case GET:
                {
                    break;
                }
            case PUT:
                {
                    connection.setDoOutput(true);
                    StringBuilder parameters = new StringBuilder();
                    parameters.append("key=");
                    parameters.append(URLEncoder.encode(key, "UTF-8"));
                    parameters.append("&value=");
                    parameters.append(URLEncoder.encode(value, "UTF-8"));
                    byte[] bytes = parameters.toString().getBytes();
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(bytes);
                    outputStream.flush();
                    outputStream.close();
                    break;
                }
            case DELETE:
                {
                    break;
                }
        }
        final char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(connection.getInputStream(), "UTF-8");
        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            if (read > 0) {
                out.append(buffer, 0, read);
            }
        } while (read >= 0);
        connection.getResponseCode();
        connection.disconnect();
        return out.toString();
    }
}
