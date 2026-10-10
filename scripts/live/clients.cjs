// Real TCP Minecraft clients; the separate server probe only sets fixtures and observes state.
const path = require('node:path')
const fs = require('node:fs')
const { createRequire } = require('node:module')
const req = createRequire(path.resolve('build/live-tools/package.json'))
const mineflayer = req('mineflayer')
const { Rcon } = req('rcon-client')
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))
const report = { version: process.env.LIVE_VERSION, bridge: process.env.LIVE_BRIDGE === 'true', suite: process.env.LIVE_SUITE, cases: [], clientErrors: [], performance: [] }
const bots = []
let rcon
const assert = (value, message) => { if (!value) throw new Error(message) }
async function command(text) { return rcon.send(text) }
async function probe(text = 'state') {
  const response = await command('mtprobe ' + text)
  const start = response.indexOf('LIVEGZIP:')
  if (start < 0) throw new Error(response)
  return JSON.parse(require('node:zlib').gunzipSync(Buffer.from(response.slice(start + 9), 'base64')).toString('utf8'))
}
async function check(name, fn) {
  try { const evidence = await fn(); report.cases.push({ name, passed: true, evidence: evidence === undefined ? undefined : structuredClone(evidence) }); console.log('PASS', name) }
  catch (e) { report.cases.push({ name, passed: false, error: e.stack }); console.log('FAIL', name, e.message) }
}
async function connect(name) {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25585, username: name, auth: 'offline', version: report.bridge ? '1.21.11' : report.version, hideErrors: true, checkTimeoutInterval: 60000 })
  // Bridged headless clients are stationary: the library cannot simulate the
  // newer server's movement/teleport state. Keep teleport acknowledgements and
  // explicit finite look packets, but suppress its automatic movement guesses.
  bot.liveWrite = bot._client.write.bind(bot._client)
  if (report.bridge) bot._client.write = (packet, data) => {
    if (['position', 'position_look', 'look', 'flying'].includes(packet)) return
    return bot.liveWrite(packet, data)
  }
  bot.liveMessages = []
  bot.livePendingPing = []
  bot.livePingLatency = []
  bot.liveStateId = 0
  bot._client.on('packet', (data, meta) => { if ((meta.name === 'set_slot' || meta.name === 'window_items') && data.stateId !== undefined) bot.liveStateId = data.stateId })
  bot.on('messagestr', msg => {
    bot.liveMessages.push(msg)
    if (msg.includes("Player Target's ping:") && bot.livePendingPing.length) bot.livePingLatency.push(Date.now() - bot.livePendingPing.shift())
  })
  bot.on('error', e => report.clientErrors.push({ name, error: e.message }))
  bot.on('kicked', reason => report.clientErrors.push({ name, kicked: String(reason) }))
  bots.push(bot)
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(name + ' spawn timeout')), 45000)
    bot.once('spawn', () => { clearTimeout(timer); bot.physicsEnabled = false; resolve() })
    bot.once('error', error => { clearTimeout(timer); reject(error) })
    bot.once('kicked', reason => { clearTimeout(timer); reject(new Error(String(reason))) })
  })
  return bot
}
async function chat(bot, text, wait = 250) { bot.chat(text); await delay(wait) }
async function open(bot) { await chat(bot, '/mt inv Target', 400); assert(bot.currentWindow?.slots.length >= 54, 'Inventory did not open on client') }
async function click(bot, slot, button = 0, mode = 0) { await bot.clickWindow(slot, button, mode); await delay(180) }
async function rawClick(bot, slot, button, mode) {
  const Item = req('prismarine-item')(bot.version)
  bot._client.write('window_click', { windowId: bot.currentWindow.id, stateId: bot.liveStateId, slot, mouseButton: button, mode, changedSlots: [], cursorItem: Item.toNotch(bot.currentWindow.selectedItem) })
  await delay(180)
}
const slot = (state, player, index) => state.players[player].slots[index]
const count = (state, player, type) => state.players[player].slots.reduce((n, s) => n + (s.type === type ? s.count : 0), 0) + (state.players[player].cursor.type === type ? state.players[player].cursor.count : 0)
async function deathCase() {
  const first = await probe('kill'); await delay(500); await probe('respawn'); await delay(300); const second = await probe('kill'); await delay(500); await probe('respawn'); const s = await probe()
  assert(s.deathMessages === 1 && s.suppressedDeaths === 1, `Death counts: first=${first.deathMessages}/${first.suppressedDeaths}, second=${second.deathMessages}/${second.suppressedDeaths}, final=${s.deathMessages}/${s.suppressedDeaths}`)
  return { visible: s.deathMessages, suppressed: s.suppressedDeaths }
}
async function explosionCases() {
  for (const kind of ['tnt', 'creeper', 'other']) {
    await check('Real explosion protection: ' + kind, async () => {
      const before = await probe(); await probe('explode ' + kind); await delay(1000); const s = await probe()
      const events = s.damageLog.slice(before.damageLog.length)
      assert(s.explosions > before.explosions, 'Explosion event did not occur')
      assert(s.players.Target.health === 20 && s.explosionBlock === 'STONE' && s.deathLog.length === before.deathLog.length, 'Explosion protection failed: ' + JSON.stringify({ damage: events, deaths: s.deathLog }))
      assert(events.length > 0 && events.every(x => x.cancelled), 'Explosion damage was not explicitly cancelled: ' + JSON.stringify(events))
      return { health: s.players.Target.health, block: s.explosionBlock, explosions: s.explosions - before.explosions, damage: events }
    })
  }
}
async function experienceCases() {
  for (const [mode, expected] of [['double', 62], ['zero', 0], ['normal', 31], ['cancelMerge', 31], ['cancel', 0]]) {
    await check('Real XP pickup: ' + mode, async () => {
      await probe('xp ' + mode); await delay(mode === 'cancel' ? 800 : 2200); const s = await probe()
      assert(s.players.Target.xp === expected, `XP ${s.players.Target.xp} != ${expected}; orbTotal=${s.orbTotal}; playerXP=${JSON.stringify(Object.fromEntries(Object.entries(s.players).map(([n,p]) => [n,p.xp])))}`)
      if (mode === 'cancel') assert(s.orbTotal === 31 && s.pickups > 0, 'Cancelled orbs not preserved')
      else assert(s.orbTotal === 0 && s.pickups > 0, 'XP not fully picked up')
      if (mode === 'cancelMerge') assert(s.blockedMerges > 0, 'Merge cancellation not exercised')
      return { xp: s.players.Target.xp, orbTotal: s.orbTotal, pickups: s.pickups, merges: s.merges, blockedMerges: s.blockedMerges }
    })
  }
  await check('Vanilla Mending repairs item before granting remaining XP', async () => {
    await probe('xp mending'); await delay(1200); const s = await probe(); assert(slot(s, 'Target', 0).damage === 60 && s.players.Target.xp === 0, 'Mending behavior changed: ' + JSON.stringify({ item: slot(s, 'Target', 0), xp: s.players.Target.xp, orbTotal: s.orbTotal })); return slot(s, 'Target', 0)
  })
}

