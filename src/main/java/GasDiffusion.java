import core.Grid;
import tools.ParticleGenerator;
import tools.PostProcessor;

import java.io.IOException;

public class GasDiffusion {
    private final static String N = "N";
    private final static String L = "L";
    private final static String EPOCH = "epoch";
    private final static String NEIGHBOR_RADIUS = "rc";
    private final static String OUTPUT_FILE = "output";


    public static void main(String[] args) {
        int n = Integer.parseInt(System.getProperty(N));
        double l = Double.parseDouble(System.getProperty(L));
        int epoch = Integer.parseInt(System.getProperty(EPOCH));
        double neighborRadius = Double.parseDouble(System.getProperty(NEIGHBOR_RADIUS));
        String outputFile = System.getProperty(OUTPUT_FILE);
        double particleRadius = 0.0015;
        double side = 0.09;
        double speed = 0.01;

        Grid grid = new Grid(2 * side, side, l, epoch, neighborRadius, particleRadius);
        ParticleGenerator.generate(n, side ,grid::addParticle, speed, particleRadius);
        long init = System.currentTimeMillis();
        try (PostProcessor postProcessor = new PostProcessor(outputFile)) {
            grid.iterator().forEachRemaining(postProcessor::processEpoch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(System.currentTimeMillis() - init);


    }
}
