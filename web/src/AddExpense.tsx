import { useState, type FormEvent } from 'react'
import { addExpense, type Category, type Expense, type PaymentMethod } from './api'
import { parseAmount, today } from './format'

interface Props {
  categories: Category[]
  onAdded: (e: Expense) => void
  onManageCategories: () => void
  /** Stores and card names used before, offered as suggestions. */
  pastStores: string[]
  pastCards: string[]
}

export default function AddExpense({ categories, onAdded, onManageCategories, pastStores, pastCards }: Props) {
  const [amount, setAmount] = useState('')
  const [categoryId, setCategoryId] = useState<string | null>(null)
  const [note, setNote] = useState('')
  const [date, setDate] = useState(today())
  const [store, setStore] = useState('')
  const [payment, setPayment] = useState<PaymentMethod | null>(null)
  const [cardName, setCardName] = useState('')
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
      onAdded(
        await addExpense({
          amount: value,
          category_id: categoryId,
          note,
          spent_on: date,
          store,
          payment_method: payment,
          card_name: cardName,
        }),
      )
      setAmount('')
      setNote('')
      setStore('')
      setCategoryId(null)
      // Payment method and card stay selected: you usually pay the same way.
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
      <input
        placeholder="Store or shop (optional)"
        aria-label="Store or shop"
        maxLength={80}
        list="past-stores"
        value={store}
        onChange={(e) => setStore(e.target.value)}
      />
      <datalist id="past-stores">
        {pastStores.map((s) => (
          <option key={s} value={s} />
        ))}
      </datalist>
      <div className="segmented" role="radiogroup" aria-label="Paid with">
        <span className="muted small">Paid with</span>
        {([null, 'cash', 'card'] as const).map((m) => (
          <button
            type="button"
            key={m ?? 'none'}
            role="radio"
            aria-checked={payment === m}
            className={'chip' + (payment === m ? ' selected' : '')}
            onClick={() => setPayment(m)}
          >
            {m === null ? 'Not set' : m === 'cash' ? '💵 Cash' : '💳 Card'}
          </button>
        ))}
      </div>
      {payment === 'card' && (
        <>
          <input
            placeholder="Card name, e.g. Visa or Amex Gold (optional)"
            aria-label="Card name"
            maxLength={40}
            list="past-cards"
            value={cardName}
            onChange={(e) => setCardName(e.target.value)}
          />
          <datalist id="past-cards">
            {pastCards.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
        </>
      )}
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
