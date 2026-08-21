import {
  type FormEvent,
  useEffect,
  useState,
} from 'react'

import { getApiErrorMessage } from '../api/getApiErrorMessage'
import {
  getPartnerStatus,
  startPartnerOauth,
  type PartnerStatus,
} from '../api/partnerApi'
import { saveApiKey } from '../api/settingsApi'
import { Button } from './ui/Button/Button'
import { Input } from './ui/Input/Input'

interface ApiKeyModalProps {
  onClose: () => void
}

export function ApiKeyModal({ onClose }: ApiKeyModalProps) {
  const [apiKey, setApiKey] = useState('')
  const [showApiKey, setShowApiKey] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [partnerStatus, setPartnerStatus] = useState<PartnerStatus | null>(null)
  const [isConnecting, setIsConnecting] = useState(false)

  useEffect(() => {
    void getPartnerStatus()
      .then(setPartnerStatus)
      .catch(() => setPartnerStatus(null))
  }, [])

  async function handlePartnerConnect() {
    try {
      setIsConnecting(true)
      setErrorMessage('')
      const authorizationUrl = await startPartnerOauth()
      window.location.assign(authorizationUrl)
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(error, 'Не удалось начать подключение'),
      )
      setIsConnecting(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!apiKey.trim()) {
      setErrorMessage('Введите API-ключ')
      return
    }

    try {
      setIsSubmitting(true)
      setErrorMessage('')
      await saveApiKey({ apiKey: apiKey.trim() })
      onClose()
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(error, 'Не удалось сохранить API-ключ'),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="
        fixed inset-0 z-[70] grid place-items-center overflow-y-auto
        bg-slate-950/55 p-4 backdrop-blur-sm
      "
      role="dialog"
      aria-modal="true"
      aria-labelledby="api-key-modal-title"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isSubmitting) {
          onClose()
        }
      }}
    >
      <section className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl sm:p-8">
        <header className="mb-6 flex items-start justify-between gap-4">
          <div>
            <h2
              id="api-key-modal-title"
              className="text-2xl font-bold text-slate-900"
            >
              Настройки Wazzup
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              Введите новый API-ключ. Текущий ключ останется активным,
              если закрыть окно без сохранения.
            </p>
          </div>
          <button
            type="button"
            aria-label="Закрыть настройки"
            className="
              grid size-10 shrink-0 place-items-center rounded-xl
              text-2xl text-slate-500 hover:bg-slate-100
            "
            disabled={isSubmitting}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          <Input
            id="settings-api-key"
            label="Новый API-ключ"
            type={showApiKey ? 'text' : 'password'}
            value={apiKey}
            autoFocus
            autoComplete="off"
            disabled={isSubmitting}
            error={errorMessage || undefined}
            onChange={(event) => {
              setApiKey(event.target.value)
              setErrorMessage('')
            }}
            rightElement={
              <button
                type="button"
                className="text-sm font-semibold text-slate-500 hover:text-violet-600"
                disabled={isSubmitting}
                onClick={() => setShowApiKey((value) => !value)}
              >
                {showApiKey ? 'Скрыть' : 'Показать'}
              </button>
            }
          />

          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <Button
              type="button"
              className="bg-slate-500 hover:bg-slate-600"
              disabled={isSubmitting}
              onClick={onClose}
            >
              Отмена
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Сохранить API-ключ
            </Button>
          </div>
        </form>

        <div className="my-6 border-t border-slate-200" />

        <section>
          <h3 className="font-bold text-slate-900">Технический API</h3>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            Подключение нужно для первоначальной выгрузки собеседников
            из истории сообщений.
          </p>

          <div className="mt-4 flex items-center justify-between gap-4">
            <span
              className={
                partnerStatus?.connected
                  ? 'text-sm font-semibold text-emerald-700'
                  : 'text-sm font-semibold text-amber-700'
              }
            >
              {partnerStatus?.connected
                ? 'Аккаунт подключён'
                : partnerStatus?.configured
                  ? 'Требуется авторизация'
                  : 'Не настроен на сервере'}
            </span>

            {!partnerStatus?.connected && (
              <Button
                type="button"
                disabled={!partnerStatus?.configured}
                isLoading={isConnecting}
                onClick={() => void handlePartnerConnect()}
              >
                Подключить
              </Button>
            )}
          </div>
        </section>
      </section>
    </div>
  )
}
