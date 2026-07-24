import { httpClient } from './httpClient'

export interface SaveApiKeyRequest {
  apiKey: string
}

export interface SaveApiKeyResponse {
  message: string
}

export interface SettingsStatus {
  configured: boolean
}

export async function getSettingsStatus(): Promise<SettingsStatus> {
  const response =
    await httpClient.get<SettingsStatus>('/settings')

  return response.data
}

export async function saveApiKey(
  request: SaveApiKeyRequest,
): Promise<SaveApiKeyResponse> {
  const response = await httpClient.put<SaveApiKeyResponse>(
    '/settings/api-key',
    request,
  )

  return response.data
}
