package utility;

import java.io.File;

public interface Putanje {

	public final static String PUTANJA_DO_LISTE_KORISNIKA = "Lista korisnika" + File.separator + "Lista korisnika.txt";

	public final static String PUTANJA_DO_SERTIFIKATA = "." + File.separator + "Sertifikati";

	public final static String PUTANJA_DO_KORISNICKIH_SERTIFIKATA = PUTANJA_DO_SERTIFIKATA + File.separator
			+ "Korisnicki sertifikati" + File.separator;

	public final static String PUTANJA_DO_ROOT_CA = PUTANJA_DO_SERTIFIKATA + File.separator + "Root CA"
			+ File.separator;

	public final static String PUTANJA_DO_KORISNICKOG_FOLDERA = "." + File.separator + "Korisnici" + File.separator;

	public final static String PUTANJA_DO_SLIKA = "." + File.separator + "Slike" + File.separator;

	public final static String PUTANJA_DO_PRIVATNOG_KLJUCA = File.separator + "Privatni kljuc" + File.separator;
}
