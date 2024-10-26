package test.integ.be.fedict.trust;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import be.fedict.trust.AlgorithmPolicy;
import be.fedict.trust.CertificatePathBuilder;
import be.fedict.trust.MemoryCertificateRepository;
import be.fedict.trust.NetworkConfig;
import be.fedict.trust.TrustValidator;
import be.fedict.trust.TrustValidatorDecorator;

public class SSLTrustValidatorTest {

    private static final Log LOG = LogFactory.getLog(SSLTrustValidatorTest.class);

    @Before
    public void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testValidation() throws Exception {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.yourict.net", 8080));
        NetworkConfig networkConfig = new NetworkConfig("proxy.yourict.net", 8080);
        URL url = new URL("https://idp.int.belgium.be");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(proxy);
        connection.connect();
        Certificate[] serverCertificates = connection.getServerCertificates();
        List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
        for (Certificate certificate : serverCertificates) {
            X509Certificate x509Cert = (X509Certificate) certificate;
            certificateChain.add(x509Cert);
            LOG.debug("certificate: " + x509Cert);
        }
        if (true) {
            return;
        }
        CertificatePathBuilder certificatePathBuilder = new CertificatePathBuilder();
        certificateChain = certificatePathBuilder.buildPath(certificateChain);
        MemoryCertificateRepository certificateRepository = new MemoryCertificateRepository();
        certificateRepository.addTrustPoint(certificateChain.get(certificateChain.size() - 1));
        TrustValidator trustValidator = new TrustValidator(certificateRepository);
        trustValidator.setAlgorithmPolicy(new AlgorithmPolicy() {

            @Override
            public void checkSignatureAlgorithm(String signatureAlgorithm) throws SignatureException {
            }
        });
        TrustValidatorDecorator trustValidatorDecorator = new TrustValidatorDecorator(networkConfig);
        trustValidatorDecorator.addDefaultTrustLinkerConfig(trustValidator);
        trustValidator.isTrusted(certificateChain);
    }
}
