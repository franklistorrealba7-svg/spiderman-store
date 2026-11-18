const { get, put } = require("@vercel/blob");

const PRODUCTS_PATH = "data/products.json";
const SALES_PATH = "data/sales.json";

async function readJSON(pathname, fallback) {
  try {
    const r = await get(pathname, { access: "public", useCache: false });
    if (!r) return fallback;
    const text = await new Response(r.stream).text();
    return JSON.parse(text);
  } catch (e) {
    if (e && /not found|404/i.test(String(e.message))) return fallback;
    throw e;
  }
}

async function writeJSON(pathname, data) {
  await put(pathname, JSON.stringify(data), {
    access: "public",
    addRandomSuffix: false,
    allowOverwrite: true,
    contentType: "application/json",
    cacheControlMaxAge: 0,
  });
}

function isAdmin(req) {
  const key = req.headers["x-admin-key"] || "";
  const expected = process.env.ADMIN_KEY || "";
  return expected.length > 0 && key === expected;
}

function json(res, status, body) {
  res.statusCode = status;
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Cache-Control", "no-store");
  res.end(JSON.stringify(body));
}

async function readBody(req) {
  const chunks = [];
  for await (const c of req) chunks.push(c);
  return Buffer.concat(chunks);
}

async function readJSONBody(req) {
  const buf = await readBody(req);
  if (!buf.length) return {};
  return JSON.parse(buf.toString("utf8"));
}

module.exports = { PRODUCTS_PATH, SALES_PATH, readJSON, writeJSON, isAdmin, json, readBody, readJSONBody };
