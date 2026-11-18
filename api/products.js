const { PRODUCTS_PATH, readJSON, writeJSON, isAdmin, json, readJSONBody } = require("./_lib");
const seed = require("../data/seed.json");

module.exports = async (req, res) => {
  try {
    if (req.method === "GET") {
      const data = await readJSON(PRODUCTS_PATH, null);
      const products = data && Array.isArray(data.products) ? data.products : seed.products;
      const all = /all=1/.test(req.url || "") && isAdmin(req);
      const list = all ? products : products.filter((p) => p.active !== false);
      res.setHeader("Cache-Control", "public, s-maxage=30, stale-while-revalidate=120");
      return json(res, 200, { products: list, updatedAt: data ? data.updatedAt : null, source: data ? "blob" : "seed" });
    }
    if (req.method === "POST" || req.method === "PUT") {
      if (!isAdmin(req)) return json(res, 401, { error: "Clave de administrador incorrecta" });
      const body = await readJSONBody(req);
      if (!Array.isArray(body.products)) return json(res, 400, { error: "Formato inválido: se espera { products: [] }" });
      const clean = body.products.map((p, i) => ({
        id: String(p.id || p.sku || "p" + Date.now() + i),
        sku: String(p.sku || "").trim(),
        brand: String(p.brand || "").trim(),
        name: String(p.name || "").trim(),
        sn: String(p.sn || "").trim(),
        g: p.g === "Hombre" ? "Hombre" : p.g === "Unisex" ? "Unisex" : "Mujer",
        set: !!p.set,
        was: p.was ? Number(p.was) : null,
        now: Number(p.now) || 0,
        f: String(p.f || "").trim(),
        no: String(p.no || "").trim(),
        desc: String(p.desc || "").trim(),
        flag: ["lim", "last", "new"].includes(p.flag) ? p.flag : "",
        img: String(p.img || "").trim(),
        hero: String(p.hero || "").trim(),
        gallery: Array.isArray(p.gallery) ? p.gallery.map(String).slice(0, 6) : [],
        active: p.active !== false,
        order: Number.isFinite(p.order) ? p.order : i,
        createdAt: p.createdAt || new Date().toISOString(),
      }));
      await writeJSON(PRODUCTS_PATH, { products: clean, updatedAt: new Date().toISOString() });
      return json(res, 200, { ok: true, count: clean.length });
    }
    return json(res, 405, { error: "Método no permitido" });
  } catch (e) {
    return json(res, 500, { error: "Error del servidor", detail: String(e && e.message) });
  }
};
