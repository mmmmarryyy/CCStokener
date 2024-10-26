package ca.usask.bioinfo.assembly;

import java.io.*;
import java.net.*;
import java.util.*;

public class Assembly {

    protected AssemblySequence consensus;

    protected String probe;

    protected List components;

    protected int maxLength;

    protected int maxName;

    protected int minOffset;

    protected int probeStart;

    protected int probeStop;

    public Assembly(File f) throws IOException {
        this(new FileInputStream(f));
    }

    public Assembly(URL url) throws IOException {
        this(url.openStream());
    }

    public Assembly(InputStream is) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(is, "ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedEncodingException("this platform does not support the ASCII character set");
        }
        String buf, name, info, seq, qual;
        StringTokenizer tok;
        name = in.readLine();
        seq = in.readLine();
        qual = in.readLine();
        consensus = new AssemblySequence(name, 0, seq, qual);
        buf = in.readLine();
        tok = new StringTokenizer(buf, ",");
        try {
            probe = tok.nextToken();
            probeStart = Integer.parseInt(tok.nextToken()) - 1;
            probeStop = Integer.parseInt(tok.nextToken()) - 1;
        } catch (Exception e) {
            throw new RuntimeException("assembly info not in expected format");
        }
        int n = 0;
        try {
            n = Integer.parseInt(in.readLine());
        } catch (Exception e) {
            throw new RuntimeException("assembly info not in expected format");
        }
        maxLength = consensus.sequence.length();
        maxName = consensus.name.length();
        minOffset = 0;
        components = new ArrayList(n);
        for (int i = 0; i < n; ++i) {
            info = in.readLine();
            name = in.readLine();
            seq = in.readLine();
            qual = in.readLine();
            AssemblySequence s = new AssemblySequence(info, name, seq, qual);
            minOffset = Math.min(minOffset, s.offset);
            maxLength = Math.max(maxLength, s.sequence.length() + s.offset);
            maxName = Math.max(maxName, s.name.length());
            components.add(s);
        }
    }

    public boolean inProbe(int pos) {
        return pos >= probeStart && pos <= probeStop;
    }
}
