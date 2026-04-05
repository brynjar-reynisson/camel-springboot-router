import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    port: 5174,
    allowedHosts: ['digitalme.breynisson.org'],
    proxy: {
      '/search': 'http://localhost:8080',
      '/semanticSearch': 'http://localhost:8080',
      '/localFile': 'http://localhost:8080',
      '/addContent': 'http://localhost:8080',
      '/summarize': 'http://localhost:8080',
    },
  },
})
