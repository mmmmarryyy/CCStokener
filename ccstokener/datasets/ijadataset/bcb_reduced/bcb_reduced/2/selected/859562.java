package com.dfruits.queries.internal.queryproviders;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.java.custos.annots.Post;
import net.java.custos.annots.Pre;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.osgi.framework.Bundle;
import com.dfruits.queries.IQueryProvider;
import com.dfruits.queries.QueriesPlugin;
import com.dfruits.queries.internal.StreamUtils;
import com.dfruits.queries.model.DFruitsQueries;
import com.dfruits.queries.model.DocumentRoot;
import com.dfruits.queries.model.Query;
import com.dfruits.queries.model.Statement;
import com.dfruits.queries.model.util.DFruitsQueriesResourceFactoryImpl;

public class EMFModelQueriesProvider implements IQueryProvider {

    public List<Query> getQueries() {
        List<Query> ret = new ArrayList<Query>();
        loadExtensionPointStatementFiles(ret);
        return ret;
    }

    private void loadExtensionPointStatementFiles(List<Query> container) {
        Map<String, Query> queries = new HashMap<String, Query>();
        List<IConfigurationElement> metaViews = QueriesPlugin.getDefault().getConfigurationElements("xmlQueries", "xml-query");
        List<URL> xmlDeclarations = new ArrayList<URL>();
        Map<String, URL> sqlDeclarations = new HashMap<String, URL>();
        for (IConfigurationElement element : metaViews) {
            String pluginId = element.getContributor().getName();
            String path = element.getAttribute("path");
            loadFromBundle(pluginId, path, xmlDeclarations, sqlDeclarations);
        }
        resolveStatements(queries, xmlDeclarations, sqlDeclarations, container);
    }

    @Post(onThrown = "$log.error( ''while loading statements from plugin: " + "pluginId='' + pluginId + '', path='' + path + " + "'', message='' + $throwable.getMessage() )")
    private void loadFromBundle(String pluginId, String path, List<URL> xmlDeclarations, Map<String, URL> sqlDeclarations) {
        Bundle bundle = Platform.getBundle(pluginId);
        Enumeration<URL> bundleXmlDeclarations = bundle.findEntries(path, "*.xml", true);
        if (bundleXmlDeclarations != null) {
            while (bundleXmlDeclarations.hasMoreElements()) {
                URL nextURL = bundleXmlDeclarations.nextElement();
                xmlDeclarations.add(nextURL);
            }
        }
        Enumeration<URL> bundleSqlDeclarations = bundle.findEntries(path, "*", true);
        if (bundleSqlDeclarations != null) {
            while (bundleSqlDeclarations.hasMoreElements()) {
                URL nextURL = bundleSqlDeclarations.nextElement();
                String fileName = new File(nextURL.getFile()).getName();
                sqlDeclarations.put(fileName, nextURL);
            }
        }
    }

    private void resolveStatementRef(Query unresolved, Map<String, Query> queries) {
        Query reffedQuery = queries.get(unresolved.getStatement().getRef());
        if (reffedQuery != null) {
            unresolved.getStatement().setValue(reffedQuery.getStatement().getValue());
        } else {
            unresolved.getStatement().setValue(null);
        }
    }

    private void resolveFileDeclarations(Query query, Map<String, URL> sqlDeclarations) {
        Statement stmt = query.getStatement();
        String fileDeclaration = stmt.getFile();
        boolean hasFileDeclaration = fileDeclaration != null && !"".equals(fileDeclaration);
        if (hasFileDeclaration) {
            try {
                URL url = sqlDeclarations.get(stmt.getFile());
                if (url != null) {
                    URLConnection conn = url.openConnection();
                    InputStream input = conn.getInputStream();
                    String sqlDeclaration = StreamUtils.obtainStreamContents(input);
                    stmt.setValue(sqlDeclaration);
                    input.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private void resolveStatements(Map<String, Query> queries, List<URL> xmlDeclarations, Map<String, URL> sqlDeclarations, List<Query> container) {
        List<Query> unresolvedStatements = new ArrayList<Query>();
        for (URL url : xmlDeclarations) {
            try {
                List<Query> stmts = loadStatementFile(url, queries);
                unresolvedStatements.addAll(stmts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Query query : unresolvedStatements) {
            resolveStatementRef(query, queries);
        }
        for (Query query : queries.values()) {
            resolveFileDeclarations(query, sqlDeclarations);
        }
        container.addAll(queries.values());
    }

    @Pre(exec = "$log.info( ''loading statement file: '' + fileURL );" + "beforeCount = queries.size();")
    @Post(exec = "afterCount = queries.size();" + "$log.info( ''queries added from file: '' + (afterCount-beforeCount) )")
    private List<Query> loadStatementFile(URL fileURL, Map<String, Query> queries) throws URISyntaxException {
        List<Query> unresolvedStatements = new ArrayList<Query>();
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new DFruitsQueriesResourceFactoryImpl());
        URI file = URI.createURI(fileURL.toURI().toString());
        Resource resource = resourceSet.getResource(file, true);
        for (EObject eObject : resource.getContents()) {
            parseDocumentRoot((DocumentRoot) eObject, queries, unresolvedStatements, file);
        }
        return unresolvedStatements;
    }

    @Post(onThrown = "$log.error( $throwable.getMessage() ")
    private void parseDocumentRoot(DocumentRoot root, Map<String, Query> queries, List<Query> unresolvedStatements, URI file) {
        DFruitsQueries maa;
        EList<Query> queryList;
        boolean hasRef;
        Statement unresolved;
        Diagnostic diag = Diagnostician.INSTANCE.validate(root);
        if (diag.getSeverity() != Diagnostic.OK) {
            Throwable throwable = diag.getException();
            throw new RuntimeException("invalid query definition: " + file.path() + ", error: " + throwable.getMessage());
        }
        maa = root.getDfruitsQueries();
        queryList = maa.getQuery();
        for (Query sType : queryList) {
            queries.put(sType.getName(), sType);
            unresolved = sType.getStatement();
            hasRef = unresolved != null && unresolved.getRef() != null && !"".equals(unresolved.getRef());
            if (hasRef) {
                unresolvedStatements.add(sType);
            }
        }
    }
}
