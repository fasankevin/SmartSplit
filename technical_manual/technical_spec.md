# Technical Specification for SmartSplit

## 1. Application Overview
- **Application Name:** SmartSplit
- **Purpose:** A mobile application to split bills among group members using OCR (Optical Character Recognition) to extract receipt details and Firebase for data storage and real-time chat.

## 2. Functional Requirements
- **User Authentication:**
  - Users can sign up and log in using Firebase Authentication.
- **Receipt Processing:**
  - Users can capture an image of a receipt using the device camera.
  - OCR (Text Recognition API) extracts itemized details and total amount from the receipt.
  - Extracted itemized details is then further processed by LLM LLama.
- **Bill Splitting:**
  - Users can assign items to group members.
  - Users can edit extracted itemized details in case of errors.
  - The application calculates how much each person owes and displays each user's owed amount.
- **Receipt Storage:**
  - Users can upload the receipt image to Firebase Storage.
  - Users can store the receipt image URL and itemized details in Firebase Firestore.
- **Group Chat:**
  - Users can share receipts and chat with group members in real-time using Firebase Firestore.

## 3. Non-Functional Requirements
- **Performance:**
  - The application should load in under 5 seconds.
  - OCR processing should complete within 15 seconds.
- **Scalability:**
  - The application should support roughly 500 concurrent users (based on a mixed usage of the free tier that Firebase has to offer).
- **Security:**
  - All data transmitted between the app and Firebase should be encrypted.
  - User authentication should use Firebase Authentication with email/password.
- **Compatibility:**
  - The application should support Android 8.0 (API level 26) and above.

## 4. System Architecture
- **Frontend:**
  1. Built using Jetpack Compose for the UI.
  2. Uses CameraX for capturing receipt images.
  3. Uses ML Kit Text Recognition for OCR.
- **Backend:**
  1. Firebase Firestore for real-time database and chat.
  2. Firebase Storage for storing receipt images.
  3. Firebase Authentication for user login and signup.
- **Data Flow:**
  1. User captures a receipt image.
  2. OCR extracts itemized details and total amount.
  3. User assigns items to group members.
  4. Application calculates amounts owed.
  5. Receipt details and image are stored in Firebase.
  6. Receipt is shared in the group chat.

## 5. Technologies Used
- **Programming Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Camera:** CameraX
- **OCR:** ML Kit Text Recognition
- **Database:** Firebase Firestore
- **Storage:** Firebase Storage
- **Authentication:** Firebase Authentication

## 6. APIs and Integrations
- **Firebase APIs:**
  - Firestore (for real-time database and chat).
  - Firebase Storage (for storing receipt images).
  - Firebase Authentication (for user login/signup).
- **ML Kit Text Recognition API (for OCR).**
- **LLM - Llama 3 8b 8192 (for text processing).**

## 7. User Interface (UI) Design
- **Screens:**
  - Login/Register Screen: For user authentication.
  - Receipt Capture Screen: To capture and process receipts.
  - Bill Splitter Screen: To assign items and calculate amounts.
  - Group Chat Screen: To share receipts and chat with group members.

## 8. Testing Strategy
- **Unit Testing:** Test individual components (e.g., OCR logic, calculation logic).
- **UI Testing:** Test UI components using Compose Testing.
- **Integration Testing:** Test integration with Firebase services.
- **Performance Testing:** Test OCR processing time and app responsiveness.
- **Security Testing:** Ensure data is encrypted and secure.

## 9. Deployment
- **Build Tools:** Gradle
- **App Distribution:** Firebase App Distribution for beta testing.
- **Play Store:** Final deployment to Google Play Store.