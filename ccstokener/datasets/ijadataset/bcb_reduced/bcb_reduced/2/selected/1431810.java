package com.hs.framework.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *  �˴������������� �������ڣ�(2003-2-20 18:16:15)
 *
 *@author     william
 *@created    2003��5��27�� @author��Administrator
 */
public final class IOToolKit {

    /**
     *  ʹ����������ϵͳʱ����ҳ�� URL
     */
    static java.net.URL urlDocumentBase;

    /**
     *  �û�����Ŀ¼
     */
    static String userDir;

    /**
     *  IOToolKit ������ע�⡣
     */
    public IOToolKit() {
        super();
    }

    /**
     *  Sets the UserDir attribute of the IOToolKit class
     */
    public static void setUserDir() {
        if (userDir == null) {
            userDir = System.getProperty("user.dir");
        }
        System.getProperty("user.dir");
    }

    /**
     *  ʹ����������ϵͳʱ�������ҳ��URL�����û�����Ŀ¼.
     *
     *@param  applet  The new UserDir value
     */
    public static void setUserDir(java.applet.Applet applet) {
        if (userDir == null) {
            try {
                java.net.URL url = applet.getDocumentBase();
                String path = url.getFile();
                int p = path.lastIndexOf('/');
                if (p >= 0) {
                    userDir = path.substring(0, p);
                } else {
                    userDir = "";
                }
                urlDocumentBase = new java.net.URL(url.getProtocol(), url.getHost(), url.getPort(), userDir);
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

    /**
     *  Gets the BufferedReadFormFile attribute of the IOToolKit class
     *
     *@param  fileName  Description of Parameter
     *@return           The BufferedReadFormFile value
     */
    public static BufferedReader getBufferedReadFormFile(String fileName) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(fileName));
        } catch (Exception ex) {
            System.out.println("���ļ�" + fileName + ex.getMessage());
        }
        return in;
    }

    /**
     *  Gets the TextFromFile attribute of the IOToolKit class
     *
     *@param  fileName                 Description of Parameter
     *@return                          The TextFromFile value
     *@exception  java.io.IOException  Description of Exception
     */
    public static final String getTextFromFile(String fileName) throws java.io.IOException {
        FileInputStream fis = new FileInputStream(fileName);
        try {
            return getTextFromInputStream(fis);
        } finally {
            try {
                fis.close();
            } catch (Throwable ex) {
            }
        }
    }

