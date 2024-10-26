package jaxlib.lang.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.jar.JarEntry;
import jaxlib.lang.AccessLevel;
import jaxlib.lang.model.ClassKind;

public final class ClassInfo extends ClasspathElementInfo {

    private static ClassInfoReader read(final InputStream in) throws IOException, IllegalClassFormatException {
        if (in == null) throw new NullPointerException("in");
        final ClassInfoReader reader = new ClassInfoReader(1024);
        reader.runPreCreate(new JarEntry("<no source>"), in);
        return reader;
    }

    public static ClassInfo read(final URL url) throws IOException, IllegalClassFormatException {
        final InputStream in = url.openStream();
        try {
            return new ClassInfo(in);
        } finally {
            in.close();
        }
    }

    private final String[] interfaceNames;

    /**
   * @see Class#getModifiers()
   *
   * @since JaXLib 1.0
   */
    public final int modifiers;

    /**
   * @see Class#getSuperclass()
   *
   * @since JaXLib 1.0
   */
    public final String superclassName;

    ClassInfo(final ClassInfoReader src) {
        super(src);
        this.interfaceNames = ClasspathElementInfo.classnames(src.interfaceNames);
        this.modifiers = src.modifiers;
        this.superclassName = ClasspathElementInfo.internClassName(src.superclassName);
    }

    public ClassInfo(final InputStream in) throws IOException, IllegalClassFormatException {
        this(read(in));
    }

    /**
   * Get the access level of the class this {@code ClassInfo} represents.
   *
   * @return
   *  the non-{@code null} access level.
   *
   * @since JaXLib 1.0
   */
    public final AccessLevel getAccessLevel() {
        return AccessLevel.valueOfModifiers(this.modifiers);
    }

    /**
   * Get the kind of the class this {@code ClassInfo} represents.
   *
   * @return
   *  the non-{@code null} kind.
   *
   * @since JaXLib 1.0
   */
    public final ClassKind getClassKind() {
        if ((this.modifiers & 0x02000) != 0) return ClassKind.ANNOTATION; else if ((this.modifiers & Modifier.INTERFACE) != 0) return ClassKind.INTERFACE; else if ((this.modifiers & 0x04000) != 0) return ClassKind.ENUM; else return ClassKind.CLASS;
    }

    /**
   * The number of interfaces implemented by the class itself.
   * The number exludes interfaces implemented by superclasses but not by the class itself.
   *
   * @return
   *  the non-negative count of interfaces.
   *
   * @see #getInterfaceNames()
   *
   * @since JaXLib 1.0
   */
    public final int getInterfaceCount() {
        return this.interfaceNames.length;
    }

    /**
   * Get the interface name of the class' interface implementation list.
   *
   * @return
   *  the non-{@code null} interface name.
   *
   * @param index
   *  the index in the list of interface names.
   *
   * @throws IndexOutOfBoundsException
   *  if {@code (index < 0) || (index >= {@link #getInterfaceCount()})}.
   *
   * @see #getInterfaceNames()
   *
   * @since JaXLib 1.0
   */
    public final String getInterfaceName(final int index) {
        return this.interfaceNames[index];
    }

    /**
   * Get the names of all interfaces implemented by the class itself.
   * The array exludes interfaces implemented by superclasses but not by the class itself.
   *
   * @return
   *  the non-{@code null} array of interface names; an empty array if the class isn't implementing any.
   *
   * @see Class#getInterfaces()
   *
   * @since JaXLib 1.0
   */
    public final String[] getInterfaceNames() {
        return (this.interfaceNames.length == 0) ? EMPTY_STRING_ARRAY : this.interfaceNames.clone();
    }

    /**
   * Determine whether the class is anonymous.
   * An anonymous class is a class with the empty string {@code ""} as name.
   *
   * @see Class#isAnonymousClass()
   *
   * @since JaXLib 1.0
   */
    @Override
    public final boolean isAnonymous() {
        return this.name == "";
    }

    /**
   * Determine whether the class itself implements the named interface.
   * This method returns {@code false} if a superclass but not the class itself is implementing the
   * interface.
   *
   * @return 
   *  {@code true} if and only if the class declaration includes the named interface.
   *
   * @param name
   *  the interface name to look for.
   *
   * @see #getInterfaceNames()
   *
   * @since JaXLib 1.0
   */
    public final boolean isInterfacePresent(final String name) {
        return isClassNamePresent(this.interfaceNames, name);
    }

    /**
   * Determine whether the class itself implements the named interface.
   * This method returns {@code false} if a superclass but not the class itself is implementing the
   * interface.
   *
   * @return 
   *  {@code true} if and only if the class declaration includes the named interface.
   *
   * @param name
   *  the interface name to look for.
   *
   * @see #getInterfaceNames()
   *
   * @since JaXLib 1.0
   */
    public final boolean isInterfacePresent(final Class<?> c) {
        return isClassNamePresent(this.interfaceNames, c);
    }

    /**
   * Determine whether the class is synthetic.
   *
   * @see Class#isSynthetic()
   *
   * @since JaXLib 1.0
   */
    public final boolean isSynthetic() {
        return (this.modifiers & ClassInfo.SYNTHETIC) != 0;
    }
}
