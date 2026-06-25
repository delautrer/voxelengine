/* ============================================================
   VoxelEngine Wiki – Script V4
   
   Data format notes (from wiki_data.js):
   - blocks[]: { id (int), name (str), type, hardness, ... }
   - items[]:  { id (str = "birch_door"), name (str = "Birch Door"), textureName, type, maxStackSize, ... }
   - recipes[]:{ _id, type, pattern, keys:{A:"engine:x"}, result:{item:"engine:x", count}, ingredients:["engine:x"] }
   - furnace_recipes[]: { input:"engine:x", result:"engine:x", count, cook_time }
   - furnace_fuels[]:   { item:"engine:x", burn_time }
   - biomes[]:  { id (str), targetTemperature, targetHumidity, baseHeight, heightVariation, topBlock, underBlock, features[] }
   - loot_tables[]: (empty for now)
   - mechanics: { key: markdownString }
   ============================================================ */

'use strict';

document.addEventListener('DOMContentLoaded', () => {
  if (typeof WIKI_DATA === 'undefined') {
    document.getElementById('content-container').innerHTML =
      `<div style="padding:40px;color:#f85149;"><h2>Error: wiki_data.js not loaded.</h2>
       <p>Run <code>build_wiki.bat</code> first, then open <code>docs/index.html</code>.</p></div>`;
    return;
  }

  /* ── Helpers ──────────────────────────────────────────── */
  // Strip engine namespace prefix  "engine:birch_door" → "birch_door"
  const strip = s => {
    if (Array.isArray(s)) return s.map(x => strip(x));
    return String(s || '').replace(/^engine:/, '').replace(/^voxelengine:/, '');
  };
  const stripItem = s => strip(typeof s === 'string' ? s : (s?.item || ''));

  // Pretty-print id  "birch_door" → "Birch Door"
  const pretty = id => {
    if (Array.isArray(id)) return id.map(x => pretty(x)).join(' or ');
    return String(id || '').split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
  };

  // Item texture path (prefer item/, fallback block/ then renders/)
  const itemSrc = id => `assets/textures/item/${id}.png`;
  const blockSrc = id => `assets/renders/${id}.png`;

  // Image element that cascades through sources
  function makeImg(sources, style = '') {
    const img = document.createElement('img');
    let idx = 0;
    img.src = sources[idx];
    img.style.cssText = style;
    img.onerror = () => {
      idx++;
      if (idx < sources.length) img.src = sources[idx];
      else { img.src = ''; img.style.display = 'none'; }
    };
    return img;
  }

  /* ── Build Lookup Maps ────────────────────────────────── */
  // Blocks: keyed by name (string), has int id
  const blockByName = new Map(WIKI_DATA.blocks.map(b => [b.name, b]));

  // Items: keyed by id (string key same as textureName basically, all lowercase_underscored)
  const itemById = new Map(WIKI_DATA.items.map(i => [i.id, i]));

  // Furnace fuels: strip prefix → burn_time
  const fuelMap = new Map(
    Object.entries(WIKI_DATA.furnace_fuels || {}).map(([k,v]) => [strip(k), v.burn_time])
  );

  // Recipes: strip all engine: prefixes from result and keys
  const recipes = (WIKI_DATA.recipes || []).map(r => ({
    ...r,
    _id: strip(r._id),
    result_id: strip(r.result?.item || (typeof r.result === 'string' ? r.result : '') || ''),
    result_count: r.result?.count || 1,
    keys_clean: Object.fromEntries(Object.entries(r.keys || {}).map(([k,v]) => [k, strip(v)])),
    ingr_clean: (r.ingredients || []).map(x => strip(x)),
  }));

  const furnaceRecipes = (WIKI_DATA.furnace_recipes || []).map(r => ({
    ...r,
    input_clean: strip(r.input || r.ingredient),
    result_clean: strip(r.result),
  }));

  function appendDynamicItemSlot(parent, itemIdOrArr) {
      if (!itemIdOrArr) return;
      const arr = Array.isArray(itemIdOrArr) ? itemIdOrArr : [itemIdOrArr];
      if (arr.length === 0) return;
      
      const img = makeImg([itemSrc(arr[0]), blockSrc(arr[0])], 'width:32px;height:32px;');
      img.title = pretty(arr[0]);
      parent.appendChild(img);
      parent.addEventListener('click', () => showEntity(arr[0]));
      
      if (arr.length > 1) {
          let idx = 0;
          setInterval(() => {
              idx = (idx + 1) % arr.length;
              img.src = itemSrc(arr[idx]);
              img.onerror = () => { img.src = blockSrc(arr[idx]); img.onerror = null; };
              img.title = pretty(arr[idx]);
              parent.onclick = () => showEntity(arr[idx]);
          }, 2000);
      }
  }

  /* ── Variant Groups ───────────────────────────────────── */
  // Suffix categories for variant grouping
  const VARIANT_SUFFIXES = ['_door', '_trapdoor', '_log', '_planks', '_slab', '_slabs', '_stairs', '_leaves', '_sapling', '_chest', '_ore'];

  function getVariantKey(id) {
    for (const suf of VARIANT_SUFFIXES) {
      if (id.endsWith(suf)) return suf;
    }
    return null;
  }

  // Build variant groups
  const variantGroups = new Map(); // suffix → [id, ...]
  const allEntityIds = new Set([
    ...WIKI_DATA.blocks.map(b => b.name),
    ...WIKI_DATA.items.map(i => i.id),
  ]);

  allEntityIds.forEach(id => {
    const key = getVariantKey(id);
    if (key) {
      if (!variantGroups.has(key)) variantGroups.set(key, []);
      const g = variantGroups.get(key);
      if (!g.includes(id)) g.push(id);
    }
  });

  /* ── Sidebar Population ───────────────────────────────── */
  function makeNavItem(id, label, icon, onClick) {
    const li = document.createElement('li');
    li.className = 'nav-item';
    li.dataset.id = id;

    if (icon) {
      const img = makeImg(icon, 'width:16px;height:16px;image-rendering:pixelated;flex-shrink:0;border-radius:2px;');
      img.className = 'nav-icon';
      li.appendChild(img);
    }

    const span = document.createElement('span');
    span.textContent = label;
    li.appendChild(span);
    li.addEventListener('click', onClick);
    return li;
  }

  function makeCollapsibleSection(header, listEl) {
    const wrapper = document.createElement('div');
    wrapper.className = 'nav-section';

    const h = document.createElement('div');
    h.className = 'nav-section-header';
    h.innerHTML = `<span>${header}</span><span class="chevron">▼</span>`;
    h.addEventListener('click', () => {
      listEl.classList.toggle('collapsed');
      h.classList.toggle('collapsed');
    });

    wrapper.appendChild(h);
    wrapper.appendChild(listEl);
    return wrapper;
  }

  const navSections = document.getElementById('nav-sections');
  navSections.innerHTML = '';

  // Mechanics
  if (WIKI_DATA.mechanics && Object.keys(WIKI_DATA.mechanics).length) {
    const ul = document.createElement('ul');
    ul.className = 'nav-list';
    Object.keys(WIKI_DATA.mechanics).sort().forEach(k => {
      ul.appendChild(makeNavItem(k, pretty(k), null, () => showMechanic(k)));
    });
    navSections.appendChild(makeCollapsibleSection('⚙️ Game Mechanics', ul));
  }

  // Biomes
  {
    const ul = document.createElement('ul');
    ul.className = 'nav-list';
    WIKI_DATA.biomes.sort((a,b) => a.id.localeCompare(b.id)).forEach(b => {
      ul.appendChild(makeNavItem(b.id, pretty(b.id), null, () => showBiome(b.id)));
    });
    navSections.appendChild(makeCollapsibleSection('🌍 Biomes', ul));
  }

  // Blocks (unique, sorted)
  {
    const ul = document.createElement('ul');
    ul.className = 'nav-list';
    const uniqueBlockNames = [...new Set(WIKI_DATA.blocks.map(b => b.name))].sort();
    uniqueBlockNames.forEach(name => {
      // Skip dolomite ores in sidebar (grouped into normal ores)
      if (name.startsWith('dolomite_') && name.endsWith('_ore')) return;
      
      const icon = [itemSrc(name), blockSrc(name)];
      ul.appendChild(makeNavItem(name, pretty(name), icon, () => showEntity(name)));
    });
    navSections.appendChild(makeCollapsibleSection('🧱 Blocks', ul));
  }

  // Pure Items (not also a block)
  {
    const ul = document.createElement('ul');
    ul.className = 'nav-list';
    const pureItems = WIKI_DATA.items.filter(i => !blockByName.has(i.id)).sort((a,b) => a.id.localeCompare(b.id));
    pureItems.forEach(i => {
      const icon = [itemSrc(i.id), blockSrc(i.id)];
      ul.appendChild(makeNavItem(i.id, pretty(i.id), icon, () => showEntity(i.id)));
    });
    navSections.appendChild(makeCollapsibleSection('🎒 Items', ul));
  }

  /* ── Search ───────────────────────────────────────────── */
  document.getElementById('search-input').addEventListener('input', e => {
    const q = e.target.value.toLowerCase().trim();
    document.querySelectorAll('.nav-item').forEach(li => {
      const text = li.querySelector('span')?.textContent.toLowerCase() || '';
      li.style.display = (!q || text.includes(q)) ? '' : 'none';
    });
  });

  /* ── Active nav highlight ─────────────────────────────── */
  function setActiveNav(id) {
    document.querySelectorAll('.nav-item').forEach(el => {
      el.classList.toggle('active', el.dataset.id === id);
    });
  }

  /* ── UI Helpers ───────────────────────────────────────── */
  const $ = id => document.getElementById(id);

  function showView(articleVisible) {
    $('welcome-message').style.display = articleVisible ? 'none' : '';
    $('article-view').style.display    = articleVisible ? '' : 'none';
  }

  function setHeader(title, typeLabel, typeClass, breadcrumbs = []) {
    $('entry-title').textContent = title;

    const badge = $('entry-type-badge');
    badge.className = `type-badge ${typeClass}`;
    badge.textContent = typeLabel;

    const bc = $('entry-breadcrumb');
    bc.innerHTML = '';
    breadcrumbs.forEach((b, i) => {
      if (i > 0) { const sep = document.createElement('span'); sep.textContent = '/'; bc.appendChild(sep); }
      if (b.onClick) {
        const a = document.createElement('a');
        a.textContent = b.label;
        a.addEventListener('click', b.onClick);
        bc.appendChild(a);
      } else {
        const s = document.createElement('span');
        s.textContent = b.label;
        bc.appendChild(s);
      }
    });
  }

  function buildTabs(tabDefs) {
    const container = $('entry-tabs');
    container.innerHTML = '';
    tabDefs.forEach((t, i) => {
      const btn = document.createElement('button');
      btn.className = 'tab-btn' + (i === 0 ? ' active' : '');
      btn.textContent = t.label;
      btn.addEventListener('click', () => {
        container.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
        document.getElementById(t.paneId).classList.add('active');
      });
      container.appendChild(btn);
    });
  }

  function clearTabs() { $('entry-tabs').innerHTML = ''; }

  /* ── Infobox Helper ───────────────────────────────────── */
  function buildInfobox(stats) {
    $('info-title').textContent = $('entry-title').textContent;

    const imgC = $('info-image-container');
    imgC.innerHTML = '';

    const statsList = $('info-stats');
    statsList.innerHTML = '';
    stats.forEach(s => {
      const li = document.createElement('li');
      const label = document.createElement('span');
      label.className = 'label';
      label.textContent = s.label;
      const val = document.createElement('span');
      val.className = 'value';
      if (s.html) val.innerHTML = s.html;
      else val.textContent = s.value;
      li.appendChild(label);
      li.appendChild(val);
      statsList.appendChild(li);
    });
  }

  /* ── Recipe Renderers ─────────────────────────────────── */
  function renderCraftingCard(recipe) {
    const card = document.createElement('div');
    card.className = 'recipe-card';

    const hdr = document.createElement('div');
    hdr.className = 'recipe-header';
    hdr.textContent = recipe.type === 'shapeless' ? '🔀 Shapeless Recipe' : '🔨 Shaped Recipe';
    card.appendChild(hdr);

    const body = document.createElement('div');
    body.className = 'recipe-body';

    if (recipe.type === 'shaped' && recipe.pattern) {
      // 3x3 grid
      const grid = document.createElement('div');
      grid.className = 'craft-grid';

      for (let r = 0; r < 3; r++) {
        const row = (recipe.pattern[r] || '   ');
        for (let c = 0; c < 3; c++) {
          const ch = row[c] || ' ';
          const itemId = recipe.keys_clean?.[ch];
          const slot = document.createElement('div');
          slot.className = 'craft-slot' + (itemId ? '' : ' empty');
          appendDynamicItemSlot(slot, itemId);
          grid.appendChild(slot);
        }
      }
      body.appendChild(grid);
    } else if (recipe.type === 'shapeless' && recipe.ingr_clean?.length) {
      // Shapeless: show ingredients in a mini grid
      const grid = document.createElement('div');
      grid.className = 'craft-grid';
      for (let i = 0; i < 9; i++) {
        const itemId = recipe.ingr_clean[i];
        const slot = document.createElement('div');
        slot.className = 'craft-slot' + (itemId ? '' : ' empty');
        appendDynamicItemSlot(slot, itemId);
        grid.appendChild(slot);
      }
      body.appendChild(grid);
    }

    // Arrow
    const arrow = document.createElement('div');
    arrow.className = 'craft-arrow';
    arrow.textContent = '→';
    body.appendChild(arrow);

    // Result
    const resultSlot = document.createElement('div');
    resultSlot.className = 'craft-result';
    const resultId = recipe.result_id;
    const rImg = makeImg([itemSrc(resultId), blockSrc(resultId)], 'width:38px;height:38px;');
    rImg.title = pretty(resultId);
    resultSlot.appendChild(rImg);
    if (recipe.result_count > 1) {
      const cnt = document.createElement('div');
      cnt.className = 'craft-count';
      cnt.textContent = recipe.result_count;
      resultSlot.appendChild(cnt);
    }
    resultSlot.addEventListener('click', () => showEntity(resultId));
    body.appendChild(resultSlot);

    card.appendChild(body);
    return card;
  }

  function renderSmeltingCard(fr) {
    const card = document.createElement('div');
    card.className = 'recipe-card';

    const hdr = document.createElement('div');
    hdr.className = 'recipe-header';
    hdr.textContent = '🔥 Smelting Recipe';
    card.appendChild(hdr);

    const body = document.createElement('div');
    body.className = 'smelt-row';

    // Input slot
    const inputSlot = document.createElement('div');
    inputSlot.className = 'craft-slot';
    const iImg = makeImg([itemSrc(fr.input_clean), blockSrc(fr.input_clean)], 'width:32px;height:32px;');
    inputSlot.appendChild(iImg);
    inputSlot.addEventListener('click', () => showEntity(fr.input_clean));
    body.appendChild(inputSlot);

    // Furnace visual
    const furnDiv = document.createElement('div');
    furnDiv.className = 'smelt-furnace';
    furnDiv.innerHTML = '<span class="smelt-fire">🔥</span>';
    // Fuel slot
    const fuelSlot = document.createElement('div');
    fuelSlot.className = 'fuel-slot';
    fuelSlot.title = 'Any fuel';
    fuelSlot.innerHTML = '<span style="font-size:0.7rem;color:#6b7280;">⛽</span>';
    furnDiv.appendChild(fuelSlot);
    body.appendChild(furnDiv);

    // Arrow
    const arrow = document.createElement('div');
    arrow.className = 'craft-arrow';
    arrow.textContent = '→';
    body.appendChild(arrow);

    // Output
    const outSlot = document.createElement('div');
    outSlot.className = 'craft-result';
    const oImg = makeImg([itemSrc(fr.result_clean), blockSrc(fr.result_clean)], 'width:38px;height:38px;');
    outSlot.appendChild(oImg);
    if (fr.count > 1) {
      const cnt = document.createElement('div');
      cnt.className = 'craft-count';
      cnt.textContent = fr.count;
      outSlot.appendChild(cnt);
    }
    outSlot.addEventListener('click', () => showEntity(fr.result_clean));
    body.appendChild(outSlot);

    card.appendChild(body);

    // Cook time info
    const info = document.createElement('div');
    info.style.cssText = 'font-size:0.78rem;color:var(--text-muted);margin-top:8px;';
    info.textContent = `Cook time: ${fr.cook_time} ticks (${(fr.cook_time/20).toFixed(1)}s)`;
    card.appendChild(info);

    return card;
  }

  /* ── Variant Cards ────────────────────────────────────── */
  function buildVariantsSection(currentId) {
    const key = getVariantKey(currentId);
    if (currentId.endsWith('_ore')) {
        const wrapper = document.createElement('div');
        
        // 1. Stone Variants Section
        const baseOreId = currentId.replace('dolomite_', '');
        const stoneVariants = [baseOreId, `dolomite_${baseOreId}`].filter(v => blockByName.has(v));
        
        if (stoneVariants.length > 0) {
            const stoneSec = document.createElement('div');
            stoneSec.className = 'variants-section';
            const stoneTitle = document.createElement('div');
            stoneTitle.className = 'variants-title';
            stoneTitle.textContent = 'Stone Variants';
            stoneSec.appendChild(stoneTitle);
            
            const stoneGrid = document.createElement('div');
            stoneGrid.className = 'variants-grid';
            stoneVariants.forEach(vid => {
                const card = document.createElement('div');
                card.className = 'variant-card' + (vid === id ? ' current' : '');
                card.style.cursor = 'pointer';
                const img = makeImg([blockSrc(vid), itemSrc(vid)]);
                img.style.cssText = 'width:48px;height:48px;image-rendering:pixelated;';
                card.appendChild(img);
                const lbl = document.createElement('span');
                lbl.textContent = vid.includes('dolomite') ? 'Dolomite' : 'Stone';
                card.appendChild(lbl);
                if (vid !== id) card.addEventListener('click', () => showEntity(vid));
                stoneGrid.appendChild(card);
            });
            stoneSec.appendChild(stoneGrid);
            wrapper.appendChild(stoneSec);
        }
        
        // 2. Other Ores Section
        const allOres = [...new Set(WIKI_DATA.blocks.map(b => b.name))].filter(n => n.endsWith('_ore') && !n.startsWith('dolomite_'));
        if (allOres.length > 1) {
            const oreSec = document.createElement('div');
            oreSec.className = 'variants-section';
            const oreTitle = document.createElement('div');
            oreTitle.className = 'variants-title';
            oreTitle.textContent = 'Other Ores';
            oreSec.appendChild(oreTitle);
            
            const oreGrid = document.createElement('div');
            oreGrid.className = 'variants-grid';
            allOres.forEach(vid => {
                const card = document.createElement('div');
                card.className = 'variant-card' + (vid === baseOreId ? ' current' : '');
                const img = makeImg([itemSrc(vid), blockSrc(vid)]);
                img.style.cssText = 'width:32px;height:32px;image-rendering:pixelated;';
                card.appendChild(img);
                const lbl = document.createElement('span');
                lbl.textContent = pretty(vid).replace(' Ore', '');
                card.appendChild(lbl);
                if (vid !== baseOreId) card.addEventListener('click', () => showEntity(vid));
                oreGrid.appendChild(card);
            });
            oreSec.appendChild(oreGrid);
            wrapper.appendChild(oreSec);
        }
        
        return wrapper;
    }

    if (!key) return null;
    const group = variantGroups.get(key) || [];
    if (group.length <= 1) return null;

    const section = document.createElement('div');
    section.className = 'variants-section';
    const title = document.createElement('div');
    title.className = 'variants-title';
    title.textContent = `Other ${pretty(key.replace('_', ''))} variants`;
    section.appendChild(title);

    const grid = document.createElement('div');
    grid.className = 'variants-grid';

    group.sort().forEach(vid => {
      const card = document.createElement('div');
      card.className = 'variant-card' + (vid === currentId ? ' current' : '');
      const img = makeImg([itemSrc(vid), blockSrc(vid)]);
      img.style.cssText = 'width:32px;height:32px;image-rendering:pixelated;';
      card.appendChild(img);
      const lbl = document.createElement('span');
      lbl.textContent = pretty(vid).replace(pretty(key.replace('_', '')), '').trim() || pretty(vid);
      card.appendChild(lbl);
      if (vid !== currentId) card.addEventListener('click', () => showEntity(vid));
      grid.appendChild(card);
    });

    section.appendChild(grid);
    return section;
  }

  /* ══════════════════════════════════════════════════════
     SHOW ENTITY (block + item, unified)
  ══════════════════════════════════════════════════════ */
  window.showEntity = function(rawId, pushState = true) {
    const id = strip(rawId);
    const block = blockByName.get(id);
    const item  = itemById.get(id);
    if (!block && !item) { console.warn('Unknown entity:', id); return; }
    if (window._oreInterval) clearInterval(window._oreInterval);

    if (pushState) {
        window.history.pushState({ id }, '', '#' + id);
    }

    showView(true);
    setActiveNav(id);

    const name = block ? pretty(block.name) : pretty(item.id);
    const isUnified = !!(block && item);
    const typeLabel = isUnified ? 'Block / Item' : (block ? 'Block' : 'Item');
    const typeClass  = block ? 'block' : 'item';
    setHeader(name, typeLabel, typeClass, [{ label: 'Home', onClick: () => showView(false) }, { label: name }]);

    /* Infobox */
    const stats = [
      { label: 'Namespace', html: `<span class="ns-badge">engine:${id}</span>` },
    ];
    if (block) {
      stats.push(
        { label: 'Block ID',  value: block.id },
        { label: 'Type',      value: block.type || 'cube' },
        { label: 'Hardness',  value: block.hardness ?? '—' },
        { label: 'Light',     value: block.lightEmission ?? 0 },
        { label: 'Solid',     value: block.isSolid !== false ? 'Yes' : 'No' },
        { label: 'Category',  value: block.category || '—' },
      );
    }
    if (item) {
      stats.push(
        { label: 'Max Stack', value: item.maxStackSize || 64 },
      );
      if (item.damage) stats.push({ label: 'Damage', value: item.damage });
      if (item.toolTier) stats.push({ label: 'Tool Tier', value: item.toolTier });
    }
    
    // Add natural generation info if available
    if (block && block.category === 'natural') {
        const genBiomes = WIKI_DATA.biomes.filter(b => 
            b.topBlock === id || b.underBlock === id || b.underwaterBlock === id || b.deepBlock === id ||
            (b.undergroundBlobs && b.undergroundBlobs[id]) || (b.underwaterBlobs && b.underwaterBlobs[id])
        );
        if (genBiomes.length > 0) {
            stats.push({ label: 'Generates In', html: genBiomes.map(b => `<span style="cursor:pointer;color:var(--accent)" onclick="window.showBiome('${b.id}')">${pretty(b.id)}</span>`).join(', ') });
        }
    }

    buildInfobox(stats);

    const imgC = $('info-image-container');
    if (block) {
      const img1 = makeImg([blockSrc(id), itemSrc(id)]);
      img1.className = 'render';
      imgC.appendChild(img1);
      
      const img2 = makeImg([itemSrc(id), blockSrc(id)]);
      img2.className = 'item';
      img2.title = 'Item texture';
      imgC.appendChild(img2);

      // Ore texture swapper
      if (window._oreInterval) clearInterval(window._oreInterval);
      if (id.endsWith('_ore') && !id.startsWith('dolomite_')) {
          const variants = [id, `dolomite_${id}`].filter(v => blockByName.has(v));
          if (variants.length > 1) {
              let swapIdx = 0;
              window._oreInterval = setInterval(() => {
                  swapIdx = (swapIdx + 1) % variants.length;
                  const v = variants[swapIdx];
                  img1.src = blockSrc(v);
                  img2.src = itemSrc(v);
                  $('info-title').innerHTML = `${$('entry-title').textContent} <span style="font-size:0.7em; color:var(--text-muted); display:block; margin-top:4px;">(${v.includes('dolomite') ? 'Dolomite' : 'Stone'})</span>`;
              }, 2000);
          }
      }
    } else {
      const img1 = makeImg([itemSrc(id), blockSrc(id)]);
      img1.className = 'render';
      imgC.appendChild(img1);
    }

    /* Tabs */
      const craftedBy   = recipes.filter(r => r.result_id === id);
      const smeltedBy    = furnaceRecipes.filter(r => r.result_clean === id);
      const usedInCraft  = recipes.filter(r => {
         const keysObj = Object.values(r.keys_clean);
         const inKeys = keysObj.some(v => Array.isArray(v) ? v.includes(id) : v === id);
         const inIngr = r.ingr_clean.some(v => Array.isArray(v) ? v.includes(id) : v === id);
         return inKeys || inIngr;
      });
      const usedInSmelt  = furnaceRecipes.filter(r => r.input_clean === id);
    const burnTime     = fuelMap.get(id);
    const ownDrops     = (WIKI_DATA.loot_tables || []).filter(l => strip(l._id) === id);

    const tabDefs = [{ label: 'Overview', paneId: 'pane-overview' }];
    if (craftedBy.length || smeltedBy.length)  tabDefs.push({ label: '⚒ Crafting', paneId: 'pane-crafting' });
    if (usedInCraft.length || usedInSmelt.length || burnTime) tabDefs.push({ label: '🔗 Used In', paneId: 'pane-usedin' });
    if (ownDrops.length)   tabDefs.push({ label: '📦 Drops', paneId: 'pane-drops' });
    buildTabs(tabDefs);

    /* Build pane container */
    const main = $('entry-main');
    main.innerHTML = '';

    /* ─ Overview Pane ─ */
    const paneOverview = document.createElement('div');
    paneOverview.id = 'pane-overview';
    paneOverview.className = 'tab-pane active';

    // Fuel banner
    if (burnTime) {
      const banner = document.createElement('div');
      banner.className = 'fuel-banner';
      banner.innerHTML = `<span class="icon">🔥</span>
        <div><strong>Can be used as Furnace Fuel</strong>
        <span>Burntime: ${burnTime} ticks &nbsp;·&nbsp; ${(burnTime/20).toFixed(1)} seconds &nbsp;·&nbsp; smelts ${(burnTime/200).toFixed(1)} items</span></div>`;
      paneOverview.appendChild(banner);
    }

    // Description
    const desc = document.createElement('div');
    desc.className = 'overview-desc md';
    desc.innerHTML = generateDesc(id, block, item);
    paneOverview.appendChild(desc);

    // Variants
    const varSection = buildVariantsSection(id);
    if (varSection) paneOverview.appendChild(varSection);

    main.appendChild(paneOverview);

    /* ─ Crafting Pane ─ */
    if (craftedBy.length || smeltedBy.length) {
      const paneCraft = document.createElement('div');
      paneCraft.id = 'pane-crafting';
      paneCraft.className = 'tab-pane';

      if (craftedBy.length) {
        const h = document.createElement('h2');
        h.className = 'section-h2';
        h.textContent = 'Crafting';
        paneCraft.appendChild(h);
        const wrap = document.createElement('div');
        wrap.className = 'recipe-wrapper';
        craftedBy.forEach(r => wrap.appendChild(renderCraftingCard(r)));
        paneCraft.appendChild(wrap);
      }

      if (smeltedBy.length) {
        const h = document.createElement('h2');
        h.className = 'section-h2';
        h.textContent = 'Smelting';
        paneCraft.appendChild(h);
        const wrap = document.createElement('div');
        wrap.className = 'recipe-wrapper';
        smeltedBy.forEach(r => wrap.appendChild(renderSmeltingCard(r)));
        paneCraft.appendChild(wrap);
      }

      main.appendChild(paneCraft);
    }

    /* ─ Used In Pane ─ */
    if (usedInCraft.length || usedInSmelt.length || burnTime) {
      const paneUsed = document.createElement('div');
      paneUsed.id = 'pane-usedin';
      paneUsed.className = 'tab-pane';

      if (usedInCraft.length) {
        const h = document.createElement('h2');
        h.className = 'section-h2';
        h.textContent = 'Used in Crafting';
        paneUsed.appendChild(h);
        const list = document.createElement('div');
        list.className = 'usedin-list';
        usedInCraft.forEach(r => {
          const rid = r.result_id;
          const item = document.createElement('div');
          item.className = 'usedin-item';
          const img = makeImg([itemSrc(rid), blockSrc(rid)], 'width:28px;height:28px;image-rendering:pixelated;');
          item.appendChild(img);
          const txt = document.createElement('div');
          txt.innerHTML = `<div class="name">${pretty(rid)}</div><div class="sub">Crafted × ${r.result_count}</div>`;
          item.appendChild(txt);
          item.addEventListener('click', () => showEntity(rid));
          list.appendChild(item);
        });
        paneUsed.appendChild(list);
      }

      if (usedInSmelt.length) {
        const h = document.createElement('h2');
        h.className = 'section-h2';
        h.textContent = 'Used in Smelting';
        paneUsed.appendChild(h);
        const list = document.createElement('div');
        list.className = 'usedin-list';
        usedInSmelt.forEach(fr => {
          const rid = fr.result_clean;
          const item = document.createElement('div');
          item.className = 'usedin-item';
          const img = makeImg([itemSrc(rid), blockSrc(rid)], 'width:28px;height:28px;image-rendering:pixelated;');
          item.appendChild(img);
          const txt = document.createElement('div');
          txt.innerHTML = `<div class="name">${pretty(rid)}</div><div class="sub">Smelted → ${pretty(rid)} · ${fr.cook_time} ticks</div>`;
          item.appendChild(txt);
          item.addEventListener('click', () => showEntity(rid));
          list.appendChild(item);
        });
        paneUsed.appendChild(list);
      }

      if (burnTime) {
        const h = document.createElement('h2');
        h.className = 'section-h2';
        h.textContent = 'As Fuel';
        paneUsed.appendChild(h);
        const note = document.createElement('p');
        note.style.color = 'var(--text-soft)';
        note.innerHTML = `<strong>${pretty(id)}</strong> can be used as fuel in a furnace.<br>
          Burntime: <strong>${burnTime} ticks</strong> · ${(burnTime/20).toFixed(1)} seconds · smelts ${(burnTime/200).toFixed(1)} items.`;
        paneUsed.appendChild(note);
      }

      main.appendChild(paneUsed);
    }

    /* ─ Drops Pane ─ */
    if (ownDrops.length) {
      const paneDrops = document.createElement('div');
      paneDrops.id = 'pane-drops';
      paneDrops.className = 'tab-pane';

      const table = document.createElement('table');
      table.className = 'wiki-table';
      table.innerHTML = `<tr><th>Item</th><th>Chance</th><th>Count</th></tr>`;
      ownDrops.forEach(lt => {
        (lt.pools || []).forEach(pool => {
          (pool.entries || []).forEach(entry => {
            const did = strip(entry.item || entry);
            const tr = document.createElement('tr');
            tr.className = 'clickable';
            tr.innerHTML = `<td class="item-cell"><img src="${itemSrc(did)}" onerror="this.src='${blockSrc(did)}'"> ${pretty(did)}</td>
              <td>${pool.rolls !== undefined ? (pool.rolls * 100).toFixed(0) + '%' : '?'}</td>
              <td>1</td>`;
            tr.addEventListener('click', () => showEntity(did));
            table.appendChild(tr);
          });
        });
      });
      paneDrops.appendChild(table);
      main.appendChild(paneDrops);
    }
  };

  /* ══════════════════════════════════════════════════════
     SHOW BIOME
  ══════════════════════════════════════════════════════ */
  function showBiome(id) {
    const b = WIKI_DATA.biomes.find(x => x.id === id);
    if (!b) return;

    showView(true);
    setActiveNav(id);
    setHeader(pretty(id), 'Biome', 'biome', [{ label: 'Home', onClick: () => showView(false) }, { label: pretty(id) }]);

    buildInfobox([], [
      { label: 'Namespace', html: `<span class="ns-badge">voxelengine:${id}</span>` },
      { label: 'Temperature', value: Array.isArray(b.temperature) ? `${b.temperature[0]} – ${b.temperature[1]}` : (b.targetTemperature ?? '—') },
      { label: 'Humidity',    value: Array.isArray(b.humidity)    ? `${b.humidity[0]} – ${b.humidity[1]}`       : (b.targetHumidity ?? '—') },
      { label: 'Base Height', value: b.baseHeight ?? '—' },
      { label: 'Height Var.', value: b.heightVariation ?? '—' },
      { label: 'Tree Prob.',  value: b.treeProbability != null ? (b.treeProbability * 100).toFixed(0) + '%' : '—' },
    ]);

    $('info-image-container').innerHTML = '<span style="font-size:2.5rem;">🌍</span>';

    buildTabs([{ label: 'Overview', paneId: 'pane-biome-overview' }]);

    const main = $('entry-main');
    main.innerHTML = '';

    const pane = document.createElement('div');
    pane.id = 'pane-biome-overview';
    pane.className = 'tab-pane active';

    // Stats Cards
    const grid = document.createElement('div');
    grid.className = 'biome-stats-grid';
    [
      { label: 'Temperature', value: Array.isArray(b.temperature) ? `${b.temperature[0]} – ${b.temperature[1]}` : (b.targetTemperature ?? '—') },
      { label: 'Humidity',    value: Array.isArray(b.humidity)    ? `${b.humidity[0]} – ${b.humidity[1]}`       : (b.targetHumidity ?? '—') },
      { label: 'Base Height', value: b.baseHeight ?? '—' },
      { label: 'Height Var.', value: b.heightVariation ?? '—' },
      { label: 'Flora Prob.', value: b.floraProbability != null ? (b.floraProbability * 100).toFixed(0) + '%' : '—' },
      { label: 'Tree Prob.',  value: b.treeProbability  != null ? (b.treeProbability  * 100).toFixed(0) + '%' : '—' },
    ].forEach(s => {
      grid.innerHTML += `<div class="biome-stat-card"><div class="label">${s.label}</div><div class="value">${s.value}</div></div>`;
    });
    pane.appendChild(grid);

    // Surface Blocks
    if (b.topBlock || b.underBlock) {
      const h = document.createElement('h2');
      h.className = 'section-h2';
      h.textContent = 'Surface Blocks';
      pane.appendChild(h);

      const list = document.createElement('div');
      list.className = 'usedin-list';
      const addBlock = (label, bid) => {
        if (!bid) return;
        const cleanId = strip(bid);
        const row = document.createElement('div');
        row.className = 'usedin-item';
        const img = makeImg([itemSrc(cleanId), blockSrc(cleanId)], 'width:28px;height:28px;image-rendering:pixelated;');
        row.appendChild(img);
        const txt = document.createElement('div');
        txt.innerHTML = `<div class="name">${pretty(cleanId)}</div><div class="sub">${label}</div>`;
        row.appendChild(txt);
        row.addEventListener('click', () => showEntity(cleanId));
        list.appendChild(row);
      };
      addBlock('Top surface block', b.topBlock);
      addBlock('Under surface block', b.underBlock);
      pane.appendChild(list);
    }

    // Trees
    if (b.trees && Object.keys(b.trees).length) {
      const h = document.createElement('h2');
      h.className = 'section-h2';
      h.textContent = 'Trees';
      pane.appendChild(h);
      const ul = document.createElement('ul');
      ul.style.cssText = 'color:var(--text-soft);padding-left:20px;';
      Object.entries(b.trees).forEach(([k,w]) => {
        const li = document.createElement('li');
        li.textContent = `${pretty(k)} (weight: ${w})`;
        ul.appendChild(li);
      });
      pane.appendChild(ul);
    }

    // Flora
    if (b.flora && Object.keys(b.flora).length) {
      const h = document.createElement('h2');
      h.className = 'section-h2';
      h.textContent = 'Flora';
      pane.appendChild(h);
      const ul = document.createElement('ul');
      ul.style.cssText = 'color:var(--text-soft);padding-left:20px;';
      Object.entries(b.flora).forEach(([k,w]) => {
        const li = document.createElement('li');
        li.textContent = `${pretty(k)} (weight: ${w})`;
        ul.appendChild(li);
      });
      pane.appendChild(ul);
    }

    // Underground Blobs
    if (b.undergroundBlobs && Object.keys(b.undergroundBlobs).length) {
      const h = document.createElement('h2');
      h.className = 'section-h2';
      h.textContent = 'Underground Blobs';
      pane.appendChild(h);
      const ul = document.createElement('ul');
      ul.style.cssText = 'color:var(--text-soft);padding-left:20px;';
      Object.entries(b.undergroundBlobs).forEach(([k,v]) => {
        const li = document.createElement('li');
        li.innerHTML = `<span style="cursor:pointer;color:var(--accent)" onclick="window.showEntity('${k}')">${pretty(k)}</span> (density: ${v})`;
        ul.appendChild(li);
      });
      pane.appendChild(ul);
    }

    main.appendChild(pane);
  }

  /* ══════════════════════════════════════════════════════
     SHOW MECHANIC
  ══════════════════════════════════════════════════════ */
  function showMechanic(id) {
    showView(true);
    setActiveNav(id);
    setHeader(pretty(id), 'Game Mechanic', 'mechanic', [{ label: 'Home', onClick: () => showView(false) }, { label: pretty(id) }]);

    $('info-image-container').innerHTML = '<span style="font-size:2.5rem;">⚙️</span>';
    $('info-stats').innerHTML = '';
    $('info-title').textContent = pretty(id);

    buildTabs([{ label: 'Documentation', paneId: 'pane-mechanic' }]);

    const main = $('entry-main');
    main.innerHTML = '';
    const pane = document.createElement('div');
    pane.id = 'pane-mechanic';
    pane.className = 'tab-pane active md';
    pane.innerHTML = marked.parse(WIKI_DATA.mechanics[id] || '*No documentation available.*');
    main.appendChild(pane);
  }

  /* ── Auto-generated description ───────────────────────── */
  function generateDesc(id, block, item) {
    if (WIKI_DATA.blocks_md && WIKI_DATA.blocks_md[id]) return marked.parse(WIKI_DATA.blocks_md[id]);
    if (WIKI_DATA.items_md && WIKI_DATA.items_md[id]) return marked.parse(WIKI_DATA.items_md[id]);

    if (id.includes('_door'))      return `${pretty(id)} is a door block that can be placed and opened by players. Use it to secure your builds.`;
    if (id.includes('_trapdoor'))  return `${pretty(id)} is a trapdoor that can be placed horizontally or vertically and opened by players.`;
    if (id.includes('_log'))       return `${pretty(id)} is a natural wood block found in tree trunks. It can be smelted into charcoal or crafted into planks.`;
    if (id.includes('_planks'))    return `${pretty(id)} is a crafted wood plank, one of the most versatile building materials in VoxelEngine.`;
    if (id.includes('_leaves'))    return `${pretty(id)} naturally generates as part of tree canopies. Breaking them may drop saplings.`;
    if (id.includes('_sapling'))   return `${pretty(id)} grows into a full tree when placed on grass or dirt. Watch it grow over time!`;
    if (id.includes('grass_block'))return 'Grass Block is one of the most common surface blocks. It spreads to adjacent dirt blocks over time.';
    if (id.includes('stone'))      return 'Stone is the most common underground block, making up the vast majority of the world below the surface.';
    if (id.includes('coal'))       return 'Coal is a valuable fuel and crafting ingredient, found underground as ore or smelted from wood.';
    if (id.includes('sand'))       return 'Sand is a gravity-affected block found in deserts and river shores. It falls when unsupported.';
    if (id.includes('gravel'))     return 'Gravel is a gravity-affected block that falls when unsupported. Can be found in riverbeds.';
    if (id.includes('bedrock'))    return 'Bedrock is an indestructible block forming the very bottom of the world. Nothing can break it.';
    if (id.includes('_ore'))       return `${pretty(id)} is a mineral ore that can be mined and smelted into a useful material.`;
    if (id.includes('_slab') || id.includes('_slabs')) return `${pretty(id)} is a half-height variant of a full block, useful for decorative builds and ramps.`;
    if (id.includes('_stairs'))    return `${pretty(id)} provides a staircase variant for smooth elevation changes in your builds.`;
    if (id.includes('_chest'))     return `${pretty(id)} is a storage block that can hold 27 item stacks.`;
    if (block) return `${pretty(id)} is a block in VoxelEngine. It can be placed in the world and interacted with.`;
    return `${pretty(id)} is an item in VoxelEngine.`;
  }

  /* 🪟 Home Link 🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟🪟 */
  $('entry-home-btn')?.addEventListener('click', () => {
      window.history.pushState(null, '', window.location.pathname);
      showView(false);
  });

  /* URL Routing */
  window.addEventListener('popstate', (e) => {
      if (e.state && e.state.id) {
          showEntity(e.state.id, false);
      } else if (window.location.hash) {
          showEntity(window.location.hash.substring(1), false);
      } else {
          showView(false);
      }
  });

  if (window.location.hash) {
      showEntity(window.location.hash.substring(1), false);
  }
});
