package com.googlecode.technorati4j;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.googlecode.technorati4j.entity.BlogInfoRequest;
import com.googlecode.technorati4j.entity.BlogInfoResponse;
import com.googlecode.technorati4j.entity.BlogPostTagsRequest;
import com.googlecode.technorati4j.entity.BlogPostTagsResponse;
import com.googlecode.technorati4j.entity.CosmosRequest;
import com.googlecode.technorati4j.entity.CosmosResponse;
import com.googlecode.technorati4j.entity.DailyCountsRequest;
import com.googlecode.technorati4j.entity.DailyCountsResponse;
import com.googlecode.technorati4j.entity.GetInfoRequest;
import com.googlecode.technorati4j.entity.GetInfoResponse;
import com.googlecode.technorati4j.entity.SearchRequest;
import com.googlecode.technorati4j.entity.SearchResponse;
import com.googlecode.technorati4j.entity.TagRequest;
import com.googlecode.technorati4j.entity.TagResponse;
import com.googlecode.technorati4j.entity.TopTagsRequest;
import com.googlecode.technorati4j.entity.TopTagsResponse;
import com.googlecode.technorati4j.util.Constants;
import com.googlecode.technorati4j.util.StringUtils;

/**
 * The main class to call Technorati APIs.
 * 
 * @author Otávio Scherer Garcia
 * @version $Revision$
 */
public final class Technorati {

    private static final Logger logger = LoggerFactory.getLogger(Technorati.class);

    /**
     * A URL search allows you to retrieve results for blogs linking to a given base URL. Technorati's cosmos query
     * allows you to specify a wide array of parameters to customize your result set.
     * 
     * @param request The request entity with API parameters.
     * @return The response entity.
     * @throws TechnoratiApiException If an error occurs.
     */
    public static CosmosResponse cosmos(CosmosRequest request) throws TechnoratiApiException {
        Element root = sendRequest(Constants.COSMOS_URL, request.getParameters());
        CosmosResponse reponse = new CosmosResponse(root);
        logger.debug("API cosmos: {}", reponse);
        return reponse;
    }

    /**
     * A keyword search lets you see what blogs contain a given search string. On the Technorati site, you can enter a
     * keyword in the searchbox and it will return a list of blogs containing it. The API version allows more features
     * and gives you a way to use the search function on your own site.
     * 
     * @param request The request entity with API parameters.
     * @return The response entity.
     * @throws TechnoratiApiException If an error occurs.
     */
    public static SearchResponse search(SearchRequest request) throws TechnoratiApiException {
        Element root = sendRequest(Constants.SEARCH_URL, request.getParameters());
        SearchResponse response = new SearchResponse(root);
        logger.debug("API search: {}", response);
        return response;
    }

    /**
     * The tag query allows you to get a list of posts with the given tag associated with it. On the Technorati site,
     * the tag pages show posts with the tag associated as well as content from external folksonomies. The API version
     * allows you to use the tagged posts in your own application.
     * 
     * @param request The request entity with API parameters.
     * @return The response entity.
     * @throws TechnoratiApiException If an error occurs.
     */
    public static TagResponse tag(TagRequest request) throws TechnoratiApiException {
        Element root = sendRequest(Constants.TAG_URL, request.getParameters());
        TagResponse response = new TagResponse(root);
        logger.debug("API tag: {}", response);
        return response;
    }

    /**
     * The dailycounts query provides daily counts of posts containing the queried keyword.
     * 
     * @param request The request entity with API parameters.
     * @return The response entity.
     * @throws TechnoratiApiException If an error occurs.
     */
    public static DailyCountsResponse dailyCount(DailyCountsRequest request) throws TechnoratiApiException {
        Element root = sendRequest(Constants.DAILY_COUNTS_URL, request.getParameters());
        DailyCountsResponse response = new DailyCountsResponse(root);
        logger.debug("API dailycount: {}", response);
        return response;
    }

