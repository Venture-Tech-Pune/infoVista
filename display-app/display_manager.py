import pygame
import logging
import textwrap
import requests
import io
import os
import cv2
import numpy as np
from datetime import datetime
from PIL import Image
try:
    from ffpyplayer.player import MediaPlayer
except ImportError:
    MediaPlayer = None

logger = logging.getLogger(__name__)

class DisplayManager:
    def __init__(self, width=1920, height=1080, fullscreen=False):
        """Initialize the Pygame display"""
        pygame.init()
        
        self.width = width
        self.height = height
        
        # Set display mode
        if fullscreen:
            self.screen = pygame.display.set_mode((width, height), pygame.FULLSCREEN)
        else:
            self.screen = pygame.display.set_mode((width, height))
        
        pygame.display.set_caption("INFOVISTA Display Board")
        
        # Colors - Modern palette
        self.colors = {
            'bg': (248, 249, 250),           # Light grey background
            'primary': (33, 150, 243),       # Blue
            'text': (26, 26, 26),            # Dark text
            'text_secondary': (102, 102, 102),  # Grey text
            'white': (255, 255, 255),
            'border': (224, 224, 224),       # Light border
            'urgent': (244, 67, 54),         # Red
            'high': (255, 152, 0),           # Orange
            'medium': (33, 150, 243),        # Blue
            'low': (76, 175, 80)             # Green
        }
        
        # Fonts
        try:
            self.font_title = pygame.font.SysFont('Arial', 32, bold=True)
            self.font_desc = pygame.font.SysFont('Arial', 24)
            self.font_meta = pygame.font.SysFont('Arial', 18)
            self.font_header = pygame.font.SysFont('Arial', 48, bold=True)
        except:
            self.font_title = pygame.font.Font(None, 32)
            self.font_desc = pygame.font.Font(None, 24)
            self.font_meta = pygame.font.Font(None, 18)
            self.font_header = pygame.font.Font(None, 48)
        
        # Cache
        self.image_cache = {}
        self.video_caps = {}
        self.video_players = {}
        self.base_url = "http://localhost:3000"  # Will be updated from config
        self.cache_dir = "cache"
        if not os.path.exists(self.cache_dir):
            os.makedirs(self.cache_dir)
        
        # Ticker state
        self.ticker_x = width
        self.last_tick = logging.Formatter.converter()
        
        # Audio state
        try:
            pygame.mixer.init()
            self.audio_channels = {}
            logger.info("✅ Audio mixer initialized")
        except Exception as e:
            logger.error(f"❌ Failed to initialize audio mixer: {e}")
        
        logger.info(f"✅ Display initialized: {width}x{height}")
    
    def calculate_grid(self, notice_count):
        """Calculate grid dimensions based on number of notices"""
        if notice_count == 0:
            return 0, 0
        elif notice_count == 1:
            return 1, 1
        elif notice_count == 2:
            return 2, 1  # 2 columns, 1 row
        elif notice_count <= 4:
            return 2, 2  # 2x2 grid
        elif notice_count <= 6:
            return 3, 2  # 3x2 grid
        else:
            return 3, 3  # 3x3 grid (max 9 notices)
    
    def draw_grid_layout(self, notices, weather_data=None):
        """Draw notices in a grid/collage layout"""
        # Clear screen
        self.screen.fill(self.colors['bg'])
        
        if not notices:
            self.draw_empty_state(weather_data)
            return
        
        # Limit to 9 notices max for clean display
        display_notices = notices[:9]
        
        # Calculate grid
        cols, rows = self.calculate_grid(len(display_notices))
        
        # Calculate box dimensions with padding
        padding = 20
        header_height = 80
        
        available_width = self.width - (padding * (cols + 1))
        available_height = self.height - header_height - (padding * (rows + 1))
        
        box_width = available_width // cols
        box_height = available_height // rows
        
        # Draw header
        self.draw_header(len(display_notices))
        
        # Draw each notice in grid
        for idx, notice in enumerate(display_notices):
            row = idx // cols
            col = idx % cols
            
            x = padding + col * (box_width + padding)
            y = header_height + padding + row * (box_height + padding)
            
            self.draw_notice_box(notice, x, y, box_width, box_height, idx)
    
    def draw_header(self, notice_count):
        """Draw header with title and notice count"""
        header_rect = pygame.Rect(0, 0, self.width, 80)
        pygame.draw.rect(self.screen, self.colors['primary'], header_rect)
        
        # Title
        title_text = self.font_header.render("📢 INFOVISTA", True, self.colors['white'])
        title_rect = title_text.get_rect(midleft=(30, 40))
        self.screen.blit(title_text, title_rect)
        
        # Notice count
        count_text = self.font_meta.render(f"{notice_count} Active Notice{'s' if notice_count != 1 else ''}", 
                                          True, self.colors['white'])
        count_rect = count_text.get_rect(midright=(self.width - 30, 40))
        self.screen.blit(count_text, count_rect)
        
        # Current time
        time_text = self.font_meta.render(datetime.now().strftime("%I:%M %p"), 
                                         True, self.colors['white'])
        time_rect = time_text.get_rect(midright=(self.width - 200, 40))
        self.screen.blit(time_text, time_rect)
    
    def load_image(self, media_url):
        """Load image from URL and cache it"""
        if not media_url:
            return None
            
        # Check cache first
        if media_url in self.image_cache:
            return self.image_cache[media_url]
        
        try:
            # Construct full URL
            image_url = f"{self.base_url}{media_url}"
            
            # Download image
            response = requests.get(image_url, timeout=5)
            response.raise_for_status()
            
            # Load with PIL
            pil_image = Image.open(io.BytesIO(response.content))
            
            # Convert to pygame surface
            mode = pil_image.mode
            size = pil_image.size
            data = pil_image.tobytes()
            
            pygame_surface = pygame.image.fromstring(data, size, mode)
            
            # Cache it
            self.image_cache[media_url] = pygame_surface
            logger.info(f"✅ Loaded image: {media_url}")
            
            return pygame_surface
        except Exception as e:
            logger.error(f"❌ Failed to load image {media_url}: {e}")
            return None

    def load_video(self, media_url, is_muted=True):
        """Initialize MediaPlayer for a URL (downloading if needed)"""
        if not media_url or MediaPlayer is None:
            return None
            
        if media_url in self.video_players:
            return self.video_players[media_url]
            
        try:
            filename = media_url.split('/')[-1]
            local_path = os.path.join(self.cache_dir, filename)
            
            if not os.path.exists(local_path):
                logger.info(f"⏳ Downloading video for player: {media_url}")
                video_url = f"{self.base_url}{media_url}"
                response = requests.get(video_url, timeout=15, stream=True)
                response.raise_for_status()
                with open(local_path, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        f.write(chunk)
            
            # Create player
            ff_opts = {'paused': False, 'volume': 0.0 if is_muted else 1.0}
            player = MediaPlayer(local_path, ff_opts=ff_opts)
            self.video_players[media_url] = player
            logger.info(f"✅ Created ffpyplayer for: {media_url} (Muted: {is_muted})")
            return player
        except Exception as e:
            logger.error(f"❌ Error loading video player {media_url}: {e}")
            return None

    def draw_notice_box(self, notice, x, y, width, height, idx=0):
        """Draw a single notice in a box"""
        # Create box with shadow
        shadow_rect = pygame.Rect(x + 4, y + 4, width, height)
        box_rect = pygame.Rect(x, y, width, height)
        
        pygame.draw.rect(self.screen, (200, 200, 200), shadow_rect, border_radius=12)
        pygame.draw.rect(self.screen, self.colors['white'], box_rect, border_radius=12)
        
        # Priority bar at top
        priority = notice.get('priority', 'medium').lower()
        priority_color = self.colors.get(priority, self.colors['medium'])
        priority_bar = pygame.Rect(x, y, width, 8)
        pygame.draw.rect(self.screen, priority_color, priority_bar, 
                        border_top_left_radius=12, border_top_right_radius=12)
        
        # Content area
        content_y = y + 20
        content_x = x + 15
        content_width = width - 30
        
        # Title
        title = notice.get('title', 'Untitled')
        title_surface = self.font_title.render(title[:40], True, self.colors['text'])
        self.screen.blit(title_surface, (content_x, content_y))
        content_y += 45
        
        # Check if notice has media
        media_url = notice.get('mediaUrl')
        media_type = notice.get('mediaType')
        
        if media_url:
            if media_type == 'image':
                # Load and display image
                image_surface = self.load_image(media_url)
                if image_surface:
                    # Reserve space for footer
                    footer_height = 40
                    available_height = (y + height) - content_y - footer_height
                    img_width = content_width
                    orig_width, orig_height = image_surface.get_size()
                    aspect_ratio = orig_height / orig_width
                    img_height = min(int(img_width * aspect_ratio), available_height)
                    if img_height == available_height:
                        img_width = int(img_height / aspect_ratio)
                    scaled_image = pygame.transform.smoothscale(image_surface, (img_width, img_height))
                    img_x = content_x + (content_width - img_width) // 2
                    self.screen.blit(scaled_image, (img_x, content_y))
                    content_y += img_height + 10
            elif media_type == 'video':
                # Load and play video using ffpyplayer
                raw_muted = notice.get('isMuted')
                is_sound_enabled = not raw_muted if isinstance(raw_muted, bool) else (str(raw_muted).lower() == 'false' if raw_muted is not None else True)
                
                logger.debug(f"📹 Video Notice: '{notice.get('title')}' | ID: {notice.get('_id')} | Raw isMuted: {raw_muted} | Sound Enabled: {is_sound_enabled}")
                
                player = self.load_video(media_url, is_muted=not is_sound_enabled)
                
                if player:
                    # Sync volume and ensure not paused
                    player.set_volume(1.0 if is_sound_enabled else 0.0)
                    if player.get_pause():
                        player.set_pause(False)
                    
                    # Get frame
                    frame, val = player.get_frame()
                    if val != 'eof' and frame is not None:
                        img, t = frame
                        
                        # Convert to pygame surface
                        size = img.get_size()
                        data = img.to_bytearray()[0]
                        video_surface = pygame.image.frombuffer(data, size, 'RGB')
                        
                        # Scale and blit
                        footer_height = 40
                        available_height = (y + height) - content_y - footer_height
                        
                        img_width = content_width
                        orig_width, orig_height = video_surface.get_size()
                        aspect_ratio = orig_height / orig_width
                        img_height = min(int(img_width * aspect_ratio), available_height)
                        
                        if img_height == available_height:
                            img_width = int(img_height / aspect_ratio)
                            
                        scaled_video = pygame.transform.smoothscale(video_surface, (img_width, img_height))
                        img_x = content_x + (content_width - img_width) // 2
                        self.screen.blit(scaled_video, (img_x, content_y))

                        # Sound Indicator
                        if not is_sound_enabled:
                            mute_font = pygame.font.SysFont('Arial', 24, bold=True)
                            mute_text = mute_font.render("🔇 MUTED", True, (255, 255, 255))
                            mute_bg = pygame.Surface((mute_text.get_width() + 20, mute_text.get_height() + 10), pygame.SRCALPHA)
                            mute_bg.fill((0, 0, 0, 180))
                            self.screen.blit(mute_bg, (img_x + 10, content_y + 10))
                            self.screen.blit(mute_text, (img_x + 20, content_y + 15))
                        
                        content_y += img_height + 10
                    elif val == 'eof':
                        # Restart video
                        player.seek(0, relative=False)
                        content_y += 110 # Placeholder space
                    else:
                        # Draw placeholder while loading/buffering
                        pygame.draw.rect(self.screen, (240, 240, 240), 
                                       pygame.Rect(content_x, content_y, content_width, 100), border_radius=8)
                        play_text = self.font_meta.render("VIDEO LOADING...", True, priority_color)
                        self.screen.blit(play_text, (content_x + 10, content_y + 40))
                        content_y += 110
                else:
                    # Draw video placeholder
                    placeholder_rect = pygame.Rect(content_x, content_y, content_width, 100)
                    pygame.draw.rect(self.screen, (240, 240, 240), placeholder_rect, border_radius=8)
                    
                    play_text = self.font_meta.render("VIDEO NOTICE", True, priority_color)
                    play_rect = play_text.get_rect(center=placeholder_rect.center)
                    self.screen.blit(play_text, play_rect)
                    content_y += 110
        
        # Description (wrapped) - minimal lines if image exists
        description = notice.get('description', '')
        if description:
            # Only show 1-2 lines if image/video exists to maximize space
            max_lines = 1 if media_url else 4
            wrapped_desc = textwrap.wrap(description, width=35)
            for i, line in enumerate(wrapped_desc[:max_lines]):
                desc_surface = self.font_desc.render(line, True, self.colors['text_secondary'])
                self.screen.blit(desc_surface, (content_x, content_y))
                content_y += 30
        
        # Category badge
        category = notice.get('category', 'general')
        category_surface = self.font_meta.render(f"• {category.upper()}", True, priority_color)
        category_rect = category_surface.get_rect(bottomleft=(content_x, y + height - 15))
        self.screen.blit(category_surface, category_rect)
        
        # Time badge
        try:
            created = notice.get('createdAt', '')
            if created:
                time_ago = self.get_time_ago(created)
                time_surface = self.font_meta.render(time_ago, True, self.colors['text_secondary'])
                time_rect = time_surface.get_rect(bottomright=(x + width - 15, y + height - 15))
                self.screen.blit(time_surface, time_rect)
        except:
            pass
    
    def get_time_ago(self, timestamp_str):
        """Convert timestamp to 'time ago' format"""
        try:
            from datetime import datetime
            created = datetime.fromisoformat(timestamp_str.replace('Z', '+00:00'))
            now = datetime.now(created.tzinfo)
            diff = now - created
            
            if diff.days > 0:
                return f"{diff.days}d ago"
            elif diff.seconds >= 3600:
                return f"{diff.seconds // 3600}h ago"
            elif diff.seconds >= 60:
                return f"{diff.seconds // 60}m ago"
            else:
                return "Just now"
        except:
            return ""
    
    def draw_empty_state(self, weather_data=None):
        """Draw empty state when no notices - show clock and weather"""
        # Full screen background
        self.screen.fill((20, 20, 30))  # Dark background for screensaver
        
        # Scale factors based on resolution (relative to 1920x1080)
        scale_w = self.width / 1920
        scale_h = self.height / 1080
        scale = min(scale_w, scale_h)

        # Calculates sizes dynamically
        logo_height = int(120 * scale)
        logo_y = int(30 * scale_h)
        logo_margin = int(50 * scale_w)
        
        # College Branding
        try:
            # Left Logo (SCOE)
            try:
                scoe_img = pygame.image.load("Logo/SCOE.png")
                # Scale while maintaining aspect ratio
                aspect = scoe_img.get_width() / scoe_img.get_height()
                new_width = int(logo_height * aspect)
                scoe_scaled = pygame.transform.smoothscale(scoe_img, (new_width, logo_height))
                self.screen.blit(scoe_scaled, (logo_margin, logo_y))
            except Exception as e:
                logger.error(f"Failed to load SCOE logo: {e}")

            # Right Logo (Swami)
            try:
                swami_img = pygame.image.load("Logo/swami.png")
                # Scale while maintaining aspect ratio
                aspect = swami_img.get_width() / swami_img.get_height()
                new_width = int(logo_height * aspect)
                swami_scaled = pygame.transform.smoothscale(swami_img, (new_width, logo_height))
                self.screen.blit(swami_scaled, (self.width - new_width - logo_margin, logo_y))
            except Exception as e:
                logger.error(f"Failed to load Swami logo: {e}")

            # College Name
            font_size = int(56 * scale)
            try:
                college_font = pygame.font.SysFont('Arial', font_size, bold=True)
            except:
                college_font = pygame.font.Font(None, font_size)
            
            # Allow text to wrap if screen is too small
            college_text = "Samarth College of Engineering & Management"
            text_surface = college_font.render(college_text, True, (255, 255, 255))
            
            # Check if text fits inside logos
            available_width = self.width - (2 * (logo_margin + new_width + 20))
            if text_surface.get_width() > available_width:
                 # Reduce font size until it fits
                 while text_surface.get_width() > available_width and font_size > 10:
                     font_size -= 2
                     try:
                        college_font = pygame.font.SysFont('Arial', font_size, bold=True)
                     except:
                        college_font = pygame.font.Font(None, font_size)
                     text_surface = college_font.render(college_text, True, (255, 255, 255))

            text_rect = text_surface.get_rect(center=(self.width // 2, logo_y + logo_height // 2))
            self.screen.blit(text_surface, text_rect)
            
        except Exception as e:
            logger.error(f"Error drawing branding: {e}")

        # Welcome Text (Big, No Background)
        welcome_size = int(80 * scale) # Increased font size
        welcome_font = pygame.font.SysFont('Arial', welcome_size, bold=True)
        welcome_text = "Welcome To Computer Department"
        welcome_surface = welcome_font.render(welcome_text, True, (255, 255, 255))
        
        # Position below header but above clock
        # Adjust center_y lower to make room (add margin)
        center_y = int(self.height / 2 + 150 * scale_h) 
        
        welcome_rect = welcome_surface.get_rect(center=(self.width // 2, center_y - int(250 * scale_h)))
        self.screen.blit(welcome_surface, welcome_rect)

        # Current time - Large digital clock
        now = datetime.now()
        time_str = now.strftime("%I:%M %p")
        date_str = now.strftime("%A, %B %d, %Y")
        
        # Draw large clock
        clock_size = int(120 * scale)
        time_font = pygame.font.SysFont('Arial', clock_size, bold=True)
        time_surface = time_font.render(time_str, True, (255, 255, 255))
        time_rect = time_surface.get_rect(center=(self.width // 2, center_y - int(80 * scale_h)))
        self.screen.blit(time_surface, time_rect)
        
        # Draw date
        date_size = int(32 * scale) 
        date_font = pygame.font.SysFont('Arial', date_size, bold=True)
        date_surface = date_font.render(date_str, True, (180, 180, 180))
        date_rect = date_surface.get_rect(center=(self.width // 2, center_y + int(20 * scale_h)))
        self.screen.blit(date_surface, date_rect)
        
        # Draw weather if available
        if weather_data:
            weather_y = center_y + int(100 * scale_h)
            
            # Temperature
            temp = weather_data.get('temperature', 'N/A')
            temp_text = f"{temp}°C"
            temp_size = int(72 * scale)
            temp_font = pygame.font.SysFont('Arial', temp_size, bold=True)
            temp_surface = temp_font.render(temp_text, True, (255, 200, 100))
            temp_rect = temp_surface.get_rect(center=(self.width // 2, weather_y))
            self.screen.blit(temp_surface, temp_rect)
            
            # Weather description
            desc_size = int(24 * scale)
            desc_font = pygame.font.SysFont('Arial', desc_size)
            description = weather_data.get('description', 'Clear').title()
            desc_surface = desc_font.render(description, True, (200, 200, 200))
            desc_rect = desc_surface.get_rect(center=(self.width // 2, weather_y + int(80 * scale_h)))
            self.screen.blit(desc_surface, desc_rect)
            
            # Additional weather info
            info_size = int(18 * scale)
            info_font = pygame.font.SysFont('Arial', info_size)
            humidity = weather_data.get('humidity', 'N/A')
            info_text = f"Humidity: {humidity}%"
            info_surface = info_font.render(info_text, True, (150, 150, 150))
            info_rect = info_surface.get_rect(center=(self.width // 2, weather_y + int(120 * scale_h)))
            self.screen.blit(info_surface, info_rect)
        
    def update(self):
        """Update the display"""
        pygame.display.flip()
    
    def cleanup(self):
        """Cleanup and quit"""
        # Release video captures
        for cap in self.video_caps.values():
            cap.release()
            
        pygame.quit()
        logger.info("Display cleanup complete")
