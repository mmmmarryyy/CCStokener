package com.taobao.top.analysis.jobmanager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.taobao.top.analysis.AnalysisConstants;
import com.taobao.top.analysis.TopAnalysisConfig;
import com.taobao.top.analysis.data.Report;
import com.taobao.top.analysis.data.ReportAlert;
import com.taobao.top.analysis.data.ReportEntry;
import com.taobao.top.analysis.util.MailUtil;
import com.taobao.top.analysis.util.ReportUtil;

/**
 * 默认的报表管理实现
 * 
 * @author fangweng
 * 
 */
public class DefaultReportManager implements IReportManager {

    private final Log logger = LogFactory.getLog(DefaultReportManager.class);

    /**
	 * 全局配置
	 */
    protected TopAnalysisConfig topAnalyzerConfig;

    public void setTopAnalyzerConfig(TopAnalysisConfig topAnalyzerConfig) {
        this.topAnalyzerConfig = topAnalyzerConfig;
    }

    @Override
    public void dispatchAlerts(List<String> alerts) {
        BufferedWriter fw = null;
        String filename = null;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String date = new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
        filename = new StringBuilder("alertReport_").append(date).append(".html").toString();
        StringBuilder alertPage = new StringBuilder();
        try {
            for (String alert : alerts) {
                BufferedReader bReader = new BufferedReader(new StringReader(alert));
                String line, line2;
                line = bReader.readLine();
                line2 = bReader.readLine();
                if (line == null || line2 == null) continue;
                alertPage.append("<table border=\"1\">");
                String[] content = line.split(",");
                alertPage.append("<tr>");
                for (String c : content) {
                    alertPage.append("<th>").append(c).append("</th>");
                }
                alertPage.append("</tr>");
                content = line2.split(",");
                alertPage.append("<tr>");
                for (String c : content) {
                    alertPage.append("<td>").append(c).append("</td>");
                }
                alertPage.append("</tr>");
                while ((line = bReader.readLine()) != null) {
                    content = line.split(",");
                    alertPage.append("<tr>");
                    for (String c : content) {
                        alertPage.append("<td>").append(c).append("</td>");
                    }
                    alertPage.append("</tr>");
                }
                alertPage.append("</table>").append("<br><br><br>");
            }
            new File(filename).createNewFile();
            fw = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(new File(filename)), "utf-8"));
            fw.write(alertPage.toString());
        } catch (Exception ex) {
            logger.error(ex, ex);
        } finally {
            try {
                if (fw != null) fw.close();
            } catch (Exception e) {
                logger.error(e, e);
            }
        }
        try {
            MailUtil sm = new MailUtil(topAnalyzerConfig.getSmtpServer());
            sm.setNamePass(topAnalyzerConfig.getMailUserName(), topAnalyzerConfig.getMailPassWord());
            sm.setSubject("此邮件是系统自动发出的TOP报表告警信息(" + date + ")");
            sm.setFrom("fangweng@taobao.com");
            sm.setTo(topAnalyzerConfig.getMailto());
            sm.setBody(alertPage.toString());
            sm.setNeedAuth(true);
            sm.send();
        } catch (Exception e) {
            logger.error(e, e);
        }
    }

    @Override
    public void dispatchReports(List<String> reports, String info) {
        if (reports != null && reports.size() > 0 && topAnalyzerConfig.getMailto() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            String date = new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
            MailUtil sm = new MailUtil(topAnalyzerConfig.getSmtpServer());
            sm.setNamePass(topAnalyzerConfig.getMailUserName(), topAnalyzerConfig.getMailPassWord());
            sm.setSubject("TOP日报表(" + date + ")");
            sm.setFrom("fangweng@taobao.com");
            sm.setTo(topAnalyzerConfig.getMailto());
            if (info != null) sm.setBody("此邮件是系统自动发出，附件中是TOP日报表原始数据" + "\r\n" + info); else sm.setBody("此邮件是系统自动发出，附件中是TOP日报表原始数据");
            sm.setNeedAuth(true);
            for (String report : reports) {
                sm.addFileAffix(report);
            }
            boolean b = sm.send();
            if (b) {
                logger.warn("日报表：" + date + "已发送成功！");
            } else {
                logger.warn("日报表：" + date + "发送失败！");
            }
        }
    }

    @Override
    public List<String> generateAlerts(Map<String, Report> reportPool, List<ReportAlert> alerts, String dir) {
        long start = System.currentTimeMillis();
        List<String> result = ReportUtil.generateAlerts(reportPool, alerts, dir);
        if (logger.isWarnEnabled()) logger.warn(new StringBuilder("generate alert end").append(", time consume: ").append((System.currentTimeMillis() - start) / 1000).toString());
        return result;
    }

    @Override
    public List<String> generateReports(Map<String, Map<String, Object>> resultPool, Map<String, Report> reportPool, String dir, boolean needTimeSuffix) {
        long start = System.currentTimeMillis();
        List<String> reports = new ArrayList<String>();
        Map<String, String> topChartExport = new TreeMap<String, String>();
        StringBuilder topHtmlExport = new StringBuilder();
        StringBuilder periodDir = new StringBuilder();
        if (resultPool == null || resultPool.size() <= 0 || reportPool == null || reportPool.size() <= 0) return reports;
        Calendar calendar = Calendar.getInstance();
        String currentTime = new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String statTime = new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-").append(calendar.get(Calendar.MONTH) + 1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
        if (dir != null && !"".equals(dir)) {
            if (!dir.endsWith(File.separator)) {
                periodDir.append(dir).append(File.separator).append("period").append(File.separator).append(currentTime).append(File.separator);
                dir = new StringBuilder(dir).append(File.separator).append(currentTime).append(File.separator).toString();
            } else {
                periodDir.append(dir).append("period").append(File.separator).append(currentTime).append(File.separator);
                dir = new StringBuilder(dir).append(currentTime).append(File.separator).toString();
            }
            File targetDir = new java.io.File(dir);
            File period = new java.io.File(periodDir.toString());
            if (!period.exists() || (period.exists() && !period.isDirectory())) {
                period.mkdirs();
            }
            if (!targetDir.exists() || (targetDir.exists() && !targetDir.isDirectory())) {
                targetDir.mkdirs();
            } else {
                if (targetDir.exists() && targetDir.isDirectory()) {
                    File[] deleteFiles = targetDir.listFiles();
                    for (File f : deleteFiles) f.delete();
                }
            }
        }
        Iterator<Report> iter = reportPool.values().iterator();
        Map<String, Object[]> result;
        while (iter.hasNext()) {
            Report report = iter.next();
            String reportFile;
            String reportDir = dir;
            if (report.isPeriod()) {
                if (reportDir == null || "".equals(reportDir)) reportDir = report.getFile(); else reportDir = new StringBuilder().append(periodDir).append(report.getFile()).append(File.separator).toString();
                File tmpDir = new java.io.File(reportDir);
                if (!tmpDir.exists() || (tmpDir.exists() && !tmpDir.isDirectory())) {
                    tmpDir.mkdirs();
                }
            }
            if (reportDir != null && !"".equals(reportDir)) if (needTimeSuffix) reportFile = new StringBuilder().append(reportDir).append(report.getFile()).append("_").append(statTime).append(".csv").toString(); else reportFile = new StringBuilder().append(reportDir).append(report.getFile()).append(".csv").toString(); else if (needTimeSuffix) reportFile = new StringBuilder(report.getFile()).append("_").append(statTime).append(".csv").toString(); else reportFile = new StringBuilder(report.getFile()).append(".csv").toString();
            if (report.isPeriod()) {
                reportFile = new StringBuilder().append(reportFile.substring(0, reportFile.indexOf(".csv"))).append("_").append(System.currentTimeMillis()).append(".csv").toString();
            }
            BufferedWriter bout = null;
            try {
                new File(reportFile).createNewFile();
                bout = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(new File(reportFile)), "gb2312"));
                result = new TreeMap<String, Object[]>();
                StringBuilder title = new StringBuilder();
                int index = 0;
                int r_count = report.getReportEntrys().size();
                for (ReportEntry entry : report.getReportEntrys()) {
                    title.append(entry.getName()).append(",");
                    Map<String, Object> m = resultPool.get(entry.getId());
                    if (m != null && m.size() > 0) {
                        Iterator<String> mIter = m.keySet().iterator();
                        while (mIter.hasNext()) {
                            String k = mIter.next();
                            if (k.startsWith("a:sum") || k.startsWith("a:count")) {
                                continue;
                            }
                            Object[] values = result.get(k);
                            if (values == null) values = new Object[r_count];
                            Object value = m.get(k);
                            if (value != null && entry.getFormatStack() != null && entry.getFormatStack().size() > 0) {
                                value = ReportUtil.formatValue(entry.getFormatStack(), value);
                            }
                            values[index] = value;
                            result.put(k, values);
                        }
                    }
                    index += 1;
                    if (report.isPeriod()) {
                        Map<String, Object> _deleted = resultPool.remove(entry.getId());
                        if (_deleted != null) _deleted.clear();
                    }
                }
                if (title.length() > 0) {
                    title.deleteCharAt(title.length() - 1);
                    title.append("\r\n");
                }
                bout.write(title.toString());
                String[] orders = null;
                if (report.getOrderby() != null) orders = report.getOrderby().split(",");
                ArrayList<Object[]> rlist = new ArrayList<Object[]>();
                rlist.addAll(result.values());
                ReportUtil.doOrder(rlist, orders, report);
                int rowCount = report.getRowCount();
                if (rowCount == 0) rowCount = rlist.size();
                for (Object[] obj : rlist) {
                    rowCount -= 1;
                    if (rowCount < 0) break;
                    StringBuilder tb = new StringBuilder();
                    for (int t = 0; t < r_count; t++) {
                        if (obj[t] != null) tb.append(obj[t]).append(","); else tb.append("0,");
                    }
                    tb.deleteCharAt(tb.length() - 1);
                    bout.write(tb.append("\r\n").toString());
                }
                if (result != null && result.size() > 0) {
                    result.clear();
                    result = null;
                }
                reports.add(reportFile);
            } catch (Exception ex) {
                logger.error(ex, ex);
            } finally {
                if (bout != null) try {
                    bout.close();
                } catch (IOException e) {
                    logger.error(e, e);
                }
            }
            if ("chart".equalsIgnoreCase(report.getExport())) {
                File rFile = new File(reportFile);
                topChartExport.put(report.getChartTitle(), new StringBuilder("<br><img src='").append(createReportChart(rFile.getAbsolutePath(), report.getChartTitle(), report.getChartType(), report.getExportCount())).append("' alt='Top Analyzer Chart'/></br>").toString());
            }
            if ("html".equalsIgnoreCase(report.getExport())) {
                File rFile = new File(reportFile);
                topHtmlExport.append("<br/>").append(ReportUtil.createReportHtml(rFile.getAbsolutePath(), report.getFile(), report.getExportCount())).append("<br/>");
            }
        }
        createTimeStampFile(dir);
        if (topChartExport.size() > 0 || topHtmlExport.length() > 0) {
            String chartPath = topAnalyzerConfig.getChartFilePath();
            if (!chartPath.endsWith(File.separator)) chartPath += File.separator;
            File cPath = new File(chartPath);
            if (!cPath.exists() || (cPath.exists() && !cPath.isDirectory())) cPath.mkdirs();
            BufferedWriter bwr = null;
            try {
                new File(chartPath + AnalysisConstants.CHART_FILENAME).createNewFile();
                bwr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(chartPath + AnalysisConstants.CHART_FILENAME)), "utf-8"));
                bwr.write("<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><script>setTimeout(\"location.href=location.href\",120000);</script>");
                if (topChartExport.size() > 0) {
                    Iterator<String> keys = topChartExport.keySet().iterator();
                    while (keys.hasNext()) {
                        bwr.write(topChartExport.get(keys.next()));
                    }
                }
                if (topHtmlExport.length() > 0) bwr.write(topHtmlExport.toString());
                bwr.write("</html>");
                bwr.flush();
            } catch (Exception ex) {
                logger.error(ex);
            } finally {
                if (bwr != null) {
                    try {
                        bwr.close();
                    } catch (IOException e) {
                        logger.error(e, e);
                    }
                }
            }
        }
        if (logger.isInfoEnabled()) logger.info(new StringBuilder("generate report end").append(", time consume: ").append((System.currentTimeMillis() - start) / 1000).toString());
        return reports;
    }

    protected void createTimeStampFile(String dir) {
        String timeStampFile = new StringBuilder().append(dir).append(AnalysisConstants.TIMESTAMP_FILE).toString();
        BufferedWriter bwr = null;
        try {
            new File(timeStampFile).createNewFile();
            bwr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(timeStampFile))));
            bwr.write(String.valueOf(System.currentTimeMillis()));
        } catch (Exception ex) {
            logger.error("createTimeStampFile error!", ex);
        } finally {
            if (bwr != null) {
                try {
                    bwr.close();
                } catch (IOException e) {
                    logger.error(e, e);
                }
            }
        }
    }

    @Override
    public void destory() {
    }

    /**
	 * 利用Google Chart API生成对应的曲线图
	 * 
	 * @param inputFile
	 * @param title
	 * @param chartType
	 *            输出访问的png图片地址
	 */
    @SuppressWarnings("unchecked")
    public String createReportChart(String inputFile, String title, String chartType, int countNum) {
        java.io.BufferedReader br = null;
        java.io.OutputStream bout = null;
        StringBuilder result = new StringBuilder();
        String pngFilePath = topAnalyzerConfig.getChartFilePath();
        if (title == null || (title != null && "".equals(title))) title = inputFile;
        String[] colors = new String[] { "00ff00", "ff0000", "01ff4f", "ffffa2", "00ffff", "afff0f", "f000ff", "fa0b0f", "0000ff", "f0f0f0", "0f0f0f" };
        try {
            File file = new File(inputFile);
            if (!file.exists()) throw new java.lang.RuntimeException("chart file not exist : " + inputFile);
            if (pngFilePath.endsWith(File.separator)) {
                pngFilePath = pngFilePath + file.getName().substring(0, file.getName().indexOf("_")) + ".png";
            } else pngFilePath = pngFilePath + File.separator + file.getName().substring(0, file.getName().indexOf("_")) + ".png";
            new File(pngFilePath).createNewFile();
            bout = new FileOutputStream(new File(pngFilePath));
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "gb2312"));
            String line = null;
            int num = 0;
            if (chartType != null && chartType.startsWith("bar")) {
                String type;
                if (chartType.equals("bar")) type = "bvg&chbh=a"; else type = chartType.substring(4);
                result.append("http://chart.apis.google.com/chart?chs=700x400&cht=").append(type).append("&chtt=").append(URLEncoder.encode(title, "utf-8")).append("&chxt=x,y&chg=0,10");
            } else result.append("http://chart.apis.google.com/chart?chs=700x400&cht=lxy&chtt=").append(URLEncoder.encode(title, "utf-8")).append("&chxt=x,y&chg=0,10");
            StringBuilder chartDataLabel = new StringBuilder().append("chdl=");
            StringBuilder chartColor = new StringBuilder().append("chco=");
            StringBuilder chartXLabel = new StringBuilder().append("chxl=0:");
            StringBuilder chartXPosition = new StringBuilder().append("chxp=0,");
            StringBuilder chartXRange = new StringBuilder().append("chxr=0,0,");
            StringBuilder chartYRange = new StringBuilder().append("|1,0,");
            StringBuilder chartData = new StringBuilder().append("chd=t:");
            DecimalFormat dformater = new DecimalFormat("0.00");
            double yMax = 0;
            List<String>[] rList = null;
            int index = 0;
            while ((line = br.readLine()) != null) {
                String[] contents = line.split(",");
                if (num == 0) {
                    rList = new List[contents.length];
                    for (int i = 0; i < contents.length; i++) {
                        rList[i] = new ArrayList<String>();
                        if (i != contents.length - 1) {
                            if (i > 0) {
                                chartDataLabel.append(URLEncoder.encode(contents[i], "utf-8")).append("|");
                                chartColor.append(colors[i % colors.length]).append(",");
                            }
                        } else {
                            chartDataLabel.append(URLEncoder.encode(contents[i], "utf-8"));
                            chartColor.append(colors[i % colors.length]);
                        }
                    }
                } else {
                    if (countNum > 0 && index >= countNum) break;
                    index += 1;
                    if (contents[0].indexOf(" ") > 0) {
                        String[] cs = contents[0].split(" ");
                        chartXLabel.append("|").append(cs[cs.length - 1]);
                    } else chartXLabel.append("|").append(contents[0]);
                    for (int i = 0; i < contents.length; i++) {
                        if (i == 0) {
                            rList[i].add(String.valueOf(num));
                        } else {
                            double y = Double.valueOf(contents[i]);
                            if (yMax < y) yMax = y;
                            rList[i].add(contents[i]);
                        }
                    }
                }
                num += 1;
            }
            if (rList != null && rList.length > 1) {
                if (yMax <= 100) chartYRange.append(100); else chartYRange.append(yMax);
                StringBuilder x = new StringBuilder();
                for (int j = 0; j < rList[0].size(); j++) {
                    x.append(dformater.format(Double.valueOf(rList[0].get(j)) * ((double) 100 / (double) num)));
                    chartXPosition.append(rList[0].get(j));
                    if (j < rList[0].size() - 1) {
                        x.append(",");
                        chartXPosition.append(",");
                    }
                }
                chartXRange.append(num);
                for (int i = 1; i < rList.length; i++) {
                    chartData.append(x).append("|");
                    for (int j = 0; j < rList[i].size(); j++) {
                        if (yMax <= 100) {
                            if (j < rList[i].size() - 1) {
                                chartData.append(dformater.format(Double.valueOf(rList[i].get(j)))).append(",");
                            } else {
                                chartData.append(dformater.format(Double.valueOf(rList[i].get(j)))).append("|");
                            }
                        } else {
                            double y = Double.valueOf(rList[i].get(j));
                            if (j < rList[i].size() - 1) {
                                chartData.append(dformater.format(y / yMax * 100)).append(",");
                            } else {
                                chartData.append(dformater.format(y / yMax * 100)).append("|");
                            }
                        }
                    }
                }
                if (chartData.toString().endsWith("|")) chartData.deleteCharAt(chartData.toString().length() - 1);
                result.append("&").append(chartDataLabel).append("&").append(chartColor).append("&").append(chartXLabel).append("&").append(chartXPosition).append("&").append(chartXRange).append(chartYRange).append("&").append(chartData);
                logger.info(result.toString());
                URL url = new URL(result.toString());
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream cin = connection.getInputStream();
                byte[] buf = new byte[1000];
                int count = 0;
                while ((count = cin.read(buf)) > 0) {
                    bout.write(buf, 0, count);
                }
            } else {
                logger.error("no result export...");
                return "";
            }
        } catch (Exception ex) {
            logger.error(ex, ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.error(e, e);
                }
            }
            if (bout != null) {
                try {
                    bout.close();
                } catch (IOException e) {
                    logger.error(e, e);
                }
            }
        }
        if (pngFilePath.indexOf(File.separator) >= 0) {
            try {
                pngFilePath = URLEncoder.encode(pngFilePath.substring((pngFilePath.lastIndexOf(File.separator)) + 1, pngFilePath.length()), "utf-8");
            } catch (Exception ex) {
                logger.error(ex, ex);
            }
        }
        return pngFilePath;
    }

    @Override
    public void init() {
    }
}
