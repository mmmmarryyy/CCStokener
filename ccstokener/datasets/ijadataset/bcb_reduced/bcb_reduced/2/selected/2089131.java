package june.engine;

import java.io.*;
import java.net.*;
import java.util.*;
import june.tree.*;
import org.objectweb.asm.*;

public abstract class Resolver {

    /**
	 * Searches imports and the package hierarchy on the classpath.
	 */
    public static class ImportResolver extends Resolver {

        /**
		 * TODO Minimal VFS to support network, in-memory, and so on? Shouldn't use Java 6 APIs for Java 5 compatibility, however.
		 */
        private final List<String> classpath;

        private final Map<String, Entity> globals;

        private final Set<String> imports;

        /**
		 * @param imports
		 *            can't really accommodate current package, since that has a specific priority. And what about "import bob: java.sql"?
		 * @param globals
		 * @param classpath
		 */
        public ImportResolver(Set<String> imports, Map<String, Entity> globals, List<String> classpath) {
            this.imports = imports;
            this.globals = globals;
            this.classpath = classpath;
        }

        private void addMatches(Set<Entity> matches, String $import, Usage usage) {
            try {
                String[] packageAndClass = splitPackageAndClass(globals, $import);
                String packageName = packageAndClass[0];
                String baseClassName = packageAndClass[1];
                boolean expectClass = false;
                if (baseClassName == null) {
                    String tempPackageName = packageName + (packageName.length() > 0 ? "." : "") + usage.name;
                    if (Package.getPackage(tempPackageName) != null) {
                        matches.add(new JunePackage());
                        return;
                    }
                    baseClassName = usage.name;
                    expectClass = true;
                }
                JuneClass $class = loadClass(globals, packageName, baseClassName);
                if ($class != null) {
                    if (expectClass) {
                        matches.add($class);
                    } else {
                        JuneMember member = $class.getMember(usage);
                        if (member != null) {
                            matches.add(member);
                        }
                    }
                }
            } catch (Exception e) {
                throw Helper.throwAny(e);
            }
        }

        @Override
        protected Entity findCurrentEntity(Usage usage) {
            Set<Entity> matches = new HashSet<Entity>();
            addMatches(matches, "", usage);
            for (String $import : imports) {
                addMatches(matches, $import, usage);
            }
            if (matches.isEmpty()) {
                return null;
            } else if (matches.size() > 1) {
                throw new RuntimeException(matches.size() + " matches for " + usage + " in " + imports);
            }
            return matches.iterator().next();
        }
    }

    /**
	 * Resolves members of the current class and superclasses, depending on visibility of members and so on. TODO How does this relate to locals and nested scopes?
	 */
    public static class MemberResolver extends Resolver {

        @Override
        protected Entity findCurrentEntity(Usage usage) {
            return null;
        }
    }

    public static JuneClass loadClass(Map<String, Entity> globals, String packageName, String baseClassName) {
        try {
            JuneClass $class = null;
            String resourceName = (packageName.length() > 0 ? packageName.replace('.', '/') + "/" : "") + baseClassName.replace('.', '$') + ".class";
            URL url = Resolver.class.getClassLoader().getResource(resourceName);
            if (url != null) {
                ClassBuilder builder = new ClassBuilder(globals);
                InputStream stream = url.openStream();
                try {
                    new ClassReader(new BufferedInputStream(stream)).accept(builder, ClassReader.SKIP_CODE);
                } finally {
                    stream.close();
                }
                $class = builder.$class;
                $class.loaded = true;
            }
            return $class;
        } catch (Exception e) {
            throw Helper.throwAny(e);
        }
    }

    public static String[] splitPackageAndClass(Map<String, Entity> globals, String qualifiedName) {
        Entity entity = globals.get(qualifiedName);
        if (entity instanceof JunePackage) {
            return new String[] { qualifiedName, null };
        } else if (entity instanceof JuneClass) {
            JuneClass $class = (JuneClass) entity;
            if ($class.$package != null) {
                return new String[] { $class.$package.name, $class.baseName };
            }
        }
        String packageName = qualifiedName;
        String baseClassName = null;
        Package $package = Package.getPackage(packageName);
        while (packageName.length() > 0 && $package == null) {
            int lastDot = packageName.lastIndexOf('.');
            baseClassName = packageName.substring(lastDot + 1) + (baseClassName == null ? "" : "." + baseClassName);
            packageName = packageName.substring(0, lastDot < 0 ? 0 : lastDot);
            $package = Package.getPackage(packageName);
        }
        if (packageName.length() > 0) {
            ClassBuilder.accessPackage(globals, packageName);
        }
        return new String[] { packageName, baseClassName };
    }

    /**
	 * TODO So far I'm not using the cool hierarchy concept. Maybe get rid of it?
	 */
    public Resolver parent;

    protected abstract Entity findCurrentEntity(Usage usage);

    public Entity findEntity(Usage usage) {
        Entity entity = findCurrentEntity(usage);
        if (entity == null && parent != null) {
            entity = parent.findEntity(usage);
        }
        return entity;
    }
}
