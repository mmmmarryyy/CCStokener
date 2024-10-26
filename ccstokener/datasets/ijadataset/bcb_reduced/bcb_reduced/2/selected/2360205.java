package br.ufpe.cin.ontocompo.module.owlrender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import br.ufpe.cin.ontocompo.module.exception.ModuleStorageException;
import br.ufpe.cin.ontocompo.module.exception.UnknownModuleException;
import br.ufpe.cin.ontocompo.module.model.Module;
import br.ufpe.cin.ontocompo.module.model.OWLModuleFormat;
import br.ufpe.cin.ontocompo.module.model.OWLModuleManager;

/**
 * Class AbstractOWLModuleStorer.java 
 
 *
 * @author Camila Bezerra (kemylle@gmail.com)
 * @date Jul 18, 2008
 */
public abstract class AbstractOWLModuleStorer implements OWLModuleStorer {

    public void storeModule(OWLModuleManager manager, Module module, URI physicalURI, OWLModuleFormat moduleFormat) throws ModuleStorageException, UnknownModuleException {
        try {
            OutputStream os;
            if (!physicalURI.isAbsolute()) {
                throw new ModuleStorageException("Physical URI must be absolute: " + physicalURI);
            }
            if (physicalURI.getScheme().equals("file")) {
                File file = new File(physicalURI);
                file.getParentFile().mkdirs();
                os = new FileOutputStream(file);
            } else {
                URL url = physicalURI.toURL();
                URLConnection conn = url.openConnection();
                os = conn.getOutputStream();
            }
            Writer w = new BufferedWriter(new OutputStreamWriter(os));
            storeModule(manager, module, w, moduleFormat);
        } catch (IOException e) {
            throw new ModuleStorageException(e);
        }
    }

    protected abstract void storeModule(OWLModuleManager manager, Module module, Writer writer, OWLModuleFormat format) throws ModuleStorageException, UnknownModuleException;
}
