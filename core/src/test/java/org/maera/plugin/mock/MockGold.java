package org.maera.plugin.mock;

public class MockGold implements MockMineral, MockThing {
    int weight;

    public MockGold() {
    }

    public MockGold(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MockGold)) return false;

        final MockGold mockGold = (MockGold) o;

        if (weight != mockGold.weight) return false;

        return true;
    }

    public int hashCode() {
        return weight;
    }
}
