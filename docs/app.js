// Maratón Doomsday · versión web.
// Usa la MISMA base de datos y el mismo formato que la app Android, así que ambas se sincronizan.

import { initializeApp } from "./vendor/firebase-app.js";
import { getAuth, signInAnonymously, onAuthStateChanged } from "./vendor/firebase-auth.js";
import {
  initializeFirestore, getFirestore, persistentLocalCache, persistentMultipleTabManager,
  doc, collection, onSnapshot, getDocFromServer, writeBatch, setDoc, updateDoc, serverTimestamp,
} from "./vendor/firebase-firestore.js";
import { firebaseConfig } from "./firebase-config.js";

// ======================================================================
// Datos iniciales y reglas
// ======================================================================

const MARATHON_START = "2026-11-22";
const DEFAULT_RULES = [1, 0, 1, 1, 1, 1, 2]; // lunes … domingo
const DEFAULT_WHERE = "Disney+";
const ORDER_STEP = 1000;
const SEED = [
  ["X-Men", 2000, 104], ["X2", 2003, 134],
  ["Capitán América: El primer vengador", 2011, 124], ["Los Vengadores", 2012, 143],
  ["Avengers: Infinity War", 2018, 149], ["Avengers: Endgame", 2019, 181],
  ["Loki T1, episodios 1–3", 2021, 150, true], ["Loki T1, episodios 4–6", 2021, 150, true],
  ["Loki T2, episodios 1–3", 2023, 150, true], ["Loki T2, episodios 4–6", 2023, 160, true],
  ["Shang-Chi y la leyenda de los Diez Anillos", 2021, 132], ["Spider-Man: Sin regreso a casa", 2021, 148],
  ["Doctor Strange en el Multiverso de la Locura", 2022, 126], ["Black Panther: Wakanda para siempre", 2022, 161],
  ["Deadpool y Wolverine", 2024, 128], ["Capitán América: Un mundo feliz", 2025, 118],
  ["Thunderbolts*", 2025, 126], ["Los Cuatro Fantásticos: Primeros pasos", 2025, 115],
];

// ======================================================================
// Fechas (siempre como texto "AAAA-MM-DD")
// ======================================================================

const pad = (n) => String(n).padStart(2, "0");
const todayStr = () => { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };
const parse = (s) => { const [y, m, d] = s.split("-").map(Number); return new Date(Date.UTC(y, m - 1, d)); };
const fmt = (dt) => dt.toISOString().slice(0, 10);
const addDays = (s, n) => { const d = parse(s); d.setUTCDate(d.getUTCDate() + n); return fmt(d); };
const dow = (s) => (parse(s).getUTCDay() + 6) % 7; // 0 = lunes
const diffDays = (a, b) => Math.round((parse(b) - parse(a)) / 86400000);
const isDate = (s) => typeof s === "string" && /^\d{4}-\d{2}-\d{2}$/.test(s);

const DAY_NAMES = ["lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"];
const DAY_SHORT = ["lun", "mar", "mié", "jue", "vie", "sáb", "dom"];
const MONTHS = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];
const MONTH_SHORT = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
const cap1 = (s) => s.charAt(0).toUpperCase() + s.slice(1);

function longDate(s, today) {
  const d = parse(s);
  const base = `${DAY_NAMES[dow(s)]} ${d.getUTCDate()} de ${MONTHS[d.getUTCMonth()]}`;
  return d.getUTCFullYear() !== parse(today).getUTCFullYear() ? `${base} de ${d.getUTCFullYear()}` : base;
}
const shortDate = (s) => { const d = parse(s); return `${DAY_SHORT[dow(s)]} ${d.getUTCDate()} ${MONTH_SHORT[d.getUTCMonth()]}`; };
function relShort(s, today) {
  if (s === today) return "Hoy";
  if (s === addDays(today, 1)) return "Mañana";
  if (s === addDays(today, -1)) return "Ayer";
  return shortDate(s);
}
function relLong(s, today) {
  if (s === today) return `hoy, ${longDate(s, today)}`;
  if (s === addDays(today, 1)) return `mañana, ${longDate(s, today)}`;
  if (s === addDays(today, -1)) return `ayer, ${longDate(s, today)}`;
  return longDate(s, today);
}
function whenPhrase(s, today) {
  if (s === today) return "hoy";
  if (s === addDays(today, 1)) return `mañana (${longDate(s, today)})`;
  return `el ${longDate(s, today)}`;
}
function noteTime(ms) {
  const d = new Date(ms);
  const h = d.getHours() % 12 || 12;
  return `${d.getDate()} ${MONTH_SHORT[d.getMonth()]}, ${h}:${pad(d.getMinutes())} ${d.getHours() < 12 ? "a.m." : "p.m."}`;
}
function duration(min, approx) {
  if (!min || min <= 0) return null;
  const h = Math.floor(min / 60), m = min % 60;
  const t = h === 0 ? `${m} min` : m === 0 ? `${h} h` : `${h} h ${m} min`;
  return approx ? `~${t}` : t;
}
/** "38 h 12 min" */
function hoursText(min) {
  const h = Math.floor(min / 60), m = min % 60;
  return h === 0 ? `${m} min` : m === 0 ? `${h} h` : `${h} h ${m} min`;
}
const movieInfo = (m) => [m.year, duration(m.durationMin, m.approxDuration), m.where || null].filter(Boolean).join("  ·  ");

// ======================================================================
// Agenda (misma lógica que la app Android)
// ======================================================================

const capacity = (rules, s) => Math.max(0, rules[dow(s)] ?? 0);
const hasAnyDay = (rules) => rules.some((r) => r > 0);
function usedByDate(movies) {
  const m = {};
  for (const x of movies) if (x.watched && x.watchedDate) m[x.watchedDate] = (m[x.watchedDate] || 0) + 1;
  return m;
}
function assignDates(movies, rules, today, start) {
  const pending = movies.filter((m) => !m.watched);
  const out = {};
  if (!pending.length || !hasAnyDay(rules)) return out;
  const used = usedByDate(movies);
  let day = today > start ? today : start;
  for (const m of pending) {
    if (m.notBefore && m.notBefore > day) day = m.notBefore;
    while ((used[day] || 0) >= capacity(rules, day)) day = addDays(day, 1);
    out[m.id] = day;
    used[day] = (used[day] || 0) + 1;
  }
  return out;
}
function nextAvailableDay(after, rules) {
  if (!hasAnyDay(rules)) return null;
  let d = addDays(after, 1);
  while (capacity(rules, d) <= 0) d = addDays(d, 1);
  return d;
}
function availableDays(movies, rules, from, to) {
  const used = usedByDate(movies);
  let n = 0;
  for (let d = from; d <= to; d = addDays(d, 1)) if (capacity(rules, d) - (used[d] || 0) > 0) n++;
  return n;
}

function buildUi() {
  const today = S.today, info = S.info;
  const sorted = [...S.movies].sort((a, b) => a.order - b.order);
  const dates = assignDates(sorted, info.rules, today, info.startDate);
  const items = sorted.map((m) => ({ movie: m, date: m.watched ? m.watchedDate : dates[m.id] || null }));
  const notesBy = {};
  for (const n of [...S.notes].sort((a, b) => a.createdAt - b.createdAt)) (notesBy[n.movieId] ||= []).push(n);
  const total = items.length;
  const watchedCount = items.filter((i) => i.movie.watched).length;
  const ui = {
    today, info, items, notesBy, total, watchedCount,
    todayPending: items.filter((i) => !i.movie.watched && i.date === today),
    todayWatched: items.filter((i) => i.movie.watched && i.movie.watchedDate === today),
    nextPending: items.find((i) => !i.movie.watched && i.date && i.date > today) || null,
    allDone: total > 0 && watchedCount === total,
    daysToCinema: info.cinemaDate ? diffDays(today, info.cinemaDate) : null,
    behind: null,
  };
  const pendingItems = items.filter((i) => !i.movie.watched);
  ui.pendingMin = pendingItems.reduce((t, i) => t + (i.movie.durationMin || 0), 0);
  ui.pendingApprox = pendingItems.some((i) => i.movie.approxDuration || !i.movie.durationMin);
  ui.watchedMin = items.filter((i) => i.movie.watched).reduce((t, i) => t + (i.movie.durationMin || 0), 0);
  if (info.cinemaDate) {
    const pend = pendingItems;
    const last = pend.map((i) => i.date).filter(Boolean).sort().pop();
    if (pend.length && (!last || last > info.cinemaDate)) {
      ui.behind = {
        pending: pend.length,
        days: today > info.cinemaDate ? 0 : availableDays(sorted, info.rules, today, info.cinemaDate),
      };
    }
  }
  return ui;
}

