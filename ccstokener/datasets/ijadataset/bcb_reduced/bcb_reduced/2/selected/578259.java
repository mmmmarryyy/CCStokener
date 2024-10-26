package com.flanderra;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TagDefineBitsJPEG2 implements IOStruct {

    public Map read(BitInputStream bis) throws IOException {
        return new HashMap();
    }

    public Bits write(Map data) throws IOException {
        String pictureurl = (String) data.get("pictureurl");
        URL url = new URL(pictureurl);
        InputStream is = url.openStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int buf = is.read();
        while (buf >= 0) {
            baos.write(buf);
            buf = is.read();
        }
        return BitUtils._concat(BitUtils._bitsUI16(TypeUtils.toLong(data.get("shapeId"))), BitUtils._bytesToBits(baos.toByteArray()));
    }
}
