package org.neurpheus.nlp.morphology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neurpheus.nlp.morphology.baseimpl.TagsetImpl;
import org.neurpheus.nlp.morphology.tagset.Tagset;

/**
 * <b>The default factory of morphology components</b>.
 * <p>
 * This factory search for implementations of morphology components 
 * in MET-INF/services/* resources.
 * </p>
 * <p>
 * To get an instance of this class use the following code:
 * <pre>
 *    MorphologyFactory morphologyFactory = DefaultMorphologyFactory.getInstance();
 * </pre>
 * </p>
 *
 * @author Jakub Strychowski
 */
public class DefaultMorphologyFactory implements MorphologyFactory {

    /** Logger for this class. */
    private static Logger logger = Logger.getLogger(DefaultMorphologyFactory.class.getName());

    /** Holds singleton of this class. */
    private static DefaultMorphologyFactory instance = new DefaultMorphologyFactory();

    /** 
     * Holds all stemmers for different languages. 
     * This attribute is a mapping between languages (locale) and collections of stemmers.
     */
    private Map stemmers;

    /** 
     * Holds only best stemmers for different languages. 
     * This attribute is a mapping between languages (locale) and stemmers.
     */
    private Map mostAccurateStemmers;

    /** 
     * Holds only fastest stemmers for different languages. 
     * This attribute is a mapping between languages (locale) and stemmers.
     */
    private Map fastestStemmers;

    /** 
     * Holds all lemmatizers for different languages. 
     * This attribute is a mapping between languages (locale) and collections of lemmatizers.
     */
    private Map lemmatizers;

    /** 
     * Holds only best lemmatizers for different languages. 
     * This attribute is a mapping between languages (locale) and lemmatizers.
     */
    private Map mostAccurateLemmatizers;

    /** 
     * Holds only fastest lemmatizers for different languages. 
     * This attribute is a mapping between languages (locale) and lemmatizers.
     */
    private Map fastestLemmatizers;

    /** 
     * Holds all morphological analysers for different languages. 
     * This attribute is a mapping between languages (locale) and collections of morphological analysers.
     */
    private Map analysiers;

    /** 
     * Holds only best morphological analysers for different languages. 
     * This attribute is a mapping between languages (locale) and morphological analysers.
     */
    private Map mostAccurateAnalysiers;

    /** 
     * Holds only fastest morphological analysers for different languages. 
     * This attribute is a mapping between languages (locale) and morphological analysers.
     */
    private Map fastestAnalysiers;

    /** 
     * Holds all generators for different languages. 
     * This attribute is a mapping between languages (locale) and collections of generators.
     */
    private Map generators;

    /** 
     * Holds only best generators for different languages. 
     * This attribute is a mapping between languages (locale) and generators.
     */
    private Map mostAccurateGenerators;

    /** 
     * Holds only fastest generators for different languages. 
     * This attribute is a mapping between languages (locale) and generators.
     */
    private Map fastestGenerators;

    /**
     * <b>Returns a singleton instance of this class.</b>
     * 
     * @return The instance of this class.
     */
    public static DefaultMorphologyFactory getInstance() {
        if (instance == null) {
            createInstance();
        }
        return instance;
    }

    /**
     * Create the singleton instance of this class.
     */
    private static synchronized void createInstance() {
        if (instance == null) {
            instance = new DefaultMorphologyFactory();
        }
    }

    /**
     * The weight denoting that a quality or a speed factor is important.
     */
    private static final int MORE_IMPORTANT = 1000;

    /**
     * The weight denoting that a quality or a speed factor is not so important.
     */
    private static final int LES_IMPORTANT = 1;

