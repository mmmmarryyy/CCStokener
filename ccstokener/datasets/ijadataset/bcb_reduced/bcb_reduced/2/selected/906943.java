package org.rubypeople.rdt.internal.ui.text.spelling;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.rubypeople.rdt.internal.ui.RubyPlugin;
import org.rubypeople.rdt.internal.ui.text.spelling.engine.DefaultSpellChecker;
import org.rubypeople.rdt.internal.ui.text.spelling.engine.ISpellCheckEngine;
import org.rubypeople.rdt.internal.ui.text.spelling.engine.ISpellCheckPreferenceKeys;
import org.rubypeople.rdt.internal.ui.text.spelling.engine.ISpellChecker;
import org.rubypeople.rdt.internal.ui.text.spelling.engine.ISpellDictionary;
import org.rubypeople.rdt.internal.ui.text.spelling.engine.PersistentSpellDictionary;

/**
 * Spell check engine for Ruby source spell checking.
 *
 * @since 3.0
 */
public class SpellCheckEngine implements ISpellCheckEngine, IPropertyChangeListener {

    /** The dictionary location */
    public static final String DICTIONARY_LOCATION = "dictionaries/";

    /** The singleton engine instance */
    private static ISpellCheckEngine fgEngine = null;

    /**
	 * Returns the available locales for this spell check engine.
	 *
	 * @return The available locales for this engine
	 */
    public static Set getAvailableLocales() {
        URL url = null;
        Locale locale = null;
        InputStream stream = null;
        final Set result = new HashSet();
        try {
            final URL location = getDictionaryLocation();
            if (location == null) return Collections.EMPTY_SET;
            final Locale[] locales = Locale.getAvailableLocales();
            for (int index = 0; index < locales.length; index++) {
                locale = locales[index];
                url = new URL(location, locale.toString().toLowerCase() + ".dictionary");
                try {
                    stream = url.openStream();
                    if (stream != null) {
                        try {
                            result.add(locale);
                        } finally {
                            stream.close();
                        }
                    }
                } catch (IOException exception) {
                }
            }
        } catch (MalformedURLException exception) {
        }
        return result;
    }

    /**
	 * Returns the default locale for this engine.
	 *
	 * @return The default locale
	 */
    public static Locale getDefaultLocale() {
        return Locale.getDefault();
    }

    /**
	 * Returns the dictionary location.
	 *
	 * @throws MalformedURLException
	 *                    if the URL could not be created
	 * @return The dictionary location, or <code>null</code> iff the location
	 *               is not known
	 */
    public static URL getDictionaryLocation() throws MalformedURLException {
        final RubyPlugin plugin = RubyPlugin.getDefault();
        if (plugin != null) return plugin.getBundle().getEntry("/" + DICTIONARY_LOCATION);
        return null;
    }

    /**
	 * Returns the singleton instance of the spell check engine.
	 *
	 * @return The singleton instance of the spell check engine
	 */
    public static final synchronized ISpellCheckEngine getInstance() {
        if (fgEngine == null) fgEngine = new SpellCheckEngine();
        return fgEngine;
    }

    /** The registered locale insenitive dictionaries */
    private final Set fGlobalDictionaries = new HashSet();

    /** The current locale */
    private Locale fLocale = null;

    /** The spell checker for fLocale */
    private ISpellChecker fChecker = null;

    /** The registered locale sensitive dictionaries */
    private final Map fLocaleDictionaries = new HashMap();

    /** The preference store where to listen */
    private IPreferenceStore fPreferences = null;

    /** The user dictionary */
    private ISpellDictionary fUserDictionary = null;

    /**
	 * Creates a new spell check manager.
	 */
    private SpellCheckEngine() {
        fGlobalDictionaries.add(new TaskTagDictionary());
        try {
            Locale locale = null;
            final URL location = getDictionaryLocation();
            for (final Iterator iterator = getAvailableLocales().iterator(); iterator.hasNext(); ) {
                locale = (Locale) iterator.next();
                fLocaleDictionaries.put(locale, new SpellReconcileDictionary(locale, location));
            }
        } catch (MalformedURLException exception) {
        }
    }

