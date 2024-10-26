package net.mjrz.fm.utils.schedule;

import net.mjrz.fm.entity.beans.*;
import net.mjrz.fm.entity.*;
import net.mjrz.fm.ui.panels.PortfolioPanel;
import net.mjrz.fm.utils.MiscUtils;
import net.mjrz.fm.actions.ActionRequest;
import net.mjrz.fm.actions.ActionResponse;
import net.mjrz.fm.actions.AddTransactionAction;
import net.mjrz.fm.actions.GetNetWorthAction;
import net.mjrz.fm.constants.*;
import net.mjrz.scheduler.task.BasicTask;
import java.math.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.io.*;
import org.apache.log4j.Logger;

/**
 * @author Mjrz contact@mjrz.net
 *
 */
public class PortfolioUpdater extends BasicTask {

    private long initTs;

    private long lastRunTime;

    private long nextRunTime;

    private boolean stopped;

    private User user;

    private static Timer timer = null;

    private long DELAY = 3 * 60 * 1000;

    private final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");

    private static final String URL_QUOTE_DATA = "http://finance.yahoo.com/d/quotes.csv?s=@&f=sl1c1";

    private static Logger logger = Logger.getLogger(PortfolioUpdater.class.getName());

    private FManEntityManager em = new FManEntityManager();

    private PortfolioPanel parent = null;

    public PortfolioUpdater(String name) {
        super(name);
    }

    public PortfolioUpdater(String name, PortfolioPanel panel, User user) throws Exception {
        super(name);
        parent = panel;
        this.user = user;
    }

    public void executeTask() {
        try {
            parent.updateMsg("<html><font color=\"red\">Updating portfolio</font></html>", true);
            update();
            parent.updateMsg("Last update: " + sdf.format(new Date()), false);
        } catch (Exception e) {
            try {
                throw e;
            } catch (Exception ex) {
                logger.error(MiscUtils.stackTrace2String(e));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void update() throws Exception {
        try {
            List portfolioList = em.getPortfolio(user.getUid());
            int sz = portfolioList.size();
            for (int i = 0; i < sz; i++) {
                Portfolio p = (Portfolio) portfolioList.get(i);
                List entries = em.getPortfolioEntries(p.getId());
                List<PortfolioEntry> deleteList = new ArrayList<PortfolioEntry>();
                int isz = entries.size();
                StringBuffer symbol = new StringBuffer();
                for (int j = 0; j < isz; j++) {
                    PortfolioEntry entry = (PortfolioEntry) entries.get(j);
                    if (entry.getNumShares().intValue() == 0) {
                        deleteList.add(entry);
                        continue;
                    }
                    symbol.append(entry.getSymbol());
                    if (j < isz - 1) symbol.append(",");
                }
                String symbolStr = symbol.toString();
                if (symbolStr.length() == 0) continue;
                String query = URL_QUOTE_DATA.replace("@", symbolStr);
                updatePortfolio(entries, query);
                deletePortfolioEntries(deleteList);
            }
        } catch (Exception e) {
            logger.error(MiscUtils.stackTrace2String(e));
            throw e;
        }
    }

    private void deletePortfolioEntries(List<PortfolioEntry> deleteList) throws Exception {
        if (deleteList == null || deleteList.size() == 0) {
            return;
        }
        for (PortfolioEntry e : deleteList) {
            logger.info("Deleting portfolio entry: " + e.getSymbol());
            int r = em.deletePortfolioEntry(e);
            parent.removePortfolioEntry(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void updatePortfolio(List entries, String query) {
        BufferedReader in = null;
        try {
            URL url = new URL(query);
            System.out.println(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int status = conn.getResponseCode();
            if (status == 200) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                int count = 0;
                while (true) {
                    String line = in.readLine();
                    if (line == null || count >= entries.size()) break;
                    updateEntry((PortfolioEntry) entries.get(count), line);
                    count++;
                }
            }
        } catch (Exception e) {
            logger.error(MiscUtils.stackTrace2String(e));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private void updateEntry(PortfolioEntry entry, String values) {
        ArrayList<String> split = MiscUtils.splitString(values, ",");
        if (split == null || split.size() != 3) return;
        String symbol = split.get(0);
        String price = split.get(1);
        String change = split.get(2);
        try {
            BigDecimal pricebd = new BigDecimal(price);
            symbol = MiscUtils.trimChars(symbol, '"');
            change = MiscUtils.trimChars(change, '"');
            BigDecimal changeBD = toNumeric(change);
            if (entry.getSymbol().equalsIgnoreCase(symbol)) {
                entry.setPricePerShare(pricebd);
                entry.setChange(change);
                entry.setLastUpdateTS(new Date());
                if (changeBD != null) {
                    entry.setDaysGain(changeBD.multiply(entry.getNumShares()));
                }
                em.addPortfolioEntry(entry);
                parent.updatePortfolio(entry, false);
            }
        } catch (Exception e) {
            logger.error(MiscUtils.stackTrace2String(e));
        }
    }

    private BigDecimal toNumeric(String s) {
        try {
            BigDecimal bd = new BigDecimal(s);
            return bd;
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }
}
