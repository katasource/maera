package org.maera.plugin.event;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ThrowableInstanceNeverThrown"})
public class NotificationExceptionTest {

    @Test
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

    @Test
    public void testListConstructorInvalid() throws Exception {
        try {
            new NotificationException((List<Throwable>) null);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) {

        }

        try {
            new NotificationException(new ArrayList<Throwable>());
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException ignored) {

        }
    }

    @Test
    public void testSingletonConstructor() throws Exception {
        Exception cause = new Exception("I don't like it");
        NotificationException notificationException = new NotificationException(cause);

        assertEquals(cause, notificationException.getCause());
        assertEquals(1, notificationException.getAllCauses().size());
        assertEquals(cause, notificationException.getAllCauses().get(0));
    }
}
