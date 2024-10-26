package org.blueoxygen.komodo.actions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.blueoxygen.jackrabbit.dao.RepositoryDAO;
import org.blueoxygen.jackrabbit.dao.RepositoryDAOAware;

/**
 * @author harry
 * email :  harry@intercitra.com
 */
public class DeleteArticle extends ViewArticle implements RepositoryDAOAware {

    private RepositoryDAO repoManager;

    public String execute() {
        String result = super.execute();
        if (result.equalsIgnoreCase(SUCCESS)) {
            try {
                repoManager.deleteAllVersion(repoManager.getRootNode().getNode(article.getId()));
                repoManager.deleteFile(repoManager.getRootNode(), article.getId());
            } catch (VersionException e) {
                e.printStackTrace();
            } catch (LockException e) {
                e.printStackTrace();
            } catch (ConstraintViolationException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            pm.remove(article);
            return SUCCESS;
        } else {
            addActionError("Artikel tidak ditemukan.");
            return ERROR;
        }
    }

    /**
	 * @param string
	 * @return
	 */
    private InputStream getResourceAsStream(String resourceName) {
        URL url = getResource(resourceName);
        try {
            return (url != null) ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
	 * @param resourceName
	 * @return
	 */
    private URL getResource(String resourceName) {
        URL url = null;
        url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (url == null) {
            url = AddArticle.class.getClassLoader().getResource(resourceName);
        }
        return url;
    }

    public void setRepositoryDAO(RepositoryDAO repositoryDAO) {
        this.repoManager = repositoryDAO;
    }
}
