package com.byterefinery.rmbench.export.model.html;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.draw2d.FreeformFigure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.ScaledGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import com.byterefinery.rmbench.export.ExportPlugin;
import com.byterefinery.rmbench.external.IModelExporter;
import com.byterefinery.rmbench.external.IExportable.ModelExport;
import com.byterefinery.rmbench.external.export.DiagramRenderer;
import com.byterefinery.rmbench.external.model.IColumn;
import com.byterefinery.rmbench.external.model.IDiagram;
import com.byterefinery.rmbench.external.model.ISchema;
import com.byterefinery.rmbench.external.model.ITable;

/**
 * a model exporter that will create a HTML site showing a static view of the model
 * 
 * @author cse
 */
public class HTMLModelExporter implements IModelExporter {

    public static final class FactoryImpl implements IModelExporter.Factory {

        public IModelExporter getExporter(ModelExport modelExport) {
            return new HTMLModelExporter(modelExport);
        }
    }

    private final String templateDirectory = "HTMLExportTemplate";

    private final ModelExport modelExport;

    public static final int TWO_FRAME_TEMPLATE = 2;

    public static final int THREE_FRAME_TEMPLATE = 3;

    /** List for indexing the tables of the model */
    private List<ITable> tablesList = new ArrayList<ITable>();

    private Dimension imageSize;

    /** number of frames to use, which means 2 frame or 3 frame template usage*/
    private int templateType;

    public HTMLModelExporter(ModelExport modelExport) {
        this.modelExport = modelExport;
        imageSize = new Dimension(800, 600);
        templateType = TWO_FRAME_TEMPLATE;
    }

    public void setTemplateType(int templateType) {
        this.templateType = templateType;
    }

    public void export(File directory) throws IOException {
        try {
            Velocity.init();
            createFileTree(directory);
            saveIndexHtml(directory);
            saveTableFiles(directory);
        } catch (Exception e) {
            ExportPlugin.logError(e);
        }
    }

    public void saveIndexHtml(File targetDirectory) throws Exception {
        URL url;
        if (templateType == THREE_FRAME_TEMPLATE) {
            url = ExportPlugin.getDefault().getBundle().getEntry(templateDirectory + File.separator + "templates" + File.separator + "index.html");
        } else {
            url = ExportPlugin.getDefault().getBundle().getEntry(templateDirectory + File.separator + "templates" + File.separator + "index2.html");
        }
        VelocityContext context = new VelocityContext();
        context.put("modelname", modelExport.getModel().getName());
        context.put("schemas", getSchemaList());
        context.put("diagrams", getDiagramList(targetDirectory));
        File targetFile = new File(targetDirectory, "index.html");
        if (!targetFile.exists()) targetFile.createNewFile();
        FileWriter writer = new FileWriter(targetFile, false);
        InputStreamReader reader = new InputStreamReader(url.openStream());
        Velocity.evaluate(context, writer, "RMBench HTML EXport", reader);
        writer.close();
        reader.close();
    }

    private List<Hashtable<String, Object>> getDiagramList(File targetDirectory) throws Exception {
        IDiagram[] diagrams = modelExport.getModel().getDiagrams();
        ArrayList<Hashtable<String, Object>> diagramList = new ArrayList<Hashtable<String, Object>>(diagrams.length);
        int diagramId = 1;
        for (int i = 0; i < diagrams.length; i++) {
            IDiagram diagram = diagrams[i];
            Hashtable<String, Object> diagramHash = new Hashtable<String, Object>();
            ITable[] tables = diagram.getTables();
            ArrayList<Hashtable<String, Object>> tableList = new ArrayList<Hashtable<String, Object>>(tables.length);
            for (int j = 0; j < tables.length; j++) {
                ITable table = tables[j];
                Hashtable<String, Object> tableHash = new Hashtable<String, Object>();
                tableHash.put("id", new Integer(tablesList.indexOf(table) + 1));
                tableHash.put("name", table.getName());
                tableList.add(tableHash);
            }
            diagramHash.put("name", diagram.getName());
            diagramHash.put("tables", tableList);
            diagramHash.put("id", new Integer(diagramId));
            createDiagramImage(diagramId, diagram, targetDirectory);
            diagramList.add(diagramHash);
            diagramId++;
        }
        return diagramList;
    }

