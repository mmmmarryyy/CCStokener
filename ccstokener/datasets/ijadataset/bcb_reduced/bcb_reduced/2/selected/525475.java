package lojban.vlastezba.definitions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Retrieves definitions from html pages in jbovlaste.lojban.org. Returns null
 * when a definition could not be found, which means another strategy should be
 * tried.
 */
public class JbovlasteClient implements IDefinitionRetrievalStrategy {

    private int max_tries;

    private String url_string;

    public JbovlasteClient(int max_tries, String url_string) {
        this.max_tries = max_tries;
        this.url_string = url_string;
    }

    public JbovlasteClient() {
        this(2, DEFAULT_URL);
    }

    public JbovlasteClient(String url_string) {
        this(DEFAULT_MAX_TRIES, url_string);
    }

    public JbovlasteClient(int max_tries) {
        this(max_tries, DEFAULT_URL);
    }

    public Definition getDefinition(String lojban_word) throws IOException {
        try {
            int tries = 0;
            while (tries < max_tries) {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(encodeUrl(url_string + lojban_word));
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.connect();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = null;
                    boolean english_block_started = false;
                    boolean def_started = false;
                    boolean rafsi_block_started = false;
                    String definition = "";
                    String rafsi = "";
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(WORD_NOT_FOUND_TAG)) {
                            return null;
                        }
                        if (rafsi_block_started) {
                            rafsi_block_started = false;
                            rafsi = line;
                        }
                        if (line.contains(RAFSI_TAG)) {
                            rafsi_block_started = true;
                        }
                        if (line.contains(ENGLISH_BLOCK_START_TAG)) {
                            english_block_started = true;
                        }
                        if (english_block_started && line.contains(DEF_START_TAG)) {
                            def_started = true;
                        }
                        if (def_started) {
                            definition += line;
                        }
                        if (def_started && line.contains(DEF_END_TAG)) {
                            break;
                        }
                    }
                    if (!def_started) {
                        if (tries < max_tries) {
                            tries += 1;
                            continue;
                        } else {
                            return null;
                        }
                    }
                    definition = cleanDefinition(definition);
                    rafsi = cleanType(rafsi);
                    Definition def = new Definition(definition);
                    def.setRafsi(rafsi);
                    return def;
                } catch (SocketTimeoutException e) {
                    tries += 1;
                } catch (Exception e) {
                    tries += 1;
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
            return null;
        } finally {
            try {
                final int _1_second = 1000;
                Thread.sleep(_1_second);
            } catch (InterruptedException e) {
            }
        }
    }

    private String cleanType(String type_text) {
        type_text = type_text.replaceAll(TD_OPEN, "");
        type_text = type_text.replaceAll(TD_CLOSE, "");
        return type_text;
    }

    private String cleanDefinition(String definition) {
        int def_tag_start = definition.indexOf(DEF_START_TAG);
        definition = definition.substring(def_tag_start + DEF_START_TAG.length());
        String table_data_open = "<td>";
        int next_table_data_open_start = definition.indexOf(table_data_open);
        definition = definition.substring(next_table_data_open_start + table_data_open.length());
        definition = definition.substring(0, definition.indexOf("</td>"));
        definition = definition.replaceAll("<sub>", "");
        definition = definition.replaceAll("</sub>", "");
        return definition;
    }

    /**
	 * @return The url string with spaces replaced by %20
	 */
    private String encodeUrl(String urlString) {
        urlString = urlString.replaceAll(" ", "%20");
        return urlString;
    }

    private static final String ENGLISH_BLOCK_START_TAG = "<font size=\"+2\">English</font>";

    private static final String DEF_START_TAG = "<td>definition</td>";

    private static final String DEF_END_TAG = "</tr>";

    private static final String WORD_NOT_FOUND_TAG = "Word not found!";

    private static final String RAFSI_TAG = "<td>rafsi</td>";

    private static final String TD_OPEN = "<td>";

    private static final String TD_CLOSE = "</td>";

    private static final int DEFAULT_MAX_TRIES = 2;

    private static final String DEFAULT_URL = "http://jbovlaste.lojban.org/dict/";
}
