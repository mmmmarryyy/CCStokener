package kfschmidt.quickvol;

import kfschmidt.geom3d.*;
import kfschmidt.geom3d.io.*;
import kfschmidt.quickvol.io.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.util.Vector;
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.awt.geom.*;

public class QuickVolControler implements MeshMakerListener {

    QuickVolListener[] mQVListeners;

    Project mCurProject;

    Project mLastProject;

    ModelManager mModelManager;

    public QuickVolControler() {
        mModelManager = new ModelManager(this);
    }

    public void meshMakerUpdate(int flag, MeshMaker source) {
        if (flag == MeshMaker.CURRENTLY_MARCHING) System.out.println("MeshMaker is marching..."); else if (flag == MeshMaker.CURRENTLY_SMOOTHING) System.out.println("MeshMaker is smoothing..."); else if (flag == MeshMaker.CURRENTLY_INDEXING) System.out.println("MeshMaker is indexing..."); else if (flag == MeshMaker.CURRENTLY_GEN_NORMALS) System.out.println("MeshMaker is generating normals..."); else if (flag == MeshMaker.FINISHED) {
            System.out.println("MeshMaker finished... test output to POV");
        }
    }

    /**
     *   adds the QuickVolListener to the notification queue
     *
     */
    public void addQuickVolListener(QuickVolListener ql) {
        if (mQVListeners == null && ql != null) {
            mQVListeners = new QuickVolListener[1];
            mQVListeners[0] = ql;
        } else if (mQVListeners != null && ql != null) {
            QuickVolListener[] tmp = new QuickVolListener[mQVListeners.length + 1];
            for (int a = 0; a < mQVListeners.length; a++) {
                tmp[a] = mQVListeners[a];
            }
            tmp[tmp.length - 1] = ql;
            mQVListeners = tmp;
        }
    }

    /**
     *   removes the QuickVolListener from the notification queue
     *
     */
    public void removeQuickVolListener(QuickVolListener ql) {
        if (mQVListeners == null && ql != null) {
            mQVListeners = new QuickVolListener[1];
            mQVListeners[0] = ql;
        } else if (mQVListeners != null && ql != null) {
            QuickVolListener[] tmp = new QuickVolListener[mQVListeners.length + 1];
            for (int a = 0; a < mQVListeners.length; a++) {
                tmp[a] = mQVListeners[a];
            }
            tmp[tmp.length - 1] = ql;
            mQVListeners = tmp;
        }
    }

    /**
     *   dispatch a QuickVol event to all registered
     *   listeners
     *
     */
    public void dispatchQVEvent(QuickVolEvent qe) {
        if (mQVListeners != null) {
            for (int a = 0; a < mQVListeners.length; a++) {
                mQVListeners[a].processQuickVolEvent(qe);
            }
        }
    }

