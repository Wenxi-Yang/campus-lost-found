const fs = require("fs"),
  path = require("path");
const root = path.resolve(__dirname, "..");
function copy(src, dst) {
  fs.mkdirSync(dst, { recursive: true });
  for (const e of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, e.name),
      d = path.join(dst, e.name);
    if (e.isDirectory()) copy(s, d);
    else fs.copyFileSync(s, d);
  }
}
const src = path.join(root, "frontend", "dist"),
  dst = path.join(root, "backend", "src", "main", "resources", "static");
if (!fs.existsSync(path.join(src, "index.html"))) throw Error("Run frontend npm run build first");
if (!dst.startsWith(root + path.sep)) throw Error("Invalid output path");
fs.rmSync(dst, { recursive: true, force: true });
copy(src, dst);
console.log("Frontend assets copied to Spring Boot static resources.");
