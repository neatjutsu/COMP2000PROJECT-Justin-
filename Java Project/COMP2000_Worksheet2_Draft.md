# COMP2000 Worksheet 2 — Final Submission

**Student name:**
Justin Romald Baroi (Student ID: 60802286)

**GitHub repo URL:** (your own fork of your team's repository, not your team's URL)
https://github.com/neatjutsu/COMP2000PROJECT-Justin-.git

---

## 1. Design Patterns

Don't forget to submit a pdf file which shows a diagram of your pattern implemenation to supplement this text description.

*(Diagram: see `pattern_diagrams.pdf`. Page 1 is Observer, page 2 is Factory. Embed `observer.png` and `factory.png` here if only two files may be submitted.)*

**1.1.** Which design pattern(s) did you implement? Name them and state where each appears in your codebase.

**Observer pattern**
- `SimulationListener.java` — the observer interface (`onBodySpawned`, `onBodyConsumed`, `onTick`, all `default` methods so a listener only overrides what it cares about).
- `SimulationPanel.java` — the *subject*. Keeps a `List<SimulationListener>`, exposes `addListener(...)`, and notifies listeners when a body is spawned, when a black hole consumes a body, and after every frame.
- `SimulationWindow.java` — a *concrete observer*. It registers itself with the panel and updates a live status bar from the notifications.

**Factory pattern**
- `BodyType.java` — an enum of the kinds of body that can be built.
- `BodyFactory.java` — the factory. `create(...)` builds any `Body` subclass from a `BodyType`; `spawnAt(...)` holds the recipes for user-spawned bodies; `asteroidBelt(...)` builds a whole ring of orbiting rocks.
- `Main.java` — builds the entire solar system (Sun, eight planets, black hole) through the factory.
- `SimulationPanel.java` — asks the factory for a body when the user clicks (left = meteor, right = black hole) or adds an asteroid belt. It never names `Meteor` or `BlackHole`.

**1.2.** For each pattern, paste a code snippet showing the key interaction between participants.

*Observer:*
```java
// SimulationListener.java  (observer interface)
public interface SimulationListener {
    default void onBodySpawned(Body body) {}
    default void onBodyConsumed(Body body, Body consumedBy) {}
    default void onTick(List<? extends Body> bodies) {}
}

// SimulationPanel.java  (subject registers and notifies listeners)
void addListener(SimulationListener listener){
    listeners.add(listener);
}
...
it.remove();
listeners.forEach(l -> l.onBodyConsumed(body, source));

// SimulationWindow.java  (concrete observer registers itself and reacts)
SimulationPanel panel = new SimulationPanel(bodies);
panel.addListener(this);
...
@Override
public void onTick(List<? extends Body> bodies){
    String mode = bodies.size() >= SimulationPanel.PARALLEL_THRESHOLD ? "parallel" : "sequential";
    status.setText(SimulationStats.summarise(bodies) + "  |  Physics: " + mode + lastEvent);
}
```

*Factory:*
```java
// BodyFactory.java
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

// Main.java  (client: the whole solar system is built through the factory)
Planet planet = (Planet) BodyFactory.create(BodyType.PLANET, name, mass,
        new Location(distance, 0), radius, new Velocity(0,0));

// SimulationPanel.java  (client: no mention of Meteor or BlackHole)
BodyType type = (e.getButton() == MouseEvent.BUTTON3) ? BodyType.BLACK_HOLE : BodyType.METEOR;
Body spawned = BodyFactory.spawnAt(type, simX, simY);
SimulationPanel.this.bodies.add(spawned);
```

**1.3.** For each pattern, explain why you chose it over an alternative. What trade-off did you make?

*Observer.* The alternative was for `SimulationPanel` to hold a reference to `SimulationWindow` and call something like `window.updateStatus(...)` directly. That would tie the panel to one specific window class, so it could not be reused or tested without it, and every new reaction to an event (a log, a sound, a score) would mean editing the panel. With Observer the panel only knows the `SimulationListener` interface. The trade-off is extra indirection: one more interface, and it is less obvious from `SimulationPanel` alone who reacts to an event. Using `default` methods reduces the cost, since an observer only implements the events it needs. Notifications are synchronous and run on the Swing thread, so a slow listener would slow the animation; that is fine for a status bar but would not suit heavy work.

*Factory.* The alternative was calling `new Meteor(...)` / `new BlackHole(...)` directly wherever bodies are created. Centralising construction means callers do not know which concrete classes exist, and recipe values (mass, radius, naming) live in one place. The trade-off is that adding a new body type means editing the factory's `switch` as well as writing the class, so it is not fully open/closed. I accepted that because there are only five types. I did not use Abstract Factory or Builder because there is only one family of products and the constructors are short, so either would have been over-engineering. A static factory method was also simpler than a factory *object* because the factory holds no configuration to inject.

