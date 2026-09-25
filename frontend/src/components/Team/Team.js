import React from 'react';
import { Users, Settings } from 'lucide-react';

const Team = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Team</h1>
        <p className="text-gray-600">Manage your organization's team members</p>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="text-center py-12">
          <Users className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">Team Management</h3>
          <p className="mt-1 text-sm text-gray-500">Invite and manage team members here.</p>
          <p className="mt-4 text-xs text-gray-400">Coming soon...</p>
        </div>
      </div>
    </div>
  );
};

export default Team;
