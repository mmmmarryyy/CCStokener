import java.applet.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;

class VideoAppletSplitDescriptorItem {

    int xOffset = -1;

    int yOffset = -1;

    int width = -1;

    int height = -1;

    public void readObject(DataInputStream din) throws IOException {
        xOffset = din.readInt();
        yOffset = din.readInt();
        width = din.readInt();
        height = din.readInt();
    }
}

class VideoAppletSplitDescriptor {

    boolean isValid = false;

    Dimension appletDimension = null;

    int messageBoxHeight = 0;

    int appletImageXoffset = -1;

    int appletImageYoffset = -1;

    int splits = -1;

    int width = -1;

    int height = -1;

    VideoAppletSplitDescriptorItem[][] item = null;

    public VideoAppletSplitDescriptor(Dimension appletDim, int msgBoxHeight) {
        appletDimension = appletDim;
        messageBoxHeight = msgBoxHeight;
    }

    public boolean isValid() {
        return isValid();
    }

    public int getSplits() {
        return splits;
    }

    public int getXoffset() {
        return appletImageXoffset;
    }

    public int getYoffset() {
        return appletImageYoffset;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean readServerInfo(DataOutputStream dout, DataInputStream din) throws IOException {
        dout.writeByte(12);
        dout.flush();
        byte serverStatus = din.readByte();
        if (serverStatus != 1) return false;
        this.readObject(din);
        isValid = true;
        return true;
    }

    public boolean readServerInfo(VideoAppletHttpRequest httpRequest) throws MalformedURLException, IOException {
        DataInputStream din = httpRequest.transmit("va-split-descriptor.gif");
        byte serverStatus = din.readByte();
        if (serverStatus != 1) {
            din.close();
            return false;
        }
        this.readObject(din);
        isValid = true;
        din.close();
        return true;
    }

    private void readObject(DataInputStream din) throws IOException {
        width = din.readInt();
        height = din.readInt();
        splits = din.readInt();
        item = new VideoAppletSplitDescriptorItem[splits][splits];
        for (int x = 0; x < splits; x++) for (int y = 0; y < splits; y++) {
            item[x][y] = new VideoAppletSplitDescriptorItem();
            item[x][y].readObject(din);
        }
        appletImageXoffset = (int) ((appletDimension.width - width) / 2);
        appletImageYoffset = (int) ((appletDimension.height - height - messageBoxHeight) / 2);
    }

    public void convertToAppletCoordinates() {
        for (int x = 0; x < splits; x++) {
            int splitOffsetY = height;
            for (int y = 0; y < splits; y++) {
                splitOffsetY = splitOffsetY - item[x][y].height;
                item[x][y].yOffset = splitOffsetY;
            }
        }
    }

    public void dumpInfo() {
        log("");
        log("image width=" + width);
        log("image height=" + height);
        log("image splits=" + splits);
        {
            for (int x = 0; x < splits; x++) for (int y = 0; y < splits; y++) {
                log("");
                log("[" + x + "," + y + "] x-offset=" + item[x][y].xOffset);
                log("[" + x + "," + y + "] y-offset=" + item[x][y].yOffset);
                log("[" + x + "," + y + "] width=" + item[x][y].width);
                log("[" + x + "," + y + "] height=" + item[x][y].height);
            }
        }
        log("");
    }

    private static void log(String msg) {
        VideoApplet.log(msg);
    }
}

class VideoAppletSplitData {

    public boolean splitUpdateReceived = false;

    public Image splitImage = null;

    public int splitSequenceNumber = -2;

    public int xOffset = -1;

    public int yOffset = -1;

    public int splitWidth = -1;

    public int splitHeight = -1;

    public float splitDiffRatio = 0;

    private int jpgImageSplitLength = 0;

    public byte[] jpgImageSplit = null;

    public VideoAppletSplitData() {
    }

    public VideoAppletSplitData(VideoAppletSplitData d) {
        splitUpdateReceived = d.splitUpdateReceived;
        splitImage = null;
        splitSequenceNumber = d.splitSequenceNumber;
        xOffset = d.xOffset;
        yOffset = d.yOffset;
        splitWidth = d.splitWidth;
        splitHeight = d.splitHeight;
        splitDiffRatio = d.splitDiffRatio;
        jpgImageSplitLength = d.jpgImageSplitLength;
        if (d.jpgImageSplit != null) {
            jpgImageSplit = new byte[d.jpgImageSplit.length];
            System.arraycopy(d.jpgImageSplit, 0, jpgImageSplit, 0, d.jpgImageSplit.length);
        }
    }

    public int readObject(DataInputStream din) throws IOException {
        splitSequenceNumber = din.readInt();
        xOffset = din.readInt();
        yOffset = din.readInt();
        splitWidth = din.readInt();
        splitHeight = din.readInt();
        splitDiffRatio = din.readFloat();
        jpgImageSplitLength = din.readInt();
        jpgImageSplit = new byte[jpgImageSplitLength];
        din.readFully(jpgImageSplit);
        return (7 * 4) + jpgImageSplitLength;
    }

    public int getReceivedImageLength() {
        return jpgImageSplitLength;
    }

    public void createSplitImage(VideoApplet applet, MediaTracker mediaTracker, int z) {
        splitImage = applet.toolkit.createImage(jpgImageSplit);
        applet.toolkit.prepareImage(splitImage, -1, -1, applet);
        mediaTracker.addImage(splitImage, z);
    }

    public void drawSplit(VideoApplet applet, VideoAppletSplitDescriptor splitDescriptor, VideoAppletSplitDescriptorItem descriptorItem) {
        applet.intGC.drawImage(splitImage, splitDescriptor.getXoffset() + xOffset, splitDescriptor.getYoffset() + descriptorItem.yOffset, null);
        if (applet.getDrawSplitFrames()) applet.intGC.drawRect(splitDescriptor.getXoffset() + xOffset, splitDescriptor.getYoffset() + descriptorItem.yOffset, splitWidth - 1, splitHeight - 1);
    }

    private static void log(String msg) {
        VideoApplet.log(msg);
    }
}

class VideoAppletSplitBuffer {

    private VideoAppletSplitDescriptor splitDescriptor = null;

