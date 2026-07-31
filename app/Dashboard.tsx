"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Image from "next/image";

type View = "总览" | "共享项目" | "用户管理" | "订单记录" | "系统设置";
type Session = { user: { id: number; name: string; email: string; role: "ADMIN" | "MEMBER" }; csrfToken: string };
type Registration = { id: number; name: string; email: string; createdAt: string };
type Service = { id: number; name: string; category: string; seatTotal: number; seatUsed: number; monthlyPriceCents: number; renewAt?: string; status: string; proxy?: { probeUrl?: string; nodeTotal: number } };
type Membership = { id: number; userId: number; userName?: string; userEmail?: string; serviceId: number; serviceName: string; category: string; startedAt: string; expiresAt: string; priceCents: number; status: string };
type User = { id: number; name: string; email: string; role: string; status: string };
type Order = { id: number; serviceId: number; membershipId: number; type: string; amountCents: number; status: string; periodStart: string; periodEnd: string; createdAt: string };
const API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8787/api";
const categoryName: Record<string, string> = { STREAMING: "流媒体", PROXY: "梯子订阅", SUBSCRIPTION: "软件订阅", AI: "AI 工具", OTHER: "其他" };
const categoryColor: Record<string, string> = { STREAMING: "#ef4444", PROXY: "#667eea", SUBSCRIPTION: "#0ea5e9", AI: "#10a37f", OTHER: "#64748b" };

export default function Dashboard() {
  const [session, setSession] = useState<Session | null>(null);
  const [checking, setChecking] = useState(true);
  useEffect(() => {
    fetch(`${API}/auth/me`, { credentials: "include" })
      .then((res) => res.ok ? res.json() : Promise.reject())
      .then(setSession)
      .catch(() => setSession(null))
      .finally(() => setChecking(false));
  }, []);
  if (checking) return <div className="auth-loading"><span className="brand-mark">拼</span><p>正在安全连接…</p></div>;
  if (!session) return <AuthScreen onLogin={setSession} />;
  return <DashboardApp session={session} onLogout={() => setSession(null)} />;
}

