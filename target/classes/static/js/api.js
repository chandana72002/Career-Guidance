const CareerAPI = (() => {
    const API_BASE = window.localStorage.getItem("career.apiBase") || window.location.origin;
    const TOKEN_KEY = "career.jwt";
    const USER_KEY = "career.user";
    const ROLE_LABELS = {
        ROLE_USER: "Student",
        ROLE_COUNSELOR: "Counselor",
        ROLE_ADMIN: "Admin"
    };

    const endpoints = {
        login: "/api/auth/login",
        register: "/api/auth/register",
        skills: "/api/skills",
        profile: "/api/profile/me",
        assessmentQuestions: "/api/assessments/default/questions",
        assessmentSubmit: "/api/assessments/default/submit",
        latestAssessment: "/api/assessments/results/latest",
        recommendations: "/api/recommendations/me",
        generateRecommendations: "/api/recommendations/generate",
        counselorOverview: "/api/counselor/overview",
        adminOverview: "/api/admin/overview",
        adminCareers: "/api/admin/careers",
        adminCareer: (id) => `/api/admin/careers/${id}`,
        careers: "/api/careers",
        saveCareer: (id) => `/api/careers/${id}/save`
    };

    function getToken() {
        return window.localStorage.getItem(TOKEN_KEY);
    }

    function setToken(token) {
        if (!token) {
            window.localStorage.removeItem(TOKEN_KEY);
            return;
        }
        window.localStorage.setItem(TOKEN_KEY, token);
    }

    function getUser() {
        const raw = window.localStorage.getItem(USER_KEY);
        return raw ? JSON.parse(raw) : null;
    }

    function normalizeRole(role) {
        if (!role) {
            return "ROLE_USER";
        }

        const normalized = String(role).trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }

        if (normalized === "COUNSELOR" || normalized === "COUNSELLOR" || normalized === "CONSELOR") {
            return "ROLE_COUNSELOR";
        }

        if (normalized === "ADMIN") {
            return "ROLE_ADMIN";
        }

        return "ROLE_USER";
    }

    function getRole() {
        return normalizeRole(getUser()?.role);
    }

    function getRoleLabel(role = getRole()) {
        return ROLE_LABELS[normalizeRole(role)] || "Student";
    }

    function setUser(user) {
        if (!user) {
            window.localStorage.removeItem(USER_KEY);
            return;
        }

        const normalizedUser = {
            ...user,
            role: normalizeRole(user.role)
        };
        window.localStorage.setItem(USER_KEY, JSON.stringify(normalizedUser));
    }

    function storeSession(payload) {
        setToken(payload.token);
        setUser(payload.user);
    }

    function clearSession() {
        setToken(null);
        setUser(null);
    }

    async function request(path, options = {}) {
        const headers = new Headers(options.headers || {});
        const token = getToken();

        if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
            headers.set("Content-Type", "application/json");
        }

        if (token) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        const response = await fetch(`${API_BASE}${path}`, {
            ...options,
            headers
        });

        const isJson = response.headers.get("content-type")?.includes("application/json");
        const payload = isJson ? await response.json() : await response.text();

        if (!response.ok) {
            const error = new Error(payload?.message || payload || "Request failed");
            error.status = response.status;
            error.payload = payload;

            if (response.status === 401) {
                clearSession();
            }

            throw error;
        }

        return payload;
    }

    function notify(message, tone = "success") {
        let stack = document.querySelector(".toast-stack");
        if (!stack) {
            stack = document.createElement("div");
            stack.className = "toast-stack";
            document.body.appendChild(stack);
        }

        const toast = document.createElement("div");
        toast.className = "toast";
        toast.dataset.tone = tone;
        toast.textContent = message;
        stack.appendChild(toast);

        window.setTimeout(() => {
            toast.remove();
        }, 3600);
    }

    function protectPage({ redirect = "/login.html" } = {}) {
        if (!getToken()) {
            window.location.href = redirect;
        }
    }

    function requireRole(allowedRoles, options = {}) {
        protectPage({ redirect: options.redirect || "/login.html" });

        const allowed = allowedRoles.map(normalizeRole);
        if (!allowed.includes(getRole())) {
            if (options.message) {
                notify(options.message, "error");
            }
            window.location.href = options.redirect || "/dashboard.html";
            return false;
        }

        return true;
    }

    function wireLogout(selector = "[data-action='logout']") {
        document.querySelectorAll(selector).forEach((node) => {
            node.addEventListener("click", () => {
                clearSession();
                window.location.href = "/login.html";
            });
        });
    }

    function splitList(value) {
        return value
            .split(",")
            .map((item) => item.trim())
            .filter(Boolean);
    }

    function formatList(list = []) {
        return Array.isArray(list) ? list.join(", ") : "";
    }

    function dashboardPathForRole() {
        return "/dashboard.html";
    }

    return {
        endpoints,
        request,
        notify,
        protectPage,
        requireRole,
        storeSession,
        clearSession,
        getUser,
        getRole,
        getRoleLabel,
        getToken,
        wireLogout,
        splitList,
        formatList,
        dashboardPathForRole
    };
})();
