// 单文件看板渲染：数据内联、零外部依赖，可直接 file:// 打开或打印。
// 只读：不修改 data/，唯一副作用是写出 dist/index.html。

import { STATE_LABELS, PRIORITY_LABELS, priorityRank } from './project.mjs';

const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, (ch) => ESC[ch]);
}

function day(iso) {
  return iso ? String(iso).slice(0, 10) : '';
}

function stamp(iso) {
  return iso ? String(iso).slice(0, 16).replace('T', ' ') : '';
}

function badge(state) {
  return `<span class="st st-${esc(state)}">${esc(STATE_LABELS[state] || state)}</span>`;
}

function prio(priority) {
  return `<span class="pr pr-${esc(priority)}">${esc(priority)}</span>`;
}

function layerTag(layer) {
  const label = { backend: '后端', frontend: '前端', fullstack: '跨端' }[layer] || layer;
  return `<span class="ly ly-${esc(layer)}">${esc(label)}</span>`;
}

function moduleChips(list) {
  if (!list || list.length === 0) return '<span class="muted">—</span>';
  return list.map((item) => `<code class="chip">${esc(item)}</code>`).join('');
}

function featureRows(features, side) {
  return features
    .filter((feature) => (side === 'backend' ? feature.layer !== 'frontend' : feature.layer !== 'backend'))
    .map((feature) => {
      const modules = side === 'backend' ? feature.backendModules : feature.frontendModules;
      const taskCell = feature.tasks.length === 0
        ? '<span class="flag">未排期</span>'
        : `${feature.tasks.length} 项`;
      const acceptanceCell = feature.acceptance
        ? `${esc(feature.acceptance.data.id)} · ${esc(feature.acceptance.data.verdict)}`
        : '<span class="muted">—</span>';
      return `<tr data-row data-state="${esc(feature.status)}" data-priority="${esc(feature.priority)}" data-layer="${esc(feature.layer)}">
  <td><a href="#${esc(feature.id)}"><code>${esc(feature.id)}</code></a><div class="t">${esc(feature.title)}</div></td>
  <td>${badge(feature.status)}</td>
  <td>${prio(feature.priority)}</td>
  <td>${moduleChips(modules)}</td>
  <td>${taskCell}</td>
  <td>${acceptanceCell}</td>
  <td class="nowrap">${feature.code ? `${esc(feature.code.shortSha)}<div class="muted">${esc(day(feature.code.date))}</div>` : '<span class="muted">—</span>'}</td>
</tr>`;
    })
    .join('\n');
}

function unfinishedGroups(features) {
  const groups = ['P0', 'P1', 'P2', 'P3'].map((priority) => ({
    priority,
    items: features.filter((feature) => feature.priority === priority),
  }));
  return groups
    .filter((group) => group.items.length > 0)
    .map((group) => `<div class="grp">
  <h3>${prio(group.priority)} <span class="muted">${esc(PRIORITY_LABELS[group.priority])} · ${group.items.length} 项</span></h3>
  <div class="cards">
${group.items.map((feature) => `    <article class="card" data-row data-state="${esc(feature.status)}" data-priority="${esc(feature.priority)}" data-layer="${esc(feature.layer)}">
      <div class="card-top"><a href="#${esc(feature.id)}"><code>${esc(feature.id)}</code></a> ${badge(feature.status)} ${layerTag(feature.layer)}</div>
      <h4>${esc(feature.title)}</h4>
      <p class="muted">${esc(feature.summary || '（无摘要）')}</p>
      <div class="card-meta">${feature.missingTasks ? '<span class="flag">未排任务</span>' : `<span class="muted">任务 ${feature.tasks.length} 项</span>`}${feature.blockers.slice(0, 2).map((item) => `<span class="blk">${esc(item)}</span>`).join('')}</div>
    </article>`).join('\n')}
  </div>
</div>`)
    .join('\n');
}

