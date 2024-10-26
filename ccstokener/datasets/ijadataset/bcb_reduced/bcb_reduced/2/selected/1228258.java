package org.gbif.checklistbank.lookup;

import org.gbif.ecat.cfg.RsGbifOrg;
import org.gbif.ecat.voc.Kingdom;
import org.gbif.ecat.voc.Rank;
import org.gbif.utils.file.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author markus
 */
public class HigherTaxaLookup {

    private Logger log = LoggerFactory.getLogger(HigherTaxaLookup.class);

    private static final Pattern NORM_WHITE = Pattern.compile("\\s+");

    private static final Set<String> NON_NAMES = new HashSet<String>();

    private static final Pattern NORM_WHITESPACE = Pattern.compile("[/\\s__-]+");

    private Map<Rank, Map<String, String>> syn = new HashMap<Rank, Map<String, String>>();

    private Map<String, Kingdom> kingdoms = new HashMap<String, Kingdom>();

    /**
   *
   */
    public HigherTaxaLookup() {
        for (Kingdom k : Kingdom.values()) {
            this.kingdoms.put(norm(k.name()), k);
            this.kingdoms.put(norm(String.valueOf(k.abbrev)), k);
        }
    }

    /**
   * Lookup higher taxa synonym dictionary across all ranks and return the first match found
   *
   * @param higherTaxon
   * @return the looked up accepted name or the original higherTaxon
   */
    public String lookup(String higherTaxon) {
        if (higherTaxon == null) {
            return null;
        }
        for (Rank r : syn.keySet()) {
            String result = lookup(higherTaxon, r);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
   * Lookup synonym for given higher rank.
   * Can be null.
   *
   * @param higherTaxon higher rank name, case insensitive
   * @param rank the rank to lookup for
   * @return the looked up accepted name, null for blacklisted names or the original higherTaxon if no synonym is known
   */
    public String lookup(String higherTaxon, Rank rank) {
        if (higherTaxon == null) {
            return null;
        }
        if (isBlacklisted(higherTaxon)) {
            return null;
        }
        if (syn.containsKey(rank)) {
            String normedHT = norm(higherTaxon);
            Map<String, String> x = syn.get(rank);
            if (syn.get(rank).containsKey(normedHT)) {
                return syn.get(rank).get(normedHT);
            }
        }
        return higherTaxon;
    }

    /**
   * Check for obvious, blacklisted garbage and return true if thats the case.
   * The underlying set is hosted at http://rs.gbif.org/dictionaries/authority/blacklisted.txt
   */
    public boolean isBlacklisted(String name) {
        if (name != null) {
            name = norm(name);
            if (NON_NAMES.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /**
   * @param x
   * @return
   */
    private String norm(String x) {
        x = StringUtils.trimToNull(x);
        if (x != null) {
            x = NORM_WHITE.matcher(x).replaceAll(" ");
            x = x.toUpperCase();
        }
        return x;
    }

    private Map<String, String> readSynonymFile(String file) {
        Map<String, String> synonyms = new HashMap<String, String>();
        InputStream in = null;
        try {
            URL url = RsGbifOrg.synonymUrl(file);
            log.debug("Reading " + url.toString());
            in = url.openStream();
            synonyms = FileUtils.streamToMap(in, 0, 1, true);
        } catch (IOException e) {
            log.warn("Cannot read synonym map from " + file + ". Use empty map instead.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        log.debug("loaded " + synonyms.size() + " synonyms from file " + file);
        return synonyms;
    }

    protected void readBlacklistFile() {
        NON_NAMES.clear();
        InputStream in = null;
        try {
            URL url = RsGbifOrg.authorityUrl(RsGbifOrg.FILENAME_BLACKLIST);
            log.debug("Reading " + url.toString());
            in = url.openStream();
            NON_NAMES.addAll(FileUtils.streamToSet(in));
        } catch (IOException e) {
            log.warn("Cannot read blacklist. Use empty set instead.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        log.debug("loaded " + NON_NAMES.size() + " blacklisted names from rs.gbif.org");
    }

    /**
   * Reloads all synonym files found on rs.gbif.org replacing existing mappings.
   */
    public void reloadOnlineDicts() {
        log.info("Reloading dictionary files from rs.gbif.org ...");
        Map<Rank, String> files = new HashMap<Rank, String>();
        files.put(Rank.KINGDOM, "kingdom.txt");
        files.put(Rank.PHYLUM, "phylum.txt");
        files.put(Rank.CLASS, "class.txt");
        files.put(Rank.ORDER, "order.txt");
        files.put(Rank.FAMILY, "family.txt");
        for (Rank rank : files.keySet()) {
            Map<String, String> synonyms = readSynonymFile(files.get(rank));
            setSynonyms(rank, synonyms);
        }
        readBlacklistFile();
    }

    /**
   * Sets the synonym lookup map for a given rank.
   * Names will be normalised and checked for existance of the same entry as key or value.
   *
   * @param rank
   * @param synonyms
   */
    public void setSynonyms(Rank rank, Map<String, String> synonyms) {
        Map<String, String> synonymsNormed = new HashMap<String, String>();
        for (Entry<String, String> entry : synonyms.entrySet()) {
            synonymsNormed.put(norm(entry.getKey()), entry.getValue());
        }
        Collection<String> syns = new HashSet<String>(synonymsNormed.keySet());
        for (String syn : syns) {
            if (synonymsNormed.containsKey(synonymsNormed.get(syn))) {
                log.warn(syn + " is both synonym and accepted - ignore synonym.");
                synonymsNormed.remove(syn);
            }
        }
        syn.put(rank, synonymsNormed);
        log.debug("Loaded " + synonyms.size() + " " + rank.name() + " synonyms ");
        if (Rank.KINGDOM == rank) {
            Map<String, String> map = syn.get(Rank.KINGDOM);
            if (map != null) {
                for (String syn : map.keySet()) {
                    Kingdom k = null;
                    String key = map.get(syn);
                    if (key != null) {
                        key = key.toLowerCase();
                        key = StringUtils.capitalize(key);
                        try {
                            k = Kingdom.valueOf(key);
                        } catch (Exception e) {
                        }
                    }
                    this.kingdoms.put(norm(syn), k);
                }
            }
            for (Kingdom k : Kingdom.values()) {
                this.kingdoms.put(norm(k.name()), k);
                this.kingdoms.put(norm(String.valueOf(k.abbrev)), k);
            }
        }
    }

    /**
   * @return the number of entries across all ranks
   */
    public int size() {
        int all = 0;
        for (Rank r : syn.keySet()) {
            all += size(r);
        }
        return all;
    }

    /**
   * @return the number of entries for a given rank
   */
    public int size(Rank rank) {
        if (syn.containsKey(rank)) {
            return syn.get(rank).size();
        }
        return 0;
    }

    public Kingdom toKingdom(String kingdom) {
        if (kingdom == null) {
            return null;
        }
        return kingdoms.get(kingdom.trim().toUpperCase());
    }

    public Kingdom toKingdomSynonymOnly(String kingdom) {
        if (kingdom == null) {
            return null;
        }
        Kingdom k = kingdoms.get(kingdom.trim().toUpperCase());
        if (k != null && k.name().equalsIgnoreCase(kingdom)) {
            return null;
        }
        return k;
    }
}
