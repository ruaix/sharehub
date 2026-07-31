"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";

type View = "总览" | "共享项目" | "用户管理" | "订单记录" | "系统设置";
type Session = { user: { id: number; name: string; email: string; role: "admin" | "member" }; csrfToken: string };
type Registration = { id: number; name: string; email: string; created_at: string };
const API = "http://localhost:8787/api";

const services = [
  { name: "Netflix 高级版", category: "流媒体", icon: "N", color: "#ef4444", used: 4, total: 5, price: 28, status: "运行中", renew: "8月18日" },
  { name: "ChatGPT Plus", category: "AI 工具", icon: "AI", color: "#10a37f", used: 3, total: 5, price: 39, status: "运行中", renew: "8月22日" },
  { name: "CloudLink Pro", category: "梯子订阅", icon: "CL", color: "#667eea", used: 8, total: 10, price: 18, status: "运行中", renew: "8月12日", probe: { online: 12, total: 14, url: "status.cloudlink.example" } },
  { name: "Spotify 家庭组", category: "流媒体", icon: "S", color: "#1db954", used: 5, total: 6, price: 15, status: "运行中", renew: "8月26日" },
];

const members = [
  { name: "林小满", email: "lin@example.com", plan: "Netflix 高级版", amount: "¥28.00", status: "正常", avatar: "林" },
  { name: "陈屿", email: "chen@example.com", plan: "CloudLink Pro", amount: "¥18.00", status: "正常", avatar: "陈" },
  { name: "许知遥", email: "xu@example.com", plan: "ChatGPT Plus", amount: "¥39.00", status: "正常", avatar: "许" },
  { name: "周一一", email: "zhou@example.com", plan: "Komari 探针", amount: "¥12.00", status: "待续费", avatar: "周" },
];

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
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [registrationOpen, setRegistrationOpen] = useState(false);
  const filtered = useMemo(() => services.filter((item) => item.name.toLowerCase().includes(query.toLowerCase())), [query]);
  useEffect(() => {
    if (view === "用户管理" && session.user.role === "admin") {
      fetch(`${API}/admin/registrations`, { credentials: "include" })
        .then((res) => res.ok ? res.json() : [])
        .then(setRegistrations);
    }
  }, [view, session.user.role]);
  useEffect(() => {
    if (view === "系统设置" && session.user.role === "admin") {
      fetch(`${API}/admin/settings`, { credentials: "include" })
        .then((res) => res.ok ? res.json() : Promise.reject())
        .then((data) => setRegistrationOpen(data.registrationEnabled))
        .catch(() => undefined);
    }
  }, [view, session.user.role]);
  const review = async (id: number, action: "approve" | "reject") => {
    const res = await fetch(`${API}/admin/registrations/${id}/${action}`, {
      method: "POST", credentials: "include", headers: { "X-CSRF-Token": session.csrfToken },
    });
    if (res.ok) setRegistrations((rows) => rows.filter((row) => row.id !== id));
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
          <div className="top-actions"><button aria-label="消息">♢<i /></button><button className="primary" onClick={() => setShowCreate(true)}>＋ 创建合租</button></div>
        </header>

        <div className="page">
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
                {filtered.map((service) => <ServiceCard key={service.name} {...service} />)}
              </div>
            </section>

            <section className="bottom-grid">
              <div className="panel members">
                <div className="panel-head"><div><h2>最近加入</h2><p>新成员与订阅状态</p></div><button onClick={() => setView("用户管理")}>用户管理 →</button></div>
                <MemberTable rows={members.slice(0, 3)} />
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

          {view === "共享项目" && <section className="panel list-page"><div className="panel-head"><div><h2>全部共享项目</h2><p>管理共享账号与梯子订阅，探针作为梯子的节点状态页</p></div><button className="primary small" onClick={() => setShowCreate(true)}>＋ 新建项目</button></div><div className="service-grid all">{filtered.map((s) => <ServiceCard key={s.name} {...s} />)}</div></section>}
          {view === "用户管理" && <section className="panel list-page"><div className="panel-head"><div><h2>用户管理</h2><p>注册申请需要管理员审批后才能登录</p></div><button className="primary small">＋ 邀请用户</button></div>{session.user.role === "admin" && <div className="approval-box"><div className="approval-title"><strong>待审批申请</strong><span>{registrations.length}</span></div>{registrations.length === 0 ? <p className="approval-empty">目前没有待审批的注册申请</p> : registrations.map((row) => <div className="approval-row" key={row.id}><span className="avatar">{row.name.slice(0, 1)}</span><div><strong>{row.name}</strong><small>{row.email} · {new Date(row.created_at).toLocaleDateString("zh-CN")}</small></div><button className="reject" onClick={() => review(row.id, "reject")}>拒绝</button><button className="approve" onClick={() => review(row.id, "approve")}>通过</button></div>)}</div>}<MemberTable rows={members} /></section>}
          {view === "订单记录" && <Empty title="订单记录" text="付款、退款和续费记录将在这里统一展示。" action="创建首笔订单" />}
          {view === "系统设置" && <section className="panel settings-page"><div className="panel-head"><div><h2>系统设置</h2><p>控制公开功能与平台安全策略</p></div></div><div className="setting-row"><div className="setting-icon">♙</div><div><strong>开放用户注册</strong><p>{registrationOpen ? "登录页显示注册入口，新用户可以提交审批申请。" : "注册入口已隐藏，注册接口也已在服务端禁用。"}</p></div><button type="button" role="switch" aria-checked={registrationOpen} className={registrationOpen ? "switch on" : "switch"} onClick={toggleRegistration}><i /></button></div><div className="setting-security"><span>✓</span><p>该设置由后端强制执行，无法通过修改前端或直接请求接口绕过；每次修改都会进入审计日志。</p></div></section>}
        </div>
      </section>

      {showCreate && <div className="modal-backdrop" onMouseDown={() => setShowCreate(false)}><div className="modal" onMouseDown={(e) => e.stopPropagation()}><button className="close" onClick={() => setShowCreate(false)}>×</button><span className="modal-icon">＋</span><h2>创建合租项目</h2><p>选择服务类型；梯子的订阅与探针信息会在下一步配置。</p><div className="category-grid">{["流媒体账号", "梯子服务", "AI 工具", "其他订阅"].map((x) => <button key={x} onClick={() => setShowCreate(false)}><span>{x.slice(0, 1)}</span>{x}</button>)}</div></div></div>}
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
    setCaptcha(null);
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
  return <main className="auth-page"><section className="auth-brand"><div><span className="brand-mark">拼</span><b>拼友</b></div><h1>合租管理，<br />清楚一点就够了。</h1><p>共享账号、订阅链接、成员租期与节点状态，集中放在一个安全、清爽的工作台。</p><div className="security-note"><span>✓</span><div><strong>安全优先</strong><small>审批制注册 · Redis 防爆破 · 敏感数据脱敏</small></div></div></section><section className="auth-card"><div className={registrationEnabled ? "auth-tabs" : "auth-tabs single"}><button className={mode === "login" ? "active" : ""} onClick={() => { setMode("login"); setMessage(""); }}>登录</button>{registrationEnabled && <button className={mode === "register" ? "active" : ""} onClick={() => { setMode("register"); setMessage(""); }}>申请注册</button>}</div><h2>{mode === "login" ? "欢迎回来" : "创建申请"}</h2><p>{mode === "login" ? "登录后进入你的合租工作台" : "提交后需等待管理员审批"}</p><form onSubmit={submit}>{mode === "register" && <label>姓名<input name="name" required minLength={2} maxLength={40} autoComplete="name" placeholder="你的姓名" /></label>}<label>邮箱<input name="email" required type="email" autoComplete="email" placeholder="name@example.com" /></label><label>密码<input name="password" required type="password" minLength={10} maxLength={128} autoComplete={mode === "login" ? "current-password" : "new-password"} placeholder="至少 10 位" /></label>{mode === "register" && <small className="password-hint">需包含大小写字母、数字和符号</small>}{mode === "login" && <label>验证码<div className="captcha-field"><input name="captchaAnswer" required minLength={5} maxLength={5} autoComplete="off" placeholder="输入图中字符" /><button type="button" onClick={refreshCaptcha} title="换一张">{captcha ? <img src={captcha.image} alt="登录验证码" /> : <span>加载中…</span>}</button></div></label>}{message && <div className="form-message">{message}</div>}<button className="primary auth-submit" disabled={busy || (mode === "login" && !captcha)}>{busy ? "请稍候…" : mode === "login" ? "安全登录" : "提交注册申请"}</button></form><small className="privacy-copy">验证码一次有效，登录行为会记录安全审计日志。</small></section></main>;
}

