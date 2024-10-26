package net.sourceforge.fluxion.beans.utils;

import net.sourceforge.fluxion.beans.RuncibleConstants;
import net.sourceforge.fluxion.beans.exception.SessionCheckException;
import net.sourceforge.fluxion.runcible.io.command.ChangeInvoker;
import net.sourceforge.fluxion.runcible.io.command.impl.DefaultChangeInvoker;
import net.sourceforge.fluxion.runcible.graph.manager.RuncibleChangeFactory;
import net.sourceforge.fluxion.runcible.graph.command.manager.GraphChangeFactory;
import net.sourceforge.fluxion.runcible.graph.mapping.MappingManager;
import net.sourceforge.fluxion.api.FluxionService;
import java.net.*;
import java.io.IOException;
import java.util.Set;
import javax.servlet.http.HttpSession;

/**
 * Javadocs go here
 *
 * @author Rob Davey
 * @date 20-Feb-2009
 */
public class ResourceUtils {

    public static boolean isUriResolvable(URI uri) throws IOException {
        try {
            if (uri.isAbsolute()) {
                URL url = uri.toURL();
                URLConnection connection = url.openConnection();
                System.out.println("Opened connection");
                connection.setConnectTimeout(100);
                System.out.println("Set timeout to " + connection.getConnectTimeout());
                connection.connect();
                System.out.println("Connecting...");
                long time = System.currentTimeMillis();
                if (connection instanceof HttpURLConnection) {
                    int response = ((HttpURLConnection) connection).getResponseCode();
                    System.out.println("(HTTP) took " + (System.currentTimeMillis() - time) + " ms");
                    if (response == HttpURLConnection.HTTP_OK) {
                        System.out.println("OK");
                        return true;
                    }
                } else {
                    System.out.println("(FILE) took " + (System.currentTimeMillis() - time) + " ms");
                    if (connection.getContentLength() != -1) {
                        System.out.println("OK");
                        return true;
                    }
                }
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void checkInvokersAndFactories(HttpSession session) throws SessionCheckException {
        MappingManager manager = (MappingManager) session.getAttribute(RuncibleConstants.MAPPING_MANAGER.key());
        if (manager.getMapping() != null) {
            System.out.println("Manager mapping OK");
            try {
                System.out.println("Creating backend Runcible objects...");
                ChangeInvoker changeInvoker = new DefaultChangeInvoker();
                RuncibleChangeFactory runcibleChangeFactory = new RuncibleChangeFactory(changeInvoker, manager.getMapping());
                GraphChangeFactory graphChangeFactory = new GraphChangeFactory(changeInvoker);
                session.setAttribute(RuncibleConstants.RUNCIBLE_CHANGE_FACTORY.key(), runcibleChangeFactory);
                session.setAttribute(RuncibleConstants.GRAPH_CHANGE_FACTORY.key(), graphChangeFactory);
                System.out.println("...done");
            } catch (Exception e) {
                e.printStackTrace();
                throw new SessionCheckException(e.getMessage());
            }
        } else {
            System.out.println("Mapping null");
            throw new SessionCheckException("Null mapping found. Mapping must be present for Invoker and Factory creation!");
        }
    }

    public static FluxionService getService(Set<FluxionService> services, URI publisherUri) {
        FluxionService fs = null;
        for (FluxionService s : services) {
            if (s.getId().equals(publisherUri)) {
                fs = s;
                break;
            }
        }
        return fs;
    }

    public static String buildDataProviderNode(String label) {
        return "<node label=\"" + label + "\"/>";
    }
}
