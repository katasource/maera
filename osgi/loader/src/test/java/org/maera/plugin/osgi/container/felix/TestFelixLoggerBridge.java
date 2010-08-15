package org.maera.plugin.osgi.container.felix;

import junit.framework.TestCase;
import org.apache.felix.framework.Logger;
import org.apache.felix.moduleloader.ResourceNotFoundException;

import static org.mockito.Mockito.*;

public class TestFelixLoggerBridge extends TestCase {
    private org.slf4j.Logger log;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        log = mock(org.slf4j.Logger.class);
    }

    public void testFrameworkLogInfo() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_INFO, "foo", null);
        verify(log).info("foo");
    }

    public void testClassNotFound() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_WARNING, "foo", new ClassNotFoundException("foo"));
        verify(log).debug("Class not found in bundle: foo");
    }

    public void testResourceNotFound() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_WARNING, "foo", new ResourceNotFoundException("foo"));
        verify(log).trace("Resource not found in bundle: foo");
    }

    public void testClassNotFoundOnDebug() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        bridge.doLog(null, Logger.LOG_WARNING, "*** foo", new ClassNotFoundException("*** foo", new ClassNotFoundException("bar")));
        verify(log).debug("Class not found in bundle: *** foo");
    }

    public void testLameClassNotFound() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        verify(log).isDebugEnabled();
        verify(log).isInfoEnabled();
        bridge.doLog(null, Logger.LOG_WARNING, "org.springframework.foo", new ClassNotFoundException("org.springframework.foo"));
        verifyNoMoreInteractions(log);
    }

    public void testLameClassNotFoundInDebug() {
        when(log.isInfoEnabled()).thenReturn(true);
        FelixLoggerBridge bridge = new FelixLoggerBridge(log);
        verify(log).isDebugEnabled();
        verify(log).isInfoEnabled();
        bridge.doLog(null, Logger.LOG_WARNING, "*** org.springframework.foo",
                new ClassNotFoundException("*** org.springframework.foo", new ClassNotFoundException("org.springframework.foo")));
        verifyNoMoreInteractions(log);
    }
}
