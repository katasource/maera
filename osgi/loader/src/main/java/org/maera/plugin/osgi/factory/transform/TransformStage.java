package org.maera.plugin.osgi.factory.transform;

/**
 * Performes an stage in the transformation from a JAR to an OSGi bundle
 *
 * @since 0.1
 */
public interface TransformStage {

    /**
     * Transforms the jar by operating on the context
     *
     * @param context The transform context to operate on
     * @throws PluginTransformationException If the stage cannot be performed and the whole operation should be aborted
     */
    void execute(TransformContext context)
            throws PluginTransformationException;

}
