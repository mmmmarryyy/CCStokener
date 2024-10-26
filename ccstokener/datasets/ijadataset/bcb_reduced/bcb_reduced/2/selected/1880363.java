package org.npsnet.v.gui;

import com.sun.j3d.loaders.*;
import com.sun.j3d.utils.behaviors.vp.*;
import com.sun.j3d.utils.universe.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import org.npsnet.v.services.gui.ContentPanel;
import org.npsnet.v.services.gui.ContentPanelContext;
import org.npsnet.v.services.gui.ContentPanelHost;
import org.npsnet.v.services.gui.UnsupportedContextException;
import org.web3d.j3d.loaders.*;

/**
 * The content panel class for model content.
 *
 * @author Andrzej Kapolka
 */
public class ModelContentPanel extends ContentPanel {

    /**
     * The owner of this panel.
     */
    private StandardContentPanelProvider owner;

    /**
     * The simple universe object.
     */
    private SimpleUniverse simpleUniverse;

    /**
     * The root of the scene graph.
     */
    private BranchGroup scene;

    /**
     * The model graph, or <code>null</code> if not initialized.
     */
    private BranchGroup model;

    /**
     * The model loader thread.
     */
    private Thread loaderThread;

    /**
     * A transform object to reuse.
     */
    private Transform3D transform;

    /**
     * A private class for loading VRML/X3D content.
     */
    private class Web3DLoaderThread extends Thread {

        /**
         * The new context to load.
         */
        private ContentPanelContext newContext;

        /**
         * Constructor.
         *
         * @param pNewContext the new context to load
         */
        public Web3DLoaderThread(ContentPanelContext pNewContext) {
            newContext = pNewContext;
        }

