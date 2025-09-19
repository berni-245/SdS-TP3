package core;

import tools.PostProcessor;

import java.util.List;

public enum WallCollisionType {
    VERTICAL_COLLISION {
        @Override
        public void updateParticle(Particle particle, double sqrSize, double time) {
            int wallId=particle.getX()>sqrSize?2:0;
            PostProcessor.processImpulse(2*Math.abs(particle.getSpeedY()),time,wallId);
            particle.updateSpeedY(-particle.getSpeedY());
        }
    },
    HORIZONTAL_COLLISION {
        @Override
        public void updateParticle(Particle particle, double sqrSize, double time) {
            double x=particle.getX();
            double rad=particle.getRadius();
            int wallId=x>sqrSize+2*rad?3:x<sqrSize-2*rad?0:1;
            PostProcessor.processImpulse(2*Math.abs(particle.getSpeedX()),time,wallId);
            particle.updateSpeedX(-particle.getSpeedX());
        }
    }
    ;

    public abstract void updateParticle(Particle particle, double sqrSize, double time);
}