    private int splits = 0;

    private int transferLimit = 0;

    private VideoAppletSplitData[][] splitData;

    private long receivedFullBytes = 0;

    private long receivedFullImages = 0;

    private long receivedSplitBytes = 0;

    private long receivedSplits = 0;

    private long transmitBytes = 0;

    private long receivedBytes = 0;

    public VideoAppletSplitBuffer(VideoAppletSplitDescriptor splitMetadata, int transferSplitLimit) {
        splitDescriptor = splitMetadata;
        splits = splitDescriptor.splits;
        transferLimit = transferSplitLimit;
        splitData = new VideoAppletSplitData[splits][splits];
        for (int x = 0; x < splits; x++) for (int y = 0; y < splits; y++) splitData[x][y] = new VideoAppletSplitData();
    }

    public VideoAppletSplitBuffer(VideoAppletSplitBuffer b) {
        splitDescriptor = b.splitDescriptor;
        splits = b.splits;
        transferLimit = b.transferLimit;
        if (b.splitData != null) {
            splitData = new VideoAppletSplitData[splits][splits];
            for (int x = 0; x < splits; x++) for (int y = 0; y < splits; y++) {
                splitData[x][y] = new VideoAppletSplitData(b.splitData[x][y]);
            }
        }
        receivedFullBytes = b.receivedFullBytes;
        receivedFullImages = b.receivedFullImages;
        receivedSplitBytes = b.receivedSplitBytes;
        receivedSplits = b.receivedSplits;
    }

    public VideoAppletSplitData[][] getSplitData() {
        return splitData;
    }

    public void markFullImageUpdate(int fullSequenceNumber, int fullBytes) {
        for (int x = 0; x < splits; x++) for (int y = 0; y < splits; y++) {
            splitData[x][y].splitSequenceNumber = fullSequenceNumber;
            splitData[x][y].jpgImageSplit = null;
        }
        receivedFullBytes = receivedFullBytes + fullBytes;
        receivedFullImages++;
        int bestTransferLimit = this.getBestSplitTransferLimit();
        if (transferLimit != bestTransferLimit) if (bestTransferLimit > 0) {
            transferLimit = bestTransferLimit;
            log(">>> split transfer limit set to " + transferLimit);
        }
    }

    public void makeSplitRequest(DataOutputStream dout, float diffRatio) throws IOException {
        int startSize = dout.size();
        dout.writeByte(13);
        dout.writeInt(splits);
        dout.writeFloat(diffRatio);
        dout.writeInt(transferLimit);
        for (int x = 0; x < splits; x++) for (int y = 0; y < splits; y++) {
            splitData[x][y].splitUpdateReceived = false;
            splitData[x][y].splitImage = null;
            splitData[x][y].jpgImageSplit = null;
            dout.writeInt(splitData[x][y].splitSequenceNumber);
        }
        dout.flush();
        transmitBytes = transmitBytes + (dout.size() - startSize);
    }

    public DataInputStream makeSplitRequest(VideoAppletHttpRequest httpRequest, float diffRatio) throws MalformedURLException, IOException {
        StringBuffer cgiData = new StringBuffer();
        cgiData.append("splits=" + splits);
        cgiData.append("&diffRatio=" + diffRatio);
        cgiData.append("&transferLimit=" + transferLimit);
        for (int x = 0; x < splits; x++) for (int y = 0; y < splits; y++) {
            splitData[x][y].splitUpdateReceived = false;
            splitData[x][y].splitImage = null;
            splitData[x][y].jpgImageSplit = null;
            cgiData.append("&splitData[" + x + "][" + y + "].splitSequenceNumber=" + splitData[x][y].splitSequenceNumber);
        }
        transmitBytes = transmitBytes + cgiData.length();
        DataInputStream din = httpRequest.transmit("va-split-request.gif", cgiData.toString());
        return din;
    }

    public int readSplitResponse(DataInputStream din, VideoApplet applet) throws IOException {
        MediaTracker mediaTracker = new MediaTracker(applet);
        int currentSequenceNumber = 0;
        int splitCount = din.readInt();
        receivedBytes = receivedBytes + 4;
        for (int z = 0; z < splitCount; z++) {
            int xSplit = din.readInt();
            int ySplit = din.readInt();
            receivedBytes = receivedBytes + 8;
            receivedBytes = receivedBytes + splitData[xSplit][ySplit].readObject(din);
            if (splitData[xSplit][ySplit].splitSequenceNumber > currentSequenceNumber) currentSequenceNumber = splitData[xSplit][ySplit].splitSequenceNumber;
            splitData[xSplit][ySplit].splitUpdateReceived = true;
            receivedSplitBytes = receivedSplitBytes + splitData[xSplit][ySplit].getReceivedImageLength();
            receivedSplits++;
            int bestTransferLimit = this.getBestSplitTransferLimit();
            if (transferLimit != bestTransferLimit) if (bestTransferLimit > 0) {
                transferLimit = bestTransferLimit;
                applet.videoSplitInfo = transferLimit;
                log(">>> split transfer limit set to " + transferLimit);
            }
        }
        return currentSequenceNumber;
    }

    public long getTotalBytes() {
        return transmitBytes + receivedBytes;
    }

    public int getAverageSplitSize() {
        if ((receivedSplitBytes == 0) || (receivedSplits == 0)) return -1;
        return (int) (receivedSplitBytes / receivedSplits);
    }

    public int getAverageFullSize() {
        if ((receivedFullBytes == 0) || (receivedFullImages == 0)) return -1;
        return (int) (receivedFullBytes / receivedFullImages);
    }

    private int getBestSplitTransferLimit() {
        int averageFullSize = this.getAverageFullSize();
        int averageSplitSize = this.getAverageSplitSize();
        if ((averageFullSize <= 0) || (averageSplitSize <= 0)) return -1;
        return (int) ((float) averageFullSize / (float) averageSplitSize);
    }

    private static void log(String msg) {
        VideoApplet.log(msg);
    }
}

class VideoAppletFullDisplayThread extends Thread {

    private VideoApplet applet;

    private VideoAppletSplitDescriptor splitDescriptor;

    private byte[] streamedJpgImage;

