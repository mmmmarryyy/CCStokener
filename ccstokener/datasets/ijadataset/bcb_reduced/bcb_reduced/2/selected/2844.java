package net.mjrz.fm.actions;

import static net.mjrz.fm.utils.Messages.tr;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import net.mjrz.fm.entity.FManEntityManager;
import net.mjrz.fm.entity.beans.PortfolioEntry;
import net.mjrz.fm.utils.MiscUtils;

public class AddPortfolioEntryAction {

    private static final String URL_QUOTE_DATA = "http://finance.yahoo.com/d/quotes.csv?s=@&f=nl1";

    private static final String QUOTE_URL_LIST[] = { "http://finance.yahoo.com/d/quotes.csv?s=@&f=nl1", "http://finance.yahoo.com/d/quotes.csv?s=@&f=nl1", "http://uk.finance.yahoo.com/d/quotes.csv?s=@&f=nl1", "http://de.finance.yahoo.com/d/quotes.csv?s=@&f=nl1", "http://fr.finance.yahoo.com/d/quotes.csv?s=@&f=nl1", "http://in.finance.yahoo.com/d/quotes.csv?s=@&f=nl1" };

    public AddPortfolioEntryAction() {
    }

    public ActionResponse executeAction(ActionRequest request) throws Exception {
        ActionResponse resp = new ActionResponse();
        try {
            FManEntityManager em = new FManEntityManager();
            String market = (String) request.getProperty("MARKET");
            PortfolioEntry entry = (PortfolioEntry) request.getProperty("ENTRY");
            populatePortfolioEntry(resp, entry, market);
            PortfolioEntry old = em.getPortfolioEntry(entry.getPortfolioId(), entry.getName());
            if (old != null) {
                entry.setId(old.getId());
                entry.setNumShares(old.getNumShares().add(entry.getNumShares()));
            }
            em.addPortfolioEntry(entry);
            resp.addResult("ENTRY", entry);
        } catch (java.net.UnknownHostException e) {
            resp.setErrorCode(ActionResponse.GENERAL_ERROR);
            resp.setErrorMessage(tr("Server not found.\nPlease check network status"));
        } catch (Exception e) {
            resp.setErrorCode(ActionResponse.GENERAL_ERROR);
            resp.setErrorMessage(e.getMessage());
        }
        return resp;
    }

    private String getURL(String market, PortfolioEntry pe) {
        FileReader in = null;
        try {
            Properties props = new Properties();
            in = new FileReader(new File("resources/se.properties"));
            props.load(in);
            String val = props.getProperty(market);
            if (val != null) {
                ArrayList<String> split = MiscUtils.splitString(val, "^");
                if (split.size() != 4) {
                    return null;
                }
                String url = split.get(1);
                String suffix = split.get(2);
                String symbol = pe.getSymbol();
                if (!suffix.equals(".")) {
                    if (!symbol.endsWith("." + suffix)) {
                        symbol += "." + suffix;
                        pe.setSymbol(symbol);
                    }
                }
                return url;
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ;
            }
        }
    }

    private void populatePortfolioEntry(ActionResponse resp, PortfolioEntry e, String market) throws Exception {
        String tmp = getURL(market, e);
        if (tmp == null) {
            tmp = URL_QUOTE_DATA;
        }
        tmp = tmp.replace("@", e.getSymbol());
        URL url = new URL(tmp);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        BufferedReader in = null;
        try {
            int status = conn.getResponseCode();
            if (status == 200) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer responseBody = new StringBuffer();
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    responseBody.append(line);
                }
                ArrayList<String> data = parseData(responseBody.toString(), ",");
                if (data.size() == 2) {
                    e.setName(MiscUtils.trimChars(data.get(0), '"'));
                    String val = data.get(1);
                    val = MiscUtils.trimChars(val.trim(), '\r');
                    val = MiscUtils.trimChars(val.trim(), '\n');
                    BigDecimal d = new BigDecimal(val);
                    e.setPricePerShare(d);
                } else {
                    resp.setErrorCode(ActionResponse.GENERAL_ERROR);
                    resp.setErrorMessage("Error retrieving data");
                }
            } else {
                resp.setErrorCode(ActionResponse.GENERAL_ERROR);
                resp.setErrorMessage("Error retrieving data Http code: " + status);
            }
        } finally {
            if (in != null) in.close();
        }
    }

    private ArrayList<String> parseData(String s, String delim) {
        ArrayList<String> ret = new ArrayList<String>();
        int pos = 0;
        int lastIdx = 0;
        while (pos < s.length()) {
            if (s.charAt(pos) == ',') {
                String tmp = s.substring(lastIdx, pos);
                addToken(ret, tmp);
                lastIdx = pos + 1;
            }
            pos++;
        }
        if (lastIdx < s.length()) {
            ret.add(s.substring(lastIdx));
        }
        return ret;
    }

    private void addToken(ArrayList<String> tokens, String tok) {
        if (tokens.size() == 0) {
            tokens.add(tok);
            return;
        }
        String x = tokens.get(tokens.size() - 1);
        if (x.length() > 0 && x.charAt(0) == '"' && x.charAt(x.length() - 1) != '"') {
            StringBuffer rem = new StringBuffer(tokens.remove(tokens.size() - 1));
            rem.append(tok);
            tokens.add(rem.toString());
        } else {
            tokens.add(tok);
        }
    }
}
