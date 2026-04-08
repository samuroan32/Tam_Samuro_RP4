const admin = require("firebase-admin");
const { onValueCreated } = require("firebase-functions/database");

admin.initializeApp();

exports.notifyPartnerOnMessage = onValueCreated(
  {
    ref: "/rooms/{roomId}/messages/{messageId}",
    region: "us-central1"
  },
  async (event) => {
    const roomId = event.params.roomId;
    const message = event.data.val();

    if (!message || !message.senderId || !message.text) {
      return;
    }

    const roomUsersSnap = await admin.database().ref(`/rooms/${roomId}/users`).get();
    if (!roomUsersSnap.exists()) {
      return;
    }

    const users = roomUsersSnap.val() || {};
    const partnerEntry = Object.entries(users).find(([userId]) => userId !== message.senderId);
    if (!partnerEntry) {
      return;
    }

    const [partnerId, partnerData] = partnerEntry;
    const token = partnerData.fcmToken;
    if (!token) {
      return;
    }

    const payload = {
      token,
      data: {
        roomId,
        senderId: String(message.senderId),
        senderName: String(message.senderName || "Партнёр"),
        text: String(message.text)
      },
      android: {
        priority: "high",
        notification: {
          channelId: "twinscale_messages",
          sound: "default"
        }
      }
    };

    try {
      await admin.messaging().send(payload);
    } catch (error) {
      if (error.code === "messaging/registration-token-not-registered") {
        await admin.database().ref(`/rooms/${roomId}/users/${partnerId}/fcmToken`).remove();
      }
      console.error("FCM send failed", error);
    }
  }
);
