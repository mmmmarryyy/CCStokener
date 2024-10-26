package Action.textMode;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import nsequence.Server;
import org.w3c.dom.Document;
import Action.lineMode.lineModeCommon.relaxer.FEATURE;
import Action.lineMode.lineModeCommon.relaxer.GROUP;
import Action.lineMode.lineModeCommon.relaxer.LineMode;
import Action.lineMode.lineModeCommon.relaxer.SEGMENT;
import Action.lineMode.lineModeCommon.relaxer.TMGFF;
import Action.species.InitXml;
import process.primer3.Primer3;
import process.primer3.Primer3Output;
import process.primer3.Primer3Param;

public class Primer3Manager {

    public static final int DEFAULT_MARGIN_LENGTH = 30;

    public static final int DEFAULT_PRIMER_PRODUCT_OPT_SIZE = 500;

    public static final int DEFAULT_PRIMER_PRODUCT_MAX_SIZE = 1000;

    public static final int DEFAULT_PRIMER_MAX_SIZE = 27;

    public Primer3Manager() {
        init();
    }

    private String primer3BinPath;

    private String workDirPath;

    private String wholeSequence;

    private static final String servletName = "/gps/GenePrimerDesignServlet";

    private int marginLength;

    private int overlapLength;

    private int upstreamSize;

    private int downstreamSize;

    private String[] targetRegions;

    private int primerProductMinSize;

    private int primerProductMaxSize;

    private int primerProductOptSize;

    private double primerMaxEndStability;

    private double primerMaxMispriming;

    private double primerPairMaxMispriming;

    private int primerMinSize;

    private int primerOptSize;

    private int primerMaxSize;

    private double primerMinTm;

    private double primerOptTm;

    private double primerMaxTm;

    private double primerMaxDiffTm;

    private double primerMinGc;

    private double primerOptGcPercent;

    private double primerMaxGc;

    private double primerSelfAny;

    private double primerSelfEnd;

    private int primerNumNsAccepted;

    private int primerMaxPolyX;

    private int primerGcClamp;

    /**
	 * @return Returns the primerMaxEndStability.
	 */
    public double getPrimerMaxEndStability() {
        return primerMaxEndStability;
    }

    /**
	 * @param primerMaxEndStability The primerMaxEndStability to set.
	 */
    public void setPrimerMaxEndStability(double primerMaxEndStablity) {
        this.primerMaxEndStability = primerMaxEndStablity;
    }

    /**
	 * @return Returns the primerMaxGc.
	 */
    public double getPrimerMaxGc() {
        return primerMaxGc;
    }

    /**
	 * @param primerMaxGc The primerMaxGc to set.
	 */
    public void setPrimerMaxGc(double primerMaxGc) {
        this.primerMaxGc = primerMaxGc;
    }

    /**
	 * @return Returns the primerMaxMispriming.
	 */
    public double getPrimerMaxMispriming() {
        return primerMaxMispriming;
    }

    /**
	 * @param primerMaxMispriming The primerMaxMispriming to set.
	 */
    public void setPrimerMaxMispriming(double primerMaxMispriming) {
        this.primerMaxMispriming = primerMaxMispriming;
    }

    /**
	 * @return Returns the primerMaxPolyX.
	 */
    public int getPrimerMaxPolyX() {
        return primerMaxPolyX;
    }

    /**
	 * @param primerMaxPolyX The primerMaxPolyX to set.
	 */
    public void setPrimerMaxPolyX(int primerMaxPolyX) {
        this.primerMaxPolyX = primerMaxPolyX;
    }

    /**
	 * @return Returns the primerMaxSize.
	 */
    public int getPrimerMaxSize() {
        return primerMaxSize;
    }

    /**
	 * @param primerMaxSize The primerMaxSize to set.
	 */
    public void setPrimerMaxSize(int primerMaxSize) {
        this.primerMaxSize = primerMaxSize;
    }

    /**
	 * @return Returns the primerMaxTm.
	 */
    public double getPrimerMaxTm() {
        return primerMaxTm;
    }

    /**
	 * @param primerMaxTm The primerMaxTm to set.
	 */
    public void setPrimerMaxTm(double primerMaxTm) {
        this.primerMaxTm = primerMaxTm;
    }

    /**
	 * @return Returns the primerMinGc.
	 */
    public double getPrimerMinGc() {
        return primerMinGc;
    }

    /**
	 * @param primerMinGc The primerMinGc to set.
	 */
    public void setPrimerMinGc(double primerMinGc) {
        this.primerMinGc = primerMinGc;
    }

    /**
	 * @return Returns the primerMinSize.
	 */
    public int getPrimerMinSize() {
        return primerMinSize;
    }

    /**
	 * @param primerMinSize The primerMinSize to set.
	 */
    public void setPrimerMinSize(int primerMinSize) {
        this.primerMinSize = primerMinSize;
    }

    /**
	 * @return Returns the primerMinTm.
	 */
    public double getPrimerMinTm() {
        return primerMinTm;
    }

    /**
	 * @param primerMinTm The primerMinTm to set.
	 */
    public void setPrimerMinTm(double primerMinTm) {
        this.primerMinTm = primerMinTm;
    }

    /**
	 * @return Returns the primerNumNsAccepted.
	 */
    public int getPrimerNumNsAccepted() {
        return primerNumNsAccepted;
    }

