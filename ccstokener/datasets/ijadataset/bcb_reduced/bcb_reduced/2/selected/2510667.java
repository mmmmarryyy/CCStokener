package com.jradar.service;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.core.CategoryIF;
import de.nava.informa.impl.basic.Category;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.io.*;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import com.nnm.parser.ParsersFacade;
import com.jradar.mapper.tagthe.TagtheMapper;
import com.jradar.mapper.tagthe.Dim;
import com.jradar.mapper.tagthe.TagtheBean;
import com.jradar.model.NewsItem;

/**
 * Alexander Kirin
 * User: helium
 * Date: May 27, 2007
 * Time: 5:15:57 PM
 * Used to retrive tags of content using the tagthe.net web service
 */
public class TagtheService {

    public static final String TEXT_PARAM = "text";

    public static final String URL_PARAM = "url";

    private String cmdUrl = "http://tagthe.net/api/";

    private String paramName = URL_PARAM;

    public TagtheService(String cmdUrl, String paramName) {
        if (cmdUrl != null) {
            this.cmdUrl = cmdUrl;
        }
        if (paramName != null) {
            this.paramName = paramName;
        }
    }

    public TagtheService() {
    }

    /**
     * retrievs text tags and appends them to an item (category) of the channel
     *
     * @param channel
     */
    public void appendTags(ChannelIF channel) {
        Set items = channel.getItems();
        Iterator<ItemIF> it = items.iterator();
        ParsersFacade facade = new ParsersFacade();
        TagtheMapper mapper = new TagtheMapper();
        while (it.hasNext()) {
            ItemIF item = it.next();
            String text = facade.getTextContent(item.getLink().toString());
            String t = this.getTags(text);
            InputStream is = this.getTagsAsInputStream(text);
            TagtheBean tb = (TagtheBean) mapper.getBean(is);
            List dims = tb.getDims();
            String[] tags = null;
            for (int i = 0; i < dims.size(); i++) {
                Dim dim = (Dim) dims.get(i);
                if ("topic".equals(dim.getType())) {
                    tags = dim.getTags();
                    if (tags != null) {
                        for (int j = 0; j < tags.length; j++) {
                            String tag = tags[j];
                            CategoryIF category = new Category();
                            category.setTitle(tag);
                            item.addCategory(category);
                        }
                    }
                }
            }
        }
    }

    public String getTags(URL url) {
        StringBuffer xml = new StringBuffer();
        OutputStreamWriter osw = null;
        BufferedReader br = null;
        try {
            String reqData = URLEncoder.encode(paramName, "UTF-8") + "=" + URLEncoder.encode(url.toString(), "UTF-8");
            URL service = new URL(cmdUrl);
            URLConnection urlConn = service.openConnection();
            urlConn.setDoOutput(true);
            urlConn.connect();
            osw = new OutputStreamWriter(urlConn.getOutputStream());
            osw.write(reqData);
            osw.flush();
            br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                xml.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return xml.toString();
    }

    public String getTags(String content) {
        StringBuffer xml = new StringBuffer();
        OutputStreamWriter osw = null;
        BufferedReader br = null;
        try {
            String reqData = URLEncoder.encode(paramName, "UTF-8") + "=" + URLEncoder.encode(content, "UTF-8");
            URL service = new URL(cmdUrl);
            URLConnection urlConn = service.openConnection();
            urlConn.setDoOutput(true);
            urlConn.connect();
            osw = new OutputStreamWriter(urlConn.getOutputStream());
            osw.write(reqData);
            osw.flush();
            br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                xml.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return xml.toString();
    }

    public InputStream getTagsAsInputStream(String content) {
        OutputStreamWriter osw = null;
        InputStream is = null;
        try {
            String reqData = URLEncoder.encode(paramName, "UTF-8") + "=" + URLEncoder.encode(content, "UTF-8");
            URL service = new URL(cmdUrl);
            URLConnection urlConn = service.openConnection();
            urlConn.setDoOutput(true);
            urlConn.connect();
            osw = new OutputStreamWriter(urlConn.getOutputStream());
            osw.write(reqData);
            osw.flush();
            is = urlConn.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return is;
    }
}
