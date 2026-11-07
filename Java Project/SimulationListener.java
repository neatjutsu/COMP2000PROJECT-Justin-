import java.util.List;

//Observer pattern: the observer interface. SimulationPanel (the subject)
//calls these methods when something happens. Default methods mean a
//listener only has to override the events it actually cares about.
public interface SimulationListener {
    default void onBodySpawned(Body body) {}
    default void onBodyConsumed(Body body, Body consumedBy) {}
    default void onTick(List<? extends Body> bodies) {}
}
