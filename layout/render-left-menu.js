/**
 * docs/menu-structure.json 기반으로 좌측 메뉴 DOM 생성
 * 사용: fetch('docs/menu-structure.json').then(r=>r.json()).then(data => renderLeftMenu(data, document.getElementById('side-nav-ul')))
 */
function renderLeftMenu(menuData, containerEl) {
  if (!containerEl || !menuData.items || !Array.isArray(menuData.items)) return;

  const menuMoveFn = menuData.menuMoveFn || 'fnTopMenuMove';

  containerEl.innerHTML = menuData.items.map((group, groupIndex) => {
    const isFirst = groupIndex === 0;
    const activeClass = isFirst ? ' mm-active' : '';
    const childActiveClass = isFirst ? ' mm-active' : '';
    const childShowClass = isFirst ? ' mm-show' : '';
    const expanded = isFirst ? 'true' : 'false';

    const childrenHtml = (group.children || []).map((item, childIndex) => {
      const liActive = isFirst && childIndex === 0 ? ' mm-active' : '';
      return `<li class="child-li${liActive}" data-url="${escapeHtml(item.url)}">
        <a href="javascript:${menuMoveFn}('${escapeHtml(item.url)}')" data-menu_id="${escapeHtml(item.menuId)}">${escapeHtml(item.label)}</a>
      </li>`;
    }).join('');

    return `<li class="side-nav-item${activeClass}">
      <a href="javascript:void(0)" class="side-nav-link" aria-expanded="${expanded}">
        <i class="${escapeHtml(group.icon)}"></i>
        <span> ${escapeHtml(group.label)} </span>
        <span class="menu-arrow"></span>
      </a>
      <ul class="side-nav-second-level mm-collapse${childShowClass}" aria-expanded="${expanded}">
        ${childrenHtml}
      </ul>
    </li>`;
  }).join('');

  function escapeHtml(s) {
    if (s == null) return '';
    const div = document.createElement('div');
    div.textContent = s;
    return div.innerHTML;
  }
}