// ======================================================================
// Estado
// ======================================================================

const store = {
  get: (k) => { try { return localStorage.getItem(k); } catch { return null; } },
  set: (k, v) => { try { v == null ? localStorage.removeItem(k) : localStorage.setItem(k, v); } catch {} },
};

const S = {
  code: store.get("maraton.code"),
  author: store.get("maraton.author"),
  user: null,
  authError: null,
  busy: false,
  error: null,
  info: null,
  mstate: "loading", // loading | found | notfound
  movies: [],
  notes: [],
  online: navigator.onLine,
  today: todayStr(),
  joining: false,
  joinCode: "",
  reorder: false,
  drafts: {},
  dialog: null,
  toast: null,
  createdCode: null,
};

// ======================================================================
// Firebase
// ======================================================================

const fbApp = initializeApp(firebaseConfig);
const auth = getAuth(fbApp);
let db;
try {
  db = initializeFirestore(fbApp, { localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }) });
} catch {
  db = getFirestore(fbApp);
}

const marathonRef = (c) => doc(db, "marathons", c);
const moviesCol = (c) => collection(db, "marathons", c, "movies");
const notesCol = (c) => collection(db, "marathons", c, "notes");

const logErr = (e) => console.warn("No se pudo guardar", e);

let unsubs = [];
function stopListening() { unsubs.forEach((u) => u()); unsubs = []; }
function startListening() {
  stopListening();
  if (!S.code || !S.user) return;
  const c = S.code;
  S.mstate = "loading"; S.info = null; S.movies = []; S.notes = [];
  unsubs.push(onSnapshot(marathonRef(c), (snap) => {
    if (snap.exists()) {
      const d = snap.data();
      const rules = Array.isArray(d.rules) && d.rules.length === 7 ? d.rules.map((x) => Number(x) || 0) : DEFAULT_RULES;
      S.info = {
        code: c, rules,
        cinemaDate: isDate(d.cinemaDate) ? d.cinemaDate : null,
        startDate: isDate(d.startDate) ? d.startDate : MARATHON_START,
      };
      S.mstate = "found";
    } else {
      S.mstate = snap.metadata.fromCache ? "loading" : "notfound";
    }
    render();
  }, (e) => console.warn(e)));
  unsubs.push(onSnapshot(moviesCol(c), (snap) => {
    S.movies = snap.docs.map((d) => {
      const x = d.data();
      return {
        id: d.id,
        title: x.title || "(sin título)",
        year: typeof x.year === "number" ? x.year : null,
        durationMin: typeof x.durationMin === "number" ? x.durationMin : null,
        approxDuration: !!x.approxDuration,
        where: x.where || "",
        order: typeof x.order === "number" ? x.order : 0,
        watched: !!x.watched,
        watchedDate: isDate(x.watchedDate) ? x.watchedDate : null,
        notBefore: isDate(x.notBefore) ? x.notBefore : null,
      };
    });
    render();
  }, (e) => console.warn(e)));
  unsubs.push(onSnapshot(notesCol(c), (snap) => {
    S.notes = snap.docs.map((d) => {
      const x = d.data();
      return {
        id: d.id, movieId: x.movieId || "", text: x.text || "", author: x.author || "",
        createdAt: typeof x.createdAt === "number" ? x.createdAt : 0,
        editedAt: typeof x.editedAt === "number" ? x.editedAt : null,
      };
    });
    render();
  }, (e) => console.warn(e)));
}

onAuthStateChanged(auth, (user) => {
  S.user = user;
  if (user) { S.authError = null; startListening(); }
  else signInAnonymously(auth).catch(() => {
    S.authError = "No pudimos conectarnos. Revisa que tengas internet y vuelve a intentar.";
    render();
  });
  render();
});

async function ensureAuth() {
  if (!auth.currentUser) await signInAnonymously(auth);
  S.user = auth.currentUser;
}

const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
function randomCode() {
  const a = new Uint32Array(6);
  crypto.getRandomValues(a);
  return [...a].map((n) => ALPHABET[n % ALPHABET.length]).join("");
}
const normalizeCode = (s) => s.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6);

/** Códigos de maratón que este celular ya usó, para entrar con un toque. */
function recentCodes() {
  try { const r = JSON.parse(store.get("maraton.recent") || "[]"); return Array.isArray(r) ? r.filter((x) => /^[A-Z0-9]{6}$/.test(x)) : []; }
  catch { return []; }
}
function rememberCode(c) {
  store.set("maraton.recent", JSON.stringify([c, ...recentCodes().filter((x) => x !== c)].slice(0, 5)));
}

function setCode(c) {
  S.code = c;
  store.set("maraton.code", c);
  if (c) rememberCode(c);
  if (c) startListening(); else { stopListening(); S.info = null; S.mstate = "loading"; }
}

async function createMarathon() {
  if (S.busy) return;
  S.busy = true; S.error = null; render();
  try {
    await ensureAuth();
    const start = S.today > MARATHON_START ? S.today : MARATHON_START;
    for (let i = 0; i < 10; i++) {
      const c = randomCode();
      const existing = await getDocFromServer(marathonRef(c));
      if (existing.exists()) continue;
      const b = writeBatch(db);
      b.set(marathonRef(c), { createdAt: serverTimestamp(), startDate: start, cinemaDate: null, rules: DEFAULT_RULES });
      SEED.forEach(([title, year, min, approx], idx) => {
        b.set(doc(moviesCol(c)), movieMap(title, year, min, !!approx, DEFAULT_WHERE, (idx + 1) * ORDER_STEP));
      });
      await b.commit();
      setCode(c);
      S.createdCode = c;
      return;
    }
    throw new Error("sin código");
  } catch (e) {
    console.warn(e);
    S.error = "No se pudo crear el maratón. Necesitas internet la primera vez. Inténtalo de nuevo.";
  } finally {
    S.busy = false; render();
  }
}

async function joinMarathon() {
  if (S.busy) return;
  const c = normalizeCode(S.joinCode);
  if (c.length !== 6) { S.error = "El código debe tener 6 letras o números."; render(); return; }
  S.busy = true; S.error = null; render();
  try {
    await ensureAuth();
    const snap = await getDocFromServer(marathonRef(c));
    if (snap.exists()) { setCode(c); S.joining = false; }
    else S.error = `No encontramos ningún maratón con el código ${c}. Revisa que esté bien escrito.`;
  } catch (e) {
    console.warn(e);
    S.error = "No se pudo revisar el código. Necesitas internet para unirte. Inténtalo de nuevo.";
  } finally {
    S.busy = false; render();
  }
}

function movieMap(title, year, durationMin, approx, where, order) {
  return { title, year, durationMin, approxDuration: approx, where, order, watched: false, watchedDate: null, notBefore: null };
}
const movieDoc = (id) => doc(db, "marathons", S.code, "movies", id);
const noteDoc = (id) => doc(db, "marathons", S.code, "notes", id);

