const axios = require('axios');

// Weather API cache to reduce API calls
let weatherCache = {
    data: null,
    timestamp: null,
    ttl: 10 * 60 * 1000 // 10 minutes cache
};
// Pune coordinates (hardcoded)
const PUNE_LATITUDE = 18.5204;
const PUNE_LONGITUDE = 73.8567;

// @desc    Get current weather for Pune using Open-Meteo (Free, No API Key)
// @route   GET /api/weather/current
// @access  Public
exports.getCurrentWeather = async (req, res) => {
    try {
        // Check cache
        if (weatherCache.data && weatherCache.timestamp && 
            (Date.now() - weatherCache.timestamp < weatherCache.ttl)) {
            return res.status(200).json({
                success: true,
                data: weatherCache.data,
                cached: true
            });
        }

        // Fetch weather data from Open-Meteo API (Free, No API Key Required)
        const weatherUrl = `https://api.open-meteo.com/v1/forecast`;
        const weatherResponse = await axios.get(weatherUrl, {
            params: {
                latitude: PUNE_LATITUDE,
                longitude: PUNE_LONGITUDE,
                current: 'temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,surface_pressure',
                timezone: 'Asia/Kolkata'
            }
        });

        const current = weatherResponse.data.current;
        
        // Map weather codes to descriptions
        const getWeatherDescription = (code) => {
            const weatherCodes = {
                0: 'Clear sky',
                1: 'Mainly clear', 2: 'Partly cloudy', 3: 'Overcast',
                45: 'Foggy', 48: 'Foggy',
                51: 'Light drizzle', 53: 'Moderate drizzle', 55: 'Dense drizzle',
                61: 'Slight rain', 63: 'Moderate rain', 65: 'Heavy rain',
                71: 'Slight snow', 73: 'Moderate snow', 75: 'Heavy snow',
                80: 'Slight rain showers', 81: 'Moderate rain showers', 82: 'Violent rain showers',
                95: 'Thunderstorm', 96: 'Thunderstorm with hail', 99: 'Thunderstorm with heavy hail'
            };
            return weatherCodes[code] || 'Unknown';
        };

        const weatherData = {
            city: 'Pune',
            country: 'IN',
            temperature: Math.round(current.temperature_2m),
            feelsLike: Math.round(current.apparent_temperature),
            humidity: current.relative_humidity_2m,
            pressure: Math.round(current.surface_pressure),
            description: getWeatherDescription(current.weather_code),
            icon: current.weather_code < 3 ? '01d' : current.weather_code < 50 ? '02d' : '10d',
            windSpeed: current.wind_speed_10m,
            timestamp: new Date()
        };

        // Update cache
        weatherCache.data = weatherData;
        weatherCache.timestamp = Date.now();

        res.status(200).json({
            success: true,
            data: weatherData,
            cached: false
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Failed to fetch weather data',
            error: error.response?.data?.reason || error.message
        });
    }
};

// @desc    Get weather forecast for Pune
// @route   GET /api/weather/forecast
// @access  Public
exports.getWeatherForecast = async (req, res) => {
    try {
        const days = req.query.days || 3;

        // Fetch forecast data from Open-Meteo
        const weatherUrl = `https://api.open-meteo.com/v1/forecast`;
        const weatherResponse = await axios.get(weatherUrl, {
            params: {
                latitude: PUNE_LATITUDE,
                longitude: PUNE_LONGITUDE,
                daily: 'temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max',
                forecast_days: days,
                timezone: 'Asia/Kolkata'
            }
        });

        const daily = weatherResponse.data.daily;
        
        const getWeatherDescription = (code) => {
            const weatherCodes = {
                0: 'Clear sky',
                1: 'Mainly clear', 2: 'Partly cloudy', 3: 'Overcast',
                45: 'Foggy', 48: 'Foggy',
                51: 'Light drizzle', 53: 'Moderate drizzle', 55: 'Dense drizzle',
                61: 'Slight rain', 63: 'Moderate rain', 65: 'Heavy rain',
                71: 'Slight snow', 73: 'Moderate snow', 75: 'Heavy snow',
                80: 'Rain showers', 81: 'Rain showers', 82: 'Heavy rain showers',
                95: 'Thunderstorm', 96: 'Thunderstorm with hail'
            };
            return weatherCodes[code] || 'Unknown';
        };

        const forecast = daily.time.map((date, index) => ({
            date: new Date(date),
            temperature: Math.round((daily.temperature_2m_max[index] + daily.temperature_2m_min[index]) / 2),
            tempMax: Math.round(daily.temperature_2m_max[index]),
            tempMin: Math.round(daily.temperature_2m_min[index]),
            humidity: daily.precipitation_probability_max[index] || 0,
            description: getWeatherDescription(daily.weather_code[index]),
            icon: daily.weather_code[index] < 3 ? '01d' : '10d'
        }));
        res.status(200).json({
            success: true,
            data: {
                city: 'Pune',
                country: 'IN',
                forecast
            }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Failed to fetch forecast data',
            error: error.response?.data?.reason || error.message
        });
    }
};
