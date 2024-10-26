package com.samaxes.stripes.test;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Logger;
import org.jboss.arquillian.api.ArquillianResource;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.samaxes.stripes.action.FooActionBean;
import com.samaxes.stripes.business.FooService;
import com.samaxes.stripes.business.FooServiceBean;
import com.samaxes.stripes.ejb3.EJBBean;
import com.samaxes.stripes.ejb3.EJBInterceptor;

/**
 * Example service test class.
 * 
 * @author Samuel Santos
 * @version $Revision$
 */
public class FooServiceIT extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(FooServiceIT.class.getName());

    @Deployment(testable = false)
    public static EnterpriseArchive createDeployment() throws IOException {
        final Collection<GenericArchive> stripesDependency = DependencyResolvers.use(MavenDependencyResolver.class).loadReposFromPom("pom.xml").artifact("net.sourceforge.stripes:stripes").exclusion("*:*").resolveAs(GenericArchive.class);
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar").addClasses(FooService.class, FooServiceBean.class);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "foo.war").addClasses(EJBBean.class, EJBInterceptor.class, FooActionBean.class).addAsLibraries(stripesDependency).addAsResource("index.jsp").setWebXML("web.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "stripes-ejb3.ear").addAsModule(ejbJar).addAsModule(war);
        LOGGER.info(ear.toString(true));
        exportArchive(ear);
        return ear;
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void shouldGreetUser(@ArquillianResource URL baseURL) throws IOException {
        final String name = "Earthlings";
        final URL url = new URL(baseURL, "Foo.action");
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        LOGGER.info("Returned response: " + builder.toString());
        Assert.assertEquals(builder.toString(), FooService.GREETING + name);
    }

    private static void exportArchive(Archive<?> archive) throws IOException {
        OutputStream out = new FileOutputStream("target/" + archive.getName() + ".zip");
        archive.as(ZipExporter.class).exportTo(out);
        out.close();
    }
}
