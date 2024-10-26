package Action.textMode;

import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import Action.lineMode.lineModeCommon.relaxer.FEATURE;
import Action.lineMode.lineModeCommon.relaxer.GROUP;
import Action.lineMode.lineModeCommon.relaxer.LineMode;
import Action.lineMode.lineModeCommon.relaxer.SEGMENT;
import Action.lineMode.lineModeCommon.relaxer.TMGFF;
import Action.lineMode.lineModeCommon.relaxer.VALUE;
import Action.lineMode.lineModeCommon.Datasource;
import Action.lineMode.lineModeCommon.DatasourceManager;
import Action.species.InitXml;
import process.primer3.Primer3Output;

public class TextModeManager {

    private Document doc;

    private String chr;

    private int start;

    private int end;

    private String alias;

    private String version;

    private String hHead;

    private String sourceUrl;

    private String strain1;

    private String strain2;

    private TreeMap strainMap;

    private boolean existStrainGenotype;

    public static final String DEFAULT_STRAIN = "C57BL/6J";

    public static final String[] EXCLUDE_DATASOURCE_STRING = { "Synteny", "Homology" };

    public static final String MOUSE_DBSNP = "Mouse dbSNP";

    private static final String servletName = "/gps/GeneTextServlet";

    static Logger log = Logger.getLogger(TextModeManager.class);

    /**
	 * �R���X�g���N�^
	 */
    public TextModeManager(String hHead, String version, String sourceUrl) {
        setHHead(hHead);
        setVersion(version);
        setAlias(hHead.substring(0, hHead.indexOf(":")));
        setChr(hHead.substring(hHead.indexOf(":") + 1, hHead.indexOf(":", hHead.indexOf(":") + 1)));
        setStart(Integer.parseInt(hHead.substring((hHead.indexOf(":", hHead.indexOf(":") + 1)) + 1, hHead.indexOf("-"))));
        setEnd(Integer.parseInt(hHead.substring(hHead.indexOf("-") + 1)));
        setSourceUrl(sourceUrl);
        setDoc();
        setStrain1("");
        setStrain2("");
        setExistStrainGenotype(existStrainGenotype());
    }

    /**
	 * @return Returns the doc.
	 */
    public Document getDoc() {
        return doc;
    }

    /**
	 * @param doc The doc to set.
	 */
    public void setDoc(Document doc) {
        this.doc = doc;
    }

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

    /**
	 * @return Returns the alias.
	 */
    public String getAlias() {
        return alias;
    }

    /**
	 * @param alias The alias to set.
	 */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
	 * @return Returns the version.
	 */
    public String getVersion() {
        return version;
    }

    /**
	 * @param version The version to set.
	 */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
	 * @return Returns the hHead.
	 */
    public String getHHead() {
        return hHead;
    }

    /**
	 * @param head The hHead to set.
	 */
    public void setHHead(String head) {
        hHead = head;
    }

    /**
	 * @return Returns the chr.
	 */
    public String getChr() {
        return chr;
    }

    /**
	 * @param chr The chr to set.
	 */
    public void setChr(String chr) {
        this.chr = chr;
    }

    /**
	 * @return Returns the sourceUrl.
	 */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
	 * @param sourceUrl The sourceUrl to set.
	 */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    /**
	 * @return Returns the strain1.
	 */
    public String getStrain1() {
        return strain1;
    }

    /**
	 * @param strain1 The strain1 to set.
	 */
    public void setStrain1(String strain1) {
        this.strain1 = strain1;
    }

    /**
	 * @return Returns the strain2.
	 */
    public String getStrain2() {
        return strain2;
    }

    /**
	 * @param strain2 The strain2 to set.
	 */
    public void setStrain2(String strain2) {
        this.strain2 = strain2;
    }

    /**
	 * @param strainMap The strainMap to set.
	 */
    public void setStrainMap(TreeMap strainMap) {
        this.strainMap = strainMap;
    }

    /**
	 * @return Returns the existStrainGenotype.
	 */
    public boolean isExistStrainGenotype() {
        return existStrainGenotype;
    }

    /**
	 * @param existStrainGenotype The existStrainGenotype to set.
	 */
    public void setExistStrainGenotype(boolean existGenotype) {
        this.existStrainGenotype = existGenotype;
    }

