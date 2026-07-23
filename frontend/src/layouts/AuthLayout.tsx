import type { ReactNode } from 'react'

interface AuthLayoutProps {
  children: ReactNode
}

export function AuthLayout({
  children,
}: AuthLayoutProps) {
  return (
    <main
      className="
        min-h-screen
        bg-gradient-to-br
        from-slate-100
        via-white
        to-violet-100
        px-5 py-10
      "
    >
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] max-w-lg items-center">
        <div className="w-full">
          {children}
        </div>
      </div>
    </main>
  )
}