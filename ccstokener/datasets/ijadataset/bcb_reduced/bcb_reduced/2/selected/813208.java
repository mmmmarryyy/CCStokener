package net.sf.freenote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import net.sf.freenote.action.ChangeColorAction;
import net.sf.freenote.action.ChangeConnectionStyleAction;
import net.sf.freenote.action.ChangePropertiesAction;
import net.sf.freenote.action.ChangeZoomLevelAction;
import net.sf.freenote.action.ExportShapeAction;
import net.sf.freenote.action.FileChooseAction;
import net.sf.freenote.action.FileSaveAsAction;
import net.sf.freenote.action.FitImageAction;
import net.sf.freenote.action.OpenShapeAction;
import net.sf.freenote.action.SelectSameAction;
import net.sf.freenote.action.TextDirectEditAction;
import net.sf.freenote.mindmap.ChangeLayoutAction;
import net.sf.freenote.mindmap.ExpandAction;
import net.sf.freenote.mindmap.model.BranchShape;
import net.sf.freenote.mindmap.model.RootShape;
import net.sf.freenote.model.CircleShape;
import net.sf.freenote.model.Connection;
import net.sf.freenote.model.EllipticalShape;
import net.sf.freenote.model.FileShape;
import net.sf.freenote.model.ImageFileShape;
import net.sf.freenote.model.LinkFileShape;
import net.sf.freenote.model.ModelElement;
import net.sf.freenote.model.RectangularShape;
import net.sf.freenote.model.Shape;
import net.sf.freenote.model.ShapesDiagram;
import net.sf.freenote.model.TextShape;
import net.sf.freenote.parts.ShapesEditPartFactory;
import net.sf.freenote.uml.model.ClassShape;
import net.sf.freenote.uml.model.SystemShape;
import net.sf.freenote.uml.model.UmlShape;
import net.sf.freenote.uml.model.UseCaseShape;
import net.sf.freenote.uml.model.UserRoleShape;
import net.sf.util.StringUtil;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.SaveAllAction;
import org.eclipse.ui.internal.SaveAsAction;
import org.eclipse.ui.internal.part.NullEditorInput;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * A graphical editor with flyout palette that can edit .shapes files. The
 * binding between the .shapes file extension and this editor is done in
 * plugin.xml
 * 
 * @author Elias Volanakis
 */
public class ShapesEditor extends GraphicalEditorWithFlyoutPalette {

    public static final String ID = "net.sf.freenote.ShapesEditor";

    /** This is the root of the editor's model. */
    private ShapesDiagram diagram;

    /** Palette component, holding the tools and shapes. */
    private static PaletteRoot PALETTE_MODEL;

    private KeyHandler sharedKeyHandler;

    private static XStream xstream = new XStream(new DomDriver());

    static {
        Class[] clazz = new Class[] { ModelElement.class, Connection.class, ShapesDiagram.class, Shape.class, CircleShape.class, EllipticalShape.class, FileShape.class, ImageFileShape.class, LinkFileShape.class, RectangularShape.class, TextShape.class, BranchShape.class, RootShape.class, UmlShape.class, ClassShape.class, SystemShape.class, UseCaseShape.class, UserRoleShape.class };
        for (Class c : clazz) {
            xstream.alias(c.getSimpleName(), c);
        }
        xstream.useAttributeFor(RGB.class, "red");
        xstream.useAttributeFor(RGB.class, "green");
        xstream.useAttributeFor(RGB.class, "blue");
        xstream.useAttributeFor(Point.class, "x");
        xstream.useAttributeFor(Point.class, "y");
        xstream.useAttributeFor(Dimension.class, "width");
        xstream.useAttributeFor(Dimension.class, "height");
        xstream.addImplicitCollection(ShapesDiagram.class, "children");
        xstream.addImplicitCollection(BranchShape.class, "children");
        xstream.addImplicitCollection(SystemShape.class, "children");
    }

