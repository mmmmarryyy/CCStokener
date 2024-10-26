package com.mindtree.techworks.insight.receiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import com.mindtree.techworks.insight.spi.LogEvent;
import com.mindtree.techworks.insight.spi.LogNamespace;

/**
*
* The <code>FileStreamReceiver</code> class is a concrete implementation of the
* AbstractReceiver class. It supports receiving and building events from 
* files.
* @author  Regunath B
* @version 1.0, 05/07/07
* @see com.mindtree.techworks.insight.receiver.AbstractReceiver
*/
public class FileStreamReceiver extends AbstractReceiver {

    /**
	 * The LogNamespace for this receiver
	 */
    private LogNamespace logNamespace;

    /**
	 * The tailing indicator for this receiver
	 */
    private boolean isTailing;

    /**
	 * The Interpreter of String to LogEvents which we get by reader.
	 */
    private LogInterpreter logInterpreter = null;

    /**
	 * Reader instance which is being used for reading the file. 
	 */
    private BufferedReader reader;

    /**
	 * Indicator to demote if end of stream has been reached. This flag depends on whether the reader has reached
	 * end of stream. However, this flag is set to true one call later than actual end signalled by the reader to
	 * permit the Loginterpreter to interpret any last events if they exist
	 */
    private boolean hasReachedEOS = false;

    /**
	 * Constructor for this class.
	 * @param namespace the LogNamespace that contains the file information to read events from 
	 * @param isTailing true if the receiver must be a tailing receiver
	 * @throws ReceiverInitializationException if the receiver is not able to initialize.
	 */
    public FileStreamReceiver(LogNamespace namespace, boolean isTailing) throws ReceiverInitializationException {
        initialize(namespace, isTailing);
    }

    /**
	 * Overriden superclass method
	 * @see com.mindtree.techworks.insight.receiver.AbstractReceiver#getNamespaces()
	 */
    public LogNamespace[] getNamespaces() {
        return new LogNamespace[] { this.logNamespace };
    }

    /**
	 * Overriden superclass method
	 * @see com.mindtree.techworks.insight.receiver.AbstractReceiver#isTailing()
	 */
    public boolean isTailing() {
        return this.isTailing;
    }

    /**
	 * Overriden superclass method
	 * @see com.mindtree.techworks.insight.receiver.AbstractReceiver#initialize(com.mindtree.techworks.insight.spi.LogNamespace, boolean)
	 */
    protected void initialize(LogNamespace namespace, boolean isTailingReceiver) throws ReceiverInitializationException {
        this.logNamespace = namespace;
        this.isTailing = isTailingReceiver;
        logInterpreter = new LogInterpreter(namespace);
        String url = namespace.getLocalSourceString();
        url = url.replaceAll("#", "%23");
        try {
            reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        } catch (IOException ioe) {
            throw new ReceiverInitializationException("Unable to load : " + (url == null ? null : url.toString()), ioe);
        }
    }

    /**
	 * Overriden superclass method
	 * @see com.mindtree.techworks.insight.receiver.AbstractReceiver#deInitialize()
	 */
    protected void deInitialize() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
	 * Overriden superclass method
	 * @see com.mindtree.techworks.insight.receiver.AbstractReceiver#getNextEvents()
	 */
    protected LogEvent[] getNextEvents() {
        LogEvent[] events = null;
        String line = null;
        try {
            line = reader.readLine();
        } catch (IOException ioe) {
            return events;
        }
        return this.logInterpreter.parseLogMessage(line);
    }

    /**
	 * Overriden superclass method
	 * @see com.mindtree.techworks.insight.receiver.AbstractReceiver#hasMoreEvents()
	 */
    protected boolean hasMoreEvents() {
        boolean hasData = false;
        try {
            hasData = reader.ready();
            if (!hasData) {
                if (hasReachedEOS) {
                    return hasData;
                } else {
                    hasReachedEOS = true;
                    return true;
                }
            }
        } catch (IOException ioe) {
        }
        return hasData;
    }
}
