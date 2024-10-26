package ar.edu.unicen.exa.server.ia.serverLogic;

import java.awt.Rectangle;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import ar.edu.unicen.exa.server.ia.variable.datatypes.IATimeElapsedFloatQueryResult;
import ar.edu.unicen.exa.server.ia.variable.datatypes.IATimeElapsedIntegerQueryResult;
import ar.edu.unicen.exa.server.ia.variable.datatypes.IATimeElapsedStringQueryResult;
import ar.edu.unicen.exa.server.serverLogic.ModelAccess;
import common.ia.datatypes.UserEvent;
import common.ia.datatypes.UserEvent.Type;
import common.scrum.BacklogItem;
import common.scrum.DailyMeeting;
import common.scrum.Estimacion;
import common.scrum.EventItem;
import common.scrum.Item;
import common.scrum.PDFItem;
import common.scrum.ReportItem;
import common.scrum.WebPageItem;
import common.scrum.BacklogItem.Estado;

/**
 * 
 * @author tio, chuki
 */
public class ScrumModelAccess {

    /**
	 * Instancia de la clase.
	 */
    private static ScrumModelAccess instance = null;

    private Statement stmt;

    private Connection con;

    public ScrumModelAccess() {
        try {
            Properties prop = new Properties();
            URL url = ModelAccess.class.getClassLoader().getResource("u3dserver.properties");
            prop.load(url.openStream());
            String database = prop.getProperty("databaseIA", "//localhost/2bsoft");
            String user = prop.getProperty("userIA", "root");
            String password = prop.getProperty("passwordIA", "");
            System.out.println("Intentando cargar el conector...");
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("Conectando a la base de IA...");
            con = DriverManager.getConnection("jdbc:mysql:" + database, user, password);
            stmt = con.createStatement();
            System.out.println("Conexion a BD establecida con la base de IA");
        } catch (SQLException ex) {
            System.out.println("Error de mysql: " + ex.getLocalizedMessage() + " " + ex.getErrorCode());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Se produjo un error inesperado: " + e.getMessage());
        }
    }

    /**
	 * @return La instancia singleton de la clase.
	 */
    public static ScrumModelAccess getInstance() {
        if (instance == null) instance = new ScrumModelAccess();
        return instance;
    }

    public static void saveEvent(UserEvent event) {
        System.out.println(event.toString());
    }

    /**
	 * Retorna el query para obtener la secuencia de eventos para timeelapsed
	 * segun el valor que se pase como parametro (int, float o string)
	 * 
	 * @return
	 */
    public static String eventSequenceQuery(String user, UserEvent.Type from, UserEvent.Type to, String value) {
        return new String("select hasta.username, hasta.datetime, hasta.type, hasta.stringvalue,(select desde.datetime from eventlog desde where (desde.datetime = (select MAX(desdeTiempo.datetime) from eventlog desdetiempo where (desdetiempo.datetime<hasta.datetime) and (desdetiempo.username='" + user + "') and (desdetiempo.type='" + from.toString() + "'))) and (desde.username='" + user + "') and (desde.type='" + from.toString() + "') and (desde." + value + "=hasta." + value + ")) as 'tiempoInicial' from eventlog hasta where (hasta.username='" + user + "') and (hasta.type='" + to.toString() + "');");
    }

    public static String eventSequenceQuery(String user, UserEvent.Type from, UserEvent.Type to, String value, String mundo) {
        return new String("select hasta.username, hasta.datetime, hasta.type, hasta.stringvalue,(select desde.datetime from eventlog desde where (desde.datetime = (select MAX(desdeTiempo.datetime) from eventlog desdetiempo where (desdetiempo.datetime<hasta.datetime) and (desdetiempo.username='" + user + "') and (desdetiempo.type='" + from.toString() + "'))) and (desde.username='" + user + "') and (desde.type='" + from.toString() + "') and (desde." + value + "=hasta." + value + ")) as 'tiempoInicial' from eventlog hasta where (hasta.username='" + user + "') and (hasta.type='" + to.toString() + "');");
    }

    public static void main(String[] args) {
        System.out.println(eventSequenceQuery("pepe", Type.ENTER_WORLD, Type.EXIT_WORLD, "stringvalue"));
    }

