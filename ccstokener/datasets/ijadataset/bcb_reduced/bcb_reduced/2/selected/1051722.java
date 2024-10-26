package javax.time.calendar.zone;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.time.CalendricalException;

/**
 * Loads time-zone rules stored in a file accessed via class loader.
 * <p>
 * This class is immutable and thread-safe.
 *
 * @author Stephen Colebourne
 */
final class ResourceZoneRulesDataProvider implements ZoneRulesDataProvider {

    /**
     * The time-zone group ID.
     */
    private final String groupID;

    /**
     * All the versions in the provider.
     */
    private final Set<ZoneRulesVersion> versions;

    /**
     * All the regions in the provider.
     */
    private final Set<String> regions;

    /**
     * The rules.
     */
    private final AtomicReferenceArray<Object> rules;

    /**
     * Loads any time-zone rules data stored in files.
     *
     * @throws RuntimeException if the time-zone rules cannot be loaded
     */
    static void load() {
        for (ResourceZoneRulesDataProvider provider : loadResources()) {
            ZoneRulesGroup.registerProvider(provider);
        }
    }

    /**
     * Loads the rules from files in the class loader, often jar files.
     *
     * @return the list of loaded rules, not null
     * @throws Exception if an error occurs
     */
    private static List<ResourceZoneRulesDataProvider> loadResources() {
        List<ResourceZoneRulesDataProvider> providers = new ArrayList<ResourceZoneRulesDataProvider>();
        URL url = null;
        try {
            Enumeration<URL> en = Thread.currentThread().getContextClassLoader().getResources("javax/time/calendar/zone/ZoneRules.dat");
            Set<String> loaded = new HashSet<String>();
            while (en.hasMoreElements()) {
                url = en.nextElement();
                if (loaded.add(url.toExternalForm())) {
                    providers.add(new ResourceZoneRulesDataProvider(url));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load time-zone rule data: " + url, ex);
        }
        return providers;
    }

    /**
     * Loads the rules from a URL, often in a jar file.
     *
     * @param providers  the list to add to, not null
     * @param url  the jar file to load, not null 
     * @throws Exception if an error occurs
     */
    private ResourceZoneRulesDataProvider(URL url) throws ClassNotFoundException, IOException {
        boolean throwing = false;
        InputStream in = null;
        try {
            in = url.openStream();
            DataInputStream dis = new DataInputStream(in);
            if (dis.readByte() != 1) {
                throw new StreamCorruptedException("File format not recognised");
            }
            this.groupID = dis.readUTF();
            int versionCount = dis.readShort();
            String[] versionArray = new String[versionCount];
            for (int i = 0; i < versionCount; i++) {
                versionArray[i] = dis.readUTF();
            }
            int regionCount = dis.readShort();
            String[] regionArray = new String[regionCount];
            for (int i = 0; i < regionCount; i++) {
                regionArray[i] = dis.readUTF();
            }
            this.regions = new HashSet<String>(Arrays.asList(regionArray));
            Set<ZoneRulesVersion> versionSet = new HashSet<ZoneRulesVersion>(versionCount);
            for (int i = 0; i < versionCount; i++) {
                int versionRegionCount = dis.readShort();
                String[] versionRegionArray = new String[versionRegionCount];
                short[] versionRulesArray = new short[versionRegionCount];
                for (int j = 0; j < versionRegionCount; j++) {
                    versionRegionArray[j] = regionArray[dis.readShort()];
                    versionRulesArray[j] = dis.readShort();
                }
                versionSet.add(new ResourceZoneRulesVersion(this, versionArray[i], versionRegionArray, versionRulesArray));
            }
            this.versions = versionSet;
            int ruleCount = dis.readShort();
            this.rules = new AtomicReferenceArray<Object>(ruleCount);
            for (int i = 0; i < ruleCount; i++) {
                byte[] bytes = new byte[dis.readShort()];
                dis.readFully(bytes);
                rules.set(i, bytes);
            }
        } catch (IOException ex) {
            throwing = true;
            throw ex;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    if (throwing == false) {
                        throw ex;
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    public String getGroupID() {
        return groupID;
    }

    /** {@inheritDoc} */
    public Set<ZoneRulesVersion> getVersions() {
        return versions;
    }

    /** {@inheritDoc} */
    public Set<String> getRegionIDs() {
        return regions;
    }

    /**
     * Loads the rule.
     * 
     * @param index  the index to retrieve
     * @return the rules, should not be null
     */
    ZoneRules loadRule(short index) throws Exception {
        Object obj = rules.get(index);
        if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
            obj = Ser.read(dis);
            rules.set(index, obj);
        }
        return (ZoneRules) obj;
    }

    @Override
    public String toString() {
        return groupID + ":#" + versions;
    }

    /**
     * Version of the rules.
     * <p>
     * ResourceZoneRulesVersion is thread-safe and immutable.
     */
    static class ResourceZoneRulesVersion implements ZoneRulesVersion {

        /** Provider. */
        private final ResourceZoneRulesDataProvider provider;

        /** Version ID. */
        private final String versionID;

        /** Region IDs. */
        private final String[] regionArray;

        /** Region IDs. */
        private final short[] ruleIndices;

        /** Constructor. */
        ResourceZoneRulesVersion(ResourceZoneRulesDataProvider provider, String versionID, String[] regions, short[] ruleIndices) {
            this.provider = provider;
            this.versionID = versionID;
            this.regionArray = regions;
            this.ruleIndices = ruleIndices;
        }

        public String getVersionID() {
            return versionID;
        }

        public boolean isRegionID(String regionID) {
            return Arrays.binarySearch(regionArray, regionID) >= 0;
        }

        public Set<String> getRegionIDs() {
            return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(regionArray)));
        }

        public ZoneRules getZoneRules(String regionID) {
            int index = Arrays.binarySearch(regionArray, regionID);
            if (index < 0) {
                return null;
            }
            try {
                return provider.loadRule(ruleIndices[index]);
            } catch (Exception ex) {
                throw new CalendricalException("Unable to load rules: " + provider.groupID + ':' + regionID + '#' + versionID, ex);
            }
        }

        @Override
        public String toString() {
            return versionID;
        }
    }
}
