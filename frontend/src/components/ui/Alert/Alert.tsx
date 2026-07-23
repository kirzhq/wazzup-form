import type { ReactNode } from 'react'

type AlertVariant = 'error' | 'success' | 'info'

interface AlertProps {
  variant: AlertVariant
  children: ReactNode
}

const variantClasses: Record<AlertVariant, string> = {
  error:
    'border-red-200 bg-red-50 text-red-700',
  success:
    'border-emerald-200 bg-emerald-50 text-emerald-700',
  info:
    'border-blue-200 bg-blue-50 text-blue-700',
}

export function Alert({
  variant,
  children,
}: AlertProps) {
  return (
    <div
      className={`
        rounded-xl border px-4 py-3
        text-sm leading-6
        ${variantClasses[variant]}
      `}
      role={variant === 'error' ? 'alert' : 'status'}
    >
      {children}
    </div>
  )
}