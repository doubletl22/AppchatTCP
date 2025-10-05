package client.view;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;

import client.model.ClientReceiver;
import client.model.ClientSender;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.FileDialog;

public class ClientShell {
	private Display display;
	private Shell shell;
	private ClientSender clientSendHandler;
	private ClientReceiver clientReceiveHandler;
	private Button btnStartConversation;
	private Composite composite_3;
	private List lstOnlineUsers;
	private Label lblErrorMessage;
	private Text txtChatBox;
	private Label currentCounterParty;
	private List lstChatHistory;
	private Button btnLoadOnlineUsers;
	private String name;
	private ScrolledComposite scrolledComposite;
	private Composite compositeChat;
	private TabItem tbtmInfo;
	private Composite composite_Info;
	private Label label_Avatar;
	private Button btnUpdateAvatar;
	private Label lblPersonalInformation;
	private Label lblUsername;
	private Text text_username;
	private Label lblPassword;
	private Text text_password;
	private String pw;
	private Image avatar;
	private TabItem tbtmBlog;
	private Composite compositeBlog;
	private Text txtEditor;
	private Button btnBlogPost;
	private Button btnViewContent;
	private Text txtTitle;
	private Label lblMyTitle;
	private Button btnLoadPosts;
	private Label lblBlogTitle;
	private Label lblBlogContent;
	private List lstBlogTitle;
	private ArrayList<String> blogPosts = new ArrayList<>();
	private Button btnDisplayPassword;

	public ClientShell() {
	}

	public ClientShell(ClientSender clientSendHandler, ClientReceiver clientReceiveHandler) {
		this.clientSendHandler = clientSendHandler;
		this.clientReceiveHandler = clientReceiveHandler;
		open();
	}

	public void setClientSendHandler(ClientSender clientSendHandler) {
		this.clientSendHandler = clientSendHandler;
		this.clientSendHandler.setClientShell(this);
	}

	public void setReceiveSendHandler(ClientReceiver setReceiveSendHandler) {
		this.clientReceiveHandler = setReceiveSendHandler;
		this.clientReceiveHandler.setClientShell(this);
	}

	public void setShellInfo(String username, String pass) {
		name = username;
		pw = pass;
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ClientShell window = new ClientShell();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		display = Display.getDefault();
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
		shell.setSize(523, 333);
		shell.setText("TCP Chat App - " + name);
		createComposites();
		registerControlListeners();
	}

