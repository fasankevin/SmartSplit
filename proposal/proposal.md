
# School of Computing

**CA326 Year 3 Project Proposal Form**

  

## Project Title: **SmartSplit**

  

### Students:

**Student 1 Name**: Kevin Fasan

**ID Number**: 22379321

  

**Student 2 Name**: Nkemjika Onwumereh

**ID Number**: 22452576

  

### Staff Member Consulted:

Boualem Benatallah

  

##  Project Description (1-2 pages):

  

SmartSplit is an intelligent mobile app designed to streamline bill-splitting among friends. Using machine learning, the app allows users to take a picture of a receipt, assign items to each person, and automatically calculate individual totals, including tax and tip. The app includes payment integration, allowing friends to settle their bills directly within the app. SmartSplit aims to eliminate the hassle of manual calculations and payment logistics, offering a seamless, user-friendly experience for group dining.

  

#### Project Objectives:

  

1.  **Automate Receipt Recognition**: Implement Optical Character Recognition (OCR) to scan and interpret receipt items and prices from images.

2.  **Easy Item Assignment**: Enable users to assign receipt items to specific friends and split shared items.

3.  **Accurate Cost Calculation**: Calculate individual costs, including tax and tip, based on assigned items.

4.  **Integrated Payment Solution**: Incorporate payment functionality to allow users to pay each other directly within the app and use Virtual Cards to pay totals at restaurants.

5.  **Expense History and Summaries**: Track past bills and provide summaries for each group.

6.  **Payment Requests and Reminders**: Send payment requests or reminders through the app, linking directly to payment services.

  

## Programming Tools:

### Frontend (Mobile Application)

  

- Platform: iOS (Swift) or Android (Kotlin), or use a cross-platform framework like React Native or Flutter.

- OCR Integration: Integrate with an OCR service like Google Cloud Vision or Tesseract to scan and interpret receipt images.

- User Interface: Design an intuitive UI for image uploading, item assignment, and cost display.

### Backend

  

- Cloud-Based Backend (e.g., Firebase, AWS, or Google Cloud): Host the backend for real-time data handling, storage, and user authentication.

- Database: Use a database (e.g., Firestore, MySQL, or PostgreSQL) to store user data, bills, item allocations, and payment records.

- API: Develop API to handle requests from the mobile app for processing receipt items, calculating costs, and storing bill records.

  

### Machine Learning (OCR and NLP)

  

- OCR Model: Use a pretrained OCR model, such as Tesseract OCR or Google Cloud Vision, to recognize and extract text from images.

- Natural Language Processing (NLP): Develop a NLP component to interpret receipt items and prices, recognizing common receipt structures and categorizing items.

  

### Payment Integration

  

- Payment API Integration: Use Stripe API to handle transactions and generate Virtual Cards.

- Transaction Tracking: Maintain transaction logs and integrate notifications for successful payments or payment reminders.

- Payment Requests: Allow users to request payment from friends within the app, generating reminders and sending push notifications.

  

## Division of Work:

-  **Kevin Fasan**: Frontend, Payment Integration

-  **Nkemjika Onwumereh**: Backend, Machine Learning

  

## Learning Challenges:

  

-  **OCR and NLP Accuracy**: Ensuring accurate recognition of receipt items with various fonts and layouts.

-  **Implementation of Stripe API**: To handle payments across the app and generation of Virtual Cards.

-  **App Development**: Designing an intuitive application utilizing relevant frameworks (Kotlin / Swift).

  
  

## Hardware / Platform:

PC / Linux

Android

  

**Special hardware/software requirements**:

N/A