package storage.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import org.nodal.filesystem.Document;
import org.nodal.model.Node;
import org.nodal.model.NodeContent;
import org.nodal.nav.Path;
import org.nodal.type.NodeType;
import org.nodal.util.Name;
import org.nodal.util.Names;
import org.nodal.util.Namespace;
import storage.framework.AbstractRepository;
import storage.framework.SBNStreamLoader;
import storage.framework.StreamBasedNode;
import storage.framework.TxnManager;

/**
 * 
 * Created on Nov 5, 2003
 * 
 * @author leei
 */
public class HttpRepository extends AbstractRepository {

    /**
   * 
   * Created on Nov 5, 2003
   * 
   * @author leei
   */
    final class Backend extends AbstractBackend {

        /**
     * @param repo
     * @param store
     */
        protected Backend() {
            super(HttpRepository.this, null, null);
        }

        public NodeContent.Editor createNode(NodeType type, Node context) {
            return HttpNode.create(this, type, context);
        }

        public NodeContent.Editor cloneNode(Node node, Node context) {
            return HttpNode.clone(this, node, context);
        }

        public TxnManager txnManager() {
            return null;
        }

        public void commitTxn(TxnManager.Resolver txn) {
        }

        /**
     * @return
     */
        Name nextId() {
            return ns.name("n" + nextId++);
        }
    }

    private URL url;

    private HttpURLConnection connection;

    private Namespace ns;

    private int nextId;

    protected final AbstractRepository.Backend createBackend() {
        return new Backend();
    }

    /**
   * Create a Repository that communicvates via HTTP.
   * 
   * @param uri
   *          the URI of this Repository
   */
    HttpRepository(Path path) throws IOException {
        super(path);
        this.url = new URL(path.toURLString());
        HttpURLConnection.setFollowRedirects(true);
        this.connection = (HttpURLConnection) url.openConnection();
        this.ns = Names.getNamespace(path);
    }

    public void close() {
        connection.disconnect();
        connection = null;
    }

    public Document document(Path abs, Path rel) {
        try {
            URL docURL = new URL(url, rel.toURLString());
            HttpURLConnection conn = (HttpURLConnection) docURL.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            System.out.println("URL = " + docURL);
            Map hdrs = conn.getHeaderFields();
            for (Iterator i = hdrs.keySet().iterator(); i.hasNext(); ) {
                Object key = i.next();
                String val = conn.getHeaderField((String) key);
                System.out.println(" : " + key + " = " + val);
            }
            System.out.println("  : content-type: " + conn.getContentType());
            System.out.println("  : content-encoding: " + conn.getContentEncoding());
            System.out.println("  : content-length: " + conn.getContentLength());
            SBNStreamLoader sbnLoader = new SBNStreamLoader(backend, abs, conn.getInputStream(), conn.getContentType());
            return StreamBasedNode.loadDocument(sbnLoader);
        } catch (IOException e) {
            return null;
        }
    }
}
