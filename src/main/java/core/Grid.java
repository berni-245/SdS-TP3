package core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class Grid implements Iterable<Time> {
    private final double base;
    private final double height;
    private final double L;
    private final int M;
    private final int N;
    private final int maxEpoch;
    private final double neighborRadius;
    private final double vCellLength;
    private final double hCellLength;
    private final EventHandler eventHandler;

    private double epoch;

    private List<List<Particle>> grid;
    private final List<Particle> particles;

    public Grid(double base, double height, double L, int M, int N, int maxEpoch, double neighborRadius, double fixedParticleRadius) {
        if (base <= 0 || height <= 0)
            throw new IllegalArgumentException("Base and Height must be positive");
        if (M <= 0 || N <= 0 || L <= 0 || maxEpoch <= 0 || neighborRadius <= 0)
            throw new IllegalArgumentException("M, N, L, maxEpoch and neighborRadius must be positive");
        if (M >= height / (neighborRadius + 2 * fixedParticleRadius))
            throw new IllegalArgumentException("M must be smaller than height/(neighborRadius + 2*parRadius)");
        if (N >= base / (neighborRadius + 2 * fixedParticleRadius))
            throw new IllegalArgumentException("N must be smaller than base/(neighborRadius + 2*parRadius)");
        this.base = base;
        this.height = height;
        this.L = L;
        this.M = M;
        this.N = N;
        this.maxEpoch = maxEpoch;
        this.neighborRadius = neighborRadius;
        this.vCellLength = height / M;
        this.hCellLength = base / N;

        epoch = 0;

        this.particles = new ArrayList<>();
        this.grid = new ArrayList<>();
        for (int i = 0; i < M * N; i++) {
            grid.add(new ArrayList<>());
        }
        this.eventHandler = new EventHandler(N);
    }

    public Grid(double base, double height, double L, int maxEpoch, double neighborRadius, double fixedParticleRadius) {
        this(
                base, height, L,
                (int) Math.round(Math.ceil(height / (neighborRadius + 2 * fixedParticleRadius) - 1)),
                (int) Math.round(Math.ceil(base / (neighborRadius + 2 * fixedParticleRadius) - 1)),
                maxEpoch, neighborRadius, fixedParticleRadius
        );
    }

    /**
     * Particles on horizontal cell borders go to the upper cell,
     * and particles on vertical cell borders go to the right cell.
     */
    public void addParticle(Particle particle) {
        addParticleToGrid(particle);
        particles.add(particle);
    }

    private void addParticleToGrid(Particle particle) {
        double parX = particle.getX();
        double parY = particle.getY();

        if (parX >= base || parX < 0 || parY >= height || parY < 0)
            throw new IndexOutOfBoundsException("The particle doesn't fit on the grid");
        int i = (int) (parX / hCellLength) + N * (int) (parY / vCellLength);
        grid.get(i).add(particle);
    }


    @Override
    public String toString() {
        return getCustomGridRepresentation(particles1 -> particles1.toString() + "\t");
    }

    private String getCustomGridRepresentation(Function<List<Particle>, String> cellToString) {
        StringBuilder sb = new StringBuilder();
        for (int row = M - 1; row >= 0; row--) {
            for (int col = 0; col < N; col++) {
                int index = row * N + col;
                sb.append(cellToString.apply(grid.get(index)));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public List<Particle> getParticles() {
        return particles;
    }

    private class TimeIterator implements Iterator<Time>{

        public TimeIterator(){
            //search first events
        }

        @Override
        public boolean hasNext() {
            return epoch < maxEpoch;
        }

        @Override
        public Time next() {
            EventType eventType = eventHandler.getNextValidEvent(); //TODO: usar la info del event
            performCellIndexMethod();
            for (int i = 0; i < M * N; i++) {
                for (Particle particle : grid.get(i)) {
                    //TODO: Change the magic 1 to the time until the next event
                    particle.move(L,base,height,1);
                }
            }

            grid = new ArrayList<>();
            for (int i = 0; i < M * N; i++) {
                grid.add(new ArrayList<>());
            }

            for (Particle particle : getParticles()) {
                addParticleToGrid(particle);
            }

            //TODO invalidar eventos de particulas involucradas en evento actual

            Time time = new Time(epoch, particles);
            epoch++; //TODO: actualizar con el time del Event
            return time;
        }
    }

    @Override
    public Iterator<Time> iterator() {
        return new TimeIterator();
    }


    public void performCellIndexMethod() {
        for (int i = 0; i < M * N; i++) {
            for (Particle particle : grid.get(i)) {
                List<Particle> neighbors = getAboveAndRightAdjacentParticles(i);
                for (Particle neighbor : neighbors) {
                    if (neighbor.getDistance(particle) <= neighborRadius) {
                        particle.addNeighbor(neighbor);
                        neighbor.addNeighbor(particle);
                    }
                }
                for (Particle neighbor : getCurrentCellParticles(i, particle)) {
                    if (neighbor.getDistance(particle) <= neighborRadius) {
                        particle.addNeighbor(neighbor);
                    }
                }
            }
        }
    }

    private List<Particle> getAboveAndRightAdjacentParticles(int cellIndex) {
        List<Particle> adjacentParticles = new ArrayList<>();

        int row = cellIndex / N;
        int col = cellIndex % N;

        int[][] directions = {
                {1, 0}, {1, 1}, // above, upper right
                {0, 1}, // right
                {-1, 1} // lower right
        };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < M && newCol >= 0 && newCol < N) {
                int neighborCellIndex = newRow * N + newCol;
                adjacentParticles.addAll(grid.get(neighborCellIndex));
            }
        }

        return adjacentParticles;
    }

    private List<Particle> getCurrentCellParticles(int i, Particle p) {
        List<Particle> toReturn = new ArrayList<>(grid.get(i));
        toReturn.remove(p);
        return toReturn;
    }
}