function taskTables(tasks) {
  const done = tasks.filter((task) => task.status === 'done');
  const open = tasks.filter((task) => task.status !== 'done');
  const doneRows = done.map((task) => `<tr data-row data-state="${esc(task.status)}" data-priority="${esc(task.priority)}">
  <td><code>${esc(task.id)}</code></td>
  <td>${esc(task.title)}</td>
  <td><a href="#${esc(task.feature)}"><code>${esc(task.feature)}</code></a></td>
  <td class="nowrap">${esc(stamp(task.doneAt))}</td>
  <td><code>${esc(task.commit || '—')}</code></td>
</tr>`).join('\n');
  const openRows = open.map((task) => `<tr data-row data-state="${esc(task.status)}" data-priority="${esc(task.priority)}">
  <td><code>${esc(task.id)}</code></td>
  <td>${esc(task.title)}</td>
  <td><a href="#${esc(task.feature)}"><code>${esc(task.feature)}</code></a></td>
  <td>${prio(task.priority)}</td>
  <td>${esc({ todo: '未开始', doing: '进行中', dropped: '已放弃' }[task.status] || task.status)}</td>
</tr>`).join('\n');
  return `<div class="two-col">
  <div>
    <h3>未完成任务 <span class="muted">${open.length} 项 · 按优先级</span></h3>
    <div class="table-wrap"><table><thead><tr><th>任务</th><th>内容</th><th>功能点</th><th>优先级</th><th>状态</th></tr></thead><tbody>
${openRows || '<tr><td colspan="5" class="muted">没有未完成任务</td></tr>'}
    </tbody></table></div>
  </div>
  <div>
    <h3>已完成任务 <span class="muted">${done.length} 项 · 含实施时间点</span></h3>
    <div class="table-wrap"><table><thead><tr><th>任务</th><th>内容</th><th>功能点</th><th>完成时间</th><th>提交</th></tr></thead><tbody>
${doneRows || '<tr><td colspan="5" class="muted">没有已完成任务</td></tr>'}
    </tbody></table></div>
  </div>
</div>`;
}

function integrationTable(cases) {
  if (cases.length === 0) return '<p class="muted">暂无联调 case。</p>';
  const rows = cases.map((item) => `<tr data-row data-state="${esc(item.review)}" data-priority="" data-layer="">
  <td><code>${esc(item.id)}</code></td>
  <td>${esc(item.title)}</td>
  <td>${(item.features || []).map((id) => `<a href="#${esc(id)}"><code>${esc(id)}</code></a>`).join(' ')}</td>
  <td>${item.needChain ? '<span class="blk">需要打通</span>' : '<span class="muted">不需要</span>'}</td>
  <td>${esc(item.review)}</td>
  <td>${(item.scenarios || []).length}</td>
</tr>`).join('\n');
  return `<div class="table-wrap"><table><thead><tr><th>Case</th><th>链路</th><th>功能点</th><th>链路要求</th><th>评审</th><th>场景数</th></tr></thead><tbody>
${rows}
  </tbody></table></div>`;
}

function gateSection(checks) {
  const errorItems = checks.errors.map((item) => `<li><span class="blk err">错误</span> <code>[${item.rule}]</code> ${esc(item.entity)}：${esc(item.message)}</li>`).join('\n');
  const warnItems = checks.warnings.map((item) => `<li><span class="blk warn">提示</span> <code>[${item.rule}]</code> ${esc(item.entity)}：${esc(item.message)}</li>`).join('\n');
  return `<div class="two-col">
  <article class="panel ${checks.errors.length ? 'panel-err' : 'panel-ok'}">
    <h3>${checks.errors.length ? `阻断项 ${checks.errors.length}` : '门禁通过'}</h3>
    <ul class="plain">${errorItems || '<li class="muted">没有阻断项：状态、引用与证据一致性检查全部通过。</li>'}</ul>
  </article>
  <article class="panel">
    <h3>提示项 ${checks.warnings.length}</h3>
    <ul class="plain">${warnItems || '<li class="muted">没有提示项。</li>'}</ul>
  </article>
</div>`;
}

