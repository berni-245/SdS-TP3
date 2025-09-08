package core;

public enum WallCollisionType {
    VERTICAL_COLLISION {
        @Override
        public void updateParticle(Particle particle) {
            particle.updateSpeedY(-particle.getSpeedY());
        }
    },
    HORIZONTAL_COLLISION {
        @Override
        public void updateParticle(Particle particle) {
            particle.updateSpeedX(-particle.getSpeedX());
        }
    }
    ;

    public abstract void updateParticle(Particle particle);
}
