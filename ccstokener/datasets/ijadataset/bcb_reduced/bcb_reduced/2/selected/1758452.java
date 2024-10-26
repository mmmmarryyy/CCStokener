package org.fao.waicent.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class FileResource implements XMLable {

    protected String name = null;

    protected String resource = null;

    protected transient String home = null;

    protected transient Boolean readonly = null;

    private String extent_code = null;

    private String extent_region = null;

    public String getFilename() throws IOException {
        String filename = "";
        String path_filename = getAbsoluteFilename();
        File file = new File(path_filename);
        if (!file.exists()) {
            throw new FileNotFoundException(path_filename);
        } else if (!file.isDirectory()) {
            int start = path_filename.lastIndexOf(System.getProperty("file.separator").charAt(0)) + 1;
            if (start == -1) {
                start = 0;
            }
            filename = path_filename.substring(start);
        }
        return filename;
    }

    public String getDirectory() throws IOException {
        String directory = "";
        String path_filename = getAbsoluteFilename();
        File file = new File(path_filename);
        if (!file.exists()) {
            throw new FileNotFoundException(path_filename);
        } else if (file.isFile()) {
            int end = path_filename.lastIndexOf(System.getProperty("file.separator").charAt(0));
            if (end == -1) {
                end = 0;
            }
            directory = path_filename.substring(0, end);
        } else {
            directory = path_filename;
        }
        return directory;
    }

    public boolean copyResourceLocal(FileResource dst_directory_resource) {
        boolean copied = false;
        String src_dir = null;
        String src_filename = null;
        try {
            src_dir = getDirectory();
            src_filename = getFilename();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        String dst_dir = null;
        try {
            dst_dir = dst_directory_resource.getDirectory();
        } catch (Exception e) {
            dst_dir = dst_directory_resource.getAbsoluteFilename();
        }
        if (dst_dir.equalsIgnoreCase(src_dir)) {
            copied = true;
        } else {
            if (copy(new FileResource(getName(), src_filename, dst_dir))) {
                home = dst_dir;
                resource = src_filename;
                copied = true;
            } else {
                System.out.println("Unable to copy " + getName() + " from " + src_filename + " to " + dst_dir);
                copied = false;
            }
        }
        return copied;
    }

    public boolean copy(FileResource dst_resource) {
        boolean flag = false;
        if (getAbsoluteFilename().equals(dst_resource.getAbsoluteFilename())) {
            return true;
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(openInputStream());
            out = dst_resource.openOutputStream();
            byte b[] = new byte[4096];
            int size = 0;
            while (true) {
                size = in.read(b, 0, 4096);
                if (size == -1) {
                    break;
                }
                out.write(b, 0, size);
            }
            out.flush();
            flag = true;
        } catch (Exception _ex) {
            flag = false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
        }
        return flag;
    }

    public boolean rename(FileResource dst_resource) {
        boolean flag;
        try {
            flag = new File(getAbsoluteFilename()).renameTo(new File(dst_resource.getAbsoluteFilename()));
            home = dst_resource.home;
            resource = dst_resource.resource;
            name = name;
        } catch (Exception _ex) {
            flag = false;
        }
        return flag;
    }

    public interface FileRecursiveApply {

        public boolean apply(String filename, File file);
    }

    public static int recursiveApply(String filename, FileRecursiveApply apply) {
        int count = 0;
        File file = new File(filename);
        if (file.exists()) {
            if (file.isDirectory()) {
                String[] list = file.list();
                for (int i = 0; i < list.length; i++) {
                    if (list[i] != null) {
                        count += recursiveApply(filename + File.separatorChar + list[i], apply);
                    }
                }
            } else {
                if (apply.apply(filename, file)) {
                    count = 1;
                }
            }
        }
        return count;
    }

    public static boolean recursiveDelete(String filename) {
        boolean result = false;
        File file = new File(filename);
        if (file.exists()) {
            if (file.isDirectory()) {
                result = true;
                String[] list = file.list();
                for (int i = 0; i < list.length; i++) {
                    if (list[i] != null) {
                        if (!recursiveDelete(filename + File.separatorChar + list[i])) {
                            result = false;
                        }
                    }
                }
                if (!file.delete()) {
                    result = false;
                }
            } else {
                result = file.delete();
            }
        }
        return result;
    }

    public boolean delete() {
        boolean flag;
        try {
            flag = new File(getAbsoluteFilename()).delete();
        } catch (Exception _ex) {
            flag = false;
            System.out.println("FileResource.delete() ERROR in deleting " + getAbsoluteFilename());
            _ex.printStackTrace();
        }
        return flag;
    }

    public boolean create(boolean overwrite) {
        boolean flag = false;
        try {
            if (overwrite) {
                if (exists()) {
                    delete();
                }
            }
            flag = new File(getAbsoluteFilename()).createNewFile();
        } catch (Exception _ex) {
            flag = false;
        }
        return flag;
    }

    public boolean isReadOnly() {
        if (readonly != null) {
            return readonly.booleanValue();
        }
        return determineIfReadOnly();
    }

    public boolean determineIfReadOnly() {
        readonly = new Boolean(true);
        try {
            readonly = new Boolean(!(new File(getAbsoluteFilename()).canWrite()));
        } catch (Exception _ex) {
        }
        return readonly.booleanValue();
    }

    /**
    *  alisaf: similar to exists(), but this makes sure the existing resource is
    *  a file and not a directory.
    */
    public boolean fileExists() {
        boolean exists = false;
        try {
            File file = new File(getAbsoluteFilename());
            exists = file.exists();
            exists &= file.isFile();
        } catch (Exception e) {
            exists = false;
        }
        return exists;
    }

    /**
    *  alisaf: verifies that this resource is an existing file OR directory.
    */
    public boolean exists() {
        boolean exists = false;
        try {
            File file = new File(getAbsoluteFilename());
            exists = file.exists();
        } catch (Exception e) {
            exists = false;
        }
        return exists;
    }

    public void ensureUnique() {
    }

    public static String constructFilenameFromName(String name) {
        if (name == null) {
            return "null";
        }
        String FilenameFromName = name.replace('~', '_').replace('!', '_').replace('@', '_').replace('#', '_').replace('$', '_').replace('%', '_').replace('^', '_').replace('&', '_').replace('*', '_').replace('(', '_').replace(')', '_').replace('+', '_').replace('`', '_').replace('-', '_').replace('=', '_').replace('<', '_').replace('>', '_').replace('?', '_').replace(',', '_').replace(':', '_').replace(';', '_').replace('"', '_').replace('\'', '_').replace('\\', '_').replace('/', '_').replace('[', '_').replace(']', '_').replace('{', '_').replace('}', '_').replace('|', '_').replace('\n', '_');
        FilenameFromName.trim();
        return FilenameFromName;
    }

    public FileResource() {
        name = "";
        resource = "";
        home = null;
    }

    public FileResource(String f) {
        name = f;
        resource = f;
        home = null;
        if (resource == null) {
            resource = "";
        }
    }

    /**
     *  alisaf: constructor:  Build object from project name and absolute
     *  path to the home directory of the project.
     *
     *  @param n  The name of the project.
     *  @param f  The absolute path to the home directory of the project.
     */
    public FileResource(String n, String f) {
        name = n;
        resource = f;
        home = null;
        if (resource == null) {
            resource = "";
        }
    }

    /**
   * @deprecated
   * Translate moved to appropriate class, like MapContext or BaseLayer.
   */
    public FileResource(String n, String f, Translate t) {
        this(n, f);
    }

    public FileResource(String n, String f, String h) {
        name = n;
        resource = f;
        home = h;
        if (resource == null) {
            resource = "";
        }
    }

    public Object clone() {
        String tmp_home = null;
        if (home != null) {
            tmp_home = new String(home);
        }
        return new FileResource(new String(name), new String(resource), tmp_home);
    }

    public String getResource() {
        return resource;
    }

    public String getName() {
        return name;
    }

    public String getExtentCode() {
        return extent_code;
    }

    public void setExtentCode(String extent_code) {
        this.extent_code = extent_code;
    }

    /**
     * This method returns the region which the selected country belongs.
     *
     * @return     return the region of type String.
     *
     * @author     macdc
     * @email      MeannCabel.delaCruz@fao.org
     * @date       February 18, 2003
     */
    public String getExtentRegion() {
        return extent_region;
    }

    public String getHome() {
        return home;
    }

    public void setName(String n) {
        name = n;
    }

    public void setResource(String r) {
        resource = r;
        if (resource == null) {
            resource = "";
        }
    }

    public FileResource setHome(String h) {
        home = h;
        return this;
    }

    public boolean hasFilename() {
        return resource.length() > 0;
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(resource);
    }

    public FileResource(DataInputStream in) throws IOException {
        name = in.readUTF();
        resource = in.readUTF();
        home = null;
    }

    public String getAbsoluteFilename() {
        return getAbsoluteFilename(home);
    }

    public String getAbsoluteFilename(String path) {
        if (resource == null) {
            resource = "";
        }
        String tmp_resource = "";
        if (path != null && path.length() > 0) {
            if (resource.indexOf(":") == -1 && !resource.startsWith("/") && !resource.startsWith("\\")) {
                tmp_resource += path;
                if (!tmp_resource.endsWith("/") && !tmp_resource.endsWith("\\")) {
                    tmp_resource += System.getProperty("file.separator");
                }
            }
        }
        tmp_resource += resource;
        tmp_resource = tmp_resource.replace('\\', System.getProperty("file.separator").charAt(0)).replace('/', System.getProperty("file.separator").charAt(0));
        String slash_slash = System.getProperty("file.separator") + System.getProperty("file.separator");
        while (tmp_resource.indexOf(slash_slash, 1) != -1) {
            int index = tmp_resource.indexOf(slash_slash);
            tmp_resource = tmp_resource.substring(0, index) + tmp_resource.substring(index + 1);
        }
        return tmp_resource;
    }

    /**
     *  utility method to retrieve the absolute path of the file represented by
     *  this FileResource object.
     *
     */
    public String getAbsoluteFilepath() {
        String path = home;
        if (resource == null) {
            resource = "";
        }
        String tmp_resource = "";
        if (path != null && path.length() > 0) {
            if (resource.indexOf(":") == -1 && !resource.startsWith("/") && !resource.startsWith("\\")) {
                tmp_resource += path;
                if (!tmp_resource.endsWith("/") && !tmp_resource.endsWith("\\")) {
                    tmp_resource += System.getProperty("file.separator");
                }
            }
        }
        tmp_resource = tmp_resource.replace('\\', System.getProperty("file.separator").charAt(0)).replace('/', System.getProperty("file.separator").charAt(0));
        String slash_slash = System.getProperty("file.separator") + System.getProperty("file.separator");
        while (tmp_resource.indexOf(slash_slash) != -1) {
            int index = tmp_resource.indexOf(slash_slash);
            tmp_resource = tmp_resource.substring(0, index) + tmp_resource.substring(index + 1);
        }
        return tmp_resource;
    }

    public InputStream openInputStream() throws IOException {
        return openInputStream(home);
    }

    public InputStream openInputStream(String path) throws IOException {
        InputStream in = null;
        if (resource == null || resource.length() == 0) {
            throw new FileNotFoundException("path=" + path + "resource=" + resource);
        } else if (resource.indexOf("://") != -1) {
            URL temp_url = new URL(resource);
            {
                in = new org.fao.waicent.util.InputStreamWithLength(temp_url.openStream(), temp_url.openConnection().getContentLength());
            }
        } else if (path != null && path.indexOf("://") != -1 || (path != null && path.startsWith("file:"))) {
            URL temp_url = new URL(new URL(path), resource);
            {
                in = new org.fao.waicent.util.InputStreamWithLength(temp_url.openStream(), temp_url.openConnection().getContentLength());
            }
        } else {
            String ff = getAbsoluteFilename(path);
            {
                in = new org.fao.waicent.util.InputStreamWithLength(new FileInputStream(ff), new File(ff).length());
            }
        }
        if (resource.endsWith(".zip") || resource.endsWith(".ZIP")) {
            ZipInputStream zin = new ZipInputStream(in);
            zin.getNextEntry();
            in = zin;
        }
        return in;
    }

    public OutputStream openOutputStream() throws IOException {
        return openOutputStream(home);
    }

    public OutputStream openOutputStream(String path) throws IOException {
        OutputStream out = null;
        if (resource == null || resource.length() == 0) {
            throw new FileNotFoundException("path=" + path + "resource=" + resource);
        } else if (resource.indexOf("://") != -1 || (path != null && path.indexOf("://") != -1)) {
            throw new IOException("Can't write to a URL: path=" + path + "resource=" + resource);
        } else {
            String abs = getAbsoluteFilename(path);
            File file = new File(abs);
            if (file.getParent() != null) {
                File parent = new File(file.getParent());
                parent.mkdirs();
            }
            out = new FileOutputStream(file);
        }
        if (resource.endsWith(".zip") || resource.endsWith(".ZIP")) {
            ZipOutputStream zout = new ZipOutputStream(out);
            zout.putNextEntry(new ZipEntry(name));
            out = zout;
        }
        return out;
    }

    public boolean setExtension(String ext) {
        int start = resource.lastIndexOf(".");
        int end = resource.length();
        if (start != -1) {
            if (resource.endsWith(".zip") || resource.endsWith(".ZIP")) {
                end = start;
                start = resource.substring(0, start).lastIndexOf(".");
            }
            if (start != -1) {
                end++;
                if (end != -1 && end < resource.length()) {
                    resource = resource.substring(0, start) + ext + resource.substring(end);
                } else {
                    resource = resource.substring(0, start) + ext;
                }
                return true;
            }
        }
        resource += ext;
        return false;
    }

    public String getUpperCaseExtension() {
        String ext = "";
        int start = resource.lastIndexOf(".");
        int end = resource.length();
        if (start != -1) {
            if (resource.endsWith(".zip") || resource.endsWith(".ZIP")) {
                end = start;
                start = resource.substring(0, start).lastIndexOf(".");
            }
            if (start != -1) {
                ext = resource.substring(start + 1, end).toUpperCase();
            }
        }
        return ext;
    }

    public String getExtension() {
        String ext = "";
        int start = resource.lastIndexOf(".");
        int end = resource.length();
        if (start != -1) {
            if (resource.endsWith(".zip") || resource.endsWith(".ZIP")) {
                end = start;
                start = resource.substring(0, start).lastIndexOf(".");
            }
            if (start != -1) {
                ext = resource.substring(start + 1, end);
            }
        }
        return ext;
    }

    /**
     *  alisaf: method to get the extension of the file represented by
     *  this fileresource object.
     */
    public String getFileType() {
        String ext = "";
        int start = resource.lastIndexOf('.');
        int end = resource.length();
        if (start != -1) {
            ext = resource.substring(start + 1, end);
        }
        return ext;
    }

    /**
     *  alisaf: method to get the name of the file (**without the file extension**)
     *  represented by this fileresource object. This method only gets the name and
     *  does not check if the file actually exists.
     */
    public String getFileName() {
        String filename = "";
        String path_filename = getAbsoluteFilename();
        int start = path_filename.lastIndexOf(System.getProperty("file.separator").charAt(0)) + 1;
        if (start == -1) {
            start = 0;
        }
        int end = path_filename.lastIndexOf('.');
        if (end == -1) {
            end = 0;
        }
        filename = path_filename.substring(start, end);
        return filename;
    }

    /**
     *  alisaf: method to get the name of the directory for the file represented by
     *  this fileresource object. This method only gets the directory name and
     *  does not check if the file actually exists.
     */
    public String getFileDirectory() {
        String directoryname = "";
        String path_filename = getAbsoluteFilename();
        int start = path_filename.lastIndexOf(System.getProperty("file.separator").charAt(0)) + 1;
        int end = path_filename.lastIndexOf(System.getProperty("file.separator").charAt(0));
        if (end == -1) {
            end = 0;
        }
        directoryname = path_filename.substring(0, end);
        return directoryname;
    }

    public void save(Document doc, Element ele) throws IOException {
        save(doc, ele, "en");
    }

    public void save(Document doc, Element ele, String lang) throws IOException {
        XMLUtil.setType(doc, ele, this);
        ele.setAttribute("resource", resource);
        if (name != null) {
            ele.setAttribute("name", name);
        }
        Element label_element = doc.createElement("Label");
        if (extent_code != null && extent_code.length() > 0) {
            ele.setAttribute("extentcode", this.extent_code);
            labels = new Translate(name, label_element);
            labels.addLabel(lang, name);
            label_element.setAttribute(lang, name);
        }
        if (extent_region != null && extent_region.length() > 0) {
            ele.setAttribute("extentregion", this.extent_region);
        }
    }

    public void load(Document doc, Element ele, String lang) throws IOException {
        XMLUtil.checkType(doc, ele, this);
        resource = ele.getAttribute("resource");
        name = ele.getAttribute("name");
        if (name == null || name.trim().equals("")) {
            name = resource;
        }
        defaultName = name;
        this.extent_code = ele.getAttribute("extentcode");
        this.extent_region = ele.getAttribute("extentregion");
    }

    public void load(Document doc, Element ele) throws IOException {
        load(doc, ele, "en");
        XMLUtil.checkType(doc, ele, this);
        NamedNodeMap nnm = ele.getAttributes();
        resource = ele.getAttribute("resource");
        name = ele.getAttribute("name");
        if (name == null) {
            name = resource;
        }
        this.extent_code = ele.getAttribute("extentcode");
        this.extent_region = ele.getAttribute("extentregion");
    }

    public FileResource(Document doc, Element ele, String lang) throws IOException {
        load(doc, ele, lang);
    }

    public FileResource(Document doc, Element ele) throws IOException {
        load(doc, ele);
    }

    public static boolean isAbsolutePath(String path) {
        if (path != null && path.length() > 0) {
            if (System.getProperties().get("os.name").toString().startsWith("Windows") && path.indexOf(":") == -1 && !path.startsWith("\\\\")) {
                return false;
            }
        }
        return true;
    }

    public static void main(String a[]) {
        FileResource fr = new FileResource("nessuno", "d:/class/maps/Asia.txt", "d:/class/maps/");
        System.out.println("Risorsa prima " + fr.getResource());
        fr.setExtension("p2a");
        System.out.println("Risorsa dopo " + fr.getResource());
    }

    String defaultName = null;

    private Translate labels = null;

    String label = null;

    /**
   *  @deprecated 
   *  ali.safarnejad@fao.org:20060826. Translate moved to MapContext,BaseLayer,etc,
   */
    public void changeLanguage(String language) {
    }

    /**
   *  @deprecated 
   *  ali.safarnejad@fao.org:20060826. use the appropriate class, MapContext,BaseLayer,etc, to set the label
   */
    public void setLabel(String lang, String nlabel) {
    }

    public String getLabel() {
        return label;
    }

    /**
   * @deprecated 
   * ali.safarnejad@fao.org: 20060826
   * use the appropriate class, MapContext,BaseLayer,etc, to get the label
   */
    public String getLabel(String lang) {
        if (labels != null) {
            return labels.getLabel(lang);
        } else {
            return name;
        }
    }
}
