package org.maera.plugin.test;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class TestPluginJarBuilder extends TestCase {

    public void testBuild() throws Exception {
        File jar = new PluginJarBuilder("foo")
                .addJava("my.Foo", "package my; public class Foo { public String hi() {return \"hi\";}}")
                .addResource("foo.txt", "Some text")
                .addPluginInformation("someKey", "someName", "1.33")
                .build();
        assertNotNull(jar);

        URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURL()}, null);
        Class cls = cl.loadClass("my.Foo");
        assertNotNull(cls);
        Object foo = cls.newInstance();
        String result = (String) cls.getMethod("hi", new Class[0]).invoke(foo, new Object[0]);
        assertEquals("hi", result);
        Assert.assertEquals("Some text", IOUtils.toString(cl.getResourceAsStream("foo.txt")));
        assertNotNull(cl.getResource("META-INF/MANIFEST.MF"));

        String xml = IOUtils.toString(cl.getResourceAsStream("atlassian-plugin.xml"));
        assertTrue(xml.indexOf("someKey") > 0);
        assertTrue(xml.indexOf("someName") > 0);
        assertTrue(xml.indexOf("1.33") > 0);
    }
}
