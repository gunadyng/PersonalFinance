import { useState, type FormEvent } from 'react'
import { supabase } from './supabase'

export default function Auth() {
  const [mode, setMode] = useState<'signIn' | 'signUp'>('signIn')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<{ kind: 'error' | 'info'; text: string } | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!supabase) return
    setBusy(true)
    setMessage(null)
    const creds = { email: email.trim(), password }
    const { data, error } =
      mode === 'signIn' ? await supabase.auth.signInWithPassword(creds) : await supabase.auth.signUp(creds)
    setBusy(false)
    if (error) setMessage({ kind: 'error', text: error.message })
    else if (mode === 'signUp' && !data.session)
      setMessage({ kind: 'info', text: 'Check your email to confirm your account, then sign in.' })
  }

  return (
    <main className="center">
      <form className="card narrow" onSubmit={submit}>
        <h1>💰 Expense Tracker</h1>
        <p className="muted">{mode === 'signIn' ? 'Sign in to see your expenses.' : 'Create your account.'}</p>
        <label>
          Email
          <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Password
          <input
            type="password"
            autoComplete={mode === 'signIn' ? 'current-password' : 'new-password'}
            minLength={6}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {message && <p className={message.kind}>{message.text}</p>}
        <button className="primary" disabled={busy}>
          {busy ? 'Please wait…' : mode === 'signIn' ? 'Sign in' : 'Create account'}
        </button>
        <button
          type="button"
          className="link"
          onClick={() => {
            setMode(mode === 'signIn' ? 'signUp' : 'signIn')
            setMessage(null)
          }}
        >
          {mode === 'signIn' ? "Don't have an account? Create one" : 'Already have an account? Sign in'}
        </button>
      </form>
    </main>
  )
}
