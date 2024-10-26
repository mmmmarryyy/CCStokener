package com.android.zweibo.imgcache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.android.zweibo.ui.R;
import com.android.zweibo.util.MD5Util;

public class ImageCacheManager {

    private Map<String, SoftReference<Bitmap>> imageCache;

    private Context context;

    public static Bitmap getUserDefaultHead;

    public ImageCacheManager(Context context) {
        this.context = context;
        imageCache = new HashMap<String, SoftReference<Bitmap>>();
        getUserDefaultHead = changeToBitmap(context.getResources().getDrawable(R.drawable.user_head));
    }

    /**
	 * 将drawable转换成bitmap
	 * @param drawable
	 * @return
	 */
    public Bitmap changeToBitmap(Drawable drawable) {
        BitmapDrawable bitmap = (BitmapDrawable) drawable;
        return bitmap.getBitmap();
    }

    /**
	 * 判断是否含有图片的url
	 * @param url
	 * @return
	 */
    public boolean contains(String url) {
        return imageCache.containsKey(url);
    }

    public Bitmap getFromCache(String url) {
        Bitmap bitmap = null;
        bitmap = this.getFromSoftCache(url);
        if (null == bitmap) {
            bitmap = getFromFile(url);
        }
        return bitmap;
    }

    /**
	 * 从文件中获取bitmap
	 * @param url
	 * @return
	 */
    private Bitmap getFromFile(String url) {
        String fileName = this.getMD5(url);
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(fileName);
            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * MD5加密
     * @param url
     * @return
     */
    private String getMD5(String url) {
        return MD5Util.getMD5String(url);
    }

    /**
	 * 
	 * @param url
	 * @return
	 */
    public Bitmap safeGet(String url) {
        Bitmap bitmap = this.getFromFile(url);
        if (null != bitmap) {
            synchronized (this) {
                imageCache.put(url, new SoftReference<Bitmap>(bitmap));
            }
            return bitmap;
        }
        return downloadImage(url);
    }

    /**
	 * 网络下载图片
	 * @param url
	 * @return
	 * @throws IOException 
	 */
    private Bitmap downloadImage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String fileName = writeToFile(getMD5(urlStr), conn.getInputStream());
            return BitmapFactory.decodeFile(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 写入到文件
     * @param md5
     * @param inputStream
     * @return
     */
    private String writeToFile(String fileName, InputStream inputStream) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(inputStream);
            bos = new BufferedOutputStream(context.openFileOutput(fileName, Context.MODE_PRIVATE));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != bis) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != bos) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return context.getFilesDir() + "/" + fileName;
    }

    /**
     * 从缓存中获取图片
     */
    private synchronized Bitmap getFromSoftCache(String url) {
        Bitmap bitmap = null;
        SoftReference<Bitmap> refer = null;
        refer = imageCache.get(url);
        if (null != refer) {
            bitmap = refer.get();
        }
        return bitmap;
    }
}
