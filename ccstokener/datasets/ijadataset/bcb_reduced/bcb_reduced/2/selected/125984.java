package ar.com.khronos.core.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.Type;

/**
 * Imagen que puede ser persistida en una base de datos.
 * <p>
 * La imagen se guarda como {@link Lob} en un array de
 * bytes.
 * 
 * @author <a href="mailto:tezequiel@gmail.com">Ezequiel Turovetzky</a>
 *
 */
@MappedSuperclass
public abstract class ImagenPersistible extends PersistibleObject {

    /**
	 * Imagen.
	 * Se mapea a nivel de atributo, porque sino
	 * cosas raras pasan.
	 */
    @Lob
    @Column(name = "imagen")
    @Type(type = "org.hibernate.type.ByteArrayBlobType")
    private byte[] imagen;

    /**
     * Crea una nueva instancia de esta clase.
     */
    public ImagenPersistible() {
    }

    /**
     * Crea una nueva instancia de esta clase.
     * 
     * @param imagen Tren de bytes que representa la imagen
     */
    public ImagenPersistible(byte[] imagen) {
        this.imagen = imagen;
    }

    /**
     * Crea una nueva instancia de esta clase.
     * 
     * @param file Archivo de la imagen
     * 
     * @throws IOException En caso de error en la lectura
     * 		   del archivo de imagen
     */
    public ImagenPersistible(File file) throws IOException {
        this(new FileInputStream(file));
    }

    /**
     * Crea una nueva instancia de esta clase.
     * 
     * @param url URL del archivo
     * 
     * @throws IOException En caso de error en la lectura
     * 		   de la URL
     */
    public ImagenPersistible(URL url) throws IOException {
        this(url.openStream());
    }

    /**
     * Crea una nueva instancia de esta clase.
     * 
     * @param inputStream Stream de la imagen
     * 
     * @throws IOException En caso de error en la lectura
     * 		   del stream
     */
    public ImagenPersistible(InputStream inputStream) throws IOException {
        int b = -1;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((b = inputStream.read()) != -1) {
            out.write(b);
        }
        imagen = out.toByteArray();
        inputStream.close();
        out.close();
    }

    /**
     * Devuelve el tren de bytes de esta
     * imagen.
     * 
     * @return Los bytes de esta imagen
     */
    public byte[] getImagen() {
        return imagen;
    }

    /**
     * Establece el tren de bytes de esta
     * imagen.
     * 
     * @param imagen Los bytes de esta imagen
     */
    public void setImagen(byte[] imagen) {
        this.imagen = imagen;
    }
}
