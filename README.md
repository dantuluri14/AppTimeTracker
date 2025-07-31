
# App Time Tracker - README

This is an Android application designed to help users monitor and manage the time they spend on other applications on their device. It provides usage statistics, allows users to set time limits, and alerts the user in real-time when those limits are exceeded.

---
## Functionalities

* **App Usage Tracking**: Monitors the total foreground time of all applications on the device over the last 24 hours.
* **Statistics Visualization**:
    * Displays a **histogram (bar chart)** showing the top 3 most used apps for a quick overview.
    * Shows a detailed **sortable table** listing all used apps, their icons, and their total usage time.
    * The table can be sorted by **App Name** (alphabetically A-Z or Z-A) or **Usage Time** (descending or ascending).
* **Time Limit Configuration**:
    * Users can set a **global default time limit** that applies to all apps without a specific rule. The initial hardcoded default is 60 minutes.
    * Users can set a **specific time limit** for any individual app, which will override the global default.
* **Background Monitoring & Alerting**:
    * A persistent **background service** runs to monitor the currently open app in real-time.
    * If an app's usage time for the day exceeds its configured limit, an **overlay alert** is displayed on top of the running app with details about the limit and current usage.
* **User Experience**:
    * A robust, multi-page structure that separates permission handling from the main functionality.
    * A **pull-to-refresh** gesture on the main statistics screen to manually update usage data.
    * A dedicated permissions checklist screen to guide the user through the initial setup, ensuring a smooth onboarding process.

---
## Code Structure

The app is architected with separate components for the UI, background logic, and data storage to ensure maintainability and stability.

* **Activities (`/java/com/example/apptimetracker`)**:
    * `SplashActivity.java`: The app's entry point. It has no UI and its only job is to check for permissions and redirect the user to the appropriate screen (`PermissionsActivity` or `MainActivity`).
    * `PermissionsActivity.java`: The screen that displays the checklist of required permissions. It handles the logic for requesting permissions and checking their status before allowing the user to proceed.
    * `MainActivity.java`: The main "home page" of the app. It displays the usage statistics chart and table and allows the user to configure time limits.
* **Background Service (`/java/com/example/apptimetracker`)**:
    * `TrackingService.java`: A foreground service that runs continuously in the background to monitor the active app and trigger alerts.
* **Database (Room) (`/java/com/example/apptimetracker`)**:
    * `AppDatabase.java`: The main Room database class that provides a centralized access point to the app's data.
    * `AppLimit.java`: The Entity class that defines the structure of the `app_limits` table in the database (columns: `package_name`, `time_limit_millis`).
    * `AppLimitDao.java`: The Data Access Object (DAO) interface that defines how to interact with the `app_limits` table (e.g., insert, update, query).
* **UI Components (`/java/com/example/apptimetracker`)**:
    * `AppUsageAdapter.java`: The `RecyclerView.Adapter` that populates the main table with app usage data.
    * `AppUsageInfo.java`: A model class to hold the data for a single app shown in the table (icon, name, usage time, package name).
* **Helpers (`/java/com/example/apptimetracker`)**:
    * `PermissionHelper.java`: Contains static methods to check for the special "Usage Stats" and "Display Over Other Apps" permissions.
    * `SettingsHelper.java`: Manages the global default time limit using `SharedPreferences`.
* **Layouts (`/res/layout`)**:
    * `activity_main.xml`: The layout for the main statistics screen, containing the chart, switch, buttons, and table.
    * `activity_permissions.xml`: The layout for the permissions checklist screen.
    * `list_item_app_usage.xml`: The layout for a single row in the statistics table.
    * `alert_view.xml`: The layout for the overlay alert popup.
* **Configuration**:
    * `AndroidManifest.xml`: Declares all activities, services, and necessary permissions for the app to function correctly.
    * `build.gradle.kts`: Manages all project dependencies, including Room for the database and MPAndroidChart for the histogram.

---
## Class and Method Details

### `MainActivity.java`

This activity is the main dashboard of the app, displayed after all permissions are granted.

* **`onCreate()`**:
    * Initializes all UI components (Toolbar, Chart, RecyclerView, Buttons, Switch).
    * Sets up the `RecyclerView` with its adapter and layout manager.
    * Sets the `onClickListener` for all interactive elements:
        * **Sortable Headers**: Calls `sortData()` to re-sort the table.
        * **Set Default Limit Button**: Calls `showSetLimitDialog()` to open the configuration popup.
        * **Tracking Switch**: Starts or stops the `TrackingService`.
    * Sets up the `OnRefreshListener` for the `SwipeRefreshLayout` to enable pull-to-refresh.
