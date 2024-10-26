package net.datao.repository.impl;

import net.datao.ElmoRoles;
import net.datao.repository.GenericDataRepository;
import org.openrdf.elmo.*;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.openrdf.sail.memory.MemoryStore;
import org.apache.webdav.lib.WebdavFile;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.provider.webdav.WebdavFileObject;
import javax.xml.namespace.QName;
import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RDFDataRepositoryImpl implements GenericDataRepository {

    ElmoManager manager;

    static Repository repository;

    static Set<String> fileNames = new HashSet();

    public RDFDataRepositoryImpl() {
        try {
            initializeElmo();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ElmoManager getElmoManager() {
        return manager;
    }

    public RDFDataRepositoryImpl(String[] filenames) {
        try {
            initializeElmo();
            feedRepositoryFromFiles(filenames);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public RDFDataRepositoryImpl(List<URL> urls) {
        try {
            initializeElmo();
            feedRepositoryFromFiles(urls);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void initializeElmo() throws RepositoryException {
        ElmoModule module = new ElmoModule();
        initializeRoles(module);
        initializeRepository();
        initializeManager(module);
    }

    private void initializeManager(ElmoModule module) {
        ElmoManagerFactory factory = new SesameManagerFactory(module, repository);
        manager = factory.createElmoManager();
    }

    private void initializeRepository() throws RepositoryException {
        try {
            repository = new SailRepository(new MemoryStore());
            repository.initialize();
        } catch (RepositoryException e) {
            try {
                if (repositoryEnabled()) repository.shutDown();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw e;
        }
    }

    private void initializeRoles(ElmoModule module) {
        for (Class role : ElmoRoles.CONCEPTS) {
            module.addRole(role);
        }
        for (Class role : ElmoRoles.BEHAVIOURS) {
            module.addRole(role);
        }
    }

    protected boolean repositoryEnabled() {
        return repository != null && repository instanceof Repository;
    }

    protected boolean managerEnabled() {
        return manager != null && manager instanceof ElmoManager && manager.isOpen();
    }

    public static void main(String[] args) {
        RDFDataRepositoryImpl a = new RDFDataRepositoryImpl(args);
        System.out.println("Nothing is done yet to test the ElmoRepository");
    }

    public void feedRepositoryFromFiles(Set<File> files) {
        for (File f : files) {
            feedRepositoryFromFile(f);
        }
    }

    public void feedRepositoryFromFiles(String[] filenames) {
        for (String filename : filenames) {
            feedRepositoryFromFile(filename);
        }
    }

    public void feedRepositoryFromFiles(Collection<URL> urls) {
        for (URL url : urls) {
            feedRepositoryFromFile(url);
            System.out.println("Feeding ont at URL: " + url);
        }
    }

    public void feedRepositoryFromFile(String filename) {
        feedRepositoryFromFile(new File(filename));
    }

    public void feedRepositoryFromFile(URL url) {
        if (!repositoryEnabled()) throw new RuntimeException("readFromFile() was called when repository is not enabled. Quitting.");
        try {
            repository.getConnection().add(url.openStream(), "", RDFFormat.RDFXML, new URIImpl(url.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) throw (RuntimeException) e; else throw new RuntimeException(e);
        }
    }

    public void feedRepositoryFromFile(File f) {
        if (!repositoryEnabled()) throw new RuntimeException("readFromFile() was called when repository is not enabled. Quitting.");
        try {
            repository.getConnection().add(f, null, RDFFormat.RDFXML);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Set getItemsOfType(Class aClass) {
        if (!managerEnabled()) throw new RuntimeException("getItemsOfType() was called when manager is not enabled. Quitting.");
        System.err.println("getItemsOfType() was called for class " + aClass.getName());
        Set resultObjects = new HashSet();
        Iterable<Object> q = manager.findAll(aClass);
        for (Object d : q) {
            if (d.getClass().isAssignableFrom(aClass)) throw new RuntimeException("Item of QName " + (d instanceof Entity ? ((Entity) d).getQName() : "unknownQName") + " was retrieved when querying for " + aClass.toString() + ". But it is not of that type.");
            resultObjects.add(d);
        }
        return resultObjects;
    }

    public Object getID(Object item) {
        if (item instanceof Entity) {
            Entity e = (Entity) item;
            return e.getQName();
        } else {
            return null;
        }
    }

    public Object getItem(Object id) {
        if (id instanceof QName) {
            return manager.find((QName) id);
        } else {
            QName idASQName;
            try {
                idASQName = QName.valueOf(id.toString());
                return manager.find(idASQName);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public void exportCatalogAsWebdav() throws Exception {
        FileOutputStream fos = null;
        try {
            WebdavFile file = new WebdavFile(new HttpsURL("https://ggxg.ath.cx/webdav/catalog.rdf"));
            fos = new FileOutputStream(file);
            repository.getConnection().export(new RDFXMLPrettyWriter(fos));
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (fos != null) fos.close();
        }
    }

    public void exportCatalogAsPut() throws Exception {
        PipedInputStream in = null;
        PipedOutputStream out = null;
        try {
            in = new PipedInputStream();
            out = new PipedOutputStream();
            PutMethod put = new PutMethod("https://ggxg.ath.cx/webdav/catalog.rdf");
            put.setRequestBody(in);
            in.connect(out);
            repository.getConnection().export(new RDFXMLPrettyWriter(out));
            HttpClient c = new HttpClient();
            c.executeMethod(put);
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (in != null) in.close();
        }
    }

    public void exportCatalogAsFile() throws Exception {
        FileOutputStream fileWriter = null;
        try {
            File catalogFile = new File("serialization.rdf");
            fileWriter = new FileOutputStream(catalogFile);
            repository.getConnection().export(new RDFXMLPrettyWriter(fileWriter));
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (fileWriter != null) fileWriter.close();
        }
    }

    public void exportCatalogAsVFS() throws Exception {
        OutputStream fileWriter = null;
        try {
            FileSystemManager fsManager = VFS.getManager();
            WebdavFileObject catalog = (WebdavFileObject) fsManager.resolveFile("webdav://www.lolive.net:443/webdav/catalog.rdf");
            fileWriter = catalog.getOutputStream();
            repository.getConnection().export(new RDFXMLPrettyWriter(fileWriter));
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (fileWriter != null) fileWriter.close();
        }
    }

    public void exportWorkspace() throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new WebdavFile(new HttpsURL("https://ggxg.ath.cx/webdav/workspace.rdf")));
            repository.getConnection().export(new RDFXMLPrettyWriter(fos));
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (fos != null) fos.close();
        }
    }
}
