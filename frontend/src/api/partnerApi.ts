import { httpClient } from './httpClient'

export interface PartnerStatus {
  configured: boolean
  connected: boolean
}

interface OauthStartResponse {
  authorizationUrl: string
}

export async function getPartnerStatus(): Promise<PartnerStatus> {
  const response = await httpClient.get<PartnerStatus>('/partner/status')
  return response.data
}

export async function startPartnerOauth(): Promise<string> {
  const response = await httpClient.get<OauthStartResponse>(
    '/partner/oauth/start',
  )
  return response.data.authorizationUrl
}

export async function completePartnerOauth(
  code: string,
  state: string,
): Promise<void> {
  await httpClient.post('/partner/oauth/complete', { code, state })
}
