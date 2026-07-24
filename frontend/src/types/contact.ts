export interface ContactData {
  chatType: string
  chatId: string
  username: string | null
  phone: string | null
}

export interface Contact {
  id: string
  responsibleUserId: string
  name: string
  contactData: ContactData[]
  uri: string | null
}
