package com.objectwave.sourceGenerator;

import com.objectwave.sourceModel.ClassElement;
import com.objectwave.templateMerge.KnownTemplates;
import com.objectwave.templateMerge.MergeTemplate;
import com.objectwave.templateMerge.XMLTemplateRead;
import com.objectwave.utility.FileFinder;
import java.io.*;
import java.util.Vector;

/**
 *  This class does very little but house some common source generation
 *  routines. These routines are shared by the various other generating
 *  utilities.
 *
 * @author  dhoag
 * @version  $Id: SourceCodeGenerator.java,v 2.0 2001/06/11 15:54:25 dave_hoag Exp $
 */
public class SourceCodeGenerator implements java.io.Serializable {

    /**
	 */
    public static MergeTemplate useThisTemplate = null;

    /**
	 */
    public SourceCodeGenerator() {
    }

    /**
	 *  Template used for generating a new class. This should probably be remove
	 *  from this class.
	 *
	 * @param  aTemplate The new GenerationTemplate value
	 */
    public static void setGenerationTemplate(final MergeTemplate aTemplate) {
        useThisTemplate = aTemplate;
    }

    /**
	 *  Generic source generation routine used by various SourceCodeGenerators. Add
	 *  the ClassElement meth into the Vector v. This may replace any existing
	 *  element (depending on the value of the boolean parameter) or just add to
	 *  end if an equal ClassElement is not already found.
	 *
	 * @param  meth The feature to be added to the ClassElement attribute
	 * @param  v The feature to be added to the ClassElement attribute
	 * @param  overwrite The feature to be added to the ClassElement attribute
	 * @see  #insertClassElement
	 */
    public static void addClassElement(final ClassElement meth, final Vector v, final boolean overwrite) {
        int idx = contains(v, meth);
        if (idx > -1) {
            if (overwrite) {
                v.setElementAt(meth, idx);
            }
        } else {
            v.addElement(meth);
        }
    }

    /**
	 *  Generic source generation routine used by various SourceCodeGenerators.
	 *  Insert the ClassElement meth into the Vector v. This may replace any
	 *  existing element (depending on the value of the boolean parameter) or just
	 *  insert to front if an equal ClassElement is not already found.
	 *
	 * @param  meth
	 * @param  v
	 * @param  overwrite
	 * @see  #addClassElement
	 */
    public static void insertClassElement(final ClassElement meth, final Vector v, final boolean overwrite) {
        int idx = contains(v, meth);
        if (idx > -1) {
            if (overwrite) {
                v.setElementAt(meth, idx);
            }
        } else {
            if (v.size() > 0) {
                v.insertElementAt(meth, 0);
            } else {
                v.addElement(meth);
            }
        }
    }

    /**
	 *  Remove the element in Vector v that is equal() to the param meth.
	 *
	 * @param  meth
	 * @param  v
	 */
    public static void removeClassElement(final ClassElement meth, final Vector v) {
        int idx = contains(v, meth);
        if (idx > -1) {
            v.removeElementAt(idx);
        }
    }

    /**
	 *  Write the class elements to a file.
	 *
	 * @param  elements
	 * @param  fName
	 * @exception  IOException
	 */
    public static void writeClassElements(final ClassElement[] elements, final String fName) throws IOException {
        FileWriter fs = new FileWriter(fName);
        BufferedWriter buf = new BufferedWriter(fs);
        for (int i = 0; i < elements.length; i++) {
            buf.write(elements[i].getFullText());
        }
        buf.flush();
        buf.close();
    }

    /**
	 *  args[0] TemplateName args[1] TemplateFile args[2] SourceFile
	 *
	 * @param  args The command line arguments
	 */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: SourceCodeGenerator <templateName> <templateFile> <sourceFile>");
            return;
        }
        String templateName = args[0];
        try {
            java.net.URL url = new FileFinder().getUrl(com.objectwave.tools.GenerateTemplateInto.class, args[1]);
            KnownTemplates temp = KnownTemplates.readStream(url.openStream());
            if (temp == null) {
                throw new RuntimeException("Failed create Knowntemplates from XML file");
            }
            KnownTemplates.setDefaultInstance(temp);
            FileReader fileRdr = new FileReader(args[2]);
            com.objectwave.sourceParser.SourceCodeReader src = new com.objectwave.sourceParser.SourceCodeReader(fileRdr);
            com.objectwave.sourceModel.JavaClassDef def = new com.objectwave.sourceModel.JavaClassDef(src.parseSource());
            ClassInformation ci = new ClassInformation();
            ci.setClassElement(def);
            MergeTemplate template = KnownTemplates.getDefaultInstance().getTemplate(templateName);
            template.generateForOn(ci, System.out);
            System.out.println("");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
	 *  Does the Vector parameter contain the ClassElement e?
	 *
	 * @param  v
	 * @param  e
	 * @return  index of element e, or -1 if not at all.
	 */
    static int contains(final Vector v, final ClassElement e) {
        int vSize = v.size();
        for (int i = 0; i < vSize; i++) {
            if (e.equals((ClassElement) v.elementAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
	 *  Generic method for getting the source code template to Generate an entire
	 *  class. This should probably be removed from this class.
	 *
	 * @return  The Template value
	 * @exception  IOException
	 */
    public MergeTemplate getTemplate() throws IOException {
        if (useThisTemplate != null) {
            return useThisTemplate;
        }
        MergeTemplate temp = com.objectwave.templateMerge.KnownTemplates.getDefaultInstance().getTemplate("JavaClassDefinition");
        return temp;
    }

    /**
	 *  This should probably be removed from this class.
	 *
	 * @param  ci
	 * @param  path
	 * @return
	 * @exception  IOException
	 */
    public String createClass(final ClassInformation ci, final String path) throws IOException {
        if (ci == null) {
            return null;
        }
        if (ci.getClassElement() == null) {
            return null;
        }
        MergeTemplate temp = getTemplate();
        if (temp == null) {
            throw new IOException("Source Code Generation Template not found!");
        }
        String fName = ci.getClassElement().getMainClass().getClassName() + ".java";
        if (path != null) {
            fName = path + File.separatorChar + fName;
        }
        com.objectwave.event.StatusManager.getDefaultManager().fireStatusEvent("Creating " + fName);
        FileOutputStream fs = new FileOutputStream(fName);
        BufferedOutputStream buf = new BufferedOutputStream(fs);
        temp.generateForOn(ci, buf);
        buf.flush();
        buf.close();
        return fName;
    }
}