* **`onResume()`**: Calls `loadUsageStatistics()` to ensure the data is fresh whenever the user returns to the app.
* **`loadUsageStatistics()`**:
    * This is the core data-loading method.
    * It queries the Android `UsageStatsManager` to get app usage data for the last 24 hours.
    * It processes this data to populate both the **chart** (with the top 3 apps) and the main **table** (with all apps).
    * It handles the "empty state" UI, showing a message if no usage data is found.
    * It manages the pull-to-refresh spinner, showing it while loading and hiding it when done.
* **`onItemClick(AppUsageInfo item)`**:
    * This method is called from the `AppUsageAdapter` when a user taps on an app in the table. It calls `showSetLimitDialog()` for that specific app.
* **`showSetLimitDialog(AppUsageInfo appInfo)`**:
    * Builds and displays an `AlertDialog` with an input field for setting a time limit in minutes.
    * If `appInfo` is `null`, it sets the global default limit and saves it using `SettingsHelper`.
    * If `appInfo` is provided, it saves a specific limit for that app to the Room database on a background thread.
* **`sortData()` / `applySort()` / `updateSortHeaders()`**:
    * These methods manage the sorting logic for the table. They update the current sort order, use a `Comparator` to sort the list of `AppUsageInfo` objects, and update the arrow icons in the headers to reflect the current sort.
* **`onCreateOptionsMenu()` / `onOptionsItemSelected()`**:
    * Inflates the menu in the toolbar and handles clicks on the "Check Permissions" item, which navigates the user back to the `PermissionsActivity`.

### `PermissionsActivity.java`

This activity is shown to the user if any required permissions are missing.

* **`onCreate()`**: Initializes the buttons, image views, and listeners for the permission checklist.
* **`onResume()`**: Calls `updatePermissionItemsUI()` to ensure the checklist is always in the correct state after the user returns from a settings page.
* **`checkAllPermissionsAndProceed()`**: Called when the "Continue" button is clicked. It verifies if all permissions are now granted and, if so, navigates to `MainActivity`.
* **`updatePermissionItemsUI()`**: Checks the status of each of the three required permissions and updates the UI accordingly, showing either a "Grant" button or a success checkmark.

### `SplashActivity.java`

This is an invisible activity that acts as the app's entry point.

* **`onCreate()`**:
    * Checks if all three required permissions are granted.
    * Redirects the user to `MainActivity` if they are all granted.
    * Redirects the user to `PermissionsActivity` if any are missing.
    * Calls `finish()` on itself so it's removed from the back stack and the user cannot navigate back to it.

### `TrackingService.java`

This service is the background workhorse of the app.

* **`CHECK_INTERVAL`**: A constant that defines the **frequency** at which the background service checks for the foreground app. It is currently set to **10 seconds** (`10000` milliseconds) as a balance between responsiveness and battery life.
* **`onCreate()`**: Initializes the database, the `WindowManager`, and populates a list of all launcher apps on the device to be ignored.
* **`onStartCommand()`**: Starts the service in the foreground (which requires a persistent notification) and begins the periodic checks using a `Handler`.
* **`checkForegroundApp()`**:
    * This method is run by the `Handler` every `CHECK_INTERVAL`.
    * It calls `getForegroundApp()` to find out what app is currently open.
    * It then runs a background task to get the total usage for that app and its configured time limit (from the database or the default).
    * If `usage > limit`, it calls `showAlert()`.
* **`showAlert()`**:
    * Uses the `WindowManager` to add a custom layout (`alert_view.xml`) as an overlay on top of all other apps.
    * It populates the alert with the app's name, its total usage, and the configured limit.
    * Sets up the "Dismiss" button to remove the alert.
* **`getForegroundApp()`**:
    * This is the real-time detection logic. It uses the `ActivityManager.getRunningAppProcesses()` method, which is the most reliable way to find the process that currently has `IMPORTANCE_FOREGROUND`.
    * It filters out our own app and any identified launcher apps to prevent false positives.
* **`getUsageForPackage()`**: A helper method that queries `UsageStatsManager` for the total usage of a single app for the current day.
