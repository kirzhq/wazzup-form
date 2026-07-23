import { httpClient } from './httpClient'

export interface SaveApiKeyRequest {
  apiKey: string
}

export interface SaveApiKeyResponse {
  message: string
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