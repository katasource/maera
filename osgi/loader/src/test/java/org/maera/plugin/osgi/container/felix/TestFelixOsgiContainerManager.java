package org.maera.plugin.osgi.container.felix;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.osgi.container.OsgiContainerException;
import org.maera.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import org.maera.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import org.maera.plugin.test.PluginJarBuilder;
import org.maera.plugin.test.PluginTestUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class TestFelixOsgiContainerManager extends TestCase {
    private File tmpdir;
    private FelixOsgiContainerManager felix;
    private URL frameworkBundlesUrl = getClass().getResource("/nothing.zip");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        tmpdir = PluginTestUtils.createTempDirectory(TestFelixOsgiContainerManager.class);
        felix = new FelixOsgiContainerManager(frameworkBundlesUrl, new DefaultOsgiPersistentCache(tmpdir), new DefaultPackageScannerConfiguration(),
                null, new DefaultPluginEventManager());
    }

    @Override
    public void tearDown() throws Exception {
        if (felix != null && felix.isRunning()) {
            for (Bundle bundle : felix.getBundles()) {
                try {
                    bundle.uninstall();
                }
                catch (BundleException ignored) {
                }
            }
        }
        if (felix != null)
            felix.stop();
        felix = null;
        tmpdir = null;
        super.tearDown();
    }

    public void testDetectXercesOverride() {
        felix.detectXercesOverride("foo.bar,baz.jim");
        felix.detectXercesOverride("foo.bar,org.apache.xerces.util;version=\"1.0\",baz.jim");
        felix.detectXercesOverride("foo.bar,org.apache.xerces.util;version=\"1.0\"");
        felix.detectXercesOverride("foo.bar,repackaged.org.apache.xerces.util,bar.baz");


        try {
            felix.detectXercesOverride("foo.bar,org.apache.xerces.util");
            fail("Should fail validation");
        }
        catch (OsgiContainerException ex) {
            // should fail
        }

        try {
            felix.detectXercesOverride("org.apache.xerces.util");
            fail("Should fail validation");
        }
        catch (OsgiContainerException ex) {
            // should fail
        }

        try {
            felix.detectXercesOverride("org.apache.xerces.util,bar.baz");
            fail("Should fail validation");
        }
        catch (OsgiContainerException ex) {
            // should fail
        }

    }

    public void testDeleteDirectory() throws IOException {
        File dir = new File(tmpdir, "base");
        dir.mkdir();
        File subdir = new File(dir, "subdir");
        subdir.mkdir();
        File kid = File.createTempFile("foo", "bar", subdir);

        FileUtils.deleteDirectory(dir);
        assertTrue(!kid.exists());
        assertTrue(!subdir.exists());
        assertTrue(!dir.exists());
    }

    public void testStartStop() {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.startsWith("felix");
            }
        };
        int filesNamedFelix = tmpdir.listFiles(filter).length;
        felix.start();
        assertTrue(felix.isRunning());
        assertEquals(1, felix.getBundles().length);
        felix.stop();
        assertEquals(filesNamedFelix, tmpdir.listFiles(filter).length);
    }

    public void testInstallBundle() throws URISyntaxException {
        felix.start();
        assertEquals(1, felix.getBundles().length);
        File jar = new File(getClass().getResource("/myapp-1.0.jar").toURI());
        felix.installBundle(jar);
        assertEquals(2, felix.getBundles().length);
    }

    public void testBootDelegation() throws Exception {
        // Server class extends JUnit TestCase class, which is not available to the bundle
        File pluginServer = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.server\n" +
                        "Bundle-ManifestVersion: 2\n" +
                        "Export-Package: my.server\n")
                .addJava("my.server.ServerClass", "package my.server; public class ServerClass extends junit.framework.TestCase {}")
                .build();

        // Client is necessary to load the server class in a Felix ContentClassLoader, to avoid the hack in Felix's
        // R4SearchPolicyCore (approx. line 591) which will use parent delegation if a class cannot be found
        // and the calling classloader is not a ContentClassLoader.
        File pluginClient = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.client\n" +
                        "Bundle-ManifestVersion: 2\n" +
                        "Import-Package: my.server\n")
                .addJava("my.client.ClientClass", "package my.client; public class ClientClass {" +
                        "public ClientClass() throws ClassNotFoundException {" +
                        "getClass().getClassLoader().loadClass(\"my.server.ServerClass\");" +
                        "}}")
                .build();

        felix.start();
        Bundle serverBundle = felix.installBundle(pluginServer);
        serverBundle.start();
        Bundle clientBundle = felix.installBundle(pluginClient);
        clientBundle.start();
        try {
            clientBundle.loadClass("my.client.ClientClass").newInstance();
            fail("Expected exception: NoClassDefFoundError for junit.framework.TestCase");
        }
        catch (NoClassDefFoundError expected) {
        }
        felix.stop();

        // This system property exposes the JUnit TestCase class from the parent classloader to the bundle
        System.setProperty("atlassian.org.osgi.framework.bootdelegation", "junit.framework,junit.framework.*");
        try {
            felix.start();
            serverBundle = felix.installBundle(pluginServer);
            serverBundle.start();
            clientBundle = felix.installBundle(pluginClient);
            clientBundle.start();
            clientBundle.loadClass("my.client.ClientClass").newInstance();
            felix.stop();
        }
        finally {
            System.clearProperty("atlassian.org.osgi.framework.bootdelegation");
        }
    }

    public void testInstallBundleTwice() throws URISyntaxException, IOException, BundleException {
        File plugin = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-ManifestVersion: 2\n")
                .addResource("foo.txt", "foo")
                .build();

        File pluginUpdate = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-ManifestVersion: 2\n")
                .addResource("bar.txt", "bar")
                .build();

        felix.start();
        assertEquals(1, felix.getBundles().length);
        Bundle bundle = felix.installBundle(plugin);
        assertEquals(2, felix.getBundles().length);
        assertEquals("my.foo.symbolicName", bundle.getSymbolicName());
        assertEquals("1.0", bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        assertEquals(Bundle.INSTALLED, bundle.getState());
        assertNotNull(bundle.getResource("foo.txt"));
        assertNull(bundle.getResource("bar.txt"));
        bundle.start();
        assertEquals(Bundle.ACTIVE, bundle.getState());
        Bundle bundleUpdate = felix.installBundle(pluginUpdate);
        assertEquals(2, felix.getBundles().length);
        assertEquals(Bundle.INSTALLED, bundleUpdate.getState());
        bundleUpdate.start();
        assertEquals(Bundle.ACTIVE, bundleUpdate.getState());
        //assertNull(bundleUpdate.getResource("foo.txt"));
        assertNotNull(bundleUpdate.getResource("bar.txt"));
    }

    public void testInstallBundleTwiceDifferentSymbolicNames() throws URISyntaxException, IOException, BundleException {
        File plugin = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.foo\n" +
                        "Atlassian-Plugin-Key: my.foo.symbolicName\n" +
                        "Bundle-ManifestVersion: 2\n")
                .addResource("foo.txt", "foo")
                .build();

        File pluginUpdate = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-Version: 1.0\n" +
                        "Atlassian-Plugin-Key: my.foo.symbolicName\n" +
                        "Bundle-SymbolicName: my.bar\n" +
                        "Bundle-ManifestVersion: 2\n")
                .addResource("bar.txt", "bar")
                .build();

        felix.start();
        assertEquals(1, felix.getBundles().length);
        Bundle bundle = felix.installBundle(plugin);
        assertEquals(2, felix.getBundles().length);
        assertEquals("1.0", bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        assertEquals(Bundle.INSTALLED, bundle.getState());
        assertNotNull(bundle.getResource("foo.txt"));
        assertNull(bundle.getResource("bar.txt"));
        bundle.start();
        assertEquals(Bundle.ACTIVE, bundle.getState());
        Bundle bundleUpdate = felix.installBundle(pluginUpdate);
        assertEquals(2, felix.getBundles().length);
        assertEquals(Bundle.INSTALLED, bundleUpdate.getState());
        bundleUpdate.start();
        assertEquals(Bundle.ACTIVE, bundleUpdate.getState());
        //assertNull(bundleUpdate.getResource("foo.txt"));
        assertNotNull(bundleUpdate.getResource("bar.txt"));
    }

    public void testInstallFailure() throws Exception {
        File plugin = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Import-Package: foo.missing.package\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-ManifestVersion: 2\n")
                .build();
        felix.start();

        Bundle bundle = felix.installBundle(plugin);
        try {
            bundle.loadClass("foo.bar");
            fail("Should have thrown exception");
        } catch (ClassNotFoundException ex) {
            // no worries
        }
    }
}
