package org.xmlcml.cml.element.test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import nu.xom.Elements;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.cml.base.CMLRuntimeException;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.element.CMLArg;
import org.xmlcml.cml.element.CMLAtom;
import org.xmlcml.cml.element.CMLBond;
import org.xmlcml.cml.element.CMLBuilder;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.cml.element.RefAttribute;
import org.xmlcml.euclid.test.StringTest;

/**
 * test CMLArg.
 *
 * @author pm286
 *
 */
public class CMLArgTest extends AbstractTest {

    /**
	 * Test method for 'org.xmlcml.cml.element.CMLArg.copy()'
	 */
    @Test
    @Ignore
    public void testCopy() {
    }

    /**
	 * Test method for 'org.xmlcml.cml.element.CMLArg.CMLArg(String, int)'
	 */
    @Test
    @Ignore
    public void testCMLArgStringInt() {
        CMLArg arg = new CMLArg("foo", 23);
        Assert.assertEquals("arg int", "foo", arg.getName());
        Assert.assertEquals("arg int", 23, arg.getInteger());
    }

    /**
	 * Test method for 'org.xmlcml.cml.element.CMLArg.addArg(CMLElement, CMLArg,
	 * int)'
	 */
    @Test
    @Ignore
    public void testAddArg() {
    }

    /**
	 * Test method for
	 * 'org.xmlcml.cml.element.CMLArg.substituteNameByValue(CMLElement)'
	 *
	 * @exception Exception
	 */
    @Test
    @Ignore
    public void testSubstituteNameByValueCMLElement() throws Exception {
        String cmlS = "" + "<cml " + CML_XMLNS + " id='a_i_'>" + "  <molecule id='mol_i_' title='orig mol'>" + "    <arg name='i' substitute='.//@*'>" + "      <scalar dataType='xsd:integer'>42</scalar>" + "    </arg>" + "    <atomArray>" + "      <atom id='m_i__a1'/>" + "      <atom id='m_i__a2'/>" + "    </atomArray>" + "  </molecule>" + "</cml>" + "";
        CMLCml cml = (CMLCml) parseValidString(cmlS);
        Assert.assertEquals("untouched id", "a_i_", cml.getId());
        Assert.assertEquals("child count", 1, cml.getChildElements().size());
        CMLMolecule mol = (CMLMolecule) cml.getChildCMLElements(CMLMolecule.TAG).get(0);
        Assert.assertEquals("mol id", "mol_i_", mol.getId());
        List<CMLAtom> atoms = mol.getAtoms();
        Assert.assertEquals("atom count", 2, atoms.size());
        Assert.assertEquals("atom id", "m_i__a1", atoms.get(0).getId());
        Assert.assertEquals("atom id", "m_i__a2", atoms.get(1).getId());
        CMLArg.substituteNameByValue(mol);
        Assert.assertEquals("untouched id", "a_i_", cml.getId());
        Assert.assertEquals("mol id", "mol42", mol.getId());
        Assert.assertEquals("atom count", 2, atoms.size());
        Assert.assertEquals("atom id", "m42_a1", atoms.get(0).getId());
        Assert.assertEquals("atom id", "m42_a2", atoms.get(1).getId());
        URL url = CMLUtil.getResource(SIMPLE_RESOURCE + File.separator + "arg1.xml");
        CMLCml arg1Cml = null;
        InputStream in = null;
        try {
            in = url.openStream();
            arg1Cml = (CMLCml) new CMLBuilder().build(in).getRootElement();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CMLRuntimeException("EXC" + e);
        } finally {
            in.close();
        }
        CMLArg.substituteNameByValue(arg1Cml);
        CMLMolecule arg1Mol = (CMLMolecule) arg1Cml.getChildCMLElements(CMLMolecule.TAG).get(0);
        Assert.assertEquals("untouched id", "a_i_", cml.getId());
        Assert.assertEquals("mol id", "oh42", arg1Mol.getId());
        atoms = arg1Mol.getAtoms();
        Assert.assertEquals("atom count", 2, atoms.size());
        Assert.assertEquals("atom id", "oh42_a1", atoms.get(0).getId());
        Assert.assertEquals("atom id", "oh42_r1", atoms.get(1).getId());
        List<CMLBond> bonds = arg1Mol.getBonds();
        Assert.assertEquals("bond count", 2, atoms.size());
        Assert.assertEquals("bond id", "b_oh42_a1_oh42_r1", bonds.get(0).getId());
        StringTest.assertEquals("bond atomRefs2", new String[] { "oh42_a1", "oh42_r1" }, bonds.get(0).getAtomRefs2());
    }

