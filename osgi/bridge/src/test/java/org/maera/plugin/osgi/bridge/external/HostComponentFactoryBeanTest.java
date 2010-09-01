package org.maera.plugin.osgi.bridge.external;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HostComponentFactoryBeanTest {

    @Test
    public void testGetService() throws Exception {
        BundleContext ctx = mock(BundleContext.class);
        ServiceReference ref = mock(ServiceReference.class);
        when(ctx.getServiceReferences(null, "(foo=bar)")).thenReturn(new ServiceReference[]{ref});
        when(ctx.getService(ref)).thenReturn(new Callable() {

            public Object call() throws Exception {
                return "foo";
            }
        });

        ArgumentCaptor<ServiceListener> serviceListener = new ArgumentCaptor<ServiceListener>();


        HostComponentFactoryBean bean = new HostComponentFactoryBean();
        bean.setBundleContext(ctx);
        bean.setFilter("(foo=bar)");
        bean.setInterfaces(new Class[]{Callable.class});
        bean.afterPropertiesSet();

        assertEquals("foo", ((Callable) bean.getObject()).call());

        verify(ctx).addServiceListener(serviceListener.capture(), (String) anyObject());
        ServiceReference updatedRef = mock(ServiceReference.class);
        when(ctx.getService(updatedRef)).thenReturn(new Callable() {

            public Object call() throws Exception {
                return "boo";
            }
        });
        serviceListener.getValue().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, updatedRef));

        assertEquals("boo", ((Callable) bean.getObject()).call());
    }
}
