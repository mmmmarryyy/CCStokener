package org.extwind.osgi.tomcat.catalina.naming.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidSearchControlsException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import org.apache.naming.NamingContextBindingsEnumeration;
import org.apache.naming.NamingContextEnumeration;
import org.apache.naming.NamingEntry;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.osgi.framework.Bundle;

/**
 * 
 * Temp naming context for osgi app, will be replace with URLContext soon.
 * 
 * @author donf.yang
 * 
 */
public class OSGiDirContext extends BaseDirContext {

    protected Bundle bundle;

    protected String basePath;

    private static org.apache.juli.logging.Log log = org.apache.juli.logging.LogFactory.getLog(OSGiDirContext.class);

    /**
	 * The descriptive information string for this implementation.
	 */
    protected static final int BUFFER_SIZE = 2048;

    public OSGiDirContext() {
        super();
    }

    /**
	 * Builds a osgi application directory context using the given environment.
	 */
    public OSGiDirContext(Hashtable env) {
        super(env);
    }

    public OSGiDirContext(Bundle bundle, String basePath, Hashtable env) {
        super(env);
        this.bundle = bundle;
        this.basePath = basePath;
    }

    /**
	 * Absolute normalized filename of the base.
	 */
    protected String absoluteBase = null;

    /**
	 * Case sensitivity.
	 */
    protected boolean caseSensitive = true;

    /**
	 * Allow linking.
	 */
    protected boolean allowLinking = false;

    /**
	 * Set case sensitivity.
	 */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
	 * Is case sensitive ?
	 */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
	 * Set allow linking.
	 */
    public void setAllowLinking(boolean allowLinking) {
        this.allowLinking = allowLinking;
    }

    /**
	 * Is linking allowed.
	 */
    public boolean getAllowLinking() {
        return allowLinking;
    }

    /**
	 * Release any resources allocated for this directory context.
	 */
    public void release() {
        super.release();
    }

    /**
	 * Retrieves the named object.
	 * 
	 * @param name
	 *            the name of the object to look up
	 * @return the object bound to name
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public Object lookup(String name) throws NamingException {
        URL url = lookupURL(name);
        if (url == null) {
            throw new NamingException(sm.getString("resources.notFound", name));
        }
        return new URLResource(url);
    }

    public URL lookupURL(String name) throws NamingException {
        if (this.bundle == null) {
            return null;
        }
        URL url = this.bundle.getResource(basePath == null ? name : basePath + name);
        if (url == null) {
            return null;
        }
        return url;
    }

    /**
	 * Enumerates the names bound in the named context, along with the class
	 * names of objects bound to them. The contents of any subcontexts are not
	 * included.
	 * <p>
	 * If a binding is added to or removed from this context, its effect on an
	 * enumeration previously returned is undefined.
	 * 
	 * @param name
	 *            the name of the context to list
	 * @return an enumeration of the names and class names of the bindings in
	 *         this context. Each element of the enumeration is of type
	 *         NameClassPair.
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public NamingEnumeration list(String name) throws NamingException {
        return new NamingContextEnumeration(internalList(name).iterator());
    }

    /**
	 * Enumerates the names bound in the named context, along with the objects
	 * bound to them. The contents of any subcontexts are not included.
	 * <p>
	 * If a binding is added to or removed from this context, its effect on an
	 * enumeration previously returned is undefined.
	 * 
	 * @param name
	 *            the name of the context to list
	 * @return an enumeration of the bindings in this context. Each element of
	 *         the enumeration is of type Binding.
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public NamingEnumeration listBindings(String name) throws NamingException {
        return new NamingContextBindingsEnumeration(internalList(name).iterator(), this);
    }

    protected Set internalList(String name) throws NamingException {
        Set entries = new HashSet();
        if (this.bundle != null) {
            String findPath = this.basePath == null ? name : this.basePath + name;
            Enumeration e = this.bundle.findEntries(findPath, null, false);
            if (e != null) {
                while (e.hasMoreElements()) {
                    URL entryURL = (URL) e.nextElement();
                    NamingEntry entry = new NamingEntry(entryURL.getFile().substring(basePath.length()), entryURL, NamingEntry.ENTRY);
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    /**
	 * Destroys the named context and removes it from the namespace. Any
	 * attributes associated with the name are also removed. Intermediate
	 * contexts are not destroyed.
	 * <p>
	 * This method is idempotent. It succeeds even if the terminal atomic name
	 * is not bound in the target context, but throws NameNotFoundException if
	 * any of the intermediate contexts do not exist.
	 * 
	 * In a federated naming system, a context from one naming system may be
	 * bound to a name in another. One can subsequently look up and perform
	 * operations on the foreign context using a composite name. However, an
	 * attempt destroy the context using this composite name will fail with
	 * NotContextException, because the foreign context is not a "subcontext" of
	 * the context in which it is bound. Instead, use unbind() to remove the
	 * binding of the foreign context. Destroying the foreign context requires
	 * that the destroySubcontext() be performed on a context from the foreign
	 * context's "native" naming system.
	 * 
	 * @param name
	 *            the name of the context to be destroyed; may not be empty
	 * @exception NameNotFoundException
	 *                if an intermediate context does not exist
	 * @exception NotContextException
	 *                if the name is bound but does not name a context, or does
	 *                not name a context of the appropriate type
	 */
    public void destroySubcontext(String name) throws NamingException {
        unbind(name);
    }