    public VideoAppletFullDisplayThread(VideoApplet a, VideoAppletSplitDescriptor sd, byte[] byteImage) {
        applet = a;
        splitDescriptor = sd;
        streamedJpgImage = byteImage;
    }

    public void run() {
        Image videoImage = applet.toolkit.createImage(streamedJpgImage);
        applet.toolkit.prepareImage(videoImage, -1, -1, applet);
        MediaTracker mediaTracker = new MediaTracker(applet);
        mediaTracker.addImage(videoImage, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (InterruptedException e) {
        }
        synchronized (applet.doubleBuffer) {
            applet.intGC.drawImage(videoImage, splitDescriptor.getXoffset(), splitDescriptor.getYoffset(), null);
            if ((applet.getLogoImage() != null) && (applet.getLogoPosition() != null)) applet.intGC.drawImage(applet.getLogoImage(), applet.getLogoPosition().x, applet.getLogoPosition().y, null);
        }
        applet.repaint();
    }

    private static void log(String msg) {
        VideoApplet.log(msg);
    }
}

class VideoAppletSplitDisplayThread extends Thread {

    VideoApplet applet;

    VideoAppletSplitDescriptor splitDescriptor;

    VideoAppletSplitBuffer splitBuffer;

    public VideoAppletSplitDisplayThread(VideoApplet videoApplet, VideoAppletSplitDescriptor sd, VideoAppletSplitBuffer sb) {
        applet = videoApplet;
        splitDescriptor = sd;
        splitBuffer = sb;
    }

    public void run() {
        VideoAppletSplitData[][] splitData = splitBuffer.getSplitData();
        MediaTracker mediaTracker = new MediaTracker(applet);
        int receivedSplitCount = 0;
        for (int x = 0; x < splitDescriptor.getSplits(); x++) for (int y = 0; y < splitDescriptor.getSplits(); y++) if (splitData[x][y].splitUpdateReceived) {
            splitData[x][y].createSplitImage(applet, mediaTracker, receivedSplitCount);
            if (receivedSplitCount > 0) try {
                mediaTracker.waitForID(receivedSplitCount - 1);
            } catch (InterruptedException ie) {
            }
            receivedSplitCount++;
        }
        if (receivedSplitCount == 0) {
            log("*** internal error in VideoAppletSplitDisplayThread.run(): receivedSplitCount == 0");
            return;
        }
        try {
            mediaTracker.waitForID(receivedSplitCount - 1);
        } catch (InterruptedException ie) {
        }
        synchronized (applet.doubleBuffer) {
            for (int x = 0; x < splitDescriptor.getSplits(); x++) for (int y = 0; y < splitDescriptor.getSplits(); y++) if (splitData[x][y].splitUpdateReceived) {
                VideoAppletSplitDescriptorItem descriptorItem = splitDescriptor.item[x][y];
                splitData[x][y].drawSplit(applet, splitDescriptor, descriptorItem);
            }
        }
        if ((applet.getLogoImage() != null) && (applet.getLogoPosition() != null)) applet.intGC.drawImage(applet.getLogoImage(), applet.getLogoPosition().x, applet.getLogoPosition().y, null);
        applet.repaint();
    }

    private static void log(String msg) {
        VideoApplet.log(msg);
    }
}

public class VideoApplet extends Applet implements Runnable, MouseListener {

    public static Color C_InitialBgColor = Color.black;

    public static Color C_InitialFgColor = Color.white;

    public static Color C_InitialInfoColor = new Color(102, 204, 255);

    public static Color C_InitialNightColor = new Color(36, 69, 137);

    public static Color C_InitialErrorColor = Color.red;

    private static Image[] welcomeImages = null;

    private static Point logoDrawPosition = null;

    static final long C_MinUpdateDelay = 130;

    static final float C_DiffRatio = 0.2f;

    static final int C_TransferSplitLimit = 4;

    static final boolean C_DrawSplitFrames = false;

    private static long optUpdateDelay = C_MinUpdateDelay;

    private static float optDiffRatio = C_DiffRatio;

    private static boolean optDrawSplitFrames = C_DrawSplitFrames;

    public Toolkit toolkit = null;

    private Dimension appletDimension;

    private AppletContext appletContext;

    private Insets insets = null;

    public Image doubleBuffer = null;

    public Graphics intGC = null;

    public MediaTracker mediaTracker = null;

    public Font defaultFont;

    private FontMetrics defaultFontMetrics;

    private Font boldFont;

    private FontMetrics boldFontMetrics;

    public Font smallFont;

    private FontMetrics smallFontMetrics;

    private Label timeLabel = null;

    private int messageBoxWidth;

    private int messageBoxHeight;

    private String videoServerHostname = null;

    private int videoServerPort = 3333;

    private int videoServerHttpPort = 80;

    private Socket videoSocket = null;

    private String videoConnectionInfo = "Not Connected";

    public int videoSplitInfo = C_TransferSplitLimit;

    private float transferSpeed = 0;

    private float frameSpeed = 0;

    private int averageSplitSize = 0;

    private int averageFullSize = 0;

    private Thread appletThread = null;

    private VideoAppletOptionsWindow optionsWindow = null;

    public Image getLogoImage() {
        return welcomeImages[1];
    }

    public Point getLogoPosition() {
        return logoDrawPosition;
    }

    public String getServerHostname() {
        return videoServerHostname;
    }

    public int getServerHttpPort() {
        return videoServerHttpPort;
    }

    public String getConnectionInfo() {
        return videoConnectionInfo;
    }

    public long getMinUpdateDelay() {
        return optUpdateDelay;
    }

    public void setMinUpdateDelay(long millis) {
        optUpdateDelay = millis;
    }

    public float getImageDiffRatio() {
        return optDiffRatio;
    }

    public void setImageDiffRatio(float diffRatio) {
        optDiffRatio = diffRatio;
    }

    public int getVideoSplitInfo() {
        return videoSplitInfo;
    }

    public boolean getDrawSplitFrames() {
        return optDrawSplitFrames;
    }

    public void setDrawSplitFrames(boolean debug) {
        optDrawSplitFrames = debug;
    }

    public float getTransferSpeed() {
        return transferSpeed;
    }

    public float getFrameSpeed() {
        return frameSpeed;
    }