    /** Create a new ShapesEditor instance. This is called by the Workspace. */
    public ShapesEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    /**
	 * Configure the graphical viewer before it receives contents.
	 * <p>
	 * This is the place to choose an appropriate RootEditPart and
	 * EditPartFactory for your editor. The RootEditPart determines the behavior
	 * of the editor's "work-area". For example, GEF includes zoomable and
	 * scrollable root edit parts. The EditPartFactory maps model elements to
	 * edit parts (controllers).
	 * </p>
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new ShapesEditPartFactory());
        ScalableFreeformRootEditPart rootPart = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootPart);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer).setParent(getCommonKeyHandler()));
        ZoomManager zoomManager = rootPart.getZoomManager();
        double[] levels = new double[] { 0.1, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 5.0, 10.0, 20.0 };
        zoomManager.setZoomLevels(levels);
        List<String> list = new ArrayList<String>();
        list.add(FreeNoteConstants.ZOOM_SEPARATOR);
        list.add(ZoomManager.FIT_ALL);
        list.add(ZoomManager.FIT_HEIGHT);
        list.add(ZoomManager.FIT_WIDTH);
        zoomManager.setZoomLevelContributions(list);
        IAction action = new ZoomInAction(zoomManager);
        getActionRegistry().registerAction(action);
        action = new ZoomOutAction(zoomManager);
        getActionRegistry().registerAction(action);
        ContextMenuProvider cmProvider = new ShapesEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);
    }

    protected void createActions() {
        super.createActions();
        ActionRegistry ar = getActionRegistry();
        IAction[] actions = new IAction[] { new TextDirectEditAction(this), new ChangeColorAction(this, FreeNoteConstants.BACKCOLOR), new ChangeColorAction(this, FreeNoteConstants.FORECOLOR), new FileChooseAction(this), new FileSaveAsAction(this), new FitImageAction(this), new AlignmentAction((IWorkbenchPart) this, PositionConstants.LEFT), new AlignmentAction((IWorkbenchPart) this, PositionConstants.CENTER), new AlignmentAction((IWorkbenchPart) this, PositionConstants.RIGHT), new AlignmentAction((IWorkbenchPart) this, PositionConstants.TOP), new AlignmentAction((IWorkbenchPart) this, PositionConstants.MIDDLE), new AlignmentAction((IWorkbenchPart) this, PositionConstants.BOTTOM), new MatchWidthAction((IWorkbenchPart) this), new MatchHeightAction((IWorkbenchPart) this), new ChangePropertiesAction((IWorkbenchPart) this), new ExpandAction((IWorkbenchPart) this), new ChangeConnectionStyleAction((IWorkbenchPart) this), new SelectSameAction((IWorkbenchPart) this), new ChangeLayoutAction((IWorkbenchPart) this, FreeNoteConstants.LAYOUT_NORMAL), new ChangeLayoutAction((IWorkbenchPart) this, FreeNoteConstants.LAYOUT_HANGING), new ChangeLayoutAction((IWorkbenchPart) this, FreeNoteConstants.LAYOUT_STAR) };
        for (IAction a : actions) {
            ar.registerAction(a);
            getSelectionActions().add(a.getId());
        }
        ar.registerAction(new SaveAsAction(this.getSite().getWorkbenchWindow()));
        ar.registerAction(new SaveAllAction(this.getSite().getWorkbenchWindow()));
        ar.registerAction(new ExportShapeAction(this.getSite().getWorkbenchWindow()));
        ar.registerAction(new ChangeZoomLevelAction(this.getSite().getPage()));
    }

    protected KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            ActionRegistry ar = getActionRegistry();
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), ar.getAction(GEFActionConstants.DIRECT_EDIT));
            sharedKeyHandler.put(KeyStroke.getPressed((char) 01, (int) 'a', SWT.CTRL), ar.getAction(ActionFactory.SELECT_ALL.getId()));
            sharedKeyHandler.put(KeyStroke.getPressed((char) 19, (int) 's', SWT.CTRL), ar.getAction(ActionFactory.SAVE.getId()));
            sharedKeyHandler.put(KeyStroke.getPressed((char) 127, 127, 0), ar.getAction(ActionFactory.DELETE.getId()));
            sharedKeyHandler.put(KeyStroke.getPressed((char) 26, (int) 'z', SWT.CTRL), ar.getAction(ActionFactory.UNDO.getId()));
            sharedKeyHandler.put(KeyStroke.getPressed((char) 25, (int) 'y', SWT.CTRL), ar.getAction(ActionFactory.REDO.getId()));
            sharedKeyHandler.put(KeyStroke.getPressed((char) 16, (int) 'p', SWT.CTRL), ar.getAction(FreeNoteConstants.PROPERTY_EDITOR));
        }
        return sharedKeyHandler;
    }

    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    /**
	 * Create a transfer drop target listener. When using a
	 * CombinedTemplateCreationEntry tool in the palette, this will enable model
	 * element creation by dragging from the palette.
	 * 
	 * @see #createPaletteViewerProvider()
	 */
    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(getGraphicalViewer()) {

            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    public void doSave(IProgressMonitor monitor) {
        if (getEditorInput() instanceof NullEditorInput) {
            doSaveAs();
        } else {
            File file = ((IPathEditorInput) getEditorInput()).getPath().toFile();
            writeContent(file);
        }
    }

    private String openFileDialog() {
        FileDialog fd = new FileDialog(getSite().getShell(), SWT.SAVE);
        fd.setText("请输入一个文件名");
        fd.setFilterExtensions(new String[] { "*.fn" });
        return fd.open();
    }

    public void doSaveAs() {
        String path = openFileDialog();
        if (path != null) {
            if (!path.toLowerCase().endsWith(".fn")) path += ".fn";
            File file = new File(path);
            if (!file.exists() || MessageDialog.openConfirm(this.getSite().getShell(), "文件覆盖确认", "文件已经存在，是否覆盖?")) {
                writeContent(file);
                setInput(new PathEditorInput(new Path(path)));
                OpenShapeAction.addToMRU(path);
            }
        }
    }

    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        return super.getAdapter(type);
    }

