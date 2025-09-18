import core.Continuum;
import core.Time;
import tools.ParticleGenerator;
import tools.PostProcessor;

import java.io.IOException;
import java.util.Locale;

public class GasDiffusion {
    private final static String N = "N";
    private final static String L = "L";
    private final static String EPOCH = "epoch";
    private final static String OUTPUT_FILE = "output";


    public static void main(String[] args) {
        int n = Integer.parseInt(System.getProperty(N));
        double l = Double.parseDouble(System.getProperty(L));
        int epoch = Integer.parseInt(System.getProperty(EPOCH));
        String outputFile = System.getProperty(OUTPUT_FILE);
        double particleRadius = 0.0015;
        double side = 0.09;
        double speed = 0.01;

        Continuum continuum = new Continuum(side, l, n, epoch, particleRadius);
        ParticleGenerator.generate(n, side , continuum::addParticle, speed, particleRadius);
        Locale.setDefault(Locale.ENGLISH);
        long init = System.currentTimeMillis();
        try (PostProcessor postProcessor = new PostProcessor(outputFile)) {
            postProcessor.processEpoch(new Time(0, continuum.getParticles()));
            continuum.iterator().forEachRemaining(postProcessor::processEpoch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(System.currentTimeMillis() - init);


    }
}
