package tristero.search.dbm;

import momoko.forum.*;
import com.sleepycat.db.*;
import tristero.ntriple.*;
import tristero.search.*;
import tristero.util.*;
import java.util.*;
import java.net.*;
import java.io.*;
import tristero.ntriple.Triple;

/** This triple store uses the DbmMap class, an implementation of the Map interface
 *  with a Berkeley database backend.
 *
 *  @author Brandon Wiley
 **/
public class DbmMapTripleStore extends AbstractTripleStore {

    public static boolean debug = false;

    static Map dbs = new Hashtable();

    static final String dc = "http://purl.org/dc/elements/1.1/#";

    public static void main(String[] args) throws Exception {
        DbmMapTripleStore store = new DbmMapTripleStore();
        System.out.println("clinical psyc & review");
        String token = store.search("", "<" + dc + "Publisher>", "", "file:tripletest.dbm", "exact");
        token = store.search("clinical psyc", "", "", token, "contains");
        token = store.search("review", "", "", token, "contains");
        System.out.println(store.fetch(token));
    }

    public DbmMapTripleStore() throws Exception {
        super();
        System.setProperty("java.protocol.handler.pkgs", "tristero.search.dbm");
    }

    public static DbmTripleBackend fetchDb(String dbName) throws DbException, IOException {
        DbmTripleBackend backend = (DbmTripleBackend) dbs.get(dbName);
        if (backend == null) {
            backend = new DbmTripleBackend(dbName);
            dbs.put(dbName, backend);
        }
        return backend;
    }

    public String test() {
        return "!.-1!";
    }

    protected List fetchList(String dbName) throws DbException, IOException {
        Set set = fetchSet(dbName);
        return new SetList(set);
    }

    protected Set fetchSet(String dbName) throws DbException, IOException {
        URL url = new URL(dbName);
        String protocol = url.getProtocol();
        if (protocol.equals("query")) return (Set) QueryManager.get(dbName); else {
            dbName = url.getFile();
            DbmTripleBackend backend = fetchDb(dbName);
            return backend.getStatements();
        }
    }

    public String superSearch(String dbName, Hashtable criteria) throws MalformedURLException, IOException, URISyntaxException, DbException {
        return superSearch(dbName, (Map) criteria);
    }

