export interface AuthUser {
  id: string
  name: string
  phone: string
  accountId: number | null
}

export interface LoginRequest {
  phone: string
}