    /**
     * Constructs this class.
     */
    private DefaultMorphologyFactory() {
        stemmers = lookupForMorphologyComponents(Stemmer.class);
        mostAccurateStemmers = createBestComponentsMap(stemmers, MORE_IMPORTANT, LES_IMPORTANT);
        fastestStemmers = createBestComponentsMap(stemmers, LES_IMPORTANT, MORE_IMPORTANT);
        lemmatizers = lookupForMorphologyComponents(Lemmatizer.class);
        mostAccurateLemmatizers = createBestComponentsMap(lemmatizers, MORE_IMPORTANT, LES_IMPORTANT);
        fastestLemmatizers = createBestComponentsMap(lemmatizers, LES_IMPORTANT, MORE_IMPORTANT);
        analysiers = lookupForMorphologyComponents(MorphologicalAnalyser.class);
        mostAccurateAnalysiers = createBestComponentsMap(analysiers, MORE_IMPORTANT, LES_IMPORTANT);
        fastestAnalysiers = createBestComponentsMap(analysiers, LES_IMPORTANT, MORE_IMPORTANT);
        generators = lookupForMorphologyComponents(WordFormGenerator.class);
        mostAccurateGenerators = createBestComponentsMap(generators, MORE_IMPORTANT, LES_IMPORTANT);
        fastestGenerators = createBestComponentsMap(generators, LES_IMPORTANT, MORE_IMPORTANT);
    }

    /**
     * Finds a components fulfilling the given criteria.
     * 
     * @param componentsMap The collection containing all components.
     * @param loc The language which have to be supported by the result component.
     * @param qualityImportance Importance of a quality of components during searching.
     * @param speedImportance Importance of a speed of components during searching.
     * @param propertyName Optional name of a property which have to be set for the result component.
     *  If <code>null</code> properties are ignored.
     * @param propertyValue The value of the property <code>propertyName</code>.
     * 
     * @return Found component or <code>null</code> if there is no such component.
     */
    private MorphologicalComponent findComponent(final Map componentsMap, final Locale loc, final double qualityImportance, final double speedImportance, final String propertyName, final Object propertyValue) {
        List list = (List) componentsMap.get(loc);
        if (list == null) {
            return null;
        }
        MorphologicalComponent result = null;
        double bestValue = -1;
        for (final Iterator it = list.iterator(); it.hasNext(); ) {
            MorphologicalComponent comp = (MorphologicalComponent) it.next();
            double value = comp.getQuality() * qualityImportance + comp.getSpeed() * speedImportance;
            if (value > bestValue) {
                boolean match = true;
                if (propertyName != null) {
                    Object v = comp.getProperty(propertyName);
                    match = v == propertyValue || (v != null && propertyValue != null && v.equals(propertyValue));
                }
                if (match) {
                    result = comp;
                    bestValue = value;
                }
            }
        }
        return result;
    }

    /**
     * Creates a map containing only best components for different languages.
     * 
     * @param components The map containing all components for different languages.
     * @param qualityImportance The importance of quality while best components selecting.
     * @param speedImportance The importance of speed while best components selecting.
     * 
     * @return Map of best components for different languages.
     */
    private Map createBestComponentsMap(final Map components, final double qualityImportance, final double speedImportance) {
        Map result = new HashMap();
        for (final Iterator it = components.keySet().iterator(); it.hasNext(); ) {
            Locale loc = (Locale) it.next();
            MorphologicalComponent component = findComponent(components, loc, qualityImportance, speedImportance, null, null);
            if (component != null) {
                result.put(loc, component);
            }
        }
        return result;
    }

