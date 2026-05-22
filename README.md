# WanderAI — AI Travel Planner

A full-stack SaaS app where you describe your trip and AI builds a day-by-day itinerary for you. Built this to get hands-on with Spring AI and payment integration — two things I hadn't touched before in Java.

## Why I built this

I was exploring Java/Spring and wanted a project that went beyond basic CRUD. The real challenge I set for myself was integrating an AI model and a payment gateway together in one project — not just following a tutorial but actually making them work end to end.

## What it does

Users sign up and get 3 free credits. They fill in a destination, budget, duration, and travel preferences — the backend sends that to an AI model which returns a structured day-by-day itinerary with hotel suggestions, restaurants, activities, and travel tips for each day. Each itinerary costs 1 credit. Users can buy more credits via Razorpay.

## Tech Stack

- **Backend** — Spring Boot 3, Spring Security, Spring AI
- **Auth** — JWT with role-based access
- **Database** — PostgreSQL + Hibernate/JPA
- **AI** — Spring AI + Groq (llama-3.3-70b-versatile)
- **Payments** — Razorpay with webhook handling
- **Frontend** — React.js + Chart.js
- **DevOps** — Docker Compose + GitHub Actions CI/CD
- **Testing** — JUnit 5 + Mockito

## Problems I ran into

**Groq model deprecation** — The model I originally configured (`llama3-8b-8192`) got decommissioned mid-development. Had to debug why the AI call was hanging with no error, traced it to the model being invalid, switched to `llama-3.3-70b-versatile`.

**Java version mismatch** — IntelliJ defaulted to Java 25 which broke Spring Boot 3.2 compilation with a `TypeTag::UNKNOWN` error. Fixed by downloading Java 17 and setting the SDK and language level correctly in Project Structure.

**Webhook idempotency** — Razorpay can fire the same webhook multiple times. Added an idempotency check so credits don't get added twice if the webhook fires more than once for the same payment.

**AI response parsing** — The AI sometimes wraps the JSON in markdown code blocks even when told not to. Added cleanup logic to strip the backticks before parsing.

## What I learned

Spring AI makes calling LLMs feel like any other Spring service — you inject a `ChatClient` bean and call it. The harder part is prompt engineering: getting the model to return clean structured JSON consistently took more iteration than I expected.

Razorpay's webhook flow taught me a lot about payment systems in general — you never trust the frontend to confirm a payment, always verify server-side via the webhook signature.

## Running locally

```bash
# Clone the repo
git clone https://github.com/SuhasP2002/wanderai.git
cd wanderai

# Set your environment variables in application.properties
# GROQ_API_KEY, RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, DB password

# Start backend
cd backend
mvn spring-boot:run

# Start frontend
cd frontend
npm install
npm start
```

Or run everything with Docker:
```bash
docker-compose up --build
```

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/auth/register | Register + get JWT |
| POST | /api/auth/login | Login |
| POST | /api/trips/generate | Generate AI itinerary (1 credit) |
| GET | /api/trips | Get all my trips |
| POST | /api/payments/create-order | Create Razorpay order |
| POST | /api/webhooks/razorpay | Handle payment webhook |

## Project structure

```
wanderai/
├── backend/
│   └── src/main/java/com/wanderai/
│       ├── config/        # Security, Razorpay bean, exception handler
│       ├── controller/    # Auth, Trip, Payment REST controllers
│       ├── dto/           # Request/response objects
│       ├── entity/        # User, Trip, Itinerary, Transaction
│       ├── repository/    # Spring Data JPA
│       ├── security/      # JWT filter + util
│       ├── service/       # Business logic + AI + payments
│       └── webhook/       # Razorpay webhook handler
└── frontend/
    └── src/
        ├── pages/         # Login, Dashboard, PlanTrip, TripDetail, Billing
        ├── context/       # Auth state
        └── services/      # Axios API calls
```