async function functional() {
  const a = await connect('AdminA'); const b = await connect('AdminB'); const t = await connect('Target'); const w = await connect('Watcher')
  await probe('prepare'); await delay(500)
  await check('Real clients join and plugin enabled without PlaceholderAPI', async () => { const s = await probe(); assert(s.enabled && Object.keys(s.players).length === 4 && !s.plugins.includes('PlaceholderAPI'), 'Missing players/plugin or unexpected PlaceholderAPI'); return { server: s.server, clients: Object.keys(s.players), plugins: s.plugins } })
  await check('Localized help and ping commands reach client', async () => {
    a.liveMessages.length = 0; await chat(a, '/mt help'); await chat(a, '/mt ping Target')
    assert(a.liveMessages.some(x => /ping|ms/i.test(x)), 'No ping response')
    assert(a.liveMessages.some(x => /help|MeowTools|vanish/i.test(x)), 'No help response'); return a.liveMessages
  })
  await check('Unauthorized inventory command denied', async () => { await chat(w, '/mt inv Target'); const s = await probe(); assert(s.players.Watcher.topSize !== 54, 'Unauthorized GUI'); assert(w.liveMessages.some(x => /permission/i.test(x)), 'No denial response') })
  await check('Blocked commands and operator bypass', async () => {
    w.liveMessages.length = 0; a.liveMessages.length = 0
    await chat(w, '/plugins'); await chat(a, '/plugins')
    assert(w.liveMessages.some(x => /Unknown command/i.test(x)), 'Blocked command not hidden')
    assert(a.liveMessages.some(x => /MeowTools/i.test(x)), 'OP bypass failed')
  })
  await probe('fixture'); await open(a); await open(b)
  await check('Two administrators see same authoritative inventory', async () => { const s = await probe(); assert(s.players.AdminA.top[9].count === 12 && s.players.AdminB.top[9].count === 12, 'Different projection') })
  await check('Left click removes item and updates both viewers', async () => {
    await click(a, 9); const s = await probe(); assert(slot(s, 'Target', 9).count === 0 && s.players.AdminA.cursor.count === 12 && s.players.AdminB.top[9].count === 0, 'Left click/observer synchronization failed')
  })
  await check('Right click splits authoritative stack', async () => {
    await click(b, 10, 1); const s = await probe(); assert(slot(s, 'Target', 10).count === 3 && s.players.AdminB.cursor.count === 4, 'Split wrong')
    await click(b, 12)
  })
  await check('Placement and shift extraction conserve items', async () => {
    await click(a, 11); await click(a, 11, 0, 1); const s = await probe(); assert(slot(s, 'Target', 11).count === 0 && count(s, 'AdminA', 'DIAMOND') === 12, 'Shift extraction failed')
    assert(['AdminA', 'AdminB', 'Target'].reduce((n, p) => n + count(s, p, 'DIAMOND'), 0) === 19, 'Diamond total changed')
  })
  await check('Number key swaps real hotbar slots', async () => {
    await click(a, 45, 0, 2); const s = await probe(); assert(slot(s, 'Target', 0).type === 'IRON_INGOT' && slot(s, 'AdminA', 0).type === 'GOLD_INGOT', 'Hotbar swap failed')
  })
  await check('Armor validation rejects non-equipment without loss', async () => {
    await click(a, 45); await click(a, 0); const s = await probe(); assert(slot(s, 'Target', 39).type === 'DIAMOND_HELMET' && s.players.AdminA.cursor.type === 'IRON_INGOT', 'Invalid armor accepted'); await click(a, 45)
  })
  await check('Decorations cannot be extracted', async () => { await click(a, 36); const s = await probe(); assert(s.players.AdminA.cursor.count === 0, 'Decoration acquired') })
  await check('Shift insertion from lower inventory is blocked', async () => {
    const before = await probe(); await click(a, 54, 0, 1); const after = await probe(); assert(JSON.stringify(before.players.Target.slots) === JSON.stringify(after.players.Target.slots), 'Lower shift inserted item')
  })
  await check('Double-click collection cannot pull projected items', async () => {
    await click(b, 12); await rawClick(b, 10, 0, 6); const s = await probe(); assert(s.players.AdminB.cursor.count === 4 && slot(s, 'Target', 10).count === 3, 'Collect bypass'); await click(b, 12)
  })
  await check('Drag across projected slots is cancelled without loss', async () => {
    await click(b, 12); await rawClick(b, -999, 0, 5); await rawClick(b, 13, 1, 5); await rawClick(b, 14, 1, 5); await rawClick(b, -999, 2, 5)
    const s = await probe(); assert(s.players.AdminB.cursor.count === 4 && slot(s, 'Target', 13).count === 0 && slot(s, 'Target', 14).count === 0, 'Drag bypass'); await click(b, 12)
  })
  await check('Valid armor replacement and offhand editing work', async () => {
    await click(a, 54); await click(a, 0); await click(a, 54); const s = await probe(); assert(slot(s, 'Target', 39).type === 'IRON_HELMET' && slot(s, 'AdminA', 9).type === 'DIAMOND_HELMET', 'Armor replacement failed')
    await click(a, 4); const removed = await probe(); assert(slot(removed, 'Target', 40).count === 0 && removed.players.AdminA.cursor.type === 'SHIELD', 'Offhand removal failed'); await click(a, 4)
  })
  await check('Closing stale GUI preserves concurrent target change', async () => {
    await probe('slot Target 9 EMERALD 23'); a.closeWindow(a.currentWindow); b.closeWindow(b.currentWindow); await delay(300)
    const s = await probe(); assert(slot(s, 'Target', 9).type === 'EMERALD' && slot(s, 'Target', 9).count === 23, 'Close overwrote real inventory'); assert(s.pluginTasks === 0, 'Refresh task leaked')
  })
  await check('Vanish hides player, repeated on is idempotent', async () => {
    await chat(a, '/mt vanish on'); await chat(a, '/mt vanish on'); const s = await probe(); assert(s.players.AdminA.vanished && s.players.AdminA.nightVision && !s.players.Watcher.seesAdminA, 'Vanish failed')
  })
  await check('Reload keeps vanish and inventory feature usable', async () => {
    for (let i = 0; i < 3; i++) await chat(a, '/mt reload')
    await open(b); const s = await probe(); assert(s.players.AdminA.vanished && !s.players.Watcher.seesAdminA && s.players.AdminB.topSize === 54, 'Reload broke active state'); b.closeWindow(b.currentWindow)
  })
  await check('Vanish off restores visibility and removes owned night vision', async () => {
    await chat(a, '/mt vanish off'); const s = await probe(); assert(!s.players.AdminA.vanished && !s.players.AdminA.nightVision && s.players.Watcher.seesAdminA, 'Vanish cleanup failed')
  })
  await check('Unknown language falls back to usable English help', async () => {
    await probe('set language unknown_language'); a.liveMessages.length = 0; await chat(a, '/mt reload'); await chat(a, '/mt help'); assert(a.liveMessages.some(x => /MeowTools|vanish/i.test(x)), 'Language fallback failed'); await probe('set language en_us'); await chat(a, '/mt reload')
  })
  await check('Invalid YAML reload reports failure and preserves file', async () => {
    const filename = path.resolve('build/live-servers', report.version, 'plugins/MeowTools/config.yml'); const good = fs.readFileSync(filename, 'utf8'); const bad = 'server_settings: [invalid yaml\n'
    try {
      fs.writeFileSync(filename, bad); a.liveMessages.length = 0; await chat(a, '/mt reload'); assert(fs.readFileSync(filename, 'utf8') === bad, 'Invalid YAML overwritten'); assert(a.liveMessages.some(x => /fail|error/i.test(x)), 'Reload failure not reported')
    } finally { fs.writeFileSync(filename, good); await chat(a, '/mt reload') }
  })
  await check('F packet triggers configured shortcut; disabled directional binding falls back', async () => {
    a.liveMessages.length = 0; a.liveWrite('look', { yaw: 0, pitch: -90, onGround: true, flags: { onGround: true, hasHorizontalCollision: false } }); await delay(100); a._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 }); await delay(400)
    assert(a.liveMessages.some(x => x.includes('LIVE_F')), 'F fallback command did not execute')
  })
  await check('Shortcut cooldown suppresses rapid repeat packets', async () => {
    a.liveMessages.length = 0
    for (let i = 0; i < 8; i++) a._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: i + 1 })
    await delay(400); const messages = a.liveMessages.filter(x => x.includes('LIVE_F')); assert(messages.length === 1, 'Unexpected shortcut count: ' + messages.length); return { executed: messages.length, sent: 8 }
  })
  await experienceCases()
  await check('Fast leaves decay respects persistent leaves and cancelled decay', async () => {
    await probe('leaves'); await probe('break'); await delay(1200); const s = await probe(); assert(s.leaves[0] === 'AIR' && s.leaves[1] === 'OAK_LEAVES' && s.leaves[2] === 'OAK_LEAVES' && s.leafEvents >= 2, 'Leaf fixture failed: ' + s.leaves); return { leaves: s.leaves, events: s.leafEvents }
  })
  await explosionCases()
  await check('Repeated death suppresses duplicate messages', deathCase)
  await check('Target quit closes all viewers and cancels refresh task', async () => {
    await open(a); await open(b); t.quit(); await delay(800); const s = await probe(); assert(!s.players.Target && s.players.AdminA.topSize !== 54 && s.players.AdminB.topSize !== 54 && s.pluginTasks === 0, 'Quit cleanup failed')
  })
  await check('Vanish quit/rejoin removes owned effects and state', async () => {
    await chat(a, '/mt vanish on'); a.quit(); await delay(500); const rejoined = await connect('AdminA'); await delay(400); const s = await probe(); assert(!s.players.AdminA.vanished && !s.players.AdminA.nightVision && s.players.Watcher.seesAdminA, 'Vanish reconnect state leaked'); return { nightVision: s.players.AdminA.nightVision }
  })
  await check('Disable clears scheduled plugin tasks and active vanish', async () => {
    const admin = bots.findLast(x => x.username === 'AdminA'); await chat(admin, '/mt vanish on'); const s = await probe('disable'); assert(!s.enabled && !s.players.AdminA.nightVision && s.players.Watcher.seesAdminA && s.pluginTasks === 0, 'Disable cleanup failed')
  })
}

