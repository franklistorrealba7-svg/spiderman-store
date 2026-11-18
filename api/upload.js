const { put } = require("@vercel/blob");
const { isAdmin, json, readBody } = require("./_lib");

const TYPES = { "image/jpeg": "jpg", "image/png": "png", "image/webp": "webp" };

module.exports = async (req, res) => {
  try {
    if (req.method !== "POST") return json(res, 405, { error: "Método no permitido" });
    if (!isAdmin(req)) return json(res, 401, { error: "Clave de administrador incorrecta" });
    const type = (req.headers["content-type"] || "").split(";")[0].trim();
    if (!TYPES[type]) return json(res, 400, { error: "Formato no permitido. Usa JPG, PNG o WebP." });
    const buf = await readBody(req);
    if (!buf.length) return json(res, 400, { error: "Archivo vacío" });
    if (buf.length > 4 * 1024 * 1024) return json(res, 413, { error: "La imagen supera 4 MB. Redúcela e inténtalo de nuevo." });
    const raw = new URL(req.url, "http://x").searchParams.get("name") || "imagen";
    const base = raw.toLowerCase().replace(/\.[a-z0-9]+$/, "").replace(/[^a-z0-9-]+/g, "-").replace(/^-+|-+$/g, "").slice(0, 40) || "imagen";
    const pathname = `productos/${base}-${Date.now().toString(36)}.${TYPES[type]}`;
    const blob = await put(pathname, buf, { access: "public", contentType: type, addRandomSuffix: false, cacheControlMaxAge: 31536000 });
    return json(res, 200, { ok: true, url: blob.url, pathname: blob.pathname });
  } catch (e) {
    return json(res, 500, { error: "Error al subir la imagen", detail: String(e && e.message) });
  }
};
