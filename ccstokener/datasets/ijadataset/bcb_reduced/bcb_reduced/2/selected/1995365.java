package org.blueoxygen.komodo.actions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Properties;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import org.blueoxygen.cimande.security.SessionCredentials;
import org.blueoxygen.cimande.security.SessionCredentialsAware;
import org.blueoxygen.jackrabbit.dao.RepositoryDAO;
import org.blueoxygen.jackrabbit.dao.RepositoryDAOAware;
import org.blueoxygen.komodo.ArtPublisher;
import org.blueoxygen.komodo.Article;
import org.blueoxygen.komodo.Contributor;
import org.blueoxygen.komodo.Creator;
import org.blueoxygen.komodo.Format;
import org.blueoxygen.komodo.Language;
import org.blueoxygen.komodo.Type;
import org.blueoxygen.komodo.category.ArticleCategory;

/**
 * @author leo
 * email :  leo@meruvian.org
 */
public class EditArticle extends ArticleForm implements SessionCredentialsAware, RepositoryDAOAware {

    private RepositoryDAO repoManager;

    private SessionCredentials sess;

    private String id;

    private File docUpload;

    private String docUploadFileName;

    private String docUploadContentType;

    public String execute() {
        article = (Article) pm.getById(Article.class, getId());
        creator = (Creator) pm.getById(Creator.class, getCreatorId());
        artPublisher = (ArtPublisher) pm.getById(ArtPublisher.class, getArtPublisherId());
        contributor = (Contributor) pm.getById(Contributor.class, getContributorId());
        language = (Language) pm.getById(Language.class, getLanguageId());
        type = (Type) pm.getById(Type.class, getTypeId());
        format = (Format) pm.getById(Format.class, getFormatId());
        articleCategory = (ArticleCategory) pm.getById(ArticleCategory.class, getCategoryId());
        if (article == null) {
            addActionError("Article not found");
            return ERROR;
        } else {
            if (getTitle().equalsIgnoreCase("")) {
                addActionError("Please insert the title");
            }
            if (getSubject().equalsIgnoreCase("")) {
                addActionError("Please insert the subject");
            }
            if (hasErrors()) {
                return INPUT;
            }
            logInfo = article.getLogInformation();
            logInfo.setLastUpdateBy(sess.getCurrentUser().getId());
            logInfo.setLastUpdateDate(new Timestamp(System.currentTimeMillis()));
            article.setLogInformation(logInfo);
            article.setTitle(getTitle());
            article.setSubject(getSubject());
            article.setDescription(getDescription());
            article.setIdentifier(getIdentifier());
            article.setSource(getSource());
            article.setRelation(getRelation());
            article.setCoverage(getCoverage());
            article.setRights(getRight());
            if (docUpload != null) {
                article.setDocUploadName(getDocUploadFileName());
                article.setDocUploadPath(docUpload.getPath());
                try {
                    repoManager.updateFile(repoManager.getRootNode().getNode(article.getId()), docUpload);
                } catch (PathNotFoundException e) {
                    e.printStackTrace();
                } catch (RepositoryException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            pm.save(article);
            id = article.getId();
            return SUCCESS;
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

    public void setSessionCredentials(SessionCredentials sessionCredentials) {
        this.sess = sessionCredentials;
    }

    public void setRepositoryDAO(RepositoryDAO repositoryDAO) {
        this.repoManager = repositoryDAO;
    }

    public File getDocUpload() {
        return docUpload;
    }

    public void setDocUpload(File docUpload) {
        this.docUpload = docUpload;
    }

    public String getDocUploadContentType() {
        return docUploadContentType;
    }

    public void setDocUploadContentType(String docUploadContentType) {
        this.docUploadContentType = docUploadContentType;
    }

    public String getDocUploadFileName() {
        return docUploadFileName;
    }

    public void setDocUploadFileName(String docUploadFileName) {
        this.docUploadFileName = docUploadFileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
