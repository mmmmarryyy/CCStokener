package portablepgp.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JDKAlgorithmParameterGenerator;
import org.bouncycastle.jce.spec.ElGamalParameterSpec;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.jdesktop.application.ResourceMap;
import portablepgp.PortablePGPApp;
import portablepgp.PortablePGPView;

/**
 * @author Primiano Tucci - http://www.primianotucci.com/
 * @author Garfield Geng, garfield.geng@gmail.com
 * @version $Revision: 1.12 $ $Date: 2011/07/02 06:49:41 $
 */
public class PGPUtils {

    private static final int BUFFER_SIZE = Character.MAX_VALUE;

    public static final String PROVIDER = "BC";

    public static final String UTF8 = CharEncoding.UTF_8;

    public static final String EMPTY = StringUtils.EMPTY;

    public static final String BS = " ";

    public static final String SIG = ".sig";

    private static final HashMap<String, String> entities = new HashMap<String, String>();

    private static final Pattern pattern = Pattern.compile("&([a-z]+);");

    public static final String ElGamal = JDKAlgorithmParameterGenerator.ElGamal.class.getSimpleName();

    public static final String DSA = JDKAlgorithmParameterGenerator.DSA.class.getSimpleName();

    public static String Error = null;

    public static String Ok = null;

    public static String Warning = null;

    public static final String PRIVATE_KEYRING_FILE = "private.bpg";

    public static final String PUBLIC_KEYRING_FILE = "public.bpg";

    public static File PubringFile = null;

    public static File SecringFile = null;

    private static final int KEY_ENCRYPTION_ALGO = PGPEncryptedData.AES_256;

    private static PGPPublicKeyRingCollection pubring;

    private static PGPSecretKeyRingCollection secring;

    public static ResourceMap resMap;

    static {
        entities.put("gt", ">");
        entities.put("amp", "&");
        entities.put("apos", ">");
        entities.put("quot", "\"");
        entities.put("lt", "<");
        Security.addProvider(new BouncyCastleProvider());
        resMap = PortablePGPApp.getApplication().getContext().getResourceMap(PortablePGPView.class);
    }

    public static String encryptText(String plainText, Collection<PGPPublicKey> enckeys, PGPSecretKey signWithKey, char[] signKeyPass) throws Exception {
        File tmpFile = File.createTempFile("enc", ".asc");
        FileOutputStream plain_out = new FileOutputStream(tmpFile);
        plain_out.write(plainText.getBytes(UTF8));
        IOUtils.closeQuietly(plain_out);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        encryptFile(bOut, tmpFile.getAbsolutePath(), enckeys, signWithKey, true, true, signKeyPass);
        tmpFile.delete();
        return new String(bOut.toByteArray(), UTF8);
    }

    @SuppressWarnings("rawtypes")
    public static void encryptFile(OutputStream out, String fileName, Collection<PGPPublicKey> encKeys, PGPSecretKey signWithKey, boolean armor, boolean withIntegrityCheck, char[] signKeyPass) throws Exception {
        PGPSignatureGenerator signatureGenerator = null;
        if (armor) {
            out = new ArmoredOutputStream(out);
        }
        PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(PGPEncryptedData.CAST5, withIntegrityCheck, new SecureRandom(), PROVIDER);
        for (PGPPublicKey encKey : encKeys) {
            encryptedDataGenerator.addMethod(encKey);
        }
        OutputStream encryptedOut = encryptedDataGenerator.open(out, new byte[BUFFER_SIZE]);
        PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
        OutputStream compressedOut = compressedDataGenerator.open(encryptedOut);
        if (signWithKey != null) {
            PGPPublicKey pubSigKey = signWithKey.getPublicKey();
            PGPPrivateKey secretKey = signWithKey.extractPrivateKey(signKeyPass, PROVIDER);
            signatureGenerator = new PGPSignatureGenerator(pubSigKey.getAlgorithm(), PGPUtil.SHA1, PROVIDER);
            signatureGenerator.initSign(PGPSignature.BINARY_DOCUMENT, secretKey);
            Iterator it = pubSigKey.getUserIDs();
            if (it.hasNext()) {
                PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
                spGen.setSignerUserID(false, (String) it.next());
                signatureGenerator.setHashedSubpackets(spGen.generate());
            }
            signatureGenerator.generateOnePassVersion(false).encode(compressedOut);
        }
        PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
        OutputStream literalOut = literalDataGenerator.open(compressedOut, PGPLiteralData.BINARY, fileName, new Date(), new byte[BUFFER_SIZE]);
        FileInputStream inputFileStream = new FileInputStream(fileName);
        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        while ((len = inputFileStream.read(buf)) > 0) {
            literalOut.write(buf, 0, len);
            if (signatureGenerator != null) {
                signatureGenerator.update(buf, 0, len);
            }
        }
        IOUtils.closeQuietly(literalOut);
        literalDataGenerator.close();
        if (signatureGenerator != null) {
            signatureGenerator.generate().encode(compressedOut);
        }
        IOUtils.closeQuietly(inputFileStream);
        IOUtils.closeQuietly(compressedOut);
        compressedDataGenerator.close();
        IOUtils.closeQuietly(encryptedOut);
        encryptedDataGenerator.close();
        IOUtils.closeQuietly(out);
    }

