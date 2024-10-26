package convertemployeesfromxmltomysql;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.bpd.pojos.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hibernate.HibernateException;
import pk_util.UtilPojosModel;

/**
 * @author u19730
 */
public class xCopyAsOfEmployeeToVwSsMaEmpleadosActivos {

    public static Double percentCounter = 0D;

    public static double percentGlobal = 0D;

    public static Double percentCurrent = 0D;

    public static Integer percentAnt = 0;

    public static NumberFormat formatter = new DecimalFormat("#0.00");

    public static void main(String argv[]) throws ParserConfigurationException, MalformedURLException, IOException, SAXException {
        try {
            setEmployees();
            setManagers();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static Boolean setEmployees() {
        Session s = getMySession();
        System.out.println("START setEmployees");
        Boolean resultado = true;
        try {
            List<AsOfEmployeeNew> employeesData = getEmployeesData();
            percentCounter = 0D;
            percentAnt = 0;
            percentGlobal = employeesData.size();
            for (Iterator<AsOfEmployeeNew> it = employeesData.iterator(); it.hasNext(); ) {
                AsOfEmployeeNew d = it.next();
                VwSsMaEmpleadosActivos o = new VwSsMaEmpleadosActivos();
                o.setId(null);
                o.setAdmRrhh("1");
                o.setAlias(d.getAlias());
                o.setBirthDate(d.getBirthDate().toString());
                o.setBloodDonorInd(d.getBloodDonorInd());
                o.setBloodTypeCode(d.getBloodTypeCode());
                o.setCargo(d.getCargo());
                o.setCentro(d.getCentro());
                o.setCodigoEmpleado(d.getEmpleado());
                o.setCodigoSupervisorOriginal(((Integer) d.getMgrEmpId()).toString());
                o.setDepartamento(d.getDepartamento());
                o.setDepositAcctNbr(d.getDepositAcctNbr());
                o.setEmpPhoto(d.getEmpPhoto());
                o.setEmpStatusCode(d.getEmpStatusCode());
                o.setEmplDesc(d.getEmplDesc());
                o.setEmplId(d.getEmplId());
                o.setEmploymentTypeCode(d.getEmploymentTypeCode());
                o.setFirstMiddleName(d.getFirstMiddleName());
                o.setFirstName(d.getFirstName());
                o.setHeight(d.getHeight());
                o.setHireDate(d.getHireDate().toString());
                o.setIndividualId(d.getIndividualId());
                o.setLastName(d.getLastName());
                o.setLocCode(d.getLocCode());
                o.setMgrEmpId(d.getMgrEmpId());
                o.setNationalId1(d.getNationalId1());
                o.setNombreEmpleado(d.getNombre());
                o.setPayGradeCode(d.getPayGradeCode());
                o.setPhone1AreaCityCode(d.getPhone1AreaCityCode());
                o.setPhone1ExtensionNbr(d.getPhone1ExtensionNbr());
                o.setPhone1Nbr(d.getPhone1Nbr());
                o.setPolizaId(d.getPolizaId());
                o.setPuesto(d.getPuesto());
                o.setRutaPhoto(d.getRutaPhoto());
                o.setSexCode(d.getSexCode());
                o.setUnidad(d.getUnidad());
                o.setWeight(d.getWeight());
                s.beginTransaction();
                s.saveOrUpdate(o);
                s.getTransaction().commit();
                showPercent("setEmployees");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("END setEmployees");
        return resultado;
    }

    public static List<AsOfEmployeeNew> getEmployeesData() {
        Session s = getMySession();
        List<AsOfEmployeeNew> lout = new ArrayList();
        Criteria c = s.createCriteria(AsOfEmployeeNew.class);
        try {
            lout = c.list();
        } catch (HibernateException he) {
            System.out.println(he.getMessage());
        } finally {
            s.close();
        }
        return lout;
    }

    public static Boolean setManagers() {
        System.out.println("START setManagers");
        Boolean resultado = true;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            URL url = new URL("http://co01sgd/sgdcent/cnt/vw_ss_ma_empleados_activos_to_xml.asp");
            InputStream inputStream = url.openStream();
            Document doc = db.parse(inputStream);
            doc.getDocumentElement().normalize();
            NodeList employees = doc.getElementsByTagName("empleado");
            percentCounter = 0D;
            percentAnt = 0;
            percentGlobal = employees.getLength();
            for (int emp = 0; emp < employees.getLength(); emp++) {
                Node fstNode = employees.item(emp);
                if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element employee = (Element) fstNode;
                    Integer codigoEmpleado = Integer.parseInt((employee.getElementsByTagName("codigo_empleado").item(0).getChildNodes().item(0)).getNodeValue().toString().trim());
                    Integer mgrEmpId = Integer.parseInt((employee.getElementsByTagName("mgr_emp_id").item(0).getChildNodes().item(0)).getNodeValue().toString().trim());
                    VwSsMaEmpleadosActivos result = updateEmployee(codigoEmpleado, mgrEmpId);
                }
                showPercent("setManagers");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("END setManagers");
        return resultado;
    }

    public static VwSsMaEmpleadosActivos updateEmployee(Integer codigoEmpleado, Integer mgrEmpId) {
        Session s = getMySession();
        Criteria c = s.createCriteria(VwSsMaEmpleadosActivos.class);
        c.add(Restrictions.eq("codigoEmpleado", codigoEmpleado));
        VwSsMaEmpleadosActivos data = (VwSsMaEmpleadosActivos) c.uniqueResult();
        if (data != null) {
            data.setMgrEmpId(mgrEmpId);
            s.beginTransaction();
            s.saveOrUpdate(data);
            s.getTransaction().commit();
        }
        s.close();
        return data;
    }

    private static void showPercent(String functionName) {
        percentCounter = percentCounter + 1D;
        percentCurrent = ((percentCounter / percentGlobal) * 100D);
        if (percentCurrent.intValue() != percentAnt) {
            percentAnt = percentCurrent.intValue();
            System.out.println(functionName + ": " + formatter.format(percentCurrent) + "%");
        }
    }

    private static Session getMySession() {
        Session s = UtilPojosModel.getSessionFactory().openSession();
        return s;
    }
}
