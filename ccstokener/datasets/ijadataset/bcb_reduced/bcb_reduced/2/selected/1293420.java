package org.jabusuite.address.session;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Set;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.jabusuite.address.Address;
import org.jabusuite.address.letter.Letter;
import org.jabusuite.core.companies.JbsCompany;
import org.jabusuite.core.users.JbsUser;
import org.jabusuite.core.utils.EJbsObject;
import org.jabusuite.core.utils.JbsManagement;
import org.jabusuite.logging.Logger;

@Stateless
public class AddressesBean extends JbsManagement implements AddressesRemote {

    private Logger logger = Logger.getLogger(AddressesBean.class);

    @PersistenceContext(unitName = "jabusuite")
    private EntityManager manager;

    public void createAddress(Address address, JbsUser user, JbsCompany company) {
        super.createDataset(manager, address, user, user, user.getMainGroup(), company);
    }

    /**
     * Checks if the specified payment exists (for deleteOldPayments)
     * @param addressLetter 
     * @param addressLetters 
     * @return
     */
    protected boolean addressLetterExists(Letter addressLetter, List<Letter> addressLetters) {
        boolean found = false;
        Iterator<Letter> it = addressLetters.iterator();
        while ((!found) && (it.hasNext())) {
            Letter exAddressLetter = it.next();
            if (exAddressLetter.getId() == addressLetter.getId()) {
                found = true;
            }
        }
        return found;
    }

    /**
     * Deletes all existing positions that do not exist anymore (for updateDataset)
     * @param manager 
     * @param invoice 
     * @param addressLetters 
     */
    protected void deleteOldAddressLetters(EntityManager manager, Address address, List<Letter> addressLetters) {
        Iterator<Letter> it = addressLetters.iterator();
        while (it.hasNext()) {
            Letter addressLetter = it.next();
            if (!addressLetterExists(addressLetter, address.getLetters())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Deleting no longer existing addressLetter " + addressLetter.getId());
                }
                manager.remove(addressLetter);
            }
        }
    }

    public void updateAddress(Address address, JbsUser changeUser) throws EJbsObject {
        Address existingAddress = manager.find(Address.class, address.getId());
        if (existingAddress != null) {
            this.deleteOldDependingObjects(manager, existingAddress.getContacts(), address.getContacts());
        }
        super.updateDataset(manager, address, changeUser);
    }

    public Address findAddress(long id) {
        return findAddress(id, true);
    }

    public Address findAddress(long id, boolean withAdditionalData) {
        Address address = manager.find(Address.class, (long) id);
        if ((address != null) && (withAdditionalData)) {
            int letterCount = address.getLetters().size();
            logger.debug("Letters: " + letterCount);
        }
        return manager.find(Address.class, (long) id);
    }

    public long getDatasetCount(String filter, JbsUser user, JbsCompany company) {
        return super.getDatasetCount(manager, Address.class, filter, user, company);
    }

    public List getDatasets(String filter, String orderFields, JbsUser user, JbsCompany company, int firstResult, int resultCount) {
        return super.getDatasets(manager, Address.class, filter, orderFields, user, company, firstResult, resultCount);
    }

    public double getDistance(Address startAddress, Address endAddress) {
        updateCoordinates(startAddress);
        updateCoordinates(endAddress);
        double distance = 0.0;
        double radius = 6367000;
        double deltaLatitude = endAddress.getLatitude() - startAddress.getLatitude();
        double deltaLongitude = endAddress.getLongitude() - startAddress.getLongitude();
        double creatCircleDistance = 2 * Math.asin(Math.min(1, Math.sqrt(Math.sin((Math.PI / 180) * deltaLatitude / 2) * Math.sin((Math.PI / 180) * deltaLatitude / 2) + Math.cos((Math.PI / 180) * endAddress.getLatitude()) * Math.cos((Math.PI / 180) * startAddress.getLatitude()) * Math.sin((Math.PI / 180) * deltaLongitude / 2) * Math.sin((Math.PI / 180) * deltaLongitude / 2))));
        distance = radius * creatCircleDistance;
        return distance;
    }

    public double getDistanceAsKM(Address startAddress, Address endAddress) {
        return Math.round(getDistance(startAddress, endAddress) / 1000);
    }

    public void updateCoordinates(Address address) {
        String mapURL = "http://maps.google.com/maps/geo?output=csv";
        String mapKey = "ABQIAAAAi__aT6y6l86JjbootR-p9xQd1nlEHNeAVGWQhS84yIVN5yGO2RQQPg9QLzy82PFlCzXtMNe6ofKjnA";
        String location = address.getStreet() + " " + address.getZip() + " " + address.getCity();
        if (logger.isDebugEnabled()) {
            logger.debug(location);
        }
        double[] coordinates = { 0.0, 0.0 };
        String content = "";
        try {
            location = URLEncoder.encode(location, "UTF-8");
            String request = mapURL + "&q=" + location + "&key=" + mapKey;
            URL url = new URL(request);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                content += line;
            }
            reader.close();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error from google: " + e.getMessage());
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(content);
        }
        StringTokenizer tokenizer = new StringTokenizer(content, ",");
        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            i++;
            String token = tokenizer.nextToken();
            if (i == 3) {
                coordinates[0] = Double.parseDouble(token);
            }
            if (i == 4) {
                coordinates[1] = Double.parseDouble(token);
            }
        }
        if ((coordinates[0] != 0) || (coordinates[1] != 0)) {
            address.setLatitude(coordinates[0]);
            address.setLongitude(coordinates[1]);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid coordinates for address " + address.getId());
            }
        }
    }
}
