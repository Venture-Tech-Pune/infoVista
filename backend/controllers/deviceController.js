const Device = require('../models/Device');

// @desc    Get all devices
// @route   GET /api/devices
// @access  Private
exports.getDevices = async (req, res) => {
    try {
        const { status, location } = req.query;

        const filter = {};
        if (status) filter.status = status;
        if (location) filter.location = new RegExp(location, 'i');

        const devices = await Device.find(filter)
            .populate('registeredBy', 'name email')
            .sort({ createdAt: -1 });

        res.status(200).json({
            success: true,
            data: devices
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Get single device
// @route   GET /api/devices/:id
// @access  Private
exports.getDevice = async (req, res) => {
    try {
        const device = await Device.findById(req.params.id)
            .populate('registeredBy', 'name email');

        if (!device) {
            return res.status(404).json({
                success: false,
                message: 'Device not found'
            });
        }

        res.status(200).json({
            success: true,
            data: device
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Register new device
// @route   POST /api/devices
// @access  Private (Admin)
exports.registerDevice = async (req, res) => {
    try {
        const deviceData = {
            ...req.body,
            registeredBy: req.user.id
        };

        // Check if device already exists
        const existingDevice = await Device.findOne({ deviceId: req.body.deviceId });
        if (existingDevice) {
            return res.status(400).json({
                success: false,
                message: 'Device with this ID already exists'
            });
        }

        const device = await Device.create(deviceData);

        res.status(201).json({
            success: true,
            message: 'Device registered successfully',
            data: device
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Update device status
// @route   PUT /api/devices/:deviceId/status
// @access  Public (called by device)
exports.updateDeviceStatus = async (req, res) => {
    try {
        const { status, ipAddress } = req.body;

        const device = await Device.findOne({ deviceId: req.params.deviceId });

        if (!device) {
            return res.status(404).json({
                success: false,
                message: 'Device not found'
            });
        }

        device.status = status || device.status;
        device.lastOnline = new Date();
        if (ipAddress) device.ipAddress = ipAddress;

        await device.save();

        res.status(200).json({
            success: true,
            message: 'Device status updated',
            data: device
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Update device
// @route   PUT /api/devices/:id
// @access  Private (Admin)
exports.updateDevice = async (req, res) => {
    try {
        const device = await Device.findById(req.params.id);

        if (!device) {
            return res.status(404).json({
                success: false,
                message: 'Device not found'
            });
        }

        Object.keys(req.body).forEach(key => {
            if (key !== 'deviceId') { // Don't allow changing device ID
                device[key] = req.body[key];
            }
        });

        await device.save();

        res.status(200).json({
            success: true,
            message: 'Device updated successfully',
            data: device
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Delete device
// @route   DELETE /api/devices/:id
// @access  Private (Admin)
exports.deleteDevice = async (req, res) => {
    try {
        const device = await Device.findById(req.params.id);

        if (!device) {
            return res.status(404).json({
                success: false,
                message: 'Device not found'
            });
        }

        await device.deleteOne();

        res.status(200).json({
            success: true,
            message: 'Device deleted successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};
