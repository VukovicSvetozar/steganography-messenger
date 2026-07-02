package utility;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import controller.ParametriSteganografije.BojaKanala;
import controller.ParametriSteganografije.TipKodiranja;
import controller.SlanjePoruke;

public class SteganografijaEnkripcija {

	private static BufferedImage slika;
	private static String putanjaIzlazneDatoteke;
	private static byte[] zaglavlje;
	private static byte[] poruka;

	private static BojaKanala bojaKanala;
	private static int noOfLSB;

	private static int x = 0, y = 0;

	public static void postaviSliku(String nosilac, String izlaz) {
		putanjaIzlazneDatoteke = izlaz;
		try {
			slika = ImageIO.read(new File(nosilac));
		} catch (IOException e) {
			String porukaOGresci = "Slika nosilac nije uèitana na odgovarajuæi naèin!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	public static void postaviPoruku(byte[] poruka) {
		SteganografijaEnkripcija.poruka = poruka;
	}

	public static void postaviZaglavlje(BojaKanala cc, int _noOfLSB) {
		bojaKanala = cc;
		noOfLSB = _noOfLSB;
		byte config1 = 0b00000000;
		byte config2 = (byte) noOfLSB;
		byte hLength = 3;

		config1 = (byte) (config1 | 1); // sets 1st bit to 1

		if (bojaKanala == BojaKanala.B || bojaKanala == BojaKanala.GB || bojaKanala == BojaKanala.RB
				|| bojaKanala == BojaKanala.RGB)
			config1 = (byte) (config1 | (1 << 2));
		if (bojaKanala == BojaKanala.G || bojaKanala == BojaKanala.GB || bojaKanala == BojaKanala.RG
				|| bojaKanala == BojaKanala.RGB)
			config1 = (byte) (config1 | (1 << 3));
		if (bojaKanala == BojaKanala.R || bojaKanala == BojaKanala.RB || bojaKanala == BojaKanala.RG
				|| bojaKanala == BojaKanala.RGB)
			config1 = (byte) (config1 | (1 << 4));

		zaglavlje = new byte[] { hLength, config1, config2 };
	}

	public static void kodiranje(TipKodiranja en, BojaKanala cc, int _noOfLSB) {
		bojaKanala = cc;
		noOfLSB = _noOfLSB;

		if (en == TipKodiranja.ZAGLAVLJE)
			doInsert(zaglavlje);
		else if (en == TipKodiranja.PORUKA) {
			doInsert(mergeMLengthWithData(poruka));
		}
		snimiSliku();
	}

	public static byte[] kompresija(String poslataPoruka) throws IOException {
		ByteArrayOutputStream bao = new ByteArrayOutputStream(poslataPoruka.length());
		GZIPOutputStream gos = new GZIPOutputStream(bao);
		gos.write(poslataPoruka.getBytes());
		gos.close();
		return bao.toByteArray();
	}
	
	private static void snimiSliku() {
		File file = new File(putanjaIzlazneDatoteke);
		String ekstenzijaIzlazneDatoteke = putanjaIzlazneDatoteke
				.substring(putanjaIzlazneDatoteke.lastIndexOf(".") + 1);
		try {
			ImageIO.write(slika, ekstenzijaIzlazneDatoteke.toUpperCase(), file);
		} catch (IOException e) {
			String porukaOGresci = "Slika nosilac nije snimljena na odgovarajuæi naèin!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	/* merge message length with data */
	private static byte[] mergeMLengthWithData(byte[] data) {

		ByteBuffer bf = ByteBuffer.allocate(4);
		bf.putInt(data.length + 4);
		byte[] mLength = bf.array();
		try {
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			bao.write(mLength);
			bao.write(data);
			data = bao.toByteArray();
		} catch (IOException e) {
			String porukaOGresci = "Greška je nastala tokom kodiranja!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
		return data;
	}

	private static void doInsert(byte[] data) {
		int[] pos;

		switch (bojaKanala) {
		case B:
			pos = new int[] { 0 };
			break;
		case G:
			pos = new int[] { 8 };
			break;
		case R:
			pos = new int[] { 16 };
			break;
		case GB:
			pos = new int[] { 0, 8 };
			break;
		case RB:
			pos = new int[] { 0, 16 };
			break;
		case RG:
			pos = new int[] { 8, 16 };
			break;
		case RGB:
			pos = new int[] { 0, 8, 16 };
			break;
		default:
			pos = new int[] { 0, 8, 16 };
			break;
		}

		int i = 128;
		int pixel = slika.getRGB(x, y);
		int width = slika.getWidth();
		int height = slika.getHeight();

		try {
			for (int b = 0, len = data.length; b < len;) {
				// iterates for i
				while (true) {
					// iterates through positions
					for (int j = 0; j < pos.length; j++) {
						// iterates through no. of LSB's each position
						for (int k = pos[j]; k < pos[j] + noOfLSB; k++) {
							// (data[b] & i) gives bit in byte b
							if ((data[b] & i) != 0) {
								// change k^th(k= 1-> 8) bit in pixel to 1
								pixel = (pixel | (1 << k));
							} else if ((data[b] & i) == 0) {
								// change k^th(k= 1-> 8) bit in pixel to 0
								pixel = (pixel & ~(1 << k));
							}
							if (i > 0)
								i /= 2;
							if (i == 0) {
								i = 128;
								b++;
							} // gets new byte
						}
					}
					if (b < data.length) {
						slika.setRGB(x++, y, pixel);
						if (x < width && y < height)
							pixel = slika.getRGB(x, y);
						else if (x >= width) {
							x = 0;
							y++;
							pixel = slika.getRGB(x, y);
						} else if (y >= height) {
							String upozorenje = "Nema dovoljno piksela kod slike nosioca poruke!";
							Dijalog.showWarningDialog("Upozorenje", "Slika nije odgovarajuce velicine!", upozorenje);
					
							// Minimalna velicina slike(sirina=visina)
							int velicina=(int)Math.ceil(Math.sqrt((SlanjePoruke.poslataPoruka.length()+8)*8));

							KriptoloskeMetode.podesavanjeSlike(slika, velicina, velicina);

						}
					} else
						throw new ArrayIndexOutOfBoundsException();
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			slika.setRGB(x++, y, pixel);

			if (x >= width) {
				x = 0;
				y++;
			}
			if (y >= height) {
				String upozorenje = "Nema dovoljno piksela kod slike nosioca poruke!";
				Dijalog.showWarningDialog("Upozorenje", "Slika nije odgovarajuce velicine!", upozorenje);
		
				// Minimalna velicina slike(sirina=visina)
				int velicina=(int)Math.ceil(Math.sqrt((SlanjePoruke.poslataPoruka.length()+8)*8));

				KriptoloskeMetode.podesavanjeSlike(slika, velicina, velicina);
			}
		}
	}

}
