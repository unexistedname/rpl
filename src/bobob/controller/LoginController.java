package bobob.controller;

import bobob.view.LoginView;

public class LoginController {

    private LoginView view;

    public void start() {
        view = new LoginView(this);
        view.setVisible(true);
    }

    public void login(String username, String password) {
        if ("admin".equals(username) && "123".equals(password)) {
            view.showInfo("Login berhasil!");
            view.dispose();
            new DashboardController().start();
        } else {
            view.showError("Username atau password salah!");
        }
    }
}
