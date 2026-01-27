import requests
import time
import logging

class APIClient:
    def __init__(self, base_url, device_id):
        self.base_url = base_url
        self.device_id = device_id
        self.logger = logging.getLogger(__name__)
        
    def get_active_notices(self):
        """Fetch active notices from the backend"""
        try:
            url = f"{self.base_url}/api/notices/active"
            params = {'deviceId': self.device_id}
            
            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()
            
            data = response.json()
            if data.get('success'):
                self.logger.info(f"✅ Fetched {len(data.get('data', []))} active notices")
                return data.get('data', [])
            else:
                self.logger.error(f"❌ API returned error: {data.get('message')}")
                return []
                
        except requests.exceptions.ConnectionError:
            self.logger.error("❌ Connection error: Cannot reach server")
            return []
        except requests.exceptions.Timeout:
            self.logger.error("❌ Request timeout")
            return []
        except requests.exceptions.RequestException as e:
            self.logger.error(f"❌ Request error: {str(e)}")
            return []
    
    def get_weather(self, city, country):
        """Fetch current weather data"""
        try:
            url = f"{self.base_url}/api/weather/current"
            params = {'city': city, 'country': country}
            
            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()
            
            data = response.json()
            if data.get('success'):
                self.logger.info(f"✅ Fetched weather data for {city}")
                return data.get('data')
            else:
                self.logger.error(f"❌ Weather API error: {data.get('message')}")
                raise Exception("API Error")
                
        except Exception as e:
            self.logger.error(f"❌ Weather fetch error: {str(e)}")
            self.logger.warning("⚠️ Using dummy weather data for display")
            # Return dummy data for testing/offline mode
            return {
                'temperature': 28,
                'description': 'Sunny',
                'humidity': 45,
                'windSpeed': 12,
                'city': city,
                'country': country
            }
    
    def update_device_status(self, status, ip_address=None):
        """Update device status on the server"""
        try:
            url = f"{self.base_url}/api/devices/{self.device_id}/status"
            data = {
                'status': status,
                'ipAddress': ip_address
            }
            
            response = requests.put(url, json=data, timeout=10)
            response.raise_for_status()
            
            result = response.json()
            if result.get('success'):
                self.logger.info(f"✅ Device status updated: {status}")
                return True
            else:
                self.logger.warning(f"⚠️  Status update failed: {result.get('message')}")
                return False
                
        except Exception as e:
            self.logger.error(f"❌ Status update error: {str(e)}")
            return False
    
    def download_media(self, media_url, save_path):
        """Download media file from server"""
        try:
            if not media_url:
                return None
            
            # Handle relative URLs
            if media_url.startswith('/'):
                url = f"{self.base_url}{media_url}"
            else:
                url = media_url
            
            response = requests.get(url, timeout=30, stream=True)
            response.raise_for_status()
            
            with open(save_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
            
            self.logger.info(f"✅ Downloaded media: {save_path}")
            return save_path
            
        except Exception as e:
            self.logger.error(f"❌ Media download error: {str(e)}")
            return None
