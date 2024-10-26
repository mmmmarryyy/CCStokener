package org.j2eebuilder.util;

import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.ArrayList;
import org.j2eebuilder.model.ManagedTransientObject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.Map;
import java.util.HashMap;
import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.j2eebuilder.model.NonManagedTransientObjectImpl;
import org.j2eebuilder.model.ejb.ManagedTransientObjectHandler;
import org.j2eebuilder.view.Request;
import com.ohioedge.j2ee.api.org.RequestBean;
import com.ohioedge.j2ee.api.org.RequestDelegate;
import com.ohioedge.j2ee.api.org.RequestParameterBean;
import java.util.Collection;
import java.util.HashSet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.regex.*;

/**
 * 
 * @(#)UtilityBean.java 1.350 01/12/03
 * 
 *                      The UtilityBean class contains generic utility methods.
 * 
 * @version 1.3
 * 
 * @see java.beans.Introspector
 * 
 */
public class UtilityBean extends NonManagedTransientObjectImpl {

    private static transient LogManager log = new LogManager(UtilityBean.class);

    private static Boolean semaphore = new Boolean(true);

    private static UtilityBean globalUtilityBean = null;

    public final String HTML_INPUT_TAG_TYPE_HIDDEN = "HIDDEN";

    public final String ATTRIBUTE_DELIMITER = "|";

    public static UtilityBean getCurrentInstance() {
        synchronized (semaphore) {
            if (globalUtilityBean == null) {
                globalUtilityBean = new UtilityBean();
            }
        }
        return globalUtilityBean;
    }

    /**
	 * 
	 * Default constructor
	 * 
	 */
    public UtilityBean() {
    }

    public boolean isNullOrEmpty(String stringVar) {
        return (stringVar == null || stringVar.length() == 0);
    }

    public boolean isNullOrEmpty(StringBuffer stringVar) {
        return (stringVar == null || stringVar.toString().trim().length() == 0);
    }

    public <T> boolean isNullOrEmpty(Collection<T> collection) {
        return (collection == null || collection.isEmpty());
    }

    public <K, V> boolean isNullOrEmpty(Map<K, V> map) {
        return (map == null || map.isEmpty());
    }

    public <T> boolean isOfSize(Collection<T> collection, int size) {
        if (collection != null && collection.size() == size) {
            return true;
        }
        return false;
    }

    public <T> boolean isOfSizeGreaterThan(Collection<T> collection, int size) {
        if (collection != null && collection.size() > size) {
            return true;
        }
        return false;
    }

    public <T> boolean isOfSizeGreaterThanOrEqualTo(Collection<T> collection, int size) {
        if (collection != null && collection.size() >= size) {
            return true;
        }
        return false;
    }

    public <T> T getNextElement(Collection<T> collection) throws UtilityException {
        if (isNullOrEmpty(collection)) throw new UtilityException("Collection is empty.");
        return collection.iterator().next();
    }

    /**
	 * fix query criteria
	 */
    public String suffixWithPercent(String criteria) {
        if (UtilityBean.getCurrentInstance().isNullOrEmpty(criteria) || ((criteria.length() == 1) && ("%".equals(criteria)))) {
            criteria = "%";
        } else {
            if (!"%".equals(criteria.substring(criteria.length() - 1))) {
                criteria = criteria + "%";
            }
        }
        return criteria;
    }

    /**
	 * getComponentVO uses o.description = ?1 (and not LIKE) thus the need to
	 * remove suffix
	 */
    public String removePercentSuffix(String criteria) {
        if (UtilityBean.getCurrentInstance().isNullOrEmpty(criteria) || ((criteria.length() == 1) && ("%".equals(criteria)))) {
            criteria = "";
        } else {
            if ("%".equals(criteria.substring(criteria.length() - 1))) {
                criteria = criteria.substring(0, criteria.length() - 1);
            }
        }
        return criteria;
    }

    /**
	 * Determine how many times a string A occurs in a string B
	 */
    public int getOccurrenceOfString(String searchFor, String searchIn) {
        int len = searchFor.length();
        int result = 0;
        if (len > 0) {
            int start = searchIn.indexOf(searchFor);
            while (start != -1) {
                result++;
                start = searchIn.indexOf(searchFor, start + len);
            }
        }
        return result;
    }

