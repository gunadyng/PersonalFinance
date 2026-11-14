import { useState, type FormEvent } from 'react'
import { addExpense, type Category, type Expense } from './api'
import { parseAmount, today } from './format'

interface Props {
  categories: Category[]
  onAdded: (e: Expense) => void
  onManageCategories: () => void
}

export default function AddExpense({ categories, onAdded, onManageCategories }: Props) {
  const [amount, setAmount] = useState('')
  const [categoryId, setCategoryId] = useState<string | null>(null)
  const [note, setNote] = useState('')
  const [date, setDate] = useState(today())
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    const value = parseAmount(amount)
    if (value === null) return setError('Enter an amount greater than 0.')
    if (!categoryId) return setError('Pick a category.')
    setBusy(true)
    setError(null)
    try {
      onAdded(await addExpense({ amount: value, category_id: categoryId, note, spent_on: date }))
      setAmount('')
      setNote('')
      setCategoryId(null)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <h2>Add expense</h2>
      <div className="amount-row">
        <span className="currency">$</span>
        <input
          className="amount"
          inputMode="decimal"
          placeholder="0.00"
          aria-label="Amount"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          autoFocus
        />
      </div>
      <div className="chips" role="radiogroup" aria-label="Category">
        {categories.map((c) => (
          <button
            type="button"
            key={c.id}
            role="radio"
            aria-checked={categoryId === c.id}
            className={'chip' + (categoryId === c.id ? ' selected' : '')}
            style={{ '--chip': c.color } as React.CSSProperties}
            onClick={() => setCategoryId(c.id)}
          >
            <span aria-hidden>{c.icon}</span> {c.name}
          </button>
        ))}
        <button type="button" className="chip add" onClick={onManageCategories}>
          ＋ New category
        </button>
      </div>
      <div className="row">
        <input placeholder="Note (optional)" maxLength={200} value={note} onChange={(e) => setNote(e.target.value)} />
        <input type="date" aria-label="Date" value={date} max={today()} onChange={(e) => setDate(e.target.value)} />
      </div>
      {error && <p className="error">{error}</p>}
      <button className="primary" disabled={busy}>
        {busy ? 'Saving…' : 'Add expense'}
      </button>
    </form>
  )
}