        /**
         * Run method.
         */
        public void run() {
            try {
                Web3DLoader web3DLoader = new Web3DLoader(Loader.LOAD_ALL & ~(Loader.LOAD_BEHAVIOR_NODES));
                web3DLoader.setBaseUrl(newContext.getURL());
                if (model != null) model.detach();
                Scene web3DScene = web3DLoader.load(newContext.getURL());
                model = web3DScene.getSceneGroup();
                model.setCapability(BranchGroup.ALLOW_DETACH);
                TransformGroup[] viewGroups = web3DScene.getViewGroups();
                if (newContext instanceof ModelContentPanelContext) {
                    ((ModelContentPanelContext) newContext).getCameraTransform(transform);
                } else if (viewGroups == null || viewGroups.length == 0) {
                    BoundingSphere bs = new BoundingSphere(new Point3d(), 5.0);
                    if (model.getBounds() != null) {
                        bs.combine(model.getBounds());
                    }
                    transform.set(new Vector3d(0, 0, bs.getRadius() * 2.0));
                } else {
                    viewGroups[0].getTransform(transform);
                }
                simpleUniverse.getViewingPlatform().getViewPlatformTransform().setTransform(transform);
                float fieldOfView;
                float[] horizontalFieldsOfView = web3DScene.getHorizontalFOVs();
                if (newContext instanceof ModelContentPanelContext) {
                    fieldOfView = (float) ((ModelContentPanelContext) newContext).getFieldOfView();
                } else if (horizontalFieldsOfView == null || horizontalFieldsOfView.length == 0) {
                    fieldOfView = (float) Math.toRadians(45.0);
                } else {
                    fieldOfView = horizontalFieldsOfView[0];
                }
                simpleUniverse.getViewer().getView().setFieldOfView(fieldOfView);
                scene.addChild(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
            loaderThread = null;
            if (getHost() != null) {
                getHost().contentPanelStoppedLoading(ModelContentPanel.this);
            }
        }
    }

    /**
     * Represents a context of this content panel.
     */
    public class ModelContentPanelContext extends ContentPanelContext {

        /**
         * The camera transform.
         */
        private Transform3D cameraTransform;

        /**
         * The field of view.
         */
        private double fieldOfView;

        /**
         * Constructor.
         *
         * @param pSource the originating content panel
         * @param pURL the URL
         * @param pCameraTransform the camera transform
         * @param pFieldOfView the field of view
         */
        public ModelContentPanelContext(ContentPanel pSource, URL pURL, Transform3D pCameraTransform, double pFieldOfView) {
            super(pSource, pURL);
            cameraTransform = new Transform3D(pCameraTransform);
            fieldOfView = pFieldOfView;
        }

        /**
         * Sets the specified transform to the value of the camera transform.
         *
         * @param transform the transform to set
         */
        public void getCameraTransform(Transform3D transform) {
            transform.set(cameraTransform);
        }

        /**
         * Returns the field of view.
         *
         * @return the field of view
         */
        public double getFieldOfView() {
            return fieldOfView;
        }
    }

    /**
     * Static initializer.
     */
    static {
        String packages = System.getProperty("uri.protocol.handler.pkgs");
        if (packages == null) {
            System.setProperty("uri.protocol.handler.pkgs", "vlc.net.protocol|org.npsnet.v.kernel");
        } else if (packages.indexOf("org.npsnet.v.kernel") == -1) {
            System.setProperty("uri.protocol.handler.pkgs", System.getProperty("uri.protocol.handler.pkgs") + "|org.npsnet.v.kernel");
        }
    }

    /**
     * Constructor.
     *
     * @param pOwner the standard content panel provider that spawned
     * this panel
     */
    public ModelContentPanel(StandardContentPanelProvider pOwner) {
        super(new BorderLayout());
        owner = pOwner;
        transform = new Transform3D();
        GraphicsConfigTemplate3D gct = new GraphicsConfigTemplate3D();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration cfg = ge.getDefaultScreenDevice().getBestConfiguration(gct);
        Canvas3D canvas = new Canvas3D(cfg);
        simpleUniverse = new SimpleUniverse(canvas);
        scene = new BranchGroup();
        scene.setCapability(Group.ALLOW_CHILDREN_WRITE);
        scene.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        OrbitBehavior ob = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ALL);
        ob.setSchedulingBounds(new BoundingSphere(new Point3d(), Double.POSITIVE_INFINITY));
        simpleUniverse.getViewingPlatform().setViewPlatformBehavior(ob);
        PlatformGeometry pg = new PlatformGeometry();
        DirectionalLight dl = new DirectionalLight();
        dl.setInfluencingBounds(new BoundingSphere(new Point3d(), Double.POSITIVE_INFINITY));
        pg.addChild(dl);
        simpleUniverse.getViewingPlatform().setPlatformGeometry(pg);
        simpleUniverse.getLocale().addBranchGraph(scene);
        simpleUniverse.getViewer().getView().setFrontClipDistance(1.0);
        simpleUniverse.getViewer().getView().setBackClipDistance(100.0);
        add(canvas, BorderLayout.CENTER);
    }

    /**
     * Sets the context of this content panel, notifying all listeners that
     * the context has changed.
     *
     * @param newContext the new context
     * @exception UnsupportedContextException if the type of the
     * specified context is unsupported
     */
    public void setContext(ContentPanelContext newContext) throws UnsupportedContextException {
        try {
            URLConnection urlc = newContext.getURL().openConnection();
            String type = urlc.getContentType();
            if (type == null || !type.startsWith("model")) {
                throw new UnsupportedContextException();
            }
            if (type.equals("model/vrml")) {
                loaderThread = new Web3DLoaderThread(newContext);
                if (getHost() != null) {
                    getHost().contentPanelStartedLoading(this);
                }
                loaderThread.start();
            } else {
                throw new UnsupportedContextException();
            }
        } catch (IOException ioe) {
            throw new UnsupportedContextException();
        }
        super.setContext(newContext);
    }

    /**
     * Returns this content panel's current context.
     *
     * @return the current context
     */
    public ContentPanelContext getContext() {
        simpleUniverse.getViewingPlatform().getViewPlatformTransform().getTransform(transform);
        return new ModelContentPanelContext(this, super.getContext().getURL(), transform, simpleUniverse.getViewer().getView().getFieldOfView());
    }

    /**
     * Checks whether this content panel is loading content in another
     * thread.  Default implementation simply returns <code>false</code>.
     *
     * @return <code>true</code> if this content panel is loading content
     * in another thread, <code>false</code> otherwise
     */
    public boolean isLoading() {
        return (loaderThread != null);
    }
}
