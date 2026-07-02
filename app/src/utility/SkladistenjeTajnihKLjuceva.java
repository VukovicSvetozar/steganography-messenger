package utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

public class SkladistenjeTajnihKLjuceva implements Putanje {

	private static PrivateKey privatniKljuc;
	private static String ime, prezime;

	public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException, InvalidKeySpecException {

		// Deklarisanje novog skladista
		File fajl = new File("skladiste.JCEKSkeystore");

		// Pristupna lozinka
		char[] lozinka = new char[] { 'd', 'e', 'r', 'f' };
		ime = "Fred";
		prezime = "Flintstone";

		// Definisanje tipa skladista
		KeyStore skladiste = KeyStore.getInstance("JCEKS");
		skladiste.load(null, new char[0]);

		// Ucitavanje tajnog kljuca
		ucitavanjePrivatnogKljuca();

		// Unos novog entiteta za alijass - PrivatniKljuc"Ime"
		skladiste.setKeyEntry("PrivatniKljuc" + ime, privatniKljuc, lozinka, null);
		FileOutputStream fos = new FileOutputStream(fajl);
		skladiste.store(fos, lozinka);

		// Trajno brisanje lozinke skladista
		Arrays.fill(lozinka, '\u0000');
		System.out.println("Privatni kljuc je sacuvan u skladistu kljuceva!");

	}

	private static void ucitavanjePrivatnogKljuca()
			throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		String putanja = KriptoloskeMetode.izracunajHes(ime, prezime);

		File putanjaPrivatnogKljuca = new File(PUTANJA_DO_KORISNICKOG_FOLDERA + putanja + File.separator
				+ PUTANJA_DO_PRIVATNOG_KLJUCA + "PrivatniKljuc.der");

		java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		byte[] privBytes = Files.readAllBytes(Paths.get(putanjaPrivatnogKljuca.getPath()));

		PKCS8EncodedKeySpec specifikacija = new PKCS8EncodedKeySpec(privBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		privatniKljuc = kf.generatePrivate(specifikacija);
	}
}
