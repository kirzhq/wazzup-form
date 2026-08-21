import axios from 'axios'
import { removeAuthUser } from '../utils/authStorage'

export const httpClient = axios.create({
  baseURL: '/api',
  timeout: 120_000,
  headers: {
    'Content-Type': 'application/json',
  },
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
