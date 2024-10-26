package org.monet.backmobile.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.monet.backmobile.model.ImageInfo;
import org.monet.backmobile.model.field.PictureField;
import org.monet.backmobile.service.ServiceProxy;
import org.monet.backmobile.util.LocalStorage;
import org.monet.backmobile.util.Log;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class ImageProvider {

    private static final int THUMBNAIL_HEIGHT = 82;

    private static final int THUMBNAIL_WIDTH = 82;

    private static final HashMap<String, WeakReference<Drawable>> imageCache = new HashMap<String, WeakReference<Drawable>>();

    private static final HashMap<String, Object> syncMap = new HashMap<String, Object>();

    public static Drawable loadImageThumbnail(String portId, ImageInfo info) {
        Drawable image = null;
        Object syncObject;
        synchronized (syncMap) {
            if (syncMap.containsKey(info.localId)) syncObject = syncMap.get(info.localId); else {
                syncObject = new Object();
                syncMap.put(info.localId, syncObject);
            }
        }
        synchronized (syncObject) {
            if (info.localId != null) {
                image = load(portId, info.thumbUid);
                if (image == null) {
                    Bitmap bitmap = loadFromUrl(info.imageId, portId);
                    File imageCacheDir = LocalStorage.getPortImageCache(portId);
                    File imageFile = new File(imageCacheDir, info.localId + ".jpg");
                    File thumbnailFile = new File(imageCacheDir, info.localId + ".png");
                    saveOnSDCache(bitmap, imageFile, false);
                    Bitmap thumbnail = saveOnSDCache(bitmap, thumbnailFile, true);
                    imageCache.put(info.thumbUid, new WeakReference<Drawable>(new BitmapDrawable(thumbnail)));
                    bitmap.recycle();
                    image = new BitmapDrawable(thumbnail);
                }
            }
        }
        return image;
    }

    public static void removeReference(ImageInfo info) {
        Object syncObject;
        synchronized (syncMap) {
            if (syncMap.containsKey(info.localId)) syncObject = syncMap.get(info.localId); else {
                syncObject = new Object();
                syncMap.put(info.localId, syncObject);
            }
        }
        synchronized (syncObject) {
            WeakReference<Drawable> ref = imageCache.get(info.localId);
            if (ref != null) {
                Drawable image = ref.get();
                if (image != null) image.setCallback(null);
            }
            ref = imageCache.get(info.thumbUid);
            if (ref != null) {
                Drawable image = ref.get();
                if (image != null) image.setCallback(null);
            }
        }
    }

    public static Bitmap saveOnSDCache(Bitmap bitmap, File imageFile, boolean thumbnail) {
        FileOutputStream stream = null;
        Bitmap imageToProcess = null;
        CompressFormat compressFormat = null;
        try {
            stream = new FileOutputStream(imageFile);
            if (thumbnail) {
                imageToProcess = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, false);
                compressFormat = CompressFormat.PNG;
            } else {
                imageToProcess = bitmap;
                compressFormat = CompressFormat.JPEG;
            }
            imageToProcess.compress(compressFormat, 90, stream);
        } catch (Exception e) {
            Log.error(e);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (Exception e2) {
            }
        }
        return imageToProcess;
    }

    private static Drawable load(String portId, String uid) {
        Drawable drawable = null;
        WeakReference<Drawable> reference = imageCache.get(uid);
        if (reference != null) drawable = reference.get();
        if (drawable == null) {
            final Bitmap bitmap = loadFromSdCard(portId, uid);
            if (bitmap != null) drawable = new BitmapDrawable(bitmap);
            imageCache.put(uid, new WeakReference<Drawable>(drawable));
        }
        return drawable;
    }

    private static Bitmap loadFromUrl(String url, String portId) {
        Bitmap bitmap = null;
        final HttpGet get = new HttpGet(url);
        HttpEntity entity = null;
        try {
            final HttpResponse response = ServiceProxy.getInstance(portId).execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                entity = response.getEntity();
                try {
                    InputStream in = entity.getContent();
                    bitmap = BitmapFactory.decodeStream(in);
                } catch (IOException e) {
                    Log.error(e);
                }
            }
        } catch (IOException e) {
            Log.error(e);
        } finally {
            if (entity != null) {
                try {
                    entity.consumeContent();
                } catch (IOException e) {
                    Log.error(e);
                }
            }
        }
        return bitmap;
    }

    private static Bitmap loadFromSdCard(String portId, String uid) {
        File file = new File(LocalStorage.getPortImageCache(portId), uid);
        if (file != null && file.exists()) {
            InputStream stream = null;
            try {
                stream = new FileInputStream(file);
                return BitmapFactory.decodeStream(stream, null, null);
            } catch (FileNotFoundException e) {
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public static void deleteCachedImages(String portId, PictureField field) {
        File imageCacheDir = LocalStorage.getPortImageCache(portId);
        for (ImageInfo info : field.getValue()) {
            new File(imageCacheDir, info.localId).delete();
            imageCache.remove(info.localId);
        }
    }

    public static Uri getUri(String portId, PictureField field, int index) {
        File file = new File(LocalStorage.getPortImageCache(portId), field.getValue().get(index).localId);
        return Uri.fromFile(file);
    }
}
