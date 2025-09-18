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
        "-L, --rect-height <numbers...>",
        "rectangle height(s), one per input or a single value for all",
        parsePositiveArray
    )
    .option("--video-fps <int>", "frames per second", parsePositiveInt, 24)
    .option("--interpolate", "Disable frame interpolation", false);

async function main() {
  program.parse();
  const opts = program.opts();
  const inputs = program.args;
  const fixedBoardSize = 0.09;
  opts.boardSize = Array(inputs.length).fill(fixedBoardSize);
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
        opts.rectHeight[i],
        opts.interpolate // Pasa el flag de interpolación
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

async function generateVideo(inputPath, outputFile, videoWidth, videoHeight, videoFps, boardSize, rectHeight, shouldInterpolate, marginPx = 20) {
  const timestepIterator = parseTextStream(inputPath);
  const canvas = createCanvas(videoWidth, videoHeight);
  const ctx = canvas.getContext("2d");

  // --- Compute scaling and offsets (ONCE) ---
  const availableSize = Math.min(videoWidth, videoHeight) - 2 * marginPx;
  const scale = availableSize / boardSize;
  const offsetX = videoWidth / 2 - boardSize * scale;
  const offsetY = (videoHeight - boardSize * scale) / 2;
  const mapX = (x) => offsetX + x * scale;
  const mapY = (y) => offsetY + (boardSize - y) * scale;
  const mapR = (r) => r * scale;
  const mapRectHeight = (h) => h * scale;

  // --- FFmpeg setup ---
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
  const rgbaBuffer = Buffer.alloc(videoWidth * videoHeight * 4);

  for await (const [time, particles] of timestepIterator) {
    if (prevTime !== null) {
      if (shouldInterpolate) {
        // --- Interpolación (opcional) ---
        for (let t = prevTime + dt; t < time; t += dt) {
          const alpha = (t - prevTime) / (time - prevTime);
          const interpParticles = prevParticles.map((p) => ({
            ...p,
            x: p.x + p.vx * alpha * (time - prevTime),
            y: p.y + p.vy * alpha * (time - prevTime),
          }));
          ctx.fillStyle = "white";
          ctx.fillRect(0, 0, videoWidth, videoHeight);
          // --- Redibujar bordes estáticos ---
          drawStaticBorders(ctx, offsetX, offsetY, boardSize, rectHeight, scale, mapRectHeight);
          // --- Dibujar partículas y contador ---
          drawParticlesAndCounter(ctx, interpParticles, mapX, mapY, mapR, collisionCount);
          // --- Enviar frame ---
          const rgba = ctx.getImageData(0, 0, videoWidth, videoHeight).data;
          rgba.forEach((val, i) => rgbaBuffer[i] = val);
          await writeFrame(ffmpeg.stdin, rgbaBuffer);
        }
      }
      collisionCount += 100;
      ctx.fillStyle = "white";
      ctx.fillRect(0, 0, videoWidth, videoHeight);
      // --- Redibujar bordes estáticos ---
      drawStaticBorders(ctx, offsetX, offsetY, boardSize, rectHeight, scale, mapRectHeight);
      // --- Dibujar partículas y contador ---
      drawParticlesAndCounter(ctx, particles, mapX, mapY, mapR, collisionCount);
      // --- Enviar frame ---
      const rgba = ctx.getImageData(0, 0, videoWidth, videoHeight).data;
      rgba.forEach((val, i) => rgbaBuffer[i] = val);
      await writeFrame(ffmpeg.stdin, rgbaBuffer);
    } else {
      ctx.fillStyle = "white";
      ctx.fillRect(0, 0, videoWidth, videoHeight);
      // --- Redibujar bordes estáticos ---
      drawStaticBorders(ctx, offsetX, offsetY, boardSize, rectHeight, scale, mapRectHeight);
      // --- Dibujar partículas y contador ---
      drawParticlesAndCounter(ctx, particles, mapX, mapY, mapR, null);
      // --- Enviar frame ---
      const rgba = ctx.getImageData(0, 0, videoWidth, videoHeight).data;
      rgba.forEach((val, i) => rgbaBuffer[i] = val);
      await writeFrame(ffmpeg.stdin, rgbaBuffer);
    }
    prevTime = time;
    prevParticles = particles;
  }
  ffmpeg.stdin.end();
  await new Promise((resolve) => ffmpeg.on("close", resolve));
}

function drawStaticBorders(ctx, offsetX, offsetY, boardSize, rectHeight, scale, mapRectHeight) {
  ctx.lineWidth = 2;
  ctx.strokeStyle = "black";
  // --- Cuadrado SxS (sin lado derecho) ---
  const squareX = offsetX;
  const squareY = offsetY;
  const squareSize = boardSize * scale;
  ctx.beginPath();
  ctx.moveTo(squareX, squareY);
  ctx.lineTo(squareX + squareSize, squareY);
  ctx.moveTo(squareX, squareY);
  ctx.lineTo(squareX, squareY + squareSize);
  ctx.moveTo(squareX, squareY + squareSize);
  ctx.lineTo(squareX + squareSize, squareY + squareSize);
  ctx.stroke();
  // --- Rectángulo SxL (sin lado izquierdo) ---
  const rectX = squareX + squareSize;
  const rectY = offsetY + (boardSize - rectHeight) * scale / 2;
  const rectW = squareSize;
  const rectH = mapRectHeight(rectHeight);
  ctx.beginPath();
  ctx.moveTo(rectX + rectW, rectY);
  ctx.lineTo(rectX, rectY);
  ctx.moveTo(rectX + rectW, rectY);
  ctx.lineTo(rectX + rectW, rectY + rectH);
  ctx.moveTo(rectX + rectW, rectY + rectH);
  ctx.lineTo(rectX, rectY + rectH);
  ctx.stroke();
  // --- Líneas de unión arriba y abajo ---
  ctx.beginPath();
  ctx.moveTo(rectX, squareY);
  ctx.lineTo(rectX, rectY);
  ctx.moveTo(rectX, rectY + rectH);
  ctx.lineTo(rectX, squareY + squareSize);
  ctx.stroke();
}

function drawParticlesAndCounter(ctx, particles, mapX, mapY, mapR, collisionCount) {
  // --- Partículas ---
  particles.forEach((p) => {
    const cx = mapX(p.x);
    const cy = mapY(p.y);
    const r = mapR(p.r);
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, 2 * Math.PI);
    ctx.fillStyle = "black";
    ctx.fill();
  });
  // --- Contador de colisiones ---
  if (collisionCount !== null) {
    ctx.fillStyle = "black";
    ctx.font = `20px sans-serif`;
    ctx.textAlign = "left";
    ctx.textBaseline = "top";
    ctx.fillText(`#Events: ${collisionCount}`, 10, 10);
  }
}

// --- parseTextStream y helpers (sin cambios) ---
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
