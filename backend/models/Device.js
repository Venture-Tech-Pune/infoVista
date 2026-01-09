const mongoose = require('mongoose');

const deviceSchema = new mongoose.Schema({
    deviceId: {
        type: String,
        required: [true, 'Please provide a device ID'],
        unique: true,
        trim: true
    },
    name: {
        type: String,
        required: [true, 'Please provide a device name'],
        trim: true
    },
    location: {
        type: String,
        required: [true, 'Please provide device location'],
        trim: true
    },
    status: {
        type: String,
        enum: ['online', 'offline', 'error'],
        default: 'offline'
    },
    lastOnline: {
        type: Date,
        default: Date.now
    },
    ipAddress: {
        type: String,
        default: null
    },
    displayType: {
        type: String,
        enum: ['lcd', 'led', 'tft', 'oled'],
        default: 'lcd'
    },
    resolution: {
        width: {
            type: Number,
            default: 1920
        },
        height: {
            type: Number,
            default: 1080
        }
    },
    isActive: {
        type: Boolean,
        default: true
    },
    registeredBy: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    }
}, {
    timestamps: true
});

// Update lastOnline on status change
deviceSchema.pre('save', function(next) {
    if (this.isModified('status') && this.status === 'online') {
        this.lastOnline = new Date();
    }
    next();
});

// Method to update device status
deviceSchema.methods.updateStatus = async function(status) {
    this.status = status;
    if (status === 'online') {
        this.lastOnline = new Date();
    }
    return await this.save();
};

module.exports = mongoose.model('Device', deviceSchema);
