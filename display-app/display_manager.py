import pygame
import logging
import textwrap
import requests
import io
import os
import sys
import platform
import threading
import subprocess
import time as time_module
import numpy as np
from datetime import datetime
from PIL import Image

# ---------------------------------------------------------------------------
# Raspberry Pi Detection
# ---------------------------------------------------------------------------
IS_RASPBERRY_PI = (
    os.path.exists('/proc/device-tree/model') or
    platform.machine() in ('armv6l', 'armv7l', 'aarch64') or
    'raspberry' in platform.node().lower()
)

if IS_RASPBERRY_PI:
    import logging as _log
    _log.getLogger(__name__).info("🍓 Raspberry Pi detected – using Pi-optimised settings")

# ---------------------------------------------------------------------------
# ffpyplayer (desktop only, optional)
# ---------------------------------------------------------------------------
try:
    if IS_RASPBERRY_PI:
        raise ImportError("Skipping ffpyplayer on Pi")
    from ffpyplayer.player import MediaPlayer
except ImportError:
    MediaPlayer = None

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Pi Video Player  –  uses an ffmpeg subprocess + background thread
# Keeps only the latest decoded frame in memory, loops automatically.
# All audio is suppressed (muted=True enforced by design).
# ---------------------------------------------------------------------------
class PiVideoPlayer:
    """
    Lightweight video player for Raspberry Pi (and any system without ffpyplayer).
    Decodes via an ffmpeg subprocess piping raw RGB24 frames into a daemon thread.
    Designed for low-RAM devices (1 GB).  Always plays silently.
    """

    # Maximum resolution for a single grid cell on Pi (keeps RAM + CPU low)
    MAX_W = 480
    MAX_H = 360
    # Target decode framerate (keeps CPU loads manageable)
    DECODE_FPS = 15

    def __init__(self, path, hint_width=480, hint_height=360):
        self.path = path
        # Clamp target decode size
        self.width  = min(hint_width,  self.MAX_W)
        self.height = min(hint_height, self.MAX_H)

        self._latest_surface = None
        self._lock = threading.Lock()
        self._stop_event = threading.Event()

        self._thread = threading.Thread(target=self._reader, daemon=True, name="PiVideoReader")
        self._thread.start()

    def _build_cmd(self):
        cmd = [
            'ffmpeg',
            '-re',                        # Read at native frame-rate
            '-stream_loop', '-1',         # Loop forever
            '-i', self.path,
            '-vf', f'scale={self.width}:{self.height}:flags=fast_bilinear',
            '-r', str(self.DECODE_FPS),
            '-f', 'rawvideo',
            '-pix_fmt', 'rgb24',
            '-an',                        # No audio (always muted)
            'pipe:1',
        ]
        # Try hardware-accelerated decoder on Pi 4 / Pi 5 (V4L2)
        # Insert before -i if the codec is present; ffmpeg falls back silently otherwise
        if IS_RASPBERRY_PI:
            cmd = ['ffmpeg', '-hwaccel', 'auto', '-re', '-stream_loop', '-1',
                   '-i', self.path,
                   '-vf', f'scale={self.width}:{self.height}:flags=fast_bilinear',
                   '-r', str(self.DECODE_FPS),
                   '-f', 'rawvideo', '-pix_fmt', 'rgb24',
                   '-an', 'pipe:1']
        return cmd

    def _reader(self):
        frame_bytes = self.width * self.height * 3
        while not self._stop_event.is_set():
            proc = None
            try:
                proc = subprocess.Popen(
                    self._build_cmd(),
                    stdout=subprocess.PIPE,
                    stderr=subprocess.DEVNULL,
                )
                while not self._stop_event.is_set():
                    raw = proc.stdout.read(frame_bytes)
                    if len(raw) < frame_bytes:
                        break  # EOF → restart loop
                    arr = np.frombuffer(raw, dtype=np.uint8).reshape(
                        (self.height, self.width, 3)
                    )
                    # pygame expects (width, height) so transpose axes
                    surf = pygame.surfarray.make_surface(arr.swapaxes(0, 1))
                    with self._lock:
                        self._latest_surface = surf
            except Exception as exc:
                logger.warning(f"PiVideoPlayer error: {exc}")
            finally:
                if proc:
                    try:
                        proc.terminate()
                        proc.wait(timeout=2)
                    except Exception:
                        pass
            if not self._stop_event.is_set():
                time_module.sleep(0.5)   # Brief pause before restart

    def get_frame(self):
        """Return the latest pygame Surface, or None if not yet ready."""
        with self._lock:
            return self._latest_surface

    def close(self):
        self._stop_event.set()


