package org.atricore.idbus.kernel.common.wagon.osgi;

import org.apache.maven.wagon.*;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class OsgiWagon extends StreamWagon {

    @Override
    public void fillInputData(InputData inputData) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        InputStream is;
        Resource resource = inputData.getResource();
        try {
            String name = resource.getName();
            String url = buildMvnUrl(resource);
            if (name.endsWith(".sha1") || name.contains("maven-metadata")) {
                is = new ByteArrayInputStream(new byte[0]);
            } else {
                is = new URL(url).openStream();
                if (is == null) throw new ResourceDoesNotExistException(resource.getName());
            }
        } catch (IOException e) {
            throw new ResourceDoesNotExistException(resource.getName());
        } catch (Exception e) {
            throw new TransferFailedException(e.getMessage(), e);
        }
        inputData.setInputStream(is);
    }

    @Override
    public void fillOutputData(OutputData outputData) throws TransferFailedException {
        throw new TransferFailedException("OSGi Wagon does not support write yet!");
    }

    @Override
    public void closeConnection() throws ConnectionException {
    }

    @Override
    public void openConnectionInternal() throws ConnectionException, AuthenticationException {
    }

    /**
     * Build a pax mvn url based on the resource name.
     *
     * <pre>
     * mvn-uri := 'mvn:' [ repository-url '!' ] group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]
     * repository-url := < rfc2396 uri > ; an url that points to a maven 2 repository
     * group-id := < maven groupId > ; group id of maven artifact
     * artifact-id := < maven artifactId > ; artifact id of maven artifact
     * version := < maven version > | 'LATEST' | range ; version of maven artifact
     * range := ( '[' | '(' ) version ',' version ( ')' | ']' )
     * type := < maven type> ; type of maven artifact
     * classifier := < maven classifier> ; maven artifact classifier
     * </pre>
     *
     */
    protected String buildMvnUrl(Resource resource) {
        String url = null;
        String name = resource.getName();
        String type = "";
        String classifier = "";
        String version = "";
        String groupId = "";
        String artifactId = "";
        String hash = "";
        if (name.contains("maven-metadata")) {
        } else if (name.endsWith("sha1")) {
            name = name.substring(0, name.lastIndexOf("."));
            hash = "sha1";
        }
        String resourceSimpleName = name.substring(name.lastIndexOf('/') + 1);
        type = resourceSimpleName.substring(resourceSimpleName.lastIndexOf(".") + 1);
        name = name.substring(0, name.lastIndexOf("/"));
        version = name.substring(name.lastIndexOf("/") + 1, name.length());
        name = name.substring(0, name.lastIndexOf("/"));
        artifactId = name.substring(name.lastIndexOf("/") + 1, name.length());
        groupId = name.substring(0, name.lastIndexOf("/"));
        groupId = groupId.replace("/", ".");
        String rs = artifactId + "-" + version + "." + type;
        if (rs.length() < resourceSimpleName.length()) {
            classifier = resourceSimpleName.substring(rs.length() - type.length() - 1, resourceSimpleName.length() - type.length() - 1);
        }
        url = "mvn:" + groupId + "/" + artifactId;
        if (version.length() > 0) url += "/" + version;
        if (type.length() > 0) {
            if (version.length() < 1) url += "/";
            url += "/" + type;
            if (hash.length() > 0) url += ".sha1";
        }
        if (classifier.length() > 0) {
            if (version.length() < 1) url += "/";
            if (type.length() < 1) url += "/";
            url += "/" + classifier;
        }
        return url;
    }
}