    public int getAverageSplitSize() {
        return averageSplitSize;
    }

    public int getAverageFullSize() {
        return averageFullSize;
    }

    public void init() {
        toolkit = this.getToolkit();
        appletDimension = this.getSize();
        appletContext = getAppletContext();
        doubleBuffer = createImage(appletDimension.width, appletDimension.height);
        intGC = doubleBuffer.getGraphics();
        defaultFont = new Font("Helvetica", Font.PLAIN, 11);
        defaultFontMetrics = this.getToolkit().getFontMetrics(defaultFont);
        boldFont = new Font("Helvetica", Font.BOLD, 11);
        boldFontMetrics = this.getToolkit().getFontMetrics(boldFont);
        smallFont = new Font("Helvetica", Font.PLAIN, 9);
        smallFontMetrics = this.getToolkit().getFontMetrics(smallFont);
        String[] welcomeImageNames = { "logo telemap cuadrado.gif", "logo_transparent.gif" };
        welcomeImages = VideoAppletHttpLoadImage.execute(this, welcomeImageNames);
        this.setLayout(null);
        insets = this.getInsets();
        Button optionsButton = new Button("Options");
        optionsButton.setFont(smallFont);
        this.add(optionsButton);
        optionsButton.addMouseListener(this);
        Dimension optionsButtonDimension = optionsButton.getMinimumSize();
        int buttonX = appletDimension.width - optionsButtonDimension.width;
        int buttonY = appletDimension.height - optionsButtonDimension.height;
        optionsButton.setBounds(buttonX - insets.right, buttonY - insets.bottom, optionsButtonDimension.width - 2, optionsButtonDimension.height - 2);
        messageBoxWidth = appletDimension.width;
        messageBoxHeight = 3 + defaultFontMetrics.getHeight() + 3;
        int optionsHeight = 2 + optionsButtonDimension.height + 1;
        if (optionsHeight > messageBoxHeight) messageBoxHeight = optionsHeight;
        timeLabel = new Label("XXX  XX XXX XXXX XX:XX:XX");
        timeLabel.setAlignment(Label.RIGHT);
        timeLabel.setBackground(C_InitialBgColor);
        timeLabel.setForeground(C_InitialFgColor);
        timeLabel.setFont(smallFont);
        timeLabel.setVisible(false);
        this.add(timeLabel);
    }

    public void start() {
        intGC.setColor(C_InitialBgColor);
        intGC.fillRect(0, 0, appletDimension.width, appletDimension.height);
        intGC.setColor(C_InitialFgColor);
        intGC.setFont(defaultFont);
        intGC.drawImage(welcomeImages[0], (appletDimension.width - welcomeImages[0].getWidth(this)) / 2, (appletDimension.height - messageBoxHeight - welcomeImages[0].getHeight(this)) / 2, this);
        intGC.setColor(C_InitialNightColor);
        intGC.drawLine(1, appletDimension.height - messageBoxHeight - 1, appletDimension.width - 2, appletDimension.height - messageBoxHeight - 1);
        intGC.drawLine(1, appletDimension.height - messageBoxHeight - 3, appletDimension.width - 2, appletDimension.height - messageBoxHeight - 3);
        intGC.setColor(C_InitialFgColor);
        videoServerHostname = this.getCodeBase().getHost();
        if (videoServerHostname.length() == 0) log("Error: Applet MUST BE loaded from network - not from disk");
        videoServerHttpPort = this.getCodeBase().getPort();
        this.setLayout(null);
        appletThread = new Thread(this);
        appletThread.start();
    }

