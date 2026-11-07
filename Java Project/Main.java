import javax.swing.SwingUtilities;

public class Main{

    //Builds the solar system. Every body comes from the BodyFactory, and every
    //planet is given the speed it needs to orbit the Sun.
    static Galaxy<Body> buildSolarSystem() throws DuplicateBodyException {
        Galaxy<Body> galaxy = new Galaxy<>();

        Body sun = BodyFactory.create(BodyType.STAR, "Sun", 1000000, new Location(0,0), 10, new Velocity(0,0));
        galaxy.add(sun);

        addPlanet(galaxy, sun, "Mercury", 1.660, 38.7, 2, false);
        addPlanet(galaxy, sun, "Venus", 24.47, 72.3, 2.5, false);
        addPlanet(galaxy, sun, "Earth", 30.03, 100, 2.7, true);
        addPlanet(galaxy, sun, "Mars", 3.227, 152.4, 2.2, false);
        addPlanet(galaxy, sun, "Jupiter", 954.6, 520.4, 6, false);
        addPlanet(galaxy, sun, "Saturn", 285.8, 957.4, 5, false);
        addPlanet(galaxy, sun, "Uranus", 43.66, 1921, 4, false);
        addPlanet(galaxy, sun, "Neptune", 51.51, 3002, 4, false);

        //A black hole far to the left. Its pull is weak at this distance, so the planets
        //orbit normally for a few minutes while the whole system slowly drifts towards it.
        galaxy.add(BodyFactory.create(BodyType.BLACK_HOLE, "Sagittarius A*", 5000000,
                new Location(-3000, 0), 8, new Velocity(0,0)));
        return galaxy;
    }

    private static void addPlanet(Galaxy<Body> galaxy, Body sun, String name, double mass,
                                  double distance, double radius, boolean hasLife) throws DuplicateBodyException {
        Planet planet = (Planet) BodyFactory.create(BodyType.PLANET, name, mass,
                new Location(distance, 0), radius, new Velocity(0,0));
        planet.hasLife = hasLife;
        planet.v = Physics.circularOrbit(planet, sun);
        galaxy.add(planet);
    }

    public static void main (String []args){
        Galaxy<Body> galaxy;
        try {
            galaxy = buildSolarSystem();
        } catch (DuplicateBodyException e) {
            System.err.println("Could not build the galaxy: " + e.getMessage());
            return;
        }

        SwingUtilities.invokeLater(() -> new SimulationWindow("Solar System", galaxy.byDescendingRadius()).start());
    }

}
