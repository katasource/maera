package org.maera.plugin.osgi.container.felix;

import org.apache.felix.framework.Logger;
import org.apache.felix.moduleloader.ResourceNotFoundException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class FelixLoggerBridgeTest {

    private org.slf4j.Logger log;

    @Before
    public void setUp() throws Exception {
        log = mock(org.slf4j.Logger.class);
    }

    @Test
    public void testClassNotFound() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_WARNING, "foo", new ClassNotFoundException("foo"));
        verify(log).debug("Class not found in bundle: foo");
    }

    @Test
    public void testClassNotFoundOnDebug() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_WARNING, "*** foo", new ClassNotFoundException("*** foo", new ClassNotFoundException("bar")));
        verify(log).debug("Class not found in bundle: *** foo");
    }

    @Test
    public void testFrameworkLogInfo() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_INFO, "foo", null);
        verify(log).info("foo");
    }

    @Test
    public void testLameClassNotFound() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        verify(log).isDebugEnabled();
        verify(log).isInfoEnabled();
        bridge.doLog(null, Logger.LOG_WARNING, "org.springframework.foo", new ClassNotFoundException("org.springframework.foo"));
        verifyNoMoreInteractions(log);
    }

    @Test
    public void testLameClassNotFoundInDebug() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        verify(log).isDebugEnabled();
        verify(log).isInfoEnabled();
        bridge.doLog(null, Logger.LOG_WARNING, "*** org.springframework.foo",
                new ClassNotFoundException("*** org.springframework.foo", new ClassNotFoundException("org.springframework.foo")));
        verifyNoMoreInteractions(log);
    }

    @Test
    public void testResourceNotFound() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_WARNING, "foo", new ResourceNotFoundException("foo"));
        verify(log).trace("Resource not found in bundle: foo");
    }
}
