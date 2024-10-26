package com.mth480.rebasedownloader;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
import com.thoughtworks.xstream.*;

public class RebaseDataRetriever {

    private static String urlAddress = "http://rebase.neb.com:80/rebase/link_parsrefs";

    private static String relativeDataFileLocation = "rebaseData/rebaseDataFile.data";

    private static String relativeXmlFileLocation = "rebaseData/";

    static ArrayList<RestrictionEnzymeData> completeEnzymeList;

    /**
	 * If an paremeter is null or empty it will be ignored during filtering.
	 * You should check for updated rebase file before using this!
	 * @param enzymeName
	 * @param prototype
	 * @param microorganism
	 * @param methSiteAndType
	 * @param commercialSource
	 * @return List of RestrictionEnzymes meeting filters passed in
	 */
    public static ArrayList<RestrictionEnzymeData> QueryRebaseFile(String enzymeName, String prototype, String microorganism, String methSiteAndType, String commercialSource, boolean doAnd) {
        if (!LocalRebaseFileExsists() || CheckForUpdatedRebaseDBFile()) {
            UpdateRebaseDBFile();
            ConvertRebaseDbIntoXmlFiles();
        }
        return queryRebaseFile(enzymeName, prototype, microorganism, methSiteAndType, commercialSource, doAnd);
    }

    private static ArrayList<RestrictionEnzymeData> queryRebaseFile(String enzymeName, String prototype, String microorganism, String methSiteAndType, String commercialSource, boolean doAnd) {
        enzymeName = enzymeName.trim().toLowerCase();
        prototype = prototype.trim().toLowerCase();
        microorganism = microorganism.trim().toLowerCase();
        methSiteAndType = methSiteAndType.trim().toLowerCase();
        commercialSource = commercialSource.trim().toLowerCase();
        if (completeEnzymeList == null || completeEnzymeList.size() == 0) completeEnzymeList = loadRebaseDataFromXml();
        ArrayList<RestrictionEnzymeData> resultsList = new ArrayList<RestrictionEnzymeData>();
        for (RestrictionEnzymeData reData : completeEnzymeList) {
            if (doAnd) {
                if (enzymeName.length() != 0) {
                    if (reData.enzymeName == null) {
                        continue;
                    } else if (reData.enzymeName.toLowerCase().lastIndexOf(enzymeName) == -1) {
                        continue;
                    }
                }
                if (prototype.length() != 0) {
                    if (reData.prototype == null) {
                        continue;
                    } else if (reData.prototype.toLowerCase().lastIndexOf(prototype) == -1) {
                        continue;
                    }
                }
                if (microorganism.length() != 0) {
                    if (reData.microorganism == null) {
                        continue;
                    } else if (reData.microorganism.toLowerCase().lastIndexOf(microorganism) == -1) {
                        continue;
                    }
                }
                if (methSiteAndType.length() != 0) {
                    if (reData.methylationSiteAndType == null) {
                        continue;
                    } else if (reData.methylationSiteAndType.toLowerCase().lastIndexOf(methSiteAndType) == -1) {
                        continue;
                    }
                }
                if (commercialSource.length() != 0) {
                    if (reData.commercialSource == null) {
                        continue;
                    } else if (reData.commercialSource.toLowerCase().lastIndexOf(commercialSource) == -1) {
                        continue;
                    }
                }
                resultsList.add(reData);
            } else {
                if (enzymeName.length() != 0 && reData.enzymeName != null && reData.enzymeName.toLowerCase().lastIndexOf(enzymeName) != -1) resultsList.add(reData); else if (prototype.length() != 0 && reData.prototype != null && reData.prototype.toLowerCase().lastIndexOf(prototype) != -1) resultsList.add(reData); else if (microorganism.length() != 0 && reData.microorganism != null && reData.microorganism.toLowerCase().lastIndexOf(microorganism) != -1) resultsList.add(reData); else if (methSiteAndType.length() != 0 && reData.methylationSiteAndType != null && reData.methylationSiteAndType.toLowerCase().lastIndexOf(methSiteAndType) != -1) resultsList.add(reData); else if (commercialSource.length() != 0 && reData.commercialSource != null && reData.commercialSource.toLowerCase().lastIndexOf(commercialSource) != -1) resultsList.add(reData);
            }
        }
        return resultsList;
    }

