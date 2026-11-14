-- Adds optional store and payment details to expenses.
-- Run once in the Supabase SQL editor after 20261114000000_init.sql. Safe to re-run.

alter table public.expenses
  add column if not exists store          text,
  add column if not exists payment_method text,
  add column if not exists card_name      text;

alter table public.expenses drop constraint if exists expenses_store_len;
alter table public.expenses add constraint expenses_store_len
  check (store is null or char_length(store) <= 80);

alter table public.expenses drop constraint if exists expenses_payment_method_check;
alter table public.expenses add constraint expenses_payment_method_check
  check (payment_method is null or payment_method in ('cash', 'card'));

-- A card name (e.g. "Amex Gold") only makes sense for card payments.
alter table public.expenses drop constraint if exists expenses_card_name_check;
alter table public.expenses add constraint expenses_card_name_check
  check (card_name is null or (payment_method = 'card' and char_length(card_name) <= 40));

-- Make PostgREST pick up the new columns straight away.
notify pgrst, 'reload schema';
