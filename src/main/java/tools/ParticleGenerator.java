package tools;

import core.Particle;

import java.util.*;
import java.util.function.Consumer;

public class ParticleGenerator {

    public static void generate(
            int particleNumber,
            double gridSize,
            Consumer<Particle> consumer,
            double speed,
            double radius) {
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        double x;
        double y;
        double direction;
        double cellSize = 2 * radius;
        GeneratedGrid grid = new GeneratedGrid(radius);
        for (int i = 0; i < particleNumber; i++) {
            x = random.nextDouble() * (gridSize - 2 * radius) + radius;
            y = random.nextDouble() * (gridSize - 2 * radius) + radius;
            int ci = (int) Math.floor(x / cellSize);
            int cj = (int) Math.floor(y / cellSize);
            Cell cell = new Cell(ci,cj);
            direction = random.nextDouble(-Math.PI, Math.PI);
            while(grid.checkColision(x,y,cell)){
                x = random.nextDouble() * (gridSize - 2 * radius) + radius;
                y = random.nextDouble() * (gridSize - 2 * radius) + radius;
                cell.setI((int) Math.floor(x / cellSize));
                cell.setJ((int) Math.floor(y / cellSize));
            }
            Particle particle = new Particle(x, y, direction, speed, radius);
            grid.addParticle(particle, cell);
            consumer.accept(particle);
        }
    }

    private static class GeneratedGrid {
        private final double cellSize;
        private final double particleRadius;
        private final Map<Cell, List<Particle>> grid;
        private final static int[][] directions = {
                {1, 0},   // derecha
                {1, 1},   // arriba derecha
                {0, 1},   // arriba
                {-1, 1},  // arriba izquierda
                {-1, 0},  // izquierda
                {-1, -1}, // abajo izquierda
                {0, -1},  // abajo
                {1, -1},  // abajo derecha
                {0, 0}    //celda actual
        };


        public GeneratedGrid(double particleRadius) {
            this.particleRadius = particleRadius;
            this.cellSize = 2 * particleRadius;
            this.grid = new HashMap<>();
        }

        public void addParticle(Particle particle, Cell cell) {
            grid.computeIfAbsent(cell, k -> new ArrayList<>()).add(particle);
        }

        public boolean checkColision(double x, double y, Cell cell) {
            for(int[] direction : directions){
                Cell neighborCell = new Cell(cell.i + direction[0], cell.j + direction[1]);
                List<Particle> particles = grid.get(neighborCell);
                if(particles != null){
                    for (Particle other : particles) {
                        double dx = x - other.getX();
                        double dy = y - other.getY();
                        double dist2 = dx * dx + dy * dy;
                        double minDist = cellSize * cellSize;
                        if (dist2 < minDist) { // d2<(2⋅r)2
                            return true; // se solapa
                        }
                    }
                }
            }
            return false;
        }


    }

    private static class Cell {
        private int i, j;

        Cell(int i, int j) {
            this.i = i;
            this.j = j;
        }

        public void setI(int i) {
            this.i = i;
        }

        public void setJ(int j) {
            this.j = j;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Cell cell && i == cell.i && j == cell.j;
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, j);
        }
    }
}
