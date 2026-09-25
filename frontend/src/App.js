import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { TaskProvider } from './contexts/TaskContext';
import { ProjectProvider } from './contexts/ProjectContext';
import Login from './components/Auth/Login';
import Register from './components/Auth/Register';
import Dashboard from './components/Dashboard/Dashboard';
import Tasks from './components/Tasks/Tasks';
import Projects from './components/Projects/Projects';
import Team from './components/Team/Team';
import SettingsPage from './components/Settings/SettingsPage';
import Layout from './components/Layout/Layout';

function App() {
  return (
    <AuthProvider>
      <ProjectProvider>
        <TaskProvider>
          <Router>
            <div className="min-h-screen bg-gray-50">
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/" element={<Layout />}>
                  <Route index element={<Navigate to="/dashboard" replace />} />
                  <Route path="dashboard" element={<Dashboard />} />
                  <Route path="tasks" element={<Tasks />} />
                  <Route path="projects" element={<Projects />} />
                  <Route path="team" element={<Team />} />
                  <Route path="settings" element={<SettingsPage />} />
                </Route>
              </Routes>
            </div>
          </Router>
        </TaskProvider>
      </ProjectProvider>
    </AuthProvider>
  );
}

export default App;
