package core;

public class Particle {
    private static int globalId = 1;
    private final int id;
    private double x, y;
    private double speedx, speedy;
    private final double radius;

    public Particle(double x, double y, double direction, double speed, double radius) {
        this.id = globalId++;
        this.x = x;
        this.y = y;
        this.speedx = speed * Math.cos(direction);
        this.speedy = speed * Math.sin(direction);
        this.radius = radius;
    }

    public double getDistance(Particle p) {
        return Math.sqrt(
                Math.pow(
                        p.x - x - radius - p.radius, 2
                ) +
                        Math.pow(
                                p.y - y - radius - p.radius, 2
                        )
        );
    }

    //TODO: This will probably put some particles inside a wall but it will be corrected when they're moved for the next event
    public void move(Particle p, double t) {
        double oldX1 = x;
        double oldY1 = y;
        double oldX2 = p.x;
        double oldY2 = p.y;
        x += speedx * t;
        y += speedy * t;
        p.x += p.speedx * t;
        p.y += p.speedy * t;
        if(oldX2>oldX1){
            double deltaX = x + radius - (p.x - p.radius);
            x -= deltaX;
            p.x += deltaX;
        } else {
            double deltaX = p.x + p.radius - (x - radius);
            x += deltaX;
            p.x -= deltaX;
        }
        speedx = -speedx;
        p.speedy = -p.speedx;
        if(oldY2>oldY1){
            double deltaY = y + radius - (p.y - p.radius);
            y -= deltaY;
            p.y += deltaY;
        } else {
            double deltaY = p.y + p.radius - (y - radius);
            y += deltaY;
            p.y -= deltaY;
        }
        speedy = -speedy;
        p.speedy = -p.speedy;
    }

    public void move(double L, double B, double H, double t) {
        double oldX = x;
        x = x + speedx*t;
        y = y + speedy*t;
        //Collide with the wall. TODO: Make this less ugly
        if (x+radius > B/2) {
            //Colision con el especio de la ranura
            if (y-radius < H / 2 - L / 2 || y+radius > H / 2 + L / 2) {
                //Colison Horizaontal
                if (oldX < B/2) {
                    x = B - x;
                    speedx = -speedx;
                } else {
                    //Colision vertical desde abajo
                    if (y+radius > H / 2 + L / 2)
                        y = H + L - y;
                        //Colision vertical desde arriba
                    else
                        y = H - L - y;
                    speedy = -speedy;
                }
            } else if (x+radius > B) {
                x = 2 * B - x;
                speedx = -speedx;
            }
        } else if (x-radius < 0) {
            x = -x;
            speedx = -speedx;
        }
        if (y+radius > H) {
            y = 2 * H - y;
            speedy = -speedy;
        } else if (y-radius < 0) {
            y = -y;
            speedy = -speedy;
        }
    }

    public void updateSpeedX(double speedX) {
        this.speedx = speedX;
    }

    public void updateSpeedY(double speedY) {
        this.speedy = speedY;
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
