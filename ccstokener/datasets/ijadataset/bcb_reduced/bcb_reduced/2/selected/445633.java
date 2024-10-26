package org.bitbrushers.jobextractor.apinfo;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bitbrushers.jobextractor.apinfo.pattern.JobPageLinkHtmlPatterns;

public class JobIndexPage {

    private String content;

    private List<JobPageLinkHtml> jobPageLinkHtmlList;

    public class JobPageLinkHtml {

        private String linkHtml;

        public JobPageLinkHtml(String linkHtml) {
            this.linkHtml = linkHtml;
        }

        /**
		 * @return the linkHtml
		 */
        public String getLinkHtml() {
            return linkHtml;
        }

        /**
		 * @param linkHtml the linkHtml to set
		 */
        public void setLinkHtml(String linkHtml) {
            this.linkHtml = linkHtml;
        }

        public String extractURL() {
            return extractData(JobPageLinkHtmlPatterns.URL);
        }

        public String extractDate() {
            DateFormat formatter1 = new SimpleDateFormat("yyyyMMdd");
            String dateStr = "200" + extractData(JobPageLinkHtmlPatterns.DATE);
            Date date = null;
            try {
                date = formatter1.parse(dateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            DateFormat formatter2 = new SimpleDateFormat("dd/MM/yy");
            return formatter2.format(date);
        }

        public String extractText() {
            return extractData(JobPageLinkHtmlPatterns.TEXT);
        }

        public String extractData(String pattern) {
            String result = null;
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(linkHtml);
            if (m.find()) {
                result = linkHtml.substring(m.start(), m.end());
            }
            return result;
        }
    }

    public JobIndexPage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();
            urlConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 4.0)");
            urlConn.setRequestProperty("Accept-Language", "en-us");
            this.content = (String) urlConn.getContent();
        } catch (IOException e) {
            System.out.println("ALERT: Cannot access \"" + urlStr + "\". Maybe the URL is not valid.");
            System.exit(1);
        }
        this.jobPageLinkHtmlList = extractJobPageLinkHtmlList();
    }

    public List<JobPageLinkHtml> extractJobPageLinkHtmlList() {
        String linkHtml = null;
        List<JobPageLinkHtml> jobPageLinkHtmlList = new ArrayList<JobPageLinkHtml>();
        Pattern p = Pattern.compile(JobPageLinkHtmlPatterns.A_HREF);
        Matcher m = p.matcher(this.content);
        while (m.find()) {
            linkHtml = this.content.substring(m.start(), m.end());
            jobPageLinkHtmlList.add(new JobPageLinkHtml(linkHtml));
        }
        return jobPageLinkHtmlList;
    }

    public List<JobPageLink> extractJobPageLinks() {
        List<JobPageLink> result = new ArrayList<JobPageLink>();
        for (JobPageLinkHtml jobPageLinkHtml : this.jobPageLinkHtmlList) {
            JobPageLink pageLink = new JobPageLink();
            pageLink.setDate(jobPageLinkHtml.extractDate());
            pageLink.setText(jobPageLinkHtml.extractText());
            pageLink.setUrl(jobPageLinkHtml.extractURL());
            result.add(pageLink);
        }
        return result;
    }
}