async function performance() {
  await connect('Target'); for (let i = 0; i < 19; i++) { await connect('Load' + String(i).padStart(2, '0')); await delay(100) }
  await probe('prepare'); await delay(15000)
  for (const phase of ['idle20', 'combined20', 'combined20_with19_inventory_views']) {
    if (phase !== 'idle20') await probe('stress true')
    if (phase.includes('inventory')) { for (const b of bots.filter(b => b.username.startsWith('Load'))) await open(b) }
    await delay(5000)
    for (const b of bots) { b.livePendingPing.length = 0; b.livePingLatency.length = 0 }
    const before = await probe('perf start'); const end = Date.now() + 60000
    let requests = 0
    while (Date.now() < end) {
      if (phase !== 'idle20') for (const b of bots.filter(b => b.username.startsWith('Load'))) {
        b.livePendingPing.push(Date.now()); b.chat('/mt ping Target'); requests++
        b._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: requests })
      }
      await delay(1000)
    }
    const s = await probe('perf stop'); const latency = bots.flatMap(b => b.livePingLatency).sort((a, b) => a - b)
    report.performance.push({ phase, ...s.perf, requests, pingResponses: latency.length, pingP95ms: latency.length ? latency[Math.ceil(latency.length * .95) - 1] : null, orbsAtEnd: s.orbs, pickupCallbacks: s.pickups - before.pickups, mergeCallbacks: s.merges - before.merges, inventoryViews: Object.values(s.players).filter(x => x.topSize === 54).length }); console.log('PERF', phase, JSON.stringify(s.perf))
  }
  await probe('stress false')
  await check('All 20 clients remain connected after stress', async () => { const s = await probe(); assert(Object.keys(s.players).length === 20, 'Clients lost during stress'); return { players: Object.keys(s.players).length } })
}