    /**
     * Creates an image of the given diagram and saves it in the image directory of
     * the target directory
     * 
     * @param diagramId index of diagram, the name of the file will be diagram_<i>id</i>
     * @param diagram the diagram to export
     */
    private void createDiagramImage(int diagramId, IDiagram diagram, File targetDirectory) throws Exception {
        IFigure figure = DiagramRenderer.render(diagram);
        Rectangle bounds;
        if (figure instanceof FreeformFigure) {
            bounds = ((FreeformFigure) figure).getFreeformExtent();
        } else {
            bounds = figure.getBounds();
        }
        Image image = new Image(Display.getCurrent(), imageSize.width, imageSize.height);
        double scaleX = 1;
        double scaleY = 1;
        if (bounds.width > imageSize.width) scaleX = (double) imageSize.width / bounds.width;
        if (bounds.height > imageSize.height) scaleY = (double) imageSize.height / bounds.height;
        IFigure layers = (IFigure) ((IFigure) ((IFigure) figure.getChildren().get(0)).getChildren().get(0)).getChildren().get(1);
        Graphics graphics;
        if ((scaleX == 1) && (scaleY == 1)) graphics = new SWTGraphics(new GC(image)); else {
            graphics = new ScaledGraphics(new SWTGraphics(new GC(image)));
            if (scaleX < scaleY) ((ScaledGraphics) graphics).scale(scaleX); else ((ScaledGraphics) graphics).scale(scaleY);
        }
        IFigure tableLayer = (IFigure) ((IFigure) layers.getChildren().get(0)).getChildren().get(0);
        IFigure connectionLayer = (IFigure) layers.getChildren().get(1);
        int minX = 0;
        int minY = 0;
        for (Iterator<?> it = tableLayer.getChildren().iterator(); it.hasNext(); ) {
            bounds = ((IFigure) it.next()).getBounds();
            if (bounds.y < minY) minY = bounds.y;
            if (bounds.x < minX) minX = bounds.x;
        }
        tableLayer.setLocation(new Point(minX, minY));
        connectionLayer.setLocation(new Point(minX, minY));
        graphics.translate(-minX, -minY);
        tableLayer.paint(graphics);
        connectionLayer.paint(graphics);
        File targetFile = new File(targetDirectory.getAbsolutePath() + File.separator + "images" + File.separator + "diagram_" + diagramId + ".jpg");
        FileOutputStream writer = new FileOutputStream(targetFile, false);
        try {
            ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[1];
            loader.data[0] = image.getImageData();
            loader.save(writer, SWT.IMAGE_JPEG);
        } catch (Exception e) {
            ExportPlugin.logError(e);
        } finally {
            graphics = null;
        }
    }

    private List<Hashtable<String, Object>> getSchemaList() {
        int tableId = 0;
        ArrayList<Hashtable<String, Object>> schemaList = new ArrayList<Hashtable<String, Object>>();
        ISchema[] schemas = modelExport.getModel().getSchemas();
        for (int i = 0; i < schemas.length; i++) {
            Hashtable<String, Object> schemaHash = new Hashtable<String, Object>();
            ISchema schema = schemas[i];
            ITable[] tables = schema.getTables();
            ArrayList<Hashtable<String, Object>> list = new ArrayList<Hashtable<String, Object>>(tables.length);
            for (int j = 0; j < tables.length; j++) {
                Hashtable<String, Object> tableData = new Hashtable<String, Object>();
                ITable table = tables[j];
                tablesList.add(table);
                tableId++;
                tableData.put("id", new Integer(tableId));
                tableData.put("name", table.getName());
                list.add(tableData);
            }
            schemaHash.put("name", schema.getName());
            schemaHash.put("tables", list);
            schemaList.add(schemaHash);
        }
        return schemaList;
    }

    private void saveTableFiles(File targetDirectory) throws Exception {
        URL url = ExportPlugin.getDefault().getBundle().getEntry(templateDirectory + File.separator + "templates" + File.separator + "table.html");
        int tableCounter = 1;
        for (Iterator<ITable> it = tablesList.iterator(); it.hasNext(); ) {
            VelocityContext context = new VelocityContext();
            File targetFile = new File(targetDirectory, "tables" + File.separator + "table_" + tableCounter + ".html");
            if (!targetFile.exists()) targetFile.createNewFile();
            context.put("columns", getColumnList((ITable) it.next()));
            FileWriter writer = new FileWriter(targetFile, false);
            InputStreamReader reader = new InputStreamReader(url.openStream());
            Velocity.evaluate(context, writer, "RMBench HTML EXport", reader);
            writer.close();
            reader.close();
            tableCounter++;
        }
    }

