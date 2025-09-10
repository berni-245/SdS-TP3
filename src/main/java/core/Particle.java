package core;

import java.util.ArrayList;
import java.util.List;

public class Particle {
    private static int globalId = 1;
    private final int id;
    private double x, y;
    private double speedx, speedy;
    private final double radius;
    private final List<Particle> neighbors;

    public Particle(double x, double y, double direction, double speed, double radius) {
        this.id = globalId++;
        this.x = x;
        this.y = y;
        this.speedx = speed * Math.cos(direction);
        this.speedy = speed * Math.sin(direction);
        this.radius = radius;
        this.neighbors = new ArrayList<>();
    }

    public double getDistance(Particle p) {
        return Math.sqrt(
                Math.pow(
                        p.x - x, 2
                ) +
                        Math.pow(
                                p.y - y, 2
                        )
        ) - radius - p.radius;
    }

    public void move(double t) {
        x = x + speedx*t;
        y = y + speedy*t;
    }

    public void addNeighbor(Particle p) {
        neighbors.add(p);
    }

    public void updateSpeedX(double speedX) {
        this.speedx = speedX;
    }

    public void updateSpeedY(double speedY) {
        this.speedy = speedY;
    }

    private double dotProd(double[] v1, double[] v2){
        return v1[0] * v2[0] + v1[1] * v2[1];
    }

    public EventType estimateCollision(Particle p) {
        double[] dr = {x-p.x,y-p.y};
        double[] dv = {speedx-p.speedx,speedy-p.speedy};
        double delta = dotProd(dv,dr);
        if(delta >= 0)
            return null;
        double deltaV = dotProd(dv,dv);
        double d = Math.pow(delta,2) - deltaV*(dotProd(dr,dr)-Math.pow(radius+p.radius,2));
        if(d<0)
            return null;
        double t = - (delta+Math.sqrt(d))/deltaV;
        return new EventType(t,this,p);
    }

    public EventType getNextWallCollision(Double L, Double B, Double H) {
        double tr,yr;
        if(speedx > 0){
            if(x < B/2){
                tr = (B/2 - (x+radius)) / speedx;
                yr = y + speedy*tr;
                if(yr+radius>H){
                    return new EventType((H - (y+radius))/ speedy, this); //Colli Vert en t=(H-(y+r))/speedy
                }else if(yr - radius < 0){
                    return new EventType(-(y-radius)/speedy,this); //Colli Vert en t=-(y-r)/speedy
                }else if ((y+radius > H - ((H-L)/2))|| y-radius<(H-L)/2){
                    return new EventType(tr,this);//Collision Horizontal en tr
                }
            } else {
                tr = (B - (x+radius)) / speedx;
                yr = y + speedy * tr;
                if( yr - radius < (H-L)/2){
                    return new EventType(((H-L)/2 - (y-radius))/speedy,this);
                }else if(yr+radius > H - (H-L)/2){
                    return new EventType((H-(H-L)/2-(y+radius))/speedy,this);
                }
                return new EventType(tr,this);
            }

        }

        if(x > B/2){
            tr = (B/2 - (x+radius)) / speedx;
            yr = y + speedy * tr;
            if(yr+radius>H-(H-L)/2){
                return new EventType(((H-(H-L)/2)-(y+radius))/speedy,this);
            }
            if(yr - radius < (H-L)/2){
                return new EventType(((H-L)/2-(y-radius))/speedy,this);
            }
        }
        //El max no es un typo. Es para diferenciar por el signo de speedy. Unos va a ser + y el otro negativo segun la direccion vertical
        return new EventType(Math.min(-(x-radius)/speedx,Math.max(H-(y+radius)/speedy,radius-y/speedy)),this) ;
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

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Particle p && id == p.id;
    }
}