    /**
	 * @param primerNumNsAccepted The primerNumNsAccepted to set.
	 */
    public void setPrimerNumNsAccepted(int primerNumNsAccepted) {
        this.primerNumNsAccepted = primerNumNsAccepted;
    }

    /**
	 * @return Returns the primerOptGcPercent.
	 */
    public double getPrimerOptGcPercent() {
        return primerOptGcPercent;
    }

    /**
	 * @param primerOptGcPercent The primerOptGcPercent to set.
	 */
    public void setPrimerOptGcPercent(double primerOptGcPercent) {
        this.primerOptGcPercent = primerOptGcPercent;
    }

    /**
	 * @return Returns the primerOptSize.
	 */
    public int getPrimerOptSize() {
        return primerOptSize;
    }

    /**
	 * @param primerOptSize The primerOptSize to set.
	 */
    public void setPrimerOptSize(int primerOptSize) {
        this.primerOptSize = primerOptSize;
    }

    /**
	 * @return Returns the primerOptTm.
	 */
    public double getPrimerOptTm() {
        return primerOptTm;
    }

    /**
	 * @param primerOptTm The primerOptTm to set.
	 */
    public void setPrimerOptTm(double primerOptTm) {
        this.primerOptTm = primerOptTm;
    }

    /**
	 * @return Returns the primerPairMaxMispriming.
	 */
    public double getPrimerPairMaxMispriming() {
        return primerPairMaxMispriming;
    }

    /**
	 * @param primerPairMaxMispriming The primerPairMaxMispriming to set.
	 */
    public void setPrimerPairMaxMispriming(double primerPairMaxMispriming) {
        this.primerPairMaxMispriming = primerPairMaxMispriming;
    }

    /**
	 * @return Returns the primerProductOptSize.
	 */
    public int getPrimerProductOptSize() {
        return primerProductOptSize;
    }

    /**
	 * @param primerProductOptSize The primerProductOptSize to set.
	 */
    public void setPrimerProductOptSize(int primerProductOptSize) {
        this.primerProductOptSize = primerProductOptSize;
    }

    /**
	 * @return Returns the primerSelfAny.
	 */
    public double getPrimerSelfAny() {
        return primerSelfAny;
    }

    /**
	 * @param primerSelfAny The primerSelfAny to set.
	 */
    public void setPrimerSelfAny(double primerSelfAny) {
        this.primerSelfAny = primerSelfAny;
    }

    /**
	 * @return Returns the primerSelfEnd.
	 */
    public double getPrimerSelfEnd() {
        return primerSelfEnd;
    }

    /**
	 * @param primerSelfEnd The primerSelfEnd to set.
	 */
    public void setPrimerSelfEnd(double primerSelfEnd) {
        this.primerSelfEnd = primerSelfEnd;
    }

    /**
	 * @return Returns the marginLength.
	 */
    public int getMarginLength() {
        return marginLength;
    }

    /**
	 * @param marginLength The marginLength to set.
	 */
    public void setMarginLength(int marginLength) {
        this.marginLength = marginLength;
    }

    /**
	 * @return Returns the overlapLength.
	 */
    public int getOverlapLength() {
        return overlapLength;
    }

    /**
	 * @param overlapLength The overlapLength to set.
	 */
    public void setOverlapLength(int overlapLength) {
        this.overlapLength = overlapLength;
    }

    /**
	 * @return Returns the wholeSequence.
	 */
    public String getWholeSequence() {
        return wholeSequence;
    }

    /**
	 * @param wholeSequence The wholeSequence to set.
	 */
    public void setWholeSequence(String wholeSequence) {
        this.wholeSequence = wholeSequence;
    }

    /**
	 * @return Returns the primer3BinPath.
	 */
    public String getPrimer3BinPath() {
        return primer3BinPath;
    }

    /**
	 * @param primer3BinPath The primer3BinPath to set.
	 */
    public void setPrimer3BinPath(String primer3BinPath) {
        this.primer3BinPath = primer3BinPath;
    }

    /**
	 * @return Returns the workDirPath.
	 */
    public String getWorkDirPath() {
        return workDirPath;
    }

    /**
	 * @param workDirPath The workDirPath to set.
	 */
    public void setWorkDirPath(String tmpDirPath) {
        this.workDirPath = tmpDirPath;
    }

    /**
	 * @return Returns the primerProductMaxSize.
	 */
    public int getPrimerProductMaxSize() {
        return primerProductMaxSize;
    }

    /**
	 * @param primerProductMaxSize The primerProductMaxSize to set.
	 */
    public void setPrimerProductMaxSize(int primerProductMaxSize) {
        this.primerProductMaxSize = primerProductMaxSize;
    }

    /**
	 * @return Returns the primerProductMinSize.
	 */
    public int getPrimerProductMinSize() {
        return primerProductMinSize;
    }

    /**
	 * @param primerProductMinSize The primerProductMinSize to set.
	 */
    public void setPrimerProductMinSize(int primerProductMinSize) {
        this.primerProductMinSize = primerProductMinSize;
    }

    /**
	 * @return Returns the primerMaxDiffTm.
	 */
    public double getPrimerMaxDiffTm() {
        return primerMaxDiffTm;
    }