async function main() {
  try {
    rcon = await Rcon.connect({ host: '127.0.0.1', port: 25586, password: 'meowtools-local-test-only', timeout: 20000 })
    if (report.suite === 'smoke') await check('Plugin enables on real Paper server', async () => { const s = await probe(); assert(s.enabled && !s.plugins.includes('ViaVersion') && !s.plugins.includes('ViaBackwards'), 'Plugin disabled or unexpected bridge'); return { server: s.server, plugins: s.plugins } })
    else if (report.suite === 'death') { await connect('Target'); await probe('prepare'); await check('Repeated death suppresses duplicate messages', deathCase) }
    else if (report.suite === 'explosion') { await connect('Target'); await probe('prepare'); await explosionCases(); await check('Repeated death suppresses duplicate messages', deathCase) }
    else if (report.suite === 'xp') { await connect('AdminA'); await connect('Target'); await probe('prepare'); await experienceCases() }
    else if (report.suite === 'performance') await performance()
    else await functional()
  } catch (e) { report.fatal = e.stack; console.error(e.stack) }
  finally {
    for (const b of bots) { try { b.quit() } catch {} }
    if (rcon) await rcon.end()
    fs.writeFileSync(process.env.LIVE_RESULT, JSON.stringify(report, null, 2))
  }
  // Mineflayer can retain timers after disconnect. Evidence is flushed above;
  // allow FIN packets to drain, then end this dedicated client process.
  await delay(250)
  process.exit(report.fatal || report.cases.some(x => !x.passed) ? 1 : 0)
}
main()
