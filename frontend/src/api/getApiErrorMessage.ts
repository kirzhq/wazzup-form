import axios from 'axios'

interface ApiErrorResponse {
  message?: string
  error?: string
}

export function getApiErrorMessage(
  error: unknown,
  fallbackMessage = 'Произошла ошибка',
): string {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return fallbackMessage
  }

  return (
    error.response?.data?.message ??
    error.response?.data?.error ??
    fallbackMessage
  )
}