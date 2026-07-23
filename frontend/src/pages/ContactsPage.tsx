import { useNavigate } from 'react-router-dom'

import { Button } from '../components/ui/Button/Button'
import {
  getAuthUser,
  removeAuthUser,
} from '../utils/authStorage'

export function ContactsPage() {
  const navigate = useNavigate()
  const user = getAuthUser()

  function handleLogout() {
    removeAuthUser()
    navigate('/login', {
      replace: true,
    })
  }

  return (
    <main className="min-h-screen bg-slate-100 px-5 py-8">
      <div className="mx-auto max-w-6xl">
        <header
          className="
            flex items-center justify-between gap-5
            rounded-2xl border border-slate-200
            bg-white px-6 py-5 shadow-sm
          "
        >
          <div>
            <p className="text-sm text-slate-500">
              Вы вошли как
            </p>

            <h1 className="text-xl font-bold text-slate-900">
              {user?.name ?? 'Сотрудник'}
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              {user?.phone}
            </p>
          </div>

          <Button
            type="button"
            onClick={handleLogout}
            className="min-h-10 bg-slate-700 px-4 hover:bg-slate-800"
          >
            Выйти
          </Button>
        </header>

        <section
          className="
            mt-6 rounded-2xl border border-slate-200
            bg-white p-6 shadow-sm
          "
        >
          <h2 className="text-2xl font-bold text-slate-900">
            Контакты
          </h2>

          <p className="mt-2 text-slate-500">
            Авторизация работает. Следующим шагом подключим
            загрузку и поиск контактов.
          </p>
        </section>
      </div>
    </main>
  )
}