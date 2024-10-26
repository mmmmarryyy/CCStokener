package net.sf.joafip.store.service.bytecode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.asm.AnnotationVisitor;
import net.sf.joafip.asm.Attribute;
import net.sf.joafip.asm.ClassReader;
import net.sf.joafip.asm.ClassVisitor;
import net.sf.joafip.asm.ClassWriter;
import net.sf.joafip.asm.FieldVisitor;
import net.sf.joafip.asm.Label;
import net.sf.joafip.asm.MethodVisitor;
import net.sf.joafip.asm.Opcodes;
import net.sf.joafip.asm.Type;
import net.sf.joafip.store.entity.bytecode.PutFieldMap;
import net.sf.joafip.store.entity.bytecode.PutFieldMapEntry;
import net.sf.joafip.store.service.proxy.ProxyManager2;
import net.sf.joafip.util.ResourceFinder;

/**
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
public final class PersistableCodeGenerator implements ClassVisitor, Opcodes {

    private static final String LJAVA_2_LANG_OBJECT_V = "(Ljava/lang/Object;Ljava/lang/Object;)V";

    private static final String $FORCE_LOAD$ = "$forceLoad$";

    private static PersistableCodeGenerator Instance;

    private final Set<String> doNotTransformSet = new TreeSet<String>();

    private final Set<String> doNotTransformSet2 = new TreeSet<String>();

    private final String notStorableClassDesc;

    private transient ClassWriter classWriter;

    private transient String classInternalName;

    private transient boolean interfaceType;

    private transient boolean notStorableType;

    private transient PutFieldMap putFieldMap;

    public static PersistableCodeGenerator getInstance() throws IOException {
        synchronized (PersistableCodeGenerator.class) {
            if (Instance == null) {
                Instance = new PersistableCodeGenerator();
            }
        }
        return Instance;
    }

    private PersistableCodeGenerator() throws IOException {
        super();
        final Type type = Type.getType(NotStorableClass.class);
        notStorableClassDesc = type.getDescriptor();
        doNotTransformSet.add(Type.getInternalName(ProxyManager2.class));
        doNotTransformSet2.add("java/lang");
        final URL url = ResourceFinder.getResource("instrumentation.properties");
        InputStream inputStream;
        try {
            inputStream = url.openStream();
        } catch (IOException exception) {
            inputStream = null;
        }
        if (inputStream != null) {
            load(inputStream);
        }
    }

    private void load(final InputStream inputStream) throws IOException {
        final Properties properties = new Properties();
        properties.load(inputStream);
        final Set<Entry<Object, Object>> entrySet = properties.entrySet();
        for (Entry<Object, Object> entry : entrySet) {
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();
            if ("off".equals(value)) {
                doNotTransformSet2.add(key.replace('.', '/'));
            }
        }
    }

    /**
	 * 
	 * @param originalCode
	 *            the bytecode of the class to be transform.
	 * @param off
	 *            the start offset of the class data.
	 * @param len
	 *            the length of the class data.
	 * @return transformed code
	 */
    public byte[] generate(final byte[] originalCode, final int off, final int len) {
        final ClassReader classReader = new ClassReader(originalCode, off, len);
        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        notStorableType = false;
        putFieldMap = new PutFieldMap();
        classReader.accept(this, 0);
        return classWriter.toByteArray();
    }

    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        interfaceType = ((access & ACC_INTERFACE) == ACC_INTERFACE);
        classInternalName = name;
        final int newAccess;
        if (notStorableType) {
            newAccess = access;
        } else {
            newAccess = access & (~ACC_FINAL);
        }
        classWriter.visit(version, newAccess, name, signature, superName, interfaces);
    }

    public void visitSource(final String file, final String debug) {
        classWriter.visitSource(file, debug);
    }

    public void visitOuterClass(final String owner, final String name, final String desc) {
        classWriter.visitOuterClass(owner, name, desc);
    }

    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        classWriter.visitInnerClass(name, outerName, innerName, access);
    }

    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        final FieldVisitor fieldVisitor = classWriter.visitField(access, name, desc, signature, value);
        return new PersistableCodeFieldGenerator(fieldVisitor);
    }

    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        final MethodVisitor methodVisitor = classWriter.visitMethod(access, name, desc, signature, exceptions);
        final boolean staticAccess = (access & ACC_STATIC) == ACC_STATIC;
        final boolean finalAccess = (access & ACC_FINAL) == ACC_FINAL;
        final boolean privateAccess = (access & ACC_PRIVATE) == ACC_PRIVATE;
        return new PersistableCodeMethodGenerator(methodVisitor, (finalAccess || privateAccess), !interfaceType && !notStorableType && !staticAccess && !"<init>".equals(name), classInternalName, putFieldMap);
    }

    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        if (notStorableClassDesc.equals(desc)) {
            notStorableType = true;
        }
        final AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(desc, visible);
        return new PersistableCodeAnnotationGenerator(annotationVisitor);
    }

    @Override
    public void visitAttribute(final Attribute attr) {
        classWriter.visitAttribute(attr);
    }

    public void visitEnd() {
        if (!interfaceType && !notStorableType) {
            {
                final MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PROTECTED, "$intercept$", "()V", null, null);
                methodVisitor.visitCode();
                methodVisitor.visitInsn(RETURN);
                methodVisitor.visitMaxs(0, 1);
                methodVisitor.visitEnd();
            }
            {
                final MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PRIVATE + ACC_STATIC, "$getClass$", "(Ljava/lang/Object;)Ljava/lang/Class;", "(Ljava/lang/Object;)Ljava/lang/Class<*>;", null);
                methodVisitor.visitCode();
                final Label label0 = new Label();
                final Label label1 = new Label();
                final Label label2 = new Label();
                methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
                methodVisitor.visitLabel(label0);
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "net/sf/joafip/store/service/proxy/ProxyManager2", "classOfObject", "(Ljava/lang/Object;)Ljava/lang/Class;");
                methodVisitor.visitVarInsn(ASTORE, 1);
                methodVisitor.visitLabel(label1);
                final Label label3 = new Label();
                methodVisitor.visitJumpInsn(GOTO, label3);
                methodVisitor.visitLabel(label2);
                methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/Exception" });
                methodVisitor.visitVarInsn(ASTORE, 2);
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                methodVisitor.visitVarInsn(ASTORE, 1);
                methodVisitor.visitLabel(label3);
                methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "java/lang/Class" }, 0, null);
                methodVisitor.visitVarInsn(ALOAD, 1);
                methodVisitor.visitInsn(ARETURN);
                methodVisitor.visitMaxs(1, 3);
                methodVisitor.visitEnd();
            }
            {
                final MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PRIVATE + ACC_STATIC, $FORCE_LOAD$, LJAVA_2_LANG_OBJECT_V, null, null);
                methodVisitor.visitCode();
                final Label label0 = new Label();
                final Label label1 = new Label();
                final Label label2 = new Label();
                methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
                methodVisitor.visitVarInsn(ALOAD, 1);
                methodVisitor.visitVarInsn(ALOAD, 0);
                final Label label3 = new Label();
                methodVisitor.visitJumpInsn(IF_ACMPEQ, label3);
                methodVisitor.visitLabel(label0);
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "net/sf/joafip/store/service/proxy/ProxyManager2", "forceLoad", "(Ljava/lang/Object;)V");
                methodVisitor.visitLabel(label1);
                methodVisitor.visitJumpInsn(GOTO, label3);
                methodVisitor.visitLabel(label2);
                methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/Throwable" });
                methodVisitor.visitVarInsn(ASTORE, 2);
                methodVisitor.visitLabel(label3);
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                methodVisitor.visitInsn(RETURN);
                methodVisitor.visitMaxs(2, 3);
                methodVisitor.visitEnd();
            }
            {
                for (PutFieldMapEntry entry : putFieldMap) {
                    final int index = entry.getIndex();
                    final String owner = entry.getOwner();
                    final String name = entry.getName();
                    final Type type = entry.getType();
                    generatePutField(index, owner, name, type);
                }
            }
        }
        classWriter.visitEnd();
    }

    private void generatePutField(final int index, final String owner, final String name, final Type type) {
        final String desc = type.getDescriptor();
        final MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PRIVATE + ACC_STATIC, "$putField" + index + "$", "(L" + owner + ";" + desc + "Ljava/lang/Object;)V", null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1 + type.getSize());
        methodVisitor.visitMethodInsn(INVOKESTATIC, classInternalName, $FORCE_LOAD$, LJAVA_2_LANG_OBJECT_V);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(type.getOpcode(ILOAD), 1);
        methodVisitor.visitFieldInsn(PUTFIELD, owner, name, desc);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
    }

    public void addToNotTransform(final String internalClassName) {
        doNotTransformSet.add(internalClassName);
    }

    public boolean canTransform(final String internalClassName) {
        boolean canTransform = !doNotTransformSet.contains(internalClassName);
        if (canTransform) {
            final Iterator<String> iterator = doNotTransformSet2.iterator();
            while (canTransform && iterator.hasNext()) {
                final String entry = iterator.next();
                canTransform = true ^ internalClassName.startsWith(entry);
            }
        }
        return canTransform;
    }
}
