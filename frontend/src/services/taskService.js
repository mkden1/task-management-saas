import api from './api';

const taskService = {
  getTasks: async () => {
    const response = await api.get('/tasks');
    return response.data;
  },

  getMyTasks: async () => {
    const response = await api.get('/tasks/my-tasks');
    return response.data;
  },

  createTask: async (taskData) => {
    const response = await api.post('/tasks', taskData);
    return response.data;
  },

  updateTask: async (id, taskData) => {
    const response = await api.put(`/tasks/${id}`, taskData);
    return response.data;
  },

  deleteTask: async (id) => {
    await api.delete(`/tasks/${id}`);
  }
};

export default taskService;
