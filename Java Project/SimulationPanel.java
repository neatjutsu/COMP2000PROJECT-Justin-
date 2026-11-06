import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

//Draws the celestial bodies and animates them using real gravity each frame.
//Also the "subject" of the Observer pattern: it tells its listeners when
//bodies are spawned or consumed, and after every frame.
public class SimulationPanel extends JPanel {

    private static final double DISTANCE_SCALE = 0.14;
    private static final double SIZE_SCALE = 0.3;
    private static final int MIN_RADIUS_PX = 2;

    private static final int TICK_DELAY_MS = 30;
    //Time scale. With the team's gravitational constant Earth needs about 1e9
    //time units for one orbit. 20 steps of 1e5 per frame (2e6 per frame) makes
    //that roughly 500 frames, i.e. an Earth year of about 15 seconds.
    private static final int STEPS_PER_TICK = 20;
    private static final double DT = 1e5;

    //Below this many bodies, starting threads costs more than it saves
    static final int PARALLEL_THRESHOLD = 64;
    //Ignore pairs closer than this, so two bodies on the same spot don't divide by zero
    private static final double MIN_DISTANCE = 1e-9;

    //How many past positions are kept to draw each body's trail
    private static final int TRAIL_LENGTH = 150;

    private final List<Body> bodies;
    private final List<SimulationListener> listeners = new ArrayList<>();
    private final Map<Body, Deque<double[]>> trails = new HashMap<>();
    private final Timer timer;
    private boolean paused = false;

    SimulationPanel(List<? extends Body> bodies){
        this.bodies = new ArrayList<>(bodies);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(1000,600));

        //Left click spawns a meteor, right click spawns a black hole
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double simX = (e.getX() - getWidth() / 2) / DISTANCE_SCALE;
                double simY = (e.getY() - getHeight() / 2) / DISTANCE_SCALE;
                BodyType type = (e.getButton() == MouseEvent.BUTTON3) ? BodyType.BLACK_HOLE : BodyType.METEOR;

                try {
                    Body spawned = BodyFactory.spawnAt(type, simX, simY);
                    SimulationPanel.this.bodies.add(spawned);
                    listeners.forEach(l -> l.onBodySpawned(spawned));
                    repaint();
                } catch (InvalidBodyException ex) {
                    System.err.println("Could not create body: " + ex.getMessage());
                }
            }
        });

        timer = new Timer(TICK_DELAY_MS, e -> tick());
        timer.start();
    }

    void addListener(SimulationListener listener){
        listeners.add(listener);
    }

    void togglePause(){
        paused = !paused;
    }

    boolean isPaused(){
        return paused;
    }

    //Drops a ring of asteroids in orbit around the heaviest body that is not a
    //black hole (the Sun). Adds enough bodies to switch gravity to its parallel path.
    void addAsteroidBelt(){
        bodies.stream()
                .filter(b -> !(b instanceof BlackHole))
                .max(Comparator.comparingDouble((Body b) -> b.mass))
                .ifPresent(centre -> {
                    for (Body rock : BodyFactory.asteroidBelt(centre, 120, 250, 420)){
                        bodies.add(rock);
                        listeners.forEach(l -> l.onBodySpawned(rock));
                    }
                });
        repaint();
    }

    private void tick(){
        if (!paused){
            for (int step = 0; step < STEPS_PER_TICK; step++){
                applyGravity();
                //Checked every step, not once per frame: otherwise a fast body
                //could jump clean over a black hole between two checks.
                handleBlackHoleCollisions();
            }
            recordTrails();
        }
        List<Body> view = Collections.unmodifiableList(bodies);
        listeners.forEach(l -> l.onTick(view));
        repaint();
    }

    //Two phases so it is safe to run in parallel:
    //  1. work out every body's acceleration. This only READS positions,
    //     and each task writes only its own slot (ax[i], ay[i]).
    //  2. apply the accelerations one at a time.
    private void applyGravity(){
        int n = bodies.size();
        Body[] snapshot = bodies.toArray(new Body[0]);
        double[] ax = new double[n];
        double[] ay = new double[n];

        IntStream indices = IntStream.range(0, n);
        if (n >= PARALLEL_THRESHOLD){
            indices = indices.parallel();
        }

        indices.forEach(i -> {
            for (int j = 0; j < n; j++){
                if (i == j) continue;
                double dx = snapshot[j].l.x - snapshot[i].l.x;
                double dy = snapshot[j].l.y - snapshot[i].l.y;
                if (Math.sqrt(dx * dx + dy * dy) < MIN_DISTANCE) continue;
                Velocity a = Physics.calculateGravity(snapshot[i], snapshot[j]);
                ax[i] += a.vx;
                ay[i] += a.vy;
            }
        });

        for (int i = 0; i < n; i++){
            Physics.updateBody(snapshot[i], new Velocity(ax[i], ay[i]), DT);
        }
    }

    private void handleBlackHoleCollisions(){
        Iterator<Body> it = bodies.iterator();
        while (it.hasNext()){
            Body body = it.next();
            if (body instanceof BlackHole) continue;

            for (Body source : bodies){
                if (!(source instanceof BlackHole)) continue;

                double dx = source.l.x - body.l.x;
                double dy = source.l.y - body.l.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < source.radius + body.radius){
                    try {
                        throw new CollisionException(body.name + " was consumed by " + source.name);
                    } catch (CollisionException ex) {
                        System.out.println(ex.getMessage());
                        it.remove();
                        trails.remove(body);
                        listeners.forEach(l -> l.onBodyConsumed(body, source));
                    }
                    break;
                }
            }
        }
    }

    private void recordTrails(){
        for (Body body : bodies){
            Deque<double[]> trail = trails.computeIfAbsent(body, b -> new ArrayDeque<>());
            trail.addLast(new double[]{body.l.x, body.l.y});
            if (trail.size() > TRAIL_LENGTH){
                trail.removeFirst();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        int centreX=getWidth()/2;
        int centreY=getHeight()/2;

        //Trails first, so the bodies are drawn on top. Older points are fainter.
        for (Map.Entry<Body, Deque<double[]>> entry : trails.entrySet()){
            Color c = entry.getKey().colour();
            double[][] points = entry.getValue().toArray(new double[0][]);
            for (int k = 1; k < points.length; k++){
                int alpha = 20 + (int) (140.0 * k / points.length);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
                g2.drawLine(centreX + (int) Math.round(points[k-1][0] * DISTANCE_SCALE),
                            centreY + (int) Math.round(points[k-1][1] * DISTANCE_SCALE),
                            centreX + (int) Math.round(points[k][0] * DISTANCE_SCALE),
                            centreY + (int) Math.round(points[k][1] * DISTANCE_SCALE));
            }
        }

        for (Body body : bodies){
            int x=centreX + (int) Math.round(body.l.x * DISTANCE_SCALE);
            int y=centreY + (int) Math.round(body.l.y * DISTANCE_SCALE);
            int r=Math.max(MIN_RADIUS_PX,(int) Math.round(body.radius * SIZE_SCALE));

            g2.setColor(body.colour());
            g2.fillOval(x-r,y-r,r*2,r*2);
            g2.drawString(body.name,x+r+4,y-r-4);
        }
    }
}
