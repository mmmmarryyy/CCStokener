package org.argouml.model.euml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.argouml.model.UmlException;
import org.argouml.model.XmiReader;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.xml.sax.InputSource;

/**
 * The implementation of the XmiReader for EUML2.
 */
class XmiReaderEUMLImpl implements XmiReader {

    /**
     * The model implementation.
     */
    private EUMLModelImplementation modelImpl;

    private static Set<String> searchDirs = new HashSet<String>();

    private Resource resource;

    /**
     * Constructor.
     * 
     * @param implementation
     *            The ModelImplementation.
     */
    public XmiReaderEUMLImpl(EUMLModelImplementation implementation) {
        modelImpl = implementation;
    }

    public int getIgnoredElementCount() {
        return 0;
    }

    public String[] getIgnoredElements() {
        return new String[0];
    }

    public Map getXMIUUIDToObjectMap() {
        if (resource == null) {
            throw new IllegalStateException();
        }
        Map<String, EObject> map = new HashMap<String, EObject>();
        Iterator<EObject> it = resource.getAllContents();
        while (it.hasNext()) {
            EObject o = it.next();
            map.put(resource.getURIFragment(o), o);
        }
        return map;
    }

    public Collection parse(InputSource inputSource) throws UmlException {
        return parse(inputSource, false);
    }

    public Collection parse(InputSource inputSource, boolean readOnly) throws UmlException {
        if (inputSource == null) {
            throw new NullPointerException("The input source must be non-null.");
        }
        InputStream is = null;
        boolean needsClosing = false;
        if (inputSource.getByteStream() != null) {
            is = inputSource.getByteStream();
        } else if (inputSource.getSystemId() != null) {
            try {
                URL url = new URL(inputSource.getSystemId());
                if (url != null) {
                    is = url.openStream();
                    if (is != null) {
                        is = new BufferedInputStream(is);
                        needsClosing = true;
                    }
                }
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }
        }
        if (is == null) {
            throw new UnsupportedOperationException();
        }
        modelImpl.clearEditingDomain();
        Resource r = UMLUtil.getResource(modelImpl, UMLUtil.DEFAULT_URI, readOnly);
        try {
            modelImpl.getModelEventPump().stopPumpingEvents();
            r.load(is, null);
        } catch (IOException e) {
            throw new UmlException(e);
        } finally {
            modelImpl.getModelEventPump().startPumpingEvents();
            if (needsClosing) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        resource = r;
        return r.getContents();
    }

    public boolean setIgnoredElements(String[] elementNames) {
        return false;
    }

    public String getTagName() {
        if (resource == null) {
            throw new IllegalStateException();
        }
        List l = resource.getContents();
        if (!l.isEmpty()) {
            return "uml:" + modelImpl.getMetaTypes().getName(l.get(0));
        } else {
            return null;
        }
    }

    public void addSearchPath(String path) {
        searchDirs.add(path);
    }

    public List<String> getSearchPath() {
        return new ArrayList<String>(searchDirs);
    }

    public void removeSearchPath(String path) {
        searchDirs.remove(path);
    }

    public String getHeader() {
        throw new NotYetImplementedException();
    }
}
