package com.dddforandroid.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Class containing all public methods of the API, and also auxiliar private
 * methods.
 * */
public class SQLiteBackup {

    public static Class<?> Kind;

    public static DefaultHttpClient authClient = new DefaultHttpClient();

    private ProgressDialog progress;

    private Context mainActivity;

    public final String ID = "id";

    public int auth = 0;

    public static final String AUTH = "authentication";

    public static final String NO_DATA = "No data available for given Database!";

    private DatabaseHelper databaseHelper = null;

    private final String LOG_TAG = getClass().getSimpleName();

    /**
	 * Dismiss the progress dialog when everything is ready
	 * */
    final Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            auth = 1;
            if (progress.isShowing()) progress.dismiss();
        }
    };

    /**
	 * Get the Database helper
	 * */
    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(mainActivity, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    /**
	 * Release the database helper
	 * */
    private void ReleaseHelper() {
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    /**
	 * Initialize the database - sql and server
	 * */
    public <T> void initializeDB(Class<T> Kind, Context mainActivity) {
        SQLiteBackup.Kind = Kind;
        this.mainActivity = mainActivity;
        AccountManager accountManager = AccountManager.get(mainActivity.getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType("com.google");
        if (accounts.length == 0) return;
        Account account = accounts[0];
        progress = ProgressDialog.show(mainActivity, "", "Authenticating...", true);
        try {
            new ClientAuthentication(mainActivity).execute(account).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        showRegistrationId(mainActivity);
    }

    /**
	 * Initialize the SQLite
	 * */
    public Void initializeSQLite() {
        Dao<Object, UUID> objectDao;
        try {
            ConnectionSource connSource = getHelper().getConnectionSource();
            TableUtils.createTableIfNotExists(connSource, SQLiteBackup.Kind);
            objectDao = getHelper().getDataDao();
            List<Object> sqlObjects = objectDao.queryForAll();
            objectDao.delete(sqlObjects);
            sqlObjects = objectDao.queryForAll();
            String serverDatabase = readServerDatabase();
            Log.v("serverDB", serverDatabase);
            if (serverDatabase.equals(NO_DATA)) return null;
            final JsonParser parser = new JsonParser();
            JsonElement jsonElements = null;
            jsonElements = parser.parse(serverDatabase);
            final JsonArray jsonArray = jsonElements.getAsJsonArray();
            for (JsonElement jsonEl : jsonArray) {
                JsonObject row = jsonEl.getAsJsonObject();
                if (row.get(ID).toString().replaceAll("\"", "").equals("null") == false) {
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Initialize the sever's database
	 * */
    private <T> Void initializeServer(String json) {
        String id = "initialize";
        String postResponse = postData(json, authClient, id);
        Log.v("post", "*" + postResponse);
        return null;
    }

    /**
	 * Post the data to the server
	 * */
    private String postData(String result, DefaultHttpClient httpclient, String id) {
        String responseMessage = null;
        try {
            HttpPost post = new HttpPost("http://3dforandroid.appspot.com/api/v2/" + id);
            StringEntity se = new StringEntity(result);
            se.setContentEncoding(HTTP.UTF_8);
            se.setContentType("application/json");
            post.setEntity(se);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "*/*");
            HttpResponse response = httpclient.execute(post);
            HttpEntity entity = response.getEntity();
            InputStream instream;
            instream = entity.getContent();
            responseMessage = read(instream);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    /**
	 * Read the response from the server
	 * 
	 * @param instream
	 *            The server's respons which needs to be parsed
	 * @return Parsed response, as a String
	 */
    private String read(InputStream instream) {
        StringBuilder sb = null;
        try {
            sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(instream));
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            instream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
	 * Create the correct JSON file accepted by the server
	 * 
	 * @param Kind
	 *            The Class of the object
	 * @return The object in JSON form
	 */
    private <T> String createJsonFile(Class<T> Kind) {
        String json = "{\"kind\":\"";
        json += Kind.getSimpleName();
        json += "\",\"property\":[";
        Field[] fields = Kind.getDeclaredFields();
        for (Field field : fields) {
            json += "\"" + field.getName() + "\",";
        }
        json += "\"id\"";
        json += "]}";
        Log.v("main", json);
        return json;
    }

    /**
	 * AsyncTask for inserting an object in SQL and server
	 */
    private class InsertBackup extends AsyncTask<Void, Void, String> {

        private Object insertObject;

        private Context context;

        public InsertBackup(Object insertObject, Context context) {
            this.insertObject = insertObject;
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... params) {
            String id = insertInSQL(insertObject, context);
            Log.v(ID, id);
            insertServer(insertObject, context, id);
            return id;
        }
    }

    /**
	 * Insert on object in the databases
	 * 
	 * @param insertObject
	 *            The object to be inserted
	 * @param context
	 *            The context of the main activity
	 * @return The id of the object, if the insertion was completed, or null if
	 *         not
	 */
    public String insertObject(Object insertObject, Context context) {
        String id = null;
        try {
            id = new InsertBackup(insertObject, context).execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
	 * Insert an object in SQLite
	 * 
	 * @param insertObject
	 *            The object to be inserted in SQLite
	 * @param context
	 *            The context of the main activity
	 * @return The id of the object, if the insertion was completed, or null if
	 *         not
	 */
    private String insertInSQL(Object insertObject, Context context) {
        UUID randomID = UUID.randomUUID();
        String id = randomID.toString();
        try {
            Dao<Object, UUID> objectDao = getHelper().getDataDao();
            objectDao.create(insertObject);
            objectDao.updateId(insertObject, randomID);
            StringBuilder sb = new StringBuilder();
            List<Object> list = objectDao.queryForAll();
            for (Object simple : list) {
                sb.append(" = ").append(simple).append("\n");
            }
        } catch (android.database.SQLException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Log.v("insertInSQL", "am inserat");
        return id;
    }

    /**
	 * Insert an object in the database from the server
	 * 
	 * @param insertObject
	 *            The object to be inserted
	 * @param mainActivity
	 *            The context of the main activity
	 * @param id
	 *            The id of the object taken from SQLite
	 * @return The response from the server
	 */
    private String insertServer(Object insertObject, Context mainActivity, String id) {
        Gson gson = new GsonBuilder().create();
        String jsonInsert = gson.toJson(insertObject);
        jsonInsert = jsonInsert.substring(0, jsonInsert.length() - 1);
        jsonInsert += ",\"" + ID + "\":\"" + id + "\"}";
        Log.v("jsonInsert", jsonInsert);
        String jsonString = createString(jsonInsert);
        Log.v("jsonCreate", jsonString);
        String postId = "create/" + SQLiteBackup.Kind.getSimpleName();
        String postResponse = postData(jsonString, authClient, postId);
        Log.v("post", postResponse);
        return postResponse;
    }

    /**
	 * Create the correct JSON String
	 * 
	 * @param content
	 *            The content of the object
	 * @return The JSON String
	 */
    private String createString(String content) {
        String output = null;
        String begin = "{\"" + SQLiteBackup.Kind.getSimpleName() + "\":[";
        String end = "]}";
        output = begin + content + end;
        return output;
    }

    /**
	 * AsyncTask for authentication
	 */
    private class ClientAuthentication extends AsyncTask<Account, Void, DefaultHttpClient> {

        private DefaultHttpClient http_client = new DefaultHttpClient();

        private Context mainActivity;

        public ClientAuthentication(Context context) {
            this.mainActivity = context;
        }

        @Override
        protected void onPostExecute(DefaultHttpClient result) {
            handler.sendEmptyMessage(0);
        }

        @Override
        protected DefaultHttpClient doInBackground(Account... params) {
            AccountManager accountManager = AccountManager.get(mainActivity);
            Account account = params[0];
            try {
                Bundle bundle = accountManager.getAuthToken(account, "ah", false, null, null).getResult();
                Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (intent != null) {
                    mainActivity.startActivity(intent);
                } else {
                    String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
                    HttpGet http_get = new HttpGet("http://3dforandroid.appspot.com/_ah" + "/login?continue=http://localhost/&auth=" + auth_token);
                    HttpResponse response = http_client.execute(http_get);
                    if (response.getStatusLine().getStatusCode() != 302) return null;
                    for (Cookie cookie : http_client.getCookieStore().getCookies()) {
                        if (cookie.getName().equals("ACSID")) {
                            authClient = http_client;
                            String json = createJsonFile(Kind);
                            initializeSQLite();
                            initializeServer(json);
                        }
                    }
                }
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return http_client;
        }
    }

    /**
	 * Modify an existing Object in SQLite and server's database
	 * 
	 * @param modifyObject
	 *            The object to be modified
	 * @param context
	 *            The context of the main activity
	 * @param id
	 *            The id of the object to be modified
	 * @return true, if the update took place, false if not
	 */
    public boolean modifyObject(Object modifyObject, Context context, String id) {
        boolean modified = false;
        try {
            modified = new ModifyBackup(modifyObject, context).execute(id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return modified;
    }

    /**
	 * AsyncTask for modifying an Object
	 */
    private class ModifyBackup extends AsyncTask<String, Void, Boolean> {

        private Object modifyObject;

        private Context context;

        public ModifyBackup(Object modifyObject, Context context) {
            this.modifyObject = modifyObject;
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... id) {
            boolean modified = modifyInSQL(modifyObject, context, id[0]);
            if (modified) modifyServer(modifyObject, context, id[0]); else return false;
            return true;
        }
    }

    /**
	 * Modify an Object in SQLite
	 * 
	 * @param modifyObject
	 *            The object to be modified
	 * @param context
	 *            The context of the main activity
	 * @param id
	 *            The id of the object to be modified
	 * @return true, if the update took place, false if not
	 */
    private boolean modifyInSQL(Object modifyObject, Context context, String id) {
        try {
            Dao<Object, UUID> objectDao = getHelper().getDataDao();
            objectDao.deleteById(UUID.fromString(id));
            objectDao.create(modifyObject);
            int rowsUpdated = objectDao.updateId(modifyObject, UUID.fromString(id));
            if (rowsUpdated == 1) return true;
        } catch (android.database.SQLException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * Modify an Object in the server's database
	 * 
	 * @param modifiedObject
	 *            The object to be modified
	 * @param context
	 *            The context of the main activity
	 * @param id
	 *            The id of the object to be modified
	 * @return The response from the server
	 */
    private String modifyServer(Object modifiedObject, Context context, String id) {
        Gson gson = new GsonBuilder().create();
        String JSONequiv = gson.toJson(modifiedObject);
        JSONequiv = JSONequiv.substring(0, JSONequiv.length() - 1);
        JSONequiv += ",\"" + ID + "\":\"" + id + "\"}";
        Log.v("modifyServer", JSONequiv);
        String putResponse = putData(id, JSONequiv, authClient);
        Log.v("modifyServer", putResponse);
        return putResponse;
    }

    /**
	 * Call the method PUT from the server
	 * */
    private String putData(String id, String updatedNote, DefaultHttpClient httpclient) {
        String responseMessage = "Error";
        try {
            HttpPut put = new HttpPut("http://3dforandroid.appspot.com/api/v2/" + "update/" + SQLiteBackup.Kind.getSimpleName() + "/" + id);
            StringEntity se = new StringEntity(updatedNote);
            se.setContentEncoding(HTTP.UTF_8);
            se.setContentType("application/json");
            put.setEntity(se);
            put.setHeader("Content-Type", "application/json");
            put.setHeader("Accept", "*/*");
            HttpResponse response = httpclient.execute(put);
            HttpEntity entity = response.getEntity();
            InputStream instream;
            instream = entity.getContent();
            responseMessage = read(instream);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    /**
	 * Delete an Object from the database
	 * 
	 * @param modifiedObject
	 *            The object to be deleted
	 * @param context
	 *            The context of the main activity
	 * @param id
	 *            The id of the object to be deleted
	 * @return true, if the delete took place, false if not
	 */
    public boolean deleteObject(Object deleteObject, Context context, String id) {
        boolean deleted = false;
        try {
            deleted = new DeleteBackup(deleteObject, context).execute(id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return deleted;
    }

    /**
	 * AsyncTask for deleting an Object
	 */
    private class DeleteBackup extends AsyncTask<String, Void, Boolean> {

        private Object deleteObject;

        private Context context;

        public DeleteBackup(Object deleteObject, Context context) {
            this.deleteObject = deleteObject;
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... id) {
            boolean deleted = deleteInSQL(deleteObject, context, id[0]);
            if (deleted) deleteServer(id[0]); else return false;
            return true;
        }
    }

    /**
	 * Delete an object from SQLite
	 * */
    private boolean deleteInSQL(Object deleteObject, Context context, String id) {
        try {
            Dao<Object, UUID> objectDao = getHelper().getDataDao();
            int rowsDeleted = objectDao.deleteById(UUID.fromString(id));
            if (rowsDeleted == 1) return true;
        } catch (android.database.SQLException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * Delete an object from the server's database
	 * */
    private String deleteServer(String deleteId) {
        String deleteResponse = deleteData(deleteId, authClient);
        Log.v("delete", deleteResponse);
        return deleteResponse;
    }

    /**
	 * Call the DELETE method from the server
	 * */
    private String deleteData(String id, DefaultHttpClient httpclient) {
        String responseMessage = "Error";
        try {
            HttpDelete del = new HttpDelete("http://3dforandroid.appspot.com/api/v2/delete/" + SQLiteBackup.Kind.getSimpleName() + "/" + id);
            del.setHeader("Content-Type", "application/json");
            del.setHeader("Accept", "*/*");
            HttpResponse response = httpclient.execute(del);
            HttpEntity entity = response.getEntity();
            InputStream instream;
            instream = entity.getContent();
            responseMessage = read(instream);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    /**
	 * Read the SQL database
	 * */
    public List<Object> readSQLDatabase() {
        List<Object> readObjects = new ArrayList<Object>();
        try {
            readObjects = new ReadSQLBackup().execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return readObjects;
    }

    /**
	 * AsyncTask for reading the SQL database
	 * */
    private class ReadSQLBackup extends AsyncTask<Void, Void, List<Object>> {

        @Override
        protected List<Object> doInBackground(Void... params) {
            List<Object> json = readFromSQL();
            return json;
        }
    }

    /**
	 * Read from SQLite
	 * */
    private List<Object> readFromSQL() {
        try {
            Dao<Object, UUID> objectDao = getHelper().getDataDao();
            List<Object> getList = objectDao.queryForAll();
            return getList;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Read the database from the server
	 * */
    public String readServerDatabase() {
        String readServer = "";
        try {
            readServer = new ReadServerBackup().execute("").get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return readServer;
    }

    /**
	 * Read the object with the given id from the server
	 * */
    public String readServerId(String id) {
        String readServer = "";
        try {
            readServer = new ReadServerBackup().execute("/" + id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return readServer;
    }

    /**
	 * AsynkTask for reading the server's database
	 * */
    private class ReadServerBackup extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... id) {
            String json = getData(authClient, id[0]);
            return json;
        }
    }

    /**
	 * Call the GET method from the server
	 * */
    public String getData(DefaultHttpClient httpclient, String id) {
        String responseMessage = "Error";
        try {
            HttpGet get = new HttpGet("http://3dforandroid.appspot.com/api/v2/get" + id + "?dbName=" + SQLiteBackup.Kind.getSimpleName());
            get.setHeader("Content-Type", "application/json");
            get.setHeader("Accept", "*/*");
            HttpResponse response = httpclient.execute(get);
            HttpEntity entity = response.getEntity();
            InputStream instream = entity.getContent();
            responseMessage = read(instream);
            if (instream != null) instream.close();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    /**
	 * Register the C2DM and get an authentication id
	 * @param context The context of the main activity
	 */
    public void register(Context context) {
        Log.v("C2DM", "start registration process");
        Intent intent = new Intent("com.google.android.c2dm.intent.REGISTER");
        intent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        intent.putExtra("sender", "c2dmreg@gmail.com");
        context.startService(intent);
    }

    /**
	 * Display the C2DM id which is shared in the SharedPreferences
	 * @param context The context of the main activity
	 */
    public void showRegistrationId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String regId = prefs.getString(AUTH, "n/a");
        Toast.makeText(context, regId, Toast.LENGTH_LONG).show();
        Log.v("Preferences", regId);
    }
}
