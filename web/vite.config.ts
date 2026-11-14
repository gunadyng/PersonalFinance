import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// BASE_PATH lets the GitHub Pages workflow serve the app from /<repo>/.
export default defineConfig({
  plugins: [react()],
  base: process.env.BASE_PATH ?? '/',
})
