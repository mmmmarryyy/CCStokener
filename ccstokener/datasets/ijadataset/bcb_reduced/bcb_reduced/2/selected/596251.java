package com.volantis.mcs.runtime.project;

import com.volantis.cache.group.GroupMock;
import com.volantis.mcs.policies.variants.VariantType;
import com.volantis.mcs.project.InternalProjectFactory;
import com.volantis.mcs.project.ProjectConfigurationReader;
import com.volantis.mcs.project.remote.RemotePolicySource;
import com.volantis.mcs.project.remote.RemotePolicySourceMock;
import com.volantis.mcs.project.xml.XMLPolicySourceMock;
import com.volantis.mcs.runtime.ExternalPathToInternalURLMapperMock;
import com.volantis.mcs.runtime.RuntimeProject;
import com.volantis.mcs.runtime.configuration.project.RuntimeProjectConfiguration;
import com.volantis.mcs.runtime.policies.cache.CacheControlConstraintsMap;
import com.volantis.mcs.runtime.policies.cache.PolicyCacheMock;
import com.volantis.mcs.runtime.policies.cache.SeparateCacheControlConstraintsMap;
import com.volantis.synergetics.testtools.TestCaseAbstract;
import java.io.InputStream;
import java.net.URL;

/**
 * Test cases for {@link RuntimeProjectConfiguratorImpl}.
 */
public class RuntimeProjectConfiguratorTestCase extends TestCaseAbstract {

    private CacheControlConstraintsMap localConstraintsMap;

    private CacheControlConstraintsMap remoteConstraintsMap;

    private ExternalPathToInternalURLMapperMock urlMapperMock;

    private PolicyCacheMock policyCacheMock;

    private RuntimePolicySourceFactoryMock policySourceFactoryMock;

    protected void setUp() throws Exception {
        super.setUp();
        urlMapperMock = new ExternalPathToInternalURLMapperMock("urlMapperMock", expectations);
        remoteConstraintsMap = new SeparateCacheControlConstraintsMap();
        localConstraintsMap = new SeparateCacheControlConstraintsMap();
        policyCacheMock = new PolicyCacheMock("policyCacheMock", expectations);
        policySourceFactoryMock = new RuntimePolicySourceFactoryMock("policySourceFactoryMock", expectations);
    }

    /**
     * Make sure that the global project has the correct characteristics.
     */
    public void testGlobal() throws Exception {
        final RemotePolicySourceMock remotePolicySourceMock = new RemotePolicySourceMock("remotePolicySourceMock", expectations);
        policySourceFactoryMock.expects.createRemotePolicySource("").returns(remotePolicySourceMock);
        RuntimeProjectConfigurator configurator = new RuntimeProjectConfiguratorImpl(localConstraintsMap, remoteConstraintsMap, urlMapperMock, policyCacheMock);
        RuntimeProject project = configurator.createGlobalProject(policySourceFactoryMock);
        assertNotNull(project.getPolicySource());
        assertTrue("Must be remote", project.isRemote());
        assertTrue("Must contain orphans", project.getContainsOrphans());
        assertNull(project.getAssetsBaseURL());
        assertNull(project.getGeneratedResourcesBaseDir());
        assertNull(project.getPrefixURL(VariantType.AUDIO));
        assertNull(project.getPrefixURL(VariantType.IMAGE));
        assertNull(project.getPrefixURL(VariantType.SCRIPT));
        assertNull(project.getPrefixURL(VariantType.TEXT));
        assertNull(project.getPrefixURL(VariantType.VIDEO));
    }

    /**
     * Ensure that building a project works properly.,
     */
    public void testBuildProject() throws Exception {
        ProjectConfigurationReader reader = new ProjectConfigurationReader();
        URL url = getClassRelativeResourceURL("a/mcs-project.xml");
        InputStream stream = url.openStream();
        RuntimeProjectConfiguration configuration;
        try {
            configuration = reader.readProject(stream, url.toExternalForm());
        } finally {
            stream.close();
        }
        final RuntimePolicySourceFactoryMock policySourceFactoryMock = new RuntimePolicySourceFactoryMock("policySourceFactoryMock", expectations);
        final XMLPolicySourceMock xmlPolicySourceMock = new XMLPolicySourceMock("xmlPolicySourceMock", expectations);
        final GroupMock groupMock = new GroupMock("groupMock", expectations);
        policyCacheMock.expects.getLocalDefaultGroup().returns(groupMock);
        urlMapperMock.expects.mapInternalURLToExternalPath(getClassRelativeResourceURLAsString("a/")).returns("/project/a/mcs-project.xml");
        String directory = getClassRelativeResourceURLAsString("a/").substring("file:".length());
        policySourceFactoryMock.expects.createXMLPolicySource(directory).returns(xmlPolicySourceMock);
        RuntimeProjectConfigurator configurator = new RuntimeProjectConfiguratorImpl(localConstraintsMap, remoteConstraintsMap, urlMapperMock, policyCacheMock);
        RuntimeProject project = configurator.buildProject(configuration, null, policySourceFactoryMock);
        assertSame(xmlPolicySourceMock, project.getPolicySource());
        assertFalse(project.isRemote());
        assertSame(localConstraintsMap, project.getCacheControlConstraintsMap());
        assertSame(groupMock, project.getCacheGroup());
    }
}
