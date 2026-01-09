# INFOVISTA Display Application

Python-based display application for Linux/Raspberry Pi to show notices on LCD screens.

## Features

- ✅ Real-time notice display with automatic rotation
- ✅ Live weather widget integration
- ✅ Multimedia support (images and videos)
- ✅ Automatic reconnection on network failure
- ✅ Device status reporting
- ✅ Cached content for offline mode
- ✅ Fullscreen display support

## Prerequisites

- Python 3.7 or higher
- Linux/Raspberry Pi OS
- Display/Monitor connected

## Installation

### 1. Install Python Dependencies

```bash
cd display-app
pip3 install -r requirements.txt
```

### 2. Install System Dependencies (Raspberry Pi)

```bash
sudo apt-get update
sudo apt-get install -y python3-pygame python3-pil
```

### 3. Configure the Application

Edit `config.json` to match your setup:

```json
{
  "device": {
    "deviceId": "DISPLAY_001",
    "name": "Main Hall Display",
    "location": "College Main Hall"
  },
  "server": {
    "baseUrl": "http://YOUR_SERVER_IP:3000"
  }
}
```

**Important**: Replace `YOUR_SERVER_IP` with your backend server's IP address.

## Running the Application

### Standard Mode
```bash
python3 main.py
```

### Auto-start on Boot (Raspberry Pi)

1. Create a systemd service file:
```bash
sudo nano /etc/systemd/system/infovista-display.service
```

2. Add the following content:
```ini
[Unit]
Description=INFOVISTA Display Application
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/infovista/display-app
ExecStart=/usr/bin/python3 /home/pi/infovista/display-app/main.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

3. Enable and start the service:
```bash
sudo systemctl enable infovista-display.service
sudo systemctl start infovista-display.service
```

4. Check status:
```bash
sudo systemctl status infovista-display.service
```

## Configuration Options

### Device Settings
- `deviceId`: Unique identifier for this display
- `name`: Human-readable name
- `location`: Physical location

### Server Settings
- `baseUrl`: Backend server URL
- `apiEndpoint`: Notices API endpoint
- `weatherEndpoint`: Weather API endpoint

### Display Settings
- `width`: Display width in pixels (default: 1920)
- `height`: Display height in pixels (default: 1080)
- `fullscreen`: Enable fullscreen mode (default: true)
- `fps`: Frame rate (default: 30)

### Application Settings
- `noticeDuration`: Seconds to display each notice (default: 10)
- `refreshInterval`: Seconds between notice refresh (default: 30)
- `weatherUpdateInterval`: Seconds between weather updates (default: 600)

### Weather Settings
- `city`: City name for weather data
- `country`: Country code (e.g., "IN" for India)
- `enabled`: Enable/disable weather widget

## Usage

### Controls
- **ESC** or **Q**: Quit application
- The application runs fullscreen by default

### Logs
Check `display.log` for application logs:
```bash
tail -f display.log
```

## Troubleshooting

### Display not showing
1. Check if backend server is running
2. Verify `baseUrl` in `config.json` is correct
3. Check network connectivity: `ping YOUR_SERVER_IP`

### No notices appearing
1. Ensure notices are created in the mobile app
2. Check if notices are active and not expired
3. Verify device is registered in backend

### Weather widget not showing
1. Ensure backend server has weather API key configured
2. Check `weather.enabled` is `true` in config
3. Verify internet connectivity

### Permission errors on Raspberry Pi
```bash
sudo chmod +x main.py
```

## Project Structure

```
display-app/
├── main.py              # Main application entry point
├── api_client.py        # Backend API communication
├── display_manager.py   # Display rendering (Pygame)
├── config.json          # Configuration file
├── requirements.txt     # Python dependencies
├── cache/              # Cached media files
└── display.log         # Application logs
```

## Hardware Requirements

### Minimum
- Raspberry Pi 3 or higher
- 2GB RAM
- HDMI display
- Network connection (WiFi/Ethernet)

### Recommended
- Raspberry Pi 4 (4GB RAM)
- 1920x1080 display
- Ethernet connection for stability

## OS Compatibility

- ✅ Raspberry Pi OS (Debian-based)
- ✅ Ubuntu Linux
- ✅ Any Linux distribution with Python 3.7+

## Performance Tips

1. **Use Ethernet**: More stable than WiFi
2. **Disable screen blanking**:
   ```bash
   sudo nano /etc/lightdm/lightdm.conf
   # Add: xserver-command=X -s 0 -dpms
   ```
3. **Optimize resolution**: Match display's native resolution
4. **Reduce FPS**: Lower `fps` in config for slower devices

## Development

### Testing without fullscreen
Edit `config.json`:
```json
{
  "display": {
    "fullscreen": false
  }
}
```

### Debugging
Enable verbose logging:
```python
# In main.py, change:
logging.basicConfig(level=logging.DEBUG)
```

## License

MIT