    public void run() {
        boolean directConnection = false;
        DataInputStream din = null;
        DataOutputStream dout = null;
        VideoAppletHttpRequest httpRequest = null;
        String imageServerTime = null;
        int currentSequenceNumber = -2;
        long lastDone = -1;
        int receiveCount = 0;
        VideoAppletSplitDescriptor splitDescriptor = null;
        VideoAppletSplitBuffer splitBuffer = null;
        int lastDisplayThreadType = 0;
        VideoAppletFullDisplayThread fullDisplayThread = null;
        VideoAppletSplitDisplayThread splitDisplayThread = null;
        long totalFullImageReceiveBytes = 0;
        long totalFrameCount = 0;
        while (true) {
            try {
                messageBoxDrawString("Connecting to Camera ...");
                this.repaint();
                try {
                    videoSocket = new Socket(videoServerHostname, videoServerPort);
                    din = new DataInputStream(new BufferedInputStream(videoSocket.getInputStream(), 8192));
                    dout = new DataOutputStream(new BufferedOutputStream(videoSocket.getOutputStream(), 512));
                    log(">>> Direct Connection Ok");
                    directConnection = true;
                    messageBoxDrawString("... direct connected");
                    this.repaint();
                } catch (Exception ex) {
                    httpRequest = new VideoAppletHttpRequest(this);
                    din = httpRequest.transmit("va-hello.gif");
                    int helloResponse = din.readInt();
                    din.close();
                    if (helloResponse != 5776) throw new IOException("error: invalid hello response received from http server");
                    log(">>> HTTP Connection Ok");
                    directConnection = false;
                    messageBoxDrawString("... HTTP connected");
                    this.repaint();
                }
                splitDescriptor = new VideoAppletSplitDescriptor(appletDimension, messageBoxHeight);
                if (directConnection) {
                    while (!splitDescriptor.readServerInfo(dout, din)) ;
                    {
                        log(">>> no server metadata ready. waiting ...");
                        try {
                            Thread.currentThread().sleep(5000);
                        } catch (Exception e) {
                        }
                    }
                    videoConnectionInfo = "Direct Connected";
                } else {
                    while (!splitDescriptor.readServerInfo(httpRequest)) ;
                    {
                        log(">>> no server metadata ready. waiting ...");
                        try {
                            Thread.currentThread().sleep(5000);
                        } catch (Exception e) {
                        }
                    }
                    videoConnectionInfo = "HTTP Connected";
                }
                splitDescriptor.convertToAppletCoordinates();
                splitBuffer = new VideoAppletSplitBuffer(splitDescriptor, C_TransferSplitLimit);
                messageBoxClear(C_InitialBgColor);
                layoutLogo(splitDescriptor);
                layoutTimeLabel(splitDescriptor);
                this.repaint();
                long startTime = System.currentTimeMillis();
                while (true) {
                    if (directConnection) splitBuffer.makeSplitRequest(dout, optDiffRatio); else din = splitBuffer.makeSplitRequest(httpRequest, optDiffRatio);
                    if (lastDone != -1) {
                        long sleepTime = lastDone + optUpdateDelay - System.currentTimeMillis();
                        if (sleepTime > 0) {
                            try {
                                Thread.currentThread().sleep(sleepTime);
                            } catch (Exception e) {
                            }
                        } else {
                            try {
                                Thread.currentThread().sleep(10);
                            } catch (Exception e) {
                            }
                        }
                    }
                    lastDone = System.currentTimeMillis();
                    byte serverCommand = din.readByte();
                    imageServerTime = din.readUTF();
                    switch(serverCommand) {
                        case 99:
                            totalFrameCount++;
                            if (updateTimeLabel(imageServerTime)) repaint();
                            break;
                        case 10:
                            totalFrameCount++;
                            currentSequenceNumber = din.readInt();
                            int imageWidth = din.readInt();
                            int imageHeight = din.readInt();
                            int imageLength = din.readInt();
                            byte[] streamedJpgImage = new byte[imageLength];
                            din.readFully(streamedJpgImage);
                            totalFullImageReceiveBytes = totalFullImageReceiveBytes + (4 * 4) + imageLength;
                            splitBuffer.markFullImageUpdate(currentSequenceNumber, imageLength);
                            if ((lastDisplayThreadType == 1) && (fullDisplayThread != null)) {
                                try {
                                    fullDisplayThread.join();
                                } catch (InterruptedException ie) {
                                }
                            }
                            if ((lastDisplayThreadType == 2) && (splitDisplayThread != null)) {
                                try {
                                    splitDisplayThread.join();
                                } catch (InterruptedException ie) {
                                }
                            }
                            fullDisplayThread = new VideoAppletFullDisplayThread(this, splitDescriptor, streamedJpgImage);
                            lastDisplayThreadType = 1;
                            fullDisplayThread.start();
                            messageBoxDrawReceiveImage(receiveCount, true);
                            updateTimeLabel(imageServerTime);
                            receiveCount++;
                            break;
                        case 11:
                            totalFrameCount++;
                            currentSequenceNumber = splitBuffer.readSplitResponse(din, this);
                            if ((lastDisplayThreadType == 1) && (fullDisplayThread != null)) {
                                try {
                                    fullDisplayThread.join();
                                } catch (InterruptedException ie) {
                                }
                            }
                            if ((lastDisplayThreadType == 2) && (splitDisplayThread != null)) {
                                try {
                                    splitDisplayThread.join();
                                } catch (InterruptedException ie) {
                                }
                            }
                            splitDisplayThread = new VideoAppletSplitDisplayThread(this, splitDescriptor, new VideoAppletSplitBuffer(splitBuffer));
                            lastDisplayThreadType = 2;
                            splitDisplayThread.start();
                            messageBoxDrawReceiveImage(receiveCount, false);
                            updateTimeLabel(imageServerTime);
                            receiveCount++;
                            break;
                        default:
                            throw new IOException("error: invalid server command received: " + serverCommand);
                    }
                    if (!directConnection) {
                        din.close();
                        din = null;
                    }
                    transferSpeed = ((float) (splitBuffer.getTotalBytes() + totalFullImageReceiveBytes)) / ((float) (System.currentTimeMillis() - startTime));
                    frameSpeed = (((float) totalFrameCount) * 1000.0f) / ((float) (System.currentTimeMillis() - startTime));
                    averageSplitSize = splitBuffer.getAverageSplitSize();
                    if (averageSplitSize < 0) averageSplitSize = 0;
                    averageFullSize = splitBuffer.getAverageFullSize();
                    if (averageFullSize < 0) averageFullSize = 0;
                }
            } catch (Exception ex) {
                log("error in applet main thread: " + ex);
                ex.printStackTrace();
            }
            try {
                if (din != null) din.close();
            } catch (Exception cex) {
            }
            try {
                if (dout != null) dout.close();
            } catch (Exception cex) {
            }
            try {
                if (videoSocket != null) videoSocket.close();
            } catch (Exception cex) {
            }
            din = null;
            dout = null;
            videoSocket = null;
            videoConnectionInfo = "Not Connected";
            totalFullImageReceiveBytes = 0;
            transferSpeed = 0;
            totalFrameCount = 0;
            frameSpeed = 0;
            averageSplitSize = 0;
            averageFullSize = 0;
            try {
                Thread.currentThread().sleep(5000);
            } catch (Exception e) {
            }
        }
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        synchronized (doubleBuffer) {
            g.drawImage(doubleBuffer, 0, 0, null);
            super.paint(g);
            Dimension d = getSize();
            g.setColor(getForeground());
            g.drawRect(1, 1, d.width - 1, d.height - 1);
        }
    }

    public void stop() {
        appletThread.stop();
        try {
            if (videoSocket != null) videoSocket.close();
        } catch (Exception ex) {
        }
        timeLabel.setVisible(false);
    }

    public static void log(String msg) {
        System.out.println(msg);
    }

    private void layoutLogo(VideoAppletSplitDescriptor splitDescriptor) {
        int x = ((appletDimension.width + splitDescriptor.getWidth()) / 2) - this.getLogoImage().getWidth(this);
        int y = ((appletDimension.height - messageBoxHeight - splitDescriptor.getHeight()) / 2);
        logoDrawPosition = new Point(x, y);
    }

    private void layoutTimeLabel(VideoAppletSplitDescriptor splitDescriptor) {
        if (splitDescriptor == null) return;
        Dimension timeLabelDimension = timeLabel.getMinimumSize();
        int x = (appletDimension.width + splitDescriptor.getWidth()) / 2;
        x = x - timeLabelDimension.width;
        int y = (appletDimension.height - messageBoxHeight + splitDescriptor.getHeight()) / 2;
        timeLabel.setBounds(x - insets.right + 2, y - insets.bottom, timeLabelDimension.width - 2, timeLabelDimension.height - 2);
        timeLabel.setVisible(true);
    }

