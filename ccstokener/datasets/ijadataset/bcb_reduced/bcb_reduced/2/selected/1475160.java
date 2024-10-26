package thirdparty.imperius;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.imperius.javaspl.Java_SPLPolicyRuleProvider;
import org.apache.imperius.spl.parser.exceptions.SPLException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestImperius {

    private Java_SPLPolicyRuleProvider jspl;

    @Before
    public void setup() throws SPLException {
        jspl = Java_SPLPolicyRuleProvider.getInstance();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void test1() {
        String policyName = "SamplePolicy";
        try {
            createPolicy(policyName);
            Map<String, Object> objMap = new HashMap<String, Object>();
            objMap.put("bean", new SimpleBean());
            Object result = jspl.executePolicy(policyName, objMap);
            System.out.println("Result is " + result);
            deletePolicy(policyName);
        } catch (SPLException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void test2() {
        String policyName = "TwoObjectsPolicy";
        try {
            createPolicy(policyName);
            Map<String, Object> objMap = new HashMap<String, Object>();
            objMap.put("bean1", new SimpleBean());
            objMap.put("bean2", new SimpleBean());
            Object result = jspl.executePolicy(policyName, objMap);
            System.out.println("Result is " + result);
            deletePolicy(policyName);
        } catch (SPLException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void createPolicy(String policyName) throws SPLException {
        URL url = getClass().getResource(policyName + ".spl");
        StringBuffer contents = new StringBuffer();
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
            input.close();
            System.out.println(policyName);
            System.out.println(contents.toString());
            boolean createReturn = jspl.createPolicy(policyName, contents.toString());
            System.out.println("Policy Created : " + policyName + " - " + createReturn);
            System.out.println("");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void deletePolicy(String policyName) {
        try {
            jspl.deletePolicy(policyName);
        } catch (Exception e) {
        }
    }

    public static void main(String args[]) {
        File dir1 = new File(".");
        File dir2 = new File("..");
        try {
            System.out.println("Current dir : " + dir1.getCanonicalPath());
            System.out.println("Parent  dir : " + dir2.getCanonicalPath());
            System.out.println("user.dir : " + System.getProperty("user.dir"));
            System.out.println("classpath : " + System.getProperty("java.class.path"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
