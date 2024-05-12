
# Кронос (Kronos)

![Kotlin](https://img.shields.io/badge/Kotlin-orange)
![Room Database](https://img.shields.io/badge/Room%20Database-blue)
![Android Notifications](https://img.shields.io/badge/Notifications-Yes-green)

Это простое приложение Todo (tasks manager), разработанное на Kotlin, которое использует библиотеку Room Database для управления задачами. Он также включает в себя функцию уведомления, чтобы напоминать пользователям о задачах в определенные даты и время.

# README.md
- en [English](https://github.com/ve3xone/todo-app/blob/main/README.en.md)
- ru [Русский](https://github.com/ve3xone/todo-app/blob/main/README.md)

## Особенности

- Оффлайн, приватность.
    - Поэтому никаких синхронизаций и нету а будет только максимум экспорт/импорт.
- Создать, читать задачи.
- Задачи храняться локально, используя базу данных (Room Database).
- Уведомления, напоминающие вам о предстоящих задачах.

## Системные требования

Для работы приложения:
- Начиная с Android 8 до Android 14 (Протестировано)

Для сборки приложения:
- Android Studio Iguana (2023.2.1)
    - Можно и ниже и выше, но я для разработки использовал версию 2023.2.1
- Gradle 5.1.1
- Gradle JDK: corretto-1.8.0_402
- Android SDK: Android 14 (API Level 34, Revision 3)

## Проблемы с кэшом

Есть проблема с кэшом у меня покрайне мере остается на Samsung Galaxy Z Fold 4 после удаления приложения и установки обратно появляется бд (в которой есть предыдущие записи)

Но при этом на другом устройстве тоже на Samsung прошивке таких проблем нету.

Проблема решается: после установки приложения, нужно очистить кэш.

## Задачи на будущее

- [x] Базовый функционал с задачами.
- [ ] Сделать как по [дизайну](https://raw.githubusercontent.com/ve3xone/kronos-todo-app/main/%D0%B7%D0%B0%D0%BA%D0%BE%D0%BD-%D0%B4%D0%B8%D0%B7%D0%B0%D0%B9%D0%BD/%D0%9F%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D1%83%D1%85%D0%B0.png) ([jam](https://github.com/ve3xone/kronos-todo-app/raw/main/%D0%B7%D0%B0%D0%BA%D0%BE%D0%BD-%D0%B4%D0%B8%D0%B7%D0%B0%D0%B9%D0%BD/%D0%9F%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D1%83%D1%85%D0%B0.jam)) (50/50).
- [x] Импорт/Экспорт базы данных.
- [x] Экспорт в .ics формат, чтоб можно задачи добавить в календарь.

Если ещё будет время:
- [ ] Поиск по задачам.
- [ ] Папки.
- [ ] Подзадачи. (Скорее всего не успею реализовать)
