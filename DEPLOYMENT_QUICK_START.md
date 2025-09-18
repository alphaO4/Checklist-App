# 🚀 Deployment Quick Start Guide

This guide provides step-by-step instructions to deploy your Feuerwehr Checklist App as a web app and mobile apps.

## 📋 Prerequisites

- Backend running on port 8000: `cd backend && python -m uvicorn app.main:app --reload`
- Node.js 18+ installed
- For mobile: Xcode (iOS) and/or Android Studio (Android)

## 🌐 Web App Deployment (5 minutes)

### 1. Build Web Version
```bash
cd "e:\Github\Checklist-App\frontend"
npm run build:web
```

### 2. Test Locally
```bash
npm run serve:web
# Opens at http://localhost:3000
```

### 3. Deploy to Vercel (Production)
```bash
# Install Vercel CLI
npm install -g vercel

# Deploy web-dist folder
cd web-dist
vercel

# Follow prompts:
# - Set up new project? Y
# - Directory: . (current directory)
# - Build command: (leave empty)
# - Output directory: . (current directory)
```

### 4. Alternative: Deploy to Netlify
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Deploy
cd web-dist
netlify deploy

# For production:
netlify deploy --prod
```

## 📱 PWA Deployment (Additional 10 minutes)

The web build already includes PWA features:
- ✅ Service worker for offline support
- ✅ Web manifest for "Add to Home Screen"
- ✅ Mobile-responsive design

### Test PWA Features
1. Open in Chrome/Edge on mobile
2. Menu → "Add to Home Screen"
3. Test offline functionality (disconnect internet)

## 📱 Mobile App Development (30 minutes setup)

### 1. Install Capacitor
```bash
cd "e:\Github\Checklist-App\frontend"
npm install @capacitor/core @capacitor/cli
npm install @capacitor/ios @capacitor/android
```

### 2. Initialize Capacitor
```bash
npx cap init "Feuerwehr Checklist" "de.feuerwehr.checklist"
```

### 3. Add Platforms
```bash
# Build web version first
npm run build:web

# Add mobile platforms
npx cap add ios
npx cap add android
```

### 4. Test on iOS Simulator (macOS only)
```bash
npm run mobile:ios
# Opens Xcode - click Play button to run in simulator
```

### 5. Test on Android Emulator
```bash
npm run mobile:android
# Opens Android Studio - click Play button to run in emulator
```

## 🔗 Browser → Desktop Integration (Protocol Handler)

### 1. Build and Install Desktop App
```bash
# Build distributable
npm run dist

# Install the generated app from release/ folder
```

### 2. Test Protocol Links
Create an HTML file to test deep linking:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Feuerwehr Test Links</title>
</head>
<body>
    <h2>Test Deep Links</h2>
    <p><a href="feuerwehr-checklist://vehicle/inspect/MTF-123">Inspect Vehicle MTF-123</a></p>
    <p><a href="feuerwehr-checklist://checklist/start/daily-check">Start Daily Checklist</a></p>
    <p><a href="feuerwehr-checklist://tuv/reminder/456">TÜV Reminder for Vehicle 456</a></p>
    
    <script>
        // Generate protocol links dynamically
        function openApp(action) {
            window.location.href = `feuerwehr-checklist://${action}`;
        }
    </script>
    
    <button onclick="openApp('vehicles')">Open Vehicles</button>
    <button onclick="openApp('tuv/expired')">Show Expired TÜV</button>
</body>
</html>
```

## 🧪 Testing Checklist

### Web App Testing
- [ ] Login functionality works
- [ ] Vehicle type management (CRUD operations)
- [ ] Vehicles listing and editing
- [ ] Groups management
- [ ] TÜV deadline tracking
- [ ] Offline functionality (disconnect internet)
- [ ] Mobile responsiveness (test on phone)

### Mobile App Testing
- [ ] Install on device/simulator
- [ ] All web features work in mobile context
- [ ] Touch interactions are responsive
- [ ] App launches correctly
- [ ] Push notifications (if implemented)

### Protocol Handler Testing
- [ ] Desktop app installs without errors
- [ ] Protocol links open the app
- [ ] Deep links navigate to correct sections
- [ ] App focuses when already running

## 🚀 Production Deployment

### Web App (Choose one)
- **Vercel**: Automatic deployments from Git
- **Netlify**: Great for static sites with forms
- **AWS S3 + CloudFront**: Enterprise-grade CDN
- **Self-hosted**: Nginx on your Ubuntu VM

### Mobile Apps
- **iOS App Store**: Requires Apple Developer Account ($99/year)
- **Google Play Store**: One-time $25 registration fee
- **Enterprise Distribution**: For internal fire department use only

### Example Nginx Configuration (Self-hosted)
```nginx
server {
    listen 80;
    server_name checklist.feuerwehr-domain.de;
    
    location / {
        root /var/www/feuerwehr-checklist;
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://127.0.0.1:8000/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 🔐 Security Considerations

### Web App
- Use HTTPS in production
- Implement proper CORS configuration
- Secure API endpoints with authentication
- Content Security Policy (CSP) headers

### Mobile Apps
- Enable certificate pinning
- Secure local storage of authentication tokens
- Implement app transport security

### Fire Department Specific
- Ensure GDPR compliance
- Data residency in Germany
- Regular security updates
- Audit trail for all actions

## 📊 Monitoring & Analytics

### Web App
```javascript
// Add to index.html for basic analytics
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/service-worker.js');
}

// Track usage
window.analytics = {
  track: (event, data) => {
    console.log('Analytics:', event, data);
    // Send to your analytics service
  }
};
```

### Performance Monitoring
- Use browser DevTools Performance tab
- Monitor Core Web Vitals
- Track API response times
- Monitor offline functionality

## 🆘 Troubleshooting

### Common Issues

**Web App won't load:**
- Check backend is running on port 8000
- Verify CORS configuration
- Check browser console for errors

**Mobile app crashes:**
- Check Capacitor logs: `npx cap logs ios/android`
- Verify all dependencies are installed
- Test on physical device if simulator fails

**Protocol handler not working:**
- Reinstall desktop app
- Check if protocol is registered: Windows Registry / macOS plist
- Test with simple HTML file first

**Offline functionality issues:**
- Check service worker registration
- Verify cache configuration
- Test with DevTools Network tab throttling

### Getting Help
- Check GitHub Issues for common problems
- Electron Discord community
- Capacitor documentation and forums
- Fire department IT support for deployment
