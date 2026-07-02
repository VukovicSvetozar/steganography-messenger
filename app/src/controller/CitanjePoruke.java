package controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

import utility.Dijalog;
import utility.KriptoloskeMetode;
import utility.Putanje;
import utility.RefreshableController;
import utility.SteganografijaDekripcija;

import static controller.GlavnaStrana.korisnik;
import static controller.GlavnaStrana.neprocitanePoruke;

public class CitanjePoruke implements RefreshableController, Putanje {

	@FXML
	private ComboBox<String> cbOdaberiPoruku;

	@FXML
	private TextArea taDekriptovanjePoruke;

	@FXML
	private Button btnDekriptuj;

	private static File porukaZaCitanje;
	private byte[] sifrovaniKljuc;
	private byte[] sifrat;
	private byte[] potpis;
	private byte[] desifrovanaPorukaBytes;
	private byte[] dekriptovaniPotpisIPoruka;
	private String primljenaPoruka;
	private PrivateKey privatniKljuc;

	@FXML
	void initialize() {
		
		taDekriptovanjePoruke.setEditable(false);
		if (korisnik.getBrojNeprocitanihPoruka() == 0)
			cbOdaberiPoruku.setDisable(true);
		else
			cbOdaberiPoruku.setDisable(false);

		ucitajNeprocitanePoruke();

	}

	@FXML
	void odaberiPoruku(ActionEvent event) {
	}

	@FXML
	void dekriptuj(ActionEvent event) {

		try {

			String putanjaOdabranePoruke = korisnik.getPutanjaKorisnickogFoldera() + "Poruke korisnika" + File.separator
					+ cbOdaberiPoruku.getSelectionModel().getSelectedItem();

			porukaZaCitanje = new File(putanjaOdabranePoruke);

			if (!porukaZaCitanje.exists()) {

				porukaZaCitanje = new File("");
				int indeks = cbOdaberiPoruku.getSelectionModel().getSelectedIndex();
				cbOdaberiPoruku.getItems().remove(indeks);
				obrisiPoruku();

				String upozorenje = "Odabrana poruka je izvan aplikacije obrisana sa fajl sistema!";
				Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", upozorenje);

			} else {

				SteganografijaDekripcija.postaviSliku(putanjaOdabranePoruke);

				SteganografijaDekripcija.dekodiranje();

				izdvajanjeSifrovanogKljucaISifrata();

				ucitavanjePrivatnogKljuca();

				dekripcijaAESKljuca();

				izdvajanjePotpisaIzPoruke();

				prikazPoruke();

				obrisiPoruku();

				validacijaPotpisaPoruke();

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
		} catch (SignatureException e) {
			String porukaOGresci = "Vaš sertifikat nije potpisan od strane odgovarajuæeg CA tijela!";
			Dijalog.showErrorDialog("Greška", "Aplikacija æe biti blokirana!", porukaOGresci);
		} catch (NullPointerException e) {
			String upozorenje = "Pokušajte ponovo";
			Dijalog.showWarningDialog("Upozorenje", "Morate odabrati posiljaoca!", upozorenje);
			e.printStackTrace();
		} catch (Exception ex) {
			String porukaOGresci = "Došlo je do greške prilikom èitanja poruke!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	private void ucitajNeprocitanePoruke() {
		File neprocitanePorukeFajl = new File(
				korisnik.getPutanjaKorisnickogFoldera() + File.separator + "Poruke korisnika");

		if (korisnik.getBrojNeprocitanihPoruka() == 0 || !(neprocitanePorukeFajl.exists())) {
			cbOdaberiPoruku.setDisable(true);
			btnDekriptuj.setDisable(true);
		} else {
			String[] neprocitanePoruke = neprocitanePorukeFajl.list();

			// Popunjavanje ComboBox-a neprocitanim porukama (slikama)
			for (int i = 0; i < neprocitanePoruke.length; i++) {
				cbOdaberiPoruku.getItems().add(neprocitanePoruke[i]);
			}

		}

	}

	private void obrisiComboBoxStavke() {
		File neprocitanePorukeFajl = new File(
				korisnik.getPutanjaKorisnickogFoldera() + File.separator + "Poruke korisnika");

		if (korisnik.getBrojNeprocitanihPoruka() == 0 || !(neprocitanePorukeFajl.exists())) {
			cbOdaberiPoruku.setDisable(true);
			btnDekriptuj.setDisable(true);
		} else {
			String[] neprocitanePoruke = neprocitanePorukeFajl.list();

			// Brisanje ComboBox stavki
			for (int i = 0; i < neprocitanePoruke.length; i++) {
				cbOdaberiPoruku.getItems().remove(neprocitanePoruke[i]);
			}
		}

	}

	private void obrisiPoruku() {

		obrisiComboBoxStavke();

		porukaZaCitanje.delete();

		korisnik.setBrojNeprocitanihPoruka(korisnik.getBrojNeprocitanihPoruka() - 1);
		neprocitanePoruke.setText("" + (korisnik.getBrojNeprocitanihPoruka()));

		ucitajNeprocitanePoruke();
	}

	private void izdvajanjeSifrovanogKljucaISifrata() throws UnsupportedEncodingException {
		byte[] dekodiranaPoruka = Base64.getDecoder().decode(SteganografijaDekripcija.vratiPoruku().getBytes("UTF-8"));
		sifrovaniKljuc = new byte[256];
		sifrat = new byte[dekodiranaPoruka.length - sifrovaniKljuc.length];

		for (int i = 0; i < dekodiranaPoruka.length; i++) {
			if (i < sifrovaniKljuc.length)
				sifrovaniKljuc[i] = dekodiranaPoruka[i];
			else
				sifrat[i - sifrovaniKljuc.length] = dekodiranaPoruka[i];
		}
	}

	private void ucitavanjePrivatnogKljuca() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		File putanjaPrivatnogKljuca = new File(
				korisnik.getPutanjaKorisnickogFoldera() + PUTANJA_DO_PRIVATNOG_KLJUCA + "PrivatniKljuc.der");

		byte[] privBytes = Files.readAllBytes(Paths.get(putanjaPrivatnogKljuca.getPath()));

		// Generisanje privatnog kljuca
		PKCS8EncodedKeySpec specifikacija = new PKCS8EncodedKeySpec(privBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		privatniKljuc = kf.generatePrivate(specifikacija);
	}

	private void dekripcijaAESKljuca() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {

		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, privatniKljuc);
		byte[] keyb = cipher.doFinal(sifrovaniKljuc);
		SecretKey skey = new SecretKeySpec(keyb, "AES");

		cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skey);
		dekriptovaniPotpisIPoruka = cipher.doFinal(sifrat);
	}

