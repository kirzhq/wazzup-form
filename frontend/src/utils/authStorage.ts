import type { AuthUser } from '../types/auth'

const AUTH_USER_KEY = 'wazzup_crm_auth_user'

export function saveAuthUser(user: AuthUser): void {
  localStorage.setItem(
    AUTH_USER_KEY,
    JSON.stringify(user),
  )
}

export function getAuthUser(): AuthUser | null {
  const storedUser = localStorage.getItem(AUTH_USER_KEY)

  if (!storedUser) {
    return null
  }

  try {
    return JSON.parse(storedUser) as AuthUser
  } catch {
    localStorage.removeItem(AUTH_USER_KEY)
    return null
  }
}

export function removeAuthUser(): void {
  localStorage.removeItem(AUTH_USER_KEY)
}