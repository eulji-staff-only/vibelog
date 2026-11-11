/*
 * error-400.js  (오류 화면 연출, #470)
 * "그 밖" 오류 화면(400·405·409·429 등) 연출 (한 번만). 라이브러리 없이 Canvas 2D.
 * 글자는 말랑한 상태 번호(vibelog-soft.js, data-soft-split="true")이고, 이 파일은 그 위 캔버스에 캐릭터·번개만 그린다.
 *
 *  1) 젠이츠: 글자 오른쪽에서 기를 모으고 왼쪽으로 질주해 사라진다 (24컷)
 *  2) 번개: 젠이츠가 사라지는 컷(꼬리·먼지가 남은 때)부터 겹쳐서, 오른쪽에서 왼쪽으로 뻗는 가로 번개가 글자를 가로지른다 (44컷). 가로선을 글자를 가르는 줄(cutY)에 맞춘다
 *  3) 번개가 끝까지 뻗는 컷(SPLIT_FRAME)에서 soft:split → 글자가 위·아래 반쪽으로 갈라져 흩어진다
 *  4) 갈라진 지 RESET_MS 뒤 soft:reset → 출렁이며 제자리로 돌아와 한 글자로 붙는다
 *
 * 그림 (static/images/playground/)
 *  - zenitsu-dash.png: 6칸 × 4줄 = 24컷, 칸 폭 362px. 줄마다 그림 높이가 달라 181px 씩 자르면 위·아래 줄 그림이 섞여 들어온다
 *    → 줄마다 그림이 있는 범위(HERO_ROWS, 원본을 재서 넣은 값)로 자르고 아래끝을 맞춘다
 *  - lightning-flicker.png: 가로 44컷, 한 컷 360×210px (원본 720×420 을 절반으로 줄인 사본. 원본 폭 31,680px 은 너무 커서)
 * ⚠️ 원작 애니메이션 캐릭터 그림을 쓴다 (저작권 확인 필요)
 */
