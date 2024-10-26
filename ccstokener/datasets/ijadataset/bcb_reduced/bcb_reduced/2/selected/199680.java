package net.mjrz.fm.actions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import net.mjrz.fm.entity.beans.CurrencyMonitor;
import net.mjrz.fm.entity.CurrencyEntityManager;

public class AddCurrencyMonitorAction {

    public ActionResponse executeAction(ActionRequest request) throws Exception {
        ActionResponse resp = new ActionResponse();
        try {
            Thread.sleep(5000);
            CurrencyMonitor cm = (CurrencyMonitor) request.getProperty("CURRENCYMONITOR");
            String value = getForex(cm.getCode());
            cm.setLastUpdateValue(new BigDecimal(value));
            cm.setLastUpdateTs(new Date());
            CurrencyEntityManager em = new CurrencyEntityManager();
            boolean exists = em.currencyMonitorExists(cm);
            if (!exists) {
                em.addCurrencyMonitor(cm);
                resp.setErrorCode(ActionResponse.NOERROR);
                resp.addResult("CURRENCYMONITOR", cm);
            } else {
                resp.setErrorCode(ActionResponse.GENERAL_ERROR);
                resp.setErrorMessage("Country already added");
            }
        } catch (Exception e) {
            resp.setErrorCode(ActionResponse.GENERAL_ERROR);
            resp.setErrorMessage("Exception occured. " + e.getMessage());
        }
        return resp;
    }

    private String getForex(String code) throws Exception {
        BufferedReader in = null;
        try {
            String tmp = RetrieveForexAction.URL.replace("@", code);
            URL url = new URL(tmp);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int status = conn.getResponseCode();
            if (status == 200) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder value = new StringBuilder();
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    value.append(line);
                }
                return value.toString();
            } else {
                return "0";
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
