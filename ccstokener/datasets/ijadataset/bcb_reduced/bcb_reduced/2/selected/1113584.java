package org.softmed.jops.fileloading;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.softmed.jops.Generator;
import org.softmed.jops.GeneratorBehaviour;
import org.softmed.jops.ParticleBehaviour;
import org.softmed.jops.ParticleRender;
import org.softmed.jops.ParticleSystem;
import org.softmed.jops.PositionAnimator;
import org.softmed.jops.SpaceAnimator;
import org.softmed.jops.fileloading.converters.AngleRadiansConverter;
import org.softmed.jops.modifiers.Modifier;
import org.softmed.jops.modifiers.PointMass;
import org.softmed.jops.space.GeneratorSpace;
import org.softmed.jops.valuelists.SimpleGenericValueList;
import org.softmed.jops.valuelists.ValueListf;
import org.softmed.jops.valuelists.ValueListp3f;
import org.softmed.jops.values.GenericValue;

public class JIBXLoader implements FileLoaderImplementation {

    IBindingFactory bfact;

    public JIBXLoader() {
        setup();
    }

    protected void setup() {
        try {
            bfact = BindingDirectory.getFactory(ParticleSystem.class);
        } catch (JiBXException e) {
            e.printStackTrace();
        }
    }

    public String getName(InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry entry = null;
        entry = zipInputStream.getNextEntry();
        if (entry == null || entry.isDirectory()) throw new FileNotFoundException("No files found inside the Zip Stream or zip stream is null");
        String zeName = entry.getName();
        return zeName;
    }

