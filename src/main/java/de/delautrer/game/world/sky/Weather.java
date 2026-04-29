package de.delautrer.game.world.sky;

public enum Weather {
    CLEAR(2.0f),
    PARTLY_CLOUDY(0.15f),
    OVERCAST(-0.1f);

    public final float cloudThreshold;

    Weather(float cloudThreshold) {
        this.cloudThreshold = cloudThreshold;
    }
}