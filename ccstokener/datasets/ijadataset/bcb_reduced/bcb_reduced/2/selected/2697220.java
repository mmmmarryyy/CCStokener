package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.HashMap;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Encapsulates the state of a single active compilation unit in a particular
 * module. State is accumulated throughout the life cycle of the containing
 * module and may be invalidated at certain times and recomputed.
 */
public abstract class CompilationUnit {

    /**
   * Encapsulates the functionality to find all nested classes of this class
   * that have compiler-generated names. All class bytes are loaded from the
   * disk and then analyzed using ASM.
   */
    static class GeneratedClassnameFinder {

        private static class AnonymousClassVisitor extends EmptyVisitor {

            List<String> classNames = new ArrayList<String>();

            public List<String> getInnerClassNames() {
                return classNames;
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                if ((access & Opcodes.ACC_SYNTHETIC) == 0) {
                    classNames.add(name);
                }
            }
        }

        private final List<String> classesToScan;

        private final TreeLogger logger;

        private final String mainClass;

        private String mainUrlBase = null;

        GeneratedClassnameFinder(TreeLogger logger, String mainClass) {
            assert mainClass != null;
            this.mainClass = mainClass;
            classesToScan = new ArrayList<String>();
            classesToScan.add(mainClass);
            this.logger = logger;
        }

        List<String> getClassNames() {
            List<String> allGeneratedClasses = new ArrayList<String>();
            for (int i = 0; i < classesToScan.size(); i++) {
                String lookupName = classesToScan.get(i);
                byte classBytes[] = getClassBytes(lookupName);
                if (classBytes == null) {
                    continue;
                }
                if (isClassnameGenerated(lookupName) && !allGeneratedClasses.contains(lookupName)) {
                    allGeneratedClasses.add(lookupName);
                }
                AnonymousClassVisitor cv = new AnonymousClassVisitor();
                new ClassReader(classBytes).accept(cv, 0);
                List<String> innerClasses = cv.getInnerClassNames();
                for (String innerClass : innerClasses) {
                    if (!innerClass.startsWith(mainClass + "$")) {
                        continue;
                    }
                    if (!classesToScan.contains(innerClass)) {
                        classesToScan.add(innerClass);
                    }
                }
            }
            Collections.sort(allGeneratedClasses, new GeneratedClassnameComparator());
            return allGeneratedClasses;
        }

