const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
// Note: You need to download your service account key from Firebase Console
// and save it as firebase-service-account.json in the backend directory

try {
    const serviceAccount = require('./firebase-service-account.json');
    
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
    
    console.log('✅ Firebase Admin initialized successfully');
} catch (error) {
    console.log('⚠️  Firebase Admin not initialized. Push notifications will not work.');
    console.log('   To enable push notifications:');
    console.log('   1. Download service account key from Firebase Console');
    console.log('   2. Save as firebase-service-account.json in backend directory');
}

// Send push notification to a device
exports.sendNotificationToDevice = async (fcmToken, title, body, data = {}) => {
    if (!admin.apps.length) {
        console.log('Firebase not initialized. Skipping notification.');
        return null;
    }

    const message = {
        notification: {
            title: title,
            body: body
        },
        data: data,
        token: fcmToken
    };

    try {
        const response = await admin.messaging().send(message);
        console.log('✅ Notification sent successfully:', response);
        return response;
    } catch (error) {
        console.error('❌ Error sending notification:', error);
        return null;
    }
};

// Send push notification to multiple devices
exports.sendNotificationToMultiple = async (fcmTokens, title, body, data = {}) => {
    if (!admin.apps.length) {
        console.log('Firebase not initialized. Skipping notification.');
        return null;
    }

    const message = {
        notification: {
            title: title,
            body: body
        },
        data: data,
        tokens: fcmTokens
    };

    try {
        const response = await admin.messaging().sendMulticast(message);
        console.log(`✅ ${response.successCount} notifications sent successfully`);
        if (response.failureCount > 0) {
            console.log(`❌ ${response.failureCount} notifications failed`);
        }
        return response;
    } catch (error) {
        console.error('❌ Error sending notifications:', error);
        return null;
    }
};

// Send notification to a topic
exports.sendNotificationToTopic = async (topic, title, body, data = {}) => {
    if (!admin.apps.length) {
        console.log('Firebase not initialized. Skipping notification.');
        return null;
    }

    const message = {
        notification: {
            title: title,
            body: body
        },
        data: data,
        topic: topic
    };

    try {
        const response = await admin.messaging().send(message);
        console.log('✅ Topic notification sent successfully:', response);
        return response;
    } catch (error) {
        console.error('❌ Error sending topic notification:', error);
        return null;
    }
};

module.exports = {
    sendNotificationToDevice: exports.sendNotificationToDevice,
    sendNotificationToMultiple: exports.sendNotificationToMultiple,
    sendNotificationToTopic: exports.sendNotificationToTopic
};
