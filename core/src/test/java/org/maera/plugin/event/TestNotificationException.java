package org.maera.plugin.event;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.3.0
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ThrowableInstanceNeverThrown"})
public class TestNotificationException extends TestCase {
    public void testSingletonConstructor() throws Exception {
        Exception cause = new Exception("I don't like it");
        NotificationException notificationException = new NotificationException(cause);

        assertEquals(cause, notificationException.getCause());
        assertEquals(1, notificationException.getAllCauses().size());
        assertEquals(cause, notificationException.getAllCauses().get(0));
    }

    public void testListConstructor() throws Exception {
        Exception cause1 = new Exception("I don't like it");
        Exception cause2 = new Exception("Me neither");
        final List<Throwable> causes = new ArrayList<Throwable>();
        causes.add(cause1);
        causes.add(cause2);

        NotificationException notificationException = new NotificationException(causes);

        assertEquals(cause1, notificationException.getCause());
        assertEquals(2, notificationException.getAllCauses().size());
        assertEquals(cause1, notificationException.getAllCauses().get(0));
        assertEquals(cause2, notificationException.getAllCauses().get(1));
    }

    public void testListConstructorInvalid() throws Exception {
        try {
            new NotificationException((List) null);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException e) {
            // Expected.
        }

        try {
            new NotificationException(new ArrayList<Throwable>());
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            // Expected.
        }
    }
}
