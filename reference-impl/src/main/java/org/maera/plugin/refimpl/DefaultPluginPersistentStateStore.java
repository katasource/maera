package org.maera.plugin.refimpl;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.maera.plugin.manager.DefaultPluginPersistentState;
import org.maera.plugin.manager.PluginPersistentState;
import org.maera.plugin.manager.PluginPersistentStateStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class DefaultPluginPersistentStateStore implements PluginPersistentStateStore {
    private static final Logger log = Logger.getLogger(DefaultPluginPersistentStateStore.class);

    private File file;

    public DefaultPluginPersistentStateStore(final File directory) {
        try {
            file = new File(directory.getParentFile(), "plugins.state");
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (final IOException e) {
            log.error("Error creating plugins.state file. " + e, e);
        }
    }

    public PluginPersistentState load() {
        final Map<String, Boolean> state = new HashMap<String, Boolean>();
        FileInputStream inputStream = null;
        try {
            final Properties properties = new Properties();
            inputStream = new FileInputStream(file);
            properties.load(inputStream);
            final Set<Object> keys = properties.keySet();
            for (final Object key : keys) {
                state.put(String.valueOf(key), Boolean.valueOf(String.valueOf(properties.get(key))));
            }
        } catch (final IOException e) {
            log.error("Error creating/reading plugins.state file. " + e, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return new DefaultPluginPersistentState(state);
    }

    public void save(final PluginPersistentState state) {
        final Properties properties = new Properties();
        final Set<Entry<String, Boolean>> entrySet = state.getMap().entrySet();
        for (final Entry<String, Boolean> entry : entrySet) {
            properties.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            properties.store(outputStream, "Saving plugins state");
        } catch (final IOException e) {
            log.error("Error saving to plugins.state file. " + e, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
