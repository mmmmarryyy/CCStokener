package org.specrunner.source.resource.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import org.specrunner.source.ISource;
import org.specrunner.source.SourceException;
import org.specrunner.source.resource.EPlace;
import org.specrunner.source.resource.EType;
import org.specrunner.source.resource.Position;
import org.specrunner.source.resource.ResourceException;

/**
 * Partial implementation of header resources.
 * 
 * @author Thiago Santos
 * 
 */
public abstract class AbstractPlacedResource extends AbstractResourceHeader {

    private static int serialNumber = 0;

    public AbstractPlacedResource(ISource parent, String path, EType ref, Position position) {
        super(parent, path, ref, position);
    }

    @Override
    public ISource writeTo(ISource output) throws ResourceException {
        try {
            Document doc = getParent().getDocument();
            Nodes places = doc.query(getPosition().getXpath());
            if (places.size() == 0) {
                places = new Nodes();
                places.append(doc.getChild(0).getChild(0));
            }
            if (places.size() > 0 && places.get(0) instanceof Element) {
                Element target = (Element) places.get(0);
                if (getType() == EType.TEXT) {
                    Element tag = getHeaderTag();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    writeAllTo(getURLs(), out);
                    String content = out.toString();
                    out.close();
                    tag.appendChild(content);
                    if (getPosition().getPlace() == EPlace.BEFORE) {
                        target.insertChild(tag, 0);
                    } else {
                        target.appendChild(tag);
                    }
                } else {
                    List<URL> files = getURLs();
                    for (URL url : files) {
                        String file = url.toString();
                        String name = file.substring(file.lastIndexOf("/") + 1) + "_res_" + serialNumber++;
                        Element tag = getHeaderTag(output, name);
                        File resFile = getFile(output, name);
                        if (!resFile.getParentFile().exists()) {
                            if (!resFile.getParentFile().mkdirs()) {
                                throw new ResourceException("Could not create resource directory '" + resFile.getParent() + "'.");
                            }
                        }
                        InputStream in = url.openStream();
                        OutputStream out = new FileOutputStream(resFile);
                        writeTo(in, out);
                        out.close();
                        in.close();
                        if (getPosition().getPlace() == EPlace.BEFORE) {
                            target.insertChild(tag, 0);
                        } else {
                            target.appendChild(tag);
                        }
                    }
                }
            } else {
                throw new ResourceException("Head element not found.");
            }
        } catch (IOException e) {
            throw new ResourceException(e);
        } catch (SourceException e) {
            throw new ResourceException(e);
        }
        return output;
    }

    protected abstract Element getHeaderTag();

    protected abstract Element getHeaderTag(ISource output, String name);

    protected abstract File getFile(ISource output, String name);

    @Override
    public String asString() {
        return null;
    }

    @Override
    public Node asNode() {
        return null;
    }
}
