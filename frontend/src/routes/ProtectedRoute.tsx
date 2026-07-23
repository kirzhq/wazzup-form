import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'

import { getAuthUser } from '../utils/authStorage'

interface ProtectedRouteProps {
  children: ReactNode
}

export function ProtectedRoute({
  children,
}: ProtectedRouteProps) {
  const user = getAuthUser()

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
      />
    )
  }

  return children
}