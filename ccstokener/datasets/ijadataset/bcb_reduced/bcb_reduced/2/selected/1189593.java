package com.rb.lottery.analysis.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import com.rb.lottery.analysis.common.EncodingConstants;
import com.rb.lottery.analysis.common.EnvironmentConstants;
import com.rb.lottery.analysis.common.SystemConstants;

/**
 * @类功能说明:
 * @类修改者:
 * @修改日期:
 * @修改说明:
 * @作者: robin
 * @创建时间: 2011-12-9 上午10:04:27
 * @版本: 1.0.0
 */
public class KjDataHttpClient {

    /**
	 * 处理GET请求，返回整个页面
	 * @param url
	 *            访问地址
	 * @param params
	 *            编码参数
	 * @return String
	 * @throws Exception
	 * @throws Exception
	 */
    public synchronized String doGet(String url, String... params) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpProtocolParams.setUserAgent(httpclient.getParams(), EnvironmentConstants.HTTPCLIENT_USERAGENT);
        String charset = EncodingConstants.UTF8_ENCODING;
        if (null != params && params.length > 0) {
            charset = params[0];
        }
        HttpGet httpget = new HttpGet();
        String content = SystemConstants.EMPTY_STRING;
        httpget.setURI(new java.net.URI(url));
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            content = EntityUtils.toString(entity, charset);
            httpget.abort();
            httpclient.getConnectionManager().shutdown();
        }
        return content;
    }
}
