import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

//Builds the status-bar summary using the Streams API
public final class SimulationStats {

    private SimulationStats() {}

    static String summarise(List<? extends Body> bodies) {
        //How many of each kind of body: {BlackHole=1, Planet=8, Star=1}
        Map<String, Long> counts = bodies.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getClass().getSimpleName(),
                        TreeMap::new,
                        Collectors.counting()));

        String breakdown = counts.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(", "));

        double totalMass = bodies.stream()
                .mapToDouble((Body b) -> b.mass)
                .sum();

        String heaviest = bodies.stream()
                .max(Comparator.comparingDouble((Body b) -> b.mass))
                .map(b -> b.name)
                .orElse("none");

        return String.format("Bodies: %d (%s)  |  Total mass: %.3g  |  Heaviest: %s",
                bodies.size(), breakdown, totalMass, heaviest);
    }
}