    /**
	 * Test method for 'org.xmlcml.cml.element.CMLArg.eval(List<CMLArg>)'
	 *
	 * @exception Exception
	 */
    @Test
    @Ignore
    public void testEval() throws Exception {
        String cmlS = "" + "<cml " + CML_XMLNS + " id='a_i_'>" + "  <molecule id='mol_i_' title='orig mol'>" + "    <arg name='i' substitute='.//@*'>" + "      <scalar dataType='xsd:integer'>42</scalar>" + "    </arg>" + "    <arg name='j' substitute='.//@*' eval='_i_+100'>" + "      <scalar dataType='xsd:integer'/>" + "    </arg>" + "    <atomArray>" + "      <atom id='m_j__a1'/>" + "      <atom id='m_j__a2'/>" + "    </atomArray>" + "  </molecule>" + "</cml>" + "";
        CMLCml cml = (CMLCml) parseValidString(cmlS);
        Assert.assertEquals("untouched id", "a_i_", cml.getId());
        Assert.assertEquals("child count", 1, cml.getChildElements().size());
        CMLMolecule mol = (CMLMolecule) cml.getChildCMLElements(CMLMolecule.TAG).get(0);
        Assert.assertEquals("mol id", "mol_i_", mol.getId());
        List<CMLAtom> atoms = mol.getAtoms();
        Assert.assertEquals("atom count", 2, atoms.size());
        Assert.assertEquals("atom id", "m_j__a1", atoms.get(0).getId());
        Assert.assertEquals("atom id", "m_j__a2", atoms.get(1).getId());
        CMLUtil.output("----------------------------");
        CMLArg.substituteNameByValue(mol);
        Assert.assertEquals("untouched id", "a_i_", cml.getId());
        Assert.assertEquals("mol id", "mol42", mol.getId());
        Assert.assertEquals("atom count", 2, atoms.size());
        Assert.assertEquals("atom id", "m142_a1", atoms.get(0).getId());
        Assert.assertEquals("atom id", "m142_a2", atoms.get(1).getId());
    }

    /**
	 * Test method for 'org.xmlcml.cml.element.CMLArg.getInteger()'
	 */
    @Test
    @Ignore
    public void testGetInteger() {
    }

    /**
	 * Test method for arg and repeat'
	 *
	 * @exception Exception
	 */
    @Test
    @Ignore
    public void testArgAndRepeat() throws Exception {
        CMLCml peo0Cml = null;
        try {
            peo0Cml = (CMLCml) new CMLBuilder().build(CMLUtil.getInputStreamFromResource(EXPERIMENTAL_RESOURCE + File.separator + "peo0.xml")).getRootElement();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CMLRuntimeException("EXC" + e);
        }
        Elements peo0Mols = peo0Cml.getChildCMLElements(CMLMolecule.TAG);
        CMLMolecule peo0Mol = (CMLMolecule) peo0Mols.get(3);
        peo0Mol.debug();
        CMLArg.substituteNameByValue(peo0Mol);
        peo0Cml.debug();
        RefAttribute.process(peo0Mol);
        peo0Mol.debug();
    }

    /**
	 * Test method for arg and repeat'
	 *
	 * @exception Exception
	 */
    @Test
    @Ignore
    public void testArg1() throws Exception {
        CMLCml peo1Cml = null;
        try {
            peo1Cml = (CMLCml) new CMLBuilder().build(CMLUtil.getInputStreamFromResource(EXPERIMENTAL_RESOURCE + S_SLASH + "peo1.xml")).getRootElement();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CMLRuntimeException("EXC" + e);
        }
        Elements peo1Mols = peo1Cml.getChildCMLElements(CMLMolecule.TAG);
        CMLMolecule peo1Mol = (CMLMolecule) peo1Mols.get(1);
        CMLArg.substituteNameByValue(peo1Mol);
        RefAttribute.process(peo1Mol);
    }

    /**
	 * Test method for arg and repeat'
	 *
	 * @exception Exception
	 */
    @Test
    @Ignore
    public void testArg2() throws Exception {
        System.out.println("=========start peo2 ============");
        CMLCml peo2Cml = null;
        InputStream in = null;
        try {
            in = CMLUtil.getInputStreamFromResource(EXPERIMENTAL_RESOURCE + U_S + "peo2.xml");
            peo2Cml = (CMLCml) new CMLBuilder().build(in).getRootElement();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CMLRuntimeException("EXC" + e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        Elements peo2Mols = peo2Cml.getChildCMLElements(CMLMolecule.TAG);
        CMLMolecule peo2Mol = (CMLMolecule) peo2Mols.get(1);
        System.out.println("================== cml ==============================");
        peo2Mol.debug();
        System.out.println("================== substitute arg (and repeat) ==============================");
        CMLArg.substituteNameByValue(peo2Mol);
        System.out.println("================== substituted ==============================");
        peo2Cml.debug();
        System.out.println("=================== process ref =============================");
        RefAttribute.process(peo2Mol);
        System.out.println("================== processed ref ==============================");
        peo2Mol.debug();
        System.out.println("=========end peo2 ============");
    }
}
