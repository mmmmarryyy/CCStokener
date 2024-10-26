package com.neurogrid.gui.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import java.util.List;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import tristero.tunnel.tcp.TcpForwarder;

/**
 * Copyright (C) 2000 NeuroGrid <sam@neurogrid.com>
 *
 * ------------------------------------------------------------------------------------
 * Transmission class to handle automated sending of emails
 *
 * @author Sam Joseph (gaijin@yha.att.ne.jp)
 *
 * Change History
 * ------------------------------------------------------------------------------------
 * 0.0   13/Jul/2000    sam       Created file
 */
public class Transport {

    private static Transport o_instance = null;

    public static Transport getTransport() {
        if (o_instance == null) o_instance = new Transport();
        return o_instance;
    }

    public static String transmitBrokerList(String p_url, byte[] p_key, byte[] p_real_sig, byte[] p_buffer) throws Exception {
        int x_key_length = p_key.length;
        int x_real_sig_length = p_real_sig.length;
        int x_buffer_length = p_buffer.length;
        System.out.println("T:x_key_length: " + x_key_length);
        System.out.println("T:x_real_sig_length: " + x_real_sig_length);
        System.out.println("T:x_buffer_length: " + x_buffer_length);
        byte[] x_message = new byte[x_key_length + x_real_sig_length + x_buffer_length + 4];
        x_message[0] = (byte) (x_key_length > 256 ? 255 : x_key_length);
        x_message[1] = (byte) (x_key_length > 256 ? x_key_length - 255 : 0);
        x_message[2] = (byte) x_real_sig_length;
        x_message[3] = (byte) x_buffer_length;
        System.arraycopy(p_key, 0, x_message, 4, x_key_length);
        System.arraycopy(p_real_sig, 0, x_message, 4 + x_key_length, x_real_sig_length);
        System.arraycopy(p_buffer, 0, x_message, 4 + x_key_length + x_real_sig_length, x_buffer_length);
        return post(p_url, x_message);
    }

