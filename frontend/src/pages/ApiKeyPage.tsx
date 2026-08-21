import {
  type FormEvent,
  useState,
} from 'react'
import { useNavigate } from 'react-router-dom'

import { getApiErrorMessage } from '../api/getApiErrorMessage'
import { saveApiKey } from '../api/settingsApi'
import { Alert } from '../components/ui/Alert/Alert'
import { Button } from '../components/ui/Button/Button'
import { Card } from '../components/ui/Card/Card'
import { Input } from '../components/ui/Input/Input'
import { AuthLayout } from '../layouts/AuthLayout'

export function ApiKeyPage() {
  const navigate = useNavigate()

  const [apiKey, setApiKey] = useState('')
  const [showApiKey, setShowApiKey] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    const normalizedApiKey = apiKey.trim()

    if (!normalizedApiKey) {
      setErrorMessage('Введите API-ключ')
      setSuccessMessage('')
      return
    }

    try {
      setIsSubmitting(true)
      setErrorMessage('')
      setSuccessMessage('')

      await saveApiKey({
        apiKey: normalizedApiKey,
      })

      setSuccessMessage(
        'API-ключ успешно сохранён',
      )

      window.setTimeout(() => {
        navigate('/login')
      }, 700)
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          'Не удалось сохранить API-ключ',
        ),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout>
      <Card>
        <header className="mb-8 flex items-start gap-4">
          <div
            className="
              grid size-14 shrink-0 place-items-center
              rounded-2xl bg-violet-600
              text-2xl font-extrabold text-white
              shadow-lg shadow-violet-200
            "
          >
            W
          </div>

          <div>
            <h1 className="text-2xl font-bold text-slate-900">
              Подключение Wazzup
            </h1>

            <p className="mt-2 leading-6 text-slate-500">
              Укажите API-ключ из личного кабинета Wazzup. Он нужен для
              проверки сотрудников и работы с контактами.
            </p>
          </div>
        </header>

        <form
          className="flex flex-col gap-5"
          onSubmit={handleSubmit}
        >
          <Input
            id="api-key"
            name="apiKey"
            label="API-ключ"
            type={showApiKey ? 'text' : 'password'}
            value={apiKey}
            placeholder="Вставьте API-ключ Wazzup"
            autoComplete="off"
            disabled={isSubmitting}
            error={errorMessage || undefined}
            onChange={(event) => {
              setApiKey(event.target.value)
              setErrorMessage('')
              setSuccessMessage('')
            }}
            rightElement={
              <button
                type="button"
                className="
                  text-sm font-semibold text-slate-500
                  transition hover:text-violet-600
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                "
                onClick={() => {
                  setShowApiKey((value) => !value)
                }}
                disabled={isSubmitting}
              >
                {showApiKey ? 'Скрыть' : 'Показать'}
              </button>
            }
          />

          {successMessage && (
            <Alert variant="success">
              {successMessage}
            </Alert>
          )}

          <Button
            type="submit"
            isLoading={isSubmitting}
            className="w-full"
          >
            Сохранить и продолжить
          </Button>
        </form>
      </Card>
    </AuthLayout>
  )
}