    public String superSearch(String dbName, Map criteria) throws MalformedURLException, IOException, URISyntaxException, DbException {
        String lastToken = null;
        Map results = new Hashtable();
        Iterator iterator = criteria.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            List values;
            Object o = criteria.get(key);
            if (o instanceof List) values = (List) o; else {
                values = new Vector();
                values.add(o);
            }
            String token = search("", "<" + dc + key + ">", "", dbName, "exact");
            Iterator i2 = values.iterator();
            while (i2.hasNext()) {
                String value = (String) i2.next();
                token = search("", "", value, token, "contains");
            }
            token = isolateSubjects(token);
            if (lastToken == null) lastToken = token; else lastToken = intersection(lastToken, token);
        }
        if (lastToken == null) return dbName; else return lastToken;
    }

    public Map superFetch(String dbName, String subjectToken, Vector predicates) throws DbException, IOException, URISyntaxException {
        return superFetch(dbName, subjectToken, (List) predicates);
    }

    public Map superFetch(String dbName, String subjectToken, List predicates) throws DbException, IOException, URISyntaxException {
        System.out.println("SUPERFETCH");
        Map map = new Hashtable();
        Set subjects = fetchSet(subjectToken);
        subjects = new StripSet(subjects);
        Iterator iterator = subjects.iterator();
        while (iterator.hasNext()) {
            String subject = (String) iterator.next();
            Map submap = new Hashtable();
            map.put(subject, submap);
            Iterator i2 = predicates.iterator();
            while (i2.hasNext()) {
                String predicate = (String) i2.next();
                System.out.println("working on predicate " + predicate);
                String token = search("<" + subject + ">", "<" + dc + predicate + ">", "", dbName, "exact");
                token = isolateObjects(token);
                Set set = fetchSet(token);
                set = new StripSet(set);
                List l = new SetList(set);
                submap.put(predicate, l);
            }
        }
        return map;
    }

    public List fetch(String uri) throws UnsupportedProtocolException, UnsupportedFormatException, MalformedURLException, FileNotFoundException, IOException, DbException, IOException {
        uri = freezeNow(uri);
        if (debug) System.out.println("frozen uri: " + uri);
        if (debug) System.out.flush();
        List l = fetchList(uri);
        if (debug) System.err.println("fetch() returning " + l);
        if (debug) System.err.flush();
        return l;
    }

    public List fetchEntities(String uri) throws UnsupportedProtocolException, UnsupportedFormatException, MalformedURLException, FileNotFoundException, IOException {
        return null;
    }

    public String freezeNow(String uri) {
        return uri;
    }

    public String freezeEntitiesNow(String uri) {
        return uri;
    }

    public int importFile(String file, String db) throws MalformedURLException, IOException {
        URL url = new URL(file);
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = br.readLine();
        while (line != null) {
            Vector parts = StringUtils.split(line, " ");
            if (parts.size() == 4) parts.remove(3);
            line = StringUtils.join(parts, " ");
            add(db, line);
            line = br.readLine();
        }
        return 0;
    }

    public int exportFile(String db, String file) throws MalformedURLException, IOException, DbException {
        URL url = new URL(file);
        File f = new File(url.getFile());
        FileOutputStream out = new FileOutputStream(f);
        PrintWriter pw = new PrintWriter(out);
        Set statements = fetchSet(db);
        Iterator iterator = statements.iterator();
        while (iterator.hasNext()) {
            String statement = (String) iterator.next();
            pw.println(statement + " .");
        }
        pw.close();
        return 0;
    }

    public int add(String uri, List triple) throws IOException {
        if (debug) System.out.println("Adding " + uri + " " + triple);
        if (debug) System.out.flush();
        addNotifier.add(uri, triple);
        String statement = NTripleWriter.format(triple, false);
        return add(uri, statement);
    }

    public int add(String uri, String statement) throws IOException {
        try {
            DbmTripleBackend backend = fetchDb(uri);
            Set set = backend.getStatements();
            set.add(statement);
            return 0;
        } catch (DbException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public String search(String s, String p, String o, String db, String type) throws MalformedURLException, IOException {
        if (debug) System.out.println("search " + s + " " + p + " " + o + " " + db + " " + type);
        Set results;
        try {
            if (type.equals("exact")) {
                URL url = new URL(db);
                if (!url.getProtocol().equals("file")) throw new MalformedURLException("Exact matches can only be made on files");
                results = exactMatch(s, p, o, url.getFile());
            } else {
                System.out.println("special search of type " + type);
                Set set = fetchSet(db);
                Filter f = (Filter) CriteriaFactory.create(db, s, p, o, type);
                System.out.println("setting new filter " + f + " " + db + " " + set.getClass());
                results = new FilteredSet(set, f);
            }
        } catch (Exception e) {
            e.printStackTrace();
            results = new HashSet();
        }
        String statement = StringUtils.join(s, p, o, " ");
        statement = StringUtils.join(statement, db, type, " ");
        try {
            String newname = "query:" + URLEncoder.encode(statement);
            if (debug) System.out.println("New search " + newname);
            QueryManager.put(newname, results);
            return newname;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException();
        }
    }

    public Set exactMatch(String s, String p, String o, String db) throws DbException, IOException {
        System.out.println("exact match");
        if (s.equals("") && p.equals("") && o.equals("")) return fetchDb(db).getStatements(); else if (s.equals("") && p.equals("")) return objectMatch(o, db); else if (s.equals("") && o.equals("")) return predicateMatch(p, db); else if (p.equals("") && o.equals("")) return subjectMatch(s, db); else if (s.equals("")) return predicateObjectMatch(p, o, db); else if (p.equals("")) return subjectObjectMatch(s, o, db); else if (o.equals("")) return subjectPredicateMatch(s, p, db); else return statementMatch(s, p, o, db);
    }

    public Set subjectMatch(String s, String db) throws DbException, IOException {
        DbmTripleBackend backend = fetchDb(db);
        Map map = backend.getSubjectMap();
        return (Set) map.get(s);
    }

    public Set predicateMatch(String p, String db) throws DbException, IOException {
        System.out.println("Predicate Match for " + db);
        DbmTripleBackend backend = fetchDb(db);
        Map map = backend.getPredicateMap();
        return (Set) map.get(p);
    }

    public Set objectMatch(String o, String db) throws DbException, IOException {
        System.out.println("objectMatch");
        DbmTripleBackend backend = fetchDb(db);
        Map map = backend.getObjectMap();
        return (Set) map.get(o);
    }

    public Set subjectPredicateMatch(String s, String p, String db) throws DbException, IOException {
        DbmTripleBackend backend = fetchDb(db);
        Map map = backend.getSubjectPredicateMap();
        return (Set) map.get(s + " " + p);
    }

    public Set subjectObjectMatch(String s, String o, String db) throws DbException, IOException {
        DbmTripleBackend backend = fetchDb(db);
        Map map = backend.getSubjectObjectMap();
        return (Set) map.get(s + " " + o);
    }

    public Set predicateObjectMatch(String p, String o, String db) throws DbException, IOException {
        DbmTripleBackend backend = fetchDb(db);
        Map map = backend.getPredicateObjectMap();
        return (Set) map.get(p + " " + o);
    }

    public Set statementMatch(String s, String p, String o, String db) throws DbException, IOException {
        DbmTripleBackend backend = fetchDb(db);
        Set statements = backend.getStatements();
        String statement = s + " " + p + " " + o;
        Set set = new HashSet();
        if (statements.contains(statement)) set.add(statement);
        return set;
    }

    public String intersection(String a, String b) throws MalformedURLException, IOException, DbException {
        Set sa = fetchSet(a);
        Set sb = fetchSet(b);
        Set sc = new IntersectionSet(sa, sb);
        String uri = "query:intersection/" + a + "/" + b;
        QueryManager.put(uri, sc);
        return uri;
    }

    public void remove(String uri, List triple) {
        try {
            if (debug) System.out.println("Removing " + triple);
            if (debug) System.out.flush();
            DbmTripleBackend backend = fetchDb(uri);
            Set statements = backend.getStatements();
            String statement = NTripleWriter.format(triple, false);
            statements.remove(statement);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String isolateSubjects(String uri) throws URISyntaxException, DbException, IOException {
        Set source = fetchSet(uri);
        Set set = new IsolationSet(source, 0);
        uri = "query:isolate-subjects/" + uri;
        QueryManager.put(uri, set);
        return uri;
    }

    public String isolatePredicates(String uri) {
        return null;
    }

    public String isolateObjects(String uri) throws URISyntaxException, DbException, IOException {
        Set source = fetchSet(uri);
        Set set = new IsolationSet(source, 2);
        uri = "query:isolate-objects/" + uri;
        QueryManager.put(uri, set);
        return uri;
    }

    public String expandSubjects(String uri, String uri2) {
        return null;
    }

    public String expandPredicates(String uri, String uri2) {
        return null;
    }

    public String expandObjects(String uri, String uri2) {
        return null;
    }
}
