import { useEffect, useState } from 'react'
import { AppRoutes } from './routes/AppRoutes'
import { completePartnerOauth } from './api/partnerApi'

function App() {
  const [oauthPending, setOauthPending] = useState(false)
  const [oauthError, setOauthError] = useState<string | null>(null)

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    const state = params.get('state')
    if (!code || !state) return

    setOauthPending(true)
    completePartnerOauth(code, state)
      .then(() => window.location.replace('/contacts?partner=connected'))
      .catch(() => {
        setOauthError('Не удалось завершить подключение Wazzup. Попробуйте ещё раз.')
        setOauthPending(false)
      })
  }, [])

  if (oauthPending) return <main className="oauth-result">Подключаем технический API Wazzup…</main>
  if (oauthError) return <main className="oauth-result">{oauthError}</main>
  return <AppRoutes />
}

export default App
