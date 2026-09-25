import api from './api';

const organizationService = {
  createOrganization: async (organizationData) => {
    const response = await api.post('/organizations/create', organizationData);
    return response.data;
  },

  getOrganizationBySlug: async (slug) => {
    const response = await api.get(`/organizations/${slug}`);
    return response.data;
  },

  getAllOrganizations: async () => {
    const response = await api.get('/organizations');
    return response.data;
  }
};

export default organizationService;