    /**
	 * Retrieves the named object, following links except for the terminal
	 * atomic component of the name. If the object bound to name is not a link,
	 * returns the object itself.
	 * 
	 * @param name
	 *            the name of the object to look up
	 * @return the object bound to name, not following the terminal link (if
	 *         any).
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public Object lookupLink(String name) throws NamingException {
        return lookup(name);
    }

    /**
	 * Retrieves the full name of this context within its own namespace.
	 * <p>
	 * Many naming services have a notion of a "full name" for objects in their
	 * respective namespaces. For example, an LDAP entry has a distinguished
	 * name, and a DNS record has a fully qualified name. This method allows the
	 * client application to retrieve this name. The string returned by this
	 * method is not a JNDI composite name and should not be passed directly to
	 * context methods. In naming systems for which the notion of full name does
	 * not make sense, OperationNotSupportedException is thrown.
	 * 
	 * @return this context's name in its own namespace; never null
	 * @exception OperationNotSupportedException
	 *                if the naming system does not have the notion of a full
	 *                name
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public String getNameInNamespace() throws NamingException {
        return docBase;
    }

    /**
	 * Retrieves selected attributes associated with a named object. See the
	 * class description regarding attribute models, attribute type names, and
	 * operational attributes.
	 * 
	 * @return the requested attributes; never null
	 * @param name
	 *            the name of the object from which to retrieve attributes
	 * @param attrIds
	 *            the identifiers of the attributes to retrieve. null indicates
	 *            that all attributes should be retrieved; an empty array
	 *            indicates that none should be retrieved
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        if (name == null) throw new NamingException(sm.getString("resources.notFound", name));
        try {
            return new URLResourceAttributes(name);
        } catch (Exception e) {
            throw new NamingException(sm.getString("resources.notFound", name));
        }
    }

    /**
	 * Retrieves the schema associated with the named object. The schema
	 * describes rules regarding the structure of the namespace and the
	 * attributes stored within it. The schema specifies what types of objects
	 * can be added to the directory and where they can be added; what mandatory
	 * and optional attributes an object can have. The range of support for
	 * schemas is directory-specific.
	 * 
	 * @param name
	 *            the name of the object whose schema is to be retrieved
	 * @return the schema associated with the context; never null
	 * @exception OperationNotSupportedException
	 *                if schema not supported
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public DirContext getSchema(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
	 * Retrieves a context containing the schema objects of the named object's
	 * class definitions.
	 * 
	 * @param name
	 *            the name of the object whose object class definition is to be
	 *            retrieved
	 * @return the DirContext containing the named object's class definitions;
	 *         never null
	 * @exception OperationNotSupportedException
	 *                if schema not supported
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
	 * Searches in a single context for objects that contain a specified set of
	 * attributes, and retrieves selected attributes. The search is performed
	 * using the default SearchControls settings.
	 * 
	 * @param name
	 *            the name of the context to search
	 * @param matchingAttributes
	 *            the attributes to search for. If empty or null, all objects in
	 *            the target context are returned.
	 * @param attributesToReturn
	 *            the attributes to return. null indicates that all attributes
	 *            are to be returned; an empty array indicates that none are to
	 *            be returned.
	 * @return a non-null enumeration of SearchResult objects. Each SearchResult
	 *         contains the attributes identified by attributesToReturn and the
	 *         name of the corresponding object, named relative to the context
	 *         named by name.
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return null;
    }

    /**
	 * Searches in a single context for objects that contain a specified set of
	 * attributes. This method returns all the attributes of such objects. It is
	 * equivalent to supplying null as the atributesToReturn parameter to the
	 * method search(Name, Attributes, String[]).
	 * 
	 * @param name
	 *            the name of the context to search
	 * @param matchingAttributes
	 *            the attributes to search for. If empty or null, all objects in
	 *            the target context are returned.
	 * @return a non-null enumeration of SearchResult objects. Each SearchResult
	 *         contains the attributes identified by attributesToReturn and the
	 *         name of the corresponding object, named relative to the context
	 *         named by name.
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException {
        return null;
    }

    /**
	 * Searches in the named context or object for entries that satisfy the
	 * given search filter. Performs the search as specified by the search
	 * controls.
	 * 
	 * @param name
	 *            the name of the context or object to search
	 * @param filter
	 *            the filter expression to use for the search; may not be null
	 * @param cons
	 *            the search controls that control the search. If null, the
	 *            default search controls are used (equivalent to (new
	 *            SearchControls())).
	 * @return an enumeration of SearchResults of the objects that satisfy the
	 *         filter; never null
	 * @exception InvalidSearchFilterException
	 *                if the search filter specified is not supported or
	 *                understood by the underlying directory
	 * @exception InvalidSearchControlsException
	 *                if the search controls contain invalid settings
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException {
        return null;
    }

    /**
	 * Searches in the named context or object for entries that satisfy the
	 * given search filter. Performs the search as specified by the search
	 * controls.
	 * 
	 * @param name
	 *            the name of the context or object to search
	 * @param filterExpr
	 *            the filter expression to use for the search. The expression
	 *            may contain variables of the form "{i}" where i is a
	 *            nonnegative integer. May not be null.
	 * @param filterArgs
	 *            the array of arguments to substitute for the variables in
	 *            filterExpr. The value of filterArgs[i] will replace each
	 *            occurrence of "{i}". If null, equivalent to an empty array.
	 * @param cons
	 *            the search controls that control the search. If null, the
	 *            default search controls are used (equivalent to (new
	 *            SearchControls())).
	 * @return an enumeration of SearchResults of the objects that satisy the
	 *         filter; never null
	 * @exception ArrayIndexOutOfBoundsException
	 *                if filterExpr contains {i} expressions where i is outside
	 *                the bounds of the array filterArgs
	 * @exception InvalidSearchControlsException
	 *                if cons contains invalid settings
	 * @exception InvalidSearchFilterException
	 *                if filterExpr with filterArgs represents an invalid search
	 *                filter
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return null;
    }

    /**
	 * Return a context-relative path, beginning with a "/", that represents the
	 * canonical version of the specified path after ".." and "." elements are
	 * resolved out. If the specified path attempts to go outside the boundaries
	 * of the current context (i.e. too many ".." path elements are present),
	 * return <code>null</code> instead.
	 * 
	 * @param path
	 *            Path to be normalized
	 */
    protected String normalize(String path) {
        String normalized = path;
        if (File.separatorChar == '\\' && normalized.indexOf('\\') >= 0) normalized = normalized.replace('\\', '/');
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        while (true) {
            int index = normalized.indexOf("//");
            if (index < 0) break;
            normalized = normalized.substring(0, index) + normalized.substring(index + 1);
        }
        while (true) {
            int index = normalized.indexOf("/./");
            if (index < 0) break;
            normalized = normalized.substring(0, index) + normalized.substring(index + 2);
        }
        while (true) {
            int index = normalized.indexOf("/../");
            if (index < 0) break;
            if (index == 0) return (null);
            int index2 = normalized.lastIndexOf('/', index - 1);
            normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
        }
        return (normalized);
    }

