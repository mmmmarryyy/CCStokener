package de.bea.services.vidya.client.datasource;

import java.awt.Color;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import de.bea.services.vidya.client.datasource.types.WSCourse;
import de.bea.services.vidya.client.datasource.types.WSCourseName;
import de.bea.services.vidya.client.datasource.types.WSCustomer;
import de.bea.services.vidya.client.datasource.types.WSLesson;
import de.bea.services.vidya.client.datasource.types.WSProject;
import de.bea.services.vidya.client.datasource.types.WSResponse;
import de.bea.services.vidya.client.datasource.types.WSRoleElem;
import de.bea.services.vidya.client.datasource.types.WSUnit;
import de.bea.services.vidya.client.datasource.types.WSUser;
import de.bea.services.vidya.client.datastructures.CAttentionComponent;
import de.bea.services.vidya.client.datastructures.CClasifComponent;
import de.bea.services.vidya.client.datastructures.CComponent;
import de.bea.services.vidya.client.datastructures.CCourse;
import de.bea.services.vidya.client.datastructures.CCourseName;
import de.bea.services.vidya.client.datastructures.CCustomer;
import de.bea.services.vidya.client.datastructures.CHotspot;
import de.bea.services.vidya.client.datastructures.CLesson;
import de.bea.services.vidya.client.datastructures.CMediaComponent;
import de.bea.services.vidya.client.datastructures.CPage;
import de.bea.services.vidya.client.datastructures.CProject;
import de.bea.services.vidya.client.datastructures.CQuizComponent;
import de.bea.services.vidya.client.datastructures.CRevealComponent;
import de.bea.services.vidya.client.datastructures.CTableComponent;
import de.bea.services.vidya.client.datastructures.CTextComponent;
import de.bea.services.vidya.client.datastructures.CTipComponent;
import de.bea.services.vidya.client.datastructures.CUnit;
import de.bea.services.vidya.client.datastructures.CUnitItem;
import de.bea.services.vidya.client.datastructures.TreeNode;
import de.bea.services.vidya.client.exceptions.ExplicantoException;
import de.beas.explicanto.client.I18N;
import de.beas.explicanto.client.rcp.projects.views.Downloader;

public class VidUtil {

    public static final String kEmptyStyleSheetType = " ";

    public static final String kEmptySkinType = " ";

    public static final String kEmptyLanguageType = " ";

    private static final String nonexMarkerF = "[ ";

    private static final String nonexMarkerR = " ]";

    private static final String nonexMarkerF_regex = "\\[ ";

    private static final String nonexMarkerR_regex = " \\]";

    private static Pattern mediaUrlPat = Pattern.compile("\\d+\\.\\w+");

    public static boolean properMediaUrl(String url) {
        return mediaUrlPat.matcher(url).matches();
    }

    public static String markNonex(String s) {
        return nonexMarkerF + s + nonexMarkerR;
    }

    public static String unmarkNonex(String s) {
        s = s.replaceFirst(nonexMarkerF_regex, "");
        return s.replaceFirst(nonexMarkerR_regex, "");
    }

