# PersonalFinance

A personal expense tracker with a native **Android app** and a **web app** that share the same data.
Enter a dollar amount, pick a category, done. It comes with 14 everyday categories and you can add your own.

| Part | Folder | Tech |
| --- | --- | --- |
| Shared backend | `supabase/` | [Supabase](https://supabase.com): Postgres, email sign-in, Row Level Security |
| Web app | `web/` | React + TypeScript + Vite, hosted on GitHub Pages |
| Android app | `android/` | Kotlin + Jetpack Compose (Material 3), Android 8.0+ |

Both apps sign in to the same Supabase account, so anything you add on your phone shows up in the browser and vice versa
(the apps refresh when you switch back to them).

## Features

- Add an expense: amount, category, and optionally the store/shop, how you paid (cash or card, plus the card's name), a note and the date (defaults to today).
- Default categories: Food & Dining, Groceries, Transport, Bills & Utilities, Housing, Shopping, Entertainment,
  Health, Travel, Education, Personal Care, Gifts & Donations, Subscriptions, Other.
- Add your own categories with an icon and colour; delete ones you don't use.
- Monthly total with a per-category breakdown, history grouped by day, browse previous months.
- Delete expenses.
- Light and dark mode.

## One-time setup

### 1. Database (Supabase)

1. In your Supabase project open **SQL Editor → New query**, paste the contents of
   [`supabase/migrations/20261114000000_init.sql`](supabase/migrations/20261114000000_init.sql) and click **Run**.
   It creates the `categories` and `expenses` tables, privacy rules so each account only sees its own rows,
   and seeds the default categories for every new account.
   Then do the same with each later file in that folder, in name order
   (e.g. [`20261114010000_store_and_payment.sql`](supabase/migrations/20261114010000_store_and_payment.sql)).
2. Optional: under **Authentication → Sign In / Providers → Email**, turn off **Confirm email** if you'd rather not
   click a confirmation link when creating your account.

### 2. Project settings

The Supabase **Project URL** and **anon public key** live in two committed files:

- `web/.env.production` → `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`
- `android/gradle.properties` → `SUPABASE_URL`, `SUPABASE_ANON_KEY`

The anon key is designed to be public; the database's Row Level Security keeps your data private.
**Never** commit the `service_role` / secret key.

### 3. Web app on GitHub Pages

In the GitHub repo go to **Settings → Pages** and set **Source** to **GitHub Actions**.
Every push to `main` then builds and publishes the site at `https://<your-user>.github.io/PersonalFinance/`.
In Supabase, add that address under **Authentication → URL Configuration → Site URL** so confirmation emails link back to it.

### 4. Android app

Every push builds an APK in GitHub Actions (**Actions → Android APK → latest run → Artifacts → expense-tracker-apk**).
Download it on your phone, unzip, and open `app-debug.apk` (allow "Install unknown apps" for your browser/files app when prompted).

## Local development

```bash
# Web
cd web
cp .env.example .env.local   # optional: point at a different Supabase project
npm install
npm run dev

# Android (needs Android Studio or the Android SDK)
cd android
./gradlew installDebug
```
