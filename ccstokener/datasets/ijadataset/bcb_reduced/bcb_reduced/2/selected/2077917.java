package net.sourceforge.epoint;

import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import net.sourceforge.epoint.pgp.*;
import net.sourceforge.epoint.util.*;
import net.sourceforge.epoint.io.*;

/**
 * Class representing an ePoint issuer
 * 
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 * 
 * @version 0.1 (no cooperation)
 */
public class Issuer {

    /**
     * Public key of the <code>Issuer<code>.
     * this is what carries his identity
     */
    private KeyBlock pubkey;

    /**
     * Base URL of the Issuer
     */
    public URL url;

    /**
     * Constructs <code>Issuer</code> from public key
     * @param pk public key block
     */
    public Issuer(KeyBlock pk) throws MalformedURLException {
        url = new URL(pk.getComment() + '/');
        pubkey = pk;
    }

    /**
     * Constructs <code>Issuer</code> from base-URL
     * @param base base-<code>URL</code>
     */
    public Issuer(URL base) throws MalformedURLException, IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        URLConnection keyURL = new URL(base, "pubkey").openConnection();
        InputStream keyStream;
        String type;
        keyURL.connect();
        type = keyURL.getContentType();
        if ((type.length() > 4) && type.substring(0, 4).equals("text")) keyStream = new ArmoredInputStream(new FastReader(keyURL.getInputStream()), true); else keyStream = keyURL.getInputStream();
        pubkey = new KeyBlock(keyStream);
        if (!chopURL(base).equals(pubkey.getComment())) throw new MalformedURLException("Base URL mismatch");
        url = base;
    }

    /**
     * obtain statistics from this issuer
     * @return stats
     */
    public IssuerStats getStats() throws MalformedURLException, IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        InputStream is = new URL(url, "info").openStream();
        IssuerStats s = new IssuerStats(is, this.getPublicKey());
        is.close();
        return s;
    }

    /**
     * validate an ePoint bill with this issuer
     * @param e the bill to validate
     * @return validated bill
     */
    public ValidEPoint validate(EPoint e) throws MalformedURLException, IOException, NoSuchAlgorithmException, InvalidEPointCertificateException, InvalidKeyException, SignatureException {
        InputStream is = new URL(url, "info?ID=" + Base16.encode(e.getMD())).openStream();
        ValidEPoint v = new ValidEPoint(this, e, is);
        is.close();
        return v;
    }

    /**
     * obtain obligation certificate from this issuer
     * @param id
     * @return certificate
     */
    public EPointCertificate getObligation(byte[] id) throws MalformedURLException, IOException, NoSuchAlgorithmException, InvalidEPointCertificateException, InvalidKeyException, SignatureException {
        return getCertificate(id, true);
    }

    /**
     * obtain rand certificate from this issuer
     * @param id
     * @return certificate
     */
    public EPointCertificate getCertificate(byte[] id) throws MalformedURLException, IOException, NoSuchAlgorithmException, InvalidEPointCertificateException, InvalidKeyException, SignatureException {
        return getCertificate(id, false);
    }

    /**
     * obtain certificate from this issuer
     * @param id
     * @param obligation <code>true</code> for obligations
     * @return certificate
     */
    private EPointCertificate getCertificate(byte[] id, boolean obligation) throws MalformedURLException, IOException, NoSuchAlgorithmException, InvalidEPointCertificateException, InvalidKeyException, SignatureException {
        InputStream is = new URL(url, (obligation ? "info?KEY=" : "info?ID=") + Base16.encode(id)).openStream();
        EPointCertificate v = new EPointCertificate(is, this.getPublicKey());
        is.close();
        if (Base16.encode(id).equals(Base16.encode(v.getID()))) return v;
        throw new InvalidEPointCertificateException("ID mismatch: " + Base16.encode(id) + "!=" + Base16.encode(v.getID()));
    }

    /**
     * obtain certificate from this issuer
     * @param sn serial number
     * @return certificate
     */
    public EPointCertificate getCertificate(long sn) throws MalformedURLException, IOException, NoSuchAlgorithmException, InvalidEPointCertificateException, InvalidKeyException, SignatureException {
        InputStream is = new URL(url, EPointCertificate.serial(sn)).openStream();
        EPointCertificate v = new EPointCertificate(is, this.getPublicKey());
        is.close();
        if (v.getSN() != sn) throw new InvalidEPointCertificateException("SN mismatch: " + sn + "!=" + v.getSN());
        return v;
    }

    /**
     * exchange an ePoint bill with this issuer
     * @param o old bill to exchange
     * @param e new bill to validate
     * @return validated bill
     */
    public ValidEPoint exchange(EPoint o, EPoint e) throws MalformedURLException, IOException, NoSuchAlgorithmException, InvalidEPointCertificateException, InvalidKeyException, SignatureException {
        URLConnection u = new URL(url, "action").openConnection();
        OutputStream os;
        InputStream is;
        u.setDoOutput(true);
        u.setDoInput(true);
        u.setAllowUserInteraction(false);
        ((HttpURLConnection) u).setInstanceFollowRedirects(false);
        os = u.getOutputStream();
        os.write(("B=" + URLEncoder.encode(o.toString(), "UTF-8") + "&D=" + Base16.encode(e.getMD())).getBytes());
        os.close();
        is = u.getInputStream();
        int res = ((HttpURLConnection) u).getResponseCode();
        if ((res >= 300) && (res < 400)) {
            String r = u.getHeaderField("Location");
            is.close();
            is = new URL(r).openStream();
        }
        ValidEPoint v = new ValidEPoint(this, e, is);
        is.close();
        return v;
    }

    /**
     * split an ePoint bill with this issuer
     * @param o old bill to exchange
     * @param e1 new bill to validate
     * @param e2 new bill to validate
     * @param v1 value of <code>e1</code>
     * @return array of 2 validated bills
     */
    public ValidEPoint[] split(EPoint o, EPoint e1, long v1, EPoint e2) throws MalformedURLException, IOException, NoSuchAlgorithmException, InvalidEPointCertificateException, InvalidKeyException, SignatureException {
        URLConnection u = new URL(url, "action").openConnection();
        OutputStream os;
        InputStream is;
        ValidEPoint[] v = new ValidEPoint[2];
        u.setDoOutput(true);
        u.setDoInput(true);
        u.setAllowUserInteraction(false);
        os = u.getOutputStream();
        os.write(("B=" + URLEncoder.encode(o.toString(), "UTF-8") + "&D=" + Base16.encode(e1.getMD()) + "&F=" + Long.toString(v1) + "&C=" + Base16.encode(e2.getMD())).getBytes());
        os.close();
        is = u.getInputStream();
        v[1] = new ValidEPoint(this, e2, is);
        is.close();
        v[0] = validate(e1);
        return v;
    }

    /**
     * @return Issuer key
     */
    public PublicKey getPublicKey() throws java.security.NoSuchAlgorithmException {
        return pubkey.getPublicKey();
    }

    /**
     * @return Issuer ID in human readable form
     */
    public String getIDText() {
        return pubkey.getKey().getFingerprintText();
    }

    /**
     * @return Issuer ID as a <code>byte</code> array
     */
    public byte[] getID() {
        return pubkey.getKey().getFingerprint();
    }

    /**
     * @return Issuer name
     */
    public String getName() {
        return pubkey.getName();
    }

    public static String chopURL(URL u) {
        String s = u.toString();
        while (s.charAt(s.length() - 1) == '/') s = s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * Write XML representation
     */
    public void writeXML(java.io.PrintWriter out) throws java.io.IOException {
        out.println("<issuer id=\"" + Base16.encode(getID()) + "\"");
        out.println("       url=\"" + chopURL(url) + "\">");
        ArmoredOutputStream p = new ArmoredOutputStream(out, Armor.PUBKEY);
        pubkey.write(p);
        p.close();
        out.println("</issuer>");
    }

    public int hashCode() {
        return new java.math.BigInteger(getID()).intValue();
    }

    public boolean equals(Object x) {
        if (!getClass().isInstance(x)) return false;
        return new java.math.BigInteger(getID()).equals(new java.math.BigInteger(((Issuer) x).getID()));
    }

    /**
    * for debugging purposes
    */
    public String toString() {
        return "(Issuer " + getIDText() + ")";
    }
}
