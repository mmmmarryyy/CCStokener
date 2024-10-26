package gate.creole.numbers;

import static gate.GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME;
import static gate.creole.numbers.AnnotationConstants.HINT_FEATURE_NAME;
import static gate.creole.numbers.AnnotationConstants.NUMBER_ANNOTATION_NAME;
import static gate.creole.numbers.AnnotationConstants.TYPE_FEATURE_NAME;
import static gate.creole.numbers.AnnotationConstants.VALUE_FEATURE_NAME;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.LanguageAnalyser;
import gate.Resource;
import gate.Utils;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ExecutionInterruptedException;
import gate.creole.ResourceInstantiationException;
import gate.creole.Transducer;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.BomStrippingInputStreamReader;
import gate.util.InvalidOffsetException;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * A GATE PR which annotates numbers which appear as both words or
 * numbers (or a combination) and determines their numeric value. Whilst
 * useful on their own the annotations produced can also be used as a
 * preliminary step towards more complex annotations such as
 * measurements or monetary units.
 * 
 * @see <a
 *      href="http://gate.ac.uk/userguide/sec:misc-creole:numbers:numbers">The
 *      GATE User Guide</a>
 * @author Mark A. Greenwood
 * @author Thomas Heitz
 */
@CreoleResource(name = "Numbers Tagger", comment = "Finds numbers in (both words and digits) and annotates them with their numeric value", icon = "numbers.png", helpURL = "http://gate.ac.uk/userguide/sec:misc-creole:numbers:numbers")
public class NumbersTagger extends AbstractLanguageAnalyser {

    private static final long serialVersionUID = 8568794158677464398L;

    private transient Logger logger = Logger.getLogger(this.getClass().getName());

    private transient Config config;

    private URL configURL;

    private URL postProcessURL;

    private LanguageAnalyser jape;

    private String encoding;

    private String annotationSetName;

    private Boolean failOnMissingInputAnnotations = Boolean.TRUE;

    private Boolean allowWithinWords = Boolean.FALSE;

    private Boolean useHintsFromOriginalMarkups = Boolean.TRUE;

    private Pattern pattern;

    private Pattern subPattern;

    private Pattern numericPattern;

    @RunTime
    @Optional
    @CreoleParameter(comment = "Throw an exception when there are none of the required input annotations (Token and Sentence)", defaultValue = "true")
    public void setFailOnMissingInputAnnotations(Boolean fail) {
        failOnMissingInputAnnotations = fail;
    }

