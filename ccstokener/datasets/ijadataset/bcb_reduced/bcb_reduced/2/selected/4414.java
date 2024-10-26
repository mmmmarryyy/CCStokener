package com.directthought.elasticweb;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.directthought.elasticweb.nettica.NetticaAPI;
import com.directthought.elasticweb.nettica.NetticaAPI.ARecord;
import com.directthought.elasticweb.nettica.NetticaException;

public class InstanceMonitor {

    private static Logger logger = Logger.getLogger(InstanceMonitor.class);

    private static final String EW_PROPERTIES = "elasticweb.properties";

    private static final String NETTICA_USER = "nettica.username";

    private static final String NETTICA_PASS = "nettica.password";

    private static final String EW_DOMAIN = "ew.domain";

    private static final String EW_HOST = "ew.host";

    private static final String EW_MIN_SERVERS = "ew.min.servers";

    private static final String EW_CAPACITY_HEADRM = "ew.capacity.headroom";

    private static final String EW_AMI = "ew.ami";

    private static final String EW_KEYPAIR = "ew.keypair";

    private static final String EW_GROUP = "ew.net.group";

    private static final String EW_MOCK_HTTP = "ew.enable.mock.http";

    private String awsAccessId;

    private String awsSecretKey;

    private RestS3Service s3;

    private S3Bucket bucket;

    private Properties props;

    private NetticaAPI dns;

    private boolean first;

    private String hostname;

    private String instanceId;

    private String externalIP;

    private String userData;

    private HashMap<String, Instance> hosts;

