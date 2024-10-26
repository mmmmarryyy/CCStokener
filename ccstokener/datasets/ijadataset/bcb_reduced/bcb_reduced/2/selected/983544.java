package net.sourceforge.javautil.common.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import net.sourceforge.javautil.common.CollectionUtil;
import net.sourceforge.javautil.common.IOUtil;
import net.sourceforge.javautil.common.diff.IComparison;
import net.sourceforge.javautil.common.diff.ComparisonComposite;
import net.sourceforge.javautil.common.diff.ComparisonReport;
import net.sourceforge.javautil.common.diff.IComparison.Result;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.impl.ArtifactCollector;
import net.sourceforge.javautil.common.io.impl.SimplePath;
import net.sourceforge.javautil.common.visitor.IVisitorSimple;

/**
 * A base for most virtual directory implementations.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: VirtualDirectoryAbstract.java 2731 2011-02-03 05:04:14Z ponderator $
 */
public abstract class VirtualDirectoryAbstract extends VirtualArtifactAbstract<IVirtualDirectory> implements IVirtualDirectory {

    public ComparisonReport<IVirtualDirectory> compare(IVirtualDirectory directory, boolean complete) {
        return new ComparisonReport(complete, compareInternal(this, directory, complete));
    }

    /**
	 * @param directory1 The first directory entity
	 * @param directory2 The second directory entity
	 * @return The comparison results
	 */
    protected IComparison<IVirtualArtifact> compareInternal(IVirtualDirectory directory1, IVirtualDirectory directory2, boolean complete) {
        List<IComparison<IVirtualArtifact>> comparisons = new ArrayList<IComparison<IVirtualArtifact>>();
        boolean equal = true;
        List<String> compared = new ArrayList<String>();
        for (IVirtualArtifact artifact : directory1) {
            IComparison comparison = null;
            if (artifact instanceof IVirtualDirectory) {
                IVirtualDirectory artifact2 = directory2.getDirectory(artifact.getName());
                if (artifact2 == null) {
                    comparison = new IComparison(artifact, artifact2, directory2.getFile(artifact.getName()) == null ? Result.SECOND_MISSING : Result.DIFFERENT);
                } else {
                    comparison = this.compareInternal((IVirtualDirectory) artifact, artifact2, complete);
                }
            } else {
                IVirtualFile file = directory2.getFile(artifact.getName());
                if (file == null) {
                    comparison = new IComparison(artifact, null, directory2.getDirectory(artifact.getName()) == null ? Result.SECOND_MISSING : Result.DIFFERENT);
                } else {
                    comparison = ((IVirtualFile) artifact).compare(file, complete).getComparison();
                }
            }
            if (equal && comparison.getResult() != Result.EQUAL) equal = false;
            comparisons.add(comparison);
            compared.add(artifact.getName());
        }
        if (complete) {
            for (IVirtualArtifact artifact : directory2) {
                if (compared.contains(artifact.getName())) continue;
                if (this.getArtifact(artifact.getName()) == null) {
                    comparisons.add(new IComparison(null, artifact, Result.FIRST_MISSING));
                    equal = false;
                }
            }
        }
        return new ComparisonComposite<IVirtualArtifact>(directory1, directory2, equal ? Result.EQUAL : Result.DIFFERENT, comparisons.toArray(new IComparison[comparisons.size()]));
    }

    @Override
    public void clearCache() {
        super.clearCache();
        Iterator<IVirtualArtifact> artifacts = getArtifacts();
        while (artifacts.hasNext()) {
            artifacts.next().clearCache();
        }
    }

    public Iterator<IVirtualArtifact> iterator() {
        return this.getArtifacts();
    }

    public <T extends IVirtualArtifact> T getArtifact(Class<T> type, IVirtualPath path) {
        try {
            IVirtualArtifact artifact = path == null ? this : this.getArtifact(path);
            if (type.isAssignableFrom(artifact.getClass())) return (T) artifact;
        } catch (VirtualArtifactNotFoundException e) {
        }
        return null;
    }

    public IVirtualDirectory createDirectory(IVirtualPath path) {
        IVirtualDirectory directory = this;
        for (String part : path.getParts()) {
            directory = directory.createDirectory(part);
        }
        return directory;
    }

    public long getRecursiveLastModified() {
        Set<IVirtualArtifact> artifacts = this.getRecursiveArtifacts();
        long timestamp = this.getLastModified();
        for (IVirtualArtifact artifact : artifacts) {
            if (artifact.getLastModified() > timestamp) timestamp = artifact.getLastModified();
        }
        return timestamp;
    }

