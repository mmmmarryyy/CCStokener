package org.indi.server;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.PropertyResourceBundle;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.indi.clientmessages.GetProperties;
import org.indi.objects.TransferType;
import org.indi.objects.Vector;
import org.indi.reactor.EventHandler;
import org.indi.reactor.EventHandlerFactory;
import org.indi.reactor.QueueHandler;
import org.indi.reactor.Reactor;
import org.indi.reactor.TimerHandler;
import org.xml.sax.SAXException;

/**
 * An Indiserver to host drivers
 * 
 * @author Dirk Hünniger
 * 
 */
public class IndiServer implements QueueHandler {

    private final Log log = LogFactory.getLog(IndiServer.class);

    /**
         * A collection of devicedrivers hosted by this server
         */
    public Collection<DeviceConnection> deviceconnections;

    /**
         * A collection of clients connected to this server
         */
    public Collection<ClientHandler> clientHandlers;

    /**
         * the reactor used by this server for event dispatching
         */
    public Reactor reactor;

    /**
         * the version of the indisever
         */
    public String version = "1.0";

    /**
         * the set of registered oberservers observing changes of a particular
         * property of a particular driver
         */
    public Set<Observer> observers;

    /**
         * event handler that handles the newly connecting clients.
         */
    private Acceptor acceptor;

    /**
     * A dispatcher dispatching information received from the cleints to the drivers.
     */
    private Dispatcher dispatcher;

    /**
     * A queue for device to write messages to this server
     */
    private BlockingQueue<Object> deviceToServerQueue;

    public IndiServer() throws IOException {
        init(new Reactor());
    }

    /**
         * class constructor
         * 
         * @throws IOException
         */
    public IndiServer(Reactor r) throws IOException {
        init(r);
    }

    public void init(Reactor r) throws IOException {
        this.observers = new HashSet<Observer>();
        this.reactor = r;
        Queue<Object> toDriverQueue = new LinkedBlockingQueue<Object>();
        this.deviceconnections = new ArrayList<DeviceConnection>();
        this.dispatcher = new Dispatcher(this);
        this.acceptor = new Acceptor(this.reactor, new EventHandlerFactory() {

            public EventHandler produce(Reactor r, SelectableChannel ch) throws ClosedChannelException, IOException {
                try {
                    return new ClientHandler(reactor, ch);
                } catch (ParserConfigurationException e) {
                    log.error("Could not start indi server", e);
                } catch (SAXException e) {
                    log.error("Could not start indi server", e);
                }
                return null;
            }
        }, 7624, this);
        deviceToServerQueue = new LinkedBlockingQueue<Object>();
        reactor.register(this);
        this.clientHandlers = acceptor.getClientHandlers();
    }

    public BlockingQueue<Object> getQueue() {
        return deviceToServerQueue;
    }

    /**
         * add a new driver to the server
         * 
         * @param driver
         *                the driver to be added
         */
    public void addDevice(Device device) {
        DeviceConnection c = new ThreadedDeviceConnection(device, this);
        device.setConnection(c);
        this.deviceconnections.add(c);
    }

    /**
     * To be called when a device threw a exception
     * @param device the device that threw the exception
     * @param e the exception thrown by the device
     */
    public void bogusDevice(BogusDevice d) {
        this.log.error("Error in Driver " + d.connection.getDevice().getClass().getName() + "\"", d.exception);
        this.deviceconnections.remove(d.connection);
    }

    /**
         * send an indiobject to all interested clients
         * 
         * @param object
         *                the indiobject to be send
         * @param type
         *                the way it should be send
         * @param message
         *                the message to be sent along with the object
         */
    public void sendToClients(org.indi.objects.Object object, TransferType type, String message) {
        for (ClientHandler ch : this.clientHandlers) {
            ch.send(object, type, message);
        }
        if (object instanceof Vector) {
            Vector vector = (Vector) object;
            vector.setTransferType(type);
            for (Observer o : this.observers) {
                if (vector.getDevice().equals(o.getDevice())) {
                    if (o.getName().equals(vector.getName()) || vector.getTransferType() == TransferType.Del) {
                        if ((vector.getTransferType() == TransferType.Set) && o.getState() == ObserverState.State) {
                            if (o.laststate == vector.getState()) {
                                return;
                            } else {
                                o.laststate = vector.getState();
                            }
                        }
                        o.onObserved(vector);
                    }
                }
                ;
            }
        }
    }

