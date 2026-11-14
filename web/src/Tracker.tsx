import { useCallback, useEffect, useMemo, useState } from 'react'
import { deleteExpense, loadCategories, loadExpenses, type Category, type Expense } from './api'
import { dayLabel, money, monthLabel, monthRange } from './format'
import { supabase } from './supabase'
import AddExpense from './AddExpense'
import CategoryManager from './CategoryManager'

export default function Tracker({ email }: { email: string }) {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth())
  const [categories, setCategories] = useState<Category[]>([])
  const [expenses, setExpenses] = useState<Expense[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [managing, setManaging] = useState(false)

  const [from, to] = monthRange(year, month)
  const isCurrentMonth = year === now.getFullYear() && month === now.getMonth()

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [cats, exps] = await Promise.all([loadCategories(), loadExpenses(from, to)])
      setCategories(cats)
      setExpenses(exps)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }, [from, to])

  useEffect(() => {
    refresh()
  }, [refresh])

  // Pick up expenses added on the phone when the tab regains focus.
  useEffect(() => {
    const onFocus = () => document.visibilityState === 'visible' && refresh()
    document.addEventListener('visibilitychange', onFocus)
    return () => document.removeEventListener('visibilitychange', onFocus)
  }, [refresh])

  const byId = useMemo(() => new Map(categories.map((c) => [c.id, c])), [categories])
  const total = expenses.reduce((s, e) => s + e.amount, 0)

  const breakdown = useMemo(() => {
    const sums = new Map<string, number>()
    for (const e of expenses) sums.set(e.category_id, (sums.get(e.category_id) ?? 0) + e.amount)
    return [...sums.entries()].sort((a, b) => b[1] - a[1])
  }, [expenses])

  const [pastStores, pastCards] = useMemo(() => {
    const uniq = (xs: (string | null)[]) => [...new Set(xs.filter((x): x is string => Boolean(x)))]
    return [uniq(expenses.map((e) => e.store)), uniq(expenses.map((e) => e.card_name))]
  }, [expenses])

  const days = useMemo(() => {
    const groups: { day: string; items: Expense[] }[] = []
    for (const e of expenses) {
      const last = groups[groups.length - 1]
      if (last?.day === e.spent_on) last.items.push(e)
      else groups.push({ day: e.spent_on, items: [e] })
    }
    return groups
  }, [expenses])

  function shiftMonth(delta: number) {
    const d = new Date(year, month + delta, 1)
    setYear(d.getFullYear())
    setMonth(d.getMonth())
  }

  function onAdded(e: Expense) {
    if (e.spent_on >= from && e.spent_on < to) {
      setExpenses((xs) =>
        [e, ...xs].sort((a, b) => b.spent_on.localeCompare(a.spent_on) || b.created_at.localeCompare(a.created_at)),
      )
    }
  }

  async function remove(e: Expense) {
    if (!confirm(`Delete ${money(e.amount)}?`)) return
    try {
      await deleteExpense(e.id)
      setExpenses((xs) => xs.filter((x) => x.id !== e.id))
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <div className="app">
      <header className="top">
        <strong>💰 Expenses</strong>
        <span className="muted small grow right">{email}</span>
        <button className="link" onClick={() => supabase?.auth.signOut()}>
          Sign out
        </button>
      </header>

      <main className="layout">
        <section className="col">
          <AddExpense
            categories={categories}
            onAdded={onAdded}
            onManageCategories={() => setManaging(true)}
            pastStores={pastStores}
            pastCards={pastCards}
          />
          <button className="secondary" onClick={() => setManaging(true)}>
            Manage categories
          </button>
        </section>

        <section className="col">
          <div className="card summary">
            <div className="month-nav">
              <button className="icon-btn" onClick={() => shiftMonth(-1)} aria-label="Previous month">
                ‹
              </button>
              <span>{monthLabel(year, month)}</span>
              <button
                className="icon-btn"
                onClick={() => shiftMonth(1)}
                disabled={isCurrentMonth}
                aria-label="Next month"
              >
                ›
              </button>
            </div>
            <div className="total">{money(total)}</div>
            <div className="muted small">
              {expenses.length} expense{expenses.length === 1 ? '' : 's'}
            </div>
            {total > 0 && (
              <>
                <div className="bar" aria-hidden>
                  {breakdown.map(([id, sum]) => (
                    <span key={id} style={{ width: `${(sum / total) * 100}%`, background: byId.get(id)?.color }} />
                  ))}
                </div>
                <ul className="breakdown">
                  {breakdown.map(([id, sum]) => {
                    const c = byId.get(id)
                    return (
                      <li key={id}>
                        <span className="dot" style={{ background: c?.color }} />
                        <span className="grow">
                          {c?.icon} {c?.name ?? 'Unknown'}
                        </span>
                        <span className="muted small">{Math.round((sum / total) * 100)}%</span>
                        <span className="num">{money(sum)}</span>
                      </li>
                    )
                  })}
                </ul>
              </>
            )}
          </div>

          {error && (
            <p className="error">
              {error}{' '}
              <button className="link" onClick={refresh}>
                Retry
              </button>
            </p>
          )}

          <div className="card">
            <h2>History</h2>
            {loading && expenses.length === 0 ? (
              <p className="muted">Loading…</p>
            ) : days.length === 0 ? (
              <p className="muted">No expenses this month yet.</p>
            ) : (
              days.map((g) => (
                <div key={g.day} className="day">
                  <div className="day-head">
                    <span>{dayLabel(g.day)}</span>
                    <span className="num">{money(g.items.reduce((s, e) => s + e.amount, 0))}</span>
                  </div>
                  <ul className="expenses">
                    {g.items.map((e) => {
                      const c = byId.get(e.category_id)
                      return (
                        <li key={e.id}>
                          <span className="badge" style={{ background: c?.color }}>
                            {c?.icon}
                          </span>
                          <span className="grow">
                            <span>{c?.name}</span>
                            {details(e) && <span className="muted small block">{details(e)}</span>}
                          </span>
                          <span className="num">{money(e.amount)}</span>
                          <button className="icon-btn" onClick={() => remove(e)} aria-label="Delete expense">
                            🗑
                          </button>
                        </li>
                      )
                    })}
                  </ul>
                </div>
              ))
            )}
          </div>
        </section>
      </main>

      {managing && (
        <CategoryManager categories={categories} onChange={setCategories} onClose={() => setManaging(false)} />
      )}
    </div>
  )
}

/** "Lunch · Starbucks · 💳 Amex Gold": the optional bits of an expense, in one line. */
function details(e: Expense): string {
  const paid =
    e.payment_method === 'cash' ? '💵 Cash' : e.payment_method === 'card' ? `💳 ${e.card_name || 'Card'}` : null
  return [e.note, e.store, paid].filter(Boolean).join(' · ')
}
