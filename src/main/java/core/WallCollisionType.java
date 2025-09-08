package core;

public enum WallCollisionType {
    LEFT_SQUARE_LEFT_WALL {
        @Override
        public void updateParticle(Particle particle, double L, double B, double H) {
            particle.updateX(-particle.getX());
            particle.updateSpeedX(-particle.getSpeedX());
        }
    },
    LEFT_SQUARE_RIGHT_WALL {
        @Override
        public void updateParticle(Particle particle, double L, double B, double H) {
            particle.updateX(B - particle.getX());
            particle.updateSpeedX(-particle.getSpeedX());
        }
    },
    LEFT_SQUARE_TOP_WALL {
        @Override
        public void updateParticle(Particle particle, double L, double B, double H) {
            particle.updateY(2 * H - particle.getY());
            particle.updateSpeedY(-particle.getSpeedY());
        }
    },
    LEFT_SQUARE_BOTTOM_WALL {
        @Override
        public void updateParticle(Particle particle, double L, double B, double H) {
            particle.updateY(-particle.getY());
            particle.updateSpeedY(-particle.getSpeedY());
        }
    },

    RIGHT_SQUARE_RIGHT_WALL {
        @Override
        public void updateParticle(Particle particle, double L, double B, double H) {
            particle.updateX(2 * B - particle.getX());
            particle.updateSpeedX(-particle.getSpeedX());
        }
    },
    RIGHT_SQUARE_TOP_WALL {
        @Override
        public void updateParticle(Particle particle, double L, double B, double H) {
            particle.updateY(H + L - particle.getY());
            particle.updateSpeedY(-particle.getSpeedY());
        }
    },
    RIGHT_SQUARE_BOTTOM_WALL {
        @Override
        public void updateParticle(Particle particle, double L, double B, double H) {
            particle.updateY(H - L - particle.getY());
            particle.updateSpeedY(-particle.getSpeedY());
        }
    }
    ;

    public abstract void updateParticle(Particle particle, double L, double B, double H);
}
