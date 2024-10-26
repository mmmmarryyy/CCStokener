package perun.isle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.rmi.RemoteException;
import perun.common.DataExchangeSupport;
import perun.common.DataExchangeSupport.Tag;
import perun.common.log.Log;
import perun.isle.lifespace.PositionInfo2DLattice;
import perun.isle.virtualmachine.GeneticCodeTape;
import perun.isle.virtualmachine.VirtualMachineFactory;

/**
 * This singleton class provide ability to perform data transfers
 * between Isle and anyone connected, probably Client
 */
public class DataExchangeServer extends Thread {

    /** DataExchangeServer will try to listen on this port on request */
    public static final int DEFAULT_PORT = 6789;

    private static final DataExchangeServer instance = new DataExchangeServer();

    private final ServerSocket socket;

    public static DataExchangeServer getInstance() {
        return instance;
    }

    private DataExchangeServer() {
        super("data exchange server");
        ServerSocket s = null;
        try {
            s = new ServerSocket(DEFAULT_PORT);
        } catch (IOException ioe) {
            try {
                s = new ServerSocket(0);
            } catch (IOException ioe1) {
                Log.exception(Log.ERROR, "Error starting data exchange server", ioe1);
            }
        }
        socket = s;
        if (socket != null) {
            Log.event(Log.INFO, "Injection server listening on port " + socket.getLocalPort());
            setDaemon(true);
            this.start();
        }
    }

    public void run() {
        try {
            while (true) {
                new Connection(socket.accept());
            }
        } catch (IOException ioe) {
            Log.exception(Log.ERROR, "Error while listening for a connection", ioe);
        }
    }

    /** Returns DataExchangeServer listening port, always. */
    public static int getLocalPort() {
        return (getInstance() != null) ? getInstance().socket.getLocalPort() : -1;
    }

    class Connection implements Runnable {

        private final Socket socket;

        private final DataExchangeSupport data;

        Connection(Socket s) {
            DataExchangeSupport d = null;
            socket = s;
            try {
                d = new DataExchangeSupport(socket, true);
                Log.event(Log.DEBUG_INFO, "New connection established, opening input stream");
            } catch (IOException ioe) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.exception(Log.VERBOSE, e);
                }
                Log.exception(Log.ERROR, "Error while establishing a connection", ioe);
            }
            data = d;
            if (data != null) new Thread(this).start();
        }

        public void run() {
            try {
                data.readTag(Tag.START);
                Tag t = data.getTag();
                Log.event(Log.DEBUG_INFO, "requested operation: " + t.getString());
                if (t == Tag.UNKNOWN) return;
                int x = (int) data.getNumber(Tag.XORIG);
                int y = (int) data.getNumber(Tag.YORIG);
                Log.event(Log.DEBUG_INFO, "at position: [" + x + ", " + y + "]");
                if (t == Tag.INJECT) inject(x, y); else if (t == Tag.EXTRACT) extract(x, y); else data.writeString("unknown type of transaction: " + t.getString());
            } catch (IOException ioe) {
                Log.exception(Log.WARNING, "Exception while receiving data from the client", ioe);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.exception(Log.VERBOSE, e);
                }
            }
        }

        private void inject(int x, int y) throws IOException {
            Tag source = data.getTag();
            Log.event(Log.DEBUG_INFO, "source: " + source.getString());
            if (source == Tag.ORGANISM) {
                String seed = data.readString();
                data.readTag(Tag.STREAM);
                injectCodeTape(data.getIn(), seed, x, y);
            } else if (source == Tag.URL) {
                String url = data.readString();
                String seed = url.substring(url.lastIndexOf('.') + 1);
                BufferedReader urlIn = null;
                try {
                    urlIn = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
                    injectCodeTape(urlIn, seed, x, y);
                } finally {
                    if (urlIn != null) urlIn.close();
                }
            } else data.writeString("unknown organism source: " + source.getString());
        }

        private void extract(int x, int y) throws RemoteException, IOException {
            data.writeTag(Tag.STREAM);
            DigitalOrganism digorg = Isle.getPopulation().getOrganism(new PositionInfo2DLattice(x, y));
            String type = digorg.getVMType();
            GeneticCodeTape ct = digorg.getVM().getCodeTape(0);
            data.writeString("# " + type);
            data.writeString(ct.toString());
        }

        /** Turn injected codetape into living organism */
        private void injectCodeTape(BufferedReader in, String type, int x, int y) throws IOException {
            try {
                GeneticCodeTape ct = VirtualMachineFactory.getInstance().getCodeTape(type, in);
                Population pop = Isle.getPopulation();
                pop.getNewOrganism(-1, new PositionInfo2DLattice(x, y), ct);
                Isle.getInstance().incTimestamp();
                data.writeTag(Tag.OK);
            } catch (Exception e) {
                data.writeString(e.toString());
            }
        }
    }
}
