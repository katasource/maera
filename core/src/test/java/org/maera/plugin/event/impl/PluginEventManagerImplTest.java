package org.maera.plugin.event.impl;

import com.atlassian.event.api.EventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.event.PluginEventListener;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PluginEventManagerImplTest {

    private DefaultPluginEventManager eventManager;

    @Before
    public void setUp() {
        eventManager = new DefaultPluginEventManager();
    }

    @After
    public void tearDown() {
        eventManager = null;
    }

    @Test
    public void testRegister() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(1, methodTestListener.called);
    }

    @Test
    public void testRegisterAnnotatedListener() {
        AnnotationTestListener listener = new AnnotationTestListener();
        eventManager.register(listener);
        eventManager.broadcast(new Object());
        assertEquals(1, listener.eventListenerCalled);
        assertEquals(1, listener.pluginEventListenerCalled);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNull() {
        eventManager.register(null);
    }

    @Test
    public void testRegisterTwice() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(1, methodTestListener.called);
    }

    @Test
    public void testRegisterWithBadListener() {
        BadListener l = new BadListener();
        try {
            eventManager.register(l);
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals(0, l.called);
        }
    }

    @Test
    public void testRegisterWithBroadcastSupertype() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast("");
        assertEquals(1, methodTestListener.called);
    }

    @Test
    public void testRegisterWithCustom() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(1, methodTestListener.called);
    }

    @Test
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

    @Test
    public void testRegisterWithFooBroadcastSupertype() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new MethodTestListener());
        assertEquals(1, methodTestListener.fooCalled);
        assertEquals(1, methodTestListener.called);
    }

    @Test
    public void testRegisterWithOverlappingSelectorsBroadcastsTwoMessages() {
        eventManager = new DefaultPluginEventManager(new ListenerMethodSelector[]{
                new MethodNameListenerMethodSelector(), new MethodNameListenerMethodSelector()});
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(2, methodTestListener.called);
    }

    @Test
    public void testSuperEvent() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new MethodTestListener());
        assertEquals(1, methodTestListener.called);
    }

    @Test
    public void testUnregister() {
        MethodTestListener methodTestListener = new MethodTestListener();
        eventManager.register(methodTestListener);
        eventManager.broadcast(new Object());
        eventManager.unregister(methodTestListener);
        eventManager.broadcast(new Object());
        assertEquals(1, methodTestListener.called);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnregisterNull() {
        eventManager.unregister(null);
    }

    public static class AnnotationTestListener {

        int eventListenerCalled = 0;
        int pluginEventListenerCalled = 0;

        @EventListener
        public void doEventNew(Object obj) {
            ++eventListenerCalled;
        }

        @PluginEventListener
        public void doEventOld(Object obj) {
            ++pluginEventListenerCalled;
        }
    }

    public static class BadListener {

        int called = 0;

        public void somemethod() {
            called++;
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
}