function detailList(features) {
  return features.map((feature) => {
    const taskItems = feature.tasks.length === 0
      ? '<li class="muted">没有安排任务</li>'
      : feature.tasks.map(({ id, task }) => (task
        ? `<li><code>${esc(id)}</code> ${esc(task.data.title)} · ${esc(task.data.status)}${task.data.doneAt ? ` · ${esc(stamp(task.data.doneAt))}` : ''}</li>`
        : `<li class="muted"><code>${esc(id)}</code> 引用的任务不存在</li>`)).join('\n');
    const pathItems = feature.paths.length === 0
      ? '<li class="muted">未声明 paths</li>'
      : feature.paths.map((path) => `<li><code>${esc(path)}</code></li>`).join('\n');
    const acceptanceLine = feature.acceptance
      ? `<code>${esc(feature.acceptance.data.id)}</code> ${esc(feature.acceptance.data.verdict)} · ${esc(feature.acceptance.data.level || '')} · ${esc(feature.acceptance.data.signedBy || '未签')} ${esc(stamp(feature.acceptance.data.signedAt))}`
      : '<span class="muted">无验收单</span>';
    return `<details class="item" id="${esc(feature.id)}" data-row data-state="${esc(feature.status)}" data-priority="${esc(feature.priority)}" data-layer="${esc(feature.layer)}">
  <summary><code>${esc(feature.id)}</code> ${esc(feature.title)} ${badge(feature.status)} ${prio(feature.priority)} ${layerTag(feature.layer)}</summary>
  <div class="item-body">
    <p>${esc(feature.summary || '（无摘要）')}</p>
    <dl>
      <dt>后端模块</dt><dd>${moduleChips(feature.backendModules)}</dd>
      <dt>前端模块</dt><dd>${moduleChips(feature.frontendModules)}</dd>
      <dt>最近提交</dt><dd>${feature.code ? `<code>${esc(feature.code.shortSha)}</code> ${esc(day(feature.code.date))} · ${esc(feature.code.subject)}` : '<span class="muted">paths 内无提交</span>'}</dd>
      <dt>验收单</dt><dd>${acceptanceLine}</dd>
      <dt>联调 case</dt><dd>${feature.integration ? `<code>${esc(feature.integration.data.id)}</code> · 需要打通：${feature.integration.data.needChain ? '是' : '否'} · 评审：${esc(feature.integration.data.review)}` : '<span class="muted">—</span>'}</dd>
      <dt>测试证据</dt><dd>${feature.runs.length ? feature.runs.map((run) => `<code>${esc(run.id)}</code> ${esc(run.result ? `${run.result.passed}/${run.result.total}` : '')}`).join(' ') : '<span class="muted">无覆盖该功能点的运行记录</span>'}</dd>
      <dt>未完成原因</dt><dd>${feature.status === 'accepted' || feature.status === 'cancelled' ? '<span class="muted">—</span>' : (feature.blockers.length ? feature.blockers.map((item) => `<span class="blk">${esc(item)}</span>`).join('') : '<span class="muted">—</span>')}</dd>
    </dl>
    <h4>任务</h4><ul>${taskItems}</ul>
    <h4>路径</h4><ul>${pathItems}</ul>
  </div>
</details>`;
  }).join('\n');
}

