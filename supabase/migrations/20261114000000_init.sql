-- Personal Finance Tracker schema.
-- Run this once in the Supabase SQL editor (or with `supabase db push`).
-- Every row belongs to one signed-in user; Row Level Security keeps each
-- user's data private, so the public "anon" key is safe to ship in the apps.

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table if not exists public.categories (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  name        text not null check (char_length(btrim(name)) between 1 and 40),
  icon        text not null default '🏷️',
  color       text not null default '#64748b' check (color ~ '^#[0-9a-fA-F]{6}$'),
  is_default  boolean not null default false,
  sort_order  integer not null default 100,
  created_at  timestamptz not null default now()
);

create unique index if not exists categories_user_name_key
  on public.categories (user_id, lower(btrim(name)));

create table if not exists public.expenses (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null default auth.uid() references auth.users (id) on delete cascade,
  amount       numeric(12, 2) not null check (amount > 0),
  category_id  uuid not null references public.categories (id) on delete restrict,
  note         text check (note is null or char_length(note) <= 200),
  spent_on     date not null default current_date,
  created_at   timestamptz not null default now()
);

create index if not exists expenses_user_spent_on_idx
  on public.expenses (user_id, spent_on desc, created_at desc);
create index if not exists expenses_category_idx
  on public.expenses (category_id);

-- An expense may only point at a category owned by the same user.
create or replace function public.expenses_check_category_owner()
returns trigger
language plpgsql
as $$
begin
  if not exists (
    select 1 from public.categories c
    where c.id = new.category_id and c.user_id = new.user_id
  ) then
    raise exception 'Category does not belong to this user';
  end if;
  return new;
end;
$$;

drop trigger if exists expenses_category_owner on public.expenses;
create trigger expenses_category_owner
  before insert or update of category_id, user_id on public.expenses
  for each row execute function public.expenses_check_category_owner();

-- ---------------------------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------------------------

alter table public.categories enable row level security;
alter table public.expenses   enable row level security;

drop policy if exists "own categories" on public.categories;
create policy "own categories" on public.categories
  for all to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

drop policy if exists "own expenses" on public.expenses;
create policy "own expenses" on public.expenses
  for all to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- Default categories
-- ---------------------------------------------------------------------------

create or replace function public.insert_default_categories(target_user uuid)
returns void
language sql
security definer
set search_path = public
as $$
  insert into public.categories (user_id, name, icon, color, is_default, sort_order)
  values
    (target_user, 'Food & Dining',     '🍔', '#f97316', true, 10),
    (target_user, 'Groceries',         '🛒', '#22c55e', true, 20),
    (target_user, 'Transport',         '🚗', '#3b82f6', true, 30),
    (target_user, 'Bills & Utilities', '💡', '#eab308', true, 40),
    (target_user, 'Housing',           '🏠', '#8b5cf6', true, 50),
    (target_user, 'Shopping',          '🛍️', '#ec4899', true, 60),
    (target_user, 'Entertainment',     '🎬', '#a855f7', true, 70),
    (target_user, 'Health',            '💊', '#ef4444', true, 80),
    (target_user, 'Travel',            '✈️', '#06b6d4', true, 90),
    (target_user, 'Education',         '📚', '#0ea5e9', true, 100),
    (target_user, 'Personal Care',     '💇', '#d946ef', true, 110),
    (target_user, 'Gifts & Donations', '🎁', '#f43f5e', true, 120),
    (target_user, 'Subscriptions',     '🔁', '#6366f1', true, 130),
    (target_user, 'Other',             '📦', '#64748b', true, 999)
  on conflict do nothing;
$$;

-- Seed defaults automatically for every new account.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.insert_default_categories(new.id);
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Callable by the apps: seeds defaults for the signed-in user if they have no
-- categories yet (covers accounts created before this migration ran).
create or replace function public.ensure_default_categories()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Not signed in';
  end if;
  if not exists (select 1 from public.categories where user_id = auth.uid()) then
    perform public.insert_default_categories(auth.uid());
  end if;
end;
$$;

revoke all on function public.insert_default_categories(uuid) from public, anon, authenticated;
revoke all on function public.ensure_default_categories() from public, anon;
grant execute on function public.ensure_default_categories() to authenticated;

grant select, insert, update, delete on public.categories to authenticated;
grant select, insert, update, delete on public.expenses   to authenticated;
