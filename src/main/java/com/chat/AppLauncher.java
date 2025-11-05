package com.chat;

import com.chat.core.ChatClientCore;
import com.chat.model.ClientViewModel;
import com.chat.ui.client.ClientController;
import com.chat.ui.client.ClientView;
import com.chat.util.UiUtils;

import javax.swing.*;

public class AppLauncher {
    public static void main(String[] args) {
        UiUtils.setupLookAndFeel();

        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            startServer();
        } else {
            startClient();
        }
    }

    private static void startClient() {
        UiUtils.invokeLater(() -> {
            // 1. Khởi tạo Model và Core
            ClientViewModel viewModel = new ClientViewModel();
            ChatClientCore core = new ChatClientCore(null);

            // 2. Khởi tạo View (Chưa có Controller)
            ClientView view = new ClientView(viewModel);

            // 3. Khởi tạo Controller (Kết nối View, Core, Model)
            ClientController controller = new ClientController(view, core, viewModel);

            // 4. Inject dependency vòng tròn (Controller hoàn thiện)
            core.setListener(controller);
            view.setController(controller);

            view.setVisible(true);
        });
    }

    private static void startServer() {
        UiUtils.invokeLater(() -> {
            // (Server startup logic goes here, following MVP)
            // ServerView view = new ServerView(...);
            // ServerController controller = new new ServerController(view, core, model);
            // view.setVisible(true);
            JFrame frame = new JFrame("Server UI (Not Refactored)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
            frame.add(new JLabel("Server UI loading...", SwingConstants.CENTER));
            frame.setVisible(true);
        });
    }
}