    private boolean updateTimeLabel(String s) {
        Color oldFgColor = timeLabel.getForeground();
        String oldText = timeLabel.getText();
        Color newFgColor;
        if (s.length() > 0) newFgColor = C_InitialFgColor; else newFgColor = C_InitialErrorColor;
        if (oldFgColor.equals(newFgColor) && (s.compareTo(oldText) == 0)) return false;
        timeLabel.setForeground(newFgColor);
        if (s.length() > 0) timeLabel.setText(s);
        return true;
    }

    private void messageBoxClear(Color bgColor) {
        synchronized (doubleBuffer) {
            intGC.setColor(bgColor);
            intGC.fillRect(0, appletDimension.height - messageBoxHeight, appletDimension.width, messageBoxHeight);
        }
    }

    private void messageBoxDrawString(String s) {
        messageBoxClear(Color.red);
        synchronized (doubleBuffer) {
            intGC.setColor(C_InitialFgColor);
            intGC.drawString(s, 9, appletDimension.height - defaultFontMetrics.getDescent() - 3);
        }
    }

    public void messageBoxDrawReceiveImage(int imageCount, boolean fullImage) {
        int imageBoxes = (imageCount % 25) + 1;
        if (imageBoxes == 1) messageBoxClear(C_InitialBgColor);
        int yOffset = ((messageBoxHeight / 2) - 3) + (appletDimension.height - messageBoxHeight);
        int xOffset = (imageBoxes * 7) + 2;
        synchronized (doubleBuffer) {
            intGC.setColor(C_InitialInfoColor);
            if (fullImage) intGC.fillRect(xOffset, yOffset, 6, 6); else intGC.drawRect(xOffset, yOffset, 5, 5);
        }
    }

    public void mouseClicked(MouseEvent mouseEvent) {
        if (optionsWindow != null) {
            optionsWindow.setVisible(false);
            optionsWindow.dispose();
        }
        optionsWindow = new VideoAppletOptionsWindow(this, "Video Applet Options");
    }

    public void mousePressed(MouseEvent mouseEvent) {
    }

    public void mouseReleased(MouseEvent mouseEvent) {
    }

    public void mouseEntered(MouseEvent mouseEvent) {
    }

    public void mouseExited(MouseEvent mouseEvent) {
    }
}

class VideoAppletOptionsWindow extends Frame implements Runnable, WindowListener, AdjustmentListener, MouseListener, ActionListener {

    VideoApplet applet;

    Label connectionInfoLabel;

    Label speedInfoLabel;

    long updateDelayValue;

    Scrollbar updateDelayScrollbar;

    Label updateDelayLabel;

    float imageDiffRatioValue;

    Scrollbar imageDiffRatioScrollbar;

    Label imageDiffRatioLabel;

    Label splitLimitLabel;

    Label splitSizeLabel;

    boolean drawSplitFramesValue;

    Checkbox drawSplitFramesCheckbox;

    Button okButton = new Button("Ok");

    Button applyButton = new Button("Apply");

    Button defaultButton = new Button("Defaults");

    Thread updateThread = null;

    DecimalFormat myFormatter = new DecimalFormat("#0.00");

    VideoAppletOptionsWindow(VideoApplet app, String title) {
        super(title);
        setBounds(300, 300, 200, 200);
        this.addWindowListener(this);
        this.setForeground(Color.white);
        this.setBackground(Color.black);
        DecimalFormatSymbols df = new DecimalFormatSymbols();
        df.setDecimalSeparator('.');
        myFormatter = new DecimalFormat("#0.00", df);
        applet = app;
        setLayout(new GridLayout(7, 3, 3, 3));
        this.setFont(applet.defaultFont);
        MenuBar menuBar = new MenuBar();
        Menu helpMenu = new Menu("Info");
        MenuItem aboutMenuItem = new MenuItem("About Vide Applet");
        aboutMenuItem.setActionCommand("About");
        helpMenu.add(aboutMenuItem);
        menuBar.setHelpMenu(helpMenu);
        this.setMenuBar(menuBar);
        helpMenu.addActionListener(this);
        add(new Label("  Connection to Camera: "));
        connectionInfoLabel = new Label(applet.getConnectionInfo());
        add(connectionInfoLabel);
        speedInfoLabel = new Label();
        speedInfoLabel.setText(" " + myFormatter.format(applet.getFrameSpeed()) + " Frames/Sec (" + myFormatter.format(applet.getTransferSpeed()) + " kB)");
        add(speedInfoLabel);
        updateDelayValue = applet.getMinUpdateDelay();
        add(new Label("  Update Delay: "));
        updateDelayScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, (int) (updateDelayValue / 10), 1, 2, 501);
        add(updateDelayScrollbar);
        updateDelayLabel = new Label(" " + updateDelayValue + " Milliseconds");
        add(updateDelayLabel);
        updateDelayScrollbar.addAdjustmentListener(this);
        imageDiffRatioValue = applet.getImageDiffRatio();
        add(new Label("  Image Diff Ratio: "));
        imageDiffRatioScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, (int) (imageDiffRatioValue * 100), 1, 1, 1001);
        add(imageDiffRatioScrollbar);
        imageDiffRatioLabel = new Label(" " + imageDiffRatioValue + " %");
        add(imageDiffRatioLabel);
        imageDiffRatioScrollbar.addAdjustmentListener(this);
        add(new Label("  Image Split Limit: "));
        splitLimitLabel = new Label("" + applet.getVideoSplitInfo() + " Splits");
        add(splitLimitLabel);
        splitSizeLabel = new Label("" + applet.getVideoSplitInfo() + " x " + myFormatter.format((float) applet.getAverageSplitSize() / 1000.0f) + " / " + myFormatter.format((float) applet.getAverageFullSize() / 1000.0f) + " kB");
        add(splitSizeLabel);
        drawSplitFramesValue = applet.getDrawSplitFrames();
        drawSplitFramesCheckbox = new Checkbox(null, drawSplitFramesValue);
        add(new Label("  Draw Split Frames: "));
        add(drawSplitFramesCheckbox);
        add(new Label(" "));
        okButton.setForeground(Color.black);
        add(okButton);
        okButton.addMouseListener(this);
        applyButton.setForeground(Color.black);
        add(applyButton);
        applyButton.addMouseListener(this);
        defaultButton.setForeground(Color.black);
        add(defaultButton);
        defaultButton.addMouseListener(this);
        Label l1 = new Label("  © 2001 by David Fischer");
        l1.setForeground(applet.C_InitialInfoColor);
        l1.setFont(applet.smallFont);
        add(l1);
        Label l2 = new Label(" ");
        l2.setFont(applet.smallFont);
        add(l2);
        Label l3 = new Label(" ");
        l3.setFont(applet.smallFont);
        add(l3);
        pack();
        show();
        updateThread = new Thread(this);
        updateThread.start();
    }

