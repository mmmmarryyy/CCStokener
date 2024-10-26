package org.lindenb.sw.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.stream.XMLStreamException;
import org.lindenb.io.IOUtils;
import org.lindenb.sw.RDFException;
import org.lindenb.sw.io.RDFHandler;
import org.lindenb.sw.nodes.Literal;
import org.lindenb.sw.nodes.RDFNode;
import org.lindenb.sw.nodes.Resource;
import org.lindenb.sw.nodes.Statement;
import org.lindenb.util.AbstractWalker;
import org.lindenb.util.Walker;
import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.JoinCursor;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

public class BDBStore {

    private static final byte OPCODE_RESOURCE = 'R';

    private static final byte OPCODE_LITERAL = 'L';

    private Environment environment;

    /** map suject to predicate/object */
    private Database statementsDB;

    private SecondaryDatabase subject2triple = null;

    private SecondaryDatabase predicate2triple = null;

    private SecondaryDatabase objectLiteral2triple = null;

    private SecondaryDatabase objectRsrc2triple = null;

    private static final StatementBinding STMT_BINDING = new StatementBinding();

    private static final ResourceBinding RESOURCE_BINDING = new ResourceBinding();

    private static final LiteralBinding LITERAL_KEY_BINDING = new LiteralBinding();

    /**
	 * 
	 * ResourceBinding
	 *
	 */
    private static class ResourceBinding extends TupleBinding<Resource> {

        public void objectToEntry(Resource rsrc, TupleOutput out) {
            out.writeString(rsrc.getURI());
        }

        public Resource entryToObject(TupleInput in) {
            return new Resource(in.readString());
        }
    }

    /**
	 * 
	 * LiteralBinding
	 *
	 */
    private static class LiteralBinding extends TupleBinding<Literal> {

        public void objectToEntry(Literal literal, TupleOutput out) {
            zip(literal.getLexicalForm(), out);
            String s = literal.getDatatypeURI();
            if (s == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                zip(s, out);
            }
            s = literal.getLanguage();
            if (s == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                zip(s, out);
            }
        }

        public Literal entryToObject(TupleInput in) {
            String s = unzip(in);
            String dataType = (in.readBoolean() ? unzip(in) : null);
            String lang = (in.readBoolean() ? unzip(in) : null);
            return new Literal(s, dataType, lang);
        }

        private String unzip(TupleInput input) {
            boolean zipped = input.readBoolean();
            if (!zipped) {
                return input.readString();
            }
            int len = input.readInt();
            try {
                byte array[] = new byte[len];
                input.read(array);
                GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(array));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copyTo(in, out);
                in.close();
                out.close();
                return new String(out.toByteArray());
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }

        private void zip(String object, TupleOutput output) {
            byte array[] = object.getBytes();
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream out = new GZIPOutputStream(baos);
                ByteArrayInputStream in = new ByteArrayInputStream(array);
                IOUtils.copyTo(in, out);
                in.close();
                out.close();
                byte array2[] = baos.toByteArray();
                if (array2.length + 4 < array.length) {
                    output.writeBoolean(true);
                    output.writeInt(array2.length);
                    output.write(array2);
                } else {
                    output.writeBoolean(false);
                    output.writeString(object);
                }
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }
    }

    /**
	 * 
	 * StatementBinding
	 *
	 */
    private static class StatementBinding extends TupleBinding<Statement> {

        public void objectToEntry(Statement stmt, TupleOutput out) {
            RESOURCE_BINDING.objectToEntry(stmt.getSubject(), out);
            RESOURCE_BINDING.objectToEntry(stmt.getPredicate(), out);
            if (stmt.getValue().isResource()) {
                out.writeByte(OPCODE_RESOURCE);
                RESOURCE_BINDING.objectToEntry(stmt.getValue().asResource(), out);
            } else {
                out.writeByte(OPCODE_LITERAL);
                LITERAL_KEY_BINDING.objectToEntry(stmt.getValue().asLiteral(), out);
            }
        }

