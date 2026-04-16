import axios, { type AxiosInstance } from 'axios'

const API_BASE_URL = '/api'
const TOKEN_KEY = 'ridesketch_token'
const USER_ID_KEY = 'ridesketch_user_id'

export function createHttpClient(timeout = 10000): AxiosInstance {
  const client = axios.create({
    baseURL: API_BASE_URL,
    timeout,
  })

  client.interceptors.request.use((config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_ID_KEY)
        window.location.href = '/'
      }
      return Promise.reject(error)
    }
  )

  return client
}
