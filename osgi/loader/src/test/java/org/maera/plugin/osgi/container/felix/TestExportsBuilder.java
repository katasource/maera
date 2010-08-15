package org.maera.plugin.osgi.container.felix;

import junit.framework.TestCase;
import org.maera.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.osgi.hostcomponents.impl.MockRegistration;
import org.twdata.pkgscanner.ExportPackage;

import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.servlet.ServletContext;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestExportsBuilder extends TestCase {
    private ExportsBuilder builder;

    @Override
    public void setUp() throws Exception {
        builder = new ExportsBuilder();
    }

    @Override
    public void tearDown() throws Exception {
        builder = null;
    }

    public void testDetermineExports() {
        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration("0.0");

        String exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), config);
        assertFalse(exports.contains(",,"));
    }

    public void testConstructAutoExports() {
        List<ExportPackage> exports = new ArrayList<ExportPackage>();
        exports.add(new ExportPackage("foo.bar", "1.0", new File("/whatever/foobar-1.0.jar")));
        exports.add(new ExportPackage("foo.bar", "1.0-asdf-asdf", new File("/whatever/foobar-1.0-asdf-asdf.jar")));
        StringBuilder sb = new StringBuilder();
        builder.constructAutoExports(sb, exports);

        assertEquals("foo.bar;version=1.0,foo.bar,", sb.toString());
    }

    public void testDetermineExportsIncludeServiceInterfaces() {
        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {{
            add(new MockRegistration(new HashAttributeSet(), AttributeSet.class));
            add(new MockRegistration(new DefaultTableModel(), TableModel.class));
        }};
        String imports = builder.determineExports(regs, new DefaultPackageScannerConfiguration());
        assertNotNull(imports);
        System.out.println(imports.replace(',', '\n'));
        assertTrue(imports.contains(AttributeSet.class.getPackage().getName()));
        assertTrue(imports.contains("javax.swing.event"));
    }

    public void testConstructJdkExports() {
        StringBuilder sb = new StringBuilder();
        builder.constructJdkExports(sb, "jdk-packages.test.txt");
        assertEquals("foo.bar,foo.baz", sb.toString());
        sb = new StringBuilder();
        builder.constructJdkExports(sb, ExportsBuilder.JDK_PACKAGES_PATH);
        assertTrue(sb.toString().contains("org.xml.sax"));
    }

    public void testConstructJdkExportsWithJdk5And6() {
        String jdkVersion = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "1.5");
            String exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration());
            assertFalse(exports.contains("javax.script"));
            System.setProperty("java.specification.version", "1.6");
            exports = builder.determineExports(new ArrayList<HostComponentRegistration>(), new DefaultPackageScannerConfiguration());
            assertTrue(exports.contains("javax.script"));
        }
        finally {
            System.setProperty("java.specification.version", jdkVersion);
        }
    }


    public void testGenerateExports() throws MalformedURLException {
        ServletContext ctx = mock(ServletContext.class);
        when(ctx.getMajorVersion()).thenReturn(5);
        when(ctx.getMinorVersion()).thenReturn(3);
        when(ctx.getResource("/WEB-INF/lib")).thenReturn(getClass().getClassLoader().getResource("scanbase/WEB-INF/lib"));
        when(ctx.getResource("/WEB-INF/classes")).thenReturn(getClass().getClassLoader().getResource("scanbase/WEB-INF/classes"));
        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration("1.0");
        config.setServletContext(ctx);
        config.setPackageIncludes(Arrays.asList("javax.*", "org.*"));

        Collection<ExportPackage> exports = builder.generateExports(config);
        assertNotNull(exports);
        assertTrue(exports.contains(new ExportPackage("org.apache.log4j", "1.2.15", new File("/whatever/log4j-1.2.15.jar"))));

        // Test falling through to servlet context scanning
        config.setJarIncludes(Arrays.asList("testlog*", "mock*"));
        config.setJarExcludes(Arrays.asList("log4j*"));
        exports = builder.generateExports(config);
        assertNotNull(exports);
        assertTrue(exports.contains(new ExportPackage("org.apache.log4j", "1.2.15", new File("/whatever/log4j-1.2.15.jar"))));

        // Test failure when even servlet context scanning fails
        config.setJarIncludes(Arrays.asList("testlog4j23*"));
        config.setJarExcludes(Collections.<String>emptyList());
        try {
            exports = builder.generateExports(config);
            fail("Should have thrown an exception");
        }
        catch (IllegalStateException ex) {
            // good stuff
        }

        // Test failure when no servlet context
        config.setJarIncludes(Arrays.asList("testlog4j23*"));
        config.setJarExcludes(Collections.<String>emptyList());
        config.setServletContext(null);
        try {
            exports = builder.generateExports(config);
            fail("Should have thrown an exception");
        }
        catch (IllegalStateException ex) {
            // good stuff
        }
    }

    public void testGenerateExportsWithCorrectServletVersion() throws MalformedURLException {
        ServletContext ctx = mock(ServletContext.class);
        when(ctx.getMajorVersion()).thenReturn(5);
        when(ctx.getMinorVersion()).thenReturn(3);
        when(ctx.getResource("/WEB-INF/lib")).thenReturn(getClass().getClassLoader().getResource("scanbase/WEB-INF/lib"));
        when(ctx.getResource("/WEB-INF/classes")).thenReturn(getClass().getClassLoader().getResource("scanbase/WEB-INF/classes"));

        DefaultPackageScannerConfiguration config = new DefaultPackageScannerConfiguration("1.0");
        config.setServletContext(ctx);
        config.setPackageIncludes(Arrays.asList("javax.*", "org.*"));

        Collection<ExportPackage> exports = builder.generateExports(config);

        int pkgsToFind = 2;
        for (ExportPackage pkg : exports) {
            if ("javax.servlet".equals(pkg.getPackageName())) {
                assertEquals("5.3.0", pkg.getVersion());
                pkgsToFind--;
            }
            if ("javax.servlet.http".equals(pkg.getPackageName())) {
                assertEquals("5.3.0", pkg.getVersion());
                pkgsToFind--;
            }
        }
        assertEquals(0, pkgsToFind);
    }


}
