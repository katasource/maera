package org.maera.plugin.hostcontainer;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestSimpleConstructorModuleFactory extends TestCase {
    public void testCreateModule() {
        final Map<Class<?>, Object> context = new HashMap<Class<?>, Object>() {
            {
                put(String.class, "bob");
            }
        };

        final SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(context);
        final Base world = factory.create(OneArg.class);
        assertEquals("bob", world.getName());
    }

    public void testCreateModuleFindBiggest() {
        final Map<Class<?>, Object> context = new HashMap<Class<?>, Object>() {
            {
                put(String.class, "bob");
                put(Integer.class, 10);
            }
        };

        final SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(context);
        final Base world = factory.create(TwoArg.class);
        assertEquals("bob 10", world.getName());
    }

    public void testCreateModuleFindSmaller() {
        final Map<Class<?>, Object> context = new HashMap<Class<?>, Object>() {
            {
                put(String.class, "bob");
            }
        };

        final SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(context);
        final Base world = factory.create(TwoArg.class);
        assertEquals("bob", world.getName());
    }

    public void testCreateModuleNoMatch() {
        final SimpleConstructorHostContainer factory = new SimpleConstructorHostContainer(Collections.<Class<?>, Object>emptyMap());
        try {
            factory.create(OneArg.class);
            fail("Should have thrown exception");
        }
        catch (final IllegalArgumentException ex) {
            // good, good
        }
    }

    public abstract static class Base {
        private final String name;

        public Base(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class OneArg extends Base {
        public OneArg(final String name) {
            super(name);
        }
    }

    public static class TwoArg extends Base {
        public TwoArg(final String name) {
            super(name);
        }

        public TwoArg(final String name, final Integer age) {
            super(name + " " + age);
        }
    }

}
