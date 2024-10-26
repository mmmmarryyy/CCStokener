package animal;

import java.awt.event.*;
import java.awt.image.*;
import java.io.InputStream;
import java.net.*;
import java.util.Iterator;
import java.util.List;
import dialog.PropertiesFrame;
import dialog.ScaledImageCanvas;
import location.Farmyard;
import location.LocationPanel;
import location.StandardLocationPanel;

/**
 *  Class that models standard animal components. This class is responsible
 *  for displaying an animal at a location. Each location can have several
 *  animals and thus several animal components.
 *
 *  @author Henrik Eriksson
 *  @version 0.01
 *  @see LocationPanel
 *  @see StandardLocationPanel
 */
public class StandardAnimalComponent extends ScaledImageCanvas implements AnimalComponent {

    /**
	 *  The scale factor for display-large mode.
	 *  @serial
	 */
    private double largeScale;

    /**
	 *  The scale factor for display-small mode.
	 *  @serial
	 */
    private double smallScale;

    private transient AnimalProxy animal_proxy;

    /**
	 *  The image source.
	 *  @serial
	 */
    private ImageProducer image_source;

    /**
	 *  Creates an animal component.
	 *
	 *  @param largeScale the scale when the component is in large mode
	 *  @param smallScale the scale when the component is in small mode
	 *  @param ap the animal proxy
	 */
    public StandardAnimalComponent(double largeScale, double smallScale, AnimalProxy ap) {
        super();
        this.largeScale = largeScale;
        this.smallScale = smallScale;
        animal_proxy = ap;
        try {
            URL url = new URL(ap.getPicture());
            if (url.getFile().equals("/~TDDB64/pics/pig.gif")) {
                setImage("../resources/pig.gif");
            } else {
                InputStream strm = url.openStream();
                strm.close();
                setImage(url);
            }
        } catch (Exception e) {
            setImage("../resources/bug.gif");
            System.err.println("Unable to get picture from URL.");
        }
        image_source = getImage().getSource();
        setScale(largeScale);
        try {
            URL url = new URL(ap.getSound());
            if (url.getFile().equals("/~TDDB64/sound/oink.au")) {
                URL pig_url = new URL(ClassLoader.getSystemResource(".") + "../resources/pig.au");
                if (pig_url != null) url = pig_url;
            }
            Farmyard.playSound(url);
        } catch (MalformedURLException e) {
            System.err.println("Malformed sound URL.");
        }
        final PropertiesFrame pf = new PropertiesFrame(animal_proxy);
        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent event) {
                if ((event.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
                    pf.show();
                    Thread th = new Thread() {

                        public void run() {
                            pf.setValues();
                        }
                    };
                    try {
                        th.start();
                    } catch (IllegalThreadStateException e) {
                    }
                }
            }
        });
    }

    /**
	 *  Gets the AnimalProxy
	 *
	 *  @return the AnimalProxy
	 */
    public AnimalProxy getAnimalProxy() {
        return animal_proxy;
    }

    /**
	 *  Sets the size of the location.
	 *
	 *  @param large size flag. True if an enlarged location is wanted, otherwise false.
	 */
    public void setLargeMode(boolean large) {
        setScale((large) ? largeScale : smallScale);
    }

    /**
	 *  Sets the image filters for this animal component. The filters can be
	 *  used to illustrate various states of the animal, such as sickness. The
	 *  filters are applied in series.
	 *
	 *  @param filters a list of new image filter for the animal picture
	 */
    public void setImageFilters(List filters) {
        ImageProducer source = image_source;
        for (Iterator e = filters.iterator(); e.hasNext(); ) source = new FilteredImageSource(source, (ImageFilter) e.next());
        setImage(createImage(source));
        repaint();
    }
}
