package tools;

import core.Particle;
import core.Time;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class PostProcessor implements Closeable {
    private static final String OUTPUT_FILE_NAME = "dynamicOutput.txt";
    private final BufferedWriter writer;

    static {
        for (int wallId = 0; wallId <= 3; wallId++) {
            Path path = Paths.get("impulse_" + wallId + ".csv");
            try {
                Files.write(path, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                System.err.println("Error clearing impulse file: " + path);
            }
        }
    }


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

    public static void processImpulse(double impulse, double time, int wallId) {
        //ID=0 for the normal square sides, ID=1 for the split right wall
        //ID=2 for the rectangle roof and floor, ID=3 for rectangle wall
        Path path = Paths.get("impulse_" + wallId + ".csv");
        String entry = time + "," + impulse + '\n';

        try {
            Files.write(path, entry.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error writing impulse file");
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
