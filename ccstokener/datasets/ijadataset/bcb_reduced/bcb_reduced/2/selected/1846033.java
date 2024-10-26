package test.org.mbari.observatory.client;

import static org.junit.Assert.assertTrue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mbari.observatory.client.JMSClientFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * This class simply tests if an HTTP message can be routed correctly to a JMS
 * message queue inside the ESB. The ServiceMix will need to be setup using the
 * build.xml file in the project (specifically the target to deploy test
 * resources). Once these test resources are deployed, this test can be run and
 * it will check if an external client can send and HTTP message, subscribe to
 * an output queues, and pick the message up off the output queue.
 * 
 * @author kgomes
 * 
 */
public class TestHTTPJMSRoundTrip {

    /**
	 * A log4j Logger
	 */
    private static Logger logger = Logger.getLogger(TestHTTPJMSRoundTrip.class);

    /**
	 * These are properties that are read from the test resources to configure
	 * the test
	 */
    private Properties testProperties = null;

    private final String testMessageHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    private final String testMessage = "<e:Envelope xmlns:e=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + "  <e:Body>\n" + "    <ping>\n" + "      <pingRequest>\n" + "        <message xmlns=\"http://soap\">hello at " + (new Date()) + "</message>\n" + "      </pingRequest>\n" + "    </ping>\n" + "  </e:Body>\n" + "</e:Envelope>";

    /**
	 * This is the JmsTemplate where the test will look for replies
	 */
    private JmsTemplate outputJmsTemplate = null;

    /**
	 * The object that will listen for messages
	 */
    private TestJMSListener testJMSListener = null;

    @Before
    public void setUp() throws Exception {
        logger.debug("setUp called");
        testProperties = new Properties();
        try {
            testProperties.load(this.getClass().getResourceAsStream("/test.properties"));
        } catch (Exception e) {
            logger.error("Exception caught trying to read properties:" + e.getMessage());
            throw e;
        }
        logger.debug("Loaded test properties: " + testProperties);
        outputJmsTemplate = JMSClientFactory.createActiveMQJmsTemplate(testProperties.getProperty("test.http.jms.roundtrip.messaging.protocol"), testProperties.getProperty("test.http.jms.roundtrip.messaging.hostname"), testProperties.getProperty("test.http.jms.roundtrip.messaging.port.number"), testProperties.getProperty("test.http.jms.roundtrip.output.queue.name"));
        logger.debug("Created outputJmsTemplate");
        this.testJMSListener = new TestJMSListener(outputJmsTemplate);
        this.testJMSListener.start();
    }

    @Test
    public void testRoundTrip() {
        try {
            URL url = new URL("http://localhost:8192/OMFHTTPJMSRoundtripService/");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(testMessageHeader + testMessage);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                logger.debug("Line: " + line);
            }
            wr.close();
            rd.close();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            logger.error("InterruptedException caught trying to sleep test: " + e.getMessage());
        }
        boolean messageReceived = false;
        ArrayList<String> messages = testJMSListener.getReceivedMessages();
        for (Iterator<String> iterator = messages.iterator(); iterator.hasNext(); ) {
            String string = (String) iterator.next();
            logger.debug("String received: " + string);
            if (testMessage.equals(string)) messageReceived = true;
        }
        assertTrue("Message should have been received", messageReceived);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
	 * This is an inner class that will be used to monitor a JMS queue for
	 * messages
	 * 
	 * @author kgomes
	 * 
	 */
    class TestJMSListener extends Thread {

        /**
		 * This is the JmsTemplate that will be monitored for messages
		 */
        private JmsTemplate jmsTemplateToListenTo;

        /**
		 * This is the collection of strings that are the messages that have
		 * been received on the monitored queue
		 */
        private ArrayList<String> receivedMessages;

        /**
		 * This is the constructor that takes in the JmsTemplate to listen to.
		 * 
		 * @param observatoryClientJmsTemplate
		 */
        public TestJMSListener(JmsTemplate jmsTemplateToListenTo) {
            logger.debug("TestJMSRoundTripListener constructor called");
            this.jmsTemplateToListenTo = jmsTemplateToListenTo;
            receivedMessages = new ArrayList<String>();
        }

        /**
		 * This method returns the array list of the messages received to date
		 * 
		 * @return
		 */
        public ArrayList<String> getReceivedMessages() {
            return receivedMessages;
        }

        /**
		 * This is the method that is run when 'start' is called on the Thread
		 */
        public void run() {
            logger.debug("Beginning to run ...");
            while (true) {
                Object message = jmsTemplateToListenTo.receiveAndConvert();
                logger.info("A message was recieved");
                if (message instanceof String) {
                    receivedMessages.add((String) message);
                    logger.debug("Got message and stored in collection:\n" + message);
                } else {
                    logger.debug("Got a message, but it was " + "not a string, so ignored");
                }
            }
        }
    }
}
