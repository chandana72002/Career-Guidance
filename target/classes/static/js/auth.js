document.addEventListener("DOMContentLoaded", () => {
    CareerAPI.wireLogout();

    const loginForm = document.querySelector("#login-form");
    const registerForm = document.querySelector("#register-form");
    const currentUserName = document.querySelector("[data-user-name]");

    if (currentUserName && CareerAPI.getUser()) {
        currentUserName.textContent = CareerAPI.getUser().fullName || "Returning user";
    }

    if (loginForm) {
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();

            const submitButton = loginForm.querySelector("button[type='submit']");
            submitButton.disabled = true;

            try {
                const payload = {
                    email: loginForm.email.value.trim(),
                    password: loginForm.password.value
                };

                const response = await CareerAPI.request(CareerAPI.endpoints.login, {
                    method: "POST",
                    body: JSON.stringify(payload)
                });

                CareerAPI.storeSession(response);
                CareerAPI.notify("Signed in successfully.");
                window.location.href = CareerAPI.dashboardPathForRole(response.user?.role);
            } catch (error) {
                CareerAPI.notify(error.message || "Unable to sign in.", "error");
            } finally {
                submitButton.disabled = false;
            }
        });
    }

    if (registerForm) {
        registerForm.addEventListener("submit", async (event) => {
            event.preventDefault();

            const submitButton = registerForm.querySelector("button[type='submit']");
            submitButton.disabled = true;

            try {
                const payload = {
                    fullName: registerForm.fullName.value.trim(),
                    email: registerForm.email.value.trim(),
                    password: registerForm.password.value,
                    role: registerForm.role.value
                };

                const response = await CareerAPI.request(CareerAPI.endpoints.register, {
                    method: "POST",
                    body: JSON.stringify(payload)
                });

                CareerAPI.storeSession(response);
                CareerAPI.notify("Account created. Profile setup is ready.");
                window.location.href = CareerAPI.dashboardPathForRole(response.user?.role);
            } catch (error) {
                CareerAPI.notify(error.message || "Unable to create account.", "error");
            } finally {
                submitButton.disabled = false;
            }
        });
    }
});