    /**
	 * 
	 * LineMode�f�[�^���p�[�X���A�\���Ώۂ̃G���g�����i�[����
	 * ���X�g��Ԃ��B
	 * 
	 * @param direction �Q�m���̕\�����
	 * @return �\���Ώۂ̃G���g�����i�[�������X�g
	 */
    public ArrayList lineMode2TextMode(String direction) {
        ArrayList plusList = new ArrayList();
        ArrayList minusList = new ArrayList();
        ArrayList snpPlusList = new ArrayList();
        ArrayList snpMinusList = new ArrayList();
        if (existStrainGenotype) setStrainMap(getStrainMap());
        if (getDoc() == null) return new ArrayList();
        LineMode lineMode = new LineMode(getDoc());
        TMGFF[] tmgffs = lineMode.getTMGFF();
        for (int i = 0; i < tmgffs.length; i++) {
            boolean excludeFlag = false;
            for (int j = 0; j < EXCLUDE_DATASOURCE_STRING.length; j++) {
                if (tmgffs[i].getSource().indexOf(EXCLUDE_DATASOURCE_STRING[j]) >= 0) {
                    excludeFlag = true;
                }
            }
            if (excludeFlag) continue;
            SEGMENT[] segments = tmgffs[i].getSEGMENT();
            for (int j = 0; j < segments.length; j++) {
                GROUP[] groups = segments[j].getGROUP();
                for (int k = 0; k < groups.length; k++) {
                    if (groups[k].getType().equals("Histogram")) {
                        continue;
                    } else if (groups[k].getType().equals("snp")) {
                        if (existStrainGenotype) {
                            String[] highlightGenotypes = { "", "" };
                            FEATURE[] features = groups[k].getFEATURE();
                            if (strain1 != null && !strain1.equals(TextModeBean.STRAIN_ID_NONE) && strain2 != null && !strain2.equals(TextModeBean.STRAIN_ID_NONE)) {
                                highlightGenotypes = getHighlightGenotype(features, strain1, strain2);
                            }
                            TreeMap featuresMap = new TreeMap();
                            String description = "";
                            if (tmgffs[i].getServer() != null && tmgffs[i].getServer().length() > 0) {
                                description = tmgffs[i].getServer() + ":" + tmgffs[i].getSource();
                            } else {
                                description = tmgffs[i].getSource();
                            }
                            if (features != null && features.length > 0) {
                                for (int l = 0; l < features.length; l++) {
                                    featuresMap.put(features[l].getId(), features[l]);
                                }
                            }
                            if (featuresMap.containsKey(highlightGenotypes[0])) {
                                TextModeBean bean = setSnp(groups[k], (FEATURE) featuresMap.get(highlightGenotypes[0]), direction, highlightGenotypes, description);
                                if (bean.getStrand().length() == 0 || bean.getStrand().equals(direction)) {
                                    snpPlusList.add(bean);
                                } else {
                                    snpMinusList.add(bean);
                                }
                                featuresMap.remove(highlightGenotypes[0]);
                            }
                            if (featuresMap.containsKey(highlightGenotypes[1])) {
                                TextModeBean bean = setSnp(groups[k], (FEATURE) featuresMap.get(highlightGenotypes[1]), direction, highlightGenotypes, description);
                                if (bean.getStrand().length() == 0 || bean.getStrand().equals(direction)) {
                                    snpPlusList.add(bean);
                                } else {
                                    snpMinusList.add(bean);
                                }
                                featuresMap.remove(highlightGenotypes[1]);
                            }
                            Iterator it = featuresMap.keySet().iterator();
                            while (it.hasNext()) {
                                TextModeBean bean = setSnp(groups[k], (FEATURE) featuresMap.get((String) it.next()), direction, highlightGenotypes, description);
                                if (bean.getStrand().length() == 0 || bean.getStrand().equals(direction)) {
                                    snpPlusList.add(bean);
                                } else {
                                    snpMinusList.add(bean);
                                }
                            }
                        } else {
                            String allele = groups[k].getLINK().getContent();
                            String[] alleles = StringUtils.split(allele, "/");
                            boolean flag = false;
                            for (int l = 0; l < alleles.length; l++) {
                                if (alleles[l].length() > 1) {
                                    flag = true;
                                }
                            }
                            for (int l = 0; l < alleles.length; l++) {
                                TextModeBean bean = new TextModeBean();
                                bean.setId(groups[k].getId());
                                bean.setName(groups[k].getLabel());
                                bean.setType(groups[k].getType());
                                bean.setStart(groups[k].getSTART());
                                bean.setEnd(groups[k].getEND());
                                bean.setStrand(groups[k].getORIENTATION());
                                bean.setLinkHref(groups[k].getLINK().getHref());
                                bean.setNote(groups[k].getNOTE());
                                bean.setColor(groups[k].getColor());
                                String alleleStr = "";
                                if (allele.indexOf("-") >= 0 || flag) {
                                    alleleStr = alleles[l];
                                    if (!direction.equals(groups[k].getORIENTATION())) alleleStr = complement(alleleStr);
                                    String tmp = "<font title='" + alleleStr + "'>*</font>";
                                    bean.setSeqStr(createSeqStrForSnp(groups[k].getORIENTATION(), groups[k].getSTART(), tmp, direction, false));
                                    alleleStr = " *=\"" + alleleStr + "\"";
                                } else {
                                    bean.setSeqStr(createSeqStrForSnp(groups[k].getORIENTATION(), groups[k].getSTART(), alleles[l], direction, false));
                                    alleleStr = " " + alleles[l];
                                }
                                String description = "";
                                if (tmgffs[i].getServer() != null && tmgffs[i].getServer().length() > 0) {
                                    description = tmgffs[i].getServer() + ":" + tmgffs[i].getSource();
                                } else {
                                    description = tmgffs[i].getSource();
                                }
                                bean.setSource(description + alleleStr);
                                if (bean.getStrand().length() == 0 || bean.getStrand().equals(direction)) {
                                    snpPlusList.add(bean);
                                } else {
                                    snpMinusList.add(bean);
                                }
                            }
                        }
                    } else {
                        TextModeBean bean = new TextModeBean();
                        bean.setId(groups[k].getId());
                        bean.setName(groups[k].getLabel());
                        if (tmgffs[i].getServer() != null && tmgffs[i].getServer().length() > 0) {
                            bean.setSource(tmgffs[i].getServer() + ":" + tmgffs[i].getSource());
                        } else {
                            bean.setSource(tmgffs[i].getSource());
                        }
                        bean.setType(groups[k].getType());
                        bean.setStart(groups[k].getSTART());
                        bean.setEnd(groups[k].getEND());
                        bean.setStrand(groups[k].getORIENTATION());
                        bean.setLinkHref(groups[k].getLINK().getHref());
                        bean.setNote(groups[k].getNOTE());
                        bean.setColor(groups[k].getColor());
                        if (groups[k].getType().equals("CAGE tags")) {
                            bean.setSeqStr(TextModeBean.CHAR_NORMAL);
                        } else {
                            bean.setSeq(createSeq(groups[k].getSTART(), groups[k].getEND(), groups[k].getORIENTATION(), groups[k].getFEATURE(), direction));
                        }
                        bean.setFeatures(createFeatures(groups[k].getORIENTATION(), groups[k].getFEATURE(), direction));
                        bean.setDsId(tmgffs[i].getIdAsString());
                        bean.setGroupIndex(k);
                        if (bean.getStrand().length() == 0 || bean.getStrand().equals(direction)) {
                            plusList.add(bean);
                        } else {
                            minusList.add(bean);
                        }
                    }
                }
            }
        }
        ArrayList itemList = new ArrayList();
        if (snpPlusList != null && snpPlusList.size() > 0) itemList.addAll(snpPlusList);
        if (snpMinusList != null && snpMinusList.size() > 0) itemList.addAll(snpMinusList);
        if (plusList != null && plusList.size() > 0) itemList.addAll(plusList);
        if (minusList != null && minusList.size() > 0) itemList.addAll(minusList);
        return itemList;
    }