# ---------------------------------------------------------------------------
# Display Manager
# ---------------------------------------------------------------------------

class DisplayManager:
    # Maximum number of images to keep in the in-memory cache (RAM guard for Pi)
    _IMAGE_CACHE_LIMIT = 20

    def __init__(self, width=1920, height=1080, fullscreen=False):
        """Initialize the Pygame display"""
        pygame.init()

        # On Pi, honour the requested resolution but cap FPS later in main.py
        self.width = width
        self.height = height

        # Set display mode
        flags = 0
        if fullscreen:
            flags |= pygame.FULLSCREEN
        # On Pi without a desktop compositor, NOFRAME can reduce tearing
        if IS_RASPBERRY_PI and fullscreen:
            flags |= pygame.NOFRAME

        self.screen = pygame.display.set_mode((width, height), flags)
        pygame.display.set_caption("INFOVISTA Display Board")

        # Colors - Modern palette
        self.colors = {
            'bg': (248, 249, 250),
            'primary': (33, 150, 243),
            'text': (26, 26, 26),
            'text_secondary': (102, 102, 102),
            'white': (255, 255, 255),
            'border': (224, 224, 224),
            'urgent': (244, 67, 54),
            'high': (255, 152, 0),
            'medium': (33, 150, 243),
            'low': (76, 175, 80),
        }

        # Fonts  (try system Arial first, fall back to pygame default)
        try:
            self.font_title  = pygame.font.SysFont('Arial', 32, bold=True)
            self.font_desc   = pygame.font.SysFont('Arial', 24)
            self.font_meta   = pygame.font.SysFont('Arial', 18)
            self.font_header = pygame.font.SysFont('Arial', 48, bold=True)
        except Exception:
            self.font_title  = pygame.font.Font(None, 32)
            self.font_desc   = pygame.font.Font(None, 24)
            self.font_meta   = pygame.font.Font(None, 18)
            self.font_header = pygame.font.Font(None, 48)

        # Caches
        self.image_cache  = {}   # url -> pygame.Surface
        self.video_caps   = {}   # legacy (unused) – kept for compat
        self.video_players = {}  # url -> {'player': <MediaPlayer|PiVideoPlayer>, 'is_muted': bool}

        self.base_url = "http://localhost:3000"
        self.cache_dir = "cache"
        os.makedirs(self.cache_dir, exist_ok=True)

        # Ticker state
        self.ticker_x = width

        # Header logo – loaded once and cached (no disk hit per frame)
        self._header_logo = None
        self._header_logo_loaded = False

        # Audio (desktop only – Pi runs silently)
        if not IS_RASPBERRY_PI:
            try:
                pygame.mixer.init()
                logger.info("✅ Audio mixer initialised")
            except Exception as e:
                logger.error(f"❌ Audio mixer init failed: {e}")

        logger.info(
            f"✅ Display initialised: {width}x{height}  |  Pi={IS_RASPBERRY_PI}  |  ffpyplayer={'yes' if MediaPlayer else 'no'}"
        )

    # ------------------------------------------------------------------
    # Grid helpers
    # ------------------------------------------------------------------

    def calculate_grid(self, notice_count):
        """Calculate grid dimensions based on number of notices"""
        if notice_count == 0:
            return 0, 0
        elif notice_count == 1:
            return 1, 1
        elif notice_count == 2:
            return 2, 1
        elif notice_count <= 4:
            return 2, 2
        elif notice_count <= 6:
            return 3, 2
        else:
            return 3, 3

    def draw_grid_layout(self, notices, weather_data=None, total_count=None):
        """Draw notices in a grid/collage layout"""
        self.screen.fill(self.colors['bg'])

        if not notices:
            self.draw_empty_state(weather_data)
            return

        display_notices = notices[:9]
        cols, rows = self.calculate_grid(len(display_notices))

        padding       = 20
        header_height = 80
        available_width  = self.width  - (padding * (cols + 1))
        available_height = self.height - header_height - (padding * (rows + 1))
        box_width  = available_width  // cols
        box_height = available_height // rows

        self.draw_header(total_count if total_count is not None else len(display_notices))

        for idx, notice in enumerate(display_notices):
            row = idx // cols
            col = idx % cols
            x = padding + col * (box_width  + padding)
            y = header_height + padding + row * (box_height + padding)
            self.draw_notice_box(notice, x, y, box_width, box_height, idx)

    def draw_header(self, notice_count):
        """Draw header with title, logo and notice count"""
        header_rect = pygame.Rect(0, 0, self.width, 80)
        pygame.draw.rect(self.screen, self.colors['primary'], header_rect)

        # --- Logo (top-left) ---
        logo_h   = 56          # height of logo inside the 80 px header
        logo_x   = 12
        logo_y   = (80 - logo_h) // 2
        logo_end = logo_x      # will advance after logo is drawn

        if not self._header_logo_loaded:
            try:
                raw = pygame.image.load("Logo/SCOE.png").convert_alpha()
                aspect = raw.get_width() / raw.get_height()
                logo_w = int(logo_h * aspect)
                self._header_logo = pygame.transform.smoothscale(raw, (logo_w, logo_h))
            except Exception as e:
                logger.warning(f"Header logo not loaded: {e}")
                self._header_logo = None
            self._header_logo_loaded = True

        if self._header_logo:
            self.screen.blit(self._header_logo, (logo_x, logo_y))
            logo_end = logo_x + self._header_logo.get_width() + 12

        # --- Title text (right of logo) ---
        title_text = self.font_header.render("INFOVISTA", True, self.colors['white'])
        self.screen.blit(title_text, title_text.get_rect(midleft=(logo_end, 40)))

        # --- Right side: time + notice count ---
        count_text = self.font_meta.render(
            f"{notice_count} Active Notice{'s' if notice_count != 1 else ''}", True, self.colors['white']
        )
        self.screen.blit(count_text, count_text.get_rect(midright=(self.width - 30, 40)))

        time_text = self.font_meta.render(datetime.now().strftime("%I:%M %p"), True, self.colors['white'])
        self.screen.blit(time_text, time_text.get_rect(midright=(self.width - 200, 40)))

    # ------------------------------------------------------------------
    # Media loading
    # ------------------------------------------------------------------

    def _scale(self, surface, size):
        """
        Scale a surface.  On Pi use fast (nearest-neighbour) scale to save CPU.
        On desktop use smoothscale for nicer quality.
        """
        if IS_RASPBERRY_PI:
            return pygame.transform.scale(surface, size)
        return pygame.transform.smoothscale(surface, size)

    def load_image(self, media_url):
        """Load image from URL and cache it (LRU-limited for low-RAM devices)"""
        if not media_url:
            return None

        if media_url in self.image_cache:
            return self.image_cache[media_url]

        try:
            image_url = f"{self.base_url}{media_url}"
            response  = requests.get(image_url, timeout=5)
            response.raise_for_status()

            pil_image     = Image.open(io.BytesIO(response.content)).convert('RGB')
            size          = pil_image.size
            data          = pil_image.tobytes()
            pygame_surface = pygame.image.fromstring(data, size, 'RGB')

            # Enforce cache size limit (evict oldest entry)
            if len(self.image_cache) >= self._IMAGE_CACHE_LIMIT:
                oldest_key = next(iter(self.image_cache))
                del self.image_cache[oldest_key]
                logger.debug(f"🗑️ Image cache full – evicted {oldest_key}")

            self.image_cache[media_url] = pygame_surface
            logger.info(f"✅ Loaded image: {media_url}")
            return pygame_surface
        except Exception as e:
            logger.error(f"❌ Failed to load image {media_url}: {e}")
            return None

    def _download_video(self, media_url):
        """Download video to local cache and return the local path."""
        filename   = media_url.split('/')[-1]
        local_path = os.path.join(self.cache_dir, filename)
        if not os.path.exists(local_path):
            logger.info(f"⏳ Downloading video: {media_url}")
            video_url = f"{self.base_url}{media_url}"
            response  = requests.get(video_url, timeout=30, stream=True)
            response.raise_for_status()
            with open(local_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
            logger.info(f"✅ Video saved: {local_path}")
        return local_path

    def load_video(self, media_url):
        """
        Return a video player for *media_url*.
        • On Raspberry Pi  → PiVideoPlayer  (ffmpeg subprocess, always muted)
        • On desktop       → MediaPlayer from ffpyplayer (if available), else PiVideoPlayer
        All videos are always muted — audio is disabled unconditionally.
        """
        if not media_url:
            return None

        if media_url in self.video_players:
            return self.video_players[media_url]['player']

        try:
            local_path = self._download_video(media_url)
        except Exception as e:
            logger.error(f"❌ Failed to download video {media_url}: {e}")
            return None

        player = None

        # --- Desktop path (ffpyplayer) ---
        if MediaPlayer is not None and not IS_RASPBERRY_PI:
            try:
                ff_opts = {'paused': False, 'volume': 0.0, 'loop': 0}  # volume=0 → muted
                player = MediaPlayer(local_path, ff_opts=ff_opts)
                if hasattr(player, 'set_mute'):
                    player.set_mute(True)
                player.set_volume(0.0)
                logger.info(f"✅ ffpyplayer created (muted): {media_url}")
            except Exception as e:
                logger.warning(f"⚠️ ffpyplayer failed ({e}), falling back to PiVideoPlayer")
                player = None

        # --- Pi / fallback path ---
        if player is None:
            try:
                player = PiVideoPlayer(local_path)
                logger.info(f"✅ PiVideoPlayer created: {media_url}")
            except Exception as e:
                logger.error(f"❌ PiVideoPlayer creation failed: {e}")
                return None

        self.video_players[media_url] = {'player': player, 'is_muted': True}
        return player

    # ------------------------------------------------------------------
    # Notice rendering
    # ------------------------------------------------------------------

    def draw_notice_box(self, notice, x, y, width, height, idx=0):
        """Draw a single notice in a box"""
        # Shadow + box
        pygame.draw.rect(self.screen, (200, 200, 200), pygame.Rect(x + 4, y + 4, width, height), border_radius=12)
        pygame.draw.rect(self.screen, self.colors['white'],   pygame.Rect(x,     y,     width, height), border_radius=12)

        # Priority bar
        priority       = notice.get('priority', 'medium').lower()
        priority_color = self.colors.get(priority, self.colors['medium'])
        pygame.draw.rect(
            self.screen, priority_color,
            pygame.Rect(x, y, width, 8),
            border_top_left_radius=12, border_top_right_radius=12,
        )

        content_y     = y + 20
        content_x     = x + 15
        content_width = width - 30

        # Title
        title = notice.get('title', 'Untitled')
        title_surface = self.font_title.render(title[:40], True, self.colors['text'])
        self.screen.blit(title_surface, (content_x, content_y))
        content_y += 45

        media_url  = notice.get('mediaUrl')
        media_type = notice.get('mediaType')
        description = notice.get('description', '')

        # Calculate max description lines and reserved height based on box height
        # When media is present, limit to 2 lines so description stays compact below image
        if height > 500:
            max_lines = 2 if media_url else 6
        else:
            max_lines = 1 if media_url else 3

        reserved_desc_height = 0
        if description:
            # Apply same 30-word cap for accurate height pre-calculation
            desc_words = description.split()
            if len(desc_words) > 30:
                description_capped = ' '.join(desc_words[:30]) + '…'
            else:
                description_capped = description
            wrapped = textwrap.wrap(description_capped, width=40)
            actual_lines = min(len(wrapped), max_lines)
            reserved_desc_height = actual_lines * 28 + 10  # 28px per line + 10px spacing

        if media_url:
            if media_type == 'image':
                image_surface = self.load_image(media_url)
                if image_surface:
                    footer_height    = 40
                    available_height = (y + height) - content_y - footer_height - reserved_desc_height
                    img_width        = content_width
                    orig_w, orig_h   = image_surface.get_size()
                    aspect_ratio     = orig_h / orig_w
                    img_height       = min(int(img_width * aspect_ratio), available_height)
                    if img_height == available_height:
                        img_width = int(img_height / aspect_ratio)
                    scaled_image = self._scale(image_surface, (img_width, img_height))
                    img_x = content_x + (content_width - img_width) // 2
                    self.screen.blit(scaled_image, (img_x, content_y))
                    content_y += img_height + 10

            elif media_type == 'video':
                # -------------------------------------------------------
                # ALL VIDEOS ARE MUTED — isMuted field is intentionally
                # ignored to reduce complexity and resource use on Pi.
                # -------------------------------------------------------
                player = self.load_video(media_url)

                if player:
                    frame = None

                    # PiVideoPlayer path
                    if isinstance(player, PiVideoPlayer):
                        frame = player.get_frame()
                    else:
                        # ffpyplayer path (desktop)
                        try:
                            player.set_volume(0.0)
                            if player.get_pause():
                                player.set_pause(False)
                            raw_frame, val = player.get_frame()
                            if val == 'eof':
                                player.seek(0, relative=False)
                            elif raw_frame is not None:
                                img, _t = raw_frame
                                size = img.get_size()
                                data = img.to_bytearray()[0]
                                frame = pygame.image.frombuffer(data, size, 'RGB')
                        except Exception as e:
                            logger.debug(f"ffpyplayer get_frame error: {e}")

                    if frame is not None:
                        footer_height    = 40
                        available_height = (y + height) - content_y - footer_height - reserved_desc_height
                        img_width        = content_width
                        orig_w, orig_h   = frame.get_size()
                        aspect_ratio     = orig_h / orig_w if orig_w else 1
                        img_height       = min(int(img_width * aspect_ratio), available_height)
                        if img_height == available_height:
                            img_width = int(img_height / aspect_ratio)

                        scaled_video = self._scale(frame, (img_width, img_height))
                        img_x = content_x + (content_width - img_width) // 2
                        self.screen.blit(scaled_video, (img_x, content_y))
                        content_y += img_height + 10
                    else:
                        # Loading placeholder
                        pygame.draw.rect(
                            self.screen, (240, 240, 240),
                            pygame.Rect(content_x, content_y, content_width, 100), border_radius=8
                        )
                        loading_text = self.font_meta.render("VIDEO LOADING…", True, priority_color)
                        self.screen.blit(loading_text, (content_x + 10, content_y + 40))
                        content_y += 110
                else:
                    # Player unavailable placeholder
                    ph_rect = pygame.Rect(content_x, content_y, content_width, 100)
                    pygame.draw.rect(self.screen, (240, 240, 240), ph_rect, border_radius=8)
                    play_text = self.font_meta.render("VIDEO NOTICE", True, priority_color)
                    self.screen.blit(play_text, play_text.get_rect(center=ph_rect.center))
                    content_y += 110

        # Description (wrapped) — capped at 30 words to match Android input limit
        # Lines are only drawn if they fit within the notice box bounds
        if description:
            words = description.split()
            MAX_WORDS = 30
            if len(words) > MAX_WORDS:
                description = ' '.join(words[:MAX_WORDS]) + '…'
            wrapped = textwrap.wrap(description, width=35)
            footer_zone = y + height - 50   # bottom boundary: leave 50 px for footer badge
            for line in wrapped[:max_lines]:
                if content_y + 28 > footer_zone:   # stop if next line would overflow
                    break
                desc_surface = self.font_desc.render(line, True, self.colors['text_secondary'])
                self.screen.blit(desc_surface, (content_x, content_y))
                content_y += 30

        # Category badge
        category         = notice.get('category', 'general')
        category_surface = self.font_meta.render(f"• {category.upper()}", True, priority_color)
        self.screen.blit(category_surface, category_surface.get_rect(bottomleft=(content_x, y + height - 15)))

        # Time badge
        try:
            created = notice.get('createdAt', '')
            if created:
                time_surface = self.font_meta.render(self.get_time_ago(created), True, self.colors['text_secondary'])
                self.screen.blit(time_surface, time_surface.get_rect(bottomright=(x + width - 15, y + height - 15)))
        except Exception:
            pass

    # ------------------------------------------------------------------
    # Utility
    # ------------------------------------------------------------------

    def get_time_ago(self, timestamp_str):
        """Convert ISO timestamp to 'time ago' string"""
        try:
            created = datetime.fromisoformat(timestamp_str.replace('Z', '+00:00'))
            now     = datetime.now(created.tzinfo)
            diff    = now - created
            if diff.days > 0:
                return f"{diff.days}d ago"
            elif diff.seconds >= 3600:
                return f"{diff.seconds // 3600}h ago"
            elif diff.seconds >= 60:
                return f"{diff.seconds // 60}m ago"
            else:
                return "Just now"
        except Exception:
            return ""

    def draw_empty_state(self, weather_data=None):
        """Draw screensaver-style idle screen with clock and weather"""
        self.screen.fill((20, 20, 30))

        scale_w = self.width  / 1920
        scale_h = self.height / 1080
        scale   = min(scale_w, scale_h)

        logo_height = int(120 * scale)
        logo_y      = int(30  * scale_h)
        logo_margin = int(50  * scale_w)

        try:
            scoe_img  = pygame.image.load("Logo/SCOE.png")
            aspect    = scoe_img.get_width() / scoe_img.get_height()
            new_width = int(logo_height * aspect)
            self.screen.blit(self._scale(scoe_img, (new_width, logo_height)), (logo_margin, logo_y))
        except Exception as e:
            logger.error(f"Failed to load SCOE logo: {e}")
            new_width = logo_height  # fallback for text placement

        try:
            swami_img   = pygame.image.load("Logo/swami.png")
            aspect      = swami_img.get_width() / swami_img.get_height()
            swami_width = int(logo_height * aspect)
            self.screen.blit(self._scale(swami_img, (swami_width, logo_height)),
                             (self.width - swami_width - logo_margin, logo_y))
        except Exception as e:
            logger.error(f"Failed to load Swami logo: {e}")

        # College name (auto-shrink to fit)
        font_size    = int(56 * scale)
        college_text = "Samarth College of Engineering & Management"
        available_w  = self.width - 2 * (logo_margin + new_width + 20)
        while font_size > 10:
            try:
                cf = pygame.font.SysFont('Arial', font_size, bold=True)
            except Exception:
                cf = pygame.font.Font(None, font_size)
            ts = cf.render(college_text, True, (255, 255, 255))
            if ts.get_width() <= available_w:
                break
            font_size -= 2
        self.screen.blit(ts, ts.get_rect(center=(self.width // 2, logo_y + logo_height // 2)))

        center_y = int(self.height / 2 + 150 * scale_h)

        # Welcome
        welcome_size = int(80 * scale)
        try:
            wf = pygame.font.SysFont('Arial', welcome_size, bold=True)
        except Exception:
            wf = pygame.font.Font(None, welcome_size)
        ws = wf.render("Welcome To Computer Department", True, (255, 255, 255))
        self.screen.blit(ws, ws.get_rect(center=(self.width // 2, center_y - int(250 * scale_h))))

        # Clock
        now      = datetime.now()
        time_str = now.strftime("%I:%M %p")
        date_str = now.strftime("%A, %B %d, %Y")

        clock_size = int(120 * scale)
        try:
            tf = pygame.font.SysFont('Arial', clock_size, bold=True)
        except Exception:
            tf = pygame.font.Font(None, clock_size)
        ts2 = tf.render(time_str, True, (255, 255, 255))
        self.screen.blit(ts2, ts2.get_rect(center=(self.width // 2, center_y - int(80 * scale_h))))

        date_size = int(32 * scale)
        try:
            df = pygame.font.SysFont('Arial', date_size, bold=True)
        except Exception:
            df = pygame.font.Font(None, date_size)
        ds = df.render(date_str, True, (180, 180, 180))
        self.screen.blit(ds, ds.get_rect(center=(self.width // 2, center_y + int(20 * scale_h))))

        # Weather
        if weather_data:
            weather_y  = center_y + int(100 * scale_h)
            temp       = weather_data.get('temperature', 'N/A')
            temp_size  = int(72 * scale)
            try:
                tempf = pygame.font.SysFont('Arial', temp_size, bold=True)
            except Exception:
                tempf = pygame.font.Font(None, temp_size)
            temp_s = tempf.render(f"{temp}°C", True, (255, 200, 100))
            self.screen.blit(temp_s, temp_s.get_rect(center=(self.width // 2, weather_y)))

            desc_size = int(24 * scale)
            try:
                descf = pygame.font.SysFont('Arial', desc_size)
            except Exception:
                descf = pygame.font.Font(None, desc_size)
            description = weather_data.get('description', 'Clear').title()
            desc_s = descf.render(description, True, (200, 200, 200))
            self.screen.blit(desc_s, desc_s.get_rect(center=(self.width // 2, weather_y + int(80 * scale_h))))

            info_size = int(18 * scale)
            try:
                infof = pygame.font.SysFont('Arial', info_size)
            except Exception:
                infof = pygame.font.Font(None, info_size)
            humidity = weather_data.get('humidity', 'N/A')
            info_s = infof.render(f"Humidity: {humidity}%", True, (150, 150, 150))
            self.screen.blit(info_s, info_s.get_rect(center=(self.width // 2, weather_y + int(120 * scale_h))))

    # ------------------------------------------------------------------
    # Display update & cleanup
    # ------------------------------------------------------------------

    def update(self):
        """Flip the display buffer"""
        pygame.display.flip()

    def cleanup(self):
        """Release all resources and quit pygame"""
        for cap in self.video_caps.values():
            try:
                cap.release()
            except Exception:
                pass

        for entry in self.video_players.values():
            try:
                player = entry['player'] if isinstance(entry, dict) else entry
                if isinstance(player, PiVideoPlayer):
                    player.close()
                else:
                    player.close_player()
            except Exception:
                pass

        pygame.quit()
        logger.info("Display cleanup complete")
