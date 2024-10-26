package de.fhkl.mHelloWorld.implementation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import de.fhkl.helloWorld.implementation.actions.ProfileRequester;
import de.fhkl.helloWorld.interfaces.model.account.configuration.Configuration;
import de.fhkl.helloWorld.interfaces.model.account.hCard.HCard;
import de.fhkl.helloWorld.interfaces.model.account.profile.Contact;
import de.fhkl.helloWorld.interfaces.model.account.profile.EncryptedSubProfile;
import de.fhkl.helloWorld.interfaces.model.account.profile.Profile;
import de.fhkl.helloWorld.interfaces.model.attribute.Attribute;
import de.fhkl.helloWorld.interfaces.model.attribute.profile.StructuredProfileAttribute;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

public class Helper {

    private static final String I = "======================= [HELLO-WORLD] " + "Helper" + ": ";

    /**
	 * IMPORTANT: USE ONLY TEMPORARY!! DELETE THE FUNCTION AFTER DEBUGGING,
	 * BECAUSE THE INPUTSTREAM WILL BE DESTROYED AFTER (WILL BE EMPTY) Writes
	 * the content of the input stream to a <code>String<code>.
	 */
    public static String toString(InputStream inputStream) throws IOException {
        String string;
        StringBuilder outputBuilder = new StringBuilder();
        try {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                while (null != (string = reader.readLine())) {
                    outputBuilder.append(string).append('\n');
                }
            }
        } catch (Exception e) {
            Log.i(I, "Problem: toString(InputStream). Cannot convert.");
        }
        return outputBuilder.toString();
    }

    public static String arrayToString(String[] a) {
        StringBuffer result = new StringBuffer();
        if (a.length > 0) {
            result.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                result.append("; ");
                result.append(a[i]);
            }
        }
        return result.toString();
    }

    /**
	 * Constructs a list of keys and values from user accounts hcard. The list
	 * could be maped to a ListView via SimpleAdapter
	 * 
	 * @return ArrayList which could be maped to a ListView via SimpleAdapter
	 */
    public static ArrayList<HashMap<String, String>> getHCardData(HCard card) {
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> item = new HashMap<String, String>();
        String value = "";
        try {
            value = card.getPhoto().get(0).getValue();
            Log.i(I, "URL: " + value);
        } catch (Exception e) {
            Log.i(I, "Photo-URL not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Photo");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getFn().getValue();
        } catch (Exception e) {
            Log.i(I, "Fullname not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Fullname");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getBday().getValue();
        } catch (Exception e) {
            Log.i(I, "Birthday not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Birthday");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getNickname().get(0).getValue();
        } catch (Exception e) {
            Log.i(I, "Nickname not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Nickname");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getEmail().get(0).getValue();
        } catch (Exception e) {
            Log.i(I, "Email not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Email");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getTel().get(0).getValue();
        } catch (Exception e) {
            Log.i(I, "Tel not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Tel");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getUrl().get(0).getValue();
        } catch (Exception e) {
            Log.i(I, "Url not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Url");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getAdr().get(0).getCountryName().getValue();
        } catch (Exception e) {
            Log.i(I, "CountryName not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Country");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getAdr().get(0).getPostOfficeBox().getValue();
        } catch (Exception e) {
            Log.i(I, "PostOfficeBox not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Post Office Box");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getAdr().get(0).getRegion().getValue();
        } catch (Exception e) {
            Log.i(I, "Region not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Region");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getAdr().get(0).getLocality().getValue();
        } catch (Exception e) {
            Log.i(I, "Locality not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Locality");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getAdr().get(0).getPostalCode().getValue();
        } catch (Exception e) {
            Log.i(I, "Postalcode not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Postalcode");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        try {
            value = card.getAdr().get(0).getExtendedAddress().getValue();
        } catch (Exception e) {
            Log.i(I, "ExtendedAddress not found");
        }
        if (!value.equals("")) {
            item.put("line1", "Extended Address");
            item.put("line2", value);
            list.add(item);
            item = null;
            item = new HashMap<String, String>();
            value = "";
        }
        return list;
    }

    public static Boolean settingsSet() {
        Configuration conf = HelloWorldBasic.userAccount.getConfiguration();
        if ((conf.getFetchMailConncetions().size() == 0) || (conf.getFileTransferConncetions().size() == 0) || (conf.getSendMailConncetions().size() == 0)) {
            return false;
        } else {
            return true;
        }
    }

    public static Bitmap getAndSetBitmapFromNet(String urlPath) {
        Bitmap bm = null;
        if (urlPath != null) {
            try {
                BufferedInputStream bis = new BufferedInputStream(new URL(urlPath).openStream(), 1024);
                final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                BufferedOutputStream out = new BufferedOutputStream(dataStream, 1024);
                copy(bis, out);
                out.flush();
                final byte[] data = dataStream.toByteArray();
                bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                Log.i(I, "data.length: " + data.length);
                out.close();
                dataStream.close();
                bis.close();
                bm = processBitmap(bm);
            } catch (IOException e) {
                Log.i(I, "URL Connection or Bitmap processing Exception");
                e.printStackTrace();
            }
        }
        return bm;
    }

    private static Bitmap processBitmap(Bitmap bm) {
        float width = bm.getWidth();
        float height = bm.getHeight();
        int maxWidth = 150;
        int maxHeight = 150;
        float oldHeight;
        int oldWidth;
        float newWidth;
        float newHeight;
        float schnitt;
        Log.i("==============>", "width: " + width);
        Log.i("==============>", "height: " + height);
        newWidth = maxWidth;
        schnitt = newWidth / width;
        newHeight = (int) (schnitt * height);
        if (newHeight > maxHeight) {
            oldHeight = newHeight;
            oldWidth = (int) newWidth;
            newHeight = maxHeight;
            schnitt = newHeight / oldHeight;
            newWidth = schnitt * oldWidth;
        }
        Log.i("==============>", "New Width: " + newWidth);
        Log.i("==============>", "New Height: " + newHeight);
        bm = Bitmap.createScaledBitmap(bm, (int) newWidth, (int) newHeight, true);
        Log.i(I, "create scaled Bitmap successful");
        return bm;
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[0xFFFF];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    public void getHCardTest() {
        StructuredProfileAttribute<String, Attribute> hcard = HelloWorldBasic.userAccount.getPrivateProfile().getHCard();
        Log.i(I, "test1: " + hcard.getAttribute("bday").getKey().toString());
        Log.i(I, "test2: " + hcard.getAttribute("bday").getType().toString());
    }

    public static String[] getImageUrls() {
        Log.i(I, "entered getContactImageUrls()!!");
        String[] urls = new String[HelloWorldBasic.userAccount.getContacts().size()];
        String imageUrl = null;
        EncryptedSubProfile friendProfile = null;
        ProfileRequester requester = null;
        int i = 0;
        for (Contact c : HelloWorldBasic.userAccount.getContacts()) {
            friendProfile = (EncryptedSubProfile) c.getProfiles().get(0);
            requester = new ProfileRequester(friendProfile);
            urls[i] = "";
            try {
                friendProfile = requester.call();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (friendProfile != null) {
                try {
                    imageUrl = friendProfile.getHCard().getPhoto().get(0).getValue();
                } catch (Exception e) {
                }
                if (imageUrl != null) {
                    urls[i] = imageUrl;
                }
            }
            i++;
        }
        return urls;
    }
}