    /**
	 * 
	 * Primer�݌v���Sequence Viewer�ł̂ݎg�p�B
	 * Flanking Region�̏���feature�Ƃ��Ēǉ�����
	 * 
	 * @param itemList �S�Ă̕\���ΏۃG���g�����i�[�������X�g
	 * @param upstreamSize 5' Flanking Region�̒���
	 * @param downstreamSize 3' Flanking Region�̒���
	 * @param direction �Q�m���̕\�����
	 * @return itemList�̈�`�qTextModeBean��Flanking Region�̏���ǉ��������X�g
	 */
    public ArrayList addFlankingRegion(ArrayList itemList, int upstreamSize, int downstreamSize, String direction) {
        ArrayList newList = new ArrayList();
        for (int i = 0; i < itemList.size(); i++) {
            TextModeBean bean = (TextModeBean) itemList.get(i);
            if (!bean.getType().equals("snp") && bean.getFeatures() != null && bean.getFeatures().size() > 0 && upstreamSize > 0 || downstreamSize > 0) {
                ArrayList featureList = bean.getFeatures();
                if (!bean.getStrand().equals(direction)) {
                    TextModeBean feature = new TextModeBean();
                    feature.setId("5' Flanking Region");
                    feature.setType("flanking");
                    feature.setStrand(bean.getStrand());
                    feature.setStart(bean.getEnd() + 1);
                    feature.setEnd(bean.getEnd() + upstreamSize);
                    featureList.add(feature);
                    feature = new TextModeBean();
                    feature.setId("3' Flanking Region");
                    feature.setType("flanking");
                    feature.setStrand(bean.getStrand());
                    feature.setStart(bean.getStart() - downstreamSize);
                    feature.setEnd(bean.getStart() - 1);
                    featureList.add(0, feature);
                } else {
                    TextModeBean feature = new TextModeBean();
                    feature.setId("5' Flanking Region");
                    feature.setType("flanking");
                    feature.setStrand(bean.getStrand());
                    feature.setStart(bean.getStart() - upstreamSize);
                    feature.setEnd(bean.getStart() - 1);
                    featureList.add(0, feature);
                    feature = new TextModeBean();
                    feature.setId("3' Flanking Region");
                    feature.setType("flanking");
                    feature.setStrand(bean.getStrand());
                    feature.setStart(bean.getEnd() + 1);
                    feature.setEnd(bean.getEnd() + downstreamSize);
                    featureList.add(feature);
                }
                bean.setSeq(createSeqForFlanking(bean.getStart(), bean.getEnd(), bean.getStrand(), direction, upstreamSize, downstreamSize, bean.getSeq()));
                bean.setFeatures(featureList);
                if (!bean.getStrand().equals(direction)) {
                    bean.setStart(bean.getStart() - downstreamSize);
                    bean.setEnd(bean.getEnd() + upstreamSize);
                } else {
                    bean.setStart(bean.getStart() - upstreamSize);
                    bean.setEnd(bean.getEnd() + upstreamSize);
                }
            }
            newList.add(bean);
        }
        return newList;
    }

    /**
	 * 
	 * DataSourceID�Ǝ擾�ł����f�[�^���̑Ή���Map�Ɋi�[����B
	 * 
	 * @return key: DataSourceID, value: �f�[�^����TreeMap
	 */
    public TreeMap countLineMode() {
        TreeMap lineModeCountMap = new TreeMap();
        if (this.doc == null) return lineModeCountMap;
        LineMode lineMode = new LineMode(this.doc);
        TMGFF[] tmgffs = lineMode.getTMGFF();
        for (int i = 0; i < tmgffs.length; i++) {
            SEGMENT[] segments = tmgffs[i].getSEGMENT();
            int count = 0;
            int dsId = -1;
            if (tmgffs[i].getParentSourceId() > 0) {
                dsId = tmgffs[i].getParentSourceId();
            } else {
                dsId = tmgffs[i].getId();
            }
            for (int j = 0; j < segments.length; j++) {
                GROUP[] groups = segments[j].getGROUP();
                for (int k = 0; k < groups.length; k++) {
                    if (groups[k].getType().equals("Histogram")) {
                        VALUE[] value = groups[k].getVALUE();
                        for (int l = 0; l < value.length; l++) {
                            if (value[l].getName().equals("count")) count += value[l].getContent();
                        }
                    } else {
                        count++;
                    }
                }
            }
            if (lineModeCountMap.containsKey(new Integer(dsId))) {
                count += ((Integer) lineModeCountMap.get(new Integer(dsId))).intValue();
            }
            lineModeCountMap.put(new Integer(dsId), new Integer(count));
        }
        return lineModeCountMap;
    }

    /**
	 * 
	 *  LineMode�h�L�������g�Ɋ܂܂���`�q�i"snp"�ȊO��feature������GROUP�j
	 *  �̏����擾����B
	 * 
	 * @return ��`�q�����i�[�������X�g
	 */
    public ArrayList getGeneList() {
        ArrayList geneList = new ArrayList();
        LineMode lineMode = new LineMode(this.doc);
        TMGFF[] tmgffs = lineMode.getTMGFF();
        for (int i = 0; i < tmgffs.length; i++) {
            SEGMENT[] segments = tmgffs[i].getSEGMENT();
            for (int j = 0; j < segments.length; j++) {
                GROUP[] groups = segments[j].getGROUP();
                for (int k = 0; k < groups.length; k++) {
                    FEATURE[] features = groups[k].getFEATURE();
                    boolean flag = false;
                    for (int l = 0; l < features.length; l++) {
                        if (!features[l].getTYPE().getContent().equals("snp")) {
                            flag = true;
                            break;
                        }
                    }
                    if (flag) {
                        TextModeBean bean = new TextModeBean();
                        bean.setId(groups[k].getId());
                        bean.setName(groups[k].getLabel());
                        if (tmgffs[i].getServer() != null && tmgffs[i].getServer().length() > 0) {
                            bean.setSource(tmgffs[i].getServer() + ":" + tmgffs[i].getSource());
                        } else {
                            bean.setSource(tmgffs[i].getSource());
                        }
                        bean.setType(groups[k].getType());
                        bean.setStart(groups[k].getSTART());
                        bean.setEnd(groups[k].getEND());
                        bean.setStrand(groups[k].getORIENTATION());
                        bean.setLinkHref(groups[k].getLINK().getHref());
                        bean.setNote(groups[k].getNOTE());
                        bean.setDsId(tmgffs[i].getIdAsString());
                        bean.setGroupIndex(k);
                        bean.setFeatures(new ArrayList(Arrays.asList(features)));
                        geneList.add(bean);
                    }
                }
            }
        }
        return geneList;
    }

