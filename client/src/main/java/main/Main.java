package main;	

import client.view.WellcomeShell;

public class Main {
	public static void main(String[] args) {
		try {
			// Where it all begins
			WellcomeShell wellcomeShell = new WellcomeShell();
			wellcomeShell.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
