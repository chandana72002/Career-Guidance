document.addEventListener("DOMContentLoaded", async () => {
    CareerAPI.protectPage();
    if (!CareerAPI.requireRole(["ROLE_USER"], {
        redirect: "/dashboard.html",
        message: "Assessment is available only for student accounts."
    })) {
        return;
    }
    CareerAPI.wireLogout();

    const questionList = document.querySelector("#question-list");
    const assessmentForm = document.querySelector("#assessment-form");
    const summaryCard = document.querySelector("#assessment-summary");
    let questions = [];

    function renderQuestions(items) {
        questionList.innerHTML = "";

        items.forEach((question, index) => {
            const card = document.createElement("article");
            card.className = "question-card";
            card.innerHTML = `
                <span class="status">${question.category || "Assessment"} · Q${index + 1}</span>
                <h3>${question.prompt}</h3>
                <div class="question-options">
                    ${(question.options || []).map((option) => `
                        <label class="choice">
                            <input type="radio" name="question-${question.id}" value="${option.key}" required>
                            <span>
                                <strong>${option.label}</strong>
                                <div class="muted">${option.description || ""}</div>
                            </span>
                        </label>
                    `).join("")}
                </div>
            `;
            questionList.appendChild(card);
        });
    }

    function renderSummary(result) {
        if (!summaryCard) {
            return;
        }

        const traits = result.traits || {};
        summaryCard.innerHTML = `
            <h3>Assessment snapshot</h3>
            <p>${result.summary || "Your strongest traits are shaping the recommendation engine."}</p>
            <div class="metric-list">
                ${Object.entries(traits).map(([name, score]) => `
                    <div>
                        <div class="meta">${name}</div>
                        <div class="score-track"><div class="score-fill" style="width:${score}%"></div></div>
                    </div>
                `).join("")}
            </div>
        `;
    }

    async function loadQuestions() {
        try {
            questions = await CareerAPI.request(CareerAPI.endpoints.assessmentQuestions);
            renderQuestions(questions);
        } catch (error) {
            questionList.innerHTML = `
                <div class="empty-card">
                    <h3>Questions unavailable</h3>
                    <p>${error.message || "The backend has not seeded assessment questions yet."}</p>
                </div>
            `;
        }
    }

    assessmentForm?.addEventListener("submit", async (event) => {
        event.preventDefault();

        const submitButton = assessmentForm.querySelector("button[type='submit']");
        submitButton.disabled = true;

        try {
            const answers = questions.map((question) => ({
                questionId: question.id,
                selectedOption: assessmentForm.querySelector(`input[name='question-${question.id}']:checked`)?.value
            }));

            if (answers.some((answer) => !answer.selectedOption)) {
                CareerAPI.notify("Answer every question before submitting.", "error");
                submitButton.disabled = false;
                return;
            }

            const result = await CareerAPI.request(CareerAPI.endpoints.assessmentSubmit, {
                method: "POST",
                body: JSON.stringify({ answers })
            });

            renderSummary(result);
            CareerAPI.notify("Assessment submitted. Recommendation signals updated.");
        } catch (error) {
            CareerAPI.notify(error.message || "Unable to submit assessment.", "error");
        } finally {
            submitButton.disabled = false;
        }
    });

    await loadQuestions();
});