const A = {
  setWatched: (m, watched, date) => updateDoc(movieDoc(m.id), { watched, watchedDate: watched ? date : null }).catch(logErr),
  setNotBefore: (m, d) => updateDoc(movieDoc(m.id), { notBefore: d }).catch(logErr),
  updateMovie: (m, f) => updateDoc(movieDoc(m.id), { title: f.title, year: f.year, durationMin: f.durationMin, approxDuration: false, where: f.where }).catch(logErr),
  addMovie: (f, maxOrder) => setDoc(doc(moviesCol(S.code)), movieMap(f.title, f.year, f.durationMin, false, f.where, maxOrder + ORDER_STEP)).catch(logErr),
  deleteMovie: (m, notes) => {
    const b = writeBatch(db);
    b.delete(movieDoc(m.id));
    notes.forEach((n) => b.delete(noteDoc(n.id)));
    b.commit().catch(logErr);
  },
  move: (items, index, delta) => {
    const movies = items.map((i) => i.movie);
    const t = index + delta;
    if (t < 0 || t >= movies.length) return;
    const re = [...movies];
    re.splice(t, 0, re.splice(index, 1)[0]);
    const b = writeBatch(db);
    let changed = false;
    re.forEach((m, i) => {
      const o = (i + 1) * ORDER_STEP;
      if (m.order !== o) { b.update(movieDoc(m.id), { order: o }); changed = true; }
    });
    if (changed) b.commit().catch(logErr);
  },
  setCinema: (d) => updateDoc(marathonRef(S.code), { cinemaDate: d }).catch(logErr),
  setRules: (r) => updateDoc(marathonRef(S.code), { rules: r }).catch(logErr),
  addNote: (movieId, text) => setDoc(doc(notesCol(S.code)), {
    movieId, text, author: S.author || "Alguien", createdAt: Date.now(), editedAt: null,
  }).catch(logErr),
  updateNote: (n, text) => updateDoc(noteDoc(n.id), { text, editedAt: Date.now() }).catch(logErr),
  deleteNote: (n) => {
    const b = writeBatch(db); b.delete(noteDoc(n.id)); b.commit().catch(logErr);
  },
};

// ======================================================================
// Fotos: las sube Cami al repositorio en docs/fotos/. Una automatización de
// GitHub genera docs/fotos/fotos.json (lista + miniaturas) en cada subida.
// ======================================================================

let gallery = null; // null = cargando, [] = sin fotos
fetch("fotos/fotos.json", { cache: "no-cache" })
  .then((r) => (r.ok ? r.json() : { fotos: [] }))
  .then((j) => { gallery = Array.isArray(j?.fotos) ? j.fotos.filter((f) => f && typeof f.archivo === "string") : []; render(); })
  .catch(() => { gallery = []; render(); });

const photoSrc = (f) => `fotos/${f.archivo.split("/").map(encodeURIComponent).join("/")}`;
const photoThumb = (f) => (f.mini ? `fotos/${f.mini.split("/").map(encodeURIComponent).join("/")}` : photoSrc(f));

// ======================================================================
// Enlace para unirse: …/#unirse=CODIGO
// ======================================================================

const baseUrl = () => location.href.split("#")[0];
const joinLink = (c) => `${baseUrl()}#unirse=${c}`;

