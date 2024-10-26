package yamaloo.HtmlProcessor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import yamaloo.HtmlProcessor.Config;

;

public class SinglePageProcessor {

    private static final String Default_URL = "http://item.tmall.com/item.htm?id=12405459484&prc=1";

    private static final String Default_File_Path = "Data\\Site\\Gap\\sites\\";

    public static ProductItem pro = new ProductItem();

    public static Config cfg = new Config();

    private static String[] desc = new String[] { "��Ʒ���", "����", "Ʒ��" };

    private static boolean saveFile(String filePath, String file) {
        try {
            FileWriter writer = new FileWriter(filePath, false);
            writer.write(file);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String setPagePro(String filePath) {
        String ret = "";
        File f = new File(filePath);
        try {
            Scanner sc = new Scanner(f);
            while (sc.hasNext()) {
                ret += sc.nextLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean excuteProcess(String filePath) {
        String pageStr = setPagePro(filePath);
        singlePageProcess(pageStr);
        getDetails(pageStr);
        if (!pro.isSet()) {
            System.err.println("page invalid!" + "\r\n" + "path : " + filePath);
            return false;
        }
        try {
            save2DB();
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void downLoadPic(String picurl, String hashcode) {
        URL url;
        BufferedInputStream in;
        FileOutputStream file;
        try {
            System.out.println("Start download picture!");
            String fileName = hashcode + ".jpg";
            String filePath = cfg.getBrandPath() + "pictures\\";
            url = new URL(picurl);
            in = new BufferedInputStream(url.openStream());
            file = new FileOutputStream(new File(filePath + fileName));
            int t;
            while ((t = in.read()) != -1) {
                file.write(t);
            }
            file.close();
            in.close();
            System.out.println("Download picture successed!\r\n" + "path : " + filePath + fileName);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("create filed failed! check the current path valid.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("get page picture failed...");
            e.printStackTrace();
        }
    }

    private static String setPage(String path, String charset, boolean isonline) {
        String ret = "";
        if (isonline) {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(path);
            get.getParams().setContentCharset(charset);
            try {
                client.executeMethod(get);
                ret = get.getResponseBodyAsString();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                get.releaseConnection();
            }
            saveFile(Default_File_Path + pro.getMD5(Default_URL) + ".html", ret);
            System.out.println(Default_File_Path + pro.getMD5(Default_URL) + ".html");
        } else {
            File f = new File(Default_File_Path + "sample.html");
            try {
                Scanner sc = new Scanner(f);
                while (sc.hasNext()) {
                    ret += sc.nextLine();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private static String getDetails(String page) {
        String ret = "";
        String tempStr = "";
        String regx = "";
        regx = "<li title=\"[^<li title=]*?\">([^</li>]+):[&nbsp;]*([\\w\\W]*?)</li>";
        Matcher m = Pattern.compile(regx).matcher(page);
        tempStr = "";
        while (m.find()) {
            tempStr = m.group(1);
            tempStr = Trim(tempStr);
            tempStr = m.group(2);
            tempStr = Trim(tempStr);
            ret = ret + tempStr + "\r\n";
        }
        pro.ProductDetail = ret;
        return ret;
    }

    private static void singlePageProcess(String responseString) {
        String tempStr = "";
        String regx = "";
        for (int i = 0; i < desc.length; i++) {
            regx = "<li title=\"[^<li title=]*?\">" + desc[i] + "[\\w\\W][&nbsp;]*([\\w\\W]*?)</li>";
            Matcher m = Pattern.compile(regx).matcher(responseString);
            tempStr = "";
            while (m.find()) {
                tempStr = m.group(1);
                tempStr = Trim(tempStr);
                if (i == 1) {
                    pro.SerialNumber = tempStr;
                } else if (i == 2) {
                    pro.Brand = tempStr;
                } else {
                    continue;
                }
            }
        }
        regx = "<strong id=\"J_StrPrice\" >([\\s\\S]*?)</strong>";
        Matcher m1 = Pattern.compile(regx).matcher(responseString);
        while (m1.find()) {
            tempStr = m1.group(1);
            tempStr = Trim(tempStr);
            pro.Price = Double.valueOf(tempStr).doubleValue();
        }
        regx = "<img id=\"J_ImgBooth\" src=\"(http://[\\s\\S]*?.jpg)\"";
        Matcher m2 = Pattern.compile(regx).matcher(responseString);
        while (m2.find()) {
            tempStr = m2.group(1);
            tempStr = Trim(tempStr);
            pro.PictureUrl = tempStr;
        }
        regx = "<input type=\"hidden\" name=\"title\" value=\"([\\s\\S]*?)\"";
        Matcher m3 = Pattern.compile(regx).matcher(responseString);
        while (m3.find()) {
            tempStr = m3.group(1);
            tempStr = Trim(tempStr);
            pro.ProductName = tempStr;
        }
        pro.ProductPageUrl = Default_URL;
    }

    private static String Trim(String s) {
        s = rightTrim(s);
        return leftTrim(s);
    }

    private static String rightTrim(String s) {
        if (s == null || s.trim().length() == 0) return null;
        if (s.trim().length() == s.length()) return s;
        if (!s.startsWith(" ")) {
            return s.trim();
        } else {
            return s.substring(0, s.indexOf(s.trim().substring(0, 1)) + s.trim().length());
        }
    }

    private static String leftTrim(String s) {
        if (s == null || s.trim().length() == 0) return null;
        if (s.trim().length() == s.length()) return s;
        if (!s.startsWith(" ")) {
            return s;
        } else {
            return s.substring(s.indexOf(s.trim().substring(0, 1)));
        }
    }

    private static void save2DB() throws Throwable {
        DBManager dm = new DBManager();
        dm.insertProductList(pro);
    }

    public static void main(String args[]) throws Throwable {
        String szPage = setPage(Default_URL, cfg.getCharSet(), true);
        singlePageProcess(szPage);
        getDetails(szPage);
        pro.showDetail();
        save2DB();
    }
}
