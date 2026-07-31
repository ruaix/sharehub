import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);
  return worker.fetch(
    new Request("http://localhost/", { headers: { accept: "text/html" } }),
    { ASSETS: { fetch: async () => new Response("Not found", { status: 404 }) } },
    { waitUntil() {}, passThroughOnException() {} },
  );
}

test("server-renders the ShareHub application shell", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<html lang="zh-CN">/i);
  assert.match(html, /<title>拼友 · 合租管理平台<\/title>/i);
  assert.match(html, /正在安全连接/);
  assert.match(html, /class="auth-loading"/);
  assert.doesNotMatch(html, /Your site is taking shape|codex-preview/i);
});

test("keeps authentication and business calls on the protected API", async () => {
  const dashboard = await readFile(new URL("../app/Dashboard.tsx", import.meta.url), "utf8");
  assert.match(dashboard, /credentials:\s*"include"/);
  assert.match(dashboard, /"X-CSRF-Token":\s*session\.csrfToken/);
  assert.match(dashboard, /\/admin\/memberships/);
  assert.match(dashboard, /\/memberships\/\$\{id\}\/cancel/);
  assert.match(dashboard, /validHttpUrl/);
  assert.doesNotMatch(dashboard, /const services = \[/);
  assert.doesNotMatch(dashboard, /const members = \[/);
});
