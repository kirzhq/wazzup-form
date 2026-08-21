import { httpClient } from './httpClient'
import type { Contact } from '../types/contact'

interface ContactsResponse {
  count: number
  data: Contact[]
}

export interface CreateContactRequest {
  name: string
  phone: string
  chatType: 'whatsapp' | 'telegram' | 'viber' | 'max'
  message: string
}

export interface ContactSearch {
  name?: string
  phone?: string
}

export async function getContacts(
  search: ContactSearch = {},
): Promise<Contact[]> {
  const name = search.name?.trim()
  const phone = search.phone?.trim()
  const response =
    await httpClient.get<ContactsResponse>('/contacts', {
      params: name || phone ? { name, phone } : undefined,
    })

  return response.data.data
}

export async function createContact(
  request: CreateContactRequest,
): Promise<Contact> {
  const response = await httpClient.post<Contact>(
    '/contacts',
    request,
  )

  return response.data
}

export async function renameContact(
  contactId: string,
  name: string,
): Promise<Contact> {
  const response = await httpClient.patch<Contact>(
    `/contacts/${encodeURIComponent(contactId)}/name`,
    { name },
  )

  return response.data
}

export interface UpdateContactRequest {
  name: string
  phone: string
  chatType: CreateContactRequest['chatType']
  chatId?: string
}

export async function updateContact(
  contactId: string,
  request: UpdateContactRequest,
): Promise<Contact> {
  const response = await httpClient.patch<Contact>(
    `/contacts/${encodeURIComponent(contactId)}`,
    request,
  )

  return response.data
}

export async function deleteContact(contactId: string): Promise<void> {
  await httpClient.delete(
    `/contacts/${encodeURIComponent(contactId)}`,
  )
}
