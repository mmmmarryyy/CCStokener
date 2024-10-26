package net.mjrz.fm.actions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import net.mjrz.fm.entity.beans.*;
import net.mjrz.fm.entity.*;
import net.mjrz.fm.entity.utils.*;
import net.mjrz.fm.utils.Credentials;
import net.mjrz.fm.utils.MiscUtils;

public class RemovePortfolioEntryAction {

    private static final String URL_QUOTE_DATA = "http://finance.yahoo.com/d/quotes.csv?s=@&f=nl1";

    public RemovePortfolioEntryAction() {
    }

    public ActionResponse executeAction(ActionRequest request) throws Exception {
        ActionResponse resp = new ActionResponse();
        try {
            FManEntityManager em = new FManEntityManager();
            PortfolioEntry entry = (PortfolioEntry) request.getProperty("ENTRY");
            BigDecimal toSell = (BigDecimal) request.getProperty("NUMTOSELL");
            BigDecimal numOld = entry.getNumShares();
            BigDecimal numAfterSale = numOld.subtract(toSell);
            if (toSell.doubleValue() <= 0) {
                resp.setErrorCode(ActionResponse.GENERAL_ERROR);
                resp.setErrorMessage("Cannot sell " + toSell + " stocks");
            } else if (numAfterSale.doubleValue() < 0) {
                resp.setErrorCode(ActionResponse.GENERAL_ERROR);
                resp.setErrorMessage("To sell value exceeds number of shares available");
            } else {
                entry.setNumShares(numAfterSale);
                populatePortfolioEntry(resp, entry);
                em.addPortfolioEntry(entry);
                resp.addResult("ENTRY", entry);
            }
        } catch (Exception e) {
            Logger.getLogger(RemovePortfolioEntryAction.class.getName()).error(MiscUtils.stackTrace2String(e));
            resp.setErrorCode(ActionResponse.GENERAL_ERROR);
            resp.setErrorMessage(e.getMessage());
        }
        return resp;
    }

    private void populatePortfolioEntry(ActionResponse resp, PortfolioEntry e) throws Exception {
        String tmp = URL_QUOTE_DATA.replace("@", e.getSymbol());
        URL url = new URL(tmp);
        BufferedReader in = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
            if (in != null) {
                in.close();
            }
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