	private void izdvajanjePotpisaIzPoruke() {
		potpis = new byte[256];
		desifrovanaPorukaBytes = new byte[dekriptovaniPotpisIPoruka.length - potpis.length];
		for (int i = 0; i < dekriptovaniPotpisIPoruka.length; i++) {
			if (i < potpis.length)
				potpis[i] = dekriptovaniPotpisIPoruka[i];
			else
				desifrovanaPorukaBytes[i - potpis.length] = dekriptovaniPotpisIPoruka[i];
		}
	}

	private void prikazPoruke() throws UnsupportedEncodingException {
		primljenaPoruka = new String(desifrovanaPorukaBytes, "UTF-8");
		taDekriptovanjePoruke.setText(primljenaPoruka);
	}

	private void validacijaPotpisaPoruke() throws FileNotFoundException, CertificateException, NoSuchAlgorithmException,
			SignatureException, InvalidKeyException {
		String drugiRed = primljenaPoruka.split("\\n")[1];
		String posiljalac = drugiRed.split(" ")[11] + " " + drugiRed.split(" ")[12];
		posiljalac = posiljalac.replace(" ", "");
		String putanjaPosiljaocaSertifikata = PUTANJA_DO_KORISNICKIH_SERTIFIKATA + posiljalac + ".cer";

		X509Certificate posiljalacSertifikata = KriptoloskeMetode.ucitajSertifikat(putanjaPosiljaocaSertifikata);

		// Izdvajanje javnog kljuca pošiljaoca poruke iz sertifikata
		PublicKey javniKljucPosiljaoca = posiljalacSertifikata.getPublicKey();

		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initVerify(javniKljucPosiljaoca);
		signature.update(desifrovanaPorukaBytes);

		if (signature.verify(potpis)) {
			String porukaOObavjestenju = "Potpis je uspješno verifikovan!";
			Dijalog.showInfoDialog("Obavještenje", "Ovo je obavještenje!", porukaOObavjestenju);
		} else {
			String porukaOUpozorenju = "Potpis nije uspješno verifikovan!";
			Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", porukaOUpozorenju);
		}
	}

	@Override
	public void refresh() {

	}

}
