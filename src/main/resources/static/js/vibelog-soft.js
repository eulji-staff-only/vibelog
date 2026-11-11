/*
 * vibelog-soft.js  (오류 화면·메인 히어로 공용, #470)
 * 메인 히어로의 "vibelog" 글자를 말랑한 도형(soft body)으로 그린다. 라이브러리 없이 Canvas 2D.
 *
 * 원리
 *  1) 글자 모양 얻기: 보이지 않는 캔버스에 굵은 글자를 그리고, 픽셀 테두리를 따라가 외곽선 점들을 얻는다.
 *     i 의 점처럼 떨어진 조각은 따로 하나의 도형이 된다. b·e·o·g 의 구멍은 장식선으로 따라 움직인다.
 *  2) 점 움직이기 (Verlet): 각 점은 "지금 위치 - 직전 위치"만큼 계속 움직인다 (속도를 따로 두지 않는다).
 *  3) 모양 기억 (shape matching): 도형마다 원래 모양을 기억해 두고, 지금 점들에 가장 잘 맞는 회전을 구해
 *     점들을 원래 자리 쪽으로 조금씩 당긴다. stiffness 가 작을수록 더 말랑하다.
 *  4) 이웃 간격: 테두리 점 사이 거리를 유지해 선이 뭉치거나 늘어지지 않게 한다.
 *  5) 충돌: 다른 도형 안으로 들어간 점은 가장 가까운 테두리로 밀어낸다. 화면 밖으로도 못 나간다.
 *  6) 끌기: 누른 곳에서 가장 가까운 점들을 마우스 쪽으로 당긴다.
 *  7) 두 번 누르기: 모든 점을 처음 자리 쪽으로 점점 세게 당겨, 출렁이며 제자리로 돌아간다.
 *  8) 거인: 7초마다 글자 뒤 아래에서 올라왔다 내려간다. 실루엣에 닿은 글자는 돌며 날아가
 *     영역 벽에 튕기며 돌아다니고, 제자리로 돌아오지 않는다. 7) 두 번 누르기로 다시 불러온다.
 *     ⚠️ 원작 캐릭터 그림을 쓴다 (저작권 확인 필요)
 */