    /**
	 * 
	 * Resets all the writable properties of toObejct
	 * 
	 * 1. Uses introspection to get writable methods of toObject
	 * 
	 * 2. Passes null values to the writable methods of toObject
	 * 
	 * @param toObject
	 *            Object to be reset.
	 * 
	 * @return Successful or toObject has no readable properties
	 * 
	 */
    public static String resetProperties(Object toObject) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(toObject.getClass());
            PropertyDescriptor[] descriptorsOfToObject = beanInfo.getPropertyDescriptors();
            if (descriptorsOfToObject.length < 1) {
                return "ToObject has no readable/writable properties";
            }
            int sizeOfToObjectProperties = descriptorsOfToObject.length;
            for (int i = 0; i < sizeOfToObjectProperties; i++) {
                PropertyDescriptor descriptorOfToObject = descriptorsOfToObject[i];
                Method writeMethod = descriptorOfToObject.getWriteMethod();
                if (writeMethod != null) {
                    StringBuffer buf = new StringBuffer();
                    buf.append(toObject.getClass().getName());
                    buf.append(".resetProperties():");
                    buf.append("Unable to invoke method [");
                    buf.append(writeMethod);
                    buf.append("]");
                    try {
                        writeMethod.invoke(toObject, new Object[] { null });
                    } catch (InvocationTargetException ite) {
                        log.warn(buf.toString() + ". " + ite.toString());
                    } catch (IllegalAccessException iae) {
                        log.warn(buf.toString() + ". " + iae.toString());
                    } catch (NullPointerException npe) {
                        log.warn(buf.toString() + ". " + npe.toString());
                    } catch (Exception e) {
                        log.warn(buf.toString() + ". " + e.toString());
                    }
                }
            }
        } catch (IntrospectionException ie) {
            log.printStackTrace(ie, LogManager.ERROR);
        }
        return "Successful";
    }

    /**
	 * 
	 * Copies properties of fromObejct to toObject
	 * 
	 * 1. Uses introspection to get readable methods of fromObject
	 * 
	 * 2. Searches corresponding write methods of toObject
	 * 
	 * 3. Invokes readable methods of fromObject
	 * 
	 * 4. Assigns returned values to the corresponding writ methods
	 * 
	 * of toObject
	 * 
	 * @param fromObject
	 *            Object to be reset.
	 * 
	 * @param toObject
	 *            Object to be reset.
	 * 
	 */
    public static String copyProperties(Object fromObject, Object toObject) {
        if (fromObject == null || toObject == null) {
            return "Both source and destination objects must be not null";
        }
        try {
            BeanInfo beanInfoOfFromObject = Introspector.getBeanInfo(fromObject.getClass());
            PropertyDescriptor[] descriptorsOfFromObject = beanInfoOfFromObject.getPropertyDescriptors();
            if (descriptorsOfFromObject.length < 1) {
                return "FromObject has no readable properties";
            }
            int sizeOfFromObjectProperties = descriptorsOfFromObject.length;
            BeanInfo beanInfo = Introspector.getBeanInfo(toObject.getClass());
            PropertyDescriptor[] descriptorsOfToObject = beanInfo.getPropertyDescriptors();
            if (descriptorsOfToObject.length < 1) {
                return "ToObject has no writable properties";
            }
            int sizeOfToObjectProperties = descriptorsOfToObject.length;
            for (int i = 0; i < sizeOfToObjectProperties; i++) {
                PropertyDescriptor descriptorOfToObject = descriptorsOfToObject[i];
                Method writeMethod = descriptorOfToObject.getWriteMethod();
                if (writeMethod != null) {
                    for (int i1 = 0; i1 < sizeOfFromObjectProperties; i1++) {
                        PropertyDescriptor descriptorOfFromObject = descriptorsOfFromObject[i1];
                        if (descriptorOfFromObject.getName().equals(descriptorOfToObject.getName())) {
                            Method readMethod = descriptorOfFromObject.getReadMethod();
                            if (readMethod != null) {
                                try {
                                    Object rObj = readMethod.invoke(fromObject, null);
                                    writeMethod.invoke(toObject, new Object[] { rObj });
                                } catch (InvocationTargetException ite) {
                                    log.warn(toObject.getClass().getName() + ".copyProperties():" + ite.toString());
                                } catch (IllegalAccessException iae) {
                                    log.warn(toObject.getClass().getName() + ".copyProperties():" + iae.toString());
                                } catch (IllegalArgumentException iae) {
                                    log.warn(toObject.getClass().getName() + ".copyProperties():The number of actual parameters supplied (by readMethod of fromObject) via args[" + readMethod.toString() + "] is different from the number of formal parameters required by the underlying method (of toObject's writeMethod):" + iae.toString());
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IntrospectionException ie) {
            log.printStackTrace(ie, LogManager.WARN);
        }
        return "Object was successfully loaded.";
    }

    /**
	 * 
	 * Copy properties in the propertyNames from fromObject to toObject
	 * 
	 * Used by ValueObjectHandlerHelper.setDataVO
	 * 
	 */
    public static String copyProperties(Object fromObject, Object toObject, String[] propertyNames) {
        if (fromObject == null || toObject == null) {
            return "Both source and destination objects must be not null";
        }
        if (propertyNames == null || propertyNames.length == 0) {
            return "No properties specified.";
        }
        try {
            BeanInfo beanInfoOfFromObject = Introspector.getBeanInfo(fromObject.getClass());
            PropertyDescriptor[] descriptorsOfFromObject = beanInfoOfFromObject.getPropertyDescriptors();
            if (descriptorsOfFromObject.length < 1) {
                return "FromObject has no readable properties";
            }
            int sizeOfFromObjectProperties = descriptorsOfFromObject.length;
            BeanInfo beanInfo = Introspector.getBeanInfo(toObject.getClass());
            PropertyDescriptor[] descriptorsOfToObject = beanInfo.getPropertyDescriptors();
            if (descriptorsOfToObject.length < 1) {
                return "ToObject has no writable properties";
            }
            int sizeOfToObjectProperties = descriptorsOfToObject.length;
            for (int p = 0; p < propertyNames.length; p++) {
                String specifiedPropertyName = propertyNames[p];
                for (int t = 0; t < sizeOfToObjectProperties; t++) {
                    PropertyDescriptor descriptorOfToObject = descriptorsOfToObject[t];
                    if (descriptorOfToObject.getName().equals(specifiedPropertyName)) {
                        Method writeMethod = descriptorOfToObject.getWriteMethod();
                        if (writeMethod != null) {
                            try {
                                for (int i = 0; i < sizeOfFromObjectProperties; i++) {
                                    PropertyDescriptor descriptorOfFromObject = descriptorsOfFromObject[i];
                                    if (descriptorOfFromObject.getName().equals(specifiedPropertyName)) {
                                        Method readMethod = descriptorOfFromObject.getReadMethod();
                                        if (readMethod != null) {
                                            try {
                                                Object rObj = readMethod.invoke(fromObject, null);
                                                writeMethod.invoke(toObject, new Object[] { rObj });
                                            } catch (InvocationTargetException ite) {
                                                log.warn(toObject.getClass().getName() + ".copyProperties():" + ite.toString());
                                            } catch (IllegalAccessException iae) {
                                                log.warn(toObject.getClass().getName() + ".copyProperties():" + iae.toString());
                                            } catch (IllegalArgumentException iae) {
                                                log.warn(toObject.getClass().getName() + ".copyProperties():The number of actual parameters supplied (by readMethod of fromObject) via args[" + readMethod.toString() + "] is different from the number of formal parameters required by the underlying method (of toObject's writeMethod):" + iae.toString());
                                            }
                                        }
                                        break;
                                    }
                                }
                            } catch (SecurityException se) {
                                log.warn("Property [" + specifiedPropertyName + "] throws SecurityException in toObject [" + toObject.getClass().getName() + "]");
                            }
                        }
                    }
                }
            }
        } catch (IntrospectionException ie) {
            log.printStackTrace(ie, LogManager.ERROR);
        }
        return "Object was successfully loaded.";
    }

    /**
	 * 
	 * copy values from the request into the valueObject
	 * 
	 * if property is not available in the request, leave its value in the
	 * valueObject as is ( do
	 * 
	 * not reset it to null)
	 * 
	 * @return object
	 * 
	 */
    public static Object copyProperties(Request requestHelperBean, Object valueObject) throws RequestParameterInvalidFormatException {
        try {
            if (requestHelperBean == null) {
                throw new org.j2eebuilder.BuilderException("Request is null.");
            }
            if (valueObject == null) {
                throw new org.j2eebuilder.BuilderException("ValueObject is null.");
            }
            boolean bInvalidFormat = false;
            StringBuffer bufInvalidFormatMessage = new StringBuffer();
            BeanInfo beanInfo = Introspector.getBeanInfo(valueObject.getClass());
            PropertyDescriptor[] descriptorsOfToObject = beanInfo.getPropertyDescriptors();
            if (descriptorsOfToObject.length < 1) {
                return "ToObject has no writable properties";
            }
            int sizeOfToObjectProperties = descriptorsOfToObject.length;
            for (int i = 0; i < sizeOfToObjectProperties; i++) {
                PropertyDescriptor descriptorOfToObject = descriptorsOfToObject[i];
                Method writeMethod = descriptorOfToObject.getWriteMethod();
                if (writeMethod != null) {
                    try {
                        Object rObj = requestHelperBean.getParameter(descriptorOfToObject.getName(), descriptorOfToObject.getPropertyType());
                        writeMethod.invoke(valueObject, new Object[] { rObj });
                    } catch (IllegalAccessException e) {
                        log.warn("copyProperties(Request, ValueObject):IllegalAccessException. Descriptor[" + descriptorOfToObject.getName() + "]" + e.toString());
                    } catch (IllegalArgumentException iae) {
                        log.warn("copyProperties(Request, ValueObject)::IllegalArgumentException. Descriptor[" + descriptorOfToObject.getName() + "]" + iae.toString());
                    } catch (InvocationTargetException iae) {
                        log.warn("copyProperties(Request, ValueObject)::InvocationTargetException. Descriptor[" + descriptorOfToObject.getName() + "]" + iae.toString());
                    } catch (RequestParameterInvalidFormatException rpe) {
                        if (!bInvalidFormat) {
                            bInvalidFormat = true;
                        }
                        bufInvalidFormatMessage.append(rpe.getMessage());
                    } catch (RequestParameterException rpe) {
                        log.debug("copyProperties(Request, ValueObject)::RequestParameterException" + rpe.toString());
                    }
                }
            }
            if (bInvalidFormat) {
                throw new RequestParameterInvalidFormatException(bufInvalidFormatMessage.toString());
            }
            try {
                if (requestHelperBean.getIsAjax()) {
                    Object bValueObject = UtilityBean.getCurrentInstance().copyPropertiesFromXmlRequestBody(requestHelperBean, valueObject.getClass());
                    UtilityBean.getCurrentInstance().copyProperties(bValueObject, valueObject);
                    log.debug("inside utilitybean - copying from xml valueObject:" + valueObject);
                }
            } catch (Exception e) {
                log.log("Unable to use the xmltoobject utility:", e, log.DEBUG);
            }
            return valueObject;
        } catch (org.j2eebuilder.BuilderException e) {
            log.warn("copyProperties(Request, ValueObject):" + e.toString());
        } catch (java.beans.IntrospectionException e) {
            log.warn("copyProperties(Request, ValueObject):" + e.toString());
        }
        return null;
    }

    /**
	 * 
	 * copy values from the request into the valueObject
	 * 
	 * if property is not available in the request, leave its value in the
	 * valueObject as is ( do
	 * 
	 * not reset it to null)
	 * 
	 * @return object
	 * 
	 */
    public static Object copyProperties(Request requestHelperBean, String[] fromProperties, ManagedTransientObject valueObject, String[] toProperties) {
        try {
            if (requestHelperBean == null) {
                throw new org.j2eebuilder.BuilderException("Request is null.");
            }
            if (valueObject == null) {
                throw new org.j2eebuilder.BuilderException("ValueObject is null.");
            }
            if (fromProperties == null || toProperties == null || fromProperties.length != toProperties.length) {
                throw new org.j2eebuilder.BuilderException("fromProperties set size not equal to toProperties set size.");
            }
            BeanInfo beanInfo = Introspector.getBeanInfo(valueObject.getClass());
            PropertyDescriptor[] descriptorsOfToObject = beanInfo.getPropertyDescriptors();
            if (descriptorsOfToObject.length < 1) {
                return "ToObject has no writable properties";
            }
            for (int p = 0; p < fromProperties.length; p++) {
                String fromProperty = fromProperties[p];
                String toProperty = toProperties[p];
                int sizeOfToObjectProperties = descriptorsOfToObject.length;
                for (int i = 0; i < sizeOfToObjectProperties; i++) {
                    PropertyDescriptor descriptorOfToObject = descriptorsOfToObject[i];
                    if (descriptorOfToObject.getName().equals(toProperty)) {
                        Method writeMethod = descriptorOfToObject.getWriteMethod();
                        if (writeMethod != null) {
                            try {
                                Object rObj = requestHelperBean.getParameter(fromProperty, descriptorOfToObject.getPropertyType());
                                writeMethod.invoke(valueObject, new Object[] { rObj });
                            } catch (IllegalAccessException e) {
                                log.warn("copyProperties(ValueObject, Request):IllegalAccessException" + e.toString());
                            } catch (IllegalArgumentException iae) {
                                log.warn("copyProperties(ValueObject, Request)::IllegalArgumentException" + iae.toString());
                            } catch (RequestParameterException rpe) {
                            }
                        }
                        break;
                    }
                }
            }
            return valueObject;
        } catch (Exception e) {
            log.warn("copyProperties(ValueObject, Request):" + e.toString());
        }
        return null;
    }

    public Object copyProperties(String[] fromProperties, Object[] fromValues, ManagedTransientObjectHandler valueObject) {
        try {
            if (valueObject == null) throw new org.j2eebuilder.BuilderException("valueObject is null.");
            if (fromProperties == null || fromValues == null || fromProperties.length < 1 || fromProperties.length != fromValues.length) throw new org.j2eebuilder.BuilderException("fromProperties set size not equal to values set size.");
            log.debug("fromProperties -" + fromProperties);
            if (fromProperties != null) {
                for (int j = 0; j < fromProperties.length; j++) {
                    log.debug("fromProperties:" + fromProperties[j]);
                }
            }
            BeanInfo beanInfo = Introspector.getBeanInfo(valueObject.getClass());
            PropertyDescriptor[] descriptorsOfToObject = beanInfo.getPropertyDescriptors();
            if (descriptorsOfToObject.length < 1) {
                throw new org.j2eebuilder.BuilderException("valueObject[" + valueObject + "] has no writable properties.");
            }
            for (int p = 0; p < fromProperties.length; p++) {
                String fromProperty = fromProperties[p];
                Object fromObject = fromValues[p];
                int sizeOfToObjectProperties = descriptorsOfToObject.length;
                for (int i = 0; i < sizeOfToObjectProperties; i++) {
                    PropertyDescriptor descriptorOfToObject = descriptorsOfToObject[i];
                    if (descriptorOfToObject.getName().equals(fromProperty)) {
                        Method writeMethod = descriptorOfToObject.getWriteMethod();
                        if (writeMethod != null) {
                            try {
                                writeMethod.invoke(valueObject, new Object[] { fromObject });
                            } catch (IllegalAccessException e) {
                                log.warn("copyProperties(fromProperties, fromValues, ValueObject):IllegalAccessException" + e.toString());
                            } catch (IllegalArgumentException iae) {
                                log.warn("copyProperties(fromProperties, fromValues, ValueObject)::IllegalArgumentException" + iae.toString());
                            }
                        }
                        break;
                    }
                }
            }
            return valueObject;
        } catch (Exception e) {
            log.warn("copyProperties(fromProperties, fromValues, ValueObject):" + e.toString());
            return null;
        }
    }

    /**
	 * 
	 * covert enumeration to collection (arrayList)
	 * 
	 */
    public static Collection collection(Enumeration fromEnum) {
        if (fromEnum == null) {
            return null;
        }
        Collection col = new ArrayList();
        while (fromEnum.hasMoreElements()) {
            col.add(fromEnum.nextElement());
        }
        return col;
    }

    public static String getReadMethod(String propertyName) {
        return "get" + capitalize(propertyName);
    }

    public static String getWriteMethod(String propertyName) {
        return "set" + capitalize(propertyName);
    }

    public static String capitalize(String s) {
        char chars[] = s.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    /**
	 * splitMultiKeyAttributeString for example split "inputID,inputTypeID" into
	 * two attributes inputID and inputTypeID for example, name =
	 * inputID|inputTypeID|inputName|inputDescription value =
	 * 12321312|92423|testname|testDesc
	 */
    public Map<String, String[]> splitMultiKeyAttributeString(String name, String[] values) {
        Map<String, String[]> mapOfAttributes = new HashMap();
        Pattern p = Pattern.compile("[\\" + ATTRIBUTE_DELIMITER + "]+");
        String[] arrayOfAttributeName = p.split(name);
        if (arrayOfAttributeName.length == 1) {
            mapOfAttributes.put(name, values);
            return mapOfAttributes;
        }
        String[][] arrayOfAttributeValues = null;
        if (values != null) {
            arrayOfAttributeValues = new String[values.length][];
            for (int j = 0; j < values.length; j++) {
                if (values[j] != null) {
                    String[] splitValues = p.split(values[j]);
                    arrayOfAttributeValues[j] = splitValues;
                }
            }
        }
        for (int i = 0; i < arrayOfAttributeName.length; i++) {
            String[] attributeValues = new String[arrayOfAttributeValues.length];
            for (int j = 0; j < arrayOfAttributeValues.length; j++) {
                try {
                    attributeValues[j] = arrayOfAttributeValues[j][i];
                } catch (NullPointerException npe) {
                    attributeValues[j] = null;
                } catch (ArrayIndexOutOfBoundsException npe) {
                    attributeValues[j] = null;
                }
            }
            mapOfAttributes.put(arrayOfAttributeName[i], attributeValues);
        }
        return mapOfAttributes;
    }

    /**
	 * used by InstanceLocator. query
	 */
    public String getCacheKeyForRemoval(String componentName, String methodName, Class[] paramClasses, Object[] paramObjects) {
        StringBuffer key = new StringBuffer();
        key.append("Component[" + componentName + "]");
        key.append(":");
        return key.toString();
    }

    /**
	 * used by InstanceLocator. query
	 */
    public String getCacheKey(String componentName, String methodName, Class[] paramClasses, Object[] paramObjects) {
        StringBuffer key = new StringBuffer();
        key.append("Component[" + componentName + "]");
        key.append(":");
        key.append("Query[" + methodName + "]");
        if (paramClasses != null) {
            for (int i = 0; i < paramClasses.length; i++) {
                key.append(":");
                key.append("Parameter[");
                key.append(i);
                key.append("]{(");
                key.append(paramClasses[i]);
                key.append(")");
                key.append(paramObjects[i]);
                key.append("}");
            }
        }
        return key.toString();
    }

    /**
	 * return line.separator
	 */
    public String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    public String getTemporaryDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    public java.sql.Timestamp getCurrentTimestamp() {
        return new java.sql.Timestamp((new java.util.Date()).getTime());
    }

    public int getCurrentDayOfMonth() {
        return java.util.Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    public int getCurrentDayOfWeek() {
        return java.util.Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
    }

    public int getCurrentYear() {
        return java.util.Calendar.getInstance().get(Calendar.YEAR);
    }

    public int getCurrentMonth() {
        return this.convertToOneBasedMonthValue(java.util.Calendar.getInstance().get(Calendar.MONTH));
    }

    private int convertToZeroBasedMonthValue(int oneBasedMonthValue) {
        return oneBasedMonthValue - 1;
    }

    private int convertToOneBasedMonthValue(int zeroBasedMonthValue) {
        return zeroBasedMonthValue + 1;
    }

    public int getPreviousMonth(int currentMonth) {
        return (currentMonth <= 1) ? 12 : (currentMonth - 1);
    }

    public int getNextMonth(int currentMonth) {
        return (currentMonth >= 12) ? 1 : (currentMonth + 1);
    }

    public Timestamp getTimestamp(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, convertToZeroBasedMonthValue(month), dayOfMonth);
        return new java.sql.Timestamp(calendar.getTime().getTime());
    }

    public String getFileSeparator() {
        return File.separator;
    }

    public String getUrlQueryParameterSeparator(boolean isFirstParam) {
        return isFirstParam ? "?" : "&";
    }

    public String buildUrlQuery(Map<String, String>[] params) {
        StringBuffer urlQuery = new StringBuffer();
        boolean isFirstParam = true;
        if (params != null) {
            for (Map<String, String> map : params) {
                for (Map.Entry mapEntry : map.entrySet()) {
                    if (isFirstParam) {
                        isFirstParam = false;
                    } else {
                        urlQuery.append("&");
                    }
                    urlQuery.append(mapEntry.getKey());
                    urlQuery.append("=");
                    urlQuery.append(mapEntry.getValue());
                }
            }
        }
        return urlQuery.toString();
    }

    /**
	 * return the content of url as string
	 */
    public static String getContentAsString(String fileUrl) throws java.io.FileNotFoundException, java.io.IOException {
        try {
            File fileContent = new File(fileUrl);
            if (fileContent != null) {
                java.net.URL url = fileContent.toURI().toURL();
                java.io.InputStream inputStream = url.openStream();
                if (inputStream != null) {
                    StringBuffer buf = null;
                    int i = -1;
                    while ((i = inputStream.read()) != -1) {
                        if (buf == null) {
                            buf = new StringBuffer();
                        }
                        buf.append((char) i);
                    }
                    inputStream.close();
                    if (buf != null) {
                        return buf.toString();
                    }
                }
            }
        } catch (java.io.FileNotFoundException e) {
            StringBuffer buf = new StringBuffer();
            buf.append("Url [");
            buf.append(fileUrl);
            buf.append("] is not a valid file. Verify the document setup.");
            throw new java.io.FileNotFoundException(buf.toString());
        } catch (java.io.IOException e) {
            StringBuffer buf = new StringBuffer();
            buf.append("Unable to retrieve content from url [");
            buf.append(fileUrl);
            buf.append("]. Verify the document setup.");
            throw new java.io.IOException(buf.toString());
        }
        return null;
    }

    public static byte[] getContentAsByteArray(String fileUrl) throws java.io.FileNotFoundException, java.io.IOException {
        try {
            File fileContent = new File(fileUrl);
            if (fileContent != null) {
                java.net.URL url = fileContent.toURI().toURL();
                java.io.InputStream inputStream = url.openStream();
                if (inputStream != null) {
                    ByteArrayOutputStream buf = null;
                    int i = -1;
                    while ((i = inputStream.read()) != -1) {
                        if (buf == null) {
                            buf = new ByteArrayOutputStream();
                        }
                        buf.write(i);
                    }
                    inputStream.close();
                    if (buf != null) {
                        return buf.toByteArray();
                    }
                }
            }
        } catch (java.io.FileNotFoundException e) {
            StringBuffer buf = new StringBuffer();
            buf.append("Url [");
            buf.append(fileUrl);
            buf.append("] is not a valid file. Verify the document setup.");
            throw new java.io.FileNotFoundException(buf.toString());
        } catch (java.io.IOException e) {
            StringBuffer buf = new StringBuffer();
            buf.append("Unable to retrieve content from url [");
            buf.append(fileUrl);
            buf.append("]. Verify the document setup.");
            throw new java.io.IOException(buf.toString());
        }
        return null;
    }

    public String buildHtmlInputTag(String name, String value, String tagType) {
        StringBuffer buf = new StringBuffer();
        buf.append("<INPUT TYPE=\"");
        buf.append(tagType);
        buf.append("\"");
        buf.append(" ");
        buf.append("NAME=\"");
        buf.append(name);
        buf.append("\"");
        buf.append(" ");
        buf.append("VALUE=\"");
        buf.append(value);
        buf.append("\"");
        buf.append(">");
        return buf.toString();
    }

    /**
	 * truncate to the given size if smaller than the given size then return as
	 * is
	 */
    public String truncate(String stringToTruncate, int truncateToLength) {
        if (isNullOrEmpty(stringToTruncate)) {
            return stringToTruncate;
        } else {
            int lengthOfStringToTruncate = stringToTruncate.length();
            if (lengthOfStringToTruncate > truncateToLength) {
                return stringToTruncate.substring(0, truncateToLength);
            } else {
                return stringToTruncate;
            }
        }
    }

    public org.j2eebuilder.model.ManagedTransientObject copyPropertiesFromXmlRequestBody(Request request, Class nonManagedBeanClass) throws org.j2eebuilder.util.RequestParameterException {
        String xmlString = null;
        xmlString = request.getStringParameter("requestBody")[0];
        return this.copyPropertiesFromXml(xmlString, nonManagedBeanClass);
    }

    private org.j2eebuilder.model.ManagedTransientObject copyPropertiesFromXml(String xmlString, Class nonManagedBeanClass) throws org.j2eebuilder.util.RequestParameterException {
        try {
            String decodedXmlString = java.net.URLDecoder.decode(xmlString, "UTF-8");
            JAXBContext jc = JAXBContext.newInstance(nonManagedBeanClass);
            Unmarshaller u = jc.createUnmarshaller();
            return (org.j2eebuilder.model.ManagedTransientObject) u.unmarshal(new ByteArrayInputStream(decodedXmlString.getBytes()));
        } catch (java.io.UnsupportedEncodingException ue) {
            log.log("Error occurred while decoding the xmlString[" + xmlString + "]", ue, log.ERROR);
        } catch (javax.xml.bind.JAXBException e) {
            log.error("Unable to unmarshal xmlString[" + xmlString + "] to object [" + nonManagedBeanClass.getName() + "]", e);
        }
        throw new org.j2eebuilder.util.RequestParameterException("Unable to unmarshal xmlString[" + xmlString + "] to object [" + nonManagedBeanClass.getName() + "]. Definition exception.");
    }

    /**
	 * copy properties from requestVO to nonManagedBeanClass
	 */
    public org.j2eebuilder.model.ManagedTransientObject copyPropertiesFromRequestBean(RequestBean requestVO, Class nonManagedBeanClass) throws org.j2eebuilder.util.RequestParameterException {
        String requestXml = (new RequestDelegate()).toXml(requestVO);
        return this.copyPropertiesFromXml(requestXml, nonManagedBeanClass);
    }

    /**
	 * return random aphanumeric string
	 * 
	 * @param size
	 * @return
	 */
    public String getRandomAlphanumeric(int size) {
        return org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(size);
    }

    /**
	 * Escape characters for text appearing as XML data, between tags.
	 * 
	 * <P>
	 * The following characters are replaced with corresponding character
	 * entities :
	 * <table border='1' cellpadding='3' cellspacing='0'>
	 * <tr>
	 * <th>Character</th>
	 * <th>Encoding</th>
	 * </tr>
	 * <tr>
	 * <td><</td>
	 * <td>&lt;</td>
	 * </tr>
	 * <tr>
	 * <td>></td>
	 * <td>&gt;</td>
	 * </tr>
	 * <tr>
	 * <td>&</td>
	 * <td>&amp;</td>
	 * </tr>
	 * <tr>
	 * <td>"</td>
	 * <td>&quot;</td>
	 * </tr>
	 * <tr>
	 * <td>'</td>
	 * <td>&#039;</td>
	 * </tr>
	 * </table>
	 * 
	 * <P>
	 * Note that JSTL's {@code <c:out>} escapes the exact same set of characters
	 * as this method. <span class='highlight'>That is, {@code <c:out>} is good
	 * for escaping to produce valid XML, but not for producing safe
	 * HTML.</span>
	 */
    public String escapeSpecialXmlCharacters(String aText) {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(aText);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == '<') {
                result.append("&lt;");
            } else if (character == '>') {
                result.append("&gt;");
            } else if (character == '\"') {
                result.append("&quot;");
            } else if (character == '\'') {
                result.append("&#039;");
            } else if (character == '&') {
                result.append("&amp;");
            } else {
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }

    /**
	 * for example replaceAll("string containing single quote ' <-- like that.",
	 * "'", "\\\\'")
	 * 
	 * @param original
	 * @param regex
	 * @param replacement
	 * @return
	 */
    public String replaceAllCharacters(String original, String regex, String replacement) {
        String result = null;
        if (original != null) {
            result = original.replaceAll(regex, replacement);
        }
        return result;
    }

    public void validateEmail(String input) throws Exception {
        Pattern p = Pattern.compile("^\\.|^\\@");
        Matcher m = p.matcher(input);
        if (m.find()) System.err.println("Email addresses don't start" + " with dots or @ signs.");
        p = Pattern.compile("^www\\.");
        m = p.matcher(input);
        if (m.find()) {
            System.out.println("Email addresses don't start" + " with \"www.\", only web pages do.");
        }
        p = Pattern.compile("[^A-Za-z0-9\\.\\@_\\-~#]+");
        m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        boolean result = m.find();
        boolean deletedIllegalChars = false;
        while (result) {
            deletedIllegalChars = true;
            m.appendReplacement(sb, "");
            result = m.find();
        }
        m.appendTail(sb);
        input = sb.toString();
        if (deletedIllegalChars) {
            System.out.println("It contained incorrect characters" + " , such as spaces or commas.");
        }
    }

    /**
	 * 
	 * condition to be met: there should be at least one unique vendor per slot;
	 * if more than one unique vendors exist, then go with the first vendor;
	 * 
	 * scenario #5 ven#1 ven#2 ven#3
	 * 
	 * <pre>
	 * 
	 *  		slot1	slot2	slot3
	 *  		col0	col1	col2
	 *  row 0	v1		null	v1
	 *  row 1	v2		v2		null
	 *  row 2	null	v3		null
	 * 			
	 * 			v2		v3		v1
	 * 			
	 *  		
	 *  result should be: 
	 *  	slot 1: v2
	 *  	slot 2: v3
	 *  	slot 3: v1
	 * 
	 * </pre>
	 * 
	 * solve: s1: v1, v3 s2: v1, v2 s3: v2 if s1:v1 then can remaining be unique
	 * i.e. s2: v2 and s3:v2 = no so s1:v1 is false if s1:v3 then can remaining
	 * be unique i.e, s2:v1 and s3: v2 =yes </pre>
	 * 
	 * @param matrixOfPossibleValues
	 * @param rowToProcess
	 * @return
	 * @throws Exception
	 * 
	 * 
	 * usage:
	 * <pre>
	 *		String[][] matrixOfPossibleValues = new String[][] {
					{ "v1", null, "v1" }, { "v2", "v2", null },
					{ null, "v3", null } };
			String[][] vendorTimeslotAssignment = (new multidimenarray())
					.solveMatrixForUniqueValues(matrixOfPossibleValues, 0);
			for (int row = 0; row < vendorTimeslotAssignment.length; row++) {
				for (int col = 0; col < vendorTimeslotAssignment[row].length; col++) {
					System.out.println("[vendorTimeslotAssignment: row(" + row
							+ "), column(" + col + ")]"
							+ vendorTimeslotAssignment[row][col]);
				}
			}
	 * 
	 * </pre>
	 */
    String[][] solveMatrixForUniqueValues(String[][] matrixOfPossibleValues, int rowToProcess) throws UtilityException {
        String[][] matrixOfUniqueValues = new String[matrixOfPossibleValues.length][matrixOfPossibleValues.length];
        int counter = 0;
        boolean rowProcessed = false;
        for (int row = 0; row < matrixOfPossibleValues.length; row++) {
            if (row == rowToProcess) {
                for (int col = 0; col < matrixOfPossibleValues[row].length; col++) {
                    if (matrixOfPossibleValues[row][col] != null) {
                        String[][] truncatedVendorAcceptedTimeslots = truncateTwoDimensionalArray(matrixOfPossibleValues, row, col);
                        try {
                            if (row < matrixOfPossibleValues.length - 1) {
                                String[][] processed = solveMatrixForUniqueValues(truncatedVendorAcceptedTimeslots, row + 1);
                                for (int row1 = 0; row1 < processed.length; row1++) {
                                    for (int col1 = 0; col1 < processed[row1].length; col1++) {
                                        if (processed[row1][col1] != null) {
                                            matrixOfUniqueValues[row1][col1] = processed[row1][col1];
                                        }
                                    }
                                }
                            }
                            matrixOfUniqueValues[row][col] = matrixOfPossibleValues[row][col];
                            return matrixOfUniqueValues;
                        } catch (Exception e) {
                            System.out.println(e.toString() + "--> will try next columns or rows");
                        }
                    }
                }
            }
        }
        throw new UtilityException("Condition of unique value per cell not met.");
    }

    /**
	 * truncate (set to null) the passed in rowToExclude and colToExclude
	 * @param matrixOfPossibleValues
	 * @param rowToExclude
	 * @param colToExclude
	 * @return
	 */
    private String[][] truncateTwoDimensionalArray(String[][] matrixOfPossibleValues, int rowToExclude, int colToExclude) {
        String[][] truncatedArray = new String[matrixOfPossibleValues.length][matrixOfPossibleValues.length];
        for (int row = 0; row < matrixOfPossibleValues.length; row++) {
            if (row != rowToExclude) {
                for (int col = 0; col < matrixOfPossibleValues[row].length; col++) {
                    if (col != colToExclude) {
                        truncatedArray[row][col] = matrixOfPossibleValues[row][col];
                    }
                }
            }
        }
        return truncatedArray;
    }
}
