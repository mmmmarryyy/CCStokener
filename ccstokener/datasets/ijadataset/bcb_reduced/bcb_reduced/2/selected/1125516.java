package uk.ac.shef.wit.saxon.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import uk.ac.shef.wit.commons.UtilCollections;
import uk.ac.shef.wit.runes.Runes;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionCannotHandle;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionNoSuchContent;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionNoSuchStructure;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionRuneExecution;
import uk.ac.shef.wit.runes.rune.Rune;
import uk.ac.shef.wit.runes.runestone.Content;
import uk.ac.shef.wit.runes.runestone.Runestone;
import uk.ac.shef.wit.runes.runestone.Structure;
import uk.ac.shef.wit.runes.runestone.StructureAndContent;
import uk.ac.shef.wit.saxon.Node;
import uk.ac.shef.wit.saxon.NodeAbstract;
import uk.ac.shef.wit.saxon.NodeRegExpImpl;
import uk.ac.shef.wit.saxon.NodeStringImpl;
import uk.ac.shef.wit.saxon.Path;
import uk.ac.shef.wit.saxon.Rule;
import uk.ac.shef.wit.saxon.SteppingStone;
import uk.ac.shef.wit.saxon.Transition;
import uk.ac.shef.wit.saxon.TransitionImpl;
import uk.ac.shef.wit.saxon.TransitionOptionalImpl;
import uk.ac.shef.wit.saxon.TransitionOrImpl;
import uk.ac.shef.wit.saxon.TransitionSequenceImpl;
import uk.ac.shef.wit.text.tokeniser.Token;
import uk.ac.shef.wit.text.tokeniser.Tokeniser;

/**
 * This Gazetteer is a re-worked version of Saxon that works from the basis that there will be many more rules than tokens.
 * By default Saxon goes through each rule to find nodes in the graph that match the start conditions. This becomes expensive
 * when there are many rules are each node in the graph maybe checked for each rule. This class works in the opposite
 * direction and checks each Token in the graph to see if it matches the start node of a rule or rules.
 * @author Mark A. Greenwood
 * @version $Id: Gazetteer.java 548 2008-11-21 14:32:46Z greenwoodma $
 */
public class Gazetteer implements Rune {

    /**
	 * The name of this application
	 */
    private static final String PRODUCT = "Gazetteer";

    /**
	 * The version number
	 */
    private static final String VERSION = "0.1";

    /**
	 * The rules in the order in which they were added to this Saxon instance
	 */
    private Map<Node, List<GazRule>> rules = null;

    /**
	 * A constant representing the fact that at this point we have reached the end
	 * of a gazetteer entry. This is needed to we know when to make the remaining
	 * transitions optional.
	 */
    private static final String STOP = "~sToP~";

    /**
	 * The label of the edge type that links the Token node and the 
	 * content node holding it's string.
	 */
    private static final String TOKEN_HAS_STRING = "$_token_has_string";

    /**
	 * The label of the content type that holds the actual string of each token.
	 */
    private static final String TOKEN_STRING = "$_string";

    /**
	 * The label of the edge type that links the current Token node
	 * to the next Token node in the document.
	 */
    private static final String TOKEN_NEXT = "next_$_token";

    private static final String FIRST_TOKEN = "$_has_first_token";

    /**
	 * The label of the Token type.
	 */
    private static final String TOKEN_TYPE = "$_token";

    /**
	 * URL of the gazetteer file which describes what we want to do
	 * with the representation we are passed in the input method
	 */
    private URL _url = null;

    /**
	 * Returns a textual description of this object
	 */
    @Override
    public String toString() {
        return PRODUCT + " v" + VERSION;
    }

    static {
        Runes.registerRune(Gazetteer.class, PRODUCT + " v" + VERSION);
    }

    /**
	 * Creates a new Gazetteer instance from the specified URL.
	 * @param url the top-level gazetteer file
	 */
    public Gazetteer(URL url) {
        this._url = url;
    }

    public Gazetteer(URL url, boolean caseInsensitive) {
        this._url = url;
        this._case_insensitive_gazetteer = caseInsensitive;
    }