    private List<Hashtable<String, Object>> getColumnList(ITable table) {
        IColumn[] columns = table.getColumns();
        ArrayList<Hashtable<String, Object>> columnList = new ArrayList<Hashtable<String, Object>>(columns.length);
        for (int i = 0; i < columns.length; i++) {
            IColumn col = columns[i];
            Hashtable<String, Object> columnData = new Hashtable<String, Object>();
            columnData.put("name", col.getName());
            columnData.put("datatype", col.getDataType().getPrimaryName());
            columnData.put("precision", new Long(col.getSize()));
            columnData.put("scale", new Integer(col.getScale()));
            if (col.getDefault() == null) columnData.put("default", ""); else columnData.put("default", col.getDefault());
            if (col.belongsToPrimaryKey()) {
                columnData.put("pk", new Integer(1));
            } else {
                columnData.put("pk", new Integer(0));
            }
            if (col.getNullable()) {
                columnData.put("not_null", new Integer(0));
            } else {
                columnData.put("not_null", new Integer(1));
            }
            if (col.getComment() == null) columnData.put("comment", ""); else columnData.put("comment", col.getComment());
            columnList.add(columnData);
        }
        return columnList;
    }

    /**
     * Creates the file tree and copies the css und js file to the new directory
     * @throws Exception
     */
    private void createFileTree(File targetDirectory) throws Exception {
        File tablesDirectory = new File(targetDirectory, "tables");
        if (!tablesDirectory.exists()) tablesDirectory.mkdir();
        File imageDirectory = new File(targetDirectory, "images");
        if (!imageDirectory.exists()) imageDirectory.mkdir();
        File jsDirectory = new File(targetDirectory, "js");
        if (!jsDirectory.exists()) jsDirectory.mkdir();
        Enumeration<?> fileEnumeration = ExportPlugin.getDefault().getBundle().getEntryPaths(templateDirectory + "/images");
        while (fileEnumeration.hasMoreElements()) {
            String fileName = (String) fileEnumeration.nextElement();
            if (fileName.indexOf(".svn") != -1) continue;
            URL imageUrl = ExportPlugin.getDefault().getBundle().getEntry(fileName);
            File targetFile = new File(targetDirectory, fileName.substring(templateDirectory.length()));
            if (!targetFile.exists()) targetFile.createNewFile();
            copyFile((FileInputStream) imageUrl.openStream(), new FileOutputStream(targetFile, false));
        }
        URL fileURL;
        if (templateType == TWO_FRAME_TEMPLATE) {
            fileURL = ExportPlugin.getDefault().getBundle().getEntry("HTMLExportTemplate/js/rmbench2f.js");
        } else {
            fileURL = ExportPlugin.getDefault().getBundle().getEntry("HTMLExportTemplate/js/rmbench.js");
        }
        File targetFile = new File(targetDirectory, File.separator + "js" + File.separator + "rmbench.js");
        if (!targetFile.exists()) targetFile.createNewFile();
        copyFile((FileInputStream) fileURL.openStream(), new FileOutputStream(targetFile, false));
        if (templateType == THREE_FRAME_TEMPLATE) {
            fileURL = ExportPlugin.getDefault().getBundle().getEntry(templateDirectory + "/rmbench.css");
        } else {
            fileURL = ExportPlugin.getDefault().getBundle().getEntry(templateDirectory + "/rmbench2f.css");
        }
        targetFile = new File(targetDirectory, "rmbench.css");
        if (!targetFile.exists()) targetFile.createNewFile();
        copyFile((FileInputStream) fileURL.openStream(), new FileOutputStream(targetFile, false));
    }

    /**
     * Copies the data from the inputstream to the outputstream and closes both streams after
     * finishing
     * @param is
     * @param os
     * @throws IOException
     */
    private void copyFile(FileInputStream is, FileOutputStream os) throws IOException {
        int data;
        while ((data = is.read()) != -1) {
            os.write(data);
        }
        is.close();
        os.close();
    }

    public void setImageSize(Dimension imageSize) {
        this.imageSize = imageSize;
    }

    public void setImageSize(int width, int height) {
        this.imageSize.width = width;
        this.imageSize.height = height;
    }
}
