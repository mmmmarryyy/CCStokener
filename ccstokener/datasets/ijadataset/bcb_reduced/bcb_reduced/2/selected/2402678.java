package uk.ac.shef.wit.saxon.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionRuneExecution;
import uk.ac.shef.wit.runes.runestone.Runestone;
import uk.ac.shef.wit.saxon.Feature;
import uk.ac.shef.wit.saxon.FeatureImpl;
import uk.ac.shef.wit.saxon.NodeAbstract;
import uk.ac.shef.wit.saxon.NodeRegExpImpl;
import uk.ac.shef.wit.saxon.NodeStringImpl;
import uk.ac.shef.wit.saxon.Rule;
import uk.ac.shef.wit.saxon.RuleSimpleImpl;
import uk.ac.shef.wit.saxon.Transition;
import uk.ac.shef.wit.saxon.TransitionImpl;
import uk.ac.shef.wit.saxon.TransitionOptionalImpl;
import uk.ac.shef.wit.saxon.TransitionOrImpl;
import uk.ac.shef.wit.saxon.TransitionRepeatImpl;
import uk.ac.shef.wit.saxon.TransitionSequenceImpl;
import uk.ac.shef.wit.saxon.compiler.RuleCompiler;

/**
 * A hand built parser which is very fussy about the file format. Should be
 * replaced with a more flexiable parser based around JavaCC or some other
 * grammar formalism. 
 * @author Mark A. Greenwood
 * @version $Id: FussyParser.java 537 2008-11-03 11:11:29Z greenwoodma $
 */
public class FussyParser implements RuleParser {

    private RuleCompiler compiler = null;

    /**
	 * Createa a new parser instance ready to process a Saxon rule file
	 * @throws RunesExceptionRuneExecution 
	 */
    public FussyParser() throws RunesExceptionRuneExecution {
        compiler = RuleCompiler.getInstance();
    }

    public List<Rule> parse(Runestone stone, URL url) throws IOException {
        List<Rule> rules = new ArrayList<Rule>();
        Map<String, String> macros = new HashMap<String, String>();
        BufferedReader bin = null;
        try {
            bin = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = bin.readLine();
            while (line != null) {
                line = line.trim();
                if (line.startsWith("Macro:")) {
                    String name = line.substring(6).trim();
                    String value = bin.readLine();
                    if (value != null) {
                        value = value.trim();
                        for (Map.Entry<String, String> macro : macros.entrySet()) {
                            value = value.replace("(" + macro.getKey() + ")", macro.getValue());
                        }
                        if (macros.containsKey(name)) System.err.println("WARNING: macro '" + name + "' is being redefined");
                        macros.put(name, value);
                    }
                }
                if (line.startsWith("Rule:")) {
                    String name = line.substring(5).trim();
                    line = bin.readLine();
                    Map<String, Boolean> options = new HashMap<String, Boolean>();
                    String[] requires = new String[0];
                    String[] provides = new String[0];
                    if (line != null && line.startsWith("Options:")) {
                        String data[] = line.substring(8).trim().split(",");
                        for (String opt : data) {
                            String[] kvp = opt.split("=");
                            options.put(kvp[0].trim(), Boolean.valueOf(kvp[1].trim()));
                        }
                        line = bin.readLine();
                    }
                    if (line != null && line.startsWith("Requires:")) {
                        requires = line.substring(9).trim().split("\\s+");
                        line = bin.readLine();
                    }
                    if (line != null && line.startsWith("Provides:")) {
                        provides = line.substring(9).trim().split("\\s+");
                        line = bin.readLine();
                    }
                    if (line == null) {
                        throw new RuntimeException("We've found the end of the file in the middle of a rule, something is really wrong");
                    }
                    String lhs = "";
                    while (line != null && (!line.trim().equals("=>") && !line.startsWith("Rule:"))) {
                        lhs += line.trim();
                        line = bin.readLine();
                    }
                    for (Map.Entry<String, String> macro : macros.entrySet()) {
                        lhs = lhs.replace("(" + macro.getKey() + ")", macro.getValue());
                    }
                    Transition t = null;
                    if (lhs.length() > 0) t = parse(stone, lhs);
                    String rhs = "";
                    if (line != null && !line.startsWith("Rule:")) line = bin.readLine();
                    while (line != null && !line.trim().startsWith("Rule:")) {
                        rhs += line + "\n";
                        line = bin.readLine();
                    }
                    rhs = rhs.trim();
                    Rule rule = null;
                    if (rhs.equals("")) {
                        rule = new RuleSimpleImpl(name, t, options, name);
                    } else if (rhs.startsWith("[")) {
                        String type = rhs.substring(1, rhs.length() - 1).trim();
                        if (type.equals("")) type = name;
                        rule = new RuleSimpleImpl(name, t, options, type);
                    } else if (rhs.startsWith("{")) {
                        rhs = rhs.substring(1, rhs.length() - 1);
                        try {
                            rule = compiler.compile(name, options, t, rhs);
                        } catch (Throwable e) {
                            System.out.println("\nSomething wicked this way comes...");
                            e.printStackTrace();
                            e.getCause().printStackTrace();
                        }
                    } else {
                        System.err.println("Unsupported type of RHS for rule '" + name + "', rule will not be compiled");
                    }
                    if (rule != null) {
                        rule.addRequired(requires);
                        rule.addProvided(provides);
                        rules.add(rule);
                    } else {
                        System.err.println("No idea what happend but the rule instance is null!");
                    }
                } else if (line.startsWith("Import:")) {
                    rules.addAll(parse(stone, new URL(url, line.substring(7).trim())));
                }
                if (line == null || !line.trim().startsWith("Rule:")) line = bin.readLine();
            }
        } finally {
            if (bin != null) {
                try {
                    bin.close();
                } catch (IOException e) {
                }
            }
        }
        return rules;
    }