    public final synchronized ISpellChecker createSpellChecker(final Locale locale, final IPreferenceStore store) {
        if (fLocale != null && fLocale.equals(locale)) return fChecker;
        if (fChecker == null) {
            fChecker = new DefaultSpellChecker(store);
            store.addPropertyChangeListener(this);
            fPreferences = store;
            ISpellDictionary dictionary = null;
            for (Iterator iterator = fGlobalDictionaries.iterator(); iterator.hasNext(); ) {
                dictionary = (ISpellDictionary) iterator.next();
                fChecker.addDictionary(dictionary);
            }
        }
        ISpellDictionary dictionary = null;
        if (fLocale != null) {
            dictionary = (ISpellDictionary) fLocaleDictionaries.get(fLocale);
            if (dictionary != null) {
                fChecker.removeDictionary(dictionary);
                dictionary.unload();
            }
        }
        fLocale = locale;
        dictionary = (ISpellDictionary) fLocaleDictionaries.get(locale);
        if (dictionary == null) {
            if (!getDefaultLocale().equals(locale)) {
                if (fPreferences != null) fPreferences.removePropertyChangeListener(this);
                fChecker = null;
                fLocale = null;
            }
        } else fChecker.addDictionary(dictionary);
        if (fPreferences != null) propertyChange(new PropertyChangeEvent(this, ISpellCheckPreferenceKeys.SPELLING_USER_DICTIONARY, null, fPreferences.getString(ISpellCheckPreferenceKeys.SPELLING_USER_DICTIONARY)));
        return fChecker;
    }

    public final Locale getLocale() {
        return fLocale;
    }

    public final void propertyChange(final PropertyChangeEvent event) {
        if (fChecker != null && event.getProperty().equals(ISpellCheckPreferenceKeys.SPELLING_USER_DICTIONARY)) {
            if (fUserDictionary != null) {
                fChecker.removeDictionary(fUserDictionary);
                fUserDictionary = null;
            }
            final String file = (String) event.getNewValue();
            if (file.length() > 0) {
                try {
                    final URL url = new URL("file", null, file);
                    InputStream stream = url.openStream();
                    if (stream != null) {
                        try {
                            fUserDictionary = new PersistentSpellDictionary(url);
                            fChecker.addDictionary(fUserDictionary);
                        } finally {
                            stream.close();
                        }
                    }
                } catch (MalformedURLException exception) {
                } catch (IOException exception) {
                }
            }
        }
    }

    public final synchronized void registerDictionary(final ISpellDictionary dictionary) {
        fGlobalDictionaries.add(dictionary);
        if (fChecker != null) fChecker.addDictionary(dictionary);
    }

    public final synchronized void registerDictionary(final Locale locale, final ISpellDictionary dictionary) {
        fLocaleDictionaries.put(locale, dictionary);
        if (fChecker != null && fLocale != null && fLocale.equals(locale)) fChecker.addDictionary(dictionary);
    }

    public final synchronized void unload() {
        ISpellDictionary dictionary = null;
        for (final Iterator iterator = fGlobalDictionaries.iterator(); iterator.hasNext(); ) {
            dictionary = (ISpellDictionary) iterator.next();
            dictionary.unload();
        }
        for (final Iterator iterator = fLocaleDictionaries.values().iterator(); iterator.hasNext(); ) {
            dictionary = (ISpellDictionary) iterator.next();
            dictionary.unload();
        }
        if (fPreferences != null) fPreferences.removePropertyChangeListener(this);
        fUserDictionary = null;
        fChecker = null;
    }

    public final synchronized void unregisterDictionary(final ISpellDictionary dictionary) {
        fGlobalDictionaries.remove(dictionary);
        fLocaleDictionaries.values().remove(dictionary);
        if (fChecker != null) fChecker.removeDictionary(dictionary);
        dictionary.unload();
    }
}
