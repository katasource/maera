package org.maera.plugin.descriptors;

import com.atlassian.util.concurrent.NotNull;
import org.apache.commons.lang.Validate;
import org.dom4j.Element;
import org.maera.plugin.*;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.loaders.LoaderUtils;
import org.maera.plugin.module.LegacyModuleFactory;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.module.PrefixDelegatingModuleFactory;
import org.maera.plugin.util.ClassUtils;
import org.maera.plugin.util.JavaVersionUtils;
import org.maera.plugin.util.validation.ValidationPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.maera.plugin.util.Assertions.notNull;
import static org.maera.plugin.util.validation.ValidationPattern.createPattern;
import static org.maera.plugin.util.validation.ValidationPattern.test;

public abstract class AbstractModuleDescriptor<T> implements ModuleDescriptor<T>, StateAware {
    protected Plugin plugin;
    String key;
    String name;
    protected String moduleClassName;
    Class<T> moduleClass;
    String description;
    boolean enabledByDefault = true;
    boolean systemModule = false;

    /**
     * @deprecated since 2.2.0
     */
    @Deprecated
    protected boolean singleton = true;
    Map<String, String> params;
    protected Resources resources = Resources.EMPTY_RESOURCES;
    private Float minJavaVersion;
    private String i18nNameKey;
    private String descriptionKey;
    private String completeKey;
    boolean enabled = false;
    protected final ModuleFactory moduleFactory;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public AbstractModuleDescriptor(final ModuleFactory moduleFactory) {
        Validate.notNull(moduleFactory, "Module creator factory cannot be null");
        this.moduleFactory = moduleFactory;
    }

