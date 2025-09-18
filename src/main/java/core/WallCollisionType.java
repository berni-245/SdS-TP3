package core;

import tools.PostProcessor;

import java.util.List;

public enum WallCollisionType {
    VERTICAL_COLLISION {
        @Override
        public void updateParticle(Particle particle, double sqrSize, double time) {
            int wallId=particle.getX()>sqrSize?0:2;
            PostProcessor.processImpulse(2*Math.abs(particle.getSpeedY()),time,wallId);
            particle.updateSpeedY(-particle.getSpeedY());
        }
    },
    HORIZONTAL_COLLISION {
        @Override
        public void updateParticle(Particle particle, double sqrSize, double time) {
            double x=particle.getX();
            double rad=particle.getRadius();
            int wallId=x>sqrSize+rad?3:x<sqrSize-rad?0:1;
            PostProcessor.processImpulse(2*Math.abs(particle.getSpeedX()),time,wallId);
            particle.updateSpeedX(-particle.getSpeedX());
        }
    }
    ;

    public abstract void updateParticle(Particle particle, double sqrSize, double time);
}
