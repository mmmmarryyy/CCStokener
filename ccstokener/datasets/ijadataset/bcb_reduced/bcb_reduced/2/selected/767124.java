package org.dbe.kb.mdrman;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import javax.jmi.model.ModelPackage;
import javax.jmi.model.MofPackage;
import javax.jmi.reflect.RefPackage;
import javax.jmi.reflect.RefObject;
import javax.jmi.xmi.*;
import org.netbeans.api.mdr.MDRepository;
import org.netbeans.api.mdr.MDRManager;
import org.netbeans.api.mdr.JMIMapper;
import org.netbeans.lib.jmi.mapping.FileStreamFactory;
import org.openide.util.Lookup;
import org.dbe.kb.metamodel.odm.OdmPackage;

/**
 * <p>MDR Manager</p>
 * <p>TUC/MUSIC 2004 </p>
 * @version 1.0
 */
class MDRmanager {

    public static String METAMODEL = null;

    public static String MODEL = null;

    public static String PACKAGE = null;

    public static String Metamodelfile = null;

    public static String MetamodelURL = null;

    public static String MetamodelData = null;

    public static MDRepository repository = null;

    public static void setMetamodelFile(String metamodelName, String metamodel) {
        Metamodelfile = metamodel;
        MetamodelURL = null;
        METAMODEL = metamodelName;
        MetamodelData = null;
    }

    public static void setMetamodelURL(String metamodelName, String metamodel) {
        MetamodelURL = metamodel;
        Metamodelfile = null;
        METAMODEL = metamodelName;
        MetamodelData = null;
    }

    public static void setMetamodelData(String metamodelName, String metamodel) {
        MetamodelURL = null;
        Metamodelfile = null;
        METAMODEL = metamodelName;
        MetamodelData = metamodel;
    }

    public static void prepare(String packageName, String modelinst) {
        MODEL = modelinst;
        PACKAGE = packageName;
    }

    public static void startTran() {
        if (repository != null) repository.beginTrans(true);
        System.out.println("Repository Start Transaction");
    }

    public static void endTran() {
        if (repository != null) repository.endTrans();
        System.out.println("Repository End Transaction");
    }

    public static void rollbackTran() {
        if (repository != null) repository.endTrans(false);
        System.out.println("Rollback Transaction");
    }

    public static void generateJMIinterfaces(String modelinst, String dirName) {
        RefObject meta = repository.getExtent(modelinst).refMetaObject();
        System.out.println("Start generating interfaces for " + modelinst);
        try {
            JMIMapper mapper = JMIMapper.getDefault();
            mapper.generate(new FileStreamFactory(new File(dirName)), meta);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("JMI Interfaces generated.");
    }

    public static void init() {
        repository = ((MDRManager) Lookup.getDefault().lookup(MDRManager.class)).getDefaultRepository();
        createExtends();
    }

    public static void initRep() {
        repository = ((MDRManager) Lookup.getDefault().lookup(MDRManager.class)).getDefaultRepository();
    }

    public static void createExtends() {
        if (MODEL == null && PACKAGE == null && METAMODEL == null) {
            return;
        }
        if (repository.getExtent(MODEL) != null) {
            return;
        }
        ModelPackage fooMMExtent = (ModelPackage) repository.getExtent(METAMODEL);
        try {
            if (fooMMExtent == null) {
                fooMMExtent = (ModelPackage) repository.createExtent(METAMODEL);
            }
            MofPackage fooMMPackage = findMDRmanagerPackage(fooMMExtent);
            if (fooMMPackage == null) {
                if (Metamodelfile != null) {
                    File f = new File(Metamodelfile);
                    loadModel(f, fooMMExtent);
                } else if (MetamodelURL != null) {
                    java.net.URL url = new java.net.URL(MetamodelURL);
                    loadModel(url.openStream(), fooMMExtent);
                } else {
                    loadModel(MetamodelData, fooMMExtent);
                }
                fooMMPackage = findMDRmanagerPackage(fooMMExtent);
            }
            repository.createExtent(MODEL, fooMMPackage);
        } catch (Exception saex) {
            saex.printStackTrace();
        }
    }

    public static void close() {
        repository.shutdown();
        repository = null;
    }

    private static MofPackage findMDRmanagerPackage(ModelPackage extent) {
        for (Iterator it = extent.getMofPackage().refAllOfClass().iterator(); it.hasNext(); ) {
            MofPackage temp = (MofPackage) it.next();
            if (temp.getName().equals(PACKAGE)) {
                return (MofPackage) temp;
            }
        }
        return null;
    }

    public static void loadModel(File file, RefPackage mofp) throws java.io.IOException, javax.jmi.xmi.MalformedXMIException {
        XmiReader xmiReader = (XmiReader) Lookup.getDefault().lookup(XmiReader.class);
        xmiReader.read(file.toURI().toString(), mofp);
    }

    public static void loadModel(InputStream in, RefPackage mofp) throws java.io.IOException, javax.jmi.xmi.MalformedXMIException {
        XmiReader xmiReader = (XmiReader) Lookup.getDefault().lookup(XmiReader.class);
        xmiReader.read(in, null, mofp);
    }

    public static void loadModel(String data, RefPackage mofp) throws java.io.IOException, javax.jmi.xmi.MalformedXMIException {
        XmiReader xmiReader = (XmiReader) Lookup.getDefault().lookup(XmiReader.class);
        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(data.getBytes());
        xmiReader.read(bis, null, mofp);
    }

    public static RefPackage getModelinst(String name) {
        return repository.getExtent(name);
    }

    public static void exportXMI(RefPackage rf, String filename) throws IOException {
        XmiWriter writer = (XmiWriter) Lookup.getDefault().lookup(XmiWriter.class);
        writer.write(new FileOutputStream(filename), rf, "1.0");
    }

    public static void exportXMI(java.util.Collection col, String filename) throws IOException {
        XmiWriter writer = (XmiWriter) Lookup.getDefault().lookup(XmiWriter.class);
        writer.write(new FileOutputStream(filename), col, "1.0");
    }

    public static void exportXMI(RefPackage rf, OutputStream outs) throws IOException {
        XmiWriter writer = (XmiWriter) Lookup.getDefault().lookup(XmiWriter.class);
        writer.write(outs, rf, "1.1");
    }

    public static void exportXMI(java.util.Collection col, OutputStream outs) throws IOException {
        XmiWriter writer = (XmiWriter) Lookup.getDefault().lookup(XmiWriter.class);
        writer.write(outs, col, "1.1");
    }

    public static void setCustomLookupProperty() {
        System.setProperty("org.openide.util.Lookup", "org.dbe.kb.mdrman.DBELookup");
    }

    public static void setRepositoryDirectoryProperty(String dir) {
        System.setProperty("org.netbeans.mdr.persistence.Dir", dir);
    }

    public static void setRepositoryFileNameProperty(String filename) {
        System.setProperty("org.netbeans.mdr.persistence.btreeimpl.filename", filename);
    }

    public static void setLogFileProperty(String filename) {
        System.setProperty("org.netbeans.lib.jmi.Logger.fileName", filename);
    }

    public static void setMemoryImplProperty() {
        System.setProperty("org.netbeans.mdr.storagemodel.StorageFactoryClassName", "org.netbeans.mdr.persistence.memoryimpl.StorageFactoryImpl");
    }
}
