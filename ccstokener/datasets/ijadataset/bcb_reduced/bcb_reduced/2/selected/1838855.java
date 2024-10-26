package org.nexopenframework.jaxws.vendor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.handler.PortInfo;
import org.codehaus.xfire.service.Service;
import org.nexopenframework.jaxws.JAXWSException;
import org.nexopenframework.jaxws.JAXWSVendorAdapter;
import org.nexopenframework.jaxws.handlers.xfire.XFireJAXWSHandler;
import org.xml.sax.InputSource;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Implementation of the {@link JAXWSVendorAdapter} for XFire 1.2.x releases.</p>
 * 
 * @see JAXWSVendorAdapter
 * @author <a href="mailto:fme@nextret.net">Francesc Xavier Magdaleno</a>
 * @version 1.0
 * @since 1.0
 */
public class XFireVendorAdapter implements JAXWSVendorAdapter {

    private String pattern = "/services/";

    /**
	 * @param pattern the pattern to set
	 */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
	 * <p>Registers the JSR-224 {@link javax.xml.ws.handler.Handler}'s
	 * with the NexOpen Bridge Adapter {@link XFireJAXWSHandler}.</p>
	 * 
	 * @see org.nexopenframework.jaxws.JAXWSVendorAdapter#postProcessEndpoint(javax.xml.ws.Endpoint)
	 */
    public void postProcessEndpoint(Endpoint ep) throws JAXWSException {
        try {
            Field service = ep.getClass().getDeclaredField("service");
            service.setAccessible(true);
            Service se = (Service) service.get(ep);
            se.addInHandler(new XFireJAXWSHandler(ep));
            se.addOutHandler(new XFireJAXWSHandler(ep, true));
        } catch (NoSuchFieldException e) {
            throw new JAXWSException("Not field service in Endpoint implementation", e);
        } catch (IllegalAccessException e) {
            throw new JAXWSException(e);
        }
    }

    /**
	 * <p></p>
	 * 
	 * @see org.nexopenframework.jaxws.JAXWSVendorAdapter#getServiceName(java.lang.String)
	 */
    public String getServiceName(String wsdlLocation) {
        if (wsdlLocation != null && wsdlLocation.length() > 0 && wsdlLocation.indexOf(pattern) > 0) {
            int end = wsdlLocation.indexOf("?wsdl");
            String serviceName = wsdlLocation.substring(wsdlLocation.indexOf(pattern) + pattern.length(), end);
            return serviceName;
        } else if (wsdlLocation != null && wsdlLocation.length() > 0) {
            try {
                QName qname = _getServiceName(wsdlLocation);
                return qname.getLocalPart();
            } catch (IOException e) {
            } catch (WSDLException e) {
            }
        }
        return "";
    }

    /**
	 * <p></p>
	 * 
	 * @see #createPortInfo(QName, QName, String)
	 * @see org.nexopenframework.jaxws.JAXWSVendorAdapter#createPortInfo(java.lang.String, java.lang.String)
	 */
    public PortInfo createPortInfo(String serviceName, String portName) throws JAXWSException {
        if ((serviceName != null && serviceName.length() > 0)) {
            QName qServiceName = QName.valueOf(serviceName);
            QName qPortName = null;
            if ((portName != null && portName.length() > 0)) {
                qPortName = QName.valueOf(portName);
            }
            return this.createPortInfo(qServiceName, qPortName, null);
        }
        return null;
    }

    /**
	 * <p></p>
	 *  
	 * @see org.nexopenframework.jaxws.JAXWSVendorAdapter#createPortInfo(javax.xml.namespace.QName, javax.xml.namespace.QName, java.lang.String)
	 */
    public PortInfo createPortInfo(QName serviceName, QName portName, String bindingID) throws JAXWSException {
        return new org.codehaus.xfire.jaxws.PortInfo(bindingID, portName, serviceName);
    }

    /**
	 * @param wsdlLocation
	 * @return
	 * @throws IOException 
	 * @throws WSDLException 
	 */
    protected QName _getServiceName(String wsdlLocation) throws IOException, WSDLException {
        URL url = new URL(wsdlLocation);
        InputStream is = null;
        QName service = null;
        try {
            is = url.openStream();
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
            Definition def = reader.readWSDL(null, new InputSource(is));
            Map services = def.getServices();
            if (services.size() == 1) {
                javax.wsdl.Service se = (javax.wsdl.Service) services.values().iterator().next();
                service = se.getQName();
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return service;
    }
}
