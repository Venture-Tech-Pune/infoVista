const express = require('express');
const router = express.Router();
const noticeController = require('../controllers/noticeController');
const { protect, authorize } = require('../middleware/auth');
const upload = require('../middleware/upload');

// Public route for devices to get active notices
router.get('/active', noticeController.getActiveNotices);

// Protected routes
router.use(protect);

// @route   GET /api/notices
// @desc    Get all notices with pagination
// @access  Private
router.get('/', noticeController.getNotices);

// @route   GET /api/notices/:id
// @desc    Get single notice
// @access  Private
router.get('/:id', noticeController.getNotice);

// Admin and Manager only routes
router.use(authorize('admin', 'manager'));

// @route   POST /api/notices
// @desc    Create new notice
// @access  Private (Admin, Manager)
router.post('/', upload.single('media'), noticeController.createNotice);

// @route   PUT /api/notices/:id
// @desc    Update notice
// @access  Private (Admin, Manager)
router.put('/:id', upload.single('media'), noticeController.updateNotice);

// @route   DELETE /api/notices/:id
// @desc    Delete notice
// @access  Private (Admin, Manager)
router.delete('/:id', noticeController.deleteNotice);

module.exports = router;
