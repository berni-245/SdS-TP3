package core;

public class Particle {
    private static int globalId = 1;
    private final int id;
    private double x, y;
    private double speedx, speedy;
    private final double radius;

    public Particle(double x, double y, double direction, double speed, double radius) {
        this.id = globalId++;
        this.x = x;
        this.y = y;
        this.speedx = speed * Math.cos(direction);
        this.speedy = speed * Math.sin(direction);
        this.radius = radius;
    }

    public void move(double deltaT) {
        x = x + speedx * deltaT;
        y = y + speedy * deltaT;
    }

    public void updateSpeedX(double speedX) {
        this.speedx = speedX;
    }

    public void updateSpeedY(double speedY) {
        this.speedy = speedY;
    }

    private double dotProd(double[] v1, double[] v2){
        return v1[0] * v2[0] + v1[1] * v2[1];
    }

    public EventType getCollisionWithParticle(Particle p, double iniTime) {
        double[] dr = {x - p.x, y - p.y};
        double[] dv = {speedx - p.speedx, speedy - p.speedy};
        double delta = dotProd(dv, dr);
        if(delta >= 0)
            return new EventType(Double.MAX_VALUE, this, p);
        double deltaV = dotProd(dv, dv);
        double d = Math.pow(delta, 2) - deltaV * (dotProd(dr, dr) - Math.pow(radius + p.radius, 2));
        if(d < 0)
            return new EventType(Double.MAX_VALUE, this, p);
        double t = -(delta + Math.sqrt(d)) / deltaV + iniTime;
        return new EventType(t,this, p);
    }

    public double getSpeedX() {
        return speedx;
    }

    public double getSpeedY() {
        return speedy;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public String toString() {
        return "%d: x=%.2f y=%.2f spx=%.2f spy=%.2f".formatted(getId(), x, y, speedx, speedy);
    }

    public String csvString() {
        return "%.8f,%.8f,%.8f,%.8f,%.8f".formatted(x, y, speedx, speedy, radius);
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Particle p && id == p.id;
    }
}
