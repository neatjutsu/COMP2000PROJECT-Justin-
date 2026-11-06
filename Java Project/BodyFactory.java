import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//Factory pattern: callers ask for a kind of body and never need to know
//which concrete class (Star, Planet, BlackHole...) gets built.
public final class BodyFactory {
    private static int spawned = 0;
    private static final Random RANDOM = new Random();

    private BodyFactory() {}

    static Body create(BodyType type, String name, double mass, Location l, double radius, Velocity v) {
        switch (type) {
            case STAR:       return new Star(name, mass, l, radius, v);
            case PLANET:     return new Planet(name, mass, l, radius, v);
            case MOON:       return new Moon(name, mass, l, radius, v);
            case METEOR:     return new Meteor(name, mass, l, radius, v);
            case BLACK_HOLE: return new BlackHole(name, mass, l, radius, v);
            default:         throw new IllegalArgumentException("Unknown body type: " + type);
        }
    }

    //Ready-made recipes for bodies the user spawns by clicking
    static Body spawnAt(BodyType type, double x, double y) {
        spawned++;
        switch (type) {
            case METEOR:
                return create(type, "Meteor " + spawned, 0.5, new Location(x, y), 2, new Velocity(0, 0));
            case BLACK_HOLE:
                return create(type, "Black Hole " + spawned, 2000000, new Location(x, y), 6, new Velocity(0, 0));
            default:
                throw new IllegalArgumentException("Cannot spawn " + type + " by clicking");
        }
    }

    //A ring of rocks, each already moving on a circular orbit around `centre`.
    //Built with a stream: one pipeline from "how many" to "list of finished bodies".
    static List<Body> asteroidBelt(Body centre, int count, double innerRadius, double outerRadius) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    double angle = RANDOM.nextDouble() * 2 * Math.PI;
                    double r = innerRadius + RANDOM.nextDouble() * (outerRadius - innerRadius);
                    Location l = new Location(centre.l.x + r * Math.cos(angle),
                                              centre.l.y + r * Math.sin(angle));
                    Body rock = create(BodyType.METEOR, "Asteroid " + (++spawned), 0.5, l, 2, new Velocity(0, 0));
                    rock.v = Physics.circularOrbit(rock, centre);
                    return rock;
                })
                .collect(Collectors.toList());
    }
}
