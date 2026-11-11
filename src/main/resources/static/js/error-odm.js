/*
 * error-odm.js  (오류 화면 연출, #470)
 * 404 화면 연출의 두 번째 장면. 입체기동 캐릭터들이 왼쪽에서 들어와 줄을 걸고 그네를 타며 오른쪽으로 지나간다. 한 번만.
 *
 * 순서 (vibelog-soft.js 와 canvas 이벤트로 주고받는다)
 *  1) 거인이 다 올라오면 말랑 글자 캔버스가 titan:up 을 보낸다 → 글자가 날아다니는 걸 잠깐 보여 준 뒤 출발
 *  2) 첫 캐릭터가 거인 가까이(무대 폭의 RELEASE_AT) 오면 titan:release 를 보낸다 → 거인이 내려가고 글자가 제자리로 돌아간다
 *  3) 캐릭터가 모두 오른쪽 밖으로 나가면 odm:done 을 보내고(글자가 제자리로 돌아간다) 그리기를 멈춘다
 *
 * 그림: static/images/playground/odm-1~4.png (캐릭터마다 한 장, 가로로 12컷, 한 컷 181×220px, 오른쪽을 본다)
 * ⚠️ 원작 캐릭터 팬아트를 쓴다 (저작권 확인 필요)
 */
(() => {
  const canvas = document.querySelector('[data-error-odm]');
  const stage = document.querySelector('[data-soft-wordmark]');
  if (!canvas || !stage) return;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const ctx = canvas.getContext('2d');

  const SPRITE_BASE = canvas.dataset.spriteBase;
  const SHEETS = ['odm-1.png', 'odm-2.png', 'odm-3.png', 'odm-4.png'];
  const CELL_W = 181;
  const CELL_H = 220;
  const SWING = [6, 7, 4, 8];   // 그네 타는 컷
  const BOOST = [9, 10, 11, 5]; // 줄을 놓고 가스를 뿜는 컷
  const FRAME_TIME = 0.09;      // 한 컷 시간(초)
  const START_DELAY = 0.9;      // 거인이 올라온 뒤 출발까지(초). 그동안 글자가 날아다닌다
  const MOVE_SPEED = 0.85;      // 캐릭터 이동 빠르기 (1 = 원래 속도). 작을수록 느리다
  const CROSS_TIME = 2.6 / MOVE_SPEED; // 왼쪽 밖 → 오른쪽 밖까지 걸리는 시간(초)
  const RELEASE_AT = 0.32;      // 첫 캐릭터가 무대 폭의 이 비율까지 오면(거인 가까이) 거인이 내려간다
  const SEGMENT = 230;          // 그네 한 번에 가는 거리(px)
  const LINE = 'rgba(15, 27, 61, 0.35)';
  // 캐릭터마다: 출발 간격(초), 크기, 진하기, 그네 높이(위에서 px)와 깊이
  const CAST = [
    { sheet: 0, delay: 0, scale: 0.42, alpha: 1, top: 40, depth: 80 },
    { sheet: 1, delay: 0.35, scale: 0.3, alpha: 0.75, top: 24, depth: 60 },
    { sheet: 2, delay: 0.7, scale: 0.42, alpha: 1, top: 60, depth: 90 },
    { sheet: 3, delay: 1.05, scale: 0.3, alpha: 0.75, top: 30, depth: 70 },
  ];

  const images = SHEETS.map(() => new Image());
  let width = 0;
  let height = 0;
  let flyers = [];
  let released = false;
  let last = 0;

  function resize() {
    const r = canvas.getBoundingClientRect();
    width = r.width;
    height = r.height;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  // x 는 시간에 따라 일정하게 오른쪽으로. y 는 그네 한 번(SEGMENT)마다 아래로 휘었다 올라오는 호
  function place(f) {
    const speed = (width + 240) / CROSS_TIME;
    f.x = -120 + f.t * speed;
    const u = (((f.x + 120) % SEGMENT) + SEGMENT) % SEGMENT / SEGMENT; // 0 → 1 (그네 한 번 안에서)
    f.u = u;
    f.y = f.top + f.depth * Math.sin(Math.PI * u);
    // 진행 방향 기울기: dy/dx
    f.tilt = Math.max(-0.45, Math.min(0.45, Math.atan2(f.depth * Math.PI * Math.cos(Math.PI * u), SEGMENT) * 0.5));
    f.anchorX = f.x - u * SEGMENT + SEGMENT / 2; // 이번 그네의 고정점: 호 가운데 위쪽
  }

  function draw(f) {
    if (f.t < 0 || f.x > width + 120) return;
    const swinging = f.u < 0.75;
    ctx.globalAlpha = f.alpha;
    if (swinging) { // 줄: 허리에서 고정점(캔버스 위쪽)까지
      ctx.strokeStyle = LINE;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(f.x, f.y);
      ctx.lineTo(f.anchorX, 0);
      ctx.stroke();
    }
    const list = swinging ? SWING : BOOST;
    const cell = list[Math.floor(f.t / FRAME_TIME) % list.length];
    const w = CELL_W * f.scale;
    const h = CELL_H * f.scale;
    ctx.save();
    ctx.translate(f.x, f.y);
    ctx.rotate(f.tilt);
    ctx.drawImage(images[f.sheet], cell * CELL_W, 0, CELL_W, CELL_H, -w / 2, -h / 2, w, h);
    ctx.restore();
    ctx.globalAlpha = 1;
  }

  function loop(now) {
    const dt = Math.min((now - last) / 1000, 1 / 30);
    last = now;
    ctx.clearRect(0, 0, width, height);
    for (const f of flyers) {
      f.t += dt;
      if (f.t >= 0) place(f);
      draw(f);
      // 첫 캐릭터가 거인 가까이 오면 거인을 내려보낸다 (거인은 무대 가운데에 있다)
      if (!released && f.t >= 0 && f.x > width * RELEASE_AT) {
        released = true;
        stage.dispatchEvent(new CustomEvent('titan:release'));
      }
    }
    if (flyers.every((f) => f.x > width + 120)) { // 모두 지나갔으면 끝 (일회성)
      ctx.clearRect(0, 0, width, height);
      stage.dispatchEvent(new CustomEvent('odm:done'));
      return;
    }
    requestAnimationFrame(loop);
  }

  function start() {
    resize();
    flyers = CAST.map((c) => ({ ...c, t: -(START_DELAY + c.delay), x: -Infinity, u: 0 }));
    last = performance.now();
    requestAnimationFrame(loop);
  }

  const loaded = Promise.all(images.map((img, i) => new Promise((resolve, reject) => {
    img.onload = resolve;
    img.onerror = reject;
    img.src = SPRITE_BASE + SHEETS[i];
  })));
  // 그림을 못 불러오면 캐릭터 없이 거인을 바로 내려보내 글자가 제자리로 돌아오게 한다
  stage.addEventListener('titan:up', () => {
    loaded.then(start, () => {
      stage.dispatchEvent(new CustomEvent('titan:release'));
      stage.dispatchEvent(new CustomEvent('odm:done'));
    });
  }, { once: true });
  window.addEventListener('resize', resize);
})();
