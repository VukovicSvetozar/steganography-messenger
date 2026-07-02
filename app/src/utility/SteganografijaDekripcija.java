package utility;

import javax.imageio.ImageIO;

import controller.ParametriSteganografije.BojaKanala;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

public class SteganografijaDekripcija {

	private static BufferedImage slika;
	private static byte[] zaglavlje;
	private static byte[] poruka;

	private static BojaKanala bojaKanala;
	private static int noOfLSB;

	private static int x = 0, y = 0;

	public static void postaviSliku(String nosilac) {
		try {
			slika = ImageIO.read(new File(nosilac));
		} catch (IOException e) {
			String porukaOGresci = "Greška je nastala tokom uèitavanja nosioca poruke!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
	}

	public static void dekodiranje() {
		x = 0;
		y = 0;
		dekodiranjeZaglavlja(BojaKanala.RGB, 2);
		dekodiranjePoruke();
		x = 0;
		y = 0;
	}

	public static String vratiPoruku() {
		String data = null;
		try {
			data = dekompresija(poruka);
		} catch (Exception e) {
			String porukaOGresci = "Greška je nastala tokom dekompresije ili tokom dekripcije!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
		return data;
	}

	private static void dekodiranjeZaglavlja(BojaKanala cc, int _noOfLSB) {
		bojaKanala = cc;
		noOfLSB = _noOfLSB;
		byte[] data;

		data = doExtract(1, true);
		int HLEN = (int) data[0];

		if (HLEN == 3 || HLEN == 20)
			zaglavlje = doExtract(HLEN, false);
		else {
			String porukaOGresci = "Na slici nije primjenjena odgovarajuæa stenografija!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
		}
		postaviDetaljeZaglavlja();
	}

	private static void dekodiranjePoruke() {
		byte[] podaci;

		podaci = doExtract(4, true);
		ByteBuffer bf = ByteBuffer.wrap(podaci);
		int MLEN = bf.getInt();
		byte[] temp = doExtract(MLEN, false);

		try {
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			for (int i = 4; i < MLEN; i++)
				bao.write(temp[i]);
			poruka = bao.toByteArray();
		} catch (Exception e) {
			String porukaOGresci = "Greška je nastala tokom dekodiranja poruke!";
			Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);

		}
	}

	private static void postaviDetaljeZaglavlja() {

		noOfLSB = (int) zaglavlje[2];

		if (((zaglavlje[1] & 16) != 0)) 			// red
			if (((zaglavlje[1] & 8) != 0)) 			// green
				if (((zaglavlje[1] & 4) != 0)) 		// blue
					bojaKanala = BojaKanala.RGB;
				else
					bojaKanala = BojaKanala.RG;
			else if (((zaglavlje[1] & 4) != 0))
				bojaKanala = BojaKanala.RB;
			else
				bojaKanala = BojaKanala.R;
		else if (((zaglavlje[1] & 8) != 0))
			if (((zaglavlje[1] & 4) != 0))
				bojaKanala = BojaKanala.GB;
			else
				bojaKanala = BojaKanala.G;
		else if (((zaglavlje[1] & 4) != 0))
			bojaKanala = BojaKanala.B;

	}

	private static byte[] doExtract(int noOfBytes, boolean isPeek) {
		int[] pos;
		int tempX = 0, tempY = 0;
		boolean useNextPixelNextTime = (!isPeek);

		if (isPeek) {
			tempX = x;
			tempY = y;
		}

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

		int pixel = slika.getRGB(x, y);
		int width = slika.getWidth();
		int height = slika.getHeight();

		byte[] data = new byte[noOfBytes];
		int i = 0, bc = 7; 

		try {
			while (true) { 
				for (int j = 0; j < pos.length; j++) { 
					for (int k = pos[j]; k < pos[j] + noOfLSB; k++) { 

						if ((pixel & (int) Math.pow(2, k)) != 0) { 																	
							data[i] = (byte) (data[i] | (1 << bc--));
						} else if ((pixel & (int) Math.pow(2, k)) == 0) {
							data[i] = (byte) (data[i] & ~(1 << bc--));
						}
						if (bc < 0) {
							bc = 7;
							i++;
						}
					}
				}
				if (i < noOfBytes) {
					x++;
				} else {
					useNextPixelNextTime = true;
					throw new ArrayIndexOutOfBoundsException();
				}

				if (x < width && y < height)
					pixel = slika.getRGB(x, y);
				else if (x >= width) {
					x = 0;
					y++;
					pixel = slika.getRGB(x, y);
				} else if (y >= height) {
					String porukaOGresci = "Nema dovoljno piksela kod slike nosioca poruke!";
					Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			if (isPeek) {
				x = tempX;
				y = tempY;
			} else {
				if (useNextPixelNextTime)
					x++;
				if (x >= width) {
					x = 0;
					y++;
				}
				if (y >= height) {
					String porukaOGresci = "Nema dovoljno piksela kod slike nosioca poruke!";
					Dijalog.showErrorDialog("Greška", "Došlo je do greške!", porukaOGresci);
				}
			}
		}
		return data;
	}

	private static String dekompresija(byte[] b) throws IOException {
		ByteArrayInputStream bai = new ByteArrayInputStream(b);
		GZIPInputStream gis = new GZIPInputStream(bai);
		BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		gis.close();
		bai.close();
		return sb.toString();
	}

}
