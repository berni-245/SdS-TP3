# Guía para correr esta fucking animación

Necesitás tener node y pnpm
Necesitás instalar [ffmpeg](www.gyan.dev/ffmpeg/builds)

Parándote en animator, corré:
```bash
pnpm install
```

Luego correr:
```bash
pnpm exec node main.js ../<sim_file> -L <L> --video-width 800 --video-fps 20
```