/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 31, 2004
 * Time: 12:58:29 PM
 */
package org.maera.plugin;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.maera.plugin.util.JavaVersionUtils;

import java.util.Collections;
import java.util.Map;

public class PluginInformation {
    private String description = "";
    private String descriptionKey;
    private String version = "0.0";
    private String vendorName = "(unknown)";
    private String vendorUrl;
    private float maxVersion;
    private float minVersion;
    private Float minJavaVersion;
    private final Map<String, String> parameters = CopyOnWriteMap.<String, String>builder().stableViews().newHashMap();

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setVendorName(final String vendorName) {
        this.vendorName = vendorName;
    }

    public void setVendorUrl(final String vendorUrl) {
        this.vendorUrl = vendorUrl;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorUrl() {
        return vendorUrl;
    }

    /**
     * @deprecated Since 2.2.0
     */
    @Deprecated
    public void setMaxVersion(final float maxVersion) {
        this.maxVersion = maxVersion;
    }

    /**
     * @deprecated Since 2.2.0
     */
    @Deprecated
    public void setMinVersion(final float minVersion) {
        this.minVersion = minVersion;
    }

    /**
     * @deprecated Since 2.2.0
     */
    @Deprecated
    public float getMaxVersion() {
        return maxVersion;
    }

    /**
     * @deprecated Since 2.2.0
     */
    @Deprecated
    public float getMinVersion() {
        return minVersion;
    }

    public Float getMinJavaVersion() {
        return minJavaVersion;
    }

    public void setMinJavaVersion(final Float minJavaVersion) {
        this.minJavaVersion = minJavaVersion;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void addParameter(final String key, final String value) {
        parameters.put(key, value);
    }

    public boolean satisfiesMinJavaVersion() {
        return (minJavaVersion == null) || JavaVersionUtils.satisfiesMinVersion(minJavaVersion);
    }

    public void setDescriptionKey(final String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

}