    private static class RoleComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            WSRoleElem re1 = (WSRoleElem) o1, re2 = (WSRoleElem) o2;
            if (re1.getRoleID() < re2.getRoleID()) return -1; else if (re1.getRoleID() > re2.getRoleID()) return 1; else return (re1.getUserID() < re2.getUserID()) ? -1 : 1;
        }
    }

    public static boolean rolesMatch(List r1, List r2) {
        RoleComparator rc = new RoleComparator();
        Object[] ra1 = r1.toArray(), ra2 = r2.toArray();
        Arrays.sort(ra1, rc);
        Arrays.sort(ra2, rc);
        if (ra1.length != ra2.length) return false; else for (int i = 0; i < ra1.length; i++) if (((WSRoleElem) ra1[i]).getRoleID() != ((WSRoleElem) ra2[i]).getRoleID()) return false; else if (((WSRoleElem) ra1[i]).getUserID() != ((WSRoleElem) ra2[i]).getUserID()) return false;
        return true;
    }

    public static void printUserList(List list) {
        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            if (o instanceof WSUser) System.out.print("\"" + ((WSUser) o).getUsername() + "\"/" + ((WSUser) o).getUid() + " "); else if (o instanceof WSRoleElem) System.out.print("r" + ((WSRoleElem) o).getRoleID() + "/\"" + ((WSRoleElem) o).getUserID() + "\" "); else {
                System.out.print("cannot print this: " + clazzName(o));
                return;
            }
        }
        System.out.println();
    }

    public static void download(String url, Shell parentShell) throws Exception {
        FileOutputStream fos = null;
        File outFile = null;
        try {
            url = url.replace('\\', '/');
            outFile = new File(url.substring(url.lastIndexOf("/")));
            FileDialog fileDialog = new FileDialog(parentShell, SWT.SAVE);
            fileDialog.setFileName(outFile.getName());
            String str = fileDialog.open();
            if (str == null) return;
            outFile = new File(str);
            HttpURLConnection httpcon = (HttpURLConnection) new URL(url).openConnection();
            httpcon.setInstanceFollowRedirects(true);
            if (httpcon.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) VidUtil.openURL(url);
            int max = httpcon.getContentLength();
            BufferedInputStream in = new BufferedInputStream(httpcon.getInputStream());
            fos = new FileOutputStream(outFile);
            ProgressMonitorDialog progress = new ProgressMonitorDialog(parentShell);
            progress.create();
            progress.getShell().setText(I18N.translate("dialog.expDownload.title"));
            Downloader dw = new Downloader(I18N.translate("dialog.expDownload.task") + ": " + url, in, fos, max);
            progress.setBlockOnOpen(false);
            progress.setOpenOnRun(true);
            progress.setCancelable(true);
            progress.run(true, true, dw);
        } catch (InterruptedException cancel) {
            fos.close();
            outFile.delete();
        } catch (Exception e) {
            throw new ExplicantoException(new WSResponse(2000, "\"" + url + "\" konnte nicht geladen werden:\n" + e.getMessage(), null, null));
        }
    }

    public static Object[] append(Object[] list, Object newElem) {
        Object[] temp = new Object[1];
        temp[0] = newElem;
        return append(list, temp);
    }

    public static Object[] append(Object[] list, Object[] newElems) {
        int le1 = list != null ? list.length : 0;
        int le2 = newElems != null ? newElems.length : 0;
        Object[] erg = new Object[le1 + le2];
        for (int i = 0; i < le1; i++) erg[i] = list[i];
        for (int i = 0; i < le2; i++) erg[le1 + i] = newElems[i];
        return erg;
    }

    public static int getPos(Object[] list, Object o) {
        int le = list != null ? list.length : 0;
        for (int i = 0; i < le; i++) if (list[i] == o) return i;
        return -1;
    }

    public static Object[] pruneAfter(Object[] list, Object lastElem) {
        int pos = getPos(list, lastElem);
        if (pos >= 0) {
            Object[] erg = new Object[pos + 1];
            for (int i = 0; i <= pos; i++) erg[i] = list[i];
            return erg;
        } else return list;
    }

    public static Object[] invert(Object[] list) {
        int le = list != null ? list.length : 0;
        Object[] erg = new Object[le];
        for (int i = 0; i < le; i++) erg[le - i - 1] = list[i];
        return erg;
    }

    public static Icon scaledIcon(ImageIcon i, int iHeight, int iWidth, int maxEdge) {
        if (iHeight <= maxEdge && iWidth <= maxEdge) return i; else {
            double proportion = (double) iWidth / (double) iHeight;
            if (proportion > 1) return new ImageIcon(i.getImage().getScaledInstance(maxEdge, (int) (maxEdge / proportion), Image.SCALE_FAST)); else return new ImageIcon(i.getImage().getScaledInstance((int) (maxEdge * proportion), maxEdge, Image.SCALE_FAST));
        }
    }

    public static String rgbString(Color color) {
        return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }

    public static String readFile(String path) {
        StringBuffer contents = new StringBuffer();
        try {
            BufferedReader input = new BufferedReader(new FileReader(path));
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contents.toString();
    }

    public static byte[] readByteFile(String path) throws Exception {
        FileInputStream fis = new FileInputStream(path);
        byte[] erg = new byte[fis.available()];
        fis.read(erg);
        fis.close();
        return erg;
    }

    public static void writeByteFile(byte[] b, String path) throws Exception {
        FileOutputStream fos = new FileOutputStream(path);
        if (b != null) fos.write(b);
        fos.close();
    }

    public static void copyFile(File source, File dest) throws IOException {
        FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(dest);
        byte b[] = new byte[5000];
        int bRead = 0;
        do {
            bRead = in.read(b);
            if (bRead != -1) out.write(b, 0, bRead);
        } while (bRead != -1);
        in.close();
        out.close();
    }

    public static String clazzName(Object o) {
        if (o != null) return o.getClass().getName().replaceFirst(o.getClass().getPackage().getName() + ".", ""); else return "";
    }

    public static void openURL(String url) throws Exception {
        if (url != null) {
            String OS = System.getProperty("os.name").toLowerCase();
            if ((OS.indexOf("nt") > -1) || (OS.indexOf("windows 2000") > -1) || (OS.indexOf("windows xp") > -1)) Runtime.getRuntime().exec("cmd.exe /c START " + url); else if (OS.indexOf("mac os x") > -1) Runtime.getRuntime().exec("open -a \"Safari\" " + url); else if (OS.indexOf("linux") > -1) Runtime.getRuntime().exec("konqueror " + url); else throw new ExplicantoException(new WSResponse(2000, "Betriebssystem nicht unterst�tzt: " + OS, null, null));
        }
    }

    public static void openURLInIE(String url) throws IOException {
        if (url != null) Runtime.getRuntime().exec("cmd.exe /c START Iexplore " + url);
    }

    public static boolean runningOnWindows() {
        String OS = System.getProperty("os.name").toLowerCase();
        if ((OS.indexOf("nt") > -1) || (OS.indexOf("windows 2000") > -1) || (OS.indexOf("windows xp") > -1)) return true;
        return false;
    }

    public static boolean above(Class c1, Class c2) {
        if (c1 == c2) return false; else if (c1 == WSCustomer.class) return c2 == WSProject.class || c2 == WSCourseName.class || c2 == WSCourse.class || c2 == WSLesson.class || c2 == WSUnit.class; else if (c1 == WSProject.class) return c2 == WSCourseName.class || c2 == WSCourse.class || c2 == WSLesson.class || c2 == WSUnit.class; else if (c1 == WSCourseName.class) return c2 == WSCourse.class || c2 == WSLesson.class || c2 == WSUnit.class; else if (c1 == WSCourse.class) return c2 == WSLesson.class || c2 == WSUnit.class; else if (c1 == WSLesson.class) return c2 == WSUnit.class; else return true;
    }

    /**
	 * Method to access fields originally inherited from WSTypeBase. Class
	 * transfer via WebServices doesn't maintain the class hierarchy.
	 */
    public static String getLockTime(Object o) {
        if (o instanceof WSCustomer) return ((WSCustomer) o).getLockTime(); else if (o instanceof WSProject) return ((WSProject) o).getLockTime(); else if (o instanceof WSCourseName) return ((WSCourseName) o).getLockTime(); else if (o instanceof WSCourse) return ((WSCourse) o).getLockTime(); else if (o instanceof WSLesson) return ((WSLesson) o).getLockTime(); else if (o instanceof WSUnit) return ((WSUnit) o).getLockTime(); else return "";
    }

    /**
	 * @param source
	 * @param target
	 *            An alternative to object orientation. This way all transfer
	 *            methods for non-primitive members are at the same place.
	 */
    public static void transferAttributes(Object source, Object target) {
        if (source != null && target != null) {
            transferPrimitiveAttributes(source, target);
            if (source instanceof TreeNode && target instanceof TreeNode) {
                if (((TreeNode) source).getParent() != null) ((TreeNode) target).setParent(((TreeNode) source).getParent());
                ((TreeNode) target).getRoles().clear();
                for (Iterator iter = ((TreeNode) source).getRoles().iterator(); iter.hasNext(); ) {
                    WSRoleElem role = (WSRoleElem) iter.next();
                    ((TreeNode) target).getRoles().add(new WSRoleElem(role.getRoleID(), role.getUserID()));
                }
            }
            if (source instanceof CUnit && target instanceof CUnit) {
                CUnit tempUnit = ((CUnit) source).cloneUnit();
                ((CUnit) target).replaceAllUnitItems(tempUnit.getChildren());
            } else if (source instanceof CCustomer && target instanceof CCustomer) {
                ((CCustomer) target).copyAttributesFrom((CCustomer) source);
            }
        }
    }

    /**
	 * @param source
	 * @param target
	 *            This method universally transfers each attribute accessible
	 *            via getter and setter methods except for non-primitive types.
	 */
    public static void transferPrimitiveAttributes(Object source, Object target) {
        Method[] methods = target.getClass().getMethods();
        Method method;
        String name;
        for (int i = 0; i < methods.length; i++) {
            name = methods[i].getName();
            if (name.startsWith("set")) {
                try {
                    method = source.getClass().getMethod(name.replaceFirst("set", "get"), null);
                } catch (NoSuchMethodException nsm) {
                    try {
                        method = source.getClass().getMethod(name.replaceFirst("set", "is"), null);
                    } catch (NoSuchMethodException nsm2) {
                        continue;
                    }
                }
                if (method.getReturnType().isPrimitive() | method.getReturnType().getName().compareTo("java.lang.String") == 0) {
                    try {
                        Object obj = method.invoke(source, null);
                        if (obj != null) {
                            methods[i].invoke(target, new Object[] { obj });
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public static void setNodeName(TreeNode node, String name) {
        if (node instanceof CProject) ((CProject) node).setProjectName(name);
        if (node instanceof CCourse) ((CCourse) node).setCourseTitle(name); else if (node instanceof CLesson) ((CLesson) node).setLessonTitle(name); else if (node instanceof CUnit) ((CUnit) node).setUnitTitle(name); else if (node instanceof CPage) ((CPage) node).setPageTitle(name);
    }

    public static String getNodeName(TreeNode element) {
        if (element instanceof CCustomer) return ((CCustomer) element).getCustomerName(); else if (element instanceof CProject) return ((CProject) element).getProjectName(); else if (element instanceof CCourseName) {
            if (!((CCourseName) element).hasMasterCourse()) {
                return I18N.translate("outlineView.labels.noMasterCourse");
            } else return ((CCourseName) element).getMasterCourse().getCourseTitle();
        } else if (element instanceof CCourse) return ((CCourse) element).getCourseTitle() + "." + ((CCourse) element).getLanguageSuffix(); else if (element instanceof CLesson) return ((CLesson) element).getLessonTitle(); else if (element instanceof CUnit) return ((CUnit) element).getUnitTitle(); else if (element instanceof CUnitItem) return ((CUnitItem) element).getPageType(); else if (element instanceof CPage) return ((CPage) element).getPageTitle(); else if (element instanceof CComponent) {
            CComponent comp = (CComponent) element;
            if (element instanceof CTableComponent) return I18N.translate("components.names.table") + " " + comp.getId(); else if (element instanceof CTextComponent) return I18N.translate("components.names.text") + " " + comp.getId(); else if (element instanceof CTipComponent) return I18N.translate("components.names.tip") + " " + comp.getId(); else if (element instanceof CAttentionComponent) return I18N.translate("components.names.attention") + " " + comp.getId(); else if (element instanceof CQuizComponent) return I18N.translate("components.names.quiz") + " " + comp.getId(); else if (element instanceof CHotspot) return ((CHotspot) element).getName(); else if (element instanceof CClasifComponent) return ((CClasifComponent) element).getName(); else if (element instanceof CMediaComponent) return I18N.translate("components.names.media") + " " + comp.getId(); else if (element instanceof CRevealComponent) return I18N.translate("components.names.reveal") + " " + comp.getId();
            return ((CComponent) element).getText();
        }
        return element != null ? element.toString() : "null node";
    }

    public static class FileExtFilter extends javax.swing.filechooser.FileFilter {

        String ext = "", description = "";

        public FileExtFilter(String extension, String desc) {
            ext = extension;
            description = desc;
        }

        public boolean accept(File f) {
            if (f.isDirectory()) return true; else if (f != null) return f.getName().endsWith("." + ext); else return false;
        }

        public String getDescription() {
            return description;
        }
    }
}
