package gaishocron.notifier.istudy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

public class IStudyBlogCrawler {

    /**
     * �}�C�i�X�̒l���w�肷�邱��
     */
    private static final int DAYS_BEFORE = -2;

    private static final String LS = System.getProperty("line.separator");

    public List<Blog> searchRecentUpdatedBlogs() throws URISyntaxException, IOException {
        Date date = DateUtils.addDays(new Date(), DAYS_BEFORE);
        String dateStr = DateFormatUtils.format(date, "yyyy/MM/dd");
        String urlString = "http://cloud.istudy.ne.jp/ies/blogView.do?";
        urlString = urlString + "TYPE=1610&SUB_TYPE=1611&OPERATION=0&USER_OPERATION=2&PAGE_COUNT=0";
        urlString = urlString + "&START_DATE=" + dateStr;
        URL url = new URL(urlString);
        InputStream in = url.openStream();
        String textHtml = getTextFromInputStream(in);
        Pattern pattern = Pattern.compile("<a href=\"javascript:next\\(0,'(\\d+)', '(\\w+)'\\)\">(.*)</a>");
        Matcher matcher = pattern.matcher(textHtml);
        List<Blog> list = new ArrayList<Blog>();
        while (matcher.find()) {
            Blog blog = new Blog();
            blog.setBlogId(matcher.group(1));
            blog.setCustCd(matcher.group(2));
            blog.setBlogName(matcher.group(3));
            list.add(blog);
        }
        return list;
    }

    private String getTextFromInputStream(InputStream in) throws UnsupportedEncodingException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append(LS);
        }
        String textHtml = sb.toString();
        return textHtml;
    }
}
