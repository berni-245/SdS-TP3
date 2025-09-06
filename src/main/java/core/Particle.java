package core;

public class Particle {
    private static int globalId = 1;
    private final int id;
    private double x, y;
    private double speedx, speedy;
    private double radius;

    public Particle(double x, double y, double direction, double speed, double radius) {
        this.id = globalId++;
        this.x = x;
        this.y = y;
        this.speedx = speed * Math.cos(direction);
        this.speedy = speed * Math.sin(direction);
        this.radius = radius;
    }

    public double getDistance(Particle p, double L) {
        return Math.sqrt(
                Math.pow(
                        p.x - x - radius - p.radius, 2
                ) +
                        Math.pow(
                                p.y - y - radius - p.radius, 2
                        )
        );
    }


    public void move(double L, double B, double H, double t) {
        double oldX = x;
        x = x + speedx*t;
        y = y + speedy*t;
        //Collide with the wall. TODO: Make this less ugly
        if (x > L) {
            //Colision con el especio de la ranura
            if (y < H / 2 - L / 2 || y > H / 2 + L / 2) {
                //Colison Horizaontal
                if (oldX < B/2) {
                    x = B - x;
                    speedx = -speedx;
                } else {
                    //Colision vertical desde abajo
                    if (y > H / 2 + L / 2)
                        y = H + L - y;
                        //Colision vertical desde arriba
                    else
                        y = H - L - y;
                    speedy = -speedy;
                }
            } else if (x > B) {
                x = 2 * B - x;
                speedx = -speedx;
            }
        } else if (x < 0) {
            x = -x;
            speedx = -speedx;
        }
        if (y > H) {
            y = 2 * H - y;
            speedy = -speedy;
        } else if (y < 0) {
            y = -y;
            speedy = -speedy;
        }
    }

    public int getNextWallCollision(Double L) {
        double tx = Math.max(Math.ceil(x / speedx), Math.ceil((x - L) / speedx));
        double ty = Math.max(Math.ceil(y / speedy), Math.ceil((y - L) / speedy));
        return (int) Math.min(tx, ty);
    }

    public double getSpeedX() {
        return speedx;
    }

    public double getSpeedY() {
        return speedy;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "%d: x=%.2f y=%.2f spx=%.2f spy=%.2f".formatted(getId(), x, y, speedx, speedy);
    }

    public String csvString() {
        return "%d;%.2f;%.2f;%.2f".formatted(id, x, y, Math.atan2(speedy, speedx));
    }

    public int getId() {
        return id;
    }
}