(() => {
  const canvas = document.querySelector('[data-error-400]');
  const soft = document.querySelector('[data-soft-wordmark]');
  if (!canvas || !soft) return;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return; // 글자만 가만히 보인다
  const ctx = canvas.getContext('2d');
  const BASE = canvas.dataset.spriteBase;

  const HERO = { src: 'zenitsu-dash.png', cols: 6, frames: 24, cellW: 362, frameMs: 70 };
  // 줄마다 [위, 아래] (원본 px). 그림: 1줄 8~194, 2줄 210~402, 3줄 410~585, 4줄 603~718 → 그 사이 빈 곳에서 나눈다
  const HERO_ROWS = [[0, 202], [202, 406], [406, 594], [594, 724]];
  const HERO_HEIGHT = 0.6;   // 가장 큰 줄 높이를 무대 높이의 이만큼으로
  const FX = { src: 'lightning-flicker.png', frames: 44, cellW: 360, cellH: 210, frameMs: 400 / 44 }; // 44컷을 0.4초에
  const FX_LINE_Y = 0.564;   // 번개 가로선 높이 (컷 높이 대비, 12번 컷을 재서 넣은 값)
  const FX_WIDTH = 2.6;      // 번개 폭 (글자 폭 대비)
  const FX_FEATHER = [0.18, 0.2]; // 번개 컷 가장자리를 흐리게 하는 폭 (가로·세로, 컷 크기 비율). 컷 경계에서 선이 뚝 끊겨 보이지 않게
  const SPLIT_FRAME = 12;    // 번개가 글자를 다 가로지르는 컷 → 이때 글자가 갈라진다
  const HERO_GONE_FRAME = 16; // 젠이츠가 사라지는 컷 (16~17 번개 꼬리, 18~23 먼지). 이때부터 번개를 겹쳐 시작한다
  const START_MS = 400;      // 페이지를 연 뒤 시작까지
  const RESET_MS = 3000;     // 갈라진 뒤 제자리로 돌아가기까지

  const hero = new Image();
  const fx = new Image();
  let fxFrames = [];          // 가장자리를 흐리게 처리한 번개 컷
  let width = 0;
  let height = 0;
  let layout = soft.softLayout || null; // 글자 자리 (vibelog-soft.js 가 알려 준다)
  soft.addEventListener('soft:layout', (e) => { layout = e.detail; });

  function resize() {
    const r = canvas.getBoundingClientRect();
    width = r.width;
    height = r.height;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  // 젠이츠: 줄마다 그림이 있는 범위만 잘라 같은 비율로 그린다. 아래끝(발끝)을 글자 아랫줄 조금 아래에 맞추고,
  // 왼쪽 끝을 글자 오른쪽 끝 조금 안쪽에 둔다 (무대 밖으로는 안 나가게)
  function drawHero(f) {
    const [top, bottom] = HERO_ROWS[Math.floor(f / HERO.cols)];
    const tallest = Math.max(...HERO_ROWS.map(([t, b]) => b - t));
    const k = (height * HERO_HEIGHT) / tallest;
    const w = HERO.cellW * k;
    const h = (bottom - top) * k;
    const x = Math.min(layout.x1 - w * 0.12, width - w);
    const groundY = Math.min(layout.y1 + tallest * k * 0.08, height);
    ctx.drawImage(hero, (f % HERO.cols) * HERO.cellW, top, HERO.cellW, bottom - top, x, groundY - h, w, h);
  }

  // 번개 컷마다 가장자리를 투명하게 흐린 캔버스를 미리 만든다 (403 손과 같은 방식)
  function prepareFx() {
    const fade = (g, grad, edge) => {
      grad.addColorStop(0, 'rgba(0,0,0,0)');
      grad.addColorStop(edge, 'rgba(0,0,0,1)');
      grad.addColorStop(1 - edge, 'rgba(0,0,0,1)');
      grad.addColorStop(1, 'rgba(0,0,0,0)');
      return grad;
    };
    fxFrames = Array.from({ length: FX.frames }, (_, f) => {
      const off = document.createElement('canvas');
      off.width = FX.cellW;
      off.height = FX.cellH;
      const g = off.getContext('2d');
      g.drawImage(fx, f * FX.cellW, 0, FX.cellW, FX.cellH, 0, 0, FX.cellW, FX.cellH);
      g.globalCompositeOperation = 'destination-in';
      g.fillStyle = fade(g, g.createLinearGradient(0, 0, FX.cellW, 0), FX_FEATHER[0]);
      g.fillRect(0, 0, FX.cellW, FX.cellH);
      g.fillStyle = fade(g, g.createLinearGradient(0, 0, 0, FX.cellH), FX_FEATHER[1]);
      g.fillRect(0, 0, FX.cellW, FX.cellH);
      return off;
    });
  }

  // 번개: 글자 가운데에, 가로선이 글자를 가르는 줄에 오도록
  function drawFx(f) {
    const w = (layout.x1 - layout.x0) * FX_WIDTH;
    const h = w * (FX.cellH / FX.cellW);
    const x = (layout.x0 + layout.x1) / 2 - w / 2;
    const lineY = layout.cutY ?? (layout.y0 + layout.y1) / 2;
    ctx.drawImage(fxFrames[f], x, lineY - h * FX_LINE_Y, w, h);
  }

  // 젠이츠가 사라지는 컷부터 번개를 겹쳐 시작한다: 남은 꼬리·먼지 컷 위로 번개가 지나간다
  function play() {
    const heroEnd = START_MS + HERO.frames * HERO.frameMs;
    const fxStart = START_MS + HERO_GONE_FRAME * HERO.frameMs;
    const fxEnd = fxStart + FX.frames * FX.frameMs;
    const start = performance.now();
    let split = false;
    const frame = (now) => {
      const t = now - start;
      if (t >= heroEnd && t >= fxEnd) { // 끝 (일회성). 글자는 말랑이가 알아서 제자리로 돌아온다
        ctx.clearRect(0, 0, width, height);
        return;
      }
      ctx.clearRect(0, 0, width, height);
      if (t < heroEnd) drawHero(Math.max(0, Math.min(HERO.frames - 1, Math.floor((t - START_MS) / HERO.frameMs))));
      if (t >= fxStart && t < fxEnd) {
        const f = Math.min(FX.frames - 1, Math.floor((t - fxStart) / FX.frameMs));
        drawFx(f);
        if (!split && f >= SPLIT_FRAME) {
          split = true;
          soft.dispatchEvent(new CustomEvent('soft:split'));
          setTimeout(() => soft.dispatchEvent(new CustomEvent('soft:reset')), RESET_MS);
        }
      }
      requestAnimationFrame(frame);
    };
    requestAnimationFrame(frame);
  }

  const load = (img, src) => new Promise((resolve, reject) => {
    img.onload = resolve;
    img.onerror = reject;
    img.src = BASE + src;
  });

  // 그림과 글자 자리가 모두 준비되면 시작한다. 그림을 못 불러오면 글자만 가만히 둔다
  const ready = new Promise((resolve) => {
    if (layout) resolve();
    else soft.addEventListener('soft:layout', resolve, { once: true });
  });
  Promise.all([load(hero, HERO.src), load(fx, FX.src), ready]).then(() => { prepareFx(); resize(); play(); }, () => {});
  window.addEventListener('resize', resize);
})();
