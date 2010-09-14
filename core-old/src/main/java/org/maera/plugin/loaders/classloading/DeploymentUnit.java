package org.maera.plugin.loaders.classloading;

import java.io.File;

/**
 * A file that is to, or has been, deployed as a plugin.  Differs to the {@link org.maera.plugin.PluginArtifact},
 * which identifies an artifact to be deployed but may not be a physical file.
 */
public final class DeploymentUnit implements Comparable<DeploymentUnit> {
    private final File path;
    private final long lastModifiedAtTimeOfDeployment;

    public DeploymentUnit(File path) {
        if (path == null) {
            throw new IllegalArgumentException("File should not be null!");
        }
        this.path = path;
        this.lastModifiedAtTimeOfDeployment = path.lastModified();
    }

    public long lastModified() {
        return lastModifiedAtTimeOfDeployment;
    }

    public File getPath() {
        return path;
    }

    public int compareTo(DeploymentUnit target) {
        int result = path.compareTo(target.getPath());
        if (result == 0)
            result = (lastModifiedAtTimeOfDeployment > target.lastModified() ? 1 :
                    lastModifiedAtTimeOfDeployment < target.lastModified() ? -1 : 0);
        return result;
    }

    public boolean equals(Object deploymentUnit) {
        if (deploymentUnit instanceof DeploymentUnit)
            return equals((DeploymentUnit) deploymentUnit);
        else
            return false;
    }

    public boolean equals(DeploymentUnit deploymentUnit) {
        if (!path.equals(deploymentUnit.path)) return false;
        if (lastModifiedAtTimeOfDeployment != deploymentUnit.lastModifiedAtTimeOfDeployment) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = path.hashCode();
        result = 31 * result + (int) (lastModifiedAtTimeOfDeployment ^ (lastModifiedAtTimeOfDeployment >>> 32));
        return result;
    }

    public String toString() {
        return "Unit: " + path.toString() + " (" + lastModifiedAtTimeOfDeployment + ")";
    }
}
