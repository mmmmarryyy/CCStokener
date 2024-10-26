package keyboardhero;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import net.sbbi.upnp.Discovery;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import keyboardhero.Game.*;
import keyboardhero.Util.*;

final class Connection {

    static final class Client {

        private static final UUID ID = UUID.randomUUID();

        private final Socket socket;

        private InetSocketAddress address = null;

        private boolean stayOpen = true;

        private UUID id = null;

        private String player = "";

        private String playerTrimmed = "";

        private int score;

        private int activity;

        private String title = "";

        Client(Socket socket) {
            this.socket = socket;
            if (Util.getDebugLevel() > 30) Util.debug("ADDRESS: " + address);
        }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (o instanceof Client) {
                return socket.equals(((Client) o).socket);
            }
            return false;
        }

        public String toString() {
            return "Client (id=" + id + "; address=" + address + ")";
        }

        final Socket getSocket() {
            return socket;
        }

        final String getPlayer() {
            return player;
        }

        final String getPlayerTrimmed() {
            return playerTrimmed;
        }

        final int getScore() {
            return score;
        }

        final String getTitle() {
            return title;
        }

        final int getActivity() {
            return activity;
        }

        final UUID getId() {
            return id;
        }

        void setAddress(int port) {
            if (Util.getDebugLevel() > 30) Util.debug("PORT: " + port + " | " + address);
            if (port < 0 || port > 65535) {
                address = null;
                return;
            }
            SocketAddress addr = socket.getRemoteSocketAddress();
            if (addr instanceof InetSocketAddress) {
                InetSocketAddress a = (InetSocketAddress) addr;
                address = new InetSocketAddress(a.getAddress(), port);
            }
            synchronized (clients) {
                Iterator<Client> iterator = clients.iterator();
                while (iterator.hasNext()) {
                    final Client client = iterator.next();
                    if (client.socket.isClosed()) {
                        iterator.remove();
                        continue;
                    }
                    if (client.stayOpen && !equals(client) && address.equals(client.address)) {
                        stayOpen = false;
                        final Client thisClient = this;
                        (new Thread() {

                            public void run() {
                                try {
                                    Thread.sleep(Util.RAND.nextInt(29000) + 1000);
                                    boolean notFound = true;
                                    synchronized (clients) {
                                        Iterator<Client> iterator = clients.iterator();
                                        while (iterator.hasNext()) {
                                            final Client client = iterator.next();
                                            if (client.socket.isClosed()) {
                                                iterator.remove();
                                                continue;
                                            }
                                            if (client.stayOpen && !equals(client) && address.equals(client.address)) {
                                                notFound = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (notFound) {
                                        stayOpen = true;
                                    } else {
                                        clients.remove(thisClient);
                                        writeClient(thisClient, 'x');
                                        try {
                                            thisClient.socket.close();
                                        } catch (IOException e) {
                                            if (Util.getDebugLevel() > 90) e.printStackTrace();
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    if (Util.getDebugLevel() > 90) e.printStackTrace();
                                }
                            }
                        }).start();
                        break;
                    }
                }
            }
        }

        void setId(final UUID id) {
            if (Util.getDebugLevel() > 30) Util.debug("UUID: " + id);
            if (id == null) {
                this.id = null;
                return;
            }
            this.id = id;
            synchronized (clients) {
                Iterator<Client> iterator = clients.iterator();
                while (iterator.hasNext()) {
                    final Client client = iterator.next();
                    if (client.socket.isClosed()) {
                        iterator.remove();
                        continue;
                    }
                    if (client.stayOpen && !equals(client) && id.equals(client.id)) {
                        stayOpen = false;
                        final Client thisClient = this;
                        (new Thread() {

                            public void run() {
                                try {
                                    Thread.sleep(Util.RAND.nextInt(29000) + 1000);
                                    boolean notFound = true;
                                    synchronized (clients) {
                                        Iterator<Client> iterator = clients.iterator();
                                        while (iterator.hasNext()) {
                                            final Client client = iterator.next();
                                            if (client.socket.isClosed()) {
                                                iterator.remove();
                                                continue;
                                            }
                                            if (client.stayOpen && !equals(client) && id.equals(client.id)) {
                                                notFound = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (notFound) {
                                        stayOpen = true;
                                    } else {
                                        clients.remove(thisClient);
                                        writeClient(thisClient, 'x');
                                        try {
                                            thisClient.socket.close();
                                        } catch (IOException e) {
                                            if (Util.getDebugLevel() > 90) e.printStackTrace();
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    if (Util.getDebugLevel() > 90) e.printStackTrace();
                                }
                            }
                        }).start();
                        break;
                    }
                }
            }
        }
    }

    /**
	 * The number of players should stored in the local toplist. At maximum this many player will be
	 * stored and displayed in the local toplist.
	 * 
	 * @my.val {@value}
	 */
    private static final int MAX_IN_TOP = 5;

    private static final int MAX_PLAYER_LENGTH = 70;

    /** The url of the game's server used globally. */
    static final String SERVER_URL = "http://keyboardhero.co.cc/";

    static final String URL_STR = SERVER_URL + "kbh.php";

    static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.0.6) Gecko/2009011913 Firefox/3.0.6 ";

    private static final int CHECK_TIME = 300000;

    private static final int DISCONNECT_CHECK_TIME = 10000;

    private static final int GATEWAY_TIMEOUT = 2000;

    private static final int MULTICAST_RECEIVING_PORT = 64642;

    private static final int MULTICAST_SENDING_PORT = 64643;

    private static final int MULITCAST_TIME_TO_LIVE = 5;

    private static final InetAddress MULTICAST_GROUP;

    static {
        InetAddress group = null;
        try {
            group = InetAddress.getByName("239.255.255.250");
        } catch (UnknownHostException e) {
        }
        MULTICAST_GROUP = group;
    }

    private static final byte[] MULTICAST_ID = (KeyboardHero.APP_NAME + ": ").getBytes();

    private static final int MULTICAST_WAIT_FOR_OTHER_PARTY_TO_CONNECT = 5000;

    private static final Status PORT_IN_USE = new Status("Err_PortInUse", Status.ERROR, 200);

    private static final Status CANT_CHECK_CLIENTS = new Status("Err_CantCheckClients_Proxy", Status.ERROR, 225);

    private static final Status UPD_CANTAUTO = new Status("Err_CannotAutoUpdate", Status.ERROR, 280);

    private static final Status UPD_CANTAUTOFILE = new Status("Err_CannotAutoUpdateFile", Status.ERROR, 290);

    private static final Status UPD_OK = new Status("YesUpdate", Status.INFORMATION, 270);

    private static final Status UPD_START = new Status("UpdateStart", Status.INFORMATION, 260);

    private static final Font PLAYERS_FONT = new Font(Font.SANS_SERIF, Font.ITALIC, 9);

    private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(new AffineTransform(), true, false);

    private static final int DOTS_LENGTH = getStringWidth("...");

    private static Proxy proxy = null;

    private static boolean wasUpdate = false;

    private static MulticastSocket multiSocket = null;

    private static ServerSocket server = null;

    private static Vector<Client> clients = new Vector<Client>();

    private static boolean threadRun = false;

    private static Thread thread = null, thread2 = null, thread3 = null;

    static synchronized void startListening() {
        final int oldPort = stopListening(true);
        try {
            server = new ServerSocket(Util.getPropInt("connPort"));
            newConnection();
            newPortMappings(oldPort, Util.getPropInt("connPort"));
        } catch (BindException e) {
            Util.conditionalError(PORT_IN_USE, "Err_PortInUse");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            multiSocket = new MulticastSocket(MULTICAST_RECEIVING_PORT);
            multiSocket.setTimeToLive(MULITCAST_TIME_TO_LIVE);
            multiSocket.setSoTimeout(0);
            multiSocket.joinGroup(MULTICAST_GROUP);
            thread3 = new Thread() {

                public void run() {
                    while (threadRun) {
                        try {
                            byte[] buff = new byte[1000];
                            DatagramPacket message = new DatagramPacket(buff, buff.length);
                            multiSocket.receive(message);
                            parseMultiInput(message);
                        } catch (SocketException e) {
                            if (Util.getDebugLevel() > 158) e.printStackTrace();
                        } catch (IOException e) {
                            if (Util.getDebugLevel() > 78) e.printStackTrace();
                        }
                    }
                }
            };
            thread3.start();
        } catch (IOException e) {
            if (Util.getDebugLevel() > 52) e.printStackTrace();
        }
        threadRun = true;
        thread = new Thread() {

            public void run() {
                try {
                    while (threadRun) {
                        checkClients();
                        Thread.sleep(CHECK_TIME);
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        thread.start();
        thread2 = new Thread() {

            public void run() {
                try {
                    while (threadRun) {
                        Thread.sleep(DISCONNECT_CHECK_TIME);
                        synchronized (clients) {
                            Iterator<Client> iterator = clients.iterator();
                            while (iterator.hasNext()) {
                                final Client client = iterator.next();
                                if (client.socket.isClosed()) {
                                    clients.remove(client);
                                    continue;
                                }
                                writeClient(client, 'n', null, null, iterator);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        thread2.start();
    }

    static int stopListening() {
        return stopListening(false);
    }

    static int stopListening(boolean fromStart) {
        int oldPort = -1;
        KeyboardHero.removeStatus(PORT_IN_USE);
        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                final Client client = iterator.next();
                if (!client.socket.isClosed()) {
                    writeClient(client, 'x', null, null, iterator);
                    try {
                        client.socket.close();
                    } catch (IOException e) {
                        if (Util.getDebugLevel() > 90) e.printStackTrace();
                    }
                }
            }
        }
        if (server != null) {
            try {
                if (!server.isClosed()) {
                    final int port;
                    if (Util.getPropInt("connPort") != (port = server.getLocalPort())) {
                        oldPort = port;
                    }
                    server.close();
                }
            } catch (Exception e) {
                if (Util.getDebugLevel() > 90) e.printStackTrace();
            }
            server = null;
        }
        clients.clear();
        threadRun = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (thread2 != null) {
            thread2.interrupt();
            thread2 = null;
        }
        if (thread3 != null) {
            thread3.interrupt();
            thread3 = null;
        }
        if (multiSocket != null) {
            multiSocket.close();
            multiSocket = null;
        }
        if (!fromStart) {
            newPortMappings(oldPort, -1);
            newPortMappings(Util.getPropInt("connPort"), -1);
        }
        return oldPort;
    }

    private static void newConnection() {
        (new Thread() {

            public void run() {
                try {
                    final Socket socket = server.accept();
                    if (Util.getDebugLevel() > 30) Util.debug("Client Connected: " + socket.getRemoteSocketAddress());
                    newConnection();
                    synchronized (clients) {
                        for (Client client : clients) {
                            if (client.socket.isClosed()) {
                                clients.remove(client);
                                continue;
                            }
                        }
                    }
                    socket.setKeepAlive(true);
                    final Client client = new Client(socket);
                    clients.add(client);
                    writeClient(client, 'q');
                    if (Util.getDebugLevel() > 30) Util.debug("Client Alive");
                    parseInput(client);
                } catch (SocketException e) {
                    if (Util.getDebugLevel() > 68) {
                        if (!e.getMessage().equals("socket closed")) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    if (Util.getDebugLevel() > 90) e.printStackTrace();
                }
            }
        }).start();
    }

    private static void checkClients() {
        try {
            sendMultiListEntry('l');
        } catch (Exception e) {
            if (Util.getDebugLevel() > 90) e.printStackTrace();
        }
        try {
            if (CANT_CHECK_CLIENTS != null) KeyboardHero.removeStatus(CANT_CHECK_CLIENTS);
            URL url = new URL(URL_STR + "?req=clients" + (server != null ? "&port=" + server.getLocalPort() : ""));
            URLConnection connection = url.openConnection(getProxy());
            connection.setRequestProperty("User-Agent", USER_AGENT);
            BufferedReader bufferedRdr = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String ln;
            if (Util.getDebugLevel() > 30) Util.debug("URL: " + url);
            while ((ln = bufferedRdr.readLine()) != null) {
                String[] parts = ln.split(":", 2);
                if (parts.length < 2) {
                    Util.debug(12, "Line read in checkClients: " + ln);
                    continue;
                }
                try {
                    InetSocketAddress address = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
                    boolean notFound = true;
                    if (Util.getDebugLevel() > 25) Util.debug("NEW Address: " + address.toString());
                    synchronized (clients) {
                        Iterator<Client> iterator = clients.iterator();
                        while (iterator.hasNext()) {
                            final Client client = iterator.next();
                            if (client.socket.isClosed()) {
                                iterator.remove();
                                continue;
                            }
                            if (Util.getDebugLevel() > 26 && client.address != null) Util.debug("Address: " + client.address.toString());
                            if (address.equals(client.address)) {
                                notFound = false;
                                break;
                            }
                        }
                    }
                    if (notFound) {
                        connectClient(address);
                    }
                } catch (NumberFormatException e) {
                }
            }
            bufferedRdr.close();
        } catch (MalformedURLException e) {
            Util.conditionalError(PORT_IN_USE, "Err_PortInUse");
            Util.error(Util.getMsg("Err_CantCheckClients"));
        } catch (FileNotFoundException e) {
            Util.error(Util.getMsg("Err_CantCheckClients_Proxy"), Util.getMsg("Err_FileNotFound"));
        } catch (SocketException e) {
            Util.error(Util.getMsg("Err_CantCheckClients_Proxy"), e.getLocalizedMessage());
        } catch (Exception e) {
            CANT_CHECK_CLIENTS.setException(e.toString());
            KeyboardHero.addStatus(CANT_CHECK_CLIENTS);
        }
    }

    static void connectClient(final InetSocketAddress address) {
        (new Thread() {

            public void run() {
                try {
                    if (Util.getDebugLevel() > 10) Util.debug("Connect: " + address.getAddress() + ":" + address.getPort());
                    Client client = new Client(new Socket(getProxy(address.getAddress())));
                    client.socket.connect(address);
                    client.socket.setKeepAlive(true);
                    SocketAddress addr = client.socket.getRemoteSocketAddress();
                    if (addr instanceof InetSocketAddress) {
                        client.address = (InetSocketAddress) addr;
                    }
                    clients.add(client);
                    writeClient(client, 'r');
                    parseInput(client);
                } catch (UnknownHostException e) {
                    if (Util.getDebugLevel() > 90) e.printStackTrace();
                } catch (ConnectException e) {
                } catch (IOException e) {
                    if (Util.getDebugLevel() > 90) e.printStackTrace();
                }
            }
        }).start();
    }

    @SuppressWarnings("null")
    private static void parseInput(Client client) {
        try {
            InputStream in = client.socket.getInputStream();
            byte b, pB = 0;
            boolean newCommand = true;
            boolean first = true;
            byte type = 0;
            int length = 0;
            int pos = 0;
            int[] nums = null;
            StringBuffer ln = new StringBuffer();
            char command = 0;
            int t;
            while ((t = in.read()) != -1) {
                b = (byte) t;
                if (first) {
                    first = false;
                    pB = b;
                } else {
                    first = true;
                    char c = (char) (((pB < 0 ? (char) pB + 256 : (char) pB) << 8) + (b < 0 ? (char) b + 256 : (char) b));
                    if (newCommand) {
                        if (c != '\r' && c != '\n') {
                            newCommand = false;
                            command = c;
                            ln.setLength(0);
                            switch(c) {
                                case 'S':
                                case 'W':
                                case 'P':
                                    type = 1;
                                    length = 2;
                                    break;
                                case 'I':
                                    type = 1;
                                    length = 8;
                                    break;
                                case 'A':
                                case 'T':
                                case 'U':
                                case 'L':
                                case 'V':
                                case 'c':
                                    type = 0;
                                    length = 0;
                                    break;
                                default:
                                    newCommand = true;
                                    handleInput(client, command, null, null);
                                    break;
                            }
                            pos = 0;
                            nums = new int[length / 2];
                        }
                    } else {
                        int ix;
                        switch(type) {
                            case 0:
                                if (c == '\r' || c == '\n') {
                                    newCommand = true;
                                    handleInput(client, command, nums, ln.toString());
                                } else {
                                    ln.append(c);
                                }
                                break;
                            case 1:
                                ix = pos / 2;
                                nums[ix] = (nums[ix] << 16) + c;
                                if (++pos == length) {
                                    newCommand = true;
                                    handleInput(client, command, nums, null);
                                }
                                break;
                            case 2:
                                ix = pos / 2;
                                nums[ix] = (nums[ix] << 16) + c;
                                if (++pos == length) {
                                    type = 0;
                                }
                                break;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            if (Util.getDebugLevel() > 68) {
                if (!e.getMessage().equals("socket closed") && !e.getMessage().equals("Connection reset")) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            if (Util.getDebugLevel() > 90) e.printStackTrace();
        }
    }

    @SuppressWarnings({ "null", "fallthrough" })
    private static void handleInput(final Client client, char command, int[] nums, String str) {
        if (Util.getDebugLevel() > 76) {
            if (command != 'n' || Util.getDebugLevel() > 241) {
                Util.debug("Input (" + (server == null ? "NULL" : server.getLocalPort()) + "): " + command + " | " + Arrays.toString(nums) + " | " + (str != null ? str : "NULL"));
            }
        }
        try {
            switch(command) {
                case 'x':
                    clients.remove(client);
                    client.socket.close();
                    repaint();
                    break;
                case 'q':
                    writeClient(client, 'P', new int[] { (server != null ? server.getLocalPort() : -1) });
                    if (Util.getDebugLevel() > 62) Util.debug("INFO REQUEST FROM SERVER SIDE: " + client.address);
                case 'r':
                    final State state = Game.getState();
                    writeClient(client, 'I', Util.UUID2ints(Client.ID));
                    writeClient(client, 'L', null, Util.getProp("name"));
                    writeClient(client, 'A', null, Game.getActivityString());
                    writeClient(client, 'T', null, state.songInfo.getTitle());
                    writeClient(client, 'S', new int[] { state.score });
                    break;
                case 'i':
                    writeClient(client, 'I', Util.UUID2ints(Client.ID));
                    break;
                case 'I':
                    client.setId(Util.ints2UUID(nums));
                    break;
                case 'p':
                    writeClient(client, 'P', new int[] { (server != null ? server.getLocalPort() : -1) });
                    break;
                case 'P':
                    client.setAddress(nums[0]);
                    break;
                case 'l':
                    writeClient(client, 'L', null, Util.getProp("name"));
                    break;
                case 'L':
                    client.player = str;
                    if (getStringWidth(str) > MAX_PLAYER_LENGTH) {
                        final int max = MAX_PLAYER_LENGTH - DOTS_LENGTH;
                        int limit = str.length() - 1;
                        while (getStringWidth(str, 0, --limit) > max) ;
                        client.playerTrimmed = str.substring(0, limit) + "...";
                    } else {
                        client.playerTrimmed = str;
                    }
                    repaint();
                    int i = 0;
                    if (Util.getDebugLevel() > 56) {
                        for (Client c : clients) {
                            Util.debug(++i + ": " + !c.socket.isClosed() + " | " + c.stayOpen + " | " + c.address + " | " + c.socket.getRemoteSocketAddress() + " | " + c.getPlayer());
                        }
                    }
                    break;
                case 'a':
                    writeClient(client, 'A', null, Game.getActivityString());
                    break;
                case 'A':
                    try {
                        client.activity = Integer.parseInt(str);
                        repaint();
                    } catch (NumberFormatException e) {
                    }
                    break;
                case 't':
                    writeClient(client, 'T', null, Game.getState().songInfo.getTitle());
                    break;
                case 'T':
                    client.title = str.trim();
                    repaint();
                    break;
                case 's':
                    writeClient(client, 'S', new int[] { Game.getState().score });
                    break;
                case 'S':
                    client.score = nums[0];
                    repaint();
                    break;
                case 'v':
                    writeClient(client, 'V', null, KeyboardHero.APP_VERSION);
                    break;
                case 'V':
                    if (Util.getDebugLevel() > 51) Util.debug(client + "'s version: " + str);
                    break;
                case 'w':
                    writeClient(client, 'W', new int[] { KeyboardHero.UPDATE_NUMBER });
                    break;
                case 'W':
                    if (Util.getDebugLevel() > 51) Util.debug(client + "'s update number: " + nums[0]);
                    break;
                case 'n':
                    break;
                case 'U':
                    if (Util.getDebugLevel() > 51) Util.debug("Unknown command has been sent to " + client + ": " + str);
                    break;
                case 'O':
                    if (Util.getDebugLevel() > 51) Util.debug(client + " is responded to checking request: O" + str);
                    break;
                case 'c':
                    if (str.equals("heck")) {
                        writeClient(client, 'O', null, "k");
                        break;
                    } else if (str.equals("heckup")) {
                        writeClient(client, 'O', null, "k");
                        clients.remove(client);
                        client.socket.close();
                        break;
                    }
                default:
                    if (Util.getDebugLevel() > 51) Util.debug("Unknown command has been received from " + client + ": " + command);
                    writeClient(client, 'U', null, String.valueOf(command));
                    break;
            }
        } catch (IOException e) {
            if (Util.getDebugLevel() > 90) e.printStackTrace();
        }
    }

    private static void writeClient(final Client client, char command) {
        writeClient(client, command, null, null, null);
    }

    private static void writeClient(final Client client, char command, int[] nums) {
        writeClient(client, command, nums, null, null);
    }

    private static void writeClient(final Client client, char command, int[] nums, String str) {
        writeClient(client, command, nums, str, null);
    }

    private static void writeClient(final Client client, char command, int[] nums, String str, Iterator<Client> iterator) {
        if (command != 'n' || Util.getDebugLevel() > 241) {
            Util.debug("Output (" + (server == null ? "NULL" : server.getLocalPort()) + "): " + command + " | " + (nums == null || nums.length == 0 ? "NULL" : nums[0]) + " | " + (str == null ? "NULL" : str));
        }
        byte[] bs = new byte[2 + (nums == null ? 0 : nums.length) * 4 + (str == null ? 0 : str.length() * 2 + 2)];
        bs[0] = (byte) (command >> 8);
        bs[1] = (byte) command;
        int i = 0;
        if (nums != null) for (; i < nums.length; ++i) {
            final int j = i * 4;
            bs[j + 2] = (byte) (nums[i] >> 24);
            bs[j + 3] = (byte) (nums[i] >> 16);
            bs[j + 4] = (byte) (nums[i] >> 8);
            bs[j + 5] = (byte) nums[i];
        }
        if (str != null) {
            str.replaceAll("[\\r\\n]", " ");
            i = i * 4 + 1;
            char[] chars = new char[str.length()];
            str.getChars(0, str.length(), chars, 0);
            for (char c : chars) {
                bs[++i] = (byte) (c >> 8);
                bs[++i] = (byte) c;
            }
            bs[++i] = (byte) ('\n' >> 8);
            bs[++i] = (byte) '\n';
        }
        try {
            client.socket.getOutputStream().write(bs);
        } catch (IOException e) {
            if (Util.getDebugLevel() > 60) {
                e.printStackTrace();
            }
            if (iterator != null) {
                iterator.remove();
            } else {
                clients.remove(client);
            }
            repaint();
            try {
                client.socket.close();
            } catch (IOException e1) {
            }
        }
    }

    static void sendMessage(char command) {
        sendMessage(command, null, null);
    }

    static void sendMessage(char command, int[] nums) {
        sendMessage(command, nums, null);
    }

    static void sendMessage(char command, int[] nums, String str) {
        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                writeClient(iterator.next(), command, nums, str, iterator);
            }
        }
    }

    static void sendName() {
        sendMessage('L', null, Util.getProp("name"));
    }

    static void sendActivity() {
        sendMessage('A', null, Game.getActivityString());
    }

    static void sendTitle() {
        sendMessage('T', null, Game.getState().songInfo.getTitle());
    }

    static void sendScore() {
        sendMessage('S', new int[] { Game.getState().score });
    }

    @SuppressWarnings("null")
    private static void parseMultiInput(DatagramPacket message) {
        byte[] buff = message.getData();
        if (Util.getDebugLevel() > 98) Util.debug("UNCHECKED MULTI MESSAGE: " + message.getSocketAddress() + " | BUFF:" + new String(buff));
        if (Util.equals(MULTICAST_ID, buff, MULTICAST_ID.length)) {
            byte b, pB = 0;
            boolean newCommand = true;
            boolean first = true;
            byte type = 0;
            int length = 0;
            int pos = 0;
            int[] nums = null;
            StringBuffer ln = new StringBuffer();
            char command = 0;
            for (int i = MULTICAST_ID.length; i < buff.length; ++i) {
                b = buff[i];
                if (first) {
                    first = false;
                    pB = b;
                } else {
                    first = true;
                    char c = (char) (((pB < 0 ? (char) pB + 256 : (char) pB) << 8) + (b < 0 ? (char) b + 256 : (char) b));
                    if (newCommand) {
                        if (c != '\r' && c != '\n') {
                            newCommand = false;
                            command = c;
                            ln.setLength(0);
                            switch(c) {
                                case 'l':
                                case 'L':
                                    type = 1;
                                    length = 10;
                                    break;
                                case 'U':
                                    type = 0;
                                    length = 0;
                                    break;
                                default:
                                    handleMultiInput(message.getAddress(), command, null, null);
                                    return;
                            }
                            pos = 0;
                            nums = new int[length / 2];
                        }
                    } else {
                        int ix;
                        switch(type) {
                            case 0:
                                if (c == '\r' || c == '\n') {
                                    handleMultiInput(message.getAddress(), command, nums, ln.toString());
                                    return;
                                } else {
                                    ln.append(c);
                                }
                                break;
                            case 1:
                                ix = pos / 2;
                                nums[ix] = (nums[ix] << 16) + c;
                                if (++pos == length) {
                                    handleMultiInput(message.getAddress(), command, nums, null);
                                    return;
                                }
                                break;
                            case 2:
                                ix = pos / 2;
                                nums[ix] = (nums[ix] << 16) + c;
                                if (++pos == length) {
                                    type = 0;
                                }
                                break;
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("fallthrough")
    private static void handleMultiInput(final InetAddress address, final char command, final int[] nums, final String str) {
        if (Util.getDebugLevel() > 76) Util.debug("MultiInput (" + (server == null ? "NULL" : server.getLocalPort()) + "): " + command + " | " + Arrays.toString(nums) + " | " + (str != null ? str : "NULL"));
        switch(command) {
            case 'l':
                {
                    int port = nums[4];
                    UUID id = Util.ints2UUID(nums);
                    if (!Client.ID.equals(id)) {
                        sendMultiListEntry('L');
                        multiConnect(address, id, port);
                    }
                }
                break;
            case 'L':
                {
                    final int port = nums[4];
                    final UUID id = Util.ints2UUID(nums);
                    if (!Client.ID.equals(id)) (new Thread() {

                        public void run() {
                            try {
                                Thread.sleep(MULTICAST_WAIT_FOR_OTHER_PARTY_TO_CONNECT);
                            } catch (InterruptedException e) {
                                if (Util.getDebugLevel() > 91) e.printStackTrace();
                            }
                            multiConnect(address, id, port);
                        }
                    }).start();
                }
                break;
            default:
                if (Util.getDebugLevel() > 51) Util.debug("Unknown multi command has been received: " + command);
                break;
        }
    }

    private static void sendMultiListEntry(char command) {
        int[] uuid = Util.UUID2ints(Client.ID);
        sendMultiMessage(command, new int[] { uuid[0], uuid[1], uuid[2], uuid[3], (server != null ? server.getLocalPort() : -1) }, null);
    }

    private static void multiConnect(InetAddress address, UUID id, int port) {
        Util.debug("---------multiConnect: " + id + " | " + Client.ID);
        if (port < 0 || port > 65535) {
            return;
        }
        boolean notFound = true;
        if (Util.getDebugLevel() > 25) Util.debug("NEW Address from Multi: " + address.toString() + ":" + port);
        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                final Client client = iterator.next();
                if (client.socket.isClosed()) {
                    iterator.remove();
                    continue;
                }
                if (Util.getDebugLevel() > 26 && client.address != null) Util.debug("Address: " + client.address.toString());
                if ((client.address != null && address.equals(client.address.getAddress()) && port == client.address.getPort()) || (id != null && id.equals(client.id))) {
                    Util.debug("id=" + id + " | ID=" + Client.ID);
                    notFound = false;
                    break;
                }
            }
        }
        if (notFound) {
            connectClient(new InetSocketAddress(address, port));
        }
    }

    private static void sendMultiMessage(char command, int[] nums, String str) {
        try {
            for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements(); ) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress()) {
                        writeMultiMessage(address, command, nums, str);
                    }
                }
            }
        } catch (SocketException e) {
            if (Util.getDebugLevel() > 50) e.printStackTrace();
        }
    }

    private static void writeMultiMessage(InetAddress address, char command, int[] nums, String str) {
        Util.debug("MultiOutput (" + (server == null ? "NULL" : server.getLocalPort()) + "): " + command + " | " + Arrays.toString(nums) + " | " + (str == null ? "NULL" : str));
        byte[] bs = new byte[MULTICAST_ID.length + 2 + (nums == null ? 0 : nums.length) * 4 + (str == null ? 0 : str.length() * 2 + 2)];
        int j = 0;
        for (; j < MULTICAST_ID.length; ++j) {
            bs[j] = MULTICAST_ID[j];
        }
        bs[j++] = (byte) (command >> 8);
        bs[j++] = (byte) command;
        if (nums != null) for (int i = 0; i < nums.length; ++i) {
            bs[j++] = (byte) (nums[i] >> 24);
            bs[j++] = (byte) (nums[i] >> 16);
            bs[j++] = (byte) (nums[i] >> 8);
            bs[j++] = (byte) nums[i];
        }
        if (str != null) {
            str.replaceAll("[\\r\\n]", " ");
            char[] chars = new char[str.length()];
            str.getChars(0, str.length(), chars, 0);
            for (char c : chars) {
                bs[j++] = (byte) (c >> 8);
                bs[j++] = (byte) c;
            }
            bs[j++] = (byte) ('\n' >> 8);
            bs[j++] = (byte) '\n';
        }
        try {
            MulticastSocket socket = new MulticastSocket(new InetSocketAddress(address, MULTICAST_SENDING_PORT));
            socket.setTimeToLive(MULITCAST_TIME_TO_LIVE);
            socket.send(new DatagramPacket(bs, bs.length, MULTICAST_GROUP, MULTICAST_RECEIVING_PORT));
            socket.close();
        } catch (IOException e) {
            if (Util.getDebugLevel() > 60) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Checks whether the user has reached a score to be displayed in the local toplist and tries to
	 * send the final information about the user's game to the server. This action is only made if
	 * the {@link #integrityCheck() integrity check} is passed.
	 * 
	 * @see #MAX_IN_TOP
	 * @see #URL_STR
	 */
    static void checkToplist() {
        if (integrityCheck()) {
            Util.debug(16, "Application (" + KeyboardHero.APP_TITLE + ") integrity check have failed!");
            return;
        }
        final State state = Game.getState();
        if (state.score <= 0) return;
        if (state.score > Game.getMaxScore(state.speed)) {
            Util.debug(18, "Impossible score number!");
            return;
        }
        final String author = state.songInfo.getAuthor();
        final String songTitle = (author == null || author.equals("") ? "" : author + ": ") + state.songInfo.getTitle().replace('¦', '|');
        Util.debug("TITLE:" + state.songInfo.getTitle());
        boolean wasntNameCheck = true;
        String codeline = null;
        int foundCount = 0;
        int newEntryNum = -1;
        String[] names = new String[MAX_IN_TOP];
        String[] songs = new String[MAX_IN_TOP];
        Integer[] scores = new Integer[MAX_IN_TOP];
        Float[] factors = new Float[MAX_IN_TOP];
        String[] lines = new String[MAX_IN_TOP];
        try {
            BufferedReader bufferedRdr = new BufferedReader(new FileReader('.' + KeyboardHero.APP_NAME + ".tls"));
            String line = null;
            String[] parts;
            String[] subparts;
            String[] ssubparts;
            int tlscore;
            float tlfactor;
            while ((line = bufferedRdr.readLine()) != null) {
                parts = line.split(" ", 2);
                if (parts.length == 2 && parts[1].equals(decrypt(Util.stringReverse(parts[0])))) {
                    try {
                        parts[1] = decrypt(parts[1]);
                        subparts = parts[1].split(" ", 3);
                        if (subparts.length != 3) {
                            Util.debug(28, "Not enough subentry in toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                            continue;
                        }
                        tlscore = Integer.parseInt(subparts[1]);
                        if (wasntNameCheck && state.score > tlscore) {
                            if (Util.getPlayerName()) {
                                return;
                            }
                            wasntNameCheck = false;
                            newEntryNum = foundCount;
                            scores[foundCount] = state.score;
                            factors[foundCount] = state.speed;
                            songs[foundCount] = songTitle;
                            names[foundCount] = (Util.isDebugMode() ? "[Debugger] " + Util.getProp("name") : Util.getProp("name"));
                            codeline = encrypt(state.speed + " " + state.score + " " + songs[foundCount] + "¦" + names[foundCount]);
                            lines[foundCount] = (codeline = Util.stringReverse(encrypt(codeline)) + " " + codeline);
                            if ((++foundCount) >= MAX_IN_TOP) break;
                        }
                        tlfactor = Float.parseFloat(subparts[0]);
                        ssubparts = subparts[2].split("¦", 2);
                        if (ssubparts.length != 2) {
                            Util.debug(26, "Not enough subsubentry in toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                            continue;
                        }
                        scores[foundCount] = tlscore;
                        factors[foundCount] = tlfactor;
                        songs[foundCount] = ssubparts[0];
                        names[foundCount] = ssubparts[1];
                        lines[foundCount] = line;
                        if ((++foundCount) >= MAX_IN_TOP) break;
                    } catch (NumberFormatException e) {
                        Util.debug(24, "Corrupted toplist score and/or level number in toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                    }
                } else {
                    Util.debug(32, "Corrupted toplist entry in toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                }
            }
            bufferedRdr.close();
            if (wasntNameCheck && foundCount < MAX_IN_TOP) {
                if (Util.getPlayerName()) {
                    return;
                }
                wasntNameCheck = false;
                newEntryNum = foundCount;
                scores[foundCount] = state.score;
                factors[foundCount] = state.speed;
                songs[foundCount] = songTitle;
                names[foundCount] = (Util.isDebugMode() ? "[Debugger] " + Util.getProp("name") : Util.getProp("name"));
                codeline = encrypt(state.speed + " " + state.score + " " + songs[foundCount] + "¦" + names[foundCount]);
                lines[foundCount++] = (codeline = Util.stringReverse(encrypt(codeline)) + " " + codeline);
            }
            writeToplist(lines, foundCount);
        } catch (Exception e) {
            Util.debug(64, "Couldn't read the toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
            if (Util.getPlayerName()) {
                return;
            }
            wasntNameCheck = false;
            newEntryNum = foundCount;
            scores[foundCount] = state.score;
            factors[foundCount] = state.speed;
            songs[foundCount] = songTitle;
            names[foundCount] = (Util.isDebugMode() ? "[Debugger] " + Util.getProp("name") : Util.getProp("name"));
            codeline = encrypt(state.speed + " " + state.score + " " + songs[foundCount] + "¦" + names[foundCount]);
            lines[foundCount++] = (codeline = Util.stringReverse(encrypt(codeline)) + " " + codeline);
            writeToplist(lines, foundCount);
        }
        if (Util.getPropBool("connToplist")) {
            if (wasntNameCheck) {
                if (Util.getPlayerName()) {
                    return;
                }
            }
            if (wasntNameCheck) {
                codeline = encrypt(state.speed + " " + state.score + " " + songTitle + "¦" + (Util.isDebugMode() ? "[Debugger] " + Util.getProp("name") : Util.getProp("name")));
                codeline = Util.stringReverse(encrypt(codeline)) + " " + codeline;
            }
            final String finalCodeline = codeline;
            (new Thread() {

                public void run() {
                    try {
                        URL url = new URL(URL_STR);
                        URLConnection connection = url.openConnection(getProxy());
                        connection.setRequestProperty("User-Agent", USER_AGENT);
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
                        output.writeBytes("app=" + URLEncoder.encode(KeyboardHero.APP_TITLE, "UTF-8") + "&req=add&code=" + URLEncoder.encode(finalCodeline, "UTF-8"));
                        output.flush();
                        output.close();
                        DataInputStream input = new DataInputStream(connection.getInputStream());
                        input.close();
                    } catch (Exception e) {
                        Util.error(Util.getMsg("CannotToplist"), Util.getMsg("CannotToplist2"));
                    }
                }
            }).start();
        }
        if (!wasntNameCheck) {
            ((DialogToplist) KeyboardHero.getDialogs().get("toplist")).open(Util.getMsgMnemonic("Menu_LocalToplist"), names, scores, songs, factors, foundCount, newEntryNum);
        }
    }

    /**
	 * Writes the given lines to hard disk drive to the local toplist file named: "."+{@value keyboardhero.KeyboardHero#APP_NAME}+".tls".
	 * This method is used whenever the player reaches a place into the local toplist.
	 * 
	 * @param lines
	 *            the lines to be written into the file.
	 * @param lineCount
	 *            the number of lines to be written.
	 */
    private static void writeToplist(String[] lines, int lineCount) {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter('.' + KeyboardHero.APP_NAME + ".tls"));
            for (int i = 0; i < lineCount && i < lines.length; i++) {
                output.write(lines[i] + "\n");
            }
        } catch (Exception e) {
            Util.error("Error while writing the toplist file: ." + KeyboardHero.APP_NAME + ".tls", e.toString());
        } finally {
            try {
                if (output != null) output.close();
            } catch (IOException e) {
                Util.debug(3, e.toString());
            }
        }
    }

    /**
	 * Opens a dialog containing the information stored in the local toplist. This information
	 * contains the name of the player and the reached score and level.
	 * 
	 * @see Connection#MAX_IN_TOP
	 */
    static void showLocalToplist() {
        int foundCount = 0;
        String[] names = new String[MAX_IN_TOP];
        String[] songs = new String[MAX_IN_TOP];
        Integer[] scores = new Integer[MAX_IN_TOP];
        Float[] factors = new Float[MAX_IN_TOP];
        try {
            BufferedReader bufferedRdr = new BufferedReader(new FileReader('.' + KeyboardHero.APP_NAME + ".tls"));
            String line = null;
            String[] parts;
            String[] subparts;
            String[] ssubparts;
            int tlscore;
            float tlfactor;
            while ((line = bufferedRdr.readLine()) != null) {
                parts = line.split(" ", 2);
                if (parts.length == 2 && parts[1].equals(decrypt(Util.stringReverse(parts[0])))) {
                    try {
                        parts[1] = decrypt(parts[1]);
                        subparts = parts[1].split(" ", 3);
                        if (subparts.length != 3) {
                            Util.debug(28, "Not enough subentry in toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                            continue;
                        }
                        tlscore = Integer.parseInt(subparts[1]);
                        tlfactor = Float.parseFloat(subparts[0]);
                        scores[foundCount] = tlscore;
                        factors[foundCount] = tlfactor;
                        ssubparts = subparts[2].split("¦", 2);
                        if (ssubparts.length != 2) {
                            Util.debug(26, "Not enough subsubentry in toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                            continue;
                        }
                        songs[foundCount] = ssubparts[0];
                        names[foundCount] = ssubparts[1];
                        if ((++foundCount) >= MAX_IN_TOP) break;
                    } catch (NumberFormatException e) {
                        Util.debug(24, "Corrupted toplist score and/or level number in toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                    }
                } else {
                    Util.debug(32, "Corrupted toplist entry in toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                }
            }
            bufferedRdr.close();
        } catch (Exception e) {
        }
        ((DialogToplist) KeyboardHero.getDialogs().get("toplist")).open(Util.getMsgMnemonic("Menu_LocalToplist"), names, scores, songs, factors, foundCount, -1);
    }

    /**
	 * Opens a dialog containing the information stored in the online toplist. This information
	 * contains the name of the player and the reached score and level.
	 * 
	 * @see #URL_STR
	 */
    static void showOnlineToplist() {
        (new Thread() {

            public void run() {
                ((DialogToplist) KeyboardHero.getDialogs().get("toplist")).open(Util.getMsgMnemonic("Menu_OnlineToplist"), Util.getMsg("Tpl_OnlineLoading"), true);
            }
        }).start();
        (new Thread() {

            public void run() {
                try {
                    URL url = new URL(URL_STR + "?req=list");
                    URLConnection connection = url.openConnection(getProxy());
                    connection.setRequestProperty("User-Agent", USER_AGENT);
                    BufferedReader bufferedRdr = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = null;
                    int foundCount = 0;
                    ArrayList<String> names = new ArrayList<String>();
                    ArrayList<String> songs = new ArrayList<String>();
                    ArrayList<Integer> scores = new ArrayList<Integer>();
                    ArrayList<Float> factors = new ArrayList<Float>();
                    String[] subparts;
                    String[] ssubparts;
                    int tlscore;
                    float tlfactor;
                    while ((line = bufferedRdr.readLine()) != null) {
                        if (line.length() > 2) {
                            try {
                                subparts = line.split(" ", 3);
                                if (subparts.length != 3) {
                                    Util.debug(28, "Not enough subentry in online toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                                    continue;
                                }
                                tlscore = Integer.parseInt(subparts[1]);
                                tlfactor = Float.parseFloat(subparts[0]);
                                scores.add(tlscore);
                                factors.add(tlfactor);
                                ssubparts = hexdecode(subparts[2]).split("¦", 2);
                                if (ssubparts.length != 2) {
                                    Util.debug(26, "Not enough subsubentry in online toplist file: ." + KeyboardHero.APP_NAME + ".tls!");
                                    continue;
                                }
                                songs.add(ssubparts[0]);
                                names.add(ssubparts[1]);
                                foundCount++;
                            } catch (NumberFormatException e) {
                                Util.debug(24, "Corrupted toplist score and/or level number in the online toplist!");
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Util.debug(25, "Corrupted toplist entry in the online toplist!");
                            }
                        }
                    }
                    bufferedRdr.close();
                    ((DialogToplist) KeyboardHero.getDialogs().get("toplist")).setContent(names.toArray(new String[0]), scores.toArray(new Integer[0]), songs.toArray(new String[0]), factors.toArray(new Float[0]), foundCount, -1);
                } catch (Exception e) {
                    ((DialogToplist) KeyboardHero.getDialogs().get("toplist")).setStatusText(Util.getMsg("CannotToplist") + "!\n\n" + e.toString(), false);
                }
            }
        }).start();
    }

    /**
	 * Opens the platform's default browser with the url stored in the application.
	 * 
	 * @see #URL_STR
	 */
    static void showBrowser() {
        final String url;
        try {
            url = URL_STR + "?var=" + URLEncoder.encode(Util.getLang(), "UTF-8") + "?" + URLEncoder.encode(Util.getProp("name"), "UTF-8");
        } catch (final Exception ex) {
            Util.error(Util.getMsg("NoBrowser"), ex.toString());
            return;
        }
        if (Util.DESKTOP != null && Util.DESKTOP.isSupported(Desktop.Action.BROWSE)) {
            try {
                Util.DESKTOP.browse(new URI(url));
            } catch (Exception e) {
                Util.error(Util.getMsg("NoBrowser"), e.toString());
            }
        } else {
            if (Util.IS_WIN) {
                try {
                    Runtime.getRuntime().exec("cmd /C \"start " + url + "\"");
                } catch (final Exception ex) {
                    Util.error(Util.getMsg("NoBrowser"), ex.toString());
                }
            } else {
                (new Thread() {

                    public void run() {
                        try {
                            Process process = Runtime.getRuntime().exec("netscape -remote openURL(" + url + ")");
                            final int exitCode = process.waitFor();
                            if (exitCode != 0) {
                                Runtime.getRuntime().exec("netscape " + url);
                            }
                        } catch (Exception ex2) {
                            Util.error(Util.getMsg("NoBrowser"), ex2.toString());
                        }
                    }
                }).start();
            }
        }
    }

    /**
	 * Asks the server whether there is any update available. And if there is one, then the update
	 * will be downloaded to the local hard disk drive. The value of the
	 * {@link KeyboardHero#UPDATE_NUMBER} field will be sent to the server. This method sets the
	 * value of the {@link #wasUpdate} field to true.
	 * 
	 * @param fromMenu
	 *            indicates whether this method is called by the user from the menu, or it is called
	 *            automatically at the start of the application. If it is true, there will be a
	 *            response even if there is no update available.
	 * @see #URL_STR
	 * @see #getProxy()
	 */
    static void checkUpdates(final boolean fromMenu) {
        (new Thread() {

            public void run() {
                try {
                    removeStatuses();
                    if (wasUpdate) {
                        if (fromMenu) {
                            Util.warning(Util.getMsg("Wrn_WasUpdate"));
                        }
                        return;
                    }
                    KeyboardHero.addStatus(UPD_START);
                    URLConnection connection = new URL(URL_STR + "?req=updcheck&upd=" + KeyboardHero.UPDATE_NUMBER).openConnection(getProxy());
                    connection.setRequestProperty("User-Agent", USER_AGENT);
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    DataInputStream input = new DataInputStream(connection.getInputStream());
                    if (input.readUnsignedByte() == 'y') {
                        input.close();
                        KeyboardHero.removeStatus(UPD_START);
                        if (KeyboardHero.confirm(Util.getMsg("OkUpdate"))) {
                            KeyboardHero.addStatus(UPD_OK);
                            connection = new URL(URL_STR + "?req=upddownload").openConnection(getProxy());
                            connection.setRequestProperty("User-Agent", USER_AGENT);
                            connection.setDoOutput(false);
                            connection.setDoInput(true);
                            input = new DataInputStream(connection.getInputStream());
                            try {
                                FileOutputStream output = new FileOutputStream(KeyboardHero.APP_FILENAME);
                                int bytesRead = 0;
                                byte[] buffer = new byte[2048];
                                bytesRead = input.read(buffer);
                                if (bytesRead == -1 || Arrays.equals(Arrays.copyOf(buffer, 21), ("No updates available!".getBytes()))) {
                                    Util.warning(Util.getMsg("NoUpdate"));
                                } else {
                                    output.write(buffer, 0, bytesRead);
                                    while ((bytesRead = input.read(buffer)) != -1) {
                                        output.write(buffer, 0, bytesRead);
                                    }
                                    output.close();
                                    wasUpdate = true;
                                    KeyboardHero.removeStatus(UPD_OK);
                                    if (KeyboardHero.confirm(Util.getMsg("OkRestart"))) {
                                        Game.exit();
                                        KeyboardHero.closure();
                                        Util.RUNTIME.exec("java -jar " + KeyboardHero.APP_FILENAME);
                                        System.exit(0);
                                    }
                                }
                            } catch (Exception e) {
                                if (fromMenu) {
                                    Util.error(Util.getMsg("Err_CannotCreateFile"), e.toString());
                                } else {
                                    UPD_CANTAUTOFILE.setException(e.toString());
                                    KeyboardHero.addStatus(UPD_CANTAUTOFILE);
                                }
                            }
                        }
                    } else {
                        input.close();
                        if (fromMenu) {
                            Util.info(Util.getMsg("NoUpdate"));
                        }
                        KeyboardHero.removeStatus(UPD_START);
                    }
                } catch (Exception e) {
                    if (fromMenu) {
                        Util.error(Util.getMsg("CannotToplist"));
                    } else {
                        UPD_CANTAUTO.setException(e.toString());
                        KeyboardHero.addStatus(UPD_CANTAUTO);
                    }
                }
            }
        }).start();
    }

    static void removeStatuses() {
        KeyboardHero.removeStatus(UPD_CANTAUTO);
        KeyboardHero.removeStatus(UPD_CANTAUTOFILE);
        KeyboardHero.removeStatus(UPD_OK);
        KeyboardHero.removeStatus(UPD_START);
    }

    static final ServerSocket getServer() {
        return server;
    }

    static final List<Client> getClients() {
        return clients;
    }

    static final boolean isListening() {
        if (server == null) return false;
        return !server.isClosed();
    }

    static Proxy getProxy(InetAddress host) {
        if (Util.getPropBool("proxyNoLocal") && (host.isSiteLocalAddress() || host.isLinkLocalAddress() || host.isLoopbackAddress() || host.isAnyLocalAddress() || host.getHostAddress().startsWith("128."))) return Proxy.NO_PROXY;
        return getProxy();
    }

    static Proxy getProxy() {
        if (proxy != null) return proxy;
        return getProxy(Util.getPropInt("proxyType"), Util.getProp("proxyAddress"), Util.getPropInt("proxyPort"));
    }

    static Proxy getProxy(final int proxyType, final String proxyAddress, final int proxyPort) {
        try {
            switch(proxyType) {
                case 0:
                    System.setProperty("java.net.useSystemProxies", "true");
                    List<Proxy> list = ProxySelector.getDefault().select(new URI(URL_STR));
                    for (Proxy p : list) {
                        final InetSocketAddress address = (InetSocketAddress) p.address();
                        if (Util.getDebugLevel() > 10) Util.debug("Found proxy: " + p.type() + " | " + (address != null ? address.getHostName() + ":" + address.getPort() : "NULL"));
                        if (p.type() != Proxy.Type.DIRECT && address != null) {
                            proxy = p;
                            return p;
                        }
                    }
                    proxy = Proxy.NO_PROXY;
                    break;
                case 1:
                    proxy = Proxy.NO_PROXY;
                    break;
                case 2:
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort));
                    break;
                case 3:
                    proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyAddress, proxyPort));
                    break;
                default:
                    proxy = Proxy.NO_PROXY;
                    break;
            }
        } catch (Exception e) {
            Util.error(Util.getMsg("Err_ProxyException"), e.toString());
            return Proxy.NO_PROXY;
        }
        return proxy;
    }

    static void resetProxy() {
        proxy = null;
    }

    static void readTotalScore() {
        try {
            String cache = Util.getProp("name");
            String str = Util.getProp("totalScore_" + cache);
            if (str.equals("")) {
                Game.getState().totalScore = 0;
                return;
            }
            final String[] parts = str.split(" ", 2);
            if (parts.length != 2) {
                Game.getState().totalScore = 0;
                return;
            }
            if (!parts[1].equals(Util.stringReverse(decrypt(parts[0])))) {
                Game.getState().totalScore = 0;
                return;
            }
            str = decrypt(parts[1]);
            cache = getTotalScoreString(cache);
            if (!str.startsWith(cache) || !str.endsWith(Util.stringReverse(cache))) {
                Game.getState().totalScore = 0;
                return;
            }
            final int length = cache.length();
            str = str.substring(length, str.length() - length);
            Game.getState().totalScore = Integer.parseInt(str);
        } catch (Exception e) {
            Game.getState().totalScore = 0;
            return;
        }
    }

    static void writeTotalScore() {
        final String name = Util.getProp("name");
        String str = getTotalScoreString(name);
        str = encrypt(str + Integer.toString(Game.getState().totalScore) + Util.stringReverse(str));
        Util.setProp("totalScore_" + name, encrypt(Util.stringReverse(str)) + " " + str);
    }

    private static final String getTotalScoreString(String name) {
        return "Total Score of " + name + " is: ";
    }

    static final void newPortMappings(final int oldPort, final int newPort) {
        newPortMappings(oldPort, newPort, false);
    }

    static final void newPortMappings(final int oldPort, final int newPort, final boolean synchronous) {
        if (synchronous) {
            newPortMappingsAsynchronous(oldPort, newPort);
        } else {
            (new Thread() {

                public void run() {
                    newPortMappingsAsynchronous(oldPort, newPort);
                }
            }).start();
        }
    }

    private static synchronized void newPortMappingsAsynchronous(final int oldPort, final int newPort) {
        try {
            if (oldPort == -1 && newPort == -1) return;
            if (Util.getDebugLevel() > 54) Util.debug("OLDPORT: " + oldPort + "\t|\tNEWPORT: " + newPort);
            InetAddress[] localHosts = null;
            try {
                localHosts = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e1) {
                try {
                    localHosts = new InetAddress[] { InetAddress.getLocalHost() };
                } catch (UnknownHostException e2) {
                    if (Util.getDebugLevel() > 60) e1.printStackTrace();
                }
                if (Util.getDebugLevel() > 64) e1.printStackTrace();
            }
            if (localHosts == null) localHosts = new InetAddress[0];
            try {
                for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
                    final NetworkInterface networkInterface = networkInterfaces.nextElement();
                    try {
                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                        if (!addresses.hasMoreElements()) continue;
                        InetAddress localHost = addresses.nextElement();
                        if (!Util.contains(localHosts, localHost)) while (addresses.hasMoreElements()) {
                            InetAddress tempHost = addresses.nextElement();
                            if (Util.contains(localHosts, tempHost)) {
                                localHost = tempHost;
                                break;
                            }
                        }
                        final String host = localHost.getHostAddress();
                        if (Util.getDebugLevel() > 110) Util.debug(networkInterface + "\t:\t" + host);
                        InternetGatewayDevice[] gatewayDevices = InternetGatewayDevice.getDevices(GATEWAY_TIMEOUT, Discovery.DEFAULT_TTL, Discovery.DEFAULT_MX, networkInterface);
                        if (gatewayDevices != null) {
                            for (int j = 0; j < gatewayDevices.length; ++j) {
                                InternetGatewayDevice gatewayDevice = gatewayDevices[j];
                                Util.debug(gatewayDevice.getExternalIPAddress() + "\t:\t" + host);
                                if (oldPort != -1) {
                                    gatewayDevice.deletePortMapping(null, oldPort, "TCP");
                                }
                                if (newPort != -1) {
                                    gatewayDevice.addPortMapping("KeyboardHero", null, newPort, newPort, host, 0, "TCP");
                                }
                            }
                        }
                        Thread.sleep(500);
                    } catch (Exception e) {
                        if (Util.getDebugLevel() > 70) e.printStackTrace();
                    }
                }
            } catch (SocketException e) {
                if (Util.getDebugLevel() > 55) e.printStackTrace();
            }
        } catch (Throwable e) {
            if (Util.getDebugLevel() > 30) e.printStackTrace();
        }
    }

    private static void repaint() {
        Graphs.getCanvas().repaint();
    }

    static void closure() {
        stopListening();
    }

    private static final int getStringWidth(String str) {
        return (int) PLAYERS_FONT.getStringBounds(str, FONT_RENDER_CONTEXT).getWidth();
    }

    private static final int getStringWidth(String str, int beginIndex, int limit) {
        return (int) PLAYERS_FONT.getStringBounds(str, beginIndex, limit, FONT_RENDER_CONTEXT).getWidth();
    }

    /**
	 * Creates a string containing some dump characters. It is used inside the
	 * {@link #encrypt(String)} method.
	 * 
	 * @param c
	 *            the character that gives the basis of the generation.
	 * @param len
	 *            the requested length of the string to be returned.
	 * @return the generated string with the requested length.
	 */
    private static String charRandRepeat(char c, int len) {
        char[] cs = new char[len];
        for (int i = 0; i < len; i++) {
            cs[i] = (char) ((c + 113 * i) % 256);
        }
        return new String(cs);
    }

    /**
	 * Decodes the given text as it in every character is stored as a hexadecimal value. The
	 * required input format is: every block of four character determines one character. Each block
	 * is a hexadecimal number representing the character code of the actual character.
	 * 
	 * @param text
	 *            the text to be decoded.
	 * @return the decoded text as a normal String.
	 */
    private static String hexdecode(String text) {
        byte[] str = text.getBytes();
        final int max = str.length;
        char[] out = new char[max / 4];
        for (int i = 0; i < max; i += 4) {
            out[i / 4] = (char) (Util.HEXD[str[i]] * 4096 + Util.HEXD[str[i + 1]] * 256 + Util.HEXD[str[i + 2]] * 16 + Util.HEXD[str[i + 3]]);
        }
        return new String(out);
    }

    /**
	 * Encrypts the given string. It is used at the storage of the local toplist and at the
	 * communication with the server.
	 * 
	 * @param text
	 *            the string to be encrypted.
	 * @return the encrypted string.
	 */
    private static String encrypt(String text) {
        text = "!3¤a$" + text + "i_5˙H/+đ";
        final int length = text.length();
        text = text.substring(0, 9) + charRandRepeat((char) (length * 45), length / 2) + text.substring(9);
        char[] chars = text.toCharArray();
        final int ln = chars.length * 2;
        byte str[] = new byte[ln];
        int i = 0;
        int k = 0;
        for (; k < ln; ) {
            str[k] = (byte) (chars[i] / 256 - 128);
            str[(k += 2) - 1] = (byte) (chars[i++] % 256 - 128);
        }
        byte hstr[] = new byte[2 * ln];
        final int m1 = -13 - (int) (3.3 * ln);
        final int m2 = 24 + (int) (1.7 * ln / 3);
        byte c;
        k = -2;
        for (i = 0; i < ln; ) {
            c = (byte) (((ENCB[((str[i++] + 128 + m1 + i * 5) % 256 + 256) % 256] + i * m2 / 3) % 256 + 256) % 256);
            hstr[k += 2] = Util.HEXE[(c + 128) / 16];
            hstr[k + 1] = Util.HEXE[(c + 128) % 16];
        }
        return new String(hstr);
    }

    /**
	 * Decrypts the given string. It is used at the reading of the local toplist and at the
	 * communication with the server.
	 * 
	 * @param text
	 *            the string to be decrypted.
	 * @return the decrypted string.
	 */
    private static String decrypt(String text) {
        final byte hstr[] = text.getBytes();
        final int ln = hstr.length / 2;
        byte str[] = new byte[ln];
        final int m1 = -13 - (int) (3.3 * ln);
        final int m2 = 24 + (int) (1.7 * ln / 3);
        int i = 0;
        int k = -2;
        for (; i < ln; ) {
            str[i++] = (byte) (((DECB[((Util.HEXD[hstr[k += 2]] * 16 + Util.HEXD[hstr[k + 1]] - i * m2 / 3) % 256 + 256) % 256] - m1 - i * 5) % 256 + 256) % 256);
        }
        char[] chars = new char[ln / 2];
        i = 0;
        for (k = 0; k < ln; ) {
            chars[i++] = (char) ((str[k] + 128) * 256 + (str[(k += 2) - 1] + 128));
        }
        text = new String(chars);
        text = text.substring(5, text.length() - 8);
        final int length = text.length();
        return text.substring(0, Math.min(4, length - (length + 14) / 3)) + text.substring(Math.min((length + 14) / 3 + 4, length));
    }

    /**
	 * This method serves security purposes. Checks all the other classes' integrity strings. Thus
	 * the application can only be altered if the source is known; otherwise the toplist will not be
	 * updated.
	 * <p>
	 * Every class in the {@link keyboardhero} package has an integrity string, and this method
	 * tests them. In case of failure no update will be made on both the local and online toplists.
	 * 
	 * @return true, if the integrity check have failed; and false if it is passed.
	 */
    private static boolean integrityCheck() {
        if (!AbstractDialog.getIntegrityString().equals("ol+sj2SäłŁ*í~.1Asá-")) return true;
        if (!Connection.getIntegrityString().equals("K^/a\\s'U*Ds!ah\"ďż˝+m")) return true;
        if (!DialogAbout.getIntegrityString().equals("U!asdf4jk3++asdfas$")) return true;
        if (!DialogNewGame.getIntegrityString().equals("sdfDg!+%asÉ\tjfg ash")) return true;
        if (!DialogRules.getIntegrityString().equals("345\nSDf+!kj+JKs1fOgđ")) return true;
        if (!DialogSettings.getIntegrityString().equals("df+!v+-.sayyďż˝6ďż˝2LMdsa")) return true;
        if (!DialogToplist.getIntegrityString().equals("u%c`J˝o!dfdU_3345TasT")) return true;
        if (!Game.getIntegrityString().equals("psA'LÍMAsÁŰSJő!5-öpp")) return true;
        if (!Graphs.getIntegrityString().equals("Dďż˝>#jIďż˝h+Sl;H5'h.j_")) return true;
        if (!KeyboardHero.getIntegrityString().equals("7zH*SI;.asdj3as-dm")) return true;
        if (!MidiDevicer.getIntegrityString().equals("Jam2,ay$sfgp23has_")) return true;
        if (!MidiSequencer.getIntegrityString().equals("5-alS,am3+-ysDD6×as-")) return true;
        if (!MidiSong.getIntegrityString().equals("jhaÍs+!.Sys-sdf+éUiáső")) return true;
        if (!RendererSwing.getIntegrityString().equals("Oa+4y!-Łs÷i3SD@n_12{")) return true;
        if (!Tester.getIntegrityString().equals("8ł2+*43-+-5zSDFasd")) return true;
        if (!Util.getIntegrityString().equals("a$sd'sEP\"s-×dfAS")) return true;
        return false;
    }

    /**
	 * Creates a string containing the most important information about the class. This method is
	 * used only for debugging and testing purposes.
	 * 
	 * @return the created string.
	 */
    static String getString() {
        return "Connection(Client.ID=" + Client.ID + "; server=" + server + "; clients.size()=" + clients.size() + "; wasUpdate=" + wasUpdate + ")";
    }

    /**
	 * This method serves security purposes. Provides an integrity string that will be checked by
	 * the {@link Connection#integrityCheck()} method; thus the application can only be altered if
	 * the source is known. Every class in the {@link keyboardhero} package has an integrity string.
	 * 
	 * @return the string of this class used for integrity checking.
	 */
    static String getIntegrityString() {
        return "K^/a\\s'U*Ds!ah\"ďż˝+m";
    }

    /**
	 * The tester object of this class. It provides a debugging menu and unit tests for this class.
	 * Its only purpose is debugging or testing.
	 */
    static final Tester TESTER = new Tester("Connection", new String[] { "getString()", "connectClient(hostname, port)", "checkClients()", "list clients", "getProxy()", "sendMessage(command, nums, str)", "writeClient(hostname, port, command, nums, str)", "writeClient(id, command, nums, str)", "writeClient(uuid, command, nums, str)" }) {

        String hostname;

        int port;

        void menu(int choice) throws Exception {
            switch(choice) {
                case 5:
                    System.out.println(getString());
                    break;
                case 6:
                    hostname = readString("String hostname");
                    port = readInt("int port");
                    connectClient(new InetSocketAddress(hostname, port));
                    break;
                case 7:
                    checkClients();
                    break;
                case 8:
                    Util.debug("LISTING CLIENTS");
                    Util.debug("id\t|\tUUID\t|\tclosed\t|\tstayOpen\t|\taddress\t|\tremoteAddress\t|\tplayer");
                    int i = 0;
                    synchronized (clients) {
                        for (Client client : clients) {
                            Util.debug(i++ + ": " + client.id + " | " + !client.socket.isClosed() + " | " + client.stayOpen + " | " + client.address + " | " + client.socket.getRemoteSocketAddress() + " | " + client.getPlayer());
                        }
                    }
                    break;
                case 9:
                    System.out.println(getProxy());
                    break;
                case 10:
                    try {
                        sendMessage(readString("char command").charAt(0), readInts("int[] nums"), readString("String str"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 11:
                    hostname = readString("String hostname");
                    port = readInt("int port");
                    synchronized (clients) {
                        for (Client client : clients) {
                            if (hostname.equals(client.address.getHostName()) && port == client.address.getPort()) {
                                writeClient(client, readString("char command").charAt(0), readInts("int[] nums"), readString("String str"));
                                return;
                            }
                        }
                    }
                    break;
                case 12:
                    writeClient(clients.get(readInt("int id")), readString("char command").charAt(0), readInts("int[] nums"), readString("String str"));
                    break;
                case 13:
                    final String uuid = readString("String uuid");
                    synchronized (clients) {
                        for (Client client : clients) {
                            if (uuid.equals(client.id.toString())) {
                                writeClient(client, readString("char command").charAt(0), readInts("int[] nums"), readString("String str"));
                                return;
                            }
                        }
                    }
                    break;
                default:
                    baseMenu(choice);
                    break;
            }
        }

        void runUnitTests() throws Exception {
            higherTestStart("Connection");
            testEq("getIntegrityString()", "K^/a\\s'U*Ds!ah\"ďż˝+m", Connection.getIntegrityString());
            test("integrityCheck()", !integrityCheck());
            higherTestEnd();
        }

        void sandbox() throws Throwable {
            Util.debug("LISTING CLIENTS");
            int i = 0;
            synchronized (clients) {
                for (Client client : clients) {
                    Util.debug(++i + ": " + !client.socket.isClosed() + " | " + client.stayOpen + " | " + client.address + " | " + client.socket.getRemoteSocketAddress() + " | " + client.getPlayer());
                }
            }
        }
    };

    /**
	 * Starts the class's developing menu. If this build is a developer's one it starts the
	 * application in a normal way with the exception that it starts the debugging tool for this
	 * class as well; otherwise exits with an error message.
	 * 
	 * @param args
	 *            the arguments given to the program.
	 * @see KeyboardHero#startApp()
	 */
    public static void main(String[] args) {
        Tester.mainer(args, TESTER);
    }

    /** A secret array used in the {@link #encrypt(String)} method. */
    private static final byte[] ENCB = { 0, -16, -39, 105, -55, -54, 46, 89, 15, 86, -77, 54, 22, -35, -99, -46, 106, 44, 52, -128, -38, -3, 127, -100, -90, -22, -51, 118, -17, -88, -94, 14, 4, -74, -31, -21, -28, 87, 35, -40, -27, 25, -114, -91, 45, 23, -63, -42, -109, -127, 48, 17, -20, -30, 64, 55, 33, -44, 50, 41, 98, 27, -52, -75, -23, -118, -123, -57, -92, -25, -70, 12, 18, -5, -8, -2, 93, 62, 104, 19, 123, 79, 59, 114, 9, 75, 58, 37, -69, -104, -121, 63, 122, 80, 5, 119, 53, 73, 68, 120, 109, 82, -6, -19, 57, -89, 103, -48, 110, -98, 2, -124, -43, 13, -119, -10, 71, 49, -67, -81, 6, 70, -41, -14, 121, 113, 39, -56, 85, -97, -85, -82, -106, -61, -72, -36, 99, 111, 3, -117, -13, -84, 10, -71, 60, 102, 83, -7, 107, 8, -29, 77, -15, -115, -76, 32, 43, 115, -126, -95, -111, 26, 29, 116, -65, -96, -110, 31, 40, -103, 65, 97, 11, -68, 78, -26, 67, -80, -34, -116, -105, -37, -50, 94, -33, -32, -112, -24, 112, -120, 1, 92, 124, 125, -60, 126, 69, -18, -11, -4, 47, -59, -1, -87, 90, 95, 101, -78, 56, 108, -53, -122, -125, -12, 36, 61, 84, -107, 51, 88, -73, 42, 91, -45, 76, -62, 100, -9, 74, -79, 38, 16, -102, -113, 28, -58, -101, 96, -93, -66, 7, 81, -108, -86, -83, -64, 72, -47, 34, 21, 30, 117, -49, 20, 24, 66 };

    /** A secret array used in the {@link #decrypt(String)} method. */
    private static final byte[] DECB = { -109, -79, 30, 84, -17, -62, 83, -38, 61, -14, -63, 11, 51, 25, -86, 105, 58, 32, 38, -80, 114, 89, 4, 52, -39, 41, 104, 108, -105, -114, -19, 1, 37, 31, -98, 110, -60, -85, -104, -23, -99, 75, 115, 2, 13, 116, 3, -9, 49, 101, 79, -118, 26, -65, -95, 92, 6, 15, -58, -40, 45, -10, 111, 36, 117, -82, 97, 5, 66, 73, 107, -61, -1, -124, -123, 82, -66, -102, 54, 124, -21, 119, -113, 95, -71, -16, -81, -6, -89, -126, -108, 53, 7, -115, 50, 56, 57, -94, -75, 22, -92, -88, 47, -59, 59, -64, -103, -93, -76, -25, 69, -100, -127, 24, -5, 12, 85, 70, -13, 99, -54, 19, -26, -55, 71, -107, -53, 74, -128, 62, -18, 10, -96, -34, -8, 112, 21, -44, 14, 44, -57, -15, -97, -120, 103, -77, -56, -49, 125, 121, -116, -83, 126, -87, 33, -67, 106, 34, 122, 39, 27, -72, 120, -90, 86, -41, 102, -2, 40, -69, 93, 28, -111, -84, -122, 72, -78, -11, -70, 90, -110, -32, -117, -73, 80, -24, -42, -46, 16, 87, -51, -37, -74, 42, 127, 48, -30, 68, -7, -12, 118, -31, 100, -43, 96, 23, 46, -47, -35, 113, -27, 18, 88, 0, -119, -91, 91, -121, 76, 94, 63, -52, 55, 77, 109, 43, -68, 8, 98, 78, 17, -22, -50, -125, -112, 20, 81, -28, -20, 9, 60, -3, -45, 29, 35, 123, -101, -33, -29, -4, -36, -48, 64, 65, 67, -106 };
}
