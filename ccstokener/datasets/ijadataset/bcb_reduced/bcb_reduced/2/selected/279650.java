package cn.poco.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class UrlConnectionUtil {

	/*
	 * public static InputStream getRequest(String path) throws Exception{ URL
	 * url = new URL(path); HttpURLConnection conn =
	 * (HttpURLConnection)url.openConnection(); conn.setConnectTimeout(10 *
	 * 3000); conn.setReadTimeout(10 * 3000); conn.setRequestMethod("GET"); int
	 * code = conn.getResponseCode(); if(code==200){ InputStream inStream =
	 * conn.getInputStream(); return inStream; }else{ throw new
	 * Exception("conn exception"); } }
	 */

	/*
	 * public static InputStream getRequest(String path) throws Exception{
	 * InputStream inStream = null; HttpGet httpRequest = new HttpGet(path);
	 * HttpClient httpClient = new DefaultHttpClient();
	 * httpClient.getParams().setIntParameter
	 * (CoreConnectionPNames.CONNECTION_TIMEOUT, 18 * 1000); HttpResponse
	 * httpResponse = httpClient.execute(httpRequest);
	 * if(httpResponse.getStatusLine().getStatusCode()==HttpStatus.SC_OK){
	 * inStream = httpResponse.getEntity().getContent(); }else{ throw new
	 * Exception("conn exception"); }
	 * 
	 * return inStream; }
	 */

	private static final DefaultHttpClient sClient;
	static {

		// Set basic data
		HttpParams params = new BasicHttpParams(); // 创建 HttpParams 以用来设置 HTTP
													// 参数
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(params, true);
		HttpProtocolParams.setUserAgent(params, "FoodParadise");

		// Make pool

		ConnPerRoute connPerRoute = new ConnPerRouteBean(12);
		ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
		ConnManagerParams.setMaxTotalConnections(params, 20);

		// Set timeout
		// 设置连接超时和 Socket 超时，以及 Socket 缓存大小
		HttpConnectionParams.setStaleCheckingEnabled(params, false);
		HttpConnectionParams.setConnectionTimeout(params, 60 * 1000);
		HttpConnectionParams.setSoTimeout(params, 60 * 1000);
		HttpConnectionParams.setSocketBufferSize(params, 8192);

		// Some client params
		HttpClientParams.setRedirecting(params, false);

		// Register http/s shemas!
		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
		sClient = new DefaultHttpClient(conMgr, params);
	}

	public UrlConnectionUtil() {
	}

	public static InputStream getRequest(String path) throws Exception {
		HttpGet httpGet = new HttpGet(path);
		HttpResponse httpResponse = sClient.execute(httpGet);
		if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(httpResponse.getEntity());
			return bufHttpEntity.getContent();
		} else {
			return null;
		}
	}

	public static InputStream executePost(String path, Map<String, String> params) throws Exception {
		HttpPost httpPost = new HttpPost(path);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> param : params.entrySet()) {
			postParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
		}
		HttpEntity entity = new UrlEncodedFormEntity(postParams, "UTF-8");
		httpPost.setEntity(entity);

		HttpResponse httpResponse = sClient.execute(httpPost);
		if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			return httpResponse.getEntity().getContent();
		} else {
			return null;
		}
	}
}
