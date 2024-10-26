package net.jomper.fetchcenter.wc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import net.jomper.fetchcenter.model.GoldenBoot;
import net.jomper.fetchcenter.model.TargetPage;

public class GoldenBootFetcher {

    public void handler(List<GoldenBoot> gbs, TargetPage target) {
        try {
            URL url = new URL(target.getUrl());
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            String include = "Top Scorers";
            while ((line = reader.readLine()) != null) {
                if (line.indexOf(include) != -1) {
                    buildGildenBoot(line, gbs);
                    break;
                }
            }
            reader.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
    }

    private void buildGildenBoot(String line, List<GoldenBoot> gbs) {
        String rankFrom = "r info\">";
        String rankTo = "</td>";
        String nationFrom = "/imgml/flags/s/";
        String nationTo = "\" alt=";
        String nameFrom = "/index.html\">";
        String nameTo = "</a>";
        String topNameFrom = "plth im\" width=\"38\" alt=\"";
        String topNameEnd = "\" height=\"51";
        String dataFrom = "c info\">";
        String dataTo = "</td>";
        GoldenBoot gb = new GoldenBoot();
        if (line.indexOf(rankFrom) != -1) {
            line = line.substring(line.indexOf(rankFrom));
            gb.setRank(line.substring(line.indexOf(rankFrom) + rankFrom.length(), line.indexOf(rankTo)));
        } else {
            return;
        }
        if (gbs.size() < 3) {
            if (line.indexOf(topNameFrom) != -1) {
                line = line.substring(line.indexOf(topNameFrom));
                gb.setPlayer(line.substring(line.indexOf(topNameFrom) + topNameFrom.length(), line.indexOf(topNameEnd)));
            }
        }
        if (line.indexOf(nationFrom) != -1) {
            line = line.substring(line.indexOf(nationFrom));
            gb.setNation(line.substring(line.indexOf(nationFrom) + nationFrom.length(), line.indexOf(nationTo)));
            if ("nga.gif".equals(gb.getNation())) {
                gb.setNation("ngr.gif");
            }
            if ("svn.gif".equals(gb.getNation())) {
                gb.setNation("slo.gif");
            }
            if ("srb.gif".equals(gb.getNation())) {
                gb.setNation("scg.gif");
            }
        }
        if (gbs.size() >= 3) {
            if (line.indexOf(nameFrom) != -1) {
                line = line.substring(line.indexOf(nameFrom));
                gb.setPlayer(line.substring(line.indexOf(nameFrom) + nameFrom.length(), line.indexOf(nameTo)));
            }
        }
        if (line.indexOf(dataFrom) != -1) {
            line = line.substring(line.indexOf(dataTo) + dataTo.length());
            gb.setGoals(line.substring(line.indexOf(dataFrom) + dataFrom.length(), line.indexOf(dataTo)));
        }
        if (line.indexOf(dataFrom) != -1) {
            line = line.substring(line.indexOf(dataTo) + dataTo.length());
            gb.setAssist(line.substring(line.indexOf(dataFrom) + dataFrom.length(), line.indexOf(dataTo)));
        }
        if (line.indexOf(dataFrom) != -1) {
            line = line.substring(line.indexOf(dataTo) + dataTo.length());
            gb.setMin(line.substring(line.indexOf(dataFrom) + dataFrom.length(), line.indexOf(dataTo)));
        }
        if (line.indexOf(dataFrom) != -1) {
            line = line.substring(line.indexOf(dataTo) + dataTo.length());
            gb.setPen(line.substring(line.indexOf(dataFrom) + dataFrom.length(), line.indexOf(dataTo)));
        }
        if (line.indexOf(dataFrom) != -1) {
            line = line.substring(line.indexOf(dataTo) + dataTo.length());
            gb.setMatches(line.substring(line.indexOf(dataFrom) + dataFrom.length(), line.indexOf(dataTo)));
        }
        gbs.add(gb);
        buildGildenBoot(line, gbs);
    }
}