    public InstanceMonitor(String awsAccessId, String awsSecretKey, String bucketName, boolean first) throws IOException {
        this.awsAccessId = awsAccessId;
        this.awsSecretKey = awsSecretKey;
        props = new Properties();
        while (true) {
            try {
                s3 = new RestS3Service(new AWSCredentials(awsAccessId, awsSecretKey));
                bucket = new S3Bucket(bucketName);
                S3Object obj = s3.getObject(bucket, EW_PROPERTIES);
                props.load(obj.getDataInputStream());
                break;
            } catch (S3ServiceException ex) {
                logger.error("problem fetching props from bucket, retrying", ex);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException iex) {
                }
            }
        }
        URL url = new URL("http://169.254.169.254/latest/meta-data/hostname");
        hostname = new BufferedReader(new InputStreamReader(url.openStream())).readLine();
        url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
        instanceId = new BufferedReader(new InputStreamReader(url.openStream())).readLine();
        url = new URL("http://169.254.169.254/latest/meta-data/public-ipv4");
        externalIP = new BufferedReader(new InputStreamReader(url.openStream())).readLine();
        this.dns = new NetticaAPI(props.getProperty(NETTICA_USER), props.getProperty(NETTICA_PASS));
        this.userData = awsAccessId + " " + awsSecretKey + " " + bucketName;
        this.first = first;
        logger.info("InstanceMonitor initialized, first=" + first);
    }

    public void run() throws Exception {
        new PingListener().start();
        new HCListener().start();
        String enable = props.getProperty(EW_MOCK_HTTP).toLowerCase();
        if (enable.equals("yes") || enable.equals("true")) {
            logger.info("Starting http service on port " + MockHTTPListener.HTTP_SOCKET);
            new MockHTTPListener(instanceId).start();
        }
        if (this.first) {
            while (true) {
                try {
                    S3Object[] objs = s3.listObjects(bucket, "IS", null);
                    for (S3Object obj : objs) {
                        s3.deleteObject(bucket, obj.getKey());
                    }
                    objs = s3.listObjects(bucket, "FI", null);
                    for (S3Object obj : objs) {
                        s3.deleteObject(bucket, obj.getKey());
                    }
                    break;
                } catch (S3ServiceException ex) {
                    logger.error("problem fetching props from bucket, retrying", ex);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException iex) {
                    }
                }
            }
            List<ARecord> entries = this.dns.listARecord(props.getProperty(EW_DOMAIN));
            for (ARecord r : entries) {
                this.dns.deleteARecord(r);
            }
            int numServers = Integer.parseInt(props.getProperty(EW_MIN_SERVERS)) - 1;
            startServers(numServers);
        }
        this.dns.addARecord(props.getProperty(EW_DOMAIN), props.getProperty(EW_HOST), externalIP, -1);
        writeStatusFile(0);
        while (true) {
            refreshHostList();
            logger.debug("entering ping loop");
            for (String key : this.hosts.keySet()) {
                Instance host = hosts.get(key);
                if (!host.hostName.equals(this.hostname) && !PingListener.pingHost(host.hostName)) {
                    logger.error("can't ping host :" + host);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException iex) {
                    }
                    if (!PingListener.pingHost(host.hostName)) {
                        handleFailure(key, host);
                    }
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException iex) {
            }
            S3Object[] objs = s3.listObjects(bucket, "SHUTDOWN", null);
            if (objs.length > 0) {
                break;
            }
        }
        this.dns.deleteARecord(props.getProperty(EW_DOMAIN), props.getProperty(EW_HOST), externalIP, -1);
        while (true) {
            try {
                s3.deleteObject(bucket, "IS" + instanceId);
                break;
            } catch (S3ServiceException ex) {
                logger.error("problem fetching props from bucket, retrying", ex);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException iex) {
                }
            }
        }
        stopServer(instanceId);
        System.exit(0);
    }

    private void refreshHostList() {
        while (true) {
            try {
                this.hosts = new HashMap<String, Instance>();
                S3Object[] objs = s3.listObjects(bucket, "IS", null);
                for (S3Object obj : objs) {
                    obj = s3.getObjectDetails(bucket, obj.getKey());
                    Instance i = new Instance((String) obj.getMetadata("hostname"), (String) obj.getMetadata("externalIP"));
                    this.hosts.put(obj.getKey().substring(2), i);
                }
                return;
            } catch (S3ServiceException ex) {
                logger.error("problem fetching status files from bucket, retrying", ex);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException iex) {
                }
            }
        }
    }

    private void startServers(int numServers) {
        Jec2 ec2 = new Jec2(awsAccessId, awsSecretKey);
        while (numServers > 0) {
            try {
                ArrayList<String> group = new ArrayList<String>();
                group.add(props.getProperty(EW_GROUP));
                ReservationDescription servers = ec2.runInstances(props.getProperty(EW_AMI), 1, numServers, group, this.userData, props.getProperty(EW_KEYPAIR));
                int numStarted = servers.getInstances().size();
                numServers -= numStarted;
            } catch (EC2Exception ex) {
                logger.error("Problem starting other instances, going to retry", ex);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException iex) {
                }
            }
        }
    }

    private void stopServer(String instanceId) {
        while (true) {
            try {
                Jec2 ec2 = new Jec2(awsAccessId, awsSecretKey);
                ec2.terminateInstances(new String[] { instanceId });
                break;
            } catch (EC2Exception ex) {
                logger.error("Problem terminating instance, going to retry", ex);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException iex) {
                }
            }
        }
    }

    private void handleFailure(String instanceId, Instance failedHost) {
        while (true) {
            try {
                S3Object[] objs = s3.listObjects(bucket, "FI", null);
                for (S3Object obj : objs) {
                    if (obj.getKey().equals("FI" + instanceId)) {
                        logger.debug("already a failure file. doing nothing");
                        ;
                        return;
                    }
                }
                break;
            } catch (S3ServiceException ex) {
                logger.error("problem fetching props from bucket, retrying", ex);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException iex) {
                }
            }
        }
        S3Object obj = registerFailure(instanceId, failedHost.hostName);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
        }
        if (confirmOwnership(obj)) {
            boolean confirmed = true;
            if (hosts.size() == 2) {
            } else {
                int confirmations = 0;
                for (String key : hosts.keySet()) {
                    String h = hosts.get(key).hostName;
                    if (!h.equals(failedHost.hostName)) {
                        boolean checkOK = HCListener.requestCheck(h, failedHost.hostName);
                        if (checkOK) {
                            confirmed = false;
                            break;
                        } else confirmations++;
                    }
                }
                logger.debug("# confirmations = " + confirmations);
            }
            if (confirmOwnership(obj)) {
                logger.debug("yes, we still own the failure file");
                if (confirmed) {
                    logger.debug("we confirmed the failure, so take care of it!");
                    startServers(1);
                    stopServer(instanceId);
                    while (true) {
                        try {
                            s3.deleteObject(bucket, "IS" + instanceId);
                            break;
                        } catch (S3ServiceException ex) {
                            logger.error("problem fetching props from bucket, retrying", ex);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException iex) {
                            }
                        }
                    }
                }
                while (true) {
                    try {
                        s3.deleteObject(bucket, obj.getKey());
                        break;
                    } catch (S3ServiceException ex) {
                        logger.error("problem fetching props from bucket, retrying", ex);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException iex) {
                        }
                    }
                }
            } else {
                ;
            }
        } else {
            ;
        }
    }

    private void writeStatusFile(int load) {
        while (true) {
            try {
                S3Object obj = new S3Object(bucket, "IS" + instanceId);
                obj.setContentLength(0);
                obj.addMetadata("hostname", hostname);
                obj.addMetadata("load", load);
                obj.addMetadata("externalIP", externalIP);
                s3.putObject(bucket, obj);
                break;
            } catch (S3ServiceException ex) {
                logger.error("Problem writing status file to S3, retrying.", ex);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException iex) {
                }
            }
        }
    }

    private S3Object registerFailure(String instanceId, String host) {
        while (true) {
            try {
                S3Object obj = new S3Object(bucket, "FI" + instanceId);
                obj.setContentLength(0);
                obj.addMetadata("hostname", host);
                obj.addMetadata("owner", this.instanceId);
                s3.putObject(bucket, obj);
                return obj;
            } catch (S3ServiceException ex) {
                logger.error("Problem writing status file to S3, retrying.", ex);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException iex) {
                }
            }
        }
    }

    private boolean confirmOwnership(S3Object obj) {
        while (true) {
            try {
                obj = s3.getObjectDetails(bucket, obj.getKey());
                break;
            } catch (S3ServiceException ex) {
                logger.error("problem fetching props from bucket, retrying", ex);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException iex) {
                }
            }
        }
        String owner = (String) obj.getMetadata("owner");
        return owner.equals(this.instanceId);
    }

    class Instance {

        public String hostName;

        public String externalIP;

        Instance(String hostName, String externalIP) {
            this.hostName = hostName;
            this.externalIP = externalIP;
        }
    }

    public static void main(String[] args) {
        try {
            DOMConfigurator.configure(InstanceMonitor.class.getClassLoader().getResource("Log4j.xml"));
            if (args.length < 3) {
                logger.error("usage : InstanceMonitor <accessId> <secretKey> <bucket> [first]");
                System.exit(0);
            }
            boolean first = args.length > 3;
            InstanceMonitor im = new InstanceMonitor(args[0], args[1], args[2], first);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException iex) {
            }
            im.run();
        } catch (Exception ex) {
            logger.error("Something unexpected happened in the Instance Monitor", ex);
        }
    }
}
