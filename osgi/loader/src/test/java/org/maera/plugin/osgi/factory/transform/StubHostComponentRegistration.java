package org.maera.plugin.osgi.factory.transform;

import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class StubHostComponentRegistration implements HostComponentRegistration {
    private String[] mainInterfaces;
    private Dictionary<String, String> properties;
    private Class[] mainInterfaceClasses;
    private Object instance;

    public StubHostComponentRegistration(Class... ifs) {
        this(null, ifs);
    }

    public StubHostComponentRegistration(String name, Class... ifs) {
        this(name, null, ifs);
    }

    public StubHostComponentRegistration(String name, Object value, Class... ifs) {
        this.mainInterfaceClasses = ifs;
        mainInterfaces = new String[ifs.length];
        for (int x = 0; x < ifs.length; x++)
            mainInterfaces[x] = ifs[x].getName();
        this.properties = new Hashtable<String, String>();
        if (name != null)
            properties.put("bean-name", name);
        instance = value;
    }

    public StubHostComponentRegistration(String[] ifs, Dictionary<String, String> props) {
        mainInterfaces = ifs;
        this.properties = props;
    }

    public Object getInstance() {
        return instance;
    }

    public Class[] getMainInterfaceClasses() {
        return mainInterfaceClasses;
    }

    public Dictionary<String, String> getProperties() {
        return properties;
    }

    public String[] getMainInterfaces() {
        return mainInterfaces;
    }
}