    @Deprecated
    public static String decodeHTML(String iText) {
        StringBuilder sb = new StringBuilder(256);
        Matcher m = pattern.matcher(iText);
        int index = 0;
        while (m.find()) {
            sb.append(iText.substring(index, m.start(0)));
            String item = m.group(1);
            if (entities.containsKey(item)) {
                sb.append(entities.get(item));
            } else {
                sb.append(m.group(0));
            }
            index = m.end(0);
        }
        sb.append(iText.substring(index, iText.length()));
        return sb.toString();
    }

    @Deprecated
    public static Collection<SearchKeyResult> searchKey(String iText, String iKeyServer) throws Exception {
        List<SearchKeyResult> outVec = new ArrayList<SearchKeyResult>();
        String uri = iKeyServer + "/pks/lookup?search=" + URLEncoder.encode(iText, UTF8);
        URL url = new URL(uri);
        BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
        Pattern regex = Pattern.compile("pub.*?<a\\s+href\\s*=\"(.*?)\".*?>\\s*(\\w+)\\s*</a>.*?(\\d+-\\d+-\\d+).*?<a\\s+href\\s*=\".*?\".*?>\\s*(.+?)\\s*</a>", Pattern.CANON_EQ);
        String line;
        while ((line = input.readLine()) != null) {
            Matcher regexMatcher = regex.matcher(line);
            while (regexMatcher.find()) {
                String id = regexMatcher.group(2);
                String downUrl = iKeyServer + regexMatcher.group(1);
                String downDate = regexMatcher.group(3);
                String name = decodeHTML(regexMatcher.group(4));
                outVec.add(new SearchKeyResult(id, name, downDate, downUrl));
            }
        }
        IOUtils.closeQuietly(input);
        return outVec;
    }

    @Deprecated
    public static PGPPublicKeyRing importRemoteKey(String iUrl) throws Exception {
        return null;
    }

