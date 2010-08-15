package org.maera.plugin.event.impl;

import com.atlassian.event.api.EventListener;
import junit.framework.TestCase;
import org.maera.plugin.event.PluginEventListener;

import java.lang.reflect.Method;

public class TestPluginEventManagerImpl extends TestCase {
    private DefaultPluginEventManager eventManager;

    public void setUp() {
        eventManager = new DefaultPluginEventManager();
    }

    public void tearDown() {
        eventManager = null;
    }

    public void testRegister() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(1, methodTestListener.called);
    }

    public void testRegisterWithBroadcastSupertype() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new String());
        assertEquals(1, methodTestListener.called);
    }

    public void testRegisterWithFooBroadcastSupertype() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new MethodTestListener());
        assertEquals(1, methodTestListener.fooCalled);
        assertEquals(1, methodTestListener.called);
    }

    public void testRegisterTwice() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(1, methodTestListener.called);
    }

    public void testRegisterWithBadListener() {
        BadListener l = new BadListener();
        try {
            eventManager.register(l);
            fail();
        }
        catch (IllegalArgumentException ex) {
            // test passed
        }
        assertEquals(0, l.called);
    }

    public void testRegisterWithCustomSelector() {
        eventManager = new DefaultPluginEventManager(new ListenerMethodSelector[]{
                new ListenerMethodSelector() {
                    public boolean isListenerMethod(Method method) {
                        return "onEvent".equals(method.getName());
                    }
                }
        });
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast("jim");
        assertEquals(1, methodTestListener.jimCalled);
    }

    public void testRegisterWithOverlappingSelectorsBroadcastsTwoMessages() {
        eventManager = new DefaultPluginEventManager(new ListenerMethodSelector[]{
                new MethodNameListenerMethodSelector(), new MethodNameListenerMethodSelector()});
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(2, methodTestListener.called);
    }

    public void testRegisterWithCustom() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(1, methodTestListener.called);
    }

    public void testRegisterAnnotatedListener() {
        AnnotationTestListener listener = new AnnotationTestListener();
        eventManager.register(listener);
        eventManager.broadcast(new Object());
        assertEquals(1, listener.eventListenerCalled);
        assertEquals(1, listener.pluginEventListenerCalled);
    }

    public void testUnregister() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        eventManager.unregister(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(1, methodTestListener.called);
    }

    public void testSuperEvent() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new MethodTestListener());
        assertEquals(1, methodTestListener.called);
    }

    public void testRegisterNull() {
        try {
            eventManager.register(null);
            fail("should have thrown exception");
        }
        catch (IllegalArgumentException ex) {
            // test passed
        }
    }

    public void testUnregisterNull() {
        try {
            eventManager.unregister(null);
            fail("should have thrown an exception");
        }
        catch (IllegalArgumentException e) {
            // passes
        }
    }

    public static class AnnotationTestListener {
        int pluginEventListenerCalled = 0;
        int eventListenerCalled = 0;

        @PluginEventListener
        public void doEventOld(Object obj) {
            ++pluginEventListenerCalled;
        }

        @EventListener
        public void doEventNew(Object obj) {
            ++eventListenerCalled;
        }
    }

    public static class MethodTestListener {
        int called = 0;
        int fooCalled = 0;
        int jimCalled = 0;

        public void channel(Object obj) {
            called++;
        }

        public void channel(MethodTestListener obj) {
            fooCalled++;
        }

        public void onEvent(String o) {
            jimCalled++;
        }
    }

    public static class BadListener {
        int called = 0;

        public void somemethod() {
            called++;
        }
    }


}
