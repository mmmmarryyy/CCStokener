package orxatas.travelme.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import orxatas.travelme.activity.AsyncActivity;
import orxatas.travelme.databases.TravelmeDatabase;
import orxatas.travelme.databases.exceptions.PhotoNoInLocal;
import orxatas.travelme.entity.Entry;
import orxatas.travelme.entity.Group;
import orxatas.travelme.entity.Photo;
import orxatas.travelme.manager.AccountManager;
import orxatas.travelme.manager.DataManager;
import orxatas.travelme.manager.EntryManager;
import orxatas.travelme.manager.GroupManager;
import orxatas.travelme.manager.PhotoManager;
import orxatas.travelme.sync.SyncData.SyncDataType;
import orxatas.travelme.sync.SyncOptions.ParseAnswer;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

public class SyncEntry extends SyncOptions {

    private EntryManager entryManager;

    private AsyncActivity asyncActivity;

    /**
	 * Base de datos.
	 * */
    private SQLiteDatabase trvlmdb;

    private TravelmeDatabase databaseHelper;

    /**
	 * True si hay una petición de actualización en curso. En caso contrario false.
	 * */
    private static boolean synchronizing = false;

    public SyncEntry(EntryManager entryManager, AsyncActivity asyncActivity) {
        this.entryManager = entryManager;
        this.asyncActivity = asyncActivity;
        databaseHelper = new TravelmeDatabase(asyncActivity.getActivity(), new AccountManager(asyncActivity).getUserLogged().getId());
    }

    private boolean checkIfSyncIsNeeded() {
        return false;
    }

    public void synchronize(SyncDataType type) {
        new DataManager(asyncActivity).syncInProgress();
        new EntrySyncCall().execute();
    }

    private class AnswerEntryList extends Answer<ArrayList<Entry>> {
    }

    private class EntrySyncCall extends AsyncInternetConnection {

