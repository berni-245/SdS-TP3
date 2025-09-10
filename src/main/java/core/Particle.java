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
            }else {
                tr = (B - (x+radius)) / speedx;
                yr = y + speedy * tr;
                if( yr -radius < (H -L) / 2){
                    return new EventType(((H-L)/2-(y-radius)),this);
                }else if(yr+radius > H -(H-L)/2){
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
        return new EventType(Math.min((x-radius)/speedx,(H-(y+radius)/speedy)),this) ;
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