const STYLE = `
:root{--paper:#faf8f3;--white:#fffefa;--ink:#24352e;--muted:#65746c;--line:#dedfd5;--green:#265b47;--green-soft:#e8f0e8;--orange:#a94a2b;--orange-soft:#faeee3;--red:#9c4037;--blue:#3d5a80;--radius:8px}
*{box-sizing:border-box}
body{margin:0;background:var(--paper);color:var(--ink);font:15px/1.75 -apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif}
a{color:var(--green);text-decoration:none}a:hover{text-decoration:underline}
code{font:12px/1.5 ui-monospace,SFMono-Regular,Menlo,monospace;background:#f1f0e8;padding:1px 5px;border-radius:4px}
h1,h2,h3,h4{margin:0;line-height:1.35}
.wrap{max-width:1400px;margin:0 auto;padding:40px 28px 80px}
header.top{border-bottom:1px solid var(--line);padding-bottom:22px;margin-bottom:26px}
.eyebrow{font-size:11px;font-weight:700;letter-spacing:2px;color:var(--green)}
h1{font-size:clamp(28px,3vw,42px);letter-spacing:-1px;margin:14px 0 12px}
h1 em{font-style:normal;color:var(--green)}
.meta{display:flex;gap:22px;flex-wrap:wrap;font-size:12px;color:var(--muted)}
.meta code{background:transparent;padding:0}
.kpis{display:grid;grid-template-columns:repeat(6,1fr);gap:12px;margin:26px 0 8px}
.kpi{background:var(--white);border:1px solid var(--line);border-radius:var(--radius);padding:14px 16px}
.kpi span{display:block;font-size:11px;color:var(--muted);letter-spacing:1px}
.kpi strong{font-size:30px;line-height:1.2}
.kpi small{font-size:12px;color:var(--muted)}
.bar{position:sticky;top:0;z-index:5;display:flex;gap:12px;flex-wrap:wrap;align-items:center;background:rgba(250,248,243,.94);backdrop-filter:blur(6px);border-bottom:1px solid var(--line);padding:12px 0;margin:22px 0 30px}
.bar input,.bar select{font:inherit;padding:7px 10px;border:1px solid var(--line);border-radius:6px;background:var(--white);color:var(--ink)}
.bar input{width:260px}
.bar button{font:inherit;padding:7px 12px;border:1px solid var(--line);border-radius:6px;background:var(--white);color:var(--ink);cursor:pointer}
.bar button:hover{border-color:var(--green);background:var(--green-soft)}
#count{margin-left:auto;font-size:12px;color:var(--muted)}
section{margin:44px 0}
section>h2{font-size:22px;margin-bottom:6px}
section>.lead{color:var(--muted);font-size:13px;margin:0 0 16px}
.table-wrap{overflow-x:auto;border:1px solid var(--line);border-radius:var(--radius);background:var(--white)}
table{width:100%;border-collapse:collapse;font-size:13px}
th,td{text-align:left;padding:10px 12px;border-bottom:1px solid var(--line);vertical-align:top}
th{font-size:11px;letter-spacing:1px;color:var(--muted);background:#f4f3ec;font-weight:600}
tr:last-child td{border-bottom:0}
.t{font-size:13px}
.nowrap{white-space:nowrap}
.muted{color:var(--muted)}
.st,.pr,.ly,.flag,.blk{display:inline-block;font-size:11px;line-height:1.6;border-radius:999px;padding:1px 9px;border:1px solid transparent;white-space:nowrap}
.st{background:var(--green-soft);color:var(--green)}
.st-todo,.st-cancelled{background:#f0efe8;color:var(--muted)}
.st-in_progress,.st-planned{background:var(--orange-soft);color:var(--orange)}
.st-implemented{background:#eef0f7;color:var(--blue)}
.st-testing{background:#fdeee9;color:var(--red);border-color:#f0d3c9}
.st-accepted{background:var(--green);color:#fff}
.pr{border-color:var(--line);color:var(--muted);font-family:ui-monospace,Menlo,monospace}
.pr-P0{border-color:#e2b8a9;color:var(--red);background:#fdf1ed}
.pr-P1{border-color:#e8cdb6;color:var(--orange)}
.ly{background:#f0efe8;color:var(--muted)}
.flag{background:#fdf1ed;color:var(--red);border-color:#eed3c8}
.blk{background:#f7f4ec;color:#6b5f4a;border-color:#e6dfcd}
.chip{display:inline-block;margin:1px 4px 1px 0;font-size:11px}
.grp{margin:20px 0}
.grp h3{font-size:14px;display:flex;gap:10px;align-items:center;margin-bottom:10px}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:12px}
.card{background:var(--white);border:1px solid var(--line);border-radius:var(--radius);padding:14px 16px}
.card-top{display:flex;gap:8px;align-items:center;margin-bottom:6px;flex-wrap:wrap}
.card h4{font-size:15px;margin:2px 0 6px}
.card p{font-size:12px;margin:0 0 10px}
.card-meta{display:flex;gap:6px;flex-wrap:wrap;font-size:11px}
.two-col{display:grid;grid-template-columns:1fr 1fr;gap:16px}
.panel{background:var(--white);border:1px solid var(--line);border-radius:var(--radius);padding:16px 18px}
.panel-err{border-color:#e3c0b4;background:#fdf6f3}
.panel-ok{border-color:#c8dbcc;background:#f3f8f3}
.panel h3{font-size:14px;margin-bottom:8px}
.plain{list-style:none;padding:0;margin:0;font-size:12px}
.plain li{padding:4px 0;border-bottom:1px dashed var(--line)}
.plain li:last-child{border-bottom:0}
.blk.err{background:#fbe3dc;color:var(--red)}
.blk.warn{background:#fbf1dd;color:#8a6520}
.item{background:var(--white);border:1px solid var(--line);border-radius:var(--radius);margin-bottom:8px}
.item summary{cursor:pointer;padding:12px 16px;display:flex;gap:8px;align-items:center;flex-wrap:wrap}
.item-body{padding:0 16px 16px;border-top:1px solid var(--line)}
.item-body p{margin:12px 0}
dl{display:grid;grid-template-columns:110px 1fr;gap:6px 12px;font-size:12px;margin:12px 0}
dt{color:var(--muted)}dd{margin:0}
.item-body h4{font-size:12px;color:var(--muted);margin:14px 0 4px;letter-spacing:1px}
.item-body ul{margin:0;padding-left:18px;font-size:12px}
footer{margin-top:60px;padding-top:18px;border-top:1px solid var(--line);font-size:12px;color:var(--muted);display:flex;gap:18px;flex-wrap:wrap}
[hidden]{display:none!important}
@media(max-width:1100px){.kpis{grid-template-columns:repeat(3,1fr)}.two-col{grid-template-columns:1fr}}
@media(max-width:700px){.kpis{grid-template-columns:repeat(2,1fr)}.bar input{width:100%}#count{margin-left:0}}
@media print{@page{size:A4;margin:12mm}body{background:#fff;font-size:10pt}.bar{display:none}.item{break-inside:avoid}section{break-after:auto}h2{break-after:avoid}}
`;

