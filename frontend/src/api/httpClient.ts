import axios from 'axios'
import { removeAuthUser } from '../utils/authStorage'

interface CsrfResponse {
  headerName: string
  token: string
}

function getCsrfToken(): Promise<CsrfResponse> {
  return axios
    .get<CsrfResponse>('/api/auth/csrf', { timeout: 10_000 })
    .then((response) => response.data)
}

export const httpClient = axios.create({
  baseURL: '/api',
  timeout: 120_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.request.use(async (config) => {
  const method = config.method?.toLowerCase()
  const requiresCsrf = method
    && ['post', 'put', 'patch', 'delete'].includes(method)
    && config.url !== '/auth/login'

  if (requiresCsrf) {
    const csrf = await getCsrfToken()
    config.headers.set(csrf.headerName, csrf.token)
  }

  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)
      && error.response?.status === 401
      && window.location.pathname !== '/login') {
      removeAuthUser()
      window.location.replace('/login')
    }
    return Promise.reject(error)
  },
)
