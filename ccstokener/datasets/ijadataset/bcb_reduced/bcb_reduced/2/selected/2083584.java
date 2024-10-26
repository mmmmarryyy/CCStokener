package uk.ac.dl.dp.coreutil.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import org.apache.log4j.Logger;
import org.globus.gsi.*;
import org.globus.gsi.gssapi.*;
import java.security.cert.CertificateException;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.*;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import uk.ac.dl.dp.coreutil.entity.Session;
import uk.ac.dl.dp.coreutil.entity.User;

/**
 *
 * @author gjd37
 */
public class Certificate {

    private String certificate;

    private GSSCredential credential;

    static Logger log = Logger.getLogger(Certificate.class);

    public Certificate(String certificate) throws CertificateException {
        this.certificate = certificate;
        loadCredential(this.certificate);
    }

    public Certificate(GSSCredential credential) throws CertificateException {
        log.debug("new Certificate(credential)");
        try {
            this.certificate = turnintoString(credential);
        } catch (IOException ioe) {
            throw new CertificateException("Unable to read in credential: " + ioe.getMessage(), ioe);
        } catch (GSSException ex) {
            throw new CertificateException("Unable to turn credential into string: " + ex.getMessage(), ex);
        }
        this.credential = credential;
    }

    /** Creates a new instance of certificate from a string */
    public Certificate(java.io.InputStream certificate) throws CertificateException, CertificateExpiredException {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(certificate));
            String inputLine;
            StringBuffer cert = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                cert.append(inputLine);
                cert.append("\n");
            }
            in.close();
            this.certificate = cert.toString();
        } catch (IOException ex) {
            throw new CertificateException("Unable to read in credential: " + ex.getMessage(), ex);
        }
        loadCredential(this.certificate);
    }

    /** Creates a new instance of certificate from a file */
    public Certificate(java.io.File certificate) throws CertificateException, CertificateExpiredException {
        try {
            BufferedReader in = new BufferedReader(new java.io.FileReader(certificate));
            String inputLine;
            StringBuffer cert = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                cert.append(inputLine);
                cert.append("\n");
            }
            in.close();
            this.certificate = cert.toString();
        } catch (FileNotFoundException ex) {
            throw new CertificateException("Unable to find in credential: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new CertificateException("Unable to read in credential: " + ex.getMessage(), ex);
        }
        loadCredential(this.certificate);
    }

    public Certificate(URL url) throws CertificateException {
        try {
            URLConnection con = url.openConnection();
            InputStream in2 = con.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(in2));
            String inputLine;
            StringBuffer cert = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                cert.append(inputLine);
                cert.append("\n");
            }
            in.close();
            this.certificate = cert.toString();
        } catch (IOException ex) {
            throw new CertificateException("Unable to read in credential: " + ex.getMessage(), ex);
        }
        loadCredential(this.certificate);
    }

    private void loadCredential(String cred) throws CertificateException, CertificateExpiredException {
        try {
            byte[] data = cred.getBytes();
            ExtendedGSSManager manager = (ExtendedGSSManager) ExtendedGSSManager.getInstance();
            credential = (GSSCredential) manager.createCredential(data, ExtendedGSSCredential.IMPEXP_OPAQUE, GSSCredential.DEFAULT_LIFETIME, null, GSSCredential.INITIATE_AND_ACCEPT);
        } catch (GSSException ex) {
            throw new CertificateException("Unable to load credential: " + ex.getMessage(), ex);
        }
        int lifetime = 0;
        try {
            lifetime = credential.getRemainingLifetime();
        } catch (GSSException ex) {
        }
        if (lifetime < 60 * 2) throw new CertificateExpiredException("Credential for " + getDn() + " has expired");
    }

    public long getLifetime() throws CertificateException {
        try {
            long lifetimeLeft = credential.getRemainingLifetime();
            return lifetimeLeft;
        } catch (GSSException ex) {
            throw new CertificateException("Unable to get remaining lifetime: " + ex.getMessage(), ex);
        }
    }

    public boolean isLifetimeLeft() throws CertificateException {
        boolean result = false;
        log.trace("Timeleft " + getLifetime());
        if (getLifetime() > 60 * 10) {
            result = true;
        }
        return result;
    }

    public String getDn() throws CertificateException {
        try {
            String DN = credential.getName().toString();
            return DN;
        } catch (GSSException ex) {
            throw new CertificateException("Unable to get DN: " + ex.getMessage(), ex);
        }
    }

    public String getStringRepresentation() {
        return this.certificate;
    }

    public String toString() {
        try {
            return getDn() + " has lifetime " + getLifetime() + " seconds";
        } catch (Exception ex) {
            return "UNKNOWN has lifetime UNKNOWN seconds";
        }
    }

    public GSSCredential getCredential() {
        return credential;
    }

    private static void prt(Certificate c) throws Exception {
        System.out.println("Certificate: " + c.toString());
        System.out.println("Lifetime: " + c.getLifetime());
        System.out.println("Any life left? " + c.isLifetimeLeft());
        System.out.println("DN: " + c.getDn());
    }

    private static synchronized String turnintoString(GSSCredential cred) throws IOException, GSSException {
        log.debug("turning credential into string");
        ExtendedGSSCredential extendcred = (ExtendedGSSCredential) cred;
        byte[] data = extendcred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
        File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "globuscred.txt");
        FileOutputStream out = new FileOutputStream(file);
        out.write(data);
        out.close();
        URL url1 = new URL("file:///" + file.getAbsolutePath());
        URLConnection con = url1.openConnection();
        InputStream in2 = con.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(in2));
        String inputLine;
        StringBuffer cert = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            cert.append(inputLine);
            cert.append("\n");
        }
        in.close();
        in2.close();
        out.close();
        log.trace("Deleted file: " + file.getAbsolutePath() + " ? " + file.delete());
        return cert.toString();
    }

    public static void main(String args[]) throws Exception {
        Certificate cert;
        cert = new Certificate(new URL("file:///E:/cog-1.1/build/cog-1.1/bin/x509up_36855.pem"));
        prt(cert);
        cert = new Certificate(new URL("file:///E:/cog-1.1/build/cog-1.1/bin/x509up_47677.pem"));
        prt(cert);
    }
}