    protected class URLResource extends Resource {

        protected URL url;

        public URLResource(URL url) {
            this.url = url;
        }

        /**
		 * @return InputStream
		 */
        public InputStream streamContent() throws IOException {
            if (binaryContent == null) {
                inputStream = url.openStream();
            }
            return super.streamContent();
        }
    }

    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new NamingException("Unsupport method");
    }

    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        throw new NamingException("Unsupport method");
    }

    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        throw new NamingException("Unsupport method");
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        throw new NamingException("Unsupport method");
    }

    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new NamingException("Unsupport method");
    }

    public void rename(String oldName, String newName) throws NamingException {
        throw new NamingException("Unsupport method");
    }

    public void unbind(String name) throws NamingException {
        throw new NamingException("Unsupport method");
    }

    protected class URLResourceAttributes extends ResourceAttributes {

        protected URLConnection urlConnection = null;

        protected boolean accessed = false;

        public URLResourceAttributes(String name) throws Exception {
            urlConnection = lookupURL(name).openConnection();
            this.name = name;
        }

        /**
		 * Get content length.
		 * 
		 * @return content length value
		 */
        public long getContentLength() {
            if (contentLength != -1L) return contentLength;
            contentLength = urlConnection.getContentLength();
            return contentLength;
        }

        /**
		 * Get creation time.
		 * 
		 * @return creation time value
		 */
        public long getCreation() {
            if (creation != -1L) return creation;
            creation = getLastModified();
            return creation;
        }

        /**
		 * Get creation date.
		 * 
		 * @return Creation date value
		 */
        public Date getCreationDate() {
            if (creation == -1L) {
                creation = getCreation();
            }
            return super.getCreationDate();
        }

        /**
		 * Get last modified time.
		 * 
		 * @return lastModified time value
		 */
        public long getLastModified() {
            if (lastModified != -1L) return lastModified;
            lastModified = urlConnection.getLastModified();
            return lastModified;
        }

        /**
		 * Get lastModified date.
		 * 
		 * @return LastModified date value
		 */
        public Date getLastModifiedDate() {
            if (lastModified == -1L) {
                lastModified = getLastModified();
            }
            return super.getLastModifiedDate();
        }

        /**
		 * Get name.
		 * 
		 * @return Name value
		 */
        public String getName() {
            return name;
        }

        /**
		 * Get resource type.
		 * 
		 * @return String resource type
		 */
        public String getResourceType() {
            return urlConnection.getContentType();
        }

        /**
		 * Get canonical path.
		 * 
		 * @return String the file's canonical path
		 */
        public String getCanonicalPath() {
            return super.getCanonicalPath();
        }
    }
}
