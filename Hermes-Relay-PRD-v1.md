# Hermes-Relay PRD (v1)

## Overview

Hermes-Relay is a self-hosted SMS relay that turns an Android phone into
an SMS gateway.

Applications send SMS requests to the Hermes Relay Server. The server
forwards the request to a connected Android app over a persistent
WebSocket connection. The Android app sends the SMS using the phone's
SIM card and reports the result back.

This document covers the initial MVP only.

------------------------------------------------------------------------

# Goals

-   Send SMS from any backend using a simple HTTP API.
-   Keep the Android app connected to the relay server.
-   Authenticate users and devices.
-   Support basic SMS delivery status.
-   Keep the architecture simple and easy to extend.

------------------------------------------------------------------------

# Out of Scope (v1)

-   Multiple devices per account
-   SMS scheduling
-   Delivery analytics
-   Rate limiting
-   SDKs
-   Webhooks
-   iOS support

------------------------------------------------------------------------

# Architecture

``` text
Application
      |
      | HTTP
      v
Hermes Relay Server
      |
      | WebSocket
      v
Android Agent
      |
      v
SIM Card
      |
      v
Carrier
```

------------------------------------------------------------------------

# Components

## Relay Server

Responsibilities:

-   User authentication
-   API key validation
-   WebSocket management
-   SMS job creation
-   Forward SMS requests
-   Store SMS history

Tech Stack

-   FastAPI
-   PostgreSQL (Supabase)
-   Vanilla HTML/CSS/JS (simple dashboard)

------------------------------------------------------------------------

## Android Agent

Responsibilities:

-   Login
-   Connect to relay server
-   Maintain WebSocket connection
-   Receive SMS jobs
-   Send SMS
-   Return success/failure

Tech Stack

-   Kotlin
-   Jetpack Compose
-   Foreground Service
-   OkHttp WebSocket

------------------------------------------------------------------------

# User Flow

1.  User creates an account.
2.  User generates an API key.
3.  User logs into the Android app.
4.  App connects to the relay server.
5.  App waits for commands.
6.  External application calls the API.
7.  Relay forwards the SMS job.
8.  Android sends the SMS.
9.  Relay stores the result.

------------------------------------------------------------------------

# Basic API

## Send SMS

POST /api/v1/messages

Headers

Authorization: Bearer `<API_KEY>`{=html}

Body

``` json
{
  "to": "+919876543210",
  "message": "Hello from Hermes!"
}
```

Response

``` json
{
  "jobId": "job_123",
  "status": "accepted"
}
```

------------------------------------------------------------------------

# WebSocket Messages

Authentication

``` json
{
  "type": "auth",
  "token": "<access_token>"
}
```

Send SMS

``` json
{
  "type": "send_sms",
  "jobId": "job_123",
  "to": "+919876543210",
  "message": "Hello!"
}
```

SMS Result

``` json
{
  "type": "sms_result",
  "jobId": "job_123",
  "status": "sent"
}
```

------------------------------------------------------------------------

# Database (MVP)

Users

-   id
-   email

API Keys

-   id
-   user_id
-   key_hash

Devices

-   id
-   user_id
-   device_name
-   status
-   last_seen

SMS Jobs

-   id
-   user_id
-   device_id
-   phone_number
-   message
-   status
-   created_at

------------------------------------------------------------------------

# Repository Structure

``` text
hermes-relay/
|
|-- relay-server/
|-- mobile-app/
|-- docs/
|-- README.md
```

------------------------------------------------------------------------

# Milestones

## Phase 1

-   Relay server setup
-   User authentication
-   API key generation
-   Android login

## Phase 2

-   Persistent WebSocket
-   Device registration
-   SMS sending
-   Delivery acknowledgement

## Phase 3

-   Basic dashboard
-   SMS history
-   Device status

------------------------------------------------------------------------

# Success Criteria

-   Android app stays connected.
-   API can trigger an SMS.
-   SMS reaches recipient.
-   Delivery result is stored.
-   Phone reconnects automatically after connection loss.
