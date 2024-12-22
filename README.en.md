# <p align="center">Kronos</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-orange">
  <img src="https://img.shields.io/badge/Room%20Database-blue">
  <img src="https://img.shields.io/badge/Notifications-Yes-blue">
  <img src="https://img.shields.io/badge/Privacy-Yes-blue">
  <img src="https://img.shields.io/badge/Safety-Yes-blue">
</p>

This is a simple Todo (tasks manager) application developed in Kotlin that uses the Room Database library to manage tasks. It also includes a notification feature to remind users of tasks on specific dates and times.

# README.md
- ru [Русский](https://github.com/ve3xone/todo-app/blob/main/README.md)
- en [English](https://github.com/ve3xone/todo-app/blob/main/README.en.md)

## Features

- Offline, privacy.
    - That's why there is no synchronization and there will be only maximum export/import.
- Create, read tasks.
- Tasks are stored locally using a database (Room Database).
- Notifications to remind you of upcoming tasks.

## System Requirements

To run the application:
- Starting with Android 8 through Android 14 (Tested)

To build the application:
- Android Studio Iguana (2023.2.1)
    - You can go lower or higher, but I used version 2023.2.1 for development
- Gradle 5.1.1
- Gradle JDK: corretto-1.8.0_402
- Android SDK: Android 14 (API Level 34, Revision 3)

## Future tasks and completed tasks

Main:

- [x] Basic functionality with tasks. (Create tasks with name, description and also receive notifications)
    - [x] Getting to-do list
    - [x] Adding and deleting tasks
    - [x] Editing list items
        - [x] Clickable links within a task (needed for links if any)
            - Displayed if you click on the task itself
    - [x] Ability to set date and time for each task (Reminders per task)
- [x] Data security
    - [x] Password is set by the user at the first login to the application
    - [x] Password is encrypted with encryption algorithms
    - [x] The password is requested each time the user exits and logs in/out of the application.
    - [x] When changing settings, you must also enter the password.
    - [x] Password must be entered after exporting/importing ics.
    - [x] After exporting the database, you must also enter the password.
    - [x] After importing a database, you must enter the password of the imported database.
- [x] Settings (Window with two sections of topic (topic selection) and other)
- [x] Import/Export Database. (settings section other)
- [x] Export to [.ics format](https://en.wikipedia.org/wiki/ICalendar) so that you can add tasks to your calendar. [x] Export to [.ics format]()
- [x] Import from [.ics format](https://en.wikipedia.org/wiki/ICalendar). [x] Import from [.ics format]()
- [x] Search by task. (by name and description)
- [x] Sort tasks by default (by task date and time)
- [x] All and active tasks tabs
    - The "all" tab displays all tasks.
    - The "active" tab displays tasks that are marked as uncompleted.
        - Tasks that are not checked are displayed.
        - [x] Ability to mark tasks as completed or uncompleted.
            - [x] Mark tasks as completed via task notification
    - [x] Swipes between tabs
- [x] English translation
- [x] Make it as [designed](https://raw.githubusercontent.com/ve3xone/kronos-todo-app/main/%D0%B7%D0%B0%D0%BA%D0%BE%D0%BD-%D0%B4%D0%B8%D0%B7%D0%B0%D0%B9%D0%BD/%D0%9F%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D1%83%D1%85%D0%B0.png) ([jam](https://github.com/ve3xone/kronos-todo-app/raw/main/%D0%B7%D0%B0%D0%BA%D0%BE%D0%BD-%D0%B4%D0%B8%D0%B7%D0%B0%D0%B9%D0%BD/%D0%9F%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D1%83%D1%85%D0%B0.jam)).
    - icons and design in general can not get the design because of the jam figma so I'm waiting for icons and that's it
        - As a result, it was decided not to take the designer's design because he made in figma jam....
            - Maximum only icons will take ...

Tasks for the 3rd semester (the most important and easiest are first):

- [x] A bunch of different fixes to make the app work correctly on Android 13 and above.

- [x] Statistics.
    - This functionality should provide in a visual form statistics of using the application - the number of completed and unfinished tasks, the number of created and deleted tasks, performance (the ratio of completed/uncompleted tasks), etc.

- Problematic in terms of UI:
    - [ ] Task Folders.
        - Grouping tasks into user-defined categories - folders.
        - The user should be able to create a “folder” to which he/she can add tasks, when opening a “folder” only tasks added to it should be displayed.

    - [ ] Multiple reminders.
        - It should be possible to set multiple notifications for one task, the latest set notification should be taken as the task deadline.

    - [ ] Subtasks.
        - When creating or editing a task, I need to add a function to add a subtask with an option to mark it as “done” or “not done”.

For the future, I probably won't have time to realize it:
- [ ] Another idea about calendar, but there are also problems in UI too complicated and time-consuming.

- The most problematic also in terms of UI (most likely I won't have time to finish it):
    - [ ] Gradual updating of the application interface - implementation of the original design. In the course of testing the application we found out that the representatives of the target audience liked the original design of the product more, that's why we plan to restore it in the new version.

- Not suitable due to privacy:
    - [ ] Widget. The user should be able to quickly create a task via a widget on the desktop without having to log in to the application.