/*
 * error-403.js  (오류 화면 연출, #470)
 * 403 화면 연출 (한 번만). 라이브러리 없이 Canvas 2D.
 *
 *  1) 손: 가운데 "403"(처음부터 끝까지 같은 크기) 위에 손가락으로 만든 구멍을 맞춰 손을 덮는다. 컷이 넘어갈수록
 *     구멍이 커져 시점이 구멍 안으로 들어가는 것처럼 보이고, 마지막 컷 뒤 손이 흐려지며 사라진다.
 *     손 그림은 칸 경계에서 직선으로 잘려 있어 가장자리를 흐리게(페더) 그린다.
 *  2) 여우: 아래에서 올라오는 동안 숫자들이 떤다. 입을 벌리면 가운데 "0"부터 하나씩 돌며 입속으로 빨려 들어가고,
 *     입을 다무는 순간 화면이 흔들리며 숫자 부스러기가 튄다. 씹는 동안에도 부스러기가 조금씩 떨어진다.
 *  3) 돌아오기: 여우가 사라지면 말랑한 "403"(vibelog-soft.js, data-soft-wordmark)을 꺼내 soft:enter 를 보낸다.
 *     글자가 무대 아래 바깥에서 벽에 눌렸다가 "띠용" 하고 제자리로 돌아오고, 그 뒤로는 404처럼 끌어 볼 수 있다.
 *
 * 그림 (static/images/playground/)
 *  - csm-hand-ok.png: 6칸 × 4줄 = 24컷, 한 컷 362×181px. 배경과 손가락 구멍이 이미 투명한 그림을 그대로 쓴다.
 *    구멍 위치는 아래 HOLES (그림을 재서 넣은 값)
 *  - csm-fox-eat.png:   6칸 × 4줄(마지막 줄 4컷) = 22컷, 칸 폭 1774/6px. 줄마다 그림 높이가 달라 FOX_ROWS 로 자른다
 * ⚠️ 원작 애니메이션 장면을 옮긴 그림을 쓴다 (저작권 확인 필요)
 */