        public Statement entryToObject(TupleInput in) {
            Resource subject = RESOURCE_BINDING.entryToObject(in);
            Resource predicate = RESOURCE_BINDING.entryToObject(in);
            RDFNode object = null;
            switch(in.readByte()) {
                case OPCODE_RESOURCE:
                    {
                        object = RESOURCE_BINDING.entryToObject(in);
                        ;
                        break;
                    }
                case OPCODE_LITERAL:
                    {
                        object = LITERAL_KEY_BINDING.entryToObject(in);
                        ;
                        break;
                    }
                default:
                    throw new IllegalStateException("Unknown opcode");
            }
            return new Statement(subject, predicate, object);
        }
    }

    /**
	 * CursorIterator
	 */
    private abstract class CursorWalker<T> extends AbstractWalker<T> {

        protected Cursor cursor;

        private DatabaseEntry keyEntry;

        private DatabaseEntry valueEntry;

        protected CursorWalker(Cursor cursor) {
            this.cursor = cursor;
            this.keyEntry = new DatabaseEntry();
            this.valueEntry = new DatabaseEntry();
        }

        protected abstract T readNext(DatabaseEntry key, DatabaseEntry value) throws DatabaseException;

        @Override
        public T next() {
            if (this.cursor == null) return null;
            try {
                T t = readNext(this.keyEntry, this.valueEntry);
                if (t == null) close();
                return t;
            } catch (DatabaseException err) {
                close();
                return null;
            }
        }