    public void move(IVirtualDirectory moveTarget) {
        this.copy(moveTarget, true);
        this.remove();
    }

    public IVirtualFile createFile(String name, byte[] contents) {
        IVirtualFile file = this.createFile(name);
        file.writeAll(contents);
        return file;
    }

    public IVirtualFile createFile(URL url) {
        return this.createFile(new File(url.getPath()).getName(), url);
    }

    public IVirtualFile createFile(String name, URL url) {
        try {
            return this.createFile(name, url.openStream());
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    public IVirtualFile createFile(String name, InputStream input) {
        IVirtualFile file = this.createFile(name);
        try {
            IOUtil.transfer(input, file.getOutputStream(), null, true).close();
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
        return file;
    }

    public IVirtualFile createFile(IVirtualPath path) {
        return this.createDirectory(new SimplePath(CollectionUtil.pop(path.getParts()))).createFile(path.getPart(path.getPartCount() - 1));
    }

    public IVirtualFile createFile(IVirtualPath path, byte[] contents) throws IOException {
        IVirtualFile file = this.createFile(path);
        file.writeAll(contents);
        return file;
    }

    public IVirtualFile createFile(IVirtualPath path, InputStream input) throws IOException {
        IVirtualFile file = this.createFile(path);
        IOUtil.transfer(input, file.getOutputStream(), null, true).close();
        return file;
    }

    public IVirtualFile createFile(IVirtualPath path, URL url) {
        try {
            return this.createFile(path, url.openStream());
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    public Set<IVirtualArtifact> getRecursiveArtifacts() {
        return this.accept(new ArtifactCollector<IVirtualArtifact>()).getCollected();
    }

    public IVirtualPath getRelativePath(IVirtualArtifact artifact) {
        return this.getPath().getRelativePath(artifact.getPath());
    }

    public <T extends IVisitorSimple<VirtualDirectoryVisitorContext>> T accept(T visitor) {
        this.visit(visitor, new VirtualDirectoryVisitorContext(this, this));
        return visitor;
    }

    public boolean isExists(IVirtualPath path) {
        try {
            return this.getArtifact(path) != null;
        } catch (VirtualArtifactNotFoundException e) {
            return false;
        }
    }

    public IVirtualArtifact getArtifact(IVirtualPath path) throws VirtualArtifactNotFoundException {
        IVirtualArtifact found = null;
        IVirtualArtifact current = this;
        int parts = 0;
        for (int p = 0; p < path.getPartCount(); p++) {
            if ("..".equals(path.getPart(p))) {
                current = current.getOwner();
                continue;
            }
            if ("".equals(path.getPart(p))) continue;
            if (current instanceof IVirtualDirectory) {
                parts = p;
                current = ((IVirtualDirectory) current).getArtifact(path.getPart(p));
            } else break;
        }
        if (parts == path.getPartCount() - 1) found = current;
        if (found == null) throw new VirtualArtifactNotFoundException(this, "Could not find: " + path.toString("/"));
        return found;
    }

    public IVirtualDirectory getDirectory(IVirtualPath path, boolean create) throws VirtualArtifactNotFoundException {
        if (!create) return this.getDirectory(path);
        IVirtualDirectory directory = this;
        for (String part : path.getParts()) {
            directory = directory.getDirectory(part, true);
        }
        directory.makeDirectories();
        return directory;
    }

    public IVirtualFile getFile(IVirtualPath path, boolean create) throws VirtualArtifactNotFoundException {
        if (!create) return this.getFile(path);
        return this.getDirectory(new SimplePath(CollectionUtil.pop(path.getParts())), true).getFile(path.getPart(path.getPartCount() - 1), true);
    }

    public List<IVirtualDirectory> getDirectories() {
        List<IVirtualDirectory> directories = new ArrayList<IVirtualDirectory>();
        Iterator<IVirtualArtifact> artifacts = this.getArtifacts();
        while (artifacts.hasNext()) {
            IVirtualArtifact artifact = artifacts.next();
            if (artifact instanceof IVirtualDirectory) directories.add((IVirtualDirectory) artifact);
        }
        return directories;
    }

    public List<IVirtualFile> getFiles() {
        List<IVirtualFile> files = new ArrayList<IVirtualFile>();
        Iterator<IVirtualArtifact> artifacts = this.getArtifacts();
        while (artifacts.hasNext()) {
            IVirtualArtifact artifact = artifacts.next();
            if (artifact instanceof IVirtualFile) files.add((IVirtualFile) artifact);
        }
        return files;
    }

    public void copy(IVirtualDirectory directory, boolean recursive) {
        Iterator<IVirtualArtifact> artifacts = this.getArtifacts();
        while (artifacts.hasNext()) {
            IVirtualArtifact artifact = artifacts.next();
            if (artifact instanceof IVirtualFile) {
                ((IVirtualFile) artifact).copy(directory);
            } else {
                IVirtualDirectory newdir = directory.getDirectory(artifact.getName(), true);
                if (recursive) ((IVirtualDirectory) artifact).copy(newdir, true);
            }
        }
    }

    public void compress(ZipOutputStream output, String prefix) throws IOException {
        Iterator<IVirtualArtifact> artifacts = this.getArtifacts();
        while (artifacts.hasNext()) {
            IVirtualArtifact artifact = artifacts.next();
            String name = prefix + (artifact instanceof IVirtualFile ? artifact.getName() : artifact.getName() + "/");
            log.info("Adding " + name + " to compressed archive");
            try {
                ZipEntry entry = new ZipEntry(name);
                output.putNextEntry(entry);
                if (artifact instanceof IVirtualFile) {
                    IOUtil.transfer(((IVirtualFile) artifact).getInputStream(), output);
                }
                output.closeEntry();
            } catch (ZipException e) {
                if (e.getMessage().contains("duplicate entry")) {
                    log.warn("Ignoring duplicate artifact: " + name);
                } else throw e;
            }
            if (artifact instanceof IVirtualDirectory) {
                ((IVirtualDirectory) artifact).compress(output, prefix + artifact.getName() + "/");
            }
        }
    }

    public boolean makeDirectories() {
        if (this.getOwner() != null && !this.getOwner().makeDirectories()) return false;
        return this.makeDirectory();
    }

    /**
	 * Convenience method for compressing to a file
	 * 
	 * @param file
	 * @throws IOException
	 */
    public void compress(IVirtualFile file) {
        ZipOutputStream output = null;
        try {
            output = new ZipOutputStream(file.getOutputStream());
            this.compress(output, "");
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        } finally {
            if (output != null) try {
                output.close();
            } catch (IOException e) {
                ThrowableManagerRegistry.caught(e);
            }
        }
    }

    /**
	 * This is the recursive method that calls itself for each artifact
	 * or descendant artifact found.
	 * 
	 * @param visitor The visitor to use in the visitor pattern search
	 * @param artifact The artifact currently to be visited
	 */
    protected void visit(IVisitorSimple<VirtualDirectoryVisitorContext> visitor, VirtualDirectoryVisitorContext ctx) {
        ctx.visit(visitor, this);
    }

    public IVirtualDirectory getDirectory(String name) {
        IVirtualArtifact artifact = this.getArtifact(name);
        if (artifact instanceof IVirtualFile) throw new VirtualArtifactException(this, name + " refers to a file, not a directory");
        return artifact instanceof IVirtualDirectory ? (IVirtualDirectory) artifact : null;
    }

    public IVirtualDirectory getDirectory(String name, boolean create) {
        IVirtualDirectory directory = this.getDirectory(name);
        return directory == null && create ? this.createDirectory(name) : directory;
    }

    public IVirtualDirectory getDirectory(IVirtualPath path) throws VirtualArtifactNotFoundException {
        IVirtualArtifact artifact = this.getArtifact(path);
        if (!(artifact instanceof IVirtualDirectory)) throw new VirtualArtifactException(artifact, "This is not a directory");
        return (IVirtualDirectory) artifact;
    }

    public IVirtualFile getFile(String name) {
        return this.getFile(name, false);
    }

    public IVirtualFile getFile(String name, boolean create) {
        IVirtualArtifact artifact = this.getArtifact(name);
        if (artifact instanceof IVirtualDirectory) throw new VirtualArtifactException(this, name + " refers to a directory, not a file");
        if (artifact == null && create) artifact = this.createFile(name);
        return artifact instanceof IVirtualFile ? (IVirtualFile) artifact : null;
    }

    public IVirtualFile getFile(IVirtualPath path) throws VirtualArtifactNotFoundException {
        IVirtualArtifact artifact = this.getArtifact(path);
        if (!(artifact instanceof IVirtualFile)) throw new VirtualArtifactException(artifact, "This is not a file");
        return (IVirtualFile) artifact;
    }

    public String toString() {
        return getPath().toString("/");
    }
}
