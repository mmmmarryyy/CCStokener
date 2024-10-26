package org.apache.xmlbeans.impl.schema;

import org.apache.xmlbeans.impl.xb.xsdschema.RedefineDocument.Redefine;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument.Schema;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.apache.xmlbeans.impl.xb.xsdschema.ImportDocument.Import;
import org.apache.xmlbeans.impl.xb.xsdschema.IncludeDocument.Include;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.Writer;
import java.io.CharArrayWriter;
import java.io.OutputStreamWriter;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.SchemaTypeLoader;
import org.apache.xmlbeans.XmlErrorCodes;
import org.apache.xmlbeans.impl.common.IOUtil;
import org.apache.xmlbeans.impl.common.XmlEncodingSniffer;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class StscImporter {

    public static SchemaToProcess[] resolveImportsAndIncludes(Schema[] startWith, boolean forceSrcSave) {
        DownloadTable engine = new DownloadTable(startWith);
        return engine.resolveImportsAndIncludes(forceSrcSave);
    }

    public static class SchemaToProcess {

        private Schema schema;

        private String chameleonNamespace;

        private List includes;

        private List redefines;

        private List redefineObjects;

        private Set indirectIncludes;

        private Set indirectIncludedBy;

        public SchemaToProcess(Schema schema, String chameleonNamespace) {
            this.schema = schema;
            this.chameleonNamespace = chameleonNamespace;
        }

        /**
         * The schema to parse.
         */
        public Schema getSchema() {
            return schema;
        }

        /**
         * The base URI for this stp
         */
        public String getSourceName() {
            return schema.documentProperties().getSourceName();
        }

        /**
         * The chameleon namespace. Null if this schema is not being treated
         * as a chameleon. (The ordinary targetNamespace will just be extracted
         * from the syntax of the schema.)
         */
        public String getChameleonNamespace() {
            return chameleonNamespace;
        }

        /**
         * This method and the remaining methods are used to represent a
         * directed graph of includes/redefines. This is required in order
         * to establish identity component by component, as required in
         * xmlschema-1, chapter 4.2.2
         * @return
         */
        public List getRedefines() {
            return redefines;
        }

        public List getRedefineObjects() {
            return redefineObjects;
        }

        private void addInclude(SchemaToProcess include) {
            if (includes == null) includes = new ArrayList();
            includes.add(include);
        }

        private void addRedefine(SchemaToProcess redefine, Redefine object) {
            if (redefines == null || redefineObjects == null) {
                redefines = new ArrayList();
                redefineObjects = new ArrayList();
            }
            redefines.add(redefine);
            redefineObjects.add(object);
        }

        private void buildIndirectReferences() {
            if (includes != null) for (int i = 0; i < includes.size(); i++) {
                SchemaToProcess schemaToProcess = (SchemaToProcess) includes.get(i);
                this.addIndirectIncludes(schemaToProcess);
            }
            if (redefines != null) for (int i = 0; i < redefines.size(); i++) {
                SchemaToProcess schemaToProcess = (SchemaToProcess) redefines.get(i);
                this.addIndirectIncludes(schemaToProcess);
            }
        }

        private void addIndirectIncludes(SchemaToProcess schemaToProcess) {
            if (indirectIncludes == null) indirectIncludes = new HashSet();
            indirectIncludes.add(schemaToProcess);
            if (schemaToProcess.indirectIncludedBy == null) schemaToProcess.indirectIncludedBy = new HashSet();
            schemaToProcess.indirectIncludedBy.add(this);
            addIndirectIncludesHelper(this, schemaToProcess);
            if (indirectIncludedBy != null) for (Iterator it = indirectIncludedBy.iterator(); it.hasNext(); ) {
                SchemaToProcess stp = (SchemaToProcess) it.next();
                stp.indirectIncludes.add(schemaToProcess);
                schemaToProcess.indirectIncludedBy.add(stp);
                addIndirectIncludesHelper(stp, schemaToProcess);
            }
        }

        private static void addIndirectIncludesHelper(SchemaToProcess including, SchemaToProcess schemaToProcess) {
            if (schemaToProcess.indirectIncludes != null) for (Iterator it = schemaToProcess.indirectIncludes.iterator(); it.hasNext(); ) {
                SchemaToProcess stp = (SchemaToProcess) it.next();
                including.indirectIncludes.add(stp);
                stp.indirectIncludedBy.add(including);
            }
        }

        public boolean indirectIncludes(SchemaToProcess schemaToProcess) {
            return indirectIncludes != null && indirectIncludes.contains(schemaToProcess);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SchemaToProcess)) return false;
            final SchemaToProcess schemaToProcess = (SchemaToProcess) o;
            if (chameleonNamespace != null ? !chameleonNamespace.equals(schemaToProcess.chameleonNamespace) : schemaToProcess.chameleonNamespace != null) return false;
            if (!(schema == schemaToProcess.schema)) return false;
            return true;
        }

        public int hashCode() {
            int result;
            result = schema.hashCode();
            result = 29 * result + (chameleonNamespace != null ? chameleonNamespace.hashCode() : 0);
            return result;
        }
    }

    private static final String PROJECT_URL_PREFIX = "project://local";

    private static String baseURLForDoc(XmlObject obj) {
        String path = obj.documentProperties().getSourceName();
        if (path == null) return null;
        if (path.startsWith("/")) return PROJECT_URL_PREFIX + path.replace('\\', '/');
        int colon = path.indexOf(':');
        if (colon > 1 && path.substring(0, colon).matches("^\\w+$")) return path;
        return PROJECT_URL_PREFIX + "/" + path.replace('\\', '/');
    }

    private static URI parseURI(String s) {
        if (s == null) return null;
        try {
            return new URI(s);
        } catch (URISyntaxException syntax) {
            return null;
        }
    }

    public static URI resolve(URI base, String child) throws URISyntaxException {
        URI childUri = new URI(child);
        URI ruri = base.resolve(childUri);
        if (childUri.equals(ruri) && !childUri.isAbsolute() && (base.getScheme().equals("jar") || base.getScheme().equals("zip"))) {
            String r = base.toString();
            int lastslash = r.lastIndexOf('/');
            r = r.substring(0, lastslash) + "/" + childUri;
            int exclPointSlashIndex = r.lastIndexOf("!/");
            if (exclPointSlashIndex > 0) {
                int slashDotDotIndex = r.indexOf("/..", exclPointSlashIndex);
                while (slashDotDotIndex > 0) {
                    int prevSlashIndex = r.lastIndexOf("/", slashDotDotIndex - 1);
                    if (prevSlashIndex >= exclPointSlashIndex) {
                        String temp = r.substring(slashDotDotIndex + 3);
                        r = r.substring(0, prevSlashIndex).concat(temp);
                    }
                    slashDotDotIndex = r.indexOf("/..", exclPointSlashIndex);
                }
            }
            return URI.create(r);
        }
        if ("file".equals(ruri.getScheme()) && !child.equals(ruri)) {
            if (base.getPath().startsWith("//") && !ruri.getPath().startsWith("//")) {
                String path = "///".concat(ruri.getPath());
                try {
                    ruri = new URI("file", null, path, ruri.getQuery(), ruri.getFragment());
                } catch (URISyntaxException uris) {
                }
            }
        }
        return ruri;
    }

    public static class DownloadTable {

        /**
         * Namespace/schemaLocation pair.
         *
         * Downloaded schemas are indexed by namespace, schemaLocation, and both.
         *
         * A perfect match is preferred, but a match-by-namespace is accepted.
         * A match-by-schemaLocation is only accepted for includes (not imports).
         */
        private static class NsLocPair {

            private String namespaceURI;

            private String locationURL;

            public NsLocPair(String namespaceURI, String locationURL) {
                this.namespaceURI = namespaceURI;
                this.locationURL = locationURL;
            }

            /**
             * Empty string for no-namespace, null for namespace-not-part-of-key
             */
            public String getNamespaceURI() {
                return namespaceURI;
            }

            public String getLocationURL() {
                return locationURL;
            }

            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof NsLocPair)) return false;
                final NsLocPair nsLocPair = (NsLocPair) o;
                if (locationURL != null ? !locationURL.equals(nsLocPair.locationURL) : nsLocPair.locationURL != null) return false;
                if (namespaceURI != null ? !namespaceURI.equals(nsLocPair.namespaceURI) : nsLocPair.namespaceURI != null) return false;
                return true;
            }

            public int hashCode() {
                int result;
                result = (namespaceURI != null ? namespaceURI.hashCode() : 0);
                result = 29 * result + (locationURL != null ? locationURL.hashCode() : 0);
                return result;
            }
        }

        private static class DigestKey {

            byte[] _digest;

            int _hashCode;

            DigestKey(byte[] digest) {
                _digest = digest;
                for (int i = 0; i < 4 && i < digest.length; i++) {
                    _hashCode = _hashCode << 8;
                    _hashCode = _hashCode + digest[i];
                }
            }

            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof DigestKey)) return false;
                return Arrays.equals(_digest, ((DigestKey) o)._digest);
            }

            public int hashCode() {
                return _hashCode;
            }
        }

        private Map schemaByNsLocPair = new HashMap();

        private Map schemaByDigestKey = new HashMap();

        private LinkedList scanNeeded = new LinkedList();

        private Set emptyNamespaceSchemas = new HashSet();

        private Map scannedAlready = new HashMap();

        private Set failedDownloads = new HashSet();

        private Schema downloadSchema(XmlObject referencedBy, String targetNamespace, String locationURL) {
            if (locationURL == null) return null;
            StscState state = StscState.get();
            URI baseURI = parseURI(baseURLForDoc(referencedBy));
            String absoluteURL = null;
            try {
                absoluteURL = baseURI == null ? locationURL : resolve(baseURI, locationURL).toString();
            } catch (URISyntaxException e) {
                state.error("Could not find resource - invalid location URL: " + e.getMessage(), XmlErrorCodes.CANNOT_FIND_RESOURCE, referencedBy);
                return null;
            }
            if (state.isFileProcessed(absoluteURL)) return null;
            if (absoluteURL != null && targetNamespace != null) {
                Schema result = (Schema) schemaByNsLocPair.get(new NsLocPair(targetNamespace, absoluteURL));
                if (result != null) return result;
            }
            if (targetNamespace != null && !targetNamespace.equals("")) {
                if (!state.shouldDownloadURI(absoluteURL)) {
                    Schema result = (Schema) schemaByNsLocPair.get(new NsLocPair(targetNamespace, null));
                    if (result != null) return result;
                }
                if (state.linkerDefinesNamespace(targetNamespace)) return null;
            }
            if (absoluteURL != null) {
                Schema result = (Schema) schemaByNsLocPair.get(new NsLocPair(null, absoluteURL));
                if (result != null) return result;
            }
            if (absoluteURL == null) {
                state.error("Could not find resource - no valid location URL.", XmlErrorCodes.CANNOT_FIND_RESOURCE, referencedBy);
                return null;
            }
            if (previouslyFailedToDownload(absoluteURL)) {
                return null;
            }
            if (!state.shouldDownloadURI(absoluteURL)) {
                state.error("Could not load resource \"" + absoluteURL + "\" (network downloads disabled).", XmlErrorCodes.CANNOT_FIND_RESOURCE, referencedBy);
                addFailedDownload(absoluteURL);
                return null;
            }
            download: try {
                XmlObject xdoc = downloadDocument(state.getS4SLoader(), targetNamespace, absoluteURL);
                Schema result = findMatchByDigest(xdoc);
                String shortname = state.relativize(absoluteURL);
                if (result != null) {
                    String dupname = state.relativize(result.documentProperties().getSourceName());
                    if (dupname != null) state.info(shortname + " is the same as " + dupname + " (ignoring the duplicate file)"); else state.info(shortname + " is the same as another schema");
                } else {
                    XmlOptions voptions = new XmlOptions();
                    voptions.setErrorListener(state.getErrorListener());
                    if (!(xdoc instanceof SchemaDocument) || !xdoc.validate(voptions)) {
                        state.error("Referenced document is not a valid schema", XmlErrorCodes.CANNOT_FIND_RESOURCE, referencedBy);
                        break download;
                    }
                    SchemaDocument sDoc = (SchemaDocument) xdoc;
                    result = sDoc.getSchema();
                    state.info("Loading referenced file " + shortname);
                }
                NsLocPair key = new NsLocPair(emptyStringIfNull(result.getTargetNamespace()), absoluteURL);
                addSuccessfulDownload(key, result);
                return result;
            } catch (MalformedURLException malformed) {
                state.error("URL \"" + absoluteURL + "\" is not well-formed", XmlErrorCodes.CANNOT_FIND_RESOURCE, referencedBy);
            } catch (IOException connectionProblem) {
                state.error(connectionProblem.toString(), XmlErrorCodes.CANNOT_FIND_RESOURCE, referencedBy);
            } catch (XmlException e) {
                state.error("Problem parsing referenced XML resource - " + e.getMessage(), XmlErrorCodes.CANNOT_FIND_RESOURCE, referencedBy);
            }
            addFailedDownload(absoluteURL);
            return null;
        }

        static XmlObject downloadDocument(SchemaTypeLoader loader, String namespace, String absoluteURL) throws MalformedURLException, IOException, XmlException {
            StscState state = StscState.get();
            EntityResolver resolver = state.getEntityResolver();
            if (resolver != null) {
                InputSource source = null;
                try {
                    source = resolver.resolveEntity(namespace, absoluteURL);
                } catch (SAXException e) {
                    throw new XmlException(e);
                }
                if (source != null) {
                    state.addSourceUri(absoluteURL, null);
                    Reader reader = source.getCharacterStream();
                    if (reader != null) {
                        reader = copySchemaSource(absoluteURL, reader, state);
                        XmlOptions options = new XmlOptions();
                        options.setLoadLineNumbers();
                        options.setDocumentSourceName(absoluteURL);
                        return loader.parse(reader, null, options);
                    }
                    InputStream bytes = source.getByteStream();
                    if (bytes != null) {
                        bytes = copySchemaSource(absoluteURL, bytes, state);
                        String encoding = source.getEncoding();
                        XmlOptions options = new XmlOptions();
                        options.setLoadLineNumbers();
                        options.setLoadMessageDigest();
                        options.setDocumentSourceName(absoluteURL);
                        if (encoding != null) options.setCharacterEncoding(encoding);
                        return loader.parse(bytes, null, options);
                    }
                    String urlToLoad = source.getSystemId();
                    if (urlToLoad == null) throw new IOException("EntityResolver unable to resolve " + absoluteURL + " (for namespace " + namespace + ")");
                    copySchemaSource(absoluteURL, state, false);
                    XmlOptions options = new XmlOptions();
                    options.setLoadLineNumbers();
                    options.setLoadMessageDigest();
                    options.setDocumentSourceName(absoluteURL);
                    URL urlDownload = new URL(urlToLoad);
                    return loader.parse(urlDownload, null, options);
                }
            }
            state.addSourceUri(absoluteURL, null);
            copySchemaSource(absoluteURL, state, false);
            XmlOptions options = new XmlOptions();
            options.setLoadLineNumbers();
            options.setLoadMessageDigest();
            URL urlDownload = new URL(absoluteURL);
            return loader.parse(urlDownload, null, options);
        }

        private void addSuccessfulDownload(NsLocPair key, Schema schema) {
            byte[] digest = schema.documentProperties().getMessageDigest();
            if (digest == null) {
                StscState.get().addSchemaDigest(null);
            } else {
                DigestKey dk = new DigestKey(digest);
                if (!schemaByDigestKey.containsKey(dk)) {
                    schemaByDigestKey.put(new DigestKey(digest), schema);
                    StscState.get().addSchemaDigest(digest);
                }
            }
            schemaByNsLocPair.put(key, schema);
            NsLocPair key1 = new NsLocPair(key.getNamespaceURI(), null);
            if (!schemaByNsLocPair.containsKey(key1)) schemaByNsLocPair.put(key1, schema);
            NsLocPair key2 = new NsLocPair(null, key.getLocationURL());
            if (!schemaByNsLocPair.containsKey(key2)) schemaByNsLocPair.put(key2, schema);
        }

        private Schema findMatchByDigest(XmlObject original) {
            byte[] digest = original.documentProperties().getMessageDigest();
            if (digest == null) return null;
            return (Schema) schemaByDigestKey.get(new DigestKey(digest));
        }

        private void addFailedDownload(String locationURL) {
            failedDownloads.add(locationURL);
        }

        private boolean previouslyFailedToDownload(String locationURL) {
            return failedDownloads.contains(locationURL);
        }

        private static boolean nullableStringsMatch(String s1, String s2) {
            if (s1 == null && s2 == null) return true;
            if (s1 == null || s2 == null) return false;
            return (s1.equals(s2));
        }

        private static String emptyStringIfNull(String s) {
            if (s == null) return "";
            return s;
        }

        private SchemaToProcess addScanNeeded(SchemaToProcess stp) {
            if (!scannedAlready.containsKey(stp)) {
                scannedAlready.put(stp, stp);
                scanNeeded.add(stp);
                return stp;
            } else return (SchemaToProcess) scannedAlready.get(stp);
        }

        private void addEmptyNamespaceSchema(Schema s) {
            emptyNamespaceSchemas.add(s);
        }

        private void usedEmptyNamespaceSchema(Schema s) {
            emptyNamespaceSchemas.remove(s);
        }

        private boolean fetchRemainingEmptyNamespaceSchemas() {
            if (emptyNamespaceSchemas.isEmpty()) return false;
            for (Iterator i = emptyNamespaceSchemas.iterator(); i.hasNext(); ) {
                Schema schema = (Schema) i.next();
                addScanNeeded(new SchemaToProcess(schema, null));
            }
            emptyNamespaceSchemas.clear();
            return true;
        }

        private boolean hasNextToScan() {
            return !scanNeeded.isEmpty();
        }

        private SchemaToProcess nextToScan() {
            SchemaToProcess next = (SchemaToProcess) scanNeeded.removeFirst();
            return next;
        }

        public DownloadTable(Schema[] startWith) {
            for (int i = 0; i < startWith.length; i++) {
                String targetNamespace = startWith[i].getTargetNamespace();
                NsLocPair key = new NsLocPair(targetNamespace, baseURLForDoc(startWith[i]));
                addSuccessfulDownload(key, startWith[i]);
                if (targetNamespace != null) addScanNeeded(new SchemaToProcess(startWith[i], null)); else addEmptyNamespaceSchema(startWith[i]);
            }
        }

        public SchemaToProcess[] resolveImportsAndIncludes(boolean forceSave) {
            StscState state = StscState.get();
            List result = new ArrayList();
            boolean hasRedefinitions = false;
            for (; ; ) {
                while (hasNextToScan()) {
                    SchemaToProcess stp = nextToScan();
                    String uri = stp.getSourceName();
                    state.addSourceUri(uri, null);
                    result.add(stp);
                    copySchemaSource(uri, state, forceSave);
                    {
                        Import[] imports = stp.getSchema().getImportArray();
                        for (int i = 0; i < imports.length; i++) {
                            Schema imported = downloadSchema(imports[i], emptyStringIfNull(imports[i].getNamespace()), imports[i].getSchemaLocation());
                            if (imported == null) continue;
                            if (!nullableStringsMatch(imported.getTargetNamespace(), imports[i].getNamespace())) {
                                StscState.get().error("Imported schema has a target namespace \"" + imported.getTargetNamespace() + "\" that does not match the specified \"" + imports[i].getNamespace() + "\"", XmlErrorCodes.MISMATCHED_TARGET_NAMESPACE, imports[i]);
                            } else {
                                addScanNeeded(new SchemaToProcess(imported, null));
                            }
                        }
                    }
                    {
                        Include[] includes = stp.getSchema().getIncludeArray();
                        String sourceNamespace = stp.getChameleonNamespace();
                        if (sourceNamespace == null) sourceNamespace = emptyStringIfNull(stp.getSchema().getTargetNamespace());
                        for (int i = 0; i < includes.length; i++) {
                            Schema included = downloadSchema(includes[i], null, includes[i].getSchemaLocation());
                            if (included == null) continue;
                            if (emptyStringIfNull(included.getTargetNamespace()).equals(sourceNamespace)) {
                                SchemaToProcess s = addScanNeeded(new SchemaToProcess(included, null));
                                stp.addInclude(s);
                            } else if (included.getTargetNamespace() != null) {
                                StscState.get().error("Included schema has a target namespace \"" + included.getTargetNamespace() + "\" that does not match the source namespace \"" + sourceNamespace + "\"", XmlErrorCodes.MISMATCHED_TARGET_NAMESPACE, includes[i]);
                            } else {
                                SchemaToProcess s = addScanNeeded(new SchemaToProcess(included, sourceNamespace));
                                stp.addInclude(s);
                                usedEmptyNamespaceSchema(included);
                            }
                        }
                    }
                    {
                        Redefine[] redefines = stp.getSchema().getRedefineArray();
                        String sourceNamespace = stp.getChameleonNamespace();
                        if (sourceNamespace == null) sourceNamespace = emptyStringIfNull(stp.getSchema().getTargetNamespace());
                        for (int i = 0; i < redefines.length; i++) {
                            Schema redefined = downloadSchema(redefines[i], null, redefines[i].getSchemaLocation());
                            if (redefined == null) continue;
                            if (emptyStringIfNull(redefined.getTargetNamespace()).equals(sourceNamespace)) {
                                SchemaToProcess s = addScanNeeded(new SchemaToProcess(redefined, null));
                                stp.addRedefine(s, redefines[i]);
                                hasRedefinitions = true;
                            } else if (redefined.getTargetNamespace() != null) {
                                StscState.get().error("Redefined schema has a target namespace \"" + redefined.getTargetNamespace() + "\" that does not match the source namespace \"" + sourceNamespace + "\"", XmlErrorCodes.MISMATCHED_TARGET_NAMESPACE, redefines[i]);
                            } else {
                                SchemaToProcess s = addScanNeeded(new SchemaToProcess(redefined, sourceNamespace));
                                stp.addRedefine(s, redefines[i]);
                                usedEmptyNamespaceSchema(redefined);
                                hasRedefinitions = true;
                            }
                        }
                    }
                }
                if (!fetchRemainingEmptyNamespaceSchemas()) break;
            }
            if (hasRedefinitions) for (int i = 0; i < result.size(); i++) {
                SchemaToProcess schemaToProcess = (SchemaToProcess) result.get(i);
                schemaToProcess.buildIndirectReferences();
            }
            return (SchemaToProcess[]) result.toArray(new SchemaToProcess[result.size()]);
        }

        private static Reader copySchemaSource(String url, Reader reader, StscState state) {
            if (state.getSchemasDir() == null) return reader;
            String schemalocation = state.sourceNameForUri(url);
            File targetFile = new File(state.getSchemasDir(), schemalocation);
            if (targetFile.exists()) return reader;
            try {
                File parentDir = new File(targetFile.getParent());
                IOUtil.createDir(parentDir, null);
                CharArrayReader car = copy(reader);
                XmlEncodingSniffer xes = new XmlEncodingSniffer(car, null);
                Writer out = new OutputStreamWriter(new FileOutputStream(targetFile), xes.getXmlEncoding());
                IOUtil.copyCompletely(car, out);
                car.reset();
                return car;
            } catch (IOException e) {
                System.err.println("IO Error " + e);
                return reader;
            }
        }

        private static InputStream copySchemaSource(String url, InputStream bytes, StscState state) {
            if (state.getSchemasDir() == null) return bytes;
            String schemalocation = state.sourceNameForUri(url);
            File targetFile = new File(state.getSchemasDir(), schemalocation);
            if (targetFile.exists()) return bytes;
            try {
                File parentDir = new File(targetFile.getParent());
                IOUtil.createDir(parentDir, null);
                ByteArrayInputStream bais = copy(bytes);
                FileOutputStream out = new FileOutputStream(targetFile);
                IOUtil.copyCompletely(bais, out);
                bais.reset();
                return bais;
            } catch (IOException e) {
                System.err.println("IO Error " + e);
                return bytes;
            }
        }

        private static void copySchemaSource(String urlLoc, StscState state, boolean forceCopy) {
            if (state.getSchemasDir() != null) {
                String schemalocation = state.sourceNameForUri(urlLoc);
                File targetFile = new File(state.getSchemasDir(), schemalocation);
                if (forceCopy || !targetFile.exists()) {
                    try {
                        File parentDir = new File(targetFile.getParent());
                        IOUtil.createDir(parentDir, null);
                        InputStream in = null;
                        URL url = new URL(urlLoc);
                        try {
                            in = url.openStream();
                        } catch (FileNotFoundException fnfe) {
                            if (forceCopy && targetFile.exists()) targetFile.delete(); else throw fnfe;
                        }
                        if (in != null) {
                            FileOutputStream out = new FileOutputStream(targetFile);
                            IOUtil.copyCompletely(in, out);
                        }
                    } catch (IOException e) {
                        System.err.println("IO Error " + e);
                    }
                }
            }
        }

        private static ByteArrayInputStream copy(InputStream is) throws IOException {
            byte[] buf = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead;
            while ((bytesRead = is.read(buf, 0, 1024)) > 0) baos.write(buf, 0, bytesRead);
            return new ByteArrayInputStream(baos.toByteArray());
        }

        private static CharArrayReader copy(Reader is) throws IOException {
            char[] buf = new char[1024];
            CharArrayWriter baos = new CharArrayWriter();
            int bytesRead;
            while ((bytesRead = is.read(buf, 0, 1024)) > 0) baos.write(buf, 0, bytesRead);
            return new CharArrayReader(baos.toCharArray());
        }
    }
}
