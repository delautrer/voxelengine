package de.delautrer.game.world.generation.biome;

public class Climate {

    /**
     * Ein konkreter Klima-Punkt für einen Block (X, Z) in der Welt.
     */
    public static class TargetPoint {
        public final float temperature;
        public final float humidity;
        public final float continentalness;
        public final float erosion;
        public final float weirdness;

        public TargetPoint(float temperature, float humidity, float continentalness, float erosion, float weirdness) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.weirdness = weirdness;
        }
    }

    /**
     * Ein Bereich (Min bis Max), in dem sich ein Biom "wohlfühlt".
     */
    public static class Parameter {
        public final float min;
        public final float max;

        public Parameter(float min, float max) {
            this.min = min;
            this.max = max;
        }

        // Berechnet, wie weit ein Wert von diesem Wohlfühlbereich entfernt ist.
        // Liegt der Wert dazwischen, ist die Distanz 0!
        public float distanceTo(float value) {
            if (value < min) return min - value;
            if (value > max) return value - max;
            return 0f;
        }
    }
}