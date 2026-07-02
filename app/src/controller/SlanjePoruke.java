package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import model.Korisnik;
import utility.Dijalog;
import utility.FxmlLoader;
import utility.KriptoloskeMetode;
import utility.Putanje;
import utility.RefreshableController;

import static controller.GlavnaStrana.korisnik;
import static controller.GlavnaStrana.sertifikatCA;

public class SlanjePoruke implements RefreshableController, Putanje {

	@FXML
	private ImageView ivSlika;

	@FXML
	private ComboBox<String> cbOdaberiSliku;

	@FXML
	private ListView<String> lvListaKorisnika;

	@FXML
	private Button btnposaljiPoruku;

	@FXML
	private TextArea taUnosPoruke;

	public ArrayList<Korisnik> korisnici = new ArrayList<>();

	public static String poslataPoruka = null;
	public static String putanjaOdabraneSlike = null;
	public static String putanjaPoslateSlike = null;

	@FXML
	public void initialize() {

		ucitavanjeSlike();
		popunjavanjeListeKorisnika();
	}

	@FXML
	public void odaberiSliku(ActionEvent event) throws MalformedURLException {
		ivSlika.setVisible(true);
		File file = new File(PUTANJA_DO_SLIKA + cbOdaberiSliku.getSelectionModel().getSelectedItem());
		String odabranaSlika = file.toURI().toURL().toString();

		Image slika = new Image(odabranaSlika, true);
		ivSlika.setSmooth(true);
		ivSlika.setCache(true);
		ivSlika.setImage(slika);
	}

	@FXML
	void posaljiPoruku(ActionEvent event) {

		try {
			String odabraniKorisnik = lvListaKorisnika.getSelectionModel().getSelectedItem();
			String[] razdvojImeIPrezime = odabraniKorisnik.split(" ");
			String hesPrimaoca = KriptoloskeMetode.izracunajHes(razdvojImeIPrezime[0], razdvojImeIPrezime[1]);
			odabraniKorisnik = odabraniKorisnik.replace(" ", "");

			// Ucitavanje sertifikata primaoca poruke
			String primalacSertifikata = PUTANJA_DO_KORISNICKIH_SERTIFIKATA + odabraniKorisnik + ".cer";
			X509Certificate sertifikat = KriptoloskeMetode.ucitajSertifikat(primalacSertifikata);

			// Provjera sertifikata primaoca poruke koristenjeme sertifikata CA tijela
			sertifikat.verify(sertifikatCA.getPublicKey());
			sertifikat.checkValidity(new Date());

			// Izdvajanje javnog kljuca primaoca poruke iz sertifikata
			PublicKey javniKljuc = sertifikat.getPublicKey();

			// Generisanje tajnog kljuca
			SecretKey tajniKljuc = KriptoloskeMetode.generisiAESKljuc();

			// Enkriptovanje tajnog kljuca javnim RSA kljucem primaoca poruke
			Cipher cipher;
			byte[] sifrovanAESKljuc = null;

			boolean keyUsagePosiljalac[] = korisnik.getSertifikat().getKeyUsage();
			boolean keyUsagePrimalac[] = sertifikat.getKeyUsage();

			if (keyUsagePrimalac[2]) {
				cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.ENCRYPT_MODE, javniKljuc);
				sifrovanAESKljuc = cipher.doFinal(tajniKljuc.getEncoded());
			} else {
				String porukaOGresci = "Sertifikat odabranog korisnika se ne može iskoristiti za šifrovanje kljuèa!";
				Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
			}

			// Kreiranje poruke za slanje
			LocalDateTime datum = LocalDateTime.now();
			DateTimeFormatter formatVremena = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

			String poruka = "Datum slanja poruke:    " + formatVremena.format(datum) + "\nPošiljalac poruke:          "
					+ korisnik.toString() + "\n\nPoruka:\n";
			poruka += taUnosPoruke.getText();

			// Kreiranje digitalnog potpisa poruke
			byte[] digitalniPotpis = null;
			if (keyUsagePosiljalac[0]) {
				Signature potpis = Signature.getInstance("SHA256withRSA");
				potpis.initSign(korisnik.getPrivatniKljuc());
				potpis.update(poruka.getBytes("UTF-8"));
				digitalniPotpis = potpis.sign();
			} else {
				String porukaOGresci = "Vaš sertifikat se ne može koristiti za digitalno potpisivanje!";
				Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
			}
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, tajniKljuc);

			String enkriptovanaPoruka = KriptoloskeMetode.enkriptovanjePorukeAESKljucem(digitalniPotpis, cipher,
					sifrovanAESKljuc, poruka);