    /**
	 *
	 * LineMode�f�[�^���p�[�X�������ʂ��A�u���E�U�ł̕\���ɂ��킹��
	 * �P�s������w�肳�ꂽ����iSEQ_NUM_PER_LINE�j�P�ʂŃ��X�g��
	 * �i�[����B
	 * 
	 * @param seq ����z��
	 * @param showAll intron�A�A�m�e�[�V�����̖����s���\�����邩�ǂ���
	 * @param direction ����z��̕��
	 * @return �\���p���X�g
	 */
    public ArrayList getLineList(ArrayList itemList, String seq, boolean showAll, String direction) {
        ArrayList lineList = new ArrayList();
        int lineTotal = (end - start + 1) / TextModeBean.SEQ_NUM_PER_LINE + 1;
        StringBuffer frontSideSeqBuf = new StringBuffer();
        StringBuffer lastSideSeqBuf = new StringBuffer();
        int frontSideNum = -1;
        int lastSideNum = -1;
        int lineStart = -1;
        int lineEnd = -1;
        if (seq.length() < lineTotal * TextModeBean.SEQ_NUM_PER_LINE) {
            for (int i = seq.length(); i < lineTotal * TextModeBean.SEQ_NUM_PER_LINE; i++) {
                seq += TextModeBean.CHAR_BLANK;
            }
        }
        for (int lineCnt = 0; lineCnt < lineTotal; lineCnt++) {
            boolean showLine = false;
            lineStart = start + lineCnt * TextModeBean.SEQ_NUM_PER_LINE;
            lineEnd = start + (lineCnt + 1) * TextModeBean.SEQ_NUM_PER_LINE - 1;
            if (!showAll) {
                showLine = isShowLine(itemList, lineStart, lineEnd);
            }
            if (showAll || showLine) {
                if (frontSideNum > 0) {
                    TextModeLine line = new TextModeLine();
                    line.setStart(frontSideNum);
                    line.setEnd(frontSideNum + TextModeBean.SEQ_NUM_PER_LINE - 1);
                    line.setSeqStr(frontSideSeqBuf.toString());
                    lineList.add(line);
                    if (lastSideNum - frontSideNum + 1 > TextModeBean.SEQ_NUM_PER_LINE * 2) {
                        line = new TextModeLine();
                        line.setStart(frontSideNum + TextModeBean.SEQ_NUM_PER_LINE);
                        line.setEnd(lastSideNum - TextModeBean.SEQ_NUM_PER_LINE);
                        lineList.add(line);
                    }
                    if (lastSideNum - frontSideNum + 1 > TextModeBean.SEQ_NUM_PER_LINE) {
                        line = new TextModeLine();
                        line.setStart(lastSideNum - TextModeBean.SEQ_NUM_PER_LINE + 1);
                        line.setEnd(lastSideNum);
                        line.setSeqStr(lastSideSeqBuf.toString());
                        lineList.add(line);
                    }
                    frontSideNum = -1;
                    lastSideNum = -1;
                }
                TextModeLine line = new TextModeLine();
                line.setStart(lineStart);
                line.setEnd(lineEnd < end ? lineEnd : end);
                line.setItemList(setAnnot(itemList, lineStart, lineEnd));
                line.setSeqStr(seq.substring(lineStart - start, lineEnd - start + 1));
                lineList.add(line);
            } else {
                if (frontSideNum < 0) {
                    frontSideNum = lineStart;
                    frontSideSeqBuf = new StringBuffer();
                    frontSideSeqBuf.append(seq.substring(lineStart - start, lineEnd - start + 1));
                }
                lastSideNum = lineEnd;
                lastSideSeqBuf = new StringBuffer();
                lastSideSeqBuf.append(seq.substring(lineStart - start, lineEnd - start + 1));
            }
        }
        if (frontSideNum > 0) {
            TextModeLine line = new TextModeLine();
            line.setStart(frontSideNum);
            line.setEnd(frontSideNum + TextModeBean.SEQ_NUM_PER_LINE - 1);
            line.setSeqStr(frontSideSeqBuf.toString());
            lineList.add(line);
            if (lastSideNum - frontSideNum + 1 > TextModeBean.SEQ_NUM_PER_LINE * 2) {
                line = new TextModeLine();
                line.setStart(frontSideNum + TextModeBean.SEQ_NUM_PER_LINE);
                line.setEnd(lastSideNum - TextModeBean.SEQ_NUM_PER_LINE);
                lineList.add(line);
            }
            if (lastSideNum - frontSideNum + 1 > TextModeBean.SEQ_NUM_PER_LINE) {
                line = new TextModeLine();
                line.setStart(lastSideNum - TextModeBean.SEQ_NUM_PER_LINE + 1);
                line.setEnd((end < lastSideNum) ? end : lastSideNum);
                line.setSeqStr(lastSideSeqBuf.toString());
                lineList.add(line);
            }
        }
        return lineList;
    }

    /**
	 * 
	 * �w�肳�ꂽalias�Aversion�ŉ{���\��DataSource�����ׂĎ擾����
	 * 
	 * @return DataSource�̔z��
	 */
    public Datasource[] getDatasource() {
        Datasource[] ds = DatasourceManager.getDatasouce(alias, version, DatasourceManager.ALL_CONTAINS_GROUP);
        return ds;
    }