function Stat({ label, value, delta, icon, tone, warning = false }: { label: string; value: string; delta: string; icon: string; tone: string; warning?: boolean }) {
  return <article className="stat"><div className={`stat-icon ${tone}`}>{icon}</div><div><p>{label}</p><h3>{value}</h3><span className={warning ? "warn" : ""}>{delta}</span></div></article>;
}

function ServiceCard(props: typeof services[number]) {
  const percent = Math.round((props.used / props.total) * 100);
  return <article className="service-card"><div className="service-top"><span className="service-logo" style={{ background: props.color }}>{props.icon}</span><span className={props.status === "运行中" ? "status" : "status warning"}>● {props.status}</span></div><h3>{props.name}</h3><p>{props.category}</p><div className="capacity"><span>{props.probe ? "有效订阅" : "席位使用"}</span><b>{props.used} / {props.total}</b></div><div className="progress"><i style={{ width: `${percent}%`, background: props.color }} /></div>{props.probe && <div className="probe-row"><span><i /> 节点状态</span><strong>{props.probe.online} / {props.probe.total} 在线</strong><button title={props.probe.url}>查看探针 →</button></div>}<div className="service-foot"><span><small>每位 / 月</small><strong>¥{props.price}</strong></span><span><small>下次续费</small><strong>{props.renew}</strong></span></div></article>;
}

function MemberTable({ rows }: { rows: typeof members }) {
  return <div className="table">{rows.map((m) => <div className="table-row" key={m.email}><span className="avatar">{m.avatar}</span><div className="member-name"><strong>{m.name}</strong><small>{m.email}</small></div><span className="plan">{m.plan}</span><strong className="amount">{m.amount}</strong><span className={m.status === "正常" ? "status" : "status warning"}>● {m.status}</span><button>⋯</button></div>)}</div>;
}

function Empty({ title, text, action }: { title: string; text: string; action: string }) {
  return <section className="panel empty"><span>◇</span><h2>{title}</h2><p>{text}</p><button className="primary small">{action}</button></section>;
}
