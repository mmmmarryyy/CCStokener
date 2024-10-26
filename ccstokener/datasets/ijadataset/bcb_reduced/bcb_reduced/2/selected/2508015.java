package net.sf.hdkp.data.io;

import java.io.*;
import java.net.URL;
import javax.script.*;
import net.sf.hdkp.data.*;

public class LuaReader extends Reader {

    private static final String DATA_CLASS = Data.class.getName();

    private static final String TOON_CLASS = Toon.class.getName();

    private static final String CONVERT_SCRIPT = "raids = 0; " + "for name, details in pairs(gdkp.players) do " + "  if (details.rcount > raids) then " + "    raids = details.rcount; " + "  end " + "end " + "data = luajava.newInstance(\"" + DATA_CLASS + "\", DKPInfo.total_points, raids); " + "for name, details in pairs(gdkp.players) do " + "  toon = luajava.newInstance(\"" + TOON_CLASS + "\", name, details.class, details.dkp_current); " + "  data:addToon(toon); " + "end";

    private final Reader rd;

    public LuaReader(File file) throws IOException {
        this(createReader(new FileInputStream(file)));
    }

    public LuaReader(URL url) throws IOException {
        this(createReader(url.openStream()));
    }

    public LuaReader(Reader rd) {
        this.rd = rd;
    }

    private static Reader createReader(InputStream is) throws IOException {
        return new InputStreamReader(is, "UTF-8");
    }

    public Data readData() throws IOException {
        final StringBuilder script = readAll();
        script.append(CONVERT_SCRIPT);
        try {
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine e = mgr.getEngineByExtension(".lua");
            e.eval(script.toString());
            return (Data) e.get("data");
        } catch (Exception e) {
            throw new IOException("Lua script error.", e);
        }
    }

    private StringBuilder readAll() throws IOException {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[1024];
        int len = this.rd.read(buf);
        while (len != -1) {
            sb.append(buf, 0, len);
            len = this.rd.read(buf);
        }
        return sb;
    }

    @Override
    public void close() throws IOException {
        this.rd.close();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        throw new IllegalStateException("Unsupported method.");
    }
}
