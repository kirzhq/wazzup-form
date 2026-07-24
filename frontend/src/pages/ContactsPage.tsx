import {
  type FormEvent,
  useCallback,
  useEffect,
  useState,
} from 'react'
import { useNavigate } from 'react-router-dom'

import {
  createContact,
  deleteContact,
  getContacts,
  updateContact,
} from '../api/contactsApi'
import { getApiErrorMessage } from '../api/getApiErrorMessage'
import { logout } from '../api/authApi'
import { Alert } from '../components/ui/Alert/Alert'
import { ApiKeyModal } from '../components/ApiKeyModal'
import { Button } from '../components/ui/Button/Button'
import { Card } from '../components/ui/Card/Card'
import { Input } from '../components/ui/Input/Input'
import type { Contact } from '../types/contact'
import {
  getAuthUser,
  removeAuthUser,
} from '../utils/authStorage'
import {
  formatPhone,
  normalizePhone,
} from '../utils/phone'

type PhoneChatType = 'whatsapp' | 'telegram' | 'viber' | 'max'

export function ContactsPage() {
  const navigate = useNavigate()
  const user = getAuthUser()

  const [contacts, setContacts] = useState<Contact[]>([])
  const [nameSearch, setNameSearch] = useState('')
  const [phoneSearch, setPhoneSearch] = useState('')
  const [activeSearch, setActiveSearch] = useState({
    name: '',
    phone: '',
  })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newName, setNewName] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [newChatType, setNewChatType] =
    useState<PhoneChatType>('whatsapp')
  const [isCreating, setIsCreating] = useState(false)

  const [editingContact, setEditingContact] = useState<Contact | null>(null)
  const [editingName, setEditingName] = useState('')
  const [editingPhone, setEditingPhone] = useState('')
  const [editingChatType, setEditingChatType] =
    useState<PhoneChatType>('whatsapp')
  const [isEditing, setIsEditing] = useState(false)
  const [openMenu, setOpenMenu] = useState<{
    id: string
    top: number
    right: number
  } | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [showSettings, setShowSettings] = useState(false)

  const loadContacts = useCallback(async (
    search: { name?: string; phone?: string } = {},
  ) => {
    try {
      setIsLoading(true)
      setError('')
      setContacts(await getContacts(search))
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          'Не удалось загрузить контакты',
        ),
      )
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadContacts()
  }, [loadContacts])

  async function handleLogout() {
    try {
      await logout()
    } finally {
      removeAuthUser()
      navigate('/login', { replace: true })
    }
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const search = {
      name: nameSearch.trim(),
      phone: normalizePhone(phoneSearch),
    }
    setActiveSearch(search)
    void loadContacts(search)
  }

  function clearSearch() {
    setNameSearch('')
    setPhoneSearch('')
    setActiveSearch({ name: '', phone: '' })
    void loadContacts()
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!user) {
      navigate('/login', { replace: true })
      return
    }

    try {
      setIsCreating(true)
      setError('')
      await createContact({
        name: newName.trim(),
        phone: normalizePhone(newPhone),
        responsibleUserId: user.id,
        chatType: newChatType,
      })
      setNewName('')
      setNewPhone('')
      setNewChatType('whatsapp')
      setShowCreateForm(false)
      await loadContacts(activeSearch)
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          'Не удалось добавить контакт',
        ),
      )
    } finally {
      setIsCreating(false)
    }
  }

  function startEdit(contact: Contact) {
    const contactData = contact.contactData?.[0]
    const chatType = contactData?.chatType

    setOpenMenu(null)
    setEditingContact(contact)
    setEditingName(contact.name)
    setEditingPhone(
      formatPhone(contactData?.phone ?? contactData?.chatId ?? ''),
    )
    setEditingChatType(
      chatType === 'telegram'
        || chatType === 'viber'
        || chatType === 'max'
        ? chatType
        : 'whatsapp',
    )
    setError('')
  }

  async function handleDelete(contact: Contact) {
    setOpenMenu(null)

    const confirmed = window.confirm(
      `Удалить контакт «${contact.name}»? Это действие нельзя отменить.`,
    )

    if (!confirmed) {
      return
    }

    try {
      setDeletingId(contact.id)
      setError('')
      await deleteContact(contact.id)
      await loadContacts(activeSearch)
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          'Не удалось удалить контакт',
        ),
      )
    } finally {
      setDeletingId(null)
    }
  }

  async function handleEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!editingContact) {
      return
    }

    try {
      setIsEditing(true)
      setError('')
      await updateContact(editingContact.id, {
        name: editingName.trim(),
        phone: normalizePhone(editingPhone),
        chatType: editingChatType,
      })
      setEditingContact(null)
      await loadContacts(activeSearch)
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          'Не удалось изменить контакт',
        ),
      )
    } finally {
      setIsEditing(false)
    }
  }

  return (
    <main className="min-h-screen bg-slate-100 px-4 py-6 sm:px-8">
      <div className="mx-auto max-w-7xl">
        <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">
              Контакты
            </h1>
            <p className="mt-1 text-slate-500">
              {user?.name ?? 'Сотрудник'}
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              aria-label="Настройки API-ключа"
              title="Настройки"
              className="
                grid size-12 place-items-center rounded-xl bg-white
                text-xl text-slate-600 shadow-sm transition
                hover:text-violet-600 focus:outline-none
                focus:ring-4 focus:ring-violet-100
              "
              onClick={() => setShowSettings(true)}
            >
              ⚙
            </button>
            <Button
              type="button"
              className="bg-slate-700 hover:bg-slate-800"
              onClick={() => void handleLogout()}
            >
              Выйти
            </Button>
          </div>
        </header>

        <div className="mb-5 grid gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(280px,1fr)]">
          <Card>
            <header className="mb-5 flex items-start gap-3">
              <div
                className="
                  grid size-11 shrink-0 place-items-center rounded-2xl
                  bg-violet-100 text-xl text-violet-700
                "
                aria-hidden="true"
              >
                ⌕
              </div>
              <div>
                <h2 className="text-lg font-bold text-slate-900">
                  Поиск контактов
                </h2>
                <p className="mt-1 text-sm leading-5 text-slate-500">
                  Найдите контакт отдельно по имени или номеру телефона.
                </p>
              </div>
            </header>

            <form
              className="
                grid gap-4 md:grid-cols-2
              "
              onSubmit={handleSearch}
            >
              <Input
                id="contact-name-search"
                label="Поиск по имени"
                value={nameSearch}
                placeholder="Например, Тест2"
                onChange={(event) => setNameSearch(event.target.value)}
              />
              <Input
                id="contact-phone-search"
                label="Поиск по телефону"
                type="tel"
                value={phoneSearch}
                placeholder="+7 (999) 123-45-67"
                onChange={(event) =>
                  setPhoneSearch(formatPhone(event.target.value))}
              />
              <div className="flex flex-wrap gap-3 md:col-span-2">
                <Button type="submit" isLoading={isLoading}>
                  Найти контакты
                </Button>
                {(activeSearch.name || activeSearch.phone) && (
                  <Button
                    type="button"
                    className="bg-slate-500 hover:bg-slate-600"
                    onClick={clearSearch}
                  >
                    Очистить фильтры
                  </Button>
                )}
              </div>
            </form>
          </Card>

          <Card
            className="
              flex flex-col justify-between overflow-hidden
              bg-gradient-to-br from-violet-600 to-indigo-700 text-white
            "
          >
            <div>
              <div
                className="
                  mb-4 grid size-11 place-items-center rounded-2xl
                  bg-white/15 text-2xl
                "
                aria-hidden="true"
              >
                +
              </div>
              <h2 className="text-xl font-bold">Новый контакт</h2>
              <p className="mt-2 text-sm leading-6 text-violet-100">
                Добавьте имя, телефон и выберите социальную сеть.
                Контакт сразу сохранится в Wazzup.
              </p>
            </div>
            <Button
              type="button"
              className="
                mt-6 w-full !bg-white !text-violet-700
                hover:!bg-violet-50
              "
              onClick={() => setShowCreateForm(true)}
            >
              Добавить контакт
            </Button>
          </Card>
        </div>

        {error && (
          <div className="mb-5">
            <Alert variant="error">{error}</Alert>
          </div>
        )}

        <Card className="overflow-hidden p-0">
          {isLoading ? (
            <p className="p-8 text-slate-500">Загрузка контактов...</p>
          ) : contacts.length === 0 ? (
            <p className="p-8 text-slate-500">
              {activeSearch.name || activeSearch.phone
                ? 'По вашему запросу ничего не найдено.'
                : 'Список контактов пуст.'}
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px]">
                <thead className="bg-slate-50 text-sm text-slate-600">
                  <tr>
                    <th className="px-6 py-4 text-left">Имя</th>
                    <th className="px-6 py-4 text-left">Телефон</th>
                    <th className="px-6 py-4 text-left">Мессенджер</th>
                    <th className="px-6 py-4 text-right">Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {contacts.map((contact) => {
                    const contactData = contact.contactData?.[0]
                    const phone =
                      contactData?.phone ?? contactData?.chatId ?? '—'

                    return (
                      <tr
                        key={contact.id}
                        className="border-t border-slate-200"
                      >
                        <td className="px-6 py-4 font-medium text-slate-900">
                          <span className="line-clamp-2">{contact.name}</span>
                        </td>
                        <td className="px-6 py-4 text-slate-700">
                          {phone === '—' ? phone : formatPhone(phone)}
                        </td>
                        <td className="px-6 py-4 text-slate-700">
                          {contactData?.chatType ?? '—'}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <>
                              <button
                                type="button"
                                aria-label={`Действия с контактом ${contact.name}`}
                                aria-expanded={openMenu?.id === contact.id}
                                className="
                                  ml-auto grid size-10 place-items-center rounded-lg
                                  text-2xl leading-none text-slate-500 transition
                                  hover:bg-slate-100 hover:text-slate-900
                                  focus:outline-none focus:ring-4 focus:ring-violet-100
                                  disabled:cursor-not-allowed disabled:opacity-50
                                "
                                disabled={deletingId === contact.id}
                                onClick={(event) => {
                                  const rect =
                                    event.currentTarget.getBoundingClientRect()

                                  setOpenMenu((current) =>
                                    current?.id === contact.id
                                      ? null
                                      : {
                                          id: contact.id,
                                          top:
                                            rect.bottom + 104 > window.innerHeight
                                              ? rect.top - 104
                                              : rect.bottom + 6,
                                          right: window.innerWidth - rect.right,
                                        })
                                }}
                              >
                                {deletingId === contact.id ? '…' : '⋮'}
                              </button>

                              {openMenu?.id === contact.id && (
                                <>
                                  <button
                                    type="button"
                                    aria-label="Закрыть меню действий"
                                    className="fixed inset-0 z-[55] cursor-default"
                                    onClick={() => setOpenMenu(null)}
                                  />
                                  <div
                                    className="
                                      fixed z-[60] w-44 overflow-hidden rounded-xl border
                                      border-slate-200 bg-white py-1 text-left
                                      shadow-xl
                                    "
                                    style={{
                                      top: openMenu.top,
                                      right: openMenu.right,
                                    }}
                                  >
                                  <button
                                    type="button"
                                    className="
                                      block w-full px-4 py-3 text-left text-sm
                                      font-medium text-slate-700
                                      hover:bg-slate-50
                                    "
                                    onClick={() => startEdit(contact)}
                                  >
                                    Редактировать
                                  </button>
                                  <button
                                    type="button"
                                    className="
                                      block w-full px-4 py-3 text-left text-sm
                                      font-medium text-red-600 hover:bg-red-50
                                    "
                                    onClick={() => void handleDelete(contact)}
                                  >
                                    Удалить
                                  </button>
                                  </div>
                                </>
                              )}
                          </>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>

      {showCreateForm && (
        <div
          className="
            fixed inset-0 z-50 grid place-items-center overflow-y-auto
            bg-slate-950/55 p-4 backdrop-blur-sm
          "
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-contact-title"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !isCreating) {
              setShowCreateForm(false)
            }
          }}
        >
          <section className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl sm:p-8">
            <header className="mb-6 flex items-start justify-between gap-4">
              <div>
                <h2
                  id="create-contact-title"
                  className="text-2xl font-bold text-slate-900"
                >
                  Новый контакт
                </h2>
                <p className="mt-2 text-sm leading-6 text-slate-500">
                  Заполните данные для сохранения контакта в Wazzup.
                </p>
              </div>
              <button
                type="button"
                aria-label="Закрыть окно"
                className="
                  grid size-10 shrink-0 place-items-center rounded-xl
                  text-2xl text-slate-500 hover:bg-slate-100
                "
                disabled={isCreating}
                onClick={() => setShowCreateForm(false)}
              >
                ×
              </button>
            </header>

            <form
              className="flex flex-col gap-5"
              onSubmit={(event) => void handleCreate(event)}
            >
              <Input
                id="new-contact-name"
                label="Имя"
                value={newName}
                required
                maxLength={200}
                autoFocus
                disabled={isCreating}
                placeholder="Например, Иван Петров"
                onChange={(event) => setNewName(event.target.value)}
              />
              <Input
                id="new-contact-phone"
                label="Телефон"
                type="tel"
                value={newPhone}
                required
                disabled={isCreating}
                placeholder="+7 (999) 123-45-67"
                onChange={(event) =>
                  setNewPhone(formatPhone(event.target.value))}
              />
              <div className="flex flex-col gap-2">
                <label
                  htmlFor="new-contact-chat-type"
                  className="text-sm font-semibold text-slate-800"
                >
                  Социальная сеть
                </label>
                <select
                  id="new-contact-chat-type"
                  className="
                    h-12 w-full rounded-xl border border-slate-300
                    bg-white px-4 text-slate-900 outline-none transition
                    focus:border-violet-500 focus:ring-4 focus:ring-violet-100
                  "
                  value={newChatType}
                  disabled={isCreating}
                  onChange={(event) =>
                    setNewChatType(event.target.value as PhoneChatType)}
                >
                  <option value="whatsapp">WhatsApp</option>
                  <option value="telegram">Telegram</option>
                  <option value="viber">Viber</option>
                  <option value="max">MAX</option>
                </select>
              </div>

              <div className="mt-2 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                <Button
                  type="button"
                  className="bg-slate-500 hover:bg-slate-600"
                  disabled={isCreating}
                  onClick={() => setShowCreateForm(false)}
                >
                  Отмена
                </Button>
                <Button type="submit" isLoading={isCreating}>
                  Сохранить контакт
                </Button>
              </div>
            </form>
          </section>
        </div>
      )}

      {editingContact && (
        <div
          className="
            fixed inset-0 z-50 grid place-items-center overflow-y-auto
            bg-slate-950/55 p-4 backdrop-blur-sm
          "
          role="dialog"
          aria-modal="true"
          aria-labelledby="edit-contact-title"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !isEditing) {
              setEditingContact(null)
            }
          }}
        >
          <section
            className="
              w-full max-w-lg rounded-3xl bg-white p-6
              shadow-2xl sm:p-8
            "
          >
            <header className="mb-6 flex items-start justify-between gap-4">
              <div>
                <h2
                  id="edit-contact-title"
                  className="text-2xl font-bold text-slate-900"
                >
                  Редактирование контакта
                </h2>
                <p className="mt-2 text-sm text-slate-500">
                  Измените данные и сохраните контакт в Wazzup.
                </p>
              </div>
              <button
                type="button"
                aria-label="Закрыть окно"
                className="
                  grid size-10 shrink-0 place-items-center rounded-xl
                  text-2xl text-slate-500 hover:bg-slate-100
                "
                disabled={isEditing}
                onClick={() => setEditingContact(null)}
              >
                ×
              </button>
            </header>

            <form
              className="flex flex-col gap-5"
              onSubmit={(event) => void handleEdit(event)}
            >
              <Input
                id="edit-contact-name"
                label="Имя"
                value={editingName}
                required
                maxLength={200}
                autoFocus
                disabled={isEditing}
                onChange={(event) => setEditingName(event.target.value)}
              />
              <Input
                id="edit-contact-phone"
                label="Телефон"
                type="tel"
                value={editingPhone}
                required
                disabled={isEditing}
                onChange={(event) =>
                  setEditingPhone(formatPhone(event.target.value))}
              />
              <div className="flex flex-col gap-2">
                <label
                  htmlFor="edit-contact-chat-type"
                  className="text-sm font-semibold text-slate-800"
                >
                  Социальная сеть
                </label>
                <select
                  id="edit-contact-chat-type"
                  className="
                    h-12 w-full rounded-xl border border-slate-300
                    bg-white px-4 text-slate-900 outline-none transition
                    focus:border-violet-500 focus:ring-4 focus:ring-violet-100
                  "
                  value={editingChatType}
                  disabled={isEditing}
                  onChange={(event) =>
                    setEditingChatType(event.target.value as PhoneChatType)}
                >
                  <option value="whatsapp">WhatsApp</option>
                  <option value="telegram">Telegram</option>
                  <option value="viber">Viber</option>
                  <option value="max">MAX</option>
                </select>
              </div>

              <div className="mt-2 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                <Button
                  type="button"
                  className="bg-slate-500 hover:bg-slate-600"
                  disabled={isEditing}
                  onClick={() => setEditingContact(null)}
                >
                  Отмена
                </Button>
                <Button type="submit" isLoading={isEditing}>
                  Сохранить изменения
                </Button>
              </div>
            </form>
          </section>
        </div>
      )}

      {showSettings && (
        <ApiKeyModal onClose={() => setShowSettings(false)} />
      )}
    </main>
  )
}
