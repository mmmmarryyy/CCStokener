package espider.libs.com.inzyme.jtrm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import espider.libs.com.inzyme.jtrm.typeconv.LittleEndianOutputStream;

public class SigClient {

    protected HttpURLConnection server;

    private static final int SIZEOF_CHAR = 1;

    private static final int SIZEOF_INT = 4;

    private static final int SIZEOF_INT32 = 4;

    private static final byte cGetGUID = 'N';

    private static final int nVersion = 3;

    private String myAddress;

    private int myPort;

    private String myProxyAddress;

    private int myProxyPort;

    public SigClient() {
        this("trm.musicbrainz.org", 4447);
    }

    public SigClient(String _address, int _port) {
        myAddress = _address;
        myPort = _port;
    }

    public byte[] getSignature(AudioSig _signature) throws IOException {
        return getSignature(_signature, "EMPTY_COLLECTION");
    }

    public byte[] getSignature(AudioSig _signature, String _collectionID) throws IOException {
        SigXDR converter = new SigXDR();
        int nGUIDLen = _collectionID.length() * SIZEOF_CHAR + SIZEOF_CHAR;
        int iSigEncodeSize = SIZEOF_INT + AudioSig.NUMSIGFIELDS * SIZEOF_INT32 + nGUIDLen;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream leos = new LittleEndianOutputStream(baos);
        leos.writeSigned8(SigClient.cGetGUID);
        leos.writeSigned32(iSigEncodeSize);
        leos.writeSigned32(SigClient.nVersion);
        iSigEncodeSize -= (nGUIDLen + SIZEOF_INT);
        byte[] fromSig = converter.fromSig(_signature);
        leos.write(fromSig);
        leos.writeNullTerminated(_collectionID);
        leos.close();
        byte[] bytes = baos.toByteArray();
        URL url = new URL("http://" + myAddress + "/cgi-bin/gateway/gateway?" + myPort);
        URLConnection urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        urlConn.setRequestProperty("Content-Type", "application/octet-stream");
        urlConn.setRequestProperty("User-Agent", "libmusicbrainz/2.1.0");
        urlConn.setRequestProperty("Accept", "*//*");
        urlConn.setRequestProperty("Host", "trm.musicbrainz.org");
        OutputStream urlOs = urlConn.getOutputStream();
        try {
            urlOs.write(bytes);
        } finally {
            urlOs.close();
        }
        InputStream is = urlConn.getInputStream();
        try {
            byte[] guid = new byte[16 * 4];
            int pos = 0;
            while (pos < guid.length) {
                pos += is.read(guid, pos, guid.length - pos);
            }
            byte[] strGuid = converter.toStrGUID(guid);
            if (strGuid.length == 0) {
                throw new IOException("Your MusicBrainz client library is too old to talk to\nthe signature server.  Please go to www.musicbrainz.org\nand upgrade to the latest version, or upgrade whatever\nsoftware package your are currently using.\n");
            }
            return strGuid;
        } finally {
            is.close();
        }
    }

    public void setProxy(String _proxyAddress, int _proxyPort) {
        myProxyAddress = _proxyAddress;
        myProxyPort = _proxyPort;
    }
}