function DashboardApp({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [view, setView] = useState<View>("总览");
  const [query, setQuery] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [showAssign, setShowAssign] = useState(false);
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [registrationOpen, setRegistrationOpen] = useState(false);
  const [services, setServices] = useState<Service[]>([]);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [notice, setNotice] = useState("");
  const isAdmin = session.user.role === "ADMIN";
  const filtered = useMemo(() => services.filter((item) => item.name.toLowerCase().includes(query.trim().toLowerCase())), [query, services]);
  const showError = useCallback((error: unknown) => setNotice(error instanceof Error ? error.message : "加载失败，请稍后重试"), []);
  const loadServices = useCallback(() => { fetch(`${API}/services`, { credentials: "include" }).then(readJson).then(setServices).catch(showError); }, [showError]);
  const loadMemberships = useCallback(() => { fetch(`${API}/${isAdmin ? "admin/" : ""}memberships`, { credentials: "include" }).then(readJson).then(setMemberships).catch(showError); }, [isAdmin, showError]);
  const loadOrders = useCallback(() => { fetch(`${API}/${isAdmin ? "admin/" : ""}orders`, { credentials: "include" }).then(readJson).then(setOrders).catch(showError); }, [isAdmin, showError]);
  useEffect(() => { loadServices(); loadMemberships(); }, [loadServices, loadMemberships]);
  useEffect(() => {
    if (view === "用户管理" && isAdmin) {
      fetch(`${API}/admin/registrations`, { credentials: "include" })
        .then(readJson).then(setRegistrations).catch(showError);
      fetch(`${API}/admin/users`, { credentials: "include" })
        .then(readJson).then(setUsers).catch(showError);
    }
    if (view === "订单记录") loadOrders();
  }, [view, isAdmin, loadOrders, showError]);
  useEffect(() => {
    if (view === "系统设置" && isAdmin) {
      fetch(`${API}/admin/settings`, { credentials: "include" })
        .then((res) => res.ok ? res.json() : Promise.reject())
        .then((data) => setRegistrationOpen(data.registrationEnabled))
        .catch(() => undefined);
    }
  }, [view, isAdmin]);
  const review = async (id: number, action: "approve" | "reject") => {
    const res = await fetch(`${API}/admin/registrations/${id}/${action}`, {
      method: "POST", credentials: "include", headers: { "X-CSRF-Token": session.csrfToken },
    });
    if (res.ok) setRegistrations((rows) => rows.filter((row) => row.id !== id));
    else showError(await apiError(res));
  };
  const logout = async () => {
    await fetch(`${API}/auth/logout`, { method: "POST", credentials: "include", headers: { "X-CSRF-Token": session.csrfToken } });
    onLogout();
  };
  const toggleRegistration = async () => {
    const next = !registrationOpen;
    const res = await fetch(`${API}/admin/settings/registration`, {
      method: "PATCH", credentials: "include",
      headers: { "Content-Type": "application/json", "X-CSRF-Token": session.csrfToken },
      body: JSON.stringify({ enabled: next }),
    });
    if (res.ok) setRegistrationOpen(next);
    else showError(await apiError(res));
  };
  const mutate = async (path: string, method: string, body?: unknown) => {
    const res = await fetch(`${API}${path}`, {
      method, credentials: "include",
      headers: { ...(body ? { "Content-Type": "application/json" } : {}), "X-CSRF-Token": session.csrfToken },
      body: body ? JSON.stringify(body) : undefined,
    });
    if (!res.ok) throw await apiError(res);
    return res.status === 204 ? null : res.json();
  };

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">拼</span><span>拼友</span></div>
        <nav>
          {(["总览", "共享项目", "用户管理", "订单记录", "系统设置"] as View[]).map((item, index) => (
            <button className={view === item ? "nav-item active" : "nav-item"} onClick={() => setView(item)} key={item}>
              <span className="nav-icon">{["⌂", "◫", "♙", "▤", "⚙"][index]}</span>{item}
              {item === "用户管理" && <em>24</em>}
            </button>
          ))}
        </nav>
        <div className="sidebar-card">
          <div className="spark">✦</div>
          <strong>运营小贴士</strong>
          <p>及时更新订阅链接，能减少 80% 的重复咨询。</p>
          <button>查看管理指南</button>
        </div>
        <div className="profile">
          <span className="avatar admin">管</span>
          <div><strong>{session.user.name}</strong><small>{session.user.email}</small></div><button className="logout" onClick={logout}>退出</button>
        </div>
      </aside>

      <section className="content">
        <header className="topbar">
          <div className="search"><span>⌕</span><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="搜索项目、用户或订单..." /><kbd>⌘ K</kbd></div>
          <div className="top-actions"><button aria-label="消息">♢<i /></button>{isAdmin && <button className="primary" onClick={() => setShowCreate(true)}>＋ 创建合租</button>}</div>
        </header>

        <div className="page">
          {notice && <div className="form-message" role="alert">{notice}<button onClick={() => setNotice("")}>×</button></div>}
          <div className="welcome">
            <div><p>星期五，7月31日</p><h1>{view === "总览" ? `下午好，${session.user.name}` : view}</h1><span>{view === "总览" ? "所有共享服务都在平稳运行，一切尽在掌握。" : "集中管理信息，保持每一项服务清晰可控。"}</span></div>
            <button className="ghost">导出报表</button>
          </div>

          {view === "总览" && <>
            <section className="stats">
              <Stat label="本月收入" value="¥2,864.00" delta="+12.8%" icon="¥" tone="mint" />
              <Stat label="活跃合租" value="12" delta="+2 个" icon="◈" tone="blue" />
              <Stat label="合租用户" value="24" delta="+4 人" icon="♙" tone="violet" />
              <Stat label="待处理" value="3" delta="需要关注" icon="!" tone="amber" warning />
            </section>

            <section className="panel">
              <div className="panel-head"><div><h2>共享项目</h2><p>查看席位使用与续费状态</p></div><button onClick={() => setView("共享项目")}>查看全部 →</button></div>
              <div className="service-grid">
                {filtered.map((service) => <ServiceCard key={service.id} service={service} />)}
              </div>
            </section>

            <section className="bottom-grid">
              <div className="panel members">
                <div className="panel-head"><div><h2>最近加入</h2><p>新成员与订阅状态</p></div><button onClick={() => setView("用户管理")}>用户管理 →</button></div>
                <MemberTable rows={memberships.slice(0, 3)} />
              </div>
              <div className="panel activity">
                <div className="panel-head"><div><h2>近期动态</h2><p>过去 7 天</p></div></div>
                {[
                  ["林小满 完成续费", "Netflix 高级版 · ¥28.00", "12 分钟前", "✓"],
                  ["订阅链接已更新", "CloudLink Pro · 林小满", "2 小时前", "↗"],
                  ["新增一名成员", "ChatGPT Plus", "昨天", "+"],
                ].map((x) => <div className="activity-row" key={x[0]}><span>{x[3]}</span><div><strong>{x[0]}</strong><small>{x[1]}</small></div><time>{x[2]}</time></div>)}
              </div>
            </section>
          </>}

          {view === "共享项目" && <section className="panel list-page"><div className="panel-head"><div><h2>全部共享项目</h2><p>管理共享账号与梯子订阅，探针作为梯子的节点状态页</p></div>{isAdmin && <button className="primary small" onClick={() => setShowCreate(true)}>＋ 新建项目</button>}</div><div className="service-grid all">{filtered.map((s) => <ServiceCard key={s.id} service={s} />)}</div></section>}
          {view === "用户管理" && <section className="panel list-page"><div className="panel-head"><div><h2>租户与租期</h2><p>每一种服务拥有独立租期，退租只影响选中的服务</p></div>{isAdmin && <button className="primary small" onClick={() => setShowAssign(true)}>＋ 分配服务</button>}</div>{isAdmin && <div className="approval-box"><div className="approval-title"><strong>待审批申请</strong><span>{registrations.length}</span></div>{registrations.length === 0 ? <p className="approval-empty">目前没有待审批的注册申请</p> : registrations.map((row) => <div className="approval-row" key={row.id}><span className="avatar">{row.name.slice(0, 1)}</span><div><strong>{row.name}</strong><small>{row.email} · {new Date(row.createdAt).toLocaleDateString("zh-CN")}</small></div><button className="reject" onClick={() => review(row.id, "reject")}>拒绝</button><button className="approve" onClick={() => review(row.id, "approve")}>通过</button></div>)}</div>}<MemberTable rows={memberships} onCancel={isAdmin ? async (id) => { try { await mutate(`/admin/memberships/${id}/cancel`, "POST"); await loadMemberships(); } catch (e) { showError(e); } } : undefined} /></section>}
          {view === "订单记录" && (orders.length ? <section className="panel list-page"><div className="panel-head"><div><h2>订单记录</h2><p>新增租期和续费会自动形成订单</p></div></div><div className="table">{orders.map((o) => <div className="table-row" key={o.id}><div className="member-name"><strong>{o.type === "RENEWAL" ? "续费" : "新租"}</strong><small>{new Date(o.createdAt).toLocaleString("zh-CN")}</small></div><span className="plan">项目 #{o.serviceId}</span><strong className="amount">¥{(o.amountCents / 100).toFixed(2)}</strong><span className="status">● {o.status}</span></div>)}</div></section> : <Empty title="订单记录" text="新增租期和续费后，订单会自动显示在这里。" action="暂无订单" />)}
          {view === "系统设置" && <section className="panel settings-page"><div className="panel-head"><div><h2>系统设置</h2><p>控制公开功能与平台安全策略</p></div></div><div className="setting-row"><div className="setting-icon">♙</div><div><strong>开放用户注册</strong><p>{registrationOpen ? "登录页显示注册入口，新用户可以提交审批申请。" : "注册入口已隐藏，注册接口也已在服务端禁用。"}</p></div><button type="button" role="switch" aria-checked={registrationOpen} className={registrationOpen ? "switch on" : "switch"} onClick={toggleRegistration}><i /></button></div><div className="setting-security"><span>✓</span><p>该设置由后端强制执行，无法通过修改前端或直接请求接口绕过；每次修改都会进入审计日志。</p></div></section>}
        </div>
      </section>

      {showCreate && <ServiceForm onClose={() => setShowCreate(false)} onSubmit={async (body) => { await mutate("/admin/services", "POST", body); setShowCreate(false); await loadServices(); }} onError={showError} />}
      {showAssign && <AssignForm users={users} services={services} onClose={() => setShowAssign(false)} onSubmit={async (body) => { await mutate("/admin/memberships", "POST", body); setShowAssign(false); await loadMemberships(); await loadServices(); }} onError={showError} />}
    </main>
  );
}

function AuthScreen({ onLogin }: { onLogin: (session: Session) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [captcha, setCaptcha] = useState<{ id: string; image: string } | null>(null);
  const [registrationEnabled, setRegistrationEnabled] = useState(false);
  useEffect(() => {
    fetch(`${API}/public/registration-status`)
      .then((res) => res.ok ? res.json() : Promise.reject())
      .then((data) => {
        setRegistrationEnabled(Boolean(data.enabled));
        if (!data.enabled) setMode("login");
      })
      .catch(() => setRegistrationEnabled(false));
  }, []);
  const refreshCaptcha = () => {
    fetch(`${API}/auth/captcha`, { credentials: "include" })
      .then((res) => res.ok ? res.json() : Promise.reject())
      .then(setCaptcha)
      .catch(() => setMessage("验证码暂时无法加载"));
  };
  useEffect(() => { if (mode === "login") refreshCaptcha(); }, [mode]);
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBusy(true); setMessage("");
    const form = new FormData(event.currentTarget);
    const payload = { ...Object.fromEntries(form.entries()), ...(mode === "login" ? { captchaId: captcha?.id } : {}) };
    try {
      const res = await fetch(`${API}/auth/${mode}`, { method: "POST", credentials: "include", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message);
      if (mode === "login") onLogin(data);
      else { setMessage(data.message); event.currentTarget.reset(); }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "请求失败，请稍后重试");
      if (mode === "login") refreshCaptcha();
    } finally { setBusy(false); }
  };
  return <main className="auth-page"><section className="auth-brand"><div><span className="brand-mark">拼</span><b>拼友</b></div><h1>合租管理，<br />清楚一点就够了。</h1><p>共享账号、订阅链接、成员租期与节点状态，集中放在一个安全、清爽的工作台。</p><div className="security-note"><span>✓</span><div><strong>安全优先</strong><small>审批制注册 · Redis 防爆破 · 敏感数据脱敏</small></div></div></section><section className="auth-card"><div className={registrationEnabled ? "auth-tabs" : "auth-tabs single"}><button className={mode === "login" ? "active" : ""} onClick={() => { setMode("login"); setMessage(""); }}>登录</button>{registrationEnabled && <button className={mode === "register" ? "active" : ""} onClick={() => { setMode("register"); setMessage(""); }}>申请注册</button>}</div><h2>{mode === "login" ? "欢迎回来" : "创建申请"}</h2><p>{mode === "login" ? "登录后进入你的合租工作台" : "提交后需等待管理员审批"}</p><form onSubmit={submit}>{mode === "register" && <label>姓名<input name="name" required minLength={2} maxLength={40} autoComplete="name" placeholder="你的姓名" /></label>}<label>邮箱<input name="email" required type="email" autoComplete="email" placeholder="name@example.com" /></label><label>密码<input name="password" required type="password" minLength={10} maxLength={128} autoComplete={mode === "login" ? "current-password" : "new-password"} placeholder="至少 10 位" /></label>{mode === "register" && <small className="password-hint">需包含大小写字母、数字和符号</small>}{mode === "login" && <label>验证码<div className="captcha-field"><input name="captchaAnswer" required minLength={5} maxLength={5} autoComplete="off" placeholder="输入图中字符" /><button type="button" onClick={refreshCaptcha} title="换一张">{captcha ? <Image src={captcha.image} alt="登录验证码" width={120} height={40} unoptimized /> : <span>加载中…</span>}</button></div></label>}{message && <div className="form-message">{message}</div>}<button className="primary auth-submit" disabled={busy || (mode === "login" && !captcha)}>{busy ? "请稍候…" : mode === "login" ? "安全登录" : "提交注册申请"}</button></form><small className="privacy-copy">验证码一次有效，登录行为会记录安全审计日志。</small></section></main>;
}

function Stat({ label, value, delta, icon, tone, warning = false }: { label: string; value: string; delta: string; icon: string; tone: string; warning?: boolean }) {
  return <article className="stat"><div className={`stat-icon ${tone}`}>{icon}</div><div><p>{label}</p><h3>{value}</h3><span className={warning ? "warn" : ""}>{delta}</span></div></article>;
}

function ServiceCard({ service }: { service: Service }) {
  const percent = Math.min(100, Math.round((service.seatUsed / service.seatTotal) * 100));
  const color = categoryColor[service.category] || categoryColor.OTHER;
  return <article className="service-card"><div className="service-top"><span className="service-logo" style={{ background: color }}>{service.name.slice(0, 2).toUpperCase()}</span><span className={service.status === "ACTIVE" ? "status" : "status warning"}>● {service.status === "ACTIVE" ? "运行中" : "已暂停"}</span></div><h3>{service.name}</h3><p>{categoryName[service.category] || "其他"}</p><div className="capacity"><span>{service.category === "PROXY" ? "有效订阅" : "席位使用"}</span><b>{service.seatUsed} / {service.seatTotal}</b></div><div className="progress"><i style={{ width: `${percent}%`, background: color }} /></div>{service.proxy?.probeUrl && <div className="probe-row"><span><i /> 节点状态</span><strong>{service.proxy.nodeTotal} 个节点</strong><a href={service.proxy.probeUrl} target="_blank" rel="noreferrer">查看探针 →</a></div>}<div className="service-foot"><span><small>每位 / 月</small><strong>¥{(service.monthlyPriceCents / 100).toFixed(2)}</strong></span><span><small>下次续费</small><strong>{service.renewAt ? new Date(service.renewAt).toLocaleDateString("zh-CN") : "未设置"}</strong></span></div></article>;
}

function MemberTable({ rows, onCancel }: { rows: Membership[]; onCancel?: (id: number) => void }) {
  return <div className="table">{rows.length === 0 && <p className="approval-empty">暂无租期记录</p>}{rows.map((m) => <div className="table-row" key={m.id}><span className="avatar">{(m.userName || "我").slice(0, 1)}</span><div className="member-name"><strong>{m.userName || "我的租期"}</strong><small>{m.userEmail || new Date(m.expiresAt).toLocaleDateString("zh-CN") + " 到期"}</small></div><span className="plan">{m.serviceName}</span><strong className="amount">¥{(m.priceCents / 100).toFixed(2)}</strong><span className={m.status === "ACTIVE" ? "status" : "status warning"}>● {membershipStatus(m.status)}</span>{onCancel && <button title="退租" onClick={() => window.confirm(`确认将 ${m.userName} 从 ${m.serviceName} 退租？`) && onCancel(m.id)}>退租</button>}</div>)}</div>;
}

function Empty({ title, text, action }: { title: string; text: string; action: string }) {
  return <section className="panel empty"><span>◇</span><h2>{title}</h2><p>{text}</p><button className="primary small">{action}</button></section>;
}

function ServiceForm({ onClose, onSubmit, onError }: { onClose: () => void; onSubmit: (body: unknown) => Promise<void>; onError: (error: unknown) => void }) {
  const [category, setCategory] = useState("STREAMING");
  const [busy, setBusy] = useState(false);
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const name = String(form.get("name") || "").trim();
    const seatTotal = Number(form.get("seatTotal"));
    const priceYuan = Number(form.get("price"));
    if (name.length < 2 || name.length > 80) return onError(new Error("项目名称需为 2–80 个字符"));
    if (!Number.isInteger(seatTotal) || seatTotal < 1 || seatTotal > 10000) return onError(new Error("席位数需为 1–10000 的整数"));
    if (!Number.isFinite(priceYuan) || priceYuan < 0 || priceYuan > 1000000) return onError(new Error("月费金额格式不正确"));
    const probeUrl = String(form.get("probeUrl") || "").trim();
    if (probeUrl && !validHttpUrl(probeUrl)) return onError(new Error("探针链接必须是有效的 HTTP(S) 地址"));
    const body = {
      name, category, seatTotal, monthlyPriceCents: Math.round(priceYuan * 100),
      accountName: String(form.get("accountName") || "").trim() || null,
      secret: String(form.get("secret") || "") || null,
      renewAt: form.get("renewAt") ? `${String(form.get("renewAt"))}:00` : null,
      notes: String(form.get("notes") || "").trim() || null,
      proxy: category === "PROXY" ? { probeUrl: probeUrl || null, panelUrl: null, nodeTotal: Number(form.get("nodeTotal") || 0), trafficLimitGb: null, deviceLimit: null } : null,
    };
    try { setBusy(true); await onSubmit(body); } catch (error) { onError(error); } finally { setBusy(false); }
  };
  return <div className="modal-backdrop" onMouseDown={onClose}><div className="modal form-modal" onMouseDown={(e) => e.stopPropagation()}><button className="close" onClick={onClose}>×</button><h2>创建合租项目</h2><p>敏感账号信息由后端加密保存，不会出现在项目列表。</p><form onSubmit={submit}><label>项目名称<input name="name" required minLength={2} maxLength={80} placeholder="例如 Netflix 高级版" /></label><label>服务类型<select value={category} onChange={(e) => setCategory(e.target.value)}>{Object.entries(categoryName).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><label>席位数量<input name="seatTotal" type="number" required min={1} max={10000} defaultValue={1} /></label><label>每月价格（元）<input name="price" type="number" required min={0} max={1000000} step="0.01" defaultValue={0} /></label><label>共享账号<input name="accountName" maxLength={200} autoComplete="off" /></label><label>共享密码 / 密钥<input name="secret" type="password" maxLength={1000} autoComplete="new-password" /></label><label>平台续费时间<input name="renewAt" type="datetime-local" /></label>{category === "PROXY" && <><label>探针链接<input name="probeUrl" type="url" maxLength={1000} placeholder="https://status.example.com" /></label><label>节点数量<input name="nodeTotal" type="number" min={0} max={100000} defaultValue={0} /></label></>}<label>备注<textarea name="notes" maxLength={1000} /></label><button className="primary auth-submit" disabled={busy}>{busy ? "正在保存…" : "创建项目"}</button></form></div></div>;
}

function AssignForm({ users, services, onClose, onSubmit, onError }: { users: User[]; services: Service[]; onClose: () => void; onSubmit: (body: unknown) => Promise<void>; onError: (error: unknown) => void }) {
  const [serviceId, setServiceId] = useState(services[0]?.id || 0);
  const [busy, setBusy] = useState(false);
  const [minimumExpiry] = useState(() => localInputValue(new Date(Date.now() + 60000)));
  const selected = services.find((service) => service.id === serviceId);
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const userId = Number(form.get("userId"));
    const expires = new Date(String(form.get("expiresAt")));
    const priceYuan = Number(form.get("price"));
    const subscriptionUrl = String(form.get("subscriptionUrl") || "").trim();
    if (!userId || !serviceId) return onError(new Error("请选择用户和服务"));
    if (!Number.isFinite(expires.getTime()) || expires <= new Date()) return onError(new Error("到期时间必须晚于当前时间"));
    if (!Number.isFinite(priceYuan) || priceYuan < 0) return onError(new Error("租期价格格式不正确"));
    if (selected?.category === "PROXY" && !validHttpUrl(subscriptionUrl)) return onError(new Error("梯子服务必须填写有效的 HTTP(S) 订阅链接"));
    try {
      setBusy(true);
      await onSubmit({ userId, serviceId, startedAt: `${localInputValue(new Date())}:00`, expiresAt: `${String(form.get("expiresAt"))}:00`, priceCents: Math.round(priceYuan * 100), subscriptionUrl: subscriptionUrl || null, note: null });
    } catch (error) { onError(error); } finally { setBusy(false); }
  };
  return <div className="modal-backdrop" onMouseDown={onClose}><div className="modal form-modal" onMouseDown={(e) => e.stopPropagation()}><button className="close" onClick={onClose}>×</button><h2>分配服务租期</h2><p>每个服务单独计时，退租不会影响用户的其他服务。</p><form onSubmit={submit}><label>租户<select name="userId" required defaultValue=""><option value="" disabled>请选择已审批用户</option>{users.map((user) => <option key={user.id} value={user.id}>{user.name} · {user.email}</option>)}</select></label><label>服务<select name="serviceId" required value={serviceId || ""} onChange={(e) => setServiceId(Number(e.target.value))}><option value="" disabled>请选择服务</option>{services.filter((service) => service.status === "ACTIVE" && service.seatUsed < service.seatTotal).map((service) => <option key={service.id} value={service.id}>{service.name}（剩余 {service.seatTotal - service.seatUsed}）</option>)}</select></label><label>本期价格（元）<input name="price" type="number" required min={0} max={1000000} step="0.01" defaultValue={selected ? selected.monthlyPriceCents / 100 : 0} /></label><label>到期时间<input name="expiresAt" type="datetime-local" required min={minimumExpiry} /></label>{selected?.category === "PROXY" && <label>该租户的订阅链接<input name="subscriptionUrl" type="url" required maxLength={2000} placeholder="https://example.com/sub/..." autoComplete="off" /></label>}<button className="primary auth-submit" disabled={busy || users.length === 0 || services.length === 0}>{busy ? "正在分配…" : "确认分配"}</button></form></div></div>;
}

async function readJson(response: Response) {
  if (!response.ok) throw await apiError(response);
  return response.json();
}

async function apiError(response: Response) {
  try {
    const data = await response.json();
    return new Error(data.message || `请求失败（${response.status}）`);
  } catch {
    return new Error(`请求失败（${response.status}）`);
  }
}

function validHttpUrl(value: string) {
  try { return ["http:", "https:"].includes(new URL(value).protocol); } catch { return false; }
}

function membershipStatus(status: string) {
  return ({ ACTIVE: "正常", EXPIRING: "即将到期", EXPIRED: "已到期", CANCELLED: "已退租", PENDING: "待生效", REFUNDED: "已退款" } as Record<string, string>)[status] || status;
}

function localInputValue(date: Date) {
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}