    public static DecryptionResult decryptText(String cipherText, char[] passwd) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(cipherText.getBytes(UTF8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DecryptionResult res = decryptFile(in, passwd, out);
        res.setDecryptedText(out.toString(UTF8));
        return res;
    }

    @SuppressWarnings("rawtypes")
    public static DecryptionResult decryptFile(InputStream in, char[] passwd, OutputStream out) throws Exception {
        DecryptionResult decryptionRes = new DecryptionResult();
        String outFileName = EMPTY;
        PGPPublicKeyEncryptedData pbe = null;
        in = PGPUtil.getDecoderStream(in);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();
        if (o == null) {
            throw new Exception(resMap.getString("Cannot.recognize.input.data.format"));
        }
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }
        Iterator encObjects = enc.getEncryptedDataObjects();
        if (!encObjects.hasNext()) {
            throw new Exception(resMap.getString("No.encrypted.data"));
        }
        PGPPrivateKey sKey = null;
        PGPSecretKey secretKey = null;
        while (encObjects.hasNext()) {
            Object obj = encObjects.next();
            if (!(obj instanceof PGPPublicKeyEncryptedData)) {
                continue;
            }
            PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;
            secretKey = getPrivateKeyByID(encData.getKeyID());
            if (secretKey == null) {
                continue;
            }
            try {
                sKey = secretKey.extractPrivateKey(passwd, PROVIDER);
            } catch (Exception ex) {
                sKey = null;
            }
            if (sKey != null) {
                pbe = encData;
                break;
            }
        }
        if (sKey == null) {
            throw new IllegalArgumentException(resMap.getString("Cannot.extract.a.suitable.private.key.to.decrypt.the.message.Verify.the.passphrase.and.try.again"));
        }
        InputStream clear = pbe.getDataStream(sKey, PROVIDER);
        PGPObjectFactory plainFact = new PGPObjectFactory(clear);
        Object message = plainFact.nextObject();
        Object sigLiteralData = null;
        PGPObjectFactory pgpFact = null;
        if (message instanceof PGPCompressedData) {
            PGPCompressedData cData = (PGPCompressedData) message;
            pgpFact = new PGPObjectFactory(cData.getDataStream());
            message = pgpFact.nextObject();
            if (message instanceof PGPOnePassSignatureList) {
                sigLiteralData = pgpFact.nextObject();
            }
        }
        if (message instanceof PGPLiteralData) {
            outFileName = processLiteralData((PGPLiteralData) message, out, null);
        } else if (message instanceof PGPOnePassSignatureList) {
            decryptionRes.setIsSigned(true);
            PGPSignatureWrapper sigWrap = new PGPSignatureWrapper(((PGPOnePassSignatureList) message).get(0));
            PGPPublicKey pubKey = getPublicKeyByID(sigWrap.getKeyID());
            if (pubKey == null) {
                decryptionRes.setSignatureException(new Exception(resMap.getString("Cannot.find.the.public.key.[0x%s].in.the.pubring", PGPUtils.keyId2Hex(sigWrap.getKeyID()))));
                outFileName = processLiteralData((PGPLiteralData) sigLiteralData, out, null);
            } else {
                decryptionRes.setSignee(new PrintablePGPPublicKey(pubKey));
                sigWrap.initVerify(pubKey, PROVIDER);
                outFileName = processLiteralData((PGPLiteralData) sigLiteralData, out, sigWrap);
                PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
                decryptionRes.setIsSignatureValid(sigWrap.verify(sigList.get(0)));
            }
        } else if (message instanceof PGPSignatureList) {
            decryptionRes.setIsSigned(true);
            PGPSignatureWrapper sigWrap = new PGPSignatureWrapper(((PGPSignatureList) message).get(0));
            PGPPublicKey pubKey = getPublicKeyByID(sigWrap.getKeyID());
            if (pubKey == null) {
                decryptionRes.setSignatureException(new Exception(resMap.getString("Cannot.find.the.public.key.[0x%s].in.the.pubring", PGPUtils.keyId2Hex(sigWrap.getKeyID()))));
                outFileName = processLiteralData((PGPLiteralData) sigLiteralData, out, null);
            } else {
                decryptionRes.setSignee(new PrintablePGPPublicKey(pubKey));
                sigWrap.initVerify(pubKey, PROVIDER);
                sigLiteralData = (PGPLiteralData) pgpFact.nextObject();
                outFileName = processLiteralData((PGPLiteralData) sigLiteralData, out, sigWrap);
                decryptionRes.setIsSignatureValid(sigWrap.verify(null));
            }
        } else {
            throw new PGPException(resMap.getString("message.is.not.a.simple.encrypted.file.-.type.unknown.\n(%s)", message.getClass()));
        }
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        if (pbe.isIntegrityProtected()) {
            if (!pbe.verify()) {
                throw new Exception(resMap.getString("Message.failed.integrity.check"));
            }
        }
        decryptionRes.setDecryptFileName(outFileName);
        return decryptionRes;
    }

    private static String processLiteralData(PGPLiteralData ld, OutputStream out, PGPSignatureWrapper sig) throws IOException, SignatureException {
        String outFileName = ld.getFileName();
        InputStream unc = ld.getInputStream();
        int ch;
        if (sig == null) {
            while ((ch = unc.read()) >= 0) {
                out.write(ch);
            }
        } else {
            while ((ch = unc.read()) >= 0) {
                out.write(ch);
                sig.update((byte) ch);
            }
        }
        return outFileName;
    }

    public static String signText(String plainText, PGPSecretKey enckey, char[] pass) throws Exception {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ArmoredOutputStream armOut = new ArmoredOutputStream(bOut);
        byte[] bs = plainText.getBytes(UTF8);
        ByteArrayInputStream bIn = new ByteArrayInputStream(bs);
        armOut.beginClearText(PGPUtil.SHA1);
        armOut.write(bs);
        armOut.write(IOUtils.LINE_SEPARATOR_WINDOWS.getBytes(UTF8));
        armOut.endClearText();
        signFile(bIn, armOut, enckey, pass, true);
        IOUtils.closeQuietly(armOut);
        IOUtils.closeQuietly(bIn);
        String result = new String(bOut.toByteArray(), UTF8);
        System.out.println("result.length()= " + result.length());
        return result;
    }

