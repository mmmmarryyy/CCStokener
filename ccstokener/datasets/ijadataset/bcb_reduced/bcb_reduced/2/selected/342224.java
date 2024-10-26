package android.image;

import java.util.Hashtable;
import java.net.URL;
import android.graphics.*;

/** Represents an image from a URL.  The image is loaded when created. 
 * 
 * In order to load images from a URL your Android application must
 *   have the permission to use the Internet.  This permission is set
 *   in the manifest file as:
 *   <tt>&lt;<span style='color:blue'>uses-permission android:name</span>=<span style='color:drakgreen'>
 *   "android.permission.INTERNET"</span> /&gt;</tt>
 */
public class FromURL extends FromFile {

    /** Create an Image from the given URL address */
    public FromURL(String url) {
        try {
            if (loaded.containsKey(url)) {
                this.img = loaded.get(url);
            } else {
                this.img = BitmapFactory.decodeStream(new URL(url).openStream());
                loaded.put(url, this.img);
            }
            this.init(this.img);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error Loading URL Image: \"" + url + "\"\n" + e.getMessage());
        }
    }

    /** Store URL Images, to avoid multiple loads, shadows
     *    <tt>FromFile.loaded</tt> so that URLs and files come
     *    from different name-spaces. */
    protected static Hashtable<String, Bitmap> loaded = new Hashtable<String, Bitmap>();
}
