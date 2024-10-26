package cn.poco.food.published;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpParameters;
//import weibo4android.Status;
//import weibo4android.Weibo;
//import weibo4android.WeiboException;
//import weibo4android.http.AccessToken;
import cn.poco.util.Cons;
import cn.poco.util.oauth.QWeiboSyncApi;
import cn.poco.util.oauth.QWeiboType.ResultType;

public class ShareBlogService {
	public static boolean shareBolgToQQ(String requestToken, String requestTokenSecrect, String content, String jing,
			String wei, String pic) {
		QWeiboSyncApi qWeiboSyncApi = new QWeiboSyncApi();
		String res = qWeiboSyncApi.publishMsg(Cons.QQ_CONSUMER_KEY, Cons.QQ_CONSUMER_SECRET, requestToken,
				requestTokenSecrect, content, jing, wei, pic, ResultType.ResultType_Xml);
		if ("0".equals(res.substring(res.indexOf("<ret>") + 5, res.indexOf("</ret>")))) {
			return true;
		}
		return false;
	}

	public static boolean shareToSina(String requestToken, String requestTokenSecrect, String status, String gpsLat,
			String gpsLong) {
		try {
			OAuthConsumer consumer = new DefaultOAuthConsumer(Cons.SINA_CONSUMER_KEY, Cons.SINA_CONSUMER_SECRET);
			consumer.setTokenWithSecret(requestToken, requestTokenSecrect);
			URL url = new URL("http://api.t.sina.com.cn/statuses/update.json");
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			request.setDoOutput(true);
			request.setRequestMethod("POST");
			HttpParameters para = new HttpParameters();
			para.put("status", URLEncoder.encode(status, "utf-8").replaceAll("\\+", "%20"));

			if (gpsLat != null && !"".equals(gpsLat)) {
				para.put("lat", URLEncoder.encode(gpsLat, "utf-8").replaceAll("\\+", "%20"));
			}

			if (gpsLong != null && !"".equals(gpsLong)) {
				para.put("long", URLEncoder.encode(gpsLong, "utf-8").replaceAll("\\+", "%20"));
			}

			consumer.setAdditionalParameters(para);
			consumer.sign(request);
			OutputStream ot = request.getOutputStream();
			ot.write(("status=" + URLEncoder.encode(status, "utf-8")).replaceAll("\\+", "%20").getBytes());
			if (gpsLat != null && !"".equals(gpsLat)) {
				ot.write(("&lat=" + URLEncoder.encode(gpsLat, "utf-8")).replaceAll("\\+", "%20").getBytes());
			}
			if (gpsLong != null && !"".equals(gpsLong)) {
				ot.write(("&long=" + URLEncoder.encode(gpsLong, "utf-8")).replaceAll("\\+", "%20").getBytes());
			}
			ot.flush();
			ot.close();
			request.connect();
			if (request.getResponseCode() == 200) {
				return true;
			}
		} catch (Exception e) {

		}
		return false;
	}

