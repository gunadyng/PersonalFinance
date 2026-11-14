import { supabase } from './supabase'

export interface Category {
  id: string
  name: string
  icon: string
  color: string
  is_default: boolean
  sort_order: number
}

export interface Expense {
  id: string
  amount: number
  category_id: string
  note: string | null
  spent_on: string // YYYY-MM-DD
  created_at: string
}

function db() {
  if (!supabase) throw new Error('Supabase is not configured')
  return supabase
}

function friendly(error: { message: string; code?: string }): Error {
  if (error.code === '23505') return new Error('A category with that name already exists.')
  if (error.code === '23503') return new Error('That category still has expenses. Delete or move them first.')
  return new Error(error.message)
}

export async function loadCategories(): Promise<Category[]> {
  const client = db()
  const query = () =>
    client.from('categories').select('id,name,icon,color,is_default,sort_order').order('sort_order').order('name')
  let { data, error } = await query()
  if (error) throw friendly(error)
  if (!data || data.length === 0) {
    // Accounts created before the database was set up get their defaults here.
    const seeded = await client.rpc('ensure_default_categories')
    if (seeded.error) throw friendly(seeded.error)
    ;({ data, error } = await query())
    if (error) throw friendly(error)
  }
  return data ?? []
}

export async function addCategory(input: { name: string; icon: string; color: string }): Promise<Category> {
  const { data, error } = await db()
    .from('categories')
    .insert({ name: input.name.trim(), icon: input.icon || '🏷️', color: input.color })
    .select('id,name,icon,color,is_default,sort_order')
    .single()
  if (error) throw friendly(error)
  return data
}

export async function deleteCategory(id: string): Promise<void> {
  const { error } = await db().from('categories').delete().eq('id', id)
  if (error) throw friendly(error)
}

/** Expenses with spent_on in [from, to). Dates are YYYY-MM-DD. */
export async function loadExpenses(from: string, to: string): Promise<Expense[]> {
  const { data, error } = await db()
    .from('expenses')
    .select('id,amount,category_id,note,spent_on,created_at')
    .gte('spent_on', from)
    .lt('spent_on', to)
    .order('spent_on', { ascending: false })
    .order('created_at', { ascending: false })
  if (error) throw friendly(error)
  return (data ?? []).map((e) => ({ ...e, amount: Number(e.amount) }))
}

export async function addExpense(input: {
  amount: number
  category_id: string
  note: string
  spent_on: string
}): Promise<Expense> {
  const { data, error } = await db()
    .from('expenses')
    .insert({ ...input, note: input.note.trim() || null })
    .select('id,amount,category_id,note,spent_on,created_at')
    .single()
  if (error) throw friendly(error)
  return { ...data, amount: Number(data.amount) }
}

export async function deleteExpense(id: string): Promise<void> {
  const { error } = await db().from('expenses').delete().eq('id', id)
  if (error) throw friendly(error)
}
