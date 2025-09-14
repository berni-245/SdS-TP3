package core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Continuum implements Iterable<Time> {
    private final double sqSize;
    private double epoch;
    private final int maxEpoch;
    private final List<Particle> particles;
    private final EventHandler eventHandler;

    private final double particleRadius;
    private final double rectRightWall;
    private final double rectUpperWall;
    private final double rectLowerWall;

    public Continuum(double sqSize, double L, int particleCount, int maxEpoch, double fixedParticleRadius) {
        if (sqSize <= 0 || L <= 0 || maxEpoch <= 0)
            throw new IllegalArgumentException("rectSize, L, maxEpoch must be positive");
        if (L > sqSize)
            throw new IllegalArgumentException("L cannot be greater than sqSize");
        this.sqSize = sqSize;
        epoch = 0;
        this.maxEpoch = maxEpoch;
        this.particles = new ArrayList<>();
        this.eventHandler = new EventHandler(particleCount);

        this.particleRadius = fixedParticleRadius;
        this.rectRightWall = 2 * sqSize;
        double halfSqHeight = sqSize / 2;
        double halfL = L / 2;
        this.rectUpperWall = halfSqHeight + halfL;
        this.rectLowerWall = halfSqHeight - halfL;
    }

    public void addParticle(Particle particle) {
        double parX = particle.getX();
        double parY = particle.getY();
        double parRightEdge = parX + particleRadius;
        double parLeftEdge = parX - particleRadius;
        double parUpEdge = parY + particleRadius;
        double parDownEdge = parY - particleRadius;

        if (parRightEdge >= rectRightWall || parLeftEdge < 0 || parUpEdge >= sqSize || parDownEdge < 0)
            throw new IndexOutOfBoundsException("The particle doesn't fit on the space");

        if (parRightEdge >= sqSize && (parUpEdge >= rectUpperWall || parDownEdge < rectLowerWall))
            throw new IndexOutOfBoundsException("The particle doesn't fit on the space");

        particles.add(particle);
    }

    @Override
    public Iterator<Time> iterator() {
        return new TimeIterator();
    }

    private class TimeIterator implements Iterator<Time> {
        public TimeIterator() {
            for (Particle particle : particles) {
                findParticleEvent(particle);
            }
        }

        @Override
        public boolean hasNext() {
            return epoch < maxEpoch;
        }

        @Override
        public Time next() {
            EventType event = eventHandler.getNextValidEvent();
            double deltaT = event.getT() - epoch;
            for (Particle particle : particles) {
                particle.move(deltaT);
            }
            event.performEvent();
            epoch = event.getT();

            Set<Particle> mustRevalidate;
            if (event.hasP2()) {
                mustRevalidate = eventHandler.invalidateParticleEvent(event.getP1(), event.getP2());
                findParticleEvent(event.getP1());
                findParticleEvent(event.getP2());
            } else {
                mustRevalidate = eventHandler.invalidateParticleEvent(event.getP1());
                findParticleEvent(event.getP1());
            }
            mustRevalidate.forEach(Continuum.this::findParticleEvent);
            return new Time(epoch, particles);
        }


    }

    private void findParticleEvent(Particle particle) {
        EventType closestEvent = getNextWallCollision(particle, epoch);
        for (Particle particle2 : particles) {
            if (!particle2.equals(particle)) {
                EventType ev = particle.getCollisionWithParticle(particle2, epoch);
                if (ev.compareTo(closestEvent) < 0) {
                    closestEvent = ev;
                }
            }
        }
        eventHandler.addEvent(closestEvent);
    }

    private EventType getNextWallCollision(Particle p, double iniTime) {
        EventType horCollision = getNextHorCollision(p, iniTime);
        EventType verCollision = getNextVerCollision(p, iniTime);
        return horCollision.compareTo(verCollision) < 0 ? horCollision : verCollision;
    }


    private EventType getNextHorCollision(Particle p, double iniTime) {
        double parRightEdge = p.getX() + particleRadius;
        double speedX = p.getSpeedX();
        if (Double.compare(speedX, 0) == 0) {
            return new EventType(Double.MAX_VALUE, p, WallCollisionType.HORIZONTAL_COLLISION);
        }

        if (speedX < 0) {
            double t = ParticleURM.LEFT.calcTime(p, iniTime, 0);
            return new EventType(t, p, WallCollisionType.HORIZONTAL_COLLISION);
        }
        if(parRightEdge < sqSize) {
            double t = ParticleURM.RIGHT.calcTime(p, iniTime, sqSize);
            double yCenterAtT = p.getY() + p.getSpeedY() * (t - iniTime);
            if(yCenterAtT - particleRadius < rectLowerWall || yCenterAtT + particleRadius >= rectUpperWall){
                return new EventType(t, p, WallCollisionType.HORIZONTAL_COLLISION);
            }
        }
        double t = ParticleURM.RIGHT.calcTime(p, iniTime, rectRightWall);
        return new EventType(t, p, WallCollisionType.HORIZONTAL_COLLISION);
    }

    private EventType getNextVerCollision(Particle p, double iniTime) {
        double parRightEdge = p.getX() + particleRadius;
        double speedY = p.getSpeedY();
        if (Double.compare(speedY, 0) == 0) {
            return new EventType(Double.MAX_VALUE, p, WallCollisionType.VERTICAL_COLLISION);
        }

        if (parRightEdge < sqSize) {
            if (speedY < 0) {
                double t = ParticleURM.DOWN.calcTime(p, iniTime, 0);
                return new EventType(t, p, WallCollisionType.VERTICAL_COLLISION);
            }
            double t = ParticleURM.UP.calcTime(p, iniTime, sqSize);
            return new EventType(t, p, WallCollisionType.VERTICAL_COLLISION);
        }
        if (speedY < 0) {
            double t = ParticleURM.DOWN.calcTime(p, iniTime, rectLowerWall);
            return new EventType(t, p, WallCollisionType.VERTICAL_COLLISION);
        }
        double t = ParticleURM.UP.calcTime(p, iniTime, rectUpperWall);
        return new EventType(t, p, WallCollisionType.VERTICAL_COLLISION);
    }
}