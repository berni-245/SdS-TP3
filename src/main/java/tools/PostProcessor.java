package tools;

import core.Particle;
import core.Time;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;

public class PostProcessor implements Closeable {
    private static final String OUTPUT_FILE_NAME = "dynamicOutput.txt";
    private final BufferedWriter writer;

    public PostProcessor(String outputName) {
        try {
            if (outputName == null)
                outputName = OUTPUT_FILE_NAME;
            writer = new BufferedWriter(new FileWriter(outputName));
        } catch (IOException e) {
            throw new RuntimeException("Error opening file");
        }
    }

    public void processEpoch(Time time) {
        try {
            writer.write(String.valueOf(time.time()));
            writer.newLine();
            time.particles().forEach(this::processParticle);
        } catch (IOException e) {
            throw new RuntimeException("Error writing on output file");
        }
    }

    private void processParticle(Particle particle) {
        try {
            writer.write(particle.csvString());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Error writing on output file");
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
