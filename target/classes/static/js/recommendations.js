document.addEventListener("DOMContentLoaded", async () => {
    CareerAPI.protectPage();
    if (!CareerAPI.requireRole(["ROLE_USER"], {
        redirect: "/dashboard.html",
        message: "Recommendations are available only for student accounts."
    })) {
        return;
    }
    CareerAPI.wireLogout();

    const list = document.querySelector("#recommendation-list");
    const resourcePanel = document.querySelector("#resource-panel");
    const summaryBand = document.querySelector("#recommendation-summary");
    const generateButton = document.querySelector("#generate-button");

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function getExternalLink(url) {
        if (!url || typeof url !== "string") {
            return "";
        }

        return /^https?:\/\//i.test(url.trim()) ? url.trim() : "";
    }

    function getResourceSource(url) {
        const href = getExternalLink(url);
        if (!href) {
            return "Platform resource";
        }

        try {
            return new URL(href).hostname.replace(/^www\./i, "");
        } catch (error) {
            return "External resource";
        }
    }

    function renderSummary(items) {
        if (!summaryBand) {
            return;
        }

        const topScore = items.length ? Math.round(items[0].compatibilityScore || 0) : 0;
        summaryBand.innerHTML = `
            <div class="score-value">${topScore}%</div>
            <div class="meta">Top compatibility score</div>
            <div class="score-track"><div class="score-fill" style="width:${topScore}%"></div></div>
        `;
    }

    function renderResources(item) {
        if (!resourcePanel) {
            return;
        }

        const resources = item.learningResources || [];
        const roadmap = item.roadmapSteps || [];
        resourcePanel.innerHTML = `
            <div class="roadmap-card">
                <h3>${escapeHtml(item.careerName || item.career?.name || "Career")} roadmap</h3>
                <ol>
                    ${roadmap.length
                        ? roadmap.map((step) => `<li>${escapeHtml(step)}</li>`).join("")
                        : "<li>Backend roadmap data is not available yet.</li>"}
                </ol>
            </div>
            <div class="glass-card">
                <div class="section-head section-head-tight">
                    <div>
                        <h3>Suggested resources</h3>
                        <p class="muted">Focused references, practice labs, and certifications tied to this path.</p>
                    </div>
                    <span class="status">${resources.length} ${resources.length === 1 ? "item" : "items"}</span>
                </div>
                <div class="resource-grid">
                    ${resources.length
                        ? resources.map((resource) => `
                            <article class="resource-item">
                                <div class="resource-item-head">
                                    <span class="status">${escapeHtml(resource.type || "Resource")}</span>
                                    ${getExternalLink(resource.url)
                                        ? `<a class="resource-link" href="${escapeHtml(getExternalLink(resource.url))}" target="_blank" rel="noopener noreferrer">Open</a>`
                                        : ""}
                                </div>
                                <h4>${escapeHtml(resource.title || "Learning resource")}</h4>
                                <p>${escapeHtml(resource.description || resource.url || "Resource details unavailable.")}</p>
                                <span class="resource-source">${escapeHtml(getResourceSource(resource.url))}</span>
                            </article>
                        `).join("")
                        : "<p class='muted'>Resources will appear here when the backend provides them.</p>"}
                </div>
            </div>
        `;
    }

    function recommendationCard(item) {
        const matched = (item.matchedSkills || []).slice(0, 4).map((skill) => `<span class="tag">${escapeHtml(skill)}</span>`).join("");
        const missing = (item.missingSkills || []).slice(0, 4).map((skill) => `<span class="tag">${escapeHtml(skill)}</span>`).join("");

        return `
            <article class="rec-card" data-recommendation-id="${item.id}">
                <div class="compatibility">
                    <div class="compatibility-badge">${Math.round(item.compatibilityScore || 0)}%</div>
                    <div>
                        <h3>${escapeHtml(item.careerName || item.career?.name || "Career suggestion")}</h3>
                        <p>${escapeHtml(item.explanation || "This recommendation reflects your current profile, interests, and assessment results.")}</p>
                    </div>
                </div>
                <div class="pill-row">
                    ${matched || "<span class='muted'>Matched skills will appear here.</span>"}
                </div>
                <p class="meta meta-spaced">Missing skills</p>
                <div class="pill-row">${missing || "<span class='muted'>No missing skills identified yet.</span>"}</div>
                <div class="panel-actions form-stack-tight">
                    <button class="btn-soft" data-action="view-details" type="button">View roadmap</button>
                    <button class="btn-ghost" data-action="save-career" data-career-id="${item.careerId || item.career?.id || ""}" type="button">Save path</button>
                </div>
            </article>
        `;
    }

    function attachRecommendationActions(items) {
        document.querySelectorAll("[data-action='view-details']").forEach((button, index) => {
            button.addEventListener("click", () => renderResources(items[index]));
        });

        document.querySelectorAll("[data-action='save-career']").forEach((button, index) => {
            button.addEventListener("click", async () => {
                const careerId = button.dataset.careerId;
                if (!careerId) {
                    CareerAPI.notify("Career id is missing from the response.", "error");
                    return;
                }

                try {
                    await CareerAPI.request(CareerAPI.endpoints.saveCareer(careerId), { method: "POST" });
                    CareerAPI.notify(`Saved ${items[index].careerName || "career path"}.`);
                } catch (error) {
                    CareerAPI.notify(error.message || "Unable to save career.", "error");
                }
            });
        });
    }

    function renderRecommendations(items) {
        list.innerHTML = "";

        if (!items.length) {
            list.innerHTML = `
                <div class="empty-card">
                    <h3>No recommendations available</h3>
                    <p>Generate recommendations after completing your profile and assessment.</p>
                </div>
            `;
            renderResources({});
            renderSummary([]);
            return;
        }

        list.innerHTML = items.map(recommendationCard).join("");
        renderResources(items[0]);
        renderSummary(items);
        attachRecommendationActions(items);
    }

    async function loadRecommendations() {
        try {
            const items = await CareerAPI.request(CareerAPI.endpoints.recommendations);
            renderRecommendations(items || []);
        } catch (error) {
            renderRecommendations([]);
        }
    }

    generateButton?.addEventListener("click", async () => {
        generateButton.disabled = true;

        try {
            const items = await CareerAPI.request(CareerAPI.endpoints.generateRecommendations, { method: "POST" });
            renderRecommendations(items || []);
            CareerAPI.notify("Recommendations refreshed.");
        } catch (error) {
            CareerAPI.notify(error.message || "Unable to generate recommendations.", "error");
        } finally {
            generateButton.disabled = false;
        }
    });

    await loadRecommendations();
});
