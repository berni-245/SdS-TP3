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
            event.performEvent(sqSize);
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
        EventType cornerCollision = getNextCornerCollision(p, iniTime);

        EventType toReturn = horCollision.compareTo(verCollision) < 0 ? horCollision : verCollision;
        return toReturn.compareTo(cornerCollision) < 0 ? toReturn : cornerCollision;
    }


    private EventType getNextHorCollision(Particle p, double iniTime) {
        double parRightEdge = p.getX() + particleRadius;
        double speedX = p.getSpeedX();
        if (Double.compare(speedX, 0) == 0) {
            return new EventType(Double.MAX_VALUE, p, WallCollisionType.HORIZONTAL_COLLISION);
        }

        if (speedX < 0) {
            double tSqLeftWall = ParticleURM.LEFT.calcTime(p, iniTime, 0);
            return new EventType(tSqLeftWall, p, WallCollisionType.HORIZONTAL_COLLISION);
        }
        if (parRightEdge < sqSize) {
            double tSqRightWall = ParticleURM.RIGHT.calcTime(p, iniTime, sqSize);
            double yCenterAtT = p.getY() + p.getSpeedY() * (tSqRightWall - iniTime);
            if (yCenterAtT - particleRadius < rectLowerWall || yCenterAtT + particleRadius >= rectUpperWall) {
                return new EventType(tSqRightWall, p, WallCollisionType.HORIZONTAL_COLLISION);
            }
        }
        double t = ParticleURM.RIGHT.calcTime(p, iniTime, rectRightWall);
        return new EventType(t, p, WallCollisionType.HORIZONTAL_COLLISION);
    }

    private EventType getNextVerCollision(Particle p, double iniTime) {
        double deltaT = 0.00001; // to ensure the event never happens at the same time
        double speedY = p.getSpeedY();
        if (Double.compare(speedY, 0) == 0) {
            return new EventType(Double.MAX_VALUE, p, WallCollisionType.VERTICAL_COLLISION);
        }

        if (speedY < 0) {
            double tRectLowerWall = ParticleURM.DOWN.calcTime(p, iniTime, rectLowerWall) + deltaT;
            double xCenterAtT = p.getX() + p.getSpeedX() * (tRectLowerWall - iniTime);
            if (tRectLowerWall >= epoch && xCenterAtT >= sqSize)
                return new EventType(tRectLowerWall, p, WallCollisionType.VERTICAL_COLLISION);
            return new EventType(
                    ParticleURM.DOWN.calcTime(p, iniTime, 0),
                    p, WallCollisionType.VERTICAL_COLLISION
            );
        }
        double tRectUpperWall = ParticleURM.UP.calcTime(p, iniTime, rectUpperWall) + deltaT;
        double xCenterAtT = p.getX() + p.getSpeedX() * (tRectUpperWall - iniTime);
        if (tRectUpperWall >= epoch && xCenterAtT >= sqSize)
            return new EventType(tRectUpperWall, p, WallCollisionType.VERTICAL_COLLISION);
        return new EventType(
                ParticleURM.UP.calcTime(p, iniTime, sqSize),
                p, WallCollisionType.VERTICAL_COLLISION
        );
    }

    private EventType getNextCornerCollision(Particle p, double iniTime) {
        double deltaT = 0.00001; // to ensure the event never happens at the same time
        double upperCornerCollision = calculateCornerCollision(p, iniTime, rectUpperWall) + deltaT;
        double lowerCornerCollision = calculateCornerCollision(p, iniTime, rectLowerWall) + deltaT;
        if (upperCornerCollision >= epoch && lowerCornerCollision < epoch)
            return new EventType(upperCornerCollision, p, WallCollisionType.VERTICAL_COLLISION);
        if (lowerCornerCollision >= epoch && upperCornerCollision < epoch)
            return new EventType(lowerCornerCollision, p, WallCollisionType.VERTICAL_COLLISION);
        if (upperCornerCollision < epoch && lowerCornerCollision < epoch)
            return new EventType(Double.MAX_VALUE, p, WallCollisionType.VERTICAL_COLLISION);
        return new EventType(Math.min(lowerCornerCollision, upperCornerCollision), p, WallCollisionType.VERTICAL_COLLISION);
    }

    private double calculateCornerCollision(Particle p, double iniTime, double rectWallPos) {
        double dx = p.getX() - sqSize;
        double dy = p.getY() - rectWallPos;
        double a = Math.pow(p.getSpeedX(), 2) + Math.pow(p.getSpeedY(), 2);
        double b = 2 * (dx * p.getSpeedX() + dy * p.getSpeedY());
        double c = Math.pow(dx, 2) + Math.pow(dy, 2) - Math.pow(particleRadius, 2);

        double disc = Math.sqrt(Math.pow(b, 2) - 4 * a * c);
        if (Double.isNaN(disc)) {
            return -1;
        }
        double t1 = (-b + disc) / (2 * a) + iniTime;
        double t2 = (-b - disc) / (2 * a) + iniTime;

        if (t1 > 0) {
            if (t2 < 0) return t1;
            return Math.min(t1, t2);
        }
        return t2;
    }

    public List<Particle> getParticles() { return particles; }
}