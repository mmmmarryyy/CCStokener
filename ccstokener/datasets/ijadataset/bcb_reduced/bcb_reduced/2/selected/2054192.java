package org.jomc.standalone.naming.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@javax.annotation.Generated(value = "org.jomc.tools.JavaSources", comments = "See http://jomc.sourceforge.net/jomc/1.0-alpha-8/jomc-tools")
public abstract class AbstractJPAContextFactory extends AbstractContextFactory {

    public final Context getInitialContext(final Hashtable<?, ?> environment) throws NamingException {
        final EntityManagerFactory entityManagerFactory = this.getPersistenceProvider().createContainerEntityManagerFactory(this.getPersistenceUnitInfo(), environment);
        this.getContext().bind(this.getEnvironment().getEntityManagerFactoryJndiName(), entityManagerFactory);
        this.getContext().bind(this.getEnvironment().getEntityManagerJndiName(), entityManagerFactory.createEntityManager());
        return null;
    }

    private static final String PERSISTENCE_NS = "http://java.sun.com/xml/ns/persistence";

    private PersistenceUnitInfo persistenceUnitInfo;

    protected abstract PersistenceProvider getPersistenceProvider();

    protected PersistenceUnitInfo getPersistenceUnitInfo() {
        if (this.persistenceUnitInfo == null) {
            this.persistenceUnitInfo = new PersistenceUnitInfo() {

                private List<ClassTransformer> transformers;

                private List<String> managedClasses;

                private List<String> mappingFileNames;

                private ClassLoader classLoader;

                public String getPersistenceUnitName() {
                    return "jomc-standalone";
                }

                public String getPersistenceProviderClassName() {
                    return getPersistenceProvider().getClass().getName();
                }

                public PersistenceUnitTransactionType getTransactionType() {
                    return PersistenceUnitTransactionType.JTA;
                }

                public DataSource getJtaDataSource() {
                    try {
                        return (DataSource) getContext().lookup(getEnvironment().getJtaDataSourceJndiName());
                    } catch (final NamingException e) {
                        getLogger().fatal(e);
                        throw new RuntimeException(e);
                    }
                }

                public DataSource getNonJtaDataSource() {
                    return null;
                }

                public List<String> getMappingFileNames() {
                    try {
                        if (this.mappingFileNames == null) {
                            this.mappingFileNames = new LinkedList<String>();
                            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            factory.setNamespaceAware(true);
                            factory.setValidating(false);
                            final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
                            for (final Enumeration<URL> e = this.getNewTempClassLoader().getResources("META-INF/persistence.xml"); e.hasMoreElements(); ) {
                                final URL url = e.nextElement();
                                final InputStream in = url.openStream();
                                final Document doc = documentBuilder.parse(in);
                                in.close();
                                final NodeList persistenceUnits = doc.getElementsByTagNameNS(PERSISTENCE_NS, "persistence-unit");
                                for (int i = persistenceUnits.getLength() - 1; i >= 0; i--) {
                                    final Element persistenceUnit = (Element) persistenceUnits.item(i);
                                    final NodeList mappingFiles = persistenceUnit.getElementsByTagNameNS(PERSISTENCE_NS, "mapping-file");
                                    for (int j = mappingFiles.getLength() - 1; j >= 0; j--) {
                                        final Element mappingFile = (Element) mappingFiles.item(j);
                                        this.mappingFileNames.add(mappingFile.getFirstChild().getNodeValue());
                                    }
                                }
                            }
                        }
                        return this.mappingFileNames;
                    } catch (final SAXException e) {
                        getLogger().fatal(e);
                        throw new RuntimeException(e);
                    } catch (final IOException e) {
                        getLogger().fatal(e);
                        throw new RuntimeException(e);
                    } catch (final ParserConfigurationException e) {
                        getLogger().fatal(e);
                        throw new RuntimeException(e);
                    }
                }

                public List<URL> getJarFileUrls() {
                    try {
                        final List<URL> jarFileUrls = new LinkedList<URL>();
                        for (final Enumeration<URL> unitUrls = this.getClassLoader().getResources("META-INF/persistence.xml"); unitUrls.hasMoreElements(); ) {
                            final URL unitUrl = unitUrls.nextElement();
                            final String externalForm = unitUrl.toExternalForm();
                            final String jarUrl = externalForm.substring(0, externalForm.indexOf("META-INF"));
                            jarFileUrls.add(new URL(jarUrl));
                        }
                        return jarFileUrls;
                    } catch (final IOException e) {
                        getLogger().fatal(e);
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }

                public URL getPersistenceUnitRootUrl() {
                    return getEnvironment().getJpaRootUrl();
                }

                public List<String> getManagedClassNames() {
                    try {
                        if (this.managedClasses == null) {
                            this.managedClasses = new LinkedList<String>();
                            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            factory.setNamespaceAware(true);
                            factory.setValidating(false);
                            final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
                            for (final Enumeration<URL> e = this.getNewTempClassLoader().getResources("META-INF/persistence.xml"); e.hasMoreElements(); ) {
                                final URL url = e.nextElement();
                                final InputStream in = url.openStream();
                                final Document doc = documentBuilder.parse(in);
                                in.close();
                                final NodeList persistenceUnits = doc.getElementsByTagNameNS(PERSISTENCE_NS, "persistence-unit");
                                for (int i = persistenceUnits.getLength() - 1; i >= 0; i--) {
                                    final Element persistenceUnit = (Element) persistenceUnits.item(i);
                                    final NodeList classes = persistenceUnit.getElementsByTagNameNS(PERSISTENCE_NS, "class");
                                    for (int j = classes.getLength() - 1; j >= 0; j--) {
                                        final Element clazz = (Element) classes.item(j);
                                        this.managedClasses.add(clazz.getFirstChild().getNodeValue());
                                    }
                                }
                            }
                        }
                        return this.managedClasses;
                    } catch (final SAXException e) {
                        getLogger().fatal(e);
                        throw new RuntimeException(e);
                    } catch (final IOException e) {
                        getLogger().fatal(e);
                        throw new RuntimeException(e);
                    } catch (final ParserConfigurationException e) {
                        getLogger().fatal(e);
                        throw new RuntimeException(e);
                    }
                }

                public boolean excludeUnlistedClasses() {
                    return false;
                }

                public Properties getProperties() {
                    return getEnvironment().getProperties();
                }

                public ClassLoader getClassLoader() {
                    if (this.classLoader == null) {
                        this.classLoader = this.getClass().getClassLoader();
                        if (this.classLoader == null) {
                            this.classLoader = ClassLoader.getSystemClassLoader();
                        }
                        this.classLoader = new URLClassLoader(new URL[] { getEnvironment().getJpaRootUrl() }, this.classLoader);
                    }
                    return this.classLoader;
                }

                public void addTransformer(final ClassTransformer transformer) {
                    if (this.transformers == null) {
                        this.transformers = new LinkedList<ClassTransformer>();
                    }
                    this.transformers.add(transformer);
                }

                public ClassLoader getNewTempClassLoader() {
                    final List<URL> jarFileUrls = this.getJarFileUrls();
                    jarFileUrls.add(getEnvironment().getJpaRootUrl());
                    return new URLClassLoader(jarFileUrls.toArray(new URL[jarFileUrls.size()]));
                }
            };
        }
        return this.persistenceUnitInfo;
    }

    /** Creates a new {@code AbstractJPAContextFactory} instance. */
    @javax.annotation.Generated(value = "org.jomc.tools.JavaSources", comments = "See http://jomc.sourceforge.net/jomc/1.0-alpha-8/jomc-tools")
    public AbstractJPAContextFactory() {
        super();
    }

    /**
     * Gets the {@code Logger} dependency.
     * <p>This method returns any available object of the {@code org.jomc.logging.Logger} specification at specification level 1.0.</p>
     * <p>That specification does not apply to any scope. A new object is returned whenever requested and bound to this instance.</p>
     * <p><b>Properties</b><dl>
     * <dt>"{@code name}"</dt>
     * <dd>Property of type {@code java.lang.String}.
     * </dd>
     * </dl>
     * @return The {@code Logger} dependency.
     * @throws org.jomc.ObjectManagementException if getting the dependency instance fails.
     */
    @javax.annotation.Generated(value = "org.jomc.tools.JavaSources", comments = "See http://jomc.sourceforge.net/jomc/1.0-alpha-8/jomc-tools")
    private org.jomc.logging.Logger getLogger() {
        final org.jomc.logging.Logger _d = (org.jomc.logging.Logger) org.jomc.ObjectManagerFactory.getObjectManager(this.getClass().getClassLoader()).getDependency(this, "Logger");
        assert _d != null : "'Logger' dependency not found.";
        return _d;
    }
}