    /**
     *   Used for UI synchronization, changes the application 
     *   mode to STACK EDIT for adding, removing and editing layers
     *
     */
    public void setModeToStackEdit() {
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.MODE_CHANGED_TO_STACK_EDIT));
    }

    /**
     *   Used for UI synchronization, changes the application 
     *   mode to FEATURE EDIT for adding, removing and editing layers
     *
     */
    public void setModeToFeatureEdit() {
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.MODE_CHANGED_TO_FEATURE_EDIT));
    }

    /**
     *   Used for UI synchronization, changes the application 
     *   mode to MODEL EDIT for adding, removing and editing layers
     *
     */
    public void setModeToModelEdit() {
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.MODE_CHANGED_TO_MODEL_EDIT));
    }

    /**
     *   Creates a new project and sets it
     *   as the current project
     *
     */
    public void createProject(String name) {
        mCurProject = new Project(name);
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.NEW_PROJECT));
    }

    /**
     * 
     *
     */
    public void setSelectedModel(int idx) {
        mCurProject.setSelectedModelIdx(idx);
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.MODEL_SELECTION_CHANGE));
    }

    /**
     *   Returns the current model list
     *
     */
    public Model[] getModelList() {
        if (mCurProject == null) return null; else return mCurProject.getModels();
    }

    /**
     *  notifies the controler that a model has been updated
     *  or reconstructed
     */
    public void modelHasBeenUpdated(Model m) {
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.MODEL_UPDATE);
        e.setAssociatedObject(m);
        dispatchQVEvent(e);
    }

    /**
     *   Returns the selected model, if any
     *
     */
    public Model getSelectedModel() {
        if (mCurProject == null) return null; else return mCurProject.getSelectedModel();
    }

    /**
     *   Returns the selected model idx or -1 
     *   if none is currently selected
     *
     */
    public int getSelectedModelIdx() {
        if (mCurProject == null) return -1; else return mCurProject.getSelectedModelIdx();
    }

    /**
     *   Deletes the feature from the current project
     * 
     */
    public void deleteFeature(Feature f) {
        mCurProject.getStack().deleteFeature(f);
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.FEATURE_DELETED);
        e.setAssociatedObject(f);
        dispatchQVEvent(e);
        Model mod = mCurProject.removeModelAssociatedWithFeature(f);
        QuickVolEvent e2 = new QuickVolEvent(QuickVolEvent.MODEL_DELETED);
        e2.setAssociatedObject(mod);
        dispatchQVEvent(e2);
    }

    /**
     *   Deletes the specified layer from the 
     *   current project
     *
     */
    public void deleteLayer(Layer l) {
        mCurProject.getStack().deleteLayer(l);
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.STACK_UPDATE);
        dispatchQVEvent(e);
    }

    /**
     *  Creates a new feature with the specifie name 
     * in the current project
     *
     */
    public Feature createFeature(String name) {
        Feature f = null;
        if (mCurProject != null && mCurProject.getStack() != null) {
            f = mCurProject.getStack().createFeature(name);
            Model mod = new Model(f, mCurProject.getDefaultGrid3D());
            mCurProject.addModel(mod);
            QuickVolEvent e = new QuickVolEvent(QuickVolEvent.FEATURE_CREATED);
            e.setAssociatedObject(f);
            dispatchQVEvent(e);
            QuickVolEvent e2 = new QuickVolEvent(QuickVolEvent.MODEL_CREATED);
            e2.setAssociatedObject(mod);
            dispatchQVEvent(e2);
        }
        return f;
    }

    /**
     *  Adds a layer or layers to the current project
     *
     *
     */
    public void addLayer(Layer[] layers) {
        if (layers != null) {
            for (int a = 0; a < layers.length; a++) {
                mCurProject.getStack().addLayer(layers[a]);
            }
            mCurProject.getStack().sortLayers();
            dispatchQVEvent(new QuickVolEvent(QuickVolEvent.STACK_UPDATE));
        }
    }

    /**
     *   Saves the currently selected model in POV
     *   file format to the file specified
     *
     */
    public void saveAsPov(File incfile) {
        try {
            Model mod = mCurProject.getSelectedModel();
            if (mod != null && mod.getSurface() != null) {
                GeomWriter povwriter = new PovWriter();
                povwriter.writeMeshToFile(mod.getSurface(), incfile, mod.getName());
                File scenefile = new File(incfile.getParentFile().getPath() + "/sample_scene.pov");
                if (!scenefile.exists()) ((PovWriter) povwriter).writeSampleScene(scenefile, mod.getName(), mCurProject.getStack().getFOV());
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     *   Saves the current project to the specified file
     *
     */
    public void saveProject(File proj_file) {
        try {
            ProjectFileWriter.writeProjectToFile(mCurProject, proj_file);
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     *   Loads the project in the file and sets the current project
     *
     */
    public void loadProject(File proj_file) {
        Project p = null;
        try {
            p = ProjectFileReader.readProject(proj_file);
            setProject(p);
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     *   Loads the project in the file and sets the current project
     *
     */
    public void loadProject(String proj_url) {
        Project p = null;
        try {
            URL url = new URL(proj_url);
            p = ProjectFileReader.readProjectFromStream(url.openStream());
            setProject(p);
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     *   Sets the current project
     *
     */
    public void setProject(Project p) {
        System.out.println("Setting project:" + p);
        if (p != null) {
            mCurProject = p;
            dispatchQVEvent(new QuickVolEvent(QuickVolEvent.NEW_PROJECT));
        } else {
            Exception e = new Exception("Project can not be null in setProject call");
            e.fillInStackTrace();
            handleException(e);
        }
    }

    /**
     *    Adds the area to the featureelement cooresponding to the 
     *    indicated feature and layer
     *
     */
    public void addAreaToFeature(Area a, Layer l, Feature f) {
        FeatureElement[] elems = f.getFeatureElementsForLayer(l);
        if (elems != null && elems.length > 0) addAreaToFeature(a, elems[0]); else {
            f.createFeatureElement(l, a);
            QuickVolEvent e = new QuickVolEvent(QuickVolEvent.FEATURE_UPDATE);
            e.setAssociatedObject(mCurProject.getStack().getSelectedFeature());
            dispatchQVEvent(e);
        }
    }

    /**
     *   Adds the new area passed in to the feature element indicated
     *
     */
    private void addAreaToFeature(Area a, FeatureElement fe) {
        fe.addArea(a);
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.FEATURE_UPDATE);
        e.setAssociatedObject(mCurProject.getStack().getSelectedFeature());
        dispatchQVEvent(e);
    }

    /**
     *   Subtracts the area passed in from the feature element indicated
     *
     */
    private void subtractAreaFromFeature(Area a, FeatureElement fe) {
        fe.subtractArea(a);
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.FEATURE_UPDATE);
        e.setAssociatedObject(mCurProject.getStack().getSelectedFeature());
        dispatchQVEvent(e);
    }

    /**
     *   Subtracts the area passed in from the FeatureElement 
     *   cooresponding to the indicated feature and layer
     *
     *
     */
    public void subtractAreaFromFeature(Area a, Layer l, Feature f) {
        FeatureElement[] elems = f.getFeatureElementsForLayer(l);
        if (elems != null && elems.length > 0) subtractAreaFromFeature(a, elems[0]);
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.FEATURE_UPDATE);
        e.setAssociatedObject(f);
        dispatchQVEvent(e);
    }

    /**
     *   Rotates the selected layer by rads radians
     *
     */
    public void rotateSelectedLayer(double rads) {
        mCurProject.getStack().getSelectedLayer().rotateLayer(rads);
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.LAYER_UPDATE);
        e.setAssociatedObject(mCurProject.getStack().getSelectedLayer());
        dispatchQVEvent(e);
    }

    /**
     *   Translates the selected layer by x and y meters
     *
     */
    public void translateSelectedLayer(double x, double y) {
        mCurProject.getStack().getSelectedLayer().translateLayer(x, y);
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.LAYER_UPDATE);
        e.setAssociatedObject(mCurProject.getStack().getSelectedLayer());
        dispatchQVEvent(e);
    }

    /**
     *   returns the stack from the current project
     *
     */
    public VolStack getStack() {
        if (mCurProject == null) return null; else return mCurProject.getStack();
    }

    /**
     *    exits the quick vol application
     */
    public void exit() {
        System.out.println("QuickvolControler is shutting down...");
        mQVListeners = null;
        mCurProject = null;
        mLastProject = null;
        mModelManager = null;
        System.gc();
    }

    /**  
     *     sets the selected layer to the next 
     *     layer below the layer currently selected, if one exists
     *
     */
    public void selectLayerBelow() {
        if (mCurProject.getStack() != null && mCurProject.getStack().getSelectedLayerIdx() > 0) {
            setSelectedLayer(mCurProject.getStack().getSelectedLayerIdx() - 1);
        }
    }

    /**  
     *     sets the selected layer to the next 
     *     layer above the layer currently selected, if one exists
     *
     */
    public void selectLayerAbove() {
        if (mCurProject.getStack() != null && mCurProject.getStack().getLayers().length > mCurProject.getStack().getSelectedLayerIdx() + 1) {
            setSelectedLayer(mCurProject.getStack().getSelectedLayerIdx() + 1);
        }
    }

    /**  
     *     sets the selected layer to the index passed in
     *
     */
    public void setSelectedLayer(int a) {
        mCurProject.getStack().setSelectedLayer(a);
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.LAYER_SELECTION_CHANGE));
    }

    /**  
     *     sets the selected feature to the index passed in
     *
     */
    public void setSelectedFeature(int a) {
        mCurProject.getStack().setSelectedFeature(a);
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.FEATURE_SELECTION_CHANGE));
    }

    /**
     *  returns the currently selected project
     */
    public Project getProject() {
        return mCurProject;
    }

    public void setSelectedLayerZ(double newz) {
        mCurProject.getStack().getSelectedLayer().setZ(newz);
        mCurProject.getStack().sortLayers();
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.LAYER_UPDATE));
    }

    public void setSelectedLayerThk(double thk) {
        mCurProject.getStack().getSelectedLayer().setThk(thk);
        mCurProject.getStack().sortLayers();
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.LAYER_UPDATE));
    }

    /**
     *  sets the color of the selected feature
     *
     */
    public void setSelectedFeatureColor(Color c) {
        mCurProject.getStack().getSelectedFeature().setPrefRenderColor(c);
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.FEATURE_UPDATE);
        e.setAssociatedObject(mCurProject.getStack().getSelectedFeature());
        dispatchQVEvent(e);
    }

    /**
     *  sets the pixel size (length on a pixel side) of the
     *  currently selected layer
     */
    public void setSelectedLayerPixelSize(double newsz) {
        mCurProject.getStack().getSelectedLayer().setPixelSizeInMeters(newsz);
        dispatchQVEvent(new QuickVolEvent(QuickVolEvent.LAYER_UPDATE));
    }

    public void updateSelectedModelDisplay() {
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.MODEL_UPDATE);
        e.setAssociatedObject(getSelectedModel());
        dispatchQVEvent(e);
    }

    public void toggleSelectedModelDisplay() {
        QuickVolEvent e = new QuickVolEvent(QuickVolEvent.TOGGLE_MODEL_DISPLAY);
        e.setAssociatedObject(getSelectedModel());
        dispatchQVEvent(e);
    }

    /**
     *   calculates the volume of the feature
     *   using the model that has been constructed
     *   NOTE this will include any smoothing
     */
    public double calcModelVolume(Model m) {
        return m.calcVolume();
    }

    /**
     *  Returns the currently selected Feature
     *
     *
     */
    public Feature getSelectedFeature() {
        return mCurProject.getStack().getSelectedFeature();
    }

    /**
     *  Gets the current QuickVolPreferences
     *
     */
    public QuickVolPreferences getQuickVolPreferences() {
        return new QuickVolPreferences();
    }

    public void handleException(Exception e) {
        System.out.println("\n\nCONTROLER HANDLING EXCEPTION");
        e.printStackTrace();
    }
}
