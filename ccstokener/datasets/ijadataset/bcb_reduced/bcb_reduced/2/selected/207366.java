package net.sf.doolin.app.sc.engine.support;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import net.sf.doolin.util.CodeException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

public class LocalNameGenerator extends StubNameGenerator {

    private List<String> lines = null;

    public LocalNameGenerator(String stub) {
        super(stub);
    }

    public LocalNameGenerator(String stub, String separator) {
        super(stub, separator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String generateName() {
        if (this.lines == null) {
            try {
                File file = getFile();
                this.lines = FileUtils.readLines(file, "UTF-8");
            } catch (IOException ex) {
                throw new CodeException("NameGenerator.IO", ex, ex);
            }
        }
        if (this.lines.isEmpty()) {
            return super.generateName();
        } else {
            Collections.shuffle(this.lines);
            String line = this.lines.get(0);
            this.lines.remove(0);
            line = StringUtils.trim(line);
            line = StringEscapeUtils.unescapeJava(line);
            return line;
        }
    }

    protected File getFile() throws IOException {
        File home = new File(System.getProperty("user.dir"));
        String fileName = String.format("%s.txt", getFilePrefix());
        File file = new File(home, fileName);
        if (file.exists()) {
            return file;
        } else {
            URL url = LocalNameGenerator.class.getResource("/" + fileName);
            if (url == null) {
                throw new IllegalStateException(String.format("Cannot find resource at %s", fileName));
            } else {
                InputStream in = url.openStream();
                try {
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    try {
                        IOUtils.copy(in, out);
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
                return file;
            }
        }
    }

    protected String getFilePrefix() {
        return StringUtils.upperCase(getStub());
    }
}
