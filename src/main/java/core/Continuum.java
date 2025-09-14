package core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Continuum implements Iterable<Time> {
    private final double sqSize;
    private final double L;
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
        this.sqSize = sqSize;
        this.L = L;
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

        if (parX >= rectRightWall || parX < 0 || parY >= sqSize || parY < 0)
            throw new IndexOutOfBoundsException("The particle doesn't fit on the space");

        if (parX >= sqSize && (parY >= rectUpperWall || parY < rectLowerWall))
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
            double eventTime = event.getT();
            for (Particle particle : particles) {
                particle.move(eventTime);
            }
            event.performEvent();

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
            epoch = event.getT();
            return new Time(epoch, particles);
        }


    }

    private void findParticleEvent(Particle particle) {
        EventType closestEvent = getNextWallCollision(particle);
        for (Particle particle2 : particles) {
            if (!particle2.equals(particle)) {
                EventType ev = particle.estimateCollision(particle2);
                if (ev != null) {
                    if (ev.compareTo(closestEvent) < 0) {
                        closestEvent = ev;
                    }
                }
            }
        }

        eventHandler.addEvent(closestEvent);
    }

    private EventType getNextWallCollision(Particle particle) {
        double x = particle.getX();
        double y = particle.getY();
        double radius = particle.getRadius();
        double parLeftEdge = x - radius;
        double parRightEdge = x + radius;
        double parUpperEdge = y - radius;
        double parLowerEdge = y + radius;
        double speedx = particle.getSpeedX();
        double speedy = particle.getSpeedY();

        double tr,yr;
        if(speedx > 0){
            if(x < sqSize) {
                tr = (sqSize - (parRightEdge)) / speedx;
                yr = y + speedy * tr;
                if (yr + radius > sqSize) {
                    return new EventType((sqSize - parUpperEdge) / speedy, particle, WallCollisionType.VERTICAL_COLLISION); //Colli Vert en t=(H-(y+r))/speedy
                } else if (yr - radius < 0) {
                    return new EventType(-parLowerEdge / speedy, particle, WallCollisionType.VERTICAL_COLLISION); //Colli Vert en t=-(y-r)/speedy
                } else if ((parUpperEdge > rectUpperWall) || parLowerEdge < rectLowerWall) {
                    return new EventType(tr, particle, WallCollisionType.HORIZONTAL_COLLISION);//Collision Horizontal en tr
                }
            }
            tr = (rectRightWall - parRightEdge) / speedx;
            yr = y + speedy * tr;
            if (yr - radius < rectLowerWall) {
                return new EventType((rectLowerWall - parLowerEdge) / speedy, particle, WallCollisionType.VERTICAL_COLLISION);
            } else if (yr + radius > rectUpperWall) {
                return new EventType((rectUpperWall - parUpperEdge) / speedy, particle, WallCollisionType.VERTICAL_COLLISION);
            }
            return new EventType(tr, particle, WallCollisionType.HORIZONTAL_COLLISION);
        }
        if(x > sqSize){
            tr = (sqSize - parLeftEdge) / speedx;
            yr = y + speedy * tr;
            if(yr + radius > rectUpperWall){
                return new EventType((rectUpperWall - parUpperEdge) / speedy, particle, WallCollisionType.VERTICAL_COLLISION);
            }
            if(yr - radius < rectLowerWall){
                return new EventType((rectLowerWall - parLowerEdge) / speedy, particle, WallCollisionType.VERTICAL_COLLISION);
            }
        }
        double tx = -parLeftEdge / speedx;
        double ty;
        if(speedy < 0)
            ty = -parLowerEdge / speedy;
        else
            ty = sqSize - parUpperEdge/speedy;
        if (tx < ty)
            return new EventType(tx, particle, WallCollisionType.HORIZONTAL_COLLISION);
        else
            return new EventType(ty, particle, WallCollisionType.VERTICAL_COLLISION);
    }
}