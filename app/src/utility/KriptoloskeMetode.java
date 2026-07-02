package utility;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.util.encoders.Hex;

public class KriptoloskeMetode {

	public static X509Certificate ucitajSertifikat(String putanjaSertifikata)
			throws FileNotFoundException, CertificateException {

		FileInputStream fin = new FileInputStream(putanjaSertifikata);
		CertificateFactory fabrika = CertificateFactory.getInstance("X.509");
		X509Certificate sertifikat = (X509Certificate) fabrika.generateCertificate(fin);

		return sertifikat;
	}

	public static String izracunajHes(String ime, String prezime)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {

		MessageDigest md = MessageDigest.getInstance("SHA-224");
		String poruka = ime + " " + prezime;
		byte[] hes = md.digest(poruka.getBytes("UTF-8"));
		return Hex.toHexString(hes);
	}

	public static SecretKey generisiAESKljuc() throws NoSuchAlgorithmException {

		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128);
		return kgen.generateKey();
	}

	public static String enkriptovanjePorukeAESKljucem(byte[] digitalniPotpis, Cipher cipher, byte[] sifrovanAESKljuc,
			String poruka) throws Exception {

		byte[] porukaBajtovi = poruka.getBytes("UTF-8");
		byte[] potpisIPorukaBajtovi = new byte[porukaBajtovi.length + digitalniPotpis.length];
		for (int i = 0; i < potpisIPorukaBajtovi.length; i++) {
			potpisIPorukaBajtovi[i] = (i < digitalniPotpis.length) ? digitalniPotpis[i]
					: porukaBajtovi[i - digitalniPotpis.length];
		}
		byte[] sifrovanPotpisIPorukaBajtovi = cipher.doFinal(potpisIPorukaBajtovi);
		byte[] kompletiraniBajtovi = new byte[sifrovanAESKljuc.length + sifrovanPotpisIPorukaBajtovi.length];
		for (int i = 0; i < kompletiraniBajtovi.length; i++) {
			kompletiraniBajtovi[i] = (i < sifrovanAESKljuc.length) ? sifrovanAESKljuc[i]
					: sifrovanPotpisIPorukaBajtovi[i - sifrovanAESKljuc.length];
		}
		byte[] encodedBytes = Base64.getEncoder().encode(kompletiraniBajtovi);
		return new String(encodedBytes, "UTF-8");
	}

	public static String izracunajImeFajlaZaSlanje(String ime, String prezime)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {

		MessageDigest md = MessageDigest.getInstance("SHA-224");
		String poruka = ime + prezime + new Date();
		byte[] hes = md.digest(poruka.getBytes("UTF-8"));
		return Hex.toHexString(hes);
	}

	public static BufferedImage podesavanjeSlike(BufferedImage img, int height, int width) {
		Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = resized.createGraphics();
		g2d.drawImage(tmp, 0, 0, width, height, null);
		g2d.dispose();
		return resized;
	}
}
