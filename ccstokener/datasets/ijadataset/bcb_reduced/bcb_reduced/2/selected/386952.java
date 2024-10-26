package org.bitbrushers.jobextractor.apinfo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import org.bitbrushers.jobextractor.apinfo.pattern.JobOfferHtmlPatterns;
import org.bitbrushers.jobextractor.to.JobOffer;

public class JobOfferPage {

    private JobPageLink link;

    private String content;

    private List<JobOfferHtml> jobOfferHtmlList;

    public class JobOfferHtml {

        private String jobOfferHtml;

        public JobOfferHtml(String jobOfferHtml) {
            this.jobOfferHtml = jobOfferHtml;
        }

        public String getJobOfferHtml() {
            return jobOfferHtml;
        }

        public void setJobOfferHtml(String jobOfferHtml) {
            this.jobOfferHtml = jobOfferHtml;
        }

        public String extractCity() {
            return extractData(JobOfferHtmlPatterns.CITY);
        }

        public String extractState() {
            return extractData(JobOfferHtmlPatterns.STATE);
        }

        public String extractTitle() {
            return extractData(JobOfferHtmlPatterns.TITLE);
        }

        public String extractDate() {
            return extractData(JobOfferHtmlPatterns.DATE);
        }

        public String extractDescription() {
            String result = extractData(JobOfferHtmlPatterns.DESCRIPTION, true);
            result = result.replace("\n", "");
            return result.replaceAll("\r", "<br>");
        }

        public String extractCompany() {
            return extractData(JobOfferHtmlPatterns.COMPANY);
        }

        public String extractEmail() {
            String result = extractData(JobOfferHtmlPatterns.EMAIL);
            if (result != null) {
                result = result.replaceAll("(&#[0-9]{3})(?=[^;])", "$1;");
                try {
                    result = URLDecoder.decode(StringEscapeUtils.unescapeHtml(result), "ISO-8859-1");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return result;
            }
            return "";
        }

        public String extractCode() {
            return extractData(JobOfferHtmlPatterns.CODE);
        }

        public String extractValues() {
            return extractData(JobOfferHtmlPatterns.VALUES);
        }

        private String extractData(String pattern) {
            return extractData(pattern, false);
        }

        private String extractData(String pattern, boolean multiLine) {
            StringBuffer result = new StringBuffer();
            Pattern p = null;
            if (multiLine) {
                p = Pattern.compile(pattern, Pattern.DOTALL);
            } else {
                p = Pattern.compile(pattern);
            }
            Matcher m = p.matcher(jobOfferHtml);
            while (m.find()) {
                result.append(jobOfferHtml.substring(m.start(), m.end()) + " ");
            }
            return result.toString();
        }
    }

    public JobOfferPage(JobPageLink link) {
        this.link = link;
        try {
            URL url = new URL(link.getUrl());
            URLConnection urlConn = url.openConnection();
            urlConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 4.0)");
            urlConn.setRequestProperty("Accept-Language", "en-us");
            this.content = (String) url.getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.jobOfferHtmlList = extractJobOfferHtmlList();
    }

    /**
	 * @return the content
	 */
    public String getContent() {
        return content;
    }

    /**
	 * @param content the content to set
	 */
    public void setContent(String content) {
        this.content = content;
    }

    /**
	 * @return the link
	 */
    public JobPageLink getLink() {
        return link;
    }

    /**
	 * @param link the link to set
	 */
    public void setLink(JobPageLink link) {
        this.link = link;
    }

    public List<JobOfferHtml> extractJobOfferHtmlList() {
        String jobOfferHtml = null;
        List<JobOfferHtml> jobOfferHtmlList = new ArrayList<JobOfferHtml>();
        Pattern p = Pattern.compile(JobOfferHtmlPatterns.JOB_OFFER_HTML, Pattern.DOTALL);
        Matcher m = p.matcher(this.content);
        while (m.find()) {
            jobOfferHtml = this.content.substring(m.start(), m.end());
            jobOfferHtmlList.add(new JobOfferHtml(jobOfferHtml));
        }
        return jobOfferHtmlList;
    }

    public List<JobOffer> extractJobOffers() {
        List<JobOffer> result = new ArrayList<JobOffer>();
        for (JobOfferHtml jobOfferHtml : this.jobOfferHtmlList) {
            JobOffer jobOffer = new JobOffer();
            jobOffer.setCity(jobOfferHtml.extractCity());
            jobOffer.setState(jobOfferHtml.extractState());
            jobOffer.setDate(jobOfferHtml.extractDate());
            jobOffer.setTitle(jobOfferHtml.extractTitle());
            jobOffer.setDescription(jobOfferHtml.extractDescription());
            jobOffer.setCompany(jobOfferHtml.extractCompany());
            jobOffer.setEmail(jobOfferHtml.extractEmail());
            jobOffer.setCode(jobOfferHtml.extractCode());
            jobOffer.setValues(jobOfferHtml.extractValues());
            result.add(jobOffer);
        }
        return result;
    }
}
