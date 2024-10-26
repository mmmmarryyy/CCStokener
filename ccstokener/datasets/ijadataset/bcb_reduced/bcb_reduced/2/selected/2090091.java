package org.ponder.groovy.builders.pdf.itext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import net.sourceforge.javautil.common.io.IVirtualFile;
import net.sourceforge.javautil.groovy.builder.interceptor.annotation.Node;
import net.sourceforge.javautil.groovy.builder.interceptor.annotation.Node.Type;
import net.sourceforge.javautil.groovy.builder.stack.ITreeRootNodeHandler;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfEncryptor;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

/**
 * This is the root node of a PDF builder object tree.
 * 
 * @author elponderador
 */
@Node(Type.Root)
public class Document extends ItextParentObject implements ITreeRootNodeHandler<OutputStream, ItextContext, ItextChildElement> {

    public enum Orientation {

        PORTRAIT, LANDSCAPE
    }

    protected String fontFamily = BaseFont.HELVETICA;

    protected float fontSize = 10;

    protected boolean closeAutomatically = true;

    protected float leading = 0;

    protected boolean encrypt = false;

    protected byte[] userPassword;

    protected byte[] ownerPassword;

    protected boolean allowPrint = true;

    protected boolean allowCopy = false;

    protected Orientation orientation = Orientation.PORTRAIT;

    protected Rectangle pageSize = PageSize.LETTER;

    protected ItextContext context;

    /**
	 * This will call {@link #apply(OutputStream)} providing a ByteArrayOutputStream()
	 */
    public OutputStream apply() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.apply(new ByteArrayOutputStream());
        return baos;
    }

    /**
	 * This will create a new iText Document, a PdfWriter, open the document, setup a proper {@link ItextContext} instance 
	 * and begin applying its children, then finally it will close the iText Document (and the outputStream if {@link #isCloseAutomatically()} returns
	 * true).
	 * 
	 * @param out The output stream the PDF data should be written to.
	 */
    public OutputStream apply(OutputStream out) {
        com.lowagie.text.Document document;
        PdfWriter writer;
        try {
            document = new com.lowagie.text.Document(orientation == Orientation.LANDSCAPE ? this.pageSize.rotate() : this.pageSize);
            writer = PdfWriter.getInstance(document, out);
            if (this.encrypt) {
                int options = 0;
                if (this.allowCopy) options |= PdfWriter.ALLOW_COPY;
                if (this.allowPrint) options |= PdfWriter.ALLOW_PRINTING;
                writer.setEncryption(userPassword, ownerPassword, options, PdfWriter.ENCRYPTION_AES_128);
            }
            document.open();
            this.context = new ItextContext(this, document, writer, true);
            this.context.setFont(this.fontFamily, this.fontSize);
            this.context.startContext();
            this.applyChildren(this.context);
            this.context.stopContext();
            document.close();
            if (this.closeAutomatically) out.close();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    /**
	 * This can be called to get a template from a URL.
	 * It will call the {@link #load(InputStream)} with the {@link URL#openStream()} as the
	 * parameter.
	 * 
	 * @param url The url whose stream will be opened.
	 * @return A {@link PdfReader} instance for the url
	 */
    public PdfReader load(URL url) {
        try {
            return this.load(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * This can be called to get a template from a File.
	 * It will call the {@link #load(InputStream)} with a new instance of {@link FileInputStream}.
	 * 
	 * @param file The file to create the template from.
	 * @return A {@link PdfReader} instance for the file
	 */
    public PdfReader load(File file) {
        try {
            return this.load(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * This can be called to get a template from a {@link VirtualFile}.
	 * 
	 * @param file The file to create the template from.
	 * @return A {@link PdfReader} instance for the file
	 */
    public PdfReader load(IVirtualFile file) {
        try {
            return this.load(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * This can be called to get a template from an arbitrary input stream.
	 * It simply creates a new instance of {@link PdfReader} and passes it the stream.
	 * 
	 * @param is The stream to read PDF data from.
	 * @return An instance of {@link PdfReader} for this stream.
	 */
    public PdfReader load(InputStream is) {
        try {
            return new PdfReader(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * @return The default font for the main context. 
	 */
    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    /**
	 * @return The default font size for the main context.
	 */
    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    /**
	 * Whether or not to close the output stream automatically once the document has been completely processed.
	 * 
	 * @return Returns true if during the {@link #apply(OutputStream)} method the output stream should be closed automatically, otherwise false.
	 */
    public boolean isCloseAutomatically() {
        return closeAutomatically;
    }

    public void setCloseAutomatically(boolean closeAutomatically) {
        this.closeAutomatically = closeAutomatically;
    }

    /**
	 * This allows the builder code to setup the page size for the document.
	 */
    public void setPageSize(Rectangle pageSize) {
        this.pageSize = pageSize;
    }

    public Rectangle getPageSize() {
        return pageSize;
    }

    public float getLeading() {
        return leading;
    }

    public void setLeading(float leading) {
        this.leading = leading;
    }
}
