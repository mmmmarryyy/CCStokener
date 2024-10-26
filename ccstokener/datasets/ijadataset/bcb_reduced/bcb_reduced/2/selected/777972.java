package com.penton.util.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.http.util.ByteArrayBuffer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

/**
 * 帮助你访问 http 资源的工具类
 * 
 * @author <a href="mailto:newcj@qq.com">newcj</a>
 * @version 1.0 2010/5/9
 */
public final class HttpHelper {

    public static final String TAG = "HttpHelper";

    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private static final String ACCEPT = "*/*";

    private static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.1) Gecko/20090624 Firefox/3.5";

    /**
	 * 1024 byte
	 */
    private static final int BUFFER_LENGTH = 1024;

    private String referer;

    private Cookies cookies;

    private int timeout = 300000;

    public HttpHelper() {
        cookies = new Cookies();
    }

    /**
	 * 获取超时时间，毫秒单位，默认为300000毫秒即5分钟
	 * 
	 * @return
	 */
    public int getTimeout() {
        return timeout;
    }

    /**
	 * 设置超时时间 ReadTimeOut 与 ConnectTimeout 均设置为该超时时间，毫秒单位
	 * 
	 * @param timeout
	 */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
	 * 获取 Referer
	 * 
	 * @return
	 */
    public String getReferer() {
        return referer;
    }

    /**
	 * 设置 Referer
	 * 
	 * @return
	 */
    public void setReferer(String referer) {
        this.referer = referer;
    }

    /**
	 * 以GET方法新建一个线程获取网页，编码方式为 gb2312，超时或编码错误返回null
	 * 
	 * @param strUrl
	 *            网页URL地址
	 * @param handler
	 *            用于向发起本次调用的线程发送结果信息
	 * @param what
	 *            handler中的what标记
	 */
    public void getHtmlByThread(String strUrl, Handler handler, int what) {
        getHtmlByThread(strUrl, null, false, "gb2312", handler, what);
    }

    /**
	 * 以GET方法新建一个线程获取网页，超时或编码错误返回null
	 * 
	 * @param strUrl
	 *            网页URL地址
	 * @param encoding
	 *            编码方式
	 * @param handler
	 *            用于向发起本次调用的线程发送结果信息
	 * @param what
	 *            handler中的what标记
	 */
    public void getHtmlByThread(String strUrl, String encoding, Handler handler, int what) {
        getHtmlByThread(strUrl, null, false, encoding, handler, what);
    }

    /**
	 * 根据GET或POST方法新建一个线程获取网页，超时返回null
	 * 
	 * @param strUrl
	 *            网页URL地址
	 * @param strPost
	 *            POST 的数据
	 * @param isPost
	 *            是否 POST，true 则为POST ,false 则为 GET
	 * @param encoding
	 *            编码方式
	 * @param handler
	 *            用于向发起本次调用的线程发送结果信息
	 * @param what
	 *            handler中的what标记
	 */
    public void getHtmlByThread(String strUrl, String strPost, boolean isPost, String encoding, Handler handler, int what) {
        if (handler == null) throw new NullPointerException("handler is null.");
        Thread t = new Thread(new Runner(strUrl, strPost, isPost, encoding, handler, what, Runner.TYPE_HTML));
        t.setDaemon(true);
        t.start();
    }

    /**
	 * 以GET方法获取网页，编码方式为 gb2312，超时或编码错误返回null
	 * 
	 * @param strUrl
	 *            网页URL地址
	 * @return 返回网页的字符串
	 */
    public String getHtml(String strUrl) {
        return getHtml(strUrl, null, false, "gb2312");
    }

    /**
	 * 以GET方法获取网页，超时或编码错误返回null
	 * 
	 * @param strUrl
	 *            网页URL地址
	 * @param encoding
	 *            编码方式
	 * @return 返回网页的字符串
	 */
    public String getHtml(String strUrl, String encoding) {
        return getHtml(strUrl, null, false, encoding);
    }

    /**
	 * 根据GET或POST方法获取网页，超时返回null
	 * 
	 * @param strUrl
	 *            网页URL地址
	 * @param strPost
	 *            POST 的数据
	 * @param isPost
	 *            是否 POST，true 则为POST ,false 则为 GET
	 * @param encoding
	 *            编码方式
	 * @return 返回网页的字符串
	 */
    public String getHtml(String strUrl, String strPost, boolean isPost, String encoding) {
        String ret = null;
        try {
            byte[] data = getHtmlBytes(strUrl, strPost, isPost, encoding);
            if (data != null) ret = new String(data, encoding);
        } catch (UnsupportedEncodingException e) {
        }
        return ret;
    }

    /**
	 * 根据GET或POST方法获取网络数据，超时返回null
	 * 
	 * @param strUrl
	 *            网页URL地址
	 * @param strPost
	 *            POST 的数据
	 * @param isPost
	 *            是否POST，true则为POST,false则为 GET
	 * @param encoding
	 *            编码方式
	 * @return 返回bytes
	 */
    public byte[] getHtmlBytes(String strUrl, String strPost, boolean isPost, String encoding) {
        byte[] ret = null;
        HttpURLConnection httpCon = null;
        InputStream is = null;
        try {
            URL url = new URL(strUrl);
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setReadTimeout(timeout);
            httpCon.setConnectTimeout(timeout);
            httpCon.setUseCaches(false);
            httpCon.setInstanceFollowRedirects(true);
            httpCon.setRequestProperty("Referer", referer);
            httpCon.setRequestProperty("Content-Type", CONTENT_TYPE);
            httpCon.setRequestProperty("Accept", ACCEPT);
            httpCon.setRequestProperty("User-Agent", USER_AGENT);
            httpCon.setRequestProperty("Cookie", cookies.toString());
            if (isPost) {
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("POST");
                httpCon.connect();
                OutputStream os = null;
                try {
                    os = httpCon.getOutputStream();
                    os.write(URLEncoder.encode(strPost, encoding).getBytes());
                    os.flush();
                } finally {
                    if (os != null) os.close();
                }
            }
            is = httpCon.getInputStream();
            ByteArrayBuffer baBuffer = null;
            byte[] buffer = new byte[BUFFER_LENGTH];
            int rNum = 0;
            baBuffer = new ByteArrayBuffer(BUFFER_LENGTH << 1);
            while ((rNum = is.read(buffer)) != -1) {
                baBuffer.append(buffer, 0, rNum);
            }
            ret = baBuffer.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() + ":" + e.getCause());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (httpCon != null) {
                cookies.putCookies(httpCon.getHeaderField("Set-Cookie"));
                referer = strUrl;
                httpCon.disconnect();
            }
        }
        return ret;
    }

    /**
	 * 新建一个线程获取一张网页图片
	 * 
	 * @param strUrl
	 * @param handler
	 *            用于向发起本次调用的线程发送结果信息
	 * @param what
	 *            handler中的what标记
	 */
    public void getBitmapByThread(String strUrl, Handler handler, int what) {
        if (handler == null) throw new NullPointerException("handler is null.");
        Thread t = new Thread(new Runner(strUrl, null, false, null, handler, what, Runner.TYPE_IMG));
        t.setDaemon(true);
        t.start();
    }

    /**
	 * 获取一张网页图片
	 * 
	 * @param strUrl
	 *            网页图片的URL地址
	 * @return
	 */
    public Bitmap getBitmap(String strUrl) {
        byte[] data = getHtmlBytes(strUrl, null, false, null);
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    private class Runner implements Runnable {

        public static final int TYPE_HTML = 1;

        public static final int TYPE_IMG = 2;

        private String strUrl;

        private String strPost;

        private boolean isPost;

        private String encoding;

        private Handler handler;

        private int what;

        private int type;

        public Runner(String strUrl, String strPost, boolean isPost, String encoding, Handler handler, int what, int type) {
            this.strUrl = strUrl;
            this.strPost = strPost;
            this.isPost = isPost;
            this.encoding = encoding;
            this.handler = handler;
            this.what = what;
            this.type = type;
        }

        @Override
        public void run() {
            Object obj = null;
            switch(type) {
                case TYPE_HTML:
                    obj = getHtml(strUrl, strPost, isPost, encoding);
                    break;
                case TYPE_IMG:
                    obj = getBitmap(strUrl);
                    break;
            }
            synchronized (handler) {
                handler.sendMessage(handler.obtainMessage(what, obj));
            }
        }
    }
}
