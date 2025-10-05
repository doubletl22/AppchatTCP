package client.view;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;

import client.model.Client;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class WellcomeShell {
	private Client client;
	private Shell shell;
	private Text txtUsername;
	private Text txtPassword;
	private Label lblMessage;
	private Button btnSignIn;
	private Button btnSignUp;

	public WellcomeShell() {
		client = new Client();
		open();
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = new Display();
		createContent();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	protected void createContent() {
		shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("Welcome to TCP Chat Application");
		createComposites();
		registerControlListeners();
	}

	private void createComposites() {
		Composite logInComposite = new Composite(shell, SWT.NONE);
		logInComposite.setBounds(98, 70, 277, 130);

		txtUsername = new Text(logInComposite, SWT.BORDER);
		txtUsername.setBounds(84, 24, 156, 21);

		txtPassword = new Text(logInComposite, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setBounds(84, 51, 156, 21);

		Label lblUsername = new Label(logInComposite, SWT.NONE);
		lblUsername.setBounds(10, 27, 55, 15);
		lblUsername.setText("Username");

		Label lblPassword = new Label(logInComposite, SWT.NONE);
		lblPassword.setBounds(10, 54, 55, 15);
		lblPassword.setText("Password");

		btnSignIn = new Button(logInComposite, SWT.NONE);
		btnSignIn.setBounds(165, 95, 75, 25);
		btnSignIn.setText("Sign in");

		btnSignUp = new Button(logInComposite, SWT.NONE);
		btnSignUp.setBounds(84, 95, 75, 25);
		btnSignUp.setText("Sign up");

		lblMessage = new Label(logInComposite, SWT.NONE);
		lblMessage.setBounds(0, 74, 277, 15);
		lblMessage.setText("");
	}

	private void registerControlListeners() {
		btnSignIn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tryToSignIn();
			}
		});

		btnSignUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tryToSignUp();
			}
		});

		txtUsername.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				txtPassword.forceFocus();
			}
		});

		txtPassword.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				tryToSignIn();
			}
		});
	}

	private void tryToSignIn() {
		if (!txtUsername.getText().equals("") && !txtPassword.getText().equals("")) {
			String username = txtUsername.getText();
			String password = txtPassword.getText();
			boolean tryLogin = client.signIn(username, password);
			if (tryLogin == false) {
				lblMessage.setText("Wrong username or password! Try again");
			} else {
				shell.dispose();
				ClientShell clientShell = new ClientShell();
				clientShell.setClientSendHandler(client.getClientSendHandler());
				clientShell.setReceiveSendHandler(client.getClientReceiveHandler());
				clientShell.setShellInfo(username, password);
				clientShell.open();

			}
		} else {
			lblMessage.setText("Username and/or password cannot be empty");
		}
	}

	private void tryToSignUp() {
		if (!txtUsername.getText().equals("") && !txtPassword.getText().equals("")) {
			String username = txtUsername.getText();
			String password = txtPassword.getText();
			boolean tryLogin = client.signUp("", username, password);
			if (tryLogin == false) {
				lblMessage.setText("Wrong username or password! Try again");
			} else {
				lblMessage.setText("Registered successfully, you can now sign in");
			}
		} else {
			lblMessage.setText("Username and/or password cannot be empty");
		}
	}

}
