import fs from "fs";
import { createCanvas } from "canvas";
import readline from "readline";
import { spawn } from "child_process";
import { Command, InvalidArgumentError } from "commander";

const program = new Command();

program
    .name("particle-video")
    .description("Render particle simulation files into mp4 videos")
    .argument("<inputs...>", "input simulation files")
    .requiredOption(
        "-S, --board-size <numbers...>",
        "board size(s), one per input or a single value for all",
        parsePositiveArray
    )
    .requiredOption(
        "-L, --rect-height <numbers...>",
        "rectangle height(s), one per input or a single value for all",
        parsePositiveArray
    )
    .option("--video-width <pixels>", "video width", parsePositiveInt, 500)
    .option("--video-height <pixels>", "video height", parsePositiveInt, 500)
    .option("--video-fps <int>", "frames per second", parsePositiveInt, 24);

async function main() {
  program.parse();
  const opts = program.opts();
  const inputs = program.args;

  opts.boardSize = normalizePerInput(opts.boardSize, inputs);
  opts.rectHeight = normalizePerInput(opts.rectHeight, inputs);

  for (let i = 0; i < inputs.length; ++i) {
    const output =
        inputs[i]
            .split("/")
            .at(-1)
            .replace(/\.\w+$/, "") + ".mp4";

    await generateVideo(
        inputs[i],
        output,
        opts.videoWidth,
        opts.videoHeight,
        opts.videoFps,
        opts.boardSize[i],
        opts.rectHeight[i]
    );

    console.log(`Video saved as ${output}`);
  }
}

main();

function writeFrame(stream, buffer) {
  return new Promise((resolve) => {
    if (!stream.write(buffer)) stream.once("drain", resolve);
    else resolve();
  });
}

async function generateVideo(inputPath, outputFile, videoWidth, videoHeight, videoFps, boardSize, rectHeight, marginPx = 20) {
  const timestepIterator = parseTextStream(inputPath);

  const canvas = createCanvas(videoWidth, videoHeight);
  const ctx = canvas.getContext("2d");

  // --- Compute scaling and offsets ---
  const availableSize = Math.min(videoWidth, videoHeight) - 2 * marginPx;
  const scale = availableSize / boardSize;
  const offsetX = videoWidth / 2 - boardSize * scale;
  const offsetY = (videoHeight - boardSize * scale) / 2;

  const mapX = (x) => offsetX + x * scale;
  const mapY = (y) => offsetY + (boardSize - y) * scale;
  const mapR = (r) => r * scale;
  const mapRectHeight = (h) => h * scale;

  const ffmpeg = spawn("ffmpeg", [
    "-y",
    "-f", "rawvideo",
    "-pix_fmt", "rgba",
    "-s", `${videoWidth}x${videoHeight}`,
    "-r", String(videoFps),
    "-i", "-",
    "-c:v", "libx264",
    "-crf", "20",
    "-preset", "fast",
    "-pix_fmt", "yuv420p",
    outputFile,
  ]);

  ffmpeg.stderr.on("data", (data) => {
    console.error("ffmpeg:", data.toString());
  });

  const dt = 1 / videoFps;

  let prevTime = null;
  let prevParticles = null;
  let collisionCount = 0;

  for await (const [time, particles] of timestepIterator) {
    if (prevTime !== null) {
      for (let t = prevTime + dt; t < time; t += dt) {
        const alpha = t - prevTime;
        const interpParticles = prevParticles.map((p) => ({
          ...p,
          x: p.x + p.vx * alpha,
          y: p.y + p.vy * alpha,
        }));
        drawFrame(ctx, interpParticles, boardSize, rectHeight, scale, offsetX, offsetY, mapX, mapY, mapR, mapRectHeight, collisionCount);
        const rgba = ctx.getImageData(0, 0, videoWidth, videoHeight).data;
        await writeFrame(ffmpeg.stdin, Buffer.from(rgba));
      }

      collisionCount++;
      drawFrame(ctx, particles, boardSize, rectHeight, scale, offsetX, offsetY, mapX, mapY, mapR, mapRectHeight, collisionCount);
      const rgba = ctx.getImageData(0, 0, videoWidth, videoHeight).data;
      await writeFrame(ffmpeg.stdin, Buffer.from(rgba));
    } else {
      drawFrame(ctx, particles, boardSize, rectHeight, scale, offsetX, offsetY, mapX, mapY, mapR, mapRectHeight, null);
      const rgba = ctx.getImageData(0, 0, videoWidth, videoHeight).data;
      await writeFrame(ffmpeg.stdin, Buffer.from(rgba));
    }

    prevTime = time;
    prevParticles = particles;
  }

  ffmpeg.stdin.end();
  await new Promise((resolve) => ffmpeg.on("close", resolve));
}

