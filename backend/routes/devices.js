const express = require('express');
const router = express.Router();
const deviceController = require('../controllers/deviceController');
const { protect, authorize } = require('../middleware/auth');

// Public route for device status updates
router.put('/:deviceId/status', deviceController.updateDeviceStatus);

// Protected routes
router.use(protect);

// @route   GET /api/devices
// @desc    Get all devices
// @access  Private
router.get('/', deviceController.getDevices);

// @route   GET /api/devices/:id
// @desc    Get single device
// @access  Private
router.get('/:id', deviceController.getDevice);

// Admin only routes
router.use(authorize('admin'));

// @route   POST /api/devices
// @desc    Register new device
// @access  Private (Admin)
router.post('/', deviceController.registerDevice);

// @route   PUT /api/devices/:id
// @desc    Update device
// @access  Private (Admin)
router.put('/:id', deviceController.updateDevice);

// @route   DELETE /api/devices/:id
// @desc    Delete device
// @access  Private (Admin)
router.delete('/:id', deviceController.deleteDevice);

module.exports = router;
