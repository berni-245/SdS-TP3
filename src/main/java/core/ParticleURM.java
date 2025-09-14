package core;

// URM = Uniform Rectilinear Motion
public enum ParticleURM {
    UP {
        @Override
        public double calcTime(Particle p, double iniTime, double endPos) {
            return (endPos - (p.getY() + p.getRadius())) / p.getSpeedY() + iniTime;
        }
    },
    DOWN {
        @Override
        public double calcTime(Particle p, double iniTime, double endPos) {
            return (endPos - (p.getY() - p.getRadius())) / p.getSpeedY() + iniTime;
        }
    },
    LEFT {
        @Override
        public double calcTime(Particle p, double iniTime, double endPos) {
            return (endPos - (p.getX() - p.getRadius())) / p.getSpeedX() + iniTime;
        }
    },
    RIGHT {
        @Override
        public double calcTime(Particle p, double iniTime, double endPos) {
            return (endPos - (p.getX() + p.getRadius())) / p.getSpeedX() + iniTime;
        }
    }
    ;

    public abstract double calcTime(Particle p, double iniTime, double endPos);
}
