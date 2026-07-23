import {
  Navigate,
  Route,
  Routes,
} from 'react-router-dom'

import { ApiKeyPage } from '../pages/ApiKeyPage'
import { ContactsPage } from '../pages/ContactsPage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ProtectedRoute } from './ProtectedRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/"
        element={<Navigate to="/settings" replace />}
      />

      <Route
        path="/settings"
        element={<ApiKeyPage />}
      />

      <Route
        path="/login"
        element={<LoginPage />}
      />

      <Route
        path="/contacts"
        element={
          <ProtectedRoute>
            <ContactsPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="*"
        element={<NotFoundPage />}
      />
    </Routes>
  )
}