    /**
	 * @param primerMaxDiffTm The primerMaxDiffTm to set.
	 */
    public void setPrimerMaxDiffTm(double primerMaxDiffTm) {
        this.primerMaxDiffTm = primerMaxDiffTm;
    }

    /**
	 * @return Returns the primerGcClamp.
	 */
    public int getPrimerGcClamp() {
        return primerGcClamp;
    }

    /**
	 * @param primerGcClamp The primerGcClamp to set.
	 */
    public void setPrimerGcClamp(int primerGcClamp) {
        this.primerGcClamp = primerGcClamp;
    }

    /**
	 * @return Returns the downstreamSize.
	 */
    public int getDownstreamSize() {
        return downstreamSize;
    }

    /**
	 * @param downstreamSize The downstreamSize to set.
	 */
    public void setDownstreamSize(int downstreamFlanking) {
        this.downstreamSize = downstreamFlanking;
    }

    /**
	 * @return Returns the upstreamSize.
	 */
    public int getUpstreamSize() {
        return upstreamSize;
    }

    /**
	 * @param upstreamSize The upstreamSize to set.
	 */
    public void setUpstreamSize(int upstreamFlankingSize) {
        this.upstreamSize = upstreamFlankingSize;
    }

    /**
	 * @return Returns the targetRegions.
	 */
    public String[] getTargetRegions() {
        return targetRegions;
    }

    /**
	 * @param targetRegions The targetRegions to set.
	 */
    public void setTargetRegions(String[] targetRegions) {
        this.targetRegions = targetRegions;
    }

