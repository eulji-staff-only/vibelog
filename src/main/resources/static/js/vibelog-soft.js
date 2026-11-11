/*
 * vibelog-soft.js  (U-01 메인 히어로 장식, #468)
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
 */
(() => {
  const canvas = document.querySelector('[data-soft-wordmark]');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const WORD = 'vibelog';
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

  let bodies = [];
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
    return { mask, w, h, advance: w - pad * 2, pad };
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
    const { mask, w, h, advance, pad } = glyphMask(ch);
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
    return { shapes, advance, pad };
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
    for (const g of glyphs) {
      for (const shape of g.shapes) {
        bodies.push(makeBody(shape.outline, shape.holes, x - g.pad * scale, y - g.pad * scale, scale));
      }
      x += (g.advance + gap) * scale;
    }
    // 처음에는 멈춘 채로 둔다. 직전 위치를 흩어 두면 글자 전체가 위아래로 밀려 벽까지 떠내려간다
  }

  /* ── 3. 물리 한 걸음 ─────────────────────────────── */

  function integrate() {
    for (const b of bodies) {
      for (const p of b.particles) {
        let vx = (p.x - p.px) * DAMPING;
        let vy = (p.y - p.py) * DAMPING;
        const speed = Math.hypot(vx, vy);
        if (speed > MAX_SPEED) { vx *= MAX_SPEED / speed; vy *= MAX_SPEED / speed; }
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
    for (const b of bodies) {
      for (const p of b.particles) {
        if (p.x < m) p.x = m;
        if (p.x > width - m) p.x = width - m;
        if (p.y < m) p.y = m;
        if (p.y > height - m) p.y = height - m;
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

  function step() {
    integrate();
    const resetting = resetLeft > 0;
    for (let i = 0; i < SUBSTEPS; i++) {
      if (resetting) goHome(); else pull();
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
    ctx.lineWidth = 2;
    ctx.lineJoin = 'round';
    for (const b of bodies) {
      ctx.beginPath();
      smoothPath(b.particles.map((p) => [p.x, p.y]));
      ctx.fillStyle = PAPER;
      ctx.fill();
      ctx.strokeStyle = INK;
      ctx.stroke();
      // 구멍은 도형의 지금 위치·회전을 따라간다
      const c = Math.cos(b.angle);
      const s = Math.sin(b.angle);
      for (const hole of b.holes) {
        ctx.beginPath();
        smoothPath(hole.map((h) => [b.cx + h.rx * c - h.ry * s, b.cy + h.rx * s + h.ry * c]));
        ctx.stroke();
      }
    }
    if (drag) {
      ctx.beginPath();
      ctx.arc(drag.x, drag.y, 5, 0, Math.PI * 2);
      ctx.fillStyle = ACCENT;
      ctx.fill();
    }
  }

  function loop() {
    step();
    draw();
    requestAnimationFrame(loop);
  }

  /* ── 5. 끌기 ─────────────────────────────────────── */

  function pointer(e) {
    const r = canvas.getBoundingClientRect();
    return { x: e.clientX - r.left, y: e.clientY - r.top };
  }

  canvas.addEventListener('pointerdown', (e) => {
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
    drag = { x, y, grab };
    canvas.setPointerCapture(e.pointerId);
    canvas.classList.add('is-dragging');
  });
  canvas.addEventListener('pointermove', (e) => {
    if (!drag) return;
    Object.assign(drag, pointer(e));
  });
  const release = () => {
    drag = null;
    canvas.classList.remove('is-dragging');
  };
  canvas.addEventListener('pointerup', release);
  canvas.addEventListener('pointercancel', release);
  canvas.addEventListener('dblclick', () => { // 두 번 누르면 제자리로 (애니메이션)
    drag = null;
    resetLeft = RESET_FRAMES;
  });

  let resizeTimer;
  window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(build, 150);
  });

  document.fonts.ready.then(() => {
    build();
    requestAnimationFrame(loop);
  });
})();
