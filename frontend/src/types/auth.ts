export interface AuthUser {
  id: string
  name: string
  phone: string
  accountId: number
}

export interface LoginRequest {
  phone: string
}