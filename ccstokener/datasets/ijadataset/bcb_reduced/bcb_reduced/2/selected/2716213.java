package jcpcotizaciones.reportes;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import jcpcotizaciones.Control.ManejadorBaseDatos;
import jcpcotizaciones.vista.vistaCotizacion;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Administrador
 */
public class GenerarReporte {

    /** Creates a new instance of GenerarReporte */
    public GenerarReporte() {
    }

    public static void generarReporte(String url, String numero) {
        try {
            ManejadorBaseDatos basedatos = ManejadorBaseDatos.getInstancia();
            Map parameters = new HashMap();
            parameters.put("NumCotizacion", numero);
            basedatos.conectar();
            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(vistaCotizacion.class.getResource(url).openStream(), parameters, basedatos.getConexion());
            JasperViewer jviewer = new JasperViewer(jasperPrint, false);
            jviewer.setVisible(true);
        } catch (JRException ex) {
            System.out.println(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "mensaje de error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "mensaje de error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void generarReporteAbsoluto(String url, String busqueda, String variable_sql) {
        try {
            String fileName = url;
            ManejadorBaseDatos basedatos = ManejadorBaseDatos.getInstancia();
            Map parameters = new HashMap();
            parameters.put(variable_sql, busqueda);
            basedatos.conectar();
            JasperPrint jasperPrint;
            JasperReport jasperReport;
            try {
                jasperPrint = JasperFillManager.fillReport(fileName, parameters, basedatos.getConexion());
                JasperViewer jviewer = new JasperViewer(jasperPrint, false);
                jviewer.setVisible(true);
            } catch (JRException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "mensaje de error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "mensaje de error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            Logger.getLogger(GenerarReporte.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