        @Override
        public void close() {
            if (this.cursor != null) {
                try {
                    this.cursor.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.cursor = null;
        }
    }

    private static class CursorAndEntries {

        Cursor cursor;

        DatabaseEntry keyEntry = new DatabaseEntry();

        DatabaseEntry valueEntry = new DatabaseEntry();
    }

    /**
	 * CursorIterator
	 */
    private class JoinWalker extends AbstractWalker<Statement> {

        private boolean _firstCall = true;

        protected List<CursorAndEntries> cursorEntries;

        private JoinCursor joinCursor = null;

        protected DatabaseEntry keyEntry = new DatabaseEntry();

        protected DatabaseEntry valueEntry = new DatabaseEntry();

        protected JoinWalker(List<CursorAndEntries> cursorsEntries) {
            this.cursorEntries = new ArrayList<CursorAndEntries>(cursorsEntries);
        }

        @Override
        public Statement next() {
            try {
                if (this._firstCall) {
                    this._firstCall = false;
                    for (CursorAndEntries ca : this.cursorEntries) {
                        if (ca.cursor.getSearchKey(ca.keyEntry, ca.valueEntry, null) != OperationStatus.SUCCESS) {
                            return null;
                        }
                    }
                    Cursor cursors[] = new Cursor[this.cursorEntries.size()];
                    for (int i = 0; i < this.cursorEntries.size(); ++i) {
                        cursors[i] = cursorEntries.get(i).cursor;
                    }
                    joinCursor = BDBStore.this.statementsDB.join(cursors, null);
                }
                if (joinCursor.getNext(keyEntry, valueEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    return STMT_BINDING.entryToObject(keyEntry);
                } else {
                    close();
                    return null;
                }
            } catch (DatabaseException err) {
                close();
                throw new RuntimeException(err);
            }
        }

        @Override
        public void close() {
            for (CursorAndEntries cursor : cursorEntries) {
                try {
                    if (cursor.cursor != null) cursor.cursor.close();
                    cursor.cursor = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                if (joinCursor != null) joinCursor.close();
                joinCursor = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * 
	 * RDFStore
	 * 
	 */
    BDBStore() {
    }

    public void open(File envFile) throws RDFException {
        close();
        if (!envFile.exists()) {
            if (!envFile.mkdir()) throw new RDFException("cannot create " + envFile);
        }
        if (!envFile.isDirectory()) throw new RDFException("not a directory " + envFile);
        try {
            EnvironmentConfig envCfg = new EnvironmentConfig();
            envCfg.setAllowCreate(true);
            this.environment = new Environment(envFile, envCfg);
            DatabaseConfig cfg = new DatabaseConfig();
            cfg.setAllowCreate(true);
            cfg.setReadOnly(false);
            this.statementsDB = this.environment.openDatabase(null, "triples", cfg);
            SecondaryConfig config2 = new SecondaryConfig();
            config2.setAllowCreate(true);
            config2.setSortedDuplicates(true);
            config2.setKeyCreator(new SecondaryKeyCreator() {

                @Override
                public boolean createSecondaryKey(SecondaryDatabase arg0, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) throws DatabaseException {
                    Statement stmt = STMT_BINDING.entryToObject(key);
                    TupleOutput out = new TupleOutput();
                    RESOURCE_BINDING.objectToEntry(stmt.getSubject(), out);
                    result.setData(out.toByteArray());
                    return true;
                }
            });
            this.subject2triple = this.environment.openSecondaryDatabase(null, "subject2triple", statementsDB, config2);
            config2 = new SecondaryConfig();
            config2.setAllowCreate(true);
            config2.setSortedDuplicates(true);
            config2.setKeyCreator(new SecondaryKeyCreator() {

                @Override
                public boolean createSecondaryKey(SecondaryDatabase arg0, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) throws DatabaseException {
                    Statement stmt = STMT_BINDING.entryToObject(key);
                    if (stmt.getValue().isResource()) return false;
                    TupleOutput out = new TupleOutput();
                    LITERAL_KEY_BINDING.objectToEntry(stmt.getValue().asLiteral(), out);
                    result.setData(out.toByteArray());
                    return true;
                }
            });
            this.objectLiteral2triple = this.environment.openSecondaryDatabase(null, "objectLiteral2triple", statementsDB, config2);
            config2 = new SecondaryConfig();
            config2.setAllowCreate(true);
            config2.setSortedDuplicates(true);
            config2.setKeyCreator(new SecondaryKeyCreator() {

                @Override
                public boolean createSecondaryKey(SecondaryDatabase arg0, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) throws DatabaseException {
                    Statement stmt = STMT_BINDING.entryToObject(key);
                    if (!stmt.getValue().isResource()) return false;
                    TupleOutput out = new TupleOutput();
                    RESOURCE_BINDING.objectToEntry(stmt.getValue().asResource(), out);
                    result.setData(out.toByteArray());
                    return true;
                }
            });
            this.objectRsrc2triple = this.environment.openSecondaryDatabase(null, "objectRsrc2triple", statementsDB, config2);
            config2 = new SecondaryConfig();
            config2.setAllowCreate(true);
            config2.setSortedDuplicates(true);
            config2.setKeyCreator(new SecondaryKeyCreator() {

                @Override
                public boolean createSecondaryKey(SecondaryDatabase arg0, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) throws DatabaseException {
                    Statement stmt = STMT_BINDING.entryToObject(key);
                    TupleOutput out = new TupleOutput();
                    RESOURCE_BINDING.objectToEntry(stmt.getPredicate(), out);
                    result.setData(out.toByteArray());
                    return true;
                }
            });
            this.predicate2triple = this.environment.openSecondaryDatabase(null, "predicate2triple", statementsDB, config2);
        } catch (DatabaseException e) {
            throw new RDFException(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Exception err) {
        }
        super.finalize();
    }

    public void close() throws RDFException {
        try {
            if (subject2triple != null) subject2triple.close();
            if (predicate2triple != null) predicate2triple.close();
            if (objectLiteral2triple != null) objectLiteral2triple.close();
            if (objectRsrc2triple != null) objectRsrc2triple.close();
            if (statementsDB != null) statementsDB.close();
            if (environment != null) {
                environment.cleanLog();
                environment.compress();
                environment.close();
            }
        } catch (DatabaseException err) {
            throw new RDFException(err);
        }
        subject2triple = null;
        predicate2triple = null;
        objectLiteral2triple = null;
        objectRsrc2triple = null;
        statementsDB = null;
        environment = null;
    }

    public void clear() throws RDFException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            Cursor c = statementsDB.openCursor(null, null);
            while (c.getNext(key, data, null) == OperationStatus.SUCCESS) {
                c.delete();
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Environment getEnvironment() {
        return this.environment;
    }

    protected Database getTripleDB() {
        return this.statementsDB;
    }

    public AbstractWalker<Statement> listStatements() throws RDFException {
        try {
            return new CursorWalker<Statement>(this.statementsDB.openCursor(null, null)) {

                @Override
                protected Statement readNext(DatabaseEntry key, DatabaseEntry value) throws DatabaseException {
                    if (this.cursor.getNext(key, value, null) == OperationStatus.SUCCESS) {
                        return STMT_BINDING.entryToObject(key);
                    }
                    return null;
                }
            };
        } catch (DatabaseException e) {
            throw new RDFException(e);
        }
    }

    public AbstractWalker<Statement> listStatements(Resource s, Resource p, RDFNode o) throws RDFException {
        if (s == null && p == null && o == null) {
            return listStatements();
        }
        try {
            List<CursorAndEntries> cursors = new ArrayList<CursorAndEntries>(3);
            if (s != null) {
                CursorAndEntries ca = new CursorAndEntries();
                ca.cursor = this.subject2triple.openCursor(null, null);
                RESOURCE_BINDING.objectToEntry(s, ca.keyEntry);
                cursors.add(ca);
            }
            if (p != null) {
                CursorAndEntries ca = new CursorAndEntries();
                ca.cursor = this.predicate2triple.openCursor(null, null);
                RESOURCE_BINDING.objectToEntry(p, ca.keyEntry);
                cursors.add(ca);
            }
            if (o != null) {
                if (o.isResource()) {
                    CursorAndEntries ca = new CursorAndEntries();
                    ca.cursor = this.objectRsrc2triple.openCursor(null, null);
                    RESOURCE_BINDING.objectToEntry(o.asResource(), ca.keyEntry);
                    cursors.add(ca);
                } else {
                    CursorAndEntries ca = new CursorAndEntries();
                    ca.cursor = this.objectLiteral2triple.openCursor(null, null);
                    LITERAL_KEY_BINDING.objectToEntry(o.asLiteral(), ca.keyEntry);
                    cursors.add(ca);
                }
            }
            return new JoinWalker(cursors);
        } catch (DatabaseException e) {
            throw new RDFException(e);
        }
    }

    public long size() throws RDFException {
        try {
            return getTripleDB().count();
        } catch (DatabaseException err) {
            throw new RDFException(err);
        }
    }

    public boolean contains(Statement stmt) throws RDFException {
        Walker<Statement> iter = null;
        try {
            iter = listStatements(stmt.getSubject(), stmt.getPredicate(), stmt.getValue());
            return (iter.next() != null);
        } catch (RDFException e) {
            throw e;
        } finally {
            if (iter != null) iter.close();
        }
    }

    public BDBStore add(Resource s, Resource p, RDFNode o) throws RDFException {
        return add(new Statement(s, p, o));
    }

    public BDBStore add(Statement stmt) throws RDFException {
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        try {
            STMT_BINDING.objectToEntry(stmt, key);
            LongBinding.longToEntry(System.currentTimeMillis(), value);
            if (getTripleDB().put(null, key, value) != OperationStatus.SUCCESS) {
                throw new RDFException("Cannot insert " + stmt);
            }
            return this;
        } catch (DatabaseException error) {
            throw new RDFException(error);
        }
    }

    public void read(InputStream in) throws IOException, RDFException, XMLStreamException {
        org.lindenb.sw.io.RDFHandler h = new RDFHandler() {

            @Override
            public void found(URI subject, URI predicate, Object value, URI dataType, String lang, int index) {
                try {
                    if (value instanceof URI) {
                        add(new Resource(subject.toString()), new Resource(predicate.toString()), new Resource(value.toString()));
                    } else {
                        add(new Resource(subject.toString()), new Resource(predicate.toString()), new Literal((String) value));
                    }
                } catch (RDFException err) {
                    throw new RuntimeException(err);
                }
            }
        };
        h.parse(in);
    }

    public static void main(String[] args) {
        BDBStore rdfStore = null;
        try {
            URL url = new URL("http://archive.geneontology.org/latest-lite/go_20090621-termdb.owl.gz");
            rdfStore = new BDBStore();
            rdfStore.open(new File("/tmp/rdfdb"));
            rdfStore.clear();
            InputStream in = new GZIPInputStream(url.openStream());
            rdfStore.read(in);
            in.close();
            System.out.println("Done");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rdfStore != null) try {
                rdfStore.close();
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }
}
