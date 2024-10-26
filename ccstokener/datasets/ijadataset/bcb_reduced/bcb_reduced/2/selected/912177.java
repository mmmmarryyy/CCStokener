package mwt.xml.xdbforms.xformlayer.transactions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import mwt.xml.xdbforms.xformlayer.transactions.impl.XFormTransactionFactoryDM;
import mwt.xml.xdbforms.xformlayer.transactions.impl.XFormTransactionFactoryDS;

/**
 * Progetto Master Web Technology
 * @author Gianfranco Murador, Cristian Castiglia, Matteo Ferri
 */
public abstract class XFormTransactionFactory {

    /**
     * Di default verrà usato il driver manager
     */
    private boolean useDataSource = false;

    /**
     * instanza statica della factory
     */
    private static XFormTransactionFactory instance = null;

    /**
     * setta un parametro della factory, per l'uso del
     * datasource. 
     * @param useDataSource
     */
    public void useDataSource(boolean useDataSource) {
        this.useDataSource = useDataSource;
    }

    /**
     * Crea una nuova istanza della factory
     * @param useDataSource, se true la factory inizializza una connessione
     * tramite data source, altrimenti usa il driver manager
     * @return XFormTransactionFactory
     */
    public static synchronized XFormTransactionFactory newInstance(boolean useDataSource) {
        if (instance == null) {
            if (useDataSource) {
                instance = new XFormTransactionFactoryDS();
            } else {
                instance = new XFormTransactionFactoryDM();
            }
        }
        return instance;
    }

    /**
     * Crea un nuova istanza della factory usando i 
     * parametri inseriti nel file di configurazione, 
     * in particolare la properties jdbc.connection.
     **/
    public static synchronized XFormTransactionFactory newInstance() {
        Properties prop = new Properties();
        @SuppressWarnings("static-access") URL url = Thread.currentThread().getContextClassLoader().getResource("mwt/xml/xdbforms/configuration/xdbforms.properties");
        try {
            InputStream inStream = url.openStream();
            prop.load(inStream);
            String jdbcConnection = prop.getProperty("jdbc.connection");
            if (jdbcConnection.equals("drivermanager")) {
                return new XFormTransactionFactoryDM();
            } else {
                return new XFormTransactionFactoryDS();
            }
        } catch (IOException ex) {
            Logger.getLogger(XFormTransactionFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Definisce un'operazione di inserimento
     * @param database, nome del database dove fare l'inserimento
     * @return oggetto che implementa l'interfaccia XFormTransaction
     */
    public abstract XFormTransaction newXFormInsertTransaction(String database);

    /**
     * Definisce un'operazione di aggiornamento
     * @param database, nome del database dove fare l'aggiornamento
     * @return oggetto che implementa l'interfaccia XFormTransaction
     */
    public abstract XFormTransaction newXFormUpdateTransaction(String database);
}
