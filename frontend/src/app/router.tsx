import { createBrowserRouter, Navigate } from 'react-router-dom';
import { MainLayout } from './layouts/MainLayout';
import { AuthLayout } from './layouts/AuthLayout';
import { KanbanBoard } from '@/features/board';
import { ArchitectureDashboard } from '@/features/dashboard';
import { LoginForm } from '@/features/auth';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <KanbanBoard />,
      },
      {
        path: 'board',
        element: <KanbanBoard />,
      },
      {
        path: 'dashboard',
        element: <ArchitectureDashboard />,
      },
      {
        path: '*',
        element: <Navigate to="/" replace />,
      },
    ],
  },
  {
    path: '/auth',
    element: <AuthLayout />,
    children: [
      {
        index: true,
        element: <Navigate to="/auth/login" replace />,
      },
      {
        path: 'login',
        element: <LoginForm />,
      },
    ],
  },
]);
