package org.maera.plugin.osgi.hostcomponents;

/**
 * Defines an object that provides host components.  Host applications that wish to register their internal components
 * should implement this interface.  Classes like the {@link org.maera.plugin.osgi.factory.OsgiPluginFactory} use
 * this interface to retreive a list of host components to register into the OSGi service registry.
 * <p/>
 * <p>Here is an example implementation that registers two host components:
 * </p>
 * <pre>
 * public class MyHostComponentProvider implements HostComponentProvider {
 *      public void provide(ComponentRegistrar registrar) {
 *          registrar.register(SomeInterface.class).forInstance(someInstance).withName("some-bean");
 *          registrar.register(InterfaceA.class, InterfaceB.class)
 *                   .forInstance(MyBean.class)
 *                   .withProperty("propertyA", "valueA")
 *                   .withProperty("propertyB", "valueB");
 *      }
 * }
 * </pre>
 */
public interface HostComponentProvider {

    /**
     * Gives the object a chance to register its host components with the registrar
     *
     * @param registrar The host component registrar
     */
    void provide(ComponentRegistrar registrar);
}