    /**
	 * 
	 * Strain����Genotype���\���Ώۂ��ǂ������擾����i�}�E�X�̂݁j
	 * 
	 * @return Strain����Genotype���\���Ώۂ��ǂ���
	 */
    public boolean existStrainGenotype() {
        boolean exist = false;
        if (alias.equals("Mm")) {
            LineMode lineMode = new LineMode(getDoc());
            TMGFF[] tmgffs = lineMode.getTMGFF();
            for (int i = 0; i < tmgffs.length; i++) {
                if (tmgffs[i] != null && tmgffs[i].getAnnotation() != null && tmgffs[i].getAnnotation().equals("dbSNP") && tmgffs[i].getSEGMENT() != null) {
                    for (int j = 0; j < tmgffs[i].getSEGMENT().length; j++) {
                        GROUP[] groups = (tmgffs[i].getSEGMENT())[j].getGROUP();
                        if (groups != null) {
                            for (int k = 0; k < groups.length; k++) {
                                if (groups[k].getFEATURE() != null && groups[k].getFEATURE().length > 0 && (groups[k].getFEATURE())[0] != null) {
                                    if ((groups[k].getFEATURE())[0].getTYPE() != null && (groups[k].getFEATURE())[0].getTYPE().getContent().equals("snp")) {
                                        exist = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return exist;
    }

    /**
	 * 
	 * Strain�����擾����B
	 * 
	 * @return key: Strain ID�Avalue: Strain����Map
	 */
    public TreeMap getStrainMap() {
        TreeMap strainMap = new TreeMap();
        String server = "";
        try {
            Datasource[] ds = DatasourceManager.getDatasouce(alias, version, DatasourceManager.ALL_CONTAINS_GROUP);
            for (int i = 0; i < ds.length; i++) {
                if (ds[i].getDescription().startsWith(MOUSE_DBSNP)) {
                    if (ds[i].getServer().length() == 0) {
                        Connection con = ds[i].getConnection();
                        strainMap = Action.lineMode.regularSQL.GenotypeDataSearchAction.getStrainMap(con);
                        break;
                    } else {
                        server = ds[i].getServer();
                        HashMap serverUrlMap = InitXml.getInstance().getServerMap();
                        String serverUrl = (String) serverUrlMap.get(server);
                        URL url = new URL(serverUrl + servletName);
                        URLConnection uc = url.openConnection();
                        uc.setDoOutput(true);
                        OutputStream os = uc.getOutputStream();
                        StringBuffer buf = new StringBuffer();
                        buf.append("viewType=getstrains");
                        buf.append("&hHead=" + hHead);
                        buf.append("&hCheck=" + version);
                        PrintStream ps = new PrintStream(os);
                        ps.print(buf.toString());
                        ps.close();
                        ObjectInputStream ois = new ObjectInputStream(uc.getInputStream());
                        strainMap = (TreeMap) ois.readObject();
                        ois.close();
                    }
                }
            }
        } catch (Exception e) {
            log.error("strain map", e);
        }
        return strainMap;
    }

    public static TreeMap getStrainMap(String alias, String version) {
        TreeMap strainMap = new TreeMap();
        try {
            Datasource[] ds = DatasourceManager.getDatasouce(alias, version, DatasourceManager.ALL_CONTAINS_GROUP);
            for (int i = 0; i < ds.length; i++) {
                if (ds[i].getDescription().startsWith(MOUSE_DBSNP)) {
                    if (ds[i].getServer().length() == 0) {
                        Connection con = ds[i].getConnection();
                        strainMap = Action.lineMode.regularSQL.GenotypeDataSearchAction.getStrainMap(con);
                        break;
                    } else {
                    }
                }
            }
        } catch (Exception e) {
            log.error("strain map", e);
        }
        if (strainMap.size() == 0) {
        }
        return strainMap;
    }

    /**
	 * 
	 * DEFAULT_STRAIN������Strain��ID���擾����B
	 * 
	 * @return DEFAULT_STRAIN������Strain��ID
	 */
    public String getDefaultStrainId(TreeMap strainMap) {
        String strainId = "";
        Iterator it = strainMap.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (strainMap.get(key).equals(TextModeManager.DEFAULT_STRAIN)) {
                strainId = key;
                break;
            }
        }
        return strainId;
    }

    /**
	 * �w�肵���f�[�^�\�[�XID�A�C���f�b�N�X������GROUP�̂�
	 * Document�Ɏc���B
	 * 
	 * @param dsId
	 * @param groupIndex
	 */
    public String[] filterDoc(String dsId, int groupIndex, int upstreamSize, int downstreamSize) {
        LineMode lineMode = new LineMode(doc);
        TMGFF newTmgff = null;
        SEGMENT newSegment = null;
        GROUP newGroup = null;
        TMGFF[] tmgffs = lineMode.getTMGFF();
        String dsName = "";
        String accession = "";
        for (int i = 0; i < tmgffs.length; i++) {
            if (tmgffs[i].getIdAsString().equals(dsId)) {
                dsName = tmgffs[i].getSource();
                newTmgff = tmgffs[i];
                SEGMENT[] segments = tmgffs[i].getSEGMENT();
                newSegment = segments[0];
                GROUP[] groups = segments[0].getGROUP();
                accession = groups[groupIndex].getId();
                newGroup = groups[groupIndex];
            }
        }
        newSegment.clearGROUP();
        newSegment.setGROUP(newGroup);
        newTmgff.clearSEGMENT();
        newTmgff.setSEGMENT(newSegment);
        lineMode.clearTMGFF();
        lineMode.setTMGFF(newTmgff);
        try {
            setDoc(lineMode.makeDocument());
        } catch (ParserConfigurationException e) {
        }
        String[] ret = { dsName, accession };
        return ret;
    }

    /**
	 * 
	 * �ЂƂ̈�`�q�́i�����́jFlanking Region/Exon/Intron�ɑ΂���
	 * �s����Primer3�̏o�͌��ʂ�TextMode�p�ɕϊ����A���X�g�Ɋi�[����B
	 * 
	 * @param outputList Primer3�̌��ʂ��i�[�������X�g
	 * @param strand ��`�q��strand
	 * @return Primer�����i�[�������X�g
	 */
    public ArrayList primer3Output2TextMode(ArrayList outputList, String strand) {
        ArrayList itemList = new ArrayList();
        for (int i = 0; i < outputList.size(); i++) {
            Primer3Output output = null;
            if (strand.equals("+")) {
                output = (Primer3Output) outputList.get(i);
            } else {
                output = (Primer3Output) outputList.get(outputList.size() - i - 1);
            }
            TextModeBean bean = new TextModeBean();
            bean = new TextModeBean();
            bean.setId(output.getPrimerSequenceId());
            bean.setName(output.getPrimerSequenceId());
            bean.setSource("primer");
            bean.setType("primer");
            if (!strand.equals("+")) {
                bean.setStart((output.getRight())[0]);
                bean.setEnd((output.getLeft())[0]);
            } else {
                bean.setStart((output.getLeft())[0]);
                bean.setEnd((output.getRight())[0]);
            }
            bean.setStrand(strand);
            bean.setLinkHref("");
            bean.setNote("");
            bean.setColor("aaaadd");
            if (!strand.equals("+")) {
                bean.setSeq(createSeqForPrimer((output.getRight())[0], (output.getRight())[0] + (output.getRight())[1], (output.getLeft())[0] - (output.getLeft())[1], (output.getLeft())[0]));
            } else {
                bean.setSeq(createSeqForPrimer((output.getLeft())[0], (output.getLeft())[0] + (output.getLeft())[1], (output.getRight())[0] - (output.getRight())[1], (output.getRight())[0]));
            }
            itemList.add(bean);
        }
        return itemList;
    }

    /**
	 * �\���p�̕�������쐬����B 
	 * 
	 * @param entryStart �G���g���[�̊J�n�ʒu
	 * @param entryEnd �G���g���[�̏I�~�ʒu
	 * @param direction ���
	 * @param features FEATURE[]
	 * @return �\���p������
	 */
    private String[] createSeq(long entryStart, long entryEnd, String strand, FEATURE[] features, String direction) {
        String[] ret = new String[getEnd() - getStart() + 1];
        if (features != null && features.length > 0 && strand != null && strand.length() > 0) {
            for (int i = 0; i < ret.length; i++) {
                if (entryStart <= getStart() + i && getStart() + i <= entryEnd) {
                    ret[i] = TextModeBean.CHAR_INTRON;
                } else {
                    ret[i] = TextModeBean.CHAR_BLANK;
                }
            }
            for (int i = 0; i < features.length; i++) {
                for (int j = 0; j < ret.length; j++) {
                    if (features[i].getSTART() <= getStart() + j && getStart() + j <= features[i].getEND()) {
                        if (direction.equals(strand)) {
                            ret[j] = TextModeBean.CHAR_FEATURE_PLUS;
                        } else {
                            ret[j] = TextModeBean.CHAR_FEATURE_MINUS;
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < ret.length; i++) {
                if (entryStart <= getStart() + i && getStart() + i <= entryEnd) {
                    if (strand.equals("0")) {
                        ret[i] = TextModeBean.CHAR_NO_STRAND;
                    } else {
                        ret[i] = TextModeBean.CHAR_NORMAL;
                    }
                } else {
                    ret[i] = TextModeBean.CHAR_BLANK;
                }
            }
        }
        return ret;
    }

    /**
	 * 
	 * �����`�q�̎w�肳�ꂽ�����Flanking Region���܂�
	 * Sequence Viewer�ŕ\������z����쐬����B
	 * 
	 * @param entryStart ��`�q�̊J�n�ʒu
	 * @param entryEnd ��`�q�̏I�n�ʒu
	 * @param strand ��`�q�̃Q�m���ɑ΂�����
	 * @param direction �Q�m���̕\�����
	 * @param upstreamSize 5' Flanking Region�̉��
	 * @param downstreamSize 3' Flanking Region�̉��
	 * @param ret Flanking Region���܂܂Ȃ��\���p�z��
	 * @return Flanking Region���܂ޕ\���p�z��
	 */
    private String[] createSeqForFlanking(long entryStart, long entryEnd, String strand, String direction, int upstreamSize, int downstreamSize, String[] ret) {
        for (int i = 0; i < ret.length; i++) {
            if (direction.equals(strand)) {
                if (entryStart - upstreamSize < getStart() + i && entryStart >= getStart() + i) {
                    ret[i] = TextModeBean.CHAR_NO_STRAND;
                }
                if (entryEnd <= getStart() + i && entryEnd + downstreamSize > getStart() + i) {
                    ret[i] = TextModeBean.CHAR_NO_STRAND;
                }
            } else {
                if (entryStart - downstreamSize < getStart() + i && entryStart >= getStart() + i) {
                    ret[i] = TextModeBean.CHAR_NO_STRAND;
                }
                if (entryEnd <= getStart() + i && entryEnd + upstreamSize > getStart() + i) {
                    ret[i] = TextModeBean.CHAR_NO_STRAND;
                }
            }
        }
        return ret;
    }

    /**
	 * �\���p�̕�������쐬����B 
	 * 
	 * @param entryStart �G���g���[�̊J�n�ʒu
	 * @param entryEnd �G���g���[�̏I�~�ʒu
	 * @param direction ���
	 * @param features FEATURE[]
	 * @return �\���p������
	 */
    private String[] createSeqForPrimer(long leftStart, long leftEnd, long rightStart, long rightEnd) {
        String[] ret = new String[getEnd() - getStart() + 1];
        for (int i = 0; i < ret.length; i++) {
            if (leftStart <= getStart() + i && getStart() + i <= leftEnd) {
                ret[i] = TextModeBean.CHAR_FEATURE_PLUS;
            } else if (rightStart <= getStart() + i && getStart() + i <= rightEnd) {
                ret[i] = TextModeBean.CHAR_FEATURE_MINUS;
            } else if (leftEnd < getStart() + i && rightStart > getStart() + i) {
                ret[i] = TextModeBean.CHAR_NORMAL;
            } else {
                ret[i] = TextModeBean.CHAR_BLANK;
            }
        }
        return ret;
    }

    /**
	 * 
	 * exon�Aintron���̏����쐬����B
	 * 
	 * @param entryStart �G���g���[�J�n�ʒu
	 * @param entryEnd �G���g���[�I�~�ʒu
	 * @param strand �G���g���[��strand
	 * @param features FEATURE[]
	 * @param direction ����z��̕��
	 * @return exon�Aintron�����i�[����TextModeBean�̃��X�g
	 */
    private ArrayList createFeatures(String strand, FEATURE[] features, String direction) {
        ArrayList featureList = new ArrayList();
        int exonCount = 0;
        long prevEnd = -1;
        for (int i = 0; i < features.length; i++) {
            FEATURE feature = null;
            if (strand.equals("-")) {
                feature = features[features.length - i - 1];
            } else {
                feature = features[i];
            }
            if (featureList.size() > 0) {
                TextModeBean bean = new TextModeBean();
                bean.setId("Intron" + exonCount + "-" + (exonCount + 1));
                bean.setType("intron");
                bean.setStrand(strand);
                if (!strand.equals(direction)) {
                    bean.setStart(feature.getEND() + 1);
                    bean.setEnd(prevEnd - 1);
                    featureList.add(0, bean);
                } else {
                    bean.setStart(prevEnd + 1);
                    bean.setEnd(feature.getSTART() - 1);
                    featureList.add(bean);
                }
            }
            exonCount++;
            TextModeBean bean = new TextModeBean();
            bean.setId("Exon" + exonCount);
            bean.setName(feature.getLabel());
            bean.setType("exon");
            bean.setStrand(strand);
            bean.setStart(feature.getSTART());
            bean.setEnd(feature.getEND());
            if (!strand.equals(direction)) {
                prevEnd = feature.getSTART();
                featureList.add(0, bean);
            } else {
                prevEnd = feature.getEND();
                featureList.add(bean);
            }
        }
        return featureList;
    }

    /**
	 * 
	 * SNP�f�[�^�̂��߂̕\���p��������쐬����B
	 * 
	 * @param strand �G���g���[��strand
	 * @param entryStart �G���g���[�̊J�n�ʒu
	 * @param genotype genotype
	 * @param direction ����z��̕��
	 * @return �\���p������
	 */
    private String createSeqStrForSnp(String strand, long entryStart, String genotype, String direction, boolean highlight) {
        String retStr = genotype;
        if (!direction.equals(strand) && retStr.indexOf("*") < 0) retStr = TextModeManager.complement(retStr);
        if (highlight) retStr = "<b>" + retStr + "</b>";
        return retStr;
    }

    /**
	 * 
	 * �ȗ��\���̏ꍇ�A���̗��\�����邩�ǂ���
	 * �i�G���g���[�̖������C���Aintron�݂̂̃��C���͕\�����Ȃ��j
	 * 
	 * @param annotList �G���g���[���i�[����TextModeBean�̃��X�g
	 * @param lineStart �G���g���[�J�n�ʒu
	 * @param lineEnd �G���g���[�I�~�ʒu
	 * @return �\�����邩�ǂ���
	 */
    private boolean isShowLine(ArrayList annotList, int lineStart, int lineEnd) {
        boolean showLine = false;
        for (int i = 0; i < annotList.size(); i++) {
            TextModeBean bean = (TextModeBean) annotList.get(i);
            if (bean.getStart() <= lineEnd && bean.getEnd() >= lineStart) {
                if (bean.getFeatures() != null && bean.getFeatures().size() > 0) {
                    ArrayList featureList = bean.getFeatures();
                    for (int j = 0; j < featureList.size(); j++) {
                        TextModeBean feature = (TextModeBean) featureList.get(j);
                        if (feature.getStart() <= lineEnd && feature.getEnd() >= lineStart && (feature.getType().equals("snp") || feature.getType().equals("exon"))) {
                            showLine = true;
                            break;
                        }
                    }
                } else {
                    showLine = true;
                    break;
                }
            }
        }
        return showLine;
    }

    /**
	 * 
	 * �w�肳�ꂽ�͈͓��i�P�s���j�ɑ��݂���G���g���[�݂̂��i�[����
	 * TextModeBean�̃��X�g���쐬����
	 * 
	 * @param annotList �S�͈͂ɑ��݂���G���g���[�̃��X�g
	 * @param lineStart �J�n�ʒu
	 * @param lineEnd �I�~�ʒu
	 * @return �P�s���ɑ��݂���G���g���[�݂̂��i�[����TextModeBean�̃��X�g
	 */
    private ArrayList setAnnot(ArrayList annotList, int lineStart, int lineEnd) {
        ArrayList lineList = new ArrayList();
        for (int i = 0; i < annotList.size(); i++) {
            TextModeBean bean = (TextModeBean) annotList.get(i);
            if (bean.getStart() <= lineEnd && bean.getEnd() >= lineStart) {
                StringBuffer buf = new StringBuffer();
                if (bean.getType().equals("snp") || bean.getType().equals("CAGE tags")) {
                    for (int j = lineStart - start; j <= lineEnd - start; j++) {
                        if (j == bean.getStart() - start) {
                            buf.append(bean.getSeqStr());
                        } else {
                            buf.append(TextModeBean.CHAR_BLANK);
                        }
                    }
                } else {
                    String[] entrySeq = bean.getSeq();
                    for (int j = lineStart - start; j <= lineEnd - start; j++) {
                        if (j < entrySeq.length) {
                            buf.append(entrySeq[j]);
                        } else {
                            buf.append(TextModeBean.CHAR_BLANK);
                        }
                    }
                }
                String name = bean.getId();
                String type = "";
                if (bean.getFeatures() != null && bean.getFeatures().size() > 0) {
                    ArrayList featureList = bean.getFeatures();
                    for (int k = 0; k < featureList.size(); k++) {
                        TextModeBean feature = (TextModeBean) featureList.get(k);
                        if (feature.getStart() <= lineEnd && feature.getEnd() >= lineStart) {
                            name = name + " " + feature.getId();
                        }
                        if (feature.getType().equals("exon")) {
                            type = "exon";
                        } else if (feature.getType().equals("intron")) {
                            if (!type.equals("exon")) type = "intron";
                        }
                    }
                }
                TextModeBean lineBean = new TextModeBean();
                lineBean.setId(name);
                lineBean.setName(bean.getId() + ":" + bean.getDsId());
                lineBean.setStart(bean.getStart());
                lineBean.setEnd(bean.getEnd());
                lineBean.setStrand(bean.getStrand());
                lineBean.setSeqStr(buf.toString());
                lineBean.setLinkHref(bean.getLinkHref());
                lineBean.setColor(bean.getColor());
                lineBean.setSource(bean.getSource());
                lineBean.setType(type.length() == 0 ? bean.getType() : type);
                lineBean.setDsId(bean.getDsId());
                lineBean.setGroupIndex(bean.getGroupIndex());
                lineList.add(lineBean);
            }
        }
        return lineList;
    }

    /**
	 * 
	 * LineMode�f�[�^���擾���Adoc�ɃZ�b�g����B 
	 *
	 */
    private void setDoc() {
        DocumentBuilderFactory _factory = DocumentBuilderFactory.newInstance();
        _factory.setNamespaceAware(false);
        _factory.setValidating(false);
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = _factory.newDocumentBuilder();
            doc = builder.parse(sourceUrl);
        } catch (Exception e) {
            doc = null;
            System.out.println(sourceUrl);
            log.error(e);
        }
        setDoc(doc);
    }

    /**
	 * 
	 * ��r�Ώۂ�Strain��Genotype�����ꂼ��擾����
	 * 
	 * @param features FEATURE[]
	 * @param strain1 Strain1
	 * @param strain2 Strain2
	 * @return strain1��Genotype�Astrain2��Genotype�̔z��
	 */
    private String[] getHighlightGenotype(FEATURE[] features, String strain1, String strain2) {
        String genotype1 = "";
        String genotype2 = "";
        for (int i = 0; i < features.length; i++) {
            String[] strains = StringUtils.split(features[i].getLabel(), ",");
            for (int j = 0; j < strains.length; j++) {
                if (strains[j].equals(strain1)) genotype1 = features[i].getId();
                if (strains[j].equals(strain2)) genotype2 = features[i].getId();
            }
        }
        if (strain2.equals(TextModeBean.STRAIN_ID_ALL)) genotype2 = "ALL";
        String[] ret = { genotype1, genotype2 };
        return ret;
    }

    private TextModeBean setSnp(GROUP group, FEATURE feature, String direction, String[] highlightGenotypes, String description) {
        TextModeBean bean = new TextModeBean();
        boolean highlight = false;
        StringBuffer strainBuf = new StringBuffer();
        if (existStrainGenotype) {
            if (feature.getLabel() == null || feature.getLabel().length() == 0) {
                strainBuf.append("Strain Unknown");
            } else {
                if ((highlightGenotypes[1].equals(feature.getId()) || (highlightGenotypes[1].equals("ALL") && !feature.getId().equals(highlightGenotypes[0]))) && highlightGenotypes[0].length() > 0) {
                    highlight = true;
                }
                String[] strainIds = StringUtils.split(feature.getLabel(), ",");
                for (int m = 0; m < strainIds.length; m++) {
                    if (this.strainMap.containsKey(strainIds[m])) {
                        if (strainBuf.length() > 0) strainBuf.append(",");
                        strainBuf.append(this.strainMap.get(strainIds[m]));
                    }
                }
            }
            if (highlightGenotypes[0].equals(highlightGenotypes[1])) highlight = false;
        }
        bean.setId(group.getId());
        bean.setType(group.getType());
        bean.setStart(group.getSTART());
        bean.setEnd(group.getEND());
        bean.setStrand(group.getORIENTATION());
        bean.setLinkHref(group.getLINK().getHref());
        if (highlight) {
            bean.setColor("ff0000");
        } else {
            bean.setColor(group.getColor());
        }
        String allele = group.getLINK().getContent();
        String[] alleles = StringUtils.split(allele, "/");
        String alleleStr = "";
        boolean flag = false;
        for (int i = 0; i < alleles.length; i++) {
            if (alleles[i].length() > 1) {
                flag = true;
            }
        }
        if (allele.indexOf("-") >= 0 || flag) {
            alleleStr = feature.getId();
            if (!direction.equals(group.getORIENTATION())) alleleStr = complement(alleleStr);
            String tmp = "<font title='" + alleleStr + "'>*</font>";
            bean.setSeqStr(createSeqStrForSnp(group.getORIENTATION(), group.getSTART(), tmp, direction, highlight));
            alleleStr = " *=\"" + alleleStr + "\"";
        } else {
            bean.setSeqStr(createSeqStrForSnp(group.getORIENTATION(), group.getSTART(), feature.getId(), direction, highlight));
            alleleStr = " " + feature.getId() + "=";
        }
        if (existStrainGenotype) {
            bean.setSource(description + alleleStr + " " + strainBuf.toString());
        } else {
            bean.setSource(description + alleleStr);
        }
        return bean;
    }

    /**
	 *	�R���v�������g�ϊ��B
	 *
	 *	@return	�R���v�������g����<BR>
	 *			����f�[�^�����ݒ�̏ꍇ��null��Ԃ�
	 */
    public static String complement(String seq) {
        char[] chars = seq.toCharArray();
        char[] revChars = new char[chars.length];
        for (int j = 0; j < chars.length; j++) {
            char c = chars[chars.length - j - 1];
            if (c == 'A') {
                revChars[j] = 'T';
            } else if (c == 'C') {
                revChars[j] = 'G';
            } else if (c == 'G') {
                revChars[j] = 'C';
            } else if (c == 'T') {
                revChars[j] = 'A';
            } else if (c == 'M') {
                revChars[j] = 'K';
            } else if (c == 'R') {
                revChars[j] = 'Y';
            } else if (c == 'W') {
                revChars[j] = 'W';
            } else if (c == 'S') {
                revChars[j] = 'S';
            } else if (c == 'Y') {
                revChars[j] = 'R';
            } else if (c == 'K') {
                revChars[j] = 'M';
            } else if (c == 'a') {
                revChars[j] = 't';
            } else if (c == 'c') {
                revChars[j] = 'g';
            } else if (c == 'g') {
                revChars[j] = 'c';
            } else if (c == 't') {
                revChars[j] = 'a';
            } else if (c == 'm') {
                revChars[j] = 'k';
            } else if (c == 'r') {
                revChars[j] = 'y';
            } else if (c == 'w') {
                revChars[j] = 'w';
            } else if (c == 's') {
                revChars[j] = 's';
            } else if (c == 'y') {
                revChars[j] = 'r';
            } else if (c == 'k') {
                revChars[j] = 'm';
            } else {
                revChars[j] = c;
            }
        }
        return String.valueOf(revChars);
    }

    public static void main(String[] args) {
        String hHead = args[0];
        String hCheck = args[1];
        String sourceUrl = args[2];
        TextModeManager manager = new TextModeManager(hHead, hCheck, sourceUrl);
        manager.lineMode2TextMode("+");
    }
}