    public ArrayList<Float> timeSequence(String user, UserEvent.Type from, UserEvent.Type to, String value, String world) {
        try {
            ResultSet result = stmt.executeQuery(eventSequenceQuery(user, from, to, value, world));
            ArrayList<Float> sequence = new ArrayList<Float>();
            while (result.next()) {
                Timestamp datetime = result.getTimestamp("datetime");
                Timestamp tiempoInicial = result.getTimestamp("tiempoInicial");
                long milisegundos = datetime.getTime() - tiempoInicial.getTime();
                float segundos = milisegundos / 1000.f;
                sequence.add(segundos);
            }
            return sequence;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<Object> eventSequence(String user, UserEvent.Type from, UserEvent.Type to, String value) {
        try {
            System.out.println(eventSequenceQuery(user, from, to, value));
            ResultSet result = stmt.executeQuery(eventSequenceQuery(user, from, to, value));
            ArrayList<Object> sequence = new ArrayList<Object>();
            while (result.next()) {
                String username = result.getString("username");
                Date datetime = result.getTimestamp("datetime");
                String type = result.getString("type");
                Date tiempoInicial = result.getTimestamp("tiempoInicial");
                if (value.equals("stringvalue")) {
                    String stringvalue = result.getString("stringvalue");
                    sequence.add(new IATimeElapsedStringQueryResult(username, datetime, type, tiempoInicial, stringvalue));
                } else if (value.equals("intvalue")) {
                    int intvalue = result.getInt("intvalue");
                    sequence.add(new IATimeElapsedIntegerQueryResult(username, datetime, type, tiempoInicial, intvalue));
                } else {
                    float floatvalue = result.getFloat("floatvalue");
                    sequence.add(new IATimeElapsedFloatQueryResult(username, datetime, type, tiempoInicial, floatvalue));
                }
            }
            return sequence;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void insertEvent(UserEvent event) {
        String consulta = "INSERT INTO `ia`.`eventlog` (`username`, `datetime`, `type`, `intvalue`, `stringvalue`, `floatvalue`) VALUES ('" + event.getUser() + "' , '" + this.parseCalendarToString(event.getDateTime()) + "' , '" + event.getType().toString() + "' ,'" + event.getIntValue() + "', '" + event.getStringValue() + "' ,'" + event.getFloatValue() + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public String parseCalendarToString(Calendar calendar) {
        Date date = calendar.getTime();
        return (date.getYear() + 1900) + "-" + (date.getMonth() + 1) + "-" + date.getDate() + " " + date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();
    }

    public void insertBacklogItem(BacklogItem bi, String idProj) {
        Rectangle sprintBound = bi.getBounds(BacklogItem.SPRINT_BOUNDS);
        Rectangle backBound = bi.getBounds(BacklogItem.DEFAULT_BOUNDS);
        String consulta = "INSERT INTO `ia`.`backlogitems` (`Id_Item`, `Descripcion`, `Titulo`, `Estimacion`, `NroSprint`, `Responsable`,`TiempoActual`,`Estado`,`" + "backlogX`,`backlogY`,`backlogW`,`backlogH`,`sprintX`,`sprintY`,`sprintW`, `sprintH`," + "`fechaIni`, `fechaFin`,`idProject`) VALUES ('" + bi.getId() + "' , '" + bi.getDescription() + "' , '" + bi.getTitle() + "' ,'" + bi.getEstimacion() + "', '" + bi.getNroSprint() + "' ,'" + bi.getResponsable() + "' ,'" + bi.getTiempoActual() + "' ,'" + bi.getEstado() + "' ,'" + backBound.getX() + "' ,'" + backBound.getY() + "' ,'" + backBound.getWidth() + "' ,'" + backBound.getHeight() + "' ,'" + sprintBound.getX() + "' ,'" + sprintBound.getY() + "' ,'" + sprintBound.getWidth() + "' ,'" + sprintBound.getHeight() + "' ,'" + parseCalendarToString(bi.getFechaInicio()) + "' ,'" + parseCalendarToString(bi.getFechaFin()) + "' ,'" + idProj + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void editBacklogItem(BacklogItem bi, String idProj) {
        String consulta = "UPDATE `ia`.`backlogitems` SET " + "Estimacion= '" + bi.getEstimacion() + "'" + ",Titulo= '" + bi.getTitle() + "'" + ",Descripcion= '" + bi.getDescription() + "'" + ",fechaIni= '" + parseCalendarToString(bi.getFechaInicio()) + "'" + ",fechaFin= '" + parseCalendarToString(bi.getFechaFin()) + "'" + ",Responsable= '" + bi.getResponsable() + "'" + " WHERE Id_Item = '" + bi.getId() + "'" + " and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void deleteBacklogItem(BacklogItem bi, String idProj) {
        String consulta = "DELETE FROM `ia`.`backlogitems` WHERE Id_Item = '" + bi.getId() + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void insertPdfItem(PDFItem pi, String idProj) {
        String consulta = "INSERT INTO `ia`.`pdfitems` (`id_item`, `pageNumber`, `url`,`idProject`) VALUES ('" + pi.getId() + "' , '" + pi.getPageNum() + "' , '" + pi.getUrl() + "' , '" + idProj + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void editPdfItem(PDFItem pi, String idProj) {
        String consulta = "UPDATE `ia`.`pdfitems` SET url=" + pi.getUrl() + "pageNumber=" + pi.getPageNum() + " WHERE id_item = '" + pi.getId() + "'" + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void deletePdfItem(PDFItem pi, String idProj) {
        String consulta = "DELETE FROM `ia`.`pdfitems` WHERE id_item = '" + pi.getId() + "'" + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void insertReportItem(ReportItem ri, String idProj) {
        String consulta = "INSERT INTO `ia`.`reportitems` (`Id_Item`, `idTarea`, `horasCargadas`, `comentario`,`usuario`,`fecha`,`idProject`) VALUES ('" + ri.getId() + "' , '" + ri.getIdTarea() + "' , '" + ri.getHoras() + "' ,'" + ri.getComentario() + "' ,'" + ri.getUser() + "' ,'" + parseCalendarToString(ri.getFecha()) + "' ,'" + idProj + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void editReportItem(ReportItem ri, String idProj) {
        String consulta = "UPDATE `ia`.`reportitems` SET horasCargadas=" + ri.getHoras() + " WHERE idTarea = '" + ri.getIdTarea() + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void deleteReportItem(ReportItem ri, String idProj) {
        String consulta = "DELETE FROM `ia`.`reportitems` WHERE Id_Item = '" + ri.getId() + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void insertEventItem(EventItem ei, String idProj) {
        String consulta = "INSERT INTO `ia`.`eventitems` (`Id_Item`, `fechaEvento`, `tema`, `descripcion`, `idProject`) VALUES ('" + ei.getId() + "' , '" + parseCalendarToString(ei.getFechaEvento()) + "' , '" + ei.getTheme() + "' ,'" + ei.getDescripcion() + "' ,'" + idProj + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void editEventItem(EventItem ei, String idProj) {
        String consulta = "UPDATE `ia`.`eventitems` SET fechaEvento= '" + parseCalendarToString(ei.getFechaEvento()) + "'," + "tema= '" + ei.getTheme() + "'," + " descripcion= '" + ei.getDescripcion() + "'" + " WHERE Id_Item = '" + ei.getId() + "' " + "and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void deleteEventItem(EventItem ei, String idProj) {
        String consulta = "DELETE FROM `ia`.`eventitems` WHERE Id_Item = '" + ei.getId() + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void insertEstimacion(Estimacion e, String idProyecto) {
        String consulta = "INSERT INTO `ia`.`estimacion` (`Id_Estimacion`, `user`, `valor`, `idTarea`, `rondaIteracion`,`idProject`) VALUES ('" + e.getIdEstimacion() + "' , '" + e.getUser() + "' , '" + e.getValor() + "' ,'" + e.getId_tarea() + "' , '" + e.getRondaIteracion() + "' , '" + idProyecto + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e1) {
            System.err.println(consulta);
            e1.printStackTrace();
        }
    }

    public void editEstimacion(Estimacion est, String idProj) {
        String consulta = "UPDATE `ia`.`estimacion` SET rondaIteracion=" + est.getRondaIteracion() + " WHERE Id_Estimacion = '" + est.getIdEstimacion() + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void deleteEstimacion(Estimacion est, String idProj) {
        String consulta = "DELETE FROM `ia`.`estimacion` WHERE Id_Estimacion = '" + est.getIdEstimacion() + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public Hashtable<Integer, Item> loadBacklogItems(String idProj) {
        Hashtable<Integer, Item> set = new Hashtable<Integer, Item>();
        String consulta = "select * from backlogitems where idProject = '" + idProj + "'";
        try {
            ResultSet res = stmt.executeQuery(consulta);
            while (res.next()) {
                BacklogItem bi = new BacklogItem();
                int id = res.getInt("Id_Item");
                bi.setId(id);
                Rectangle backBounds = new Rectangle();
                backBounds.setBounds(res.getInt("backlogX"), res.getInt("backlogY"), res.getInt("backlogW"), res.getInt("backlogH"));
                Rectangle sprintBounds = new Rectangle();
                sprintBounds.setBounds(res.getInt("sprintX"), res.getInt("sprintY"), res.getInt("sprintW"), res.getInt("sprintH"));
                bi.setTitle(res.getString("Titulo"));
                bi.setDescription(res.getString("Descripcion"));
                bi.setEstimacion(res.getFloat("Estimacion"));
                bi.setNroSprint(res.getInt("NroSprint"));
                bi.setResponsable(res.getString("Responsable"));
                bi.setTiempoActual(res.getFloat("TiempoActual"));
                bi.setEstado(Estado.valueOf(res.getString("Estado")));
                Calendar ini = new GregorianCalendar();
                Date date = res.getDate("fechaIni");
                if (date != null) {
                    ini.setTime(date);
                    bi.setFechaInicio(ini);
                }
                Calendar fin = new GregorianCalendar();
                Date date2 = res.getDate("fechaFin");
                if (date2 != null) {
                    fin.setTime(date2);
                    bi.setFechaFin(fin);
                }
                set.put(id, bi);
            }
            return set;
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
        return null;
    }

    public Hashtable<Integer, Item> loadPdfItems(String idProj) {
        String consulta = "select * from pdfitems where idProject = '" + idProj + "'";
        Hashtable<Integer, Item> set = new Hashtable<Integer, Item>();
        try {
            ResultSet res = stmt.executeQuery(consulta);
            while (res.next()) {
                PDFItem pi = new PDFItem(null);
                Rectangle pdfBounds = new Rectangle();
                pdfBounds.setBounds(res.getInt("pdfX"), res.getInt("pdfY"), res.getInt("pdfW"), res.getInt("pdfH"));
                int id = res.getInt("Id_Item");
                pi.setId(id);
                pi.setPageNum(res.getInt("pageNumber"));
                pi.setUrl(res.getURL("url"));
                set.put(id, pi);
            }
            return set;
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
        return null;
    }

    public Hashtable<Integer, Item> loadReportItems(String idProyecto) {
        String consulta = "select * from reportitems where idProject='" + idProyecto + "'";
        Hashtable<Integer, Item> set = new Hashtable<Integer, Item>();
        try {
            ResultSet res = stmt.executeQuery(consulta);
            while (res.next()) {
                ReportItem ri = new ReportItem();
                int id = res.getInt("Id_Item");
                ri.setId(id);
                ri.setHoras(res.getFloat("horasCargadas"));
                ri.setIdTarea(res.getInt("idTarea"));
                ri.setComentario(res.getString("comentario"));
                ri.setUser(res.getString("usuario"));
                Calendar cal = new GregorianCalendar();
                cal.setTime(res.getDate("fecha"));
                ri.setFecha(cal);
                set.put(id, ri);
            }
            return set;
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
        return null;
    }

    public Hashtable<Integer, Item> loadEventItems(String idProj) {
        Hashtable<Integer, Item> set = new Hashtable<Integer, Item>();
        String consulta = "select * from eventitems where  idProject = '" + idProj + "'";
        try {
            ResultSet res = stmt.executeQuery(consulta);
            while (res.next()) {
                EventItem ei = new EventItem(null, null);
                int id = res.getInt("Id_Item");
                ei.setId(id);
                ei.setDescripcion(res.getString("descripcion"));
                ei.setTheme(res.getString("tema"));
                Calendar cal = new GregorianCalendar();
                cal.setTime(res.getDate("fechaEvento"));
                ei.setFechaEvento(cal);
                set.put(id, ei);
            }
            return set;
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
        return null;
    }

    public void insertWebPage(WebPageItem w, String idProj) {
        String consulta = "INSERT INTO `ia`.`webitems` (`id_item`,`url`,`idProject`) VALUES ('" + w.getId() + "' , '" + w.getUrl() + "' , '" + idProj + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void deleteWebPage(WebPageItem w, String idProj) {
        String consulta = "DELETE FROM `ia`.`webitems` WHERE Id_Item = '" + w.getId() + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void editWebPage(WebPageItem w, String idProj) {
        String consulta = "UPDATE `ia`.`webitems` SET urln=" + w.getUrl() + " WHERE Id_Item = '" + w.getId() + "' and idProject = '" + idProj + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (Exception e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public Hashtable<Integer, Item> loadWebPagesItems(String idProj) {
        Hashtable<Integer, Item> set = new Hashtable<Integer, Item>();
        String consulta = "select * from webitems where idProject = '" + idProj + "'";
        try {
            ResultSet res = stmt.executeQuery(consulta);
            while (res.next()) {
                WebPageItem wi = new WebPageItem(null);
                int id = res.getInt("Id_Item");
                wi.setId(id);
                wi.setUrl(res.getString("url"));
                set.put(id, wi);
            }
            return set;
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
        return null;
    }

    public Vector<String> loadProjects() {
        Vector<String> v = new Vector<String>();
        String consulta = "select * from project";
        try {
            ResultSet res = stmt.executeQuery(consulta);
            while (res.next()) {
                String id = res.getString("idProject");
                v.add(id);
            }
            return v;
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
        return null;
    }

    public Vector<DailyMeeting> loadMeetings(String idProj) {
        Vector<DailyMeeting> v = new Vector<DailyMeeting>();
        String consulta = "select * from meeting where idProject = '" + idProj + "'";
        try {
            ResultSet res = stmt.executeQuery(consulta);
            while (res.next()) {
                DailyMeeting dm = new DailyMeeting();
                String user = res.getString("user");
                String tarea = res.getString("tituloTarea");
                int idtarea = res.getInt("idTarea");
                String preg1 = res.getString("quest1");
                String preg2 = res.getString("quest2");
                String preg3 = res.getString("quest3");
                dm.setId_tarea(idtarea);
                dm.setTarea(tarea);
                dm.setPregunta1(preg1);
                dm.setPregunta2(preg2);
                dm.setPregunta3(preg3);
                dm.setUsuario(user);
                v.add(dm);
            }
            return v;
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
        return null;
    }

    public void updateProject(String old, String _new) {
        String consulta = "UPDATE `ia`.`project` SET idProject='" + _new + "'" + "WHERE idProject='" + old + "';";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void insertProject(String project) {
        String consulta = "INSERT INTO `ia`.`project` (`idProject`) VALUES ('" + project + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void deleteProject(String project) {
        String consulta = "DELETE FROM `ia`.`project` WHERE idProject = '" + project + "'";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }

    public void insertMeeting(DailyMeeting dm, String idProject) {
        String consulta = "INSERT INTO `ia`.`meeting` (`idTarea`,`tituloTarea`,`user`,`quest1`,`quest2`,`quest3`,`idProject`) VALUES ('" + dm.getId_tarea() + "' , '" + dm.getTarea() + "' , '" + dm.getUsuario() + "' , '" + dm.getPregunta1() + "' , '" + dm.getPregunta2() + "' , '" + dm.getPregunta3() + "' , '" + idProject + "')";
        try {
            stmt.executeUpdate(consulta);
        } catch (SQLException e) {
            System.err.println(consulta);
            e.printStackTrace();
        }
    }
}
