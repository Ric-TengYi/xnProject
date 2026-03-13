import axios from 'axios';
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { message } from 'antd';

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    if (res.code !== 200) {
      message.error(res.message || 'Error');
      if (res.code === 401) {
        localStorage.removeItem('token');
        window.location.href = '/login';
      }
      return Promise.reject(new Error(res.message || 'Error'));
    }
    return res;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    message.error(error.response?.data?.message || error.message || 'Network Error');
    return Promise.reject(error);
  }
);

export interface CustomResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

const http = {
  get: <T = any>(url: string, config?: any): Promise<CustomResponse<T>> => request.get(url, config) as any,
  post: <T = any>(url: string, data?: any, config?: any): Promise<CustomResponse<T>> => request.post(url, data, config) as any,
  put: <T = any>(url: string, data?: any, config?: any): Promise<CustomResponse<T>> => request.put(url, data, config) as any,
  delete: <T = any>(url: string, config?: any): Promise<CustomResponse<T>> => request.delete(url, config) as any,
};

export default http;
