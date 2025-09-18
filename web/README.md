# Web Platform - Secondary Implementation

## Overview
The web platform has been restructured as a **secondary platform** in the Android-first architecture. It provides compatibility for users who need web access while the native Android app serves as the primary experience.

## Quick Start

```bash
# Install dependencies
npm install

# Development - Start with backend running on default port
npm run build
npm start

# Development with watch mode
npm run dev
```

## Configuration

The frontend now supports configurable backend URLs instead of hardcoded IPs. See [CONFIGURATION.md](./CONFIGURATION.md) for detailed information.

### Basic Setup

1. Copy the environment template:
   ```bash
   cp .env.template .env.local
   ```

2. Edit `.env.local` for your setup:
   ```
   BACKEND_HOST=127.0.0.1
   BACKEND_PORT=8000
   BACKEND_PROTOCOL=http
   ```

3. Build and run:
   ```bash
   npm run build
   npm start
   ```

### Environment Override

```bash
# For different backend server
BACKEND_HOST=192.168.1.100 npm run build
npm start

# For production HTTPS backend  
BACKEND_HOST=api.example.com BACKEND_PROTOCOL=https npm run build
npm run dist
```

## Architecture

- **Main Process**: TypeScript-based with secure IPC handlers
- **Renderer Process**: Vanilla JavaScript components with modular configuration
- **Configuration**: Build-time generation with environment variable support
- **Security**: Dynamic CSP generation, context isolation, no node integration

## Development

1. Ensure backend is running on configured port (default: 8000)
2. Run `npm run build` to compile TypeScript and generate configuration
3. Run `npm start` to launch Electron app
4. Use `npm run dev` for watch mode during development

For detailed configuration options, see [CONFIGURATION.md](./CONFIGURATION.md).