    public ParticleSystem load(InputStream inputStream) throws IOException, DataFormatException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        byte[] byteArray = readFully(bis);
        File test = new File("temp.ops");
        FileOutputStream fos = new FileOutputStream(test);
        fos.write(byteArray);
        fos.flush();
        fos.close();
        ParticleSystem ps = load(new File("temp.ops"));
        test.delete();
        return ps;
    }

    public String getName(URL url) {
        System.out.println("Trying to load : " + url);
        String fileName = url.getFile();
        return getName(fileName);
    }

    public static void send(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[2048];
        int c;
        while ((c = in.read(buffer)) > 0) out.write(buffer, 0, c);
    }

    public static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        send(in, buffer);
        return buffer.toByteArray();
    }

    public ParticleSystem load(URL url) throws IOException, DataFormatException {
        InputStream stream = url.openStream();
        ParticleSystem ps = load(stream);
        stream.close();
        return ps;
    }

    public String getName(String filePath) {
        String simplename = null;
        if (filePath == null) simplename = filePath;
        int lastIndex = filePath.lastIndexOf('\\');
        if (lastIndex > 1) simplename = filePath.substring(lastIndex + 1); else simplename = filePath;
        return simplename;
    }

    public ParticleSystem load(File file) throws IOException, DataFormatException {
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipEntry entry = null;
        entry = entries.nextElement();
        if (entry == null || entry.isDirectory()) throw new FileNotFoundException("No JOPS file inside the Zip File");
        InputStream input = zipFile.getInputStream(entry);
        try {
            IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
            ParticleSystem obj = (ParticleSystem) uctx.unmarshalDocument(input, null);
            ParticleSystem ps = obj;
            setObjectReferences(ps);
            input.close();
            zipFile.close();
            return obj;
        } catch (JiBXException e) {
            e.printStackTrace();
            throw new DataFormatException(e);
        }
    }

    private void convertAnglesToRadians(ParticleSystem ps) {
        List<PositionAnimator> panims = ps.getPanimators();
        for (PositionAnimator PAnimator : panims) {
            convertAnglesToRadians(PAnimator.getBiasAngle());
            convertAnglesToRadians(PAnimator.getBiasSpin());
        }
        List<SpaceAnimator> anims = ps.getAnimators();
        for (SpaceAnimator spaceAnimator : anims) {
            convertAnglesToRadians(spaceAnimator.getSpaceAngle());
            convertAnglesToRadians(spaceAnimator.getSpaceSpin());
        }
        List<GeneratorBehaviour> gbs = ps.getGenBehaviours();
        for (GeneratorBehaviour generatorBehaviour : gbs) {
            convertAnglesToRadians(generatorBehaviour.getAngle());
            convertAnglesToRadians(generatorBehaviour.getAngleH());
            convertAnglesToRadians(generatorBehaviour.getAngleV());
            convertAnglesToRadians(generatorBehaviour.getAngleSpreadH());
            convertAnglesToRadians(generatorBehaviour.getAngleSpreadV());
            convertAnglesToRadians(generatorBehaviour.getParticleAngleH());
            convertAnglesToRadians(generatorBehaviour.getParticleAngleV());
            convertAnglesToRadians(generatorBehaviour.getParticleSpinH());
            convertAnglesToRadians(generatorBehaviour.getParticleSpinV());
            convertAnglesToRadians(generatorBehaviour.getSpin());
            convertAnglesToRadians(generatorBehaviour.getSpinH());
            convertAnglesToRadians(generatorBehaviour.getSpinV());
            convertAlpha(generatorBehaviour.getAlpha());
        }
        List<ParticleBehaviour> pbs = ps.getBehaviours();
        for (ParticleBehaviour generatorBehaviour : pbs) {
            convertAnglesToRadians(generatorBehaviour.getAngle());
            convertAnglesToRadians(generatorBehaviour.getParticleAngleH());
            convertAnglesToRadians(generatorBehaviour.getParticleAngleV());
            convertAnglesToRadians(generatorBehaviour.getParticleSpinH());
            convertAnglesToRadians(generatorBehaviour.getParticleSpinV());
            convertAnglesToRadians(generatorBehaviour.getSpin());
            convertAlpha(generatorBehaviour.getAlpha());
        }
    }

    private void convertAlpha(ValueListf vlf) {
        convertAlpha(vlf.getMainValueList());
        convertAlpha(vlf.getRandomValueList());
    }

    private void convertAnglesToRadians(ValueListf vlf) {
        convertAnglesToRadians(vlf.getMainValueList());
        convertAnglesToRadians(vlf.getRandomValueList());
    }

    private void convertAnglesToRadians(ValueListp3f vlf) {
        convertAnglesToRadians(vlf.getMainValueList());
        convertAnglesToRadians(vlf.getRandomValueList());
    }

    private void convertAnglesToRadians(SimpleGenericValueList svlf) {
        AngleRadiansConverter converter = new AngleRadiansConverter();
        List<GenericValue> values = svlf.getValues();
        for (GenericValue genericValue : values) {
            genericValue.setValue(converter.fromExternalToInternal(genericValue.getValue()));
        }
    }

    private void convertAlpha(SimpleGenericValueList svlf) {
        List<GenericValue> values = svlf.getValues();
        for (GenericValue genericValue : values) {
            genericValue.setValue(((Float) genericValue.getValue()) / 255f);
        }
    }

    public void save(File file, ParticleSystem ps) throws IOException, DataFormatException {
        prepareParticleSystemForSaving(ps);
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos, 2048);
        ZipOutputStream out = new ZipOutputStream(bos);
        String filename = file.getName().substring(0, file.getName().indexOf('.'));
        ZipEntry entry = new ZipEntry(filename + ".xml");
        out.putNextEntry(entry);
        out.setMethod(ZipOutputStream.DEFLATED);
        IMarshallingContext mctx;
        try {
            mctx = bfact.createMarshallingContext();
            mctx.setIndent(4);
            mctx.marshalDocument(ps, "UTF-8", null, out);
            cleanParticleSystemAfterSaving(ps);
        } catch (JiBXException e) {
            e.printStackTrace();
            throw new DataFormatException(e);
        }
    }

    protected void cleanParticleSystemAfterSaving(ParticleSystem ps) {
        List<Generator> gens = ps.getGenerators();
        for (Generator generator : gens) {
            generator.setModifierIndexes(null);
        }
    }

    protected void prepareParticleSystemForSaving(ParticleSystem ps) {
        List<Modifier> modifiers = ps.getModifiers();
        PointMass mass = null;
        int index = -1;
        for (Modifier modifier : modifiers) {
            if (modifier instanceof PointMass) {
                mass = (PointMass) modifier;
                if (mass.getPositionAnimator() != null) {
                    index = ps.getPanimators().indexOf(mass.getPositionAnimator());
                    mass.setPositionAnimatorIndex(index);
                } else mass.setPositionAnimatorIndex(-1);
            }
        }
        List<Generator> gens = ps.getGenerators();
        PositionAnimator panimator = null;
        ParticleBehaviour behaviour = null;
        GeneratorBehaviour genBehhaviour = null;
        SpaceAnimator animator = null;
        ParticleRender render = null;
        GeneratorSpace space = null;
        index = -1;
        for (Generator generator : gens) {
            behaviour = generator.getPb();
            if (behaviour != null) {
                index = ps.getBehaviours().indexOf(behaviour);
                generator.setParticleBehaviourIndex(index);
            } else generator.setParticleBehaviourIndex(-1);
            genBehhaviour = generator.getGb();
            if (genBehhaviour != null) {
                index = ps.getGenBehaviours().indexOf(genBehhaviour);
                generator.setGeneratorBehaviourIndex(index);
            } else generator.setGeneratorBehaviourIndex(-1);
            render = generator.getRender();
            if (render != null) {
                index = ps.getRenders().indexOf(render);
                generator.setRenderIndex(index);
            } else generator.setRenderIndex(-1);
            space = generator.getSpace();
            if (space != null) {
                index = ps.getSpaces().indexOf(space);
                generator.setSpaceIndex(index);
            } else generator.setSpaceIndex(-1);
            animator = generator.getAnimator();
            if (animator != null) {
                index = ps.getAnimators().indexOf(animator);
                generator.setSpaceAnimatorIndex(index);
            } else generator.setSpaceAnimatorIndex(-1);
            panimator = generator.getPositionAnimator();
            if (panimator != null) {
                index = ps.getPanimators().indexOf(panimator);
                generator.setPositionAnimatorIndex(index);
            } else generator.setPositionAnimatorIndex(-1);
            modifiers = generator.getModifiers();
            if (modifiers.size() > 0) {
                List<Integer> indexes = new ArrayList<Integer>();
                generator.setModifierIndexes(indexes);
                for (Modifier mod : modifiers) {
                    index = ps.getModifiers().indexOf(mod);
                    indexes.add(index);
                }
            } else generator.setModifierIndexes(null);
        }
    }

    protected void setObjectReferences(ParticleSystem ps) {
        List<Modifier> modifiers = ps.getModifiers();
        PointMass mass = null;
        PositionAnimator panimator = null;
        int index = -1;
        for (Modifier modifier : modifiers) {
            if (modifier instanceof PointMass) {
                mass = (PointMass) modifier;
                index = mass.getPositionAnimatorIndex();
                if (index > -1) {
                    panimator = ps.getPanimators().get(index);
                    mass.setPositionAnimator(panimator);
                }
            }
        }
        List<Generator> gens = ps.getGenerators();
        panimator = null;
        ParticleBehaviour behaviour = null;
        GeneratorBehaviour genBehhaviour = null;
        SpaceAnimator animator = null;
        ParticleRender render = null;
        Modifier modifier = null;
        GeneratorSpace space = null;
        index = -1;
        for (Generator generator : gens) {
            index = generator.getParticleBehaviourIndex();
            if (index > -1) {
                behaviour = ps.getBehaviours().get(index);
                generator.setPb(behaviour);
            } else generator.setPb(null);
            index = generator.getGeneratorBehaviourIndex();
            if (index > -1) {
                genBehhaviour = ps.getGenBehaviours().get(index);
                generator.setGb(genBehhaviour);
            } else generator.setGb(null);
            index = generator.getRenderIndex();
            if (index > -1) {
                render = ps.getRenders().get(index);
                generator.setRender(render);
            } else generator.setRender(null);
            index = generator.getSpaceIndex();
            if (index > -1) {
                space = ps.getSpaces().get(index);
                generator.setSpace(space);
            } else generator.setSpace(null);
            index = generator.getSpaceAnimatorIndex();
            if (index > -1) {
                animator = ps.getAnimators().get(index);
                generator.setAnimator(animator);
            } else generator.setAnimator(null);
            index = generator.getPositionAnimatorIndex();
            System.out.println("PositionAnimator Index ->" + index);
            if (index > -1) {
                panimator = ps.getPanimators().get(index);
                System.out.println("Setting a PositionAnimator ->" + panimator);
                generator.setPositionAnimator(panimator);
            } else generator.setPositionAnimator(null);
            List<Integer> indexes = generator.getModifierIndexes();
            if (indexes != null) {
                for (Integer integer : indexes) {
                    modifier = ps.getModifiers().get(integer);
                    generator.getModifiers().add(modifier);
                }
                generator.setModifierIndexes(null);
            }
        }
    }
}
