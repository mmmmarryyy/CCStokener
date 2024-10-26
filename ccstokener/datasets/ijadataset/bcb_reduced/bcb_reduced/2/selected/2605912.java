package services.core.commands.state;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import services.core.commands.SimpleBinaryCommandResponse;
import services.core.commands.SimpleCommand;
import services.core.commands.SimpleCommandRequest;
import services.core.commands.SimpleCommandResponse;

public class FetchResourceCommand extends SimpleCommand {

    public static final String RESOURCE_HOME = "RESOURCE_HOME";

    public static final String SPECIAL_PARAMETER_RESOURCE_FILE_PATH = "resourceFilePath";

    public SimpleCommandResponse execute(SimpleCommandRequest commandRequest) {
        String[] requiredParameters = new String[] { "resourceFilePath" };
        if (!commandRequest.doesRequiredParametersExist(requiredParameters)) {
            return SimpleCommandResponse.createMissingParameterResponse(requiredParameters);
        }
        String resourceFilePath = commandRequest.getSimpleValue("resourceFilePath");
        URL resourceFileUrl = getResourceFileUrl(resourceFilePath);
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String resourceContentType = fileNameMap.getContentTypeFor(resourceFileUrl.getFile());
        if (null != resourceContentType && resourceContentType.trim().length() > 0) {
            resourceContentType = "text/plain";
        }
        String resourceContentTypeParameter = commandRequest.getSimpleValue("resourceContentType");
        if (null != resourceContentTypeParameter && resourceContentTypeParameter.trim().length() > 0) {
            resourceContentType = resourceContentTypeParameter;
        }
        try {
            byte[] resourceContentData = getUrlContentsAsBytes(resourceFileUrl);
            return SimpleBinaryCommandResponse.createSuccessfulResponse(resourceContentType, resourceContentData);
        } catch (Exception e) {
            e.printStackTrace();
            return SimpleCommandResponse.createFailureResponse(e.getMessage());
        }
    }

    public String getResourceHomePath() {
        if (null != System.getProperty(RESOURCE_HOME)) {
            return System.getProperty(RESOURCE_HOME);
        } else if (null != System.getenv(RESOURCE_HOME)) {
            return System.getenv(RESOURCE_HOME);
        } else {
            return new File(".").getAbsolutePath();
        }
    }

    public URL getResourceFileUrl(String resourceFilePath) {
        URL propertyFile = null;
        try {
            if (resourceFilePath.startsWith("http")) {
                propertyFile = new URL(resourceFilePath);
            } else {
                propertyFile = (new File(getResourceHomePath() + "/" + resourceFilePath)).toURL();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return propertyFile;
    }

    protected static byte[] getUrlContentsAsBytes(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        InputStream urlInputStream = url.openStream();
        int urlFileLength = connection.getContentLength();
        byte[] urlFileData = new byte[urlFileLength];
        urlInputStream.read(urlFileData);
        return urlFileData;
    }
}
