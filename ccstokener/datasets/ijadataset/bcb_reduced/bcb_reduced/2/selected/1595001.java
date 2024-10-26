package webOffline;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 *
 * @author tktung
 */
public class RequestPages implements Runnable {

    public Thread thread;

    private ArrayList<String> arrayDownloadedURL = new ArrayList<String>();

    private String projectXMLFile;

    private String session;

    private ProjectDownloadManager projectDM;

    RequestPages(ProjectDownloadManager pdm, String filePath, String session) {
        this.projectDM = pdm;
        this.projectXMLFile = filePath;
        this.session = session;
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        try {
            this.projectDM.setStartingAddress(new ProjectDownloadLinks(this.projectDM.getProjectAddress().getAddress(), 1, this.projectDM.getProjectAddress().getProjectName(), this.projectDM.getProjectAddress().getSaveTo()));
            download(this.projectDM.getStartingAddress(), 3);
            JOptionPane.showMessageDialog(null, "project " + projectDM.getProjectAddress().getProjectName() + " has finished!");
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    public void download(ProjectDownloadLinks projDownloadLink, int upToLink) {
        try {
            for (; upToLink > 0; --upToLink) {
                downloadWebFile(projDownloadLink.getUrl(), projDownloadLink.getSaveTo() + projDownloadLink.getName() + ".html");
                parseHtmlFile(projDownloadLink.getSaveTo() + projDownloadLink.getName() + ".html", projDownloadLink);
                for (int idx = 0; idx < projDownloadLink.getArrayResourceLinks().size(); ++idx) {
                    downloadFiles(projDownloadLink.getArrayResourceLinks().get(idx), projDownloadLink.getArrayResourcePaths().get(idx));
                }
                DOMXml.saveProject(projectDM, projectDM.getProjectAddress().getSaveTo());
                for (int idx = 0; idx < projDownloadLink.getArrayLinks().size(); ++idx) {
                    download(projDownloadLink.getArrayLinks().get(idx), upToLink - 1);
                }
                DOMXml.saveProject(projectDM, projectDM.getProjectAddress().getSaveTo());
            }
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    public void downloadWebFile(String connectionString, String fileName) {
        try {
            if (createFolder(fileName) == false) {
                return;
            }
            URL url = new URL(connectionString);
            URLConnection urlCon = url.openConnection();
            urlCon.setRequestProperty("User-agent", "Mozilla/4.0");
            DataInputStream in = new DataInputStream(urlCon.getInputStream());
            FileOutputStream out = new FileOutputStream(fileName);
            byte[] buf = new byte[1024];
            int dem;
            while ((dem = in.read(buf)) > 0) {
                out.write(buf, 0, dem);
            }
            in.close();
            out.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public void downloadFiles(String connectionString, String fileName) {
        try {
            if (createFolder(fileName) == false) {
                return;
            }
            URL url = new URL(connectionString);
            URLConnection urlCon = url.openConnection();
            DataInputStream in = new DataInputStream(urlCon.getInputStream());
            FileOutputStream out = new FileOutputStream(fileName);
            byte[] buf = new byte[1024];
            int dem;
            while ((dem = in.read(buf)) > 0) {
                out.write(buf, 0, dem);
            }
            out.close();
            in.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public void parseHtmlFile(String fileName, ProjectDownloadLinks projDLink) {
        try {
            BufferedReader re = new BufferedReader(new FileReader(fileName));
            String data = "";
            String line;
            while ((line = re.readLine()) != null) {
                data += line + "\n";
            }
            re.close();
            data = getResources(data, "\\s*src=\\s*" + ".*?[\\s,>]", " src=", '"', '"', projDLink);
            data = getResources(data, "\\s*url[(].*?[)]", " url", '(', ')', projDLink);
            data = getLinks(data, projDLink);
            BufferedWriter wr = new BufferedWriter(new FileWriter(fileName));
            String[] slip = data.split("\n");
            for (int i = 0; i < slip.length; i++) {
                wr.write(slip[i]);
                wr.newLine();
            }
            wr.flush();
            wr.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public String getLinks(String data, ProjectDownloadLinks projDLink) {
        String startingAddress = projDLink.getUrl();
        String domainName = getDomainName(startingAddress);
        Pattern pattern = Pattern.compile("\\s*?href\\s*?=\\s*?.*?[\\s,>]");
        Matcher matcher = pattern.matcher(data);
        while (matcher.find() == true) {
            int begin = matcher.group().indexOf("=", matcher.group().indexOf("href")) + 1;
            int end = matcher.group().length() - 1;
            String url = matcher.group().substring(begin, end);
            url = url.replace(" ", "");
            url = url.replace("'", "");
            url = url.replace("\"", "");
            url = url.replace("(", "");
            url = url.replace(")", "");
            if (url.startsWith("#") == true) {
                continue;
            }
            String path = url;
            if (url.contains("http://") == true || url.contains("https://") == true) {
                begin = url.indexOf("//") + 2;
                end = url.indexOf("/", begin);
                String str;
                if (end != -1) {
                    str = url.substring(begin, end);
                } else {
                    str = url.substring(begin);
                }
                begin = str.indexOf("www.");
                if (begin != -1) {
                    str = str.substring(begin + 4);
                }
                if (startingAddress.contains(str) == false) {
                    continue;
                }
                path = url.substring(end);
            } else if (url.startsWith("../")) {
                url = url.substring(3);
            } else if (url.startsWith("/")) {
                url = url.substring(1);
            } else {
                url = domainName + '/' + url;
            }
            if (path.startsWith("/") == true) {
                path = path.substring(1);
            }
            if (path.endsWith("/") == true) {
                path += "index.html";
            }
            path = path.replace("?", "-");
            if (hasURL(projDLink.getArrayLinks(), url) == false) {
                try {
                    projDLink.addToArrayLinks(new ProjectDownloadLinks(projDLink.getUrl(), projDLink.getLevel() + 1, projDLink.getName(), projDLink.getSaveTo()));
                    projDLink.addToArrayFiles(new ProjectDownloadFiles(url));
                } catch (Exception ex) {
                    Logger.getLogger(RequestPages.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            data = data.replace(url, path);
        }
        return data;
    }

    public boolean hasURL(ArrayList<ProjectDownloadLinks> arrayLinks, String url) {
        for (int idx = 0; idx < arrayLinks.size(); ++idx) {
            if (arrayLinks.get(idx).getUrl().contains(url)) {
                return true;
            }
        }
        return false;
    }

    public String getDomainName(String startingAddress) {
        int begin = startingAddress.indexOf("//") + 2;
        int end = startingAddress.indexOf("/", begin);
        if (end != -1) {
            return startingAddress.substring(0, end);
        } else {
            return startingAddress;
        }
    }

    public String getResources(String data, String regex, String att, char cBegin, char cEnd, ProjectDownloadLinks projDLink) {
        String startingAddress = projDLink.getUrl();
        String domainName = getDomainName(startingAddress);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);
        while (matcher.find() == true) {
            int begin = matcher.group().indexOf('=');
            if (begin == -1) {
                begin = matcher.group().indexOf(cBegin);
            }
            begin += 1;
            int end = matcher.group().length() - 1;
            String url = matcher.group().substring(begin, end);
            url = url.replace(" ", "");
            url = url.replace("'", "");
            url = url.replace("\"", "");
            url = url.replace("(", "");
            url = url.replace(")", "");
            String resourceURL = url;
            String path = url;
            System.out.println(url);
            if (url.contains("http://") == true || url.contains("https://") == true) {
                begin = url.indexOf("//") + 2;
                end = url.indexOf("/", begin);
                path = url.substring(end + 1);
            } else if (url.startsWith("../")) {
                url = url.substring(3);
            } else if (url.startsWith("/")) {
                url = url.substring(1);
            } else {
                try {
                    resourceURL = domainName + '/' + url;
                    URL _url = new URL(resourceURL);
                    URLConnection urlCon = _url.openConnection();
                    DataInputStream in = new DataInputStream(urlCon.getInputStream());
                } catch (Exception ex) {
                    System.err.println(ex);
                    resourceURL = startingAddress + '/' + url;
                }
            }
            if (path.startsWith("/") == true) {
                path = path.substring(1);
            }
            path = path.replace("?", "-");
            String pathResource = projDLink.getSaveTo() + path;
            if (hasResourceURL(projDLink.getArrayResourceLinks(), url) == false) {
                projDLink.addToArrayResourceLinks(resourceURL);
                projDLink.addToArrayResoucePaths(pathResource);
            }
            data = data.replace(url, path);
        }
        return data;
    }

    public boolean hasResourceURL(ArrayList<String> arrayResource, String url) {
        for (int idx = 0; idx < arrayResource.size(); ++idx) {
            if (arrayResource.get(idx).contains(url)) {
                return true;
            }
        }
        return false;
    }

    public boolean createFolder(String folderName) {
        boolean value = false;
        File file = new File(folderName);
        if (file.exists() == true) {
            value = true;
        } else {
            int end = folderName.lastIndexOf("/");
            folderName = folderName.substring(0, end);
            file = new File(folderName);
            file.mkdirs();
            value = true;
        }
        return value;
    }

    public ArrayList<ProjectDownloadFiles> filter(ArrayList<webOffline.Pattern> listPattern, ArrayList<ProjectDownloadFiles> listFile, ProjectDownloadLinks projDLink) {
        String startingAddress = projDLink.getUrl();
        String domainName = getDomainName(startingAddress);
        for (int i = 0; i < listPattern.size(); i++) {
            webOffline.Pattern pattern = listPattern.get(i);
            ArrayList<Extension> listExtension = pattern.getArrayExtension();
            String path = domainName + pattern.getPattern();
            path = path.replace("//", "/");
            int index = path.lastIndexOf("/");
            path = path.substring(0, index);
            if (startingAddress.contains(path) == true) {
                PermissionPattern permissionPattern = pattern.getPermission();
                if (permissionPattern == PermissionPattern.ALLOW_ALL) {
                    break;
                } else if (permissionPattern == PermissionPattern.DENY_ALL) {
                    listFile = new ArrayList<ProjectDownloadFiles>();
                    break;
                } else if (permissionPattern == PermissionPattern.ALLOW_ITEMS) {
                    ArrayList<ProjectDownloadFiles> temp = new ArrayList<ProjectDownloadFiles>();
                    for (int j = 0; j < listExtension.size(); j++) {
                        Extension extension = listExtension.get(j);
                        if (extension.getPermission() == PermissionExtension.ALLOW) {
                            for (int k = 0; k < listFile.size(); k++) {
                                ProjectDownloadFiles file = listFile.get(k);
                                if (extension.getExtension().equals(file.getExtension()) == true) {
                                    temp.add(file);
                                }
                            }
                        }
                    }
                    listFile = temp;
                    break;
                } else if (permissionPattern == PermissionPattern.DENY_ITEMS) {
                    ArrayList<ProjectDownloadFiles> temp = listFile;
                    for (int j = 0; j < listExtension.size(); j++) {
                        Extension extension = listExtension.get(j);
                        if (extension.getPermission() == PermissionExtension.DENY) {
                            for (int k = 0; k < temp.size(); k++) {
                                ProjectDownloadFiles file = temp.get(k);
                                if (extension.getExtension().equals(file.getExtension()) == true) {
                                    temp.remove(k);
                                    k--;
                                }
                            }
                        }
                    }
                    listFile = temp;
                    break;
                }
            }
        }
        return listFile;
    }
}
