package org.maera.plugin.web.descriptors;

import java.util.Comparator;

/**
 * A simple comparator for any weighted descriptor - lowest weights first.
 */
public class WeightedDescriptorComparator implements Comparator<WeightedDescriptor> {
    public int compare(WeightedDescriptor w1, WeightedDescriptor w2) {
        if (w1.getWeight() < w2.getWeight())
            return -1;
        else if (w1.getWeight() > w2.getWeight())
            return 1;
        else
            return 0;
    }
}