    ShapesDiagram getModel() {
        return diagram;
    }

    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_MODEL == null) PALETTE_MODEL = ShapesEditorPaletteFactory.createPalette();
        return PALETTE_MODEL;
    }

    private void handleLoadException(Exception e) {
        System.err.println("** Load failed. Using default model. **");
        diagram = new ShapesDiagram();
    }

    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
        viewer.addDropTargetListener(createTransferDropTargetListener());
    }

    @Override
    public GraphicalViewer getGraphicalViewer() {
        return super.getGraphicalViewer();
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    private void writeContent(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            xstream.toXML(getModel(), fos);
            fos.close();
            getCommandStack().markSaveLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void setInput(IEditorInput input) {
        super.setInput(input);
        try {
            if (input instanceof NullEditorInput && StringUtil.isNotNull(((NullEditorInput) input).getToolTipText())) {
                String path = ((NullEditorInput) input).getToolTipText();
                URL url = FileLocator.find(ShapesPlugin.getDefault().getBundle(), new Path(path), null);
                InputStream in = url.openStream();
                diagram = (ShapesDiagram) xstream.fromXML(in);
                in.close();
            } else {
                File file = ((IPathEditorInput) input).getPath().toFile();
                FileInputStream fis = new FileInputStream(file);
                diagram = (ShapesDiagram) xstream.fromXML(fis);
                fis.close();
                setPartName(file.getName());
            }
        } catch (Exception e) {
            handleLoadException(e);
        }
    }

    public IAction getAction(String id) {
        return getActionRegistry().getAction(id);
    }
}
