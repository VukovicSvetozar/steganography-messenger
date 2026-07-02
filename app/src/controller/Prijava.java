package controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import utility.Dijalog;
import utility.FileLogger;
import utility.FxmlLoader;
import utility.Putanje;

public class Prijava implements Putanje {

	@FXML
	private Label lblStatus;

	@FXML
	private TextField txtUserName;

	@FXML
	private PasswordField txtPassword;

	protected static String ime;
	protected static String prezime;

	private static final int BROJ_ITERACIJA = 10000;
	private static final int DUZINA_KLJUCA = 256;

	@FXML
	public void Login(ActionEvent event) {
		try {
			if (prijava(txtUserName.getText(), new String(txtPassword.getText()))) {
				FxmlLoader.load(getClass(), "/view/GlavnaStrana.fxml", "Glavna Strana");
			} else {
				lblStatus.setText("Pokušajte ponovo");

				String upozorenje = "Uneseno korisnièko ime ili šifra nisu korektni!";
				Dijalog.showWarningDialog("Upozorenje", "Niste se uspješno prijavili!", upozorenje);
			}
		} catch (Exception ex) {
			FileLogger.log(Level.SEVERE, null, ex);
		}
	}

	public boolean prijava(String korisnickoIme, String unesenaLozinka) {
		try (BufferedReader in = new BufferedReader(new FileReader(PUTANJA_DO_LISTE_KORISNIKA))) {
			String linijaDatoteke;
			String[] podaci;
			while ((linijaDatoteke = in.readLine()) != null) {
				podaci = linijaDatoteke.split("#");
				if (korisnickoIme.equals(podaci[0])) {
					String zasticenaLozinka = podaci[4];
					String salt = podaci[3];
					boolean podudaranje = verifikacijaKorisnickeLozinka(unesenaLozinka, zasticenaLozinka, salt);
					if (podudaranje) {
						ime = podaci[1];
						prezime = podaci[2];
						return true;
					}
				}
			}
		} catch (IOException ex) {
			String porukaOGresci = "IO greška se javlja tokom èitanja iz stream-a.!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}

		return false;
	}

	private boolean verifikacijaKorisnickeLozinka(String unesenaLozinka, String zasticenaLozinka, String salt) {
		boolean povratnaVrijednost = false;
		byte[] hesVrijednost = hes(unesenaLozinka.toCharArray(), salt.getBytes());
		String novaZasticenaLozinka = Base64.getEncoder().encodeToString(hesVrijednost);
		povratnaVrijednost = novaZasticenaLozinka.equalsIgnoreCase(zasticenaLozinka);

		return povratnaVrijednost;
	}

	private static byte[] hes(char[] lozinka, byte[] salt) {
		PBEKeySpec specifikacija = new PBEKeySpec(lozinka, salt, BROJ_ITERACIJA, DUZINA_KLJUCA);
		Arrays.fill(lozinka, Character.MIN_VALUE);
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return skf.generateSecret(specifikacija).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new AssertionError("Greška tokom hešovanja lozinke: " + e.getMessage(), e);
		} finally {
			specifikacija.clearPassword();
		}
	}
}
