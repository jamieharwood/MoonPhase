package org.iHarwood.MoonPhaseModule;

/**
 * Registry of all planetary orbital calculators.
 * Replaces the individual MercuryDistance, VenusDistance, MarsDistance, etc. classes
 * with a single parameterised {@link PlanetDistance} per body.
 */
public final class Planets {
    private Planets() {}

    public static final PlanetDistance MERCURY = new PlanetDistance(
            "Mercury", 0.38709927, 0.20563593, 174.7947670, 4.09233445, 29.12427);

    public static final PlanetDistance VENUS = new PlanetDistance(
            "Venus", 0.72333566, 0.00677672, 50.37663, 1.60213034, 54.85229);

    public static final PlanetDistance MARS = new PlanetDistance(
            "Mars", 1.523679, 0.0934, 19.3870, 0.5240207766, 336.04084);

    public static final PlanetDistance JUPITER = new PlanetDistance(
            "Jupiter", 5.2026, 0.0489, 20.0202, 0.0831294, 14.75385);

    public static final PlanetDistance SATURN = new PlanetDistance(
            "Saturn", 9.5549, 0.0557, 317.0207, 0.0334442, 92.43194);

    public static final PlanetDistance URANUS = new PlanetDistance(
            "Uranus", 19.18916464, 0.04716771, 142.238599, 0.01176904, 96.93735);

    public static final PlanetDistance NEPTUNE = new PlanetDistance(
            "Neptune", 30.06992276, 0.00858587, 256.228347, 0.00598103, 276.33630);

    public static final PlanetDistance PLUTO = new PlanetDistance(
            "Pluto", 39.48211675, 0.24882730, 14.53, 0.003975, 224.0);
}