        private byte[] getClassBytes(String slashedName) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(slashedName + ".class");
            if (url == null) {
                logger.log(TreeLogger.DEBUG, "Unable to find " + slashedName + " on the classPath");
                return null;
            }
            String urlStr = url.toExternalForm();
            if (slashedName.equals(mainClass)) {
                mainUrlBase = urlStr.substring(0, urlStr.lastIndexOf('/'));
            } else {
                assert mainUrlBase != null;
                if (!mainUrlBase.equals(urlStr.substring(0, urlStr.lastIndexOf('/')))) {
                    logger.log(TreeLogger.DEBUG, "Found " + slashedName + " at " + urlStr + " The base location is different from  that of " + mainUrlBase + " Not loading");
                    return null;
                }
            }
            try {
                URLConnection conn = url.openConnection();
                return Util.readURLConnectionAsBytes(conn);
            } catch (IOException ignored) {
                logger.log(TreeLogger.DEBUG, "Unable to load " + urlStr + ", in trying to load " + slashedName);
            }
            return null;
        }
    }

    protected static final DiskCache diskCache = new DiskCache();

    private static final Pattern GENERATED_CLASSNAME_PATTERN = Pattern.compile(".+\\$\\d.*");

    /**
   * Checks if the class names is generated. Accepts any classes whose names
   * match .+$\d.* (handling named classes within anonymous classes and multiple
   * named classes of the same name in a class, but in different methods).
   * Checks if the class or any of its enclosing classes are anonymous or
   * synthetic.
   * <p>
   * If new compilers have different conventions for anonymous and synthetic
   * classes, this code needs to be updated.
   * </p>
   * 
   * @param className name of the class to be checked.
   * @return true iff class or any of its enclosing classes are anonymous or
   *         synthetic.
   */
    @Deprecated
    public static boolean isClassnameGenerated(String className) {
        return GENERATED_CLASSNAME_PATTERN.matcher(className).matches();
    }

    /**
   * Map from the className in javac to the className in jdt. String represents
   * the part of className after the compilation unit name. Emma-specific.
   */
    private Map<String, String> anonymousClassMap = null;

    @Deprecated
    public final boolean constructAnonymousClassMappings(TreeLogger logger) {
        anonymousClassMap = new HashMap<String, String>();
        for (String topLevelClass : getTopLevelClasses()) {
            List<String> javacClasses = new GeneratedClassnameFinder(logger, topLevelClass).getClassNames();
            List<String> jdtClasses = getJdtClassNames(topLevelClass);
            if (javacClasses.size() != jdtClasses.size()) {
                anonymousClassMap = Collections.emptyMap();
                return false;
            }
            int size = javacClasses.size();
            for (int i = 0; i < size; i++) {
                if (!javacClasses.get(i).equals(jdtClasses.get(i))) {
                    anonymousClassMap.put(javacClasses.get(i), jdtClasses.get(i));
                }
            }
        }
        return true;
    }

    @Deprecated
    public final boolean createdClassMapping() {
        return anonymousClassMap != null;
    }

    /**
   * Overridden to finalize; always returns object identity.
   */
    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Deprecated
    public final Map<String, String> getAnonymousClassMap() {
        if (anonymousClassMap == null) {
            return Collections.emptyMap();
        }
        return anonymousClassMap;
    }

    /**
   * Returns the user-relevant location of the source file. No programmatic
   * assumptions should be made about the return value.
   */
    public abstract String getDisplayLocation();

    public abstract List<JsniMethod> getJsniMethods();

    /**
   * Returns the last modified time of the compilation unit.
   */
    public abstract long getLastModified();

    /**
   * @return a way to lookup method argument names for this compilation unit.
   */
    public abstract MethodArgNamesLookup getMethodArgs();

    /**
   * Returns the source code for this unit.
   */
    @Deprecated
    public abstract String getSource();

    /**
   * Returns the fully-qualified name of the top level public type.
   */
    public abstract String getTypeName();

    @Deprecated
    public final boolean hasAnonymousClasses() {
        for (CompiledClass cc : getCompiledClasses()) {
            if (isAnonymousClass(cc)) {
                return true;
            }
        }
        return false;
    }

    /**
   * Overridden to finalize; always returns identity hash code.
   */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
   * Returns <code>true</code> if this unit is compiled and valid.
   */
    public boolean isCompiled() {
        return true;
    }

    /**
   * Returns <code>true</code> if this unit had errors.
   */
    public boolean isError() {
        return false;
    }

    /**
   * Returns <code>true</code> if this unit was generated by a
   * {@link com.google.gwt.core.ext.Generator}.
   */
    @Deprecated
    public abstract boolean isGenerated();

    /**
   * 
   * @return true if the Compilation Unit is from a super-source.
   */
    @Deprecated
    public abstract boolean isSuperSource();

    /**
   * Overridden to finalize; always returns {@link #getDisplayLocation()}.
   */
    @Override
    public final String toString() {
        return getDisplayLocation();
    }

    /**
   * Returns all contained classes.
   */
    abstract Collection<CompiledClass> getCompiledClasses();

    /**
   * Returns the content ID for the source with which this unit was compiled.
   */
    abstract ContentId getContentId();

    abstract Set<ContentId> getDependencies();

    abstract CategorizedProblem[] getProblems();

    private List<String> getJdtClassNames(String topLevelClass) {
        List<String> classNames = new ArrayList<String>();
        for (CompiledClass cc : getCompiledClasses()) {
            if (isAnonymousClass(cc) && cc.getInternalName().startsWith(topLevelClass + "$")) {
                classNames.add(cc.getInternalName());
            }
        }
        Collections.sort(classNames, new GeneratedClassnameComparator());
        return classNames;
    }

    private List<String> getTopLevelClasses() {
        List<String> topLevelClasses = new ArrayList<String>();
        for (CompiledClass cc : getCompiledClasses()) {
            if (cc.getEnclosingClass() == null) {
                topLevelClasses.add(cc.getInternalName());
            }
        }
        return topLevelClasses;
    }

    /**
   * TODO(amitmanjhi): what is the difference between an anonymous and local
   * class for our purposes? All our unit tests pass whether or not we do the
   * additional {@link #isClassnameGenerated} check. We either need to find the
   * real difference and add a unit test, or else simply this.
   */
    private boolean isAnonymousClass(CompiledClass cc) {
        return cc.isLocal() && isClassnameGenerated(cc.getInternalName());
    }
}
