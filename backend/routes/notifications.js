const express = require('express');
const router = express.Router();
const { protect } = require('../middleware/auth');
const User = require('../models/User');

// @route   POST /api/notifications/register-token
// @desc    Register or update FCM token for user
// @access  Private
router.post('/register-token', protect, async (req, res) => {
    try {
        const { fcmToken } = req.body;

        if (!fcmToken) {
            return res.status(400).json({
                success: false,
                message: 'FCM token is required'
            });
        }

        // Update user's FCM token
        const user = await User.findById(req.user.id);
        user.fcmToken = fcmToken;
        await user.save();

        res.status(200).json({
            success: true,
            message: 'FCM token registered successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// @route   DELETE /api/notifications/unregister-token
// @desc    Remove FCM token (logout)
// @access  Private
router.delete('/unregister-token', protect, async (req, res) => {
    try {
        const user = await User.findById(req.user.id);
        user.fcmToken = null;
        await user.save();

        res.status(200).json({
            success: true,
            message: 'FCM token removed successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

module.exports = router;
