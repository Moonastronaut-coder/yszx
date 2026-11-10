import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export function createAppViteConfig(port: number) {
  const target = `http://127.0.0.1:${process.env.BACKEND_PORT ?? '8080'}`;
  return defineConfig({
    plugins: [vue()],
    resolve: { dedupe: ['vue', 'pinia'] },
    server: {
      host: '127.0.0.1',
      port,
      strictPort: true,
      proxy: { '/api': target, '/actuator/health': target },
    },
    preview: { host: '127.0.0.1', strictPort: true },
    build: { sourcemap: false },
  });
}
