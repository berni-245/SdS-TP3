import core.Grid;
import tools.ParticleGenerator;
import tools.PostProcessor;

import java.io.IOException;

public class GasDiffusion {
    private final static String N = "N";
    private final static String L = "L";
    private final static String V = "V";
    private final static String EPOCH = "epoch";
    private final static String OUTPUT_FILE = "output";


    public static void main(String[] args) {
        int n = Integer.parseInt(System.getProperty(N));
        double l = Double.parseDouble(System.getProperty(L));
        double v = Double.parseDouble(System.getProperty(V));
        int epoch = Integer.parseInt(System.getProperty(EPOCH));
        String outputFile = System.getProperty(OUTPUT_FILE);

        Grid grid = new Grid(l, epoch, 1); // TODO remove 1 = rc
        ParticleGenerator.generate(n, l, grid::addParticle, v, 0, false); // TODO remove noise and randomNeighborDirection
        long init =  System.currentTimeMillis();
        try(PostProcessor postProcessor  = new PostProcessor(outputFile)){
            grid.iterator().forEachRemaining(postProcessor::processEpoch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(System.currentTimeMillis() - init);


    }
}
