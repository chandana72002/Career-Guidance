document.addEventListener("DOMContentLoaded", async () => {
    CareerAPI.protectPage();
    CareerAPI.wireLogout();

    const user = CareerAPI.getUser() || {};
    const role = CareerAPI.getRole();

    const dashboardNav = document.querySelector("#dashboard-nav");
    const sidebarCopy = document.querySelector("#sidebar-copy");
    const sidebarNote = document.querySelector("#sidebar-note");
    const hero = document.querySelector("#dashboard-hero");
    const metrics = document.querySelector("#dashboard-metrics");
    const primary = document.querySelector("#dashboard-primary");
    const secondary = document.querySelector("#dashboard-secondary");

    const EDUCATION_LEVELS = [
        ["HIGH_SCHOOL", "High school"],
        ["DIPLOMA", "Diploma"],
        ["UNDERGRADUATE", "Undergraduate"],
        ["POSTGRADUATE", "Postgraduate"]
    ];
    const PERSONALITY_TYPES = [
        ["ANALYTICAL", "Analytical"],
        ["CREATIVE", "Creative"],
        ["SOCIAL", "Social"],
        ["LEADERSHIP", "Leadership"],
        ["STRUCTURED", "Structured"]
    ];

    const state = {
        profile: null,
        recommendations: [],
        careers: [],
        skills: [],
        adminOverview: null,
        editingCareerId: null
    };

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function formatDate(value) {
        if (!value) {
            return "Recently";
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "Recently";
        }

        return new Intl.DateTimeFormat(undefined, {
            day: "numeric",
            month: "short",
            year: "numeric"
        }).format(date);
    }

    function capitalizeWords(value) {
        return String(value || "")
            .toLowerCase()
            .split(/[_\s]+/)
            .filter(Boolean)
            .map((item) => item.charAt(0).toUpperCase() + item.slice(1))
            .join(" ");
    }

    function formatMetricValue(value) {
        return typeof value === "number" ? value.toLocaleString() : value;
    }

    function renderNav(items) {
        dashboardNav.innerHTML = items.map((item) => {
            if (item.type === "button") {
                return `<button data-action="${item.action}" type="button">${item.label}</button>`;
            }
            return `<a class="${item.active ? "is-active" : ""}" href="${item.href}">${item.label}</a>`;
        }).join("");

        CareerAPI.wireLogout();
    }

    function renderSidebar(content) {
        sidebarCopy.textContent = content.copy;
        sidebarNote.innerHTML = `
            <span class="status">${content.noteLabel}</span>
            <p>${content.note}</p>
        `;
        renderNav(content.nav);
    }

    function renderHero(config) {
        const actions = (config.actions || []).map((action) => {
            if (action.type === "button") {
                return `<button class="${action.tone || "btn"}" id="${action.id}" type="button">${action.label}</button>`;
            }
            return `<a class="${action.tone || "btn"}" href="${action.href}">${action.label}</a>`;
        }).join("");

        hero.innerHTML = `
            <div>
                <span class="eyebrow">${config.eyebrow}</span>
                <h1>${config.title}</h1>
                <p>${config.description}</p>
            </div>
            <div class="inline-actions">${actions}</div>
        `;
    }

    function renderMetrics(items) {
        metrics.innerHTML = items.map((item) => `
            <article class="metric-card">
                <small>${escapeHtml(item.label)}</small>
                <div class="metric-number">${escapeHtml(formatMetricValue(item.value))}</div>
                <p>${escapeHtml(item.detail)}</p>
            </article>
        `).join("");
    }

    function renderStudentShell() {
        renderSidebar({
            copy: "Student workspace for profile building, assessments, and explainable recommendations.",
            noteLabel: "Student mode",
            note: "Complete your profile deeply before generating fresh recommendations.",
            nav: [
                { href: "#student-profile", label: "Profile dashboard", active: true },
                { href: "/assessment.html", label: "Assessment" },
                { href: "/recommendations.html", label: "Recommendations" },
                { type: "button", label: "Logout", action: "logout" }
            ]
        });

        renderHero({
            eyebrow: `${CareerAPI.getRoleLabel(role)} workspace`,
            title: `${escapeHtml(user.fullName || "Student")}, build a stronger recommendation signal.`,
            description: "Your dashboard updates in real time as you refine the profile, complete the assessment, and review the latest ranked career paths.",
            actions: [
                { href: "/assessment.html", label: "Take assessment", tone: "btn-soft" },
                { href: "/recommendations.html", label: "View recommendations", tone: "btn" }
            ]
        });

        metrics.innerHTML = `
            <article class="metric-card">
                <small>Profile completion</small>
                <div class="metric-number" id="profile-progress">0%</div>
                <p>Based on the signals the recommendation engine can actually use.</p>
            </article>
            <article class="metric-card">
                <small>Latest assessment</small>
                <div class="metric-number" id="latest-assessment">Pending</div>
                <p>The most recent trait signal driving compatibility scoring.</p>
            </article>
            <article class="metric-card">
                <small>Recommendations</small>
                <div class="metric-number" id="recommendation-count">0</div>
                <p>Career paths currently ranked for this profile.</p>
            </article>
            <article class="metric-card">
                <small>Saved paths</small>
                <div class="metric-number" id="saved-career-count">0</div>
                <p>Recommendations marked for follow-up and deeper review.</p>
            </article>
        `;

        primary.innerHTML = `
            <section class="form-card" id="student-profile">
                <h2>Profile data</h2>
                <p class="muted">These details feed directly into career compatibility, skill-gap analysis, and roadmap quality.</p>
                <form id="profile-form" class="field-grid form-stack form-stack-tight">
                    <div class="field full">
                        <label for="fullName">Full name</label>
                        <input id="fullName" name="fullName" type="text" placeholder="Your name">
                    </div>
                    <div class="field">
                        <label for="age">Age</label>
                        <input id="age" name="age" type="number" min="15" max="60" placeholder="21">
                    </div>
                    <div class="field">
                        <label for="educationLevel">Education level</label>
                        <select id="educationLevel" name="educationLevel">
                            <option value="">Select</option>
                            ${EDUCATION_LEVELS.map(([value, label]) => `<option value="${value}">${label}</option>`).join("")}
                        </select>
                    </div>
                    <div class="field">
                        <label for="course">Course / branch</label>
                        <input id="course" name="course" type="text" placeholder="B.Tech CSE">
                    </div>
                    <div class="field">
                        <label for="currentYear">Current year / semester</label>
                        <input id="currentYear" name="currentYear" type="text" placeholder="3rd year">
                    </div>
                    <div class="field">
                        <label for="cgpa">CGPA / marks</label>
                        <input id="cgpa" name="cgpa" type="number" min="0" max="10" step="0.01" placeholder="8.3">
                    </div>
                    <div class="field">
                        <label for="personalityType">Personality type</label>
                        <select id="personalityType" name="personalityType">
                            <option value="">Select</option>
                            ${PERSONALITY_TYPES.map(([value, label]) => `<option value="${value}">${label}</option>`).join("")}
                        </select>
                    </div>
                    <div class="field">
                        <label for="preferredWorkType">Preferred work type</label>
                        <select id="preferredWorkType" name="preferredWorkType">
                            <option value="">Select</option>
                            <option value="REMOTE">Remote</option>
                            <option value="HYBRID">Hybrid</option>
                            <option value="ONSITE">On-site</option>
                            <option value="FLEXIBLE">Flexible</option>
                        </select>
                    </div>
                    <div class="field full">
                        <label for="preferredIndustry">Preferred industry</label>
                        <input id="preferredIndustry" name="preferredIndustry" type="text" placeholder="Fintech, healthcare, cloud, consulting">
                    </div>
                    <div class="field full">
                        <label for="interests">Interests</label>
                        <input id="interests" name="interests" type="text" placeholder="Data visualization, coding, solving puzzles">
                    </div>
                    <div class="field full">
                        <label for="skills">Skills</label>
                        <input id="skills" name="skills" type="text" placeholder="Java, SQL, communication, Figma">
                    </div>
                    <div class="field full">
                        <label for="strengths">Strengths</label>
                        <textarea id="strengths" name="strengths" placeholder="Describe what you are naturally good at."></textarea>
                    </div>
                    <div class="field full">
                        <label for="weaknesses">Weaknesses</label>
                        <textarea id="weaknesses" name="weaknesses" placeholder="Describe areas you want to improve."></textarea>
                    </div>
                    <div class="field full">
                        <label for="longTermGoal">Long-term goal</label>
                        <textarea id="longTermGoal" name="longTermGoal" placeholder="Describe the role or impact you want in the long run."></textarea>
                    </div>
                    <div class="field full">
                        <button class="btn" type="submit">Save profile</button>
                    </div>
                </form>
            </section>
            <section class="glass-card" id="student-recommendations-preview">
                <div class="section-head section-head-tight">
                    <div>
                        <h2 class="subsection-title">Top recommendation preview</h2>
                        <p class="muted">The strongest matches from your latest recommendation set.</p>
                    </div>
                    <a class="btn-ghost" href="/recommendations.html">Open full list</a>
                </div>
                <div class="rec-list form-stack-tight" id="dashboard-recommendations"></div>
            </section>
        `;

        secondary.innerHTML = `
            <section class="glass-card">
                <h3>Current skill signals</h3>
                <p class="muted">Skills in this list directly affect recommendation quality and missing-skill analysis.</p>
                <div class="tag-list form-stack-tight" id="profile-skill-tags"></div>
            </section>
            <section class="glass-card">
                <h3>What to do next</h3>
                <div class="data-list">
                    <article class="data-row">
                        <strong>1. Strengthen the profile</strong>
                        <span class="muted">Include industry preference, strengths, and high-signal skills.</span>
                    </article>
                    <article class="data-row">
                        <strong>2. Complete the assessment</strong>
                        <span class="muted">Trait signals improve ranking confidence and role fit explanations.</span>
                    </article>
                    <article class="data-row">
                        <strong>3. Review saved paths</strong>
                        <span class="muted">Use saved recommendations to compare skill gaps and roadmap quality.</span>
                    </article>
                </div>
            </section>
        `;
    }

    function setStudentProfileMetrics(profile) {
        const profileProgress = document.querySelector("#profile-progress");
        if (typeof profile?.profileCompletion === "number" && profileProgress) {
            profileProgress.textContent = `${profile.profileCompletion}%`;
            return;
        }

        const fields = [
            profile?.educationLevel,
            profile?.course,
            profile?.cgpa,
            profile?.personalityType,
            ...(profile?.interests || []),
            ...(profile?.skills || [])
        ].filter(Boolean);
        const progress = Math.min(100, Math.round((fields.length / 10) * 100));
        if (profileProgress) {
            profileProgress.textContent = `${progress}%`;
        }
    }

    function renderStudentTags(items) {
        const savedSkills = document.querySelector("#profile-skill-tags");
        if (!savedSkills) {
            return;
        }

        savedSkills.innerHTML = "";
        (items || []).forEach((item) => {
            const tag = document.createElement("span");
            tag.className = "tag";
            tag.textContent = item;
            savedSkills.appendChild(tag);
        });

        if (!savedSkills.children.length) {
            savedSkills.innerHTML = "<span class='muted'>Add skills in your profile to improve match quality.</span>";
        }
    }

    function fillStudentProfile(profile) {
        const profileForm = document.querySelector("#profile-form");
        if (!profileForm || !profile) {
            return;
        }

        profileForm.fullName.value = profile.fullName || user.fullName || "";
        profileForm.age.value = profile.age || "";
        profileForm.educationLevel.value = profile.educationLevel || "";
        profileForm.course.value = profile.course || "";
        profileForm.currentYear.value = profile.currentYear || "";
        profileForm.cgpa.value = profile.cgpa || "";
        profileForm.preferredWorkType.value = profile.preferredWorkType || "";
        profileForm.preferredIndustry.value = profile.preferredIndustry || "";
        profileForm.personalityType.value = profile.personalityType || "";
        profileForm.interests.value = CareerAPI.formatList(profile.interests);
        profileForm.skills.value = CareerAPI.formatList(profile.skills);
        profileForm.strengths.value = profile.strengths || "";
        profileForm.weaknesses.value = profile.weaknesses || "";
        profileForm.longTermGoal.value = profile.longTermGoal || "";

        renderStudentTags(profile.skills);
        setStudentProfileMetrics(profile);
    }

    function renderStudentRecommendations(recommendations) {
        const recommendationContainer = document.querySelector("#dashboard-recommendations");
        if (!recommendationContainer) {
            return;
        }

        recommendationContainer.innerHTML = "";
        if (!recommendations.length) {
            recommendationContainer.innerHTML = `
                <div class="empty-card">
                    <h3>No recommendations yet</h3>
                    <p>Complete your profile, take the assessment, then generate your first set of ranked career matches.</p>
                </div>
            `;
            return;
        }

        recommendations.slice(0, 3).forEach((item) => {
            const card = document.createElement("article");
            card.className = "rec-card";
            card.innerHTML = `
                <div class="compatibility">
                    <div class="compatibility-badge">${Math.round(item.compatibilityScore || 0)}%</div>
                    <div>
                        <h3>${escapeHtml(item.careerName || "Career")}</h3>
                        <p>${escapeHtml(item.explanation || "A strong fit based on your latest profile and assessment signals.")}</p>
                    </div>
                </div>
                <div class="tag-list">
                    ${(item.matchedSkills || []).slice(0, 4).map((skill) => `<span class="tag">${escapeHtml(skill)}</span>`).join("")}
                </div>
            `;
            recommendationContainer.appendChild(card);
        });
    }

    async function initializeStudentDashboard() {
        renderStudentShell();

        const latestAssessment = document.querySelector("#latest-assessment");
        const recommendationCount = document.querySelector("#recommendation-count");
        const savedCareerCount = document.querySelector("#saved-career-count");
        const profileForm = document.querySelector("#profile-form");

        async function loadProfile() {
            try {
                const profile = await CareerAPI.request(CareerAPI.endpoints.profile);
                state.profile = profile;
                fillStudentProfile(profile);
            } catch (error) {
                if (error.status !== 404) {
                    CareerAPI.notify(error.message || "Unable to load profile.", "error");
                }
            }
        }

        async function loadAssessment() {
            try {
                const assessment = await CareerAPI.request(CareerAPI.endpoints.latestAssessment);
                latestAssessment.textContent = `${assessment.overallScore || 0}%`;
            } catch (error) {
                latestAssessment.textContent = "Pending";
            }
        }

        async function loadRecommendations() {
            try {
                const recommendations = await CareerAPI.request(CareerAPI.endpoints.recommendations);
                state.recommendations = recommendations || [];
                recommendationCount.textContent = state.recommendations.length;
                savedCareerCount.textContent = state.recommendations.filter((item) => item.saved === true).length;
                renderStudentRecommendations(state.recommendations);
            } catch (error) {
                recommendationCount.textContent = "0";
                savedCareerCount.textContent = "0";
                renderStudentRecommendations([]);
            }
        }

        profileForm?.addEventListener("submit", async (event) => {
            event.preventDefault();

            const submitButton = profileForm.querySelector("button[type='submit']");
            submitButton.disabled = true;

            try {
                const payload = {
                    fullName: profileForm.fullName.value.trim(),
                    age: Number(profileForm.age.value) || null,
                    educationLevel: profileForm.educationLevel.value || null,
                    course: profileForm.course.value.trim(),
                    currentYear: profileForm.currentYear.value.trim(),
                    cgpa: profileForm.cgpa.value ? Number(profileForm.cgpa.value) : null,
                    preferredWorkType: profileForm.preferredWorkType.value || null,
                    preferredIndustry: profileForm.preferredIndustry.value.trim(),
                    personalityType: profileForm.personalityType.value || null,
                    interests: CareerAPI.splitList(profileForm.interests.value),
                    skills: CareerAPI.splitList(profileForm.skills.value),
                    strengths: profileForm.strengths.value.trim(),
                    weaknesses: profileForm.weaknesses.value.trim(),
                    longTermGoal: profileForm.longTermGoal.value.trim()
                };

                const response = await CareerAPI.request(CareerAPI.endpoints.profile, {
                    method: "PUT",
                    body: JSON.stringify(payload)
                });

                state.profile = response;
                fillStudentProfile(response);
                CareerAPI.notify("Profile saved.");
            } catch (error) {
                CareerAPI.notify(error.message || "Unable to save profile.", "error");
            } finally {
                submitButton.disabled = false;
            }
        });

        await Promise.all([loadProfile(), loadAssessment(), loadRecommendations()]);
    }

    function renderSimpleList(title, items, emptyMessage) {
        if (!items.length) {
            return `
                <section class="glass-card">
                    <h3>${title}</h3>
                    <div class="empty-card compact-empty">
                        <p>${emptyMessage}</p>
                    </div>
                </section>
            `;
        }

        return `
            <section class="glass-card">
                <h3>${title}</h3>
                <div class="data-list form-stack-tight">
                    ${items.map((item) => `
                        <article class="data-row">
                            <strong>${escapeHtml(item.label)}</strong>
                            <span>${escapeHtml(String(item.value))}</span>
                            <small class="muted">${escapeHtml(item.detail)}</small>
                        </article>
                    `).join("")}
                </div>
            </section>
        `;
    }

    async function initializeCounselorDashboard() {
        renderSidebar({
            copy: "Counselor view for spotting learner readiness, common gaps, and the strongest emerging domains.",
            noteLabel: "Counselor mode",
            note: "Prioritize students who have strong profile signals but still lack assessment-backed recommendations.",
            nav: [
                { href: "#learner-board", label: "Learner board", active: true },
                { href: "#signal-board", label: "Signals" },
                { type: "button", label: "Logout", action: "logout" }
            ]
        });

        renderHero({
            eyebrow: "Counselor workspace",
            title: `${escapeHtml(user.fullName || "Counselor")}, track learner readiness at a glance.`,
            description: "This view focuses on who needs guidance now, where recommendation trends are forming, and which recurring gaps are blocking student progress."
        });

        try {
            const overview = await CareerAPI.request(CareerAPI.endpoints.counselorOverview);
            renderMetrics(overview.metrics || []);

            primary.innerHTML = `
                <section class="glass-card" id="learner-board">
                    <div class="section-head section-head-tight">
                        <div>
                            <h2 class="subsection-title">Learner board</h2>
                            <p class="muted">Recent student snapshots sorted by newest account creation.</p>
                        </div>
                    </div>
                    <div class="record-grid form-stack-tight">
                        ${(overview.learners || []).map((learner) => `
                            <article class="record-card">
                                <div class="record-head">
                                    <div>
                                        <h3>${escapeHtml(learner.name)}</h3>
                                        <p class="muted">${escapeHtml(learner.email)}</p>
                                    </div>
                                    <span class="status">${escapeHtml(String(learner.profileCompletion || 0))}% complete</span>
                                </div>
                                <div class="data-list compact-list">
                                    <article class="data-row">
                                        <strong>Course</strong>
                                        <span>${escapeHtml(learner.course || learner.educationLevel || "Profile not detailed yet")}</span>
                                    </article>
                                    <article class="data-row">
                                        <strong>Latest assessment</strong>
                                        <span>${escapeHtml(learner.latestAssessment)}</span>
                                    </article>
                                    <article class="data-row">
                                        <strong>Top path</strong>
                                        <span>${escapeHtml(learner.topCareer)}${learner.topScore ? ` · ${escapeHtml(String(learner.topScore))}%` : ""}</span>
                                    </article>
                                    <article class="data-row">
                                        <strong>Saved paths</strong>
                                        <span>${escapeHtml(String(learner.savedPaths || 0))}</span>
                                    </article>
                                </div>
                            </article>
                        `).join("")}
                    </div>
                </section>
                <section class="glass-card">
                    <h3>Suggested counselor actions</h3>
                    <div class="data-list form-stack-tight">
                        ${(overview.actionNotes || []).map((note) => `
                            <article class="data-row">
                                <strong>Guidance prompt</strong>
                                <span class="muted">${escapeHtml(note)}</span>
                            </article>
                        `).join("")}
                    </div>
                </section>
            `;

            secondary.innerHTML = `
                ${renderSimpleList("Career signals", overview.careerSignals || [], "Generate more recommendations to surface domain demand.")}
                <div id="signal-board">
                    ${renderSimpleList("Skill alerts", overview.skillAlerts || [], "Missing-skill patterns will appear after students generate recommendations.")}
                </div>
            `;
        } catch (error) {
            metrics.innerHTML = "";
            primary.innerHTML = `
                <div class="empty-card">
                    <h3>Counselor overview unavailable</h3>
                    <p>${escapeHtml(error.message || "Unable to load counselor dashboard.")}</p>
                </div>
            `;
            secondary.innerHTML = "";
        }
    }

    function buildAdminCareerForm() {
        return `
            <section class="form-card" id="career-lab">
                <div class="section-head section-head-tight">
                    <div>
                        <h2 class="subsection-title">Career studio</h2>
                        <p class="muted">Create, refine, and remove career tracks without leaving the admin workspace.</p>
                    </div>
                    <button class="btn-ghost" id="career-form-reset" type="button">Clear form</button>
                </div>
                <form id="career-form" class="field-grid form-stack">
                    <div class="field">
                        <label for="career-name">Career name</label>
                        <input id="career-name" name="name" type="text" placeholder="Cloud Engineer" required>
                    </div>
                    <div class="field">
                        <label for="career-domain">Domain</label>
                        <input id="career-domain" name="domain" type="text" placeholder="Cloud Infrastructure" required>
                    </div>
                    <div class="field full">
                        <label for="career-description">Description</label>
                        <textarea id="career-description" name="description" placeholder="Describe what this role does." required></textarea>
                    </div>
                    <div class="field">
                        <label for="required-education">Required education</label>
                        <select id="required-education" name="requiredEducation" required>
                            <option value="">Select</option>
                            ${EDUCATION_LEVELS.map(([value, label]) => `<option value="${value}">${label}</option>`).join("")}
                        </select>
                    </div>
                    <div class="field">
                        <label for="preferred-personality">Preferred personality</label>
                        <select id="preferred-personality" name="preferredPersonality" required>
                            <option value="">Select</option>
                            ${PERSONALITY_TYPES.map(([value, label]) => `<option value="${value}">${label}</option>`).join("")}
                        </select>
                    </div>
                    <div class="field">
                        <label for="salary-range">Salary range</label>
                        <input id="salary-range" name="salaryRange" type="text" placeholder="5 LPA - 18 LPA" required>
                    </div>
                    <div class="field">
                        <label for="future-scope">Future scope</label>
                        <input id="future-scope" name="futureScope" type="text" placeholder="Why this role matters long-term" required>
                    </div>
                    <div class="field full">
                        <label for="related-industries">Related industries</label>
                        <input id="related-industries" name="relatedIndustries" type="text" placeholder="Fintech, SaaS, Healthcare">
                    </div>
                    <div class="field full">
                        <label for="roadmap-steps">Roadmap steps</label>
                        <textarea id="roadmap-steps" name="roadmapSteps" placeholder="Comma-separated roadmap steps"></textarea>
                    </div>
                    <div class="field full">
                        <label for="recommended-certifications">Recommended certifications</label>
                        <textarea id="recommended-certifications" name="recommendedCertifications" placeholder="Comma-separated certifications"></textarea>
                    </div>
                    <div class="field">
                        <label for="weight-analytical">Analytical weight</label>
                        <input id="weight-analytical" name="analyticalWeight" type="number" min="0" max="100" value="20">
                    </div>
                    <div class="field">
                        <label for="weight-creativity">Creativity weight</label>
                        <input id="weight-creativity" name="creativityWeight" type="number" min="0" max="100" value="10">
                    </div>
                    <div class="field">
                        <label for="weight-leadership">Leadership weight</label>
                        <input id="weight-leadership" name="leadershipWeight" type="number" min="0" max="100" value="10">
                    </div>
                    <div class="field">
                        <label for="weight-technical">Technical weight</label>
                        <input id="weight-technical" name="technicalWeight" type="number" min="0" max="100" value="20">
                    </div>
                    <div class="field">
                        <label for="weight-communication">Communication weight</label>
                        <input id="weight-communication" name="communicationWeight" type="number" min="0" max="100" value="10">
                    </div>
                    <div class="field">
                        <label for="weight-problem">Problem-solving weight</label>
                        <input id="weight-problem" name="problemSolvingWeight" type="number" min="0" max="100" value="20">
                    </div>
                    <div class="field full">
                        <label>Skill mapping</label>
                        <div class="skill-check-grid" id="career-skill-grid"></div>
                    </div>
                    <div class="field full">
                        <button class="btn" type="submit">Save career</button>
                    </div>
                </form>
            </section>
        `;
    }

    function renderSkillGrid(selected = []) {
        const container = document.querySelector("#career-skill-grid");
        if (!container) {
            return;
        }

        const selectedMap = new Map((selected || []).map((item) => [item.skillId, item.importanceWeight]));
        container.innerHTML = state.skills.map((skill) => `
            <label class="skill-option">
                <span class="skill-option-head">
                    <input type="checkbox" data-skill-id="${skill.id}" ${selectedMap.has(skill.id) ? "checked" : ""}>
                    <span>
                        <strong>${escapeHtml(skill.name)}</strong>
                        <small class="muted">${escapeHtml(skill.category || "Skill")}</small>
                    </span>
                </span>
                <input type="number" min="1" max="100" value="${selectedMap.get(skill.id) || 20}" data-weight-for="${skill.id}">
            </label>
        `).join("");
    }

    function buildCareerPayload(form) {
        const selectedSkills = Array.from(form.querySelectorAll("[data-skill-id]:checked")).map((checkbox) => {
            const skillId = Number(checkbox.dataset.skillId);
            const weightInput = form.querySelector(`[data-weight-for="${skillId}"]`);
            return {
                skillId,
                importanceWeight: Number(weightInput?.value || 0)
            };
        });

        return {
            name: form.name.value.trim(),
            domain: form.domain.value.trim(),
            description: form.description.value.trim(),
            requiredEducation: form.requiredEducation.value,
            futureScope: form.futureScope.value.trim(),
            salaryRange: form.salaryRange.value.trim(),
            preferredPersonality: form.preferredPersonality.value,
            analyticalWeight: Number(form.analyticalWeight.value || 0),
            creativityWeight: Number(form.creativityWeight.value || 0),
            leadershipWeight: Number(form.leadershipWeight.value || 0),
            technicalWeight: Number(form.technicalWeight.value || 0),
            communicationWeight: Number(form.communicationWeight.value || 0),
            problemSolvingWeight: Number(form.problemSolvingWeight.value || 0),
            relatedIndustries: CareerAPI.splitList(form.relatedIndustries.value),
            roadmapSteps: CareerAPI.splitList(form.roadmapSteps.value),
            recommendedCertifications: CareerAPI.splitList(form.recommendedCertifications.value),
            skills: selectedSkills
        };
    }

    function resetCareerForm() {
        const form = document.querySelector("#career-form");
        if (!form) {
            return;
        }

        form.reset();
        state.editingCareerId = null;
        renderSkillGrid([]);
        const submitButton = form.querySelector("button[type='submit']");
        if (submitButton) {
            submitButton.textContent = "Save career";
        }
    }

    function populateCareerForm(career) {
        const form = document.querySelector("#career-form");
        if (!form || !career) {
            return;
        }

        state.editingCareerId = career.id;
        form.name.value = career.name || "";
        form.domain.value = career.domain || "";
        form.description.value = career.description || "";
        form.requiredEducation.value = career.requiredEducation || "";
        form.futureScope.value = career.futureScope || "";
        form.salaryRange.value = career.salaryRange || "";
        form.preferredPersonality.value = career.preferredPersonality || "";
        form.analyticalWeight.value = career.analyticalWeight || 0;
        form.creativityWeight.value = career.creativityWeight || 0;
        form.leadershipWeight.value = career.leadershipWeight || 0;
        form.technicalWeight.value = career.technicalWeight || 0;
        form.communicationWeight.value = career.communicationWeight || 0;
        form.problemSolvingWeight.value = career.problemSolvingWeight || 0;
        form.relatedIndustries.value = CareerAPI.formatList(career.relatedIndustries);
        form.roadmapSteps.value = CareerAPI.formatList(career.roadmapSteps);
        form.recommendedCertifications.value = CareerAPI.formatList(career.recommendedCertifications);

        renderSkillGrid((career.skills || []).map((skill) => ({
            skillId: skill.skillId,
            importanceWeight: skill.importanceWeight
        })));

        const submitButton = form.querySelector("button[type='submit']");
        if (submitButton) {
            submitButton.textContent = `Update ${career.name}`;
        }
    }

    function renderAdminCareerCatalog() {
        const catalog = document.querySelector("#career-catalog");
        if (!catalog) {
            return;
        }

        const healthMap = new Map((state.adminOverview?.careerHealth || []).map((item) => [item.id, item]));
        catalog.innerHTML = state.careers.map((career) => {
            const health = healthMap.get(career.id) || {};
            return `
                <article class="record-card">
                    <div class="record-head">
                        <div>
                            <h3>${escapeHtml(career.name)}</h3>
                            <p class="muted">${escapeHtml(career.domain || "Career domain")}</p>
                        </div>
                        <span class="status">${escapeHtml(capitalizeWords(career.requiredEducation || ""))}</span>
                    </div>
                    <div class="tag-list compact-tags">
                        ${(career.relatedIndustries || []).slice(0, 3).map((industry) => `<span class="tag">${escapeHtml(industry)}</span>`).join("")}
                    </div>
                    <div class="data-list compact-list form-stack-tight">
                        <article class="data-row">
                            <strong>Recommendations</strong>
                            <span>${escapeHtml(String(health.recommendationCount || 0))}</span>
                        </article>
                        <article class="data-row">
                            <strong>Saved paths</strong>
                            <span>${escapeHtml(String(health.savedCount || 0))}</span>
                        </article>
                    </div>
                    <div class="panel-actions form-stack-tight">
                        <button class="btn-soft" data-action="edit-career" data-career-id="${career.id}" type="button">Edit</button>
                        <button class="btn-ghost" data-action="delete-career" data-career-id="${career.id}" type="button">Delete</button>
                    </div>
                </article>
            `;
        }).join("");

        catalog.querySelectorAll("[data-action='edit-career']").forEach((button) => {
            button.addEventListener("click", () => {
                const career = state.careers.find((item) => item.id === Number(button.dataset.careerId));
                populateCareerForm(career);
                document.querySelector("#career-lab")?.scrollIntoView({ behavior: "smooth", block: "start" });
            });
        });

        catalog.querySelectorAll("[data-action='delete-career']").forEach((button) => {
            button.addEventListener("click", async () => {
                const careerId = Number(button.dataset.careerId);
                const career = state.careers.find((item) => item.id === careerId);
                if (!career || !window.confirm(`Delete ${career.name}?`)) {
                    return;
                }

                try {
                    await CareerAPI.request(CareerAPI.endpoints.adminCareer(careerId), { method: "DELETE" });
                    CareerAPI.notify(`${career.name} deleted.`);
                    await loadAdminWorkspace();
                    resetCareerForm();
                } catch (error) {
                    CareerAPI.notify(error.message || "Unable to delete career.", "error");
                }
            });
        });
    }

    function renderAdminPanels() {
        primary.innerHTML = `
            ${buildAdminCareerForm()}
            <section class="glass-card">
                <div class="section-head section-head-tight">
                    <div>
                        <h2 class="subsection-title">Career inventory</h2>
                        <p class="muted">All mapped career tracks with recommendation and saved-path signals.</p>
                    </div>
                </div>
                <div class="record-grid form-stack-tight" id="career-catalog"></div>
            </section>
        `;

        secondary.innerHTML = `
            ${renderSimpleList("Role mix", state.adminOverview?.roleMix || [], "Role distribution will appear here.")}
            <section class="glass-card">
                <h3>Recent accounts</h3>
                <div class="data-list form-stack-tight">
                    ${(state.adminOverview?.recentUsers || []).map((account) => `
                        <article class="data-row">
                            <strong>${escapeHtml(account.name)}</strong>
                            <span>${escapeHtml(CareerAPI.getRoleLabel(account.role))} · ${escapeHtml(account.email)}</span>
                            <small class="muted">Joined ${escapeHtml(formatDate(account.createdAt))}</small>
                        </article>
                    `).join("")}
                </div>
            </section>
            ${renderSimpleList("Skill trends", state.adminOverview?.skillTrends || [], "Learner skill trends will appear once profiles are populated.")}
            <section class="glass-card">
                <h3>System notes</h3>
                <div class="data-list form-stack-tight">
                    ${(state.adminOverview?.systemNotes || []).map((note) => `
                        <article class="data-row">
                            <strong>Ops note</strong>
                            <span class="muted">${escapeHtml(note)}</span>
                        </article>
                    `).join("")}
                </div>
            </section>
        `;

        renderSkillGrid([]);
        renderAdminCareerCatalog();

        const form = document.querySelector("#career-form");
        const resetButton = document.querySelector("#career-form-reset");
        resetButton?.addEventListener("click", resetCareerForm);

        form?.addEventListener("submit", async (event) => {
            event.preventDefault();

            const submitButton = form.querySelector("button[type='submit']");
            submitButton.disabled = true;

            try {
                const payload = buildCareerPayload(form);
                const endpoint = state.editingCareerId
                    ? CareerAPI.endpoints.adminCareer(state.editingCareerId)
                    : CareerAPI.endpoints.adminCareers;
                const method = state.editingCareerId ? "PUT" : "POST";

                await CareerAPI.request(endpoint, {
                    method,
                    body: JSON.stringify(payload)
                });

                CareerAPI.notify(state.editingCareerId ? "Career updated." : "Career created.");
                await loadAdminWorkspace();
                resetCareerForm();
            } catch (error) {
                CareerAPI.notify(error.message || "Unable to save career.", "error");
            } finally {
                submitButton.disabled = false;
            }
        });
    }

    async function loadAdminWorkspace() {
        const [overview, careers, skills] = await Promise.all([
            CareerAPI.request(CareerAPI.endpoints.adminOverview),
            CareerAPI.request(CareerAPI.endpoints.careers),
            CareerAPI.request(CareerAPI.endpoints.skills)
        ]);

        state.adminOverview = overview;
        state.careers = careers || [];
        state.skills = skills || [];
        renderMetrics(overview.metrics || []);
        renderAdminPanels();
    }

    async function initializeAdminDashboard() {
        renderSidebar({
            copy: "Admin workspace for managing careers, monitoring adoption, and keeping recommendation data healthy.",
            noteLabel: "Admin mode",
            note: "Use the career studio to update recommendation inventory without leaving the dashboard.",
            nav: [
                { href: "#career-lab", label: "Career studio", active: true },
                { href: "#career-catalog", label: "Inventory" },
                { type: "button", label: "Logout", action: "logout" }
            ]
        });

        renderHero({
            eyebrow: "Admin workspace",
            title: `${escapeHtml(user.fullName || "Admin")}, manage the engine behind every recommendation.`,
            description: "This view is focused on system health, career inventory quality, role mix, and the content that drives counselor and learner experiences."
        });

        try {
            await loadAdminWorkspace();
        } catch (error) {
            metrics.innerHTML = "";
            primary.innerHTML = `
                <div class="empty-card">
                    <h3>Admin workspace unavailable</h3>
                    <p>${escapeHtml(error.message || "Unable to load admin tools.")}</p>
                </div>
            `;
            secondary.innerHTML = "";
        }
    }

    if (role === "ROLE_COUNSELOR") {
        await initializeCounselorDashboard();
        return;
    }

    if (role === "ROLE_ADMIN") {
        await initializeAdminDashboard();
        return;
    }

    await initializeStudentDashboard();
});
