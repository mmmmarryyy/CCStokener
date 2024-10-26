package org.ncgr.cmtv.isys;

import org.ncgr.cmtv.*;
import org.ncgr.isys.system.*;
import org.ncgr.isys.objectmodel.LinearObject;
import org.ncgr.isys.objectmodel.LinearObjectPosition;
import org.ncgr.isys.objectmodel.LinearlyLocatedObject;
import org.ncgr.isys.objectmodel.impl.URLImpl;
import org.ncgr.isys.service.AnnotatedLinearObjectParser;
import org.ncgr.isys.service.AnnotatedLinearObjectListingParser;
import java.util.*;
import javax.swing.JFrame;
import java.net.URL;
import java.io.*;

/**
 * The <code>Service</code> that creates the <code>CompMapViewerClient</code> in a DynamicDiscovery context
 */
public class CompMapViewerService extends AbstractDynamicViewerService implements IsysConstants, CommandLineArgsService {

    public CompMapViewerService(ServiceProvider sp) {
        super(sp, COMPONENT_NAME, COMPONENT_DESCRIPTION);
    }

    public Client execute() {
        IsysObjectCollection ioc = getViewableData();
        Collection los = ioc.getObjectsWithAttributes(new Class[] { LinearObject.class });
        Collection llos = ioc.getObjectsWithAttributes(new Class[] { LinearlyLocatedObject.class });
        if (los.size() == 0 && llos.size() == 0) return null;
        CompMapViewerWrapper wrapper = ((CompMapViewerProvider) sp).getWrapper();
        wrapper.addLinearObjects(los);
        wrapper.addLinearlyLocatedObjects(llos);
        JFrame f = wrapper.getViewer().getMainFrame();
        f.show();
        ArrayList clients = new ArrayList(los.size());
        if (los.size() == 1) {
            for (Iterator itr = los.iterator(); itr.hasNext(); ) for (Iterator itr2 = ((IsysObject) itr.next()).getAttribute(LinearObject.class).iterator(); itr2.hasNext(); ) clients.add(wrapper.addMapDisplay((LinearObject) itr2.next()));
        }
        if (getSyncPartner() != null) {
            for (int i = 0; i < clients.size(); i++) {
                Isys.getInstance().synchronizeClients((Client) clients.get(i), getSyncPartner());
            }
        }
        if (clients.size() > 0) {
            return (Client) clients.get(0);
        } else {
            return null;
        }
    }

    public void invoke(String args[]) {
        System.err.println("invoked with args of size " + args.length);
        try {
            for (int i = 0; i < args.length; i++) {
                System.err.println("processing URL: " + args[i]);
                URL url = new URL(args[i]);
                AnnotatedLinearObjectParser parserObj = findParserForURL(url);
                if (parserObj == null) {
                    continue;
                }
                InputStream data = url.openStream();
                CompMapViewerWrapper wrapper = ((CompMapViewerProvider) sp).getWrapper();
                wrapper.parseIntoDataModel(data, new URLImpl(url.toString()), parserObj, false);
                JFrame f = wrapper.getViewer().getMainFrame();
                f.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    AnnotatedLinearObjectParser findParserForURL(URL url) {
        try {
            if (url.toString().startsWith("data:")) {
            }
            if (url.toString().indexOf("cmap") != -1) return (AnnotatedLinearObjectParser) Class.forName("org.ncgr.isys.ncgr.webcmap.CMapTabbedOutputParser").newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
