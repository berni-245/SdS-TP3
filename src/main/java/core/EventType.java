package core;

public class EventType implements Comparable<EventType> {
    private final double t;
    private final Particle p1;
    private final Particle p2;
    private final WallCollisionType wallCollisionType;
    private boolean isValid;

    // Wall collision
    public EventType(double t, Particle p, WallCollisionType wallCollisionType) {
        if (p == null)
            throw new NullPointerException();
        this.t = t;
        this.p1 = p;
        this.wallCollisionType = wallCollisionType;
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
        wallCollisionType = null;
        isValid = true;
    }

    public void performEvent(double squareSize) {
        if (!isValid)
            throw new IllegalStateException("The event is no longer valid");

        if (wallCollisionType != null) { // wall collision
            wallCollisionType.updateParticle(p1,squareSize,t);
            return;
        }
        if (p2 != null) { // particle collision
            double deltaX = p2.getX() - p1.getX();
            double deltaY = p2.getY() - p1.getY();
            double deltaVX = p2.getSpeedX() - p1.getSpeedX();
            double deltaVY = p2.getSpeedY() - p1.getSpeedY();

            double deltaV_deltaR = deltaVX * deltaX + deltaVY * deltaY;
            double sigma = p1.getRadius() + p2.getRadius();

            double J = deltaV_deltaR / sigma; // since mi and mj are 1, I can simplify some things of the formula

            double Jx = J * deltaX / sigma;
            double Jy = J * deltaY / sigma;

            p1.updateSpeedX(p1.getSpeedX() + Jx);
            p1.updateSpeedY(p1.getSpeedY() + Jy);

            p2.updateSpeedX(p2.getSpeedX() - Jx);
            p2.updateSpeedY(p2.getSpeedY() - Jy);

        }
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
