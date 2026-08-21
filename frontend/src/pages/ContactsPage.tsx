import {
  type FormEvent,
  useCallback,
  useEffect,
  useMemo,
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
import {
  approvePendingContact,
  dismissPendingContact,
  getPendingContacts,
  type PendingContact,
} from '../api/partnerApi'
import { Alert } from '../components/ui/Alert/Alert'
import { ApiKeyModal } from '../components/ApiKeyModal'
import { Button } from '../components/ui/Button/Button'
import { Card } from '../components/ui/Card/Card'
import { Input } from '../components/ui/Input/Input'
import { SettingsIcon } from '../components/icons/SettingsIcon'
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
type ContactSort = 'newest' | 'name-asc' | 'name-desc'
const INITIAL_CONTACT_MESSAGE = 'Здравствуйте! '

function SelectChevron() {
  return (
    <svg
      viewBox="0 0 20 20"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="pointer-events-none absolute right-4 top-1/2 size-4 -translate-y-1/2 text-slate-500"
      aria-hidden="true"
    >
      <path d="m6 8 4 4 4-4" />
    </svg>
  )
}

function getContactPhone(contact: Contact): string {
  const contactData = contact.contactData ?? []
  const data = contactData.find((item) => item.phone)
    ?? contactData.find((item) =>
      (item.chatType === 'whatsapp' || item.chatType === 'viber')
      && item.chatId)
  if (data?.phone) return data.phone
  if (data && (data.chatType === 'whatsapp' || data.chatType === 'viber')) {
    return data.chatId ?? ''
  }

  for (const network of ['telegram', 'max']) {
    const networkData = contactData.filter((item) =>
      item.chatType === network && item.chatId)
    if (networkData.length < 2) continue
    const phoneData = networkData.find((item) => {
      const digits = normalizePhone(item.chatId ?? '')
      return digits.length === 11 && digits.startsWith('7')
    })
    if (phoneData?.chatId) return phoneData.chatId
  }
  return ''
}

function getContactNetworks(contact: Contact): string[] {
  return Array.from(new Set(
    (contact.contactData ?? [])
      .map((data) => data.chatType)
      .filter((network): network is string => Boolean(network)),
  ))
}

function getContactChatId(contact: Contact, network: PhoneChatType): string {
  const items = (contact.contactData ?? []).filter((data) =>
    data.chatType === network && data.chatId)
  if (network === 'telegram' || network === 'max') {
    const platformId = items.find((data) => {
      const digits = normalizePhone(data.chatId ?? '')
      return !(digits.length === 11 && digits.startsWith('7'))
    })
    if (platformId?.chatId) return platformId.chatId
  }
  return items[0]?.chatId ?? ''
}

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
  const [notice, setNotice] = useState('')
  const [pendingContacts, setPendingContacts] = useState<PendingContact[]>([])
  const [pendingActionId, setPendingActionId] = useState<string | null>(null)
  const [hiddenNetworks, setHiddenNetworks] = useState<Set<string>>(
    () => new Set(),
  )
  const [showNetworkFilter, setShowNetworkFilter] = useState(false)
  const [contactSort, setContactSort] = useState<ContactSort>('newest')

  const availableNetworks = useMemo(() => Array.from(new Set(
    contacts
      .flatMap(getContactNetworks)
      .filter((network): network is string => Boolean(network)),
  )).sort((left, right) => left.localeCompare(right, 'ru')), [contacts])

  const selectedNetworkCount = availableNetworks.length
    - availableNetworks.filter((network) => hiddenNetworks.has(network)).length

  const displayedContacts = useMemo(() => {
    const filtered = hiddenNetworks.size === 0
      ? [...contacts]
      : contacts.filter((contact) => {
      const networks = getContactNetworks(contact)
      return networks.some((network) => !hiddenNetworks.has(network))
    })
    if (contactSort === 'newest') return filtered.reverse()
    return filtered.sort((left, right) => {
      const leftStartsWithLetter = /^\p{L}/u.test(left.name.trim())
      const rightStartsWithLetter = /^\p{L}/u.test(right.name.trim())
      if (leftStartsWithLetter !== rightStartsWithLetter) {
        return leftStartsWithLetter ? -1 : 1
      }
      const result = left.name.localeCompare(right.name, 'ru', {
        sensitivity: 'base',
      })
      return contactSort === 'name-asc' ? result : -result
    })
  }, [contacts, hiddenNetworks, contactSort])

  function toggleNetwork(network: string) {
    setHiddenNetworks((current) => {
      const next = new Set(current)
      if (next.has(network)) next.delete(network)
      else next.add(network)
      return next
    })
  }

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newName, setNewName] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [newMessage, setNewMessage] = useState(INITIAL_CONTACT_MESSAGE)
  const [newMessageError, setNewMessageError] = useState('')
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
    void getPendingContacts().then(setPendingContacts).catch(() => undefined)
  }, [loadContacts])

  async function handleApprovePending(candidate: PendingContact) {
    const suggestedName = candidate.name ?? candidate.username ?? ''
    const name = window.prompt(
      'Проверьте имя перед созданием контакта',
      suggestedName,
    )?.trim()
    if (!name) return
    try {
      setPendingActionId(candidate.id)
      setError('')
      await approvePendingContact(candidate.id, name)
      setPendingContacts(await getPendingContacts())
      await loadContacts(activeSearch)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Не удалось подтвердить контакт'))
    } finally {
      setPendingActionId(null)
    }
  }

  async function handleDismissPending(candidate: PendingContact) {
    try {
      setPendingActionId(candidate.id)
      setError('')
      await dismissPendingContact(candidate.id)
      setPendingContacts(await getPendingContacts())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Не удалось отклонить контакт'))
    } finally {
      setPendingActionId(null)
    }
  }

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
      phone: phoneSearch.replace(/\D/g, ''),
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

    const message = newMessage.trim()
    if (!message) {
      setNewMessageError(
        'Введите первое сообщение — без него создать контакт невозможно',
      )
      return
    }

    try {
      setIsCreating(true)
      setError('')
      setNotice('')
      await createContact({
        name: newName.trim(),
        phone: normalizePhone(newPhone),
        chatType: newChatType,
        message,
      })
      setNewName('')
      setNewPhone('')
      setNewChatType('whatsapp')
      setNewMessage(INITIAL_CONTACT_MESSAGE)
      setNewMessageError('')
      setShowCreateForm(false)
      await loadContacts(activeSearch)
      setNotice(
        'Контакт и чат созданы в Wazzup. Первое сообщение отправлено.',
      )
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
      formatPhone(getContactPhone(contact)),
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
        chatId: getContactChatId(editingContact, editingChatType),
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

          <div
            className="flex items-center gap-1 rounded-2xl border border-slate-200 bg-white p-1 shadow-sm"
            aria-label="Действия пользователя"
          >
            <button
              type="button"
              aria-label="Настройки API-ключа"
              title="Настройки"
              className="
                inline-flex h-12 items-center justify-center gap-2 rounded-xl px-4
                text-sm font-semibold text-slate-700 transition
                hover:bg-violet-50 hover:text-violet-700
                focus:outline-none focus:ring-4 focus:ring-violet-100
              "
              onClick={() => setShowSettings(true)}
            >
              <SettingsIcon className="size-5" />
              <span>Настройки</span>
            </button>
            <button
              type="button"
              className="
                inline-flex h-12 items-center justify-center gap-2 rounded-xl px-4
                text-sm font-semibold text-slate-700 transition
                hover:bg-violet-50 hover:text-violet-700
                focus:outline-none focus:ring-4 focus:ring-violet-100
              "
              onClick={() => void handleLogout()}
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="size-5"
                aria-hidden="true"
              >
                <path d="M10 17l5-5-5-5" />
                <path d="M15 12H3" />
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
              </svg>
              Выйти
            </button>
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
                placeholder="Например, 12345"
                onChange={(event) => setPhoneSearch(
                  event.target.value.replace(/[^\d()+\-\s]/g, ''),
                )}
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
        {notice && (
          <div className="mb-5">
            <Alert variant="success">{notice}</Alert>
          </div>
        )}

        {pendingContacts.length > 0 && (
          <Card className="mb-5 border border-amber-200 bg-amber-50">
            <div className="mb-4">
              <h2 className="text-lg font-bold text-slate-900">
                Новые собеседники без контакта: {pendingContacts.length}
              </h2>
              <p className="mt-1 text-sm text-slate-600">
                Они найдены в технической выгрузке сообщений, но для них нет
                карточки контакта с достоверным именем.
              </p>
            </div>
            <div className="grid gap-3">
              {pendingContacts.map((candidate) => (
                <div
                  key={candidate.id}
                  className="rounded-2xl border border-amber-200 bg-white p-4"
                >
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p className="font-semibold text-slate-900">
                        {candidate.name ?? candidate.username ?? 'Имя не определено'}
                      </p>
                      <p className="mt-1 text-sm text-slate-500">
                        {candidate.chatType} · ID {candidate.chatId}
                        {candidate.phone
                          ? ` · ${formatPhone(candidate.phone)}`
                          : ''}
                      </p>
                      {candidate.lastActivityAt && (
                        <p className="mt-1 text-xs text-slate-400">
                          Последняя активность:{' '}
                          {new Date(candidate.lastActivityAt).toLocaleString(
                            'ru-RU',
                          )}
                        </p>
                      )}
                    </div>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        disabled={pendingActionId === candidate.id}
                        onClick={() => void handleApprovePending(candidate)}
                      >
                        Проверить и добавить
                      </Button>
                      <Button
                        type="button"
                        className="bg-slate-500 hover:bg-slate-600"
                        disabled={pendingActionId === candidate.id}
                        onClick={() => void handleDismissPending(candidate)}
                      >
                        Скрыть
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        )}

        <Card className="p-0">
          <div
            className="
              flex flex-wrap items-center justify-between gap-3
              border-b border-slate-200 px-6 py-4
            "
          >
            <p className="font-semibold text-slate-900">
              Контакты: {displayedContacts.length}
              {displayedContacts.length !== contacts.length && ` из ${contacts.length}`}
            </p>
            <div className="flex flex-wrap items-center gap-4">
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <span>Сортировка</span>
                <span className="relative">
                  <select
                    className="appearance-none rounded-xl border border-slate-300 bg-white py-2 pl-3 pr-11 outline-none transition focus:border-violet-500 focus:ring-4 focus:ring-violet-100"
                    value={contactSort}
                    onChange={(event) => setContactSort(event.target.value as ContactSort)}
                  >
                    <option value="newest">Сначала новые</option>
                    <option value="name-asc">От А до Я</option>
                    <option value="name-desc">От Я до А</option>
                  </select>
                  <SelectChevron />
                </span>
              </label>
              <div
                className="flex items-center gap-2 text-sm text-slate-700"
                onKeyDown={(event) => {
                  if (event.key === 'Escape') setShowNetworkFilter(false)
                }}
              >
                <span>Мессенджеры</span>
                <div className="relative">
                  <button
                    type="button"
                    aria-haspopup="true"
                    aria-expanded={showNetworkFilter}
                    disabled={availableNetworks.length === 0}
                    className="relative min-w-40 rounded-xl border border-slate-300 bg-white py-2 pl-3 pr-11 text-left font-medium outline-none transition hover:border-violet-400 focus:border-violet-500 focus:ring-4 focus:ring-violet-100 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-400"
                    onClick={() => setShowNetworkFilter((current) => !current)}
                  >
                    {selectedNetworkCount === availableNetworks.length
                      ? 'Все'
                      : `Выбрано: ${selectedNetworkCount}`}
                    <SelectChevron />
                  </button>

                  {showNetworkFilter && (
                    <>
                      <button
                        type="button"
                        className="fixed inset-0 z-20 cursor-default"
                        aria-label="Закрыть фильтр мессенджеров"
                        onClick={() => setShowNetworkFilter(false)}
                      />
                      <fieldset className="absolute right-0 z-30 mt-2 min-w-56 rounded-2xl border border-slate-200 bg-white p-2 shadow-xl shadow-slate-200/70">
                        <legend className="sr-only">
                          Фильтр по мессенджерам
                        </legend>
                        {availableNetworks.map((network) => (
                          <label
                            key={network}
                            className="group flex cursor-pointer select-none items-center gap-3 rounded-xl px-3 py-2.5 font-medium text-slate-700 transition hover:bg-violet-50"
                          >
                            <input
                              type="checkbox"
                              className="peer sr-only"
                              checked={!hiddenNetworks.has(network)}
                              onChange={() => toggleNetwork(network)}
                            />
                            <span className="grid size-5 shrink-0 place-items-center rounded-md border-2 border-slate-300 bg-white text-white transition group-hover:border-violet-400 peer-checked:border-violet-600 peer-checked:bg-violet-600 peer-focus-visible:ring-4 peer-focus-visible:ring-violet-100">
                              <svg
                                viewBox="0 0 16 16"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.5"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                className="size-3.5"
                                aria-hidden="true"
                              >
                                <path d="m3 8 3 3 7-7" />
                              </svg>
                            </span>
                            <span className="capitalize">{network}</span>
                          </label>
                        ))}
                        {hiddenNetworks.size > 0 && (
                          <button
                            type="button"
                            className="mt-1 w-full rounded-xl px-3 py-2 text-left text-sm font-semibold text-violet-700 transition hover:bg-violet-50"
                            onClick={() => setHiddenNetworks(new Set())}
                          >
                            Выбрать все
                          </button>
                        )}
                      </fieldset>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
          {isLoading ? (
            <p className="p-8 text-slate-500">Загрузка контактов...</p>
          ) : contacts.length === 0 ? (
            <p className="p-8 text-slate-500">
              {activeSearch.name || activeSearch.phone
                ? 'По вашему запросу ничего не найдено.'
                : 'Список контактов пуст.'}
            </p>
          ) : (
            <div className="overflow-x-auto rounded-b-3xl">
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
                  {displayedContacts.map((contact) => {
                    const phone = getContactPhone(contact) || '—'
                    const networks = getContactNetworks(contact)

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
                          {networks.length > 0 ? networks.join(', ') : '—'}
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
                <div className="relative">
                  <select
                    id="new-contact-chat-type"
                    className="
                      h-12 w-full appearance-none rounded-xl border border-slate-300
                      bg-white pl-4 pr-12 text-slate-900 outline-none transition
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
                  <SelectChevron />
                </div>
              </div>

              <div className="flex flex-col gap-2">
                <label
                  htmlFor="new-contact-message"
                  className="text-sm font-semibold text-slate-800"
                >
                  Первое сообщение
                </label>
                <textarea
                  id="new-contact-message"
                  className={`
                    min-h-28 w-full resize-y rounded-xl border bg-white px-4 py-3
                    text-slate-900 outline-none transition
                    focus:ring-4
                    ${newMessageError
                      ? 'border-red-400 focus:border-red-500 focus:ring-red-100'
                      : 'border-slate-300 focus:border-violet-500 focus:ring-violet-100'}
                  `}
                  value={newMessage}
                  maxLength={4000}
                  disabled={isCreating}
                  aria-required="true"
                  placeholder="Введите сообщение, которое получит клиент"
                  aria-invalid={Boolean(newMessageError)}
                  aria-describedby={newMessageError
                    ? 'new-contact-message-error'
                    : undefined}
                  onChange={(event) => {
                    setNewMessage(event.target.value)
                    setNewMessageError('')
                  }}
                />
                {newMessageError && (
                  <p
                    id="new-contact-message-error"
                    className="text-sm font-medium text-red-600"
                  >
                    {newMessageError}
                  </p>
                )}
                <p className="text-xs leading-5 text-slate-500">
                  Сообщение будет отправлено сразу, чтобы чат появился в Wazzup.
                </p>
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
                <div className="relative">
                  <select
                    id="edit-contact-chat-type"
                    className="
                      h-12 w-full appearance-none rounded-xl border border-slate-300
                      bg-white pl-4 pr-12 text-slate-900 outline-none transition
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
                  <SelectChevron />
                </div>
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
