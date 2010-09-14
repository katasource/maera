package org.maera.plugin.mock;

public class MockBear implements MockAnimal, MockThing {
    public int hashCode() {
        return 7;
    }

    public boolean equals(Object obj) {
        return obj instanceof MockBear;
    }
}
