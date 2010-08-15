package org.maera.plugin.osgi.hostcomponents.impl;

import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;


/**
 * A registration of a host component
 */
class Registration implements HostComponentRegistration {
    private final String[] mainInterfaces;
    private final Class<?>[] mainInterfaceClasses;
    private final Dictionary<String, String> properties = new Hashtable<String, String>();
    private Object instance;

    public Registration(final Class<?>[] ifs) {
        mainInterfaceClasses = ifs;
        mainInterfaces = new String[ifs.length];
        for (int x = 0; x < ifs.length; x++) {
            if (!ifs[x].isInterface()) {
                throw new IllegalArgumentException("Services can only be registered against interfaces");
            }

            mainInterfaces[x] = ifs[x].getName();
        }
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(final Object instance) {
        this.instance = instance;
    }

    public Dictionary<String, String> getProperties() {
        return properties;
    }

    public String[] getMainInterfaces() {
        return mainInterfaces;
    }

    public Class<?>[] getMainInterfaceClasses() {
        return mainInterfaceClasses;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        final Registration that = (Registration) o;

        if (!instance.equals(that.instance)) {
            return false;
        }
        if (!Arrays.equals(mainInterfaces, that.mainInterfaces)) {
            return false;
        }
        if (!properties.equals(that.properties)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = Arrays.hashCode(mainInterfaces);
        result = 31 * result + instance.hashCode();
        result = 31 * result + properties.hashCode();
        return result;
    }
}
