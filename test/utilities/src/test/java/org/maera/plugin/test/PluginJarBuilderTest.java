package org.maera.plugin.test;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.*;

public class PluginJarBuilderTest {

    @Test
    public void testBuild() throws Exception {
        File jar = new PluginJarBuilder("foo")
                .addJava("my.Foo", "package my; public class Foo { public String hi() {return \"hi\";}}")
                .addResource("foo.txt", "Some text")
                .addPluginInformation("someKey", "someName", "1.33")
                .build();
        assertNotNull(jar);

        URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()}, null);
        Class cls = cl.loadClass("my.Foo");
        assertNotNull(cls);
        Object foo = cls.newInstance();
        String result = (String) cls.getMethod("hi").invoke(foo);
        assertEquals("hi", result);
        Assert.assertEquals("Some text", IOUtils.toString(cl.getResourceAsStream("foo.txt")));
        assertNotNull(cl.getResource("META-INF/MANIFEST.MF"));

        String xml = IOUtils.toString(cl.getResourceAsStream("maera-plugin.xml"));
        assertTrue(xml.indexOf("someKey") > 0);
        assertTrue(xml.indexOf("someName") > 0);
        assertTrue(xml.indexOf("1.33") > 0);
    }
}
