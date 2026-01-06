# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A serverless health checker Lambda function written in ClojureScript that monitors URLs and sends alerts via AWS SNS (SMS) or SES (email) when health checks fail.

## Tech Stack

- **ClojureScript** compiled with **shadow-cljs** targeting Node.js
- **AWS SAM** for Lambda deployment
- **AWS SNS** for SMS notifications
- **AWS SES** for email notifications

## Build & Deploy Commands

```shell
# Install dependencies
npm install

# Build ClojureScript to JavaScript (outputs to target/main.js)
npx shadow-cljs release :lib

# Deploy to AWS (builds and deploys)
npm run deploy

# Start shadow-cljs REPL for development
npx shadow-cljs node-repl
```

## Architecture

**Single source file:** `src/healthcheck/core.cljs`

The Lambda handler flow:
1. `main` - Entry point, reads env vars, builds context map
2. `check-urls` - Fetches all URLs in parallel using native `fetch`
3. `maybe-notify` - Checks if any URL returned `:nok` status
4. `notify` - Routes to `send-email` or `send-sms` based on `DELIVERY_METHOD`

**shadow-cljs config:** `shadow-cljs.edn` - Builds `:lib` target as Node library, exports `healthcheck.core/main` as `check`

**SAM config:** `template.yaml` - Lambda runs on 15-min cron schedule

## Environment Variables

Source `.env.sh` before deploying, or configure in `template.yaml`:
- `HEALTHCHECK_URLS` - Comma-separated URLs to check
- `DELIVERY_METHOD` - `email` or `sms`
- `EMAIL_RECIPIENT`, `EMAIL_SENDER` - For email alerts
- `SMS_PHONE_NUMBER`, `SMS_SENDER_ID` - For SMS alerts
- `AWS_REGION` - AWS region for deployment
