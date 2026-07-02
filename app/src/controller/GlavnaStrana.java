package controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import model.Korisnik;

import utility.Dijalog;
import utility.KriptoloskeMetode;
import utility.Putanje;
import utility.RefreshableController;

public class GlavnaStrana implements Putanje {

	@FXML
	private Label lbPrezime;

	@FXML
	private Label lbIme;

	@FXML
	private TabPane tabKontejner;

	@FXML
	private Tab tab1;

	@FXML
	private Tab tab2;

	@FXML
	private Tab tab3;

	@FXML
	private Button izlazDugme;

	@FXML
	private Stage primaryStage;

	@FXML
	private HBox hbKontejnerLabela;

	public static Korisnik korisnik;
	public static X509Certificate sertifikatCA;

	public static Label neprocitanePoruke;

	@FXML
	public void initialize() {

		inicijalizacijaKorisnika();
		ucitavanjeSertifikata();
		provjeraSertifikata();
		postavljanjePutanjeDoLicnogFoldera();
		ucitavanjePrivatnogKljuca();
		postaviBrojNeprocitanihPoruka();

		try {
			ucitajTab(tab1, "/view/SlanjePoruke.fxml");
			ucitajTab(tab2, "/view/CitanjePoruke.fxml");
		} catch (IOException e) {
			String porukaOGresci = "Aplikacija nije u moguænosti da prikaže tabove!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	@FXML
	public void izadji(ActionEvent event) {
		Platform.exit();
	}

	private void inicijalizacijaKorisnika() {
		korisnik = new Korisnik(Prijava.ime, Prijava.prezime);
		korisnik.setPutanjaSertifikata(
				PUTANJA_DO_KORISNICKIH_SERTIFIKATA + korisnik.getIme() + korisnik.getPrezime() + ".cer");

		lbIme.setText(korisnik.getIme());
		lbPrezime.setText(korisnik.getPrezime());
	}

	private static void ucitavanjeSertifikata() {
		try {
			korisnik.setSertifikat(KriptoloskeMetode.ucitajSertifikat(korisnik.getPutanjaSertifikata()));
			sertifikatCA = KriptoloskeMetode.ucitajSertifikat(PUTANJA_DO_ROOT_CA + "RootCA.cer");
		} catch (FileNotFoundException e) {
			String porukaOGresci = "Navedena datoteka nije pronaðena na datoj putanji!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (CertificateException e) {
			String porukaOGresci = "Sertifikat se ne može uèitati!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	private void provjeraSertifikata() {
		try {
			X509Certificate sertifikat = KriptoloskeMetode.ucitajSertifikat(korisnik.getPutanjaSertifikata());
			sertifikat.verify(sertifikatCA.getPublicKey());
			sertifikat.checkValidity(new Date());
		} catch (FileNotFoundException e) {
			String porukaOGresci = "Navedena datoteka nije pronaðena na datoj putanji!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (InvalidKeyException e) {
			String porukaOGresci = "Kljuè nije ispravan!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (NoSuchAlgorithmException e) {
			String porukaOGresci = "Nije podržan algoritam potpisa!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (NoSuchProviderException e) {
			String porukaOGresci = "Ne postoji podrazumijevani provajder!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (SignatureException e) {
			String porukaOGresci = "Vaš sertifikat nije potpisan od strane odgovarajuæeg CA tijela!";
			Dijalog.showErrorDialog("Greška", "Aplikacija æe biti blokirana!", porukaOGresci);
		} catch (CertificateExpiredException ex) {
			String porukaOGresci = "Vaš sertifikat je istekao.\nNe možete slati poruke drugim korisnicima!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (CertificateException e) {
			String porukaOGresci = "Greška je nastala tokom kodiranja!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	private void postavljanjePutanjeDoLicnogFoldera() {
		try {
			String putanja = KriptoloskeMetode.izracunajHes(korisnik.getIme(), korisnik.getPrezime());
			korisnik.setPutanjaKorisnickogFoldera(PUTANJA_DO_KORISNICKOG_FOLDERA + putanja + File.separator);
		} catch (NoSuchAlgorithmException e) {
			String porukaOGresci = "Nijedan provajder ne podržava implementaciju MessageDigestSpi za navedeni algoritam!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (UnsupportedEncodingException e) {
			String porukaOGresci = "Nije podržan odgovarajuæi skup znakova!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}

	}

	private void ucitavanjePrivatnogKljuca() {
		File licniFolder = new File(korisnik.getPutanjaKorisnickogFoldera());
		if (!licniFolder.exists()) {
			String porukaOGresci = "Vaš lièni folder je premješten ili obrisan sa sistema!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} else {
			// Ucitavanje privatnog kljuca
			File privatniKljuc = new File(
					korisnik.getPutanjaKorisnickogFoldera() + PUTANJA_DO_PRIVATNOG_KLJUCA + "PrivatniKljuc.der");
			if (privatniKljuc.exists()) {

				java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

				try {
					byte privatni[] = ucitajKljuc(privatniKljuc.getPath());
					PKCS8EncodedKeySpec specifikacija = new PKCS8EncodedKeySpec(privatni);
					KeyFactory kf = KeyFactory.getInstance("RSA");
					korisnik.setPrivatniKljuc(kf.generatePrivate(specifikacija));
				} catch (IOException e) {
					String porukaOGresci = "IO greška se javlja tokom èitanja iz stream-a.!";
					Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
				} catch (NoSuchAlgorithmException e) {
					String porukaOGresci = "Nije podržan algoritam potpisa!!";
					Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
				} catch (InvalidKeySpecException e) {
					String porukaOGresci = "Kljuè nije ispravan!";
					Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
				}
			} else {
				String porukaOGresci = "Vaš privatni kljuè je premješten ili obrisan sa sistema!";
				Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
			}
		}
	}

	/* Pomocna metoda za uèitavanje broja nepocitanih poruka datog korisnika. */
	public void postaviBrojNeprocitanihPoruka() {
		File neprocitanePorukePutanja = new File(
				korisnik.getPutanjaKorisnickogFoldera() + File.separator + "Poruke korisnika");
		korisnik.setBrojNeprocitanihPoruka(neprocitanePorukePutanja.list().length);

		neprocitanePoruke = new Label(Integer.toString(korisnik.getBrojNeprocitanihPoruka()));
		neprocitanePoruke.setFont(new Font("System", 17));
		neprocitanePoruke.setTextFill(Color.web("#e42f06"));
		neprocitanePoruke.setFont(Font.font("Verdana", FontWeight.BOLD, 70));
		neprocitanePoruke.setFont(Font.font("Verdana", FontPosture.ITALIC, 20));
		hbKontejnerLabela.getChildren().add(neprocitanePoruke);
	}

	private static byte[] ucitajKljuc(String nazivFajla) throws IOException {
		Path lokacija = Paths.get(nazivFajla);
		return Files.readAllBytes(lokacija);
	}

	/* Pomocna metoda za uèitavanje fxml fajlova u tabove ovog kontrolera. */
	private void ucitajTab(Tab tab, String fxml) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
		Parent root = loader.load();
		RefreshableController controller = loader.getController();

		AnchorPane pane = (AnchorPane) tab.getContent();
		pane.getChildren().add(root);
		AnchorPane.setBottomAnchor(root, 0.0);
		AnchorPane.setLeftAnchor(root, 0.0);
		AnchorPane.setRightAnchor(root, 0.0);
		AnchorPane.setTopAnchor(root, 0.0);

		// Ažuriraj podatke svaki put kada se otvori dati tab
		tab.setOnSelectionChanged(e -> {
			if (tab.selectedProperty().get())
				controller.refresh();
		});
	}

}
