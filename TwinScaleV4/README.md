# TwinScaleV4

## Firebase setup
1. Создайте проект в Firebase Console.
2. Добавьте Android-приложение с package name `com.twinscalev4`.
3. Скачайте `google-services.json` и поместите его в `app/google-services.json`.
4. Включите Realtime Database (режим Locked, затем настройте правила).
5. Включите Cloud Messaging.
6. Разверните Cloud Functions из папки `cloud-functions/`, чтобы пуши отправлялись партнёру при новых сообщениях.

## Realtime Database structure
```text
rooms/
  roomId/
    users/
    messages/
    state/
users/
  userId/
    name
    size
    mode
    fcmToken
messages/
  messageId/
    senderId
    text
    timestamp
```

## Suggested Realtime Database rules (example)
```json
{
  "rules": {
    "rooms": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "users": {
      "$userId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid === $userId"
      }
    }
  }
}
```

## Cloud Functions deploy
```bash
cd cloud-functions
npm install
firebase deploy --only functions
```

## Manual files
- `app/google-services.json` (обязательно)
