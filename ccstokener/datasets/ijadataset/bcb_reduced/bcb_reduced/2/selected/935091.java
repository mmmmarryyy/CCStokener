package com.ravi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Utils {

    static {
        if (System.getProperty("TTOOL_HOME") == null) {
            System.setProperty("TTOOL_HOME", System.getProperty("user.dir"));
        }
    }

    public static String PC_ID = System.getProperty("user.name") + "-" + System.getProperty("os.name") + "-";

    private static ApplicationContext spring;

    public static ApplicationContext getSpringContext() {
        if (spring == null) spring = new ClassPathXmlApplicationContext(new String[] { "file:" + System.getProperty("TTOOL_HOME") + File.separator + "conf" + File.separator + "spring_camel.xml" });
        return spring;
    }

    public static String getMQAddress() {
        String url = ((ActiveMQConnectionFactory) getSpringContext().getBean("jmsConnectionFactory")).getBrokerURL();
        return url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(":"));
    }

    public static int getMQAddrPort() throws Exception {
        String url = ((ActiveMQConnectionFactory) getSpringContext().getBean("jmsConnectionFactory")).getBrokerURL();
        String port = url.substring(url.lastIndexOf(":") + 1);
        return Integer.parseInt(port);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===========" + Utils.getMQAddress());
    }

    public static String getSystemInfo(String type) {
        try {
            if (System.getProperty(type) != null) {
                return System.getProperty(type);
            }
            InetAddress i = InetAddress.getLocalHost();
            if (type.equalsIgnoreCase("ip")) {
                Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
                while (nics.hasMoreElements()) {
                    NetworkInterface nic = nics.nextElement();
                    if (nic.isUp() && !nic.isLoopback() && !nic.isVirtual() && !nic.isPointToPoint()) {
                        Enumeration<InetAddress> addrs = nic.getInetAddresses();
                        while (addrs.hasMoreElements()) {
                            InetAddress addr = addrs.nextElement();
                            if (addr instanceof Inet4Address) return addr.getHostAddress();
                        }
                    }
                }
                return i.getHostName();
            }
            if (i.getHostName() != null && i.getHostName().length() > 0) return i.getHostName();
            return i.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isEmailAURI(String uri) {
        return true;
    }

    static void load_jars(File file) throws Exception {
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        addURL.setAccessible(true);
        ClassLoader sysloader = Thread.currentThread().getContextClassLoader();
        if (file.isFile() && file.getName().endsWith(".jar")) {
            try {
                addURL.invoke(sysloader, new Object[] { file.toURI().toURL() });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static InputStream getInputStream(String filepath) throws Exception {
        if (isUrl(filepath)) {
            URL url = URI.create(filepath).toURL();
            return url.openStream();
        } else {
            return new FileInputStream(new File(filepath));
        }
    }

    public static boolean isUrl(String path) {
        return path.startsWith("http:") || path.startsWith("ftp:");
    }

    public static String trimEnd(String s) {
        int len = s.length();
        byte[] val = s.getBytes();
        while ((len > 0) && (val[len - 1] <= ' ')) {
            len--;
        }
        return s.substring(0, len);
    }

    /**
	 * Performs a busy wait.
	 * 
	 * @param duration
	 *            The number of milli-seconds to wait.
	 */
    public static final void busyWait(long duration) {
        long time = System.currentTimeMillis();
        long stop = time + duration;
        while (time <= stop) {
            time = System.currentTimeMillis();
        }
    }

    public static ArrayList<ArrayList<String>> expandDatas(Map.Entry<String, Object> entry) {
        ArrayList<ArrayList<String>> al = new ArrayList<ArrayList<String>>();
        Object o = entry.getValue();
        if (o == null || o instanceof String) {
            ArrayList<String> tmp = new ArrayList<String>();
            tmp.add(entry.getKey());
            tmp.add((String) o);
            tmp.add("0");
            al.add(tmp);
        } else if (o instanceof ArrayList) {
            ArrayList<String> v = (ArrayList<String>) o;
            for (int i = 0; i < v.size(); i++) {
                ArrayList<String> tmp = new ArrayList<String>();
                tmp.add(entry.getKey());
                tmp.add((String) v.get(i));
                tmp.add("0");
                al.add(tmp);
            }
        } else if (o instanceof HashMap) {
            HashMap<String, String> hm = (HashMap<String, String>) o;
            Iterator it = hm.keySet().iterator();
            while (it.hasNext()) {
                ArrayList<String> tmp = new ArrayList<String>();
                tmp.add(entry.getKey());
                String value = (String) it.next();
                tmp.add(value);
                Object rst = hm.get(value);
                if (rst == null || rst.toString().length() == 0) {
                    tmp.add("0");
                } else {
                    if (rst.toString().equals("&skip")) {
                        continue;
                    }
                    tmp.add(rst.toString());
                }
                al.add(tmp);
            }
        }
        return al;
    }

    public static String getExceptionDesc(Exception e) {
        StringBuffer sb = new StringBuffer();
        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < trace.length; i++) sb.append("\tat " + trace[i]);
        Throwable ourCause = e.getCause();
        if (ourCause != null) sb.append("\tat " + ourCause);
        return sb.toString();
    }
}
