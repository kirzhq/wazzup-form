import type {
  ButtonHTMLAttributes,
  ReactNode,
} from 'react'

interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode
  isLoading?: boolean
}

export function Button({
  children,
  isLoading = false,
  disabled,
  className = '',
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      disabled={disabled || isLoading}
      className={`
        inline-flex min-h-12 items-center justify-center
        rounded-xl bg-violet-600 px-5
        font-semibold text-white
        transition
        hover:bg-violet-700
        focus:outline-none
        focus:ring-4
        focus:ring-violet-200
        disabled:cursor-not-allowed
        disabled:opacity-60
        ${className}
      `}
    >
      {isLoading ? 'Загрузка...' : children}
    </button>
  )
}
