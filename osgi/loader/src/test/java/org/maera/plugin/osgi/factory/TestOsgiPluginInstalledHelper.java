package org.maera.plugin.osgi.factory;

import junit.framework.TestCase;
import org.maera.plugin.AutowireCapablePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOsgiPluginInstalledHelper extends TestCase {
    private Bundle bundle;
    private BundleContext bundleContext;
    private Dictionary dict;
    private OsgiPluginHelper helper;
    private PackageAdmin packageAdmin;

    @Override
    public void setUp() {
        bundle = mock(Bundle.class);
        dict = new Hashtable();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        when(bundle.getHeaders()).thenReturn(dict);
        bundleContext = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bundleContext);

        helper = mock(OsgiPluginHelper.class);
        when(helper.getBundle()).thenReturn(bundle);

        packageAdmin = mock(PackageAdmin.class);

        helper = new OsgiPluginInstalledHelper(bundle, packageAdmin);
    }

    @Override
    public void tearDown() {
        bundle = null;
        packageAdmin = null;
        helper = null;
        dict = null;
        bundleContext = null;
    }

    public void testAutowireObject() {
        StaticListableBeanFactory bf = new StaticListableBeanFactory();
        bf.addBean("child", new ChildBean());
        DefaultListableBeanFactory autowireBf = new DefaultListableBeanFactory(bf);

        when(bundle.getSymbolicName()).thenReturn("foo");
        helper.setPluginContainer(new GenericApplicationContext(autowireBf));
        SetterInjectedBean bean = new SetterInjectedBean();
        helper.autowire(bean, AutowireCapablePlugin.AutowireStrategy.AUTOWIRE_BY_NAME);
        assertNotNull(bean.getChild());
    }

    public void testAutowireNoSpringButThereShouldBe() {
        Object obj = new Object();
        try {
            helper.autowire(obj, AutowireCapablePlugin.AutowireStrategy.AUTOWIRE_AUTODETECT);
            fail("Should throw exception");
        }
        catch (RuntimeException ex) {
            // test passed
        }
    }

    public void testOnDisableWithoutEnabling() {
        // needs to work without onEnable being called first.
        try {
            helper.onDisable();
        }
        catch (NullPointerException e) {
            fail("NullPointerException encountered.");
        }
    }

    public static class ChildBean {
    }


    public static class SetterInjectedBean {
        private ChildBean child;

        public ChildBean getChild() {
            return child;
        }

        public void setChild(ChildBean child) {
            this.child = child;
        }
    }
}
