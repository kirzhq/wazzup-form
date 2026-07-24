import { httpClient } from './httpClient'
import type {
  AuthUser,
  LoginRequest,
} from '../types/auth'

export async function login(
  request: LoginRequest,
): Promise<AuthUser> {
  const response = await httpClient.post<AuthUser>(
    '/auth/login',
    request,
  )

  return response.data
}

export async function logout(): Promise<void> {
  await httpClient.post('/auth/logout')
}
