import type {
  InputHTMLAttributes,
  ReactNode,
} from 'react'

interface InputProps
  extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  rightElement?: ReactNode
}

export function Input({
  label,
  error,
  rightElement,
  id,
  className = '',
  ...props
}: InputProps) {
  return (
    <div className="flex flex-col gap-2">
      <label
        htmlFor={id}
        className="text-sm font-semibold text-slate-800"
      >
        {label}
      </label>

      <div className="relative">
        <input
          {...props}
          id={id}
          className={`
            h-12 w-full rounded-xl border bg-white px-4
            text-slate-900 outline-none transition
            placeholder:text-slate-400
            focus:ring-4
            disabled:cursor-not-allowed
            disabled:bg-slate-100
            ${
              error
                ? 'border-red-400 focus:border-red-500 focus:ring-red-100'
                : 'border-slate-300 focus:border-violet-500 focus:ring-violet-100'
            }
            ${rightElement ? 'pr-24' : ''}
            ${className}
          `}
        />

        {rightElement && (
          <div className="absolute inset-y-0 right-3 flex items-center">
            {rightElement}
          </div>
        )}
      </div>

      {error && (
        <p className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}