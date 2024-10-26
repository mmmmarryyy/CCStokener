import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngineManager;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnConnectionPNames;
import org.apache.http.conn.params.ConnConnectionParamBean;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.RouteInfo;
import org.apache.http.conn.routing.RouteTracker;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RoutedRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.dom4j.Comment;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Entity;
import org.dom4j.Text;
import org.dom4j.io.SAXReader;
import org.dom4j.io.SAXWriter;
import org.dom4j.io.XMLWriter;

class T implements Runnable {

    @Override
    public void run() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        for (int i = 0; i < 100; i++) {
            try {
                URL url = new URL("http://192.168.1.139/num/uareveice?sid=123&cid=123&verifycode=12345678abcdef&pin=bW89JnVhPU9wZXJhJnA9YWM1ZmE0NzBjNDIxMDJkNGQ0NWM4N2NiOWI4NjZjNTA=");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                conn.getInputStream().close();
                conn.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

public class Test {

    private static Pattern pattern = Pattern.compile(".*<hRet>(.*)</hRet>.*");

    private static void gbk2utf8(File dir, FileFilter filter) throws Exception {
        for (File file : dir.listFiles(filter)) {
            if (file.isDirectory()) {
                gbk2utf8(file, filter);
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                    sb.append("\r\n");
                }
                in.close();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                out.write(sb.toString());
                out.close();
            }
        }
    }

    private static void test(File file) throws Exception {
        System.out.println(file);
        sb.append(file.getName().replace("proxy-info-2011-09-20-", "").replace(".log", "点"));
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        int count = 0, success = 0, fail = 0;
        while ((line = in.readLine()) != null) {
            if (line.contains("<msgType>BuyGameToolResp</msgType>")) {
                count++;
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    String s = m.group(1);
                    if ("0".equals(s)) {
                        success++;
                    } else if ("1".equals(s)) {
                        fail++;
                    }
                }
            }
        }
        in.close();
        sb.append("共计费");
        sb.append(count);
        sb.append("次，成功");
        sb.append(success);
        sb.append("次，失败");
        sb.append(fail);
        sb.append("\r\n");
    }

    private static Map<String, String> gm = new HashMap<String, String>();

    private static Map<String, String> om = new HashMap<String, String>();

    static {
        gm.put("g+i游戏", "搜狐");
        gm.put("g+暴风城", "动力创想");
        gm.put("g+电影大片", "掌中米格");
        gm.put("g+好又多", "盈正");
        gm.put("g+嘉年华", "摩瑞贝");
        gm.put("g+尽情玩吧", "兆荣联合");
        gm.put("g+经典游戏", "新浪");
        gm.put("g+开心畅游", "掌趣");
        gm.put("g+热门游戏-热舞天使", "空中信使");
        gm.put("g+热门游戏T", "空中信使");
        gm.put("g+热门游戏", "雷霆万钧");
        gm.put("g+私藏经典", "秦网");
        gm.put("g+旺旺包", "易动无限");
        gm.put("g+新潮流", "申达宏通");
        gm.put("g+游戏狂人", "丰尚佳诚");
        gm.put("g+游戏大作", "掌中地带");
        gm.put("g+游戏发烧包", "掌趣");
        gm.put("g+游戏盒子", "空中信使");
        gm.put("g+游戏达人", "摩讯");
        gm.put("g+游戏区", "因特莱斯");
        gm.put("g+最佳游戏", "中西网联");
        gm.put("g+游戏精选", "丰尚佳诚");
        gm.put("g+精选合集", "新浪");
        gm.put("g+休闲部落", "搜狐");
        gm.put("g+掌上乐园", "广州诠星网络科技");
        gm.put("g+游戏大本营", "广州蓝喜鹊");
    }

    private static StringBuilder sb = new StringBuilder();

    public static strictfp void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("D:\\G+.csv"), "GBK"));
        Map<String, Long> map = new LinkedHashMap<String, Long>();
        String line;
        while ((line = in.readLine()) != null) {
            String[] s = line.split(",");
            if (s.length == 4) {
                if (!"0".equals(s[3])) {
                    String key = s[1] + "," + s[2];
                    Long l = map.get(key);
                    if (l == null) l = 0L;
                    map.put(key, l + Long.parseLong(s[3]));
                }
            } else {
                System.out.println("无效记录：" + line);
            }
        }
        in.close();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("D:\\G+收入.csv"), "GBK"));
        Iterator<Entry<String, Long>> i = map.entrySet().iterator();
        while (i.hasNext()) {
            Entry<String, Long> e = i.next();
            String[] s = e.getKey().split(",");
            out.write(s[0]);
            if (gm.containsKey(s[0])) {
                out.write('（');
                out.write(gm.get(s[0]));
                out.write('）');
            }
            out.write(',');
            out.write(s[1]);
            out.write(',');
            out.write(e.getValue().toString());
            out.write("\r\n");
        }
        out.close();
    }

    public static long ip2long(String ip) {
        String[] ips = ip.split("[.]");
        long num = 16777216L * Long.parseLong(ips[0]) + 65536L * Long.parseLong(ips[1]) + 256 * Long.parseLong(ips[2]) + Long.parseLong(ips[3]);
        return num;
    }

    public static void bd() throws Exception {
        String path = "C:/Documents and Settings/Administrator/桌面/Civilization_T320X480";
        ClassPool cp = ClassPool.getDefault();
        cp.insertClassPath(path);
        CtClass cc = cp.get("bd");
        cc.addField(CtField.make("private long crack = System.currentTimeMillis();", cc));
        for (CtMethod cm : cc.getDeclaredMethods()) {
            String str = "{System.out.println(\"crack:" + Modifier.toString(cm.getModifiers()) + " " + cm.getName() + cm.getMethodInfo().getDescriptor() + ":\"+System.currentTimeMillis());}";
            cm.insertBefore(str);
            System.out.println(str);
            String methodInfo = cm.getMethodInfo().toString();
            System.out.println(methodInfo);
            if ("a (Lcf;)Z".equals(methodInfo)) {
                StringBuilder sb = new StringBuilder();
                sb.append("long time = 2000 - System.currentTimeMillis() + crack;");
                sb.append("System.out.println(\"crack:\"+time);");
                sb.append("if(time>0) Thread.sleep(time);");
                sb.append("crack = System.currentTimeMillis();");
                cm.insertBefore(sb.toString());
                System.out.println(sb.toString());
            }
        }
        cc.writeFile();
    }

    public static void bb() throws Exception {
        String path = "C:/Documents and Settings/Administrator/桌面/Civilization_T320X480";
        ClassPool cp = ClassPool.getDefault();
        cp.insertClassPath(path);
        CtClass cc = cp.get("bb");
        cc.addField(CtField.make("private long crack = System.currentTimeMillis();", cc));
        for (CtMethod cm : cc.getDeclaredMethods()) {
            String str = "{System.out.println(\"crack:" + Modifier.toString(cm.getModifiers()) + " " + cm.getName() + cm.getMethodInfo().getDescriptor() + ":\"+System.currentTimeMillis());}";
            cm.insertBefore(str);
            System.out.println(str);
            String methodInfo = cm.getMethodInfo().toString();
            System.out.println(methodInfo);
        }
        cc.writeFile();
    }

    public static void testCalculator() {
        Calculator c = new Calculator();
        c.appendNumber(3);
        c.inverse();
        c.operator('+');
        c.appendNumber(2);
        System.out.println(c.equal());
    }

    public static void testClient() throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://cnota.cn/g.jsp?18500_895");
        HttpResponse response = client.execute(request);
        System.out.println(response.getStatusLine());
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            System.out.println(EntityUtils.toString(entity));
        }
        request.abort();
        client.getConnectionManager().shutdown();
    }

    public static void testPost() throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost("http://192.168.1.139:8080/activate_notify/log?gid=1&mt=3");
        HttpEntity entity = new StringEntity("900");
        request.setEntity(entity);
        client.execute(request);
    }
}
