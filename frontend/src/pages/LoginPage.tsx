import {
  type FormEvent,
  useState,
} from 'react'
import {
  Link,
  useNavigate,
} from 'react-router-dom'

import { login } from '../api/authApi'
import { getApiErrorMessage } from '../api/getApiErrorMessage'
import { Alert } from '../components/ui/Alert/Alert'
import { Button } from '../components/ui/Button/Button'
import { Card } from '../components/ui/Card/Card'
import { Input } from '../components/ui/Input/Input'
import { AuthLayout } from '../layouts/AuthLayout'
import { saveAuthUser } from '../utils/authStorage'

export function LoginPage() {
  const navigate = useNavigate()

  const [phone, setPhone] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    const normalizedPhone = phone.replace(/\D/g, '')

    if (!normalizedPhone) {
      setErrorMessage('Введите номер телефона')
      return
    }

    if (normalizedPhone.length < 10) {
      setErrorMessage('Введите корректный номер телефона')
      return
    }

    try {
      setIsSubmitting(true)
      setErrorMessage('')

      const user = await login({
        phone: normalizedPhone,
      })

      saveAuthUser(user)
      navigate('/contacts', {
        replace: true,
      })
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          'Не удалось авторизоваться. Проверьте номер телефона.',
        ),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout>
      <Card>
        <header className="mb-8">
          <div
            className="
              mb-5 grid size-14 place-items-center
              rounded-2xl bg-violet-600
              text-2xl font-extrabold text-white
              shadow-lg shadow-violet-200
            "
          >
            W
          </div>

          <h1 className="text-2xl font-bold text-slate-900">
            Вход сотрудника
          </h1>

          <p className="mt-2 leading-6 text-slate-500">
            Введите номер телефона, указанный
            в профиле сотрудника Wazzup.
          </p>
        </header>

        <form
          className="flex flex-col gap-5"
          onSubmit={handleSubmit}
        >
          <Input
            id="phone"
            name="phone"
            label="Номер телефона"
            type="tel"
            value={phone}
            placeholder="+7 999 123-45-67"
            autoComplete="tel"
            disabled={isSubmitting}
            onChange={(event) => {
              setPhone(event.target.value)
              setErrorMessage('')
            }}
          />

          {errorMessage && (
            <Alert variant="error">
              {errorMessage}
            </Alert>
          )}

          <Button
            type="submit"
            isLoading={isSubmitting}
            className="w-full"
          >
            Войти
          </Button>

          <Link
            to="/settings"
            className="
              text-center text-sm font-semibold
              text-slate-500 transition
              hover:text-violet-600
            "
          >
            Изменить API-ключ
          </Link>
        </form>
      </Card>
    </AuthLayout>
  )
}