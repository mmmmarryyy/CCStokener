package com.memoire.foo;

import com.memoire.foo.*;
import java.net.*;
import java.io.*;

public class FooURL {

    private static FooCategory pkg_ = null;

    public static final FooCategory init() {
        if (pkg_ == null) {
            pkg_ = FooCategory.create(URL.class);
            pkg_.setMessage(">>", FooURL.class, "read");
        }
        return pkg_;
    }

    public static byte[] read(URL _url) {
        byte[] r = null;
        try {
            InputStream in = _url.openStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (in.available() > 0) out.write(in.read());
            r = out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(FooLib.getClassName(ex.getClass()) + ":" + ex.getMessage());
        }
        return r;
    }
}
