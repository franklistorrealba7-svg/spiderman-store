const { SALES_PATH, readJSON, writeJSON, isAdmin, json, readJSONBody } = require("./_lib");

module.exports = async (req, res) => {
  try {
    if (!isAdmin(req)) return json(res, 401, { error: "Clave de administrador incorrecta" });
    const data = await readJSON(SALES_PATH, { sales: [] });
    if (req.method === "GET") return json(res, 200, data);
    if (req.method === "POST") {
      const s = await readJSONBody(req);
      if (!Array.isArray(s.items) || !s.items.length) return json(res, 400, { error: "La venta no tiene productos" });
      const sale = {
        id: "V" + Date.now().toString(36).toUpperCase(),
        at: s.at || new Date().toISOString(),
        items: s.items.map((it) => ({ sku: String(it.sku || ""), name: String(it.name || ""), qty: Math.max(1, Number(it.qty) || 1), price: Number(it.price) || 0 })),
        method: String(s.method || "Yape"),
        customer: String(s.customer || "").slice(0, 120),
        phone: String(s.phone || "").slice(0, 30),
        note: String(s.note || "").slice(0, 300),
        discount: Number(s.discount) || 0,
        shipping: Number(s.shipping) || 0,
      };
      const subtotal = sale.items.reduce((a, it) => a + it.qty * it.price, 0);
      sale.total = Math.max(0, subtotal - sale.discount + sale.shipping);
      data.sales.unshift(sale);
      await writeJSON(SALES_PATH, data);
      return json(res, 200, { ok: true, sale });
    }
    if (req.method === "DELETE") {
      const id = new URL(req.url, "http://x").searchParams.get("id");
      data.sales = data.sales.filter((s) => s.id !== id);
      await writeJSON(SALES_PATH, data);
      return json(res, 200, { ok: true });
    }
    return json(res, 405, { error: "Método no permitido" });
  } catch (e) {
    return json(res, 500, { error: "Error del servidor", detail: String(e && e.message) });
  }
};