---

## 2. Streams / Lambdas / Parallelism

**2.1.** List every file in your project that uses lambda expressions or the Streams API.

- `Galaxy.java` — Streams API (`stream().sorted(...).collect(...)`) with a `Comparator` built from a lambda.
- `SimulationStats.java` — Streams API (`groupingBy`, `counting`, `mapToDouble`, `max`, `map`, `joining`) with several lambdas.
- `BodyFactory.java` — `IntStream.range(...).mapToObj(...).collect(...)` builds the asteroid belt.
- `SimulationPanel.java` — `IntStream` (parallel above a threshold) for gravity; a `stream().filter().max()` pipeline to pick the belt's centre; lambdas for the `Timer` callback, `listeners.forEach(...)` notifications, `computeIfAbsent`, and the `IntStream.forEach` body.
- `SimulationWindow.java` — lambdas as `ActionListener`s for the Pause and Asteroid belt buttons.
- `Main.java` — a lambda passed to `SwingUtilities.invokeLater(...)`.

**2.2.** For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?

```java
// SimulationStats.java
Map<String, Long> counts = bodies.stream()
        .collect(Collectors.groupingBy(
                b -> b.getClass().getSimpleName(),
                TreeMap::new,
                Collectors.counting()));

String breakdown = counts.entrySet().stream()
        .map(e -> e.getKey() + " " + e.getValue())
        .collect(Collectors.joining(", "));

double totalMass = bodies.stream().mapToDouble((Body b) -> b.mass).sum();

String heaviest = bodies.stream()
        .max(Comparator.comparingDouble((Body b) -> b.mass))
        .map(b -> b.name)
        .orElse("none");
```

This builds the live status-bar text, e.g. `Bodies: 10 (BlackHole 1, Planet 8, Star 1) | Total mass: 6.00e+06 | Heaviest: Sagittarius A*`. `groupingBy` groups bodies by class name and counts each group (a `TreeMap` keeps the kinds alphabetical). `mapToDouble(...).sum()` totals the mass, and `max(...)` finds the heaviest body, with `Optional.orElse` handling an empty list without a null check.

A loop version would need a `HashMap` with `containsKey`/`put` bookkeeping, a separate accumulator for mass, a "best so far" variable with null handling, and a `StringBuilder` with comma logic. The stream version states *what* is wanted (group, count, sum, max) instead of *how* to track it, so each statistic is one readable pipeline and cannot be broken by a forgotten reset or off-by-one. With about ten bodies the stream is not faster; the benefit is clarity and fewer places for bugs.

**2.3.** Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?

Yes — a parallel stream in `SimulationPanel.applyGravity()`. I did not write a custom collector or use Fork/Join directly (parallel streams run on the common Fork/Join pool implicitly).

```java
IntStream indices = IntStream.range(0, n);
if (n >= PARALLEL_THRESHOLD){      // 64
    indices = indices.parallel();
}
indices.forEach(i -> {
    for (int j = 0; j < n; j++){
        if (i == j) continue;
        ...
        Velocity a = Physics.calculateGravity(snapshot[i], snapshot[j]);
        ax[i] += a.vx;
        ay[i] += a.vy;
    }
});
// phase 2, sequential:
for (int i = 0; i < n; i++){
    Physics.updateBody(snapshot[i], new Velocity(ax[i], ay[i]), DT);
}
```

Thread safety comes from splitting the work into two phases. In phase 1 (parallel) positions are only *read*, and each task writes only its own slots `ax[i]` / `ay[i]`, so no two threads ever write the same memory and no locks are needed. `forEach` completes before phase 2, which mutates positions and velocities one body at a time on a single thread. I tested it by running 100 bodies (parallel path) against a plain sequential loop; positions matched exactly (maximum difference `0.0`). The original code updated each body as it went, so later bodies saw earlier bodies' new positions; the two-phase version updates everything from the same snapshot, which is both thread-safe and more accurate.

Gravity is O(n²), so cost grows quickly with the number of bodies. The default solar system has 10 bodies and uses the sequential path. The **Add asteroid belt** button adds 120 rocks, which switches the status bar to "Physics: parallel".

**Measured results** *(TODO — run `Bench.java` on your own computer twice, once as-is and once with `PARALLEL_THRESHOLD` set to 1000000, and fill in this table; cores = ____):*

| bodies | sequential (ms/frame) | parallel (ms/frame) |
|---|---|---|
| 64 | | |
| 130 | | |
| 300 | | |
| 600 | | |

*(Note from my own test on a machine with a single CPU core: parallel gave no speedup and was slightly slower at small sizes because of thread overhead, which is why a threshold exists. Sequential reached about 40 ms per frame at 600 bodies, over the 30 ms frame budget, which is where parallelism should matter on a multi-core machine. Report YOUR numbers, not these.)*

