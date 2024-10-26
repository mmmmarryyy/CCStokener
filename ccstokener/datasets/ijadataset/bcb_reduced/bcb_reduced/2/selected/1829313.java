package bank.egloff.servlet.client;

import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import bank.*;
import bank.egloff.commands.Command;

public class Driver implements BankDriver {

    private Bank bank;

    private Socket socket;

    private ObjectOutputStream os;

    private InputStream is;

    private URL url;

    private URLConnection connection;

    public void connect(String[] args) {
        try {
            url = new URL("http://localhost:8080/ServletBank/ServletBank");
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        bank = new ServletBank(this);
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bank = null;
    }

    public Bank getBank() {
        return bank;
    }

    private Object readObject() throws ClassNotFoundException {
        int i;
        int state = 0;
        try {
            is = connection.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            return ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Command executeCommand(Command c) {
        try {
            connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            os = new ObjectOutputStream(new BufferedOutputStream(connection.getOutputStream()));
            os.writeObject(c);
            os.flush();
            c = (Command) readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return c;
    }
}
