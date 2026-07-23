import type { ReactNode } from 'react'

interface CardProps {
  children: ReactNode
  className?: string
}

export function Card({
  children,
  className = '',
}: CardProps) {
  return (
    <section
      className={`
        rounded-3xl border border-slate-200
        bg-white p-8
        shadow-[0_20px_60px_rgba(15,23,42,0.08)]
        ${className}
      `}
    >
      {children}
    </section>
  )
}