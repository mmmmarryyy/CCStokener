package bsys.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Maven wrapper
 * 
 * @author <a href="mailto:bbou@ac-toulouse.fr">Bernard Bou</a>
 */
public class Maven {

    private static final Logger LOG = Logger.getLogger(Maven.class);

    /**
	 * Central bsys.maven repositories
	 */
    static final List<String> theRepos = Maven.getRepos();

    /**
	 * Cache
	 */
    private static Map<String, String> theLocalRepositoryCache = new HashMap<String, String>();

    /**
	 * Get jar path in local repo
	 * 
	 * @param thisGroupId
	 *            groupid
	 * @param thisArtifactId
	 *            artifactid
	 * @param thisVersion
	 *            version
	 * @return jar path in local repo
	 * @throws ArtifactNotFoundException
	 * @throws MavenException
	 */
    public static String get(final String thisGroupId, final String thisArtifactId, final String thisVersion) throws ArtifactNotFoundException, MavenException {
        final String thisPath = ManageArtifact.getArtifact(thisGroupId, thisArtifactId, thisVersion, Maven.theRepos);
        return thisPath;
    }

    /**
	 * Get latest jar path in local repo
	 * 
	 * @param thisGroupId
	 *            groupid
	 * @param thisArtifactId
	 *            artifactid
	 * @return latest jar path in local repo
	 * @throws ArtifactNotFoundException
	 * @throws MavenException
	 */
    public static String getLatest(final String thisGroupId, final String thisArtifactId) throws ArtifactNotFoundException, MavenException {
        final String thisKey = thisGroupId + ":" + thisArtifactId;
        if (Maven.theLocalRepositoryCache.containsKey(thisGroupId + ":" + thisArtifactId)) return Maven.theLocalRepositoryCache.get(thisKey);
        final String thisLatest = Maven.queryLatestVersion(thisGroupId, thisArtifactId);
        Maven.LOG.debug("latest version is " + thisArtifactId + ':' + thisLatest);
        Maven.LOG.debug("downloading " + thisArtifactId + ':' + thisLatest);
        final String thisPath = Maven.get(thisGroupId, thisArtifactId, thisLatest);
        Maven.LOG.debug("downloaded to local " + thisPath);
        Maven.theLocalRepositoryCache.put(thisKey, thisPath);
        return thisPath;
    }

    /**
	 * Query artifact versions
	 * 
	 * @param thisRepoUrl
	 *            repo url
	 * @param thisGroupId
	 *            groupid
	 * @param thisArtifactId
	 *            artifactid
	 * @return set of versions
	 * @throws MalformedURLException
	 * @throws XPathExpressionException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
    private static Set<String> queryVersions(final String thisRepoUrl, final String thisGroupId, final String thisArtifactId) {
        final String thisMetaData = Maven.makeMetadataUrl(thisRepoUrl, thisGroupId, thisArtifactId);
        List<String> theseMetadataVersions = null;
        try {
            theseMetadataVersions = Maven.queryVersionsFromMetadata(thisMetaData);
            Maven.LOG.debug("metadata versions " + theseMetadataVersions);
        } catch (final Exception e) {
            Maven.LOG.debug("query versions from metadata : " + e.toString());
        }
        List<String> theseDirectoryVersions = null;
        try {
            theseDirectoryVersions = Maven.queryVersionsFromRepositoryDirectory(thisRepoUrl, thisGroupId, thisArtifactId);
            Maven.LOG.debug("directory versions " + thisArtifactId + ':' + theseDirectoryVersions);
        } catch (final Exception e) {
            Maven.LOG.debug("query versions from repo directory : " + e.toString());
        }
        final Set<String> theseVersions = new TreeSet<String>();
        if (theseMetadataVersions != null) {
            theseVersions.addAll(theseMetadataVersions);
        }
        if (theseDirectoryVersions != null) {
            theseVersions.addAll(theseDirectoryVersions);
        }
        if (theseVersions.isEmpty()) {
            Maven.LOG.warn("query versions returned none");
        }
        return theseVersions;
    }

    /**
	 * Query artifact versions
	 * 
	 * @param thisGroupId
	 *            groupid
	 * @param thisArtifactId
	 *            artifactid
	 * @return sorted set of versions
	 * @throws MalformedURLException
	 * @throws XPathExpressionException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
    public static SortedSet<ArtifactVersion> queryArtifactVersions(final String thisGroupId, final String thisArtifactId) {
        final Set<String> theseVersions = new TreeSet<String>();
        for (final String thisRepoUrl : Maven.theRepos) {
            theseVersions.addAll(Maven.queryVersions(thisRepoUrl, thisGroupId, thisArtifactId));
        }
        return Maven.toArtifactVersions(theseVersions);
    }

    /**
	 * Query latest artifact version
	 * 
	 * @param thisGroupId
	 *            groupid
	 * @param thisArtifactId
	 *            artifactid
	 * @return latest artifact version
	 */
    public static String queryLatestVersion(final String thisGroupId, final String thisArtifactId) {
        final Set<String> theseVersions = new TreeSet<String>();
        for (final String thisRepo : Maven.theRepos) {
            theseVersions.addAll(Maven.queryVersions(thisRepo, thisGroupId, thisArtifactId));
        }
        return Maven.getLatestVersion(theseVersions);
    }

