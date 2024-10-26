package com.ywh.train.logic;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import com.ywh.train.Constants;
import com.ywh.train.Util;
import com.ywh.train.bean.Result;
import com.ywh.train.bean.TrainQueryInfo;
import com.ywh.train.bean.UserInfo;

/**
 * 车票订购网络处理core
 */
public class TrainClient {

    public static String JSESSIONID = null;

    public static String BIGipServerotsweb = null;

    Logger log = Logger.getLogger(getClass());

    private HttpClient httpclient = null;

    /**
	 * 构造函数 
	 */
    public TrainClient(HttpClient client) {
        this.httpclient = client;
    }

    /**
	 * 获取令牌
	 * @return
	 */
    public String getToken() {
        log.debug("-------------------get token start-------------------");
        HttpGet get = new HttpGet(Constants.GET_TOKEN_URL);
        String token = null;
        BufferedReader br = null;
        try {
            HttpResponse response = httpclient.execute(get);
            HttpEntity entity = response.getEntity();
            br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.indexOf("org.apache.struts.taglib.html.TOKEN") > -1) {
                    token = line;
                }
            }
            if (token != null) {
                int start = token.indexOf("value=\"");
                int end = token.indexOf("\"></div>");
                token = token.substring(start + 7, end);
            } else {
                log.warn("book tikte error, can't get token!");
            }
        } catch (Exception e) {
            log.error(e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.debug("TOKEN = " + token);
        log.debug("-------------------get token end-------------------");
        return token;
    }

    /** 
	 * 预订车票

	 * @param rangDate
	 * @param startDate
	 * @param train
	 * @return
	 */
    public Result book(String rangDate, String startDate, TrainQueryInfo train) {
        log.debug("-------------------book start-------------------");
        Result rs = new Result();
        HttpPost post = new HttpPost(Constants.BOOK_URL);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("from_station_telecode", train.getFromStationCode()));
        formparams.add(new BasicNameValuePair("from_station_telecode_name", train.getFromStation()));
        formparams.add(new BasicNameValuePair("include_student", "00"));
        formparams.add(new BasicNameValuePair("lishi", "" + Util.getHour2Min(train.getTakeTime())));
        formparams.add(new BasicNameValuePair("round_start_time_str", rangDate));
        formparams.add(new BasicNameValuePair("round_train_date", Util.getCurDate()));
        formparams.add(new BasicNameValuePair("seattype_num", ""));
        formparams.add(new BasicNameValuePair("single_round_type", "1"));
        formparams.add(new BasicNameValuePair("start_time_str", rangDate));
        formparams.add(new BasicNameValuePair("station_train_code", train.getTrainCode()));
        formparams.add(new BasicNameValuePair("to_station_telecode", train.getToStationCode()));
        formparams.add(new BasicNameValuePair("to_station_telecode_name", train.getToStation()));
        formparams.add(new BasicNameValuePair("train_class_arr", "QB#D#Z#T#K#QT#"));
        formparams.add(new BasicNameValuePair("train_date", startDate));
        formparams.add(new BasicNameValuePair("train_pass_type", "QB"));
        formparams.add(new BasicNameValuePair("train_start_time", train.getStartTime()));
        BufferedReader br = null;
        try {
            UrlEncodedFormEntity uef = new UrlEncodedFormEntity(formparams, HTTP.UTF_8);
            post.setEntity(uef);
            HttpResponse response = httpclient.execute(post);
            HttpEntity entity = response.getEntity();
            log.debug(response.getStatusLine());
            br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
            while ((br.readLine()) != null) {
            }
            rs.setState(Result.SUCC);
            rs.setMsg(response.getStatusLine().toString());
        } catch (Exception e) {
            log.error(e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.debug("-------------------book end-------------------");
        return rs;
    }

    /**
	 * 提交订单
	 * @param randCode
	 * @param token
	 * @param user
	 * @param train
	 * @return
	 */
    public Result submiOrder(String randCode, String token, List<UserInfo> users, TrainQueryInfo train) {
        log.debug("-------------------submit order start-------------------");
        Result rs = new Result();
        HttpPost post = new HttpPost(Constants.SUBMIT_URL);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        for (int i = 0; i < users.size(); i++) {
            formparams.add(new BasicNameValuePair("checkbox" + i, "" + i));
        }
        formparams.add(new BasicNameValuePair("checkbox9", "Y"));
        formparams.add(new BasicNameValuePair("checkbox9", "Y"));
        formparams.add(new BasicNameValuePair("checkbox9", "Y"));
        formparams.add(new BasicNameValuePair("checkbox9", "Y"));
        formparams.add(new BasicNameValuePair("checkbox9", "Y"));
        for (UserInfo user : users) {
            formparams.add(new BasicNameValuePair("oldPassengers", user.getSimpleText()));
        }
        for (int i = users.size(); i < 5; i++) {
            formparams.add(new BasicNameValuePair("oldPassengers", ""));
        }
        formparams.add(new BasicNameValuePair("orderRequest.bed_level_order_num", "000000000000000000000000000000"));
        formparams.add(new BasicNameValuePair("orderRequest.cancel_flag", "1"));
        formparams.add(new BasicNameValuePair("orderRequest.end_time", train.getEndTime()));
        formparams.add(new BasicNameValuePair("orderRequest.from_station_name", train.getFromStation()));
        formparams.add(new BasicNameValuePair("orderRequest.from_station_telecode", train.getFromStationCode()));
        formparams.add(new BasicNameValuePair("orderRequest.id_mode", "Y"));
        formparams.add(new BasicNameValuePair("orderRequest.reserve_flag", "A"));
        formparams.add(new BasicNameValuePair("orderRequest.seat_type_code", ""));
        formparams.add(new BasicNameValuePair("orderRequest.start_time", train.getStartTime()));
        formparams.add(new BasicNameValuePair("orderRequest.station_train_code", train.getTrainNo()));
        formparams.add(new BasicNameValuePair("orderRequest.ticket_type_order_num", ""));
        formparams.add(new BasicNameValuePair("orderRequest.to_station_name", train.getToStation()));
        formparams.add(new BasicNameValuePair("orderRequest.to_station_telecode", train.getToStationCode()));
        formparams.add(new BasicNameValuePair("orderRequest.train_date", train.getTrainDate()));
        formparams.add(new BasicNameValuePair("orderRequest.train_no", train.getTrainCode()));
        formparams.add(new BasicNameValuePair("org.apache.struts.taglib.html.TOKEN", token));
        for (UserInfo user : users) {
            formparams.add(new BasicNameValuePair("passengerTickets", user.getText()));
        }
        for (int i = 1; i <= users.size(); i++) {
            UserInfo user = users.get(i - 1);
            formparams.add(new BasicNameValuePair("passenger_" + i + "_cardno", user.getID()));
            formparams.add(new BasicNameValuePair("passenger_" + i + "_cardtype", user.getCardType()));
            formparams.add(new BasicNameValuePair("passenger_" + i + "_mobileno", user.getPhone()));
            formparams.add(new BasicNameValuePair("passenger_" + i + "_name", user.getName()));
            formparams.add(new BasicNameValuePair("passenger_" + i + "_seat", user.getSeatType()));
            formparams.add(new BasicNameValuePair("passenger_" + i + "_ticket", user.getTickType()));
        }
        formparams.add(new BasicNameValuePair("randCode", randCode));
        formparams.add(new BasicNameValuePair("textfield", "中文或拼音首字母"));
        String responseBody = null;
        try {
            UrlEncodedFormEntity uef = new UrlEncodedFormEntity(formparams, HTTP.UTF_8);
            post.setEntity(uef);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            responseBody = httpclient.execute(post, responseHandler);
            String ans = Util.getMessageFromHtml(responseBody);
            if (ans.isEmpty()) {
                rs.setState(Result.UNCERTAINTY);
                rs.setMsg("好像订票成功了");
            } else {
                if (ans.contains("由于您取消次数过多")) {
                    rs.setState(Result.CANCEL_TIMES_TOO_MUCH);
                    rs.setMsg(ans);
                } else if (ans.contains("验证码不正确")) {
                    rs.setState(Result.RAND_CODE_ERROR);
                    rs.setMsg(ans);
                } else if (ans.contains("售票实行实名制")) {
                    rs.setState(Result.REPEAT_BUY_TICKET);
                    rs.setMsg(ans);
                } else if (ans.contains("号码输入有误")) {
                    rs.setState(Result.ERROR_CARD_NUMBER);
                    rs.setMsg(ans);
                } else {
                    rs.setState(Result.OTHER);
                    rs.setMsg(ans);
                }
            }
            log.debug(ans);
        } catch (Exception e) {
            log.error(e);
        }
        log.debug("-------------------submit order end-------------------");
        return rs;
    }

    /**
	 * 查询列车信息
	 * @param from
	 * @param to
	 * @param startDate
	 * @param rangDate
	 * @return
	 */
    public List<TrainQueryInfo> queryTrain(String from, String to, String startDate, String rangDate) {
        log.debug("-------------------query train start-------------------");
        if (rangDate == null || rangDate.isEmpty()) {
            rangDate = "00:00--24:00";
        }
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("method", "queryLeftTicket"));
        parameters.add(new BasicNameValuePair("includeStudent", "00"));
        parameters.add(new BasicNameValuePair("orderRequest.from_station_telecode", Util.getCityCode(from)));
        parameters.add(new BasicNameValuePair("orderRequest.start_time_str", rangDate));
        parameters.add(new BasicNameValuePair("orderRequest.to_station_telecode", Util.getCityCode(to)));
        parameters.add(new BasicNameValuePair("orderRequest.train_date", startDate));
        parameters.add(new BasicNameValuePair("orderRequest.train_no", ""));
        parameters.add(new BasicNameValuePair("seatTypeAndNum", ""));
        parameters.add(new BasicNameValuePair("trainClass", "QB#D#Z#T#K#QT#"));
        parameters.add(new BasicNameValuePair("trainPassType", "QB"));
        HttpGet get = new HttpGet(Constants.QUERY_TRAIN_URL + URLEncodedUtils.format(parameters, HTTP.UTF_8));
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = null;
        List<TrainQueryInfo> all = new ArrayList<TrainQueryInfo>();
        try {
            responseBody = httpclient.execute(get, responseHandler);
            all = Util.parserQueryInfo(responseBody, startDate);
        } catch (Exception e) {
            log.error(e);
        }
        log.debug("-------------------query train end-------------------");
        return all;
    }

    /**
	 * 登录
	 * @param username
	 * @param password
	 * @param randCode
	 * @return
	 */
    public Result login(String username, String password, String randCode) {
        log.debug("-----------------login start-----------------------");
        Result rs = new Result();
        HttpPost httppost = new HttpPost(Constants.LOGIN_URL);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("method", "login"));
        parameters.add(new BasicNameValuePair("loginUser.user_name", username));
        parameters.add(new BasicNameValuePair("nameErrorFocus", ""));
        parameters.add(new BasicNameValuePair("passwordErrorFocus", ""));
        parameters.add(new BasicNameValuePair("randCode", randCode));
        parameters.add(new BasicNameValuePair("randErrorFocus", ""));
        parameters.add(new BasicNameValuePair("user.password", password));
        String responseBody = null;
        try {
            UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, HTTP.UTF_8);
            httppost.setEntity(uef);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            responseBody = httpclient.execute(httppost, responseHandler);
            String info = Util.removeTagFromHtml(responseBody);
            if (responseBody.contains(Constants.USER_NOT_EXIST)) {
                log.error("用户:" + username + Constants.USER_NOT_EXIST);
                rs.setState(Result.ACC_ERROR);
                rs.setMsg(Constants.USER_NOT_EXIST);
            } else if (info.contains(Constants.USER_PWD_ERR)) {
                log.error("用户:" + username + Constants.USER_PWD_ERR);
                rs.setState(Result.PWD_ERROR);
                rs.setMsg(Constants.USER_PWD_ERR);
            } else if (info.contains(Constants.USER_SUCC_INFO)) {
                int index = responseBody.indexOf("-->");
                log.debug(responseBody.substring(index + 4));
                rs.setState(Result.SUCC);
                rs.setMsg(Constants.LOGIN_SUC);
                List<Cookie> cookies = ((DefaultHttpClient) httpclient).getCookieStore().getCookies();
                for (Cookie cookie : cookies) {
                    String name = cookie.getName();
                    if ("JSESSIONID".equals(name)) {
                        JSESSIONID = cookie.getValue();
                    } else if ("BIGipServerotsweb".equals(name)) {
                        BIGipServerotsweb = cookie.getValue();
                    }
                }
                System.out.println("JSESSIONID=" + TrainClient.JSESSIONID + ",BIGipServerotsweb=" + TrainClient.BIGipServerotsweb);
            } else if (info.contains(Constants.CODE_ERROR)) {
                log.warn("用户:" + username + Constants.CODE_ERROR);
                rs.setState(Result.RAND_CODE_ERROR);
                rs.setMsg(Constants.CODE_ERROR);
            } else if (responseBody.contains(Constants.LOGIN_ERR_INFO)) {
                log.info("用户:" + username + Constants.USER_RELOGIN);
                rs.setState(Result.LOGIN_ERROR);
                rs.setMsg(Constants.USER_RELOGIN);
            } else if (responseBody.contains(Constants.LOGIN_LOSTS_POEPLE)) {
                log.info("用户:" + username + Constants.LOGIN_LOSTS_POEPLE);
                rs.setState(Result.LOST_OF_PEOPLE);
                rs.setMsg(Constants.LOGIN_LOSTS_POEPLE);
            } else {
                log.info("用户:" + username + Constants.UNKNOW_ERROR);
                rs.setState(Result.OTHER);
                rs.setMsg(Constants.UNKNOW_ERROR);
                System.out.println(responseBody);
            }
        } catch (Exception e) {
            log.error(e);
        }
        log.debug("-------------------login end---------------------");
        return rs;
    }

    /**
	 * 查询预订信息
	 * 
	 * @param httpclient
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
    public Result queryOrder() {
        log.debug("-------------------query order start-------------------");
        Result rs = new Result();
        HttpGet httpget = new HttpGet(Constants.QUERY_ORDER_URL);
        StringBuilder responseBody = new StringBuilder();
        BufferedReader br = null;
        try {
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                responseBody.append(line);
            }
            String msg = Util.removeTagFromHtml(responseBody.toString());
            if (!msg.isEmpty()) {
                int index = msg.indexOf("-->");
                msg = msg.substring(index + 4);
                String[] allInfo = msg.split("！");
                if (allInfo.length > 1) {
                    String usefulInfo = allInfo[1];
                    if (usefulInfo.contains("待支付")) {
                        rs.setState(Result.HAVE_NO_PAY_TICKET);
                        rs.setMsg(usefulInfo);
                    } else if (usefulInfo.contains("取消次数过多")) {
                        rs.setState(Result.CANCEL_TIMES_TOO_MUCH);
                        rs.setMsg(usefulInfo);
                    } else {
                        rs.setMsg(usefulInfo);
                    }
                } else {
                    rs.setState(Result.NO_BOOKED_TICKET);
                    rs.setMsg(msg);
                }
            } else {
                rs.setMsg(msg);
            }
        } catch (Exception e) {
            log.error(e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.debug("-------------------query order end---------------------");
        return rs;
    }

    /**
	 * 弹窗显示指定url的验证码并手工输入
	 * @param url
	 * @return
	 * @throws IOException
	 */
    String getCode(String url) throws IOException {
        JFrame frame = new JFrame("验证码");
        JLabel label = new JLabel(new ImageIcon(getCodeByte(url)), JLabel.CENTER);
        frame.add(label);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        String rd = br.readLine();
        frame.dispose();
        return rd;
    }

    /**
	 * 获取指定url的验证码图片字节信息
	 * @param url
	 * @return
	 */
    public byte[] getCodeByte(String url) {
        log.debug("-------------------get randcode start-------------------");
        HttpGet get = new HttpGet(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            HttpResponse response = httpclient.execute(get);
            HttpEntity entity = response.getEntity();
            log.debug(response.getStatusLine());
            if (entity != null) {
                InputStream is = entity.getContent();
                byte[] buf = new byte[1024];
                int len = -1;
                while ((len = is.read(buf)) > -1) {
                    baos.write(buf, 0, len);
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        log.debug("-------------------get randcode end-------------------");
        return baos.toByteArray();
    }
}