	public static boolean shareToSinaWithFile(String requestToken, String requestTokenSecrect, String blogContent,
			String gpsLat, String gpsLong, String picture) {
		try {
			String glat = "";
			String Gpslat = "";
			String glong = "";
			String Gpslong = "";
			OAuthConsumer consumer = new DefaultOAuthConsumer(Cons.SINA_CONSUMER_KEY, Cons.SINA_CONSUMER_SECRET);
			consumer.setTokenWithSecret(requestToken, requestTokenSecrect);
			URL url = new URL("http://api.t.sina.com.cn/statuses/upload.json");
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			request.setDoOutput(true);
			request.setRequestMethod("POST");
			HttpParameters para = new HttpParameters();
			// String status =
			// URLEncoder.encode(blogContent,"utf-8").replaceAll("\\+", "%20");
			String status = blogContent;
			if (gpsLong != null && !"".equals(gpsLong)) {
				Gpslong = URLEncoder.encode(gpsLong, "utf-8").replaceAll("\\+", "%20");
			}
			if (gpsLat != null && !"".equals(gpsLat)) {
				Gpslat = URLEncoder.encode(gpsLat, "utf-8").replaceAll("\\+", "%20");
			}
			para.put("status", URLEncoder.encode(blogContent, "utf-8").replaceAll("\\+", "%20"));
			if (gpsLat != null && !"".equals(gpsLat)) {
				para.put("lat", URLEncoder.encode(gpsLat, "utf-8").replaceAll("\\+", "%20"));
			}
			if (gpsLong != null && !"".equals(gpsLong)) {
				para.put("long", URLEncoder.encode(gpsLong, "utf-8").replaceAll("\\+", "%20"));
			}
			String boundary = "---------------------------37531613912423";
			String content = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"status\"\r\n\r\n";
			if (gpsLat != null && !"".equals(gpsLat)) {
				glat = "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"lat\"\r\n\r\n";
			}
			if (gpsLong != null && !"".equals(gpsLong)) {
				glong = "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"long\"\r\n\r\n";
			}
			String pic = "\r\n--"
					+ boundary
					+ "\r\nContent-Disposition: form-data; name=\"pic\"; filename=\"icon.png\"\r\nContent-Type: image/jpeg\r\n\r\n";
			byte[] end_data = ("\r\n--" + boundary + "--\r\n").getBytes();
			File f = new File(picture);
			FileInputStream stream = new FileInputStream(f);
			byte[] file = new byte[(int) f.length()];
			stream.read(file);
			request.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary); // 设置表单类型和分隔符
			request.setRequestProperty(
					"Content-Length",
					String.valueOf(content.getBytes().length + status.getBytes().length + glat.getBytes().length
							+ Gpslat.getBytes().length + glong.getBytes().length + Gpslong.getBytes().length
							+ pic.getBytes().length + f.length() + end_data.length)); // 设置内容长度

			consumer.setAdditionalParameters(para);
			consumer.sign(request);
			OutputStream ot = request.getOutputStream();
			ot.write(content.getBytes());
			ot.write(status.getBytes());
			if (gpsLat != null && !"".equals(gpsLat)) {
				ot.write(glat.getBytes());
				ot.write((Gpslat).getBytes());
			}
			if (gpsLong != null && !"".equals(gpsLong)) {
				ot.write(glong.getBytes());
				ot.write((Gpslong).getBytes());
			}
			ot.write(pic.getBytes());
			ot.write(file);
			ot.write(end_data);
			ot.flush();
			ot.close();
			request.connect();
			if (request.getResponseCode() == 200) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean uploadStatus(String token, String tokenSecret, File aFile, String status) {
		OAuthConsumer httpOAuthConsumer;
		httpOAuthConsumer = new DefaultOAuthConsumer(token, tokenSecret);
		httpOAuthConsumer.setTokenWithSecret(token, tokenSecret);
		boolean result = false;
		try {
			URL url = new URL("http://api.t.sina.com.cn/statuses/upload.json");
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			request.setDoOutput(true);
			request.setRequestMethod("POST");
			HttpParameters para = new HttpParameters();
			para.put("status", URLEncoder.encode(status, "utf-8").replaceAll("\\+", "%20"));
			String boundary = "---------------------------37531613912423";
			String content = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"status\"\r\n\r\n";
			String pic = "\r\n--"
					+ boundary
					+ "\r\nContent-Disposition: form-data; name=\"pic\"; filename=\"image.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n";
			byte[] end_data = ("\r\n--" + boundary + "--\r\n").getBytes();
			FileInputStream stream = new FileInputStream(aFile);
			byte[] file = new byte[(int) aFile.length()];
			stream.read(file);
			request.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary); // 设置表单类型和分隔符
			request.setRequestProperty(
					"Content-Length",
					String.valueOf(content.getBytes().length + status.getBytes().length + pic.getBytes().length
							+ aFile.length() + end_data.length)); // 设置内容长度
			httpOAuthConsumer.setAdditionalParameters(para);
			httpOAuthConsumer.sign(request);
			OutputStream ot = request.getOutputStream();
			ot.write(content.getBytes());
			ot.write(status.getBytes());
			ot.write(pic.getBytes());
			ot.write(file);
			ot.write(end_data);
			ot.flush();
			ot.close();
			request.connect();
			System.out.println(request.getResponseCode());
			if (200 == request.getResponseCode()) {

				result = true;
			} else {
				result = false;
			}

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthMessageSignerException e) {
			e.printStackTrace();
		} catch (OAuthExpectationFailedException e) {
			e.printStackTrace();
		} catch (OAuthCommunicationException e) {
			e.printStackTrace();
		}
		return result;
	}

	/*public static boolean uploadStatusSdk(String token, String tokenSecret, File aFile, String content) {
		boolean b = false;
		Weibo weibo = new Weibo();
		weibo.setToken(token, tokenSecret);
		 AccessToken accessToken = null;
		try {
			Status status = weibo.uploadStatus(content, aFile);
			System.out.println(status.getText());
		} catch (WeiboException e) {
			e.printStackTrace();
		}
		return false;
	}*/

}