    public Boolean getFailOnMissingInputAnnotations() {
        return failOnMissingInputAnnotations;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Use annotations from the Original markups as hints for annotating numbers", defaultValue = "true")
    public void setUseHintsFromOriginalMarkups(Boolean useHints) {
        useHintsFromOriginalMarkups = useHints;
    }

    public Boolean getUseHintsFromOriginalMarkups() {
        return useHintsFromOriginalMarkups;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Allow numbers to appear within words", defaultValue = "false")
    public void setAllowWithinWords(Boolean allowWithinWords) {
        this.allowWithinWords = allowWithinWords;
    }

    public Boolean getAllowWithinWords() {
        return allowWithinWords;
    }

    public String getAnnotationSetName() {
        return annotationSetName;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "The name of annotation set used for the generated annotations")
    public void setAnnotationSetName(String outputAnnotationSetName) {
        this.annotationSetName = outputAnnotationSetName;
    }

    public URL getConfigURL() {
        return configURL;
    }

    @CreoleParameter(defaultValue = "resources/languages/all.xml", suffixes = ".xml")
    public void setConfigURL(URL url) {
        configURL = url;
    }

    public URL getPostProcessURL() {
        return postProcessURL;
    }

    @CreoleParameter(defaultValue = "resources/jape/post-process.jape", suffixes = ".jape")
    public void setPostProcessURL(URL url) {
        postProcessURL = url;
    }

    @CreoleParameter(defaultValue = "UTF-8")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    /**
   * Turns an array of words into a FeatureMap containing their total
   * numeric value and the type of words used
   * 
   * @param words an sequence of words that represent a number
   * @return a FeatureMap detailing the numeric value and type of the
   *         number, or null if the words are not a number
   */
    private FeatureMap calculateValue(String... words) {
        boolean hasWords = false;
        boolean hasNumbers = false;
        TreeMap<Double, Double> values = new TreeMap<Double, Double>();
        values.put(0d, 0d);
        for (Multiplier m : config.multipliers.values()) {
            if (m.type.equals(Multiplier.Type.BASE_10)) values.put(m.value, 0d);
        }
        for (String word : words) {
            Double value;
            Multiplier multiplier;
            if (word.matches(numericPattern.pattern())) {
                values.put(0d, values.get(0d) + Double.parseDouble(word.replaceAll(Pattern.quote(config.digitGroupingSymbol), "").replaceAll(Pattern.quote(config.decimalSymbol), ".")));
                hasNumbers = true;
            } else if ((value = config.words.get(word.toLowerCase())) != null) {
                values.put(0d, values.get(0d) + value);
                hasWords = true;
            } else if ((multiplier = config.multipliers.get(word.toLowerCase())) != null) {
                value = multiplier.value;
                if (multiplier.type.equals(Multiplier.Type.FRACTION)) {
                    double sum = 0;
                    for (double power : values.keySet()) {
                        System.out.println(power + ", " + values.get(power) + ", " + (values.get(power) / value));
                        values.put(power, values.get(power) / value);
                        sum += values.get(power);
                    }
                    if (sum == 0) {
                        values.put(0d, 1 / value);
                    }
                } else if (multiplier.type.equals(Multiplier.Type.POWER)) {
                    double sum = 0;
                    for (double power : values.keySet()) {
                        double actual = values.get(power) * Math.round(Math.pow(10, power));
                        actual = Math.pow(actual, value);
                        actual = actual / Math.round(Math.pow(10, power));
                        values.put(power, actual);
                        sum += values.get(power);
                    }
                    if (sum == 0) {
                        values.put(0d, 1d / value);
                    }
                } else {
                    int sum = 0;
                    for (double power : values.keySet()) {
                        if (power == value) {
                            break;
                        }
                        values.put(value, values.get(value) + values.get(power) * Math.round(Math.pow(10, power)));
                        sum += values.get(power);
                        values.put(power, 0D);
                    }
                    if (sum == 0) {
                        values.put(value, 1D);
                    }
                }
                hasWords = true;
            } else {
                return null;
            }
        }
        double result = 0;
        for (double v : values.keySet()) {
            result += values.get(v) * Math.pow(10, v);
        }
        String type = "numbers";
        if (hasWords && hasNumbers) type = "wordsAndNumbers"; else if (hasWords) type = "words";
        FeatureMap fm = Factory.newFeatureMap();
        fm.put(VALUE_FEATURE_NAME, result);
        fm.put(TYPE_FEATURE_NAME, type);
        return fm;
    }

    @Override
    public void execute() throws ExecutionException {
        interrupted = false;
        if (document == null) throw new ExecutionException("No Document provided!");
        AnnotationSet annotationSet = document.getAnnotations(annotationSetName);
        AnnotationSet tokens = annotationSet.get(TOKEN_ANNOTATION_TYPE);
        if (tokens == null || tokens.size() < 1) {
            if (failOnMissingInputAnnotations) {
                throw new ExecutionException("No tokens to process in document " + document.getName() + "\n" + "Please run a tokeniser first!");
            }
            Utils.logOnce(logger, Level.INFO, "Numbers Tagger: no token annotations in input document - see debug log for details.");
            logger.debug("No input annotations in document " + document.getName());
            return;
        }
        AnnotationSet sentences = annotationSet.get(SENTENCE_ANNOTATION_TYPE);
        if (sentences == null || sentences.size() < 1) {
            if (failOnMissingInputAnnotations) {
                throw new ExecutionException("No sentences to process in document " + document.getName() + "\n" + "Please run a sentence splitter first!");
            }
            Utils.logOnce(logger, Level.INFO, "Numbers Tagger: no sentence annotations in input document - see debug log for details.");
            logger.debug("No input annotations in document " + document.getName());
            return;
        }
        long startTime = System.currentTimeMillis();
        fireStatusChanged("Tagging Numbers in " + document.getName());
        fireProgressChanged(0);
        String text = document.getContent().toString();
        Matcher matcher = numericPattern.matcher(text);
        while (matcher.find()) {
            if (isInterrupted()) {
                throw new ExecutionInterruptedException("The execution of the \"" + getName() + "\" Numbers Tagger has been abruptly interrupted!");
            }
            FeatureMap fm = calculateValue(matcher.group());
            if (fm != null) {
                try {
                    annotationSet.add((long) matcher.start(), (long) matcher.end(), NUMBER_ANNOTATION_NAME, fm);
                } catch (InvalidOffsetException e) {
                }
            }
        }
        matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (isInterrupted()) {
                throw new ExecutionInterruptedException("The execution of the \"" + getName() + "\" Numbers Tagger has been abruptly interrupted!");
            }
            List<String> words = new ArrayList<String>();
            Matcher subMatcher = subPattern.matcher(matcher.group());
            while (subMatcher.find()) {
                words.add(subMatcher.group(1));
            }
            FeatureMap fm = calculateValue(words.toArray(new String[words.size()]));
            if (fm != null) {
                try {
                    annotationSet.removeAll(annotationSet.getContained((long) matcher.start(), (long) matcher.end()).get(NUMBER_ANNOTATION_NAME));
                    annotationSet.add((long) matcher.start(), (long) matcher.end(), NUMBER_ANNOTATION_NAME, fm);
                } catch (InvalidOffsetException e) {
                }
            }
        }
        if (isInterrupted()) {
            throw new ExecutionInterruptedException("The execution of the \"" + getName() + "\" Numbers Tagger has been abruptly interrupted!");
        }
        if (useHintsFromOriginalMarkups) {
            AnnotationSet supAnnotations = document.getAnnotations(ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("sup");
            for (Annotation sup : supAnnotations) {
                AnnotationSet numbers = annotationSet.getContained(sup.getStartNode().getOffset(), sup.getEndNode().getOffset());
                for (Annotation num : numbers) {
                    num.getFeatures().put(HINT_FEATURE_NAME, "sup");
                }
            }
        }
        try {
            jape.setDocument(getDocument());
            jape.setParameterValue("inputASName", annotationSetName);
            jape.setParameterValue("outputASName", annotationSetName);
            jape.getFeatures().put("decimalSymbol", config.decimalSymbol);
            jape.getFeatures().put("digitGroupingSymbol", config.digitGroupingSymbol);
            jape.getFeatures().put("allowWithinWords", allowWithinWords);
            jape.execute();
        } catch (ResourceInstantiationException e) {
            throw new ExecutionException(e);
        } finally {
            jape.setDocument(null);
        }
        fireProcessFinished();
        fireStatusChanged(document.getName() + " tagged with Numbers in " + NumberFormat.getInstance().format((double) (System.currentTimeMillis() - startTime) / 1000) + " seconds!");
    }

    public void cleanup() {
        Factory.deleteResource(jape);
    }

    public Resource init() throws ResourceInstantiationException {
        if (configURL == null) throw new ResourceInstantiationException("No configuration file specified!");
        if (postProcessURL == null) throw new ResourceInstantiationException("No post-processing JAPE file specified!");
        try {
            XStream xstream = Config.getXStream(configURL, getClass().getClassLoader());
            BomStrippingInputStreamReader in = new BomStrippingInputStreamReader(configURL.openStream(), encoding);
            config = (Config) xstream.fromXML(in);
            in.close();
        } catch (Exception e) {
            throw new ResourceInstantiationException(e);
        }
        if (config.decimalSymbol.equals(config.digitGroupingSymbol)) throw new ResourceInstantiationException("The decimal symbol and digit grouping symbol must be different!");
        Set<String> allWords = config.words.keySet();
        Set<String> allMultipliers = config.multipliers.keySet();
        List<String> conjunctions = new ArrayList<String>(config.conjunctions.keySet());
        if (allWords.removeAll(allMultipliers)) throw new ResourceInstantiationException("The set of words and multipliers must be disjoint!");
        List<String> list = new ArrayList<String>();
        list.addAll(allWords);
        list.addAll(allMultipliers);
        if (list.removeAll(conjunctions)) throw new ResourceInstantiationException("Conjunctions cannot also be words or multipliers!");
        Comparator<String> lengthComparator = new Comparator<String>() {

            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        };
        Collections.sort(conjunctions, lengthComparator);
        StringBuilder withSpaces = new StringBuilder();
        StringBuilder withoutSpaces = new StringBuilder();
        for (String conjunction : conjunctions) {
            withSpaces.append("|").append(conjunction);
            if (!config.conjunctions.get(conjunction)) {
                withoutSpaces.append("|").append(conjunction);
            }
        }
        String separatorsRegex = "(?i:\\s{1,2}(?:-" + withSpaces + ")\\s{1,2}|\\s{1,2}|-" + withoutSpaces + ")";
        String numericRegex = "[-+]?(?:(?:(?:(?:[0-9]+" + Pattern.quote(config.digitGroupingSymbol) + ")*[0-9]+(?:" + Pattern.quote(config.decimalSymbol) + "[0-9]+)?))|(?:" + Pattern.quote(config.decimalSymbol) + "[0-9]+))";
        numericPattern = Pattern.compile(numericRegex);
        Collections.sort(list, lengthComparator);
        StringBuilder builder = new StringBuilder("(?i:");
        for (String word : list) {
            builder.append(Pattern.quote(word)).append("|");
        }
        String numbersRegex = builder.substring(0, builder.length() - 1);
        numbersRegex += ")";
        String regex = "(?:(?:" + numericRegex + separatorsRegex + "?)?(?:" + numbersRegex + separatorsRegex + "){0,10}" + numbersRegex + ")";
        pattern = Pattern.compile("(?:^|(?<=\\s|[^\\p{Alnum}]))" + regex + "(?:(?=\\s|[^\\p{Alnum}])|$)", Pattern.CASE_INSENSITIVE + Pattern.UNICODE_CASE);
        regex = "(" + numbersRegex + "|" + numericRegex + ")" + separatorsRegex + "?";
        subPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE + Pattern.UNICODE_CASE);
        FeatureMap params = Factory.newFeatureMap();
        params.put("grammarURL", postProcessURL);
        if (jape == null) {
            FeatureMap hidden = Factory.newFeatureMap();
            Gate.setHiddenAttribute(hidden, true);
            jape = (Transducer) Factory.createResource("gate.creole.Transducer", params, hidden);
        } else {
            jape.setParameterValues(params);
            jape.reInit();
        }
        return this;
    }

    /**
   * This class provides access to the configuration file in a simple
   * fashion. It is instantiated using XStream to map from the XML
   * configuration file to the Object structure.
   * 
   * @author Mark A. Greenwood
   */
    static class Config {

        private String description;

        private Map<String, Double> words;

        private Map<String, Multiplier> multipliers;

        private Map<String, Boolean> conjunctions;

        private Map<URL, String> imports;

        private String decimalSymbol;

        private String digitGroupingSymbol;

        public String toString() {
            return description + " -- words: " + words.size() + ", multiplies: " + multipliers.size() + ", conjunctions: " + conjunctions.size();
        }

        /**
     * Ensures that all fields have been initialised to useful values
     * after the instance has been created from the configuration file.
     * This includes creating default values if certain aspects have not
     * been specified and the importing of linked configuration files.
     * 
     * @return a correctly initialised Config object.
     */
        private Object readResolve() {
            if (words == null) words = new HashMap<String, Double>();
            if (multipliers == null) multipliers = new HashMap<String, Multiplier>();
            if (conjunctions == null) conjunctions = new HashMap<String, Boolean>();
            if (decimalSymbol == null) decimalSymbol = ".";
            if (digitGroupingSymbol == null) digitGroupingSymbol = ",";
            if (imports == null) {
                imports = new HashMap<URL, String>();
            } else {
                for (Map.Entry<URL, String> entry : imports.entrySet()) {
                    URL url = entry.getKey();
                    String encoding = entry.getValue();
                    XStream xstream = getXStream(url, getClass().getClassLoader());
                    BomStrippingInputStreamReader in = null;
                    try {
                        in = new BomStrippingInputStreamReader(url.openStream(), encoding);
                        Config c = (Config) xstream.fromXML(in);
                        words.putAll(c.words);
                        multipliers.putAll(c.multipliers);
                        conjunctions.putAll(c.conjunctions);
                    } catch (IOException ioe) {
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            return this;
        }

        /**
     * Creates a correctly configured XStream for reading the XML
     * configuration files.
     * 
     * @param url the URL of the config file you are loading. This is
     *          required so that we can correctly handle relative paths
     *          in import statements.
     * @param cl the Classloader which has access to the classes
     *          required. This is needed as otherwise loading this
     *          through GATE we somehow can't find some of the classes.
     * @return an XStream instance that can load the XML config files
     *         for this PR.
     */
        static XStream getXStream(final URL url, ClassLoader cl) {
            if (url == null) throw new IllegalArgumentException("You must specify the URL of the file you are processing");
            XStream xstream = new XStream();
            if (cl != null) xstream.setClassLoader(cl);
            xstream.alias("config", Config.class);
            xstream.registerConverter(new Converter() {

                @SuppressWarnings("rawtypes")
                public boolean canConvert(Class type) {
                    return type.equals(HashMap.class);
                }

                public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                    throw new RuntimeException("Writing config files is not currently supported!");
                }

                @SuppressWarnings({ "rawtypes", "unchecked" })
                public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                    HashMap map = new HashMap();
                    while (reader.hasMoreChildren()) {
                        try {
                            if (reader.getNodeName().equals("imports")) {
                                String encoding = reader.getAttribute("encoding");
                                reader.moveDown();
                                String rURL = reader.getValue();
                                reader.moveUp();
                                map.put(new URL(url, rURL), encoding);
                            } else if (reader.getNodeName().equals("conjunctions")) {
                                String value = reader.getAttribute("whole");
                                reader.moveDown();
                                String word = reader.getValue().toLowerCase();
                                reader.moveUp();
                                map.put(word, Boolean.parseBoolean(value));
                            } else {
                                String[] values = reader.getAttribute("value").split("/");
                                String type = reader.getAttribute("type");
                                reader.moveDown();
                                String word = reader.getValue().toLowerCase();
                                reader.moveUp();
                                double value = Double.parseDouble(values[0]);
                                for (int i = 1; i < values.length; ++i) {
                                    value = value / Double.parseDouble(values[i]);
                                }
                                if (reader.getNodeName().equals("multipliers")) {
                                    map.put(word, new Multiplier(value, type == null ? "e" : type));
                                } else {
                                    map.put(word, value);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return map;
                }
            });
            return xstream;
        }
    }

    private static class Multiplier {

        private enum Type {

            BASE_10("e"), FRACTION("/"), POWER("^");

            final String description;

            Type(String description) {
                this.description = description;
            }

            public static Type get(String type) {
                for (Type t : EnumSet.allOf(Type.class)) {
                    if (t.description.equals(type)) return t;
                }
                throw new IllegalArgumentException("'" + type + "' is not a valid multiplier type type");
            }
        }

        Type type;

        Double value;

        public Multiplier(Double value, String type) {
            this.value = value;
            this.type = Type.get(type);
        }
    }
}
