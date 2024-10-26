package net.disy.legato.net.protocol.classpath;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Hashtable;

public class Handler extends URLStreamHandler {

    private final Class<?> _class;

    public Handler() {
        this._class = getClass();
    }

    public Handler(Class<?> _class) {
        this._class = _class;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new ClassPathURLConnection(url, _class);
    }

    public static void register() throws Exception {
        final Field handlersField = URL.class.getDeclaredField("handlers");
        handlersField.setAccessible(true);
        @SuppressWarnings("unchecked") final Hashtable<String, URLStreamHandler> handlers = (Hashtable<String, URLStreamHandler>) handlersField.get(null);
        handlers.put("classpath", new Handler());
    }
}