    public void run() {
        while (true) {
            try {
                Thread.currentThread().sleep(2000);
            } catch (Exception e) {
            }
            connectionInfoLabel.setText(applet.getConnectionInfo());
            speedInfoLabel.setText(" " + myFormatter.format(applet.getFrameSpeed()) + " Frames/Sec (" + myFormatter.format(applet.getTransferSpeed()) + " kB)");
            splitLimitLabel.setText("" + applet.getVideoSplitInfo() + " Splits");
            splitSizeLabel.setText("" + applet.getVideoSplitInfo() + " x " + myFormatter.format((float) applet.getAverageSplitSize() / 1000.0f) + " / " + myFormatter.format((float) applet.getAverageFullSize() / 1000.0f) + " kB");
            repaint();
        }
    }

    private static void log(String msg) {
        VideoApplet.log(msg);
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (e.getSource() == imageDiffRatioScrollbar) {
            int v = ((Scrollbar) e.getSource()).getValue();
            imageDiffRatioValue = ((float) v / 100.f);
            imageDiffRatioLabel.setText(" " + imageDiffRatioValue + " %");
            repaint();
        }
        if (e.getSource() == updateDelayScrollbar) {
            int v = ((Scrollbar) e.getSource()).getValue();
            updateDelayValue = v * 10;
            updateDelayLabel.setText(" " + updateDelayValue + " Milliseconds");
            repaint();
        }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        updateThread.stop();
        setVisible(false);
        dispose();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        if ((e.getSource() == okButton) || (e.getSource() == applyButton)) {
            drawSplitFramesValue = drawSplitFramesCheckbox.getState();
            applet.setMinUpdateDelay(updateDelayValue);
            applet.setImageDiffRatio(imageDiffRatioValue);
            applet.setDrawSplitFrames(drawSplitFramesValue);
            if (e.getSource() == okButton) {
                updateThread.stop();
                this.setVisible(false);
            }
            return;
        }
        if (e.getSource() == defaultButton) {
            updateDelayValue = applet.C_MinUpdateDelay;
            updateDelayScrollbar.setValue((int) (updateDelayValue / 10));
            updateDelayLabel.setText(" " + updateDelayValue + " Milliseconds");
            imageDiffRatioValue = applet.C_DiffRatio;
            imageDiffRatioScrollbar.setValue((int) (imageDiffRatioValue * 100));
            imageDiffRatioLabel.setText(" " + imageDiffRatioValue + " %");
            drawSplitFramesValue = applet.C_DrawSplitFrames;
            drawSplitFramesCheckbox.setState(drawSplitFramesValue);
            repaint();
            return;
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        log("Action Command = " + actionCommand);
        if (actionCommand.compareTo("About") == 0) {
            VideoAppletAboutDialog aboutDialog = new VideoAppletAboutDialog(this);
            return;
        }
    }
}

class VideoAppletAboutDialog extends Dialog implements ActionListener {

    VideoAppletAboutDialog(Frame parent) {
        super(parent, "About Video Applet", true);
        Point parloc = parent.getLocation();
        setLocation(parloc.x + 30, parloc.y + 30);
        this.setForeground(Color.white);
        this.setBackground(Color.black);
        setLayout(new FlowLayout());
        Label l1 = new Label(" Video Applet Version V1.0 ");
        add(l1);
        String text = "Copyright 2001 by David Fischer, Bern, Switzerland\r\n" + "All Rights Reserved\r\n" + "For further information e-mail to fischer@d-fischer.com";
        TextArea textArea = new TextArea(text, 3, 60, TextArea.SCROLLBARS_NONE);
        textArea.setEditable(false);
        textArea.setForeground(Color.black);
        textArea.setBackground(Color.white);
        add(textArea);
        Button button = new Button("Ok");
        button.setForeground(Color.black);
        add(button);
        button.addActionListener(this);
        pack();
        show();
    }

    public void actionPerformed(ActionEvent e) {
        setVisible(false);
        dispose();
    }
}

class VideoAppletRecordStartButton extends Button implements MouseListener, Runnable {

    public static final int STATE_DISABLED = 0;

    public static final int STATE_STOPPED = 1;

    public static final int STATE_STARTED = 2;

    private VideoApplet applet;

    private Image i[];

    private int state = STATE_STOPPED;

    private int w = 0;

    private int h = 0;

    private int iw, ih;

    Thread flashThread = null;

    boolean mouseStopOver = false;

    public VideoAppletRecordStartButton(VideoApplet app, Image im[]) {
        super(" ");
        applet = app;
        i = new Image[im.length];
        i = im;
        iw = i[0].getWidth(this);
        ih = i[0].getHeight(this);
        setSize(iw, ih);
        addMouseListener(this);
    }

    public void setState(int s) {
        if (s == state) return;
        state = s;
        switch(state) {
            case STATE_DISABLED:
                super.setEnabled(false);
                stopFlashThread();
                break;
            case STATE_STOPPED:
                super.setEnabled(true);
                stopFlashThread();
                break;
            case STATE_STARTED:
                super.setEnabled(true);
                startFlashThread();
                break;
            default:
                break;
        }
        repaint();
    }

    public int getState() {
        return state;
    }

