package org.smartcc;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.smartcc.metadata.*;

/**
 * Baseclass for deployers.
 */
public abstract class Deployer {

    /**
     * Ask, if the deployer is able to deploy the specified unit.
     */
    public abstract boolean isResponsibleFor(URL url);

    /**
     * Ask, if the unit has been deployed already.
     */
    public abstract boolean isDeployed(URL url) throws DeploymentException;

    /**
     * Deploy a unit.
     */
    public abstract void deploy(URL url) throws DeploymentException;

    /**
     * Undeploy a unit.
     */
    public abstract void undeploy(URL url) throws DeploymentException;

    protected static Document getDocument(URL url) throws DeploymentException {
        try {
            InputSource source = new InputSource(url.openStream());
            return getDocument(source, url.toString());
        } catch (IOException e) {
            throw new DeploymentException(e.getMessage());
        }
    }

    protected static Document getDocument(InputSource source, String fileName) throws DeploymentException {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setValidating(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            EntityResolver entityResolver = new LocalEntityResolver();
            docBuilder.setEntityResolver(entityResolver);
            DescriptorErrorHandler errorHandler = new DescriptorErrorHandler();
            errorHandler.setFileName(fileName);
            docBuilder.setErrorHandler(errorHandler);
            return docBuilder.parse(source);
        } catch (SAXParseException e) {
            System.out.println(e.getMessage() + ":" + e.getColumnNumber() + ":" + e.getLineNumber());
            e.printStackTrace();
            throw new DeploymentException(e.getMessage(), e);
        } catch (SAXException e) {
            System.out.println(e.getException());
            throw new DeploymentException(e.getMessage(), e);
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }
}
