export function normalizePhone(value: string): string {
  const digits = value.replace(/\D/g, '')

  if (digits.length === 11 && digits.startsWith('8')) {
    return `7${digits.slice(1)}`
  }

  return digits
}

export function formatPhone(value: string): string {
  let digits = normalizePhone(value)

  if (!digits) {
    return ''
  }

  if (digits.length <= 10 && !digits.startsWith('7')) {
    digits = `7${digits}`
  }

  digits = digits.slice(0, 11)

  if (!digits.startsWith('7')) {
    return `+${digits.replace(/(\d{3})(?=\d)/g, '$1 ').trim()}`
  }

  const country = digits.slice(0, 1)
  const area = digits.slice(1, 4)
  const first = digits.slice(4, 7)
  const second = digits.slice(7, 9)
  const third = digits.slice(9, 11)

  let formatted = `+${country}`

  if (area) {
    formatted += ` (${area}`
  }
  if (area.length === 3) {
    formatted += ')'
  }
  if (first) {
    formatted += ` ${first}`
  }
  if (second) {
    formatted += `-${second}`
  }
  if (third) {
    formatted += `-${third}`
  }

  return formatted
}
