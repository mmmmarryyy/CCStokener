package storage.jar;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.nodal.Repository;
import org.nodal.filesystem.Document;
import org.nodal.model.Node;
import org.nodal.model.NodeContent;
import org.nodal.nav.Path;
import org.nodal.nav.PathOperator;
import org.nodal.nav.Paths;
import org.nodal.type.NodeType;
import org.nodal.util.Name;
import org.nodal.util.Names;
import org.nodal.util.Namespace;
import storage.framework.AbstractRepository;
import storage.framework.DocFromURL;
import storage.framework.TxnManager;

/**
 * @author leei
 */
public class JarRepository extends AbstractRepository implements Repository {

    private int idNumber;

    private Namespace ns;

    private JarURLConnection connection;

    private URL url;

    private static Map jars = new HashMap();

    /**
   * Constructor for JarRepository. Takes a jar: Path and constructs a new
   * repository.
   * 
   * @param path
   *                the jar: <url>! Path of this repository
   */
    private JarRepository(Path path) throws IOException {
        super(path);
        this.url = new URL(path.toURLString());
        this.connection = (JarURLConnection) url.openConnection();
        this.ns = Names.getNamespace(path);
        this.idNumber = 0;
    }

    protected AbstractRepository.Backend createBackend() {
        return new Backend();
    }

    class Backend extends AbstractBackend {

        private Backend() {
            super(JarRepository.this, null);
        }

        public NodeContent.Editor createNode(NodeType type, Node context) {
            return JarNode.create(this, type, context);
        }

        public NodeContent.Editor cloneNode(Node node, Node context) {
            return JarNode.clone(JarRepository.this, node, context);
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
            return ns.name("n" + idNumber++);
        }
    }

    public Document document(Path abs, Path rel) throws Path.Failure {
        System.out.println(this + ".document(" + rel + ") = " + abs);
        try {
            return DocFromURL.loadDocument(this, abs);
        } catch (MalformedURLException e) {
            throw new Path.Failure("Malformed URL: " + abs);
        } catch (IOException e) {
            throw new Path.Failure("IOException " + e);
        }
    }

    public void close() {
    }

    /**
   * @param path
   * @return
   */
    public static Repository create(Path path) throws IOException {
        try {
            PathOperator root = Paths.createOp("docroot");
            Path jarPath = path.apply(root);
            JarRepository jar = (JarRepository) jars.get(jarPath);
            if (jar == null) {
                jar = new JarRepository(jarPath);
                jars.put(jarPath, jar);
            }
            return jar;
        } catch (Path.Failure e) {
            return null;
        }
    }
}