    /**
	 * Convert a textual feature list into a Set of Feature instances.
	 * @param line a textual description of the feature set
	 * @return the feature set described by the input parameter
	 */
    private static Set<Feature> toFeatureSet(Runestone stone, String line) {
        Set<Feature> found = new HashSet<Feature>();
        int start = 0;
        int level = 0;
        List<String> f = new ArrayList<String>();
        for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == '{') ++level;
            if (c == '}') --level;
            if (c == ',' && level == 0) {
                f.add(line.substring(start, i));
                start = i + 1;
            }
        }
        if (start > 0) {
            f.add(line.substring(start, line.length()));
            for (String data : f) found.addAll(toFeatureSet(stone, data));
            return found;
        }
        if (line.indexOf("{") == -1) {
            found.add(new FeatureImpl(line, null));
            return found;
        }
        String edge = line.substring(0, line.indexOf("{"));
        String features = line.substring(line.indexOf("{") + 1, line.lastIndexOf("}"));
        NodeAbstract<?> n = null;
        boolean invert = false;
        if (features.startsWith("!")) {
            invert = true;
            features = features.substring(1);
        }
        if (features.startsWith("=")) {
            String[] struc = stone.getTypeStructure(edge);
            String type = null;
            if (struc == null) {
                System.err.println("WARNING: Attempting to guess structural information for " + edge);
                type = edge.split("_has_|_from_")[1];
                if (edge.startsWith("$_")) type = "$_" + type;
            } else {
                type = struc[struc.length - 1];
            }
            n = new NodeStringImpl(type, features.substring(1), invert);
        } else if (features.startsWith("~")) {
            String[] struc = stone.getTypeStructure(edge);
            String type = null;
            if (struc == null) {
                System.err.println("WARNING: Attempting to guess structural information for " + edge);
                type = edge.split("_has_|_from_")[1];
                if (edge.startsWith("$_")) type = "$_" + type;
            } else {
                type = struc[struc.length - 1];
            }
            n = new NodeRegExpImpl(type, features.substring(1), invert);
        } else {
            n = new NodeStringImpl(null, null);
            n.addFeatures(toFeatureSet(stone, features));
        }
        found.add(new FeatureImpl(edge, n));
        return found;
    }

    /**
	 * Parse a textual description of a transition into a Transition instance.
	 * @param line a description of a transition
	 * @return a Transition instance matching the textual input
	 */
    public Transition parse(Runestone stone, String line) {
        Transition t = null;
        int s = 0;
        int e = line.length();
        if (line.startsWith("(")) {
            s = line.indexOf("(") + 1;
            e = line.lastIndexOf(")");
        }
        String content = line.substring(s, e);
        if (line.indexOf(")") != line.lastIndexOf(")")) {
            List<String> temp = new ArrayList<String>();
            Map<String, Object> config = new HashMap<String, Object>();
            config.put("hidden", content.startsWith("?"));
            if (content.startsWith("?")) content = content.substring(1);
            int level = 0;
            int start = 0;
            for (int i = 0; i < content.length(); ++i) {
                char c = content.charAt(i);
                if (c == '(') ++level;
                if (c == ')') --level;
                if (c == '|' && level == 0) {
                    temp.add(content.substring(start, i));
                    start = i + 1;
                }
            }
            if (temp.size() > 0) {
                temp.add(content.substring(start, content.length()));
                List<Transition> ts = new ArrayList<Transition>();
                for (String segment : temp) ts.add(parse(stone, segment));
                t = new TransitionOrImpl(ts.toArray(new Transition[ts.size()]), config);
            } else {
                int feature = 0;
                level = 1;
                start = 0;
                for (int i = 1; i < content.length(); ++i) {
                    char c = content.charAt(i);
                    if (c == '(') ++level;
                    if (c == ')') --level;
                    if (c == '{') ++feature;
                    if (c == '}') --feature;
                    if (c == '(' && level == 1) {
                        temp.add(content.substring(start, i));
                        start = i;
                    }
                }
                temp.add(content.substring(start, content.length()));
                Transition[] ts = new Transition[temp.size()];
                for (int i = 0; i < temp.size(); ++i) ts[i] = parse(stone, temp.get(i));
                t = new TransitionSequenceImpl(ts, config);
            }
        } else {
            String[] parts = content.split("\\{", 2);
            String edge = parts[0];
            Map<String, Object> config = new HashMap<String, Object>();
            config.put("hidden", edge.startsWith("?"));
            if (edge.startsWith("?")) edge = edge.substring(1);
            NodeAbstract<?> n = null;
            if (parts.length == 2) {
                String data = parts[1].substring(0, parts[1].length() - 1);
                boolean invert = false;
                if (data.startsWith("!")) {
                    invert = true;
                    data = data.substring(1);
                }
                if (data.startsWith("=")) {
                    n = new NodeStringImpl(null, data, invert);
                } else if (data.startsWith("~")) {
                    n = new NodeRegExpImpl(null, data, invert);
                } else {
                    n = new NodeStringImpl(null, null);
                    n.addFeatures(toFeatureSet(stone, data));
                }
            }
            t = new TransitionImpl(edge, n, config);
        }
        if (e + 1 < line.length()) {
            char modifier = line.charAt(e + 1);
            switch(modifier) {
                case '?':
                    t = new TransitionOptionalImpl(t);
                    break;
                case '*':
                    t = new TransitionRepeatImpl(t, true);
                    break;
                case '+':
                    t = new TransitionRepeatImpl(t, false);
                    break;
                case ':':
                    StringBuilder name = new StringBuilder();
                    int i = e + 2;
                    while (i < line.length() && Character.isLetterOrDigit(line.charAt(i))) {
                        name.append(line.charAt(i));
                        ++i;
                    }
                    Set<String> names = new HashSet<String>();
                    names.add(name.toString());
                    t.setConfig("named", names);
                    break;
            }
        }
        return t;
    }
}