    /**
     * The keyinfo query provides information on daily usage of an API key. Key Info queries do not count against a
     * key's daily query limit.
     * 
     * @param request The request entity with API parameters.
     * @return The response entity.
     * @throws TechnoratiApiException If an error occurs.
     */
    public static TopTagsResponse topTags(TopTagsRequest request) throws TechnoratiApiException {
        Element root = sendRequest(Constants.TOP_TAGS_URL, request.getParameters());
        TopTagsResponse response = new TopTagsResponse(root);
        logger.debug("API toptags: {}", response);
        return response;
    }

    /**
     * The bloginfo query provides info on what blog, if any, is associated with a given URL.
     * 
     * @param request The request entity with API parameters.
     * @return The response entity.
     * @throws TechnoratiApiException If an error occurs.
     */
    public static BlogInfoResponse blogInfo(BlogInfoRequest request) throws TechnoratiApiException {
        Element root = sendRequest(Constants.BLOG_INFO_URL, request.getParameters());
        BlogInfoResponse response = new BlogInfoResponse(root);
        logger.debug("API bloginfo: {}", response);
        return response;
    }

    /**
     * The keyinfo query provides information on daily usage of an API key. Key Info queries do not count against a
     * key's daily query limit.
     * 
     * @param request The request entity with API parameters.
     * @return The response entity.
     * @throws TechnoratiApiException If an error occurs.
     */
    public static BlogPostTagsResponse blogPostTags(BlogPostTagsRequest request) throws TechnoratiApiException {
        Element root = sendRequest(Constants.BLOG_POST_TAG_URL, request.getParameters());
        BlogPostTagsResponse response = new BlogPostTagsResponse(root);
        logger.debug("API blogposttags: {}", response);
        return response;
    }

    /**
     * The getinfo query tells you things that Technorati knows about a member.
     * 
     * @param request The request entity with API parameters.
     * @return The response entity.
     * @throws TechnoratiApiException If an error occurs.
     */
    public static GetInfoResponse getInfo(GetInfoRequest request) throws TechnoratiApiException {
        Element root = sendRequest(Constants.GET_INFO_URL, request.getParameters());
        GetInfoResponse response = new GetInfoResponse(root);
        logger.debug("API getinfo: {}", response);
        return response;
    }

    /**
     * Send request to technorati restful service. The result is the root element of service response. If an error
     * occurs, the {@link TechnoratiApiException} will be throws with the error message.
     * 
     * @param strUrl The url to call.
     * @param params The parameters that will be append to url.
     * @return The root element of response.
     * @throws TechnoratiApiException If an error occurs when call the api.
     */
    private static Element sendRequest(String strUrl, String params) throws TechnoratiApiException {
        URLConnection conn = null;
        OutputStream out = null;
        InputStream in = null;
        Element root = null;
        try {
            final URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            logger.debug("requesting URL: {}?{}", url, params);
            out = conn.getOutputStream();
            out.write(params.getBytes());
            out.flush();
            in = conn.getInputStream();
            final SAXBuilder builder = new SAXBuilder();
            builder.setEntityResolver(new DTDEntityResolver());
            final Document doc = builder.build(in);
            root = doc.getRootElement().getChild("document");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TechnoratiApiException(e);
        } finally {
            closeQuietly(out);
            closeQuietly(in);
        }
        handleErrors(root);
        return root;
    }

    /**
     * Check if the response contains an error. If the error node is found, this method throws an
     * {@link TechnoratiApiException} with the message. For more information, please see
     * http://technorati.com/developers/api/error.html.
     * 
     * @param root The root node of service response.
     * @throws TechnoratiApiException If the error node is found this exception will be throws.
     */
    private static void handleErrors(Element root) throws TechnoratiApiException {
        Element e = root.getChild("result");
        e = (e == null ? null : e.getChild("error"));
        if (e != null && StringUtils.isNotEmpty(e.getText())) {
            throw new TechnoratiApiException(e.getText());
        }
    }

    /**
     * Close the {@link Closeable} object ignoring any errors.
     * 
     * @param closeable The {@link Closeable} object to close.
     */
    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
        }
    }
}
