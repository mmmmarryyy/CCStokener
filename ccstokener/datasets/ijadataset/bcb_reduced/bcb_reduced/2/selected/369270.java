package org.qtitools.constructr.itembank;

import java.io.IOException;
import java.net.URL;
import org.qtitools.util.ContentPackage;

public class SimpleItem implements Item {

    private static final long serialVersionUID = 1L;

    protected URL cp_url;

    protected String name;

    protected String description;

    protected SimpleItem() {
    }

    public SimpleItem(URL cp_path, String name, String description) {
        this.cp_url = cp_path;
        this.name = name;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public ContentPackage resolveItem() {
        try {
            return new ContentPackage(cp_url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "item(name=\"" + name + "\")";
    }
}
