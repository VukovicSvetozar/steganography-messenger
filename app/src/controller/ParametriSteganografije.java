package controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import utility.Dijalog;
import utility.SteganografijaEnkripcija;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ParametriSteganografije {

	@FXML
	private CheckBox cbPlavo;

	@FXML
	private CheckBox cbCrveno;

	@FXML
	private CheckBox cbZeleno;

	@FXML
	private Label lblVelicinaTeksta;

	@FXML
	private Label lblKompresovanaVelicinaTeksta;

	@FXML
	private Label lblPotrebanBrojPikselaNosioca;

	@FXML
	private Label lblUkupanBrojPikselaNosioca;

	@FXML
	private Label lblMaksimalnaVelicinaPodatka;

	@FXML
	private Label lblVrijednostKlizaca;

	@FXML
	private Label lblGreska;

	@FXML
	private Button btnKodiranje;

	@FXML
	private HBox hbKlizac;

	private Slider klizacLSB;

	private static String putanjaNosioca;
	private static BojaKanala bojaKanala;
	private static int LSBs = 1;

	private static int originalnaVelicinaTeksta;
	private static int kompresovanaVelicinaTeksta;
	private static int potrebanBrojPiksela;
	private static int brojPikselaNosioca;

	public enum BojaKanala {
		B, G, R, GB, RG, RB, RGB
	}

	public enum TipKodiranja {
		ZAGLAVLJE, PORUKA
	}

	@FXML
	public void initialize() {

		postaviBojuKanala();

		inicijalizacijaPolja();

		kreiranjeKlizaca();

	}

	@FXML
	void kodiranje(ActionEvent event) {
		if (!greska()) {
			btnKodiranje.setDisable(true);
			kodiranje();
			
	    	Stage stage = (Stage) btnKodiranje.getScene().getWindow();
			stage.close();
		}	
	}

	@FXML
	void odaberiBoju(ActionEvent event) {
		postaviBojuKanala();
		lblGreska.setText("Parametri su podešeni");
	}

	private void inicijalizacijaPolja() {
		try {
			putanjaNosioca = SlanjePoruke.putanjaOdabraneSlike;
			brojPikselaNosioca = getBrojPikselaNosioca();
			originalnaVelicinaTeksta = SlanjePoruke.poslataPoruka.length();
			kompresovanaVelicinaTeksta = SteganografijaEnkripcija.kompresija(SlanjePoruke.poslataPoruka).length;
			
			refreshDetails();

		} catch (Exception e) {
			String porukaOGresci = "Greška je nastala tokom kompresije datoteke!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	private void postaviBojuKanala() {
		if (cbCrveno.isSelected())
			if (cbZeleno.isSelected())
				if (cbPlavo.isSelected())
					bojaKanala = BojaKanala.RGB;
				else
					bojaKanala = BojaKanala.RG;
			else if (cbPlavo.isSelected())
				bojaKanala = BojaKanala.RB;
			else
				bojaKanala = BojaKanala.R;
		else if (cbZeleno.isSelected())
			if (cbPlavo.isSelected())
				bojaKanala = BojaKanala.GB;
			else
				bojaKanala = BojaKanala.G;
		else if (cbPlavo.isSelected())
			bojaKanala = BojaKanala.B;
		else
			bojaKanala = BojaKanala.RGB; // u slucaju da ništa nije odabrano

		refreshDetails();
	}

	private boolean greska() {
		boolean daLiJeGreska = false;
		if (!cbCrveno.isSelected() && !cbZeleno.isSelected() && !cbPlavo.isSelected()) {
			daLiJeGreska = true;
			lblGreska.setText("Greška! Odaberite boju piksela.");
		} else if (potrebanBrojPiksela > brojPikselaNosioca) {
			lblGreska.setText("Greška! Nosilac je isuviše mali.");
			daLiJeGreska = true;			
		}
		return daLiJeGreska;
	}

	private void refreshDetails() {
		String cc = bojaKanala.toString();

		// LSBs 1->8, cc 1, 2 or 3
		potrebanBrojPiksela = (((originalnaVelicinaTeksta * 8) + 250) / (cc.length() * LSBs));

		// Maksimalno 250 bajta za zaglavlje
		int maksimumNosioca = (cc.length() * LSBs * brojPikselaNosioca) / 8 - (250 / 8);

		lblVelicinaTeksta.setText(String.valueOf(originalnaVelicinaTeksta) + "  Bajta");
		lblKompresovanaVelicinaTeksta.setText(String.valueOf(kompresovanaVelicinaTeksta) + "  Bajta");
		lblPotrebanBrojPikselaNosioca.setText(String.valueOf(potrebanBrojPiksela) + "  Piksela");
		lblUkupanBrojPikselaNosioca.setText(String.valueOf(brojPikselaNosioca) + "  Piksela");
		lblMaksimalnaVelicinaPodatka.setText(String.valueOf(maksimumNosioca) + "  Bajta");

	}

	private static int getBrojPikselaNosioca() {
		int noOfPixels = 0;
		try {
			BufferedImage img = ImageIO.read(new File(putanjaNosioca));
			noOfPixels = img.getHeight() * img.getWidth();
		} catch (IOException e) {
			String upozorenje = "Pokušajte ponovo";
			Dijalog.showWarningDialog("Upozorenje", "Morate odabrati sliku za nosioca poruke!", upozorenje);
		}
		return noOfPixels;
	}

	private static void kodiranje() {
		try {

			SteganografijaEnkripcija.postaviSliku(SlanjePoruke.putanjaOdabraneSlike, SlanjePoruke.putanjaPoslateSlike);

			SteganografijaEnkripcija.postaviZaglavlje(bojaKanala, LSBs);

			SteganografijaEnkripcija.kodiranje(TipKodiranja.ZAGLAVLJE, BojaKanala.RGB, 2);

			SteganografijaEnkripcija.postaviPoruku(SteganografijaEnkripcija.kompresija(SlanjePoruke.poslataPoruka));

			SteganografijaEnkripcija.kodiranje(TipKodiranja.PORUKA, bojaKanala, LSBs);

			String porukaOObavjestenju = "Poruka je uspješno ubaèena u sliku i poslata odabranom korisniku!";
			Dijalog.showInfoDialog("Obavjestenje", "Ovo je obavjestenje!", porukaOObavjestenju);
			
		} catch (Exception e) {
			String porukaOGresci = "Greška je nastala tokom kompresije ili enkripcije podataka!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	private void kreiranjeKlizaca() {

		klizacLSB = new Slider();

		hbKlizac.getChildren().add(klizacLSB);

		klizacLSB.setMin(1);
		klizacLSB.setMax(8);
		klizacLSB.setValue(1);
		klizacLSB.setShowTickLabels(true);
		klizacLSB.setShowTickMarks(true);
		klizacLSB.setBlockIncrement(1);
		klizacLSB.setMinWidth(200);
		klizacLSB.setMaxWidth(200);

		klizacLSB.valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				LSBs = newValue.intValue();
				lblVrijednostKlizaca.setText("" + LSBs);
				refreshDetails();
			}
		});
	}

}
