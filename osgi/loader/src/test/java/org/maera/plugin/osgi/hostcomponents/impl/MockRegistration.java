package org.maera.plugin.osgi.hostcomponents.impl;

import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class MockRegistration implements HostComponentRegistration {
    private Dictionary<String, String> properties = new Hashtable<String, String>();
    private Class[] mainInterfaceClasses;
    private String[] mainInterfaces;
    private Object instance;

    public MockRegistration(String... interfaces) {
        this.mainInterfaces = interfaces;
    }

    public MockRegistration(Object instance, Class... mainInterfaceClasses) {
        setMainInterfaceClasses(mainInterfaceClasses);
        this.instance = instance;
    }

    public Dictionary<String, String> getProperties() {
        return properties;
    }

    public String[] getMainInterfaces() {
        return mainInterfaces;
    }

    public Object getInstance() {
        return instance;
    }

    public Class[] getMainInterfaceClasses() {
        return mainInterfaceClasses;
    }

    public void setProperties(Dictionary<String, String> properties) {
        this.properties = properties;
    }

    public void setMainInterfaceClasses(Class[] mainInterfaceClasses) {
        this.mainInterfaces = new String[mainInterfaceClasses.length];
        this.mainInterfaceClasses = mainInterfaceClasses;
        for (int x = 0; x < mainInterfaceClasses.length; x++) {
            mainInterfaces[x] = mainInterfaceClasses[x].getName();
        }
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }
}
