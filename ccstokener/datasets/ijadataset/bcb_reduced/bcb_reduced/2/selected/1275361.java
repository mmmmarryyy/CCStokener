package net.sf.doolin.app.sc.client.local;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.doolin.app.sc.common.service.NameCategory;
import net.sf.doolin.app.sc.common.service.impl.StubNameGenerator;
import net.sf.doolin.util.CodeException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class LocalNameGenerator extends StubNameGenerator {

    private Map<NameCategory, List<String>> linesPerCategory = new HashMap<NameCategory, List<String>>();

    @Override
    @SuppressWarnings("unchecked")
    public String generate(NameCategory category) {
        List<String> lines = this.linesPerCategory.get(category);
        if (lines == null) {
            try {
                File file = getFile(category);
                lines = FileUtils.readLines(file);
            } catch (IOException ex) {
                throw new CodeException("NameGenerator.IO", ex, category, ex);
            }
            this.linesPerCategory.put(category, lines);
        }
        if (lines.isEmpty()) {
            return super.generate(category);
        } else {
            Collections.shuffle(lines);
            String line = lines.get(0);
            lines.remove(0);
            line = StringUtils.trim(line);
            return line;
        }
    }

    protected File getFile(NameCategory category) throws IOException {
        File home = new File(System.getProperty("user.dir"));
        String fileName = String.format("%s.txt", category);
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
}