    /**
     * Looks up for implementations of the given interface of morphological components.
     * 
     * @param componentInterface The interface implemented by the component.
     * 
     * @return Mapping between languages and lists of found components.
     */
    private Map lookupForMorphologyComponents(final Class componentInterface) {
        Map componentsMap = new HashMap();
        try {
            List implementations = lookupForImplementations(componentInterface, null, null, false, true);
            for (final Iterator it = implementations.iterator(); it.hasNext(); ) {
                MorphologicalComponent comp = (MorphologicalComponent) it.next();
                Collection locales = comp.getSupportedLocales();
                if (locales != null) {
                    for (final Iterator lit = locales.iterator(); lit.hasNext(); ) {
                        Object obj = lit.next();
                        if (obj != null && obj instanceof Locale) {
                            Locale loc = (Locale) obj;
                            try {
                                MorphologicalComponent mc = comp.getInstance(loc);
                                List list = (List) componentsMap.get(loc);
                                if (list == null) {
                                    list = new ArrayList();
                                    componentsMap.put(loc, list);
                                }
                                list.add(mc);
                            } catch (MorphologyException e) {
                                logger.log(Level.WARNING, "Cannot create instance of component " + comp.getClass().getName() + " for locale " + loc.toString(), e);
                            }
                        } else {
                            logger.warning("The morphology component " + comp.getClass().getName() + " has returned invalid object as locale.");
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Cannot find any implementation of " + componentInterface.getName());
            }
        }
        return componentsMap;
    }

    /**
     * Returns a list of implementations of the given interface or abstract class.
     * <p>
     * This method returns implementations the <code>clazz</code> interface according to the following rules:
     * <ul>
     * <li> 
     * If there is a system property having name equal to the full name of <code>clazz</code>
     * then a value of the property is added to the result list.
     * </li>
     * <li> 
     * If there are resources at location META-INF/services/<code>FULL_NAME_OF_CLAZZ</code> then
     * all that resources provides names of implementations. Each resource
     * contains a list of full class names separated by new line sign.
     * The '#' character starts a comment in the line.
     * The "#-" string at the start of a line marks all classes which should be excluded
     * from the implementations list.
     * </li>
     * <li>
     * All implementations from the <code>defaultImplementations</code> array are added to the result list.
     * </li>
     * <li>
     * If the required interface (<code>clazz</code>) represents a concrete class 
     * then this class is added to the result list.
     * </li>
     * </ul>
     * <p>
     * This method looks up for all implementations, and next loads all classes
     * using current class loader, and optionally creates a instances of the loaded classes.
     *
     *  @param clazz        The interface of an abstract class, for which an implementation has to be found.
     *  @param loader       The class loader used by this method. If <code>null</code> this method 
     *                      uses class loader provided by the <code>clazz</code> class.
     *  @param defaultImplementations   The array with names of default 
     *                                  implementations of the <code>clazz</code> interface.     
     *  @param onlyFirst    If <code>true</code> this method returns only first found implementation.
     *  @param returnInstances          If <code>true</code> returns instances of found implementations.
     *
     *  @return A list of objects representing classes (<code>Class</code> objects) 
     *          or a list of instances of these classes.
     *
     *  @throws ClassNotFoundException if there are no an implementation 
     *      or the implementation cannot be created.
     */
    private static List lookupForImplementations(final Class clazz, final ClassLoader loader, final String[] defaultImplementations, final boolean onlyFirst, final boolean returnInstances) throws ClassNotFoundException {
        if (clazz == null) {
            throw new IllegalArgumentException("Argument 'clazz' cannot be null!");
        }
        ClassLoader classLoader = loader;
        if (classLoader == null) {
            classLoader = clazz.getClassLoader();
        }
        String interfaceName = clazz.getName();
        ArrayList tmp = new ArrayList();
        ArrayList toRemove = new ArrayList();
        String className = System.getProperty(interfaceName);
        if (className != null && className.trim().length() > 0) {
            tmp.add(className.trim());
        }
        Enumeration en = null;
        try {
            en = classLoader.getResources("META-INF/services/" + clazz.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (en != null && en.hasMoreElements()) {
            URL url = (URL) en.nextElement();
            InputStream is = null;
            try {
                is = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                do {
                    line = reader.readLine();
                    boolean remove = false;
                    if (line != null) {
                        if (line.startsWith("#-")) {
                            remove = true;
                            line = line.substring(2);
                        }
                        int pos = line.indexOf('#');
                        if (pos >= 0) {
                            line = line.substring(0, pos);
                        }
                        line = line.trim();
                        if (line.length() > 0) {
                            if (remove) {
                                toRemove.add(line);
                            } else {
                                tmp.add(line);
                            }
                        }
                    }
                } while (line != null);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (defaultImplementations != null) {
            for (int i = 0; i < defaultImplementations.length; i++) {
                tmp.add(defaultImplementations[i].trim());
            }
        }
        if (!clazz.isInterface()) {
            int m = clazz.getModifiers();
            if (!Modifier.isAbstract(m) && Modifier.isPublic(m) && !Modifier.isStatic(m)) {
                tmp.add(interfaceName);
            }
        }
        tmp.removeAll(toRemove);
        ArrayList res = new ArrayList();
        for (Iterator it = tmp.iterator(); it.hasNext(); ) {
            className = (String) it.next();
            try {
                Class c = Class.forName(className, false, classLoader);
                if (c != null) {
                    if (clazz.isAssignableFrom(c)) {
                        if (returnInstances) {
                            Object o = null;
                            try {
                                o = c.newInstance();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            if (o != null) {
                                res.add(o);
                                if (onlyFirst) {
                                    return res;
                                }
                            }
                        } else {
                            res.add(c);
                            if (onlyFirst) {
                                return res;
                            }
                        }
                    } else {
                        logger.warning("MetaInfLookup: Class '" + className + "' is not a subclass of class : " + interfaceName);
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.log(Level.WARNING, "Cannot create implementation of interface: " + interfaceName, e);
            }
        }
        if (res.size() == 0) {
            throw new ClassNotFoundException("Cannot find any implemnetation of class " + interfaceName);
        }
        return res;
    }

    /** 
     * Returns a list of all available stemmers for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return The list of objects implementing the {@link Stemmer} interface.
     */
    public List getAllStemmers(final Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("The 'locale' argument cannot be null.");
        }
        List result = (List) stemmers.get(locale);
        if (result == null) {
            result = Collections.EMPTY_LIST;
        }
        return result;
    }

    /**
     * Returns the stemmer for the given language and the given value of a property.
     * 
     * @param locale The locale object which represents the language.
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property.
     * 
     * @return Found stemmer.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching stemmer.
     */
    public Stemmer getStemmer(final Locale locale, final String propertyName, final Object propertyValue) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("The 'propertyName' argument cannot be null.");
        }
        MorphologicalComponent component = findComponent(stemmers, locale, 1, 1, propertyName, propertyValue);
        if (component == null) {
            throw new MorphologyException("Cannot find stemmer for locales : " + locale.toString());
        } else {
            return (Stemmer) component;
        }
    }

    /**
     * Returns the most accurate stemmer for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return Found stemmer.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching stemmer.
     */
    public Stemmer getMostAccurateStemmer(final Locale locale) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        Stemmer component = (Stemmer) mostAccurateStemmers.get(locale);
        if (component == null) {
            throw new MorphologyException("Cannot find stemmer for locales : " + locale.toString());
        }
        return component;
    }

    /**
     * Returns the fastest stemmer for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return Found stemmer.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching stemmer.
     */
    public Stemmer getFastestStemmer(final Locale locale) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        Stemmer component = (Stemmer) fastestStemmers.get(locale);
        if (component == null) {
            throw new MorphologyException("Cannot find stemmer for locales : " + locale.toString());
        }
        return component;
    }

    /**
     * Returns a stemmer fulfilling the given criteria.
     * 
     * @param locale The locale object which represents the language.
     * @param qualityImportance 
     *  The weight for the value of a quality property used during a selection of the result component.
     * @param speedImportance
     *  The weight for the value of a speed property used during a selection of the result component.
     * 
     * @return Found stemmer.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching stemmer.
     */
    public Stemmer getStemmer(final Locale locale, final double qualityImportance, final double speedImportance) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        MorphologicalComponent component = findComponent(stemmers, locale, qualityImportance, speedImportance, null, null);
        if (component == null) {
            throw new MorphologyException("Cannot find stemmer for locales : " + locale.toString());
        } else {
            return (Stemmer) component;
        }
    }

    /** 
     * Returns a list of all available lemmatizers for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return The list of objects implementing the {@link Lemmatizer} interface.
     */
    public List getAllLemmatizers(final Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("The 'locale' argument cannot be null.");
        }
        List result = (List) lemmatizers.get(locale);
        if (result == null) {
            result = Collections.EMPTY_LIST;
        }
        return result;
    }

    /**
     * Returns the lemmatizer for the given language and the given value of a property.
     * 
     * @param locale The locale object which represents the language.
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property.
     * 
     * @return Found lemmatizer.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching lemmatizer.
     */
    public Lemmatizer getLemmatizer(final Locale locale, final String propertyName, final Object propertyValue) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("The 'propertyName' argument cannot be null.");
        }
        MorphologicalComponent component = findComponent(lemmatizers, locale, 1, 1, propertyName, propertyValue);
        if (component == null) {
            throw new MorphologyException("Cannot find lemmatizer for locales : " + locale.toString());
        } else {
            return (Lemmatizer) component;
        }
    }

    /**
     * Returns the most accurate lemmatizer for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return Found lemmatizer.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching lemmatizer.
     */
    public Lemmatizer getMostAccurateLemmatizer(final Locale locale) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        Lemmatizer component = (Lemmatizer) mostAccurateLemmatizers.get(locale);
        if (component == null) {
            throw new MorphologyException("Cannot find lemmatizer for locales : " + locale.toString());
        }
        return component;
    }

