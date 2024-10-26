package net.sourceforge.esw.collection;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URL;
import java.net.URLConnection;

/**
 * Performs Transduction to or against a URL.
 * <p>
 *
 * This transduction occurs both ways; <code>put()</code> takes data
 * from the <code>IMetaCollection</code> instance to which this
 * <code>ITransducer</code> instance is attached and places it into the data
 * source represented by this <code>ITransducer</code> instance. The
 * <code>get()</code> method takes data from the data source represented by this
 * <code>ITransducer</code> instance and places it into the
 * <code>IMetaCollection</code> instance to which this <code>ITransducer</code>
 * instance is attached.
 * <p>
 *
 * This IIOTransducer instance provides stream-based access for stream-based
 * ITransducer implementors. It abstracts out the setting and getting of
 * the streams, or the readers and the writers, into this interface.
 * <p>
 *
 * This implementation of the URLTransducerAdapter delegates all
 * <code>get</code> and <code>put</code> method invocations to the contained
 * IIOTransducer instance. Also, this URLTransducerAdapter instance provides
 * convenience methods to transduce to or against a URL.
 * <p>
 *
 * Example:
 * <pre>
 *    IMetaCollection meta = MetaFactory.createMetaCollection();
 *    IIOTransducer transducer = createIOTransducer() // for whatever
 *                                                    // IIOTransducer instance
 *                                                    // you need to use.
 *    URLTransducerAdapter adapter = new URLTransducerAdapter( getURLStream(), // the URL data
 *                                                             transducer );
 *    meta.setTransducer( adapter ); // Notice it is the Adapter set on the
 *                                   // IMetaCollectsion instance!
 *    try {
 *      meta.get();
 *    }
 *    catch ( TransducerException te ) {
 *      te.printStackTrace(); // in case something went wrong.
 *    }
 * </pre>
 * <p>
 *
 */
public class URLTransducerAdapter extends ATransducerAdapter implements ITransducer {

    /**
   * The URL for the data this URLTransducerAdapter instance will transduce.
   */
    protected String url;

    /****************************************************************************
   *  Creates a new URLTransducerAdapter instance.
   */
    public URLTransducerAdapter() {
        this(null, null);
    }

    /****************************************************************************
   * Creates a new URLTransducerAdapter instance with the specified URL.
   *
   * @param aURL the String representing the URL this URLTransducerAdapter
   *             instance will transduce against.
   */
    public URLTransducerAdapter(String aURL) {
        this(aURL, null);
    }

    /****************************************************************************
   * Creates a new URLTransducerAdapter instance, delegating ITransducer
   * implemented method invocations to the specified IIOTransducer instance.
   *
   * @param aIIOTransducer the IIOTransducer instance to which to delegate.
   */
    public URLTransducerAdapter(IIOTransducer aIIOTransducer) {
        this(null, aIIOTransducer);
    }

    /****************************************************************************
   * Creates a new URLTransducerAdapter instance with the specified URL and
   * IIOTransducer instance to which to delegate ITransducer implemented
   * method invocations.
   *
   * @param aURL the String representing the URL this URLTransducerAdapter
   *             instance will transduce against.
   * @param aIIOTransducer the IIOTransducer instance to which to delegate.
   */
    public URLTransducerAdapter(String aURL, IIOTransducer aIIOTransducer) {
        super();
        try {
            setIOTransducer(aIIOTransducer);
            setURL(aURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /****************************************************************************
   * Sets the name of the URL this URLTransducerAdapter instance will transduce
   * against.
   *
   * @param aURL the String representing the URL this URLTransducerAdapter
   *             instance will transduce against.
   * @see #getURL()
   */
    public void setURL(String aURL) {
        url = aURL;
    }

    /****************************************************************************
   * Returns the URL for this URLTransducerAdapter instance.
   *
   * @see #setURL( String )
   */
    public String getURL() {
        return url;
    }

    /****************************************************************************
   * Gets the data source's data represented by this ITransducer instance into
   * the referenced IMetaCollection instance.
   * <p>
   *
   * This implementation creates a Reader instance on the previously specified
   * URL and sets the Reader instance on the previously specified
   * IIOTransducer instance, then calls <code>get</code> on the IIOTransducer
   * instance.
   *
   * @param aCollection the IMetaCollection instance into which to read.
   * @throws TransducerException if an error occurs during transduction.
   *
   * @see #put( IMetaCollection )
   */
    public void get(IMetaCollection aCollection) throws TransducerException {
        if (null != ioTransducer) {
            try {
                URL urlObj = new URL(url);
                URLConnection urlConn = urlObj.openConnection();
                InputStreamReader inr = new InputStreamReader(urlConn.getInputStream());
                ioTransducer.setReader(new BufferedReader(inr));
                ioTransducer.get(aCollection);
            } catch (Exception e) {
                throw new TransducerException(e);
            }
        } else {
            throw new TransducerException("An IIOTransducer instance must first be set on the URLTransducerAdapter.");
        }
    }

    /****************************************************************************
   * Puts the referenced IMetaCollection instance's data into the data source
   * represented by this ITransducer instance.
   * <p>
   *
   * This implementation creates a Writer instance from the URL and sets the
   * Writer instance on the previously specified IIOTransducer instance, then
   * calls <code>get</code> on the IIOTransducer instance.
   *
   * @param aCollection the IMetaCollection instance from which to take data.
   * @throws TransducerException if an error occurs during transduction.
   *
   * @see #get( IMetaCollection )
   */
    public void put(IMetaCollection aCollection) throws TransducerException {
        if (null != ioTransducer) {
            try {
                URL urlObj = new URL(url);
                URLConnection urlConn = urlObj.openConnection();
                OutputStreamWriter sw = new OutputStreamWriter(urlConn.getOutputStream());
                ioTransducer.setWriter(new BufferedWriter(sw));
                ioTransducer.put(aCollection);
            } catch (Exception e) {
                throw new TransducerException(e);
            }
        } else {
            throw new TransducerException("An IIOTransducer instance must first be set on the URLTransducerAdapter.");
        }
    }
}