			String lokacija = PUTANJA_DO_KORISNICKOG_FOLDERA + hesPrimaoca + File.separator;

			putanjaPoslateSlike = lokacija + "Poruke korisnika" + File.separator
					+ KriptoloskeMetode.izracunajImeFajlaZaSlanje(korisnik.getIme(), korisnik.getPrezime()) + ".png";

			putanjaOdabraneSlike = PUTANJA_DO_SLIKA + cbOdaberiSliku.getSelectionModel().getSelectedItem();



			if (cbOdaberiSliku.getSelectionModel().isEmpty()) {
				String upozorenje = "Pokušajte ponovo";
				Dijalog.showWarningDialog("Upozorenje", "Morate odabrati sliku za nosioca poruke!", upozorenje);
			} else {
				
				taUnosPoruke.clear();
				taUnosPoruke.setDisable(true);
				lvListaKorisnika.getSelectionModel().clearSelection();
				lvListaKorisnika.setDisable(true);
				cbOdaberiSliku.setDisable(true);
				ivSlika.setImage(null);
				btnposaljiPoruku.setDisable(true);
				
				poslataPoruka = enkriptovanaPoruka;
				FxmlLoader.load(getClass(), "/view/ParametriSteganografije.fxml", "Parametri");
			}

		} catch (NoSuchAlgorithmException e) {
			String porukaOGresci = "Nije podržan algoritam potpisa!!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (NoSuchPaddingException e) {
			String porukaOGresci = "Mehanizam za dopunjavanje nije dostupan u okruženju!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (InvalidKeyException e) {
			String porukaOGresci = "Kljuè nije ispravan!!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (IllegalBlockSizeException e) {
			String porukaOGresci = "Dužina podataka ne odgovara velièini bloka cipher-a!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (BadPaddingException e) {
			String porukaOGresci = "Podaci nisu pravilno postavljeni za dati mehanizam dopunjavanja!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (CertificateExpiredException e) {
			String porukaOGresci = "Vaš sertifikat je istekao.\nNe možete slati poruke drugim korisnicima!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (CertificateNotYetValidException e) {
			String porukaOGresci = "Trenutni ili naznaèeni datum je prije datuma u odgovarajuæem periodu važenja sertifikata!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (CertificateException e) {
			String porukaOGresci = "Greška je nastala tokom kodiranja!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (NoSuchProviderException e) {
			String porukaOGresci = "Ne postoji podrazumijevani provajder!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (SignatureException e) {
			String porukaOGresci = "Vaš sertifikat nije potpisan od strane odgovarajuæeg CA tijela!";
			Dijalog.showErrorDialog("Greška", "Aplikacija æe biti blokirana!", porukaOGresci);
		} catch (NullPointerException e) {
			String upozorenje = "Pokušajte ponovo";
			Dijalog.showWarningDialog("Upozorenje", "Morate odabrati posiljaoca!", upozorenje);
		} catch (Exception e) {
			String porukaOGresci = "Desila se greška prilikom upisivanja i slanja poruke. \nPoruka nije poslata!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
			e.printStackTrace();
		}
	}

	@Override
	public void refresh() {
	}

	/* Pomoæna metoda za popunjavanje comboBox-a dostupnim slikama */
	private void ucitavanjeSlike() {

		File slike = new File(PUTANJA_DO_SLIKA);
		String[] naziviSlika = slike.list();
		for (int i = 0; i < naziviSlika.length; i++)
			cbOdaberiSliku.getItems().add(naziviSlika[i]);
	}

	private void popunjavanjeListeKorisnika() {
		try (BufferedReader in = new BufferedReader(new FileReader(Prijava.PUTANJA_DO_LISTE_KORISNIKA))) {
			String linijaDatoteke;
			while ((linijaDatoteke = in.readLine()) != null) {
				String[] niz = linijaDatoteke.split("#");
				if (!(niz[1] + " " + niz[2]).equals(korisnik.getIme() + " " + korisnik.getPrezime())
						&& !(niz[1] + " " + niz[2]).equals("ime prezime")) {
					Korisnik korisnikLokalno = new Korisnik(niz[1], niz[2]);
					korisnici.add(korisnikLokalno);
				}
			}
		} catch (FileNotFoundException e) {
			String porukaOGresci = "Navedena datoteka nije pronaðena na datoj putanji!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		} catch (IOException e1) {
			String porukaOGresci = "IO greška se javlja tokom èitanja iz stream-a.!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}

		for (int i = 0; i < korisnici.size(); i++) {
			lvListaKorisnika.getItems().add(korisnici.get(i).toString());
		}
	}

}
