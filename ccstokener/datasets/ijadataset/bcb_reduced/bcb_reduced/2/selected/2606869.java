package org.mcisb.massspectrometry.mascot;

import java.net.*;
import java.nio.charset.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.mcisb.util.*;
import org.mcisb.util.io.*;
import org.mcisb.util.net.*;

/**
 * This class can submit a query to a Mascot server and return the result.
 * 
 * @author Daniel Jameson
 */
public class MascotQuery extends PropertyChangeSupported {

    /**
	 * 
	 */
    private MascotParameters params;

    /**
	 * 
	 */
    private final File spectrum;

    /**
	 * 
	 */
    private String date;

    /**
	 * 
	 */
    private String mascotFilename;

    /**
	 * 
	 * @param params
	 * @param spectrum
	 */
    public MascotQuery(final MascotParameters params, final File spectrum) {
        this.params = params;
        this.spectrum = spectrum;
        final CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cm);
    }

    /**
	 * 
	 * @throws IOException
	 */
    public void run() throws IOException {
        doLogin();
        doQuery();
    }

    /**
	 * 
	 * @param os
	 * @param dat
	 * @param ignoreIonsScoreBelow 
	 * @param significanceThreshold 
	 * @throws IOException
	 */
    public void getResult(final OutputStream os, final boolean dat, final float ignoreIonsScoreBelow, final float significanceThreshold) throws IOException {
        HttpURLConnection http = null;
        try {
            final StringBuffer url = new StringBuffer();
            if (dat) {
                url.append(params.getServer() + "/x-cgi/ms-status.exe?Show=RESULTFILE&DateDir=" + date + "&ResJob=" + mascotFilename);
            } else {
                url.append(params.getServer() + "/cgi/export_dat_2.pl?");
                url.append(buildString("file", "../data/" + date + "/" + mascotFilename));
                url.append(buildString("do_export", "1"));
                url.append(buildString("prot_hit_num", "1"));
                url.append(buildString("prot_acc", "1"));
                url.append(buildString("pep_query", "1"));
                url.append(buildString("pep_rank", "1"));
                url.append(buildString("pep_isbold", "1"));
                url.append(buildString("pep_exp_mz", "1"));
                url.append(buildString("_showallfromerrortolerant", ""));
                url.append(buildString("_onlyerrortolerant", ""));
                url.append(buildString("_noerrortolerant", ""));
                url.append(buildString("_show_decoy_report", ""));
                url.append(buildString("export_format", "XML"));
                url.append(buildString("_sigthreshold", Float.valueOf(significanceThreshold)));
                url.append(buildString("REPORT", "AUTO"));
                url.append(buildString("_server_mudpit_switch", Double.valueOf((params.isMudPit()) ? 0.000000001 : 99999999)));
                url.append(buildString("_ignoreionsscorebelow", Float.valueOf(ignoreIonsScoreBelow)));
                url.append(buildString("_showsubsets", "1"));
                url.append(buildString("search_master", "1"));
                url.append(buildString("show_header", "1"));
                url.append(buildString("show_mods", "1"));
                url.append(buildString("show_params", "1"));
                url.append(buildString("show_format", "1"));
                url.append(buildString("show_masses", "1"));
                url.append(buildString("protein_master", "1"));
                url.append(buildString("prot_score", "1"));
                url.append(buildString("prot_desc", "1"));
                url.append(buildString("prot_mass", "1"));
                url.append(buildString("prot_matches", "1"));
                url.append(buildString("peptide_master", "1"));
                url.append(buildString("pep_exp_mr", "1"));
                url.append(buildString("pep_exp_z", "1"));
                url.append(buildString("pep_calc_mr", "1"));
                url.append(buildString("pep_delta", "1"));
                url.append(buildString("pep_start", "1"));
                url.append(buildString("pep_end", "1"));
                url.append(buildString("pep_miss", "1"));
                url.append(buildString("pep_score", "1"));
                url.append(buildString("pep_expect", "1"));
                url.append(buildString("pep_seq", "1"));
                url.append(buildString("pep_var_mod", "1"));
                url.append(buildString("pep_scan_title", "1"));
                url.append(buildString("query_master", "1"));
                url.append(buildString("query_title", "1"));
                url.append(buildString("query_qualifiers", "1"));
                url.append(buildString("query_params", "1"));
                url.append(buildString("query_peaks", "1"));
                url.append(buildString("query_raw", "1"));
                url.append(buildString("show_same_sets", "1"));
            }
            http = (HttpURLConnection) new URL(url.toString()).openConnection();
            http.setRequestMethod("GET");
            new StreamReader(http.getInputStream(), new BufferedOutputStream(os)).read();
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    /**
	 * @throws IOException
	 */
    private void doLogin() throws IOException {
        HttpURLConnection http = null;
        try {
            final URL url = new URL(params.getServer() + "/cgi/login.pl");
            http = NetUtils.doPostMultipart(url, getLogin());
            final String response = new String(StreamReader.read(http.getInputStream()), Charset.defaultCharset());
            if (!response.contains("Logged in successfuly")) {
                throw new IOException(response);
            }
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    /**
	 * @throws IOException
	 */
    private void doQuery() throws IOException {
        HttpURLConnection http = null;
        try {
            URL url = new URL(params.getServer() + "/cgi/nph-mascot.exe?1");
            final Map<String, Object> parameters = new LinkedHashMap<String, Object>();
            parameters.putAll(params.getParameters());
            parameters.put("FILE", spectrum);
            http = NetUtils.doPostMultipart(url, parameters);
            final String response = new String(ProgressStreamReader.read(http.getInputStream(), support.getPropertyChangeListeners()), Charset.defaultCharset());
            if (response.contains("Writing results file")) {
                final Pattern pattern = Pattern.compile("file=\\.\\./data/(\\d+)/(\\w+\\.dat)");
                Matcher matcher = pattern.matcher(response);
                matcher.find();
                date = matcher.group(1);
                mascotFilename = matcher.group(2);
            } else {
                throw new IOException(response);
            }
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    /**
	 * 
	 * @return Map<String,Object>
	 */
    private Map<String, Object> getLogin() {
        final Map<String, Object> login = new LinkedHashMap<String, Object>();
        login.put("username", params.getUser());
        login.put("password", params.getPassword());
        login.put("submit", "Login");
        login.put("referer", "");
        login.put("display", "nothing");
        login.put("savecookie", "1");
        login.put("action", "login");
        login.put("userid", "");
        login.put("onerrdisplay", "login_prompt");
        return login;
    }

    /**
	 * 
	 * @param key
	 * @param value
	 * @return String
	 */
    private String buildString(final Object key, final Object value) {
        return ("&" + key + "=" + value);
    }
}
