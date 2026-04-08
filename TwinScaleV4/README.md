# TwinScaleV4

## Firebase setup
1. Создайте проект в Firebase Console.
2. Добавьте Android-приложение с package name `com.twinscalev4`.
3. Скачайте `google-services.json` и поместите его в `app/google-services.json`.
4. Включите Realtime Database (режим Locked, затем настройте правила).
5. Включите Cloud Messaging.
6. Для отправки push при новых сообщениях используйте серверный компонент (Cloud Functions / ваш backend), который читает `rooms/{roomId}/messages` и отправляет FCM на токен партнёра.

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

## Manual files
- `app/google-services.json` (обязательно)
