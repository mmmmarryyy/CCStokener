package org.dave.bracket.properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A better Properties class. This class is thread-safe.
 * 
 * @author Dave
 *
 */
public class PropertiesImpl extends AbstractPropertiesBase implements Properties {

    public PropertiesImpl() {
        super();
        initMap();
    }

    protected void initMap() {
        map = new LinkedHashMap<String, ValueModel>();
    }

    public PropertiesImpl(java.util.Properties legacy) {
        this();
        lock.lock();
        try {
            Set<Object> set = legacy.keySet();
            for (Object key : set) {
                String val = legacy.getProperty(String.valueOf(key));
                this.put(String.valueOf(key), val);
            }
        } finally {
            lock.unlock();
        }
    }

    public PropertiesImpl(URL url) {
        this();
        InputStream in = null;
        lock.lock();
        try {
            in = url.openStream();
            PropertiesLexer lexer = new PropertiesLexer(in);
            lexer.lex();
            List<PropertiesToken> list = lexer.getList();
            new PropertiesParser(list, this).parse();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
            lock.unlock();
        }
    }

    public PropertiesImpl(InputStream in) {
        this();
        lock.lock();
        try {
            PropertiesLexer lexer = new PropertiesLexer(in);
            lexer.lex();
            List<PropertiesToken> list = lexer.getList();
            new PropertiesParser(list, this).parse();
        } finally {
            lock.unlock();
        }
    }

    public PropertiesImpl(Reader in) {
        this();
        lock.lock();
        try {
            PropertiesLexer lexer = new PropertiesLexer(in);
            lexer.lex();
            List<PropertiesToken> list = lexer.getList();
            new PropertiesParser(list, this).parse();
        } finally {
            lock.unlock();
        }
    }

    /**
	 * Get the value of the property; concatenate multiple lines.
	 * @param key
	 * @return
	 * @throws RuntimeException if key is not present.
	 * 
	 */
    public String get(String key) {
        lock.lock();
        try {
            ValueModel val = map.get(key);
            if (val == null) throw new RuntimeException("Missing value " + key);
            return val.getValue();
        } finally {
            lock.unlock();
        }
    }

    /**
	 * if values exist, they are removed and replaced
	 * @param key
	 * @param values
	 */
    public void put(String key, String... values) {
        lock.lock();
        try {
            if (!map.containsKey(key)) {
                map.put(key, new ValueModel(values));
            } else {
                ValueModel val = map.get(key);
                val.getValues().clear();
                for (String s : values) {
                    val.getValues().add(s);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public Map<String, ValueModel> getPropertyMap() {
        return map;
    }

    public List<String> getComments(String key) {
        lock.lock();
        try {
            if (!containsKey(key)) return null;
            return getPropertyMap().get(key).getComments();
        } finally {
            lock.unlock();
        }
    }

    public char getSeparator(String key) {
        lock.lock();
        try {
            if (!containsKey(key)) return '\0';
            return getPropertyMap().get(key).getSeparator();
        } finally {
            lock.unlock();
        }
    }

    public List<String> getKeyGroup(String keyBase) {
        lock.lock();
        try {
            List<String> list = new ArrayList<String>();
            for (String s : map.keySet()) {
                if (s.indexOf(keyBase) == 0) {
                    list.add(s);
                }
            }
            return list;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int hashCode() {
        lock.lock();
        try {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((map == null) ? 0 : map.hashCode());
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean equals(Object obj) {
        lock.lock();
        try {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            PropertiesImpl other = (PropertiesImpl) obj;
            if (map == null) {
                if (other.map != null) return false;
            } else if (!map.equals(other.map)) return false;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
	 * This is a merge function, existing keys which do not collide
	 * with the incoming are kept, keys that collide are overwritten with the new values
	 * 
	 * TODO, cause comments to come over as well
	 * 
	 * @param props
	 */
    public Properties merge(Properties props) {
        merge(props, false);
        return this;
    }

    public Properties merge(Properties props, boolean mergeComments) {
        lock.lock();
        try {
            Set<String> set = props.getPropertyMap().keySet();
            for (String key : set) {
                if (mergeComments) {
                    List<String> comments = this.getComments(key);
                    List<String> newComments = props.getComments(key);
                    if (comments == null) comments = new ArrayList<String>();
                    if (newComments != null) {
                        for (String s : newComments) comments.add(s);
                    }
                    ValueModel model = new ValueModel(comments, props.getPropertyMap().get(key).getValues());
                    this.getPropertyMap().put(key, model);
                } else {
                    this.getPropertyMap().put(key, props.getPropertyMap().get(key));
                }
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    /**
	 * Use "\\." as the separator
	 */
    public Node getTree() {
        return getTree(new GroupParams());
    }

    public Node getTree(GroupParams params) {
        lock.lock();
        try {
            TreeBuilder builder = new TreeBuilder(this, params.rootNodeName);
            Set<String> keys = map.keySet();
            for (String key : keys) {
                if (key.startsWith(params.getPartialKey())) {
                    ValueModel value = map.get(key);
                    builder.createNode(key, value, params.getSeparator());
                }
            }
            return builder.tree();
        } finally {
            lock.unlock();
        }
    }

    public Properties getGroup(GroupParams params) {
        PropertiesImpl impl = new PropertiesImpl();
        for (String key : this.getPropertyMap().keySet()) {
            if (key.startsWith(params.getPartialKey())) {
                ValueModel value = map.get(key);
                impl.getPropertyMap().put(key, value);
            }
        }
        return impl;
    }

    public int intValue(String key) {
        return Integer.parseInt(get(key));
    }

    public long longValue(String key) {
        return Long.parseLong(get(key));
    }

    public Date dateValue(String key) {
        lock.lock();
        try {
            String val = get(key);
            if (val.trim().length() != 13) throw new RuntimeException("Value does not look like a long that could be used as a date");
            return new java.util.Date(longValue(key));
        } finally {
            lock.unlock();
        }
    }

    public Date dateValue(String key, String format) throws ParseException {
        lock.lock();
        try {
            SimpleDateFormat f = new SimpleDateFormat(format);
            return f.parse(get(key));
        } finally {
            lock.unlock();
        }
    }

    public boolean hasValue(String key) {
        lock.lock();
        try {
            String val = get(key);
            return val != null && (!val.equals(""));
        } finally {
            lock.unlock();
        }
    }

    public void synchronize(Node rootNode) {
        lock.lock();
        try {
            new TreeSynchronizer(this, rootNode).synch();
        } finally {
            lock.unlock();
        }
    }
}