function checkJoinLink() {
  const m = location.hash.match(/^#\/?unirse=([A-Za-z0-9]{6})/);
  if (!m) return false;
  const c = normalizeCode(m[1]);
  history.replaceState(null, "", `${location.pathname}${location.search}#hoy`);
  if (S.code === c) { render(); return true; }
  const join = () => { if (S.code) setCode(null); S.joining = true; S.joinCode = c; render(); joinMarathon(); };
  if (!S.code) join();
  else confirm("¿Cambiar de maratón?", `Este enlace es del maratón ${c}. En este celular están en el ${S.code}.`, "Cambiar", join);
  return true;
}

// ======================================================================
// Rutas: #hoy, #lista, #fotos, #ajustes, #peli/ID, #foto/ID
// ======================================================================

function route() {
  const h = location.hash.replace(/^#\/?/, "");
  if (h.startsWith("peli/")) return { tab: null, movieId: decodeURIComponent(h.slice(5)), photoId: null };
  if (h.startsWith("foto/")) return { tab: "fotos", movieId: null, photoId: decodeURIComponent(h.slice(5)) };
  return { tab: ["lista", "fotos", "ajustes"].includes(h) ? h : "hoy", movieId: null, photoId: null };
}
const go = (h) => { location.hash = h; };
function goBack() {
  if (history.length > 1 && route().movieId) history.back();
  else go("lista");
}

// ======================================================================
// Pequeñas piezas de HTML
// ======================================================================

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

const ICON = {
  check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="square"><path d="M4 12.5l5 5L20 6.5"/></svg>',
  plus: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="square"><path d="M12 4v16M4 12h16"/></svg>',
  home: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 3l9 8h-3v9h-4.5v-6h-3v6H6v-9H3z"/></svg>',
  list: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 5h3v3H3zm5 0h13v3H8zM3 10.5h3v3H3zm5 0h13v3H8zM3 16h3v3H3zm5 0h13v3H8z"/></svg>',
  gear: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10.3 2h3.4l.5 2.6 1.7.8 2.2-1.5 2.4 2.4-1.5 2.2.8 1.7 2.6.5v3.4l-2.6.5-.8 1.7 1.5 2.2-2.4 2.4-2.2-1.5-1.7.8-.5 2.6h-3.4l-.5-2.6-1.7-.8-2.2 1.5-2.4-2.4 1.5-2.2-.8-1.7L2 13.7v-3.4l2.6-.5.8-1.7-1.5-2.2 2.4-2.4 2.2 1.5 1.7-.8zM12 8.5a3.5 3.5 0 100 7 3.5 3.5 0 000-7z"/></svg>',
  back: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="square"><path d="M20 12H5M11 5l-7 7 7 7"/></svg>',
  edit: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.3V21h3.7L17.8 9.9l-3.7-3.7zM20.7 7a1 1 0 000-1.4l-2.3-2.3a1 1 0 00-1.4 0l-1.8 1.8 3.7 3.7z"/></svg>',
  trash: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 7h12l-1 14H7zM9 3h6l1 2h4v2H4V5h4z"/></svg>',
  share: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M18 16a3 3 0 00-2.1.9L8.9 13a3 3 0 000-2l7-3.9A3 3 0 1015 5a3 3 0 00.1.7l-7 3.9a3 3 0 100 4.8l7 4A3 3 0 1018 16z"/></svg>',
  camera: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9 3h6l1.5 2H21v15H3V5h4.5zM12 8a5 5 0 100 10 5 5 0 000-10zm0 2.2a2.8 2.8 0 110 5.6 2.8 2.8 0 010-5.6z"/></svg>',
  copy: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 3h11v13h-2V5H8zM4 7h11v14H4z"/></svg>',
  close: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="square"><path d="M5 5l14 14M19 5L5 19"/></svg>',
  up: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="square"><path d="M5 15l7-7 7 7"/></svg>',
  down: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="square"><path d="M5 9l7 7 7-7"/></svg>',
};

function panel({ label = null, accent = "holo", cls = "", blink = false, scan = false, act = "", data = "" }, inner) {
  return `<section class="panel a-${accent} ${cls} ${act ? "tap" : ""}" ${act ? `data-act="${act}" role="button" tabindex="0"` : ""} ${data}>
    ${scan ? '<i class="scan" aria-hidden="true"></i>' : ""}<i class="bk tr"></i><i class="bk bl"></i>
    ${label ? `<div class="plabel">${blink ? '<b class="dot blink"></b>' : '<b class="sq"></b>'}<span>${esc(label)}</span><em></em><u></u><u></u><u></u></div>` : ""}
    ${inner}</section>`;
}

function btn(text, act, { tone = "red", icon = null, size = "", disabled = false, data = "", id = "" } = {}) {
  return `<span class="bw t-${tone} ${disabled ? "dis" : ""}"><button class="btn t-${tone} ${size}" data-act="${act}" ${data} ${id ? `id="${id}"` : ""} ${disabled ? "disabled" : ""}>${icon ? ICON[icon] : ""}<span>${esc(text)}</span></button></span>`;
}

const stat = (label, value, gold = false) => `<div class="stat ${gold ? "g" : ""}"><span class="lbl">${esc(label)}</span><b>${esc(value)}</b></div>`;

function header(title, kicker) {
  return `<header class="hh"><div class="hrow"><span class="lbl holo">${esc(kicker)}</span>
    <span class="net ${S.online ? "on" : "off"}"><b class="dot blink"></b>${S.online ? "EN LÍNEA" : "SIN RED · LOCAL"}</span></div>
    <h1>${esc(title)}</h1><div class="hline"></div></header>`;
}

let gaugeSeq = 0;
function gauge(done, total, { size = 150, lit = "#FFC24A", center = "" } = {}) {
  const id = `g${++gaugeSeq}`;
  const r = size / 2, ringR = r * 0.72, stroke = r * 0.12;
  const C = 2 * Math.PI * ringR;
  const n = Math.max(total, 1);
  const gap = n > 1 ? (C * 4) / 360 : 0;
  const seg = C / n - gap;
  const k = Math.min(done, n);
  const litDash = k > 0 ? `${Array(k).fill(`${seg.toFixed(2)} ${gap.toFixed(2)}`).join(" ")} 0 ${C.toFixed(2)}` : `0 ${C.toFixed(2)}`;
  const rot = `rotate(${-90 + 2} ${r} ${r})`;
  let ticks = "";
  for (let i = 0; i < 60; i++) {
    const a = (i * 6 * Math.PI) / 180, long = i % 5 === 0;
    const o = r * 0.9, inn = o - (long ? 9 : 4);
    ticks += `<line x1="${(r + Math.cos(a) * inn).toFixed(1)}" y1="${(r + Math.sin(a) * inn).toFixed(1)}" x2="${(r + Math.cos(a) * o).toFixed(1)}" y2="${(r + Math.sin(a) * o).toFixed(1)}" stroke="rgba(92,225,255,${long ? 0.6 : 0.3})" stroke-width="1.5"/>`;
  }
  return `<div class="gauge" style="width:${size}px;height:${size}px" aria-hidden="true">
    <svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}">
      <defs><radialGradient id="${id}"><stop offset="0" stop-color="#5CE1FF" stop-opacity=".3"/><stop offset="1" stop-color="#5CE1FF" stop-opacity="0"/></radialGradient></defs>
      <circle cx="${r}" cy="${r}" r="${r * 0.62}" fill="url(#${id})"/>
      <g class="spin" style="transform-origin:${r}px ${r}px"><circle cx="${r}" cy="${r}" r="${r * 0.97 - 1}" fill="none" stroke="rgba(92,225,255,.55)" stroke-width="1.5" stroke-dasharray="7 5"/></g>
      <g class="spin-rev" style="transform-origin:${r}px ${r}px">${ticks}</g>
      <circle cx="${r}" cy="${r}" r="${ringR}" fill="none" stroke="rgba(92,225,255,.16)" stroke-width="${stroke}" stroke-dasharray="${seg.toFixed(2)} ${gap.toFixed(2)}" transform="${rot}"/>
      <circle cx="${r}" cy="${r}" r="${ringR}" fill="none" stroke="${lit}" stroke-opacity=".25" stroke-width="${stroke * 1.8}" stroke-dasharray="${litDash}" transform="${rot}"/>
      <circle cx="${r}" cy="${r}" r="${ringR}" fill="none" stroke="${lit}" stroke-width="${stroke}" stroke-dasharray="${litDash}" transform="${rot}"/>
      <circle cx="${r}" cy="${r}" r="${r * 0.56}" fill="none" stroke="rgba(92,225,255,.65)" stroke-width="1"/>
    </svg><div class="ctr">${center}</div></div>`;
}

const plural = (n, one, many) => (n === 1 ? one : many);
const authorColor = (a) => (a === "Papá" ? "var(--gold)" : "var(--holo)");

// ======================================================================
// Pantallas
// ======================================================================

function screenSetup() {
  const body = !S.joining
    ? `${btn("Crear maratón", "create", { tone: "red", size: "lg", disabled: S.busy })}
       ${btn("Unirse con código", "join-open", { tone: "gold", size: "lg", disabled: S.busy })}`
    : panel({ label: "Código de acceso", accent: "gold" }, `
       <input id="join-code" class="inp code" maxlength="6" autocomplete="off" autocapitalize="characters" spellcheck="false" placeholder="······" value="${esc(S.joinCode)}" aria-label="Código de 6 caracteres">
       ${btn("Unirme", "join", { tone: "gold", disabled: S.busy || S.joinCode.length !== 6, id: "join-btn" })}
       ${btn("Regresar", "join-close", { tone: "ghost", size: "md" })}`);
  const status = S.busy ? `<p class="console">Conectando con la base de datos</p>`
    : S.error ? panel({ label: "Error", accent: "red", blink: true }, `<p class="t-body hi" style="text-align:left">${esc(S.error)}</p>`)
    : `<p class="console">Esperando instrucciones</p>`;
  const recent = recentCodes();
  const saved = recent.length && !S.joining ? panel({ label: "Guardados en este celular", accent: "gold" },
    recent.map((c) => btn(`Entrar al maratón ${c}`, "rejoin", { tone: "ghost", size: "md", disabled: S.busy, data: `data-v="${c}"` })).join("")) : "";
  return `<div class="page center nonav">
    <div style="display:flex;justify-content:center">${gauge(12, 12, { size: 170, lit: "#5CE1FF", center: '<span class="num" style="font-size:26px">m3</span>' })}</div>
    <div><p class="lbl holo">Sistema de maratón · v1.0</p><h1 class="title-xl">PROTOCOLO<br>DOOMSDAY</h1></div>
    <p class="muted">Maratón rumbo al cine. Un celular crea el maratón y el otro se une con el código.</p>
    ${saved}${body}${status}</div>`;
}

function screenAuthor() {
  return `<div class="page center nonav">
    <p class="lbl holo">Identificación</p>
    <h1 class="t-big">¿Quién usa este celular?</h1>
    <p class="muted">Así sabremos quién escribió cada nota. Se puede cambiar después en Ajustes.</p>
    ${btn("Soy Cami", "author", { tone: "red", size: "lg", data: 'data-v="Cami"' })}
    ${btn("Soy Papá", "author", { tone: "gold", size: "lg", data: 'data-v="Papá"' })}</div>`;
}

function screenLoading() {
  return `<div class="page center nonav">
    <div style="display:flex;justify-content:center">${gauge(4, 12, { size: 170, lit: "#5CE1FF", center: '<span class="num" style="font-size:26px">···</span>' })}</div>
    <p class="console">Sincronizando maratón</p>
    ${S.authError ? `${panel({ label: "Sin conexión", accent: "red", blink: true }, `<p class="hi" style="text-align:left">${esc(S.authError)}</p>`)}
      ${btn("Reintentar", "retry")}${btn("Usar otro código", "leave-now", { tone: "ghost", size: "md" })}` : ""}</div>`;
}

function screenNotFound() {
  return `<div class="page center nonav">
    ${panel({ label: "Maratón no encontrado", accent: "red", blink: true }, `<p class="t-head hi" style="text-align:left">Ya no encontramos este maratón.</p>`)}
    ${btn("Crear o unirse a otro", "leave-now")}</div>`;
}

function notesSection(movieId, notes) {
  const draft = S.drafts[movieId] || "";
  const list = notes.length
    ? notes.map((n, i) => `<div class="log" style="--lc:${authorColor(n.author)}">
        <div class="bar"></div>
        <div class="body"><div class="meta">#${pad(i + 1)}  [${esc(n.author.toUpperCase())}]  ${esc(noteTime(n.createdAt).toUpperCase())}${n.editedAt ? " · EDITADA" : ""}</div>
        <div class="txt">${esc(n.text)}</div></div>
        <div class="acts">
          <button class="sqbtn s" data-act="note-edit" data-id="${esc(n.id)}" aria-label="Editar nota">${ICON.edit}</button>
          <button class="sqbtn s r" data-act="note-del" data-id="${esc(n.id)}" aria-label="Borrar nota">${ICON.trash}</button>
        </div></div>`).join("")
    : `<p class="muted">Sin registros todavía. Anoten personajes, pistas, teorías o escenas post-créditos.</p>`;
  return `${list}
    <textarea id="draft-${esc(movieId)}" class="inp" rows="2" data-draft="${esc(movieId)}" placeholder="Escribe una nota nueva">${esc(draft)}</textarea>
    ${btn("Agregar nota", "note-add", { tone: "gold", icon: "plus", disabled: !draft.trim(), data: `data-id="${esc(movieId)}"`, id: `addbtn-${esc(movieId)}` })}`;
}

function screenToday(ui) {
  const focus = ui.todayPending[0] || ui.todayWatched[ui.todayWatched.length - 1] || null;
  let main = "";
  const next = ui.nextPending && ui.nextPending.date
    ? `<div style="margin-top:6px"><p class="lbl holo">La siguiente</p>
        <p class="t-head gold" style="font-size:26px">${esc(ui.nextPending.movie.title)}</p>
        <p class="t-title hi" style="font-size:21px">${esc(cap1(whenPhrase(ui.nextPending.date, ui.today)))}</p></div>` : "";
  const status = (label, accent, title, body) => panel({ label, accent }, `
      <p class="t-big hi tc">${esc(title)}</p>${body ? `<p class="muted tc">${esc(body)}</p>` : ""}${next}`);

  if (ui.allDone) main = status("Protocolo completo", "gold", "¡Terminamos el maratón!", `Ya vimos las ${ui.total}. Solo falta ir al cine.`);
  else if (ui.todayPending.length) {
    const n = ui.todayPending.length;
    main = ui.todayPending.map((it, i) => panel({ label: n > 1 ? `Hoy toca · ${i + 1} de ${n}` : "Hoy toca", accent: "red", blink: true, scan: true, cls: "target" }, `
      <p class="t-big hi tc" style="font-size:38px;padding:6px 0">${esc(it.movie.title)}</p>
      <div class="stats">${stat("Año", it.movie.year ?? "—")}${stat("Duración", duration(it.movie.durationMin, it.movie.approxDuration) ?? "—")}</div>
      ${stat("Dónde verla", it.movie.where || "—", true)}
      <div style="margin-top:8px">${btn("Ya la vimos", "watched", { tone: "gold", icon: "check", size: "lg", data: `data-id="${esc(it.movie.id)}"` })}</div>
      ${btn("Ver detalle", "open", { tone: "ghost", size: "md", data: `data-id="${esc(it.movie.id)}"` })}`)).join("");
  } else if (ui.todayWatched.length) {
    main = status("Misión cumplida", "gold", "¡Listo por hoy!", `Ya vimos ${ui.todayWatched.map((i) => `“${i.movie.title}”`).join(" y ")}.`);
  } else main = status("Modo descanso", "holo", "Hoy descansamos 😴", null);

  const behind = ui.behind ? panel({ label: "Alerta de agenda", accent: "red", blink: true }, `
      <p class="t-title hi">Vamos atrasados: nos faltan ${ui.behind.pending} ${plural(ui.behind.pending, "película", "películas")} y quedan ${ui.behind.days} ${plural(ui.behind.days, "día disponible", "días disponibles")}.</p>
      <p class="muted t-small">Tranquilos: en Ajustes podemos sumar películas a algún día.</p>`) : "";

  const pct = ui.total ? Math.floor((ui.watchedCount * 100) / ui.total) : 0;
  const progress = panel({ label: "Progreso del maratón" }, `
    <div class="row">${gauge(ui.watchedCount, ui.total, { center: `<span class="num">${pad(ui.watchedCount)}</span><span class="lbl holo">DE ${ui.total}</span>` })}
      <div class="col grow"><p class="t-title hi">${ui.watchedCount} de ${ui.total} vistas</p>
        ${stat("Pendientes", String(ui.total - ui.watchedCount))}${stat("Completado", `${pct} %`, true)}</div></div>`);

  const ph = Math.floor(ui.pendingMin / 60), pm = ui.pendingMin % 60;
  const pendN = ui.total - ui.watchedCount;
  const hoursPanel = panel({ label: "Horas por ver", accent: "holo" }, `
    <div class="count holo-count"><b>${ui.pendingApprox && ui.pendingMin ? "~" : ""}${pad(ph)}</b><span class="lbl">${ph === 1 ? "hora" : "horas"}</span>
      ${pm ? `<b class="sub">${pad(pm)}</b><span class="lbl">min</span>` : ""}</div>
    <p class="t-title hi">${pendN === 0 ? "¡Ya no nos queda nada por ver!" : `Nos quedan ${ui.pendingApprox ? "aprox. " : ""}${hoursText(ui.pendingMin)} de películas por ver.`}</p>
    <p class="muted t-small">${pendN} ${plural(pendN, "película pendiente", "películas pendientes")} · ya vimos ${hoursText(ui.watchedMin)}.</p>`);

  const d = ui.daysToCinema;
  const countText = d == null ? "Fecha del cine: por definir"
    : d > 1 ? `Faltan ${d} días para ver Doomsday en el cine`
    : d === 1 ? "¡Mañana vamos a ver Doomsday al cine!"
    : d === 0 ? "¡Hoy vamos a ver Doomsday al cine!" : "Ya fuimos a ver Doomsday";
  const countdown = panel({ label: "Cuenta regresiva · cine", accent: "gold" }, `
    <div class="count"><b>${d == null ? "--" : pad(Math.max(d, 0))}</b><span class="lbl">${d === 1 ? "día" : "días"}</span></div>
    <p class="t-title hi">${esc(countText)}</p>
    <p class="muted t-small">${ui.info.cinemaDate ? esc(cap1(longDate(ui.info.cinemaDate, ui.today))) : "Se elige en Ajustes cuando la sepamos."}</p>`);

  const notes = focus ? panel({ label: "Bitácora · notas" }, `
    <p class="t-title gold">${esc(focus.movie.title)}</p>${notesSection(focus.movie.id, ui.notesBy[focus.movie.id] || [])}`) : "";

  return `<div class="page">${header("Hoy", longDate(ui.today, ui.today))}${main}${behind}${progress}${hoursPanel}${countdown}${notes}
    <div class="codeline"><span class="lbl muted">Código del maratón</span><b>${esc(S.code)}</b>
      <button class="sqbtn s" data-act="copy-code" aria-label="Copiar código">${ICON.copy}</button></div></div>`;
}

function diamond(num, watched, accent) {
  return `<div class="dia a-${accent}"><svg class="bg" viewBox="0 0 42 42"><path d="M21 1L41 21 21 41 1 21Z" fill="${watched ? "var(--a)" : "var(--void)"}" stroke="var(--a)" stroke-width="2"/></svg>
    ${watched ? `<span class="ck">${ICON.check}</span>` : `<span class="n">${pad(num)}</span>`}</div>`;
}

function screenList(ui) {
  const rows = ui.items.map((it, i) => {
    const m = it.movie;
    const isToday = !m.watched && it.date === ui.today;
    const accent = isToday ? "red" : m.watched ? "gold" : "holo";
    const dateText = m.watched ? `✓ Vista · ${it.date ? relShort(it.date, ui.today) : ""}`
      : isToday ? "▶ Hoy" : it.date ? relShort(it.date, ui.today) : "Sin fecha";
    const arrows = S.reorder ? `<div class="col" style="gap:6px">
        <button class="sqbtn" data-act="up" data-i="${i}" ${i === 0 ? "disabled" : ""} aria-label="Subir">${ICON.up}</button>
        <button class="sqbtn" data-act="down" data-i="${i}" ${i === ui.items.length - 1 ? "disabled" : ""} aria-label="Bajar">${ICON.down}</button></div>` : "";
    return `<div class="tli ${i === 0 ? "first" : ""} ${i === ui.items.length - 1 ? "last" : ""}">
      <div class="rail">${diamond(i + 1, m.watched, accent)}</div>
      <div class="card">${panel({ accent, cls: `sm ${isToday ? "fill-red" : ""}`, act: S.reorder ? "" : "open", data: `data-id="${esc(m.id)}"` }, `
        <div class="row"><div class="grow">
          <p class="lbl" style="color:var(--a);font-size:15px">${esc(dateText)}</p>
          <p class="t-title" style="color:${m.watched ? "var(--muted)" : "var(--text)"}">${esc(m.title)}</p>
          <p class="mono">${esc(movieInfo(m))}</p></div>${arrows}</div>`)}</div></div>`;
  }).join("");
  return `<div class="page">${header("Lista", `${ui.watchedCount} de ${ui.total} vistas · faltan ${hoursText(ui.pendingMin)}`)}
    ${btn(S.reorder ? "Listo, guardar orden" : "Reordenar", "reorder", { tone: S.reorder ? "gold" : "ghost", size: "md" })}
    <div class="tl">${rows}</div>
    ${btn("Agregar película", "movie-add", { tone: "ghost", icon: "plus" })}</div>`;
}

function screenSettings(ui) {
  const who = ["Cami", "Papá"].map((n) => btn(S.author === n ? `✓ ${n}` : n, "author", { tone: S.author === n ? "red" : "ghost", data: `data-v="${n}"` })).join("");
  const rules = DAY_NAMES.map((d, i) => {
    const v = ui.info.rules[i] ?? 0;
    return `<div class="rule ${v === 0 ? "zero" : ""}"><span class="d">${cap1(d)}</span>
      <button class="sqbtn" data-act="rule" data-i="${i}" data-d="-1" ${v <= 0 ? "disabled" : ""} aria-label="Menos">−</button>
      <span class="v">${v}</span>
      <button class="sqbtn" data-act="rule" data-i="${i}" data-d="1" ${v >= 5 ? "disabled" : ""} aria-label="Más">${ICON.plus}</button></div>`;
  }).join("");
  const isDefault = ui.info.rules.join() === DEFAULT_RULES.join();
  return `<div class="page">${header("Ajustes", "Panel de control")}
    ${panel({ label: "¿Quién usa este celular?" }, `<div class="two">${who}</div>`)}
    ${panel({ label: "Fecha del cine", accent: "gold" }, `
      <p class="t-head gold" style="font-size:26px">${ui.info.cinemaDate ? esc(cap1(longDate(ui.info.cinemaDate, ui.today))) : "Por definir"}</p>
      ${btn(ui.info.cinemaDate ? "Cambiar fecha" : "Elegir fecha", "cinema", { tone: "gold" })}
      ${ui.info.cinemaDate ? btn("Quitar fecha (por definir)", "cinema-clear", { tone: "ghost", size: "md" }) : ""}`)}
    ${panel({ label: "Películas por día" }, `
      <p class="muted t-small">Si un día no se ve nada (como el martes de D&amp;D), déjalo en 0.</p>${rules}
      ${isDefault ? "" : btn("Volver a las reglas originales", "rules-reset", { tone: "ghost", size: "md" })}`)}
    ${panel({ label: "Código de acceso", accent: "gold" }, `
      <div class="codebox">${esc(ui.info.code)}</div>
      <p class="muted t-small">Este celular ya guardó el código: no hay que volver a escribirlo. El enlace para compartir lleva el código, así que tu papá solo lo toca y entra directo.</p>
      <div class="two">${btn("Copiar código", "copy-code", { tone: "ghost", icon: "copy", size: "md" })}${btn("Copiar enlace", "copy-link", { tone: "ghost", icon: "copy", size: "md" })}</div>
      ${btn("Compartir por WhatsApp…", "share", { tone: "gold", icon: "share" })}`)}
    ${btn("Salir de este maratón", "leave", { tone: "red", size: "md" })}</div>`;
}

function screenDetail(ui, it) {
  const m = it.movie, today = ui.today;
  const number = ui.items.indexOf(it) + 1;
  const accent = m.watched ? "gold" : "red";
  const [label, text] = m.watched && it.date ? ["Estado · vista", `La vimos el ${relLong(it.date, today)}`]
    : m.watched ? ["Estado · vista", "Ya la vimos"]
    : it.date ? ["Programada", `Toca: ${relLong(it.date, today)}`]
    : ["Sin fecha", "Revisa las reglas en Ajustes"];
  let actions = "";
  if (!m.watched) {
    actions += btn("Ya la vimos", "watched", { tone: "gold", icon: "check", size: "lg", data: `data-id="${esc(m.id)}"` });
    const next = it.date ? nextAvailableDay(it.date, ui.info.rules) : null;
    if (next) actions += `<div class="col" style="gap:4px">${btn("Mover al siguiente día disponible", "postpone", { tone: "ghost", data: `data-id="${esc(m.id)}" data-d="${it.date}"` })}
      <p class="lbl muted" style="padding-left:4px">→ ${esc(longDate(next, today))}</p></div>`;
    if (m.notBefore && m.notBefore > today) actions += btn("Deshacer el cambio de día", "postpone-clear", { tone: "ghost", size: "md", data: `data-id="${esc(m.id)}"` });
  } else {
    actions += btn("Cambiar la fecha en que la vimos", "watched-date", { tone: "ghost", data: `data-id="${esc(m.id)}"` });
    actions += btn("Desmarcar: todavía no la vemos", "unwatch", { tone: "ghost", size: "md", data: `data-id="${esc(m.id)}"` });
  }
  return `<div class="page nonav">
    <div class="topbar"><button class="sqbtn" data-act="back" aria-label="Regresar">${ICON.back}</button>
      <div><p class="lbl holo">Expediente</p><p class="lbl gold" style="font-size:16px">Película #${pad(number)} de ${ui.total}</p></div></div>
    ${panel({ accent }, `<h1 class="t-big hi">${esc(m.title)}</h1>
      <div class="stats">${stat("Año", m.year ?? "—")}${stat("Duración", duration(m.durationMin, m.approxDuration) ?? "—")}</div>
      ${stat("Dónde verla", m.where || "—", true)}
      <div style="margin-top:4px"><p class="lbl" style="color:var(--a)">${esc(label)}</p><p class="t-title hi">${esc(cap1(text))}</p></div>`)}
    ${actions}
    ${btn("Editar datos", "movie-edit", { tone: "ghost", size: "md", data: `data-id="${esc(m.id)}"` })}
    ${panel({ label: "Bitácora · notas" }, notesSection(m.id, ui.notesBy[m.id] || []))}
    ${btn("Quitar del maratón", "movie-del", { tone: "red", size: "md", data: `data-id="${esc(m.id)}"` })}
    <p class="muted t-small">Quitarla también borra sus notas en los dos celulares.</p></div>`;
}

function gallerySections() {
  const groups = [];
  for (const [i, f] of (gallery || []).entries()) {
    const sec = f.seccion || "General";
    let g = groups.find((x) => x.name === sec);
    if (!g) groups.push((g = { name: sec, items: [] }));
    g.items.push({ f, i });
  }
  return groups;
}

function screenPhotos() {
  let body;
  if (gallery === null) body = `<p class="console">Cargando fotos</p>`;
  else if (!gallery.length) body = panel({ label: "Sin fotos todavía", accent: "gold" }, `
    <p class="t-title hi">Aquí van a salir las fotos del maratón.</p>
    <p class="muted t-small">Se agregan subiéndolas a la carpeta docs/fotos del repositorio en GitHub.</p>`);
  else body = gallerySections().map((g) => panel({ label: `${g.name} · ${g.items.length}` }, `
    <div class="grid">${g.items.map(({ f, i }) => `<button class="ph" data-act="photo-open" data-i="${i}" aria-label="${esc(f.texto || "Abrir foto")}">
      <img src="${esc(photoThumb(f))}" alt="${esc(f.texto || "")}" loading="lazy" decoding="async"></button>`).join("")}</div>`)).join("");
  const n = gallery ? gallery.length : 0;
  return `<div class="page">${header("Fotos", gallery ? `${n} ${plural(n, "foto", "fotos")} del maratón` : "Galería")}${body}</div>`;
}

function viewerHtml(i) {
  const f = gallery?.[i];
  if (!f) return "";
  const prev = i > 0 ? `<button class="sqbtn" data-act="photo-go" data-i="${i - 1}" aria-label="Anterior">${ICON.back}</button>` : `<span style="width:52px"></span>`;
  const next = i < gallery.length - 1 ? `<button class="sqbtn" data-act="photo-go" data-i="${i + 1}" aria-label="Siguiente" style="transform:scaleX(-1)">${ICON.back}</button>` : `<span style="width:52px"></span>`;
  return `<div class="viewer" role="dialog" aria-modal="true">
    <div class="vtop"><span class="lbl holo">${esc(f.seccion || "General")} · ${i + 1} de ${gallery.length}</span>
      <button class="sqbtn" data-act="photo-close" aria-label="Cerrar">${ICON.close}</button></div>
    <div class="vimg"><img src="${esc(photoSrc(f))}" alt="${esc(f.texto || "")}"></div>
    ${f.texto ? `<p class="t-title hi tc">${esc(f.texto)}</p>` : ""}
    <div class="vnav">${prev}<a class="lbl gold" href="${esc(photoSrc(f))}" download>Descargar</a>${next}</div></div>`;
}

function nav(tab) {
  const t = [["hoy", "Hoy", "home"], ["lista", "Lista", "list"], ["fotos", "Fotos", "camera"], ["ajustes", "Ajustes", "gear"]];
  return `<nav class="nav"><div class="in">${t.map(([k, l, i]) =>
    `<button class="${tab === k ? "sel" : ""}" data-act="tab" data-v="${k}" ${tab === k ? 'aria-current="page"' : ""}>${ICON[i]}<span>${l}</span></button>`).join("")}</div></nav>`;
}

// ---------- Diálogos ----------

function dialogHtml() {
  const d = S.dialog;
  if (!d) return "";
  let inner = "";
  if (d.type === "confirm") {
    inner = panel({ label: "Confirmar", accent: "red", cls: "fill-hi" }, `
      <p class="t-head hi" style="font-size:26px">${esc(d.title)}</p><p class="muted">${esc(d.text)}</p>
      ${btn(d.confirmText, "dlg-ok", { tone: "red", size: "md" })}${btn("Cancelar", "dlg-cancel", { tone: "ghost", size: "md" })}`);
  } else if (d.type === "movie") {
    const v = d.vals;
    inner = panel({ label: d.movie ? "Editar película" : "Nueva película", accent: "gold", cls: "fill-hi" }, `
      <label class="field"><span class="lbl">Título</span><input id="dm-title" class="inp" data-dlg="title" value="${esc(v.title)}"></label>
      <div class="two"><label class="field"><span class="lbl">Año</span><input id="dm-year" class="inp" inputmode="numeric" data-dlg="year" value="${esc(v.year)}"></label>
      <label class="field"><span class="lbl">Minutos</span><input id="dm-dur" class="inp" inputmode="numeric" data-dlg="dur" value="${esc(v.dur)}"></label></div>
      <label class="field"><span class="lbl">Dónde verla</span><input id="dm-where" class="inp" data-dlg="where" value="${esc(v.where)}"></label>
      ${btn("Guardar", "dlg-ok", { tone: "red", size: "md", disabled: !v.title.trim(), id: "dlg-ok" })}${btn("Cancelar", "dlg-cancel", { tone: "ghost", size: "md" })}`);
  } else if (d.type === "note") {
    inner = panel({ label: "Editar nota", cls: "fill-hi" }, `
      <textarea id="dn-text" class="inp" rows="4" data-dlg="text">${esc(d.vals.text)}</textarea>
      ${btn("Guardar", "dlg-ok", { tone: "red", size: "md", disabled: !d.vals.text.trim(), id: "dlg-ok" })}${btn("Cancelar", "dlg-cancel", { tone: "ghost", size: "md" })}`);
  } else if (d.type === "date") {
    inner = panel({ label: d.title, accent: "gold", cls: "fill-hi" }, `
      <input id="dd-date" type="date" class="inp" data-dlg="date" value="${esc(d.vals.date)}">
      ${btn("Aceptar", "dlg-ok", { tone: "gold", size: "md", disabled: !isDate(d.vals.date), id: "dlg-ok" })}${btn("Cancelar", "dlg-cancel", { tone: "ghost", size: "md" })}`);
  } else if (d.type === "code") {
    inner = panel({ label: "Maratón creado", accent: "gold", cls: "fill-hi" }, `
      <p class="muted">Este es su código de acceso:</p><div class="codebox">${esc(d.code)}</div>
      <p class="hi">Mándale a tu papá el enlace de esta página junto con el código. Ahí elige “Unirse con código” y lo escribe. También lo ves en Ajustes.</p>
      ${btn("Compartir enlace y código", "share", { tone: "gold", icon: "share", size: "md" })}${btn("Entendido", "dlg-cancel", { tone: "ghost", size: "md" })}`);
  }
  return `<div class="overlay" data-act="dlg-backdrop" role="dialog" aria-modal="true">${inner}</div>`;
}

function toastHtml(hasNav) {
  if (!S.toast) return "";
  return `<div class="toast ${hasNav ? "" : "nonav"}" role="status"><span>${esc(S.toast.text)}</span>${S.toast.undo ? '<button data-act="toast-undo">Deshacer</button>' : ""}</div>`;
}

let toastTimer = null;
function showToast(text, undo = null) {
  S.toast = { text, undo };
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { S.toast = null; render(); }, 6000);
  render();
}

// ======================================================================
// Render
// ======================================================================

const root = document.getElementById("app");
let lastUi = null;

function render() {
  let html = "", hasNav = false;
  if (!S.code) html = screenSetup();
  else if (S.mstate === "notfound") html = screenNotFound();
  else if (S.mstate !== "found" || !S.info) html = screenLoading();
  else if (!S.author) html = screenAuthor();
  else {
    const ui = buildUi();
    lastUi = ui;
    const r = route();
    const it = r.movieId ? ui.items.find((i) => i.movie.id === r.movieId) : null;
    if (r.movieId && it) html = screenDetail(ui, it);
    else {
      if (r.movieId && !it && ui.items.length) { go("lista"); return; }
      const tab = r.tab || "lista";
      html = (tab === "hoy" ? screenToday(ui) : tab === "lista" ? screenList(ui) : tab === "fotos" ? screenPhotos() : screenSettings(ui)) + nav(tab);
      if (r.photoId != null) html += viewerHtml(Number(r.photoId));
      hasNav = true;
    }
  }
  if (S.createdCode && !S.dialog) S.dialog = { type: "code", code: S.createdCode };

  // Conservar el foco y el cursor si alguien está escribiendo cuando llega un cambio del otro celular.
  const ae = document.activeElement;
  const fid = ae && ae.id;
  const sel = fid && typeof ae.selectionStart === "number" ? [ae.selectionStart, ae.selectionEnd] : null;
  root.innerHTML = html + dialogHtml() + toastHtml(hasNav);
  if (fid) {
    const el = document.getElementById(fid);
    if (el) {
      el.focus({ preventScroll: true });
      if (sel && el.setSelectionRange) try { el.setSelectionRange(sel[0], sel[1]); } catch {}
    }
  }
  root.querySelectorAll(".target").forEach((p) => p.style.setProperty("--scan-h", `${p.offsetHeight}px`));
}

// ======================================================================
// Eventos
// ======================================================================

const findItem = (id) => lastUi?.items.find((i) => i.movie.id === id);
const findNote = (id) => S.notes.find((n) => n.id === id);

function markWatched(m, date = S.today) {
  A.setWatched(m, true, date);
  showToast(`¡Listo! “${m.title}” quedó como vista ✓`, () => A.setWatched(m, false, null));
}

function confirm(title, text, confirmText, onOk) {
  S.dialog = { type: "confirm", title, text, confirmText, onOk };
  render();
}

async function share() {
  const url = joinLink(S.code);
  const text = `¡Únete a nuestro maratón rumbo a Doomsday! 🍿\nToca este enlace y entras directo (código ${S.code}):\n${url}`;
  try {
    if (navigator.share) { await navigator.share({ title: "Maratón Doomsday", text }); return; }
  } catch (e) { if (e && e.name === "AbortError") return; }
  try { await navigator.clipboard.writeText(text); showToast("Copiado. Pégalo en WhatsApp para mandarlo."); }
  catch { showToast(`Código: ${S.code} · Página: ${url}`); }
}

async function copyText(text, okMsg) {
  try { await navigator.clipboard.writeText(text); showToast(okMsg); }
  catch { showToast(text); }
}

root.addEventListener("click", (ev) => {
  const el = ev.target.closest("[data-act]");
  if (!el || el.disabled) return;
  const act = el.dataset.act, id = el.dataset.id;
  if (act === "dlg-backdrop" && ev.target !== el) return; // clic dentro del panel
  switch (act) {
    case "create": createMarathon(); break;
    case "join-open": S.joining = true; S.error = null; render(); document.getElementById("join-code")?.focus(); break;
    case "join-close": S.joining = false; S.error = null; render(); break;
    case "join": joinMarathon(); break;
    case "author": S.author = el.dataset.v; store.set("maraton.author", S.author); render(); break;
    case "retry": S.authError = null; render(); signInAnonymously(auth).catch(() => { S.authError = "No pudimos conectarnos. Revisa tu internet."; render(); }); break;
    case "leave-now": setCode(null); render(); break;
    case "tab": go(el.dataset.v); window.scrollTo(0, 0); break;
    case "open": go(`peli/${encodeURIComponent(id)}`); window.scrollTo(0, 0); break;
    case "back": goBack(); break;
    case "watched": { const it = findItem(id); if (it) markWatched(it.movie); break; }
    case "unwatch": { const it = findItem(id); if (it) confirm("¿Desmarcar?", `“${it.movie.title}” volverá a quedar pendiente y se acomodará en la agenda.`, "Desmarcar", () => A.setWatched(it.movie, false, null)); break; }
    case "watched-date": { const it = findItem(id); if (it) { S.dialog = { type: "date", title: "¿Qué día la vimos?", vals: { date: it.movie.watchedDate || S.today }, onOk: (d) => A.setWatched(it.movie, true, d) }; render(); } break; }
    case "postpone": { const it = findItem(id); const nx = it && nextAvailableDay(el.dataset.d, lastUi.info.rules); if (nx) { A.setNotBefore(it.movie, nx); showToast(`Movida al ${longDate(nx, S.today)}`); } break; }
    case "postpone-clear": { const it = findItem(id); if (it) A.setNotBefore(it.movie, null); break; }
    case "movie-add": S.dialog = { type: "movie", movie: null, vals: { title: "", year: "", dur: "", where: DEFAULT_WHERE } }; render(); document.getElementById("dm-title")?.focus(); break;
    case "movie-edit": { const it = findItem(id); if (it) { const m = it.movie; S.dialog = { type: "movie", movie: m, vals: { title: m.title, year: m.year ?? "", dur: m.durationMin ?? "", where: m.where } }; render(); } break; }
    case "movie-del": { const it = findItem(id); if (it) confirm(`¿Quitar “${it.movie.title}”?`, "Se borrará del maratón junto con sus notas, en los dos celulares.", "Quitar", () => { A.deleteMovie(it.movie, lastUi.notesBy[it.movie.id] || []); go("lista"); }); break; }
    case "reorder": S.reorder = !S.reorder; render(); break;
    case "up": case "down": A.move(lastUi.items, Number(el.dataset.i), act === "up" ? -1 : 1); break;
    case "note-add": { const t = (S.drafts[id] || "").trim(); if (t) { A.addNote(id, t); S.drafts[id] = ""; render(); } break; }
    case "note-edit": { const n = findNote(id); if (n) { S.dialog = { type: "note", note: n, vals: { text: n.text } }; render(); document.getElementById("dn-text")?.focus(); } break; }
    case "note-del": { const n = findNote(id); if (n) confirm("¿Borrar nota?", `“${n.text.slice(0, 80)}”`, "Borrar", () => A.deleteNote(n)); break; }
    case "cinema": S.dialog = { type: "date", title: "Fecha del cine", vals: { date: lastUi.info.cinemaDate || S.today }, onOk: (d) => A.setCinema(d) }; render(); break;
    case "cinema-clear": A.setCinema(null); break;
    case "rule": { const r = [...lastUi.info.rules]; const i = Number(el.dataset.i); r[i] = Math.min(5, Math.max(0, (r[i] || 0) + Number(el.dataset.d))); A.setRules(r); break; }
    case "rules-reset": A.setRules(DEFAULT_RULES); break;
    case "share": share(); break;
    case "copy-code": copyText(S.code, `Código ${S.code} copiado`); break;
    case "copy-link": copyText(joinLink(S.code), "Enlace copiado. Pégalo en WhatsApp."); break;
    case "rejoin": S.joinCode = el.dataset.v; S.joining = false; joinMarathon(); break;
    case "photo-open": go(`foto/${el.dataset.i}`); break;
    case "photo-go": history.replaceState(null, "", `#foto/${el.dataset.i}`); render(); break;
    case "photo-close": if (history.length > 1) history.back(); else go("fotos"); break;
    case "leave": confirm("¿Salir del maratón?", `Solo se quita de este celular. Nada se borra: pueden volver a entrar con el código ${S.code}.`, "Salir", () => { setCode(null); go("hoy"); }); break;
    case "toast-undo": S.toast?.undo?.(); S.toast = null; render(); break;
    case "dlg-cancel": case "dlg-backdrop":
      if (S.dialog?.type === "code") S.createdCode = null;
      S.dialog = null; render(); break;
    case "dlg-ok": {
      const d = S.dialog; S.dialog = null;
      if (d.type === "confirm") d.onOk();
      else if (d.type === "date" && isDate(d.vals.date)) d.onOk(d.vals.date);
      else if (d.type === "note" && d.vals.text.trim()) A.updateNote(d.note, d.vals.text.trim());
      else if (d.type === "movie" && d.vals.title.trim()) {
        const f = { title: d.vals.title.trim(), year: parseInt(d.vals.year, 10) || null, durationMin: parseInt(d.vals.dur, 10) || null, where: d.vals.where.trim() };
        if (d.movie) A.updateMovie(d.movie, f);
        else A.addMovie(f, Math.max(0, ...lastUi.items.map((i) => i.movie.order)));
      }
      render(); break;
    }
  }
});

root.addEventListener("keydown", (ev) => {
  if (ev.key === "Enter" && ev.target.matches?.(".panel.tap")) ev.target.click();
  if (ev.key === "Enter" && ev.target.id === "join-code") joinMarathon();
  if (ev.key === "Escape" && !S.dialog && route().photoId != null) { history.back(); return; }
  if (ev.key === "Escape" && S.dialog) { if (S.dialog.type === "code") S.createdCode = null; S.dialog = null; render(); }
});

root.addEventListener("input", (ev) => {
  const t = ev.target;
  if (t.id === "join-code") {
    const v = normalizeCode(t.value);
    if (v !== t.value) t.value = v;
    S.joinCode = v;
    const b = document.getElementById("join-btn");
    if (b) { b.disabled = S.busy || v.length !== 6; b.parentElement.classList.toggle("dis", b.disabled); }
  } else if (t.dataset.draft !== undefined) {
    S.drafts[t.dataset.draft] = t.value;
    const b = document.getElementById(`addbtn-${t.dataset.draft}`);
    if (b) { b.disabled = !t.value.trim(); b.parentElement.classList.toggle("dis", b.disabled); }
  } else if (t.dataset.dlg && S.dialog) {
    let v = t.value;
    if (t.dataset.dlg === "year") v = v.replace(/\D/g, "").slice(0, 4);
    if (t.dataset.dlg === "dur") v = v.replace(/\D/g, "").slice(0, 3);
    if (v !== t.value) t.value = v;
    S.dialog.vals[t.dataset.dlg] = v;
    const b = document.getElementById("dlg-ok");
    if (b) {
      const ok = S.dialog.type === "date" ? isDate(S.dialog.vals.date) : (S.dialog.vals.title ?? S.dialog.vals.text ?? "").trim().length > 0;
      b.disabled = !ok; b.parentElement.classList.toggle("dis", !ok);
    }
  }
});

window.addEventListener("hashchange", () => { if (!checkJoinLink()) render(); });
window.addEventListener("online", () => { S.online = true; render(); });
window.addEventListener("offline", () => { S.online = false; render(); });
setInterval(() => { const t = todayStr(); if (t !== S.today) { S.today = t; render(); } }, 30000);
document.addEventListener("visibilitychange", () => { if (!document.hidden) { S.today = todayStr(); render(); } });

if (!checkJoinLink()) render();