(() => {
  const canvas = document.querySelector('[data-error-403]');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const BASE = canvas.dataset.spriteBase;
  const TEXT = '403';
  const css = getComputedStyle(canvas.parentElement);
  const INK = css.getPropertyValue('--soft-ink').trim() || '#0f1b3d';
  const FONT = getComputedStyle(document.body).fontFamily;

  // 손 (24컷). 구멍 가운데 (cx, cy, 컷 안 원본 좌표). 0~21컷은 닫힌 구멍, 22~23컷은 구멍이 열려 투명한 곳 전체의 가운데를 잰 값
  const HAND = { src: 'csm-hand-ok.png', cols: 6, frames: 24, cellW: 362, cellH: 181, frameMs: 1200 / 24 };
  const HOLES = [
    [171, 107], [177, 107], [179, 104], [182, 102], [182, 100], [175, 98],
    [162, 102], [166, 101], [169, 100], [167, 97], [169, 95], [172, 93],
    [166, 92], [167, 90], [168, 89], [164, 88], [161, 86], [162, 83],
    [157, 88], [159, 89], [159, 91], [158, 94], [159, 98], [161, 98],
  ];
  const HAND_FADE_MS = 30;    // 마지막 컷 뒤 손이 흐려지며 사라지는 시간 (여우가 나오는 것과 겹친다)
  const FEATHER = [0.12, 0.06]; // 가장자리를 흐리게 하는 폭 (가로·세로, 칸 크기 비율)

  // 여우 (22컷). 줄마다 [위, 아래] (원본 px). 그림 아래끝을 맞춰 무대 아래에 붙인다
  // openFrame~eatFrame-1: 입을 벌린 컷 (숫자를 빨아들인다), eatFrame: 입을 다문 첫 컷 (먹는다)
  const FOX = { src: 'csm-fox-eat.png', cols: 6, frames: 22, frameMs: 1400 / 15, openFrame: 6, eatFrame: 12, chewEnd: 18 };
  // 처음 몇 컷(올라오기)은 따로 빠르게: 앞의 FAST_FRAMES 컷을 합쳐 FAST_MS 동안, 나머지는 한 컷 frameMs
  const FOX_FAST = { frames: 7, ms: 300 };
  const foxFrameMs = (f) => (f < FOX_FAST.frames ? FOX_FAST.ms / FOX_FAST.frames : FOX.frameMs);
  // f 번째 컷이 시작되는 시각(ms, 여우 장면 안에서). f = FOX.frames 면 여우 장면 전체 길이
  const foxTimeOf = (f) => {
    let t = 0;
    for (let i = 0; i < f; i++) t += foxFrameMs(i);
    return t;
  };
  // 여우 장면 안 시각 t 에 보일 컷
  const foxFrameAt = (t) => {
    let f = 0;
    while (f < FOX.frames - 1 && t >= foxTimeOf(f + 1)) f++;
    return f;
  };
  const FOX_MOUTH = [0.27, 0.42]; // 입속 자리 (컷 폭·높이 비율, 7~12컷을 재서 넣은 값). 이 자리를 무대 가운데에 둔다
  const FOX_ROWS = [[17, 239], [239, 459], [459, 663], [663, 878]];
  const FOX_HEIGHT = 0.95;       // 가장 큰 줄 높이를 무대 높이의 이만큼으로
  const SUCK_ORDER = [1, 0, 2];  // 빨려 들어가는 순서별 늦게 출발하는 정도: "4"=1, "0"=0(먼저), "3"=2
  const SHAKE_MS = 280;          // 먹는 순간 화면 흔들림
  const SHAKE_PX = 7;

  const START_MS = 400;     // 페이지를 연 뒤 시작까지
  const BETWEEN_MS = 0;     // 손 마지막 컷이 끝나고 여우가 나오기까지 (0 = 바로, 손이 흐려지는 동안 겹쳐 나온다)
  const AFTER_FOX_MS = 3000; // 여우가 사라진 뒤 말랑 "403"이 튀어 들어오기까지 (빈 무대에 부스러기만 떨어진다)
  const CRUMBS_MS = 1200;   // 말랑 "403"을 꺼낸 뒤 남은 부스러기가 떨어지는 시간

  let width = 0;
  let height = 0;
  const hand = new Image();
  const fox = new Image();
  let handFrames = [];      // 가장자리를 흐리게 처리한 손 컷 (원본 크기)
  let crumbs = [];          // 숫자 부스러기
  let lastNow = 0;
  const soft = document.querySelector('[data-soft-wordmark]'); // 마지막 장면의 말랑 "403"
  let handedOff = false;

  // 연출 속 숫자를 지우고 말랑 "403"을 꺼낸다. 이후로는 이 캔버스에 숫자를 그리지 않는다
  function handOff(animated) {
    if (handedOff || !soft) return;
    handedOff = true;
    ctx.clearRect(0, 0, width, height); // 연출 속 숫자를 지워 말랑 "403"과 겹치지 않게
    soft.classList.add('is-shown');
    if (animated) soft.dispatchEvent(new CustomEvent('soft:enter'));
  }

  function resize() {
    const r = canvas.getBoundingClientRect();
    width = r.width;
    height = r.height;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  const normalSize = () => Math.min(height * 0.62, width * 0.3);

  /* ── 숫자 ─────────────────────────────────────── */

  // 숫자 하나씩의 제자리 (가운데 정렬한 "403"에서 각 글자의 가운데)
  function digitHomes() {
    ctx.font = `900 ${normalSize()}px ${FONT}`;
    const widths = [...TEXT].map((ch) => ctx.measureText(ch).width);
    const total = widths.reduce((a, b) => a + b, 0);
    let x = width / 2 - total / 2;
    return widths.map((w) => {
      const home = { x: x + w / 2, y: height / 2 };
      x += w;
      return home;
    });
  }

  // d: [{x, y, scale, rot, alpha}] 숫자마다
  function drawDigits(d) {
    ctx.save();
    ctx.fillStyle = INK;
    ctx.font = `900 ${normalSize()}px ${FONT}`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    [...TEXT].forEach((ch, i) => {
      const s = d[i];
      if (!s || s.alpha <= 0 || s.scale <= 0) return;
      ctx.save();
      ctx.globalAlpha = s.alpha;
      ctx.translate(s.x, s.y);
      ctx.rotate(s.rot);
      ctx.scale(s.scale, s.scale);
      ctx.fillText(ch, 0, 0);
      ctx.restore();
    });
    ctx.restore();
  }

  const still = (alpha = 1) => digitHomes().map((h) => ({ ...h, scale: 1, rot: 0, alpha }));

  /* ── 손 ───────────────────────────────────────── */

  // 컷마다 잘라, 가장자리를 투명하게 흐린 캔버스를 미리 만든다 (칸 경계의 직선이 안 보이게)
  function prepareHand() {
    const bandH = HAND.cellH;
    handFrames = Array.from({ length: HAND.frames }, (_, f) => {
      const off = document.createElement('canvas');
      off.width = HAND.cellW;
      off.height = bandH;
      const g = off.getContext('2d');
      g.drawImage(hand, (f % HAND.cols) * HAND.cellW, Math.floor(f / HAND.cols) * bandH, HAND.cellW, bandH,
        0, 0, HAND.cellW, bandH);
      g.globalCompositeOperation = 'destination-in';
      const fade = (grad, edge) => {
        grad.addColorStop(0, 'rgba(0,0,0,0)');
        grad.addColorStop(edge, 'rgba(0,0,0,1)');
        grad.addColorStop(1 - edge, 'rgba(0,0,0,1)');
        grad.addColorStop(1, 'rgba(0,0,0,0)');
        return grad;
      };
      g.fillStyle = fade(g.createLinearGradient(0, 0, HAND.cellW, 0), FEATHER[0]);
      g.fillRect(0, 0, HAND.cellW, bandH);
      g.fillStyle = fade(g.createLinearGradient(0, 0, 0, bandH), FEATHER[1]);
      g.fillRect(0, 0, HAND.cellW, bandH);
      return off;
    });
  }

  // 손 한 컷: 구멍 가운데를 무대 가운데("403" 가운데)에 맞춰 덮는다
  function drawHand(f, alpha = 1) {
    const img = handFrames[f];
    const s = height / img.height;
    const [cx, cy] = HOLES[f];
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.drawImage(img, width / 2 - cx * s, height / 2 - cy * s, img.width * s, img.height * s);
    ctx.restore();
  }

  /* ── 여우 ─────────────────────────────────────── */

  // 컷 그리기 + 입속 자리 (무대 좌표)
  function foxBox(f) {
    const cw = fox.width / FOX.cols;
    const [top, bottom] = FOX_ROWS[Math.floor(f / FOX.cols)];
    const tallest = Math.max(...FOX_ROWS.map(([t, b]) => b - t));
    const k = (height * FOX_HEIGHT) / tallest;
    const w = cw * k;
    const h = (bottom - top) * k;
    const x = width / 2 - w * FOX_MOUTH[0];
    const y = height - h;
    return { cw, top, bottom, x, y, w, h, mouth: { x: x + w * FOX_MOUTH[0], y: y + h * FOX_MOUTH[1] } };
  }

  function drawFox(f) {
    const b = foxBox(f);
    ctx.drawImage(fox, (f % FOX.cols) * b.cw, b.top, b.cw, b.bottom - b.top, b.x, b.y, b.w, b.h);
  }

  const easeIn = (q) => q * q * q;
  const clamp01 = (v) => Math.max(0, Math.min(1, v));

  // 여우 장면의 숫자: 떨기 → 빨려 들어가기 → 없음
  function foxDigits(t, f) {
    const homes = digitHomes();
    if (f < FOX.openFrame) {
      // 여우가 다가올수록 더 크게 떤다
      const amp = 0.6 + 2.6 * (t / foxTimeOf(FOX.openFrame));
      return homes.map((h, i) => ({
        x: h.x + Math.sin(t * 0.09 + i * 2.1) * amp,
        y: h.y + Math.cos(t * 0.11 + i * 1.7) * amp,
        scale: 1, rot: Math.sin(t * 0.07 + i) * 0.02 * amp, alpha: 1,
      }));
    }
    if (f >= FOX.eatFrame) return [];
    const p = (t - foxTimeOf(FOX.openFrame)) / (foxTimeOf(FOX.eatFrame) - foxTimeOf(FOX.openFrame)); // 0 → 1
    const mouth = foxBox(f).mouth;
    return homes.map((h, i) => {
      const q = clamp01((p - SUCK_ORDER[i] * 0.16) / 0.62); // 숫자마다 조금씩 늦게 출발
      const e = easeIn(q);
      const side = h.x < mouth.x ? -1 : h.x > mouth.x ? 1 : 0;
      return {
        // 입 쪽으로 가면서 살짝 위로 휘어 빨려 들어간다
        x: h.x + (mouth.x - h.x) * e,
        y: h.y + (mouth.y - h.y) * e - Math.sin(Math.PI * q) * height * 0.08,
        scale: 1 - 0.92 * e,
        rot: side * e * 1.4 + (side === 0 ? e * 2.2 : 0),
        alpha: q < 0.85 ? 1 : (1 - q) / 0.15,
      };
    });
  }

  function spawnCrumbs(at, count, power) {
    for (let i = 0; i < count; i++) {
      const a = Math.random() * Math.PI * 2;
      const v = (0.4 + Math.random()) * power;
      crumbs.push({
        x: at.x + (Math.random() - 0.5) * 24, y: at.y + (Math.random() - 0.5) * 12,
        vx: Math.cos(a) * v, vy: Math.sin(a) * v - power * 0.6,
        size: 3 + Math.random() * 5, rot: Math.random() * Math.PI, spin: (Math.random() - 0.5) * 0.3, life: 1,
      });
    }
  }

  // 부스러기: 중력을 받아 떨어지고 점점 사라진다 (dt: 초)
  function drawCrumbs(dt) {
    const step = dt * 60;
    crumbs = crumbs.filter((c) => c.life > 0 && c.y < height + 20);
    ctx.save();
    ctx.fillStyle = INK;
    for (const c of crumbs) {
      c.vy += 0.35 * step;
      c.x += c.vx * step;
      c.y += c.vy * step;
      c.rot += c.spin * step;
      c.life -= dt / 1.1;
      ctx.globalAlpha = Math.max(0, c.life);
      ctx.save();
      ctx.translate(c.x, c.y);
      ctx.rotate(c.rot);
      ctx.fillRect(-c.size / 2, -c.size / 2, c.size, c.size * 0.7);
      ctx.restore();
    }
    ctx.restore();
  }

  /* ── 장면 순서 ───────────────────────────────── */

  // 장면 목록: [끝나는 시각(ms), 그리는 함수(지금 장면 안에서 지난 시간 ms, 이번 프레임 간격 초)]
  function timeline() {
    const handEnd = START_MS + HAND.frames * HAND.frameMs;
    // 손 마지막 컷이 끝나면 여우가 바로 나오고, 처음 HAND_FADE_MS 동안 손이 그 위에서 흐려지며 겹친다
    const foxStart = handEnd + BETWEEN_MS;
    const foxEnd = foxStart + foxTimeOf(FOX.frames);
    let lastFoxFrame = -1;
    return [
      [START_MS, () => { drawDigits(still()); drawHand(0); }],
      [handEnd, (t) => { drawDigits(still()); drawHand(Math.min(HAND.frames - 1, Math.floor(t / HAND.frameMs))); }],
      [foxStart, () => drawDigits(still())],
      [foxEnd, (t, dt) => {
        const f = foxFrameAt(t);
        if (f !== lastFoxFrame) { // 컷이 바뀔 때 한 번씩
          if (f === FOX.eatFrame) spawnCrumbs(foxBox(f).mouth, 16, 5);              // 먹는 순간
          else if (f > FOX.eatFrame && f < FOX.chewEnd) spawnCrumbs(foxBox(f).mouth, 2, 2); // 씹는 동안
          lastFoxFrame = f;
        }
        const sinceEat = t - foxTimeOf(FOX.eatFrame);
        const shaking = sinceEat >= 0 && sinceEat < SHAKE_MS;
        ctx.save();
        if (shaking) {
          const a = SHAKE_PX * (1 - sinceEat / SHAKE_MS);
          ctx.translate((Math.random() - 0.5) * 2 * a, (Math.random() - 0.5) * 2 * a);
        }
        drawFox(f);
        drawDigits(foxDigits(t, f)); // 여우 위에 그려야 입속으로 들어가는 것이 보인다
        drawCrumbs(dt);
        ctx.restore();
        // 여우가 나오는 동안 손 마지막 컷이 맨 위에서 흐려지며 사라진다
        if (t < HAND_FADE_MS) drawHand(HAND.frames - 1, 1 - t / HAND_FADE_MS);
      }],
      [foxEnd + AFTER_FOX_MS, (t, dt) => drawCrumbs(dt)],
      [foxEnd + AFTER_FOX_MS + CRUMBS_MS, (t, dt) => { handOff(true); drawCrumbs(dt); }],
    ];
  }

  function play() {
    const scenes = timeline();
    const start = performance.now();
    lastNow = start;
    const frame = (now) => {
      const t = now - start;
      const dt = Math.min((now - lastNow) / 1000, 1 / 30);
      lastNow = now;
      ctx.clearRect(0, 0, width, height);
      let from = 0;
      for (const [end, draw] of scenes) {
        if (t < end) {
          draw(t - from, dt);
          requestAnimationFrame(frame);
          return;
        }
        from = end;
      }
      handOff(true); // 끝: 말랑 "403"이 남는다 (일회성)
    };
    requestAnimationFrame(frame);
  }

  const load = (img, src) => new Promise((resolve, reject) => {
    img.onload = resolve;
    img.onerror = reject;
    img.src = BASE + src;
  });

  document.fonts.ready.then(() => {
    resize();
    if (reduceMotion) { handOff(false); return; } // 움직임 줄이기: 연출 없이 말랑 "403"만 제자리에
    // 그림을 기다리는 동안에도 숫자가 보이게 먼저 그려 두고, 대체 번호(data-soft-fallback)를 숨긴다
    drawDigits(still());
    document.querySelector('[data-soft-fallback]')?.setAttribute('hidden', '');
    Promise.all([load(hand, HAND.src), load(fox, FOX.src)])
      .then(() => { prepareHand(); play(); }, () => handOff(false)); // 그림을 못 불러오면 말랑 "403"만
  });
  window.addEventListener('resize', () => {
    resize(); // 연출 중이면 다음 프레임이 다시 그린다. 끝난 뒤에는 말랑 "403"이 스스로 다시 만든다
  });
})();
