package visad.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import visad.Data;
import visad.DataImpl;
import visad.VisADException;

public class FunctionFormFamily extends FormFamily {

    public FunctionFormFamily(String name) {
        super(name);
    }

    /**
    * Base class which tries to perform an operation on an object
    * using the first valid Form.
    */
    abstract class FormFunction {

        /**
      * Return 'true' if this object's name applies to the given node.
      */
        abstract boolean check(FormFileInformer node);

        /**
      * Return an InputStream for the object.
      *
      * Used to read in the first block of the object.
      */
        abstract InputStream getStream() throws IOException;

        /**
      * The operation to be performed on the object.
      */
        abstract boolean function(FormNode node);

        /**
      * Perform an operation on an object
      * using the first valid Form.
      *
      * If a Form successfully performs the operation, return 'true'.
      */
        public boolean run() throws IOException {
            for (Enumeration en = forms.elements(); en.hasMoreElements(); ) {
                FormNode node = (FormNode) en.nextElement();
                if (node instanceof FormFileInformer) {
                    try {
                        if (check((FormFileInformer) node)) {
                            if (function(node)) {
                                return true;
                            }
                        }
                    } catch (Exception e) {
                    } catch (Error e) {
                    }
                }
            }
            byte[] block = new byte[2048];
            InputStream is = getStream();
            if (is != null) {
                is.read(block);
                is.close();
                for (Enumeration en = forms.elements(); en.hasMoreElements(); ) {
                    FormNode node = (FormNode) en.nextElement();
                    if (node instanceof FormFileInformer) {
                        try {
                            if (((FormFileInformer) node).isThisType(block)) {
                                if (function(node)) {
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                        } catch (Error e) {
                        }
                    }
                }
            }
            for (Enumeration en = forms.elements(); en.hasMoreElements(); ) {
                FormNode node = (FormNode) en.nextElement();
                try {
                    if (function(node)) {
                        return true;
                    }
                } catch (Exception e) {
                } catch (UnsatisfiedLinkError ule) {
                }
            }
            return false;
        }
    }

    /**
    * Perform an operation on a local file object
    * using the first valid Form.
    */
    abstract class FileFunction extends FormFunction {

        String name;

        public FileFunction() {
            name = null;
        }

        boolean check(FormFileInformer node) {
            return node.isThisType(name);
        }

        InputStream getStream() throws IOException {
            return new FileInputStream(name);
        }
    }

    /**
    * Save a Data object to a local file
    * using the first valid Form.
    */
    class SaveForm extends FileFunction {

        private Data data;

        private boolean replace;

        public SaveForm(String name, Data data, boolean replace) {
            this.name = name;
            this.data = data;
            this.replace = replace;
        }

        boolean function(FormNode node) {
            try {
                node.save(name, data, replace);
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        InputStream getStream() throws IOException {
            FileInputStream stream;
            try {
                stream = new FileInputStream(name);
            } catch (FileNotFoundException fnfe) {
                stream = null;
            }
            return stream;
        }
    }

    /**
    * Add a Data object to an existing local file
    * using the first valid Form.
    */
    class AddForm extends FileFunction {

        private Data data;

        private boolean replace;

        public AddForm(String name, Data data, boolean replace) {
            this.name = name;
            this.data = data;
            this.replace = replace;
        }

        boolean function(FormNode node) {
            try {
                node.add(name, data, replace);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
    }

    /**
    * Read a Data object from a local file
    * using the first valid Form.
    */
    class OpenStringForm extends FileFunction {

        private DataImpl data;

        public OpenStringForm(String name) {
            this.name = name;
            data = null;
        }

        boolean function(FormNode node) {
            try {
                data = node.open(name);
            } catch (OutOfMemoryError t) {
                throw t;
            } catch (Throwable t) {
                return false;
            }
            return true;
        }

        public DataImpl getData() {
            return data;
        }
    }

    /**
    * Perform an operation on a remote file object
    * using the first valid Form.
    */
    abstract class URLFunction extends FormFunction {

        URL url;

        public URLFunction() {
            url = null;
        }

        boolean check(FormFileInformer node) {
            return (node.isThisType(url.getFile()) || node.isThisType(url.toString()));
        }

        InputStream getStream() throws IOException {
            return url.openStream();
        }
    }

    /**
    * Read a Data object from a remote file
    * using the first valid Form.
    */
    class OpenURLForm extends URLFunction {

        private DataImpl data;

        public OpenURLForm(URL url) {
            this.url = url;
            data = null;
        }

        boolean function(FormNode node) {
            try {
                data = node.open(url);
            } catch (Throwable t) {
                return false;
            }
            return true;
        }

        public DataImpl getData() {
            return data;
        }
    }

    /**
    * Save a Data object using the first appropriate Form.
    */
    public synchronized void save(String id, Data data, boolean replace) throws BadFormException, RemoteException, IOException, VisADException {
        SaveForm s = new SaveForm(id, data, replace);
        if (!s.run()) {
            throw new BadFormException("Data object not compatible with \"" + getName() + "\" data family");
        }
    }

    /**
    * Add data to an existing data object using the first appropriate Form.
    */
    public synchronized void add(String id, Data data, boolean replace) throws BadFormException {
        AddForm a = new AddForm(id, data, replace);
        try {
            if (a.run()) {
                return;
            }
        } catch (IOException e) {
        }
        throw new BadFormException("Data object not compatible with \"" + getName() + "\" data family");
    }

    /**
    * Open a local data object using the first appropriate Form.
    */
    public synchronized DataImpl open(String id) throws BadFormException, VisADException {
        if (id == null) {
            return null;
        }
        URL url;
        try {
            url = new URL(id);
        } catch (MalformedURLException mue) {
            url = null;
        }
        DataImpl data = null;
        if (url != null) {
            OpenURLForm u = new OpenURLForm(url);
            try {
                if (!u.run()) {
                    data = null;
                } else {
                    data = u.getData();
                }
            } catch (Exception e) {
                data = null;
            }
        }
        String file = null;
        if (data == null) {
            if (url == null) {
                file = id;
            } else if (url.getProtocol() == "file") {
                file = url.getFile();
                if (file.length() > 2 && file.charAt(2) == ':' && file.charAt(0) == '/') {
                    file = file.substring(1);
                }
            }
        }
        if (file != null) {
            OpenStringForm o = new OpenStringForm(file);
            try {
                if (!o.run()) {
                    data = null;
                } else {
                    data = o.getData();
                }
            } catch (IOException ioe) {
                data = null;
            }
        }
        if (data == null) {
            if (file != null && !new java.io.File(file).exists()) {
                throw new BadFormException("No such data object \"" + id + "\"");
            }
            throw new BadFormException("Data object \"" + id + "\" not compatible with \"" + getName() + "\" data family");
        }
        return data;
    }

    /**
    * Open a remote data object using the first appropriate Form.
    */
    public synchronized DataImpl open(URL url) throws BadFormException, IOException, VisADException {
        OpenURLForm o = new OpenURLForm(url);
        if (!o.run()) {
            throw new BadFormException("Data object \"" + url + "\" not compatible with \"" + getName() + "\" data family");
        }
        return o.getData();
    }
}
