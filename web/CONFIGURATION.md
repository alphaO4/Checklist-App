# Frontend Configuration System

The frontend has been updated to use a modular configuration system that allows dynamic backend URL configuration at build time and runtime.

## Configuration Files

### Environment Files
- `.env.template` - Template file showing all available configuration options
- `.env.development` - Development environment defaults
- `.env.production` - Production environment defaults  
- `.env.local` - Local overrides (gitignored, create from template)

### Configuration Variables
```bash
BACKEND_HOST=127.0.0.1        # Backend server hostname/IP
BACKEND_PORT=8000             # Backend server port
BACKEND_PROTOCOL=http         # http or https
BACKEND_WS_PROTOCOL=ws        # ws or wss (WebSocket protocol)
```

## Build Process

The build process now includes configuration generation:

1. **`npm run generate-config`** - Generates runtime configuration based on environment
2. **`npm run build`** - Full build including config generation, TypeScript compilation, and asset copying
3. **`npm run dev`** - Development mode with watch and config generation

### Configuration Generation Flow

1. Loads environment files in precedence order:
   - `.env.template` (base)
   - `.env.{NODE_ENV}` (environment-specific) 
   - `.env.local` (local overrides)
   - Environment variables (highest priority)

2. Generates `dist/renderer/config.js` with runtime configuration
3. Processes `src/renderer/index.html` template to inject dynamic CSP rules
4. Outputs processed `dist/renderer/index.html`

## Runtime Usage

### In Renderer Process (Components)

```javascript
// Use the configUtils helper for API calls
const response = await window.configUtils.fetchBackend('/vehicles', {
  method: 'GET'
});

// Or get URLs directly
const backendUrl = window.getBackendUrl();
const wsUrl = window.getBackendWsUrl();

// Access full configuration
const config = window.APP_CONFIG.backend;
```

### In Main Process (TypeScript)

```typescript
import { getBackendConfig } from '../shared/config';

const config = getBackendConfig();
const backendClient = new BackendClient(config.baseUrl);
```

## Deployment Scenarios

### Development
```bash
# Use default development configuration
npm run build
npm start

# Override for different development server
BACKEND_HOST=192.168.1.100 npm run build
npm start
```

### Production Build
```bash
# Set production environment variables
export NODE_ENV=production
export BACKEND_HOST=your-server.com
export BACKEND_PROTOCOL=https
export BACKEND_WS_PROTOCOL=wss

npm run build
npm run dist  # Creates production Electron package
```

### Docker/Container Deployment
```bash
# Environment variables can be set at container runtime
docker run -e BACKEND_HOST=api.example.com -e BACKEND_PROTOCOL=https your-app
```

## Security

- **Content Security Policy (CSP)** is dynamically generated based on backend configuration
- Environment files with sensitive data (`.env.local`) are gitignored
- Configuration is validated and falls back to safe defaults

## Migration from Hardcoded URLs

The following components have been updated to use the new configuration system:

- `vehicleEditModal.js` - All API calls now use `configUtils.fetchBackend()`
- `groupEditModal.js` - Updated to use configurable endpoints  
- `fahrzeugePage.js` - Delete operations use config
- `index.html` - CSP rules are dynamically generated
- `backend.ts` - Main process backend client uses config

## Backward Compatibility

- Falls back to `http://localhost:8000` if configuration is not available
- Environment variable `BACKEND_URL` is still supported for simple overrides
- Existing authentication flows remain unchanged

## Troubleshooting

### Build Issues
- Ensure all environment files use valid syntax (KEY=value)
- Check that `scripts/generate-config.js` has proper permissions
- Verify TypeScript compilation succeeds after config generation

### Runtime Issues  
- Check browser DevTools console for configuration loading errors
- Verify `config.js` is loaded before other scripts in `index.html`
- Ensure `configUtils.js` is available to components

### CSP Violations
- CSP is auto-generated based on backend configuration
- If using custom domains, ensure they match your environment variables
- Check browser DevTools Security tab for CSP violations
