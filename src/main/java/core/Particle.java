package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Particle {
    private static int globalId = 1;
    private final int id;
    private double x, y;
    private double speedx, speedy;

    public Particle(double x, double y, double direction, double speed) {
        this.id = globalId++;
        this.x = x;
        this.y = y;
        this.speedx = speed*Math.cos(direction);
        this.speedy = speed*Math.sin(direction);
    }

    public double getDistance(Particle p, double L) {
        return Math.sqrt(
                    Math.pow(
                            p.x - x, 2
                    ) +
                    Math.pow(
                            p.y - y, 2
                    )
        );
    }

    public void move(double L) {
        x = x + speedx;
        y = y + speedy;

        // Readjust particle if out of bounds
        x = x % L;
        if (x < 0) {
            x += L;
        }
        y = y % L;
        if (y < 0) {
            y += L;
        }
    }

    public double getSpeedX() {
        return speedx;
    }

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
        return "%d;%.2f;%.2f;%.2f;%.2f".formatted(id, x, y, speedx, speedy);
    }

    public int getId() {
        return id;
    }

    public void updateSpeedX() {
        speedx = -speedx;
    }
    public void updateSpeedY() {
        speedy = -speedy;
    }
}
