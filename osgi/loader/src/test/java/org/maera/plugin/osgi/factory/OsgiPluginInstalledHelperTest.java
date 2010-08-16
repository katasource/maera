package org.maera.plugin.osgi.factory;

import org.junit.Before;
import org.junit.Test;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OsgiPluginInstalledHelperTest {

    private Bundle bundle;
    private OsgiPluginHelper helper;

    @Before
    public void setUp() {
        bundle = mock(Bundle.class);
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        when(bundle.getHeaders()).thenReturn(dict);
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bundleContext);

        helper = mock(OsgiPluginHelper.class);
        when(helper.getBundle()).thenReturn(bundle);

        PackageAdmin packageAdmin = mock(PackageAdmin.class);
        helper = new OsgiPluginInstalledHelper(bundle, packageAdmin);
    }

    @Test
    public void testAutowireNoSpringButThereShouldBe() {
        Object obj = new Object();
        try {
            helper.autowire(obj, AutowireCapablePlugin.AutowireStrategy.AUTOWIRE_AUTODETECT);
            fail("Should throw exception");
        }
        catch (RuntimeException ignored) {

        }
    }

    @Test
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

    @Test
    public void testOnDisableWithoutEnabling() {
        helper.onDisable();
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