    public void run() {
        while (true) {
            try {
                Thread.currentThread().sleep(1000);
                repaint();
                if (Thread.currentThread().isInterrupted()) return;
            } catch (InterruptedException ie) {
                return;
            }
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(iw, ih);
    }

    public Dimension getMinimumSize() {
        return new Dimension(iw, ih);
    }

    public void setBounds(int x, int y, int width, int height) {
        w = width;
        h = height;
        super.setBounds(x, y, width, height);
    }

    public void setSize(int width, int height) {
        w = width;
        h = height;
        super.setSize(width, height);
    }

    public void setEnabled(boolean enabled) {
        if (!enabled) {
            stopFlashThread();
            state = STATE_DISABLED;
        } else state = STATE_STOPPED;
        repaint();
        super.setEnabled(enabled);
    }

    public void paint(Graphics g) {
        boolean flashThreadAlive = false;
        if (flashThread != null) flashThreadAlive = flashThread.isAlive();
        if ((state == STATE_STARTED) && (flashThreadAlive)) {
            long currentTime = System.currentTimeMillis() / 1000;
            g.drawImage(i[1 + (int) (currentTime % 2)], 0, 0, w, h, this);
        } else {
            if (state == STATE_STOPPED) {
                if (mouseStopOver) g.drawImage(i[3], 0, 0, w, h, this); else g.drawImage(i[1], 0, 0, w, h, this);
            } else {
                g.drawImage(i[state], 0, 0, w, h, this);
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (state == STATE_STARTED) {
            state = STATE_STOPPED;
            stopFlashThread();
            repaint();
            return;
        }
        if (state == STATE_STOPPED) {
            state = STATE_STARTED;
            startFlashThread();
            repaint();
            return;
        }
    }

    private void startFlashThread() {
        flashThread = new Thread(this);
        flashThread.start();
    }

    private void stopFlashThread() {
        if (flashThread != null) if (flashThread.isAlive()) flashThread.interrupt();
    }

    public void mouseEntered(MouseEvent e) {
        if (state == STATE_STOPPED) if (i.length >= 3) {
            mouseStopOver = true;
            repaint();
        }
        switch(state) {
            case STATE_STOPPED:
                applet.showStatus("Video Applet: Start Record");
                break;
            case STATE_STARTED:
                applet.showStatus("Video Applet: Stop Record");
                break;
            default:
                break;
        }
    }

    public void mouseExited(MouseEvent e) {
        if (state == STATE_STOPPED) if (i.length >= 3) {
            mouseStopOver = false;
            repaint();
        }
        applet.showStatus("Video Applet Running");
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}

class VideoAppletRecordDownloadButton extends Button implements MouseListener {

    public static final int STATE_DISABLED = 0;

    public static final int STATE_DOWNLOAD_READY = 1;

    public static final int STATE_DOWNLOAD_DONE = 2;

    private VideoApplet applet;

    private Image i[];

    private int state = STATE_DISABLED;

    private int w = 0;

    private int h = 0;

    private int iw, ih;

    boolean mouseOver = false;

    public VideoAppletRecordDownloadButton(VideoApplet app, Image im[]) {
        super(" ");
        applet = app;
        i = new Image[im.length];
        i = im;
        iw = i[0].getWidth(this);
        ih = i[0].getHeight(this);
        setSize(iw, ih);
        addMouseListener(this);
    }

    public void setState(int s) {
        if (s == state) return;
        state = s;
        super.setEnabled(state != STATE_DISABLED);
        repaint();
    }

    public Dimension getPreferredSize() {
        return new Dimension(iw, ih);
    }

    public Dimension getMinimumSize() {
        return new Dimension(iw, ih);
    }

    public void setBounds(int x, int y, int width, int height) {
        w = width;
        h = height;
        super.setBounds(x, y, width, height);
    }

    public void setSize(int width, int height) {
        w = width;
        h = height;
        super.setSize(width, height);
    }

    public void setEnabled(boolean e) {
        state = STATE_DISABLED;
        repaint();
        super.setEnabled(e);
    }

    public void paint(Graphics g) {
        if (mouseOver && (state != STATE_DISABLED)) {
            g.drawImage(i[state + 2], 0, 0, w, h, this);
        } else g.drawImage(i[state], 0, 0, w, h, this);
    }

    public void mouseClicked(MouseEvent e) {
        if (state == STATE_DOWNLOAD_READY) {
            state = STATE_DOWNLOAD_DONE;
            repaint();
        }
    }

    public void mouseEntered(MouseEvent e) {
        if (state != STATE_DISABLED) {
            mouseOver = true;
            repaint();
        }
    }

    public void mouseExited(MouseEvent e) {
        if (state != STATE_DISABLED) {
            mouseOver = false;
            repaint();
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}

class VideoAppletHttpLoadImage {

    public static Image[] execute(VideoApplet applet, String[] imageFileName) {
        int maxImage = imageFileName.length;
        Image[] image = new Image[maxImage];
        MediaTracker mediaTracker = new MediaTracker(applet);
        for (int x = 0; x < maxImage; x++) {
            image[x] = applet.getImage(applet.getDocumentBase(), imageFileName[x]);
            mediaTracker.addImage(image[x], x);
        }
        try {
            mediaTracker.waitForAll();
        } catch (InterruptedException ie) {
        }
        return image;
    }
}

class VideoAppletHttpRequest {

    String serverHost = null;

    int serverPort = -1;

    String urlString = null;

    DataInputStream din = null;

    public VideoAppletHttpRequest(VideoApplet applet) {
        serverHost = applet.getServerHostname();
        serverPort = applet.getServerHttpPort();
        urlString = "http://" + serverHost + ":" + serverPort + "/";
    }

    public DataInputStream transmit(String requestFile) throws MalformedURLException, IOException {
        return transmit(requestFile, null);
    }

    public DataInputStream transmit(String requestFile, String requestParam) throws MalformedURLException, IOException {
        din = null;
        urlString = "http://" + serverHost + ":" + serverPort + "/";
        String connectString = urlString + requestFile;
        if (requestParam == null) connectString = connectString + "?dummy=" + System.currentTimeMillis(); else connectString = connectString + "?" + requestParam + "&dummy=" + System.currentTimeMillis();
        URL url = new URL(connectString);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        din = new DataInputStream(new BufferedInputStream(connection.getInputStream()));
        return din;
    }

    private static void log(String msg) {
        VideoApplet.log(msg);
    }
}
