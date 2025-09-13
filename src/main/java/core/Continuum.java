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

    public List<Particle> getParticles() {
        return particles;
    }

    private void findParticleEvent(Particle particle) {
        EventType ec = null;
        EventType wc = getNextWallCollision(particle);
        for (Particle particle2 : particles) {
            if (!particle2.equals(particle)) {
                EventType ev = particle.estimateCollision(particle2);
                if (ev != null) {
                    if (ec == null || ec.compareTo(ev) > 0) {
                        ec = ev;
                    }
                }
            }
        }
        if (ec != null)
            eventHandler.addEvent(ec.compareTo(wc) > 0 ? wc : ec);
        else
            eventHandler.addEvent(wc);
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
                //TODO: Falta ver si hay uno o mas P3 que piensa/n
                // que se va a chocar con P1 o P2
            } else {
                mustRevalidate = eventHandler.invalidateParticleEvent(event.getP1());
                findParticleEvent(event.getP1());
            }
            mustRevalidate.forEach(Continuum.this::findParticleEvent);
            epoch = event.getT();
            return new Time(epoch, particles);
        }

    }
    @Override
    public Iterator<Time> iterator() {
        return new TimeIterator();
    }

    private EventType getNextWallCollision(Particle particle) {
        double x = particle.getX();
        double y = particle.getY();
        double speedx = particle.getSpeedX();
        double speedy = particle.getSpeedY();
        double radius = particle.getRadius();

        double tr,yr;
        if(speedx > 0){
            if(x < sqSize) {
                tr = (sqSize - (x + radius)) / speedx;
                yr = y + speedy * tr;
                if (yr + radius > sqSize) {
                    return new EventType((sqSize - (y + radius)) / speedy, particle, WallCollisionType.VERTICAL_COLLISION); //Colli Vert en t=(H-(y+r))/speedy
                } else if (yr - radius < 0) {
                    return new EventType(-(y - radius) / speedy, particle, WallCollisionType.VERTICAL_COLLISION); //Colli Vert en t=-(y-r)/speedy
                } else if ((y + radius > sqSize - ((sqSize - L) / 2)) || y - radius < (sqSize - L) / 2) {
                    return new EventType(tr, particle, WallCollisionType.HORIZONTAL_COLLISION);//Collision Horizontal en tr
                } else {
                    tr = (rectRightWall - (x + radius)) / speedx;
                    yr = y + speedy * tr;
                    if (yr - radius < (sqSize - L) / 2) {
                        return new EventType(((sqSize - L) / 2 - (y - radius)) / speedy, particle, WallCollisionType.VERTICAL_COLLISION);
                    } else if (yr + radius > sqSize - (sqSize - L) / 2) {
                        return new EventType((sqSize - (sqSize - L) / 2 - (y + radius)) / speedy, particle, WallCollisionType.VERTICAL_COLLISION);
                    }
                    return new EventType(tr, particle, WallCollisionType.HORIZONTAL_COLLISION);
                }
            }
        }

        if(x > sqSize){
            tr = (sqSize - (x+radius)) / speedx;
            yr = y + speedy * tr;
            if(yr+radius>sqSize-(sqSize-L)/2){
                return new EventType(((sqSize-(sqSize-L)/2)-(y+radius))/speedy,particle, WallCollisionType.VERTICAL_COLLISION);
            }
            if(yr - radius < (sqSize-L)/2){
                return new EventType(((sqSize-L)/2-(y-radius))/speedy,particle, WallCollisionType.VERTICAL_COLLISION);
            }
        }
        //El max no es un typo. Es para diferenciar por el signo de speedy. Unos va a ser + y el otro negativo segun la direccion vertical
        return new EventType(Math.min(-(x-radius)/speedx,Math.max(sqSize-(y+radius)/speedy,radius-y/speedy)), particle, WallCollisionType.HORIZONTAL_COLLISION) ;
        // TODO puse Horizontal Collision pero realmente no entiendo este choclo, hablar con @AlekDG
    }
}