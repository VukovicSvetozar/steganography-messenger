package utility;

import java.io.*;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

public class UzimanjeTajnogKljuca {

	private static String ime;

	public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException, UnrecoverableKeyException {

		// Pristupna lozinka za skladiste
		char[] lozinka = new char[] { 'd', 'e', 'r', 'f' };
		ime = "Fred";

		// Ucitavanje skladista
		KeyStore skladiste = KeyStore.getInstance("JCEKS");
		FileInputStream fis = new FileInputStream("skladiste.JCEKSkeystore");
		skladiste.load(fis, lozinka);

		// Uzimanje tajnog kljuca preko alijasa
		Key kljuc = skladiste.getKey("PrivatniKljuc" + ime, lozinka);
		byte[] privatniKljuc = kljuc.getEncoded();

		// Prikaz tajnog kljuca
		System.out.println("Kljuc je uspjesno preuzet iz skladista!");
		for (int i : privatniKljuc) {
			System.out.print(i + ", ");
		}

		Arrays.fill(lozinka, '\u0000');
		System.out.println("\nKljuc (bit): " + (privatniKljuc.length * 8) + " bitova.");

	}
}
