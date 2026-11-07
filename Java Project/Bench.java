import java.lang.reflect.*;
import java.util.*;

//Benchmark for worksheet 2.3: how long does one animation frame take with N bodies?
//Run it twice and write both sets of numbers in your worksheet:
//  1. As-is (threshold 64: parallel for 64+ bodies)
//  2. After changing PARALLEL_THRESHOLD in SimulationPanel.java to 1000000 (always sequential)
//Compile:  javac *.java      Run:  java -Djava.awt.headless=true Bench
public class Bench {
    public static void main(String[] a) throws Exception {
        System.out.println("CPU cores: " + Runtime.getRuntime().availableProcessors());
        for (int n : new int[]{20, 64, 130, 300, 600}) {
            Body sun = BodyFactory.create(BodyType.STAR, "Sun", 1000000, new Location(0, 0), 10, new Velocity(0, 0));
            List<Body> l = new ArrayList<>();
            l.add(sun);
            l.addAll(BodyFactory.asteroidBelt(sun, n - 1, 250, 420));
            SimulationPanel p = new SimulationPanel(l);
            Field tf = SimulationPanel.class.getDeclaredField("timer");
            tf.setAccessible(true);
            ((javax.swing.Timer) tf.get(p)).stop();
            Method tick = SimulationPanel.class.getDeclaredMethod("tick");
            tick.setAccessible(true);
            for (int i = 0; i < 50; i++) tick.invoke(p);   // warm-up
            long t0 = System.nanoTime();
            for (int i = 0; i < 200; i++) tick.invoke(p);
            System.out.printf("n=%d: %.2f ms/frame%n", n, (System.nanoTime() - t0) / 1e6 / 200);
        }
        System.exit(0);
    }
}