    /**
     * @Deprecated since 2.5.0 use the constructor which requires a
     * {@link org.maera.plugin.module.ModuleFactory}
     */
    @Deprecated
    public AbstractModuleDescriptor() {
        this(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    public void init(@NotNull final Plugin plugin, @NotNull final Element element) throws PluginParseException {
        validate(element);

        this.plugin = notNull("plugin", plugin);
        this.key = element.attributeValue("key");
        this.name = element.attributeValue("name");
        this.i18nNameKey = element.attributeValue("i18n-name-key");
        this.completeKey = buildCompleteKey(plugin, key);
        this.description = element.elementTextTrim("description");
        this.moduleClassName = element.attributeValue("class");
        final Element descriptionElement = element.element("description");
        this.descriptionKey = (descriptionElement != null) ? descriptionElement.attributeValue("key") : null;
        params = LoaderUtils.getParams(element);

        if ("disabled".equalsIgnoreCase(element.attributeValue("state"))) {
            enabledByDefault = false;
        }

        if ("true".equalsIgnoreCase(element.attributeValue("system"))) {
            systemModule = true;
        }

        if (element.element("java-version") != null) {
            minJavaVersion = Float.valueOf(element.element("java-version").attributeValue("min"));
        }

        if ("false".equalsIgnoreCase(element.attributeValue("singleton"))) {
            singleton = false;
        } else if ("true".equalsIgnoreCase(element.attributeValue("singleton"))) {
            singleton = true;
        } else {
            singleton = isSingletonByDefault();
        }

        resources = Resources.fromXml(element);
    }

    /**
     * Validates the element, collecting the rules from
     * {@link #provideValidationRules(ValidationPattern)}
     *
     * @param element The configuration element
     * @since 2.2.0
     */
    private void validate(final Element element) {
        notNull("element", element);
        final ValidationPattern pattern = createPattern();
        provideValidationRules(pattern);
        pattern.evaluate(element);
    }

    /**
     * Provides validation rules for the pattern
     *
     * @param pattern The validation pattern
     * @since 2.2.0
     */
    protected void provideValidationRules(final ValidationPattern pattern) {
        pattern.rule(test("@key").withError("The key is required"));
    }

    /**
     * Override this for module descriptors which don't expect to be able to
     * load a class successfully
     *
     * @param plugin
     * @param element
     * @deprecated Since 2.1.0, use {@link #loadClass(Plugin,String)} instead
     */
    @Deprecated
    protected void loadClass(final Plugin plugin, final Element element) throws PluginParseException {
        loadClass(plugin, element.attributeValue("class"));
    }

    /**
     * Loads the module class that this descriptor provides, and will not
     * necessarily be the implementation class. Override this for module
     * descriptors whose type cannot be determined via generics.
     *
     * @param clazz The module class name to load
     * @throws IllegalStateException If the module class cannot be determined
     *                               and the descriptor doesn't define a module type via generics
     */
    protected void loadClass(final Plugin plugin, final String clazz) throws PluginParseException {
        if (moduleClassName != null) {
            if (moduleFactory instanceof LegacyModuleFactory) // not all plugins
            // have to have a
            // class
            {
                moduleClass = ((LegacyModuleFactory) moduleFactory).getModuleClass(moduleClassName, this);
            }

            // This is only here for backwards compatibility with old code that
            // uses {@link
            // org.maera.plugin.PluginAccessor#getEnabledModulesByClass(Class)}
            else if (moduleFactory instanceof PrefixDelegatingModuleFactory) {
                moduleClass = ((PrefixDelegatingModuleFactory) moduleFactory).guessModuleClass(moduleClassName, this);
            }
        }
        // If this module has no class, then we assume Void
        else {
            moduleClass = (Class<T>) Void.class;
        }

        // Usually is null when a prefix is used in the class name
        if (moduleClass == null) {
            try {

                Class moduleTypeClass = null;
                try {
                    moduleTypeClass = ClassUtils.getTypeArguments(AbstractModuleDescriptor.class, getClass()).get(0);
                }
                catch (RuntimeException ex) {
                    log.debug("Unable to get generic type, usually due to Class.forName() problems", ex);
                    moduleTypeClass = getModuleReturnClass();
                }
                moduleClass = moduleTypeClass;
            }
            catch (final ClassCastException ex) {
                throw new IllegalStateException("The module class must be defined in a concrete instance of " + "ModuleDescriptor and not as another generic type.");
            }

            if (moduleClass == null) {
                throw new IllegalStateException("The module class cannot be determined, likely because it needs a concrete "
                        + "module type defined in the generic type it passes to AbstractModuleDescriptor");
            }
        }
    }

    Class<?> getModuleReturnClass() {
        try {
            return getClass().getMethod("getModule").getReturnType();
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("The getModule() method is missing (!) on " + getClass());
        }
    }

    /**
     * Build the complete key based on the provided plugin and module key. This
     * method has no side effects.
     *
     * @param plugin    The plugin for which the module belongs
     * @param moduleKey The key for the module
     * @return A newly constructed complete key, null if the plugin is null
     */
    private String buildCompleteKey(final Plugin plugin, final String moduleKey) {
        if (plugin == null) {
            return null;
        }

        final StringBuffer completeKeyBuffer = new StringBuffer(32);
        completeKeyBuffer.append(plugin.getKey()).append(":").append(moduleKey);
        return completeKeyBuffer.toString();
    }

    /**
     * Override this if your plugin needs to clean up when it's been removed.
     */
    public void destroy(final Plugin plugin) {
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault && satisfiesMinJavaVersion();
    }

    public boolean isSystemModule() {
        return systemModule;
    }

    /**
     * @deprecated Since 2.2.0
     */
    @Deprecated
    public boolean isSingleton() {
        return singleton;
    }

    /**
     * @deprecated Since 2.2.0
     */
    @Deprecated
    protected boolean isSingletonByDefault() {
        return true;
    }

    /**
     * Check that the module class of this descriptor implements a given
     * interface, or extends a given class.
     *
     * @param requiredModuleClazz The class this module's class must implement
     *                            or extend.
     * @throws PluginParseException If the module class does not implement or
     *                              extend the given class.
     */
    final protected void assertModuleClassImplements(final Class<T> requiredModuleClazz) throws PluginParseException {
        if (!enabled) {
            throw new PluginParseException("Plugin module " + getKey() + " not enabled");
        }
        if (!requiredModuleClazz.isAssignableFrom(getModuleClass())) {
            throw new PluginParseException("Given module class: " + getModuleClass().getName() + " does not implement " + requiredModuleClazz.getName());
        }
    }

    public String getCompleteKey() {
        return completeKey;
    }

    public String getPluginKey() {
        return getPlugin().getKey();
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public Class<T> getModuleClass() {
        return moduleClass;
    }

    public abstract T getModule();

    public String getDescription() {
        return description;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getI18nNameKey() {
        return i18nNameKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public List<ResourceDescriptor> getResourceDescriptors() {
        return resources.getResourceDescriptors();
    }

    public List<ResourceDescriptor> getResourceDescriptors(final String type) {
        return resources.getResourceDescriptors(type);
    }

    public ResourceLocation getResourceLocation(final String type, final String name) {
        return resources.getResourceLocation(type, name);
    }

    public ResourceDescriptor getResourceDescriptor(final String type, final String name) {
        return resources.getResourceDescriptor(type, name);
    }

    public Float getMinJavaVersion() {
        return minJavaVersion;
    }

    public boolean satisfiesMinJavaVersion() {
        if (minJavaVersion != null) {
            return JavaVersionUtils.satisfiesMinVersion(minJavaVersion);
        }
        return true;
    }

    /**
     * Sets the plugin for the ModuleDescriptor
     */
    public void setPlugin(final Plugin plugin) {
        this.completeKey = buildCompleteKey(plugin, key);
        this.plugin = plugin;
    }

    /**
     * @return The plugin this module descriptor is associated with
     */
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public String toString() {
        return getCompleteKey() + " (" + getDescription() + ")";
    }

    /**
     * Enables the descriptor by loading the module class. Classes overriding
     * this method MUST call super.enabled() before their own enabling code.
     *
     * @since 2.1.0
     */
    public void enabled() {
        loadClass(plugin, moduleClassName);
        enabled = true;
    }

    /**
     * Disables the module descriptor. Classes overriding this method MUST call
     * super.disabled() after their own disabling code.
     */
    public void disabled() {
        enabled = false;
        moduleClass = null;
    }
}
