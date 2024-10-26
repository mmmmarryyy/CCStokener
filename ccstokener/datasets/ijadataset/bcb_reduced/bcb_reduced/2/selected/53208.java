package org.xith3d.loaders.models.util.specific;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.xith3d.loaders.models.Model;
import org.xith3d.loaders.models.ModelLoader;
import org.xith3d.loaders.models.animations.ModelAnimation;
import org.xith3d.loaders.models.animations.PrecomputedAnimationKeyFrame;
import org.xith3d.loaders.models.animations.PrecomputedAnimationKeyFrameController;
import org.xith3d.scenegraph.Geometry;
import org.xith3d.scenegraph.Shape3D;

/**
 * Utility methods for OBJ models.
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class OBJTools {

    private static Model loadOBJFrames(ModelLoader loader, final String baseURL, ArrayList<Geometry[]> frames) {
        Model baseModel = null;
        int frameCount = -1;
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String num = Integer.toString(i);
            final float length = num.length();
            for (int j = 0; j < 6 - length; j++) {
                num = "0" + num;
            }
            final String frameURL = baseURL + "_" + num + ".obj";
            try {
                Model frameModel = loader.loadModel(new URL(frameURL));
                if (i == 1) {
                    baseModel = frameModel;
                    if (baseModel.getShapesCount() == 0) {
                        System.err.println("Incorrectly loaded file : " + frameURL);
                    }
                }
                if (frameModel.getShapesCount() != baseModel.getShapesCount()) {
                    throw new Error("Incorrectly loaded file : " + frameURL);
                }
                Geometry[] geoms = new Geometry[frameModel.getShapesCount()];
                for (int j = 0; j < frameModel.getShapesCount(); j++) {
                    geoms[j] = frameModel.getShape(j).getGeometry();
                }
                frames.add(geoms);
            } catch (final FileNotFoundException e) {
                if (frameCount == -1) {
                    e.printStackTrace();
                }
                return (baseModel);
            } catch (final IOException e) {
                if (frameCount == -1) {
                    e.printStackTrace();
                }
                return (baseModel);
            }
            frameCount++;
        }
        return (baseModel);
    }

    public static Model loadPrecomputedModel(URL url) {
        ArrayList<Geometry[]> frames = new ArrayList<Geometry[]>();
        if (url.toExternalForm().endsWith(".amo")) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String objFileName = reader.readLine();
                objFileName = url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf("/")) + "/" + objFileName;
                Model baseModel = loadOBJFrames(ModelLoader.getInstance(), objFileName, frames);
                ArrayList<ModelAnimation> anims = new ArrayList<ModelAnimation>();
                String line;
                while ((line = reader.readLine()) != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    String animName = tokenizer.nextToken();
                    int from = Integer.valueOf(tokenizer.nextToken());
                    int to = Integer.valueOf(tokenizer.nextToken());
                    tokenizer.nextToken();
                    int numFrames = to - from + 1;
                    PrecomputedAnimationKeyFrameController[] controllers = new PrecomputedAnimationKeyFrameController[baseModel.getShapesCount()];
                    for (int i = 0; i < baseModel.getShapesCount(); i++) {
                        Shape3D shape = baseModel.getShape(i);
                        PrecomputedAnimationKeyFrame[] keyFrames = new PrecomputedAnimationKeyFrame[numFrames];
                        int k = 0;
                        for (int j = from; j <= to; j++) {
                            keyFrames[k++] = new PrecomputedAnimationKeyFrame(frames.get(j)[i]);
                        }
                        controllers[i] = new PrecomputedAnimationKeyFrameController(keyFrames, shape);
                    }
                    anims.add(new ModelAnimation(animName, numFrames, 25f, controllers));
                }
                baseModel.setAnimations(anims.toArray(new ModelAnimation[anims.size()]));
                return (baseModel);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return (null);
            } catch (IOException e) {
                e.printStackTrace();
                return (null);
            }
        }
        {
            Model baseModel = loadOBJFrames(ModelLoader.getInstance(), url.toExternalForm(), frames);
            PrecomputedAnimationKeyFrameController[] controllers = new PrecomputedAnimationKeyFrameController[baseModel.getShapesCount()];
            for (int i = 0; i < baseModel.getShapesCount(); i++) {
                Shape3D shape = baseModel.getShape(i);
                PrecomputedAnimationKeyFrame[] keyFrames = new PrecomputedAnimationKeyFrame[frames.size()];
                for (int j = 0; j < frames.size(); j++) {
                    keyFrames[j] = new PrecomputedAnimationKeyFrame(frames.get(j)[i]);
                }
                controllers[i] = new PrecomputedAnimationKeyFrameController(keyFrames, shape);
            }
            ModelAnimation[] anims = new ModelAnimation[] { new ModelAnimation("default", frames.size(), 25f, controllers) };
            baseModel.setAnimations(anims);
            return (baseModel);
        }
    }

    public static Model loadPrecomputedModel(File file) {
        try {
            return (loadPrecomputedModel(file.toURI().toURL()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return (null);
        }
    }

    public static Model loadPrecomputedModel(String filename) {
        return (loadPrecomputedModel(new File(filename)));
    }
}