    private static ArrayList<RestrictionEnzymeData> loadRebaseDataFromXml() {
        XStream xstream = new XStream();
        ArrayList<RestrictionEnzymeData> list = new ArrayList<RestrictionEnzymeData>();
        File dir = new File(relativeXmlFileLocation);
        String[] children = dir.list();
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".rxml");
            }
        };
        children = dir.list(filter);
        int count = 0;
        try {
            for (int i = 0; i < children.length; i++) {
                BufferedReader in = new BufferedReader(new FileReader(relativeXmlFileLocation + children[i]));
                list.add((RestrictionEnzymeData) xstream.fromXML(in));
                in.close();
                count++;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return list;
    }

    public static void ConvertRebaseDbIntoXmlFiles() {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(relativeDataFileLocation));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            int state = 1;
            RestrictionEnzymeData currentEnzyme = new RestrictionEnzymeData();
            int enzymeNumber = 0;
            while ((strLine = br.readLine()) != null) {
                strLine = strLine.trim();
                if (strLine.equals("References:")) break; else if (strLine.length() < 3) state = 1; else if (state == 1) {
                    currentEnzyme = new RestrictionEnzymeData();
                    if (strLine.charAt(1) == '1') {
                        currentEnzyme.enzymeName = strLine.substring(3);
                        state++;
                    }
                } else if (state == 2) {
                    if (strLine.charAt(1) == '2') {
                        if (strLine.length() >= 4) currentEnzyme.prototype = strLine.substring(3);
                        state++;
                    } else state = 1;
                } else if (state == 3) {
                    if (strLine.charAt(1) == '3') {
                        if (strLine.length() >= 4) currentEnzyme.microorganism = strLine.substring(3);
                        state++;
                    } else state = 1;
                } else if (state == 4) {
                    if (strLine.charAt(1) == '4') {
                        if (strLine.length() >= 4) currentEnzyme.source = strLine.substring(3);
                        state++;
                    } else state = 1;
                } else if (state == 5) {
                    if (strLine.charAt(1) == '5') {
                        currentEnzyme.recognitionSequence = strLine.substring(3);
                        state++;
                    } else state = 1;
                } else if (state == 6) {
                    if (strLine.charAt(1) == '6') {
                        if (strLine.length() >= 4) currentEnzyme.methylationSiteAndType = strLine.substring(3);
                        state++;
                    } else state = 1;
                } else if (state == 7) {
                    if (strLine.charAt(1) == '7') {
                        if (strLine.length() >= 4) currentEnzyme.commercialSource = strLine.substring(3);
                        state++;
                    } else state = 1;
                } else if (state == 8) {
                    if (strLine.charAt(1) == '8') {
                        currentEnzyme.FileNumber = enzymeNumber;
                        CreateResourceFile(currentEnzyme, enzymeNumber);
                        enzymeNumber++;
                        state = 1;
                    } else state = 1;
                } else state = 1;
            }
            in.close();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }

    private static void CreateResourceFile(RestrictionEnzymeData REdata, int FileNumber) throws IOException {
        XStream xstream = new XStream();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(relativeXmlFileLocation + "/" + FileNumber + ".rxml"));
            out.write(xstream.toXML(REdata));
            out.close();
        } catch (IOException e) {
        }
    }

    /**
	 * stuff related to getting data file from rebase
	 */
    public static boolean LocalRebaseFileExsists() {
        File file = new File(relativeDataFileLocation);
        if (file.length() == 0F) return false;
        return file.exists();
    }

    /**
	 * Checks rebase site to see if there is a newer rebase data file than on local machine
	 * @return true if DB version on local machine is older than one on Rebase site
	 */
    public static boolean CheckForUpdatedRebaseDBFile() {
        Date officalDate = new Date();
        try {
            URL url = new URL(urlAddress);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)");
            DateFormat df = new SimpleDateFormat("dd MMM yyyy");
            officalDate = (Date) df.parse(conn.getHeaderField("Last-modified").substring(5, 16));
        } catch (Exception e) {
        }
        File file = new File(relativeDataFileLocation);
        if (file.lastModified() == 0L) return true;
        Date modifiedTime = new Date(file.lastModified());
        return officalDate.compareTo(modifiedTime) > 0;
    }

    public static boolean UpdateRebaseDBFile() {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(urlAddress);
            File folder = new File(relativeXmlFileLocation);
            if (!folder.exists()) folder.mkdir();
            out = new BufferedOutputStream(new FileOutputStream(relativeDataFileLocation));
            conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)");
            int totalSize = Integer.parseInt(conn.getHeaderField("content-length"));
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
        return true;
    }

    /**
	 * Tester
	 */
    public static void main(String[] args) {
        QueryRebaseFile("Aa", "", "", "", "", true);
        System.out.println("2nd search");
        QueryRebaseFile("AagI", "", "", "", "", true);
        System.out.println("done");
    }
}
