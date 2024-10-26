package com.mycila.guice.spi;

import com.mycila.guice.Loader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class ASMClassFinder {

    static {
        try {
            ASMClassFinder.class.getClassLoader().loadClass("org.objectweb.asm.Type");
        } catch (ClassNotFoundException ignored) {
            throw new AssertionError("ASM is missing in your classpath");
        }
    }

    private final String annotationClassDesc;

    private final Loader loader;

    public ASMClassFinder(Class<? extends Annotation> annotationClass, Loader loader) {
        this.annotationClassDesc = org.objectweb.asm.Type.getDescriptor(annotationClass);
        this.loader = loader;
    }

    public Class<?> resolve(URL url) throws IOException {
        InputStream is = null;
        try {
            is = url.openStream();
            org.objectweb.asm.ClassReader classReader = new org.objectweb.asm.ClassReader(is);
            ClassAnnotationVisitor visitor = new ClassAnnotationVisitor();
            classReader.accept(visitor, org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
            if ((visitor.access & org.objectweb.asm.Opcodes.ACC_PUBLIC) != 0 && visitor.annotations.contains(annotationClassDesc)) return loader.loadClass(visitor.name.replace('/', '.'));
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static final class ClassAnnotationVisitor implements org.objectweb.asm.ClassVisitor {

        String name;

        int access;

        List<String> annotations = new ArrayList<String>(2);

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.name = name;
            this.access = access;
        }

        @Override
        public void visitSource(String source, String debug) {
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc) {
        }

        @Override
        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            annotations.add(desc);
            return null;
        }

        @Override
        public void visitAttribute(org.objectweb.asm.Attribute attr) {
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            return null;
        }

        @Override
        public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return null;
        }

        @Override
        public void visitEnd() {
        }
    }
}
