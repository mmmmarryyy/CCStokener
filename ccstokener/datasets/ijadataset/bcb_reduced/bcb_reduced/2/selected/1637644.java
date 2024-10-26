package it.hotel.model.sms.manager;

import it.hotel.model.CalendarUtils;
import it.hotel.model.booking.Booking;
import it.hotel.model.customer.Customer;
import it.hotel.model.customer.manager.ICustomerManager;
import it.hotel.model.room.Room;
import it.hotel.model.room.manager.IRoomManager;
import it.hotel.model.structure.Structure;
import it.hotel.model.structure.manager.IStructureManager;
import it.hotel.model.user.User;
import it.hotel.model.user.manager.IUserManager;
import it.hotel.system.exception.SystemException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import javax.annotation.Resource;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SmsManager implements ISmsManager {

    private IUserManager userManager = null;

    private IStructureManager structureManager;

    private IRoomManager roomManager = null;

    private ICustomerManager customerManager = null;

    /** 
	 *  
	 * 
	 * @param x description 
	 * @return x description. 
	 * @throws exception description
	 */
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public List<Booking> parse(String message) {
        String parts[] = message.split("#");
        List<Booking> bookings = new ArrayList<Booking>();
        String text = parts[0];
        String bookinginfo[] = text.split(" ");
        String username = bookinginfo[0];
        String password = bookinginfo[1];
        int room = Integer.parseInt(bookinginfo[2]);
        String date = bookinginfo[3];
        int days = Integer.parseInt(bookinginfo[4]);
        String customername = bookinginfo[5];
        String customersurname = bookinginfo[6];
        GregorianCalendar startdate = CalendarUtils.GetGregorianCalendarFromSms(date);
        GregorianCalendar finishdate = CalendarUtils.GetCalendarDaysAfter(startdate, days);
        User user = null;
        try {
            user = userManager.getUser(username, password);
            Booking booking = new Booking();
            booking.setBeginDate(startdate);
            booking.setFinishDate(finishdate);
            Structure structure = (Structure) structureManager.get(user.getStructureId());
            booking.setStructure(structure);
            Room roomToBook = roomManager.getRoomFromRoomNumber(user.getStructureId(), room);
            booking.setRoom(roomToBook);
            Customer customer = new Customer();
            customer.setName(customername);
            customer.setSurname(customersurname);
            customer.setStructure(structure);
            customerManager.add(customer);
            booking.setCustomer(customer);
            bookings.add(booking);
        } catch (SystemException e) {
        }
        return bookings;
    }

    public boolean smsResponse(String customerPhoneNumber) throws ClientProtocolException, IOException {
        boolean message = true;
        String textMessage = "La%20sua%20prenotazione%20e%60%20andata%20a%20buon%20fine";
        DefaultHttpClient httpclient = new DefaultHttpClient();
        String uri = "http://smswizard.globalitalia.it/smsgateway/send.asp";
        String other = "http://smswizard.globalitalia.it/smsgateway/send.asp";
        String url = uri + "?" + "Account=sardricerche" + "&Password=v8LomdZT" + "&PhoneNumbers=" + "%2b393285683484" + "&SMSData=" + textMessage + "&Recipients=1" + "&Sender=Web Hotel" + "&ID=11762";
        String urlProva = other + "?" + "Account=sardricerche" + "&Password=v8LomdZT" + "&PhoneNumbers=" + customerPhoneNumber + "&SMSData=" + textMessage + "&Recipients=1" + "&Sender=+393337589951" + "&ID=11762";
        HttpPost httpPost = new HttpPost(urlProva);
        HttpResponse response = httpclient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        return message;
    }

    @Resource(name = "userManager")
    public void setUserManager(IUserManager userManager) {
        this.userManager = userManager;
    }

    @Resource(name = "roomRawManager")
    public void setRoomManager(IRoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Resource(name = "customerRawManager")
    public void setCustomerManager(ICustomerManager customerManager) {
        this.customerManager = customerManager;
    }

    @Resource(name = "localizedStructureManager")
    public void setStructureManager(IStructureManager structureManager) {
        this.structureManager = structureManager;
    }
}
