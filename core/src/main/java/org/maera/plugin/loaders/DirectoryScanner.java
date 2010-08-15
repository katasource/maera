package org.maera.plugin.loaders;

import org.apache.commons.lang.Validate;
import org.maera.plugin.PluginException;
import org.maera.plugin.loaders.classloading.DeploymentUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * Scans the filesystem for changed or added plugin files and stores a map of the currently known ones.  Files beginning
 * with "." are ignored.
 *
 * @since 2.1.0
 */
class DirectoryScanner implements org.maera.plugin.loaders.classloading.Scanner {
    private static Logger log = LoggerFactory.getLogger(DirectoryScanner.class);

    /**
     * Tracks the classloading
     */
    private final File pluginsDirectory;

    /**
     * A Map of {@link String} absolute file paths to {@link DeploymentUnit}s.
     */
    private final Map<String, DeploymentUnit> scannedDeploymentUnits = new TreeMap<String, DeploymentUnit>();


    /**
     * Constructor for scanner.
     *
     * @param pluginsDirectory the directory that the scanner should monitor for plugins
     */
    public DirectoryScanner(File pluginsDirectory) {
        Validate.notNull(pluginsDirectory, "Plugin scanner directory must not be null");
        this.pluginsDirectory = pluginsDirectory;
    }

    private DeploymentUnit createAndStoreDeploymentUnit(File file) {
        if (isScanned(file))
            return null;

        DeploymentUnit unit = new DeploymentUnit(file);
        scannedDeploymentUnits.put(file.getAbsolutePath(), unit);

        return unit;
    }

    /**
     * Given a file, finds the deployment unit for it if one has already been scanned.
     *
     * @param file a jar file.
     * @return the stored deploymentUnit matching the file or null if none exists.
     */
    public DeploymentUnit locateDeploymentUnit(File file) {
        return scannedDeploymentUnits.get(file.getAbsolutePath());
    }

    /**
     * Finds whether the given file has been scanned already.
     */
    private boolean isScanned(File file) {
        return locateDeploymentUnit(file) != null;
    }

    /**
     * Tells the Scanner to forget about a file it has loaded so that it will reload it
     * next time it scans.
     *
     * @param file a file that may have already been scanned.
     */
    public void clear(File file) {
        scannedDeploymentUnits.remove(file.getAbsolutePath());
    }

    /**
     * Scans for all files and directories that have been added or modified since the
     * last call to scan. This will ignore all files or directories starting with
     * the '.' character.
     *
     * @return Collection of {@link DeploymentUnit}s that describe newly added files or directories.
     */
    public Collection<DeploymentUnit> scan() {
        // Checks to see if we have deleted any of the deployment units.
        List<File> removedFiles = new ArrayList<File>();
        for (DeploymentUnit unit : scannedDeploymentUnits.values()) {
            if (!unit.getPath().exists() || !unit.getPath().canRead()) {
                removedFiles.add(unit.getPath());
            }
        }
        clear(removedFiles);

        // Checks for new files that don't start in '.'
        Collection<DeploymentUnit> result = new ArrayList<DeploymentUnit>();
        File files[] = pluginsDirectory.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        });

        if (files == null) {
            log.error("listFiles returned null for directory " + pluginsDirectory.getAbsolutePath());
            return result;
        }

        Arrays.sort(files); // sorts by filename for deterministic load order
        for (File file : files) {
            if (isScanned(file) && isModified(file)) {
                clear(file);
                DeploymentUnit unit = createAndStoreDeploymentUnit(file);
                if (unit != null)
                    result.add(unit);
            } else if (!isScanned(file)) {
                DeploymentUnit unit = createAndStoreDeploymentUnit(file);
                if (unit != null)
                    result.add(unit);
            }
        }
        return result;
    }

    private boolean isModified(File file) {
        DeploymentUnit unit = locateDeploymentUnit(file);
        return file.lastModified() > unit.lastModified();
    }

    private void clear(List<File> toUndeploy) {
        for (File aToUndeploy : toUndeploy) {
            clear(aToUndeploy);
        }
    }

    /**
     * Retrieve all the {@link DeploymentUnit}s currently stored.
     *
     * @return the complete unmodifiable list of scanned {@link DeploymentUnit}s.
     */
    public Collection<DeploymentUnit> getDeploymentUnits() {
        return Collections.unmodifiableCollection(scannedDeploymentUnits.values());
    }

    /**
     * Clears the list of scanned deployment units.
     */
    public void reset() {
        scannedDeploymentUnits.clear();
    }

    public void remove(DeploymentUnit unit) throws PluginException {
        if (unit.getPath().exists()) {
            if (!unit.getPath().delete()) {
                throw new PluginException("Unable to delete file: " + unit.getPath());
            }
        } else {
            log.debug("Plugin file <" + unit.getPath().getPath() + "> doesn't exist to delete.  Ignoring.");
        }

        clear(unit.getPath());
    }
}