    /**
	 * Parses and loads the gazetter files.
	 * @throws RunesExceptionRuneExecution 
	 * @throws RunesExceptionNoSuchContent 
	 * @throws RunestoneException if an error occurs accessing Runestone
	 */
    private void load(Runestone stone) throws RunesExceptionRuneExecution, RunesExceptionNoSuchContent {
        final Tokeniser tokeniser = stone.<Tokeniser>getContent("tokeniser").iterator().next();
        rules = new HashMap<Node, List<GazRule>>();
        System.out.println("Loading Gaz from: " + _url);
        if (_url == null) return;
        BufferedReader typesIn = null, entryIn = null;
        try {
            typesIn = new BufferedReader(new InputStreamReader(_url.openStream()));
            String tData = typesIn.readLine();
            while (tData != null) {
                Map<String, Map> gaz = new HashMap<String, Map>();
                String[] data = tData.split(":");
                URL listURL = new URL(_url, data[0]);
                System.err.println("Loading from " + listURL);
                entryIn = new BufferedReader(new InputStreamReader(listURL.openStream()));
                String entry = entryIn.readLine();
                while (entry != null) {
                    entry = entry.trim();
                    if (!entry.equals("")) {
                        final List<Token> tokens;
                        try {
                            tokens = tokeniser.tokenise(entry);
                        } catch (IOException e) {
                            throw new RunesExceptionRuneExecution(e, this);
                        }
                        Map<String, Map> m = gaz;
                        for (Token t : tokens) {
                            String token = t.getString();
                            if (_case_insensitive_gazetteer) token = token.toLowerCase();
                            @SuppressWarnings("unchecked") Map<String, Map> next = m.get(token);
                            if (next == null) next = new HashMap<String, Map>();
                            m.put(token, next);
                            m = next;
                        }
                        m.put(STOP, null);
                    }
                    entry = entryIn.readLine();
                }
                for (Map.Entry<String, Map> er : gaz.entrySet()) {
                    NodeAbstract start = new NodeStringImpl(TOKEN_TYPE, null);
                    if (_case_insensitive_gazetteer) {
                        start.addFeature(TOKEN_HAS_STRING, new NodeRegExpImpl(TOKEN_STRING, "(?i:" + er.getKey().toLowerCase() + ")"));
                    } else {
                        start.addFeature(TOKEN_HAS_STRING, new NodeStringImpl(TOKEN_STRING, er.getKey()));
                    }
                    @SuppressWarnings("unchecked") Transition transition = mapToTransition(er.getValue());
                    String major = data[1];
                    String minor = (data.length == 3 ? data[2] : null);
                    GazRule gr = new GazRule(major, minor, transition);
                    List<GazRule> rl = rules.get(start);
                    if (rl == null) rl = new ArrayList<GazRule>();
                    rl.add(gr);
                    rules.put(start, rl);
                }
                entryIn.close();
                System.err.println(rules.size());
                tData = typesIn.readLine();
            }
        } catch (IOException e) {
            throw new RunesExceptionRuneExecution(e, this);
        } finally {
            try {
                if (typesIn != null) typesIn.close();
            } catch (IOException e) {
            }
            try {
                if (entryIn != null) entryIn.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * Applies this gazetteer instance to the supplied Runestone graph.
	 * @param stone provides access to the underlying data
	 * @throws RunesExceptionNoSuchStructure 
	 * @throws RunesExceptionNoSuchContent 
	 * @throws RunesExceptionCannotHandle 
	 * @throws RunesExceptionRuneExecution 
	 */
    public void carve(Runestone stone) throws RunesExceptionNoSuchStructure, RunesExceptionNoSuchContent, RunesExceptionCannotHandle, RunesExceptionRuneExecution {
        updateOptionalParameters(stone);
        if (rules == null || _reload_gazetteer) load(stone);
        System.out.println("Gazetteer contains " + rules.size() + " rules");
        final Content<String> string = stone.getContent(TOKEN_STRING);
        final Structure tokenHasString = stone.getStructure(TOKEN_HAS_STRING);
        final Structure next = stone.getStructure(TOKEN_NEXT);
        for (final int[] id : stone.getStructure("$_has_first_token")) {
            int tokenId = id[1];
            do {
                NodeAbstract n = new NodeStringImpl(TOKEN_TYPE, null);
                if (_case_insensitive_gazetteer) {
                    n.addFeature(TOKEN_HAS_STRING, new NodeRegExpImpl(TOKEN_STRING, "(?i:" + string.retrieve(tokenHasString.follow(tokenId)).toLowerCase() + ")"));
                } else {
                    n.addFeature(TOKEN_HAS_STRING, new NodeStringImpl(TOKEN_STRING, string.retrieve(tokenHasString.follow(tokenId))));
                }
                List<GazRule> matched = rules.get(n);
                if (matched == null) continue;
                for (GazRule r : matched) {
                    if (r.getTransition() == null) {
                        Set<Path> paths = new HashSet<Path>();
                        Path path = new Path(new SteppingStone[] { new SteppingStone(tokenId, TOKEN_TYPE, null) }, null);
                        paths.add(path);
                        r.annotate(stone, paths);
                    } else {
                        r.apply(stone, TOKEN_TYPE, tokenId);
                    }
                }
            } while (0 != (tokenId = next.follow(tokenId)));
        }
    }

    /**
	 * Tranforms a sequence of maps into a single Saxon transition
	 * @param lookup the map to transform
	 * @return a Transition instance representing the information in the map
	 */
    private Transition mapToTransition(Map<String, Map> lookup) {
        Set<Transition> found = new HashSet<Transition>();
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("hidden", false);
        for (Map.Entry<String, Map> entry : lookup.entrySet()) {
            if (entry.getKey().equals(STOP)) continue;
            NodeAbstract t = new NodeStringImpl(null, null);
            if (_case_insensitive_gazetteer) {
                t.addFeature(TOKEN_HAS_STRING, new NodeRegExpImpl(TOKEN_STRING, "(?i:" + entry.getKey().toLowerCase() + ")"));
            } else {
                t.addFeature(TOKEN_HAS_STRING, new NodeStringImpl(TOKEN_STRING, entry.getKey()));
            }
            Transition t1 = new TransitionImpl(TOKEN_NEXT, t, new HashMap<String, Object>());
            @SuppressWarnings("unchecked") Transition t2 = mapToTransition(entry.getValue());
            if (t2 != null) t1 = new TransitionSequenceImpl(new Transition[] { t1, t2 }, options);
            found.add(t1);
        }
        if (found.size() == 0) return null;
        Transition complete = null;
        if (found.size() == 1) {
            complete = found.iterator().next();
        } else {
            complete = new TransitionOrImpl(found.toArray(new Transition[found.size()]), options);
        }
        if (lookup.containsKey(STOP)) complete = new TransitionOptionalImpl(complete);
        return complete;
    }

    /**
	 * This class represents a set of gazetteer entries as a Saxon rule
	 * @author Mark A. Greenwood
	 */
    static class GazRule extends Rule {

        /**
		 * The major type that this gazetteer rule represents
		 */
        private String major = null;

        /**
		 * The minor type that this gazetteer rule represents
		 */
        private String minor = null;

        /**
		 * A static map that holds the options used to create the Saxon rule
		 */
        private static final Map<String, Boolean> options = new HashMap<String, Boolean>();

        static {
            options.put("allMatches", false);
        }

        /**
		 * Creates a new Saxon rule that represents one or more entries in a gazetteer list
		 * @param major the major type of the gazetteer lookup
		 * @param minor the minor type of the gazetteer lookup
		 * @param transition the transition representing the rest of the gazetteer entry or entries
		 * @param options a set of options describing how the rule behaves
		 */
        protected GazRule(String major, String minor, Transition transition) {
            super("Gaz_" + major + "_" + minor, transition, options);
            this.major = major;
            this.minor = minor;
        }

        @Override
        protected void annotate(Runestone stone, Set<Path> paths) throws RunesExceptionNoSuchStructure, RunesExceptionNoSuchContent, RunesExceptionCannotHandle {
            final StructureAndContent<String> strings = stone.getStructureAndContent(TOKEN_STRING);
            final Structure tokenHasString = stone.getStructure(TOKEN_HAS_STRING);
            final StructureAndContent<Integer> tokenStartOffsets = stone.getStructureAndContent("start_offset_in_$");
            final Structure tokenHasStart = stone.getStructure("token_has_start_offset_in_$");
            final StructureAndContent<Integer> tokenEndOffsets = stone.getStructureAndContent("end_offset_in_$");
            final Structure tokenHasEnd = stone.getStructure("token_has_end_offset_in_$");
            final Structure lookups = stone.getStructure("lookup", "lookup_has_rule_name", "lookup_has_major_type", "lookup_has_minor_type", "lookup_has_path", "lookup_has_start_offset", "lookup_has_end_offset", "lookup_has_string", "lookup_from_$");
            final StructureAndContent<String> ruleNames = stone.getStructureAndContent("rule_name");
            final StructureAndContent<String> majorTypes = stone.getStructureAndContent("major_type");
            final StructureAndContent<String> minorTypes = stone.getStructureAndContent("minor_type");
            final StructureAndContent<Path> vinyl = stone.getStructureAndContent("path");
            final StructureAndContent<Integer> startOffsets = stone.getStructureAndContent("start_offset");
            final StructureAndContent<Integer> endOffsets = stone.getStructureAndContent("end_offset");
            final Structure first_token = stone.getStructure("token_has_lookup");
            final Structure last_token = stone.getStructure("lookup_has_last_token");
            final Structure tokenFromDoc = stone.getStructure("token_from_$");
            for (Path path : paths) {
                StringBuilder content = new StringBuilder();
                for (SteppingStone code : path.getSteppingStone()) {
                    content.append(" ").append(strings.retrieve(tokenHasString.follow(code.id)));
                }
                final int string = strings.encode(content.toString().trim());
                final int rule_name = ruleNames.encode(getName());
                final int major_type = majorTypes.encode(major);
                final int minor_type = minorTypes.encode(minor);
                final int lp = vinyl.encode(path);
                final int firstTokenID = path.getNodes()[0];
                final int lastTokenID = path.getNodes()[path.getNodes().length - 1];
                final int docID = tokenFromDoc.follow(firstTokenID);
                final int startOffset = tokenStartOffsets.retrieve(tokenHasStart.follow(firstTokenID));
                final int endOffset = tokenEndOffsets.retrieve(tokenHasEnd.follow(lastTokenID));
                System.out.println("\t" + content.toString() + ": " + major + "/" + minor + " " + startOffset + "-->" + endOffset + " " + hashCode());
                final int startID = startOffsets.encode(startOffset);
                final int endID = endOffsets.encode(endOffset);
                final int lookup = lookups.encode(rule_name, major_type, minor_type, lp, startID, endID, string, docID);
                first_token.inscribe(firstTokenID, lookup);
                last_token.inscribe(lookup, lastTokenID);
                System.out.println("end of gaz rule");
            }
        }

        @Override
        public Set<String> analyseRequired(Runestone stone) {
            return null;
        }
    }

    /**
	 * Returns details of the data added to the underlying model by the Gazetteer
	 * @param stone provides access to the underlying model
	 * @return the set of data types added to the underlying model
	 * @throws RunesExceptionCannotHandle 
	 */
    public Set<String> analyseProvided(Runestone stone) {
        return UtilCollections.add(new HashSet<String>(), "lookup", "lookup_has_rule_name|lookup|rule_name", "lookup_has_major_type|lookup|major_type", "lookup_has_minor_type|lookup|minor_type", "lookup_has_path|lookup|path", "lookup_has_start_offset|lookup|start_offset", "start_offset", "end_offset", "lookup_has_end_offset|lookup|end_offset", "rule_name", "major_type", "minor_type", "path", "lookup_has_string|lookup|string", "string", "lookup_has_last_token|lookup|" + TOKEN_TYPE, "token_has_lookup|" + TOKEN_TYPE + "|lookup", "lookup_from_$|lookup|$", "reload_gazetteer", "case_insensitive_gazetteer", "gaz" + hashCode());
    }

    /**
	 * Returns details of the data that must be present for the Gazetteer to run.
	 * @param stone provides access to the underlying data
	 * @return the set of data types which must be present for this Saxon instance
	 *         to be applied to the underlying data
	 * @throws RunesExceptionCannotHandle
	 */
    public Set<String> analyseRequired(Runestone stone) {
        return UtilCollections.add(new HashSet<String>(), FIRST_TOKEN, TOKEN_NEXT, TOKEN_HAS_STRING, TOKEN_STRING, "token_has_start_offset_in_document", "token_has_end_offset_in_document", "start_offset_in_document", "end_offset_in_document", "reload_gazetteer", "case_insensitive_gazetteer", "gaz" + hashCode(), "tokeniser");
    }

    private boolean _reload_gazetteer = false;

    private boolean _case_insensitive_gazetteer = false;

    private void updateOptionalParameters(final Runestone stone) {
        try {
            _reload_gazetteer = stone.<Boolean>getContent("reload_gazetteer").iterator().next();
        } catch (RunesExceptionNoSuchContent ignore) {
        } catch (NoSuchElementException ignore) {
        }
    }
}
