import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class AlbumGen implements Comparable {

    static int imagesPerPage = 10;

    static int thumbArea = 12000;

    static float imageQuality = .9f;

    static float thumbQuality = .75f;

    static String untitled;

    static final int INDEX_BY_DATE = 1;

    static int imageCount;

    private static void printUsage() {
        System.err.println("Usage: [options] album_root");
        System.err.println("  -d <dir>             Output to <dir>");
        System.err.println("  -area n              Output thumbnails with area n");
        System.err.println("  -qi f                Set image quality to f");
        System.err.println("  -qt f                Set thumbnail quality to f");
        System.err.println("  -size n              Output n images per page");
        System.err.println("  -hasparent           Output a .. folder, even in the root");
        System.err.println("  -autorun             Output an AUTORUN.INF file");
        System.err.println("  -untitled \"caption\"  Specify a caption for untitled pictures");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        File albumRoot = null;
        File outDir = null;
        boolean hasParent = false;
        boolean autorun = false;
        boolean index = false;
        int indexFlags = 0;
        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].charAt(0) == '-') {
                    if (args[i].equals("-d")) outDir = new File(args[++i]); else if (args[i].equals("-area")) thumbArea = Integer.parseInt(args[++i]); else if (args[i].equals("-qi")) imageQuality = Float.parseFloat(args[++i]); else if (args[i].equals("-qt")) thumbQuality = Float.parseFloat(args[++i]); else if (args[i].equals("-size")) imagesPerPage = Integer.parseInt(args[++i]); else if (args[i].equals("-hasparent")) hasParent = true; else if (args[i].equals("-index")) index = true; else if (args[i].equals("-bydate")) indexFlags |= INDEX_BY_DATE; else if (args[i].equals("-autorun")) autorun = true; else if (args[i].equals("-untitled")) untitled = args[++i]; else throw new IllegalArgumentException(args[i]);
                } else albumRoot = new File(args[i]);
            }
        } catch (Exception e) {
            printUsage();
        }
        if (outDir == null) {
            outDir = new File(albumRoot, "../");
        }
        if (albumRoot == null) printUsage();
        AlbumGen top = new AlbumGen(null, outDir, false);
        AlbumGen gen = new AlbumGen(albumRoot, new File(outDir, albumRoot.getName()), true);
        if (!gen.generate()) System.exit(1);
        top.childAlbums.add(gen);
        System.out.println(imageCount + " images");
        if (index) {
            System.out.println("here");
            if (!index(top, gen.outDir, outDir, indexFlags)) System.exit(1);
        }
        top.output();
        if (autorun) {
            System.out.println("Generating View Album.html...");
            File f = new File(outDir, "View Album.html");
            PrintWriter pw = new PrintWriter(new FileOutputStream(f));
            pw.println("<HTML>");
            pw.println("<HEAD>");
            pw.println("<META http-equiv=\"refresh\" content=\"0;url=index.html\" >");
            pw.println("</HEAD>");
            pw.println("<body><a href=\"index.html\">Click here for album</A>");
            pw.println("</HTML>");
            pw.close();
            System.out.println("Generating AUTORUN.BAT...");
            f = new File(outDir, "AUTORUN.BAT");
            pw = new PrintWriter(new FileOutputStream(f));
            pw.println("rundll32.exe shell32.dll ShellExec_RunDLL index.html");
            pw.close();
            System.out.println("Generating AUTORUN.INF...");
            f = new File(outDir, "AUTORUN.INF");
            pw = new PrintWriter(new FileOutputStream(f));
            pw.println("[autorun]");
            pw.println("open=\"AUTORUN.BAT\"");
            pw.close();
        }
        System.exit(0);
    }

    private File dir;

    private File outDir;

    private String title;

    private List images = new ArrayList();

    private List childAlbums = new ArrayList();

    private boolean hasParent;

    private AlbumGen(File dir, File outDir, boolean hasParent) {
        if (outDir == null) outDir = dir;
        this.dir = dir;
        this.outDir = outDir;
        this.hasParent = hasParent;
        title = outDir.getName();
        if (title.equals(".") || title.equals("..")) try {
            title = outDir.getCanonicalFile().getName();
        } catch (IOException e) {
        }
    }

    boolean generate() {
        File[] children = dir.listFiles();
        Arrays.sort(children);
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                AlbumGen gen = new AlbumGen(child, new File(outDir, child.getName()), true);
                if (!gen.generate()) return false;
                childAlbums.add(gen);
            }
        }
        try {
            parse();
        } catch (Throwable e) {
            System.err.println("Unable to process " + dir);
            e.printStackTrace();
            return false;
        }
        try {
            output();
        } catch (Throwable e) {
            System.err.println("Unable to generate " + dir);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static boolean index(AlbumGen topAlbum, File dir, File outDir, int indexBy) {
        try {
            if ((indexBy & INDEX_BY_DATE) != 0) {
                HashMap yearHashMap = new HashMap();
                HashMap decadeHashMap = new HashMap();
                try {
                    addImagesToHashMapByDate(dir, dir.getCanonicalFile().getName(), decadeHashMap, yearHashMap);
                } catch (Throwable e) {
                    System.err.println("Unable to process " + dir);
                    e.printStackTrace();
                    return false;
                }
                System.out.println("Writing index-by-date files..." + dir);
                AlbumGen byYearAlbum = new AlbumGen(null, new File(outDir, "/Indexed By Year"), true);
                String[] decades = (String[]) decadeHashMap.keySet().toArray(new String[decadeHashMap.size()]);
                Arrays.sort(decades);
                for (int i = 0; i < decades.length; i++) {
                    String decade = decades[i];
                    AlbumGen decadeAlbum = new AlbumGen(null, new File(byYearAlbum.outDir, "/" + decade), true);
                    List l = (List) decadeHashMap.get(decade);
                    for (Iterator it = l.iterator(); it.hasNext(); ) {
                        ImageDefinition id = (ImageDefinition) it.next();
                        decadeAlbum.images.add(id);
                    }
                    String decadePrefix = decade.substring(0, 3);
                    int count = 0;
                    for (int y = 0; y < 10; y++) {
                        String year = decadePrefix + y;
                        l = (List) yearHashMap.get(year);
                        if (l != null) count++;
                    }
                    if (count > 1 || decades.length == 1) {
                        for (int y = 0; y < 10; y++) {
                            String year = decadePrefix + y;
                            l = (List) yearHashMap.get(year);
                            if (l == null) continue;
                            AlbumGen yearAlbum = new AlbumGen(null, new File(decadeAlbum.outDir, "/" + year), true);
                            for (Iterator it = l.iterator(); it.hasNext(); ) {
                                ImageDefinition id = (ImageDefinition) it.next();
                                yearAlbum.images.add(id);
                            }
                            yearAlbum.output("../../../");
                            decadeAlbum.childAlbums.add(yearAlbum);
                        }
                    }
                    decadeAlbum.output("../../");
                    if (decades.length == 1) {
                        byYearAlbum.images = decadeAlbum.images;
                        byYearAlbum.childAlbums = decadeAlbum.childAlbums;
                    } else {
                        byYearAlbum.childAlbums.add(decadeAlbum);
                    }
                }
                byYearAlbum.output("../");
                topAlbum.childAlbums.add(byYearAlbum);
            }
        } catch (Throwable e) {
            System.err.println("Unable to generate " + outDir);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static void addImagesToHashMapByDate(File dir, String path, HashMap decadeHashMap, HashMap yearHashMap) throws IOException {
        System.out.println("Indexing: " + dir);
        File[] children = dir.listFiles();
        Arrays.sort(children);
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                addImagesToHashMapByDate(child, path + "/" + child.getName(), decadeHashMap, yearHashMap);
            } else if (child.getName().equals("thumball.html")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(child)));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("<a href=\"view.html")) {
                            int imgIndex = line.indexOf("img=");
                            int endImgIndex = line.indexOf("&", imgIndex);
                            String image = URLDecoder.decode(line.substring(imgIndex + "img=".length(), endImgIndex), "UTF8");
                            int captionIndex = line.indexOf("&caption=");
                            int endCaptionIndex = line.indexOf("\"", captionIndex + 1);
                            String caption = URLDecoder.decode(line.substring(captionIndex + "&caption=".length(), endCaptionIndex), "UTF8");
                            int thumbIndex = line.indexOf("src=\"");
                            int endThumbIndex = line.indexOf("\"", thumbIndex + "src=\"".length() + 1);
                            String thumb = line.substring(thumbIndex + "src=\"".length(), endThumbIndex);
                            ImageDefinition id = new ImageDefinition();
                            id.caption = caption;
                            id.uri = path + "/" + image;
                            id.thumbUri = path + "/" + thumb;
                            Pattern p = Pattern.compile("[12]\\d\\d\\d");
                            Matcher m = p.matcher(caption);
                            while (m.find()) {
                                List l;
                                String year = m.group();
                                l = (List) yearHashMap.get(year);
                                if (l == null) yearHashMap.put(year, l = new ArrayList());
                                l.add(id);
                                String decade = year.substring(0, 3) + "0's";
                                l = (List) decadeHashMap.get(decade);
                                if (l == null) decadeHashMap.put(decade, l = new ArrayList());
                                l.add(id);
                            }
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }

    static void addImagesToAlbum(File dir, String path, AlbumGen albumGen) throws IOException {
        File[] children = dir.listFiles();
        Arrays.sort(children);
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                addImagesToAlbum(child, path + "/" + child.getName(), albumGen);
            } else if (child.getName().equals("thumball.html")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(child)));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("<a href=\"view.html")) {
                            int imgIndex = line.indexOf("img=");
                            int endImgIndex = line.indexOf("&", imgIndex);
                            String image = URLDecoder.decode(line.substring(imgIndex + "img=".length(), endImgIndex), "UTF8");
                            int captionIndex = line.indexOf("&caption=");
                            int endCaptionIndex = line.indexOf("\"", captionIndex + 1);
                            String caption = URLDecoder.decode(line.substring(captionIndex + "&caption=".length(), endCaptionIndex), "UTF8");
                            int thumbIndex = line.indexOf("src=\"");
                            int endThumbIndex = line.indexOf("\"", thumbIndex + "src=\"".length() + 1);
                            String thumb = line.substring(thumbIndex + "src=\"".length(), endThumbIndex);
                            ImageDefinition id = new ImageDefinition();
                            id.caption = caption;
                            id.uri = path + "/" + image;
                            id.thumbUri = path + "/" + thumb;
                            albumGen.images.add(id);
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }

    void parse() throws IOException {
        File[] imageFiles = dir.listFiles(new FilenameFilter() {

            public boolean accept(File f, String s) {
                s = s.toLowerCase();
                return s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png");
            }
        });
        Set allImages = new HashSet();
        Set excludedImages = new HashSet();
        Set outImages = new HashSet();
        for (int i = 0; i < imageFiles.length; i++) {
            File imageFile = imageFiles[i];
            Tools t = new Tools(imageFile);
            int type = t.getType();
            if (type == Tools.BASE) allImages.add(t); else if (type == Tools.OUT) {
                allImages.add(t);
                outImages.add(t);
            } else if (type == Tools.EXCLUDE) excludedImages.add(t);
        }
        allImages.removeAll(excludedImages);
        allImages.removeAll(outImages);
        allImages.addAll(outImages);
        for (Iterator i = allImages.iterator(); i.hasNext(); ) {
            Tools t = (Tools) i.next();
            ImageDefinition def = parseImageDefinition(t);
            if (t.getType() != Tools.OUT) {
                if (def.orientation == 6 || def.orientation == 8) {
                    System.out.println("Orienting " + t.getSourceFile());
                    t.load(t.getSourceFile());
                    if (def.orientation == 8) t.rotateLeft(); else t.rotateRight();
                    t.save(t.getOutFile(), true);
                }
            }
            images.add(def);
        }
        Collections.sort(images);
        imageCount += images.size();
    }

    private void applyInfo(ImageDefinition src, ImageDefinition dst) {
        dst.caption = src.caption;
        dst.date = src.date;
        dst.orientation = src.orientation;
    }

    ImageDefinition parseImageDefinition(Tools tools) {
        ImageDefinition def = new ImageDefinition();
        System.out.println("Parsing " + tools.getSourceFile());
        def.srcFile = tools.getSourceFile();
        if (tools.getType() == Tools.OUT) {
            try {
                applyInfo(tools.parseInfo(), def);
            } catch (IOException e) {
            }
        }
        if (def.caption == null) {
            try {
                applyInfo(tools.parseInfo(), def);
            } catch (IOException e) {
            }
        }
        if (def.caption == null) {
            if (untitled != null) def.caption = untitled; else def.caption = removeExtension(tools.getBaseFile().getName());
        }
        def.destFile = new File(outDir, def.srcFile.getName());
        def.uri = def.destFile.getName();
        def.thumbFile = tools.getThumbFile(outDir);
        def.thumbUri = def.thumbFile.getName();
        def.tools = tools;
        return def;
    }

    String removeExtension(String name) {
        return name.substring(0, name.lastIndexOf("."));
    }

    void output() throws IOException {
        output("");
    }

    void output(String uriPrefix) throws IOException {
        outDir.mkdirs();
        outputIndex("index.html", "view.html");
        copyResource("view.html");
        copyResource("slideshow.js");
        copyResource("utils.js");
        copyResource("folder.gif");
        copyResource("print.gif");
        if (hasParent) copyResource("parentfolder.gif");
        PrintWriter allWriter = new PrintWriter(new FileOutputStream(new File(outDir, "thumball.html")));
        PrintWriter pageWriter = null;
        startPage(allWriter, -1);
        int page = -1;
        for (int i = 0; i < images.size(); i++) {
            if (i % imagesPerPage == 0) {
                if (pageWriter != null) {
                    finishPage(pageWriter, page);
                    pageWriter.close();
                }
                page++;
                pageWriter = new PrintWriter(new FileOutputStream(new File(outDir, "thumb" + page + ".html")));
                startPage(pageWriter, page);
            }
            ImageDefinition def = (ImageDefinition) images.get(i);
            if (def.destFile != null) conditionalCopyFile(def.destFile, def.srcFile);
            if (def.thumbFile != null) conditionalMakeThumb(def.thumbFile, def.tools);
            if (def.tools != null) def.tools.dispose();
            addImage(pageWriter, def, uriPrefix);
            addImage(allWriter, def, uriPrefix);
        }
        if (pageWriter == null) {
            pageWriter = new PrintWriter(new FileOutputStream(new File(outDir, "thumb0.html")));
            startPage(pageWriter, page);
        }
        finishPage(pageWriter, page);
        finishPage(allWriter, -1);
        pageWriter.close();
        allWriter.close();
    }

    void startPage(PrintWriter os, int page) throws IOException {
        os.println("<HTML><head><script language=\"javascript\" src=\"utils.js\"></script><script language=\"javascript\" src=\"slideshow.js\"></script></head><BODY><font face=\"Arial\" size=-1>");
        os.println("<style type=\"text/css\">");
        os.println("<!--");
        os.println("A:hover { text-decoration:underline }");
        os.println("A { text-decoration: none; }");
        os.println("-->");
        os.println("</style>");
        printNavBar(os, page);
        if (images.size() > 0) os.println("<form><a href=\"Slide Show\" onClick=\"return slideShow()\" >Slide Show</a> <input type=button value=\" | | \" onClick=\"pause()\"> <input type=button value=\"+\" onClick=\"faster()\"> <input type=button value=\"-\" onClick=\"slower()\"></form>");
        os.println("<p>");
        if (hasParent) os.println("<a href=\"" + "../index.html\" target=\"_top\"><img border=0 src=\"parentfolder.gif\"><br>[Parent]</a><br>");
        for (int i = 0; i < childAlbums.size(); i++) {
            AlbumGen child = (AlbumGen) childAlbums.get(i);
            os.println("<a href=\"" + child.outDir.getName() + "/index.html\" target=\"_top\"><img border=0 src=\"folder.gif\"><br>" + child.title + "</a><br>");
        }
        os.println("<p>");
        os.flush();
        if (os.checkError()) throw new IOException("Error writing page");
    }

    void printNavBar(PrintWriter os, int page) {
        int pageCount = (images.size() + imagesPerPage - 1) / imagesPerPage;
        if (pageCount > 1) {
            os.print("Page: ");
            for (int i = 0; i < pageCount; i++) {
                if (i > 0) os.print(' ');
                if (i == page) os.print(i + 1); else os.print("<a href=\"thumb" + i + ".html\">" + (i + 1) + "</a>");
            }
            if (page >= 0) os.print(" <a href=\"thumball.html\">all</a>");
            os.println();
        }
    }

    String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding UTF8");
        }
    }

    void addImage(PrintWriter os, ImageDefinition def, String uriPrefix) throws IOException {
        String name = uriPrefix + def.thumbUri;
        os.println("<a href=\"view.html?img=" + urlEncode(uriPrefix + def.uri) + "&caption=" + urlEncode(def.caption) + "\" target=\"viewer\" onClick=\"instance++\" ><img border=0 src=\"" + name + "\" ><br>" + def.caption + "</a><br><BR>");
        os.flush();
        if (os.checkError()) throw new IOException("Error writing page");
    }

    void finishPage(PrintWriter os, int page) throws IOException {
        printNavBar(os, page);
        if (page == -1) {
            os.println("<script language=\"javascript\">");
            os.println("<!--");
            os.println("if (getUrlParam('slideshow')) slideShow()");
            os.println("// -->");
            os.println("</script>");
        }
        os.println("</font></BODY></HTML>");
        os.flush();
        if (os.checkError()) throw new IOException("Error writing page");
    }

    void outputIndex(String name, String viewName) throws IOException {
        PrintWriter out = new PrintWriter(new FileOutputStream(new File(outDir, name)));
        out.write("<html><head><title>" + title + "</title></head>\r\n" + "<frameset cols=\"200, *\">\r\n" + "<frame name=\"thumbs\" src=\"thumb0.html\" scrolling=\"auto\">\r\n" + "<frame name=\"viewer\" src=\"" + viewName + "\" scrolling=\"no\">\r\n" + "<noframes><body><p>This page uses frames, but your browser doesn't support them.</p></body></noframes>\r\n" + "</frameset></html>");
        out.flush();
        out.close();
    }

    void copyResource(String name) throws IOException {
        URL url = getClass().getResource(name);
        InputStream is = url.openStream();
        OutputStream os = new FileOutputStream(new File(outDir, name));
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
        is.close();
        os.close();
    }

    boolean done = false;

    public int compareTo(Object o) {
        return title.compareTo(((AlbumGen) o).title);
    }

    public boolean equals(Object o) {
        return compareTo(o) == 0;
    }

    static void conditionalCopyFile(File dst, File src) throws IOException {
        if (dst.equals(src)) return;
        if (!dst.isFile() || dst.lastModified() < src.lastModified()) {
            System.out.println("Copying " + src);
            InputStream is = new FileInputStream(src);
            OutputStream os = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.close();
            is.close();
        }
    }

    static void conditionalMakeThumb(File thumb, Tools tools) throws IOException {
        if (!thumb.isFile() || thumb.lastModified() < tools.getSourceFile().lastModified()) {
            System.out.println("Making thumb " + thumb);
            tools.load(tools.getSourceFile());
            tools.makeThumb(thumbArea);
            tools.setQuality(thumbQuality);
            tools.save(thumb);
        }
    }

    static class ImageDefinition implements Comparable {

        String caption;

        Date date;

        int orientation;

        File srcFile;

        File destFile;

        String uri;

        File thumbFile;

        String thumbUri;

        Tools tools;

        public int compareTo(Object o) {
            return tools.compareTo(((ImageDefinition) o).tools);
        }
    }
}