	private void createComposites() {

		TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setBounds(0, 0, 511, 298);

		tbtmInfo = new TabItem(tabFolder, SWT.NONE);
		tbtmInfo.setText("Personal Info");

		composite_Info = new Composite(tabFolder, SWT.NONE);
		composite_Info.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		tbtmInfo.setControl(composite_Info);

		label_Avatar = new Label(composite_Info, SWT.NONE);
		label_Avatar.setBounds(24, 24, 128, 128);
		clientSendHandler.sendMessage("Get avatar", name);

		btnUpdateAvatar = new Button(composite_Info, SWT.NONE);
		
		btnUpdateAvatar.setBounds(40, 170, 96, 25);
		btnUpdateAvatar.setText("Update Avatar...");

		lblPersonalInformation = new Label(composite_Info, SWT.NONE);
		lblPersonalInformation.setAlignment(SWT.CENTER);
		lblPersonalInformation.setText("Personal Information");
		lblPersonalInformation.setFont(SWTResourceManager.getFont("Century Gothic", 20, SWT.NORMAL));
		lblPersonalInformation.setBounds(190, 10, 303, 42);

		lblUsername = new Label(composite_Info, SWT.NONE);
		lblUsername.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		lblUsername.setAlignment(SWT.CENTER);
		lblUsername.setBounds(190, 78, 71, 25);
		lblUsername.setText("Username");

		text_username = new Text(composite_Info, SWT.BORDER);
		text_username.setEditable(false);
		text_username.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		text_username.setBounds(267, 75, 226, 25);
		text_username.setText(name);

		lblPassword = new Label(composite_Info, SWT.NONE);
		lblPassword.setText("Password");
		lblPassword.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		lblPassword.setAlignment(SWT.CENTER);
		lblPassword.setBounds(190, 139, 71, 25);

		text_password = new Text(composite_Info, SWT.BORDER);
		text_password.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		text_password.setEditable(false);
		text_password.setBounds(267, 136, 226, 25);
		text_password.setEchoChar('*');
		text_password.setText(pw);
		
		btnDisplayPassword = new Button(composite_Info, SWT.NONE);
		btnDisplayPassword.setBounds(365, 170, 128, 25);
		btnDisplayPassword.setText("Display Password");

		TabItem tbtmChat = new TabItem(tabFolder, SWT.NONE);
		tbtmChat.setText("Chat");

		compositeChat = new Composite(tabFolder, SWT.NONE);
		compositeChat.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		tbtmChat.setControl(compositeChat);

		Composite composite_2 = new Composite(compositeChat, SWT.NONE);
		composite_2.setLocation(160, 0);
		composite_2.setSize(343, 46);

		btnStartConversation = new Button(composite_2, SWT.NONE);
		btnStartConversation.setLocation(0, 0);
		btnStartConversation.setSize(109, 25);
		btnStartConversation.setText("Start Conversation");

		lblErrorMessage = new Label(composite_2, SWT.NONE);
		lblErrorMessage.setBounds(0, 31, 343, 15);
		lblErrorMessage.setText("");

		currentCounterParty = new Label(composite_2, SWT.NONE);
		currentCounterParty.setBounds(201, 5, 142, 20);
		currentCounterParty.setText("");

		composite_3 = new Composite(compositeChat, SWT.NONE);
		composite_3.setLocation(160, 52);
		composite_3.setSize(343, 218);

		txtChatBox = new Text(composite_3, SWT.BORDER);
		txtChatBox.setBounds(0, 169, 343, 49);

		scrolledComposite = new ScrolledComposite(composite_3, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setBounds(0, 0, 343, 163);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		lstChatHistory = new List(scrolledComposite, SWT.BORDER);
		scrolledComposite.setContent(lstChatHistory);
		scrolledComposite.setAlwaysShowScrollBars(true);
		lstOnlineUsers = new List(compositeChat, SWT.BORDER);
		lstOnlineUsers.setLocation(0, 49);
		lstOnlineUsers.setSize(152, 221);

		btnLoadOnlineUsers = new Button(compositeChat, SWT.NONE);
		btnLoadOnlineUsers.setLocation(10, 10);
		btnLoadOnlineUsers.setSize(134, 25);
		btnLoadOnlineUsers.setText("Load online users");
		
		tbtmBlog = new TabItem(tabFolder, SWT.NONE);
		tbtmBlog.setText("Blog");
		
		compositeBlog = new Composite(tabFolder, SWT.NONE);
		tbtmBlog.setControl(compositeBlog);
		
		txtEditor = new Text(compositeBlog, SWT.BORDER);
		txtEditor.setBounds(126, 207, 375, 59);
		
		lstBlogTitle = new List(compositeBlog, SWT.BORDER);
		lstBlogTitle.setBounds(10, 10, 110, 162);
		
		lblBlogContent = new Label(compositeBlog, SWT.NONE);
		lblBlogContent.setBounds(126, 39, 375, 133);
		
		btnBlogPost = new Button(compositeBlog, SWT.NONE);
		btnBlogPost.setBounds(45, 241, 75, 25);
		btnBlogPost.setText("Publish");
		
		btnViewContent = new Button(compositeBlog, SWT.NONE);
		btnViewContent.setBounds(10, 205, 110, 25);
		btnViewContent.setText("View Content");
		
		txtTitle = new Text(compositeBlog, SWT.BORDER);
		txtTitle.setBounds(164, 178, 337, 21);
		
		lblMyTitle = new Label(compositeBlog, SWT.NONE);
		lblMyTitle.setBounds(126, 178, 32, 15);
		lblMyTitle.setText("Title");
		
		btnLoadPosts = new Button(compositeBlog, SWT.NONE);
		btnLoadPosts.setText("Load posts");
		btnLoadPosts.setBounds(10, 174, 110, 25);
		
		lblBlogTitle = new Label(compositeBlog, SWT.NONE);
		lblBlogTitle.setBounds(125, 10, 376, 23);
	}

	private void registerControlListeners() {

		btnStartConversation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (lstOnlineUsers.getSelection().length == 1) {
					currentCounterParty.setText(lstOnlineUsers.getSelection()[0]);
				} else {
					lblErrorMessage.setText("Select an online user to start a conversation");
				}
			}
		});

		txtChatBox.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if (!currentCounterParty.getText().equals("")) {
					clientSendHandler.sendMessage("Unicast", currentCounterParty.getText(), txtChatBox.getText());
					txtChatBox.setText("");
				}
			}
		});

		btnLoadOnlineUsers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				clientSendHandler.sendMessage("Online User Lists");
			}
		});
		
		btnBlogPost.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(!txtEditor.getText().isEmpty() && ! txtTitle.getText().isEmpty()){
					clientSendHandler.sendMessage("Blog post", name, txtTitle.getText(), txtEditor.getText());
				}
			}
		});
		
		btnLoadPosts.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				clientSendHandler.sendMessage("Load posts");
			}
		});
		
		btnViewContent.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(lstBlogTitle.getSelection().length == 1){
					String title = lstBlogTitle.getSelection()[0];
					Optional<String> content = blogPosts.stream()
							.filter(a -> a.split(":")[1].equals(title))
							.findFirst().map(b -> b.split(":")[2]);
					
					if(content.isPresent()){
						lblBlogTitle.setText(title);
						lblBlogContent.setText(content.get());
					}
				}
			}
		});
		
		btnDisplayPassword.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(text_password.getEchoChar() != '\0'){
					text_password.setEchoChar( '\0' );
				} else
					text_password.setEchoChar('*');
			}
		});
		
		btnUpdateAvatar.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Open a Dialog to select and upload avatar
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
				fd.setText("Choose File...");
				fd.setFilterPath("C:/");
				String[] filterExt = { "*.jpg", "*.JPG", "*.jpeg", "*.JPEG", "*png", "*.PNG", "*.bmp", "*.BMP",
						"*.gif" };
				fd.setFilterExtensions(filterExt);
				if (fd.open() != null) {
					String path = fd.getFilterPath();
					String fileName = fd.getFileName();
					String fullpath = path + "\\" + fileName;
					System.out.println(fullpath);
					Image img = SWTResourceManager.getImage(fullpath);
					avatar = new Image(display, img.getImageData().scaledTo(128, 128));
					clientSendHandler.sendMessage("Avatar changed", name, fullpath);
				}
				label_Avatar.setImage(avatar);
			}
		});
		
	}

	public void updateOnlineUsersList(String... users) {
		lstOnlineUsers.setItems(users);
	}

	public void updateChatHistory(String newMessage) {
		lstChatHistory.add(newMessage);
	}
	
	public void updatePostContentAndTitle(String content, String title){
		lblBlogContent.setText(content);
		lblBlogTitle.setText(title);
	}
	
	public Display getDisplay() {
		return display;
	}

	public void updateBlogPostsList(String... blogPosts) {
		this.blogPosts.clear();
		this.blogPosts.addAll(Arrays.asList(blogPosts));
		lstBlogTitle.setItems(this.blogPosts.stream()
					.map(a -> a.split(":")[1])
					.toArray(String[]::new));
	}

	public void updateAvatar(String avatarPath) {
		Image img = SWTResourceManager.getImage(avatarPath);
		avatar = new Image(display, img.getImageData().scaledTo(128, 128));
		label_Avatar.setImage(avatar);
	}
}
