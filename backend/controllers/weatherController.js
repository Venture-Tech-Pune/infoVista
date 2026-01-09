const axios = require('axios');

// Weather API cache to reduce API calls
let weatherCache = {
    data: null,
    timestamp: null,
    ttl: 10 * 60 * 1000 // 10 minutes cache
};

// @desc    Get current weather
// @route   GET /api/weather/current
// @access  Public
exports.getCurrentWeather = async (req, res) => {
    try {
        const { city = 'Pune', country = 'IN' } = req.query;

        // Check cache
        if (weatherCache.data && weatherCache.timestamp && 
            (Date.now() - weatherCache.timestamp < weatherCache.ttl)) {
            return res.status(200).json({
                success: true,
                data: weatherCache.data,
                cached: true
            });
        }

        // Fetch from API
        const apiKey = process.env.WEATHER_API_KEY;
        if (!apiKey) {
            return res.status(500).json({
                success: false,
                message: 'Weather API key not configured'
            });
        }

        const url = `${process.env.WEATHER_API_URL}/weather`;
        const response = await axios.get(url, {
            params: {
                q: `${city},${country}`,
                appid: apiKey,
                units: 'metric'
            }
        });

        const weatherData = {
            city: response.data.name,
            country: response.data.sys.country,
            temperature: Math.round(response.data.main.temp),
            feelsLike: Math.round(response.data.main.feels_like),
            humidity: response.data.main.humidity,
            pressure: response.data.main.pressure,
            description: response.data.weather[0].description,
            icon: response.data.weather[0].icon,
            windSpeed: response.data.wind.speed,
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
            error: error.response?.data?.message || error.message
        });
    }
};

// @desc    Get weather forecast
// @route   GET /api/weather/forecast
// @access  Public
exports.getWeatherForecast = async (req, res) => {
    try {
        const { city = 'Pune', country = 'IN', days = 3 } = req.query;

        const apiKey = process.env.WEATHER_API_KEY;
        if (!apiKey) {
            return res.status(500).json({
                success: false,
                message: 'Weather API key not configured'
            });
        }

        const url = `${process.env.WEATHER_API_URL}/forecast`;
        const response = await axios.get(url, {
            params: {
                q: `${city},${country}`,
                appid: apiKey,
                units: 'metric',
                cnt: days * 8 // 8 forecasts per day (3-hour intervals)
            }
        });

        const forecast = response.data.list.map(item => ({
            date: new Date(item.dt * 1000),
            temperature: Math.round(item.main.temp),
            humidity: item.main.humidity,
            description: item.weather[0].description,
            icon: item.weather[0].icon
        }));

        res.status(200).json({
            success: true,
            data: {
                city: response.data.city.name,
                country: response.data.city.country,
                forecast
            }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Failed to fetch forecast data',
            error: error.response?.data?.message || error.message
        });
    }
};
