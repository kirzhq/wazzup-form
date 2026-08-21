import { httpClient } from './httpClient'

export interface PartnerStatus {
  configured: boolean
  connected: boolean
}

export interface PendingContact {
  id: string
  chatType: string
  chatId: string
  name: string | null
  username: string | null
  phone: string | null
  source: string
  updatedAt: string
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

export async function getPendingContacts(): Promise<PendingContact[]> {
  const response = await httpClient.get<PendingContact[]>(
    '/partner/pending-contacts',
  )
  return response.data
}

export async function approvePendingContact(
  id: string,
  name: string,
): Promise<void> {
  await httpClient.post(`/partner/pending-contacts/${id}/approve`, { name })
}

export async function dismissPendingContact(id: string): Promise<void> {
  await httpClient.post(`/partner/pending-contacts/${id}/dismiss`)
}
