import { useState, type FormEvent } from 'react'
import { addCategory, deleteCategory, type Category } from './api'

const COLORS = ['#f97316', '#22c55e', '#3b82f6', '#eab308', '#8b5cf6', '#ec4899', '#ef4444', '#06b6d4', '#64748b']
const ICONS = ['🏷️', '🐶', '☕', '🍺', '🎮', '👶', '🏋️', '💼', '🔧', '📱', '🚕', '⛽', '🎓', '💐']

interface Props {
  categories: Category[]
  onChange: (categories: Category[]) => void
  onClose: () => void
}

export default function CategoryManager({ categories, onChange, onClose }: Props) {
  const [name, setName] = useState('')
  const [icon, setIcon] = useState(ICONS[0])
  const [color, setColor] = useState(COLORS[0])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function add(e: FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    setBusy(true)
    setError(null)
    try {
      const created = await addCategory({ name, icon, color })
      onChange([...categories, created].sort((a, b) => a.sort_order - b.sort_order || a.name.localeCompare(b.name)))
      setName('')
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(c: Category) {
    if (!confirm(`Delete the "${c.name}" category?`)) return
    setError(null)
    try {
      await deleteCategory(c.id)
      onChange(categories.filter((x) => x.id !== c.id))
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <div className="overlay" onClick={onClose}>
      <div className="card sheet" role="dialog" aria-label="Categories" onClick={(e) => e.stopPropagation()}>
        <div className="sheet-head">
          <h2>Categories</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        <form onSubmit={add} className="new-cat">
          <div className="row">
            <select aria-label="Icon" value={icon} onChange={(e) => setIcon(e.target.value)} className="icon-select">
              {ICONS.map((i) => (
                <option key={i}>{i}</option>
              ))}
            </select>
            <input placeholder="New category name" maxLength={40} value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="swatches" role="radiogroup" aria-label="Colour">
            {COLORS.map((c) => (
              <button
                type="button"
                key={c}
                role="radio"
                aria-checked={color === c}
                aria-label={c}
                className={'swatch' + (color === c ? ' selected' : '')}
                style={{ background: c }}
                onClick={() => setColor(c)}
              />
            ))}
          </div>
          <button className="primary" disabled={busy || !name.trim()}>
            Add category
          </button>
        </form>
        {error && <p className="error">{error}</p>}
        <ul className="cat-list">
          {categories.map((c) => (
            <li key={c.id}>
              <span className="dot" style={{ background: c.color }} />
              <span aria-hidden>{c.icon}</span>
              <span className="grow">{c.name}</span>
              {c.is_default && <span className="tag">default</span>}
              <button className="icon-btn" onClick={() => remove(c)} aria-label={`Delete ${c.name}`}>
                🗑
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
