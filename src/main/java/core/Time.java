package core;

import java.util.List;

public class Time {
    private final double time;
    private final List<Particle> particles;

    public Time(double time, List<Particle> particles) {
        this.time = time;
        this.particles = particles;
    }

    public double getTime() {
        return time;
    }
    public List<Particle> getParticles() {
        return particles;
    }
}
