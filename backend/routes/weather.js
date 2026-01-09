const express = require('express');
const router = express.Router();
const weatherController = require('../controllers/weatherController');

// @route   GET /api/weather/current
// @desc    Get current weather
// @access  Public
router.get('/current', weatherController.getCurrentWeather);

// @route   GET /api/weather/forecast
// @desc    Get weather forecast
// @access  Public
router.get('/forecast', weatherController.getWeatherForecast);

module.exports = router;