    public static String post(String p_url, List p_messages) throws Exception {
        System.out.println("posting to: " + p_url);
        URL x_url = new URL(p_url);
        URLConnection x_conn = x_url.openConnection();
        x_conn.setDoInput(true);
        x_conn.setDoOutput(true);
        x_conn.setUseCaches(false);
        DataOutputStream x_printout;
        DataInputStream x_input;
        x_printout = new DataOutputStream(x_conn.getOutputStream());
        x_printout.writeLong((long) (p_messages.size()));
        byte[] x_byte_array = null;
        for (int i = 0; i < p_messages.size(); i++) {
            x_byte_array = (byte[]) (p_messages.get(i));
            x_printout.writeLong((long) (x_byte_array.length));
            x_printout.write(x_byte_array, 0, x_byte_array.length);
        }
        x_printout.flush();
        x_printout.close();
        InputStreamReader is_reader = new InputStreamReader(x_conn.getInputStream());
        System.out.println("Got input stream reader");
        BufferedReader reader = new BufferedReader(is_reader);
        System.out.println("Got reader");
        String line = null;
        StringBuffer x_buf = new StringBuffer();
        System.out.println("*******************RESPONSE******************************");
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            x_buf.append(line).append("\n");
        }
        return x_buf.toString();
    }

    public void firewallPost(String p_url, List p_messages, ControllerServlet p_servlet) throws Exception {
        Thread x_contact_broker = new Thread(new BrokerConnection(p_url, p_messages, p_servlet));
        x_contact_broker.start();
    }

    public static byte[] toByteArray(short foo) {
        return toByteArray(foo, new byte[2]);
    }

    public static byte[] toByteArray(int foo) {
        return toByteArray(foo, new byte[4]);
    }

    public static byte[] toByteArray(long foo) {
        return toByteArray(foo, new byte[8]);
    }

    private static byte[] toByteArray(long foo, byte[] array) {
        for (int iInd = 0; iInd < array.length; ++iInd) {
            array[iInd] = (byte) ((foo >> (iInd * 8)) % 0xFF);
        }
        return array;
    }

    /**
  *  a thread that wll maintain connection with the broker
  * 
  */
    private class BrokerConnection implements Runnable {

        String o_url = null;

        List o_messages = null;

        ControllerServlet o_servlet = null;

        public BrokerConnection(String p_url, List p_messages, ControllerServlet p_servlet) {
            o_url = p_url;
            o_messages = p_messages;
            o_servlet = p_servlet;
        }

        public void run() {
            try {
                while (true) {
                    System.out.println("posting to: " + o_url);
                    URL x_url = new URL(o_url);
                    int port = x_url.getPort();
                    if (port == -1) port = 80;
                    Socket o_socket = new Socket(x_url.getHost(), port);
                    BufferedWriter o_out = null;
                    OutputStreamWriter o_out_write = null;
                    BufferedReader o_in = null;
                    InputStreamReader o_in_read = null;
                    InputStream o_is = o_socket.getInputStream();
                    o_in_read = new InputStreamReader(o_is);
                    o_in = new BufferedReader(o_in_read, 2048);
                    OutputStream o_os = o_socket.getOutputStream();
                    o_out_write = new OutputStreamWriter(o_os);
                    o_out = new BufferedWriter(o_out_write, 2048);
                    long x_size = o_messages.size();
                    ByteArrayOutputStream x_bytes = new ByteArrayOutputStream(1024);
                    ByteBuffer x_bbuf = ByteBuffer.allocate(8);
                    x_bbuf.putLong(x_size);
                    x_bbuf.order(ByteOrder.LITTLE_ENDIAN);
                    x_bytes.write(x_bbuf.array());
                    byte[] x_byte_array = null;
                    for (int i = 0; i < x_size; i++) {
                        x_bbuf = ByteBuffer.allocate(8);
                        x_byte_array = (byte[]) (o_messages.get(i));
                        x_bbuf.putLong((long) x_byte_array.length);
                        x_bbuf.order(ByteOrder.LITTLE_ENDIAN);
                        x_bytes.write(x_bbuf.array());
                        x_bytes.write(x_byte_array, 0, x_byte_array.length);
                    }
                    byte[] x_to_send = x_bytes.toByteArray();
                    o_out.write("POST ");
                    o_out.write(x_url.getPath());
                    o_out.write(" HTTP/1.1");
                    o_out.write("\r");
                    o_out.write("\n");
                    o_out.write("User-Agent: Java1.4.0_01");
                    o_out.write("\r");
                    o_out.write("\n");
                    o_out.write("Host: ");
                    o_out.write(x_url.getHost());
                    o_out.write("\r");
                    o_out.write("\n");
                    o_out.write("Accept: text/html, image/gif, image/jpeg");
                    o_out.write("\r");
                    o_out.write("\n");
                    o_out.write("Connection: keep-alive");
                    o_out.write("\r");
                    o_out.write("\n");
                    o_out.write("\r");
                    o_out.write("\n");
                    o_out.flush();
                    o_os.write(x_to_send);
                    o_os.flush();
                    String line = null;
                    Hashtable x_table = new Hashtable();
                    String x_template_name = null;
                    Template x_template = null;
                    Context x_context = null;
                    StringBuffer x_buf = new StringBuffer();
                    System.out.println("*******************RESPONSE******************************");
                    boolean x_first_ping = false;
                    try {
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                        while ((line = o_in.readLine()) != null) {
                            System.out.print("from broker " + o_url + ": ");
                            System.out.println(line);
                            if (line.equals("ping")) {
                                x_first_ping = true;
                                o_out.write("pong");
                                o_out.write("\r");
                                o_out.write("\n");
                                o_out.flush();
                                System.out.println("sent pong in return");
                            } else if (line.startsWith("GET") || line.startsWith("POST")) {
                                System.out.println("x_first_ping == " + x_first_ping);
                                if (x_first_ping == true) {
                                    System.out.println("sending something to remote");
                                    writeOutputToStream(line, o_in, o_out);
                                    System.out.println("Transport: termination");
                                    o_out.write("<firewall-termination>");
                                    o_out.write("\r");
                                    o_out.write("\n");
                                    o_out.flush();
                                }
                            }
                        }
                        System.out.println("Nothing from broker ... reseting");
                        o_out.close();
                        o_in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeOutputToStream(String p_line, BufferedReader p_in, Writer p_out) throws Exception {
        System.out.println("Trying to write to local socket  ... ");
        Socket x_socket = new Socket("localhost", 8080);
        BufferedWriter x_out = null;
        OutputStreamWriter x_out_write = null;
        BufferedReader x_in = null;
        InputStreamReader x_in_read = null;
        InputStream x_is = x_socket.getInputStream();
        x_in_read = new InputStreamReader(x_is);
        x_in = new BufferedReader(x_in_read, 2048);
        OutputStream x_os = x_socket.getOutputStream();
        x_out_write = new OutputStreamWriter(x_os);
        x_out = new BufferedWriter(x_out_write, 2048);
        {
            {
                System.out.println("Transport->: " + p_line);
                x_out.write(p_line);
                x_out.write("\r");
                x_out.write("\n");
                x_out.write("\r");
                x_out.write("\n");
                x_out.write("\r");
                x_out.write("\n");
                x_out.flush();
            }
        }
        while ((p_line = x_in.readLine()) != null) {
            p_out.write(p_line);
            p_out.write("\r");
            p_out.write("\n");
            p_out.flush();
        }
    }

    public static String post(String p_url, byte[] p_message) throws Exception {
        System.out.println("posting to: " + p_url);
        URL x_url = new URL(p_url);
        URLConnection x_conn = x_url.openConnection();
        x_conn.setDoInput(true);
        x_conn.setDoOutput(true);
        x_conn.setUseCaches(false);
        DataOutputStream x_printout;
        DataInputStream x_input;
        x_printout = new DataOutputStream(x_conn.getOutputStream());
        x_printout.write(p_message, 0, p_message.length);
        x_printout.flush();
        x_printout.close();
        InputStreamReader is_reader = new InputStreamReader(x_conn.getInputStream());
        System.out.println("Got input stream reader");
        BufferedReader reader = new BufferedReader(is_reader);
        System.out.println("Got reader");
        String line = null;
        StringBuffer x_buf = new StringBuffer();
        System.out.println("*******************RESPONSE******************************");
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            x_buf.append(line).append("\n");
        }
        return x_buf.toString();
    }

    public static String get(String p_url, String p_message) throws Exception {
        System.out.println("getting from: " + p_url);
        URL x_url = new URL(p_url + p_message);
        System.out.println("get to --> " + x_url.toString());
        URLConnection x_conn = x_url.openConnection();
        InputStreamReader is_reader = new InputStreamReader(x_conn.getInputStream());
        System.out.println("Got input stream reader");
        BufferedReader reader = new BufferedReader(is_reader);
        System.out.println("Got reader");
        String line = null;
        StringBuffer x_buf = new StringBuffer();
        System.out.println("*******************RESPONSE******************************");
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            x_buf.append(line).append("\n");
        }
        return x_buf.toString();
    }

    public static void main(String[] args) {
        try {
            URL x_url = new URL("http://localhost:8080/tristero/servlet/tunnel?method=openTunnel");
            URLConnection x_conn = x_url.openConnection();
            InputStreamReader is_reader = new InputStreamReader(x_conn.getInputStream());
            System.out.println("Got input stream reader");
            BufferedReader reader = new BufferedReader(is_reader);
            System.out.println("Got reader");
            String line = null;
            StringBuffer x_buf = new StringBuffer();
            System.out.println("*******************RESPONSE******************************");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                x_buf.append(line).append("\n");
            }
            String address = x_buf.toString();
            String uri = address.substring("tcp:".length());
            TcpForwarder x_forwarder = new TcpForwarder();
            x_forwarder.forward(uri, "localhost:8080");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
