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

    public double getDistance(Particle p) {
        return Math.sqrt(
                Math.pow(
                        p.x - x, 2
                ) +
                Math.pow(
                        p.y - y, 2
                )
        ) - radius - p.radius;
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

    public EventType estimateCollision(Particle p) {
        double[] dr = {x - p.x, y - p.y};
        double[] dv = {speedx - p.speedx, speedy - p.speedy};
        double delta = dotProd(dv, dr);
        if(delta >= 0)
            return null;
        double deltaV = dotProd(dv, dv);
        double d = Math.pow(delta, 2) - deltaV * (dotProd(dr, dr) - Math.pow(radius + p.radius, 2));
        if(d < 0)
            return null;
        double t = -(delta + Math.sqrt(d)) / deltaV;
        return new EventType(t,this,p);
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
        return "%d;%.2f;%.2f;%.2f".formatted(id, x, y, Math.atan2(speedy, speedx));
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
