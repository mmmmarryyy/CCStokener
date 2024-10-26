package uk.ac.dl.dp.core.sessionbeans;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import uk.ac.dl.dp.coreutil.exceptions.SessionException;

public abstract class EJBObject {

    static Logger log = Logger.getLogger(EJBObject.class);

    @PersistenceContext(unitName = "dataportal")
    protected EntityManager em;

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public Object mergeEntity(Object entity) {
        return em.merge(entity);
    }

    public Object persistEntity(Object entity) {
        em.persist(entity);
        return entity;
    }

    public Object refreshEntity(Object entity) {
        em.refresh(entity);
        return entity;
    }

    public void removeEntity(Object entity) {
        em.remove(em.merge(entity));
    }

    @PostConstruct
    public void init() {
        URL url = this.getClass().getResource("/uk/ac/dl/dp/core/messages/facility.properties");
        Properties props = new Properties();
        String facilityLogFile = null;
        try {
            props.load(url.openStream());
            facilityLogFile = props.getProperty("facility.name");
        } catch (Exception mre) {
            facilityLogFile = "ISIS";
            System.out.println("Unable to load props file, setting log as  " + facilityLogFile + "\n" + mre);
        }
        if (new File(System.getProperty("user.home") + File.separator + "." + facilityLogFile + "-dp-core-log4j.xml").exists()) {
            PropertyConfigurator.configure(System.getProperty("user.home") + File.separator + "." + facilityLogFile + "-dp-core-log4j.xml");
        } else {
            PropertyConfigurator.configure(System.getProperty("user.home") + File.separator + "." + facilityLogFile + "-dp-core-log4j.properties");
        }
    }

    @AroundInvoke
    public Object logMethods(InvocationContext ctx) throws Exception {
        Object[] args = ctx.getParameters();
        String className = ctx.getTarget().getClass().getName();
        String methodName = ctx.getMethod().getName();
        String target = className + "." + methodName + "()";
        long start = System.currentTimeMillis();
        StringBuilder builder = new StringBuilder();
        builder.append(className + "." + methodName + "(");
        try {
            int i = 1;
            if (className.indexOf("admin") != -1) {
                log.trace("Admin method called");
                builder.append(target);
            } else if (args != null) {
                for (Object arg : args) {
                    if (arg == null) {
                        log.trace("Cannot pass null into argument " + i + " into: " + className + "." + methodName + "() method.");
                        throw new SessionException("Cannot pass null into argument #" + i + " for this method.");
                    } else if (arg instanceof String && ((String) arg).length() == 0) {
                        log.trace("Cannot pass empty string into argument " + i + " into: " + className + "." + methodName + "() method.");
                        throw new SessionException("Cannot pass empty string into argument #" + i + " for this method.");
                    }
                    if (i == args.length) {
                        builder.append(arg + ")");
                    } else {
                        builder.append(arg + ", ");
                    }
                    i++;
                }
            } else {
                builder.append(target);
            }
            return ctx.proceed();
        } catch (IllegalArgumentException e) {
            throw new SessionException(e.getMessage());
        } catch (Exception e) {
            throw e;
        } finally {
            if (methodName.contains("isFinished") || methodName.contains("getCompleted") || methodName.contains("login")) {
            } else {
                long time = System.currentTimeMillis() - start;
                log.trace("Exiting " + builder + " , This method takes " + time / 1000f + "s to execute");
                log.trace("\n");
            }
        }
    }
}
