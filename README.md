# QuickRide - Advanced Ride-Sharing Application

Welcome to the **QuickRide** project documentation. This document serves as a comprehensive "0 to 100" guide for students and developers to understand the flow, architecture, and advanced features of the QuickRide platform.

## 📱 App Flow (How it Works)

1. **Onboarding**: Users sign up and choose a role (**Rider** or **Driver**). Verification is enforced via email.
2. **Going Online (Driver)**: Drivers toggle a switch to go online. This starts a **Foreground Service** that keeps them visible to riders even if they switch apps or lock their phone.
3. **Booking a Ride (Rider)**: Riders select a pickup and destination. They can choose between different vehicle types (Economy, Bike, Premium).
4. **Real-time Matching**: The app searches for the nearest drivers using **GeoFire**.
5. **Background Alerts**: When a rider requests a ride, all nearby drivers receive a **Voice Notification** (*"New ride request received"*) even if their app is minimized.
6. **Ride Handshake**: Once a driver accepts, both parties are connected. The driver navigates to the pickup, and the rider sees the driver's live location.
7. **Voice Updates**: The app speaks to both parties during key moments (Arrival, Completion) for a premium experience.
8. **Completion**: Fare is calculated based on distance, and the ride is saved to history.

---

## 🌟 Core Features & Modules

### 1. Real-Time Background System (New!)
*   **Foreground Service**: Located in `services/DriverForegroundService.java`. This keeps the driver's GPS active in the background. Without this, Android would kill the app, and the driver would go "Offline" unexpectedly.
*   **OneSignal Voice Broadcasting**: Located in `utils/MyNotificationServiceExtension.java`. It uses Push Notifications to wake up devices and play voice alerts (*Text-to-Speech*) even when the app is minimized.

### 2. Smart Matchmaking
*   **Incremental Search Algorithm**: Found in `CustomerMapActivity.java`. It searches in circles (2km → 5km → 10km) to find the absolute closest driver, saving battery and server costs.
*   **Trusted Driver Mode**: A toggle in the Customer app that allows riders to only see drivers they have marked as "Trusted". *Note: Keep this OFF for manual/public rides.*

### 3. Carpooling (Shared Rides)
*   **Fixed Routes**: Found in `driver/CreateFixedRouteActivity.java`. Drivers can post a fixed route with available seats.
*   **Seat Management**: In `customer/FixedRoutesSearchActivity.java`, riders can join seats. Once all seats are booked, the route is automatically hidden from others.

### 4. Voice Feedback System
*   **VoiceHelper**: A centralized utility in `utils/VoiceHelper.java` that converts text to speech.
*   **Arrival Alert**: *"Your driver is waiting outside"* (plays on Rider's phone).
*   **Completion Alert**: *"Ride completed successfully"* (plays on both phones).

---

## 📂 Project Structure Guide

*   **`auth/`**: Registration, Login, and Role Selection.
*   **`customer/`**: Main map for riders, search logic, and settings.
*   **`driver/`**: Main map for drivers, request management, and foreground service control.
*   **`utils/`**: The "brain" of the app.
    *   `NotificationHelper`: Sends push notifications via REST API.
    *   `VoiceHelper`: Handles all voice/audio feedback.
    *   `RouteHelper`: Draws polylines on the map using Google Directions API.
*   **`services/`**: Contains `DriverForegroundService` for background persistence.

---

## 🛠 Setup Requirements

1.  **Firebase**: Connect your project to Firebase and download `google-services.json`.
2.  **Google Maps**: Enable Maps SDK, Places API, and Directions API in Google Cloud Console.
3.  **OneSignal**: Create an account at OneSignal.com and add your `app_id` and `rest_api_key` in `res/values/strings.xml`.
4.  **Device**: Must have **Google Play Services** installed for Maps and Notifications to function.

---
*Developed for a seamless, hands-free ride-sharing experience.*
