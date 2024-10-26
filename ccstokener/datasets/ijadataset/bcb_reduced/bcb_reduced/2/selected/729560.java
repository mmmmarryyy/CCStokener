package org.knopflerfish.util.metatype;

import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.*;
import org.osgi.service.metatype.*;
import java.io.*;
import java.util.*;
import java.net.URL;

/**
 * Implementation of the ObjectClassDefinition interface.
 *
 */
public class OCD implements ObjectClassDefinition {

    URL sourceURL;

    String id;

    String name;

    String localized_name;

    String desc;

    String localized_desc;

    List optAttrs;

    List reqAttrs;

    List localized_optAttrs;

    List localized_reqAttrs;

    Hashtable icons = new Hashtable();

    Hashtable localized_icons = null;

    int maxInstances = 1;

    /**
   * Create a new, empty ObjectClassDefinition.
   *
   * @param id unique ID of the definition.
   * @param name human-readable name of the definition. If set to 
   *         <tt>null</tt>,
   *        use <i>id</i> as name.
   * @param desc human-readable description of the definition
   * @throws IllegalArgumentException if <i>id</i> is <null> or empty
   */
    public OCD(String id, String name, String desc, URL sourceURL) {
        if (id == null || "".equals(id)) {
            throw new IllegalArgumentException("Id must not be null or empty");
        }
        this.id = id;
        this.name = name != null ? name : id;
        this.desc = desc;
        this.optAttrs = new ArrayList();
        this.reqAttrs = new ArrayList();
        this.sourceURL = sourceURL;
    }

    /**
   * Creates an OCD with attribute definitions from an existing 
   * dictionary.
   *
   * @param id unique ID of the definition.
   * @param name human-readable name of the definition. If set to <tt>null</tt>,
   *       use <i>id</i> as name.
   * @param desc human-readable description of the definition
   * @param props set of key value pairs used for attribute definitions.
   *        all entries in <i>props</i> will be set as REQUIRED
   *        atttributes.
   * @throws IllegalArgumentException if <i>id</i> is <null> or empty
   *
   */
    public OCD(String id, String name, String desc, Dictionary props) {
        this(id, name, desc, (URL) null);
        for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            if ("service.pid".equals(key.toLowerCase())) {
                continue;
            }
            if ("service.factorypid".equals(key.toLowerCase())) {
                continue;
            }
            Object val = props.get(key);
            int card = 0;
            int type = AD.getType(val);
            if (val instanceof Vector) {
                card = Integer.MIN_VALUE;
            } else if (val.getClass().isArray()) {
                card = Integer.MAX_VALUE;
            }
            AD ad = new AD(key, type, card, key, card == 0 ? new String[] { AD.toString(val) } : null);
            add(ad, REQUIRED);
        }
    }

    /**
   * Add an attribute definition
   *
   * @param attr definition to add
   * @param filter either OPTIONAL or REQUIRED
   * @throws Illegalargumentexception if filter is not OPTIONAL or REQUIRED
   */
    public void add(AD attr, int filter) {
        switch(filter) {
            case OPTIONAL:
                optAttrs.add(attr);
                break;
            case REQUIRED:
                reqAttrs.add(attr);
                break;
            default:
                throw new IllegalArgumentException("Unsupported filter=" + filter);
        }
    }

    public AttributeDefinition[] getAttributeDefinitions(int filter) {
        AttributeDefinition[] attrs = null;
        switch(filter) {
            case ALL:
                {
                    ArrayList all = new ArrayList();
                    if (localized_optAttrs == null) {
                        all.addAll(reqAttrs);
                        all.addAll(optAttrs);
                    } else {
                        all.addAll(localized_reqAttrs);
                        all.addAll(localized_optAttrs);
                    }
                    AttributeDefinition[] ads = new AttributeDefinition[all.size()];
                    all.toArray(ads);
                    return ads;
                }
            case REQUIRED:
                {
                    AttributeDefinition[] ads = new AttributeDefinition[reqAttrs.size()];
                    if (localized_reqAttrs == null) {
                        reqAttrs.toArray(ads);
                    } else {
                        localized_reqAttrs.toArray(ads);
                    }
                    return ads;
                }
            case OPTIONAL:
                {
                    AttributeDefinition[] ads = new AttributeDefinition[optAttrs.size()];
                    if (localized_optAttrs == null) {
                        optAttrs.toArray(ads);
                    } else {
                        localized_optAttrs.toArray(ads);
                    }
                    return ads;
                }
            default:
                throw new IllegalArgumentException("Unsupported filter=" + filter);
        }
    }

    /**
   * Get description of OCD. 
   */
    public String getDescription() {
        if (localized_desc != null) {
            return localized_desc;
        } else {
            return desc;
        }
    }

    /**
   * This code is handles multiple icon sizes but the spec. currently
   * only allows on size.
   *
   * @param size Size of icon requested, if size is 0 return largest icon.
   */
    String getIconURL(int size) {
        Hashtable itab = (localized_icons != null) ? localized_icons : icons;
        if (size == 0) {
            for (Enumeration keys = itab.keys(); keys.hasMoreElements(); ) {
                int i = ((Integer) keys.nextElement()).intValue();
                if (size < i) {
                    size = i;
                }
            }
        }
        return (String) itab.get(new Integer(size));
    }

    /**
   * This code is handles multiple icon sizes but the spec. currently
   * only allows on size.
   *
   * @param size Size of icon requested, if size is 0 return largest icon.
   */
    public InputStream getIcon(int size) throws IOException {
        String url = getIconURL(size);
        if (url != null) {
            if (sourceURL != null) {
                return new URL(new URL(sourceURL, "/"), url).openStream();
            } else {
                return new URL(url).openStream();
            }
        }
        return null;
    }

    /**
   * Get maximum number of instances. Services return 1, factories
   * &gt; 1.
   */
    public int getMaxInstances() {
        return maxInstances;
    }

    /**
   * Set URL to icon
   */
    public void setIconURL(String url) {
        icons.put(new Integer(Integer.MAX_VALUE), url);
    }

    public void addIcon(int size, String url) {
        icons.put(new Integer(size), url);
    }

    public String getID() {
        return id;
    }

    public String getName() {
        if (localized_name != null) {
            return localized_name;
        } else {
            return name;
        }
    }

    void localize(Dictionary dict) {
        if (dict != null) {
            localized_name = localizeName(name, dict);
            localized_desc = localizeName(desc, dict);
            localized_icons = (Hashtable) icons.clone();
            for (Iterator it = localized_icons.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry e = (Map.Entry) it.next();
                e.setValue(localizeName((String) e.getValue(), dict));
            }
            localized_reqAttrs = new ArrayList();
            for (Iterator it = reqAttrs.iterator(); it.hasNext(); ) {
                AD attr = (AD) it.next();
                localized_reqAttrs.add(attr.localize(dict));
            }
            localized_optAttrs = new ArrayList();
            for (Iterator it = optAttrs.iterator(); it.hasNext(); ) {
                AD attr = (AD) it.next();
                localized_optAttrs.add(attr.localize(dict));
            }
        }
    }

    String localizeName(String name, Dictionary dict) {
        if (name.startsWith("%")) {
            String sub;
            if ((sub = (String) dict.get(name.substring(1))) != null) {
                return sub;
            } else {
                return name;
            }
        }
        return name;
    }
}
