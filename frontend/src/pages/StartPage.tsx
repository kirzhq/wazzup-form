import {
  useEffect,
  useState,
} from 'react'
import { Navigate } from 'react-router-dom'

import { getSettingsStatus } from '../api/settingsApi'
import { Alert } from '../components/ui/Alert/Alert'
import { Card } from '../components/ui/Card/Card'
import { AuthLayout } from '../layouts/AuthLayout'
import { getAuthUser } from '../utils/authStorage'

export function StartPage() {
  const [configured, setConfigured] = useState<boolean | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    getSettingsStatus()
      .then((status) => setConfigured(status.configured))
      .catch(() => setFailed(true))
  }, [])

  if (failed) {
    return (
      <AuthLayout>
        <Card>
          <Alert variant="error">
            Не удалось подключиться к серверу приложения.
          </Alert>
        </Card>
      </AuthLayout>
    )
  }

  if (configured === null) {
    return (
      <AuthLayout>
        <Card>
          <p className="text-slate-500">Проверяем настройки...</p>
        </Card>
      </AuthLayout>
    )
  }

  if (!configured) {
    return <Navigate to="/settings" replace />
  }

  return (
    <Navigate
      to={getAuthUser() ? '/contacts' : '/login'}
      replace
    />
  )
}