(() => {
  const canvas = document.querySelector('[data-soft-wordmark]');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const WORD = canvas.dataset.softWord || 'vibelog'; // 404 화면은 data-soft-word="404"
  // 두 동강 모드 (그 밖 오류 화면, data-soft-split="true"): 글자마다 가운데 가로줄 하나를 비워 위·아래 반쪽 도형으로 만든다.
  // 갈라지기(soft:split) 전에는 같은 글자의 반쪽끼리 부딪히지 않아 한 글자처럼 붙어 있다 (같은 색으로 칠하면 이음매가 안 보인다)
  const SPLIT = canvas.dataset.softSplit === 'true';
  let splitOpen = false;
  const FONT_PX = 150;          // 글자를 따올 때 크기 (실제 화면 크기는 맞춰 줄인다)
  const SPACING = 5;            // 외곽선 점 사이 간격(px, 따올 때 기준). 작을수록 모서리가 살아난다
  const SMOOTH_PASSES = 1;      // 외곽선 다듬기 횟수. 많을수록 둥글어진다
  const GLYPH_FONT = '"Noto Sans KR", "Apple SD Gothic Neo", "Segoe UI", Arial, sans-serif'; // 사이트 글꼴(common.css --font-sans)
  const GAP = 10;               // 글자 사이 처음 간격(px, 따올 때 기준)
  const THICKNESS = 5;          // 도형끼리 이만큼(px) 떨어져 있게 한다. 선이 겹쳐 보이지 않게
  const RESET_FRAMES = 50;      // 두 번 눌렀을 때 제자리로 돌아가는 시간(프레임, 60프레임 = 1초)
  const STIFFNESS = 0.06;       // 원래 모양으로 돌아가는 힘 (0~1)
  const EDGE_STIFFNESS = 0.5;   // 이웃 점 간격 유지 힘
  const DAMPING = 0.985;        // 공기 저항
  const SUBSTEPS = 5;
  const MAX_SPEED = 6;          // 한 프레임에 점이 움직일 수 있는 최대 거리(px). 빠르면 얇은 글자를 뚫고 지나간다
  const MAX_PULL = 1.5;         // 끌 때 한 번에 당기는 최대 거리(px)
  // 색은 CSS(.vl-soft 의 --soft-*)에서 받는다. 없으면 기본값
  const css = getComputedStyle(canvas.parentElement);
  const color = (name, fallback) => css.getPropertyValue(name).trim() || fallback;
  const INK = color('--soft-ink', '#1a1a1a');
  const PAPER = color('--soft-fill', '#ffffff');
  const ACCENT = color('--soft-accent', '#2563eb');

  // 거인 (titan-rise.png: 6칸 × 3줄 = 18컷. 올라옴 0~5 → 버팀 6~13 →내려감 14~17)
  // 글자 뒤에서 캔버스 아래 가운데로 올라왔다 내려간다. 올라올 때 실루엣이 글자 점을 밀어낸다
  const TITAN_SRC = canvas.dataset.titanSrc;
  const TITAN_COLS = 6;
  const TITAN_ROWS = 3;
  const TITAN_FRAMES = 18;
  const TITAN_HOLD = [6, 13];            // 다 올라온 채 버티는 컷
  const TITAN_SPEED = 1.3;               // 거인 컷 넘기기 빠르기 (1 = 원래 속도). 작을수록 느리다
  const TITAN_FRAME_MS = 110 / TITAN_SPEED;
  const TITAN_RISE_SPEED = 1.5;          // 올라오는 컷(0~5)만 이만큼 더 빠르게. 내려가는 컷은 TITAN_SPEED 그대로
  const TITAN_RISE_MS = TITAN_FRAME_MS / TITAN_RISE_SPEED;
  const TITAN_HOLD_TOTAL_MS = 2000;      // 다 올라온 뒤 버티는 시간 (버팀 컷 전체)
  const TITAN_HOLD_MS = TITAN_HOLD_TOTAL_MS / (TITAN_HOLD[1] - TITAN_HOLD[0] + 1);
  // 한 번만 나오는 연출 (404 화면, data-titan-once="true"): 올라와서 글자를 날리고, titan:release 를 받으면 내려가며
  // 사라진다. 글자는 odm:done(입체기동 캐릭터가 다 지나감)을 받으면 제자리로 돌린다.
  // 올라온 때(titan:up)와 사라진 때(titan:gone)를 canvas 이벤트로 알린다 (error-odm.js 가 듣는다)
  const TITAN_ONCE = canvas.dataset.titanOnce === 'true';
  const TITAN_FIRST_MS = TITAN_ONCE ? 600 : 2000; // 페이지를 연 뒤 첫 등장까지
  const TITAN_REST_MS = 7000;            // 사라진 뒤 다시 나오기까지
  const TITAN_SIZE = 0.85;               // 한 컷을 그릴 높이 (캔버스 높이 대비)
  const PROFILE_STEP = 4;                // 실루엣 높이를 재는 간격 (원본 px)
  const KNOCK_SPEED = 10;                // 거인에 맞은 글자가 날아가는 빠르기 (px/프레임)
  const KNOCK_SPIN = 0.06;               // 맞은 글자가 도는 빠르기 (최대, 라디안/프레임)
  const FLY_MAX_SPEED = 10;              // 날아다니는 글자의 최대 빠르기. 너무 빠르면 다른 글자를 뚫고 지나간다
  const FLY_DAMPING = 0.997;             // 날아다니는 글자의 공기 저항. 1에 가까울수록 오래 돌아다닌다
  const WALL_BOUNCE = 0.85;              // 벽에 부딪힌 뒤 남는 빠르기 비율
  const KNOCK_COOLDOWN = 20;             // 한 번 맞은 글자는 이 프레임 동안 다시 맞지 않는다
  const titan = { img: null, cw: 0, ch: 0, profiles: [], frame: -1, next: 0 };

  let bodies = [];
  let frameNo = 0;
  let scale = 1;
  let width = 0;
  let height = 0;
  let drag = null;
  let resetLeft = 0; // 제자리로 돌아가는 중이면 남은 프레임 수

  /* ── 1. 글자 → 외곽선 점 ───────────────────────── */

  function glyphMask(ch) {
    const off = document.createElement('canvas');
    const pad = 24;
    const g = off.getContext('2d', { willReadFrequently: true });
    const font = `800 ${FONT_PX}px ${GLYPH_FONT}`;
    g.font = font;
    const w = Math.ceil(g.measureText(ch).width) + pad * 2;
    const h = Math.ceil(FONT_PX * 1.35) + pad * 2;
    off.width = w;
    off.height = h;
    g.font = font;
    g.textBaseline = 'alphabetic';
    g.fillStyle = '#000';
    const baseline = pad + FONT_PX;
    g.fillText(ch, pad, baseline);
    const data = g.getImageData(0, 0, w, h).data;
    const mask = new Uint8Array(w * h);
    for (let i = 0; i < w * h; i++) mask[i] = data[i * 4 + 3] > 128 ? 1 : 0;
    // 두 동강 모드: 글자 픽셀이 있는 줄의 한가운데 한 줄을 비운다 (네 방향으로만 이어 보므로 한 줄이면 두 덩어리로 나뉜다)
    let cutRow = null;
    if (SPLIT) {
      let top = -1;
      let bottom = -1;
      for (let yy = 0; yy < h; yy++) {
        let any = false;
        for (let xx = 0; xx < w && !any; xx++) any = mask[yy * w + xx] === 1;
        if (any) { if (top < 0) top = yy; bottom = yy; }
      }
      if (top >= 0) {
        cutRow = Math.floor((top + bottom) / 2);
        mask.fill(0, cutRow * w, (cutRow + 1) * w);
      }
    }
    return { mask, w, h, advance: w - pad * 2, pad, cutRow };
  }

  // 같은 값으로 이어진 픽셀 묶음(연결 요소)을 찾는다
  function components(mask, w, h, value) {
    const label = new Int32Array(w * h).fill(-1);
    const comps = [];
    for (let i = 0; i < w * h; i++) {
      if (mask[i] !== value || label[i] !== -1) continue;
      const id = comps.length;
      const pixels = [];
      const stack = [i];
      label[i] = id;
      let touchesEdge = false;
      while (stack.length) {
        const p = stack.pop();
        pixels.push(p);
        const x = p % w;
        const y = (p - x) / w;
        if (x === 0 || y === 0 || x === w - 1 || y === h - 1) touchesEdge = true;
        const next = [x > 0 && p - 1, x < w - 1 && p + 1, y > 0 && p - w, y < h - 1 && p + w];
        for (const n of next) {
          if (n === false || mask[n] !== value || label[n] !== -1) continue;
          label[n] = id;
          stack.push(n);
        }
      }
      comps.push({ pixels, touchesEdge, id });
    }
    return { comps, label };
  }

  // Moore 이웃 따라가기: 덩어리의 바깥 테두리 픽셀을 순서대로 얻는다
  const DIRS = [[-1, 0], [-1, -1], [0, -1], [1, -1], [1, 0], [1, 1], [0, 1], [-1, 1]];
  function traceBoundary(inside, start) {
    const pts = [start];
    let [cx, cy] = start;
    let d = 0;
    for (let iter = 0; iter < 20000; iter++) {
      let found = false;
      for (let k = 1; k <= 8; k++) {
        const nd = (d + k) % 8;
        const nx = cx + DIRS[nd][0];
        const ny = cy + DIRS[nd][1];
        if (!inside(nx, ny)) continue;
        const prev = (d + k - 1) % 8;
        const bx = cx + DIRS[prev][0] - nx;
        const by = cy + DIRS[prev][1] - ny;
        d = DIRS.findIndex(([dx, dy]) => dx === bx && dy === by);
        cx = nx;
        cy = ny;
        found = true;
        break;
      }
      if (!found || (cx === start[0] && cy === start[1])) break;
      pts.push([cx, cy]);
    }
    return pts;
  }

  // 테두리를 일정한 간격의 점으로 다시 찍고, 몇 번 다듬어 둥글게 만든다
  function resample(pts, spacing) {
    const n = pts.length;
    let total = 0;
    const seg = [];
    for (let i = 0; i < n; i++) {
      const a = pts[i];
      const b = pts[(i + 1) % n];
      const len = Math.hypot(b[0] - a[0], b[1] - a[1]);
      seg.push(len);
      total += len;
    }
    const count = Math.max(8, Math.round(total / spacing));
    const step = total / count;
    const out = [];
    let i = 0;
    let acc = 0;
    for (let k = 0; k < count; k++) {
      const target = k * step;
      while (acc + seg[i] < target && i < n - 1) acc += seg[i++];
      const a = pts[i];
      const b = pts[(i + 1) % n];
      const t = seg[i] ? (target - acc) / seg[i] : 0;
      out.push([a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t]);
    }
    for (let pass = 0; pass < SMOOTH_PASSES; pass++) {
      const copy = out.map((p) => p.slice());
      for (let k = 0; k < out.length; k++) {
        const p = copy[(k - 1 + out.length) % out.length];
        const q = copy[(k + 1) % out.length];
        out[k][0] = (p[0] + copy[k][0] * 2 + q[0]) / 4;
        out[k][1] = (p[1] + copy[k][1] * 2 + q[1]) / 4;
      }
    }
    return out;
  }

  function glyphShapes(ch) {
    const { mask, w, h, advance, pad, cutRow } = glyphMask(ch);
    const inside = (x, y) => x >= 0 && y >= 0 && x < w && y < h && mask[y * w + x] === 1;
    const solid = components(mask, w, h, 1);
    const empty = components(mask, w, h, 0);
    const shapes = solid.comps
      .filter((c) => c.pixels.length > 30)
      .map((c) => {
        const first = Math.min(...c.pixels);
        const start = [first % w, Math.floor(first / w)];
        const own = new Set(c.pixels);
        const outline = resample(traceBoundary((x, y) => inside(x, y) && own.has(y * w + x), start), SPACING);
        // 이 덩어리 안에 갇힌 빈 칸 = 구멍 (b, e, o, g)
        const holes = empty.comps
          .filter((e) => !e.touchesEdge && e.pixels.length > 20)
          .filter((e) => {
            const p = e.pixels[0];
            const x = p % w;
            const y = (p - x) / w;
            // 구멍 바로 위 픽셀이 이 덩어리면 이 덩어리의 구멍이다
            for (let yy = y - 1; yy >= 0; yy--) if (mask[yy * w + x]) return own.has(yy * w + x);
            return false;
          })
          .map((e) => {
            const firstHole = Math.min(...e.pixels);
            const hs = [firstHole % w, Math.floor(firstHole / w)];
            const holeSet = new Set(e.pixels);
            return resample(traceBoundary((x, y) => holeSet.has(y * w + x), hs), SPACING);
          });
        return { outline, holes };
      });
    return { shapes, advance, pad, cutRow };
  }

  /* ── 2. 도형(soft body) 만들기 ───────────────────── */

  function makeBody(outline, holes, ox, oy, s) {
    const pts = outline.map(([x, y]) => ({ x: ox + x * s, y: oy + y * s }));
    let cx = 0;
    let cy = 0;
    for (const p of pts) { cx += p.x; cy += p.y; }
    cx /= pts.length;
    cy /= pts.length;
    // hx, hy: 제자리(두 번 눌렀을 때 돌아갈 곳)
    const particles = pts.map((p) => ({ x: p.x, y: p.y, px: p.x, py: p.y, hx: p.x, hy: p.y, rx: p.x - cx, ry: p.y - cy }));
    const rest = particles.map((p, i) => {
      const q = particles[(i + 1) % particles.length];
      return Math.hypot(q.x - p.x, q.y - p.y);
    });
    const holePts = holes.map((hole) => hole.map(([x, y]) => ({ rx: ox + x * s - cx, ry: oy + y * s - cy })));
    return { particles, rest, holes: holePts, angle: 0, cx, cy };
  }

  function build() {
    release(); // 끄는 중에 다시 만들면 이전 점을 붙잡고 있게 되므로 끌기를 끝낸다
    const rect = canvas.getBoundingClientRect();
    width = rect.width;
    height = rect.height;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    const glyphs = [...WORD].map(glyphShapes);
    const gap = GAP;
    const raw = glyphs.reduce((sum, g) => sum + g.advance, 0) + gap * (glyphs.length - 1);
    scale = Math.min((width * 0.86) / raw, (height * 0.75) / (FONT_PX * 1.35));
    let x = (width - raw * scale) / 2;
    const y = (height - FONT_PX * 1.35 * scale) / 2 - 10;

    bodies = [];
    let cutY = null;
    glyphs.forEach((g, gi) => {
      const oy = y - g.pad * scale;
      const glyphCut = g.cutRow === null ? null : oy + g.cutRow * scale;
      if (glyphCut !== null) cutY = glyphCut;
      for (const shape of g.shapes) {
        const body = makeBody(shape.outline, shape.holes, x - g.pad * scale, oy, scale);
        body.glyph = gi; // 몇 번째 글자인지 (i 의 점처럼 한 글자가 도형 여러 개일 수 있다)
        body.top = glyphCut !== null && body.cy < glyphCut; // 두 동강 모드: 위쪽 반쪽인지
        bodies.push(body);
      }
      x += (g.advance + gap) * scale;
    });
    splitOpen = false;
    // 글자가 차지한 자리와 가르는 줄 높이를 알린다 (그 밖 오류 화면의 이펙트를 글자에 맞춰 놓는다, error-400.js)
    //   이벤트를 듣기 전에 만들어졌을 수 있어 canvas.softLayout 에도 남겨 둔다
    const box = bodies.map(bounds);
    canvas.softLayout = {
      x0: Math.min(...box.map((bb) => bb.x0)), x1: Math.max(...box.map((bb) => bb.x1)),
      y0: Math.min(...box.map((bb) => bb.y0)), y1: Math.max(...box.map((bb) => bb.y1)), cutY,
    };
    canvas.dispatchEvent(new CustomEvent('soft:layout', { detail: canvas.softLayout }));
    // 처음에는 멈춘 채로 둔다. 직전 위치를 흩어 두면 글자 전체가 위아래로 밀려 벽까지 떠내려간다
  }

  /* ── 3. 물리 한 걸음 ─────────────────────────────── */

  function integrate() {
    for (const b of bodies) {
      for (const p of b.particles) {
        // 거인에 맞은 글자는 공기 저항이 적고 조금 더 빨리 움직일 수 있다
        const damping = b.flying ? FLY_DAMPING : DAMPING;
        const max = b.flying ? FLY_MAX_SPEED : MAX_SPEED;
        let vx = (p.x - p.px) * damping;
        let vy = (p.y - p.py) * damping;
        const speed = Math.hypot(vx, vy);
        if (speed > max) { vx *= max / speed; vy *= max / speed; }
        p.px = p.x;
        p.py = p.y;
        p.x += vx;
        p.y += vy;
      }
    }
  }

  function shapeMatch(b) {
    const ps = b.particles;
    let cx = 0;
    let cy = 0;
    for (const p of ps) { cx += p.x; cy += p.y; }
    cx /= ps.length;
    cy /= ps.length;
    // 원래 모양을 얼마나 돌리면 지금 점들과 가장 잘 맞는지
    let dot = 0;
    let cross = 0;
    for (const p of ps) {
      const qx = p.x - cx;
      const qy = p.y - cy;
      dot += p.rx * qx + p.ry * qy;
      cross += p.rx * qy - p.ry * qx;
    }
    const angle = Math.atan2(cross, dot);
    const c = Math.cos(angle);
    const s = Math.sin(angle);
    for (const p of ps) {
      const gx = cx + p.rx * c - p.ry * s;
      const gy = cy + p.rx * s + p.ry * c;
      p.x += (gx - p.x) * STIFFNESS;
      p.y += (gy - p.y) * STIFFNESS;
    }
    b.angle = angle;
    b.cx = cx;
    b.cy = cy;
  }

  function keepEdges(b) {
    const ps = b.particles;
    for (let i = 0; i < ps.length; i++) {
      const p = ps[i];
      const q = ps[(i + 1) % ps.length];
      const dx = q.x - p.x;
      const dy = q.y - p.y;
      const len = Math.hypot(dx, dy) || 1e-6;
      const diff = ((len - b.rest[i]) / len) * 0.5 * EDGE_STIFFNESS;
      p.x += dx * diff;
      p.y += dy * diff;
      q.x -= dx * diff;
      q.y -= dy * diff;
    }
  }

  function bounds(b) {
    let x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity;
    for (const p of b.particles) {
      if (p.x < x0) x0 = p.x;
      if (p.y < y0) y0 = p.y;
      if (p.x > x1) x1 = p.x;
      if (p.y > y1) y1 = p.y;
    }
    return { x0, y0, x1, y1 };
  }

  function insidePolygon(ps, x, y) {
    let inside = false;
    for (let i = 0, j = ps.length - 1; i < ps.length; j = i++) {
      const a = ps[i];
      const c = ps[j];
      if ((a.y > y) !== (c.y > y) && x < ((c.x - a.x) * (y - a.y)) / (c.y - a.y) + a.x) inside = !inside;
    }
    return inside;
  }

  // ponytail: 모든 점 × 모든 도형 검사(O(n²)). 글자 8개라 충분, 도형이 많아지면 공간 해시로 바꾼다
  function collide() {
    const boxes = bodies.map(bounds);
    const r = THICKNESS;
    for (let a = 0; a < bodies.length; a++) {
      for (let bIdx = 0; bIdx < bodies.length; bIdx++) {
        if (a === bIdx) continue;
        // 두 동강 모드: 갈라지기 전에는 같은 글자의 반쪽끼리 밀어내지 않는다 (붙어 있어야 한 글자로 보인다)
        if (SPLIT && !splitOpen && bodies[a].glyph === bodies[bIdx].glyph) continue;
        const A = boxes[a];
        const B = boxes[bIdx];
        if (A.x1 + r < B.x0 || B.x1 + r < A.x0 || A.y1 + r < B.y0 || B.y1 + r < A.y0) continue;
        const other = bodies[bIdx].particles;
        for (const p of bodies[a].particles) {
          if (p.x < B.x0 - r || p.x > B.x1 + r || p.y < B.y0 - r || p.y > B.y1 + r) continue;
          // 가장 가까운 테두리 선분
          let best = Infinity, bx = 0, by = 0, bi = 0, bt = 0;
          for (let i = 0; i < other.length; i++) {
            const s0 = other[i];
            const s1 = other[(i + 1) % other.length];
            const ex = s1.x - s0.x;
            const ey = s1.y - s0.y;
            const t = Math.max(0, Math.min(1, ((p.x - s0.x) * ex + (p.y - s0.y) * ey) / (ex * ex + ey * ey || 1)));
            const qx = s0.x + ex * t;
            const qy = s0.y + ey * t;
            const d = (p.x - qx) ** 2 + (p.y - qy) ** 2;
            if (d < best) { best = d; bx = qx; by = qy; bi = i; bt = t; }
          }
          const inside = insidePolygon(other, p.x, p.y);
          const dist = Math.sqrt(best);
          if (!inside && dist >= r) continue;
          // 점이 있어야 할 곳: 테두리에서 바깥쪽으로 r 만큼 떨어진 곳
          let nx = p.x - bx;
          let ny = p.y - by;
          const len = Math.hypot(nx, ny) || 1e-6;
          nx /= len;
          ny /= len;
          if (inside) { nx = -nx; ny = -ny; }
          const tx = bx + nx * r;
          const ty = by + ny * r;
          const dx = tx - p.x;
          const dy = ty - p.y;
          // 점과 상대 선분이 반씩 나눠 움직인다
          p.x += dx * 0.5;
          p.y += dy * 0.5;
          const s0 = other[bi];
          const s1 = other[(bi + 1) % other.length];
          s0.x -= dx * 0.5 * (1 - bt);
          s0.y -= dy * 0.5 * (1 - bt);
          s1.x -= dx * 0.5 * bt;
          s1.y -= dy * 0.5 * bt;
        }
      }
    }
  }

  function walls() {
    const m = 4;
    // 날아다니는 글자는 벽에서 튕긴다: 벽 쪽으로 가던 속도를 뒤집는다 (Verlet 이라 직전 위치를 옮긴다).
    // 이미 안쪽으로 돌아오는 중이면 뒤집지 않는다. 안 그러면 다음 계산에서 다시 벽 밖으로 튕겨 나간다
    const bounce = (b, pos, prev, limit, outward) => {
      const v = pos - prev;
      return b.flying && v * outward > 0 ? limit + v * WALL_BOUNCE : prev;
    };
    for (const b of bodies) {
      for (const p of b.particles) {
        if (p.x < m) { p.px = bounce(b, p.x, p.px, m, -1); p.x = m; }
        if (p.x > width - m) { p.px = bounce(b, p.x, p.px, width - m, 1); p.x = width - m; }
        if (p.y < m) { p.py = bounce(b, p.y, p.py, m, -1); p.y = m; }
        if (p.y > height - m) { p.py = bounce(b, p.y, p.py, height - m, 1); p.y = height - m; }
      }
    }
  }

  function pull() {
    if (!drag) return;
    for (const { p, w } of drag.grab) {
      let dx = (drag.x + p.ox - p.x) * 0.3 * w;
      let dy = (drag.y + p.oy - p.y) * 0.3 * w;
      const d = Math.hypot(dx, dy);
      if (d > MAX_PULL) { dx *= MAX_PULL / d; dy *= MAX_PULL / d; }
      p.x += dx;
      p.y += dy;
    }
  }

  // 두 번 누르면: 각 점을 제자리 쪽으로 점점 세게 당긴다. Verlet 이라 당긴 만큼 속도가 생겨 살짝 출렁이며 멈춘다.
  // 돌아가는 동안은 글자끼리 길을 막지 않게 충돌을 끈다
  function goHome() {
    const t = 1 - resetLeft / RESET_FRAMES;      // 0 → 1
    const k = 0.04 + 0.16 * t * t;               // 처음엔 부드럽게, 끝으로 갈수록 확실하게
    for (const b of bodies) {
      for (const p of b.particles) {
        p.x += (p.hx - p.x) * k;
        p.y += (p.hy - p.y) * k;
      }
    }
  }

  /* ── 거인 ────────────────────────────────────────── */

  // 컷마다 "열(x)별로 몸이 시작되는 높이"를 재 둔다. 이 선보다 아래에 들어온 글자 점을 밀어낸다
  function loadTitan() {
    if (!TITAN_SRC || reduceMotion) return;
    const img = new Image();
    img.onload = () => {
      titan.cw = img.width / TITAN_COLS;
      titan.ch = img.height / TITAN_ROWS;
      const off = document.createElement('canvas');
      off.width = img.width;
      off.height = img.height;
      const g = off.getContext('2d', { willReadFrequently: true });
      g.drawImage(img, 0, 0);
      const { data } = g.getImageData(0, 0, img.width, img.height);
      const W = img.width;
      // 붉은 살 부분만 몸으로 본다. 흰 김과 김 테두리의 얇은 붉은 선은 빼려고 아래로 12px 이어진 곳만 센다
      const flesh = (x, y) => {
        if (y >= img.height) return false;
        const i = (y * W + x) * 4;
        return data[i + 3] > 180 && data[i] > 110 && data[i] > data[i + 1] * 1.4;
      };
      for (let f = 0; f < TITAN_FRAMES; f++) {
        const x0 = Math.floor((f % TITAN_COLS) * titan.cw);
        const y0 = Math.floor(Math.floor(f / TITAN_COLS) * titan.ch);
        const raw = [];
        for (let sx = 0; sx < titan.cw - 1; sx += PROFILE_STEP) {
          let top = Infinity;
          for (let sy = 0; sy < titan.ch - 12; sy++) {
            if (flesh(x0 + sx, y0 + sy) && flesh(x0 + sx, y0 + sy + 6) && flesh(x0 + sx, y0 + sy + 12)) { top = sy; break; }
          }
          raw.push(top);
        }
        // 손가락 사이 같은 작은 틈은 메운다 (양옆 2칸 중 가장 높은 곳)
        titan.profiles.push(raw.map((_, i) => Math.min(...raw.slice(Math.max(0, i - 2), i + 3))));
      }
      titan.img = img;
      titan.next = performance.now() + TITAN_FIRST_MS;
    };
    img.src = TITAN_SRC;
  }

  function updateTitan(now) {
    if (!titan.img || now < titan.next) return;
    titan.frame++;
    if (TITAN_ONCE) {
      if (titan.frame === TITAN_HOLD[0] && !titan.upSent) {
        titan.upSent = true;
        knockAll();
        canvas.dispatchEvent(new CustomEvent('titan:up'));
      }
      if (titan.frame > TITAN_HOLD[1] && !titan.released) titan.frame = TITAN_HOLD[0]; // 풀어 줄 때까지 버틴다
    }
    if (titan.frame >= TITAN_FRAMES) {
      titan.frame = -1;
      if (TITAN_ONCE) {
        titan.next = Infinity; // 한 번만
        canvas.dispatchEvent(new CustomEvent('titan:gone')); // 글자는 캐릭터가 다 지나간 뒤(odm:done) 돌아온다
        return;
      }
      titan.next = now + TITAN_REST_MS;
      return;
    }
    const hold = titan.frame >= TITAN_HOLD[0] && titan.frame <= TITAN_HOLD[1];
    const rising = titan.frame < TITAN_HOLD[0];
    titan.next = now + (hold ? TITAN_HOLD_MS : rising ? TITAN_RISE_MS : TITAN_FRAME_MS);
  }

  // 캔버스 아래 가운데에 붙인다. k = 원본 1px 이 화면에서 몇 px 인지
  function titanBox() {
    const size = height * TITAN_SIZE;
    const w = size * (titan.cw / titan.ch);
    return { w, size, left: (width - w) / 2, top: height - size, k: size / titan.ch };
  }

  function titanTop(prof, sx) {
    const i = Math.round(sx / PROFILE_STEP);
    return i < 0 || i >= prof.length ? Infinity : prof[i];
  }

  // 실루엣에 닿은 글자는 맞은 쪽(머리 위는 위로, 어깨 쪽은 옆·위로)으로 회전하며 날아간다.
  // 날아간 글자는 영역 벽에 튕기고 다른 글자와 부딪히며 돌아다닌다. 제자리로 돌아오지 않는다 (두 번 누르면 제자리로)
  function titanPush() {
    if (titan.frame < 0) return;
    const prof = titan.profiles[titan.frame];
    const { w, left, top, k } = titanBox();
    for (const b of bodies) {
      if (frameNo - (b.knockedAt ?? -Infinity) < KNOCK_COOLDOWN) continue; // 방금 맞은 글자는 한 번만 튕긴다
      for (const p of b.particles) {
        const sx = (p.x - left) / k;
        const t = titanTop(prof, sx);
        if (t === Infinity || p.y <= top + t * k) continue;
        const tl = titanTop(prof, sx - 8);
        const tr = titanTop(prof, sx + 8);
        const slope = Number.isFinite(tl) && Number.isFinite(tr) ? (tr - tl) / 16 : 0;
        // 닿은 면의 바깥 방향 + 거인 발밑 가운데에서 글자 쪽 방향을 섞어, 글자들이 사방으로 흩어지게
        const n = Math.hypot(slope, 1);
        const rx = b.cx - (left + w / 2);
        const ry = b.cy - height;
        const r = Math.hypot(rx, ry) || 1;
        const dx = slope / n + (rx / r) * 0.6;
        const dy = -1 / n + (ry / r) * 0.6;
        const d = Math.hypot(dx, dy) || 1;
        knock(b, dx / d, dy / d);
        break;
      }
    }
  }

  function knock(b, nx, ny) {
    const speed = KNOCK_SPEED * (0.8 + Math.random() * 0.4);
    const spin = (Math.random() - 0.5) * KNOCK_SPIN;
    // Verlet 이라 직전 위치를 옮겨 속도를 준다. 회전은 중심에서 떨어진 만큼 옆으로 더 빠르게
    for (const p of b.particles) {
      p.px = p.x - (nx * speed - spin * (p.y - b.cy));
      p.py = p.y - (ny * speed + spin * (p.x - b.cx));
    }
    b.flying = true;
    b.knockedAt = frameNo;
  }

  function drawTitan() {
    if (titan.frame < 0) return;
    const { w, size, left, top } = titanBox();
    const f = titan.frame;
    ctx.drawImage(titan.img, (f % TITAN_COLS) * titan.cw, Math.floor(f / TITAN_COLS) * titan.ch,
      titan.cw, titan.ch, left, top, w, size);
  }

  function step() {
    integrate();
    const resetting = resetLeft > 0;
    for (let i = 0; i < SUBSTEPS; i++) {
      if (resetting) goHome(); else { pull(); titanPush(); }
      for (const b of bodies) { shapeMatch(b); keepEdges(b); }
      walls();
      // 충돌을 마지막에 푼다. 벽을 나중에 하면 벽에 눌린 글자가 이웃 글자 안으로 다시 들어간다
      if (!resetting) { collide(); collide(); collide(); }
    }
    if (resetting) resetLeft--;
  }

  /* ── 4. 그리기 ───────────────────────────────────── */

  function smoothPath(points) {
    const n = points.length;
    const mid = (a, b) => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
    let m = mid(points[n - 1], points[0]);
    ctx.moveTo(m[0], m[1]);
    for (let i = 0; i < n; i++) {
      const p = points[i];
      m = mid(p, points[(i + 1) % n]);
      ctx.quadraticCurveTo(p[0], p[1], m[0], m[1]);
    }
    ctx.closePath();
  }

  function draw() {
    ctx.clearRect(0, 0, width, height);
    drawTitan(); // 글자보다 먼저 = 글자 뒤
    ctx.lineWidth = 2;
    ctx.lineJoin = 'round';
    for (const b of bodies) {
      // 바깥선과 구멍(4·0·b·e·o·g 안쪽)을 한 도형으로 그려, 구멍은 칠하지 않고 비워 둔다 (evenodd).
      // 구멍은 도형의 지금 위치·회전을 따라간다
      const c = Math.cos(b.angle);
      const s = Math.sin(b.angle);
      ctx.beginPath();
      smoothPath(b.particles.map((p) => [p.x, p.y]));
      for (const hole of b.holes) {
        smoothPath(hole.map((h) => [b.cx + h.rx * c - h.ry * s, b.cy + h.rx * s + h.ry * c]));
      }
      ctx.fillStyle = PAPER;
      ctx.fill('evenodd');
      ctx.strokeStyle = INK;
      ctx.stroke();
    }
    if (drag) {
      ctx.beginPath();
      ctx.arc(drag.x, drag.y, 5, 0, Math.PI * 2);
      ctx.fillStyle = ACCENT;
      ctx.fill();
    }
  }

  // 물리는 1/60초 고정 간격으로 계산한다. 화면 주사율(60·144Hz)이 달라도 움직이는 빠르기·복원 시간이 같다
  const STEP_MS = 1000 / 60;
  let lastNow = 0;
  let stepAcc = 0;
  function loop(now) {
    stepAcc += lastNow ? Math.min(now - lastNow, 100) : STEP_MS; // 탭을 오래 비웠다 와도 한꺼번에 몰아 계산하지 않게
    lastNow = now;
    updateTitan(now);
    while (stepAcc >= STEP_MS) {
      frameNo++;
      step();
      stepAcc -= STEP_MS;
    }
    draw();
    requestAnimationFrame(loop);
  }

  /* ── 5. 끌기 ─────────────────────────────────────── */

  function pointer(e) {
    const r = canvas.getBoundingClientRect();
    return { x: e.clientX - r.left, y: e.clientY - r.top };
  }

  canvas.addEventListener('pointerdown', (e) => {
    if (reduceMotion || drag) return; // 움직임 줄이기면 끌지 않는다. 이미 끄는 중이면 다른 손가락은 무시한다
    const { x, y } = pointer(e);
    const radius = 40;
    const grab = [];
    for (const b of bodies) {
      for (const p of b.particles) {
        const d = Math.hypot(p.x - x, p.y - y);
        if (d < radius) grab.push({ p, w: 1 - d / radius });
      }
    }
    if (!grab.length) return;
    for (const g of grab) { g.p.ox = g.p.x - x; g.p.oy = g.p.y - y; }
    drag = { id: e.pointerId, x, y, grab };
    canvas.setPointerCapture(e.pointerId);
    canvas.classList.add('is-dragging');
  });
  canvas.addEventListener('pointermove', (e) => {
    if (!drag || e.pointerId !== drag.id) return; // 끌기를 시작한 포인터만 따라간다
    const { x, y } = pointer(e);
    drag.x = x;
    drag.y = y;
  });
  function release() {
    drag = null;
    canvas.classList.remove('is-dragging');
  }
  const releaseOwn = (e) => { if (drag && e.pointerId === drag.id) release(); };
  canvas.addEventListener('pointerup', releaseOwn);
  canvas.addEventListener('pointercancel', releaseOwn);
  // 날아간 글자까지 모두 출렁이며 제자리로
  function goHomeAll() {
    release();
    resetLeft = RESET_FRAMES;
    splitOpen = false; // 제자리로 돌아가면 반쪽끼리 다시 한 글자로 붙는다
    for (const b of bodies) b.flying = false;
  }

  // 한 번만 나오는 연출: 다 올라온 순간 아직 안 맞은 글자도 거인 발밑 가운데에서 바깥쪽으로 날린다 (모든 글자가 날아가게)
  function knockAll() {
    const { w, left } = titanBox();
    for (const b of bodies) {
      if (b.flying) continue;
      const dx = b.cx - (left + w / 2);
      const dy = b.cy - height;
      const d = Math.hypot(dx, dy) || 1;
      knock(b, dx / d, dy / d);
    }
  }

  canvas.addEventListener('dblclick', goHomeAll); // 두 번 누르면 제자리로 (애니메이션)

  // 키보드: 방향키로 글자 전체를 그쪽으로 밀고, Enter·Esc 로 제자리로 (메인 화면 캔버스는 tabindex="0")
  const PUSH = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] };
  canvas.addEventListener('keydown', (e) => {
    if (reduceMotion) return;
    if (e.key === 'Enter' || e.key === 'Escape') { goHomeAll(); e.preventDefault(); return; }
    const dir = PUSH[e.key];
    if (!dir) return;
    e.preventDefault(); // 방향키로 페이지가 스크롤되지 않게
    for (const b of bodies) {
      for (const p of b.particles) { // Verlet 이라 직전 위치를 옮겨 속도를 준다
        p.px -= dir[0] * MAX_SPEED;
        p.py -= dir[1] * MAX_SPEED;
      }
    }
  });
  canvas.addEventListener('odm:done', goHomeAll);
  canvas.addEventListener('soft:reset', goHomeAll); // 그 밖 오류 화면: 흩어진 뒤 정해진 시간이 지나면 제자리로
  // 그 밖 오류 화면: 가로 번개에 맞아 글자가 위·아래 반쪽으로 갈라져 흩어진다 (위쪽은 위로, 아래쪽은 아래로, 옆으로 조금씩)
  canvas.addEventListener('soft:split', () => {
    splitOpen = true;
    for (const b of bodies) {
      const dx = (Math.random() - 0.5) * 1.2;
      const dy = b.top ? -1 : 1;
      const d = Math.hypot(dx, dy);
      knock(b, dx / d, dy / d);
    }
  });  // 404 연출: 입체기동 캐릭터가 다 지나가면 제자리로
  // 403 연출: 글자마다 정한 방향의 무대 바깥으로 옮겨 둔 뒤 제자리로 부른다. 보이지 않는 벽에 막혀 테두리에
  // 눌렸다가 출렁이며("띠용") 제자리로 튀어 들어온다.
  // ENTER_FROM[글자 순서] = [가로, 세로] 이동 비율 (무대 폭·높이 기준). 403: 4 = 왼쪽 위, 0 = 아래, 3 = 오른쪽
  const ENTER_FROM = [[-0.55, -0.8], [0, 0.85], [0.6, 0]];
  canvas.addEventListener('soft:enter', () => {
    bodies.forEach((b) => {
      const [fx, fy] = ENTER_FROM[b.glyph % ENTER_FROM.length];
      const dx = fx * width;
      const dy = fy * height;
      for (const p of b.particles) {
        p.x += dx; p.y += dy; p.px = p.x; p.py = p.y;
      }
    });
    walls();
    goHomeAll();
  });
  // 404 연출: 입체기동 캐릭터(error-odm.js)가 지나가며 닿은 글자를 캐릭터가 가는 쪽(오른쪽)으로 쳐 낸다.
  //   detail = { x, y, r }: 캐릭터 가운데와 닿는 거리 (이 캔버스 좌표, 두 캔버스는 같은 자리·크기)
  canvas.addEventListener('soft:hit', (e) => {
    const { x, y, r } = e.detail;
    for (const b of bodies) {
      if (frameNo - (b.knockedAt ?? -Infinity) < KNOCK_COOLDOWN) continue;
      if (!b.particles.some((p) => Math.hypot(p.x - x, p.y - y) < r)) continue;
      // 부딪힌 자리에서 글자 쪽 방향 + 캐릭터가 가는 오른쪽, 살짝 위로
      const d = Math.hypot(b.cx - x, b.cy - y) || 1;
      const dx = (b.cx - x) / d + 0.8;
      const dy = (b.cy - y) / d - 0.3;
      const n = Math.hypot(dx, dy) || 1;
      knock(b, dx / n, dy / n);
    }
  });
  // 입체기동 캐릭터가 나타나면 내려간다. 버티는 중이면 바로 내려가는 컷으로 넘긴다
  canvas.addEventListener('titan:release', () => {
    titan.released = true;
    if (titan.frame >= TITAN_HOLD[0] && titan.frame <= TITAN_HOLD[1]) {
      titan.frame = TITAN_HOLD[1];
      titan.next = performance.now();
    }
  });

  let resizeTimer;
  window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => { build(); if (reduceMotion) draw(); }, 150);
  });

  document.fonts.ready.then(() => {
    build();
    // 보이는 캔버스가 글자를 그리면 대체 번호를 숨긴다. 403 의 숨겨 둔 캔버스(is-overlay)는 숨기지 않는다 (error-403.js 가 숨긴다)
    if (!canvas.classList.contains('is-overlay')) document.querySelector('[data-soft-fallback]')?.setAttribute('hidden', '');
    if (reduceMotion) { draw(); return; } // 움직임 줄이기: 한 번만 그리고 물리 계산·거인 연출을 돌리지 않는다
    loadTitan();
    requestAnimationFrame(loop);
  });
})();
