import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class Omegle implements Runnable {

    public static final Pattern str_regex = Pattern.compile("(\")((?>(?:(?>[^\"\\\\]+)|\\\\.)*))\\1");

    public static final Pattern escape_regex = Pattern.compile("\\\\([\'\"\\\\bfnrt]|u(....))");

    public static final String EV_CONNECTING, EV_WAITING, EV_CONNECTED, EV_TYPING, EV_STOPPED_TYPING, EV_MSG, EV_DISCONNECT;

    public static final String OMEGLE_ROOT = "http://omegle.com/";

    static {
        EV_CONNECTING = "connecting";
        EV_WAITING = "waiting";
        EV_CONNECTED = "connected";
        EV_TYPING = "typing";
        EV_MSG = "gotMessage";
        EV_STOPPED_TYPING = "stoppedTyping";
        EV_DISCONNECT = "strangerDisconnected";
        try {
            InputStream is = Omegle.class.getResourceAsStream("server_name.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String serverName = br.readLine();
            setOmegleRoot("http://" + serverName + "/");
        } catch (Exception ex) {
            setOmegleRoot(OMEGLE_ROOT);
        }
    }

    static URL start_url, events_url, send_url, disc_url, type_url, stoptype_url, count_url;

    private String chatId;

    private boolean dead;

    private List<OmegleListener> listeners;

    public Omegle() {
        chatId = null;
        dead = false;
        listeners = new LinkedList<OmegleListener>();
    }

    public static void setOmegleRoot(String omegle_root) {
        try {
            start_url = new URL(omegle_root + "start");
            events_url = new URL(omegle_root + "events");
            send_url = new URL(omegle_root + "send");
            disc_url = new URL(omegle_root + "disconnect");
            type_url = new URL(omegle_root + "typing");
            stoptype_url = new URL(omegle_root + "stoppedtyping");
            count_url = new URL(omegle_root + "count");
        } catch (MalformedURLException ex) {
        }
    }

    public void addOmegleListener(OmegleListener ol) {
        listeners.add(ol);
    }

    public void removeOmegleListener(OmegleListener ol) {
        listeners.remove(ol);
    }

    public boolean isConnected() {
        return chatId != null;
    }

    public boolean start() {
        if (chatId != null || dead) return false;
        String startr = wget(start_url, true);
        if (startr == null) return false;
        Matcher m = str_regex.matcher(startr);
        if (m.matches()) {
            chatId = m.group(2);
        } else {
            return false;
        }
        new Thread(this).start();
        return true;
    }

    public void run() {
        String eventr;
        while (chatId != null && (eventr = wget(events_url, true, "id", chatId)) != null && !eventr.equals("null")) {
            List<List<String>> events = new LinkedList<List<String>>();
            List<String> currentEvent = null;
            Matcher m = str_regex.matcher(eventr);
            while (m.find()) {
                if (eventr.charAt(m.start() - 1) == '[') {
                    currentEvent = new LinkedList<String>();
                    events.add(currentEvent);
                }
                currentEvent.add(unJsonify(m.group(2)));
            }
            for (List<String> ev : events) {
                String name = ev.remove(0);
                String[] args = ev.toArray(new String[0]);
                for (OmegleListener ol : listeners) ol.eventFired(this, name, args);
            }
        }
        if (chatId != null) {
            chatId = null;
        }
    }

    public boolean typing() {
        if (chatId == null) return false;
        String r = wget(type_url, true, "id", chatId);
        return r != null && r.equals("win");
    }

    public boolean stoppedTyping() {
        if (chatId == null) return false;
        String r = wget(stoptype_url, true, "id", chatId);
        return r != null && r.equals("win");
    }

    public boolean sendMsg(String msg) {
        if (chatId == null) return false;
        String sendr = wget(send_url, true, "id", chatId, "msg", msg);
        if (sendr == null) return false;
        boolean b = sendr.equals("win");
        if (b) {
            for (OmegleListener ol : listeners) ol.messageSent(this, msg);
        }
        return b;
    }

    public boolean disconnect() {
        if (chatId == null) return false;
        String oldChatId = chatId;
        chatId = null;
        String d = wget(disc_url, true, "id", oldChatId);
        boolean b = d != null && d.equals("win");
        if (b) {
            dead = true;
        } else {
            System.out.println("PUT IT BACK, PUT IT BACK");
            chatId = oldChatId;
        }
        return b;
    }

    public void finalize() {
        if (chatId != null) disconnect();
    }

    public static String unJsonify(String jsonString) {
        Matcher m = escape_regex.matcher(jsonString);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String escaped = m.group(1);
            char e = escaped.charAt(0);
            char c;
            switch(e) {
                case '\'':
                case '\"':
                case '\\':
                    c = e;
                    break;
                case 'r':
                    c = '\r';
                    break;
                case 'n':
                    c = '\n';
                    break;
                case 'b':
                    c = '\b';
                    break;
                case 'f':
                    c = '\f';
                    break;
                case 't':
                    c = '\t';
                    break;
                case 'u':
                    String hex = m.group(2);
                    c = (char) Integer.parseInt(hex, 16);
                    break;
                default:
                    c = e;
                    break;
            }
            try {
                m.appendReplacement(sb, "" + c);
            } catch (Exception ex) {
                System.err.println("[" + new Date() + "]:");
                System.err.println("sb = " + sb.toString());
                System.err.println("e = " + e);
                System.err.println("c = " + c);
                System.err.println("escaped = " + escaped);
                System.err.println("m.group(0) = " + m.group(0));
                ex.printStackTrace();
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static int count() {
        try {
            return Integer.parseInt(wget(count_url, false));
        } catch (Exception ex) {
            return 0;
        }
    }

    public static String wget(URL url, boolean post, String... post_data) {
        try {
            URLConnection urlcon = url.openConnection();
            if (post) {
                String msg = "";
                boolean key = false;
                for (String s : post_data) {
                    msg += URLEncoder.encode(s, "UTF-8");
                    if (key = !key) msg += "="; else msg += "&";
                }
                urlcon.setDoOutput(true);
                urlcon.getOutputStream().write(msg.getBytes());
            }
            InputStream urlin = urlcon.getInputStream();
            String data = "";
            int len;
            byte[] buffer = new byte[1023];
            while ((len = urlin.read(buffer)) >= 0) {
                data += new String(buffer, 0, len);
            }
            return data;
        } catch (Exception ex) {
            System.err.println("[" + url + "]:");
            ex.printStackTrace();
            return null;
        }
    }
}

interface OmegleListener {

    public void eventFired(Omegle src, String event, String... args);

    public void messageSent(Omegle src, String msg);
}