    /**
     * Returns the fastest lemmatizer for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return Found lemmatizer.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching lemmatizer.
     */
    public Lemmatizer getFastestLemmatizer(final Locale locale) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        Lemmatizer component = (Lemmatizer) fastestLemmatizers.get(locale);
        if (component == null) {
            throw new MorphologyException("Cannot find lemmatizer for locales : " + locale.toString());
        }
        return component;
    }

    /**
     * Returns a lemmatizer fulfilling the given criteria.
     * 
     * @param locale The locale object which represents the language.
     * @param qualityImportance 
     *  The weight for the value of a quality property used during a selection of the result component.
     * @param speedImportance
     *  The weight for the value of a speed property used during a selection of the result component.
     * 
     * @return Found lemmatizer.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching lemmatizer.
     */
    public Lemmatizer getLemmatizer(final Locale locale, final double qualityImportance, final double speedImportance) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        MorphologicalComponent component = findComponent(lemmatizers, locale, qualityImportance, speedImportance, null, null);
        if (component == null) {
            throw new MorphologyException("Cannot find lemmatizer for locales : " + locale.toString());
        } else {
            return (Lemmatizer) component;
        }
    }

    /** 
     * Returns a list of all available morphological analysers for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return The list of objects implementing the {@link MorphologicalAnalyser} interface.
     */
    public List getAllMorphologicalAnalysers(final Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("The 'locale' argument cannot be null.");
        }
        List result = (List) analysiers.get(locale);
        if (result == null) {
            result = Collections.EMPTY_LIST;
        }
        return result;
    }

    /**
     * Returns the morphological analyser for the given language and the given value of a property.
     * 
     * @param locale The locale object which represents the language.
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property.
     * 
     * @return Found morphological analyser.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException 
     * if there is no matching morphological analyser.
     */
    public MorphologicalAnalyser getMorphologicalAnalyser(final Locale locale, final String propertyName, final Object propertyValue) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("The 'propertyName' argument cannot be null.");
        }
        MorphologicalComponent component = findComponent(analysiers, locale, 1, 1, propertyName, propertyValue);
        if (component == null) {
            throw new MorphologyException("Cannot find morphological analyser for locales : " + locale.toString());
        } else {
            return (MorphologicalAnalyser) component;
        }
    }

    /**
     * Returns the most accurate morphological analyser for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return Found morphological analyser.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException 
     * if there is no matching morphological analyser.
     */
    public MorphologicalAnalyser getMostAccurateMorphologicalAnalyser(final Locale locale) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        MorphologicalAnalyser component = (MorphologicalAnalyser) mostAccurateAnalysiers.get(locale);
        if (component == null) {
            throw new MorphologyException("Cannot find morphological analyser for locales : " + locale.toString());
        }
        return component;
    }

    /**
     * Returns the fastest morphological analyser for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return Found morphological analyser.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException 
     * if there is no matching morphological analyser.
     */
    public MorphologicalAnalyser getFastestMorphologicalAnalyser(final Locale locale) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        MorphologicalAnalyser component = (MorphologicalAnalyser) fastestAnalysiers.get(locale);
        if (component == null) {
            throw new MorphologyException("Cannot find morphological analyser for locales : " + locale.toString());
        }
        return component;
    }

    /**
     * Returns a morphological analyser fulfilling the given criteria.
     * 
     * @param locale The locale object which represents the language.
     * @param qualityImportance 
     *  The weight for the value of a quality property used during a selection of the result component.
     * @param speedImportance
     *  The weight for the value of a speed property used during a selection of the result component.
     * 
     * @return Found morphological analyser.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException 
     * if there is no matching morphological analyser.
     */
    public MorphologicalAnalyser getMorphologicalAnalyser(final Locale locale, final double qualityImportance, final double speedImportance) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        MorphologicalComponent component = findComponent(analysiers, locale, qualityImportance, speedImportance, null, null);
        if (component == null) {
            throw new MorphologyException("Cannot find morphological analyser for locales : " + locale.toString());
        } else {
            return (MorphologicalAnalyser) component;
        }
    }

    /** 
     * Returns a list of all available generators for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return The list of objects implementing the {@link WordFormGenerator} interface.
     */
    public List getAllGenerators(final Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("The 'locale' argument cannot be null.");
        }
        List result = (List) generators.get(locale);
        if (result == null) {
            result = Collections.EMPTY_LIST;
        }
        return result;
    }

    /**
     * Returns the generator for the given language and the given value of a property.
     * 
     * @param locale The locale object which represents the language.
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property.
     * 
     * @return Found generator.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching generator.
     */
    public WordFormGenerator getGenerator(final Locale locale, final String propertyName, final Object propertyValue) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("The 'propertyName' argument cannot be null.");
        }
        MorphologicalComponent component = findComponent(generators, locale, 1, 1, propertyName, propertyValue);
        if (component == null) {
            throw new MorphologyException("Cannot find generator for locales : " + locale.toString());
        } else {
            return (WordFormGenerator) component;
        }
    }

    /**
     * Returns the most accurate generator for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return Found generator.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching generator.
     */
    public WordFormGenerator getMostAccurateGenerator(final Locale locale) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        WordFormGenerator component = (WordFormGenerator) mostAccurateGenerators.get(locale);
        if (component == null) {
            throw new MorphologyException("Cannot find generator for locales : " + locale.toString());
        }
        return component;
    }

    /**
     * Returns the fastest generator for the given language.
     * 
     * @param locale The locale object which represents the language.
     * 
     * @return Found generator.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching generator.
     */
    public WordFormGenerator getFastestGenerator(final Locale locale) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        WordFormGenerator component = (WordFormGenerator) fastestGenerators.get(locale);
        if (component == null) {
            throw new MorphologyException("Cannot find generator for locales : " + locale.toString());
        }
        return component;
    }

    /**
     * Returns a generator fulfilling the given criteria.
     * 
     * @param locale The locale object which represents the language.
     * @param qualityImportance 
     *  The weight for the value of a quality property used during a selection of the result component.
     * @param speedImportance
     *  The weight for the value of a speed property used during a selection of the result component.
     * 
     * @return Found generator.
     * 
     * @throws org.neurpheus.nlp.morphology.MorphologyException if there is no matching generator.
     */
    public WordFormGenerator getGenerator(final Locale locale, final double qualityImportance, final double speedImportance) throws MorphologyException {
        if (locale == null) {
            throw new IllegalArgumentException("The 'loc' argument cannot be null.");
        }
        MorphologicalComponent component = findComponent(generators, locale, qualityImportance, speedImportance, null, null);
        if (component == null) {
            throw new MorphologyException("Cannot find generator for locales : " + locale.toString());
        } else {
            return (WordFormGenerator) component;
        }
    }

    /**
     * Creates an empty tagset.
     * 
     * @return The empty tagset.
     */
    public Tagset createTagset() {
        return new TagsetImpl();
    }
}
