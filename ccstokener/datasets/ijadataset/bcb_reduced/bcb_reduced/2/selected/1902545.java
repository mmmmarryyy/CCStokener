package org.gdi3d.vrmlloader.impl;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.gdi3d.vrmlloader.VrmlLoader;
import org.gdi3d.vrmlloader.VrmlLoaderSettings;
import org.gdi3d.vrmlloader.WrongMIMETypeException;

/**  Description of the Class */
class ContentNegotiator {

    org.gdi3d.vrmlloader.impl.Sound sound;

    URL url;

    boolean locked;

    byte[] buffer;

    Object content;

    static final int SOUND_LOADER = 1;

    static final int URL_BYTE_LOADER = 2;

    int negotiation = 0;

    WrongMIMETypeException wrongMIMETypeException = null;

    /**
	 *Constructor for the ContentNegotiator object
	 *
	 *@param  url Description of the Parameter
	 */
    ContentNegotiator(URL url) {
        this.url = url;
        this.locked = true;
        negotiation = URL_BYTE_LOADER;
        run();
    }

    /**
	 *Constructor for the ContentNegotiator object
	 *
	 *@param  sound Description of the Parameter
	 */
    ContentNegotiator(Sound sound) {
        this.sound = sound;
        this.locked = true;
        negotiation = SOUND_LOADER;
        run();
    }

    /**  Main processing method for the ContentNegotiator object */
    public void run() {
        startLoading();
    }

    /**
	 *  Gets the content attribute of the ContentNegotiator object
	 *
	 *@return  The content value
	 */
    synchronized Object getContent() throws WrongMIMETypeException {
        if (locked) {
            try {
                wait();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        if (wrongMIMETypeException != null) {
            System.out.println("ContentNegotiator.getContent throwing WrongMIMETypeException");
            throw wrongMIMETypeException;
        }
        return content;
    }

    /**  Description of the Method */
    synchronized void startLoading() {
        if (negotiation == URL_BYTE_LOADER) {
            try {
                VrmlLoaderSettings settings = VrmlLoader.loaderSettings.get(Thread.currentThread());
                String encoding = settings.encoding;
                DataInputStream d;
                int contentLength = -1;
                URLConnection urlc = url.openConnection();
                if (encoding != null) {
                    urlc.setRequestProperty("Authorization", "Basic " + encoding);
                }
                urlc.connect();
                String content_type = urlc.getContentType();
                wrongMIMETypeException = null;
                long time0 = System.currentTimeMillis();
                if (content_type == null || content_type.equalsIgnoreCase("x-world/x-vrml") || content_type.equalsIgnoreCase("model/vrml") || content_type.equalsIgnoreCase("model/vrml;charset=ISO-8859-1")) {
                    InputStream is = urlc.getInputStream();
                    d = new DataInputStream(is);
                    contentLength = urlc.getContentLength();
                    buffer = new byte[contentLength];
                    content = buffer;
                    if (d != null) {
                        d.readFully(buffer, 0, contentLength);
                    }
                } else if (content_type.equalsIgnoreCase("model/vrml.gzip")) {
                    InputStream is = urlc.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    GZIPInputStream gis = new GZIPInputStream(bis);
                    StringBuffer sb = new StringBuffer();
                    BufferedReader zipReader = new BufferedReader(new InputStreamReader(gis));
                    char chars[] = new char[10240];
                    int len = 0;
                    contentLength = 0;
                    while ((len = zipReader.read(chars, 0, chars.length)) >= 0) {
                        sb.append(chars, 0, len);
                        contentLength += len;
                    }
                    chars = null;
                    gis.close();
                    zipReader.close();
                    bis.close();
                    is.close();
                    content = sb.toString().getBytes();
                } else if (content_type.equalsIgnoreCase("model/vrml.encrypted")) {
                    InputStream is = urlc.getInputStream();
                    StringBuffer sb = new StringBuffer();
                    Cipher pbeCipher = createCipher();
                    if (pbeCipher != null) {
                        CipherInputStream cis = new CipherInputStream(is, pbeCipher);
                        BufferedReader bufReader = new BufferedReader(new InputStreamReader(cis));
                        char chars[] = new char[1024];
                        int len = 0;
                        contentLength = 0;
                        while ((len = bufReader.read(chars, 0, chars.length)) >= 0) {
                            sb.append(chars, 0, len);
                            contentLength += len;
                        }
                        chars = null;
                        cis.close();
                        bufReader.close();
                        content = sb.toString().getBytes();
                    }
                } else if (content_type.equalsIgnoreCase("model/vrml.gzip.encrypted")) {
                    InputStream is = urlc.getInputStream();
                    StringBuffer sb = new StringBuffer();
                    Cipher pbeCipher = createCipher();
                    if (pbeCipher != null) {
                        CipherInputStream cis = new CipherInputStream(is, pbeCipher);
                        GZIPInputStream gis = new GZIPInputStream(cis);
                        BufferedReader bufReader = new BufferedReader(new InputStreamReader(gis));
                        char chars[] = new char[1024];
                        int len = 0;
                        contentLength = 0;
                        while ((len = bufReader.read(chars, 0, chars.length)) >= 0) {
                            sb.append(chars, 0, len);
                            contentLength += len;
                        }
                        chars = null;
                        bufReader.close();
                        gis.close();
                        cis.close();
                        content = sb.toString().getBytes();
                    }
                } else if (content_type.equalsIgnoreCase("text/html;charset=utf-8")) {
                    System.out.println("text/html;charset=utf-8");
                    wrongMIMETypeException = new WrongMIMETypeException();
                    wrongMIMETypeException.setMIMEType(content_type);
                    wrongMIMETypeException.setServerError(urlc.getHeaderField(null));
                } else {
                    System.err.println("ContentNegotiator.startLoading unsupported MIME type: " + content_type);
                }
                long time1 = System.currentTimeMillis();
                System.out.println("transmission with " + ((double) contentLength / 1024.0 / (time1 - time0) * 1000.0) + " KBytes/s");
            } catch (IOException ie) {
                System.out.println("ContentNegotiator.startLoading ERROR");
                ie.printStackTrace();
            }
            locked = false;
            notify();
        } else if (negotiation == SOUND_LOADER) {
            content = sound;
            locked = false;
            notify();
        }
    }

    private Cipher createCipher() {
        Cipher pbeCipher = null;
        try {
            byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99 };
            int count = 20;
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
            char[] sdfiRgsdgfed = "gFer4FgwertJk8OSDby543HwddfMoaQ".toCharArray();
            PBEKeySpec pbeKeySpec = new PBEKeySpec(sdfiRgsdgfed);
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
            pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pbeCipher;
    }
}
