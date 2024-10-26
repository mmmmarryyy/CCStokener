package paolomind.commons.xml;

import java.beans.XMLDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import paolomind.commons.NamedObject;
import paolomind.commons.ObjectContainer;

/**
 * classe di ogetti che si preoccupano di leggere uno stream di dati in formato
 * XML e ne ricavo degli ogetti. Si consiglia nel file XML di aggiungere alla
 * fine il seguente codice per mappare gli ogetti desiderati:
 *
 * <pre>
 *   &lt;void property=&quot;owner&quot;&gt;
 *   &lt;void method=&quot;register&quot;&gt;
 *   &lt;string&gt;integer&lt;/string&gt;
 *   &lt;object idref=&quot;num&quot; /&gt;
 *   &lt;/void&gt;
 *   &lt;/void&gt;
 * </pre>
 *
 * <li>owner è questo stesso oggetto</li>
 * <li>register è il metodo per registrare gli oggetti</li>
 * <li>la prima stringa è l'argomento chiave,l'identificativo dell'oggetto</li>
 * <li>il secondo argomento è l'ogetto da mappare</li>
 *
 * @author paolo
 * @see java.beans.XMLDecoder,
 */
public class XMLObjectContainer implements ObjectContainer {

    /** */
    private Map objectmap;

    /** */
    private XMLListener listener;

    /** */
    private ClassLoader loader;

    /**
   * costruttore vuoto che inizializza la hashmap.
   */
    public XMLObjectContainer() {
        objectmap = new HashMap();
    }

    /**
   * costruttore che si occupa di interpretare lo stream di input.
   * @param in stream di input (uno stream con formato XML)
   * @param l il listner degli eventi
   */
    public XMLObjectContainer(final InputStream in, final XMLListener l) {
        this();
        listener = l;
        read(in);
    }

    /**
   * costruttore che si occupa di interpretare lo stream di input.
   * @param in stream di input (uno stream con formato XML)
   * @param l il listner degli eventi
   * @param cl il classloader da cui caricare le risorse
   */
    public XMLObjectContainer(final InputStream in, final XMLListener l, final ClassLoader cl) {
        this(l, cl);
        read(in);
    }

    /**
   * costruttore di inizializzazione.
   * @param l il listner degli eventi
   * @param cl il classloader da cui caricare le risorse
   */
    public XMLObjectContainer(final XMLListener l, final ClassLoader cl) {
        this();
        listener = l;
        this.loader = cl;
    }

    /**
   * interpreta da uno stream l'oggetto .
   * @param in lo stream da interpretare
   */
    public final void read(final InputStream in) {
        if (listener != null) {
            listener.startreading();
        }
        XMLDecoder xmldec = new XMLDecoder(in, this, listener);
        try {
            while (true) {
                Object o = xmldec.readObject();
                if (listener != null) {
                    listener.read(o);
                }
            }
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            if (listener != null) {
                listener.endreading();
            }
        }
    }

    /**
   * registra un ogetto associandolo alla chiave specificata.
   * @param name chiave
   * @param o valore
   */
    public final void register(final String name, final Object o) {
        objectmap.put(name, o);
        if (listener != null) {
            listener.objectRegister(name, o);
        }
    }

    /**
   * reperisce un elemento registrato.
   *
   * @param name
   *            nome dell'oggetto registrato
   * @return restituisce un ogetto registrato oppure null
   * @see paolomind.commons.ObjectContainer#get(java.lang.String)
   */
    public final Object get(final String name) {
        return objectmap.get(name);
    }

    /**
   * restituisce una risorsa ricavandola dal class loader.
   * @param name percosro alla risorsa relativo al classpath
   * @return lo stream aperto dalla risorsa
   * @see  java.lang.ClassLoader#getResourceAsStream(String)
   */
    public final InputStream getResourceAsStream(final String name) {
        ClassLoader cl;
        if (loader == null) {
            cl = this.getClass().getClassLoader();
        } else {
            cl = loader;
            cl.getResourceAsStream(name);
        }
        return getStreamFromUrl(cl.getResource(name));
    }

    /**
   * apre uno stream da una url specificata.
   * @param url url della risorsa
   * @return lo stream aperto dalla risorsa
   */
    public final InputStream getStreamFromUrl(final URL url) {
        try {
            if (listener != null) {
                listener.openedStream(url);
            }
            return url.openStream();
        } catch (IOException e) {
            listener.exceptionThrown(e);
            return null;
        }
    }

    /**
   * apre uno stream da una url specificata come una stringa.
   * @param url la risorsa
   * @return lo stream aperto dalla risorsa
   */
    public final InputStream getStreamFromUrl(final String url) {
        try {
            return getStreamFromUrl(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
   * restituisce tutti gli identificativi registrati.
   * @return tutti gli identificativi registrati
   */
    public final Iterator getIds() {
        return objectmap.keySet().iterator();
    }

    /**
   * registra un ogetto.
   *
   * @param element
   *            l'oggetto da registrare con il suo nome
   * @see paolomind.commons.ObjectContainer#register(paolomind.commons.NamedObject)
   */
    public final void register(final NamedObject element) {
        register(element.getSelfId(), element);
    }

    /**
   * questo metodo non è supportato da questa classe.
   * rilancia l'eccezione UnsupportedOperationException
   * @param name ho detto che non &egrave; supportato
   * @return ma ci insisti a continuare a leggere!!! non ritorna nulla!!!
   * @see paolomind.commons.ObjectContainer#select(java.lang.String)
   * @deprecated
   */
    public final boolean select(final String name) {
        throw new UnsupportedOperationException("Metodo select non supportato");
    }

    /**
   * restituisce tutti gli elementi.
   *
   * @return tutti gli elementi registrati
   * @see paolomind.commons.ObjectContainer#getAll()
   */
    public final Iterator getAll() {
        return objectmap.values().iterator();
    }

    /**
   * setta il listener per la gestione di eventi prevedibili.
   * @param plistener listener per la gestione di eventi prevedibili.
   */
    public final void setListener(final XMLListener plistener) {
        this.listener = plistener;
    }

    /**
   * setta il classloader per caricare risorse eventualmente richieste.
   * se null usa il proprio classloader.
   * @param ploader classloader per caricare risorse eventualmente richieste.
   */
    public final void setLoader(final ClassLoader ploader) {
        this.loader = ploader;
    }

    /**
   * restituisce la mappa degli oggetti registrati.
   * @return la mappa degli oggetti registrati
   */
    protected final Map getObjectmap() {
        return objectmap;
    }

    /**
   * setta la mappa degli oggetti registrati.
   * @param pobjectmap la mappa degli oggetti registrati
   */
    protected final void setObjectmap(final Map pobjectmap) {
        this.objectmap = pobjectmap;
    }

    /**
   * restituisce il listener associato.
   * @return il listener associato.
   */
    protected final XMLListener getListener() {
        return listener;
    }
}