    /**
	 * 
	 * Document�Ɋ܂܂���`�q��̂��ׂĂ�Exon��Ώۂ�Primer3��s����B
	 * 
	 * @param doc ��`�q��ЂƂ܂�LineMode�h�L�������g
	 * @return ���ׂĂ�Exon�ɑ΂��Đ݌v���ꂽPrimer(Primer3Output)�̃��X�g
	 */
    public ArrayList exec(Document doc) {
        ArrayList outputList = null;
        LineMode lineMode = new LineMode(doc);
        TMGFF[] tmgffs = lineMode.getTMGFF();
        if (tmgffs.length != 1) return outputList;
        SEGMENT[] segments = tmgffs[0].getSEGMENT();
        if (segments.length != 1) return outputList;
        GROUP[] groups = segments[0].getGROUP();
        String sequence = wholeSequence;
        int start = (int) groups[0].getSTART();
        int end = (int) groups[0].getEND();
        ArrayList annotList = getAnnotList(groups[0].getORIENTATION(), groups[0].getFEATURE(), start, end);
        ArrayList regionList = getRegionList(annotList);
        if (groups[0].getORIENTATION().equals("-")) {
            sequence = TextModeManager.complement(wholeSequence);
        }
        if (primerProductOptSize <= 0) {
            if (primerProductMaxSize < Primer3Manager.DEFAULT_PRIMER_PRODUCT_OPT_SIZE) {
                primerProductOptSize = primerProductMaxSize;
            }
            if (primerProductMinSize > Primer3Manager.DEFAULT_PRIMER_PRODUCT_OPT_SIZE) {
                primerProductOptSize = primerProductMinSize;
            } else {
                primerProductOptSize = Primer3Manager.DEFAULT_PRIMER_PRODUCT_OPT_SIZE;
            }
        }
        ArrayList paramList = new ArrayList();
        for (int i = 0; i < regionList.size(); i++) {
            int[] region = (int[]) regionList.get(i);
            int regionCnt = (int) region[0];
            int regionStart = region[1];
            int regionEnd = region[2];
            int regionLength = regionEnd - regionStart + 1;
            if (regionLength + 2 * marginLength < primerProductOptSize) {
                String id = "Region" + regionCnt;
                int targetStart = 1 >= regionStart - marginLength ? 1 : regionStart - marginLength;
                int targetEnd = wholeSequence.length() <= regionEnd + marginLength ? wholeSequence.length() : regionEnd + marginLength;
                paramList.add(createParam(id, sequence, targetStart, targetEnd));
            } else {
                int totalCnt = (regionLength + 2 * marginLength - overlapLength) / (primerProductOptSize - overlapLength) + 1;
                int remainder = totalCnt * primerProductOptSize - (totalCnt - 1) * overlapLength - (regionEnd - regionStart + marginLength * 2);
                int tmpOverlapLength = remainder / (totalCnt - 1) + overlapLength;
                for (int j = 0; j < totalCnt; j++) {
                    String id = "Region" + regionCnt + "_" + (j + 1);
                    int targetStart = regionStart - marginLength + j * (primerProductOptSize - tmpOverlapLength);
                    int targetEnd = regionStart - marginLength + (j + 1) * primerProductOptSize - j * tmpOverlapLength;
                    targetStart = 1 >= targetStart ? 1 : targetStart;
                    targetEnd = wholeSequence.length() <= targetEnd ? wholeSequence.length() : targetEnd;
                    paramList.add(createParam(id, sequence, targetStart, targetEnd));
                }
            }
        }
        if (getPrimer3BinPath() != null && getPrimer3BinPath().length() > 0 && (new File(getPrimer3BinPath())).exists()) {
            outputList = exec(paramList);
        } else {
            try {
                outputList = execAtParentServer(paramList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        outputList = getProductSequence(outputList, sequence);
        outputList = filterPrimer(regionList, annotList, outputList);
        outputList = adjustPosition(outputList, groups[0].getORIENTATION(), start, end);
        deleteOldFiles(getWorkDirPath());
        return outputList;
    }

    /**
	 * �K�v�ȃp�����[�^��n���APrimer3��s����
	 * @param paramList Primer3�̃p�����[�^�Ɖ���z��A
	 *                   �z���ł�Product�̈ʒu��i�[�������X�g
	 * @return Primer3���ʂ̃��X�g
	 */
    private ArrayList exec(ArrayList paramList) {
        ArrayList outputList = null;
        Primer3 primer3 = new Primer3();
        primer3.setPrimer3BinPath(getPrimer3BinPath());
        primer3.setWorkDirPath(getWorkDirPath());
        primer3.setParamList(paramList);
        String inputFilePath = primer3.createInputFile();
        outputList = primer3.exec(inputFilePath, inputFilePath + ".out");
        new File(inputFilePath).delete();
        new File(inputFilePath + ".out").delete();
        return outputList;
    }

    /**
	 * �q�T�[�o����̗v����󂯁APrimer3��s����B
	 * @param paramList Primer3�̃p�����[�^�Ɖ���z��A
	 *                   �z���ł�Product�̈ʒu��i�[�������X�g
	 * @return Primer3���ʂ̃��X�g
	 */
    public ArrayList execParent(ArrayList paramList) {
        ArrayList outputList = null;
        if (getPrimer3BinPath() != null && getPrimer3BinPath().length() > 0 && (new File(getPrimer3BinPath())).exists()) {
            outputList = exec(paramList);
        } else {
            try {
                outputList = execAtParentServer(paramList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return outputList;
    }

    /**
	 * 
	 * 5'/Exon/Intron/3'�̒��ŁAGUI�őI�Ⳃꂽ��̂㊃X�g�ŕԂ��B
	 * 
	 * @param strand ��`�q��strand
	 * @param features ��`�q�Ɋ܂܂��
	 * @param start ��`�q�̊J�n�ʒu
	 * @param end ��`�q�̏I�n�ʒu
	 * @return Primer�݌v�Ώۂ�5'/Exon/Intron/3'�̃��X�g
	 */
    private ArrayList getAnnotList(String strand, FEATURE[] features, int start, int end) {
        int sequenceMargin = getMarginLength() + getPrimerMaxSize() + getPrimerProductMaxSize();
        ArrayList annotList = new ArrayList();
        for (int i = 0; i < targetRegions.length; i++) {
            if (targetRegions[i].equals("upstream")) {
                Annotation annot = new Annotation();
                annot.setAnnotName("5' Flanking Region");
                annot.setStart(sequenceMargin);
                annot.setEnd(sequenceMargin + upstreamSize);
                annotList.add(annot);
            }
        }
        int exonCount = 0;
        for (int i = 0; i < features.length; i++) {
            FEATURE feature = null;
            if (strand.equals("-")) {
                feature = features[features.length - i - 1];
            } else {
                feature = features[i];
            }
            exonCount++;
            for (int j = 0; j < targetRegions.length; j++) {
                if (targetRegions[j].equals("exon" + exonCount)) {
                    Annotation annot = new Annotation();
                    annot.setAnnotName("Exon" + exonCount);
                    if (strand.equals("-")) {
                        annot.setStart(end + sequenceMargin + upstreamSize - (int) feature.getEND());
                        annot.setEnd(end + sequenceMargin + upstreamSize - (int) feature.getSTART());
                    } else {
                        annot.setStart((int) feature.getSTART() - start + sequenceMargin + upstreamSize);
                        annot.setEnd((int) feature.getEND() - start + sequenceMargin + upstreamSize);
                    }
                    annotList.add(annot);
                }
            }
            for (int j = 0; j < targetRegions.length; j++) {
                if (targetRegions[j].equals("intron" + exonCount + "-" + (exonCount + 1))) {
                    Annotation annot = new Annotation();
                    annot.setAnnotName("Intron" + exonCount + "-" + (exonCount + 1));
                    if (strand.equals("-")) {
                        FEATURE tmpFeature = features[features.length - i - 2];
                        annot.setStart(end + sequenceMargin + upstreamSize - (int) feature.getSTART() + 1);
                        annot.setEnd(end + sequenceMargin + upstreamSize - (int) tmpFeature.getEND() - 1);
                    } else {
                        FEATURE tmpFeature = features[i + 1];
                        annot.setStart((int) feature.getEND() - start + sequenceMargin + upstreamSize + 1);
                        annot.setEnd((int) tmpFeature.getSTART() - start + sequenceMargin + upstreamSize - 1);
                    }
                    annotList.add(annot);
                }
            }
        }
        for (int i = 0; i < targetRegions.length; i++) {
            if (targetRegions[i].equals("downstream")) {
                Annotation annot = new Annotation();
                annot.setAnnotName("3' Flanking Region");
                annot.setStart(end - start + sequenceMargin + upstreamSize);
                annot.setEnd(end - start + sequenceMargin + upstreamSize + downstreamSize);
                annotList.add(annot);
            }
        }
        return annotList;
    }

    /**
	 * 
	 * Primer3�p�̃p�����[�^��ݒ肷��B
	 * 
	 * @param id ����
	 * @param sequence ��`�q�Ƃ��̎�ӗ̈�̉���z��
	 * @param targetStart ��Lsequence���ł�Primer�݌v�ΏۊJ�n�ʒu
	 * @param targetEnd ��Lsequence���ł�Primer�݌v�ΏۏI�n�ʒu
	 * @return
	 */
    public Primer3Param createParam(String id, String sequence, int targetStart, int targetEnd) {
        Primer3Param param = new Primer3Param();
        int[] target = { targetStart, targetEnd };
        param.setPrimerSequenceId(id);
        param.setSequence(sequence);
        param.setTarget(target);
        param.setPrimerProductMinSize(getPrimerProductMinSize());
        param.setPrimerProductMaxSize(getPrimerProductMaxSize());
        param.setPrimerProductOptSize(getPrimerProductOptSize());
        param.setPrimerNumReturn(1);
        param.setPrimerMaxEndStability(getPrimerMaxEndStability());
        param.setPrimerMaxMispriming(getPrimerMaxMispriming());
        param.setPrimerPairMaxMispriming(getPrimerPairMaxMispriming());
        param.setPrimerMinSize(getPrimerMinSize());
        param.setPrimerOptSize(getPrimerOptSize());
        param.setPrimerMaxSize(getPrimerMaxSize());
        param.setPrimerMinTm(getPrimerMinTm());
        param.setPrimerOptTm(getPrimerOptTm());
        param.setPrimerMaxTm(getPrimerMaxTm());
        param.setPrimerMaxDiffTm(getPrimerMaxDiffTm());
        param.setPrimerMinGc(getPrimerMinGc());
        param.setPrimerOptGcPercent(getPrimerOptGcPercent());
        param.setPrimerMaxGc(getPrimerMaxGc());
        param.setPrimerSelfAny(getPrimerSelfAny());
        param.setPrimerSelfEnd(getPrimerSelfEnd());
        param.setPrimerNumNsAccepted(getPrimerNumNsAccepted());
        param.setPrimerMaxPolyX(getPrimerMaxPolyX());
        param.setPrimerGcClamp(getPrimerGcClamp());
        return param;
    }

    /**
	 * 
	 * �p�����[�^�̏���
	 *
	 */
    private void init() {
        wholeSequence = "";
        marginLength = 30;
        overlapLength = 100;
        primerProductMinSize = 100;
        primerProductMaxSize = 1000;
        primerProductOptSize = 200;
        primerMaxEndStability = 100.0;
        primerMaxMispriming = 12.00;
        primerPairMaxMispriming = 24.00;
        primerMinSize = 18;
        primerOptSize = 20;
        primerMaxSize = 27;
        primerMinTm = 57.0;
        primerOptTm = 60.0;
        primerMaxTm = 63.0;
        primerMaxDiffTm = 100.0;
        primerMinGc = 20.0;
        primerOptGcPercent = 50.0;
        primerMaxGc = 80.0;
        primerSelfAny = 8.00;
        primerSelfEnd = 3.00;
        primerNumNsAccepted = 0;
        primerMaxPolyX = 5;
        primerGcClamp = 0;
    }

    /**
	 * 
	 * �S�������A�܂��͑���Product�ɓ����
	 * Primer�̃Z�b�g���݌v���ꂽ�ꍇ�́A�폜��
	 * �ԍ���t���ւ���B
	 * 
	 * @param regionInfoList Primer�݌v�Ώۗ̈�̃��X�g
	 * @param annotList Primer�݌v�Ώ�Exon�EIntron�EFlanking�̈�̃��X�g
	 * @param outputList �݌v���ꂽPrimer(Primer3Output)�̃��X�g
	 * @return outputList����s�v��Primer��폜�������X�g
	 */
    private ArrayList filterPrimer(ArrayList regionList, ArrayList annotList, ArrayList outputList) {
        ArrayList primerList = new ArrayList();
        for (int i = 0; i < regionList.size(); i++) {
            int[] region = (int[]) regionList.get(i);
            int regionCnt = (int) region[0];
            int regionStart = region[1];
            int regionEnd = region[2];
            boolean addFlag = false;
            for (int j = 0; j < outputList.size(); j++) {
                Primer3Output output = (Primer3Output) outputList.get(j);
                if (output.getPrimerSequenceId().equals("Region" + regionCnt) || (output.getPrimerSequenceId().startsWith("Region" + regionCnt + "_") && (output.getLeft())[0] <= regionStart - marginLength && (output.getRight())[0] >= regionEnd + marginLength)) {
                    primerList.add(output);
                    addFlag = true;
                    break;
                }
            }
            if (!addFlag) {
                int j = 0;
                while (j < outputList.size()) {
                    int maxIndex = Integer.MAX_VALUE;
                    Primer3Output output = (Primer3Output) outputList.get(j);
                    if (output.getPrimerSequenceId().startsWith("Region" + regionCnt + "_")) {
                        if (j < outputList.size() - 1) {
                            Primer3Output nextOutput = (Primer3Output) outputList.get(j + 1);
                            if ((nextOutput.getLeft())[0] <= regionStart - marginLength) {
                                j++;
                                continue;
                            }
                        }
                        if ((output.getRight())[0] >= regionEnd + marginLength) {
                            primerList.add(output);
                            break;
                        }
                        for (int k = j + 1; k < outputList.size(); k++) {
                            Primer3Output tmpOutput = (Primer3Output) outputList.get(k);
                            if (tmpOutput.getPrimerSequenceId().startsWith("Region" + regionCnt + "_")) {
                                if ((output.getRight())[0] >= (tmpOutput.getLeft())[0] + overlapLength) {
                                    maxIndex = k;
                                }
                            }
                        }
                        primerList.add(output);
                        j = maxIndex;
                    } else {
                        j++;
                    }
                }
            }
        }
        TreeMap uniqMap = new TreeMap();
        for (int j = 0; j < primerList.size(); j++) {
            Primer3Output output = (Primer3Output) primerList.get(j);
            Iterator it = uniqMap.keySet().iterator();
            boolean addFlag = true;
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                Primer3Output tmpOutput = (Primer3Output) uniqMap.get(key);
                if ((output.getLeft())[0] == (tmpOutput.getLeft())[0] && (output.getRight())[0] == (tmpOutput.getRight())[0]) {
                    addFlag = false;
                    break;
                } else if ((output.getLeft())[0] >= (tmpOutput.getLeft())[0] && (output.getRight())[0] <= (tmpOutput.getRight())[0]) {
                    addFlag = false;
                    break;
                } else if ((output.getLeft())[0] <= (tmpOutput.getLeft())[0] && (output.getRight())[0] >= (tmpOutput.getRight())[0]) {
                    uniqMap.remove(key);
                }
            }
            if (addFlag) uniqMap.put(new Integer((output.getLeft())[0]), output);
        }
        primerList = new ArrayList();
        Iterator it = uniqMap.keySet().iterator();
        int cnt = 0;
        while (it.hasNext()) {
            Primer3Output output = (Primer3Output) uniqMap.get(it.next());
            output.setPrimerSequenceId("Primer" + (cnt + 1));
            cnt++;
            ArrayList coveringRegionList = new ArrayList();
            for (int j = 0; j < annotList.size(); j++) {
                Annotation annot = (Annotation) annotList.get(j);
                String name = annot.getAnnotName();
                int annotStart = annot.getStart();
                int annotEnd = annot.getEnd();
                int targetStart = 1 >= annotStart - marginLength ? 1 : annotStart - marginLength;
                int targetEnd = wholeSequence.length() <= annotEnd + marginLength ? wholeSequence.length() : annotEnd + marginLength;
                if (((output.getLeft())[0] <= targetEnd && (output.getRight())[0] >= targetEnd) || ((output.getLeft())[0] <= targetStart && (output.getRight())[0] >= targetEnd) || ((output.getLeft())[0] >= targetStart && (output.getRight())[0] <= targetEnd) || ((output.getLeft())[0] <= targetStart && (output.getRight())[0] >= targetStart)) {
                    coveringRegionList.add(name);
                }
            }
            output.setCoveringRegion(coveringRegionList);
            primerList.add(output);
        }
        return primerList;
    }

    /**
	 * 
	 * Primer3�͈�`�q�{��ӗ̈��؂�o�����z��ɑ΂��čs�����߁A
	 * ��F�̏�̈ʒu�E���ɕ␳����
	 * 
	 * @param outputList Primer3���ʁiPrimer3Output�j���X�g
	 * @param strand ��`�q�̐�F�̂ɑ΂�����
	 * @param start �؂�o�����z��̐�F�̂ł̊J�n�ʒu
	 * @param end �؂�o�����z��̐�F�̂ł̏I�~�ʒu
	 * @return �ʒu�E����␳����Primer3Output�̃��X�g
	 */
    private ArrayList adjustPosition(ArrayList outputList, String strand, int start, int end) {
        ArrayList adjustList = new ArrayList();
        for (int i = 0; i < outputList.size(); i++) {
            Primer3Output output = (Primer3Output) outputList.get(i);
            int[] left = output.getLeft();
            int[] right = output.getRight();
            if (strand.equals("-")) {
                output.setLeft(new int[] { end + (marginLength + primerProductMaxSize + primerMaxSize + upstreamSize) - left[0], left[1] });
                output.setRight(new int[] { end + (marginLength + primerProductMaxSize + primerMaxSize + upstreamSize) - right[0], right[1] });
            } else {
                output.setLeft(new int[] { left[0] + start - (marginLength + primerProductMaxSize + primerMaxSize + upstreamSize), left[1] });
                output.setRight(new int[] { right[0] + start - (marginLength + primerProductMaxSize + primerMaxSize + upstreamSize), right[1] });
            }
            adjustList.add(output);
        }
        return adjustList;
    }

    /**
	 * 
	 * work�f�B���N�g����ɂ���A�쐬���Ă����莞�ԁi���j�ȏ�
	 * �o�߂����t�@�C����폜����B
	 * 
	 * @param workDirPath work�f�B���N�g���̃p�X
	 */
    private void deleteOldFiles(String workDirPath) {
        SimpleDateFormat dateformatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
        File directory = new File(workDirPath);
        File[] fileList = directory.listFiles();
        Date nowDate = new Date();
        String oldDateStr = "";
        int limit = 24 * 60 * 60 * 1000;
        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                String fileName = fileList[i].getName();
                if (fileName.indexOf(".out") > 0) {
                    oldDateStr = fileName.substring(0, fileName.indexOf(".out"));
                }
                Date oldDate = null;
                try {
                    oldDate = dateformatter.parse(fileName);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (oldDate != null) {
                    if (nowDate.getTime() - oldDate.getTime() > limit) {
                        new File(workDirPath + File.separator + fileName).delete();
                    }
                }
            }
        }
    }

    /**
	 * 
	 * Primer�݌v�Ώۗ̈�̃��X�g��Ԃ��B
	 * �ׂ̑ΏۂƋߐځiprimerProductOptSize�̔����ȓ�j���Ă���
	 * �ꍇ�́A�ЂƂɂ܂Ƃ߂�B 
	 * 
	 * @param annotList Primer�݌v�Ώۂ�5'/Exon/Intron/3'�̃��X�g
	 * @return Primer�݌v�Ώۗ̈�̃��X�g
	 */
    private ArrayList getRegionList(ArrayList annotList) {
        ArrayList regionList = new ArrayList();
        int i = 0;
        int cnt = 0;
        int start = 0;
        int prevEnd = 0;
        while (i < annotList.size()) {
            Annotation annot = (Annotation) annotList.get(i);
            start = annot.getStart();
            prevEnd = annot.getEnd();
            if (annotList.size() == 1) {
                cnt++;
                int[] region = new int[3];
                region[0] = cnt;
                region[1] = start;
                region[2] = prevEnd;
                regionList.add(region);
                break;
            }
            for (int j = i + 1; j < annotList.size(); j++) {
                Annotation tmpAnnot = (Annotation) annotList.get(j);
                if (prevEnd + primerProductOptSize / 2 < tmpAnnot.getStart()) {
                    cnt++;
                    int[] region = new int[3];
                    region[0] = cnt;
                    region[1] = start;
                    region[2] = prevEnd;
                    regionList.add(region);
                    i = j;
                    if (j == annotList.size() - 1) {
                        cnt++;
                        region = new int[3];
                        region[0] = cnt;
                        region[1] = tmpAnnot.getStart();
                        region[2] = tmpAnnot.getEnd();
                        regionList.add(region);
                        i = Integer.MAX_VALUE;
                    }
                    break;
                } else {
                    prevEnd = tmpAnnot.getEnd();
                    if (j == annotList.size() - 1) {
                        cnt++;
                        int[] region = new int[3];
                        region[0] = cnt;
                        region[1] = start;
                        region[2] = prevEnd;
                        regionList.add(region);
                        i = Integer.MAX_VALUE;
                    }
                }
            }
        }
        return regionList;
    }

    /**
	 * 
	 * �ePrimer Pair�ɑ΂���Product�̔z���擾����B
	 * 
	 * @param outputList �݌v���ꂽPrimer���iPrimer3Output�j�̃��X�g
	 * @param sequence ���␳�ς݂̑S�̂̔z��
	 * @return Product�z���ǉB���Primer3Output�̃��X�g
	 */
    private ArrayList getProductSequence(ArrayList outputList, String sequence) {
        ArrayList newOutputList = new ArrayList();
        for (int i = 0; i < outputList.size(); i++) {
            Primer3Output output = (Primer3Output) outputList.get(i);
            if (output != null && output.getLeft() != null && output.getRight() != null) {
                output.setProductSequence(sequence.substring((output.getLeft())[0], (output.getRight())[0] + 1));
                newOutputList.add(output);
            }
        }
        return newOutputList;
    }

    /**
     * 
     * �e�T�[�o�ɂ�Primer3��s����
     * �e�T�[�o�̃T�[�u���b�g�ɕK�v�ȃp�����[�^�𑗂�A���ʂ�
     * ArrayList�iPrimer3Output�̃��X�g�j�œ���B<p>
     * 
     * @param hHead hHead
     * @param hCheck hCheck
     * @return ����z��
     * @throws Exception
     */
    private ArrayList execAtParentServer(ArrayList paramList) throws Exception {
        ArrayList outputList = null;
        String message = "";
        try {
            HashMap serverUrlMap = InitXml.getInstance().getServerMap();
            Iterator it = serverUrlMap.keySet().iterator();
            while (it.hasNext()) {
                String server = (String) it.next();
                String serverUrl = (String) serverUrlMap.get(server);
                serverUrl = serverUrl + Primer3Manager.servletName;
                URL url = new URL(serverUrl);
                URLConnection uc = url.openConnection();
                uc.setDoOutput(true);
                OutputStream os = uc.getOutputStream();
                StringBuffer buf = new StringBuffer();
                buf.append("actionType=designparent");
                for (int i = 0; i < paramList.size(); i++) {
                    Primer3Param param = (Primer3Param) paramList.get(i);
                    if (i == 0) {
                        buf.append("&sequence=" + param.getSequence());
                        buf.append("&upstream_size" + upstreamSize);
                        buf.append("&downstreamSize" + downstreamSize);
                        buf.append("&MARGIN_LENGTH=" + marginLength);
                        buf.append("&OVERLAP_LENGTH=" + overlapLength);
                        buf.append("&MUST_XLATE_PRODUCT_MIN_SIZE=" + param.getPrimerProductMinSize());
                        buf.append("&MUST_XLATE_PRODUCT_MAX_SIZE=" + param.getPrimerProductMaxSize());
                        buf.append("&PRIMER_PRODUCT_OPT_SIZE=" + param.getPrimerProductOptSize());
                        buf.append("&PRIMER_MAX_END_STABILITY=" + param.getPrimerMaxEndStability());
                        buf.append("&PRIMER_MAX_MISPRIMING=" + param.getPrimerMaxMispriming());
                        buf.append("&PRIMER_PAIR_MAX_MISPRIMING=" + param.getPrimerPairMaxMispriming());
                        buf.append("&PRIMER_MIN_SIZE=" + param.getPrimerMinSize());
                        buf.append("&PRIMER_OPT_SIZE=" + param.getPrimerOptSize());
                        buf.append("&PRIMER_MAX_SIZE=" + param.getPrimerMaxSize());
                        buf.append("&PRIMER_MIN_TM=" + param.getPrimerMinTm());
                        buf.append("&PRIMER_OPT_TM=" + param.getPrimerOptTm());
                        buf.append("&PRIMER_MAX_TM=" + param.getPrimerMaxTm());
                        buf.append("&PRIMER_MAX_DIFF_TM=" + param.getPrimerMaxDiffTm());
                        buf.append("&PRIMER_MIN_GC=" + param.getPrimerMinGc());
                        buf.append("&PRIMER_OPT_GC_PERCENT=" + param.getPrimerOptGcPercent());
                        buf.append("&PRIMER_MAX_GC=" + param.getPrimerMaxGc());
                        buf.append("&PRIMER_SELF_ANY=" + param.getPrimerSelfAny());
                        buf.append("&PRIMER_SELF_END=" + param.getPrimerSelfEnd());
                        buf.append("&PRIMER_NUM_NS_ACCEPTED=" + param.getPrimerNumNsAccepted());
                        buf.append("&PRIMER_MAX_POLY_X=" + param.getPrimerMaxPolyX());
                        buf.append("&PRIMER_GC_CLAMP=" + param.getPrimerGcClamp());
                    }
                    buf.append("&target=" + param.getPrimerSequenceId() + "," + (param.getTarget())[0] + "," + (param.getTarget())[1]);
                }
                PrintStream ps = new PrintStream(os);
                ps.print(buf.toString());
                ps.close();
                ObjectInputStream ois = new ObjectInputStream(uc.getInputStream());
                outputList = (ArrayList) ois.readObject();
                ois.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if ((outputList == null || outputList.size() == 0) && message != null && message.length() > 0) {
            throw new Exception(message);
        }
        return outputList;
    }

    public static void main(String[] args) {
        Primer3Manager manager = new Primer3Manager();
        String url = "http://localhost/gps/service?db=Mm.NCBIm33.INDEX&hCheck=NCBIm33&hHead=Mm:4:122179725-122379725&selectedList=39&forceNoHistogram=true";
        DocumentBuilderFactory _factory = DocumentBuilderFactory.newInstance();
        _factory.setNamespaceAware(false);
        _factory.setValidating(false);
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = _factory.newDocumentBuilder();
            doc = builder.parse(url);
        } catch (Exception e) {
            doc = null;
        }
        String seq = "";
        try {
            Server server = new Server("F:/SequenceServer/files/SpeciesConfig.txt");
            seq = server.getString("Mm", "NCBIm33", "4", "122286792", "122308372");
        } catch (Exception e) {
        }
        manager.setWholeSequence(seq);
        manager.setPrimer3BinPath("E:/primer3/primer3.exe");
        manager.setWorkDirPath("E:/primer3/tmp");
        ArrayList outputList = manager.exec(doc);
        for (int i = 0; i < outputList.size(); i++) {
            Primer3Output output = (Primer3Output) outputList.get(i);
            System.out.println(output.getPrimerSequenceId());
            System.out.println("LEFT	" + (output.getLeft())[0] + "	" + (output.getLeft())[1] + "	" + output.getLeftTm() + "	" + output.getLeftGcPercent() + "	" + output.getLeftSelfAny() + "	" + output.getLeftSelfEnd() + "	" + output.getLeftSequence());
            System.out.println("RIGHT	" + (output.getRight())[0] + "	" + (output.getRight())[1] + "	" + output.getRightTm() + "	" + output.getRightGcPercent() + "	" + output.getRightSelfAny() + "	" + output.getRightSelfEnd() + "	" + output.getRightSequence());
            System.out.println("PRODUCT SIZE: " + output.getProductSize());
            System.out.println("");
        }
    }

    /**
	 * 
	 * 
	 * 5' Flanking Region/Exon/Intron/3' Flanking Region�̏��
	 * ��i�[���邽�߂̃I�u�W�F�N�g
	 * </p>
	 * @author yuko
	 * @version $Revision: 1.6 $ $Date: 2006/09/25 07:22:34 $
	 */
    private class Annotation {

        String annotName;

        int start;

        int end;

        /**
		 * @return Returns the end.
		 */
        public int getEnd() {
            return end;
        }

        /**
		 * @param end The end to set.
		 */
        public void setEnd(int end) {
            this.end = end;
        }

        /**
		 * @return Returns the annotName.
		 */
        public String getAnnotName() {
            return annotName;
        }

        /**
		 * @param annotName The annotName to set.
		 */
        public void setAnnotName(String regionName) {
            this.annotName = regionName;
        }

        /**
		 * @return Returns the start.
		 */
        public int getStart() {
            return start;
        }

        /**
		 * @param start The start to set.
		 */
        public void setStart(int start) {
            this.start = start;
        }
    }
}
