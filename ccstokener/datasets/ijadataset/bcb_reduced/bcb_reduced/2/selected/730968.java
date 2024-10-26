package shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Praca
 */
public class Tools {

    public static Object convertObjectToHashmap(Object object) {
        if (object instanceof String || object instanceof Date || object instanceof Boolean || object instanceof Double || object instanceof Integer || object instanceof Long || object instanceof Map) {
            return object;
        }
        if (object instanceof List) {
            ArrayList list = new ArrayList();
            List objectList = (List) object;
            for (int i = 0; i < objectList.size(); i++) {
                Object object1 = objectList.get(i);
                list.add(convertObjectToHashmap(object1));
            }
            return list;
        }
        HashMap newObject = new HashMap();
        Method[] methods = object.getClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!isGetter(method)) {
                continue;
            }
            try {
                Object returns = method.invoke(object, new Object[0]);
                newObject.put(getFieldNameByGetterMethodName(method), convertObjectToHashmap(returns));
            } catch (IllegalAccessException illegalAccessException) {
                illegalAccessException.printStackTrace();
            } catch (IllegalArgumentException illegalArgumentException) {
                illegalArgumentException.printStackTrace();
            } catch (InvocationTargetException invocationTargetException) {
                invocationTargetException.printStackTrace();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return newObject;
    }

    public static <V> V convertHashmapToObject(Map parameters, Class<V> objectClass) throws Exception {
        V instance = objectClass.newInstance();
        Method[] methods = instance.getClass().getMethods();
        for (Object key : parameters.keySet()) {
            Object value = parameters.get(key);
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (!method.getName().equals(getMethodSetterNameByFieldName(key + ""))) {
                    continue;
                }
                if (method.getParameterTypes().length != 1) {
                    continue;
                }
                Class parameterType = method.getParameterTypes()[0];
                if (parameterType.isAssignableFrom(String.class) || parameterType.isAssignableFrom(Date.class) || parameterType.isAssignableFrom(Boolean.class) || parameterType.isAssignableFrom(Double.class) || parameterType.isAssignableFrom(Integer.class) || parameterType.isAssignableFrom(Long.class)) {
                    method.invoke(instance, value);
                    continue;
                }
                if (value instanceof Map) {
                    if (parameterType.isAssignableFrom(HashMap.class)) {
                        method.invoke(instance, value);
                        continue;
                    }
                    method.invoke(instance, convertHashmapToObject((Map) value, parameterType));
                }
                if (value instanceof List) {
                    ArrayList list = new ArrayList();
                    List valueList = (List) value;
                    Annotation[] annos = method.getAnnotations();
                    Class listClass = null;
                    for (Annotation a : annos) {
                        if (a instanceof ListType) {
                            listClass = ((ListType) a).arrayClass();
                            break;
                        }
                    }
                    if (listClass == null) {
                        throw new Exception("Annotation " + ListType.class + " is missing for method " + method + " for class " + instance.getClass());
                    }
                    for (int j = 0; j < valueList.size(); j++) {
                        Object object = valueList.get(j);
                        if (object instanceof Map) {
                            list.add(convertHashmapToObject((Map) object, listClass));
                        }
                    }
                    method.invoke(instance, list);
                }
            }
        }
        return instance;
    }

    public static boolean isGetter(Method method) {
        if (!method.getName().startsWith("get") && !method.getName().startsWith("is")) {
            return false;
        }
        if (method.getParameterTypes().length != 0) {
            return false;
        }
        if (void.class.equals(method.getReturnType())) {
            return false;
        }
        return true;
    }

    public static boolean isSetter(Method method) {
        if (!method.getName().startsWith("set")) {
            return false;
        }
        if (method.getParameterTypes().length != 1) {
            return false;
        }
        return true;
    }

    private static Object getFieldNameByGetterMethodName(Method method) throws Exception {
        if (!isGetter(method)) {
            throw new Exception("Method: " + method + " is not getter");
        }
        String methodName = method.getName();
        if (methodName.startsWith("is")) {
            String getterName = methodName.substring(2);
            return lowerCaseFirstLetter(getterName);
        }
        if (methodName.startsWith("get")) {
            String getterName = methodName.substring(3);
            return lowerCaseFirstLetter(getterName);
        }
        return method.getName();
    }

    private static String getMethodSetterNameByFieldName(String fieldName) throws Exception {
        return "set" + upperCaseFirstLetter(fieldName);
    }

    public static String lowerCaseFirstLetter(String line) {
        String newLine = "";
        for (int i = 0; i < line.length(); i++) {
            newLine += line.charAt(i);
        }
        return newLine;
    }

    public static String upperCaseFirstLetter(String line) {
        String newLine = "";
        for (int i = 0; i < line.length(); i++) {
            if (i == 0) {
                newLine += (line.charAt(0) + "").toUpperCase();
            } else {
                newLine += line.charAt(i);
            }
        }
        return newLine;
    }

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static <T extends HashMap> T translateHashmapIntoBean(HashMap source, T destiny) {
        for (Object key : source.keySet()) {
            destiny.put(key, source.get(key));
        }
        return destiny;
    }

    public static String convertDateToStringWithSeconds(Date date) {
        return sdf.format(date);
    }

    public static String returnNotNullString(String string) {
        if (string == null) {
            return "";
        }
        return string;
    }

    public static Double convertBigDecimal2Double(Object doubleValue) {
        if (doubleValue == null) {
            return null;
        }
        if (doubleValue instanceof Double) {
            return (Double) doubleValue;
        }
        if (doubleValue instanceof BigDecimal) {
            return ((BigDecimal) doubleValue).doubleValue();
        }
        return null;
    }

    public static String convertStackTrace2String(StackTraceElement[] stackTrace) {
        String out = "";
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement stackTraceElement = stackTrace[i];
            out += stackTraceElement.toString() + "\n";
            break;
        }
        return out;
    }

    public static String convertException2String(Exception exception) {
        String out = exception.getMessage() + " || \n";
        StackTraceElement[] stackTrace = exception.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement stackTraceElement = stackTrace[i];
            out += stackTraceElement.toString() + "\n";
            break;
        }
        return out;
    }

    public void sendPostRequest(String urlAddress, String data) {
        try {
            URL url = new URL(urlAddress);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data);
            writer.flush();
            StringBuffer answer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                answer.append(line);
            }
            writer.close();
            reader.close();
            System.out.println("Answer:" + answer.toString());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String displayHtml(ArrayList in) {
        return displayHtml(in, 0);
    }

    public static String displayRaw(ArrayList in) {
        return displayRaw(in, 0);
    }

    public static String displayHtml(ArrayList in, int level) {
        String out = "";
        for (int i = 0; i < in.size(); i++) {
            Object object = in.get(i);
            if (object instanceof ArrayList) {
                out += displayHtml((ArrayList) object, level + 1) + "";
            } else {
                out += getSeparator(level) + (object + "").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;") + "<br>";
            }
        }
        return out;
    }

    public static String displayRaw(ArrayList in, int level) {
        String out = "";
        for (int i = 0; i < in.size(); i++) {
            Object object = in.get(i);
            if (object instanceof ArrayList) {
                out += displayRaw((ArrayList) object, level + 1) + "";
            } else {
                out += getSeparator(level) + object + "<br " + level + ">\n";
            }
        }
        return out;
    }

    public static final String SEPARATOR = "-";

    public static String getSeparator(int level) {
        String out = "";
        for (int i = 0; i < level * 4; i++) {
            out += SEPARATOR + "";
        }
        return out + " ";
    }
}
