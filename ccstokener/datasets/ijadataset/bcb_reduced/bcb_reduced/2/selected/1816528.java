package eu.funcnet.clients.goa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class GoaHttpClient {

    private static final String __urlTempl4 = "http://www.ebi.ac.uk/QuickGO/GAnnotation?ancestor=%s&evidence=%s&tax=%d&limit=%d&format=tsv&col=proteinID";

    private static final int __connTimeout = 5 * 1000;

    private static final int __readTimeout = 30 * 1000;

    private static final String __upAccBase = "^(?:[A-NR-Z][0-9][A-Z][A-Z0-9][A-Z0-9][0-9])|(?:[OPQ][0-9][A-Z0-9][A-Z0-9][A-Z0-9][0-9])";

    private static final Pattern __upStdPat = Pattern.compile(__upAccBase);

    private static final Pattern __upVarPat = Pattern.compile(__upAccBase + "[.-][0-9]*");

    public static Set<String> getProteins(final String goCode, final Set<String> evCodes, final int taxon, final int limit) throws IOException {
        final Set<String> proteins = new HashSet<String>();
        HttpURLConnection connection = null;
        try {
            final String evCodeList = join(evCodes);
            final URL url = new URL(String.format(__urlTempl4, goCode, evCodeList, taxon, limit + 1));
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(__connTimeout);
            connection.setReadTimeout(__readTimeout);
            connection.setRequestProperty("Connection", "close");
            connection.connect();
            final BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                proteins.add(line.trim());
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
        return filter(proteins);
    }

    private static String join(final Set<String> strings) {
        final StringBuilder sb = new StringBuilder();
        for (final String string : strings) {
            sb.append(string);
            sb.append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static Set<String> filter(final Set<String> proteins) {
        final Set<String> out = new HashSet<String>();
        for (final String protein : proteins) {
            if (__upStdPat.matcher(protein).matches()) {
                out.add(protein);
            } else if (__upVarPat.matcher(protein).matches()) {
                out.add(protein.substring(0, 6));
            }
        }
        return out;
    }
}
