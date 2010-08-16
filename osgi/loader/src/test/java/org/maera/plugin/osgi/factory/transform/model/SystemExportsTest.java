package org.maera.plugin.osgi.factory.transform.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SystemExportsTest {

    @Test
    public void testExportPackageWithVersion() {
        SystemExports exports = new SystemExports("foo.bar;version=\"4.0\"");

        assertEquals("foo.bar;version=\"[4.0,4.0]\"", exports.getFullExport("foo.bar"));
        assertEquals("foo.baz", exports.getFullExport("foo.baz"));
    }
}
