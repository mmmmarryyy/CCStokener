package android.control;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.AndroidHttpTransport;
import android.util.Log;
import domain.Date;
import domain.Playlist;
import domain.Song;
import domain.User;

public class WebMedia {

    private static final String URL = "http://192.168.1.102:8080/WebMedia/services/WebMedia?wsdl";

    private static final String NAMESPACE = "http://control";

    private static final int SIZE = 1024;

    public static boolean createUser(User user) {
        String method_Name = "createUser";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        PropertyInfo info = new PropertyInfo();
        info.setName("user");
        info.setValue(user);
        info.setType(user.getClass());
        request.addProperty(info);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive result = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean login(String username, String password) {
        String method_Name = "login";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        request.addProperty("password", password);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive result = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateUser(User user) {
        String method_Name = "updateUser";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        PropertyInfo info = new PropertyInfo();
        info.setName("user");
        info.setValue(user);
        info.setType(user.getClass());
        request.addProperty(info);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive result = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean logout(String username) {
        String method_Name = "close";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive result = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean recoverUser(String email) {
        String method_Name = "recoverUser";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("email", email);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            return Boolean.parseBoolean(envelope.getResponse().toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static User getUser(String username) {
        User user = null;
        String method_Name = "getUser";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject resUser = (SoapObject) envelope.getResponse();
            SoapObject resDate = (SoapObject) resUser.getProperty("birthday");
            user = new User(resUser.getProperty("fullName").toString(), new Date(Integer.parseInt(resDate.getProperty("year").toString()), Integer.parseInt(resDate.getProperty("month").toString()), Integer.parseInt(resDate.getProperty("day").toString())), resUser.getProperty("email").toString(), resUser.getProperty("userName").toString(), resUser.getProperty("password").toString(), Integer.parseInt(resUser.getProperty("log").toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    public static boolean createAPlaylist(String username, String playlistNameNew) {
        String method_Name = "createAPlaylist";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        request.addProperty("playlistNameNew", playlistNameNew);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updatePlaylistName(String username, String playlistNameOld, String playlistNameNew) {
        String method_Name = "updatePlaylistName";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        request.addProperty("playlistNameOld", playlistNameOld);
        request.addProperty("playlistNameNew", playlistNameNew);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deletePlaylist(String username, String playlistName) {
        String method_Name = "deletePlaylist";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        request.addProperty("playlistName", playlistName);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ArrayList<Playlist> getAllPlaylistName(String username) {
        ArrayList<Playlist> result = new ArrayList<Playlist>();
        String method_Name = "getAllPlaylistName";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resPlaylist = (SoapObject) response.getProperty(index);
                Playlist playlist = new Playlist(resPlaylist.getProperty("name").toString(), true, new ArrayList<Song>());
                result.add(playlist);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean deleteASongInPlaylist(String username, String playlistName, String name, String performer, String writer) {
        String method_Name = "deleteASongInPlaylist";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        request.addProperty("playlistName", playlistName);
        request.addProperty("name", name);
        request.addProperty("performer", performer);
        request.addProperty("writer", writer);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addASongInPlaylist(String username, String playlistName, String name, String performer, String writer) {
        String method_Name = "addASongInPlaylist";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        request.addProperty("playlistName", playlistName);
        request.addProperty("name", name);
        request.addProperty("performer", performer);
        request.addProperty("writer", writer);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean rateSong(String name, String performer, String writer, int rate) {
        String method_Name = "rateSong";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("name", name);
        request.addProperty("performer", performer);
        request.addProperty("writer", writer);
        request.addProperty("rate", rate);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
            return Boolean.parseBoolean(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean uploadSong(String username, String playlistName, Song song, FileInputStream resource) {
        String method_Name = "createASongInfo";
        String link = "";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("usernname", username);
        request.addProperty("playlistName", playlistName);
        PropertyInfo info = new PropertyInfo();
        info.setName("song");
        info.setValue(song);
        info.setType(song.getClass());
        request.addProperty(info);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            link = envelope.getResponse().toString();
            System.out.println(link);
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            BufferedInputStream inp = new BufferedInputStream(resource);
            BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream());
            byte[] array = new byte[SIZE];
            int read;
            while ((read = inp.read(array)) != -1) {
                out.write(array, 0, read);
            }
            inp.close();
            out.close();
            connection.getResponseCode();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean downloadSong(String link, FileOutputStream dest) {
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            BufferedInputStream inp = new BufferedInputStream(connection.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(dest);
            byte[] array = new byte[SIZE];
            int read;
            while ((read = inp.read(array)) != -1) {
                out.write(array, 0, read);
            }
            inp.close();
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static ArrayList<Song> getAllSongInAPlaylist(String username, String playlistName) {
        ArrayList<Song> result = new ArrayList<Song>();
        String method_Name = "getAllSongInAPlaylist";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("username", username);
        request.addProperty("playlistName", playlistName);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resSong = (SoapObject) response.getProperty(index);
                SoapObject resDate = (SoapObject) resSong.getProperty("date");
                Date date = new Date(Integer.parseInt(resDate.getProperty("year").toString()), Integer.parseInt(resDate.getProperty("month").toString()), Integer.parseInt(resDate.getProperty("day").toString()));
                Song song = new Song(resSong.getProperty("name").toString(), resSong.getProperty("performer").toString(), resSong.getProperty("writer").toString(), resSong.getProperty("genre").toString(), Integer.parseInt(resSong.getProperty("rate").toString()), Long.parseLong(resSong.getProperty("size").toString()), Boolean.parseBoolean(resSong.getProperty("online").toString()), date, resSong.getProperty("resource").toString());
                result.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Errorrr", e.getLocalizedMessage());
        }
        return result;
    }

    public static ArrayList<Song> findByNSong(String name) {
        ArrayList<Song> result = new ArrayList<Song>();
        String method_Name = "findByNSong";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("name", name);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resSong = (SoapObject) response.getProperty(index);
                SoapObject resDate = (SoapObject) resSong.getProperty("date");
                Date date = new Date(Integer.parseInt(resDate.getProperty("year").toString()), Integer.parseInt(resDate.getProperty("month").toString()), Integer.parseInt(resDate.getProperty("day").toString()));
                Song song = new Song(resSong.getProperty("name").toString(), resSong.getProperty("performer").toString(), resSong.getProperty("writer").toString(), resSong.getProperty("genre").toString(), Integer.parseInt(resSong.getProperty("rate").toString()), Long.parseLong(resSong.getProperty("size").toString()), Boolean.parseBoolean(resSong.getProperty("online").toString()), date, resSong.getProperty("resource").toString());
                result.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Errorrr", e.getLocalizedMessage());
        }
        return result;
    }

    public static ArrayList<Song> findByPSong(String performer) {
        ArrayList<Song> result = new ArrayList<Song>();
        String method_Name = "findByPSong";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("performer", performer);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resSong = (SoapObject) response.getProperty(index);
                SoapObject resDate = (SoapObject) resSong.getProperty("date");
                Date date = new Date(Integer.parseInt(resDate.getProperty("year").toString()), Integer.parseInt(resDate.getProperty("month").toString()), Integer.parseInt(resDate.getProperty("day").toString()));
                Song song = new Song(resSong.getProperty("name").toString(), resSong.getProperty("performer").toString(), resSong.getProperty("writer").toString(), resSong.getProperty("genre").toString(), Integer.parseInt(resSong.getProperty("rate").toString()), Long.parseLong(resSong.getProperty("size").toString()), Boolean.parseBoolean(resSong.getProperty("online").toString()), date, resSong.getProperty("resource").toString());
                result.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Errorrr", e.getLocalizedMessage());
        }
        return result;
    }

    public static ArrayList<Song> findByWSong(String writer) {
        ArrayList<Song> result = new ArrayList<Song>();
        String method_Name = "findByPSong";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("writer", writer);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resSong = (SoapObject) response.getProperty(index);
                SoapObject resDate = (SoapObject) resSong.getProperty("date");
                Date date = new Date(Integer.parseInt(resDate.getProperty("year").toString()), Integer.parseInt(resDate.getProperty("month").toString()), Integer.parseInt(resDate.getProperty("day").toString()));
                Song song = new Song(resSong.getProperty("name").toString(), resSong.getProperty("performer").toString(), resSong.getProperty("writer").toString(), resSong.getProperty("genre").toString(), Integer.parseInt(resSong.getProperty("rate").toString()), Long.parseLong(resSong.getProperty("size").toString()), Boolean.parseBoolean(resSong.getProperty("online").toString()), date, resSong.getProperty("resource").toString());
                result.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Errorrr", e.getLocalizedMessage());
        }
        return result;
    }

    public static ArrayList<Song> findByGSong(String genre) {
        ArrayList<Song> result = new ArrayList<Song>();
        String method_Name = "findByGSong";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("genre", genre);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resSong = (SoapObject) response.getProperty(index);
                SoapObject resDate = (SoapObject) resSong.getProperty("date");
                Date date = new Date(Integer.parseInt(resDate.getProperty("year").toString()), Integer.parseInt(resDate.getProperty("month").toString()), Integer.parseInt(resDate.getProperty("day").toString()));
                Song song = new Song(resSong.getProperty("name").toString(), resSong.getProperty("performer").toString(), resSong.getProperty("writer").toString(), resSong.getProperty("genre").toString(), Integer.parseInt(resSong.getProperty("rate").toString()), Long.parseLong(resSong.getProperty("size").toString()), Boolean.parseBoolean(resSong.getProperty("online").toString()), date, resSong.getProperty("resource").toString());
                result.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Errorrr", e.getLocalizedMessage());
        }
        return result;
    }

    public static ArrayList<Song> findByNPWGSong(String find) {
        ArrayList<Song> result = new ArrayList<Song>();
        String method_Name = "findByNPWGSong";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("find", find);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resSong = (SoapObject) response.getProperty(index);
                SoapObject resDate = (SoapObject) resSong.getProperty("date");
                Date date = new Date(Integer.parseInt(resDate.getProperty("year").toString()), Integer.parseInt(resDate.getProperty("month").toString()), Integer.parseInt(resDate.getProperty("day").toString()));
                Song song = new Song(resSong.getProperty("name").toString(), resSong.getProperty("performer").toString(), resSong.getProperty("writer").toString(), resSong.getProperty("genre").toString(), Integer.parseInt(resSong.getProperty("rate").toString()), Long.parseLong(resSong.getProperty("size").toString()), Boolean.parseBoolean(resSong.getProperty("online").toString()), date, resSong.getProperty("resource").toString());
                result.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Errorrr", e.getLocalizedMessage());
        }
        return result;
    }

    public static ArrayList<Playlist> getMainPlaylist() {
        ArrayList<Playlist> result = new ArrayList<Playlist>();
        String method_Name = "getMainPlaylist";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resPlaylist = (SoapObject) response.getProperty(index);
                Playlist playlist = new Playlist(resPlaylist.getProperty("name").toString(), true, new ArrayList<Song>());
                result.add(playlist);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<Song> getAllSongMainPlaylist(String playlistName) {
        ArrayList<Song> result = new ArrayList<Song>();
        String method_Name = "getAllSongMainPlaylist";
        String soap_Action = NAMESPACE + ":" + method_Name;
        SoapObject request = new SoapObject(NAMESPACE, method_Name);
        request.addProperty("playlistName", playlistName);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        AndroidHttpTransport tran = new AndroidHttpTransport(URL);
        try {
            tran.call(soap_Action, envelope);
            SoapObject response = (SoapObject) envelope.bodyIn;
            int count = response.getPropertyCount();
            for (int index = 0; index < count; index++) {
                SoapObject resSong = (SoapObject) response.getProperty(index);
                SoapObject resDate = (SoapObject) resSong.getProperty("date");
                Date date = new Date(Integer.parseInt(resDate.getProperty("year").toString()), Integer.parseInt(resDate.getProperty("month").toString()), Integer.parseInt(resDate.getProperty("day").toString()));
                Song song = new Song(resSong.getProperty("name").toString(), resSong.getProperty("performer").toString(), resSong.getProperty("writer").toString(), resSong.getProperty("genre").toString(), Integer.parseInt(resSong.getProperty("rate").toString()), Long.parseLong(resSong.getProperty("size").toString()), Boolean.parseBoolean(resSong.getProperty("online").toString()), date, resSong.getProperty("resource").toString());
                result.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Errorrr", e.getLocalizedMessage());
        }
        return result;
    }
}
