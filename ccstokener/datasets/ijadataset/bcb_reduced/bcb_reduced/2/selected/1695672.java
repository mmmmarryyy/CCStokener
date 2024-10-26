package org.cloudlet.web.test;

import com.google.appengine.api.rdbms.dev.LocalRdbmsService.ServerType;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalRdbmsServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.testing.TearDown;
import com.google.common.testing.TearDownAccepter;
import com.google.guiceberry.GuiceBerryEnvMain;
import com.google.guiceberry.TestWrapper;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import java.net.URL;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class TestModule extends AbstractModule {

    private static final class PersistServiceStarter implements GuiceBerryEnvMain {

        @Inject
        private LocalServiceTestHelper helper;

        @Inject
        private PersistService persistService;

        @Override
        public void run() {
            helper.setUp();
            persistService.start();
            helper.tearDown();
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    protected void configure() {
        LogManager logManager = LogManager.getLogManager();
        try {
            URL url = LoggingUtil.searchLoggingFile();
            logManager.readConfiguration(url.openStream());
            logger.config("Config logging use " + url);
        } catch (Exception e) {
            System.err.println("TestingModule: Load logging configuration failed");
            System.err.println("" + e);
        }
        bind(GuiceBerryEnvMain.class).to(PersistServiceStarter.class);
    }

    @Provides
    @Singleton
    LocalServiceTestHelper localServiceTestHelperProvider() {
        LocalRdbmsServiceTestConfig localRdbmsServiceTestConfig = new LocalRdbmsServiceTestConfig();
        localRdbmsServiceTestConfig.setServerType(ServerType.HOSTED);
        LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalMemcacheServiceTestConfig(), localRdbmsServiceTestConfig);
        return helper;
    }

    @Provides
    TestWrapper testWrapperProvider(final TearDownAccepter tearDownAccepter, final LocalServiceTestHelper helper, final UnitOfWork unitOfWork) {
        return new TestWrapper() {

            @Override
            public void toRunBeforeTest() {
                tearDownAccepter.addTearDown(new TearDown() {

                    @Override
                    public void tearDown() throws Exception {
                        unitOfWork.end();
                        helper.tearDown();
                    }
                });
                helper.setUp();
                unitOfWork.begin();
            }
        };
    }
}
