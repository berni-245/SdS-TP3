package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Particle {
    private static int globalId = 1;
    private final int id;
    private double x, y;
    private double speedx, speedy;
    private double radius;

    public Particle(double x, double y, double direction, double speed, double radius) {
        this.id = globalId++;
        this.x = x;
        this.y = y;
        this.speedx = speed*Math.cos(direction);
        this.speedy = speed*Math.sin(direction);
        this.radius = radius;
    }

    public double getDistance(Particle p, double L) {
        return Math.sqrt(
                    Math.pow(
                            p.x - x - radius - p.radius, 2
                    ) +
                    Math.pow(
                            p.y - y - radius - p.radius, 2
                    )
        );
    }

    public void move(double L) {
        x = x + speedx;
        y = y + speedy;
        //Collide with the wall. TODO: Make this less ugly
        if (x > L) {
            x = 2 * L - x;
            speedx = -speedx;
        } else if (x < 0) {
            x = -x;
            speedx = -speedx;
        }
        if (y > L) {
            y = 2 * L - y;
            speedy = -speedy;
        } else if (y < 0) {
            y = -y;
            speedy = -speedy;
        }
    }

    public int getNextWallCollision(Double L) {
        double tx = Math.max(Math.ceil(x/speedx),Math.ceil((x-L)/speedx));
        double ty = Math.max(Math.ceil(y/speedy),Math.ceil((y-L)/speedy));
        return (int)Math.min(tx,ty);
    }

    public double getSpeedX() { return speedx; }

    public double getSpeedY() { return speedy; }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "%d: x=%.2f y=%.2f spx=%.2f spy=%.2f".formatted(getId(), x, y, speedx, speedy);
    }

    public String csvString() {
        return "%d;%.2f;%.2f;%.2f".formatted(id, x, y, Math.atan2(speedy,speedx));
    }

    public int getId() {
        return id;
    }
}