    @SuppressWarnings("rawtypes")
    public static void signFile(InputStream in, OutputStream out, PGPSecretKey key, char[] pass, boolean textmode) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, PGPException, SignatureException {
        PGPPrivateKey priK = key.extractPrivateKey(pass, PROVIDER);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(key.getPublicKey().getAlgorithm(), PGPUtil.SHA1, PROVIDER);
        if (textmode) {
            sGen.initSign(PGPSignature.CANONICAL_TEXT_DOCUMENT, priK);
        } else {
            sGen.initSign(PGPSignature.BINARY_DOCUMENT, priK);
        }
        Iterator it = key.getPublicKey().getUserIDs();
        if (it.hasNext()) {
            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, (String) it.next());
            sGen.setHashedSubpackets(spGen.generate());
        }
        BCPGOutputStream bOut = new BCPGOutputStream(out);
        int rSize = 0;
        byte[] buf = new byte[BUFFER_SIZE];
        while ((rSize = in.read(buf)) >= 0) {
            sGen.update(buf, 0, rSize);
        }
        PGPSignature sig = sGen.generate();
        sig.encode(bOut);
    }

    public static String verifyText(String plainText) throws Exception {
        System.out.println("plainText= " + plainText);
        final String template = "-----BEGIN PGP SIGNED MESSAGE-----\\r?\\n.*?\\r?\\n\\r?\\n(.*)\\r?\\n(-----BEGIN PGP SIGNATURE-----\\r?\\n.*-----END PGP SIGNATURE-----)";
        Pattern regex = Pattern.compile(template, Pattern.CANON_EQ | Pattern.DOTALL);
        Matcher matcher = regex.matcher(plainText);
        if (matcher.find()) {
            String dataText = StringUtils.chomp(matcher.group(1));
            String signText = matcher.group(2);
            ByteArrayInputStream dataIn = new ByteArrayInputStream(dataText.getBytes(UTF8));
            ByteArrayInputStream signIn = new ByteArrayInputStream(signText.getBytes(UTF8));
            return verifyFile(dataIn, signIn);
        }
        throw new Exception(resMap.getString("Cannot.recognize.input.data.format"));
    }

    public static String verifyFile(InputStream dataIn, InputStream signIn) throws Exception {
        signIn = PGPUtil.getDecoderStream(signIn);
        PGPObjectFactory pgpFact = new PGPObjectFactory(signIn);
        PGPSignatureList p3 = null;
        Object o;
        try {
            o = pgpFact.nextObject();
            if (o == null) {
                throw new Exception();
            }
        } catch (Exception ex) {
            throw new Exception(resMap.getString("Invalid.input.data") + BS + ex);
        }
        if (o instanceof PGPCompressedData) {
            PGPCompressedData c1 = (PGPCompressedData) o;
            pgpFact = new PGPObjectFactory(c1.getDataStream());
            p3 = (PGPSignatureList) pgpFact.nextObject();
        } else {
            p3 = (PGPSignatureList) o;
        }
        int ch;
        PGPSignature sig = p3.get(0);
        PGPPublicKey key = getPublicKeyByID(sig.getKeyID());
        if (key == null) {
            throw new Exception(resMap.getString("Cannot.find.key.0x%s.in.the.pubring", PGPUtils.keyId2Hex(sig.getKeyID()).toUpperCase()));
        }
        sig.initVerify(key, PROVIDER);
        while ((ch = dataIn.read()) >= 0) {
            System.out.print((char) ch);
            sig.update((byte) ch);
        }
        IOUtils.closeQuietly(dataIn);
        IOUtils.closeQuietly(signIn);
        if (sig.verify()) {
            return new PrintablePGPPublicKey(key).toString();
        } else {
            return null;
        }
    }

    public static String formatLineBreak(String text) {
        return text.replace(IOUtils.LINE_SEPARATOR_UNIX, IOUtils.LINE_SEPARATOR_WINDOWS);
    }

    public static void loadKeyrings(String pubringFileName, String secringFileName) {
        Error = resMap.getString("Error");
        Ok = resMap.getString("Ok");
        Warning = resMap.getString("Warning");
        PubringFile = new File(pubringFileName);
        SecringFile = new File(secringFileName);
        InputStream pubIn = null, secIn = null;
        try {
            if (PubringFile.exists()) {
                pubIn = new FileInputStream(PubringFile);
                pubring = new PGPPublicKeyRingCollection(pubIn);
            } else {
                pubring = new PGPPublicKeyRingCollection(Collections.EMPTY_LIST);
            }
            if (SecringFile.exists()) {
                secIn = new FileInputStream(SecringFile);
                secring = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(secIn));
            } else {
                secring = new PGPSecretKeyRingCollection(Collections.EMPTY_LIST);
            }
            System.out.println("Loading keyrings...");
            System.out.println("Pubring: " + PubringFile.getAbsolutePath());
            System.out.println("Secring: " + SecringFile.getAbsolutePath());
            System.out.println("---Done---");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            IOUtils.closeQuietly(pubIn);
            IOUtils.closeQuietly(secIn);
        }
    }

    public static PGPPublicKeyRing importPublicKey(File file) throws Exception {
        FileInputStream fis = null;
        PGPPublicKeyRing newKey = null;
        try {
            fis = new FileInputStream(file);
            newKey = new PGPPublicKeyRing(new ArmoredInputStream(fis));
            pubring = PGPPublicKeyRingCollection.addPublicKeyRing(pubring, newKey);
            savePubring();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fis);
        }
        return newKey;
    }

    public static PGPSecretKeyRing importPrivateKey(File file) throws Exception {
        BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(file));
        PGPSecretKeyRing newKey = null;
        iStream.mark(128 * 1024);
        try {
            newKey = new PGPSecretKeyRing(new ArmoredInputStream(iStream));
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new Exception(ex);
        } finally {
            IOUtils.closeQuietly(iStream);
        }
        if (newKey == null) {
            throw new Exception();
        }
        secring = PGPSecretKeyRingCollection.addSecretKeyRing(secring, newKey);
        saveSecring();
        return newKey;
    }

    public static void generateNewKeyPair(int iKeySize, int iStrength, String iUserID, char[] iPassphrase) throws Exception {
        System.err.format("iKeySize= %4s, iStrength= %2s, iUserID= %10s, iPassphrase= %10s\n", iKeySize, iStrength, iUserID, Arrays.toString(iPassphrase));
        KeyPairGenerator dsaKpg = KeyPairGenerator.getInstance(PGPUtils.DSA, PGPUtils.PROVIDER);
        dsaKpg.initialize(iKeySize);
        KeyPair dsaKp = dsaKpg.generateKeyPair();
        KeyPairGenerator elgKpg = KeyPairGenerator.getInstance(PGPUtils.ElGamal, PGPUtils.PROVIDER);
        if (iStrength > 0) {
            ElGamalParametersGenerator paramGen = new ElGamalParametersGenerator();
            paramGen.init(iKeySize, iStrength, new SecureRandom());
            ElGamalParameters genParams = paramGen.generateParameters();
            ElGamalParameterSpec elParams = new ElGamalParameterSpec(genParams.getP(), genParams.getG());
            elgKpg.initialize(elParams);
        } else {
            BigInteger g = new BigInteger("153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7749199681ee5b212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b410b7a0f12ca1cb9a428cc", 16);
            BigInteger p = new BigInteger("9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd38744d488cf8e31db8bcb7d33b41abb9e5a33cca9144b1cef332c94bf0573bf047a3aca98cdf3b", 16);
            ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, g);
            elgKpg.initialize(elParams);
        }
        KeyPair elgKp = elgKpg.generateKeyPair();
        PGPKeyPair dsaKeyPair = new PGPKeyPair(PGPPublicKey.DSA, dsaKp, new Date());
        PGPKeyPair elgKeyPair = new PGPKeyPair(PGPPublicKey.ELGAMAL_ENCRYPT, elgKp, new Date());
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair, iUserID, KEY_ENCRYPTION_ALGO, iPassphrase, true, null, null, new SecureRandom(), PGPUtils.PROVIDER);
        keyRingGen.addSubKey(elgKeyPair);
        pubring = PGPPublicKeyRingCollection.addPublicKeyRing(pubring, keyRingGen.generatePublicKeyRing());
        savePubring();
        secring = PGPSecretKeyRingCollection.addSecretKeyRing(secring, keyRingGen.generateSecretKeyRing());
        saveSecring();
    }

    @Deprecated
    @SuppressWarnings("rawtypes")
    public static void changePrivateKeyPassphrase(PGPSecretKeyRing iKeyRing, char[] iOldPassphrase, char[] iNewPassphrase) throws Exception {
        PGPSecretKeyRing newKeyring = iKeyRing;
        Iterator it = iKeyRing.getSecretKeys();
        while (it.hasNext()) {
            PGPSecretKey oldKey = (PGPSecretKey) it.next();
            PGPSecretKey newKey = PGPSecretKey.copyWithNewPassword(oldKey, iOldPassphrase, iNewPassphrase, KEY_ENCRYPTION_ALGO, new SecureRandom(), PGPUtils.PROVIDER);
            newKeyring = PGPSecretKeyRing.removeSecretKey(newKeyring, oldKey);
            newKeyring = PGPSecretKeyRing.insertSecretKey(newKeyring, newKey);
        }
        secring = PGPSecretKeyRingCollection.removeSecretKeyRing(secring, secring.getSecretKeyRing(newKeyring.getSecretKey().getKeyID()));
        secring = PGPSecretKeyRingCollection.addSecretKeyRing(secring, newKeyring);
        saveSecring();
    }

    @SuppressWarnings("rawtypes")
    public static Collection<PrintablePGPPublicKeyRing> getPublicKeys() {
        List<PrintablePGPPublicKeyRing> outVec = new ArrayList<PrintablePGPPublicKeyRing>();
        for (Iterator it = pubring.getKeyRings(); it.hasNext(); ) {
            PGPPublicKeyRing kr = (PGPPublicKeyRing) it.next();
            outVec.add(new PrintablePGPPublicKeyRing(kr));
        }
        return outVec;
    }

    @SuppressWarnings("rawtypes")
    public static Collection<PrintablePGPSecretKeyRing> getPrivateKeys() {
        List<PrintablePGPSecretKeyRing> outVec = new ArrayList<PrintablePGPSecretKeyRing>();
        for (Iterator it = secring.getKeyRings(); it.hasNext(); ) {
            PGPSecretKeyRing kr = (PGPSecretKeyRing) it.next();
            outVec.add(new PrintablePGPSecretKeyRing(kr));
        }
        return outVec;
    }

    private static PGPSecretKey getPrivateKeyByID(long iID) throws PGPException {
        return secring.getSecretKey(iID);
    }

    private static PGPPublicKey getPublicKeyByID(long iID) throws PGPException {
        return pubring.getPublicKey(iID);
    }

    public static void deletePublicKey(PGPPublicKeyRing iKey) throws IOException {
        pubring = PGPPublicKeyRingCollection.removePublicKeyRing(pubring, iKey);
        savePubring();
    }

    public static void deletePrivateKey(PGPSecretKeyRing iKey) throws IOException {
        secring = PGPSecretKeyRingCollection.removeSecretKeyRing(secring, iKey);
        saveSecring();
    }

    private static void savePubring() throws IOException {
        OutputStream pub_out = new FileOutputStream(PubringFile);
        pubring.encode(pub_out);
        IOUtils.closeQuietly(pub_out);
    }

    private static void saveSecring() throws IOException {
        OutputStream sec_out = new FileOutputStream(SecringFile);
        secring.encode(sec_out);
        IOUtils.closeQuietly(sec_out);
    }

    public static String keyId2Hex(long id) {
        return Integer.toHexString((int) id).toUpperCase();
    }

    /**
     * for decreasing the lines in PortablePGPView.java<br/>
     * Garfield Geng added at 2011/07/12.
     * @param printablePGPPublicKeyRing
     */
    public static Collection<PGPPublicKey> getPGPPublicKeyCollection(Object printablePGPPublicKeyRing) {
        List<PGPPublicKey> recipients = new ArrayList<PGPPublicKey>();
        if (printablePGPPublicKeyRing instanceof PublicKeyCollection) {
            for (PrintablePGPPublicKeyRing item : ((PublicKeyCollection) printablePGPPublicKeyRing).getKeys()) {
                recipients.add(item.getEncryptionKey());
            }
        } else {
            recipients.add(((PrintablePGPPublicKeyRing) printablePGPPublicKeyRing).getEncryptionKey());
        }
        return recipients;
    }

    public static void saveKey(byte[] data, File outFile) throws Exception {
        OutputStream ostream = new ArmoredOutputStream(new FileOutputStream(outFile));
        ostream.write(data);
        IOUtils.closeQuietly(ostream);
    }
}
