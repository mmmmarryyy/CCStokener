package es.juanrak.svn.cnf;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import es.juanrak.svn.modelo.SVNLogEntradaTipo;

public class SVNConfiguracion {

    /** Nombre del Parametro -D que apunta al fichero de configuracion. */
    public static final String PARAMETRO_FICHERO = "juanrak.svn.cnf.file";

    private String url = "http://svn.svnkit.com/repos/svnkit/trunk/doc";

    private String usuario = "anonymous";

    private String password = "anonymous";

    private ArrayList<SVNLogEntradaTipo> tipoEntradas = new ArrayList<SVNLogEntradaTipo>();

    private ArrayList<SVNMenuConfiguracion> menuConfiguracion = new ArrayList<SVNMenuConfiguracion>();

    private ArrayList<SVNParametro> parametros = new ArrayList<SVNParametro>();

    HashMap<String, SVNParametro> hashParametros = null;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    /**
     * Obtiene la configuracion del menu.
     */
    public ArrayList<SVNMenuConfiguracion> getMenuConfiguracion() {
        return menuConfiguracion;
    }

    /**
     * Establece la configuracion de menu.
     */
    public void setMenuConfiguracion(ArrayList<SVNMenuConfiguracion> menuConfiguracion) {
        this.menuConfiguracion = menuConfiguracion;
    }

    /**
     * Obtiene el tipo de entradas del Log disponibles.
     */
    public ArrayList<SVNLogEntradaTipo> getTipoEntradas() {
        return tipoEntradas;
    }

    /**
     * Establece el tipo de entradas del Log disponibles.
     */
    public void setTipoEntradas(ArrayList<SVNLogEntradaTipo> tipos) {
        this.tipoEntradas = tipos;
    }

    public SVNParametro getParametro(String parametro) {
        if (this.hashParametros == null) {
            loadParametros(this.parametros);
        }
        return this.hashParametros.get(parametro);
    }

    public void setParametro(SVNParametro parametro) {
        SVNParametro actual = hashParametros.get(parametro.getNombre());
        if (actual != null) {
            this.parametros.remove(actual);
        }
        this.hashParametros.put(parametro.getNombre(), parametro);
        this.parametros.add(parametro);
    }

    public ArrayList<SVNParametro> getParametros() {
        return parametros;
    }

    public void setParametros(ArrayList<SVNParametro> parametrosRecibo) {
        if (parametrosRecibo == null) {
            parametrosRecibo = new ArrayList<SVNParametro>();
        }
        loadParametros(parametrosRecibo);
        this.parametros = parametrosRecibo;
    }

    private void loadParametros(ArrayList<SVNParametro> parametros) {
        if (this.hashParametros == null) {
            this.hashParametros = new HashMap<String, SVNParametro>();
        }
        for (int i = 0, total = parametros.size(); i < total; i++) {
            SVNParametro param = parametros.get(i);
            this.hashParametros.put(param.getNombre(), param);
        }
    }

    private URL urlFichero;

    private File fdFichero;

    public void guardar() {
        if (fdFichero != null) {
            save(fdFichero, this);
        }
    }

    protected void setFicheroConfiguracion(File fdFichero) {
        this.fdFichero = fdFichero;
    }

    protected void setFicheroConfiguracion(URL urlFichero) {
        this.urlFichero = urlFichero;
    }

    /**
     * Se puede escribir sobre el fichero de configuracion sobre el que hemos leido la actual configuracion.
     */
    public boolean isConfiguracionWriteable() {
        return ((fdFichero != null) && fdFichero.isFile() && fdFichero.canWrite());
    }

    public static SVNConfiguracion load(File fd) {
        SVNConfiguracion configuracion = null;
        try {
            FileInputStream fis = new FileInputStream(fd);
            XMLDecoder xenc = new XMLDecoder(fis);
            configuracion = (SVNConfiguracion) xenc.readObject();
            configuracion.setFicheroConfiguracion(fd);
            xenc.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return configuracion;
    }

    public static SVNConfiguracion load(URL urlConfiguracion) {
        SVNConfiguracion configuracion = null;
        try {
            XMLDecoder xenc = new XMLDecoder(urlConfiguracion.openStream());
            configuracion = (SVNConfiguracion) xenc.readObject();
            configuracion.setFicheroConfiguracion(urlConfiguracion);
            xenc.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return configuracion;
    }

    public static void save(File fd, SVNConfiguracion conf) {
        FileOutputStream fos = null;
        try {
            try {
                fos = new FileOutputStream(fd);
                XMLEncoder xenc = new XMLEncoder(fos);
                xenc.writeObject(conf);
                xenc.close();
                System.out.println("Encoding completed....");
            } finally {
                fos.flush();
                fos.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static final void crearConfiguracionInical(File fdFichero) throws Exception {
        SVNConfiguracion conf = new SVNConfiguracion();
        conf.setUrl("pp");
        conf.setParametro(new SVNParametro("RUTA_PARCIAL_DESDE", "TRUNK"));
        SVNMenuConfiguracion menu1 = new SVNMenuConfiguracion();
        {
            menu1.setNombre("Comparaciones");
            SVNMenuConfiguracion s1 = new SVNMenuConfiguracion();
            s1.setNombre("Comparacion Tipo 1");
            s1.setComando("kdiff3");
            menu1.getSubMenus().add(s1);
            SVNMenuConfiguracion s2 = new SVNMenuConfiguracion();
            s2.setNombre("Comparacion Tipo 2");
            s2.setComando("tkdiff");
            menu1.getSubMenus().add(s2);
        }
        conf.getMenuConfiguracion().add(menu1);
        SVNMenuConfiguracion menu2 = new SVNMenuConfiguracion();
        {
            menu2.setNombre("Comparaciones");
            menu2.getParametros().add("REVISION");
            menu2.getParametros().add("-P");
        }
        conf.getMenuConfiguracion().add(menu2);
        SVNConfiguracion.save(fdFichero, conf);
    }

    public static void main(String argv[]) throws Exception {
        if (argv == null || argv.length < 1) {
            System.err.println("SVNConfiguracion <directorio>");
            System.err.println("Parametro configuracion " + PARAMETRO_FICHERO);
            System.exit(-1);
        }
        File fd = new File(argv[0]);
        if (!fd.exists() || !fd.isDirectory() || !fd.canWrite()) {
            System.err.println("SVNConfiguracion <directorio>");
            System.err.println("El parametro o no es directorio o no tiene permisos de escritura.");
            System.exit(-2);
        }
        SVNConfiguracion.crearConfiguracionInical(new File(fd, "svnconfiguracion.xml"));
    }
}
