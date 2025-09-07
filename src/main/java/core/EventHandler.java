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

    public void invalidateParticleEvent(Particle particle) {
        particleEvents.get(particle.getId() - 1).forEach(EventType::invalidate);
    }

    public void invalidateParticleEvent(Particle particle1, Particle particle2) {
        invalidateParticleEvent(particle1);
        invalidateParticleEvent(particle2);
    }


}