    /**
     *  Gets the TextFromInputStream attribute of the IOToolKit class
     *
     *@param  is                       Description of Parameter
     *@return                          The TextFromInputStream value
     *@exception  java.io.IOException  Description of Exception
     */
    public static final String getTextFromInputStream(InputStream is) throws java.io.IOException {
        if (is == null) {
            return null;
        }
        try {
            is.reset();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        java.io.InputStreamReader r = new java.io.InputStreamReader(is);
        BufferedInputStream read = new BufferedInputStream(is);
        StringBuffer buf = new StringBuffer();
        for (; ; ) {
            int c = r.read();
            if (c <= 0) {
                break;
            }
            buf.append((char) c);
        }
        return buf.toString();
    }

    /**
     *  װ����Դ�õ�һ���ı�.���� txt,properties ���ı���Դ���μ� <code>java.lang.Class.getSource()
     *  </code>�˽�����й���Դ�ĸ���.��Դ���·�������������mywork ��·����Ҳ������Դ mywork/res/text.txt Ӧ��
     *  ��ʾΪ res/text.txt . ���ӣ� <blockquote><pre>
     *
     *  String text = IoTookit.getTextFromResource("sndata/CreateDatabase0.txt");
     *  </pre</blockquote>
     *
     *@param  resource                 ����� mywork ����Դ��
     *@return                          ����Դ�õ����ı�
     *@exception  java.io.IOException  Description of Exception
     */
    public static final String getTextFromResource(String resource) throws java.io.IOException {
        return getTextFromInputStream(com.hs.framework.common.util.config.Config.class.getResourceAsStream(resource));
    }

    /**
     *  Gets the UrlInputStream attribute of the IOToolKit class
     *
     *@param  url              Description of Parameter
     *@return                  The UrlInputStream value
     *@exception  IOException  Description of Exception
     */
    public static InputStream getUrlInputStream(java.net.URL url) throws IOException {
        java.net.URLConnection conn = url.openConnection();
        return conn.getInputStream();
    }

    /**
     *  �����û�����Ŀ¼.
     *
     *@return    �û�����Ŀ¼
     */
    public static String getUserDir() {
        return userDir;
    }

    /**
     *  Gets the ZipInputStream attribute of the IOToolKit class
     *
     *@param  szZipFileName  Description of Parameter
     *@param  szEntryName    Description of Parameter
     *@return                The ZipInputStream value
     */
    public static final ZipInputStream getZipInputStream(String szZipFileName, String szEntryName) {
        boolean bFound = false;
        ZipInputStream ins = null;
        try {
            ins = new ZipInputStream(new FileInputStream(szZipFileName));
            for (; ; ) {
                ZipEntry entry = ins.getNextEntry();
                if (entry == null) {
                    break;
                }
                String szName = entry.getName();
                if (szName.equalsIgnoreCase(szEntryName)) {
                    bFound = true;
                    break;
                }
                ins.closeEntry();
            }
            if (!bFound) {
                ins.close();
                ins = null;
            }
        } catch (Exception ex) {
            ins = null;
        }
        return ins;
    }

    /**
     *  Description of the Method
     *
     *@param  fileName  Description of Parameter
     *@return           Description of the Returned Value
     */
    public static String ReadFile(String fileName) {
        String strlinetemp = "";
        String strwhole = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            while ((strlinetemp = in.readLine()) != null) {
                strwhole = strwhole + strlinetemp;
            }
            in.close();
        } catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }
        return strwhole;
    }

    /**
     *  Description of the Method
     *
     *@param  from                               Description of Parameter
     *@param  to                                 Description of Parameter
     *@return                                    Description of the Returned
     *      Value
     *@exception  java.io.FileNotFoundException  Description of Exception
     *@exception  java.io.IOException            Description of Exception
     */
    public static final int copyFile(String from, String to) throws java.io.FileNotFoundException, java.io.IOException {
        java.io.FileInputStream is = null;
        java.io.FileOutputStream os = null;
        int n = 0;
        try {
            is = new java.io.FileInputStream(from);
            os = new java.io.FileOutputStream(to);
            for (; ; ) {
                int c = is.read();
                if (c < 0) {
                    break;
                }
                os.write(c);
                n++;
            }
        } finally {
            try {
                is.close();
            } catch (Exception ex) {
            }
            try {
                os.close();
            } catch (Exception ex) {
            }
        }
        return n;
    }

    /**
     *  Description of the Method
     *
     *@param  in               Description of Parameter
     *@param  out              Description of Parameter
     *@exception  IOException  Description of Exception
     */
    public static final void copyStream(InputStream in, OutputStream out) throws IOException {
        int count;
        for (count = 0; ; count++) {
            int ch = in.read();
            if (ch == -1) {
                break;
            }
            out.write(ch);
        }
        in.close();
        out.flush();
        out.close();
    }

    /**
     *  Description of the Method
     *
     *@param  szDirectory   Description of Parameter
     *@param  szFileFilter  Description of Parameter
     *@return               Description of the Returned Value
     */
    public static final String[] enumFileNamesInDir(String szDirectory, String szFileFilter) {
        File file = new File(szDirectory);
        String names[] = file.list();
        if (names == null || szFileFilter == null) {
            return names;
        }
        java.util.ArrayList v = new java.util.ArrayList();
        for (int i = 0; i < names.length; i++) {
            if (Utilities.like(names[i], szFileFilter, true)) {
                v.add(names[i]);
            }
        }
        return (String[]) (v.toArray(new String[0]));
    }

    /**
     *  Description of the Method
     *
     *@param  args  Description of Parameter
     */
    public static void main(String args[]) {
        System.out.println(ManageFile.ReadFile("D:\\icpro\\icpro\\src\\zxt\\pub\\config\\test.properties"));
    }

    /**
     *  Description of the Method
     *
     *@param  dir  Description of Parameter
     *@return      Description of the Returned Value
     */
    public static final boolean makeDir(String dir) {
        File f = new File(dir);
        if (f.exists()) {
            return f.isDirectory();
        }
        if (f.mkdir()) {
            return true;
        }
        dir = f.getAbsolutePath();
        char separator = System.getProperty("file.separator").charAt(0);
        if (separator != '/') {
            dir = Utilities.replaceChar(dir, '/', separator);
        }
        if (separator != '\\') {
            dir = Utilities.replaceChar(dir, '\\', separator);
        }
        int p = dir.lastIndexOf(separator);
        if (p > 0 && !makeDir(dir.substring(0, p))) {
            return false;
        }
        return f.mkdir();
    }

    /**
     *  Description of the Method
     *
     *@param  fileName  Description of Parameter
     *@return           Description of the Returned Value
     */
    public static final boolean makeDirForFilename(String fileName) {
        File f = new File(fileName);
        fileName = f.getAbsolutePath();
        char separator = System.getProperty("file.separator").charAt(0);
        int p = fileName.lastIndexOf(separator);
        if (p >= 0) {
            return makeDir(fileName.substring(0, p));
        } else {
            return makeDir("");
        }
    }

    /**
     *  Description of the Method
     *
     *@param  fileName         Description of Parameter
     *@return                  Description of the Returned Value
     *@exception  IOException  Description of Exception
     */
    public static InputStream openUserDirFile(String fileName) throws IOException {
        if (urlDocumentBase != null) {
            java.net.URL url = new java.net.URL(urlDocumentBase.getProtocol(), urlDocumentBase.getHost(), urlDocumentBase.getPort(), urlDocumentBase.getFile() + '/' + fileName);
            return getUrlInputStream(url);
        } else {
            return new FileInputStream(Utilities.getUserDir() + '/' + fileName);
        }
    }

    /**
     *  ���������ж�ȡָ�����ȵ��ֽ�����
     *
     *@param  is      Description of Parameter
     *@param  length  Description of Parameter
     *@return         Description of the Returned Value
     */
    public static byte[] read(InputStream is, int length) {
        byte buffer[] = null;
        try {
            if (length <= 0) {
                return null;
            }
            buffer = new byte[length];
            BufferedInputStream in = new BufferedInputStream(is, 1024 * 1024);
            int iBytes = 0;
            int totalBytes = 0;
            while (totalBytes < length && iBytes != -1) {
                iBytes = in.read(buffer, totalBytes, length - totalBytes);
                if (iBytes != -1) {
                    totalBytes += iBytes;
                }
            }
            return buffer;
        } catch (java.io.IOException ex) {
            LogUtil.getLogger().error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     *  Description of the Method
     *
     *@param  ous              Description of Parameter
     *@param  ob               Description of Parameter
     *@exception  IOException  Description of Exception
     */
    public static final void writeDataObject(DataOutputStream ous, Object ob) throws IOException {
        Class objectDataTypes[] = { null, Boolean.class, Byte.class, Short.class, Integer.class, Long.class, BigInteger.class, String.class, Float.class, Double.class, BigDecimal.class, java.util.Date.class, java.sql.Date.class, java.sql.Timestamp.class, String.class, Character.class };
        int nType = 0;
        if (ob != null) {
            if (ob instanceof byte[]) {
                nType = 100;
            } else {
                for (nType = 1; nType < objectDataTypes.length; nType++) {
                    if (ob.getClass() == objectDataTypes[nType]) {
                        break;
                    }
                }
            }
        }
        if (nType == 7 && ((String) ob).length() > 65535) {
            nType = 14;
        }
        ous.writeByte(nType);
        if (ob == null) {
            return;
        }
        double dlValue = 0;
        String szText = null;
        switch(nType) {
            case 1:
                ous.writeBoolean(((Boolean) ob).booleanValue());
                break;
            case 2:
                ous.writeByte(((Number) ob).byteValue());
                break;
            case 3:
                ous.writeShort(((Number) ob).shortValue());
                break;
            case 4:
                ous.writeInt(((Number) ob).intValue());
                break;
            case 5:
                ous.writeLong(((Number) ob).longValue());
                break;
            case 6:
                ous.writeLong(((Number) ob).longValue());
                break;
            case 7:
                ous.writeUTF((String) ob);
                break;
            case 8:
                ous.writeFloat(((Float) ob).floatValue());
                break;
            case 9:
                ous.writeDouble(((Double) ob).doubleValue());
                break;
            case 10:
                {
                    java.math.BigDecimal x = (java.math.BigDecimal) ob;
                    int scale = x.scale();
                    byte bs[] = x.unscaledValue().toByteArray();
                    if (bs.length > 0xffff || scale > 0xffff) {
                        throw new RuntimeException();
                    }
                    ous.writeShort(bs.length);
                    ous.write(bs);
                    ous.writeShort(scale);
                }
                {
                    java.math.BigDecimal ob1 = (java.math.BigDecimal) ob;
                    int nScal = ob1.scale();
                    java.math.BigDecimal ob2 = ob1.movePointRight(nScal);
                    ous.writeLong(ob2.longValue());
                    ous.writeInt(ob1.scale());
                }
                break;
            case 11:
                ous.writeLong(((java.util.Date) ob).getTime());
                break;
            case 12:
                ous.writeLong(((java.sql.Date) ob).getTime());
                break;
            case 13:
                ous.writeLong(((java.sql.Timestamp) ob).getTime());
                break;
            case 14:
                {
                    byte bs[] = ((String) ob).getBytes();
                    ous.writeInt(bs.length);
                    ous.write(bs);
                }
                break;
            case 15:
                ous.writeChar(((Character) ob).charValue());
                break;
            case 100:
                {
                    ous.writeInt(((byte[]) ob).length);
                    ous.write((byte[]) ob);
                }
                break;
            default:
                throw new RuntimeException("writeDataObject:unknown type " + nType + "," + ob.getClass() + "," + ob);
        }
    }
}