    /**
	 * convert versions to artifact versions
	 * 
	 * @param theseVersions
	 *            list of versions
	 * @return list of artifact versions
	 */
    private static SortedSet<ArtifactVersion> toArtifactVersions(final Collection<String> theseVersions) {
        final SortedSet<ArtifactVersion> theseArtifactVersions = new TreeSet<ArtifactVersion>(new Comparator<ArtifactVersion>() {

            @Override
            public int compare(final ArtifactVersion thisVersion, final ArtifactVersion thatVersion) {
                final int thisResult = ((DefaultArtifactVersion) thisVersion).compareTo(thatVersion);
                return thisResult;
            }
        });
        for (final String thisVersion : theseVersions) {
            final ArtifactVersion thisArtifactVersion = new DefaultArtifactVersion(thisVersion);
            theseArtifactVersions.add(thisArtifactVersion);
        }
        return theseArtifactVersions;
    }

    /**
	 * Query artifact versions from metadata Version "//metadata/version" LastUpdated "//metadata/versioning/lastUpdated" LatestVersion
	 * "//metadata/versioning/latest" ReleasedVersion "//metadata/versioning/release" AvailableVersions "//metadata/versioning/versions/version" Snapshot
	 * "//metadata/versioning/snapshot" Timestamp "//metadata/versioning/snapshot/timestamp" BuildNumber "//metadata/versioning/snapshot/buildNumber"
	 * 
	 * @param thisMetaData
	 *            metadata url
	 * @return list of versions
	 * @throws MalformedURLException
	 * @throws XPathExpressionException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
    private static List<String> queryVersionsFromMetadata(final String thisMetaData) throws MalformedURLException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
        Maven.LOG.debug("metadata " + thisMetaData);
        final URL thisUrl = new URL(thisMetaData);
        final Document doc = Maven.getDocument(thisUrl);
        return Maven.getTexts("//metadata/versioning/versions/version", doc);
    }

    /**
	 * Query artifact versions from repository directory
	 * 
	 * @param thisRepoUrl
	 *            repo url
	 * @param thisGroupId
	 *            groupid
	 * @param thisArtifactId
	 *            artifactid
	 * @return list of versions
	 * @throws IOException
	 * @throws MalformedURLException
	 */
    private static List<String> queryVersionsFromRepositoryDirectory(final String thisRepoUrl, final String thisGroupId, final String thisArtifactId) throws MalformedURLException, IOException {
        final String url = Maven.makeFolderUrl(thisRepoUrl, thisGroupId, thisArtifactId);
        return new MavenRepositoryDirectoryScanner().scan(new InputStreamReader(new URL(url).openStream()));
    }

