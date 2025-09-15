package core;

import java.util.*;

public class EventHandler {
    private final Queue<EventType> queue;
    private final List<List<EventType>> particleEvents;

    public EventHandler(int particleCount) {
        this.queue = new PriorityQueue<>();
        this.particleEvents = new ArrayList<>();
        for (int i = 0; i < particleCount; i++) {
            particleEvents.add(new ArrayList<>());
        }
    }

    public void addEvent(EventType event) {
        queue.add(event);
        particleEvents.get(event.getP1().getId() - 1).add(event);
        if(event.hasP2()){
            particleEvents.get(event.getP2().getId() - 1).add(event);
        }
    }

    public EventType getNextValidEvent() {
        EventType event = null;
        while(!queue.isEmpty()) {
            event = queue.poll();
            if(event.isValid()){
                return event;
            }
        }
        return event;
    }

    public Set<Particle> invalidateParticleEvent(Particle particle) {
        Set<Particle> invalidatedParticles = new HashSet<>();
        particleEvents.get(particle.getId() - 1).forEach(event -> {
            if(event.isValid()){
                event.invalidate();
                if(!event.getP1().equals(particle))
                    invalidatedParticles.add(event.getP1());

                if(event.hasP2() && !event.getP2().equals(particle))
                    invalidatedParticles.add(event.getP2());
            }
        });
        return invalidatedParticles;
    }



    public Set<Particle> invalidateParticleEvent(Particle particle1, Particle particle2) {
        Set<Particle> set = invalidateParticleEvent(particle1);
        set.addAll(invalidateParticleEvent(particle2));
        return set;
    }


}
