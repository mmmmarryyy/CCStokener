package net.taylor.portal.theme;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import net.taylor.portal.entity.theme.ThemeResource;
import org.jboss.seam.Component;

public class Util {

    private static final String PROTOCOL = "db://";

    private static final String FACELET_SELECT = "from ThemeResource where path = :viewId";

    public static boolean faceletExists(String viewId) {
        try {
            EntityManager em = getEntityManager();
            if (em == null) {
                return false;
            }
            Query qry = em.createQuery(FACELET_SELECT);
            qry.setParameter("viewId", format(viewId));
            qry.getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    public static ThemeResource getFacelet(String viewId) {
        Query qry = getEntityManager().createQuery(FACELET_SELECT);
        qry.setParameter("viewId", format(viewId));
        return (ThemeResource) qry.getSingleResult();
    }

    protected static String format(String viewId) {
        if (viewId.startsWith(PROTOCOL)) {
            return viewId.replace(PROTOCOL, "");
        } else {
            return viewId;
        }
    }

    public static URL createFacelectUrl(String viewId) {
        try {
            return new URL(null, PROTOCOL + viewId, new URLStreamHandler() {

                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    return new URLConnection(url) {

                        @Override
                        public void connect() throws IOException {
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            ThemeResource f = getFacelet(getURL().getFile());
                            return new ByteArrayInputStream(f.getText().getBytes());
                        }
                    };
                }
            });
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static EntityManager getEntityManager() {
        return (EntityManager) Component.getInstance("portalEntityManager");
    }
}
