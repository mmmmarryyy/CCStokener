package fr.cephb.lindenb.bio.snp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class RsId implements Comparable<RsId>, Serializable {

    private static final long serialVersionUID = 1L;

    /** general pattern for rs##" */
    public static final Pattern RS_PATTERN = Pattern.compile("rs[1-9][0-9]*", Pattern.CASE_INSENSITIVE);

    private int id;

    public RsId(String rsId) {
        rsId = rsId.trim();
        if (!RS_PATTERN.matcher(rsId).matches()) {
            throw new IllegalArgumentException(rsId + " Doesn't match pattern " + RS_PATTERN.pattern());
        }
        this.id = Integer.parseInt(rsId.substring(2));
    }

    public RsId(int rsId) {
        this.id = rsId;
        if (rsId <= 0) throw new IllegalArgumentException("Bad rsid " + rsId);
    }

    public String getName() {
        return "rs" + getId();
    }

    public int getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return 31 + getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RsId other = (RsId) obj;
        if (id != other.id) return false;
        return true;
    }

    @Override
    public int compareTo(RsId o) {
        return this.getId() - o.getId();
    }

    @Override
    public String toString() {
        return getName();
    }

    /** common activity : reading a list of SNP from a reader */
    public static Set<RsId> readSNPList(Reader r) throws IOException {
        Set<RsId> set = new TreeSet<RsId>();
        String line;
        BufferedReader in = new BufferedReader(r);
        while ((line = in.readLine()) != null) {
            if (line.startsWith("#")) continue;
            line = line.trim();
            if (line.length() == 0) continue;
            set.add(new RsId(line));
        }
        return set;
    }

    /** common activity : reading a list of SNP from a file */
    public static Set<RsId> readSNPList(File file) throws IOException {
        FileReader r = new FileReader(file);
        Set<RsId> set = readSNPList(r);
        r.close();
        return set;
    }

    /** common activity : reading a list of SNP from a file */
    public static Set<RsId> readSNPList(InputStream in) throws IOException {
        return readSNPList(new InputStreamReader(in));
    }

    /** return url in snpdia */
    public String getSNPDiaURL() {
        return "http://www.snpedia.com/index.php?title=rs" + getId();
    }

    public static InputStream openEFetchStream(Set<RsId> rsids) throws IOException {
        StringBuilder sb = new StringBuilder("db=snp&retmode=xml");
        for (RsId rs : rsids) {
            sb.append("&id=" + rs.getId());
        }
        URL url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi");
        URLConnection con = url.openConnection();
        con.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(sb.toString());
        wr.flush();
        wr.close();
        return con.getInputStream();
    }
}