function drawFrame(ctx, particles, boardSize, rectHeight, scale, offsetX, offsetY, mapX, mapY, mapR, mapRectHeight, collisionCount = null) {
  // Background
  ctx.fillStyle = "black";
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);

  // Cuadrado SxS
  ctx.strokeStyle = "#FFFFFF";
  ctx.lineWidth = 2;
  ctx.strokeRect(offsetX, offsetY, boardSize * scale, boardSize * scale);

  // Rectángulo SxL centrado verticalmente y a la derecha del cuadrado
  ctx.strokeStyle = "white";
  ctx.lineWidth = 2;
  const rectX = offsetX + boardSize * scale; // justo a la derecha del cuadrado
  const rectY = offsetY + (boardSize - rectHeight) * scale / 2;
  ctx.strokeRect(rectX, rectY, boardSize * scale, mapRectHeight(rectHeight));


  // Collision counter
  if (collisionCount !== null) {
    ctx.fillStyle = "white";
    ctx.font = `20px sans-serif`;
    ctx.textAlign = "left";
    ctx.textBaseline = "top";
    ctx.fillText(`Collisions: ${collisionCount}`, 10, 10);
  }

  // Particles
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillStyle = "white";
  ctx.font = `${12 * scale}px sans-serif`;

  particles.forEach((p, i) => {
    const cx = mapX(p.x);
    const cy = mapY(p.y);
    const r = mapR(p.r);

    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, 2 * Math.PI);
    ctx.fillStyle = "white";
    ctx.fill();

    ctx.fillStyle = "black";
    const fontSize = Math.max(r * 1.5, 8);
    ctx.font = `${fontSize}px sans-serif`;
    ctx.fillText(String(i + 1), cx, cy);
  });
}

// --- parseTextStream y helpers (igual que antes) ---
async function* parseTextStream(path) {
  const fileStream = fs.createReadStream(path);
  const rl = readline.createInterface({ input: fileStream });

  let particles = [];
  let currentTime = null;

  for await (const line of rl) {
    const trimmed = line.trim();
    if (!trimmed) continue;

    if (/^\d+(\.\d+)?$/.test(trimmed)) {
      if (currentTime !== null) {
        yield [currentTime, particles];
        particles = [];
      }
      currentTime = parseFloat(trimmed);
    } else {
      const [x, y, vx, vy, r] = trimmed.split(",").map(Number);
      particles.push({ x, y, vx, vy, r });
    }
  }

  if (currentTime !== null && particles.length) {
    yield [currentTime, particles];
  }
}

function parsePositive(value) {
  const num = Number(value);
  if (isNaN(num) || num <= 0) {
    throw new InvalidArgumentError("Must be a positive number");
  }
  return num;
}

function parsePositiveArray(value, previous) {
  if (!previous) previous = [];
  return previous.concat([parsePositive(value)]);
}

function parsePositiveInt(value) {
  const num = parsePositive(value);
  if (!Number.isInteger(num)) {
    throw new InvalidArgumentError("Must be a positive integer");
  }
  return num;
}

function normalizePerInput(values, inputs) {
  if (values.length === 0) return [];
  if (values.length === 1) return Array(inputs.length).fill(values[0]);
  if (values.length !== inputs.length) {
    throw new InvalidArgumentError(`Expected 1 or ${inputs.length} values, got ${values.length}`);
  }
  return values;
}