        public EntrySyncCall() {
            super(TRVLMWS);
            List<NameValuePair> pairs = basicGETParams(new AccountManager(asyncActivity), METHOD_EM, ACTION_EM_LISTE);
            addGETParams(pairs);
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                Answer<ArrayList<Entry>> answer = new ParseAnswer<AnswerEntryList>().parse(response, true, AnswerEntryList.class);
                if (answer.getState() != 0) {
                    return;
                }
                ArrayList<Entry> entryList = answer.getObj();
                ArrayList<Integer> oldList = getEntryList();
                boolean changesInEntry = false;
                for (Entry newE : entryList) {
                    Entry oldE = null;
                    for (int i = 0; i < oldList.size(); i++) {
                        Entry oe = loadEntryFromDB(oldList.get(i));
                        if (oe.getIdEntryOnline() == newE.getIdEntryOnline()) {
                            oldE = oe;
                            break;
                        }
                    }
                    if (oldE != null) {
                        if (oldE.getLastUpdate() < newE.getLastUpdate()) {
                            ContentValues ve = new ContentValues();
                            ve.put(TravelmeDatabase.ENTRY_CDATE, newE.getDate());
                            ve.put(TravelmeDatabase.ENTRY_CENDED, 1);
                            ve.put(TravelmeDatabase.ENTRY_CFEELING, newE.getFeeling());
                            ve.put(TravelmeDatabase.ENTRY_CLASTUPDATE, newE.getLastUpdate());
                            ve.put(TravelmeDatabase.ENTRY_COPTTEXT, newE.getOpcionalText());
                            ve.put(TravelmeDatabase.ENTRY_CSYNC, 1);
                            trvlmdb = databaseHelper.getWritableDatabase();
                            trvlmdb.update(TravelmeDatabase.ENTRY_TNAME, ve, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + oldE.getIdEntryOffline(), null);
                            trvlmdb.close();
                            changesInEntry = true;
                        }
                    } else {
                        ContentValues ve = new ContentValues();
                        ve.put(TravelmeDatabase.ENTRY_CDATE, newE.getDate());
                        ve.put(TravelmeDatabase.ENTRY_CENDED, 1);
                        ve.put(TravelmeDatabase.ENTRY_CFEELING, newE.getFeeling());
                        ve.put(TravelmeDatabase.ENTRY_CLASTUPDATE, newE.getLastUpdate());
                        ve.put(TravelmeDatabase.ENTRY_COPTTEXT, newE.getOpcionalText());
                        ve.put(TravelmeDatabase.ENTRY_CSYNC, 1);
                        ve.put(TravelmeDatabase.ENTRY_CCREATED, newE.getCreation());
                        ve.put(TravelmeDatabase.ENTRY_CIDAUTOR, newE.getIdAutor());
                        ve.put(TravelmeDatabase.ENTRY_CIDENTRY, newE.getIdEntryOnline());
                        trvlmdb = databaseHelper.getWritableDatabase();
                        Cursor cg = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.GROUP_TNAME + " WHERE " + TravelmeDatabase.GROUP_CID + " = " + newE.getIdGroup(), null);
                        cg.moveToFirst();
                        int idgl = cg.getInt(cg.getColumnIndex(TravelmeDatabase.GROUP_CIDL));
                        cg.close();
                        ve.put(TravelmeDatabase.ENTRY_CIDGROUPL, idgl);
                        ve.put(TravelmeDatabase.ENTRY_CIDPLACE, newE.getIdPlace());
                        trvlmdb.insert(TravelmeDatabase.ENTRY_TNAME, null, ve);
                        trvlmdb.close();
                        changesInEntry = true;
                    }
                }
                for (int i = 0; i < oldList.size(); i++) {
                    Entry olde = loadEntryFromDB(oldList.get(i));
                    boolean was = false;
                    for (Entry ne : entryList) {
                        if (olde.getIdEntryOnline() == ne.getIdEntryOnline()) {
                            was = true;
                            break;
                        }
                    }
                    if (!was) {
                        trvlmdb = databaseHelper.getWritableDatabase();
                        trvlmdb.delete(TravelmeDatabase.ENTRY_TNAME, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + olde.getIdEntryOffline(), null);
                        changesInEntry = true;
                        trvlmdb.close();
                    }
                }
                DataManager dm = new DataManager(asyncActivity);
                dm.syncInProgressEnd();
                dm.asyncNotice(AsyncNoticeCode.ENTRY_CHANGES);
            } catch (WithoutObject e) {
                e.printStackTrace();
            }
        }
    }

    public Entry getEntry(int idEntry) {
        return loadEntryFromDB(idEntry);
    }

    public Entry getEntryNotEnded(int idEntry) {
        trvlmdb = databaseHelper.getReadableDatabase();
        Cursor ce = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.ENTRY_TNAME + " WHERE " + TravelmeDatabase.ENTRY_CIDENTRYL + " = " + idEntry, null);
        int cid_idl = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDENTRYL);
        int cid_ido = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDENTRY);
        int cid_idgroup = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDGROUPL);
        int cid_idautor = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDAUTOR);
        int cid_idplace = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDPLACE);
        int cid_opttext = ce.getColumnIndex(TravelmeDatabase.ENTRY_COPTTEXT);
        int cid_feeling = ce.getColumnIndex(TravelmeDatabase.ENTRY_CFEELING);
        int cid_date = ce.getColumnIndex(TravelmeDatabase.ENTRY_CDATE);
        int cid_created = ce.getColumnIndex(TravelmeDatabase.ENTRY_CCREATED);
        int cid_lastupdate = ce.getColumnIndex(TravelmeDatabase.ENTRY_CLASTUPDATE);
        Entry entry;
        if (ce.moveToFirst()) {
            int idLocal = ce.getInt(cid_idl);
            int idO = ce.getInt(cid_ido);
            int idgroup = ce.getInt(cid_idgroup);
            int idautor = ce.getInt(cid_idautor);
            int idplace = ce.getInt(cid_idplace);
            String opttext = ce.getString(cid_opttext);
            int feeling = ce.getInt(cid_feeling);
            int date = ce.getInt(cid_date);
            int created = ce.getInt(cid_created);
            int lastupdate = ce.getInt(cid_lastupdate);
            entry = new Entry(idLocal, idgroup, idplace, idautor);
            entry.setIdEntryOnline(idO);
            entry.setCreation(created);
            entry.setOpcionalText(opttext);
            entry.setFeeling(feeling);
            entry.setDate(date);
            entry.setLastUpdate(lastupdate);
            Cursor cp = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.PHOTO_TNAME + " WHERE " + TravelmeDatabase.PHOTO_CIDENTRYL + " = " + idLocal, null);
            int cid_idpl = cp.getColumnIndex(TravelmeDatabase.PHOTO_CIDL);
            ArrayList<Integer> pl = new ArrayList<Integer>();
            while (cp.moveToNext()) {
                int idpl = cp.getInt(cid_idpl);
                pl.add(idpl);
            }
            entry.setPhotos(pl);
            cp.close();
        } else entry = null;
        ce.close();
        trvlmdb.close();
        return entry;
    }

    private Entry loadEntryFromDB(int idEntry) {
        trvlmdb = databaseHelper.getReadableDatabase();
        Cursor ce = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.ENTRY_TNAME + " WHERE " + TravelmeDatabase.ENTRY_CENDED + " = 1 AND " + TravelmeDatabase.ENTRY_CIDENTRYL + " = " + idEntry, null);
        int cid_idl = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDENTRYL);
        int cid_ido = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDENTRY);
        int cid_idgroup = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDGROUPL);
        int cid_idautor = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDAUTOR);
        int cid_idplace = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDPLACE);
        int cid_opttext = ce.getColumnIndex(TravelmeDatabase.ENTRY_COPTTEXT);
        int cid_feeling = ce.getColumnIndex(TravelmeDatabase.ENTRY_CFEELING);
        int cid_date = ce.getColumnIndex(TravelmeDatabase.ENTRY_CDATE);
        int cid_created = ce.getColumnIndex(TravelmeDatabase.ENTRY_CCREATED);
        int cid_lastupdate = ce.getColumnIndex(TravelmeDatabase.ENTRY_CLASTUPDATE);
        Entry entry;
        if (ce.moveToFirst()) {
            int idLocal = ce.getInt(cid_idl);
            int idO = ce.getInt(cid_ido);
            int idgroup = ce.getInt(cid_idgroup);
            int idautor = ce.getInt(cid_idautor);
            int idplace = ce.getInt(cid_idplace);
            String opttext = ce.getString(cid_opttext);
            int feeling = ce.getInt(cid_feeling);
            int date = ce.getInt(cid_date);
            int created = ce.getInt(cid_created);
            int lastupdate = ce.getInt(cid_lastupdate);
            entry = new Entry(idLocal, idgroup, idplace, idautor);
            entry.setIdEntryOnline(idO);
            entry.setCreation(created);
            entry.setOpcionalText(opttext);
            entry.setFeeling(feeling);
            entry.setDate(date);
            entry.setLastUpdate(lastupdate);
            Cursor cp = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.PHOTO_TNAME + " WHERE " + TravelmeDatabase.PHOTO_CIDENTRYL + " = " + idLocal, null);
            int cid_idpl = cp.getColumnIndex(TravelmeDatabase.PHOTO_CIDL);
            ArrayList<Integer> pl = new ArrayList<Integer>();
            while (cp.moveToNext()) {
                int idpl = cp.getInt(cid_idpl);
                pl.add(idpl);
            }
            entry.setPhotos(pl);
            cp.close();
        } else entry = null;
        ce.close();
        trvlmdb.close();
        return entry;
    }

    public ArrayList<Integer> getEntryList() {
        trvlmdb = databaseHelper.getReadableDatabase();
        Cursor ce = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.ENTRY_TNAME + " WHERE " + TravelmeDatabase.ENTRY_CENDED + " = 1", null);
        int cid_idl = ce.getColumnIndex(TravelmeDatabase.ENTRY_CIDENTRYL);
        ArrayList<Integer> entryListInt = new ArrayList<Integer>();
        while (ce.moveToNext()) {
            int idLocal = ce.getInt(cid_idl);
            entryListInt.add(idLocal);
        }
        ce.close();
        trvlmdb.close();
        return entryListInt;
    }

    /**
	 * Create a new entry. Mark it as not-ended and not-to-sync
	 * */
    public Entry newEntry(int idGroup, int idPlace) {
        trvlmdb = databaseHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TravelmeDatabase.ENTRY_CIDGROUPL, idGroup);
        v.put(TravelmeDatabase.ENTRY_CIDAUTOR, new AccountManager(asyncActivity).getUserLogged().getId());
        v.put(TravelmeDatabase.ENTRY_CIDPLACE, idPlace);
        int lastID = (int) trvlmdb.insert(TravelmeDatabase.ENTRY_TNAME, null, v);
        trvlmdb.close();
        Entry newEntry = new Entry(lastID, new GroupManager(asyncActivity).getGroup(idGroup).getIdOnline(), idPlace, new AccountManager(asyncActivity).getUserLogged().getId());
        return newEntry;
    }

    public void addFeelingTo(int idEntry, int feeling) {
        trvlmdb = databaseHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TravelmeDatabase.ENTRY_CFEELING, feeling);
        trvlmdb.update(TravelmeDatabase.ENTRY_TNAME, v, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + idEntry, null);
        trvlmdb.close();
    }

    public void addOptionalTextTo(int idEntry, String optionalText) {
        trvlmdb = databaseHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TravelmeDatabase.ENTRY_COPTTEXT, optionalText);
        trvlmdb.update(TravelmeDatabase.ENTRY_TNAME, v, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + idEntry, null);
        trvlmdb.close();
    }

    public void removeOptionalTextOf(int idEntry) {
        trvlmdb = databaseHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TravelmeDatabase.ENTRY_COPTTEXT, "");
        trvlmdb.update(TravelmeDatabase.ENTRY_TNAME, v, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + idEntry, null);
        trvlmdb.close();
    }

    public void addPhotoTo(int idEntry, String pathPhoto) {
        trvlmdb = databaseHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TravelmeDatabase.PHOTO_CIDENTRYL, idEntry);
        v.put(TravelmeDatabase.PHOTO_CLOCALDIR, pathPhoto);
        trvlmdb.insert(TravelmeDatabase.PHOTO_TNAME, null, v);
        trvlmdb.close();
    }

    public void removePhotoOf(int idEntry, int idPhoto) {
        trvlmdb = databaseHelper.getWritableDatabase();
        trvlmdb.delete(TravelmeDatabase.PHOTO_TNAME, TravelmeDatabase.PHOTO_CIDL + " = " + idPhoto + " AND " + TravelmeDatabase.PHOTO_CIDENTRYL + " = " + idEntry, null);
        trvlmdb.close();
    }

    private class AnswerEntry extends Answer<Entry> {
    }

    private class SendEntryCall extends AsyncInternetConnection {

        private Entry entry;

        public SendEntryCall(Entry entry) {
            super(TRVLMWS);
            this.entry = entry;
            List<NameValuePair> pairs = basicGETParams(new AccountManager(asyncActivity), METHOD_EM, ACTION_EM_ADD);
            pairs.add(new BasicNameValuePair("gid", "" + new GroupManager(asyncActivity).getGroup(entry.getIdGroup()).getIdOnline()));
            pairs.add(new BasicNameValuePair("pid", "" + entry.getIdPlace()));
            pairs.add(new BasicNameValuePair("feeling", "" + entry.getFeeling()));
            pairs.add(new BasicNameValuePair("date", "" + entry.getDate()));
            addGETParams(pairs);
            if (!entry.getOpcionalText().equals("")) {
                List<NameValuePair> postpairs = new ArrayList<NameValuePair>();
                postpairs.add(new BasicNameValuePair("text", entry.getOpcionalText()));
                addPOSTParams(postpairs);
            }
        }

        @Override
        protected void onPostExecute(String response) {
            Answer<Entry> answer;
            try {
                answer = new ParseAnswer<AnswerEntry>().parse(response, true, AnswerEntry.class);
                if (answer.getState() != 0) {
                    Log.e("ENTRY", "Bad creation.");
                    return;
                } else {
                    Entry sentry = answer.getObj();
                    trvlmdb = databaseHelper.getWritableDatabase();
                    ContentValues ve = new ContentValues();
                    ve.put(TravelmeDatabase.ENTRY_CSYNC, 1);
                    ve.put(TravelmeDatabase.ENTRY_CIDENTRY, sentry.getIdEntryOnline());
                    ve.put(TravelmeDatabase.ENTRY_CCREATED, sentry.getCreation());
                    ve.put(TravelmeDatabase.ENTRY_CDATE, sentry.getDate());
                    ve.put(TravelmeDatabase.ENTRY_CLASTUPDATE, sentry.getLastUpdate());
                    trvlmdb.update(TravelmeDatabase.ENTRY_TNAME, ve, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + entry.getIdEntryOffline(), null);
                    trvlmdb.close();
                    entry.setIdEntryOnline(sentry.getIdEntryOnline());
                    if (entry.getPhotos().size() > 0) new SendPhotoCall(entry, 0).execute(); else new EndEntryCall(entry).execute();
                }
            } catch (WithoutObject e) {
                Log.e("ENTRY", "Bad creation.");
            }
        }
    }

    /**
	 * Cutre clase para enviar fotos.
	 * */
    private class SendPhotoCall extends AsyncTask<Void, Void, String> {

        private Entry entry;

        /**
		 * URL de la petición.
		 * */
        private final String url;

        private String urlFormated;

        private List<NameValuePair> GETparamList = null;

        private List<NameValuePair> POSTparamList = null;

        private int indexP;

        private String localfilename;

        public SendPhotoCall(Entry entry, int i) {
            this.entry = entry;
            this.indexP = i;
            trvlmdb = databaseHelper.getWritableDatabase();
            Cursor c = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.PHOTO_TNAME + " WHERE " + TravelmeDatabase.PHOTO_CIDL + " = " + entry.getPhotos().get(indexP), null);
            if (!c.moveToFirst()) Log.d("PHOTO", "Error inesperado en las fotos.");
            this.localfilename = c.getString(c.getColumnIndex(TravelmeDatabase.PHOTO_CLOCALDIR));
            c.close();
            trvlmdb.close();
            url = TRVLMWS;
            GETparamList = basicGETParams(new AccountManager(asyncActivity), METHOD_EM, ACTION_EM_ADDI);
            GETparamList.add(new BasicNameValuePair("eid", "" + entry.getIdEntryOnline()));
            POSTparamList = new ArrayList<NameValuePair>();
            POSTparamList.add(new BasicNameValuePair("image", localfilename));
        }

        private void formatCall() {
            if (GETparamList != null && GETparamList.size() > 0) {
                if (!url.endsWith("?")) urlFormated = url + "?";
                urlFormated += URLEncodedUtils.format(GETparamList, "utf-8");
            }
            Log.d("URLWS", urlFormated);
        }

        @Override
        protected void onPreExecute() {
            formatCall();
        }

        private class AnswerPhoto extends Answer<Photo> {
        }

        private String processAnswer(HttpResponse response) {
            if (response != null) {
                try {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        InputStream stream;
                        stream = entity.getContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        stream.close();
                        String responseString = sb.toString();
                        return responseString;
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                Answer<Photo> answer = new ParseAnswer<AnswerPhoto>().parse(response, true, AnswerPhoto.class);
                if (answer.getState() != 0) Log.d("ENTRY", "Bad photo upload!");
                Photo newP = answer.getObj();
                trvlmdb = databaseHelper.getWritableDatabase();
                ContentValues v = new ContentValues();
                v.put(TravelmeDatabase.PHOTO_CSYNC, 1);
                v.put(TravelmeDatabase.PHOTO_CIDO, newP.getIdOnline());
                v.put(TravelmeDatabase.PHOTO_CURL, newP.getUrl());
                v.put(TravelmeDatabase.PHOTO_CREQUESTED, 0);
                v.put(TravelmeDatabase.PHOTO_CLOCALDIR, "");
                trvlmdb.update(TravelmeDatabase.PHOTO_TNAME, v, TravelmeDatabase.PHOTO_CIDL + " = " + entry.getPhotos().get(indexP), null);
                trvlmdb.close();
            } catch (WithoutObject e) {
                Log.d("ENTRY", "Bad photo upload!");
            }
            if (indexP == entry.getPhotos().size() - 1) {
                new EndEntryCall(entry).execute();
            } else {
                new SendPhotoCall(entry, indexP + 1).execute();
            }
            return;
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpPost httpPost = new HttpPost(urlFormated);
            try {
                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                for (int index = 0; index < POSTparamList.size(); index++) {
                    if (POSTparamList.get(index).getName().equalsIgnoreCase("image")) {
                        entity.addPart(POSTparamList.get(index).getName(), new FileBody(new File(POSTparamList.get(index).getValue())));
                    } else {
                        entity.addPart(POSTparamList.get(index).getName(), new StringBody(POSTparamList.get(index).getValue()));
                    }
                }
                httpPost.setEntity(entity);
                HttpResponse response = httpClient.execute(httpPost, localContext);
                return processAnswer(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class EndEntryCall extends AsyncInternetConnection {

        private final Entry entry;

        public EndEntryCall(Entry e) {
            super(TRVLMWS);
            entry = e;
            List<NameValuePair> pairs = basicGETParams(new AccountManager(asyncActivity), METHOD_EM, ACTION_EM_FINISH);
            pairs.add(new BasicNameValuePair("eid", "" + entry.getIdEntryOnline()));
            addGETParams(pairs);
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                Answer<Entry> answer = new ParseAnswer<AnswerEntry>().parse(response, true, AnswerEntry.class);
                if (answer.getState() != 0) {
                    Log.e("ENTRY", "Bad creation of the entry.");
                    return;
                }
                Entry e = answer.getObj();
                trvlmdb = databaseHelper.getReadableDatabase();
                ContentValues v = new ContentValues();
                v.put(TravelmeDatabase.ENTRY_CCREATED, e.getCreation());
                v.put(TravelmeDatabase.ENTRY_CLASTUPDATE, e.getLastUpdate());
                v.put(TravelmeDatabase.ENTRY_CENDED, 1);
                v.put(TravelmeDatabase.ENTRY_CSYNC, 1);
                trvlmdb.update(TravelmeDatabase.ENTRY_TNAME, v, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + e.getIdEntryOffline(), null);
                trvlmdb.close();
                new DataManager(asyncActivity).asyncNotice(AsyncNoticeCode.ENTRY_CHANGES);
                new DataManager(asyncActivity).asyncNotice(AsyncNoticeCode.PHOTO_CHANGES);
            } catch (WithoutObject e) {
                Log.e("ENTRY", "Bad creation of the entry.");
                e.printStackTrace();
            }
        }
    }

    public Entry sendEntry(int idEntry) {
        Entry entry = getEntryNotEnded(idEntry);
        trvlmdb = databaseHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TravelmeDatabase.ENTRY_CENDED, 1);
        v.put(TravelmeDatabase.ENTRY_CDATE, new Date().getTime() / 1000);
        v.put(TravelmeDatabase.ENTRY_CCREATED, new Date().getTime() / 1000);
        trvlmdb.update(TravelmeDatabase.ENTRY_TNAME, v, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + idEntry, null);
        trvlmdb.close();
        new SendEntryCall(entry).execute();
        return entry;
    }

    public void discardEntry(int idEntry) {
        trvlmdb = databaseHelper.getWritableDatabase();
        trvlmdb.delete(TravelmeDatabase.ENTRY_TNAME, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + idEntry + " AND " + TravelmeDatabase.ENTRY_CENDED + " = 0", null);
        trvlmdb.close();
    }

    public void addDateTo(int idEntry, int date) {
        trvlmdb = databaseHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TravelmeDatabase.ENTRY_CDATE, date);
        trvlmdb.update(TravelmeDatabase.ENTRY_TNAME, v, TravelmeDatabase.ENTRY_CIDENTRYL + " = " + idEntry, null);
        trvlmdb.close();
    }
}
