import { useEffect, useState } from 'react'
import type { Session } from '@supabase/supabase-js'
import { isConfigured, supabase } from './supabase'
import Auth from './Auth'
import Tracker from './Tracker'

export default function App() {
  const [session, setSession] = useState<Session | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    if (!supabase) return
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session)
      setReady(true)
    })
    const { data } = supabase.auth.onAuthStateChange((_event, s) => setSession(s))
    return () => data.subscription.unsubscribe()
  }, [])

  if (!isConfigured) {
    return (
      <main className="center">
        <div className="card narrow">
          <h1>💰 Expense Tracker</h1>
          <p>
            The app isn't connected to a database yet. Set <code>VITE_SUPABASE_URL</code> and{' '}
            <code>VITE_SUPABASE_ANON_KEY</code> (see the README) and rebuild.
          </p>
        </div>
      </main>
    )
  }
  if (!ready) return <main className="center muted">Loading…</main>
  return session ? <Tracker email={session.user.email ?? ''} /> : <Auth />
}