    /**
	 * Get Document
	 * 
	 * @param thisUrl
	 *            document url
	 * @return document
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
    private static Document getDocument(final URL thisUrl) throws SAXException, IOException, ParserConfigurationException {
        final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputStream thisInputStream = null;
        try {
            return builder.parse(thisInputStream = thisUrl.openStream());
        } finally {
            if (thisInputStream != null) {
                thisInputStream.close();
            }
        }
    }

    /**
	 * Get text contents of nodes matching xpath expression
	 * 
	 * @param thisXPathExpr
	 *            xpath expression
	 * @param thisDocument
	 *            document
	 * @return text contents of nodes matching xpath expression
	 * @throws XPathExpressionException
	 */
    private static List<String> getTexts(final String thisXPathExpr, final Document thisDocument) throws XPathExpressionException {
        final XPath thisXPath = XPathFactory.newInstance().newXPath();
        final XPathExpression thisExpression = thisXPath.compile(thisXPathExpr);
        final Object result = thisExpression.evaluate(thisDocument, XPathConstants.NODESET);
        if (result == null) return null;
        final List<String> thisList = new ArrayList<String>();
        final NodeList theseNodes = (NodeList) result;
        for (int i = 0; i < theseNodes.getLength(); i++) {
            thisList.add(theseNodes.item(i).getTextContent());
        }
        return thisList;
    }

    /**
	 * Extract latest artifact version
	 * 
	 * @param theseVersions
	 *            list of versions
	 * @return latest artifact version
	 */
    public static ArtifactVersion getLatestArtifactVersion(final Collection<String> theseVersions) {
        final SortedSet<ArtifactVersion> theseSortedVersions = Maven.toArtifactVersions(theseVersions);
        final ArtifactVersion thisLatestArtifact = theseSortedVersions != null && !theseSortedVersions.isEmpty() ? theseSortedVersions.last() : null;
        Maven.LOG.debug("latest " + thisLatestArtifact);
        return thisLatestArtifact;
    }

    /**
	 * Extract latest version
	 * 
	 * @param theseVersions
	 *            list of versions
	 * @return latest version
	 */
    public static String getLatestVersion(final Collection<String> theseVersions) {
        final ArtifactVersion thisLatestArtifact = Maven.getLatestArtifactVersion(theseVersions);
        return thisLatestArtifact == null ? null : thisLatestArtifact.toString();
    }

    /**
	 * Make metadata url
	 * 
	 * @param thisRepoUrl
	 *            repo url
	 * @param thisGroupId
	 *            groupid
	 * @param thisArtifactId
	 *            artifactid
	 * @return metadata url
	 */
    private static String makeFolderUrl(final String thisRepoUrl, final String thisGroupId, final String thisArtifactId) {
        return thisRepoUrl + "/" + thisGroupId.replace('.', '/') + "/" + thisArtifactId;
    }

    /**
	 * Make folder url
	 * 
	 * @param thisRepoUrl
	 *            repo url
	 * @param thisGroupId
	 *            groupid
	 * @param thisArtifactId
	 *            artifactid
	 * @return folder url
	 */
    private static String makeMetadataUrl(final String thisRepoUrl, final String thisGroupId, final String thisArtifactId) {
        return Maven.makeFolderUrl(thisRepoUrl, thisGroupId.replace('.', '/'), thisArtifactId) + "/bsys.maven-metadata.xml";
    }

    /**
	 * Get list of repositories
	 * 
	 * @return list of repositories
	 */
    private static List<String> getRepos() {
        final List<String> theseRepos = new ArrayList<String>();
        try {
            final Properties theseProperties = new Properties();
            theseProperties.loadFromXML(Maven.class.getResource("repos.xml").openStream());
            for (final Object thisValue : theseProperties.values()) {
                theseRepos.add((String) thisValue);
            }
        } catch (final Exception e) {
            Maven.LOG.error("repo list (defaulting to repos1 and repos2)", e);
            theseRepos.add("http://repo1.maven.org/maven2");
            theseRepos.add("http://repo2.maven.org/maven2");
        }
        return theseRepos;
    }
}
