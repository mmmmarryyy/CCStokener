package be.kuleuven.peno3.mobiletoledo.view.campusnetlogin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import be.kuleuven.peno3.mobiletoledo.R;
import be.kuleuven.peno3.mobiletoledo.Data.UserDAO;
import be.kuleuven.peno3.mobiletoledo.model.User;

public class CampusnetLoginView extends Activity implements Runnable {

    private TextView status;

    public static final String FILENAME = "Settings";

    private ProgressDialog dialog;

    /**
     * Is called by the separate Thread when logging in succeeds or fails.
     */
    private Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 0:
                    status.setText("Login geslaagd");
                    break;
                case 1:
                    status.setText("Login mislukt");
                    break;
                default:
            }
            dialog.dismiss();
        }
    };

    /**
	 * Initialize and setup the Activity. Reload the settings file
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.campusnetloginlayout);
        TextView login_topbar = (TextView) findViewById(R.id.login_bar);
        login_topbar.setText("Login");
        status = (TextView) findViewById(R.string.status);
        Button connect = (Button) findViewById(R.string.ConnectButton);
        Button edit = (Button) findViewById(R.string.EditButton);
        connect.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (loadSettings()) {
                    status.setVisibility(View.VISIBLE);
                    dialog = ProgressDialog.show(CampusnetLoginView.this, "Logging in", "Er wordt ingelogd...");
                    Thread thread = new Thread(CampusnetLoginView.this);
                    thread.start();
                }
            }
        });
        edit.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Intent i = new Intent(CampusnetLoginView.this, EditData.class);
                startActivity(i);
            }
        });
    }

    /**
	 * run-method that attempts to login
	 */
    @Override
    public void run() {
        User user = User.getInstance();
        if (login(user.getIdNumber(), user.getPassword())) {
            myHandler.sendEmptyMessage(0);
        } else {
            myHandler.sendEmptyMessage(1);
        }
    }

    /**
	 * Parses the settings file
	 */
    private boolean loadSettings() {
        UserDAO userManager = new UserDAO(this);
        userManager.open();
        boolean success = userManager.readUserData();
        userManager.close();
        if (!success) {
            Intent i = new Intent(this, EditData.class);
            startActivity(i);
            return false;
        }
        return true;
    }

    /**
	 * Attempt to login on Campusnet
	 * 
	 * @param username username used to log in.
	 * @param password password used to log in.
	 * @return true if login succeeded.
	 */
    private boolean login(String username, String password) {
        String pwdtextfieldname = "";
        try {
            URL url = new URL("https://netlogin.kuleuven.be/cgi-bin/wayf.pl?inst=kuleuven&lang=nl&submit=Ga+verder+/+Continue");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(10 * 1000);
            connection.connect();
            Log.d("Username", username);
            Log.d("Password", password);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            boolean success = false;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("autocomplete") != -1) {
                    int start = line.indexOf("name=\"") + 6;
                    line = line.substring(start);
                    int end = line.indexOf("\"");
                    line = line.substring(0, end);
                    pwdtextfieldname = line;
                    success = true;
                }
            }
            if (!success) return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            URL url = new URL("https://netlogin.kuleuven.be/cgi-bin/netlogin.pl");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setReadTimeout(10 * 1000);
            PrintWriter w = new PrintWriter(connection.getOutputStream());
            w.print("inst=kuleuven&lang=nl&uid=" + username + "&" + pwdtextfieldname + "=" + password + "&submit=Login");
            w.flush();
            w.close();
            connection.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            boolean success = false;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("Login geslaagd") != -1) {
                    success = true;
                }
            }
            if (!success) return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
