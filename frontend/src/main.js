import Vue from "vue";
import ElementUI from "element-ui";
import "element-ui/lib/theme-chalk/index.css";
import VueRouter from "vue-router";
import App from "./App.vue";
import "./style.css";
Vue.use(ElementUI);
Vue.use(VueRouter);
Vue.config.productionTip = false;
const router = new VueRouter({
  routes: [
    { path: "/", name: "home" },
    { path: "/notices", name: "notices" },
    { path: "/login", name: "login" },
    { path: "/register", name: "register" },
    { path: "/mine", name: "mine" },
    { path: "/admin", name: "admin" },
  ],
});
new Vue({ router, render: (h) => h(App) }).$mount("#app");
