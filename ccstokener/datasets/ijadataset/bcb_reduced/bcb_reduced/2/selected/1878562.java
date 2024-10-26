package de.grogra.pf.ui.registry;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import de.grogra.graph.impl.Node.NType;
import de.grogra.persistence.Manageable;
import de.grogra.pf.io.*;
import de.grogra.pf.registry.*;
import de.grogra.pf.ui.*;
import de.grogra.util.*;
import de.grogra.vfs.FileSystem;

public class FileFactory extends ObjectItemFactory {

    String directory;

    String mimeType;

    public static final NType $TYPE;

    public static final NType.Field directory$FIELD;

    public static final NType.Field mimeType$FIELD;

    private static final class _Field extends NType.Field {

        private final int id;

        _Field(String name, int modifiers, de.grogra.reflect.Type type, de.grogra.reflect.Type componentType, int id) {
            super(FileFactory.$TYPE, name, modifiers, type, componentType);
            this.id = id;
        }

        @Override
        protected void setObjectImpl(Object o, Object value) {
            switch(id) {
                case 0:
                    ((FileFactory) o).directory = (String) value;
                    return;
                case 1:
                    ((FileFactory) o).mimeType = (String) value;
                    return;
            }
            super.setObjectImpl(o, value);
        }

        @Override
        public Object getObject(Object o) {
            switch(id) {
                case 0:
                    return ((FileFactory) o).directory;
                case 1:
                    return ((FileFactory) o).mimeType;
            }
            return super.getObject(o);
        }
    }

    static {
        $TYPE = new NType(new FileFactory());
        $TYPE.addManagedField(directory$FIELD = new _Field("directory", 0 | _Field.SCO, de.grogra.reflect.ClassAdapter.wrap(String.class), null, 0));
        $TYPE.addManagedField(mimeType$FIELD = new _Field("mimeType", 0 | _Field.SCO, de.grogra.reflect.ClassAdapter.wrap(String.class), null, 1));
        $TYPE.validate();
    }

    @Override
    protected NType getNTypeImpl() {
        return $TYPE;
    }

    @Override
    protected de.grogra.graph.impl.Node newInstance() {
        return new FileFactory();
    }

    @Override
    protected Item createItemImpl(Context ctx) {
        Object[] a = read(ctx.getWorkbench().getRegistry(), null, true, ctx.getWorkbench());
        return (a == null) ? null : (Item) a[1];
    }

    private Object[] read(Registry reg, FilterSource src, boolean forceItem, Workbench ui) {
        IOFlavor flavor = IOFlavor.valueOf(getObjectType().getImplementationClass());
        boolean srcProvided = src != null;
        if (src == null) {
            FileChooserResult fr = ui.chooseFileToOpen(null, flavor);
            if (fr == null) {
                return null;
            }
            src = fr.createFileSource(reg, new StringMap(this));
        }
        Object o = ui.readObject(src, flavor);
        if (o == null) {
            return null;
        }
        Object[] a = { o, null };
        boolean embeddable = (o instanceof Manageable) && ((Manageable) o).getManageableType().isSerializable();
        if (!forceItem && embeddable) {
            return a;
        }
        ObjectItem item;
        if (src instanceof FileSource) {
            File f = ((FileSource) src).getInputFile();
            switch(srcProvided ? 0 : ui.getWindow().showChoiceDialog(IO.toName(src.getSystemId()), UI.I18N, "addfiledialog", embeddable ? new String[] { "add", "link", "embed" } : new String[] { "add", "link" })) {
                case 0:
                    FileSystem pfs = reg.getFileSystem();
                    Object dir = pfs.getRoot();
                    if (directory != null) {
                        try {
                            dir = pfs.create(dir, directory, true);
                        } catch (IOException e) {
                            ui.logGUIInfo(IO.I18N.msg("mkdir.failed", directory), e);
                            return null;
                        }
                    }
                    Object dest;
                    try {
                        dest = pfs.create(dir, f.getName(), false, true);
                        pfs.addLocalFile(f, dir, pfs.getName(dest));
                    } catch (IOException e) {
                        ui.logGUIInfo(IO.I18N.msg("addfile.failed", directory), e);
                        return null;
                    }
                    item = new FileObjectItem(IO.toSystemId(pfs, dest), src.getFlavor().getMimeType(), o, getTypeAsString());
                    break;
                case 1:
                    item = new FileObjectItem((FileSource) src, o, getTypeAsString());
                    break;
                case 2:
                    item = ObjectItem.createReference(reg, o, IO.toSimpleName(src.getSystemId()));
                    break;
                default:
                    return null;
            }
        } else {
            item = ObjectItem.createReference(reg, o, IO.toSimpleName(src.getSystemId()));
        }
        a[1] = item;
        return a;
    }

    @Override
    public Object evaluate(RegistryContext ctx, de.grogra.util.StringMap args) {
        URL url = null;
        if (getBranch() != null) {
            Object[] a = getArgs((Item) getBranch(), ctx, args, this);
            if ((a.length > 0) && (a[0] instanceof URL)) {
                url = (URL) a[0];
            }
        }
        return addFromURL(ctx.getRegistry(), url, args, ((Context) args.get("context")).getWorkbench());
    }

    /**
	 * Reads an object from <code>url</code>. If necessary,
	 * an item reference to the object is created in the registry.
	 * 
	 * @param reg registry in which the reference shall be created
	 * @param url URL to read
	 * @param params parameters, may be <code>null</code>
	 * @param ui workbench to use for UI
	 * @return the import object, or <code>null</code> in case of problems
	 */
    public Object addFromURL(Registry reg, URL url, ModifiableMap params, Workbench ui) {
        FilterSource fs = null;
        if (url != null) {
            try {
                MimeType mt = (mimeType != null) ? MimeType.valueOf(mimeType) : IO.getMimeType(url.toString());
                File file = Utils.urlToFile(url);
                if (file.exists()) {
                    fs = new FileSource(file, mt, reg, params);
                } else {
                    fs = new InputStreamSourceImpl(url.openStream(), url.toString(), mt, reg, params);
                }
            } catch (IOException e) {
                ui.logGUIInfo(IO.I18N.msg("openfile.failed", url.getFile()), e);
                return null;
            }
        }
        Object[] a = read(reg, fs, false, ui);
        if (a == null) {
            return null;
        }
        ObjectItem i = (ObjectItem) a[1];
        if (i != null) {
            i.setObjDescribes(true);
            ResourceDirectory rd = ResourceDirectory.get(this);
            rd.getProjectDirectory(reg).addUserItemWithUniqueName(i, rd.getBaseName());
        }
        return a[0];
    }
}
