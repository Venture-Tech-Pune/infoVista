const Notice = require('../models/Notice');
const { validationResult } = require('express-validator');

// @desc    Get all notices
// @route   GET /api/notices
// @access  Private
exports.getNotices = async (req, res) => {
    try {
        const { page = 1, limit = 10, priority, category, isActive } = req.query;

        // Build filter
        const filter = {};
        if (priority) filter.priority = priority;
        if (category) filter.category = category;
        if (isActive !== undefined) filter.isActive = isActive === 'true';

        const notices = await Notice.find(filter)
            .populate('createdBy', 'name email')
            .populate('targetDevices', 'name location')
            .sort({ priority: -1, scheduledAt: -1 })
            .limit(limit * 1)
            .skip((page - 1) * limit);

        const count = await Notice.countDocuments(filter);

        res.status(200).json({
            success: true,
            data: notices,
            pagination: {
                currentPage: parseInt(page),
                totalPages: Math.ceil(count / limit),
                totalItems: count
            }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Get active notices for display
// @route   GET /api/notices/active
// @access  Public
exports.getActiveNotices = async (req, res) => {
    try {
        const { deviceId } = req.query;
        const now = new Date();

        const filter = {
            isActive: true,
            scheduledAt: { $lte: new Date(now.getTime() + 60000) } // 1 minute grace for clock skew
        };

        // Combine expiry and device filters using $and to avoid overwriting $or
        filter.$and = [
            {
                $or: [
                    { expiresAt: null },
                    { expiresAt: { $gt: now } }
                ]
            }
        ];

        // If deviceId is provided, filter by target devices
        if (deviceId) {
            const Device = require('../models/Device');
            const device = await Device.findOne({ deviceId });
            if (device) {
                filter.$and.push({
                    $or: [
                        { targetDevices: { $size: 0 } },
                        { targetDevices: device._id }
                    ]
                });
                console.log(`Filtering for device: ${deviceId} (${device._id})`);
            } else {
                console.log(`Device ID ${deviceId} not found in database. Showing all public notices.`);
            }
        }

        console.log('Active Notice Filter:', JSON.stringify(filter, null, 2));
        console.log('Current Time (Server):', now.toISOString());

        const notices = await Notice.find(filter)
            .populate('createdBy', 'name')
            .sort({ priority: -1, scheduledAt: -1 });

        console.log(`Found ${notices.length} matching notices.`);

        res.status(200).json({
            success: true,
            data: notices
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Get single notice
// @route   GET /api/notices/:id
// @access  Private
exports.getNotice = async (req, res) => {
    try {
        const notice = await Notice.findById(req.params.id)
            .populate('createdBy', 'name email')
            .populate('targetDevices', 'name location');

        if (!notice) {
            return res.status(404).json({
                success: false,
                message: 'Notice not found'
            });
        }

        res.status(200).json({
            success: true,
            data: notice
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Create new notice
// @route   POST /api/notices
// @access  Private (Admin, Manager)
exports.createNotice = async (req, res) => {
    try {
        const noticeData = {
            ...req.body,
            createdBy: req.user.id
        };

        // If file uploaded, add media URL
        if (req.file) {
            noticeData.mediaUrl = `/uploads/${req.file.filename}`;
            noticeData.mediaType = req.file.mimetype.startsWith('image') ? 'image' : 'video';
        }

        const notice = await Notice.create(noticeData);

        // Emit socket event for real-time update
        if (req.app.get('io')) {
            req.app.get('io').emit('notice:created', notice);
        }

        // Send push notification to all users
        try {
            const User = require('../models/User');
            const { sendNotificationToMultiple } = require('../config/firebase');
            
            // Get all active users with FCM tokens
            const users = await User.find({ 
                isActive: true, 
                fcmToken: { $exists: true, $ne: null } 
            });
            
            if (users.length > 0) {
                const fcmTokens = users.map(user => user.fcmToken);
                await sendNotificationToMultiple(
                    fcmTokens,
                    `New ${notice.priority.toUpperCase()} Notice`,
                    notice.title,
                    {
                        noticeId: notice._id.toString(),
                        priority: notice.priority,
                        category: notice.category
                    }
                );
            }
        } catch (notifError) {
            console.error('Push notification error:', notifError);
            // Don't fail the request if notification fails
        }

        res.status(201).json({
            success: true,
            message: 'Notice created successfully',
            data: notice
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Update notice
// @route   PUT /api/notices/:id
// @access  Private (Admin, Manager)
exports.updateNotice = async (req, res) => {
    try {
        let notice = await Notice.findById(req.params.id);

        if (!notice) {
            return res.status(404).json({
                success: false,
                message: 'Notice not found'
            });
        }

        // Check authorization
        if (notice.createdBy.toString() !== req.user.id && req.user.role !== 'admin') {
            return res.status(403).json({
                success: false,
                message: 'Not authorized to update this notice'
            });
        }

        // Update fields
        Object.keys(req.body).forEach(key => {
            notice[key] = req.body[key];
        });

        // If new file uploaded
        if (req.file) {
            notice.mediaUrl = `/uploads/${req.file.filename}`;
            notice.mediaType = req.file.mimetype.startsWith('image') ? 'image' : 'video';
        }

        await notice.save();

        // Emit socket event
        if (req.app.get('io')) {
            req.app.get('io').emit('notice:updated', notice);
        }

        res.status(200).json({
            success: true,
            message: 'Notice updated successfully',
            data: notice
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// @desc    Delete notice
// @route   DELETE /api/notices/:id
// @access  Private (Admin, Manager)
exports.deleteNotice = async (req, res) => {
    try {
        const notice = await Notice.findById(req.params.id);

        if (!notice) {
            return res.status(404).json({
                success: false,
                message: 'Notice not found'
            });
        }

        // Check authorization - admins can delete any notice
        const isAdmin = req.user.role === 'admin';
        const isOwner = notice.createdBy.toString() === req.user.id.toString();
        
        if (!isAdmin && !isOwner) {
            return res.status(403).json({
                success: false,
                message: 'Not authorized to delete this notice'
            });
        }

        // Delete the notice
        await Notice.findByIdAndDelete(req.params.id);

        // Emit socket event for real-time update
        if (req.app.get('io')) {
            req.app.get('io').emit('notice:deleted', { id: req.params.id });
        }

        res.status(200).json({
            success: true,
            message: 'Notice deleted successfully'
        });
    } catch (error) {
        console.error('Delete notice error:', error);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};
