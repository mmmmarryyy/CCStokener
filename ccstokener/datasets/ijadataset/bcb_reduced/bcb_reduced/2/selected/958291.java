package org.openXpertya.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;

/**
 * Descripción de Clase
 *
 *
 * @version    2.2, 12.10.07
 * @author     Equipo de Desarrollo de openXpertya    
 */
public class MProductDownload extends X_M_ProductDownload {

    /**
     * Descripción de Método
     *
     *
     * @param ctx
     */
    public static void migrateDownloads(Properties ctx) {
        String sql = "SELECT COUNT(*) FROM M_ProductDownload";
        int no = DB.getSQLValue(null, sql);
        if (no > 0) {
            return;
        }
        int count = 0;
        sql = "SELECT AD_Client_ID, AD_Org_ID, M_Product_ID, Name, DownloadURL " + "FROM M_Product " + "WHERE DownloadURL IS NOT NULL";
        PreparedStatement pstmt = null;
        try {
            pstmt = DB.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int AD_Client_ID = rs.getInt(1);
                int AD_Org_ID = rs.getInt(2);
                int M_Product_ID = rs.getInt(3);
                String Name = rs.getString(4);
                String DownloadURL = rs.getString(5);
                MProductDownload pdl = new MProductDownload(ctx, 0, null);
                pdl.setClientOrg(AD_Client_ID, AD_Org_ID);
                pdl.setM_Product_ID(M_Product_ID);
                pdl.setName(Name);
                pdl.setDownloadURL(DownloadURL);
                if (pdl.save()) {
                    count++;
                    String sqlUpdate = "UPDATE M_Product SET DownloadURL = NULL WHERE M_Product_ID=" + M_Product_ID;
                    int updated = DB.executeUpdate(sqlUpdate);
                    if (updated != 1) {
                        s_log.warning("Product not updated");
                    }
                } else {
                    s_log.warning("Product Download not created M_Product_ID=" + M_Product_ID);
                }
            }
            rs.close();
            pstmt.close();
            pstmt = null;
        } catch (Exception e) {
            s_log.log(Level.SEVERE, sql, e);
        }
        try {
            if (pstmt != null) {
                pstmt.close();
            }
            pstmt = null;
        } catch (Exception e) {
            pstmt = null;
        }
        s_log.info("#" + count);
    }

    /** Descripción de Campos */
    private static CLogger s_log = CLogger.getCLogger(MProductDownload.class);

    /**
     * Constructor de la clase ...
     *
     *
     * @param ctx
     * @param M_ProductDownload_ID
     * @param trxName
     */
    public MProductDownload(Properties ctx, int M_ProductDownload_ID, String trxName) {
        super(ctx, M_ProductDownload_ID, trxName);
        if (M_ProductDownload_ID == 0) {
        }
    }

    /**
     * Constructor de la clase ...
     *
     *
     * @param ctx
     * @param rs
     * @param trxName
     */
    public MProductDownload(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    /**
     * Descripción de Método
     *
     *
     * @return
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("MProductDownload[").append(getID()).append(",M_Product_ID=").append(getM_Product_ID()).append(",").append(getDownloadURL()).append("]");
        return sb.toString();
    }

    /**
     * Descripción de Método
     *
     *
     * @return
     */
    public String getDownloadName() {
        String url = getDownloadURL();
        if ((url == null) || !isActive()) {
            return null;
        }
        int pos = Math.max(url.lastIndexOf('/'), url.lastIndexOf('\\'));
        if (pos != -1) {
            return url.substring(pos + 1);
        }
        return url;
    }

    /**
     * Descripción de Método
     *
     *
     * @param directory
     *
     * @return
     */
    public URL getDownloadURL(String directory) {
        String dl_url = getDownloadURL();
        if ((dl_url == null) || !isActive()) {
            return null;
        }
        URL url = null;
        try {
            if (dl_url.indexOf("://") != -1) {
                url = new URL(dl_url);
            } else {
                File f = getDownloadFile(directory);
                if (f != null) {
                    url = f.toURI().toURL();
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, dl_url, ex);
            return null;
        }
        return url;
    }

    /**
     * Descripción de Método
     *
     *
     * @param directory
     *
     * @return
     */
    public File getDownloadFile(String directory) {
        File file = new File(getDownloadURL());
        if (file.exists()) {
            return file;
        }
        if ((directory == null) || (directory.length() == 0)) {
            log.log(Level.SEVERE, "Not found " + getDownloadURL());
            return null;
        }
        String downloadURL2 = directory;
        if (!downloadURL2.endsWith(File.separator)) {
            downloadURL2 += File.separator;
        }
        downloadURL2 += getDownloadURL();
        file = new File(downloadURL2);
        if (file.exists()) {
            return file;
        }
        log.log(Level.SEVERE, "Not found " + getDownloadURL() + " + " + downloadURL2);
        return null;
    }

    /**
     * Descripción de Método
     *
     *
     * @param directory
     *
     * @return
     */
    public InputStream getDownloadStream(String directory) {
        String dl_url = getDownloadURL();
        if ((dl_url == null) || !isActive()) {
            return null;
        }
        InputStream in = null;
        try {
            if (dl_url.indexOf("://") != -1) {
                URL url = new URL(dl_url);
                in = url.openStream();
            } else {
                File file = getDownloadFile(directory);
                if (file == null) {
                    return null;
                }
                in = new FileInputStream(file);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, dl_url, ex);
            return null;
        }
        return in;
    }
}
