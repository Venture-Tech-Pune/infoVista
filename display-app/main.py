import pygame
import logging
import json
import time
import socketio
from api_client import APIClient
from display_manager import DisplayManager, IS_RASPBERRY_PI

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class DisplayApp:
    def __init__(self):
        """Initialize the display application"""
        logger.info("="*60)
        logger.info("🚀 INFOVISTA Display Application Starting...")
        logger.info("="*60)
        
        # Load configuration
        self.config = self.load_config()
        logger.info("✅ Configuration loaded")
        
        # Initialize API client
        self.api_client = APIClient(
            base_url=self.config['server']['baseUrl'],
            device_id=self.config['device']['deviceId']
        )
        
        # Initialize display
        display_config = self.config['display']
        self.display = DisplayManager(
            width=display_config['width'],
            height=display_config['height'],
            fullscreen=display_config['fullscreen']
        )
        
        # Configure base URL for image loading
        self.display.base_url = self.config['server']['baseUrl']
        
        # Initialize Socket.IO client for real-time updates
        self.sio = socketio.Client()
        self.setup_socketio()
        
        # State
        self.notices = []
        self.weather = None
        self.running = True
        self.current_slide_index = 0
        self.last_slide_switch_time = time.time()
        
        logger.info(f"📱 Device ID: {self.config['device']['deviceId']}")
        logger.info(f"🌐 Server: {self.config['server']['baseUrl']}")
        logger.info("✅ Display application initialized")
    
    def load_config(self):
        """Load configuration from config.json"""
        try:
            with open('config.json', 'r') as f:
                return json.load(f)
        except FileNotFoundError:
            logger.error("❌ config.json not found")
            raise
    
    def setup_socketio(self):
        """Setup Socket.IO event handlers for real-time updates"""
        @self.sio.on('connect')
        def on_connect():
            logger.info("🔌 Connected to server via Socket.IO")
        
        @self.sio.on('disconnect')
        def on_disconnect():
            logger.info("� Disconnected from Socket.IO")
        
        @self.sio.on('notice:created')
        def on_notice_created(data):
            logger.info(f"📢 New notice received: {data.get('title', 'Untitled')}")
            self.fetch_notices()
        
        @self.sio.on('notice:updated')
        def on_notice_updated(data):
            logger.info(f"✏️ Notice updated: {data.get('title', 'Untitled')}")
            self.fetch_notices()
        
        @self.sio.on('notice:deleted')
        def on_notice_deleted(data):
            logger.info(f"🗑️ Notice deleted: ID {data.get('_id')}")
            self.fetch_notices()
        
        # Connect to server
        try:
            socket_url = self.config['server']['baseUrl']
            self.sio.connect(socket_url, wait_timeout=5)
        except Exception as e:
            logger.warning(f"⚠️ Socket.IO connection failed: {e}")
    
    def fetch_notices(self):
        """Fetch active notices from server"""
        notices = self.api_client.get_active_notices()
        if notices is not None:
            if notices != self.notices:
                self.notices = notices
                self.current_slide_index = 0
                self.last_slide_switch_time = time.time()
                logger.info(f"📋 Loaded new notices. Count: {len(notices)}")
            else:
                logger.debug("📋 Notices fetched, but no changes detected.")
    
    def fetch_weather(self):
        """Fetch weather data"""
        weather_config = self.config.get('weather', {})
        if weather_config.get('enabled', False):
            self.weather = self.api_client.get_weather(
                city=weather_config.get('city', 'Pune'),
                country=weather_config.get('country', 'IN')
            )
    
    def update_device_status(self):
        """Update device status on server"""
        self.api_client.update_device_status('online')
    
    def run(self):
        """Main application loop"""
        logger.info("🎬 Starting main display loop...")
        
        clock = pygame.time.Clock()
        
        # Initial data fetch
        self.fetch_notices()
        self.fetch_weather()
        self.update_device_status()
        
        last_notice_refresh = time.time()
        last_weather_refresh = time.time()
        last_status_update = time.time()
        
        notice_refresh_interval = self.config['settings'].get('refreshInterval', 30)
        weather_refresh_interval = self.config['settings'].get('weatherUpdateInterval', 600)
        
        try:
            while self.running:
                # Handle events
                for event in pygame.event.get():
                    if event.type == pygame.QUIT:
                        logger.info("Quit signal received")
                        self.running = False
                    elif event.type == pygame.KEYDOWN:
                        if event.key == pygame.K_ESCAPE or event.key == pygame.K_q:
                            logger.info("Escape key pressed")
                            self.running = False
                
                current_time = time.time()
                
                # Refresh notices periodically (backup to Socket.IO)
                if current_time - last_notice_refresh >= notice_refresh_interval:
                    self.fetch_notices()
                    last_notice_refresh = current_time
                
                # Refresh weather
                if current_time - last_weather_refresh >= weather_refresh_interval:
                    self.fetch_weather()
                    last_weather_refresh = current_time
                
                # Update device status
                if current_time - last_status_update >= 60:
                    self.update_device_status()
                    last_status_update = current_time
                
                # Filter notices to only keep those with media
                media_notices = [n for n in self.notices if n.get('mediaUrl') and n.get('mediaType') in ('image', 'video')]
                
                # Chunk into pages/slides of size at most 2
                slides = [media_notices[i:i + 2] for i in range(0, len(media_notices), 2)]
                
                # Cap at max 2 slides in rotation
                slides = slides[:2]
                
                # Determine current slide to display
                current_slide = []
                if slides:
                    num_slides = len(slides)
                    if self.current_slide_index >= num_slides:
                        self.current_slide_index = 0
                    
                    current_slide = slides[self.current_slide_index]
                    
                    # Calculate slide duration (max displayDuration of notices on this slide, default 10s)
                    slide_duration = max([n.get('displayDuration', 10) for n in current_slide]) if current_slide else 10
                    
                    if current_time - self.last_slide_switch_time >= slide_duration:
                        self.current_slide_index = (self.current_slide_index + 1) % num_slides
                        self.last_slide_switch_time = current_time
                        logger.info(f"🔄 Slideshow: switched to slide index {self.current_slide_index} (showing {len(slides[self.current_slide_index])} notices)")
                        current_slide = slides[self.current_slide_index]

                # Draw grid layout
                self.display.draw_grid_layout(current_slide, self.weather, total_count=len(media_notices))
                self.display.update()
                
                # Control frame rate (cap to 15 fps on Pi to reduce CPU load)
                configured_fps = self.config['display'].get('fps', 30)
                effective_fps  = min(configured_fps, 15) if IS_RASPBERRY_PI else configured_fps
                clock.tick(effective_fps)
        
        except KeyboardInterrupt:
            logger.info("Keyboard interrupt received")
        
        finally:
            self.shutdown()
    
    def shutdown(self):
        """Shutdown the application"""
        logger.info("🛑 Shutting down...")
        
        # Update status to offline
        self.update_device_status()
        
        # Disconnect Socket.IO
        if self.sio.connected:
            self.sio.disconnect()
        
        # Cleanup display
        self.display.cleanup()
        
        logger.info("="*60)
        logger.info("👋 INFOVISTA Display Application Stopped")
        logger.info("="*60)

if __name__ == "__main__":
    app = DisplayApp()
    app.run()