---

## 3. Uniqueness and Creativity

**3.1.** What did you add to your simulation that went beyond the in-class activities? Describe the feature and what inspired it.

*(Confirm which of these you personally wrote and which were your teammates'.)*
- A `BlackHole` body, and a `hasLife` flag on `Planet` that turns a planet green.
- A live n-body gravity animation (every body pulls on every other body) driven by a Swing `Timer`, with a **time scale chosen so an Earth year lasts about 15 seconds**.
- **Orbital velocities for every planet** (`Physics.circularOrbit`, speed = √(G·M/r)). Before this only Earth was moving.
- Orbit trails that fade with age.
- Black holes that consume any body that falls inside them, using `CollisionException`. The black hole **absorbs the consumed body's mass and momentum**, so it grows as it feeds and momentum is conserved.
- A glowing orange ring for black holes (a black disc on a black background is invisible), added through polymorphic `halo()` and `showLabel()` methods on `Body`, so the drawing code needs no type checks.
- Click-to-spawn (left click = meteor, right click = black hole), a Pause button, and an **asteroid belt** button (120 orbiting rocks).
- A live status bar: counts by type, total mass, the heaviest body, the latest consumption event, and whether physics is running in parallel.

[TODO — write what inspired it in your own words.]

**3.2.** Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.

*The surprising outcome: bodies could pass straight through a black hole.* After speeding the simulation up so orbits were watchable, a meteor placed *inside* a black hole survived. Each frame now covers many physics steps, and the collision check ran only once per frame, so a fast body could jump from one side of the black hole to the other between two checks (the simulation version of "tunnelling"). The fix was to check after every physics step:

```java
for (int step = 0; step < STEPS_PER_TICK; step++){
    applyGravity();
    //Checked every step, not once per frame: otherwise a fast body
    //could jump clean over a black hole between two checks.
    handleBlackHoleCollisions();
}
```

*The emergent behaviour:* the rules are simple — everything attracts everything, and a body inside a black hole's radius is removed and its mass and momentum are added to the black hole:

```java
private void absorb(Body blackHole, Body eaten){
    double total = blackHole.mass + eaten.mass;
    blackHole.v = new Velocity(
            (blackHole.mass * blackHole.v.vx + eaten.mass * eaten.v.vx) / total,
            (blackHole.mass * blackHole.v.vy + eaten.mass * eaten.v.vy) / total);
    blackHole.mass = total;
}
```

but the outcome is not scripted. In a headless 60-minute run, Earth held a steady orbit (distance from the Sun 100.0 after 45 seconds). The black hole, 3000 units away, slowly pulled the whole system towards it, and at about 3 minutes it consumed Saturn, then the Sun, then Earth. With the Sun gone, the other planets were on their own: Jupiter, Uranus and Neptune were **captured into orbit around the black hole** (still 2,000–5,500 units from it after 20, 40 and 60 minutes), while Mercury, Venus and Mars were **flung away** and kept drifting outward. The black hole did not consume everything. Its mass grew from 5.00e6 to 6.00e6. A black hole or asteroid belt added by the user changes the outcome again. My first version did not absorb momentum, and the black hole itself was flung away after the Sun vanished, which is why I added `absorb`.

[TODO — run it yourself and describe what YOU saw (which planets went first, what ended up orbiting the black hole). The numbers above come from my headless test, not from watching a window.]

---

**3.3.** If you had another 6 weeks, what would you add or change?

[TODO — your own ideas. Candidates that follow from the code: merge colliding planets conserving momentum instead of only black holes consuming; a better integrator (leapfrog/Verlet) or an adaptive time step instead of a fixed `DT`; a Barnes–Hut tree to make gravity O(n log n) instead of O(n²); speed controls built on the Observer pattern; a Strategy pattern for swapping physics models.]

---

## 4. Log Book and Final Reflection

**4.1.** Attach or link your log book entries for Weeks 7–13.

[TODO — your own logbook.]

**4.2.** Reflect on the semester:

- What was the most important concept you learned?

[TODO]

- What design trade-off did you face, and how did you resolve it?

[TODO — one real option from your own work: parallel gravity versus sequential (resolved with a size threshold and a two-phase design), the fixed time step (large enough to watch, small enough to catch collisions, resolved by checking every step), or a direct panel-to-window call versus Observer.]

- If you started over, what would you do differently?

[TODO — real experiences you could use: the simulation looked completely frozen at first (one Earth orbit would have taken about 21 hours of real time) and I only noticed after measuring it; my first Git commit was empty because I copied files from the wrong folder; my work sat on a branch while my fork's `main` stayed unchanged.]

- What feedback from the Week 7 submission did you act on?

[TODO — use the feedback you actually received on Submission 1 (82%).]
