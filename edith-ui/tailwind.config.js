/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'edith-cyan': '#00f3ff',
        'edith-dark': '#0a0f1a',
        'edith-panel': 'rgba(10, 15, 26, 0.7)',
        'edith-border': 'rgba(0, 243, 255, 0.3)',
      },
      fontFamily: {
        'edith': ['Orbitron', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
