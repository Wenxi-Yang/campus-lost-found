const path = require("path");
module.exports = {
  publicPath: "./",
  productionSourceMap: false,
  devServer: {
    host: "127.0.0.1",
    proxy: { "/api": { target: "http://localhost:8081", changeOrigin: false } },
  },
};
