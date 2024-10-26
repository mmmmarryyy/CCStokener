package abstrasy;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.File;
import java.net.URL;

/**
 * Abstrasy Interpreter
 * 
 * Copyright : Copyright (c) 2006-2012, Luc Bruninx.
 *
 * Concédée sous licence EUPL, version 1.1 uniquement (la «Licence»).
 * 
 * Vous ne pouvez utiliser la présente oeuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 *   http://www.osor.eu/eupl
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous
 * la Licence est distribué "en l’état", SANS GARANTIES OU CONDITIONS QUELLES
 * QU’ELLES SOIENT, expresses ou implicites.
 * 
 * Consultez la Licence pour les autorisations et les restrictions
 * linguistiques spécifiques relevant de la Licence.
 *
 *
 * @author Luc Bruninx
 * @version 1.0
 */
public class SourceFile {

    private String source = null;

    private String path = null;

    private URL url = null;

    public static final String EXT_DEFAULT_D = "Script Abstrasy";

    public static final String EXT_COMPRESSED_D = "Script Abstrasy compressé";

    public static final String EXT_DEFAULT = ".abstrasy";

    public static final String EXT_COMPRESSED = ".abstrasyz";

    public static final String ENCODE = "UTF-8";

    public SourceFile() {
    }

    public SourceFile(String path) {
        setPath(path);
    }

    public SourceFile(URL surl) {
        setURL(surl);
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public URL getURL() {
        return this.url;
    }

    public void setPath(String path) {
        this.path = path;
        try {
            this.url = new File(this.path).toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPath() {
        if (this.path != null) {
            return this.path;
        } else {
            return this.url.toString();
        }
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return this.source;
    }

    public static File findSourceFileName(String spath) {
        String[] ext = { "", EXT_COMPRESSED, EXT_DEFAULT };
        for (int i = 0; i < ext.length; i++) {
            File tf = new File(spath + ext[i]);
            if (tf.exists()) {
                if (tf.isFile()) {
                    return tf;
                }
            }
        }
        return null;
    }

    public static URL findSourceURLName(String spath) {
        String[] ext = { "", EXT_COMPRESSED, EXT_DEFAULT };
        for (int i = 0; i < ext.length; i++) {
            try {
                URL turl = new URL(spath + ext[i]);
                if (turl.openStream() != null) {
                    return turl;
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public void save() throws Exception {
        if (path.endsWith(EXT_DEFAULT)) {
            OutputStream out = new FileOutputStream(path);
            out.write(source.getBytes(ENCODE));
            out.flush();
            out.close();
        } else if (path.endsWith(EXT_COMPRESSED)) {
            byte[] btmp = compress(source.getBytes(ENCODE));
            OutputStream out = new FileOutputStream(path);
            DataOutputStream output = new DataOutputStream(out);
            output.writeInt(btmp.length);
            output.write(btmp);
            output.flush();
            out.flush();
            out.close();
        } else {
            throw new IOException("Nom de fichier non valide : " + path);
        }
    }

    private InputStream getSourceInputStream() {
        InputStream in = null;
        if (url != null) {
            try {
                return url.openStream();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (path != null) {
            try {
                return new FileInputStream(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return in;
    }

    public void load() throws Exception {
        if (getPath().endsWith(EXT_DEFAULT)) {
            InputStream in = getSourceInputStream();
            byte[] buf = new byte[in.available()];
            in.read(buf);
            in.close();
            setSource(new String(buf, ENCODE));
        } else if (getPath().endsWith(EXT_COMPRESSED)) {
            InputStream in = getSourceInputStream();
            DataInputStream input = new DataInputStream(in);
            byte[] buf = new byte[input.readInt()];
            in.read(buf);
            in.close();
            setSource(new String(uncompress(buf), ENCODE));
        } else {
            throw new IOException("Nom de fichier non valide : " + path);
        }
    }

    public byte[] compress(byte[] bytes) throws Exception {
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);
        compressor.setInput(bytes);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
        byte[] buffer = new byte[1024];
        while (!compressor.finished()) {
            int cnt = compressor.deflate(buffer);
            bos.write(buffer, 0, cnt);
        }
        bos.close();
        return bos.toByteArray();
    }

    public byte[] uncompress(byte[] bytes) throws Exception {
        Inflater decompressor = new Inflater();
        decompressor.setInput(bytes);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
        byte[] buffer = new byte[1024];
        while (!decompressor.finished()) {
            int cnt = decompressor.inflate(buffer);
            bos.write(buffer, 0, cnt);
        }
        bos.close();
        return bos.toByteArray();
    }
}
