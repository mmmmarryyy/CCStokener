package net.sourceforge.fluxion.datasource.impl;

import net.sourceforge.fluxion.api.DataSource;
import net.sourceforge.fluxion.api.DataSourceException;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;

/**
 * Factory for constructing new Datasources given a url to a jar file that
 * represents the datasource.
 *
 * @author Tony Burdett
 * @date 13-Mar-2009
 */
public class BeanModelDataSourceFactory {

    private ClassLoader loader;

    private Set<URL> jarUrls;

    /**
   * Default constructor for the factory that produces bean modelling
   * datasources.  Note that this will by default load all datasources on the
   * current classpath.
   */
    public BeanModelDataSourceFactory() {
        loader = getClass().getClassLoader();
        jarUrls = new HashSet<URL>();
    }

    /**
   * Constructor for the factory that produces bean modelling datasources.  This
   * will load datasources from the given classloader.
   *
   * @param classLoader the classloader to load datasources from
   */
    public BeanModelDataSourceFactory(ClassLoader classLoader) {
        loader = classLoader;
        jarUrls = new HashSet<URL>();
    }

    /**
   * Adds an extra URL to the factory, so that additional datasources can be
   * loaded from outside the current classloader.  {@link #createDataSources()}
   * will then generate datasources for all datasources found in the current
   * classloader or jar files at the additional urls specified.
   *
   * @param datasourceJarURL the url to load additional datasources from
   * @return true if the add operation succeeds
   */
    public boolean addAdditionalDataSource(URL datasourceJarURL) {
        return jarUrls.add(datasourceJarURL);
    }

    /**
   * Removes a URL to the factory, so that additional datasources will not be
   * loaded from outside the current classloader.  {@link #createDataSources()}
   * will generate datasources for all datasources found in the current
   * classloader or jar files at the additional urls specified, but no longer
   * including this one.
   *
   * @param datasourceJarURL the url to remove from datasources generation
   * @return true if the remove operation succeeds
   */
    public boolean removeAdditionalDataSource(URL datasourceJarURL) {
        return jarUrls.add(datasourceJarURL);
    }

    /**
   * Clears the set of urls from which additional datasources are loaded
   */
    public void clearAdditionalDataSources() {
        jarUrls.clear();
    }

    /**
   * Returns the classloader this factory is currently configured with.  Any
   * datasources found in this classloader will be included in datasource
   * creation
   *
   * @return the classloader for this factory
   */
    public ClassLoader getFactoryClassLoader() {
        return loader;
    }

    /**
   * Returns the list of urls that is currently configured as additional
   * datasource locations
   *
   * @return the currently configured list of additional datasources
   */
    public Set<URL> getAdditionalDataSources() {
        return jarUrls;
    }

    /**
   * Creates all datasources found either in the current classloader or at any
   * of the additional locations specified by {@link #getAdditionalDataSources()}.
   *
   * @return the set of datasources generated
   * @throws DataSourceException if there is a problem generating new
   *                             datasources
   */
    public Set<DataSource> createDataSources() throws DataSourceException {
        Set<DataSource> datasources = new HashSet<DataSource>();
        URL[] urlArray = jarUrls.toArray(new URL[jarUrls.size()]);
        ClassLoader contextLoader = new URLClassLoader(urlArray, loader);
        try {
            Enumeration<URL> urls = contextLoader.getResources("META-INF/artifact.properties");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                datasources.add(loadSingleDataSource(contextLoader, url));
            }
        } catch (IOException e) {
            throw new DataSourceException(e);
        }
        return datasources;
    }

    /**
   * Loads a single datasource, forcing isolation of resources based on the URL.
   * The supplied url parameter should be the location of the
   * artifact.properties file in a datasource.  This is used to obtain a base
   * url (i.e. the URL of the file, up to "META-INF/artifact.properties"), and
   * this base url used to load any child resources.
   *
   * @param contextLoader the classloader to use to load classes
   * @param url           the URL of the artifact.properties file that
   *                      designates this a datasource
   * @return the loaded datasource
   * @throws java.io.IOException if any resources could not be accessed as
   *                             expected
   * @throws net.sourceforge.fluxion.api.DataSourceException
   *                             if initializing the datasource failed
   */
    private DataSource loadSingleDataSource(ClassLoader contextLoader, URL url) throws IOException, DataSourceException {
        URI datasourceURI;
        OWLOntology datasourceOntology = null;
        URL baseURL = new URL(url.toString().replace("META-INF/artifact.properties", ""));
        Properties properties = new Properties();
        properties.load(url.openStream());
        String fileName = properties.get("db").toString() + ".owl";
        String pkg = properties.get("package").toString();
        datasourceURI = URI.create("http://" + properties.get("host").toString() + "/" + fileName);
        Set<Class> beans = new HashSet<Class>();
        if (baseURL.toString().startsWith("jar") && baseURL.toString().endsWith("!/")) {
            JarURLConnection jarConn = (JarURLConnection) baseURL.openConnection();
            Enumeration<JarEntry> entries = jarConn.getJarFile().entries();
            while (entries.hasMoreElements()) {
                JarEntry next = entries.nextElement();
                if (next.getName().startsWith(pkg.replace('.', '/')) && next.getName().endsWith(".class")) {
                    String fullClassName = next.getName().replace('/', '.').replace(".class", "");
                    try {
                        beans.add(contextLoader.loadClass(fullClassName));
                    } catch (ClassNotFoundException e) {
                        throw new DataSourceException("Unable to locate " + fullClassName + ".class", e);
                    }
                } else if (next.getName().equals(fileName)) {
                    String resName = next.getName();
                    URL owl = contextLoader.getResource(resName);
                    try {
                        datasourceOntology = OWLManager.createOWLOntologyManager().loadOntologyFromPhysicalURI(owl.toURI());
                    } catch (URISyntaxException e) {
                        throw new DataSourceException("Bad syntax converting url -> uri: " + owl.toString(), e);
                    } catch (OWLOntologyCreationException e) {
                        throw new DataSourceException("Couldn't create ontology from " + owl.toString(), e);
                    }
                }
            }
            if (beans.size() == 0) {
                throw new DataSourceException("Failed to load beans for the datasource at " + url);
            }
            if (datasourceOntology == null) {
                throw new DataSourceException("The datasource at " + url + " contains no ontology, or the ontology could not be loaded");
            }
            return new BeanModelDataSource(datasourceURI, datasourceOntology);
        } else {
            throw new DataSourceException("Unable to create a datasource, cannot load classes with the " + "given URL protocol (" + baseURL + ")");
        }
    }
}
