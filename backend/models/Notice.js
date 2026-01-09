const mongoose = require('mongoose');

const noticeSchema = new mongoose.Schema({
    title: {
        type: String,
        required: [true, 'Please provide a title'],
        trim: true,
        maxlength: [200, 'Title cannot be more than 200 characters']
    },
    description: {
        type: String,
        required: [true, 'Please provide a description'],
        trim: true
    },
    priority: {
        type: String,
        enum: ['low', 'medium', 'high', 'urgent'],
        default: 'medium'
    },
    category: {
        type: String,
        enum: ['general', 'academic', 'event', 'emergency', 'announcement'],
        default: 'general'
    },
    mediaType: {
        type: String,
        enum: ['text', 'image', 'video', 'mixed'],
        default: 'text'
    },
    mediaUrl: {
        type: String,
        default: null
    },
    scheduledAt: {
        type: Date,
        default: Date.now
    },
    expiresAt: {
        type: Date,
        default: null
    },
    isActive: {
        type: Boolean,
        default: true
    },
    displayDuration: {
        type: Number,
        default: 10,
        min: [5, 'Display duration must be at least 5 seconds'],
        max: [60, 'Display duration cannot exceed 60 seconds']
    },
    targetDevices: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Device'
    }],
    createdBy: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    viewCount: {
        type: Number,
        default: 0
    }
}, {
    timestamps: true
});

// Index for faster queries
noticeSchema.index({ priority: 1, scheduledAt: -1 });
noticeSchema.index({ isActive: 1, expiresAt: 1 });

// Virtual field to check if notice is expired
noticeSchema.virtual('isExpired').get(function() {
    if (!this.expiresAt) return false;
    return new Date() > this.expiresAt;
});

// Method to check if notice should be displayed
noticeSchema.methods.shouldDisplay = function() {
    const now = new Date();
    const isScheduled = this.scheduledAt <= now;
    const notExpired = !this.expiresAt || this.expiresAt > now;
    return this.isActive && isScheduled && notExpired;
};

module.exports = mongoose.model('Notice', noticeSchema);
