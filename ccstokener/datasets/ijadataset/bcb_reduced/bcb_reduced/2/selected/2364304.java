package net.webappservicefixture.fixture;

import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSInput;
import java.net.URL;
import java.io.IOException;

class DefaultLSResourceResolver implements LSResourceResolver {

    private final String schemaBasePath;

    public DefaultLSResourceResolver(String aSchemaBasePath) {
        schemaBasePath = aSchemaBasePath;
    }

    public LSInput resolveResource(String aType, String aNamespaceURI, String aPublicId, String aSystemId, String aBaseURI) {
        LSInput result = resolveResource(aPublicId, aSystemId, aBaseURI, false);
        if (result == null) {
            Loggers.SERVICE_LOG.warn("couldn't find schema:" + aSystemId);
        }
        return result;
    }

    private LSInput resolveResource(String aPublicId, String aSystemId, String aBaseURI, boolean baseUsed) {
        LSInput lsInput = new DefaultLSInput();
        lsInput.setPublicId(aPublicId);
        lsInput.setSystemId(aSystemId);
        String base = null;
        try {
            int baseEndPos = -1;
            if (aBaseURI != null) {
                baseEndPos = aBaseURI.lastIndexOf("/");
            }
            if (baseEndPos <= 0) {
                if (baseUsed) {
                    return null;
                } else {
                    return resolveResource(aPublicId, aSystemId, schemaBasePath + "/" + aSystemId, true);
                }
            }
            base = aBaseURI.substring(0, baseEndPos);
            URL url = new URL(base + "/" + aSystemId);
            lsInput.setByteStream(url.openConnection().getInputStream());
            return lsInput;
        } catch (IOException e) {
            return resolveResource(aPublicId, aSystemId, base, baseUsed);
        }
    }
}