    public void shutDown() {
        for (ClientHandler ch : this.clientHandlers) {
            ch.shutDown();
        }
        try {
            this.acceptor.channel().close();
        } catch (IOException e) {
            log.error("could not close connection on server shutdown.", e);
        }
    }

    /**
         * Main server start routine.
         * 
         * @param args
         *                command line arguments with a array of devices names
         *                or classes to start in the server.
         */
    public static void main(String[] args) {
        try {
            new IndiServer().startServer(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
         * Start the indi server and add the devices specified in the arguments.
         * 
         * @param args
         *                command line arguments with a array of devices names
         *                or classes to start in the server.
         */
    public void startServer(String[] args) {
        try {
            ArrayList<String> deveiceToStart = new ArrayList<String>();
            nameScanForDevicesToStart(args, deveiceToStart);
            classNameScanForDevicesToStart(args, deveiceToStart);
            if (!startAtLeastOneDevice(deveiceToStart)) {
                this.log.error("no drivers successfully started!");
            } else {
                handleEvents();
            }
        } catch (IOException e) {
            this.log.error("could not start indi server due to exception", e);
        }
    }

    protected void handleEvents() throws IOException {
        while (true) {
            this.reactor.handleEvents(10);
        }
    }

    private void classNameScanForDevicesToStart(String[] args, ArrayList<String> devicesToStart) {
        for (String element : args) {
            if (element.length() > 0) {
                try {
                    Class driverClass = Thread.currentThread().getContextClassLoader().loadClass(element);
                    if (driverClass.getClass().getSuperclass().isAssignableFrom(BasicDevice.class)) {
                        devicesToStart.add(element);
                    } else {
                        this.log.error("argument is not a driver or driver class (" + element + ")");
                    }
                } catch (ClassNotFoundException e) {
                    this.log.error("argument is not a driver class (" + element + ")", e);
                }
            }
        }
    }

    private void nameScanForDevicesToStart(String[] args, ArrayList<String> driversToStart) throws IOException {
        Enumeration<URL> driverresources = Thread.currentThread().getContextClassLoader().getResources("META-INF/indi");
        while (driverresources.hasMoreElements()) {
            URL url = driverresources.nextElement();
            PropertyResourceBundle bundle = new PropertyResourceBundle(url.openStream());
            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String driverClassName = keys.nextElement();
                String driverName = bundle.getString(driverClassName);
                boolean driverActivated = false;
                for (int index = 0; index < args.length; index++) {
                    if (args[index].equals(driverName)) {
                        driversToStart.add(driverClassName);
                        driverActivated = true;
                        args[index] = "";
                    }
                }
                if (!driverActivated) {
                    this.log.info("detected deacivated driver for " + driverName + " (" + driverClassName + ")");
                } else {
                    this.log.info("detected acivated driver for " + driverName + " (" + driverClassName + ")");
                }
            }
        }
    }

    /**
         * 
         * @param driversToStart
         * @return
         */
    private boolean startAtLeastOneDevice(ArrayList<String> driversToStart) {
        boolean atLeastOnDriverStarted = false;
        for (String driverClassName : driversToStart) {
            try {
                Class driverClass = Thread.currentThread().getContextClassLoader().loadClass(driverClassName);
                addDevice((BasicDevice) driverClass.getConstructor().newInstance());
                this.log.info("Successfuly started driver class " + driverClassName);
                atLeastOnDriverStarted = true;
            } catch (Exception e) {
                this.log.error("could not instanciate Driver: " + driverClassName + " due to exception", e);
            }
        }
        return atLeastOnDriverStarted;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void bogusClient(ClientHandler client, Exception e) {
        this.log.error("Error while reading or processing data from client ", e);
        reactor.unregister(client);
        this.clientHandlers.remove(client);
    }

    public void onRead(Object input) {
        if (input instanceof BogusDevice) {
            bogusDevice((BogusDevice) input);
        }
        if (input instanceof IndiTransfer) {
            IndiTransfer t = (IndiTransfer) input;
            sendToClients(t.object, t.type, t.message);
        }
    }
}
