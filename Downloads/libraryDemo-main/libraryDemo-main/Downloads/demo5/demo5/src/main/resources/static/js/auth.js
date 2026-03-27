(function () {
  const STORAGE_KEY = 'demo5_current_user';

  function normalizeUser(user) {
    if (!user || typeof user.username !== 'string' || !user.username.trim()) {
      return null;
    }
    return {
      id: user.id ?? null,
      username: user.username.trim(),
      role: user.role || null
    };
  }

  function getStoredUser() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw);
      return normalizeUser(parsed);
    } catch (error) {
      return normalizeUser({ username: raw, role: null });
    }
  }

  function getStoredUsername() {
    return getStoredUser()?.username || '';
  }

  function isAdmin(user) {
    return !!user && user.role === 'ADMIN';
  }

  function saveUser(user) {
    const normalized = normalizeUser(user);
    if (!normalized) {
      clearUser();
      return;
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
  }

  function clearUser() {
    localStorage.removeItem(STORAGE_KEY);
  }

  async function loadCurrentUser(forceRefresh) {
    const stored = getStoredUser();
    if (!stored?.username) {
      return null;
    }
    if (!forceRefresh && stored.role) {
      return stored;
    }

    try {
      const res = await fetch(`/api/users/${encodeURIComponent(stored.username)}/profile`);
      if (!res.ok) {
        if (res.status === 401 || res.status === 404) {
          clearUser();
          return null;
        }
        return stored;
      }

      const body = await res.json();
      const merged = normalizeUser({
        id: body.id ?? stored.id,
        username: body.username ?? stored.username,
        role: body.role ?? stored.role ?? 'USER'
      });
      saveUser(merged);
      return merged;
    } catch (error) {
      return stored;
    }
  }

  function buildAuthHeaders(user) {
    const username = user?.username || getStoredUsername();
    return username ? { 'X-Auth-Username': username } : {};
  }

  async function requireAdminPage(options) {
    const settings = {
      loginUrl: '/login.html',
      fallbackUrl: '/tianxia.html',
      ...options
    };
    const user = await loadCurrentUser(true);
    if (!user?.username) {
      window.location.href = settings.loginUrl;
      return null;
    }
    if (!isAdmin(user)) {
      window.location.href = settings.fallbackUrl;
      return null;
    }
    return user;
  }

  window.Demo5Auth = {
    STORAGE_KEY,
    getStoredUser,
    getStoredUsername,
    isAdmin,
    saveUser,
    clearUser,
    loadCurrentUser,
    buildAuthHeaders,
    requireAdminPage
  };
})();