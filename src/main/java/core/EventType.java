package core;

public class EventType implements Comparable<EventType> {
    private final double t;
    private final Particle p1;
    private final Particle p2;
    private boolean isValid;

    // Wall collision
    public EventType(double t, Particle p) {
        if (p == null)
            throw new NullPointerException();
        this.t = t;
        this.p1 = p;
        this.p2 = null;
        isValid = true;
    }

    // Particle collision
    public EventType(double t, Particle p1, Particle p2) {
        if (p1 == null || p2 == null)
            throw new NullPointerException("Particles cannot be null");

        this.t = t;
        this.p1 = p1;
        this.p2 = p2;
    }

    public double getT() {
        return t;
    }

    public Particle getP1() {
        return p1;
    }

    public Particle getP2() {
        return p2;
    }

    public boolean hasP2(){
        return p2 != null;
    }

    @Override
    public int compareTo(EventType o) {
        return Double.compare(t, o.t);
    }

    public void invalidate() {
        isValid = false;
    }

    public boolean isValid() {
        return isValid;
    }
}