export function renderBoard(view, checks, digest) {
  const { stats, features, tasks, unfinished, integrations } = view;
  const backendCount = features.filter((feature) => feature.layer !== 'frontend').length;
  const frontendCount = features.filter((feature) => feature.layer !== 'backend').length;
  const fullstackCount = features.filter((feature) => feature.layer === 'fullstack').length;
  const backendOnly = features.filter((feature) => feature.layer === 'backend').length;
  const frontendOnly = features.filter((feature) => feature.layer === 'frontend').length;
  const html = `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="color-scheme" content="light">
<meta name="status-digest" content="${esc(digest)}">
<meta name="description" content="FoodMap 项目进度台账看板：功能点、任务、测试证据与验收门禁。生成物，请勿手改。">
<title>FoodMap 进度看板 · ${esc(view.head.shortSha)}</title>
<style>${STYLE}</style>
</head>
<body>
<div class="wrap">
<header class="top">
  <div class="eyebrow">FOODMAP · PROJECT LEDGER</div>
  <h1>事实台账，<em>已完成 ${stats.accepted}</em> / 待验收 ${stats.pendingAcceptance}</h1>
  <div class="meta">
    <span>生成 <code>${esc(stamp(view.generatedAt))}</code></span>
    <span>HEAD <code>${esc(view.head.shortSha)}</code> ${esc(view.head.branch || '')}</span>
    <span>数据摘要 <code>${esc(digest)}</code></span>
    <span>功能点 ${stats.total} · 跨端 ${fullstackCount} · 仅前端 ${frontendOnly} · 仅后端 ${backendOnly}</span>
  </div>
</header>

<div class="kpis">
  <div class="kpi"><span>功能点</span><strong>${stats.total}</strong><small>含未开始</small></div>
  <div class="kpi"><span>已完成</span><strong>${stats.accepted}</strong><small>通过验收</small></div>
  <div class="kpi"><span>待验收</span><strong>${stats.pendingAcceptance}</strong><small>已实现或已提测</small></div>
  <div class="kpi"><span>未排任务</span><strong>${stats.missingTasks}</strong><small>未完成且无任务</small></div>
  <div class="kpi"><span>P0 未完成</span><strong>${stats.p0Unfinished}</strong><small>最高优先级</small></div>
  <div class="kpi"><span>任务</span><strong>${stats.tasksDone}<small>/${stats.tasksTotal}</small></strong><small>已完成 / 总数</small></div>
</div>

<div class="bar">
  <input id="q" type="search" placeholder="搜索功能点、任务、模块…">
  <select id="state"><option value="">全部状态</option>${Object.entries(STATE_LABELS).map(([key, label]) => `<option value="${esc(key)}">${esc(label)}</option>`).join('')}</select>
  <select id="priority"><option value="">全部优先级</option>${['P0', 'P1', 'P2', 'P3'].map((p) => `<option value="${p}">${esc(PRIORITY_LABELS[p])}</option>`).join('')}</select>
  <select id="layer"><option value="">全部端</option><option value="backend">后端</option><option value="frontend">前端</option><option value="fullstack">跨端</option></select>
  <button id="expand" type="button">展开全部</button>
  <button id="collapse" type="button">收起全部</button>
  <span id="count" role="status" aria-live="polite"></span>
</div>

<section id="backend">
  <h2>后端功能点（微服务视角）</h2>
  <p class="lead">共 ${backendCount} 项（含跨端 ${fullstackCount} 项，只显示后端一侧）。模块为后端微服务名。</p>
  <div class="table-wrap"><table><thead><tr><th>功能点</th><th>状态</th><th>优先级</th><th>微服务</th><th>任务</th><th>验收单</th><th>最近提交</th></tr></thead><tbody>
${featureRows(features, 'backend')}
  </tbody></table></div>
</section>

<section id="frontend">
  <h2>前端功能点（iOS 模块视角）</h2>
  <p class="lead">共 ${frontendCount} 项（含跨端 ${fullstackCount} 项，只显示前端一侧）。模块为 <code>Features/*</code> 业务模块与 <code>Core/*</code> 基础设施模块。</p>
  <div class="table-wrap"><table><thead><tr><th>功能点</th><th>状态</th><th>优先级</th><th>模块</th><th>任务</th><th>验收单</th><th>最近提交</th></tr></thead><tbody>
${featureRows(features, 'frontend')}
  </tbody></table></div>
</section>

<section id="unfinished">
  <h2>未完成功能点（按优先级）</h2>
  <p class="lead">P0 最高。标记"未排任务"的项说明还没有可执行任务，需要先排期。</p>
${unfinishedGroups(unfinished)}
</section>

<section id="tasks">
  <h2>任务</h2>
  <p class="lead">已完成任务记录实施时间点与提交；未完成任务按优先级排序。</p>
${taskTables(tasks)}
</section>

<section id="integration">
  <h2>联调链路 case</h2>
  <p class="lead">需要打通链路的能力必须单独评审；未评审通过会阻断功能点转为已完成。</p>
${integrationTable(integrations)}
</section>

<section id="gates">
  <h2>门禁与对账</h2>
  <p class="lead">错误项会阻断提交；提示项只标注风险，不改变状态。</p>
${gateSection(checks)}
</section>

<section id="detail">
  <h2>功能点明细</h2>
  <p class="lead">每项包含模块、路径、任务、验收单、测试证据与未完成原因。</p>
${detailList(features)}
</section>

<footer>
  <span>FoodMap 进度台账 · 由 <code>status build</code> 生成，请勿手改</span>
  <span>HEAD ${esc(view.head.shortSha)} · ${esc(day(view.head.date))}</span>
  <span>时区 ${esc(view.config.timezone || 'Asia/Shanghai')}</span>
  <a href="#backend">回到顶部 ↑</a>
</footer>
</div>
<script>
(() => {
  const rows = [...document.querySelectorAll('[data-row]')];
  const q = document.getElementById('q');
  const state = document.getElementById('state');
  const priority = document.getElementById('priority');
  const layer = document.getElementById('layer');
  const count = document.getElementById('count');
  function apply() {
    const query = q.value.trim().toLowerCase();
    let visible = 0;
    rows.forEach((row) => {
      const okQuery = !query || row.textContent.toLowerCase().includes(query);
      const okState = !state.value || row.dataset.state === state.value;
      const okPriority = !priority.value || row.dataset.priority === priority.value;
      const okLayer = !layer.value || row.dataset.layer === layer.value;
      row.hidden = !(okQuery && okState && okPriority && okLayer);
      if (!row.hidden) visible += 1;
    });
    count.textContent = visible + ' / ' + rows.length + ' 行可见';
  }
  [q, state, priority, layer].forEach((el) => el.addEventListener('input', apply));
  document.getElementById('expand').addEventListener('click', () => document.querySelectorAll('details').forEach((d) => { d.open = true; }));
  document.getElementById('collapse').addEventListener('click', () => document.querySelectorAll('details').forEach((d) => { d.open = false; }));
  apply();
})();
</script>
</body>
</html>
`;
  return html;
}
