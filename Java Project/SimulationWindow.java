import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

//Simulation Window. Also an observer: it listens to the SimulationPanel
//and shows live statistics in a status bar.
public class SimulationWindow extends JFrame implements SimulationListener {

    private final JLabel status = new JLabel(" ");
    private String lastEvent = "";

    SimulationWindow(String title, List<? extends Body> bodies){
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        SimulationPanel panel = new SimulationPanel(bodies);
        panel.addListener(this);

        JButton pause = new JButton("Pause");
        pause.addActionListener(e -> {
            panel.togglePause();
            pause.setText(panel.isPaused() ? "Resume" : "Pause");
        });
        JButton belt = new JButton("Add asteroid belt (+120)");
        belt.addActionListener(e -> panel.addAsteroidBelt());
        JLabel help = new JLabel("   Left click: meteor   |   Right click: black hole");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(pause);
        controls.add(belt);
        controls.add(help);

        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(controls, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void onBodyConsumed(Body body, Body consumedBy){
        lastEvent = "  |  " + body.name + " was consumed by " + consumedBy.name;
    }

    @Override
    public void onTick(List<? extends Body> bodies){
        String mode = bodies.size() >= SimulationPanel.PARALLEL_THRESHOLD ? "parallel" : "sequential";
        status.setText(SimulationStats.summarise(bodies) + "  |  Physics: " + mode + lastEvent);
    }

    void start(){
        setVisible(true);
    }
}
