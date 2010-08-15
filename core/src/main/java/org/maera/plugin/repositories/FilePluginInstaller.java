package org.maera.plugin.repositories;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.RevertablePluginInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;

/**
 * File-based implementation of a PluginInstaller which writes plugin artifact
 * to a specified directory.  Handles reverting installs by keeping track of the first installation for a given
 * instance, and restores it.  Installation of plugin artifacts with different names will overwrite an existing artifact
 * of that same name, if it exists, with the only exception being the backup of the first overwritten artifact to
 * support reverting.
 *
 * @see RevertablePluginInstaller
 */
public class FilePluginInstaller implements RevertablePluginInstaller {
    private File directory;
    private static final Logger log = LoggerFactory.getLogger(FilePluginInstaller.class);

    private final Map<String, OriginalFile> installedPlugins = CopyOnWriteMap.<String, OriginalFile>builder().stableViews().newHashMap();

    public static final String ORIGINAL_PREFIX = ".original-";

    /**
     * @param directory where plugin JARs will be installed.
     */
    public FilePluginInstaller(File directory) {
        Validate.isTrue(directory != null && directory.exists(), "The plugin installation directory must exist");
        this.directory = directory;
    }

    /**
     * If there is an existing JAR with the same filename, it is replaced.
     *
     * @throws RuntimeException if there was an exception reading or writing files.
     */
    public void installPlugin(String key, PluginArtifact pluginArtifact) {
        Validate.notNull(key, "The plugin key must be specified");
        Validate.notNull(pluginArtifact, "The plugin artifact must not be null");

        File newPluginFile = new File(directory, pluginArtifact.getName());
        try {
            backup(key, newPluginFile);
            if (newPluginFile.exists()) {
                // would happen if the plugin was installed for a previous instance
                newPluginFile.delete();
            }
        }
        catch (IOException e) {
            log.warn("Unable to backup old file", e);
        }

        OutputStream os = null;
        InputStream in = null;
        try {
            os = new FileOutputStream(newPluginFile);
            in = pluginArtifact.getInputStream();
            IOUtils.copy(in, os);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not install plugin: " + pluginArtifact, e);
        }
        finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * Reverts an installed plugin.  Handles plugin file overwrites and different names over time.
     *
     * @param pluginKey The plugin key to revert
     * @since 2.5.0
     */
    public void revertInstalledPlugin(String pluginKey) {
        OriginalFile orig = installedPlugins.get(pluginKey);
        if (orig != null) {
            File origFile = new File(orig.getBackupFile().getParent(), orig.getOriginalName());
            if (origFile.exists()) {
                origFile.delete();
            }

            if (orig.isUpgrade()) {
                try {
                    FileUtils.moveFile(orig.getBackupFile(), origFile);
                }
                catch (IOException e) {
                    log.warn("Unable to restore old plugin for " + pluginKey);
                }
            }
        }
    }

    /**
     * Deletes all backup files in the plugin directory
     *
     * @since 2.5.0
     */
    public void clearBackups() {
        for (File file : directory.listFiles(new BackupNameFilter())) {
            file.delete();
        }
        installedPlugins.clear();
    }

    private void backup(String pluginKey, File oldPluginFile) throws IOException {
        if (!installedPlugins.containsKey(pluginKey)) {
            OriginalFile orig;
            if (oldPluginFile.exists()) {
                File backupFile = new File(oldPluginFile.getParent(), ORIGINAL_PREFIX + oldPluginFile.getName());
                if (backupFile.exists()) {
                    throw new IOException("Existing backup found for plugin " + pluginKey + ".  Cannot install.");
                }

                FileUtils.copyFile(oldPluginFile, backupFile);
                orig = new OriginalFile(backupFile, oldPluginFile.getName());
            } else {
                orig = new OriginalFile(oldPluginFile, oldPluginFile.getName());
            }
            installedPlugins.put(pluginKey, orig);
        }
    }

    private static class BackupNameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.startsWith(ORIGINAL_PREFIX);
        }
    }

    private static class OriginalFile {
        private final File backupFile;
        private final String originalName;
        private final boolean isUpgrade;

        public OriginalFile(File backupFile, String originalName) {
            this.backupFile = backupFile;
            this.originalName = originalName;
            this.isUpgrade = !backupFile.getName().equals(originalName);
        }

        public File getBackupFile() {
            return backupFile;
        }

        public String getOriginalName() {
            return originalName;
        }

        public boolean isUpgrade() {
            return isUpgrade;
        }
    }
}
