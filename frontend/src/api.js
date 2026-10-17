import axios from "axios";
const api = axios.create({
  baseURL: "/api",
  timeout: 15000,
  headers: { "X-Requested-With": "campus" },
});
api.interceptors.response.use(
  (r) => r.data,
  (e) => {
    const error = new Error(
      (e.response && e.response.data && e.response.data.message) || "网络连接失败，请确认后端已启动"
    );
    error.status = e.response && e.response.status;
    return Promise.reject(error);
  }
);
export default api;
