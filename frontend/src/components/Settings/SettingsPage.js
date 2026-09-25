import React from 'react';
import { Settings } from 'lucide-react';

const SettingsPage = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-600">Manage your account and organization settings</p>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="text-center py-12">
          <Settings className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">Settings</h3>
          <p className="mt-1 text-sm text-gray-500">Configure your preferences here.</p>
          <p className="mt-4 text-xs text-gray-400">Coming soon...</p>
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;
