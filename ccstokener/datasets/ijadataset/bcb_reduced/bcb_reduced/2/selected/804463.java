package com.fluendo.player;

import com.fluendo.utils.Base64Converter;
import com.fluendo.utils.Debug;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;

/**
 *
 * @author maik
 */
public class DurationScanner {

    static final int NOTDETECTED = -1;

    static final int UNKNOWN = 0;

    static final int VORBIS = 1;

    static final int THEORA = 2;

    private long contentLength = -1;

    private long responseOffset;

    private Hashtable streaminfo = new Hashtable();

    private SyncState oy = new SyncState();

    private Page og = new Page();

    private Packet op = new Packet();

    public DurationScanner() {
        oy.init();
    }

    private InputStream openWithConnection(URL url, String userId, String password, long offset) throws IOException {
        InputStream dis = null;
        String userAgent = "Cortado";
        URLConnection uc = url.openConnection();
        uc.setRequestProperty("Connection", "Keep-Alive");
        String range;
        if (offset != 0 && contentLength != -1) {
            range = "bytes=" + offset + "-" + (contentLength - 1);
        } else if (offset != 0) {
            range = "bytes=" + offset + "-";
        } else {
            range = null;
        }
        if (range != null) {
            Debug.info("doing range: " + range);
            uc.setRequestProperty("Range", range);
        }
        uc.setRequestProperty("User-Agent", userAgent);
        if (userId != null && password != null) {
            String userPassword = userId + ":" + password;
            String encoding = Base64Converter.encode(userPassword.getBytes());
            uc.setRequestProperty("Authorization", "Basic " + encoding);
        }
        uc.setRequestProperty("Content-Type", "application/octet-stream");
        dis = uc.getInputStream();
        String responseRange = uc.getHeaderField("Content-Range");
        if (responseRange == null) {
            Debug.info("Response contained no Content-Range field, assuming offset=0");
            responseOffset = 0;
        } else {
            try {
                MessageFormat format = new MessageFormat("bytes {0,number}-{1,number}", Locale.US);
                java.lang.Object parts[] = format.parse(responseRange);
                responseOffset = ((Number) parts[0]).longValue();
                if (responseOffset < 0) {
                    responseOffset = 0;
                }
                Debug.debug("Stream successfully with offset " + responseOffset);
            } catch (Exception e) {
                Debug.info("Error parsing Content-Range header");
                responseOffset = 0;
            }
        }
        contentLength = uc.getHeaderFieldInt("Content-Length", -1) + responseOffset;
        return dis;
    }

    private void determineType(Packet packet, StreamInfo info) {
        com.fluendo.jheora.Comment tc = new com.fluendo.jheora.Comment();
        com.fluendo.jheora.Info ti = new com.fluendo.jheora.Info();
        tc.clear();
        ti.clear();
        int ret = ti.decodeHeader(tc, packet);
        if (ret == 0) {
            info.decoder = ti;
            info.type = THEORA;
            info.decodedHeaders++;
            return;
        }
        com.jcraft.jorbis.Comment vc = new com.jcraft.jorbis.Comment();
        com.jcraft.jorbis.Info vi = new com.jcraft.jorbis.Info();
        vc.init();
        vi.init();
        ret = vi.synthesis_headerin(vc, packet);
        if (ret == 0) {
            info.decoder = vi;
            info.type = VORBIS;
            info.decodedHeaders++;
            return;
        }
        info.type = UNKNOWN;
    }

    public float getDurationForBuffer(byte[] buffer, int bufbytes) {
        float time = -1;
        int offset = oy.buffer(bufbytes);
        java.lang.System.arraycopy(buffer, 0, oy.data, offset, bufbytes);
        oy.wrote(bufbytes);
        while (oy.pageout(og) == 1) {
            Integer serialno = new Integer(og.serialno());
            StreamInfo info = (StreamInfo) streaminfo.get(serialno);
            if (info == null) {
                info = new StreamInfo();
                info.streamstate = new StreamState();
                info.streamstate.init(og.serialno());
                streaminfo.put(serialno, info);
                Debug.info("DurationScanner: created StreamState for stream no. " + serialno);
            }
            info.streamstate.pagein(og);
            while (info.streamstate.packetout(op) == 1) {
                int type = info.type;
                if (type == NOTDETECTED) {
                    determineType(op, info);
                    info.startgranule = og.granulepos();
                }
                switch(type) {
                    case VORBIS:
                        {
                            com.jcraft.jorbis.Info i = (com.jcraft.jorbis.Info) info.decoder;
                            float t = (float) (og.granulepos() - info.startgranule) / i.rate;
                            if (t > time) {
                                time = t;
                            }
                        }
                        break;
                    case THEORA:
                        {
                            com.fluendo.jheora.Info i = (com.fluendo.jheora.Info) info.decoder;
                        }
                        break;
                }
            }
        }
        return time;
    }

    public float getDurationForURL(URL url, String user, String password) {
        try {
            int headbytes = 24 * 1024;
            int tailbytes = 128 * 1024;
            float time = 0;
            byte[] buffer = new byte[1024];
            InputStream is = openWithConnection(url, user, password, 0);
            int read = 0;
            long totalbytes = 0;
            read = is.read(buffer);
            while (totalbytes < headbytes && read > 0) {
                totalbytes += read;
                float t = getDurationForBuffer(buffer, read);
                time = t > time ? t : time;
                read = is.read(buffer);
            }
            is.close();
            is = openWithConnection(url, user, password, contentLength - tailbytes);
            if (responseOffset == 0) {
                Debug.warning("DurationScanner: Couldn't complete duration scan due to failing range requests!");
                return -1;
            }
            read = is.read(buffer);
            while (read > 0 && totalbytes < (headbytes + tailbytes) * 2) {
                totalbytes += read;
                float t = getDurationForBuffer(buffer, read);
                time = t > time ? t : time;
                read = is.read(buffer);
            }
            return time;
        } catch (IOException e) {
            Debug.error(e.toString());
            return -1;
        }
    }

    private class StreamInfo {

        public Object decoder;

        public int decodedHeaders = 0;

        public int type = NOTDETECTED;

        public long startgranule;

        public StreamState streamstate;

        public boolean ready = false;
    }

    public static void main(String[] args) throws IOException {
        URL url;
        url = new URL(args[0]);
        System.out.println(new DurationScanner().getDurationForURL(url, null, null));
    }
}
