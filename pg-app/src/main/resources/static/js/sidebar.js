(function () {
  var KEY = 'pg-sidebar-collapsed';
  var sidebar = document.querySelector('.dashboard-sidebar');
  var btn = document.getElementById('sidebarToggle');
  if (!sidebar || !btn) return;

  function load() {
    try {
      if (localStorage.getItem(KEY) === '1') sidebar.classList.add('collapsed');
    } catch (e) {}
  }

  function save() {
    try {
      localStorage.setItem(KEY, sidebar.classList.contains('collapsed') ? '1' : '0');
    } catch (e) {}
  }

  btn.addEventListener('click', function () {
    sidebar.classList.toggle('collapsed');
    btn.setAttribute('title', sidebar.classList.contains('collapsed') ? '메뉴 펼치기' : '메뉴 접기');
    save();
  });

  load();
  btn.setAttribute('title', sidebar.classList.contains('collapsed') ? '메뉴 펼치기' : '메뉴 접기');
})();
