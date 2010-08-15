package org.maera.plugin.module;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.PluginParseException;

import java.lang.reflect.Constructor;

/**
 * Legacy module factory that provides module classes for descriptors that aren't using an injected ModuleFactory
 *
 * @since 2.5.0
 */
public class LegacyModuleFactory implements ModuleFactory {

    public <T> T createModule(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException {
        throw new UnsupportedOperationException(" create Module not supported by LegacyModuleFactory. Use PrefixDelegatingModuleFactory instead.");
    }

    public <T> Class<T> getModuleClass(String name, ModuleDescriptor<T> moduleDescriptor) throws ModuleClassNotFoundException {

        try {
            // First try and load the class, to make sure the class exists
            @SuppressWarnings("unchecked")
            final Class<T> loadedClass = (Class<T>) moduleDescriptor.getPlugin().loadClass(name, null); // TODO: null means context classloader?

            // Then instantiate the class, so we can see if there are any dependencies that aren't satisfied
            try {
                final Constructor<T> noargConstructor = loadedClass.getConstructor(new Class[]{});
                if (noargConstructor != null) {
                    loadedClass.newInstance();
                }
            }
            catch (final NoSuchMethodException e) {
                // If there is no "noarg" constructor then don't do the check
            }
            return loadedClass;
        }
        catch (final ClassNotFoundException e) {
            throw new PluginParseException("Could not load class: " + name, e);
        }
        catch (final NoClassDefFoundError e) {
            throw new PluginParseException("Error retrieving dependency of class: " + name + ". Missing class: " + e.getMessage(), e);
        }
        catch (final UnsupportedClassVersionError e) {
            throw new PluginParseException("Class version is incompatible with current JVM: " + name, e);
        }
        catch (final Throwable t) {
            throw new PluginParseException(t);
        }
    }
}
