package com.dddforandroid.design;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import test.content.provider.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import com.dddforandroid.database.local.ReadNote;
import com.dddforandroid.database.server.Note;
import com.dddforandroid.database.server.SyncServerDatabase;

public class CurrentTasksActivity extends Activity {

    private ListView list;

    private SyncServerDatabase nosql;

    public static DefaultHttpClient authClient = new DefaultHttpClient();

    public static ImageButton sync;

    public static ProgressDialog progress;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasks_list);
        list = (ListView) findViewById(R.id.mainlist);
        list.setItemsCanFocus(false);
        nosql = new SyncServerDatabase();
        Account account = (Account) getIntent().getExtras().get("account");
        progress = ProgressDialog.show(CurrentTasksActivity.this, "", "Getting data from server");
        new ClientAuthentication(this.getApplicationContext(), nosql).execute(account);
        sync = (ImageButton) findViewById(R.id.sync);
        sync.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                progress = ProgressDialog.show(CurrentTasksActivity.this, "", "Getting data from server");
                new ReadNote(getApplicationContext(), nosql, list).execute(authClient);
                sync.setClickable(false);
            }
        });
    }

    /**
	 * Method for displaying all the notes in the database
	 * 
	 * @throws ParseException
	 */
    private void displayAllNotes() throws ParseException {
        ContentResolver provider = getContentResolver();
        String[] projection = new String[] { Note.ID, Note.NAME, Note.NOTE, Note.DATE };
        Uri queryUri = Note.CONTENT_URI;
        Cursor mCur = provider.query(queryUri, projection, null, null, null);
        if (mCur.moveToFirst()) {
            Note currentNote = new Note();
            do {
                currentNote = getCurrentNote(mCur);
                Toast.makeText(CurrentTasksActivity.this, currentNote.getName() + " " + currentNote.getNote() + " " + currentNote.getDate(), Toast.LENGTH_SHORT).show();
                Log.i("ExampleContentProvider", "displayed " + currentNote.getName() + " " + currentNote.getNote());
            } while (mCur.moveToNext());
        }
        mCur.close();
    }

    /**
	 * Method used to initialize a new Note object with the current Strings from
	 * the database
	 * 
	 * @param mCur
	 *            The cursor used to move through the query results
	 * @return The Note object newly initialized
	 * @throws ParseException
	 */
    private Note getCurrentNote(Cursor mCur) throws ParseException {
        Note note = new Note();
        note.setId(mCur.getString(mCur.getColumnIndex(Note.ID)));
        note.setName(mCur.getString(mCur.getColumnIndex(Note.NAME)));
        note.setNote(mCur.getString(mCur.getColumnIndex(Note.NOTE)));
        String date_str = mCur.getString(mCur.getColumnIndex(Note.DATE));
        DateFormat formatter;
        Date date;
        formatter = new SimpleDateFormat("dd-MMM-yyyy");
        date = (Date) formatter.parse(date_str);
        note.setDate(date);
        return note;
    }

    public class ClientAuthentication extends AsyncTask<Account, Void, DefaultHttpClient> {

        private DefaultHttpClient http_client = new DefaultHttpClient();

        private Context mainActivity;

        private SyncServerDatabase nosql;

        public ClientAuthentication(Context context, SyncServerDatabase nosql) {
            this.mainActivity = context;
            this.nosql = nosql;
        }

        @Override
        protected DefaultHttpClient doInBackground(Account... params) {
            AccountManager accountManager = AccountManager.get(mainActivity);
            Account account = params[0];
            accountManager.getAuthToken(account, "ah", false, new GetAuthTokenCallback(), null);
            return http_client;
        }

        private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {

            public void run(AccountManagerFuture<Bundle> result) {
                Bundle bundle;
                try {
                    bundle = result.getResult();
                    Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                    if (intent != null) {
                        mainActivity.startActivity(intent);
                    } else {
                        String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                        http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
                        HttpGet http_get = new HttpGet("http://3dforandroid.appspot.com/_ah" + "/login?continue=http://localhost/&auth=" + auth_token);
                        HttpResponse response = http_client.execute(http_get);
                        if (response.getStatusLine().getStatusCode() != 302) return;
                        for (Cookie cookie : http_client.getCookieStore().getCookies()) {
                            if (cookie.getName().equals("ACSID")) {
                                authClient = http_client;
                                new ReadNote(mainActivity, nosql, list).execute(http_client);
                            }
                        }
                    }
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
                }
                return;
            }
        }

        ;
    }
}
