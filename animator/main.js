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
        1200,
        500,
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
  // Fondo blanco
  ctx.fillStyle = "white";
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);

  ctx.lineWidth = 2;
  ctx.strokeStyle = "black";

  // --- Cuadrado SxS (sin lado derecho) ---
  const squareX = offsetX;
  const squareY = offsetY;
  const squareSize = boardSize * scale;

  ctx.beginPath();
  ctx.moveTo(squareX, squareY);                       // arriba izq
  ctx.lineTo(squareX + squareSize, squareY);          // arriba der
  ctx.moveTo(squareX, squareY);                       // arriba izq
  ctx.lineTo(squareX, squareY + squareSize);          // abajo izq
  ctx.moveTo(squareX, squareY + squareSize);          // abajo izq
  ctx.lineTo(squareX + squareSize, squareY + squareSize); // abajo der
  ctx.stroke();

  // --- Rectángulo SxL (sin lado izquierdo) ---
  const rectX = squareX + squareSize;
  const rectY = offsetY + (boardSize - rectHeight) * scale / 2;
  const rectW = squareSize;
  const rectH = mapRectHeight(rectHeight);

  ctx.beginPath();
  ctx.moveTo(rectX + rectW, rectY);                   // arriba der
  ctx.lineTo(rectX, rectY);                           // arriba izq
  ctx.moveTo(rectX + rectW, rectY);                   // arriba der
  ctx.lineTo(rectX + rectW, rectY + rectH);           // abajo der
  ctx.moveTo(rectX + rectW, rectY + rectH);           // abajo der
  ctx.lineTo(rectX, rectY + rectH);                   // abajo izq
  ctx.stroke();

  // --- Líneas de unión arriba y abajo ---
  ctx.beginPath();
  ctx.moveTo(rectX, squareY);                     // borde superior cuadrado
  ctx.lineTo(rectX, rectY);                       // inicio rectángulo arriba
  ctx.moveTo(rectX, rectY + rectH);               // fin rectángulo abajo
  ctx.lineTo(rectX, squareY + squareSize);        // borde inferior cuadrado
  ctx.stroke();

  // --- Contador de colisiones ---
  if (collisionCount !== null) {
    ctx.fillStyle = "black";
    ctx.font = `20px sans-serif`;
    ctx.textAlign = "left";
    ctx.textBaseline = "top";
    ctx.fillText(`#Events: ${collisionCount}`, 10, 10);
  }

  // --- Partículas ---
  particles.forEach((p, i) => {
    const cx = mapX(p.x);
    const cy = mapY(p.y);
    const r = mapR(p.r);

    // círculo negro
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, 2 * Math.PI);
    ctx.fillStyle = "black";
    ctx.fill();
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
